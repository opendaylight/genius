/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.utils.hwvtep;

import org.opendaylight.genius.utils.cache.CacheUtil;

import java.util.Map;

public class HACacheUtils {
    public static final String HA_CACHE_NAME = "HA";

    static {
        if (CacheUtil.getCache(HA_CACHE_NAME) == null) {
            CacheUtil.createCache(HA_CACHE_NAME);
        }
    }

    public static void addDeviceToCache(String nodeId) {
        Map cache = CacheUtil.getCache(HA_CACHE_NAME);
        cache.put(nodeId, nodeId);
    }

    public static void removeDeviceFromCache(String nodeId) {
        Map cache = CacheUtil.getCache(HA_CACHE_NAME);
        cache.remove(nodeId);
    }

    public static boolean isHAEnabledDevice(String nodeId) {
        boolean enabled = CacheUtil.getCache(HA_CACHE_NAME).containsKey(nodeId);
        if (!enabled) {
            int idx = nodeId.indexOf("/physicalswitch");
            if (idx > 0) {
                nodeId = nodeId.substring(0, idx);
                return CacheUtil.getCache(HA_CACHE_NAME).containsKey(nodeId);
            }
        }
        return enabled;
    }

}
