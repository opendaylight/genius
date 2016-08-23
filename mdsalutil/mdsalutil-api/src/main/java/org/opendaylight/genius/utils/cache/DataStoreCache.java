/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.utils.cache;

import com.google.common.base.Optional;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.mdsalutil.MDSALDataStoreUtils;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.concurrent.ConcurrentMap;

/**
 * Manages a per-blade cache, which is feeded by a clustered data change
 * listener.
 *
 */
public class DataStoreCache {
    public static void create(String cacheName) {
        if (CacheUtil.getCache(cacheName) == null) {
            CacheUtil.createCache(cacheName);
        }
    }

    public static void add(String cacheName, Object key, Object value) {
        getCache(cacheName).put(key, value);
    }

    public static <T extends DataObject> Object get(String cacheName, InstanceIdentifier<T> identifier, String key, DataBroker broker, boolean isConfig) {
        if(getCache(cacheName)!=null) {
            Object dataObject = getCache(cacheName).get(key);
            Optional<T> optionalObject = Optional.absent();
            if (dataObject == null) {
                if (isConfig) {
                    optionalObject = MDSALDataStoreUtils.read(broker, LogicalDatastoreType.CONFIGURATION, identifier);
                } else {
                    optionalObject = MDSALDataStoreUtils.read(broker, LogicalDatastoreType.OPERATIONAL, identifier);
                }
                if (optionalObject.isPresent()) {
                    dataObject = optionalObject.get();
                    add(cacheName, key, dataObject);
                }
            }
            return dataObject;
        }
        else
            return null;
    }

    public static void remove(String cacheName, String key) {
        getCache(cacheName).remove(key);
    }

    private static ConcurrentMap<Object, Object> getCache(String cacheName) {
        return  (ConcurrentMap<Object, Object>)CacheUtil.getCache(cacheName);
    }


}
