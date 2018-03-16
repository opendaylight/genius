/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.utils.cache;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.genius.mdsalutil.cache.DataObjectCache;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * "Per-blade" (?) Cache for DataBroker DataObject reads.
 *
 * <p>
 * We do not recommend that projects use this class; because of its following
 * design flaws:<ul>
 *
 * <li>no automatic cache invalidation logic on datastore change, manual remove() method
 *
 * <li>no cache expiration functionality, could grow very big and never purge if you cache a lot
 *
 * <li>mixes cache usage with monitoring and administration (used by
 * CLI command in ITM, which should be part of the cache infra; proper cache API
 * should have separate interface for those two orthogonal concerns
 *
 * <li>Cache key is sometimes Object and sometimes String; instead of
 * simply InstanceIdentifier identifier plus LogicalDatastoreType
 * (thus also uses no T extend DataObject generics but Object)
 *
 * <li>returns null everywhere instead of Optional (contrary to
 * DataBroker Transaction read()
 *
 * <li>static methods instead of OSGi service and dependency inject-able in tests; thus impossible to use
 * properly in component tests (no reset between tests)
 * </ul>
 *
 * @deprecated This class has a number of serious design flaws. Use {@link DataObjectCache} instead.
 *
 * @author unascribed (Ericsson India?) - original code
 * @author Michael Vorburger.ch - JavaDoc
 */
@Deprecated
public final class DataStoreCache {

    private DataStoreCache() { }

    public static void create(String cacheName) {
        if (CacheUtil.getCache(cacheName) == null) {
            CacheUtil.createCache(cacheName);
        }
    }

    public static void add(String cacheName, Object key, Object value) {
        getCache(cacheName).put(key, value);
    }

    public static <T extends DataObject> Object get(String cacheName, InstanceIdentifier<T> identifier, String key,
            DataBroker broker, boolean isConfig) throws ReadFailedException {
        Object dataObject = getCache(cacheName).get(key);
        if (dataObject == null) {
            Optional<T> datastoreObject = SingleTransactionDataBroker.syncReadOptional(broker,
                    isConfig ? LogicalDatastoreType.CONFIGURATION : LogicalDatastoreType.OPERATIONAL, identifier);
            if (datastoreObject.isPresent()) {
                dataObject = datastoreObject.get();
                add(cacheName, key, dataObject);
            }
        }
        return dataObject;
    }

    public static Object get(String cacheName, Object key) {
        return getCache(cacheName).get(key);
    }

    public static void remove(String cacheName, Object key) {
        getCache(cacheName).remove(key);
    }

    @SuppressWarnings("unchecked")
    private static ConcurrentMap<Object, Object> getCache(String cacheName) {
        return (ConcurrentMap<Object, Object>) CacheUtil.getCache(cacheName);
    }

    public static boolean isCacheValid(String cacheName) {
        return CacheUtil.isCacheValid(cacheName);
    }

    public static List<Object> getValues(String cacheName) {
        ConcurrentHashMap<Object, Object> map = (ConcurrentHashMap<Object, Object>) DataStoreCache.getCache(cacheName);
        List<Object> values = null;
        if (map != null) {
            values = new ArrayList<>();
            for (Map.Entry<Object, Object> entry : map.entrySet()) {
                values.add(entry.getValue());
            }
        }
        return values;
    }

    public static List<Object> getKeys(String cacheName) {
        ConcurrentHashMap<Object, Object> map = (ConcurrentHashMap<Object, Object>) DataStoreCache.getCache(cacheName);
        List<Object> keys = null;
        if (map != null) {
            keys = Collections.list(map.keys());
        }
        return keys;
    }

}
