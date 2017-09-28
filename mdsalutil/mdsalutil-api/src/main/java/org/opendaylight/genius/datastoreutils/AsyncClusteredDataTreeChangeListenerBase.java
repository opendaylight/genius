/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.datastoreutils;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import javax.annotation.PreDestroy;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.utils.SuperTypeUtil;
import org.opendaylight.infrautils.utils.concurrent.Executors;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public abstract class AsyncClusteredDataTreeChangeListenerBase
    <T extends DataObject, K extends ClusteredDataTreeChangeListener<T>>
        implements ClusteredDataTreeChangeListener<T>, ChainableDataTreeChangeListener<T>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(AsyncClusteredDataTreeChangeListenerBase.class);

    private ListenerRegistration<K> listenerRegistration;
    private final ChainableDataTreeChangeListenerImpl<T> chainingDelegate = new ChainableDataTreeChangeListenerImpl<>();

    private final ExecutorService dataTreeChangeHandlerExecutor =
            Executors.newSingleThreadExecutor("AsyncClusteredDataTreeChangeListenerBase-DataTreeChangeHandler", LOG);

    protected final Class<T> clazz;

    protected AsyncClusteredDataTreeChangeListenerBase() {
        this.clazz = SuperTypeUtil.getTypeParameter(getClass(), 0);
    }

    @Deprecated
    public AsyncClusteredDataTreeChangeListenerBase(Class<T> clazz, Class<K> eventClazz) {
        this.clazz = Preconditions.checkNotNull(clazz, "Class can not be null!");
    }

    @Override
    public void addBeforeListener(DataTreeChangeListener<T> listener) {
        chainingDelegate.addBeforeListener(listener);
    }

    @Override
    public void addAfterListener(DataTreeChangeListener<T> listener) {
        chainingDelegate.addAfterListener(listener);
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<T>> changes) {
        if (changes == null || changes.isEmpty()) {
            return;
        }

        DataTreeChangeHandler dataTreeChangeHandler = new DataTreeChangeHandler(changes);
        dataTreeChangeHandlerExecutor.execute(dataTreeChangeHandler);
    }

    public void registerListener(LogicalDatastoreType dsType, final DataBroker db) {
        final DataTreeIdentifier<T> treeId = new DataTreeIdentifier<>(dsType, getWildCardPath());
        listenerRegistration = db.registerDataTreeChangeListener(treeId, getDataTreeChangeListener());
    }

    @Override
    @PreDestroy
    public void close() {
        dataTreeChangeHandlerExecutor.shutdownNow();

        if (listenerRegistration != null) {
            try {
                listenerRegistration.close();
            } finally {
                listenerRegistration = null;
            }
        }
    }

    protected abstract InstanceIdentifier<T> getWildCardPath();

    protected abstract void remove(InstanceIdentifier<T> key, T dataObjectModification);

    protected abstract void update(InstanceIdentifier<T> key,
            T dataObjectModificationBefore, T dataObjectModificationAfter);

    protected abstract void add(InstanceIdentifier<T> key, T dataObjectModification);

    protected abstract K getDataTreeChangeListener();

    public class DataTreeChangeHandler implements Runnable {
        private final Collection<DataTreeModification<T>> changes;

        public DataTreeChangeHandler(Collection<DataTreeModification<T>> changes) {
            chainingDelegate.notifyBeforeOnDataTreeChanged(changes);
            this.changes = changes;
        }

        @Override
        public void run() {
            for (DataTreeModification<T> change : changes) {
                final InstanceIdentifier<T> key = change.getRootPath().getRootIdentifier();
                final DataObjectModification<T> mod = change.getRootNode();

                switch (mod.getModificationType()) {
                    case DELETE:
                        remove(key, mod.getDataBefore());
                        break;
                    case SUBTREE_MODIFIED:
                        update(key, mod.getDataBefore(), mod.getDataAfter());
                        break;
                    case WRITE:
                        if (mod.getDataBefore() == null) {
                            add(key, mod.getDataAfter());
                        } else {
                            update(key, mod.getDataBefore(), mod.getDataAfter());
                        }
                        break;
                    default:
                        // FIXME: May be not a good idea to throw.
                        throw new IllegalArgumentException("Unhandled modification type " + mod.getModificationType());
                }
            }
            chainingDelegate.notifyAfterOnDataTreeChanged(changes);
        }
    }
}
