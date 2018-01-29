/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.impl;

import java.math.BigInteger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.infrautils.caches.Cache;
import org.opendaylight.infrautils.caches.CacheConfigBuilder;
import org.opendaylight.infrautils.caches.CacheProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ItmCacheManager {

    private static final Logger LOG = LoggerFactory.getLogger(ItmCacheManager.class);

    private final DataBroker dataBroker;
    private final CacheProvider cacheProvider;
    private final IInterfaceManager ifManager;
    private static final String DPNID_TO_IP_CACHE = "ItmDpnIdToIpCache";
    private static final String IP_TO_DPNID_CACHE = "ItmIpToDpnIdCache";
    private Cache<BigInteger, String> dpnIdToIpCache;
    private Cache<String, BigInteger> ipToDpnIdCache;

    @Inject
    public ItmCacheManager(final DataBroker dataBroker, final CacheProvider cacheProvider,
                           final IInterfaceManager interfaceManager) {
        this.dataBroker = dataBroker;
        this.cacheProvider = cacheProvider;
        this.ifManager = interfaceManager;
    }

    @PostConstruct
    public void start() {
        //createCaches
        dpnIdToIpCache = cacheProvider.newCache(
            new CacheConfigBuilder<BigInteger, String>()
                .anchor(this)
                .id(DPNID_TO_IP_CACHE)
                .cacheFunction(key -> dpnIdToIpCacheLookup(key))
                .description("ITM cache for dpnId to Ip")
                .build());
        ipToDpnIdCache = cacheProvider.newCache(
            new CacheConfigBuilder<String, BigInteger>()
                .anchor(this)
                .id(DPNID_TO_IP_CACHE)
                .cacheFunction(key -> ipToDpnIdCacheLookup(key))
                .description("ITM cache for Ip to")
                .build());
    }

    private String dpnIdToIpCacheLookup(BigInteger dpnId) {
        //TOD: Check return and hook into appropriate DS read to fetch
        return ifManager.getEndpointIpForDpn(dpnId);
    }

    private BigInteger ipToDpnIdCacheLookup(String ipStr) {
        // TODO: Hook this into appropriate API to fetch value
        return BigInteger.ZERO;
    }

    @PreDestroy
    public void close() throws Exception {
        if (dpnIdToIpCache != null) {
            dpnIdToIpCache.close();
        }
        LOG.info("ItmProvider Closed");
    }
}
