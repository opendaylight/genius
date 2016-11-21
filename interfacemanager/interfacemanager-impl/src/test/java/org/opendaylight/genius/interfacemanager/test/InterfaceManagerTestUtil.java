/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.test;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.renderer.ovs.utilities.SouthboundUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlanBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.math.BigInteger;
import java.util.Arrays;

import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;

public class InterfaceManagerTestUtil {
    public static final String parentInterface = "tap23701c04-7e";
    public static final String interfaceName = "23701c04-7e58-4c65-9425-78a80d49a218";
    public static final String tunnelInterfaceName = "tun414a856a7a4";
    public static final TopologyId OVSDB_TOPOLOGY_ID = new TopologyId(new Uri("ovsdb:1"));
    public static final NodeKey nodeKey = new NodeKey(new NodeId("openflow:1"));

    public static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface buildStateInterface(
            String ifName, String dpnId, String portNo, String phyAddress, Class<? extends InterfaceType> ifType) {
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder ifaceBuilder =
                new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder();
        if(ifType != null) {
            ifaceBuilder.setType(ifType);
        }
        ifaceBuilder.setKey(IfmUtil.getStateInterfaceKeyFromName(ifName));
        ifaceBuilder.setOperStatus(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus.Up);
        ifaceBuilder.setLowerLayerIf(Arrays.asList("openflow:"+dpnId+":"+portNo));
        if(phyAddress != null) {
            ifaceBuilder.setPhysAddress(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress.getDefaultInstance(phyAddress));
        }
        return ifaceBuilder.build();
    }


    public static Node buildInventoryDpnNode(BigInteger dpnId) {
        NodeId nodeId = new NodeId("openflow:" + dpnId);
        Node nodeDpn = new NodeBuilder().setId(nodeId).setKey(new NodeKey(nodeId)).build();

        return nodeDpn;
    }

    public static Interface buildInterface(String ifName, String desc, boolean enabled, Object ifType,
                                           String parentInterface) {
        InterfaceBuilder builder = new InterfaceBuilder().setKey(new InterfaceKey(ifName)).setName(ifName)
                .setDescription(desc).setEnabled(enabled).setType((Class<? extends InterfaceType>) ifType);
        ParentRefs parentRefs = new ParentRefsBuilder().setParentInterface(parentInterface).build();
        builder.addAugmentation(ParentRefs.class, parentRefs);
        if(ifType.equals(L2vlan.class)){
            IfL2vlan l2vlan = new IfL2vlanBuilder().setVlanId(VlanId.getDefaultInstance("0"))
                    .setL2vlanMode(IfL2vlan.L2vlanMode.Trunk).build();
            builder.addAugmentation(IfL2vlan.class, l2vlan);
        }else if(ifType.equals(IfTunnel.class)){
            IfTunnel tunnel = new IfTunnelBuilder().setTunnelDestination(null).setTunnelGateway(null).setTunnelSource(null)
                    .setTunnelInterfaceType(null).build();
            builder.addAugmentation(IfTunnel.class, tunnel);
        }
        return builder.build();
    }

    public static Interface buildTunnelInterface(BigInteger dpn, String ifName, String desc, boolean enabled, Class<? extends TunnelTypeBase> tunType,
                                                 String remoteIpStr, String localIpStr) {
        InterfaceBuilder builder = new InterfaceBuilder().setKey(new InterfaceKey(ifName)).setName(ifName)
                .setDescription(desc).setEnabled(enabled).setType(Tunnel.class);
        ParentRefs parentRefs = new ParentRefsBuilder().setDatapathNodeIdentifier(dpn).build();
        builder.addAugmentation(ParentRefs.class, parentRefs);
        IpAddress remoteIp = new IpAddress(Ipv4Address.getDefaultInstance(remoteIpStr));
        IpAddress localIp =  new IpAddress(Ipv4Address.getDefaultInstance(localIpStr));
        IfTunnel tunnel = new IfTunnelBuilder().setTunnelDestination(remoteIp).setTunnelGateway(localIp).setTunnelSource(localIp)
                .setTunnelInterfaceType( tunType).setInternal(true).setMonitorEnabled(false).build();
        builder.addAugmentation(IfTunnel.class, tunnel);
        return builder.build();
    }

    public static InstanceIdentifier<TerminationPoint> getTerminationPointId(InstanceIdentifier<?> bridgeIid, String portName){
        InstanceIdentifier<TerminationPoint> tpIid = SouthboundUtils.createTerminationPointInstanceIdentifier(
                InstanceIdentifier.keyOf(bridgeIid.firstIdentifierOf(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node.class)), portName);
        return tpIid;
    }

    public static void deleteInterfaceConfig(DataBroker dataBroker, String ifaceName){
        InstanceIdentifier<Interface> vlanInterfaceEnabledInterfaceInstanceIdentifier = IfmUtil.buildId(
                ifaceName);
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.delete(CONFIGURATION, vlanInterfaceEnabledInterfaceInstanceIdentifier);
        tx.submit();
    }

    public static void putInterfaceConfig(DataBroker dataBroker, String ifaceName, ParentRefs parentRefs,
                                          Class<? extends InterfaceType> ifType){
        Interface interfaceInfo;
        if(!Tunnel.class.equals(ifType)) {
            interfaceInfo = InterfaceManagerTestUtil.buildInterface(
                    ifaceName, ifaceName, true, ifType, parentRefs.getParentInterface());
        }else{
            interfaceInfo = buildTunnelInterface(parentRefs.getDatapathNodeIdentifier(),ifaceName, ifaceName, true, TunnelTypeVxlan.class,
                    "1.1.1.1", "2.2.2.2");
        }
        InstanceIdentifier<Interface> interfaceInstanceIdentifier = IfmUtil.buildId(
                ifaceName);
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.put(CONFIGURATION, interfaceInstanceIdentifier, interfaceInfo, true);
        tx.submit();
    }

    public static void putInterfaceState(DataBroker dataBroker, String interfaceName, Class<? extends InterfaceType> ifType){
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface ifaceState =
                InterfaceManagerTestUtil.buildStateInterface(interfaceName, "1", "2", "AA:AA:AA:AA:AA:AA", ifType);
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.put(OPERATIONAL, IfmUtil.buildStateInterfaceId(interfaceName), ifaceState, true);
        tx.submit();
    }
}
