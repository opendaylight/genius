/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.datastoreutils;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.utils.SuperTypeUtil;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public abstract class AsyncChainableClusteredDataTreeChangeListenerBase
    <T extends DataObject, T2 extends DataObject, K extends ClusteredDataTreeChangeListener<T>>
        implements ClusteredDataTreeChangeListener<T>, ChainableDataTreeChangeListener<T2>, AutoCloseable {

    private static final int DATATREE_CHANGE_HANDLER_THREAD_POOL_CORE_SIZE = 1;
    private static final int DATATREE_CHANGE_HANDLER_THREAD_POOL_MAX_SIZE = 1;
    private static final int DATATREE_CHANGE_HANDLER_THREAD_POOL_KEEP_ALIVE_TIME_SECS = 300;
    private final ChainableDataTreeChangeListenerImpl<T2> chainingDelegate = new ChainableDataTreeChangeListenerImpl<>();

    private ListenerRegistration<K> listenerRegistration;

    private static ThreadPoolExecutor dataTreeChangeHandlerExecutor = new ThreadPoolExecutor(
            DATATREE_CHANGE_HANDLER_THREAD_POOL_CORE_SIZE,
            DATATREE_CHANGE_HANDLER_THREAD_POOL_MAX_SIZE,
            DATATREE_CHANGE_HANDLER_THREAD_POOL_KEEP_ALIVE_TIME_SECS,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>());

    protected final Class<T> clazz;

    protected AsyncChainableClusteredDataTreeChangeListenerBase() {
        this.clazz = SuperTypeUtil.getTypeParameter(getClass(), 0);
    }

    @Override
    public void addBeforeListener(DataTreeChangeListener<T2> listener) {
        chainingDelegate.addBeforeListener(listener);
    }

    @Override
    public void addAfterListener(DataTreeChangeListener<T2> listener) {
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

    protected abstract Collection<DataTreeModification<T2>>getDelegateChanges(
        Collection<DataTreeModification<T>> changes);

    public class DataTreeChangeHandler implements Runnable {
        private final Collection<DataTreeModification<T>> changes;
        private final Collection<DataTreeModification<T2>> delegatedChanges;

        public DataTreeChangeHandler(Collection<DataTreeModification<T>> changes) {
            this.delegatedChanges = getNullSafeDelegateChanges(changes);
            chainingDelegate.notifyBeforeOnDataTreeChanged(delegatedChanges);
            this.changes = changes;
        }

        private Collection<DataTreeModification<T2>> getNullSafeDelegateChanges(Collection<DataTreeModification<T>> changes2) {
            Collection<DataTreeModification<T2>> delegateChanges = getDelegateChanges(changes);
            return delegateChanges == null ? Collections.emptyList() : delegateChanges;
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
            chainingDelegate.notifyAfterOnDataTreeChanged(delegatedChanges);
        }
    }
}
