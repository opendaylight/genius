/*
 * Copyright (c) 2020 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.listeners;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Singleton;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;


@Singleton
public class InternalTunnelCache {

    private volatile ConcurrentMap<String, Interface> internalTunnelCache = new ConcurrentHashMap<>();

    public synchronized void add(String tunnelName, Interface iface) {
        internalTunnelCache.putIfAbsent(tunnelName, iface);
    }

    public synchronized Interface get(String tunnelName) {
        return internalTunnelCache.get(tunnelName);
    }

    public synchronized Interface remove(String tunnelName) {
        return internalTunnelCache.remove(tunnelName);
    }
}
