/*
 * Copyright (c) 2017 - 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.listeners.internal;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.listeners.DataTreeEventCallbackRegistrar;
import org.opendaylight.infrautils.utils.concurrent.Executors;
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
@Service(classes = DataTreeEventCallbackRegistrar.class)
public class DataTreeEventCallbackRegistrarImpl implements DataTreeEventCallbackRegistrar {

    // This implementation is, intentionally, kept very simple and thin.  If during usage we see
    // that registering many listeners to the DataBroker causes any sort of real overhead, then we will
    // change this implementation to register 1 single listener to the DataBroker, and instead do the
    // dispatching etc. ourselves within this implementation.

    private static final Logger LOG = LoggerFactory.getLogger(DataTreeEventCallbackRegistrarImpl.class);

    private final DataBroker dataBroker;

    private final ScheduledExecutorService scheduledExecutorService;

    @Inject
    public DataTreeEventCallbackRegistrarImpl(@Reference DataBroker dataBroker) {
        this(dataBroker, Executors.newSingleThreadScheduledExecutor("DataTreeEventCallbackRegistrar-Timeouter", LOG));
    }

    @VisibleForTesting
    public DataTreeEventCallbackRegistrarImpl(DataBroker dataBroker,
            ScheduledExecutorService scheduledExecutorService) {
        this.dataBroker = dataBroker;
        this.scheduledExecutorService = scheduledExecutorService;
    }

    @Override
    public <T extends DataObject> void onUpdate(LogicalDatastoreType store, InstanceIdentifier<T> path,
            BiFunction<T, T, NextAction> callback) {
        on(Operation.UPDATE, store, path, callback, Duration.ZERO, null);
    }

    @Override
    public <T extends DataObject> void onUpdate(LogicalDatastoreType store, InstanceIdentifier<T> path,
            BiFunction<T, T, NextAction> callback, Duration timeoutDuration,
            Consumer<DataTreeIdentifier<T>> timedOutCallback) {
        validateTimeout(timeoutDuration);
        on(Operation.UPDATE, store, path, callback, timeoutDuration,
                requireNonNull(timedOutCallback, "timedOutCallback"));
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
        validateTimeout(timeoutDuration);
        on(Operation.ADD, store, path, biify(callback), timeoutDuration,
                requireNonNull(timedOutCallback, "timedOutCallback"));
    }

    @Override
    public <T extends DataObject> void onAddOrUpdate(LogicalDatastoreType store, InstanceIdentifier<T> path,
                                             BiFunction<T, T, NextAction> callback) {
        on(Operation.ADD_OR_UPDATE, store, path, callback, Duration.ZERO, null);
    }

    @Override
    public <T extends DataObject> void onAddOrUpdate(LogicalDatastoreType store, InstanceIdentifier<T> path,
            BiFunction<@org.eclipse.jdt.annotation.Nullable T, T, NextAction> callback, Duration timeoutDuration,
            Consumer<DataTreeIdentifier<T>> timedOutCallback) {
        validateTimeout(timeoutDuration);
        on(Operation.ADD_OR_UPDATE, store, path, callback, timeoutDuration,
                requireNonNull(timedOutCallback, "timedOutCallback"));
    }

    @Override
    public <T extends DataObject> void onRemove(LogicalDatastoreType store, InstanceIdentifier<T> path,
            Function<T, NextAction> callback) {
        on(Operation.REMOVE, store, path, biify(callback), Duration.ZERO, null);
    }

    @Override
    public <T extends DataObject> void onRemove(LogicalDatastoreType store, InstanceIdentifier<T> path,
            Function<T, NextAction> callback, Duration timeoutDuration,
            Consumer<DataTreeIdentifier<T>> timedOutCallback) {
        validateTimeout(timeoutDuration);
        on(Operation.REMOVE, store, path, biify(callback), timeoutDuration,
                requireNonNull(timedOutCallback, "timedOutCallback"));
    }

    private static void validateTimeout(Duration timeoutDuration) {
        if (timeoutDuration.isZero() || timeoutDuration.isNegative()) {
            throw new IllegalArgumentException("timeoutDuration <= 0");
        }
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

    private <T extends DataObject> void on(Operation op, LogicalDatastoreType store, InstanceIdentifier<T> path,
            BiFunction<T, T, NextAction> cb, Duration timeoutDuration,
            @Nullable Consumer<DataTreeIdentifier<T>> timedOutCallback) {
        DataTreeIdentifier<T> dtid = new DataTreeIdentifier<>(store, path);
        DataTreeEventCallbackChangeListener<T> listener = new DataTreeEventCallbackChangeListener<>(op, cb, () -> {
            if (timedOutCallback != null) {
                timedOutCallback.accept(dtid);
            }
        });

        if (timedOutCallback != null) {
            listener.setTimeOutScheduledFuture(
                    scheduledExecutorService.schedule(listener, timeoutDuration.toMillis(), TimeUnit.MILLISECONDS));
        }

        ListenerRegistration<DataTreeEventCallbackChangeListener<T>> reg
            = dataBroker.registerDataTreeChangeListener(dtid, listener);
        listener.setRegistration(reg);
    }

    private static final class DataTreeEventCallbackChangeListener<T extends DataObject>
            implements ClusteredDataTreeChangeListener<T>, Runnable {

        private final Operation operation;
        private final BiFunction<T, T, NextAction> callback;
        private final Runnable timedOutCallback;

        private volatile @Nullable ScheduledFuture<?> timeOutScheduledFuture;

        private final Object closeSync = new Object();
        private final Object notificationSync = new Object();

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
            synchronized (notificationSync) {
                if (timeOutScheduledFuture != null && timeOutScheduledFuture.isDone()) {
                    LOG.debug("Timeout task already ran");
                    return;
                }

                NextAction unregisterOrCallAgain;
                for (final DataTreeModification<T> dataTreeModification : changes) {
                    final DataObjectModification<T> dataObjectModification = dataTreeModification.getRootNode();
                    final T dataBefore = dataObjectModification.getDataBefore();
                    final T dataAfter = dataObjectModification.getDataAfter();

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
                        if (timeOutScheduledFuture != null) {
                            if (!timeOutScheduledFuture.cancel(false)) {
                                LOG.warn("Timeout scheduled task could not be cancelled; possibly concurrency issue!");
                            } else {
                                LOG.debug("Successfully cancelled the scheduled timeout task");
                            }
                        }
                        break; // for loop
                    }
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

        void setTimeOutScheduledFuture(ScheduledFuture<?> scheduledFuture) {
            this.timeOutScheduledFuture = scheduledFuture;
        }

        @Override
        // This gets invoked on timeout (if any)
        public void run() {
            synchronized (notificationSync) {
                if (!timeOutScheduledFuture.isDone()) {
                    closeRegistration();
                    timedOutCallback.run();
                    LOG.debug("Closed datastore listener and ran the time-out task now");
                }
            }
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
