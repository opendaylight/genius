/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.interfacemanager.globals.IfmConstants;
import org.opendaylight.genius.itm.cache.DpnTepStateCache;
import org.opendaylight.genius.itm.cache.TunnelStateCache;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ITMBatchingUtils;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.utils.DpnTepInterfaceInfo;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.FlowEntity;
import org.opendaylight.genius.mdsalutil.InstructionInfo;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.MatchInfoBase;
import org.opendaylight.genius.mdsalutil.MetaDataUtil;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.mdsalutil.actions.ActionDrop;
import org.opendaylight.genius.mdsalutil.actions.ActionOutput;
import org.opendaylight.genius.mdsalutil.actions.ActionSetFieldTunnelId;
import org.opendaylight.genius.mdsalutil.instructions.InstructionGotoTable;
import org.opendaylight.genius.mdsalutil.instructions.InstructionWriteMetadata;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.genius.mdsalutil.matches.MatchInPort;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.ReleaseIdInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.ReleaseIdInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBfd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeMplsOverGre;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.ovs.bridge.ref.info.OvsBridgeRefEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.ovs.bridge.ref.info.OvsBridgeRefEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelOperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.dpns.teps.RemoteDpns;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
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
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class DirectTunnelUtils {

    private static final Logger LOG = LoggerFactory.getLogger(DirectTunnelUtils.class);
    private static final String ENTITY = "ovsBridgeRefEntryOperation";

    // BFD parameters
    private static final String BFD_PARAM_ENABLE = "enable";
    private static final String BFD_PARAM_MIN_TX = "min_tx";
    private static final String BFD_PARAM_FORWARDING_IF_RX = "forwarding_if_rx";
    private static final String BFD_ENABLE_KEY = "enable";
    private static final String BFD_ENABLE_VALUE = "true";
    public static final String BFD_OP_STATE = "state";
    public static final String BFD_STATE_UP = "up";
    private static final String BFD_MIN_TX_VAL = "100";
    private static final String BFD_FORWARDING_IF_RX_VAL = "true";

    // Tunnel options
    private static final String TUNNEL_OPTIONS_KEY = "key";
    private static final String TUNNEL_OPTIONS_LOCAL_IP = "local_ip";
    private static final String TUNNEL_OPTIONS_REMOTE_IP = "remote_ip";
    private static final String TUNNEL_OPTIONS_DESTINATION_PORT = "dst_port";
    private static final String TUNNEL_OPTIONS_TOS = "tos";

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
    private static final String TUNNEL_OPTIONS_TOS_VALUE_INHERIT = "inherit";

    private static final TopologyId OVSDB_TOPOLOGY_ID = new TopologyId(new Uri("ovsdb:1"));

    private static final int INVALID_ID = 0;

    // To keep the mapping between Tunnel Types and Tunnel Interfaces
    private static final Map<Class<? extends TunnelTypeBase>, Class<? extends InterfaceTypeBase>> TUNNEL_TYPE_MAP
            = new HashMap<Class<? extends TunnelTypeBase>, Class<? extends InterfaceTypeBase>>() {{
                    put(TunnelTypeGre.class, InterfaceTypeGre.class);
                    put(TunnelTypeMplsOverGre.class, InterfaceTypeGre.class);
                    put(TunnelTypeVxlan.class, InterfaceTypeVxlan.class);
                    put(TunnelTypeVxlanGpe.class, InterfaceTypeVxlan.class);
                }
            };

    private static final String TUNNEL_PORT_REGEX = "tun[0-9a-f]{11}";
    private static final Pattern TUNNEL_PORT_PATTERN = Pattern.compile(TUNNEL_PORT_REGEX);
    public  static final Predicate<String> TUNNEL_PORT_PREDICATE
            = portName -> TUNNEL_PORT_PATTERN.matcher(portName).matches();

    private final DataBroker dataBroker;
    private final JobCoordinator jobCoordinator;
    private final IdManagerService idManager;
    private final IMdsalApiManager mdsalApiManager;
    private final ManagedNewTransactionRunner txRunner;
    private final DpnTepStateCache dpnTepStateCache;
    private final TunnelStateCache tunnelStateCache;

    @Inject
    public DirectTunnelUtils(DataBroker dataBroker, JobCoordinator jobCoordinator, IdManagerService idManager,
                             IMdsalApiManager mdsalApiManager,
                             DpnTepStateCache dpnTepStateCache, TunnelStateCache tunnelStateCache) {
        this.dataBroker = dataBroker;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.jobCoordinator = jobCoordinator;
        this.idManager = idManager;
        this.mdsalApiManager = mdsalApiManager;
        this.dpnTepStateCache = dpnTepStateCache;
        this.tunnelStateCache = tunnelStateCache;
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

    public static NodeId getNodeIdFromNodeConnectorId(NodeConnectorId ncId) {
        return new NodeId(ncId.getValue().substring(0,ncId.getValue().lastIndexOf(":")));
    }

    // Convert Interface Oper State to Tunnel Oper state
    public static TunnelOperStatus convertInterfaceToTunnelOperState(Interface.OperStatus opState) {

        java.util.Optional<TunnelOperStatus> tunnelOperStatus = TunnelOperStatus.forName(opState.getName());
        if (tunnelOperStatus.isPresent()) {
            return tunnelOperStatus.get();
        }
        return TunnelOperStatus.Ignore;
    }

    public void createOvsBridgeRefEntry(BigInteger dpnId, InstanceIdentifier<?> bridgeIid) {
        LOG.debug("Creating bridge ref entry for dpn: {} bridge: {}", dpnId, bridgeIid);
        OvsBridgeRefEntryKey bridgeRefEntryKey = new OvsBridgeRefEntryKey(dpnId);
        InstanceIdentifier<OvsBridgeRefEntry> bridgeEntryId = getOvsBridgeRefEntryIdentifier(bridgeRefEntryKey);
        OvsBridgeRefEntryBuilder tunnelDpnBridgeEntryBuilder = new OvsBridgeRefEntryBuilder().setKey(bridgeRefEntryKey)
                .setDpid(dpnId).setOvsBridgeReference(new OvsdbBridgeRef(bridgeIid));
        jobCoordinator.enqueueJob(ENTITY,
            () -> Collections.singletonList(txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx
                -> tx.put(LogicalDatastoreType.OPERATIONAL, bridgeEntryId, tunnelDpnBridgeEntryBuilder.build(),
                    true))));

    }

    public void deleteOvsBridgeRefEntry(BigInteger dpnId) {
        LOG.debug("Deleting bridge ref entry for dpn: {}", dpnId);
        OvsBridgeRefEntryKey bridgeRefEntryKey = new OvsBridgeRefEntryKey(dpnId);
        InstanceIdentifier<OvsBridgeRefEntry> bridgeEntryId = getOvsBridgeRefEntryIdentifier(bridgeRefEntryKey);
        jobCoordinator.enqueueJob(ENTITY,
            () -> Collections.singletonList(txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx
                -> tx.delete(LogicalDatastoreType.OPERATIONAL, bridgeEntryId))));
    }

    public static InstanceIdentifier<OvsBridgeTunnelEntry>
        getBridgeTunnelEntryIdentifier(OvsBridgeEntryKey bridgeEntryKey,
                                       OvsBridgeTunnelEntryKey bridgeInterfaceEntryKey) {
        return InstanceIdentifier.builder(BridgeTunnelInfo.class).child(OvsBridgeEntry.class, bridgeEntryKey)
                .child(OvsBridgeTunnelEntry.class, bridgeInterfaceEntryKey).build();

    }

    public static void createBridgeTunnelEntryInConfigDS(BigInteger dpId, String childInterface) {
        OvsBridgeEntryKey bridgeEntryKey = new OvsBridgeEntryKey(dpId);
        OvsBridgeTunnelEntryKey bridgeTunnelEntryKey = new OvsBridgeTunnelEntryKey(childInterface);
        InstanceIdentifier<OvsBridgeTunnelEntry> bridgeTunnelEntryIid =
                getBridgeTunnelEntryIdentifier(bridgeEntryKey, bridgeTunnelEntryKey);
        OvsBridgeTunnelEntryBuilder entryBuilder = new OvsBridgeTunnelEntryBuilder().setKey(bridgeTunnelEntryKey)
                .setTunnelName(childInterface);
        ITMBatchingUtils.write(bridgeTunnelEntryIid, entryBuilder.build(), ITMBatchingUtils.EntityType.DEFAULT_CONFIG);
    }

    public static InstanceIdentifier<OvsBridgeRefEntry>
        getOvsBridgeRefEntryIdentifier(OvsBridgeRefEntryKey bridgeRefEntryKey) {
        return InstanceIdentifier.builder(OvsBridgeRefInfo.class)
                .child(OvsBridgeRefEntry.class, bridgeRefEntryKey).build();
    }

    public static InstanceIdentifier<OvsBridgeEntry> getOvsBridgeEntryIdentifier(OvsBridgeEntryKey bridgeEntryKey) {
        return InstanceIdentifier.builder(BridgeTunnelInfo.class).child(OvsBridgeEntry.class, bridgeEntryKey).build();
    }

    public boolean isNodeConnectorPresent(NodeConnectorId nodeConnectorId) throws ReadFailedException {
        NodeId nodeId = getNodeIdFromNodeConnectorId(nodeConnectorId);
        InstanceIdentifier<NodeConnector> ncIdentifier = InstanceIdentifier.builder(Nodes.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class,
                        new NodeKey(nodeId))
                .child(NodeConnector.class, new NodeConnectorKey(nodeConnectorId)).build();
        return SingleTransactionDataBroker
                .syncReadOptional(dataBroker, LogicalDatastoreType.OPERATIONAL, ncIdentifier).isPresent();
    }

    public boolean isNodePresent(NodeConnectorId nodeConnectorId) throws ReadFailedException {
        NodeId nodeID = getNodeIdFromNodeConnectorId(nodeConnectorId);
        InstanceIdentifier<Node> nodeInstanceIdentifier = InstanceIdentifier.builder(Nodes.class).child(Node.class,
                new NodeKey(nodeID)).build();
        return SingleTransactionDataBroker
                .syncReadOptional(dataBroker, LogicalDatastoreType.OPERATIONAL, nodeInstanceIdentifier).isPresent();
    }

    public List<Action> getEgressActionsForInterface(String interfaceName, long tunnelKey, Integer actionKey) {
        List<ActionInfo> listActionInfo = getEgressActionInfosForInterface(interfaceName, tunnelKey,
                actionKey == null ? 0 : actionKey);
        return listActionInfo.stream().map(ActionInfo::buildAction).collect(Collectors.toList());
    }

    private List<ActionInfo> getEgressActionInfosForInterface(String interfaceName, long tunnelKey,
                                                              int actionKeyStart) {
        DpnTepInterfaceInfo interfaceInfo = dpnTepStateCache.getTunnelFromCache(interfaceName);
        if (interfaceInfo == null) {
            LOG.error("Interface information not present in config DS for {}", interfaceName);
            return Collections.singletonList(new ActionDrop());
        }
        Optional<StateTunnelList> ifState;
        try {
            ifState = tunnelStateCache.get(tunnelStateCache.getStateTunnelListIdentifier(interfaceName));
        } catch (ReadFailedException e) {
            LOG.error("Interface information not present in oper DS for {} ", interfaceName, e);
            return Collections.singletonList(new ActionDrop());
        }
        if (ifState.isPresent()) {
            String tunnelType = ItmUtils.convertTunnelTypetoString(interfaceInfo.getTunnelType());
            return getEgressActionInfosForInterface(tunnelType, ifState.get().getPortNumber(), tunnelKey,
                    actionKeyStart);
        }
        LOG.error("Interface information not present in oper DS for {}", interfaceName);
        return Collections.singletonList(new ActionDrop());
    }


    private static List<ActionInfo> getEgressActionInfosForInterface(String tunnelType, String portNo, Long tunnelKey,
                                                                    int actionKeyStart) {
        List<ActionInfo> result = new ArrayList<>();
        switch (tunnelType) {
            case ITMConstants.TUNNEL_TYPE_GRE:
            case ITMConstants.TUNNEL_TYPE_MPLSoGRE:
                // Invoke IFM RPC and pass it on to the caller.
                LOG.warn("Interface Type {} not handled by ITM", tunnelType);
                break;
            case ITMConstants.TUNNEL_TYPE_VXLAN:
                //TODO tunnel_id to encode GRE key, once it is supported
                // Until then, tunnel_id should be "cleaned", otherwise it stores the value coming from a VXLAN tunnel
                result.add(new ActionSetFieldTunnelId(actionKeyStart++,
                        BigInteger.valueOf(tunnelKey != null ? tunnelKey : 0L)));
                result.add(new ActionOutput(actionKeyStart, new Uri(portNo)));
                break;

            default:
                LOG.warn("Interface Type {} not handled yet", tunnelType);
                break;
        }
        return result;
    }

    public void addTunnelPortToBridge(IfTunnel ifTunnel, InstanceIdentifier<?> bridgeIid,
                                      org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
                                              .interfaces.rev140508.interfaces.Interface iface, String portName) {
        LOG.debug("adding tunnel port {} to bridge {}", portName, bridgeIid);

        Class<? extends InterfaceTypeBase> type = TUNNEL_TYPE_MAP.get(ifTunnel.getTunnelInterfaceType());

        if (type == null) {
            LOG.warn("Unknown Tunnel Type obtained while creating interface: {}", iface);
            return;
        }

        int vlanId = 0;
        IfL2vlan ifL2vlan = iface.getAugmentation(IfL2vlan.class);
        if (ifL2vlan != null && ifL2vlan.getVlanId() != null) {
            vlanId = ifL2vlan.getVlanId().getValue();
        }

        Map<String, String> options = Maps.newHashMap();

        // Options common to any kind of tunnel
        IpAddress localIp = ifTunnel.getTunnelSource();
        options.put(TUNNEL_OPTIONS_LOCAL_IP, localIp.getIpv4Address().getValue());

        IpAddress remoteIp = ifTunnel.getTunnelDestination();
        options.put(TUNNEL_OPTIONS_REMOTE_IP, remoteIp.getIpv4Address().getValue());

        options.put(TUNNEL_OPTIONS_TOS, TUNNEL_OPTIONS_TOS_VALUE_INHERIT);

        // Specific options for each type of tunnel
        if (!ifTunnel.getTunnelInterfaceType().equals(TunnelTypeMplsOverGre.class)) {
            options.put(TUNNEL_OPTIONS_KEY, TUNNEL_OPTIONS_VALUE_FLOW);
        }

        if (ifTunnel.getTunnelInterfaceType().equals(TunnelTypeVxlanGpe.class)) {
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
        addTerminationPoint(bridgeIid, portName, vlanId, type, options, ifTunnel);
    }

    private void addTerminationPoint(InstanceIdentifier<?> bridgeIid, String portName, int vlanId,
                                     Class<? extends InterfaceTypeBase> type, Map<String, String> options,
                                     IfTunnel ifTunnel) {
        final InstanceIdentifier<TerminationPoint> tpIid = createTerminationPointInstanceIdentifier(
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
                optionsBuilder.setKey(new OptionsKey(entry.getKey()));
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
            List<InterfaceBfd> bfdParams = getBfdParams(ifTunnel);
            tpAugmentationBuilder.setInterfaceBfd(bfdParams);
        }

        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.setKey(InstanceIdentifier.keyOf(tpIid));
        tpBuilder.addAugmentation(OvsdbTerminationPointAugmentation.class, tpAugmentationBuilder.build());

        ITMBatchingUtils.write(tpIid, tpBuilder.build(), ITMBatchingUtils.EntityType.TOPOLOGY_CONFIG);
    }

    public InstanceIdentifier<TerminationPoint> createTerminationPointInstanceIdentifier(
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

    public boolean bfdMonitoringEnabled(IfTunnel ifTunnel) {
        if (ifTunnel.isMonitorEnabled()
                && TunnelMonitoringTypeBfd.class.isAssignableFrom(ifTunnel.getMonitorProtocol())) {
            return true;
        }
        return false;
    }

    public boolean bfdMonitoringEnabled(RemoteDpns remoteDpn) {
        return (remoteDpn.isMonitoringEnabled()) ? true : false;
    }

    public boolean bfdMonitoringEnabled(List<InterfaceBfd> interfaceBfds) {
        if (interfaceBfds != null && !interfaceBfds.isEmpty()) {
            for (InterfaceBfd interfaceBfd : interfaceBfds) {
                if (interfaceBfd.getBfdKey().equalsIgnoreCase(BFD_ENABLE_KEY)) {
                    String bfdEnable = interfaceBfd.getBfdValue();
                    return BFD_ENABLE_VALUE.equalsIgnoreCase(bfdEnable);
                }
            }
        }
        return false;
    }

    private List<InterfaceBfd> getBfdParams(IfTunnel ifTunnel) {
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
        return bfdParams;
    }

    private InterfaceBfd getIfBfdObj(String key, String value) {
        InterfaceBfdBuilder bfdBuilder = new InterfaceBfdBuilder();
        bfdBuilder.setBfdKey(key).setKey(new InterfaceBfdKey(key)).setBfdValue(value);
        return bfdBuilder.build();
    }

    public Integer allocateId(String poolName, String idKey) {
        AllocateIdInput getIdInput = new AllocateIdInputBuilder().setPoolName(poolName).setIdKey(idKey).build();
        try {
            Future<RpcResult<AllocateIdOutput>> result = idManager.allocateId(getIdInput);
            RpcResult<AllocateIdOutput> rpcResult = result.get();
            if (rpcResult.isSuccessful()) {
                return rpcResult.getResult().getIdValue().intValue();
            } else {
                LOG.warn("RPC Call to Get Unique Id returned with Errors {}", rpcResult.getErrors());
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Exception when getting Unique Id",e);
        }
        return INVALID_ID;
    }

    private void releaseId(String poolName, String idKey) {
        ReleaseIdInput idInput = new ReleaseIdInputBuilder().setPoolName(poolName).setIdKey(idKey).build();
        try {
            Future<RpcResult<Void>> result = idManager.releaseId(idInput);
            RpcResult<Void> rpcResult = result.get();
            if (!rpcResult.isSuccessful()) {
                LOG.warn("RPC Call to release Id with Key {} returned with Errors {}",
                        idKey, rpcResult.getErrors());
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Exception when releasing Id for key {}", idKey, e);
        }
    }

    public void makeTunnelIngressFlow(DpnTepInterfaceInfo dpnTepConfigInfo, BigInteger dpnId, long portNo,
                                      String interfaceName, int ifIndex, int addOrRemoveFlow) {
        LOG.debug("make tunnel ingress flow for {}", interfaceName);
        String flowRef =
                getTunnelInterfaceFlowRef(dpnId, NwConstants.VLAN_INTERFACE_INGRESS_TABLE, interfaceName);
        List<MatchInfoBase> matches = new ArrayList<>();

        List<InstructionInfo> mkInstructions = new ArrayList<>();
        if (NwConstants.ADD_FLOW == addOrRemoveFlow) {
            matches.add(new MatchInPort(dpnId, portNo));
            mkInstructions.add(new InstructionWriteMetadata(MetaDataUtil.getElanTagMetadata(ifIndex)
                    .or(BigInteger.ONE), MetaDataUtil.METADATA_MASK_LPORT_TAG_SH_FLAG));
            short tableId = (dpnTepConfigInfo.getTunnelType().isAssignableFrom(TunnelTypeMplsOverGre.class))
                    ? NwConstants.L3_LFIB_TABLE
                    : dpnTepConfigInfo.isInternal() ? NwConstants.INTERNAL_TUNNEL_TABLE
                    : NwConstants.DHCP_TABLE_EXTERNAL_TUNNEL;
            mkInstructions.add(new InstructionGotoTable(tableId));
        }

        FlowEntity flowEntity = MDSALUtil.buildFlowEntity(dpnId, NwConstants.VLAN_INTERFACE_INGRESS_TABLE, flowRef,
                ITMConstants.DEFAULT_FLOW_PRIORITY, interfaceName, 0, 0, NwConstants.COOKIE_VM_INGRESS_TABLE, matches,
                mkInstructions);
        if (NwConstants.ADD_FLOW == addOrRemoveFlow) {
            mdsalApiManager.batchedAddFlow(dpnId, flowEntity);
        } else {
            mdsalApiManager.batchedRemoveFlow(dpnId, flowEntity);
        }
    }

    private String getTunnelInterfaceFlowRef(BigInteger dpnId, short tableId, String ifName) {
        return String.valueOf(dpnId) + tableId + ifName;
    }

    public void deleteTunnelStateEntry(String interfaceName, WriteTransaction transaction) {
        LOG.debug(" deleteTunnelStateEntry tunnels state for {}", interfaceName);
        InstanceIdentifier<StateTunnelList> stateTnlId =
                ItmUtils.buildStateTunnelListId(new StateTunnelListKey(interfaceName));
        transaction.delete(LogicalDatastoreType.OPERATIONAL, stateTnlId);
    }

    public void removeLportTagInterfaceMap(WriteTransaction tx, String infName) {
        // workaround to get the id to remove from lport tag interface map
        Integer ifIndex = allocateId(IfmConstants.IFM_IDPOOL_NAME , infName);
        releaseId(IfmConstants.IFM_IDPOOL_NAME, infName);
        LOG.debug("removing lport tag to interface map for {}",infName);
        InstanceIdentifier<IfIndexTunnel> id = InstanceIdentifier.builder(IfIndexesTunnelMap.class)
                .child(IfIndexTunnel.class, new IfIndexTunnelKey(ifIndex)).build();
        tx.delete(LogicalDatastoreType.OPERATIONAL, id);
    }

    public void updateBfdParamtersForTerminationPoint(InstanceIdentifier<?> bridgeIid,
                                                       RemoteDpns remoteDpn) {
        final InstanceIdentifier<TerminationPoint> tpIid = createTerminationPointInstanceIdentifier(
            InstanceIdentifier.keyOf(bridgeIid.firstIdentifierOf(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang
                    .network.topology.rev131021.network.topology.topology.Node.class)), remoteDpn.getTunnelName());
        OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();

        if (bfdMonitoringEnabled(remoteDpn)) {
            List<InterfaceBfd> bfdParams = getBfdParams(remoteDpn);
            tpAugmentationBuilder.setInterfaceBfd(bfdParams);
        }

        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.setKey(InstanceIdentifier.keyOf(tpIid));
        tpBuilder.addAugmentation(OvsdbTerminationPointAugmentation.class, tpAugmentationBuilder.build());

        ITMBatchingUtils.update(tpIid, tpBuilder.build(), ITMBatchingUtils.EntityType.TOPOLOGY_CONFIG);
    }

}