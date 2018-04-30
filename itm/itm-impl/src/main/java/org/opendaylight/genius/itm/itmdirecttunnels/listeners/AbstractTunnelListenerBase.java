/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.itmdirecttunnels.listeners;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.interfacemanager.globals.IfmConstants;
import org.opendaylight.genius.itm.cache.DPNTEPsInfoCache;
import org.opendaylight.genius.itm.cache.DpnTepStateCache;
import org.opendaylight.genius.itm.cache.UnprocessedNodeConnectorCache;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ITMBatchingUtils;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.genius.itm.utils.DpnTepInterfaceInfo;
import org.opendaylight.genius.itm.utils.NodeConnectorInfo;
import org.opendaylight.genius.itm.utils.NodeConnectorInfoBuilder;
import org.opendaylight.genius.itm.utils.TunnelEndPointInfo;
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
import org.opendaylight.genius.tools.mdsal.listener.AbstractClusteredSyncDataTreeChangeListener;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlanGpe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.IfIndexesTunnelMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210._if.indexes.tunnel.map.IfIndexTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210._if.indexes.tunnel.map.IfIndexTunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210._if.indexes.tunnel.map.IfIndexTunnelKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TepTypeInternal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelOperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.state.tunnel.list.DstInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.state.tunnel.list.SrcInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbPortInterfaceAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceBfd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.OperationFailedException;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractTunnelListenerBase<T extends DataObject> extends AbstractClusteredSyncDataTreeChangeListener<T> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractTunnelListenerBase.class);

    protected final DpnTepStateCache dpnTepStateCache;
    protected final DPNTEPsInfoCache dpntePsInfoCache;
    protected final UnprocessedNodeConnectorCache unprocessedNCCache;

    private final EntityOwnershipUtils entityOwnershipUtils;
    private final IdManagerService idManager;
    private final IMdsalApiManager mdsalApiManager;

    AbstractTunnelListenerBase(final DataBroker dataBroker,
                               final LogicalDatastoreType logicalDatastoreType,
                               final InstanceIdentifier<T> instanceIdentifier,
                               final IdManagerService idManager,
                               final IMdsalApiManager mdsalApiManager,
                               final DpnTepStateCache dpnTepStateCache,
                               final DPNTEPsInfoCache dpntePsInfoCache,
                               final UnprocessedNodeConnectorCache unprocessedNodeConnectorCache,
                               final EntityOwnershipUtils entityOwnershipUtils) {
        super(dataBroker, logicalDatastoreType, instanceIdentifier);
        this.dpnTepStateCache = dpnTepStateCache;
        this.dpntePsInfoCache = dpntePsInfoCache;
        this.mdsalApiManager = mdsalApiManager;
        this.unprocessedNCCache = unprocessedNodeConnectorCache;
        this.idManager = idManager;
        this.entityOwnershipUtils = entityOwnershipUtils;
    }

    public boolean entityOwner() {
        return entityOwnershipUtils.isEntityOwner(ITMConstants.ITM_CONFIG_ENTITY, ITMConstants.ITM_CONFIG_ENTITY);
    }

    public List<ListenableFuture<Void>> addState(InstanceIdentifier<FlowCapableNodeConnector> key,
                                                 String interfaceName, FlowCapableNodeConnector fcNodeConnectorNew)
            throws ExecutionException, InterruptedException, OperationFailedException {
        boolean unableToProcess = false;
        // Retrieve Port No from nodeConnectorId
        NodeConnectorId nodeConnectorId = InstanceIdentifier.keyOf(key.firstIdentifierOf(NodeConnector.class)).getId();
        long portNo = DirectTunnelUtils.getPortNumberFromNodeConnectorId(nodeConnectorId);
        if (portNo == ITMConstants.INVALID_PORT_NO) {
            LOG.trace("Cannot derive port number, not proceeding with Interface State "
                    + "addition for interface: {}", interfaceName);
            return null;
        }

        LOG.info("adding interface state to Oper DS for interface: {}", interfaceName);

        Interface.OperStatus operStatus = Interface.OperStatus.Up;
        Interface.AdminStatus adminStatus = Interface.AdminStatus.Up;

        // Fetch the interface/Tunnel from config DS if exists
        TunnelEndPointInfo tunnelEndPointInfo = dpnTepStateCache.getTunnelEndPointInfoFromCache(interfaceName);

        if (tunnelEndPointInfo != null) {
            DpnTepInterfaceInfo dpnTepConfigInfo = dpnTepStateCache.getDpnTepInterface(
                    new BigInteger(tunnelEndPointInfo.getSrcEndPointInfo()),
                    new BigInteger(tunnelEndPointInfo.getDstEndPointInfo()));
            if (dpnTepConfigInfo != null) {
                StateTunnelList stateTnl = addStateEntry(tunnelEndPointInfo, interfaceName,
                        operStatus, adminStatus, nodeConnectorId);

                // This will be only tunnel If so not required
                // If this interface is a tunnel interface, create the tunnel ingress flow,
                // and start tunnel monitoring
                if (stateTnl != null) {
                    handleTunnelMonitoringAddition(nodeConnectorId, stateTnl.getIfIndex(), dpnTepConfigInfo,
                            interfaceName, portNo);
                }
            } else {
                LOG.error("DpnTepINfo is NULL while addState for interface {} ", interfaceName);
                unableToProcess = true;
            }
        } else {
            LOG.error("TunnelEndPointInfo is NULL while addState for interface {} ", interfaceName);
            unableToProcess = true;
        }
        if (unableToProcess) {
            LOG.debug("Unable to process the NodeConnector ADD event for {} as Config not available."
                    + "Hence parking it", interfaceName);
            NodeConnectorInfo nodeConnectorInfo = new NodeConnectorInfoBuilder().setNodeConnectorId(key)
                    .setNodeConnector(fcNodeConnectorNew).build();
            unprocessedNCCache.add(interfaceName, nodeConnectorInfo);
        }
        return Collections.emptyList();
    }

    private StateTunnelList addStateEntry(TunnelEndPointInfo tunnelEndPointInfo, String interfaceName,
                                          Interface.OperStatus operStatus, Interface.AdminStatus adminStatus,
                                          NodeConnectorId nodeConnectorId)
            throws ExecutionException, InterruptedException, OperationFailedException {
        LOG.debug("Start addStateEntry adding interface state for {}", interfaceName);
        final StateTunnelListBuilder stlBuilder = new StateTunnelListBuilder();
        Class<? extends TunnelTypeBase> tunnelType;

        // Retrieve Port No from nodeConnectorId
        final long portNo = DirectTunnelUtils.getPortNumberFromNodeConnectorId(nodeConnectorId);
        DpnTepInterfaceInfo dpnTepInfo = dpnTepStateCache.getDpnTepInterface(
                new BigInteger(tunnelEndPointInfo.getSrcEndPointInfo()),
                new BigInteger(tunnelEndPointInfo.getDstEndPointInfo()));
        LOG.debug("Source Dpn TEP Interface Info {}" , dpnTepInfo);
        tunnelType = dpnTepInfo.getTunnelType();

        final SrcInfoBuilder srcInfoBuilder =
                new SrcInfoBuilder().setTepDeviceId(tunnelEndPointInfo.getSrcEndPointInfo());
        final DstInfoBuilder dstInfoBuilder =
                new DstInfoBuilder().setTepDeviceId(tunnelEndPointInfo.getDstEndPointInfo());

        java.util.Optional<DPNTEPsInfo> srcDpnTepsInfo = dpntePsInfoCache
                .getDPNTepFromDPNId(new BigInteger(tunnelEndPointInfo.getSrcEndPointInfo()));

        LOG.debug("Source Dpn TEP Info {}", srcDpnTepsInfo);
        TunnelEndPoints srcEndPtInfo = srcDpnTepsInfo.get().getTunnelEndPoints().get(0);
        srcInfoBuilder.setTepIp(srcEndPtInfo.getIpAddress());
        // As ITM Direct Tunnels deals with only Internal Tunnels.
        // Relook at this when it deals with external as well
        srcInfoBuilder.setTepDeviceType(TepTypeInternal.class);

        java.util.Optional<DPNTEPsInfo> dstDpnTePsInfo = dpntePsInfoCache
                .getDPNTepFromDPNId(new BigInteger(tunnelEndPointInfo.getDstEndPointInfo()));

        LOG.debug("Dest Dpn TEP Info {}", dstDpnTePsInfo);
        TunnelEndPoints dstEndPtInfo = dstDpnTePsInfo.get().getTunnelEndPoints().get(0);
        dstInfoBuilder.setTepIp(dstEndPtInfo.getIpAddress());
        // As ITM Direct Tunnels deals with only Internal Tunnels.
        // Relook at this when it deals with external as well
        dstInfoBuilder.setTepDeviceType(TepTypeInternal.class);

        // ITM Direct Tunnels NOT SETTING THE TEP TYPe coz its not available. CHECK IF REQUIRED
        TunnelOperStatus tunnelOperStatus = DirectTunnelUtils.convertInterfaceToTunnelOperState(operStatus);
        boolean tunnelState = operStatus.equals(Interface.OperStatus.Up);

        StateTunnelListKey tlKey = new StateTunnelListKey(interfaceName);
        stlBuilder.setKey(tlKey)
                .setOperState(tunnelOperStatus).setTunnelState(tunnelState)
                .setDstInfo(dstInfoBuilder.build()).setSrcInfo(srcInfoBuilder.build()).setTransportType(tunnelType)
                .setPortNumber(String.valueOf(portNo));
        int ifIndex;
        ifIndex = allocateId(ITMConstants.ITM_IDPOOL_NAME, interfaceName);
        createLportTagInterfaceMap(interfaceName, ifIndex);
        stlBuilder.setIfIndex(ifIndex);
        InstanceIdentifier<StateTunnelList> stListId = ItmUtils.buildStateTunnelListId(tlKey);
        LOG.trace("Batching the Creation of tunnel_state: {} for Id: {}", stlBuilder.build(), stListId);
        ITMBatchingUtils.write(stListId, stlBuilder.build(), ITMBatchingUtils.EntityType.DEFAULT_OPERATIONAL);
        return stlBuilder.build();
    }

    private void handleTunnelMonitoringAddition(NodeConnectorId nodeConnectorId,
                                                Integer ifindex, DpnTepInterfaceInfo dpnTepConfigInfo,
                                                String interfaceName, long portNo) {
        BigInteger dpId = DirectTunnelUtils.getDpnFromNodeConnectorId(nodeConnectorId);
        makeTunnelIngressFlow(dpnTepConfigInfo, dpId, portNo, interfaceName,
                ifindex, NwConstants.ADD_FLOW);
    }

    protected void makeTunnelIngressFlow(DpnTepInterfaceInfo dpnTepConfigInfo, BigInteger dpnId, long portNo,
                                       String interfaceName, int ifIndex, int addOrRemoveFlow) {
        LOG.debug("make tunnel ingress flow for {}", interfaceName);
        String flowRef = getTunnelInterfaceFlowRef(dpnId, NwConstants.VLAN_INTERFACE_INGRESS_TABLE, interfaceName);
        List<MatchInfoBase> matches = new ArrayList<>();

        List<InstructionInfo> mkInstructions = new ArrayList<>();
        if (NwConstants.ADD_FLOW == addOrRemoveFlow) {
            matches.add(new MatchInPort(dpnId, portNo));
            mkInstructions.add(new InstructionWriteMetadata(MetaDataUtil.getLportTagMetaData(ifIndex)
                    .or(BigInteger.ONE), MetaDataUtil.METADATA_MASK_LPORT_TAG_SH_FLAG));
            short tableId = NwConstants.INTERNAL_TUNNEL_TABLE;
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

    private void createLportTagInterfaceMap(String infName, Integer ifIndex) {
        LOG.debug("creating lport tag to interface map for {}",infName);
        InstanceIdentifier<IfIndexTunnel> id = InstanceIdentifier.builder(IfIndexesTunnelMap.class)
                .child(IfIndexTunnel.class, new IfIndexTunnelKey(ifIndex)).build();
        IfIndexTunnel ifIndexInterface = new IfIndexTunnelBuilder().setIfIndex(ifIndex)
                .setKey(new IfIndexTunnelKey(ifIndex)).setInterfaceName(infName).build();
        ITMBatchingUtils.write(id, ifIndexInterface, ITMBatchingUtils.EntityType.DEFAULT_OPERATIONAL);
    }

    private int allocateId(String poolName, String idKey)
            throws InterruptedException, ExecutionException , OperationFailedException {
        AllocateIdInput getIdInput = new AllocateIdInputBuilder().setPoolName(poolName).setIdKey(idKey).build();
        RpcResult<AllocateIdOutput> rpcResult = idManager.allocateId(getIdInput).get();
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
        IfL2vlan ifL2vlan = iface.getAugmentation(IfL2vlan.class);
        if (ifL2vlan != null && ifL2vlan.getVlanId() != null) {
            vlanId = ifL2vlan.getVlanId().getValue();
        }

        Builder<String, String> options = new ImmutableMap.Builder<>();

        // Options common to any kind of tunnel
        IpAddress localIp = ifTunnel.getTunnelSource();
        options.put(DirectTunnelUtils.TUNNEL_OPTIONS_LOCAL_IP, localIp.getIpv4Address().getValue());

        IpAddress remoteIp = ifTunnel.getTunnelDestination();
        options.put(DirectTunnelUtils.TUNNEL_OPTIONS_REMOTE_IP, remoteIp.getIpv4Address().getValue());

        options.put(DirectTunnelUtils.TUNNEL_OPTIONS_TOS, DirectTunnelUtils.TUNNEL_OPTIONS_TOS_VALUE_INHERIT);

        // Specific options for each type of tunnel
        if (ifTunnel.getTunnelInterfaceType().equals(TunnelTypeVxlanGpe.class)) {
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
        // checkBfdMonEnabled
        if (ifTunnel.isMonitorEnabled()
                && TunnelMonitoringTypeBfd.class.isAssignableFrom(ifTunnel.getMonitorProtocol())) {
            List<InterfaceBfd> bfdParams = DirectTunnelUtils.getBfdParams(ifTunnel);
            tpAugmentationBuilder.setInterfaceBfd(bfdParams);
        }

        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.setKey(InstanceIdentifier.keyOf(tpIid));
        tpBuilder.addAugmentation(OvsdbTerminationPointAugmentation.class, tpAugmentationBuilder.build());

        ITMBatchingUtils.write(tpIid, tpBuilder.build(), ITMBatchingUtils.EntityType.TOPOLOGY_CONFIG);
    }

    public void deleteTunnelStateEntry(String interfaceName) {
        LOG.debug(" deleteTunnelStateEntry tunnels state for {}", interfaceName);
        InstanceIdentifier<StateTunnelList> stateTnlId =
                ItmUtils.buildStateTunnelListId(new StateTunnelListKey(interfaceName));

        ITMBatchingUtils.delete(stateTnlId, ITMBatchingUtils.EntityType.DEFAULT_OPERATIONAL);
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
        Future<RpcResult<Void>> result = idManager.releaseId(idInput);
        RpcResult<Void> rpcResult = result.get();
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
}
