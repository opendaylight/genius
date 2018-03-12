/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.cache;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.mdsalutil.BigIntCacheKey;
import org.opendaylight.genius.mdsalutil.CacheKeyBase;
import org.opendaylight.infrautils.caches.CacheProvider;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public class KeyedDataObjectCache<K extends CacheKeyBase, V extends DataObject> extends DataObjectCache<V> {

    protected final ConcurrentMap<CacheKeyBase<V>, V> keyedCache = new ConcurrentHashMap<>();
    protected final Class<V> dataObjectClass;

    public KeyedDataObjectCache(Class<V> dataObjectClass, DataBroker dataBroker, LogicalDatastoreType datastoreType,
                                InstanceIdentifier<V> listetenerRegistrationPath, CacheProvider cacheProvider) {
        super(dataObjectClass, dataBroker, datastoreType, listetenerRegistrationPath, cacheProvider);
        this.dataObjectClass = dataObjectClass;
    }

    @Override
    protected void added(InstanceIdentifier<V> path, V dataObject) {
        keyedCache.put(getKey(path, dataObject), dataObject);
    }

    @Override
    protected void removed(InstanceIdentifier<V> path, V dataObject) {
        keyedCache.remove(getKey(path, dataObject));
    }

    public Collection<V> getAll() {
        return keyedCache.values();
    }

    public V get(CacheKeyBase<V> keyBase) {
        return keyedCache.get(keyBase);
    }

    protected CacheKeyBase<V> getKey(InstanceIdentifier<V> path, V dataObj) {
        return new BigIntCacheKey<>(dataObjectClass, path);
    }
}
