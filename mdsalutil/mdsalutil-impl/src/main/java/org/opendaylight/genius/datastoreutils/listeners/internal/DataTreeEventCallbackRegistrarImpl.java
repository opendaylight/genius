/*
 * Copyright (c) 2017 - 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.listeners.internal;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import java.util.Collection;
import java.util.function.BiFunction;
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

    private final DataBroker dataBroker;

    @Inject
    public DataTreeEventCallbackRegistrarImpl(@OsgiService DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    @Override
    public <T extends DataObject> void onUpdate(LogicalDatastoreType store, InstanceIdentifier<T> path,
            BiFunction<T, T, NextAction> callback) {
        on(Operation.UPDATE, store, path, callback);
    }

    @Override
    public <T extends DataObject> void onAdd(LogicalDatastoreType store, InstanceIdentifier<T> path,
            Function<T, NextAction> callback) {
        on(Operation.ADD, store, path, biify(callback));
    }

    @Override
    public <T extends DataObject> void onAddOrUpdate(LogicalDatastoreType store, InstanceIdentifier<T> path,
                                             BiFunction<T, T, NextAction> callback) {
        on(Operation.ADD_OR_UPDATE, store, path, callback);
    }

    @Override
    public <T extends DataObject> void onRemove(LogicalDatastoreType store, InstanceIdentifier<T> path,
            Function<T, NextAction> callback) {
        on(Operation.REMOVE, store, path, biify(callback));
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
            BiFunction<T, T, NextAction> cb) {
        DataTreeIdentifier<T> dtid = new DataTreeIdentifier<>(store, path);
        DataTreeEventCallbackChangeListener<T> listener = new DataTreeEventCallbackChangeListener<>(op, cb);
        ListenerRegistration<DataTreeEventCallbackChangeListener<T>> reg
            = dataBroker.registerDataTreeChangeListener(dtid, listener);
        listener.setRegistration(reg);
    }

    private static final class DataTreeEventCallbackChangeListener<T extends DataObject>
            implements DataTreeChangeListener<T> {

        private final Operation operation;
        private final BiFunction<T, T, NextAction> callback;

        private final Object closeSync = new Object();

        @GuardedBy("closeSync")
        private boolean gotNotification;

        @Nullable
        @GuardedBy("closeSync")
        private ListenerRegistration<DataTreeEventCallbackChangeListener<T>> listenerRegistration;

        DataTreeEventCallbackChangeListener(Operation operation, BiFunction<T, T, NextAction> callback) {
            this.operation = operation;
            this.callback = callback;
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
                    synchronized (closeSync) {
                        if (listenerRegistration != null) {
                            listenerRegistration.close();
                            listenerRegistration = null;
                        } else {
                            gotNotification = true;
                        }
                    }
                    break; // for loop
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
