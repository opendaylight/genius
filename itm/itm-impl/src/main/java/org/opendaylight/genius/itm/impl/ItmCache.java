/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.impl;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnel;
import java.util.Set;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class ItmCache {
    private ConcurrentHashMap<String, Interface> interfaces = null;
    private ConcurrentHashMap<String, ExternalTunnel> externalTunnels = null;
    private ConcurrentHashMap<String, InternalTunnel> internalTunnels = null;

    public ItmCache() {
        this.interfaces = new ConcurrentHashMap<>();
        this.internalTunnels = new ConcurrentHashMap<>();
        this.externalTunnels = new ConcurrentHashMap<>();
    }

    public void addInterface(Interface iface) {
        this.interfaces.put(iface.getName(), iface);
    }

    public Interface getInterface(String name) {
        return this.interfaces.get(name);
    }

    public Interface removeInterface(String name) {
        return this.interfaces.remove(name);
    }

    public Collection<Interface> getAllInterfaces() {
        return this.interfaces.values();
    }

    public void addExternalTunnel(ExternalTunnel tunnel) {
        this.externalTunnels.put(tunnel.getTunnelInterfaceName(), tunnel);
    }

    public ExternalTunnel getExternalTunnel(String name) {
        return this.externalTunnels.get(name);
    }

    public ExternalTunnel removeExternalTunnel(String name) {
        return this.externalTunnels.remove(name);
    }

    public void addInternalTunnel(InternalTunnel tunnel) {
        this.internalTunnels.put(tunnel.getTunnelInterfaceName(), tunnel);
    }

    public InternalTunnel getInternalTunnel(String name) {
        return this.internalTunnels.get(name);
    }

    public InternalTunnel removeInternalTunnel(String name) {
        return this.internalTunnels.remove(name);
    }
    public Collection<ExternalTunnel> getAllExternalTunnel() {
        return this.externalTunnels.values();
    }

    public Collection<InternalTunnel> getAllInternalTunnel() {
        return this.internalTunnels.values();
    }

    public Set<String> getAllInternalInterfaces() {
        return this.internalTunnels.keySet();
    }

    public Set<String> getAllExternalInterfaces() {
        return this.externalTunnels.keySet();
    }


}
