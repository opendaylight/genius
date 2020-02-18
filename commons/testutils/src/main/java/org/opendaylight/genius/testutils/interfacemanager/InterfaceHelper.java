/*
 * Copyright © 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.testutils.interfacemanager;

import java.util.Collections;
import java.util.concurrent.ExecutionException;

import org.opendaylight.genius.datastoreutils.ExpectedDataObjectNotFoundException;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.genius.interfacemanager.globals.InterfaceInfo;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev170119.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev170119.Tunnel;
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

public final class InterfaceHelper {

    private static final String DEFAULT_GW = "0.0.0.0";

    private InterfaceHelper() {
    }

    public static InstanceIdentifier<Interface> buildIId(String interfaceName) {
        return InstanceIdentifier
                .builder(Interfaces.class)
                .child(Interface.class, new InterfaceKey(interfaceName))
                .build();
    }

    public static Interface readInterfaceFromConfigDs(String interfaceName,
                                                      DataBroker dataBroker) throws
            ExecutionException, InterruptedException, ExpectedDataObjectNotFoundException {
        return SingleTransactionDataBroker.syncRead(
                dataBroker,
                LogicalDatastoreType.CONFIGURATION,
                buildIId(interfaceName));
    }

    public static Interface buildVlanInterfaceFromInfo(InterfaceInfo interfaceInfo) {
        ParentRefs parentRefs = new ParentRefsBuilder()
                .setDatapathNodeIdentifier(interfaceInfo.getDpId())
                .setParentInterface(interfaceInfo.getInterfaceName())
                .build();

        return new InterfaceBuilder()
                .withKey(new InterfaceKey(interfaceInfo.getInterfaceName()))
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
                .setParentInterface(interfaceInfo.getInterfaceName())
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
                .withKey(new InterfaceKey(interfaceInfo.getInterfaceName()))
                .setName(interfaceInfo.getInterfaceName())
                .setDescription("Tunnel interface")
                .setEnabled(true)
                .setType(Tunnel.class)
                .addAugmentation(ParentRefs.class, parentRefs)
                .addAugmentation(IfTunnel.class, tunnel)
                .build();
    }
}
