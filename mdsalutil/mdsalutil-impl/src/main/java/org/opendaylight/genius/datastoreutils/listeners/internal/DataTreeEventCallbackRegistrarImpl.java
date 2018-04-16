/*
 * Copyright (c) 2017 - 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.listeners.internal;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.infrautils.utils.concurrent.Executors.newScheduledThreadPool;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.listeners.DataTreeEventCallbackRegistrar;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.ops4j.pax.cdi.api.OsgiService;
import org.ops4j.pax.cdi.api.OsgiServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of DataTreeEventCallbackRegistrar.
 *
 * @author Josh original idea and design feedback
 * @author Michael Vorburger.ch API design and first implementation
 * @author Tom Pantelis review and feedback on concurrency issue in implementation
 */
@Beta
@Singleton
@OsgiServiceProvider(classes = DataTreeEventCallbackRegistrar.class)
public class DataTreeEventCallbackRegistrarImpl implements DataTreeEventCallbackRegistrar {

    // This implementation is, intentionally, kept very simple and thin.  If during usage we see
    // that registering many listeners to the DataBroker causes any sort of real overhead, then we will
    // change this implementation to register 1 single listener to the DataBroker, and instead do the
    // dispatching etc. ourselves within this implementation.

    private static final Logger LOG = LoggerFactory.getLogger(DataTreeEventCallbackRegistrarImpl.class);

    private final DataBroker dataBroker;

    private final ScheduledExecutorService scheduledExecutorService;

    @Inject
    public DataTreeEventCallbackRegistrarImpl(@OsgiService DataBroker dataBroker) {
        this.dataBroker = dataBroker;

        int timeoutCallbackThreadPoolSize = 16; // arbitrary value; perhaps should be made configurable?
        this.scheduledExecutorService = newScheduledThreadPool(timeoutCallbackThreadPoolSize,
                "DataTreeEventCallbackRegistrar-Timeouter", LOG);
    }

    @Override
    public <T extends DataObject> void onUpdate(LogicalDatastoreType store, InstanceIdentifier<T> path,
            BiFunction<T, T, NextAction> callback) {
        on(Operation.UPDATE, store, path, callback, Duration.ZERO, null);
    }

    @Override
    public <T extends DataObject> void onAdd(LogicalDatastoreType store, InstanceIdentifier<T> path,
            Function<T, NextAction> callback) {
        on(Operation.ADD, store, path, biify(callback), Duration.ZERO, null);
    }

    @Override
    public <T extends DataObject> void onAdd(LogicalDatastoreType store, InstanceIdentifier<T> path,
            Function<T, NextAction> callback, Duration timeoutDuration,
            Consumer<DataTreeIdentifier<T>> timedOutCallback) {
        if (timeoutDuration.isZero() || timeoutDuration.isNegative()) {
            throw new IllegalArgumentException("timeoutDuration <= 0");
        }
        on(Operation.ADD, store, path, biify(callback), timeoutDuration,
                requireNonNull(timedOutCallback, "timedOutCallback"));
    }

    @Override
    public <T extends DataObject> void onAddOrUpdate(LogicalDatastoreType store, InstanceIdentifier<T> path,
                                             BiFunction<T, T, NextAction> callback) {
        on(Operation.ADD_OR_UPDATE, store, path, callback, Duration.ZERO, null);
    }

    @Override
    public <T extends DataObject> void onRemove(LogicalDatastoreType store, InstanceIdentifier<T> path,
            Function<T, NextAction> callback) {
        on(Operation.REMOVE, store, path, biify(callback), Duration.ZERO, null);
    }

    private <T, U, R> BiFunction<T, T, R> biify(Function<T, R> function) {
        return new BiFunction<T, T, R>() {
            @Override
            public R apply(T first, T second) {
                return function.apply(first);
            }

            @Override
            public String toString() {
                return "IgnoringSecondBiFunction{" + function + "}";
            }
        };
    }

    @SuppressWarnings("resource") // thanks but we're good
    private <T extends DataObject> void on(Operation op, LogicalDatastoreType store, InstanceIdentifier<T> path,
            BiFunction<T, T, NextAction> cb, Duration timeoutDuration,
            @Nullable Consumer<DataTreeIdentifier<T>> timedOutCallback) {
        DataTreeIdentifier<T> dtid = new DataTreeIdentifier<>(store, path);
        DataTreeEventCallbackChangeListener<T> listener = new DataTreeEventCallbackChangeListener<>(op, cb,
            () -> timedOutCallback.accept(dtid));
        ListenerRegistration<DataTreeEventCallbackChangeListener<T>> reg
            = dataBroker.registerDataTreeChangeListener(dtid, listener);
        listener.setRegistration(reg);

        if (timedOutCallback != null) {
            scheduledExecutorService.schedule(listener, timeoutDuration.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    private static final class DataTreeEventCallbackChangeListener<T extends DataObject>
            implements DataTreeChangeListener<T>, Runnable {

        private final Operation operation;
        private final BiFunction<T, T, NextAction> callback;
        private final Runnable timedOutCallback;

        private final Object closeSync = new Object();

        @GuardedBy("closeSync")
        private boolean gotNotification;

        @Nullable
        @GuardedBy("closeSync")
        private ListenerRegistration<DataTreeEventCallbackChangeListener<T>> listenerRegistration;

        DataTreeEventCallbackChangeListener(Operation operation, BiFunction<T, T, NextAction> callback,
                Runnable timedOutCallback) {
            this.operation = operation;
            this.callback = callback;
            this.timedOutCallback = timedOutCallback;
        }

        void setRegistration(ListenerRegistration<DataTreeEventCallbackChangeListener<T>> reg) {
            synchronized (closeSync) {
                if (gotNotification) {
                    reg.close();
                } else {
                    this.listenerRegistration = reg;
                }
            }
        }

        @Override
        public void onDataTreeChanged(Collection<DataTreeModification<T>> changes) {
            // Code almost identical to org.opendaylight.genius.datastoreutils.listeners.DataTreeChangeListenerActions
            for (final DataTreeModification<T> dataTreeModification : changes) {
                final DataObjectModification<T> dataObjectModification = dataTreeModification.getRootNode();
                final T dataBefore = dataObjectModification.getDataBefore();
                final T dataAfter = dataObjectModification.getDataAfter();

                NextAction unregisterOrCallAgain;
                switch (dataObjectModification.getModificationType()) {
                    case SUBTREE_MODIFIED:
                        unregisterOrCallAgain = update(dataBefore, dataAfter);
                        break; // switch
                    case DELETE:
                        unregisterOrCallAgain = remove(dataBefore);
                        break; // switch
                    case WRITE:
                        if (dataBefore == null) {
                            unregisterOrCallAgain = add(dataAfter);
                        } else {
                            unregisterOrCallAgain = update(dataBefore, dataAfter);
                        }
                        break; // switch
                    default:
                        unregisterOrCallAgain = NextAction.UNREGISTER;
                        break; // switch
                }

                if (unregisterOrCallAgain.equals(NextAction.UNREGISTER)) {
                    closeRegistration();
                    break; // for loop
                }
            }
        }

        void closeRegistration() {
            synchronized (closeSync) {
                if (listenerRegistration != null) {
                    listenerRegistration.close();
                    listenerRegistration = null;
                } else {
                    gotNotification = true;
                }
            }
        }

        @Override
        // This gets invoked on timeout (if any)
        public void run() {
            closeRegistration();
            timedOutCallback.run();
        }

        NextAction add(T newDataObject) {
            switch (operation) {
                case ADD:
                    return callback.apply(newDataObject, null);
                case ADD_OR_UPDATE:
                    return callback.apply(null, newDataObject);
                default:
                    return NextAction.CALL_AGAIN;
            }
        }

        NextAction remove(T removedDataObject) {
            if (operation == Operation.REMOVE) {
                return callback.apply(removedDataObject, null);
            } else {
                return NextAction.CALL_AGAIN;
            }
        }

        NextAction update(T originalDataObject, T updatedDataObject) {
            if (operation == Operation.UPDATE || operation == Operation.ADD_OR_UPDATE) {
                return callback.apply(originalDataObject, updatedDataObject);
            } else {
                return NextAction.CALL_AGAIN;
            }
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("operation", operation)
                    .add("gotNotification", gotNotification)
                    .add("callback", callback).toString();
        }
    }

    private enum Operation { ADD, UPDATE, ADD_OR_UPDATE, REMOVE }

}
