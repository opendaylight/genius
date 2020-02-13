/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.impl;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnelKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItmCache {

    private final ConcurrentHashMap<String, Interface> interfaces;
    private final ConcurrentHashMap<String, ExternalTunnel> externalTunnels;
    private final ConcurrentHashMap<String, InternalTunnel> internalTunnels;
    private final ConcurrentHashMap<ExternalTunnelKey, ExternalTunnel> externalTunnelKeyToExternalTunnels;

    private static final Logger LOG = LoggerFactory.getLogger(ItmCache.class);

    public ItmCache() {
        this.interfaces = new ConcurrentHashMap<>();
        this.internalTunnels = new ConcurrentHashMap<>();
        this.externalTunnels = new ConcurrentHashMap<>();
        this.externalTunnelKeyToExternalTunnels = new ConcurrentHashMap<>();
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
        List<String> tunnelInterfaceNames = tunnel.getTunnelInterfaceNames();
        if (tunnelInterfaceNames != null) {
            for (String tunnelInterfaceName : tunnelInterfaceNames) {
                LOG.debug("Adding Internal Tunnel - {} to Cache ", tunnelInterfaceName) ;
                this.internalTunnels.put(tunnelInterfaceName, tunnel);
            }
        }
    }

    public InternalTunnel getInternalTunnel(String name) {
        return this.internalTunnels.get(name);
    }

    public InternalTunnel removeInternalTunnel(String name) {
        LOG.debug(" Removing Internal Tunnel - {} from Cache ", name) ;
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

    public ConcurrentHashMap<ExternalTunnelKey, ExternalTunnel> getExternalTunnelKeyToExternalTunnels() {
        return externalTunnelKeyToExternalTunnels;
    }

    public void addExternalTunnelKeyToExternalTunnelCache(ExternalTunnel externalTunnel) {
        this.externalTunnelKeyToExternalTunnels.put(externalTunnel.key(), externalTunnel);
    }

    public void removeExternalTunnelfromExternalTunnelKeyCache(ExternalTunnelKey key) {
        this.externalTunnelKeyToExternalTunnels.remove(key);
    }

    // non-public package local method for use only in ItmTestUtils
    void clear() {
        interfaces.clear();
        externalTunnels.clear();
        internalTunnels.clear();
        externalTunnelKeyToExternalTunnels.clear();
    }
}
