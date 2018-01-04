/*
 * Copyright © 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.impl;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Maps;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeMplsOverGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlanGpe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TepTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TepTypeExternal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TepTypeHwvtep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TepTypeInternal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.NodeIdTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.NodeIdTypeHwvtep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.NodeIdTypeIp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.NodeIdTypeOvsdb;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.TepStates;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.TunnelInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tep.states.TepState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tep.states.TepStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tep.states.TepStateKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tunnel.info.SrcTep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tunnel.info.SrcTepKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tunnel.info.src.tep.DstTep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tunnel.zones.TunnelZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tunnel.zones.tunnel.zone.Vteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceExternalIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class ItmTepUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ItmTepUtils.class);
    private final DataBroker dataBroker;
    private final IInterfaceManager ifManager;


    // To keep the mapping between Tunnel Types and Tunnel Interfaces
    public static final Map<Class<? extends TunnelTypeBase>, Class<? extends InterfaceTypeBase>>
        TUNNEL_INTERFACE_TYPE_MAP = new HashMap<Class<? extends TunnelTypeBase>, Class<? extends InterfaceTypeBase>>() {
            {
                put(TunnelTypeGre.class, InterfaceTypeGre.class);
                put(TunnelTypeMplsOverGre.class, InterfaceTypeGre.class);
                put(TunnelTypeVxlan.class, InterfaceTypeVxlan.class);
                put(TunnelTypeVxlanGpe.class, InterfaceTypeVxlan.class);
            }
        };

    public static final ImmutableBiMap<Class<? extends NodeIdTypeBase>, Class<? extends TepTypeBase>>
        TEP_NODEID_DEVICE_TYPE_MAP =
        new ImmutableBiMap.Builder<Class<? extends NodeIdTypeBase>, Class<? extends TepTypeBase>>()
            .put(NodeIdTypeOvsdb.class, TepTypeInternal.class)
            .put(NodeIdTypeHwvtep.class, TepTypeHwvtep.class)
            .put(NodeIdTypeIp.class, TepTypeExternal.class)
            .build();

    public static final ImmutableBiMap<Class<? extends NodeIdTypeBase>, Class<? extends TunnelTypeBase>>
        TEP_NODEID_TUNNEL_TYPE_MAP =
        new ImmutableBiMap.Builder<Class<? extends NodeIdTypeBase>, Class<? extends TunnelTypeBase>>()
            .put(NodeIdTypeHwvtep.class, TunnelTypeVxlan.class)
            .put(NodeIdTypeIp.class, TunnelTypeMplsOverGre.class)
            .build();

    // Tunnel options
    private static final String TUNNEL_OPTIONS_KEY = "key";
    private static final String TUNNEL_OPTIONS_LOCAL_IP = "local_ip";
    private static final String TUNNEL_OPTIONS_REMOTE_IP = "remote_ip";
    private static final String TUNNEL_OPTIONS_DESTINATION_PORT = "dst_port";
    public static final String TUNNEL_OPTIONS_TOS = "tos";

    // Option values for VxLAN-GPE + NSH tunnels
    private static final String TUNNEL_OPTIONS_EXTS = "exts";
    private static final String TUNNEL_OPTIONS_NSI = "nsi";
    private static final String TUNNEL_OPTIONS_NSP = "nsp";
    private static final String TUNNEL_OPTIONS_NSHC1 = "nshc1";
    private static final String TUNNEL_OPTIONS_NSHC2 = "nshc2";
    private static final String TUNNEL_OPTIONS_NSHC3 = "nshc3";
    private static final String TUNNEL_OPTIONS_NSHC4 = "nshc4";

    // Option values for VxLAN-GPE + NSH tunnels
    private static final String TUNNEL_OPTIONS_VALUE_FLOW = "flow";
    private static final String TUNNEL_OPTIONS_VALUE_GPE = "gpe";
    // UDP port for VxLAN-GPE Tunnels
    private static final String TUNNEL_OPTIONS_VALUE_GPE_DESTINATION_PORT = "4880";

    //tos option value for tunnels
    public static final String TUNNEL_OPTIONS_TOS_VALUE_INHERIT = "inherit";

    public static final String OVSDB_EXTERNAL_ID_HOSTNAME = "hostname";
    public static final TopologyId OVSDB_TOPOLOGY_ID = new TopologyId(new Uri("ovsdb:1"));
    public static final String IFACE_EXTERNAL_ID_TUNNEL_ZONE = "tunnel-zone";
    public static final String IFACE_EXTERNAL_ID_TUNNEL_TYPE = "tunnel-type";

    public static final String IFM_IDPOOL_NAME = "interfaces";

    @Inject
    public ItmTepUtils(final DataBroker dataBroker,
                       final IInterfaceManager interfaceManager) {
        this.dataBroker = dataBroker;
        this.ifManager = interfaceManager;
    }

    public static InstanceIdentifier<TerminationPoint> createTerminationPointInstanceIdentifier(
        String nodeIdStr, String portName) {
        NodeId nodeId = new NodeId(new Uri(nodeIdStr));
        return InstanceIdentifier.create(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(OVSDB_TOPOLOGY_ID))
            .child(Node.class,new NodeKey(nodeId))
            .child(TerminationPoint.class, new TerminationPointKey(new TpId(portName)));
    }

    public static TerminationPoint createOvsdbTerminationPoint(String nodeId, String ipStr,
                                                               TunnelZone zone) {
        Map<String, String> options = Maps.newHashMap();
        options.put(TUNNEL_OPTIONS_LOCAL_IP, String.valueOf(ipStr));
        options.put(TUNNEL_OPTIONS_REMOTE_IP, TUNNEL_OPTIONS_VALUE_FLOW);
        options.put(TUNNEL_OPTIONS_TOS, TUNNEL_OPTIONS_TOS_VALUE_INHERIT);
        // Specific options for each type of tunnel
        Class<? extends InterfaceTypeBase> ifType = TUNNEL_INTERFACE_TYPE_MAP.get(zone.getTunnelType());
        if (ifType.equals(TunnelTypeMplsOverGre.class)) {
            options.put(TUNNEL_OPTIONS_KEY, TUNNEL_OPTIONS_VALUE_FLOW);
        }
        if (ifType.equals(TunnelTypeVxlanGpe.class)) {
            options.put(TUNNEL_OPTIONS_EXTS, TUNNEL_OPTIONS_VALUE_GPE);
            options.put(TUNNEL_OPTIONS_NSI, TUNNEL_OPTIONS_VALUE_FLOW);
            options.put(TUNNEL_OPTIONS_NSP, TUNNEL_OPTIONS_VALUE_FLOW);
            options.put(TUNNEL_OPTIONS_NSHC1, TUNNEL_OPTIONS_VALUE_FLOW);
            options.put(TUNNEL_OPTIONS_NSHC2, TUNNEL_OPTIONS_VALUE_FLOW);
            options.put(TUNNEL_OPTIONS_NSHC3, TUNNEL_OPTIONS_VALUE_FLOW);
            options.put(TUNNEL_OPTIONS_NSHC4, TUNNEL_OPTIONS_VALUE_FLOW);
            // VxLAN-GPE interfaces will not use the default UDP port to avoid problems with other meshes
            options.put(TUNNEL_OPTIONS_DESTINATION_PORT, TUNNEL_OPTIONS_VALUE_GPE_DESTINATION_PORT);
        }

        String ifName = ItmUtils.getTrunkInterfaceName(null, nodeId, ipStr, zone.getTunnelType().getName());
        OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();
        tpAugmentationBuilder.setName(ifName);
        if (ifType != null) {
            tpAugmentationBuilder.setInterfaceType(ifType);
        }
        if (options != null) {
            List<Options> optionsList = new ArrayList<>();
            for (Map.Entry<String, String> entry : options.entrySet()) {
                OptionsBuilder optionsBuilder = new OptionsBuilder();
                optionsBuilder.setKey(new OptionsKey(entry.getKey()));
                optionsBuilder.setOption(entry.getKey());
                optionsBuilder.setValue(entry.getValue());
                optionsList.add(optionsBuilder.build());
            }
            tpAugmentationBuilder.setOptions(optionsList);
        }

        List<InterfaceExternalIds> ifaceExternalIds = new ArrayList<>();
        InterfaceExternalIdsBuilder ifaceExternalIdBuilder = new InterfaceExternalIdsBuilder();
        ifaceExternalIdBuilder.setExternalIdKey(IFACE_EXTERNAL_ID_TUNNEL_ZONE);
        ifaceExternalIdBuilder.setExternalIdValue(zone.getTunnelZoneName());
        ifaceExternalIds.add(ifaceExternalIdBuilder.build());

        ifaceExternalIdBuilder = new InterfaceExternalIdsBuilder();
        ifaceExternalIdBuilder.setExternalIdKey(IFACE_EXTERNAL_ID_TUNNEL_TYPE);
        ifaceExternalIdBuilder.setExternalIdValue(ItmUtils.TUNNEL_TYPE_MAP.inverse().get(zone.getTunnelType()));
        ifaceExternalIds.add(ifaceExternalIdBuilder.build());
        tpAugmentationBuilder.setInterfaceExternalIds(ifaceExternalIds);

        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        InstanceIdentifier<TerminationPoint> tpIid =
            createTerminationPointInstanceIdentifier(nodeId, ifName);
        tpBuilder.setKey(InstanceIdentifier.keyOf(tpIid));
        tpBuilder.addAugmentation(OvsdbTerminationPointAugmentation.class, tpAugmentationBuilder.build());
        return tpBuilder.build();
    }

    public static TepState createTepState(DataBroker dataBroker,
                                          InstanceIdentifier<OvsdbTerminationPointAugmentation> iid,
                                          OvsdbTerminationPointAugmentation tp) {
        TepStateBuilder tsBuilder = new TepStateBuilder();
        String bridgeId = iid.firstKeyOf(Node.class).getNodeId().getValue();
        tsBuilder.setTepNodeId(bridgeId);
        tsBuilder.setTepIfName(tp.getName());
        tsBuilder.setTepState(true);
        tsBuilder.setTepOfPort(tp.getOfport());
        tsBuilder.setTepNodeType(NodeIdTypeOvsdb.class);

        InstanceIdentifier<Node> bridgeIid = iid.firstIdentifierOf(Node.class);
        // TODO: Use cache to eliminate this
        Optional<Node> optBridge = ItmUtils.read(LogicalDatastoreType.OPERATIONAL, bridgeIid, dataBroker);
        OvsdbBridgeAugmentation bridge =
            optBridge.isPresent() ? optBridge.get().getAugmentation(OvsdbBridgeAugmentation.class) : null;
        if (bridge != null) {
            BigInteger dpnId = getDpnId(bridge.getDatapathId());
            if (dpnId != null) {
                tsBuilder.setDpnId(dpnId);
            }
            // TODO: Use cache to eliminate this
            InstanceIdentifier<Node> nodeIid = (InstanceIdentifier<Node>)bridge.getManagedBy().getValue();
            Optional<Node> optNode = ItmUtils.read(LogicalDatastoreType.OPERATIONAL, nodeIid, dataBroker);
            OvsdbNodeAugmentation ovsNode =
                optNode.isPresent() ? optNode.get().getAugmentation(OvsdbNodeAugmentation.class) : null;
            if (ovsNode != null) {
                String hostname = getHostname(ovsNode);
                if (hostname != null && !hostname.isEmpty()) {
                    tsBuilder.setTepHostName(hostname);
                }
            }
        }
        if (InterfaceTypeVxlan.class.equals(tp.getInterfaceType())) {
            tsBuilder.setTepType(TunnelTypeVxlan.class);
        } else if (InterfaceTypeGre.class.equals(tp.getInterfaceType())) {
            tsBuilder.setTepType(TunnelTypeGre.class);
        }
        if (tp.getOptions() != null && !tp.getOptions().isEmpty()) {
            for (Options option : tp.getOptions()) {
                if (TUNNEL_OPTIONS_LOCAL_IP.equals(option.getOption())) {
                    tsBuilder.setTepIp(new IpAddress(option.getValue().toCharArray()));
                } else if (TUNNEL_OPTIONS_VALUE_GPE.equals(option.getOption())) {
                    tsBuilder.setTepType(TunnelTypeVxlanGpe.class);
                }
            }
        }
        return tsBuilder.build();
    }

    public static InstanceIdentifier<TepState> createTepStateIdentifier(String tepNodeId) {
        return InstanceIdentifier.create(TepStates.class)
            .child(TepState.class, new TepStateKey(tepNodeId));
    }

    public static BigInteger getDpnId(DatapathId datapathId) {
        if (datapathId != null) {
            String dpIdStr = datapathId.getValue().replace(":", "");
            BigInteger dpnId =  new BigInteger(dpIdStr, 16);
            return dpnId;
        }
        return null;
    }

    private static String getHostname(OvsdbNodeAugmentation ovsNode) {
        if (ovsNode != null && ovsNode.getOpenvswitchExternalIds() != null
            && !ovsNode.getOpenvswitchExternalIds().isEmpty()) {
            for (OpenvswitchExternalIds externalId : ovsNode.getOpenvswitchExternalIds()) {
                if (OVSDB_EXTERNAL_ID_HOSTNAME.equals(externalId.getExternalIdKey())) {
                    return externalId.getExternalIdValue();
                }
            }
        }
        return null;
    }

    private SrcTep getSrcTep(String tepNodeId) {
        InstanceIdentifier<SrcTep> srcTepIid = InstanceIdentifier.create(TunnelInfo.class)
            .child(SrcTep.class, new SrcTepKey(tepNodeId));
        Optional<SrcTep> optSrcTep = ItmUtils.read(LogicalDatastoreType.OPERATIONAL, srcTepIid, dataBroker);
        return optSrcTep.isPresent() ? optSrcTep.get() : null;
    }

    private TepState getTepState(DstTep dstTep) {
        InstanceIdentifier<TepState> iid = createTepStateIdentifier(dstTep.getDstTepNodeId());
        Optional<TepState> optTepState = ItmUtils.read(LogicalDatastoreType.OPERATIONAL, iid, dataBroker);
        return optTepState.isPresent() ? optTepState.get() : null;
    }


    public String getTepPortName(String nodeId, IpAddress ipAddr, Class<? extends TunnelTypeBase> tunnelType) {
        return ItmUtils.getTrunkInterfaceName(nodeId, String.valueOf(ipAddr.getValue()), null,  tunnelType.getName());
    }

    public String getVtepPortName(Vteps srcVtep, Vteps dstVtep, TunnelZone zone) {
        Class<? extends TunnelTypeBase> tunnelType = getTunnelType(srcVtep, dstVtep, zone);
        return getTepPortName(srcVtep.getNodeId(), srcVtep.getIpAddress(), tunnelType);
    }

    public String getVtepInterfaceName(Vteps srcVtep, Vteps dstVtep, TunnelZone zone) {
        Class<? extends TunnelTypeBase> tunnelType = getTunnelType(srcVtep, dstVtep, zone);
        String portName = getTepPortName(srcVtep.getNodeId(), srcVtep.getIpAddress(), tunnelType);
        return ItmUtils.getTrunkInterfaceName(portName, srcVtep.getNodeId(), dstVtep.getNodeId(), tunnelType.getName());
    }

    public long getGroupId(Integer intIfIndex) {
        return ifManager.getLogicalTunnelSelectGroupId(intIfIndex);
    }

    public void releaseIfIndex(String ifName) {
        ifManager.releaseIfIndex(ifName);
    }

    public Integer allocateIfIndex(String ifName) {
        return ifManager.allocateIfIndex(ifName);
    }

    public Class<? extends TunnelTypeBase> getTunnelType(Vteps srcVtep, Vteps dstVtep, TunnelZone zone) {
        return dstVtep.getVtepTunnelType() != null ? dstVtep.getVtepTunnelType()
            : srcVtep.getVtepTunnelType() != null ? srcVtep.getVtepTunnelType() : zone.getTunnelType();
    }
}
