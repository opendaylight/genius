/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yangtools.yang.common.Uint64;

public interface IITMProvider {
    // APIs used by i
    void createLocalCache(Uint64 dpnId, String portName, Integer vlanId, String ipAddress, String subnetMask,
            String gatewayIp, String transportZone);

    void commitTeps();

    DataBroker getDataBroker();

    List<String> showTeps();

    void showState(Collection<StateTunnelList> tunnels);

    void showBridges(Map dpnIdBridgeRefMap);

    void showCache(String cacheName);

    void deleteVtep(Uint64 dpnId, String portName, Integer vlanId, String ipAddress, String subnetMask,
            String gatewayIp, String transportZone);

    void configureTunnelType(String transportZone, String tunnelType);

    void configureTunnelMonitorParams(boolean monitorEnabled, String monitorProtocol);

    void configureTunnelMonitorInterval(int interval);

    void addExternalEndpoint(java.lang.Class<? extends TunnelTypeBase> tunType, IpAddress dcgwIP);

    void remExternalEndpoint(java.lang.Class<? extends TunnelTypeBase> tunType, IpAddress dcgwIP);

    boolean validateIP(String ip);

    Interface getInterface(String tunnelName);

    Optional<StateTunnelList> getTunnelState(String interfaceName) throws ReadFailedException;
}
