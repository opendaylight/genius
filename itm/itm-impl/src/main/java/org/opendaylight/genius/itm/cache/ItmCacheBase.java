/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cache;

import org.opendaylight.infrautils.caches.Cache;
import org.opendaylight.infrautils.caches.CacheConfigBuilder;
import org.opendaylight.infrautils.caches.CacheProvider;

/**
 * Base class for ITM non DataObject caches.
 *
 * @author Vishal Thapar
 */
public abstract class ItmCacheBase<K, V> implements AutoCloseable {

    protected final Cache<K, V> cache;

    public ItmCacheBase(CacheProvider cacheProvider, String cacheName, String description) {
        cache = cacheProvider.newCache(
            new CacheConfigBuilder<K, V>()
                .anchor(this)
                .id(cacheName)
                .cacheFunction(key -> lookup(key))
                .description(description)
                .build());
    }

    @Override
    public void close() throws Exception {
        cache.close();
    }

    public void put(K key, V value) {
        cache.put(key, value);
    }

    public V get(K key) {
        return cache.get(key);
    }

    public void remove(K key) {
        cache.evict(key);
    }

    protected abstract V lookup(K key);
}
