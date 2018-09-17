/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cache;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Singleton;
import org.opendaylight.genius.itm.utils.TunnelStateInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class UnprocessedNodeConnectorEndPointCache {

    private static final Logger LOG = LoggerFactory.getLogger(UnprocessedNodeConnectorEndPointCache.class);

    private final ConcurrentMap<String, Set<TunnelStateInfo>> unProcessedNodeConnectorEndPtMap =
            new ConcurrentHashMap<>();

    public void add(String dpnId, Collection<TunnelStateInfo> ncList) {
        unProcessedNodeConnectorEndPtMap.computeIfAbsent(dpnId, key -> ConcurrentHashMap.newKeySet()).addAll(ncList);
    }

    public void add(String dpnId, TunnelStateInfo tunnelStateInfo) {
        unProcessedNodeConnectorEndPtMap.computeIfAbsent(dpnId, key -> ConcurrentHashMap.newKeySet())
                .add(tunnelStateInfo);
    }

    public Collection<TunnelStateInfo> remove(String dpnId) {
        return unProcessedNodeConnectorEndPtMap.remove(dpnId);
    }

    public void remove(String dpnId, TunnelStateInfo ncInfo) {
        Collection<TunnelStateInfo> tunnelStateInfoList = get(dpnId);
        if (tunnelStateInfoList != null) {
            tunnelStateInfoList.remove(ncInfo);
        } else {
            LOG.debug("TunnelStateInfo List for DPN Id {} is null", dpnId);
        }
    }

    public Collection<TunnelStateInfo> get(String dpnId) {
        return unProcessedNodeConnectorEndPtMap.get(dpnId);
    }

    public ConcurrentMap<String, Set<TunnelStateInfo>> getAllPresent() {
        return unProcessedNodeConnectorEndPtMap;
    }
}
