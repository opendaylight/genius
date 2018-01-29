/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cache;

import java.math.BigInteger;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.infrautils.caches.CacheProvider;

/**
 * Cache for DpnId to Ip.
 *
 * @author Vishal Thapar
 */
@Singleton
public class DpnIdIpCache extends ItmCacheBase<String, String> {
    private final IInterfaceManager ifManager;

    @Inject
    public DpnIdIpCache(CacheProvider cacheProvider, final IInterfaceManager interfaceManager) {
        super(cacheProvider, "ItmDpnIdToIpCache", "ITM cache for dpnId to Ip");
        this.ifManager = interfaceManager;
    }

    protected String lookup(String dpnId) {
        //TODO: Check return and hook into appropriate DS read to fetch
        return ifManager.getEndpointIpForDpn(new BigInteger(dpnId));
    }
}
