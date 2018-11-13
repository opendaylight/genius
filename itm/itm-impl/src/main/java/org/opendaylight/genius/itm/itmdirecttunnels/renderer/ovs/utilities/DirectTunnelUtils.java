/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.opendaylight.genius.infra.Datastore.Configuration;
import org.opendaylight.genius.infra.TypedReadWriteTransaction;
import org.opendaylight.genius.infra.TypedWriteTransaction;
import org.opendaylight.genius.interfacemanager.globals.IfmConstants;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ITMBatchingUtils;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.mdsalutil.FlowEntity;
import org.opendaylight.genius.mdsalutil.InstructionInfo;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.MatchInfoBase;
import org.opendaylight.genius.mdsalutil.MetaDataUtil;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.mdsalutil.instructions.InstructionGotoTable;
import org.opendaylight.genius.mdsalutil.instructions.InstructionWriteMetadata;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.genius.mdsalutil.matches.MatchInPort;
import org.opendaylight.infrautils.utils.concurrent.KeyedLocks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.ReleaseIdInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.ReleaseIdInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.ReleaseIdOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBfd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlanGpe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.BridgeTunnelInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.IfIndexesTunnelMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.OvsBridgeRefInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210._if.indexes.tunnel.map.IfIndexTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210._if.indexes.tunnel.map.IfIndexTunnelKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.bridge.tunnel.info.OvsBridgeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.bridge.tunnel.info.OvsBridgeEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.bridge.tunnel.info.ovs.bridge.entry.OvsBridgeTunnelEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.bridge.tunnel.info.ovs.bridge.entry.OvsBridgeTunnelEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.bridge.tunnel.info.ovs.bridge.entry.OvsBridgeTunnelEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.ovs.bridge.ref.info.OvsBridgeRefEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.ovs.bridge.ref.info.OvsBridgeRefEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnTepsState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelOperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.DpnsTeps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.DpnsTepsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.dpns.teps.RemoteDpns;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbPortInterfaceAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceBfd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceBfdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceBfdKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.OperationFailedException;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class DirectTunnelUtils {

    private static final Logger LOG = LoggerFactory.getLogger(DirectTunnelUtils.class);

    private static final String BFD_PARAM_ENABLE = "enable";
    private static final String BFD_PARAM_MIN_TX = "min_tx";
    private static final String BFD_PARAM_FORWARDING_IF_RX = "forwarding_if_rx";
    // BFD parameters
    private static final String BFD_ENABLE_KEY = "enable";
    private static final String BFD_ENABLE_VALUE = "true";
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
    public static final String TUNNEL_OPTIONS_EXTS = "exts";
    public static final String TUNNEL_OPTIONS_NSI = "nsi";
    public static final String TUNNEL_OPTIONS_NSP = "nsp";
    public static final String TUNNEL_OPTIONS_NSHC1 = "nshc1";
    public static final String TUNNEL_OPTIONS_NSHC2 = "nshc2";
    public static final String TUNNEL_OPTIONS_NSHC3 = "nshc3";
    public static final String TUNNEL_OPTIONS_NSHC4 = "nshc4";

    // Option values for VxLAN-GPE + NSH tunnels
    public static final String TUNNEL_OPTIONS_VALUE_FLOW = "flow";
    public static final String TUNNEL_OPTIONS_VALUE_GPE = "gpe";
    // UDP port for VxLAN-GPE Tunnels
    public static final String TUNNEL_OPTIONS_VALUE_GPE_DESTINATION_PORT = "4880";

    //tos option value for tunnels
    public static final String TUNNEL_OPTIONS_TOS_VALUE_INHERIT = "inherit";

    private static final TopologyId OVSDB_TOPOLOGY_ID = new TopologyId(new Uri("ovsdb:1"));

    private static final long INVALID_ID = 0;
    private final KeyedLocks<String> tunnelLocks = new KeyedLocks<>();

    // To keep the mapping between Tunnel Types and Tunnel Interfaces

    public static final ImmutableMap<Class<? extends TunnelTypeBase>,
            Class<? extends InterfaceTypeBase>> TUNNEL_TYPE_MAP = new ImmutableMap
            .Builder<Class<? extends TunnelTypeBase>, Class<? extends InterfaceTypeBase>>()
            .put(TunnelTypeGre.class, InterfaceTypeGre.class)
            .put(TunnelTypeVxlan.class, InterfaceTypeVxlan.class)
            .put(TunnelTypeVxlanGpe.class, InterfaceTypeVxlan.class).build();

    private static final String TUNNEL_PORT_REGEX = "tun[0-9a-f]{11}";
    private static final Pattern TUNNEL_PORT_PATTERN = Pattern.compile(TUNNEL_PORT_REGEX);
    public  static final Predicate<String> TUNNEL_PORT_PREDICATE =
        portName -> TUNNEL_PORT_PATTERN.matcher(portName).matches();

    private final IdManagerService idManagerService;
    private final IMdsalApiManager mdsalApiManager;

    @Inject
    public DirectTunnelUtils(final IdManagerService idManagerService, final IMdsalApiManager mdsalApiManager) {
        this.idManagerService = idManagerService;
        this.mdsalApiManager = mdsalApiManager;
    }

    public KeyedLocks<String> getTunnelLocks() {
        return tunnelLocks;
    }

    public BigInteger getDpnId(DatapathId datapathId) {
        if (datapathId != null) {
            String dpIdStr = datapathId.getValue().replace(":", "");
            return new BigInteger(dpIdStr, 16);
        }
        return null;
    }

    public static BigInteger getDpnFromNodeConnectorId(NodeConnectorId portId) {
        /*
         * NodeConnectorId is of form 'openflow:dpnid:portnum'
         */
        return new BigInteger(portId.getValue().split(ITMConstants.OF_URI_SEPARATOR)[1]);
    }

    public static long getPortNumberFromNodeConnectorId(NodeConnectorId portId) {
        String portNo = getPortNoFromNodeConnectorId(portId);
        try {
            return Long.parseLong(portNo);
        } catch (NumberFormatException ex) {
            LOG.error("Unable to retrieve port number from nodeconnector id for {} ", portId, ex);
            return ITMConstants.INVALID_PORT_NO;
        }
    }

    private static String getPortNoFromNodeConnectorId(NodeConnectorId portId) {
        /*
         * NodeConnectorId is of form 'openflow:dpnid:portnum'
         */
        return portId.getValue().split(ITMConstants.OF_URI_SEPARATOR)[2];
    }

    // Convert Interface Oper State to Tunnel Oper state
    public static TunnelOperStatus convertInterfaceToTunnelOperState(Interface.OperStatus opState) {

        java.util.Optional<TunnelOperStatus> tunnelOperStatus = TunnelOperStatus.forName(opState.getName());
        if (tunnelOperStatus.isPresent()) {
            return tunnelOperStatus.get();
        }
        return TunnelOperStatus.Ignore;
    }

    public static InstanceIdentifier<OvsBridgeTunnelEntry> getBridgeTunnelEntryIdentifier(
            OvsBridgeEntryKey bridgeEntryKey, OvsBridgeTunnelEntryKey bridgeInterfaceEntryKey) {
        return InstanceIdentifier.builder(BridgeTunnelInfo.class)
                .child(OvsBridgeEntry.class, bridgeEntryKey)
                .child(OvsBridgeTunnelEntry.class, bridgeInterfaceEntryKey).build();

    }

    public static InstanceIdentifier<OvsBridgeRefEntry>
        getOvsBridgeRefEntryIdentifier(OvsBridgeRefEntryKey bridgeRefEntryKey) {
        return InstanceIdentifier.builder(OvsBridgeRefInfo.class)
                .child(OvsBridgeRefEntry.class, bridgeRefEntryKey).build();
    }

    public static InstanceIdentifier<DpnsTeps> createDpnTepsInstanceIdentifier(BigInteger sourceDpnId) {
        return InstanceIdentifier.builder(DpnTepsState.class).child(DpnsTeps.class,
            new DpnsTepsKey(sourceDpnId)).build();
    }

    public static InstanceIdentifier<OvsBridgeEntry> getOvsBridgeEntryIdentifier(OvsBridgeEntryKey bridgeEntryKey) {
        return InstanceIdentifier.builder(BridgeTunnelInfo.class).child(OvsBridgeEntry.class, bridgeEntryKey).build();
    }

    public static InstanceIdentifier<TerminationPoint> createTerminationPointInstanceIdentifier(
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021
                    .network.topology.topology.NodeKey nodekey, String portName) {
        InstanceIdentifier<TerminationPoint> terminationPointPath = InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(OVSDB_TOPOLOGY_ID))
                .child(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021
                        .network.topology.topology.Node.class, nodekey)
                .child(TerminationPoint.class, new TerminationPointKey(new TpId(portName)));

        LOG.debug("Termination point InstanceIdentifier generated : {}", terminationPointPath);
        return terminationPointPath;
    }

    public static List<InterfaceBfd> getBfdParams(IfTunnel ifTunnel) {
        List<InterfaceBfd> bfdParams = new ArrayList<>();
        bfdParams.add(getIfBfdObj(BFD_PARAM_ENABLE,ifTunnel != null ? ifTunnel.isMonitorEnabled().toString()
                : "false"));
        bfdParams.add(getIfBfdObj(BFD_PARAM_MIN_TX, ifTunnel != null &&  ifTunnel.getMonitorInterval() != null
                ? ifTunnel.getMonitorInterval().toString() : BFD_MIN_TX_VAL));
        bfdParams.add(getIfBfdObj(BFD_PARAM_FORWARDING_IF_RX, BFD_FORWARDING_IF_RX_VAL));
        return bfdParams;
    }

    public List<InterfaceBfd> getBfdParams(RemoteDpns remoteDpn) {
        List<InterfaceBfd> bfdParams = new ArrayList<>();
        bfdParams.add(getIfBfdObj(BFD_PARAM_ENABLE, remoteDpn != null ? remoteDpn.isMonitoringEnabled().toString()
            : "false"));
        bfdParams.add(getIfBfdObj(BFD_PARAM_MIN_TX, remoteDpn != null && remoteDpn.getMonitoringInterval() != null
            ? remoteDpn.getMonitoringInterval().toString() : BFD_MIN_TX_VAL));
        bfdParams.add(getIfBfdObj(BFD_PARAM_FORWARDING_IF_RX, BFD_FORWARDING_IF_RX_VAL));
        LOG.debug("getBfdParams {}", bfdParams);
        return bfdParams;
    }

    private static InterfaceBfd getIfBfdObj(String key, String value) {
        InterfaceBfdBuilder bfdBuilder = new InterfaceBfdBuilder();
        bfdBuilder.setBfdKey(key).withKey(new InterfaceBfdKey(key)).setBfdValue(value);
        return bfdBuilder.build();
    }

    public static boolean bfdMonitoringEnabled(List<InterfaceBfd> interfaceBfds) {
        if (interfaceBfds != null && !interfaceBfds.isEmpty()) {
            for (InterfaceBfd interfaceBfd : interfaceBfds) {
                if (BFD_ENABLE_KEY.equalsIgnoreCase(interfaceBfd.getBfdKey())) {
                    return BFD_ENABLE_VALUE.equalsIgnoreCase(interfaceBfd.getBfdValue());
                }
            }
        }
        return false;
    }

    public static boolean changeInBfdMonitoringDetected(OvsdbTerminationPointAugmentation tpOld,
                                                        OvsdbTerminationPointAugmentation tpNew) {
        return tpOld != null
                && bfdMonitoringEnabled(tpNew.getInterfaceBfd()) != bfdMonitoringEnabled(tpOld.getInterfaceBfd());
    }

    public static boolean ifBfdStatusNotEqual(OvsdbTerminationPointAugmentation tpOld,
                                              OvsdbTerminationPointAugmentation tpNew) {
        return tpNew.getInterfaceBfdStatus() != null
                && (tpOld == null || !tpNew.getInterfaceBfdStatus().equals(tpOld.getInterfaceBfdStatus()));
    }

    public int allocateId(String poolName, String idKey)
            throws InterruptedException, ExecutionException , OperationFailedException {
        AllocateIdInput getIdInput = new AllocateIdInputBuilder().setPoolName(poolName).setIdKey(idKey).build();
        RpcResult<AllocateIdOutput> rpcResult = idManagerService.allocateId(getIdInput).get();
        if (rpcResult.isSuccessful()) {
            return rpcResult.getResult().getIdValue().intValue();
        } else {
            Optional<RpcError> rpcError = rpcResult.getErrors().stream().findFirst();
            String msg = String.format("RPC Call to Get Unique Id returned with Errors for the key %s", idKey);
            if (rpcError.isPresent()) {
                throw new OperationFailedException(msg, rpcError.get());
            }
            else {
                throw new OperationFailedException(msg);
            }
        }
    }

    public static void createBridgeTunnelEntryInConfigDS(BigInteger dpId, String childInterface) {
        OvsBridgeEntryKey bridgeEntryKey = new OvsBridgeEntryKey(dpId);
        OvsBridgeTunnelEntryKey bridgeTunnelEntryKey = new OvsBridgeTunnelEntryKey(childInterface);
        InstanceIdentifier<OvsBridgeTunnelEntry> bridgeTunnelEntryIid =
                getBridgeTunnelEntryIdentifier(bridgeEntryKey, bridgeTunnelEntryKey);
        OvsBridgeTunnelEntryBuilder entryBuilder = new OvsBridgeTunnelEntryBuilder().withKey(bridgeTunnelEntryKey)
                .setTunnelName(childInterface);
        ITMBatchingUtils.write(bridgeTunnelEntryIid, entryBuilder.build(), ITMBatchingUtils.EntityType.DEFAULT_CONFIG);
    }

    public void addTunnelIngressFlow(TypedWriteTransaction<Configuration> tx, BigInteger dpnId, long portNo,
        String interfaceName, int ifIndex) {
        LOG.debug("Adding tunnel ingress flow for {}", interfaceName);
        List<MatchInfoBase> matches = new ArrayList<>();

        List<InstructionInfo> mkInstructions = new ArrayList<>();
        matches.add(new MatchInPort(dpnId, portNo));
        mkInstructions.add(new InstructionWriteMetadata(MetaDataUtil.getLportTagMetaData(ifIndex)
            .or(BigInteger.ONE), MetaDataUtil.METADATA_MASK_LPORT_TAG_SH_FLAG));
        short tableId = NwConstants.INTERNAL_TUNNEL_TABLE;
        mkInstructions.add(new InstructionGotoTable(tableId));

        String flowRef =
            getTunnelInterfaceFlowRef(dpnId, NwConstants.VLAN_INTERFACE_INGRESS_TABLE, interfaceName);

        FlowEntity flowEntity = MDSALUtil.buildFlowEntity(dpnId, NwConstants.VLAN_INTERFACE_INGRESS_TABLE, flowRef,
            ITMConstants.DEFAULT_FLOW_PRIORITY, interfaceName, 0, 0, NwConstants.COOKIE_VM_INGRESS_TABLE, matches,
            mkInstructions);
        mdsalApiManager.addFlow(tx, flowEntity);
    }

    public void removeTunnelIngressFlow(TypedReadWriteTransaction<Configuration> tx, BigInteger dpnId,
        String interfaceName) throws ExecutionException, InterruptedException {
        LOG.debug("Removing tunnel ingress flow for {}", interfaceName);
        String flowRef =
            getTunnelInterfaceFlowRef(dpnId, NwConstants.VLAN_INTERFACE_INGRESS_TABLE, interfaceName);

        mdsalApiManager.removeFlow(tx, dpnId, flowRef, NwConstants.VLAN_INTERFACE_INGRESS_TABLE);
    }

    private String getTunnelInterfaceFlowRef(BigInteger dpnId, short tableId, String ifName) {
        return String.valueOf(dpnId) + tableId + ifName;
    }

    public void addTunnelPortToBridge(IfTunnel ifTunnel, InstanceIdentifier<?> bridgeIid,
                                      org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
                                              .interfaces.rev140508.interfaces.Interface iface, String portName) {
        LOG.debug("adding tunnel port {} to bridge {}", portName, bridgeIid);

        Class<? extends InterfaceTypeBase> type =
                DirectTunnelUtils.TUNNEL_TYPE_MAP.get(ifTunnel.getTunnelInterfaceType());
        if (type == null) {
            LOG.warn("Unknown Tunnel Type obtained while creating interface: {}", iface);
            return;
        }

        int vlanId = 0;
        IfL2vlan ifL2vlan = iface.augmentation(IfL2vlan.class);
        if (ifL2vlan != null && ifL2vlan.getVlanId() != null) {
            vlanId = ifL2vlan.getVlanId().getValue();
        }

        Builder<String, String> options = new ImmutableMap.Builder<>();

        // Options common to any kind of tunnel
        options.put(TUNNEL_OPTIONS_KEY, TUNNEL_OPTIONS_VALUE_FLOW);
        if (ifTunnel.isTunnelSourceIpFlow()) {
            options.put(TUNNEL_OPTIONS_LOCAL_IP, TUNNEL_OPTIONS_VALUE_FLOW);
        } else {
            IpAddress localIp = ifTunnel.getTunnelSource();
            options.put(TUNNEL_OPTIONS_LOCAL_IP, localIp.getIpv4Address().getValue());
        }
        if (ifTunnel.isTunnelRemoteIpFlow()) {
            options.put(TUNNEL_OPTIONS_REMOTE_IP, TUNNEL_OPTIONS_VALUE_FLOW);
        } else {
            IpAddress remoteIp = ifTunnel.getTunnelDestination();
            options.put(TUNNEL_OPTIONS_REMOTE_IP, remoteIp.getIpv4Address().getValue());
        }
        options.put(DirectTunnelUtils.TUNNEL_OPTIONS_TOS, DirectTunnelUtils.TUNNEL_OPTIONS_TOS_VALUE_INHERIT);

        // Specific options for each type of tunnel
        if (TunnelTypeVxlanGpe.class.equals(ifTunnel.getTunnelInterfaceType())) {
            options.put(DirectTunnelUtils.TUNNEL_OPTIONS_EXTS, DirectTunnelUtils.TUNNEL_OPTIONS_VALUE_GPE);
            options.put(DirectTunnelUtils.TUNNEL_OPTIONS_NSI, DirectTunnelUtils.TUNNEL_OPTIONS_VALUE_FLOW);
            options.put(DirectTunnelUtils.TUNNEL_OPTIONS_NSP, DirectTunnelUtils.TUNNEL_OPTIONS_VALUE_FLOW);
            options.put(DirectTunnelUtils.TUNNEL_OPTIONS_NSHC1, DirectTunnelUtils.TUNNEL_OPTIONS_VALUE_FLOW);
            options.put(DirectTunnelUtils.TUNNEL_OPTIONS_NSHC2, DirectTunnelUtils.TUNNEL_OPTIONS_VALUE_FLOW);
            options.put(DirectTunnelUtils.TUNNEL_OPTIONS_NSHC3, DirectTunnelUtils.TUNNEL_OPTIONS_VALUE_FLOW);
            options.put(DirectTunnelUtils.TUNNEL_OPTIONS_NSHC4, DirectTunnelUtils.TUNNEL_OPTIONS_VALUE_FLOW);
            // VxLAN-GPE interfaces will not use the default UDP port to avoid problems with other meshes
            options.put(DirectTunnelUtils.TUNNEL_OPTIONS_DESTINATION_PORT,
                    DirectTunnelUtils.TUNNEL_OPTIONS_VALUE_GPE_DESTINATION_PORT);
        }
        addTerminationPoint(bridgeIid, portName, vlanId, type, options.build(), ifTunnel);
    }

    private void addTerminationPoint(InstanceIdentifier<?> bridgeIid, String portName, int vlanId,
                                     Class<? extends InterfaceTypeBase> type, Map<String, String> options,
                                     IfTunnel ifTunnel) {
        final InstanceIdentifier<TerminationPoint> tpIid = DirectTunnelUtils.createTerminationPointInstanceIdentifier(
                InstanceIdentifier.keyOf(bridgeIid.firstIdentifierOf(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang
                                .network.topology.rev131021.network.topology.topology.Node.class)), portName);
        OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();

        tpAugmentationBuilder.setName(portName);

        if (type != null) {
            tpAugmentationBuilder.setInterfaceType(type);
        }

        if (options != null) {
            List<Options> optionsList = new ArrayList<>();
            for (Map.Entry<String, String> entry : options.entrySet()) {
                OptionsBuilder optionsBuilder = new OptionsBuilder();
                optionsBuilder.withKey(new OptionsKey(entry.getKey()));
                optionsBuilder.setOption(entry.getKey());
                optionsBuilder.setValue(entry.getValue());
                optionsList.add(optionsBuilder.build());
            }
            tpAugmentationBuilder.setOptions(optionsList);
        }

        if (vlanId != 0) {
            tpAugmentationBuilder.setVlanMode(OvsdbPortInterfaceAttributes.VlanMode.Access);
            tpAugmentationBuilder.setVlanTag(new VlanId(vlanId));
        }

        if (ifTunnel.isMonitorEnabled()
                && TunnelMonitoringTypeBfd.class.isAssignableFrom(ifTunnel.getMonitorProtocol())) { //checkBfdMonEnabled
            if (isOfTunnel(ifTunnel)) {
                LOG.warn("BFD Monitoring not supported for OFTunnels");
            } else {
                List<InterfaceBfd> bfdParams = DirectTunnelUtils.getBfdParams(ifTunnel);
                tpAugmentationBuilder.setInterfaceBfd(bfdParams);
            }
        }

        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.withKey(InstanceIdentifier.keyOf(tpIid));
        tpBuilder.addAugmentation(OvsdbTerminationPointAugmentation.class, tpAugmentationBuilder.build());

        ITMBatchingUtils.write(tpIid, tpBuilder.build(), ITMBatchingUtils.EntityType.TOPOLOGY_CONFIG);
    }

    public void removeLportTagInterfaceMap(String infName)
            throws ExecutionException, InterruptedException, OperationFailedException {
        // workaround to get the id to remove from lport tag interface map
        Integer ifIndex = allocateId(IfmConstants.IFM_IDPOOL_NAME, infName);
        releaseId(IfmConstants.IFM_IDPOOL_NAME, infName);
        LOG.debug("removing lport tag to interface map for {}", infName);
        InstanceIdentifier<IfIndexTunnel> id = InstanceIdentifier.builder(IfIndexesTunnelMap.class)
                .child(IfIndexTunnel.class, new IfIndexTunnelKey(ifIndex)).build();
        ITMBatchingUtils.delete(id, ITMBatchingUtils.EntityType.DEFAULT_OPERATIONAL);
    }

    private void releaseId(String poolName, String idKey) throws InterruptedException, ExecutionException,
            OperationFailedException {
        ReleaseIdInput idInput = new ReleaseIdInputBuilder().setPoolName(poolName).setIdKey(idKey).build();
        ListenableFuture<RpcResult<ReleaseIdOutput>> result = idManagerService.releaseId(idInput);
        RpcResult<ReleaseIdOutput> rpcResult = result.get();
        if (!rpcResult.isSuccessful()) {
            LOG.error("RPC Call to release Id with Key {} returned with Errors {}", idKey, rpcResult.getErrors());
            Optional<RpcError> rpcError = rpcResult.getErrors().stream().findFirst();
            String msg = String.format("RPC Call to release Id returned with Errors for the key %s", idKey);
            if (rpcError.isPresent()) {
                throw new OperationFailedException(msg, rpcError.get());
            }
            else {
                throw new OperationFailedException(msg);
            }
        }
    }

    public void deleteTunnelStateEntry(String interfaceName) {
        LOG.debug(" deleteTunnelStateEntry tunnels state for {}", interfaceName);
        InstanceIdentifier<StateTunnelList> stateTnlId =
                ItmUtils.buildStateTunnelListId(new StateTunnelListKey(interfaceName));
        ITMBatchingUtils.delete(stateTnlId, ITMBatchingUtils.EntityType.DEFAULT_OPERATIONAL);
    }

    public void updateBfdConfiguration(BigInteger srcDpnId, RemoteDpns remoteDpn,
                                       @Nonnull com.google.common.base.Optional<OvsBridgeRefEntry> ovsBridgeRefEntry) {
        if (ovsBridgeRefEntry.isPresent()) {
            LOG.debug("creating bridge interface on dpn {}", srcDpnId);
            InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIid =
                (InstanceIdentifier<OvsdbBridgeAugmentation>) ovsBridgeRefEntry.get()
                    .getOvsBridgeReference().getValue();
            updateBfdParamtersForTerminationPoint(bridgeIid, remoteDpn);
        }
    }

    public void updateBfdParamtersForTerminationPoint(InstanceIdentifier<?> bridgeIid, RemoteDpns remoteDpn) {
        final InstanceIdentifier<TerminationPoint> tpIid = createTerminationPointInstanceIdentifier(
            InstanceIdentifier.keyOf(bridgeIid.firstIdentifierOf(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang
                    .network.topology.rev131021.network.topology.topology.Node.class)), remoteDpn.getTunnelName());
        OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();
        List<InterfaceBfd> bfdParams = getBfdParams(remoteDpn);
        tpAugmentationBuilder.setInterfaceBfd(bfdParams);
        LOG.debug("OvsdbTerminationPointAugmentation: {}", tpAugmentationBuilder);
        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.withKey(InstanceIdentifier.keyOf(tpIid));
        tpBuilder.addAugmentation(OvsdbTerminationPointAugmentation.class, tpAugmentationBuilder.build());
        ITMBatchingUtils.update(tpIid, tpBuilder.build(), ITMBatchingUtils.EntityType.TOPOLOGY_CONFIG);
    }

    public static boolean isOfTunnel(IfTunnel ifTunnel) {
        return Boolean.TRUE.equals(ifTunnel.isTunnelRemoteIpFlow())
                || Boolean.TRUE.equals(ifTunnel.isTunnelSourceIpFlow());
    }
}


