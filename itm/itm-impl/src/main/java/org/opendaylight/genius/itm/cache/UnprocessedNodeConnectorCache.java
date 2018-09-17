/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Singleton;
import org.opendaylight.genius.itm.utils.TunnelStateInfo;

@Singleton
public class UnprocessedNodeConnectorCache {

    private final ConcurrentMap<String, TunnelStateInfo> unprocessedNodeConnectorMap = new ConcurrentHashMap<>();

    public void add(String tunnelName, TunnelStateInfo tunnelStateInfo) {
        unprocessedNodeConnectorMap.put(tunnelName, tunnelStateInfo);
    }

    public TunnelStateInfo get(String tunnelName) {
        return unprocessedNodeConnectorMap.get(tunnelName);
    }

    public TunnelStateInfo remove(String tunnelName) {
        return unprocessedNodeConnectorMap.remove(tunnelName);
    }

    public Map<String, TunnelStateInfo> getAllPresent() {
        return unprocessedNodeConnectorMap;
    }
}
