/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.scaling.renderer.ovs.utilities;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ITMBatchingUtils;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.mdsalutil.FlowEntity;
import org.opendaylight.genius.mdsalutil.InstructionInfo;
import org.opendaylight.genius.mdsalutil.MatchInfoBase;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.MetaDataUtil;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.mdsalutil.instructions.InstructionGotoTable;
import org.opendaylight.genius.mdsalutil.instructions.InstructionWriteMetadata;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.genius.mdsalutil.matches.MatchInPort;
import org.opendaylight.genius.utils.cache.DataStoreCache;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeMplsOverGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TepTypeInternal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelOperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelsState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.DpnsTeps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.state.tunnel.list.DstInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.state.tunnel.list.SrcInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* Equivalent of InterfaceManagerCommonUtils */
public class TunnelUtils {
    private static final Logger LOG = LoggerFactory.getLogger(TunnelUtils.class);

    // ITM Direct Tunnels -- Retaining BFD State Map temporarily.
    public static ConcurrentHashMap<String, OperStatus> bfdStateMap =
            new ConcurrentHashMap<>();

    private static final String TUNNEL_PORT_REGEX = "tun[0-9a-f]{11}";
    private static final Pattern TUNNEL_PORT_PATTERN = Pattern.compile(TUNNEL_PORT_REGEX);
    public static final Predicate<String> TUNNEL_PORT_PREDICATE =
        portName -> TUNNEL_PORT_PATTERN.matcher(portName).matches();

    public static NodeConnector getNodeConnectorFromInventoryOperDS(NodeConnectorId nodeConnectorId,
                                                                    DataBroker dataBroker) {
        NodeId nodeId = ItmScaleUtils.getNodeIdFromNodeConnectorId(nodeConnectorId);
        InstanceIdentifier<NodeConnector> ncIdentifier = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(nodeId))
                .child(NodeConnector.class, new NodeConnectorKey(nodeConnectorId)).build();

        Optional<NodeConnector> nodeConnectorOptional = ItmUtils.read(LogicalDatastoreType.OPERATIONAL, ncIdentifier,
                dataBroker);
        if (!nodeConnectorOptional.isPresent()) {
            return null;
        }
        return nodeConnectorOptional.get();
    }


    public static boolean isNodeConnectorPresent(DataBroker dataBroker, NodeConnectorId nodeConnectorId) {
        if (getNodeConnectorFromInventoryOperDS(nodeConnectorId, dataBroker) != null) {
            return true;
        }
        return false;
    }

    public static boolean isNodePresent(DataBroker dataBroker, NodeConnectorId nodeConnectorId) {
        NodeId nodeID = ItmScaleUtils.getNodeIdFromNodeConnectorId(nodeConnectorId);
        InstanceIdentifier<Node> nodeInstanceIdentifier = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(nodeID)).build();
        Optional<Node> node = ItmUtils.read(LogicalDatastoreType.OPERATIONAL, nodeInstanceIdentifier, dataBroker);

        if (node.isPresent()) {
            return true;
        }
        return false;
    }

    public static InstanceIdentifier<StateTunnelList> getStateTunnelListIdentifier(StateTunnelListKey key) {
        InstanceIdentifier.InstanceIdentifierBuilder<StateTunnelList> stateTnlIIBuilder = InstanceIdentifier
                .builder(TunnelsState.class).child(StateTunnelList.class, key);
        return stateTnlIIBuilder.build();
    }

        /**
         * Searches for an tunnel by its name.
         * @param tunnelName of the interface to search for
         * @param dataBroker data tree store to start searching for the interface
         * @return the StateTunnelList object
         *
         */
        // This is not used as there is another one getTunnelFromOperationalDS
    public static StateTunnelList getTunnelFromOperDS(String tunnelName, DataBroker dataBroker) {
        StateTunnelList stateTnl =
                (StateTunnelList) DataStoreCache.get(ITMConstants.TUNNEL_STATE_CACHE_NAME, tunnelName);
        if (stateTnl != null) {
            return stateTnl;
        }
        InstanceIdentifier<StateTunnelList> stateTnlId =
                getStateTunnelListIdentifier(new StateTunnelListKey(tunnelName));
        Optional<StateTunnelList> tunnelOptional =
                ItmUtils.read(LogicalDatastoreType.CONFIGURATION, stateTnlId, dataBroker);
        if (tunnelOptional.isPresent()) {
            stateTnl = tunnelOptional.get();
        }
        return stateTnl;
    }

    public static DpnTepInterfaceInfo getTunnelFromConfigDS(String tunnelName, DataBroker dataBroker) {
        TunnelEndPointInfo endPointInfo = ItmScaleUtils.getTunnelEndPointInfoFromCache(tunnelName);
        DpnTepInterfaceInfo dpnTepInfo = null ;
        if (endPointInfo != null) {
            dpnTepInfo = ItmScaleUtils.getDpnTepInterfaceFromCache(new BigInteger(endPointInfo.getSrcDpnId()),
                    new BigInteger(endPointInfo.getDstDpnId()));
            if (dpnTepInfo != null) {
                return dpnTepInfo;
            }
            else {
                //SF419 TO DO
                // Read if from Datastore
                // Fill in IfTunnel
            }

        }
        return dpnTepInfo;

    }

    public static Interface getInterfaceFromConfigDS(String tunnelName, DataBroker dataBroker) {
        TunnelEndPointInfo endPointInfo = ItmScaleUtils.getTunnelEndPointInfoFromCache(tunnelName);
        Interface iface = null ;
        DpnTepInterfaceInfo dpnTepInfo;
        if (endPointInfo != null) {
            DpnsTeps srcDpnTeps = ItmScaleUtils.getDpnsTepsFromCache(new BigInteger(endPointInfo.getSrcDpnId()));
            DpnsTeps dstDpnTeps = ItmScaleUtils.getDpnsTepsFromCache(new BigInteger(endPointInfo.getDstDpnId()));
            dpnTepInfo = ItmScaleUtils.getDpnTepInterfaceFromCache(new BigInteger(endPointInfo.getSrcDpnId()),
                    new BigInteger(endPointInfo.getDstDpnId()));
            if (dpnTepInfo != null) {
                List<DPNTEPsInfo> srcDpnTEPsInfo = ItmUtils.getDPNTepListFromDPNId(dataBroker,
                        new ArrayList<>(Arrays.asList(new BigInteger(endPointInfo.getSrcDpnId()))));
                List<DPNTEPsInfo> dstDpnTEPsInfo = ItmUtils.getDPNTepListFromDPNId(dataBroker,
                        new ArrayList<>(Arrays.asList(new BigInteger(endPointInfo.getDstDpnId()))));
                Boolean monitorEnabled = ItmUtils.readMonitoringStateFromCache(dataBroker);
                Integer monitorInterval = ItmUtils.determineMonitorInterval(dataBroker);
                Class<? extends TunnelMonitoringTypeBase> monitorProtocol =
                        ItmUtils.determineMonitorProtocol(dataBroker);
                String description = String.format("%s %s",
                        ItmUtils.convertTunnelTypetoString(dpnTepInfo.getTunnelType()), "Trunk Interface");
                iface = ItmUtils.buildTunnelInterface(new BigInteger(endPointInfo.getSrcDpnId()), tunnelName,
                        description, true, dpnTepInfo.getTunnelType(),
                        srcDpnTEPsInfo.get(0).getTunnelEndPoints().get(0).getIpAddress(),
                        dstDpnTEPsInfo.get(0).getTunnelEndPoints().get(0).getIpAddress(),
                        srcDpnTEPsInfo.get(0).getTunnelEndPoints().get(0).getGwIpAddress(),
                        srcDpnTEPsInfo.get(0).getTunnelEndPoints().get(0).getVLANID(),
                        true, monitorEnabled, monitorProtocol, monitorInterval, false, null);//doubt
            } else {
                //SF419 TO DO
                // Read if from Datastore
            }
        }
        return iface;
    }

    public static StateTunnelList getTunnelFromOperationalDS(String tunnelName, DataBroker dataBroker) {
        StateTunnelList stateTnl = (StateTunnelList)DataStoreCache.get(ITMConstants.TUNNEL_STATE_CACHE_NAME,
                tunnelName);
        if (stateTnl != null) {
            return stateTnl;
        }
        else {
            InstanceIdentifier<StateTunnelList> stateTnlII =
                    ItmUtils.buildStateTunnelListId(new StateTunnelListKey(tunnelName));
            Optional<StateTunnelList> tnlStateOptional =
                    ItmUtils.read(LogicalDatastoreType.OPERATIONAL, stateTnlII, dataBroker);
            if (tnlStateOptional.isPresent()) {
                return tnlStateOptional.get();
            }
        }
        return null;
    }

    public static void makeTunnelIngressFlow(List<ListenableFuture<Void>> futures, IMdsalApiManager mdsalApiManager,
                                             DpnTepInterfaceInfo dpnTepConfigInfo, BigInteger dpnId, long portNo,
                                             String interfaceName, int ifIndex, int addOrRemoveFlow) {
        LOG.debug("make tunnel ingress flow for {}", interfaceName);
        String flowRef =
                TunnelUtils.getTunnelInterfaceFlowRef(dpnId, NwConstants.VLAN_INTERFACE_INGRESS_TABLE, interfaceName);
        List<MatchInfoBase> matches = new ArrayList<>();

        List<InstructionInfo> mkInstructions = new ArrayList<>();
        if (NwConstants.ADD_FLOW == addOrRemoveFlow) {
            matches.add(new MatchInPort(dpnId, portNo));
            /*matches.add(new MatchInfo(MatchFieldType.in_port, new BigInteger[]
            { dpnId, BigInteger.valueOf(portNo) }));*/
            mkInstructions.add(new InstructionWriteMetadata(MetaDataUtil.getElanTagMetadata(ifIndex)
                    .or(BigInteger.ONE), MetaDataUtil.METADATA_MASK_LPORT_TAG_SH_FLAG));
            /*mkInstructions.add(new InstructionInfo(InstructionType.write_metadata,
                    new BigInteger[] { MetaDataUtil.getLportTagMetaData(ifIndex).or(BigInteger.ONE),
                            MetaDataUtil.METADATA_MASK_LPORT_TAG_SH_FLAG }));*/
            short tableId = (dpnTepConfigInfo.getTunnelType().isAssignableFrom(TunnelTypeMplsOverGre.class))
                    ? NwConstants.L3_LFIB_TABLE
                    : dpnTepConfigInfo.isInternal() ? NwConstants.INTERNAL_TUNNEL_TABLE
                    : NwConstants.DHCP_TABLE_EXTERNAL_TUNNEL;
            mkInstructions.add(new InstructionGotoTable(tableId));
            /*mkInstructions.add(new InstructionInfo(InstructionType.goto_table, new long[] { tableId }));*/

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

    public static String getTunnelInterfaceFlowRef(BigInteger dpnId, short tableId, String ifName) {
        return String.valueOf(dpnId) + tableId + ifName;
    }

    public static StateTunnelList addStateEntry(TunnelEndPointInfo tunnelEndPointInfo, String interfaceName,
                                                WriteTransaction transaction, IdManagerService idManager,
                                                OperStatus operStatus, AdminStatus adminStatus,
                                                NodeConnectorId nodeConnectorId) {
        LOG.debug("Start addStateEntry adding interface state for {}", interfaceName);
        final StateTunnelListBuilder stlBuilder = new StateTunnelListBuilder();
        final Integer ifIndex;
        Class<? extends TunnelTypeBase> tunnelType = null;
        //ITM Direct Tunnels IS THIS REQIURED CHECK?? TunnelEndPoints will not be NULL ini this case??
        //if (tunnelEndPointInfo != null) {
        /*
        if(!interfaceInfo.isEnabled()){
            operStatus = OperStatus.Down;
        }
        */
        //Retrieve Port No from nodeConnectorId
        final long portNo = ItmScaleUtils.getPortNumberFromNodeConnectorId(nodeConnectorId);
        DpnTepInterfaceInfo dpnTepInfo = ItmScaleUtils
                .getDpnTepInterfaceFromCache(new BigInteger(tunnelEndPointInfo.getSrcDpnId()),
                        new BigInteger(tunnelEndPointInfo.getDstDpnId()));
        LOG.debug("Source Dpn TEP Interface Info {}" , dpnTepInfo);
        tunnelType = dpnTepInfo.getTunnelType();

        final DstInfoBuilder dstInfoBuilder = new DstInfoBuilder();
        SrcInfoBuilder srcInfoBuilder = new SrcInfoBuilder();

        srcInfoBuilder.setTepDeviceId(tunnelEndPointInfo.getSrcDpnId());
        DPNTEPsInfo srcDpnTePsInfo = (DPNTEPsInfo) DataStoreCache.get(ITMConstants.DPN_TEPs_Info_CACHE_NAME,
            new BigInteger(tunnelEndPointInfo.getSrcDpnId()));
        LOG.debug("Source Dpn TEP Info {}", srcDpnTePsInfo);
        TunnelEndPoints srcEndPtInfo = srcDpnTePsInfo.getTunnelEndPoints().get(0);
        srcInfoBuilder.setTepIp(srcEndPtInfo.getIpAddress());
        //As ITM Direct Tunnels deals with only Internal Tunnels.
        //Relook at this when it deals with external as well
        srcInfoBuilder.setTepDeviceType(TepTypeInternal.class);

        dstInfoBuilder.setTepDeviceId(tunnelEndPointInfo.getDstDpnId());
        DPNTEPsInfo dstDpnTePsInfo = (DPNTEPsInfo) DataStoreCache.get(ITMConstants.DPN_TEPs_Info_CACHE_NAME,
            new BigInteger(tunnelEndPointInfo.getDstDpnId()));
        LOG.debug("Dest Dpn TEP Info {}", dstDpnTePsInfo);
        TunnelEndPoints dstEndPtInfo = dstDpnTePsInfo.getTunnelEndPoints().get(0);
        dstInfoBuilder.setTepIp(dstEndPtInfo.getIpAddress());
        //As ITM Direct Tunnels deals with only Internal Tunnels.
        //Relook at this when it deals with external as well
        dstInfoBuilder.setTepDeviceType(TepTypeInternal.class);

        // ITM Direct Tunnels NOT SETTING THE TEP TYPe coz its not available. CHECK IF REQUIRED
        TunnelOperStatus tunnelOperStatus = ItmScaleUtils.convertInterfaceToTunnelOperState(operStatus);
        boolean tunnelState = (operStatus.equals(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
                .interfaces.rev140508.interfaces.state.Interface.OperStatus.Up)) ? (true) : (false);

        StateTunnelListKey tlKey = new StateTunnelListKey(interfaceName);
        stlBuilder.setKey(tlKey).setTunnelInterfaceName(interfaceName)
                .setOperState(tunnelOperStatus).setTunnelState(tunnelState)
                .setDstInfo(dstInfoBuilder.build()).setSrcInfo(srcInfoBuilder.build()).setTransportType(tunnelType)
                .setPortNumber(String.valueOf(portNo));
        // ITM DIRECT TUnnels CHECK ifIndex is required ??
        ifIndex = ItmUtils.allocateId(idManager, ITMConstants.ITM_IDPOOL_NAME, interfaceName);
        TunnelMetaUtils.createLportTagInterfaceMap(transaction, interfaceName, ifIndex);

        stlBuilder.setIfIndex(ifIndex);
        InstanceIdentifier<StateTunnelList> stListId = ItmUtils.buildStateTunnelListId(tlKey);
        LOG.trace("Batching the Creation of tunnel_state: {} for Id: {}", stlBuilder.build(), stListId);
        ITMBatchingUtils.write(stListId, stlBuilder.build(), ITMBatchingUtils.EntityType.DEFAULT_OPERATIONAL);

        return stlBuilder.build();
    }

    public static void deleteTunnelStateEntry(String interfaceName, WriteTransaction transaction) {
        LOG.debug(" deleteTunnelStateEntry tunnels state for {}", interfaceName);
        InstanceIdentifier<StateTunnelList> stateTnlId = ItmUtils.buildStateTunnelListId(
                                          new StateTunnelListKey(interfaceName));
        transaction.delete(LogicalDatastoreType.OPERATIONAL, stateTnlId);
    }

    public static void deleteTunnelStateInformation(String interfaceName, WriteTransaction transaction,
            IdManagerService idManagerService) {
        LOG.debug("removing tunnels state information for {}", interfaceName);
        InstanceIdentifier<StateTunnelList> stateTnlId = ItmUtils.buildStateTunnelListId(
                              new StateTunnelListKey(interfaceName));
        transaction.delete(LogicalDatastoreType.OPERATIONAL, stateTnlId);
        TunnelMetaUtils.removeLportTagInterfaceMap(idManagerService, transaction, interfaceName);
    }

    /*
     * update operational state of interface based on events like tunnel
     * monitoring
     */
    public static void updateOpState(WriteTransaction transaction, String interfaceName,
            TunnelOperStatus operStatus) {
        StateTunnelListKey stateTnlKey = new StateTunnelListKey(interfaceName);
        InstanceIdentifier<StateTunnelList> stateTnlII = ItmUtils.buildStateTunnelListId(stateTnlKey);
        LOG.debug("updating tep interface state as {} for {}", operStatus.name(), interfaceName);
        StateTunnelListBuilder stateTnlBuilder = new StateTunnelListBuilder().setKey(stateTnlKey);
        stateTnlBuilder.setOperState(operStatus);
        transaction.merge(LogicalDatastoreType.OPERATIONAL, stateTnlII, stateTnlBuilder.build(), false);
    }

    public static OperStatus getBfdStateFromCache(String interfaceName) {
        return bfdStateMap.get(interfaceName);
    }

    public static void addBfdStateToCache(String interfaceName,
                                          OperStatus operStatus) {
        bfdStateMap.put(interfaceName, operStatus);
    }

    public static OperStatus removeBfdStateFromCache(String interfaceName) {
        return bfdStateMap.remove(interfaceName);
    }
}
