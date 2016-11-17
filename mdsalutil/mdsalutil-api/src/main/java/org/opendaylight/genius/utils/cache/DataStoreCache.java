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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
        Object dataObject = getCache(cacheName).get(key);
        if (dataObject == null) {
            Optional<T> datastoreObject = MDSALDataStoreUtils.read(broker,
                    isConfig ? LogicalDatastoreType.CONFIGURATION : LogicalDatastoreType.OPERATIONAL, identifier);
            if (datastoreObject.isPresent()) {
                dataObject = datastoreObject.get();
                add(cacheName, key, dataObject);
            }
        }
        return dataObject;
    }

    public static Object get( String cacheName, Object key ) {
        return getCache( cacheName).get(key) ;
    }

    public static void remove(String cacheName, Object key) {
        getCache(cacheName).remove(key);
    }

    private static ConcurrentMap<Object, Object> getCache(String cacheName) {
        return  (ConcurrentMap<Object, Object>)CacheUtil.getCache(cacheName);
    }

    public static boolean isCacheValid(String cacheName) {
         return CacheUtil.isCacheValid( cacheName ) ;
    }

    public static List<Object> getValues( String cacheName) {
        ConcurrentHashMap<Object, Object> map = (ConcurrentHashMap<Object,Object>)DataStoreCache.getCache(cacheName);
        List<Object> values = null ;
        if(  map != null ) {
             if( map.entrySet() != null ) {
                 values = new ArrayList<>();
                 for (Map.Entry<Object, Object> entry : map.entrySet()) {
                     values.add(entry.getValue());
                 }
             }
        }
        return values ;
    }

    public static List<Object> getKeys( String cacheName) {
        ConcurrentHashMap<Object, Object> map = (ConcurrentHashMap<Object,Object>)DataStoreCache.getCache(cacheName);
        List<Object> keys = null ;
        if( map != null ) {
            if( map.keys() != null) {
                keys = Collections.list(map.keys());
            }
        }
        return keys;
    }

}
