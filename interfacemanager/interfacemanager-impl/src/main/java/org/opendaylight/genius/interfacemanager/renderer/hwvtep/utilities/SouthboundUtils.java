/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.renderer.hwvtep.utilities;

import static org.opendaylight.mdsal.binding.api.WriteTransaction.CREATE_MISSING_PARENTS;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.genius.infra.Datastore.Operational;
import org.opendaylight.genius.infra.TypedWriteTransaction;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.Tunnels;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.TunnelsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.tunnel.attributes.BfdParams;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.tunnel.attributes.BfdParamsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.tunnel.attributes.BfdParamsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SouthboundUtils {
    private static final Logger LOG = LoggerFactory.getLogger(SouthboundUtils.class);
    private static final Logger EVENT_LOGGER = LoggerFactory.getLogger("GeniusEventLogger");

    public static final String HWVTEP_TOPOLOGY = "hwvtep:1";
    public static final TopologyId HWVTEP_TOPOLOGY_ID = new TopologyId(new Uri(HWVTEP_TOPOLOGY));
    public static final String TEP_PREFIX = "vxlan_over_ipv4:";
    public static final String BFD_OP_STATE = "state";
    public static final String BFD_STATE_UP = "up";
    public static final String PS_NODE_ID_PREFIX = "/physicalswitch";

    // BFD parameters
    static final String BFD_PARAM_ENABLE = "enable";
    static final String BFD_PARAM_MIN_RX = "min_rx";
    static final String BFD_PARAM_MIN_TX = "min_tx";
    static final String BFD_PARAM_DECAY_MIN_RX = "decay_min_rx";
    static final String BFD_PARAM_FORWARDING_IF_RX = "forwarding_if_rx";
    static final String BFD_PARAM_CPATH_DOWN = "cpath_down";
    static final String BFD_PARAM_CHECK_TNL_KEY = "check_tnl_key";

    // BFD Local/Remote Configuration parameters
    static final String BFD_CONFIG_BFD_DST_MAC = "bfd_dst_mac";
    static final String BFD_CONFIG_BFD_DST_IP = "bfd_dst_ip";

    // BFD parameters
    private static final String BFD_MIN_RX_VAL = "1000";
    private static final String BFD_MIN_TX_VAL = "100";
    private static final String BFD_DECAY_MIN_RX_VAL = "200";
    private static final String BFD_FORWARDING_IF_RX_VAL = "true";
    private static final String BFD_CPATH_DOWN_VAL = "false";
    private static final String BFD_CHECK_TNL_KEY_VAL = "false";

    private SouthboundUtils() {

    }

    public static InstanceIdentifier<Node> createPhysicalSwitchInstanceIdentifier(String psNodeIdString) {
        NodeId physicalSwitchNodeId = new NodeId(psNodeIdString);
        InstanceIdentifier<Node> psNodeId = InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(HWVTEP_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(physicalSwitchNodeId));
        return psNodeId;
    }

    public static InstanceIdentifier<Node> createGlobalNodeInstanceIdentifier(DataBroker dataBroker,
            String physicalSwitchNodeId) {
        InstanceIdentifier<Node> psNodeId = InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(HWVTEP_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(new NodeId(physicalSwitchNodeId)));
        Optional<Node> physicalSwitchOptional = IfmUtil.read(LogicalDatastoreType.OPERATIONAL, psNodeId, dataBroker);
        if (!physicalSwitchOptional.isPresent()) {
            LOG.debug("physical switch is not present for {}", physicalSwitchNodeId);
            return null;
        }
        Node physicalSwitch = physicalSwitchOptional.get();
        PhysicalSwitchAugmentation physicalSwitchAugmentation = physicalSwitch
                .augmentation(PhysicalSwitchAugmentation.class);
        return (InstanceIdentifier<Node>) physicalSwitchAugmentation.getManagedBy().getValue();
    }

    public static @Nullable InstanceIdentifier<Node> createGlobalNodeInstanceIdentifier(String psNodeIdString) {
        String globalNodeIdStr;
        try {
            globalNodeIdStr = psNodeIdString.substring(0, psNodeIdString.indexOf(PS_NODE_ID_PREFIX));
        } catch (StringIndexOutOfBoundsException ex) {
            LOG.error("cannot determine global-node-id for the physical node {}", psNodeIdString);
            return null;
        }
        NodeId globalNodeId = new NodeId(globalNodeIdStr);
        InstanceIdentifier<Node> globalNodeInstanceId = InstanceIdentifier
            .create(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(HWVTEP_TOPOLOGY_ID))
            .child(Node.class,new NodeKey(globalNodeId));
        return globalNodeInstanceId;
    }

    public static InstanceIdentifier<TerminationPoint> createTEPInstanceIdentifier(InstanceIdentifier<Node> nodeIid,
            IpAddress ipAddress) {
        TerminationPointKey localTEP = SouthboundUtils.getTerminationPointKey(ipAddress.getIpv4Address().getValue());
        return createInstanceIdentifier(nodeIid, localTEP);
    }

    public static InstanceIdentifier<TerminationPoint> createInstanceIdentifier(InstanceIdentifier<Node> nodeIid,
            TerminationPointKey tpKey) {
        return nodeIid.child(TerminationPoint.class, tpKey);
    }

    public static InstanceIdentifier<Tunnels> createTunnelsInstanceIdentifier(InstanceIdentifier<Node> nodeId,
            IpAddress localIP, IpAddress remoteIp) {
        InstanceIdentifier<TerminationPoint> localTEPInstanceIdentifier = createTEPInstanceIdentifier(nodeId, localIP);
        InstanceIdentifier<TerminationPoint> remoteTEPInstanceIdentifier = createTEPInstanceIdentifier(nodeId,
                remoteIp);

        TunnelsKey tunnelsKey = new TunnelsKey(new HwvtepPhysicalLocatorRef(localTEPInstanceIdentifier),
                new HwvtepPhysicalLocatorRef(remoteTEPInstanceIdentifier));
        return InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(HWVTEP_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(nodeId.firstKeyOf(Node.class)))
                .augmentation(PhysicalSwitchAugmentation.class).child(Tunnels.class, tunnelsKey).build();
    }

    public static InstanceIdentifier<Tunnels> createTunnelsInstanceIdentifier(InstanceIdentifier<Node> nodeId,
            InstanceIdentifier<TerminationPoint> localTEPInstanceIdentifier,
            InstanceIdentifier<TerminationPoint> remoteTEPInstanceIdentifier) {
        TunnelsKey tunnelsKey = new TunnelsKey(new HwvtepPhysicalLocatorRef(localTEPInstanceIdentifier),
                new HwvtepPhysicalLocatorRef(remoteTEPInstanceIdentifier));

        InstanceIdentifier<Tunnels> tunnelInstanceId = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(HWVTEP_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(nodeId.firstKeyOf(Node.class)))
                .augmentation(PhysicalSwitchAugmentation.class).child(Tunnels.class, tunnelsKey).build();
        return tunnelInstanceId;
    }

    public static String getTerminationPointKeyString(String ipAddress) {
        String tpKeyStr = null;
        if (ipAddress != null) {
            tpKeyStr = TEP_PREFIX + ipAddress;
        }
        return tpKeyStr;
    }

    public static TerminationPointKey getTerminationPointKey(String ipAddress) {
        TerminationPointKey tpKey = null;
        String tpKeyStr = getTerminationPointKeyString(ipAddress);
        if (tpKeyStr != null) {
            tpKey = new TerminationPointKey(new TpId(tpKeyStr));
        }
        return tpKey;
    }

    public static void setDstIp(HwvtepPhysicalLocatorAugmentationBuilder tpAugmentationBuilder, IpAddress ipAddress) {
        IpAddress ip = new IpAddress(ipAddress);
        tpAugmentationBuilder.setDstIp(ip);
    }

    public static void addStateEntry(Interface interfaceInfo, IfTunnel ifTunnel,
            TypedWriteTransaction<Operational> transaction) {
        LOG.debug("adding tep interface state for {}", interfaceInfo);
        if (interfaceInfo == null) {
            return;
        }

        OperStatus operStatus = OperStatus.Up;
        AdminStatus adminStatus = AdminStatus.Up;
        if (!interfaceInfo.isEnabled()) {
            operStatus = OperStatus.Down;
        }
        List<String> childLowerLayerIfList = new ArrayList<>();
        childLowerLayerIfList.add(0, SouthboundUtils
                .getTerminationPointKeyString(ifTunnel.getTunnelDestination().getIpv4Address().getValue()));
        InterfaceBuilder ifaceBuilder = new InterfaceBuilder().setAdminStatus(adminStatus).setOperStatus(operStatus)
                .setLowerLayerIf(childLowerLayerIfList);

        ifaceBuilder.setType(interfaceInfo.getType());

        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.interfaces.rev140508.interfaces.state.Interface> ifStateId = IfmUtil
            .buildStateInterfaceId(interfaceInfo.getName());
        ifaceBuilder.withKey(IfmUtil.getStateInterfaceKeyFromName(interfaceInfo.getName()));
        transaction.put(ifStateId, ifaceBuilder.build(), CREATE_MISSING_PARENTS);
        EVENT_LOGGER.info("IFM-TepInterfaceState,ADD {}", interfaceInfo.getName());
    }

    public static void fillBfdParameters(List<BfdParams> bfdParams, IfTunnel ifTunnel) {
        setBfdParamForEnable(bfdParams, ifTunnel != null ? ifTunnel.isMonitorEnabled() : true);
        bfdParams.add(getBfdParams(BFD_PARAM_MIN_TX,
                ifTunnel != null ? ifTunnel.getMonitorInterval().toString() : BFD_MIN_TX_VAL));
        bfdParams.add(getBfdParams(BFD_PARAM_MIN_RX, BFD_MIN_RX_VAL));
        bfdParams.add(getBfdParams(BFD_PARAM_DECAY_MIN_RX, BFD_DECAY_MIN_RX_VAL));
        bfdParams.add(getBfdParams(BFD_PARAM_FORWARDING_IF_RX, BFD_FORWARDING_IF_RX_VAL));
        bfdParams.add(getBfdParams(BFD_PARAM_CPATH_DOWN, BFD_CPATH_DOWN_VAL));
        bfdParams.add(getBfdParams(BFD_PARAM_CHECK_TNL_KEY, BFD_CHECK_TNL_KEY_VAL));
    }

    public static void setBfdParamForEnable(List<BfdParams> bfdParams, boolean isEnabled) {
        bfdParams.add(getBfdParams(BFD_PARAM_ENABLE, Boolean.toString(isEnabled)));
    }

    public static BfdParams getBfdParams(String key, String value) {
        return new BfdParamsBuilder().setBfdParamKey(key).withKey(new BfdParamsKey(key)).setBfdParamValue(value)
            .build();
    }
}
