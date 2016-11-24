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
 * <p>This class wanted to be Cache when it grows up. Currently it's actually just
 * a nested ConcurrentMap, and thus has fairly limited real value.
 *
 * <p>The usage of static methods here, instead of an (OSGi) "service"
 * (dependency inject-able in tests!) makes it impossible to easily properly
 * use code relying on this in component tests (as there would be no automatic
 * reset between tests; you would have to manually {@link #destroyCache(String)}
 * {@literal @}Before each test).
 *
 * @deprecated We recommend you simply use your own ConcurrentHashMap instead.
 *
 * @author unascribed (Ericsson India?) - original code
 * @author Michael Vorburger.ch - JavaDoc
 */
@Deprecated
public class CacheUtil {

    private static final ConcurrentMap<String, ConcurrentMap<?, ?>> MAP_OF_MAP = new ConcurrentHashMap<>();

    public static ConcurrentMap<?, ?> getCache(String cacheName) {
        return MAP_OF_MAP.get(cacheName);
    }

    public static void createCache(String cacheName) {
        if (MAP_OF_MAP.get(cacheName) == null) {
            MAP_OF_MAP.put(cacheName, new ConcurrentHashMap<>());
        }
    }

    public static boolean isCacheValid(String cacheName) {
        return MAP_OF_MAP.containsKey(cacheName);
    }

    public static void destroyCache(String cacheName) {
        MAP_OF_MAP.remove(cacheName);
    }
}
