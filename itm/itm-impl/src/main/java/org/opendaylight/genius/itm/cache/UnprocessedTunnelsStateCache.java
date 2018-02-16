/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cache;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Singleton;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelOperStatus;

@Singleton
public class UnprocessedTunnelsStateCache {

    private final ConcurrentMap<String, TunnelOperStatus> unprocessedTunnelsStateMap = new ConcurrentHashMap<>();

    public void add(String tunnelName, TunnelOperStatus operState) {
        unprocessedTunnelsStateMap.put(tunnelName, operState);
    }

    public TunnelOperStatus get(String tunnelName) {
        return unprocessedTunnelsStateMap.get(tunnelName);
    }

    public TunnelOperStatus remove(String tunnelName) {
        return unprocessedTunnelsStateMap.remove(tunnelName);
    }

    public Set<String> getAllUnprocessedTunnels() {
        return this.unprocessedTunnelsStateMap.keySet();
    }
}
