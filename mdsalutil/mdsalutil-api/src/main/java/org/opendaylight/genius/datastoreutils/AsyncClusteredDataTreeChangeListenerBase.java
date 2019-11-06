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
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.utils.SuperTypeUtil;
import org.opendaylight.infrautils.metrics.MetricProvider;
import org.opendaylight.infrautils.utils.concurrent.Executors;
import org.opendaylight.serviceutils.tools.mdsal.listener.ChainableDataTreeChangeListener;
import org.opendaylight.serviceutils.tools.mdsal.listener.ChainableDataTreeChangeListenerImpl;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deprecated DS listener.
 * @deprecated Please use
 * {@link org.opendaylight.serviceutils.tools.mdsal.listener.AbstractClusteredAsyncDataTreeChangeListener}
 *     instead of this!
 */
@Deprecated
public abstract class AsyncClusteredDataTreeChangeListenerBase
    <T extends DataObject, K extends ClusteredDataTreeChangeListener<T>>
        implements ClusteredDataTreeChangeListener<T>, ChainableDataTreeChangeListener<T>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(AsyncClusteredDataTreeChangeListenerBase.class);

    private ListenerRegistration<K> listenerRegistration;
    private final ChainableDataTreeChangeListenerImpl<T> chainingDelegate = new ChainableDataTreeChangeListenerImpl<>();
    private final ExecutorService dataTreeChangeHandlerExecutor;
    protected final Class<T> clazz;
    private @Nullable DataStoreMetrics dataStoreMetrics;

    protected AsyncClusteredDataTreeChangeListenerBase() {
        this.clazz = SuperTypeUtil.getTypeParameter(getClass(), 0);
        this.dataTreeChangeHandlerExecutor = newThreadPoolExecutor(clazz);
    }

    @Deprecated
    public AsyncClusteredDataTreeChangeListenerBase(Class<T> clazz, Class<K> eventClazz) {
        this.clazz = Preconditions.checkNotNull(clazz, "Class can not be null!");
        this.dataTreeChangeHandlerExecutor = newThreadPoolExecutor(clazz);
    }

    protected AsyncClusteredDataTreeChangeListenerBase(MetricProvider metricProvider) {
        this();
        this.dataStoreMetrics = new DataStoreMetrics(metricProvider, getClass());
    }

    private static ExecutorService newThreadPoolExecutor(Class<?> clazz) {
        return Executors.newSingleThreadExecutor(
                // class name first so it shows up in logs' prefix, but fixed length
                clazz.getName() + "_AsyncClusteredDataTreeChangeListenerBase-DataTreeChangeHandler", LOG);
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

    public void deregisterListener() {
        if (listenerRegistration != null) {
            try {
                listenerRegistration.close();
            } finally {
                listenerRegistration = null;
            }
        }
    }

    @Override
    @PreDestroy
    public void close() {
        dataTreeChangeHandlerExecutor.shutdownNow();
        deregisterListener();
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
                try {
                    processDataTreeModification(change);
                } catch (IllegalStateException e) {
                    LOG.error("Catch an IllegalStateException: ", e);
                } catch (IllegalArgumentException e1) {
                    LOG.error("Catch an IllegalArgumentException: ", e1);
                } catch (Exception e2) {
                    LOG.error("Catch an Exception: {}.", e2.getMessage());
                }
            }
            chainingDelegate.notifyAfterOnDataTreeChanged(changes);
        }

        private void processDataTreeModification(DataTreeModification<T> change) {
            final InstanceIdentifier<T> key = change.getRootPath().getRootIdentifier();
            final DataObjectModification<T> mod = change.getRootNode();

            switch (mod.getModificationType()) {
                case DELETE:
                    if (dataStoreMetrics != null) {
                        dataStoreMetrics.incrementDeleted();
                    }
                    remove(key, mod.getDataBefore());
                    break;
                case SUBTREE_MODIFIED:
                    if (dataStoreMetrics != null) {
                        dataStoreMetrics.incrementUpdated();
                    }
                    update(key, mod.getDataBefore(), mod.getDataAfter());
                    break;
                case WRITE:
                    if (mod.getDataBefore() == null) {
                        if (dataStoreMetrics != null) {
                            dataStoreMetrics.incrementAdded();
                        }
                        add(key, mod.getDataAfter());
                    } else {
                        if (dataStoreMetrics != null) {
                            dataStoreMetrics.incrementUpdated();
                        }
                        update(key, mod.getDataBefore(), mod.getDataAfter());
                    }
                    break;
                default:
                    // FIXME: May be not a good idea to throw.
                    throw new IllegalArgumentException("Unhandled modification type " + mod.getModificationType());
            }
        }
    }
}
