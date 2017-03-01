/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils;

import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.infra.ThreadFactoryProvider;
import org.opendaylight.genius.utils.SuperTypeUtil;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public abstract class AsyncDataChangeListenerBase<T extends DataObject, K extends DataChangeListener>
        implements DataChangeListener, ChainableDataChangeListener, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(AsyncDataChangeListenerBase.class);

    private static final int DATATREE_CHANGE_HANDLER_THREAD_POOL_CORE_SIZE = 1;
    private static final int DATATREE_CHANGE_HANDLER_THREAD_POOL_MAX_SIZE = 1;
    private static final int DATATREE_CHANGE_HANDLER_THREAD_POOL_KEEP_ALIVE_TIME_SECS = 300;

    private static ThreadPoolExecutor dataChangeHandlerExecutor = new ThreadPoolExecutor(
            DATATREE_CHANGE_HANDLER_THREAD_POOL_CORE_SIZE,
            DATATREE_CHANGE_HANDLER_THREAD_POOL_MAX_SIZE,
            DATATREE_CHANGE_HANDLER_THREAD_POOL_KEEP_ALIVE_TIME_SECS,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            ThreadFactoryProvider.builder()
                .namePrefix("AsyncDataChangeListenerBase-DataChangeHandler")
                .logger(LOG)
                .build().get());

    private ListenerRegistration<?> listenerRegistration;
    private final ChainableDataChangeListenerImpl chainingDelegate = new ChainableDataChangeListenerImpl();
    protected final Class<T> clazz;

    protected AsyncDataChangeListenerBase() {
        this.clazz = SuperTypeUtil.getTypeParameter(getClass(), 0);
    }

    @Deprecated
    public AsyncDataChangeListenerBase(Class<T> clazz, Class<K> eventClazz) {
        this.clazz = Preconditions.checkNotNull(clazz, "Class can not be null!");
    }

    @Override
    public void addBeforeListener(DataChangeListener listener) {
        chainingDelegate.addBeforeListener(listener);
    }

    @Override
    public void addAfterListener(DataChangeListener listener) {
        chainingDelegate.addAfterListener(listener);
    }

    public void registerListener(final LogicalDatastoreType dsType, final DataBroker db) {
        listenerRegistration = db.registerDataChangeListener(
                            dsType, getWildCardPath(), getDataChangeListener(), getDataChangeScope());
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changeEvent) {
        if (changeEvent == null) {
            return;
        }

        DataChangeHandler dataChangeHandler = new DataChangeHandler(changeEvent);
        dataChangeHandlerExecutor.execute(dataChangeHandler);
    }

    @SuppressWarnings("unchecked")
    private void createData(final Map<InstanceIdentifier<?>, DataObject> createdData) {
        for (Map.Entry<InstanceIdentifier<?>, DataObject> createdEntry : createdData.entrySet()) {
            InstanceIdentifier<?> key = createdEntry.getKey();
            if (clazz.equals(key.getTargetType())) {
                InstanceIdentifier<T> createKeyIdent = key.firstIdentifierOf(clazz);
                DataObject value = createdEntry.getValue();
                if (value != null) {
                    this.add(createKeyIdent, (T) value);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void updateData(final Map<InstanceIdentifier<?>, DataObject> updateData,
                            final Map<InstanceIdentifier<?>, DataObject> originalData) {
        for (Map.Entry<InstanceIdentifier<?>, DataObject> updatedEntry : updateData.entrySet()) {
            InstanceIdentifier<?> key = updatedEntry.getKey();
            if (clazz.equals(key.getTargetType())) {
                InstanceIdentifier<T> updateKeyIdent = key.firstIdentifierOf(clazz);
                final DataObject value = updatedEntry.getValue();
                final DataObject original = originalData.get(key);
                if (value != null && original != null) {
                    this.update(updateKeyIdent, (T) original, (T) value);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void removeData(final Set<InstanceIdentifier<?>> removeData,
                            final Map<InstanceIdentifier<?>, DataObject> originalData) {

        for (InstanceIdentifier<?> key : removeData) {
            if (clazz.equals(key.getTargetType())) {
                final InstanceIdentifier<T> ident = key.firstIdentifierOf(clazz);
                final DataObject removeValue = originalData.get(key);
                this.remove(ident, (T)removeValue);
            }
        }
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

    protected abstract void remove(InstanceIdentifier<T> identifier, T del);

    protected abstract void update(InstanceIdentifier<T> identifier, T original, T update);

    protected abstract void add(InstanceIdentifier<T> identifier, T add);

    protected abstract InstanceIdentifier<T> getWildCardPath();

    protected abstract DataChangeListener getDataChangeListener();

    protected abstract AsyncDataBroker.DataChangeScope getDataChangeScope();

    public class DataChangeHandler implements Runnable {
        private final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changeEvent;

        public DataChangeHandler(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changeEvent) {
            chainingDelegate.notifyBeforeOnDataChanged(changeEvent);
            this.changeEvent = changeEvent;
        }

        @Override
        public void run() {
            Preconditions.checkNotNull(changeEvent,"Async ChangeEvent can not be null!");

            /* All DataObjects for create */
            final Map<InstanceIdentifier<?>, DataObject> createdData = changeEvent.getCreatedData() != null
                    ? changeEvent.getCreatedData() : Collections.emptyMap();
            /* All DataObjects for remove */
            final Set<InstanceIdentifier<?>> removeData = changeEvent.getRemovedPaths() != null
                    ? changeEvent.getRemovedPaths() : Collections.emptySet();
            /* All DataObjects for updates */
            final Map<InstanceIdentifier<?>, DataObject> updateData = changeEvent.getUpdatedData() != null
                    ? changeEvent.getUpdatedData() : Collections.emptyMap();
            /* All Original DataObjects */
            final Map<InstanceIdentifier<?>, DataObject> originalData = changeEvent.getOriginalData() != null
                    ? changeEvent.getOriginalData() : Collections.emptyMap();

            createData(createdData);
            updateData(updateData, originalData);
            removeData(removeData, originalData);

            chainingDelegate.notifyAfterOnDataChanged(changeEvent);
        }
    }
}
