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
 * Cache for DpnId to NodeId.
 *
 * @author Vishal Thapar
 */
@Singleton
public class DpnIdNodeIdCache extends ItmCacheBase<String, String> {
    private final IInterfaceManager ifManager;

    @Inject
    public DpnIdNodeIdCache(CacheProvider cacheProvider, final IInterfaceManager interfaceManager) {
        super(cacheProvider, "ItmDpnIdToNodeIdCache", "ITM cache for dpnId to nodeId");
        this.ifManager = interfaceManager;
    }

    protected String lookup(String dpnId) {
        return ifManager.getOvsdbBridgeNodeId(new BigInteger(dpnId));
    }
}
