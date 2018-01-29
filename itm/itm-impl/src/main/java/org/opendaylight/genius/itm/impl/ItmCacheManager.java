/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.impl;

import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.itm.cache.DpnIdIpCache;
import org.opendaylight.genius.itm.cache.IpDpnIdCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ItmCacheManager {

    private static final Logger LOG = LoggerFactory.getLogger(ItmCacheManager.class);

    private final DataBroker dataBroker;
    private final DpnIdIpCache dpnIdIpCache;
    private final IpDpnIdCache ipDpnIdCache;

    @Inject
    public ItmCacheManager(final DataBroker dataBroker, final DpnIdIpCache dpnIdIpCache,
                           final IpDpnIdCache ipDpnIdCache) {
        this.dataBroker = dataBroker;
        this.dpnIdIpCache = dpnIdIpCache;
        this.ipDpnIdCache = ipDpnIdCache;
    }

    public void addDpnIdAndIp(String dpnId, String ip) {
        dpnIdIpCache.put(dpnId, ip);
        ipDpnIdCache.put(ip, dpnId);
    }
}
