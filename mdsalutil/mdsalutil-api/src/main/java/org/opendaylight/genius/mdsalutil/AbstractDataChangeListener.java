/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil;

import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * AbstractDataChangeListener implemented basic {@link DataChangeListener} processing for
 * MDSAL Data Objects.
 */
public abstract class AbstractDataChangeListener<T extends DataObject> implements DataChangeListener {

    protected final Class<T> clazz;

    /**
     * @param clazz - for which the data change event is received
     */
    public AbstractDataChangeListener(Class<T> clazz) {
        this.clazz = Preconditions.checkNotNull(clazz, "Class can not be null!");
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changeEvent) {
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

        this.createData(createdData);
        this.updateData(updateData, originalData);
        this.removeData(removeData, originalData);
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

    protected abstract void remove(InstanceIdentifier<T> identifier, T del);

    protected abstract void update(InstanceIdentifier<T> identifier, T original, T update);

    protected abstract void add(InstanceIdentifier<T> identifier, T add);

}


