/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cache;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Singleton;
import org.opendaylight.genius.itm.utils.NodeConnectorInfo;

@Singleton
public class UnprocessedNodeConnectorCache {

    private final ConcurrentMap<String, NodeConnectorInfo> unprocessedNodeConnectorMap = new ConcurrentHashMap<>();

    public void add(String tunnelName, NodeConnectorInfo ncInfo) {
        unprocessedNodeConnectorMap.put(tunnelName, ncInfo);
    }

    public NodeConnectorInfo get(String tunnelName) {
        return unprocessedNodeConnectorMap.get(tunnelName);
    }

    public NodeConnectorInfo remove(String tunnelName) {
        return unprocessedNodeConnectorMap.remove(tunnelName);
    }

    public Set<Map.Entry<String, NodeConnectorInfo>> getAllEntries() {

        return unprocessedNodeConnectorMap.entrySet();
    }
}
