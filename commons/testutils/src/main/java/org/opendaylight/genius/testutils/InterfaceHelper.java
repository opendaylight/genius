/*
 * Copyright © 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.testutils;

import java.util.Collections;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.interfacemanager.globals.InterfaceInfo;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfaceHelper {

    public static final String DEFAULT_GW = "0.0.0.0";

    public static InstanceIdentifier<Interface> buildIid(String interfaceName) {
        return InstanceIdentifier
                .builder(Interfaces.class)
                .child(Interface.class, new InterfaceKey(interfaceName))
                .build();
    }

    public static Interface readInterfaceFromConfigDs(String interfaceName,
                                                      DataBroker dataBroker) {
        return MDSALUtil.read(
                LogicalDatastoreType.CONFIGURATION,
                buildIid(interfaceName),
                dataBroker).orNull();
    }

    public static Interface buildVlanInterfaceFromInfo(InterfaceInfo interfaceInfo) {
        ParentRefs parentRefs = new ParentRefsBuilder()
                .setDatapathNodeIdentifier(interfaceInfo.getDpId())
                .setParentInterface(interfaceInfo.getInterfaceName())//TODO remove this if not needed
                .build();

        return new InterfaceBuilder()
                .setKey(new InterfaceKey(interfaceInfo.getInterfaceName()))
                .setName(interfaceInfo.getInterfaceName())
                .setDescription("Vlan interface")
                .setEnabled(true)
                .setType(L2vlan.class)
                .addAugmentation(ParentRefs.class, parentRefs)
                .build();
    }

    public static Interface buildVxlanTunnelInterfaceFromInfo(TunnelInterfaceDetails tunnelInterfaceDetails) {
        InterfaceInfo interfaceInfo = tunnelInterfaceDetails.getInterfaceInfo();
        ParentRefs parentRefs = new ParentRefsBuilder()
                .setDatapathNodeIdentifier(interfaceInfo.getDpId())
                .setParentInterface(interfaceInfo.getInterfaceName())//TODO remove this if not needed
                .build();

        IfTunnel tunnel = new IfTunnelBuilder()
                .setTunnelDestination(new IpAddress(new Ipv4Address(tunnelInterfaceDetails.getDstIp())))
                .setTunnelGateway(new IpAddress(new Ipv4Address(DEFAULT_GW)))
                .setTunnelSource(new IpAddress(new Ipv4Address(tunnelInterfaceDetails.getSrcIp())))
                .setTunnelInterfaceType(TunnelTypeVxlan.class)
                .setInternal(!tunnelInterfaceDetails.isExternal())
                .setTunnelRemoteIpFlow(false)
                .setTunnelOptions(Collections.emptyList())
                .build();

        return new InterfaceBuilder()
                .setKey(new InterfaceKey(interfaceInfo.getInterfaceName()))
                .setName(interfaceInfo.getInterfaceName())
                .setDescription("Tunnel interface")
                .setEnabled(true)
                .setType(Tunnel.class)
                .addAugmentation(ParentRefs.class, parentRefs)
                .addAugmentation(IfTunnel.class, tunnel)
                .build();
    }
}
