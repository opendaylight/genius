/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.utils.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Cache wannabe.
 *
 * <p>This class wanted to be Cache when it was grown up. Currently it's actually just
 * a nested ConcurrentMap, and thus has fairly limited real value.
 *
 * <p>The usage of static methods here, instead of an (OSGi) "service"
 * (dependency inject-able in tests!) makes it impossible to easily properly
 * use code relying on this in component tests (as there would be no automatic
 * reset between tests; you would have to manually {@link #destroyCache(String)}
 * {@literal @}Before each test).
 *
 * <p>This class' "static" Singleton doesn't play nice with OSGi e.g. for hot reload.
 *
 * <p>This class' (necessary) use &lt;?&gt; generics causes {@literal @}SuppressWarnings("unchecked") when used.
 *
 * <p>Perhaps you would like to use <a href="https://github.com/google/guava/wiki/CachesExplained">
 * Google Guava's simple Caches</a>, if not a full blown JSR 107 javax.cache (JCache) implementation,
 * such as <a href="http://infinispan.org">Infinispan</a> or <a href="http://www.ehcache.org">Ehcache</a>,
 * instead of this class?
 *
 * @deprecated We now recommend you simply use your own {@code new ConcurrentHashMap<>()} instead.
 *
 * @author unascribed (Ericsson India?) - original code
 * @author Michael Vorburger.ch - JavaDoc
 */
@Deprecated
public final class CacheUtil {

    // package local instead of private for CacheTestUtil
    static final ConcurrentMap<String, ConcurrentMap<?, ?>> MAP_OF_MAP = new ConcurrentHashMap<>();

    private CacheUtil() { }

    public static ConcurrentMap<?, ?> getCache(String cacheName) {
        return MAP_OF_MAP.get(cacheName);
    }

    public static void createCache(String cacheName) {
        MAP_OF_MAP.computeIfAbsent(cacheName, k -> new ConcurrentHashMap<>());
    }

    public static boolean isCacheValid(String cacheName) {
        return MAP_OF_MAP.containsKey(cacheName);
    }

    public static void destroyCache(String cacheName) {
        MAP_OF_MAP.remove(cacheName);
    }
}
