/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.impl;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.ReleaseIdInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.ReleaseIdInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeMplsOverGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlanGpe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbPortInterfaceAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceExternalIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsBuilder;
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
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class ItmTepUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ItmTepUtils.class);
    private final DataBroker dataBroker;
    private final IInterfaceManager ifManager;
    private final IdManagerService idManager;
    private final ItmCacheManager itmCacheManager;


    private static final String TUNNEL_PORT_PREFIX = "tun";

    // To keep the mapping between Tunnel Types and Tunnel Interfaces
    public static final ImmutableMap<Class<? extends TunnelTypeBase>, Class<? extends InterfaceTypeBase>>
        TUNNEL_INTERFACE_TYPE_MAP =
        new ImmutableMap.Builder<Class<? extends TunnelTypeBase>, Class<? extends InterfaceTypeBase>>()
            .put(TunnelTypeGre.class, InterfaceTypeGre.class)
            .put(TunnelTypeMplsOverGre.class, InterfaceTypeGre.class)
            .put(TunnelTypeVxlan.class, InterfaceTypeVxlan.class)
            .put(TunnelTypeVxlanGpe.class, InterfaceTypeVxlan.class)
            .build();


    private static final String BFD_PARAM_ENABLE = "enable";
    private static final String BFD_PARAM_MIN_TX = "min_tx";
    private static final String BFD_PARAM_FORWARDING_IF_RX = "forwarding_if_rx";

    // BFD parameters
    public static final String BFD_OP_STATE = "state";
    public static final String BFD_STATE_UP = "up";
    private static final String BFD_MIN_TX_VAL = "100";
    private static final String BFD_FORWARDING_IF_RX_VAL = "true";

    // Tunnel options
    public static final String TUNNEL_OPTIONS_KEY = "key";
    public static final String TUNNEL_OPTIONS_LOCAL_IP = "local_ip";
    public static final String TUNNEL_OPTIONS_REMOTE_IP = "remote_ip";
    public static final String TUNNEL_OPTIONS_DESTINATION_PORT = "dst_port";
    public static final String TUNNEL_OPTIONS_TOS = "tos";

    // Option values for VxLAN-GPE + NSH tunnels
    private static final String TUNNEL_OPTIONS_EXTS = "exts";
    private static final String TUNNEL_OPTIONS_NSI = "nsi";
    private static final String TUNNEL_OPTIONS_NSP = "nsp";
    private static final String TUNNEL_OPTIONS_NSHC1 = "nshc1";
    private static final String TUNNEL_OPTIONS_NSHC2 = "nshc2";
    private static final String TUNNEL_OPTIONS_NSHC3 = "nshc3";
    private static final String TUNNEL_OPTIONS_NSHC4 = "nshc4";

    // Tunnel options for MPLS-GRE tunnels [requires OVS 2.8+)
    private static final String TUNNEL_OPTIONS_PKT_TYPE = "packet_type";

    // Option values for VxLAN-GPE + NSH tunnels
    public static final String TUNNEL_OPTIONS_VALUE_FLOW = "flow";
    private static final String TUNNEL_OPTIONS_VALUE_GPE = "gpe";
    // UDP port for VxLAN-GPE Tunnels
    private static final String TUNNEL_OPTIONS_VALUE_GPE_DESTINATION_PORT = "4880";

    // Tunnel option values for MPLS-GRE tunnels [requires OVS 2.8+)
    private static final String TUNNEL_OPTIONS_VALUE_LEGACY_L3 = "legacy_l3";

    //tos option value for tunnels
    public static final String TUNNEL_OPTIONS_TOS_VALUE_INHERIT = "inherit";

    public static final String OVSDB_EXTERNAL_ID_HOSTNAME = "hostname";
    public static final TopologyId OVSDB_TOPOLOGY_ID = new TopologyId(new Uri("ovsdb:1"));
    public static final String IFACE_EXTERNAL_ID_TUNNEL_TYPE = "odl-tunnel-type";
    public static final String IFACE_EXTERNAL_ID_GROUPID = "odl-group-id";
    public static final String IFACE_EXTERNAL_ID_DPNID = "odl-dpn-id";
    public static final String IFACE_EXTERNAL_ID_PEER_ID = "odl-peer-id";

    public static final long INVALID_GROUP_ID = 0;

    @Inject
    public ItmTepUtils(final DataBroker dataBroker,
                       final IInterfaceManager interfaceManager,
                       final IdManagerService idManagerService,
                       final ItmCacheManager itmCacheManager) {
        this.dataBroker = dataBroker;
        this.ifManager = interfaceManager;
        this.idManager = idManagerService;
        this.itmCacheManager = itmCacheManager;
    }

    public static InstanceIdentifier<TerminationPoint> createTerminationPointInstanceIdentifier(
        String nodeIdStr, String portName) {
        NodeId nodeId = new NodeId(new Uri(nodeIdStr));
        return InstanceIdentifier.create(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(OVSDB_TOPOLOGY_ID))
            .child(Node.class,new NodeKey(nodeId))
            .child(TerminationPoint.class, new TerminationPointKey(new TpId(portName)));
    }

    private TerminationPoint getOvsdbTerminationPoint(String nodeId, String ifName, String localIp, String remoteIp,
                                                      Class<? extends TunnelTypeBase> tunnelType, int vlanId,
                                                      BigInteger dpnId, String peerNodeId, long groupId) {
        //TODO: Add monitoring params
        Class<? extends InterfaceTypeBase> ifType = TUNNEL_INTERFACE_TYPE_MAP.get(tunnelType);

        //Populate caches - FIXME: Demonstrative only, should be populated much earlier in config path
        itmCacheManager.addDpnIdAndIp(dpnId, localIp);

        OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();
        tpAugmentationBuilder.setName(ifName);
        tpAugmentationBuilder.setInterfaceType(ifType);
        if (vlanId != 0) {
            tpAugmentationBuilder.setVlanMode(OvsdbPortInterfaceAttributes.VlanMode.Access);
            tpAugmentationBuilder.setVlanTag(new VlanId(vlanId));
        }
        Map<String, String> options = Maps.newHashMap();
        options.put(TUNNEL_OPTIONS_LOCAL_IP, localIp);
        options.put(TUNNEL_OPTIONS_REMOTE_IP, remoteIp);
        options.put(TUNNEL_OPTIONS_TOS, TUNNEL_OPTIONS_TOS_VALUE_INHERIT);
        // Specific options for each type of tunnel
        if (ifType.equals(TunnelTypeMplsOverGre.class)) {
            options.put(TUNNEL_OPTIONS_PKT_TYPE, TUNNEL_OPTIONS_VALUE_LEGACY_L3);
        } else {
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
        List<Options> optionsList = new ArrayList<>();
        for (Map.Entry<String, String> entry : options.entrySet()) {
            OptionsBuilder optionsBuilder = new OptionsBuilder();
            optionsBuilder.setOption(entry.getKey());
            optionsBuilder.setValue(entry.getValue());
            optionsList.add(optionsBuilder.build());
        }
        tpAugmentationBuilder.setOptions(optionsList);
        Map<String, String> externalIds = Maps.newHashMap();
        String tunTypeStr = ItmUtils.TUNNEL_TYPE_MAP.inverse().get(tunnelType);
        externalIds.put(IFACE_EXTERNAL_ID_TUNNEL_TYPE, tunTypeStr);
        externalIds.put(IFACE_EXTERNAL_ID_DPNID, dpnId.toString(10));
        externalIds.put(IFACE_EXTERNAL_ID_PEER_ID, peerNodeId);
        if (groupId == INVALID_GROUP_ID) {
            //TODO: Reference on how to create groupId for a given Tep.
            groupId = allocateDestGroupId(getUniqueTepIdentifier(peerNodeId, tunTypeStr));
        }
        externalIds.put(IFACE_EXTERNAL_ID_GROUPID, String.valueOf(groupId));
        List<InterfaceExternalIds> ifaceExternalIds = new ArrayList<>();
        for (Map.Entry<String, String> entry : externalIds.entrySet()) {
            InterfaceExternalIdsBuilder externalIdsBuilder = new InterfaceExternalIdsBuilder();
            externalIdsBuilder.setExternalIdKey(entry.getKey());
            externalIdsBuilder.setExternalIdValue(entry.getValue());
            ifaceExternalIds.add(externalIdsBuilder.build());
        }

        tpAugmentationBuilder.setInterfaceExternalIds(ifaceExternalIds);

        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        InstanceIdentifier<TerminationPoint> tpIid =
            createTerminationPointInstanceIdentifier(nodeId, ifName);
        tpBuilder.setKey(InstanceIdentifier.keyOf(tpIid));
        tpBuilder.addAugmentation(OvsdbTerminationPointAugmentation.class, tpAugmentationBuilder.build());
        return tpBuilder.build();
    }

    private String getHostname(OvsdbNodeAugmentation ovsNode) {
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


    private String getUniqueIdString(String idKey) {
        return UUID.nameUUIDFromBytes(idKey.getBytes(StandardCharsets.UTF_8)).toString()
            .substring(0, 12).replace("-", "");
    }

    private String getTunnelInterfaceName(String uniqueTepKey, String localHostName, String remoteHostName,
                                          Class<? extends TunnelTypeBase> tunnelType) {
        String tunnelTypeStr = ItmUtils.TUNNEL_TYPE_MAP.inverse().get(tunnelType);
        String trunkInterfaceName = String.format("%s:%s:%s:%s", uniqueTepKey, localHostName,
            remoteHostName, tunnelTypeStr);
        LOG.trace("trunk interface name is {}", trunkInterfaceName);
        trunkInterfaceName = String.format("%s%s", TUNNEL_PORT_PREFIX, getUniqueIdString(trunkInterfaceName));
        return trunkInterfaceName;
    }


    /*
     * This is to create a Identifier that uniquely identifies a TEP.
     * Aim is to generate key to be used for getting GroupId that will be
     * used to add actions to send pkts to this tep as destination. Can be
     * used for other purposes to uniquely identify a tep also.
     *
     * @param tepNodeId dpnid for OVS, nodeID for Hwvtep and IP for DCGW
     * @param tepType String tunnelType as defined in ItmUtils.TUNNEL_TYPE_MAP
     * @return "tepNodeId:tepType"
     */
    private String getUniqueTepIdentifier(String tepNodeId, String tunnelType) {
        return String.format("%s:%s",tepNodeId, tunnelType);
    }

    public String getOption(String optionKey, List<Options> options) {
        if (options != null) {
            for (Options option: options) {
                if (option.getOption().equals(optionKey)) {
                    return option.getValue();
                }
            }
        }
        return null;
    }

    public Map<String, String> getIfaceExternalIds(OvsdbTerminationPointAugmentation tp) {
        Map<String, String> externalIds = new HashMap<String, String>();
        if (tp.getInterfaceExternalIds() != null) {
            for (InterfaceExternalIds externalId : tp.getInterfaceExternalIds()) {
                externalIds.put(externalId.getExternalIdKey(), externalId.getExternalIdValue());
            }
        }
        return externalIds;
    }

    public long allocateDestGroupId(String key) {
        AllocateIdInput getIdInput =
            new AllocateIdInputBuilder().setPoolName(ITMConstants.ITM_IDPOOL_NAME).setIdKey(key).build();
        try {
            Future<RpcResult<AllocateIdOutput>> result = idManager.allocateId(getIdInput);
            RpcResult<AllocateIdOutput> rpcResult = result.get();
            if (rpcResult.isSuccessful()) {
                return ITMConstants.ITM_GROUPID_POOL_START + rpcResult.getResult().getIdValue();
            } else {
                LOG.warn("RPC Call to Get Unique Id returned with Errors {}", rpcResult.getErrors());
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Exception when getting Unique Id", e);
        }
        return INVALID_GROUP_ID;
    }

    public void releaseDestGroupId(String key) {
        ReleaseIdInput idInput =
            new ReleaseIdInputBuilder().setPoolName(ITMConstants.ITM_IDPOOL_NAME).setIdKey(key).build();
        try {
            Future<RpcResult<Void>> result = idManager.releaseId(idInput);
            RpcResult<Void> rpcResult = result.get();
            if (!rpcResult.isSuccessful()) {
                LOG.warn("RPC Call to release Id {} with Key {} returned with Errors {}", key, rpcResult.getErrors());
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Exception when releasing Id for key {}", key, e);
        }
    }

    public BigInteger getDpnId(InstanceIdentifier<OvsdbTerminationPointAugmentation> iid) {

        InstanceIdentifier<Node> bridgeIid = iid.firstIdentifierOf(Node.class);
        // TODO: Use cache to eliminate this
        Optional<Node> optBridge = ItmUtils.read(LogicalDatastoreType.OPERATIONAL, bridgeIid, dataBroker);
        OvsdbBridgeAugmentation bridge =
            optBridge.isPresent() ? optBridge.get().getAugmentation(OvsdbBridgeAugmentation.class) : null;
        if (bridge != null) {
            BigInteger dpnId = getDpnId(bridge.getDatapathId());
            return dpnId;
        }
        return null;
    }

    public BigInteger getDpnId(DatapathId datapathId) {
        if (datapathId != null) {
            String dpIdStr = datapathId.getValue().replace(":", "");
            BigInteger dpnId =  new BigInteger(dpIdStr, 16);
            return dpnId;
        }
        return null;
    }

    public BigInteger getDpnId(String portName) {
        return ifManager.getDpnForInterface(portName);
    }

    public Long getGroupId(String ovsPortName) {
        OvsdbTerminationPointAugmentation tp = ifManager.getTerminationPointForInterface(ovsPortName);
        if (tp.getInterfaceExternalIds() != null) {
            for (InterfaceExternalIds externalId : tp.getInterfaceExternalIds()) {
                if (IFACE_EXTERNAL_ID_GROUPID.equals(externalId.getExternalIdKey())) {
                    return Long.parseLong(externalId.getExternalIdValue());
                }
            }
        }
        return null;
    }
}
