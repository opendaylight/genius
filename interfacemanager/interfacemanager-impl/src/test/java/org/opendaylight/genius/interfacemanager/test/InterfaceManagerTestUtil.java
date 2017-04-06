/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.test;

import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;
import static org.opendaylight.genius.interfacemanager.renderer.hwvtep.utilities.SouthboundUtils.HWVTEP_TOPOLOGY_ID;

import java.math.BigInteger;
import java.util.Arrays;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.datastoreutils.testutils.AsyncEventsWaiter;
import org.opendaylight.genius.datastoreutils.testutils.JobCoordinatorEventsWaiter;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.renderer.ovs.utilities.SouthboundUtils;
import org.opendaylight.genius.utils.hwvtep.HwvtepSouthboundUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.flow.capable.port.State;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.flow.capable.port.StateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlanBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBfd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.interfaces._interface.NodeIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.interfaces._interface.NodeIdentifierBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.interfaces._interface.NodeIdentifierKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InterfaceManagerTestUtil {
    private static final Logger LOG = LoggerFactory.getLogger(InterfaceManagerConfigurationTest.class);
    public static final String PARENT_INTERFACE = "tap23701c04-7e";
    public static final String INTERFACE_NAME = "23701c04-7e58-4c65-9425-78a80d49a218";
    public static final String TUNNEL_INTERFACE_NAME = "tun414a856a7a4";
    public static final String HWVTEP_INTERFACE_NAME = "tun414a856a7a5";
    public static final String TRUNK_INTERFACE_NAME = "23701c04-7e58-4c65-9425-78a80d49a219";

    public static final String DPN_ID_1 = "1";
    public static final String PORT_NO_1 = "2";
    public static final TopologyId OVSDB_TOPOLOGY_ID = new TopologyId(new Uri("ovsdb:1"));
    public static final NodeKey NODE_KEY = new NodeKey(new NodeId("openflow:1"));

    static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface
        buildStateInterface(String ifName, String dpnId, String portNo, String phyAddress,
                            Class<? extends InterfaceType> ifType) {
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder ifaceBuilder =
                new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                        .ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder();
        if (ifType != null) {
            ifaceBuilder.setType(ifType);
        }
        ifaceBuilder.setKey(IfmUtil.getStateInterfaceKeyFromName(ifName));
        ifaceBuilder.setOperStatus(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                        .ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus.Up);
        ifaceBuilder.setLowerLayerIf(Arrays.asList("openflow:" + dpnId + ":" + portNo));
        if (phyAddress != null) {
            ifaceBuilder.setPhysAddress(
                    org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress
                            .getDefaultInstance(phyAddress));
        }
        ifaceBuilder.setIfIndex(1);
        return ifaceBuilder.build();
    }

    static Node buildInventoryDpnNode(BigInteger dpnId) {
        NodeId nodeId = new NodeId("openflow:" + dpnId);
        Node nodeDpn = new NodeBuilder().setId(nodeId).setKey(new NodeKey(nodeId)).build();

        return nodeDpn;
    }


    static org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector
        buildFlowCapableNodeConnector(NodeConnectorId ncId, String portName) {
        NodeConnectorBuilder ncBuilder = new NodeConnectorBuilder()
                .setId(ncId)
                .setKey(new NodeConnectorKey(ncId));
        ncBuilder.addAugmentation(FlowCapableNodeConnector.class,
                buildFlowCapableNodeConnector(false, true,"AA:AA:AA:AA:AA:AA", portName));
        return ncBuilder.build();
    }

    static FlowCapableNodeConnector buildFlowCapableNodeConnector(boolean isPortDown, boolean isLive,
                                                                  String macAddress, String portName) {
        PortConfig portConfig = new PortConfig(false, false, false, isPortDown);
        State state = new StateBuilder().setBlocked(true).setLinkDown(false).setLive(isLive).build();
        FlowCapableNodeConnectorBuilder fcNodeConnector = new FlowCapableNodeConnectorBuilder().setName(portName)
                .setHardwareAddress(MacAddress.getDefaultInstance(macAddress)).setConfiguration(portConfig)
                .setState(state);
        return fcNodeConnector.build();
    }

    static NodeConnectorId buildNodeConnectorId(BigInteger dpn, long portNo) {
        return new NodeConnectorId(buildNodeConnectorString(dpn, portNo));
    }

    static String buildNodeConnectorString(BigInteger dpn, long portNo) {
        return IfmConstants.OF_URI_PREFIX + dpn + IfmConstants.OF_URI_SEPARATOR + portNo;
    }

    static InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector>
        buildNodeConnectorInstanceIdentifier(BigInteger dpn, long portNo) {
        NodeConnectorId nodeConnectorId = buildNodeConnectorId(dpn, portNo);
        NodeId nodeId = IfmUtil.getNodeIdFromNodeConnectorId(nodeConnectorId);
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector>
                ncIdentifier = InstanceIdentifier.builder(Nodes.class).child(Node.class, new NodeKey(nodeId))
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector.class,
                        new NodeConnectorKey(nodeConnectorId)).build();
        return ncIdentifier;
    }

    static Interface buildInterface(String ifName, String desc, boolean enabled, Object ifType,
                                           String parentInterface, IfL2vlan.L2vlanMode l2vlanMode) {
        InterfaceBuilder builder = new InterfaceBuilder().setKey(new InterfaceKey(ifName)).setName(ifName)
                .setDescription(desc).setEnabled(enabled).setType((Class<? extends InterfaceType>) ifType);
        ParentRefs parentRefs = new ParentRefsBuilder().setParentInterface(parentInterface).build();
        builder.addAugmentation(ParentRefs.class, parentRefs);
        if (ifType.equals(L2vlan.class)) {
            IfL2vlanBuilder ifL2vlanBuilder = new IfL2vlanBuilder().setL2vlanMode(l2vlanMode);
            if (IfL2vlan.L2vlanMode.TrunkMember.equals(l2vlanMode)) {
                ifL2vlanBuilder.setVlanId(new VlanId(100));
            } else {
                ifL2vlanBuilder.setVlanId(VlanId.getDefaultInstance("0"));
            }
            builder.addAugmentation(IfL2vlan.class, ifL2vlanBuilder.build());
        } else if (ifType.equals(IfTunnel.class)) {
            IfTunnel tunnel = new IfTunnelBuilder().setTunnelDestination(null).setTunnelGateway(null)
                    .setTunnelSource(null).setTunnelInterfaceType(null).build();
            builder.addAugmentation(IfTunnel.class, tunnel);
        }
        return builder.build();
    }

    static Interface buildTunnelInterface(BigInteger dpn, String ifName, String desc, boolean enabled,
                                          Class<? extends TunnelTypeBase> tunType, String remoteIpStr,
                                          String localIpStr) {
        InterfaceBuilder builder = new InterfaceBuilder().setKey(new InterfaceKey(ifName)).setName(ifName)
                .setDescription(desc).setEnabled(enabled).setType(Tunnel.class);
        ParentRefs parentRefs = new ParentRefsBuilder().setDatapathNodeIdentifier(dpn).build();
        builder.addAugmentation(ParentRefs.class, parentRefs);
        IpAddress remoteIp = new IpAddress(Ipv4Address.getDefaultInstance(remoteIpStr));
        IpAddress localIp = new IpAddress(Ipv4Address.getDefaultInstance(localIpStr));
        IfTunnel tunnel = new IfTunnelBuilder().setTunnelDestination(remoteIp).setTunnelGateway(localIp)
                .setTunnelSource(localIp).setTunnelInterfaceType(tunType).setInternal(true).setMonitorEnabled(false)
                .build();
        builder.addAugmentation(IfTunnel.class, tunnel);
        return builder.build();
    }

    public static Interface buildHWvTEPInterface(NodeIdentifier nodeId, String ifName, String desc, boolean enabled,
                                                 Class<? extends TunnelTypeBase> tunType) {
        InterfaceBuilder builder = new InterfaceBuilder().setKey(new InterfaceKey(ifName)).setName(ifName)
            .setDescription(desc).setEnabled(enabled).setType(Tunnel.class);
        ParentRefs parentRefs = new ParentRefsBuilder().setNodeIdentifier(Arrays.asList(nodeId)).build();
        builder.addAugmentation(ParentRefs.class, parentRefs);
        IpAddress remoteIp = new IpAddress(Ipv4Address.getDefaultInstance("1.1.1.1"));
        IpAddress localIp =  new IpAddress(Ipv4Address.getDefaultInstance("2.2.2.2"));
        IfTunnel tunnel = new IfTunnelBuilder().setTunnelDestination(remoteIp).setTunnelGateway(localIp)
                .setTunnelSource(localIp).setTunnelInterfaceType(tunType).setInternal(true)
                .setMonitorEnabled(false).build();
        builder.addAugmentation(IfTunnel.class, tunnel);
        return builder.build();
    }

    public static InstanceIdentifier<TerminationPoint> getTerminationPointId(InstanceIdentifier<?> bridgeIid,
                                                                             String portName) {
        InstanceIdentifier<TerminationPoint> tpIid = SouthboundUtils.createTerminationPointInstanceIdentifier(
                InstanceIdentifier.keyOf(bridgeIid.firstIdentifierOf(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns
                        .yang.network.topology.rev131021.network.topology.topology.Node.class)), portName);
        return tpIid;
    }

    static void deleteInterfaceConfig(DataBroker dataBroker, String ifaceName) throws TransactionCommitFailedException {
        InstanceIdentifier<Interface> vlanInterfaceEnabledInterfaceInstanceIdentifier = IfmUtil.buildId(
                ifaceName);
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.delete(CONFIGURATION, vlanInterfaceEnabledInterfaceInstanceIdentifier);
        tx.submit().checkedGet();
    }

    static void updateInterfaceAdminState(DataBroker dataBroker, String ifaceName, boolean isEnabled)
            throws TransactionCommitFailedException {
        InstanceIdentifier<Interface> vlanInterfaceEnabledInterfaceInstanceIdentifier = IfmUtil.buildId(ifaceName);
        InterfaceBuilder builder = new InterfaceBuilder().setKey(new InterfaceKey(ifaceName)).setName(ifaceName)
            .setEnabled(isEnabled);
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.merge(CONFIGURATION, vlanInterfaceEnabledInterfaceInstanceIdentifier, builder.build());
        tx.submit().checkedGet();
    }

    static void updateTunnelMonitoringAttributes(DataBroker dataBroker, String ifaceName)
            throws TransactionCommitFailedException {
        InstanceIdentifier<Interface> tunnelInstanceIdentifier = IfmUtil.buildId(ifaceName);
        InterfaceBuilder builder = new InterfaceBuilder().setKey(new InterfaceKey(ifaceName)).setName(ifaceName);
        IfTunnel tunnel = new IfTunnelBuilder().setMonitorProtocol(TunnelMonitoringTypeBfd.class)
            .setMonitorEnabled(true).build();
        builder.addAugmentation(IfTunnel.class, tunnel);
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.merge(CONFIGURATION, tunnelInstanceIdentifier, builder.build());
        tx.submit().checkedGet();
    }


    static void putInterfaceConfig(DataBroker dataBroker, String ifaceName, ParentRefs parentRefs,
                                          Class<? extends InterfaceType> ifType)
            throws TransactionCommitFailedException {
        Interface interfaceInfo;
        if (!Tunnel.class.equals(ifType)) {
            interfaceInfo = InterfaceManagerTestUtil.buildInterface(ifaceName, ifaceName, true, ifType,
                    parentRefs.getParentInterface(), IfL2vlan.L2vlanMode.Trunk);
        } else {
            interfaceInfo = buildTunnelInterface(parentRefs.getDatapathNodeIdentifier(),ifaceName, ifaceName,
                    true, TunnelTypeVxlan.class, "1.1.1.1", "2.2.2.2");
        }
        InstanceIdentifier<Interface> interfaceInstanceIdentifier = IfmUtil.buildId(ifaceName);
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.put(CONFIGURATION, interfaceInstanceIdentifier, interfaceInfo, true);
        tx.submit().checkedGet();
    }

    static void putVlanInterfaceConfig(DataBroker dataBroker, String ifaceName, String parentRefs,
                                              IfL2vlan.L2vlanMode l2vlanMode) throws TransactionCommitFailedException {
        Interface interfaceInfo = InterfaceManagerTestUtil.buildInterface(ifaceName, ifaceName,
                true, L2vlan.class, parentRefs, l2vlanMode);
        InstanceIdentifier<Interface> interfaceInstanceIdentifier = IfmUtil.buildId(ifaceName);
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.put(CONFIGURATION, interfaceInstanceIdentifier, interfaceInfo, true);
        tx.submit().checkedGet();
    }

    static void createFlowCapableNodeConnector(DataBroker dataBroker, String interfaceName,
                                                      Class<? extends InterfaceType> ifType)
            throws TransactionCommitFailedException {
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();

        NodeConnector nodeConnector = InterfaceManagerTestUtil
                .buildFlowCapableNodeConnector(buildNodeConnectorId(BigInteger.valueOf(1), 2), interfaceName);
        tx.put(OPERATIONAL,buildNodeConnectorInstanceIdentifier(BigInteger.valueOf(1), 2), nodeConnector, true);
        tx.submit().checkedGet();
    }

    static void putHwvtepInterfaceConfig(DataBroker dataBroker, String ifaceName, InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network
            .topology.topology.Node> physicalSwitchId)
            throws TransactionCommitFailedException {
        NodeIdentifier nodeIdentifier = new NodeIdentifierBuilder().setTopologyId(
                org.opendaylight.genius.interfacemanager.renderer.hwvtep.utilities.SouthboundUtils.HWVTEP_TOPOLOGY)
                .setNodeId("hwvtep:1/192.168.56.1/6640").setKey(new NodeIdentifierKey(org.opendaylight.genius.interfacemanager.renderer.hwvtep.utilities.SouthboundUtils.HWVTEP_TOPOLOGY))
                .build();
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface
                hwVtepInterface = InterfaceManagerTestUtil.buildHWvTEPInterface(nodeIdentifier, HWVTEP_INTERFACE_NAME,
                "test", true, TunnelTypeVxlan.class);
        InstanceIdentifier<Interface> interfaceInstanceIdentifier = IfmUtil.buildId(ifaceName);
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.put(CONFIGURATION, interfaceInstanceIdentifier, hwVtepInterface, true);
        tx.submit().checkedGet();
    }

    static void putHwvtepPhysicalSwitchAugmentation(DataBroker dataBroker, String ifaceName, String physicalSwitchId)
            throws TransactionCommitFailedException {
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> psNodeId =
                InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, new TopologyKey(HWVTEP_TOPOLOGY_ID))
                .child(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node.class,
                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId(physicalSwitchId.toString())));
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> nodeId = HwvtepSouthboundUtils
                .createInstanceIdentifier(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId(physicalSwitchId));
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder nodeBuilder =
                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder().setNodeId(
                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId(String.valueOf(physicalSwitchId)));
        nodeBuilder.addAugmentation(PhysicalSwitchAugmentation.class,
                new PhysicalSwitchAugmentationBuilder().setManagedBy(new HwvtepGlobalRef(psNodeId)).build());
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.put(OPERATIONAL, nodeId, nodeBuilder.build(), true);
        tx.submit().checkedGet();
    }

    static void waitTillOperationCompletes(JobCoordinatorEventsWaiter coordinatorEventsWaiter,
                                           AsyncEventsWaiter asyncEventsWaiter) {
        coordinatorEventsWaiter.awaitEventsConsumption();
        asyncEventsWaiter.awaitEventsConsumption();
    }
}
