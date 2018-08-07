/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.listeners;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Singleton;

@Singleton
public class InternalTunnelIgnoreCache {

    private final ConcurrentMap<String, String> internalTunnelIgnoreCache = new ConcurrentHashMap<>();

    public void add(String tunnelName) {
        internalTunnelIgnoreCache.put(tunnelName, tunnelName);
    }

    public String remove(String tunnelName) {
        return internalTunnelIgnoreCache.remove(tunnelName);
    }

    public boolean isPresent(String tunnelName) {
        return internalTunnelIgnoreCache.containsKey(tunnelName);
    }
}
