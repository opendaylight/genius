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
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
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
 * {@link org.opendaylight.serviceutils.tools.mdsal.listener.AbstractAsyncDataTreeChangeListener} instead of this!
 */
@Deprecated
public abstract class AsyncDataTreeChangeListenerBase<T extends DataObject, K extends DataTreeChangeListener<T>>
        implements DataTreeChangeListener<T>, ChainableDataTreeChangeListener<T>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(AsyncDataTreeChangeListenerBase.class);

    private ListenerRegistration<K> listenerRegistration;
    private final ChainableDataTreeChangeListenerImpl<T> chainingDelegate = new ChainableDataTreeChangeListenerImpl<>();
    private final ExecutorService dataTreeChangeHandlerExecutor;
    protected final Class<T> clazz;
    private DataStoreMetrics dataStoreMetrics;

    protected AsyncDataTreeChangeListenerBase() {
        this.clazz = SuperTypeUtil.getTypeParameter(getClass(), 0);
        this.dataTreeChangeHandlerExecutor = newThreadPoolExecutor(clazz);
    }

    protected AsyncDataTreeChangeListenerBase(MetricProvider metricProvider) {
        this();
        this.dataStoreMetrics = new DataStoreMetrics(metricProvider, getClass());
    }

    @Deprecated
    public AsyncDataTreeChangeListenerBase(Class<T> clazz, Class<K> eventClazz) {
        this.clazz = Preconditions.checkNotNull(clazz, "Class can not be null!");
        this.dataTreeChangeHandlerExecutor = newThreadPoolExecutor(clazz);
    }

    private static ExecutorService newThreadPoolExecutor(Class<?> clazz) {
        return Executors.newSingleThreadExecutor(
                // class name first so it shows up in logs' prefix, but fixed length
                clazz.getName() + "_AsyncDataTreeChangeListenerBase-DataTreeChangeHandler", LOG);
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

    /**
     * Subclasses override this and place initialization logic here, notably
     * calls to registerListener(). Note that the overriding method MUST repeat
     * the PostConstruct annotation, because JSR 250 specifies that lifecycle
     * methods "are called unless a subclass of the declaring class overrides
     * the method without repeating the annotation".  (The blueprint-maven-plugin
     * would gen. XML which calls PostConstruct annotated methods even if they are
     * in a subclass without repeating the annotation, but this is wrong and not
     * JSR 250 compliant, and while working in BP, then causes issues e.g. when
     * wiring with Guice for tests, so do always repeat it.)
     */
    @PostConstruct
    protected void init() {
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
            chainingDelegate.notifyAfterOnDataTreeChanged(changes);
        }
    }
}
