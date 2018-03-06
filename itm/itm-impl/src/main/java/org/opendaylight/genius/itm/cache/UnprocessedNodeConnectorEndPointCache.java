/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.genius.itm.utils.NodeConnectorInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class UnprocessedNodeConnectorEndPointCache {

    private static final Logger LOG = LoggerFactory.getLogger(UnprocessedNodeConnectorEndPointCache.class);

    private final ConcurrentMap<String, List<NodeConnectorInfo>> unProcessedNodeConnectorEndPtMap =
            new ConcurrentHashMap();

    @Inject
    public UnprocessedNodeConnectorEndPointCache() {

    }

    public void addNodeConnectorEndPtInfoToCache(String dpnId, List<NodeConnectorInfo> ncList) {
        unProcessedNodeConnectorEndPtMap.put(dpnId, ncList);
    }

    public void addNodeConnectorEndPtInfoToCache(String dpnId, NodeConnectorInfo ncInfo) {
        List<NodeConnectorInfo> ncList = getUnprocessedNodeConnectorEndPt(dpnId);
        if (ncList == null) {
            ncList = new ArrayList<NodeConnectorInfo>();
        }
        ncList.add(ncInfo);
        unProcessedNodeConnectorEndPtMap.put(dpnId, ncList);
    }

    public List<NodeConnectorInfo> getUnprocessedNodeConnectorEndPt(String dpnId) {
        return unProcessedNodeConnectorEndPtMap.get(dpnId);
    }

    public void removeNodeConnectorEndPtInfoFromCache(String dpnId) {
        unProcessedNodeConnectorEndPtMap.remove(dpnId);
    }

    public void removeNodeConnectorEndPtInfoFromCache(String dpnId, NodeConnectorInfo ncInfo) {
        List<NodeConnectorInfo> ncList = getUnprocessedNodeConnectorEndPt(dpnId);
        if (ncList != null) {
            ncList.remove(ncInfo);
            unProcessedNodeConnectorEndPtMap.put(dpnId, ncList);
        } else {
            LOG.error("NodeConnectorInfo List for DPN Id {} is null", dpnId);
        }
    }
}
