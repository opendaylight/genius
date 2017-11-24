/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.listeners.internal;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of DataTreeEventCallbackRegistrar.
 *
 * @author Josh original idea and design feedback
 * @author Michael Vorburger.ch API design and implementation
 * @author Tom Pantelis feedback on concurrency issue in implementation
 */
@Singleton
@OsgiServiceProvider(classes = DataTreeEventCallbackRegistrar.class)
public class DataTreeEventCallbackRegistrarImpl implements DataTreeEventCallbackRegistrar {

    private static final Logger LOG = LoggerFactory.getLogger(DataTreeEventCallbackRegistrarImpl.class);

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
            BiFunction<T, T, UnregisterOrCallAgain> callback) {
        on(Operation.UPDATE, store, path, callback);
    }

    @Override
    public <T extends DataObject> void onAdd(LogicalDatastoreType store, InstanceIdentifier<T> path,
            Function<T, UnregisterOrCallAgain> callback) {
        on(Operation.ADD, store, path, biify(callback));
    }

    @Override
    public <T extends DataObject> void onRemove(LogicalDatastoreType store, InstanceIdentifier<T> path,
            Function<T, UnregisterOrCallAgain> callback) {
        on(Operation.REMOVE, store, path, biify(callback));
    }

    private <T, U, R> BiFunction<T, T, R> biify(Function<T, R> function) {
        return (first, second) -> function.apply(first);
    }

    @SuppressWarnings("resource") // thanks but we're good
    private <T extends DataObject> void on(Operation op, LogicalDatastoreType store, InstanceIdentifier<T> path,
            BiFunction<T, T, UnregisterOrCallAgain> cb) {
        DataTreeIdentifier<T> dtid = new DataTreeIdentifier<>(store, path);
        DataTreeEventCallbackChangeListener<T> listener = new DataTreeEventCallbackChangeListener<>(Operation.ADD, cb);
        ListenerRegistration<DataTreeEventCallbackChangeListener<T>> reg
            = dataBroker.registerDataTreeChangeListener(dtid, listener);
        listener.setRegistration(reg);
    }

    private static final class DataTreeEventCallbackChangeListener<T extends DataObject>
            implements DataTreeChangeListener<T> {

        private final Operation operation;
        private final BiFunction<T, T, UnregisterOrCallAgain> callback;

        private final Object closeSync = new Object();

        @GuardedBy("closeSync")
        private boolean gotNotification;

        @Nullable
        @GuardedBy("closeSync")
        private ListenerRegistration<DataTreeEventCallbackChangeListener<T>> listenerRegistration;

        DataTreeEventCallbackChangeListener(Operation operation, BiFunction<T, T, UnregisterOrCallAgain> callback) {
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

                UnregisterOrCallAgain unregisterOrCallAgain;
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
                        unregisterOrCallAgain = UnregisterOrCallAgain.UNREGISTER;
                        break; // switch
                }

                if (unregisterOrCallAgain.equals(UnregisterOrCallAgain.UNREGISTER)) {
                    synchronized (closeSync) {
                        if (listenerRegistration != null) {
                            listenerRegistration.close();
                        } else {
                            gotNotification = true;
                        }
                    }
                    break; // for loop
                }
            }
        }

        UnregisterOrCallAgain add(T newDataObject) {
            if (operation == Operation.ADD) {
                return callback.apply(newDataObject, null);
            } else {
                return UnregisterOrCallAgain.CALL_AGAIN;
            }
        }

        UnregisterOrCallAgain remove(T removedDataObject) {
            if (operation == Operation.REMOVE) {
                return callback.apply(removedDataObject, null);
            } else {
                return UnregisterOrCallAgain.CALL_AGAIN;
            }
        }

        UnregisterOrCallAgain update(T originalDataObject, T updatedDataObject) {
            if (operation == Operation.UPDATE) {
                return callback.apply(originalDataObject, updatedDataObject);
            } else {
                return UnregisterOrCallAgain.CALL_AGAIN;
            }
        }

    }

    private enum Operation { ADD, UPDATE, REMOVE }

}
