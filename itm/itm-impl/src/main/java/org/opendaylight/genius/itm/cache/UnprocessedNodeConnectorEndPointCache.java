/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cache;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Singleton;
import org.opendaylight.genius.itm.utils.NodeConnectorInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class UnprocessedNodeConnectorEndPointCache {

    private static final Logger LOG = LoggerFactory.getLogger(UnprocessedNodeConnectorEndPointCache.class);

    private final ConcurrentMap<String, Set<NodeConnectorInfo>> unProcessedNodeConnectorEndPtMap =
            new ConcurrentHashMap<>();

    public void add(String dpnId, Collection<NodeConnectorInfo> ncList) {
        unProcessedNodeConnectorEndPtMap.computeIfAbsent(dpnId, key -> ConcurrentHashMap.newKeySet()).addAll(ncList);
    }

    public void add(String dpnId, NodeConnectorInfo ncInfo) {
        unProcessedNodeConnectorEndPtMap.computeIfAbsent(dpnId, key -> ConcurrentHashMap.newKeySet()).add(ncInfo);
    }

    public void remove(String dpnId) {
        unProcessedNodeConnectorEndPtMap.remove(dpnId);
    }

    public void remove(String dpnId, NodeConnectorInfo ncInfo) {
        Collection<NodeConnectorInfo> ncList = get(dpnId);
        if (ncList != null) {
            ncList.remove(ncInfo);
        } else {
            LOG.debug("NodeConnectorInfo List for DPN Id {} is null", dpnId);
        }
    }

    public Collection<NodeConnectorInfo> get(String dpnId) {
        return unProcessedNodeConnectorEndPtMap.get(dpnId);
    }

    public Set<Map.Entry<String, Set<NodeConnectorInfo>>> getAllEntries() {

        return unProcessedNodeConnectorEndPtMap.entrySet();
    }
}
