/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.itmdirecttunnels.listeners;

import com.google.common.util.concurrent.ListenableFuture;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.datastoreutils.listeners.AbstractClusteredSyncDataTreeChangeListener;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.cache.DPNTEPsInfoCache;
import org.opendaylight.genius.itm.cache.DpnTepStateCache;
import org.opendaylight.genius.itm.cache.TunnelStateCache;
import org.opendaylight.genius.itm.cache.UnprocessedNodeConnectorCache;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ITMBatchingUtils;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.genius.itm.utils.DpnTepInterfaceInfo;
import org.opendaylight.genius.itm.utils.NodeConnectorInfo;
import org.opendaylight.genius.itm.utils.NodeConnectorInfoBuilder;
import org.opendaylight.genius.itm.utils.TunnelEndPointInfo;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortReason;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Class is a Data Change Listener for FlowCapableNodeConnector updates.
 * This creates an entry in the tunnels-state OperDS for every node-connector used.
 */
public class TunnelInventoryStateListener extends
        AbstractClusteredSyncDataTreeChangeListener<FlowCapableNodeConnector> {
    private static final Logger LOG = LoggerFactory.getLogger(TunnelInventoryStateListener.class);

    private final DataBroker dataBroker;
    private final IdManagerService idManager;
    private final IMdsalApiManager mdsalApiManager;
    private final JobCoordinator coordinator;
    private final ManagedNewTransactionRunner txRunner;
    private final EntityOwnershipUtils entityOwnershipUtils;
    private final DirectTunnelUtils directTunnelUtils;
    private final DPNTEPsInfoCache dpntePsInfoCache;
    private final TunnelStateCache tunnelStateCache;
    private final DpnTepStateCache dpnTepStateCache;
    private final UnprocessedNodeConnectorCache unprocessedNCCache;

    public TunnelInventoryStateListener(final DataBroker dataBroker, final IdManagerService idManagerService,
                                        final IMdsalApiManager mdsalApiManager,
                                        final IInterfaceManager interfaceManager,
                                        final JobCoordinator coordinator,
                                        final EntityOwnershipUtils entityOwnershipUtils,
                                        final DirectTunnelUtils directTunnelUtils,
                                        final DPNTEPsInfoCache dpntePsInfoCache,
                                        final TunnelStateCache tunnelStateCache,
                                        final DpnTepStateCache dpnTepStateCache,
                                        final UnprocessedNodeConnectorCache unprocessedNCCache) {
        super(dataBroker, LogicalDatastoreType.OPERATIONAL,InstanceIdentifier.create(Nodes.class).child(Node.class)
                .child(NodeConnector.class).augmentation(FlowCapableNodeConnector.class));
        this.dataBroker = dataBroker;
        this.idManager = idManagerService;
        this.mdsalApiManager = mdsalApiManager;
        this.coordinator = coordinator;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.entityOwnershipUtils = entityOwnershipUtils;
        this.directTunnelUtils = directTunnelUtils;
        this.dpntePsInfoCache = dpntePsInfoCache;
        this.tunnelStateCache = tunnelStateCache;
        this.dpnTepStateCache = dpnTepStateCache;
        this.unprocessedNCCache = unprocessedNCCache;
    }

    @Override
    public void remove(InstanceIdentifier<FlowCapableNodeConnector> key,
                          FlowCapableNodeConnector flowCapableNodeConnectorOld) {
        String portName = flowCapableNodeConnectorOld.getName();
        LOG.debug("InterfaceInventoryState Remove for {}", portName);
        // ITM Direct Tunnels Return if its not tunnel port and if its not Internal
        if (!directTunnelUtils.TUNNEL_PORT_PREDICATE.test(portName)) {
            LOG.debug("Node Connector Remove - {} Interface is not a tunnel I/f, so no-op", portName);
            return;
        } else {
            try {
                if (!tunnelStateCache.isInternalBasedOnState(portName)) {
                    LOG.debug("Node Connector Remove {} Interface is not a internal tunnel I/f, so no-op", portName);
                    return;
                }
            } catch (ReadFailedException e) {
                LOG.debug("Tunnel {} is not present in operational DS ", portName);
                return;
            }
        }
        if (!entityOwnershipUtils.isEntityOwner(ITMConstants.ITM_CONFIG_ENTITY, ITMConstants.ITM_CONFIG_ENTITY)) {
            return;
        }
        LOG.debug("Received NodeConnector Remove Event: {}, {}", key, flowCapableNodeConnectorOld);
        NodeConnectorId nodeConnectorId = InstanceIdentifier.keyOf(key.firstIdentifierOf(NodeConnector.class)).getId();
        remove(nodeConnectorId, null, flowCapableNodeConnectorOld, portName);
    }

    private void remove(NodeConnectorId nodeConnectorIdNew, NodeConnectorId nodeConnectorIdOld,
                        FlowCapableNodeConnector fcNodeConnectorNew, String portName) {
        LOG.debug("InterfaceInventoryState REMOVE for {}", portName);
        InterfaceStateRemoveWorker portStateRemoveWorker = new InterfaceStateRemoveWorker(nodeConnectorIdNew,
                nodeConnectorIdOld, fcNodeConnectorNew, portName, portName);
        coordinator.enqueueJob(portName, portStateRemoveWorker, ITMConstants.JOB_MAX_RETRIES);
    }

    @Override
    public void update(InstanceIdentifier<FlowCapableNodeConnector> key, FlowCapableNodeConnector fcNodeConnectorOld,
                          FlowCapableNodeConnector fcNodeConnectorNew) {
        String portName = fcNodeConnectorNew.getName();
        if (directTunnelUtils.TUNNEL_PORT_PREDICATE.test(portName)) {
            LOG.debug("Node Connector Update - {} Interface is not a tunnel I/f, so no-op", portName);
            return;
        } else if (!dpnTepStateCache.isInternal(portName)) {
            LOG.debug("Node Connector Update {} Interface is not a internal tunnel I/f, so no-op", portName);
            return;
        }
        if (fcNodeConnectorNew.getReason() == PortReason.Delete
               || !entityOwnershipUtils.isEntityOwner(ITMConstants.ITM_CONFIG_ENTITY, ITMConstants.ITM_CONFIG_ENTITY)) {
            return;
        }
        LOG.debug("Received NodeConnector Update Event: {}, {}, {}", key, fcNodeConnectorOld, fcNodeConnectorNew);

        InterfaceStateUpdateWorker portStateUpdateWorker = new InterfaceStateUpdateWorker(key, fcNodeConnectorOld,
                fcNodeConnectorNew, portName);
        coordinator.enqueueJob(portName, portStateUpdateWorker, ITMConstants.JOB_MAX_RETRIES);
    }

    @Override
    public void add(InstanceIdentifier<FlowCapableNodeConnector> key, FlowCapableNodeConnector fcNodeConnectorNew) {
        String portName = fcNodeConnectorNew.getName();
        LOG.debug("InterfaceInventoryState ADD for {}", portName);
        // SF419 Return if its not tunnel port and if its not Internal
        if (!directTunnelUtils.TUNNEL_PORT_PREDICATE.test(portName)) {
            LOG.debug("Node Connector Add {} Interface is not a tunnel I/f, so no-op", portName);
            return;
        }
        if (!entityOwnershipUtils.isEntityOwner(ITMConstants.ITM_CONFIG_ENTITY, ITMConstants.ITM_CONFIG_ENTITY)) {
            return;
        }
        if (!dpnTepStateCache.isConfigAvailable(portName)) {
            // Park the notification
            LOG.debug("Unable to process the NodeConnector ADD event for {} as Config not available."
                    + "Hence parking it", portName);
            NodeConnectorInfo nodeConnectorInfo = new NodeConnectorInfoBuilder().setNodeConnectorId(key)
                    .setNodeConnector(fcNodeConnectorNew).build();
            unprocessedNCCache.add(portName, nodeConnectorInfo);
            return;
        } else if (!dpnTepStateCache.isInternal(portName)) {
            LOG.debug("{} Interface is not a internal tunnel I/f, so no-op", portName);
            return;
        }

        LOG.debug("Received NodeConnector Add Event: {}, {}", key, fcNodeConnectorNew);
        if (directTunnelUtils.TUNNEL_PORT_PREDICATE.test(portName) && dpnTepStateCache.isInternal(portName)) {
            //NodeConnectorId nodeConnectorId =
            // InstanceIdentifier.keyOf(key.firstIdentifierOf(NodeConnector.class)).getId();


            InterfaceStateAddWorker ifStateAddWorker = new InterfaceStateAddWorker(key,
                    fcNodeConnectorNew, portName);
            coordinator.enqueueJob(portName, ifStateAddWorker, ITMConstants.JOB_MAX_RETRIES);
        }
    }

    private class InterfaceStateAddWorker implements Callable {
        private final InstanceIdentifier<FlowCapableNodeConnector> key;
        private final FlowCapableNodeConnector fcNodeConnectorNew;
        private final String interfaceName;

        InterfaceStateAddWorker(InstanceIdentifier<FlowCapableNodeConnector> key,
                                FlowCapableNodeConnector fcNodeConnectorNew, String portName) {
            this.key = key;
            this.fcNodeConnectorNew = fcNodeConnectorNew;
            this.interfaceName = portName;
        }

        @Override
        public Object call() throws Exception {
            // If another renderer(for eg : CSS) needs to be supported, check can be performed here
            // to call the respective helpers.
            List<ListenableFuture<Void>> futures = addState(key, interfaceName, fcNodeConnectorNew);
            return futures;
        }

        @Override
        public String toString() {
            return "InterfaceStateAddWorker{fcNodeConnectorIdentifier=" + key + ", fcNodeConnectorNew="
                    + fcNodeConnectorNew + ", interfaceName='" + interfaceName + '\'' + '}';
        }
    }

    private class InterfaceStateUpdateWorker implements Callable {
        private InstanceIdentifier<FlowCapableNodeConnector> key;
        private final FlowCapableNodeConnector fcNodeConnectorOld;
        private final FlowCapableNodeConnector fcNodeConnectorNew;
        private String interfaceName;

        InterfaceStateUpdateWorker(InstanceIdentifier<FlowCapableNodeConnector> key,
                                   FlowCapableNodeConnector fcNodeConnectorOld,
                                   FlowCapableNodeConnector fcNodeConnectorNew,
                                   String portName) {
            this.key = key;
            this.fcNodeConnectorOld = fcNodeConnectorOld;
            this.fcNodeConnectorNew = fcNodeConnectorNew;
            this.interfaceName = portName;
        }

        @Override
        public Object call() throws Exception {
            // If another renderer(for eg : CSS) needs to be supported, check can be performed here
            // to call the respective helpers.
            List<ListenableFuture<Void>> futures = updateState(key, interfaceName,
                    fcNodeConnectorNew, fcNodeConnectorOld);
            return futures;
        }

        @Override
        public String toString() {
            return "InterfaceStateUpdateWorker{key=" + key + ", fcNodeConnectorOld=" + fcNodeConnectorOld
                    + ", fcNodeConnectorNew=" + fcNodeConnectorNew + ", interfaceName='" + interfaceName + '\'' + '}';
        }
    }

    private class InterfaceStateRemoveWorker implements Callable {
        private final NodeConnectorId nodeConnectorIdNew;
        private NodeConnectorId nodeConnectorIdOld;
        FlowCapableNodeConnector fcNodeConnectorOld;
        private final String interfaceName;
        private final String parentInterface;

        InterfaceStateRemoveWorker(NodeConnectorId nodeConnectorIdNew,
                                   NodeConnectorId nodeConnectorIdOld, FlowCapableNodeConnector fcNodeConnectorOld,
                                   String interfaceName, String parentInterface) {
            this.nodeConnectorIdNew = nodeConnectorIdNew;
            this.nodeConnectorIdOld = nodeConnectorIdOld;
            this.fcNodeConnectorOld = fcNodeConnectorOld;
            this.interfaceName = interfaceName;
            this.parentInterface = parentInterface;
        }

        @Override
        public Object call() throws Exception {
            // If another renderer(for eg : CSS) needs to be supported, check can be performed here
            // to call the respective helpers.

            List<ListenableFuture<Void>> futures = null;

            futures = removeInterfaceStateConfiguration(nodeConnectorIdNew, nodeConnectorIdOld, interfaceName,
                    fcNodeConnectorOld, parentInterface);

            return futures;
        }

        @Override
        public String toString() {
            return "InterfaceStateRemoveWorker{nodeConnectorIdNew=" + nodeConnectorIdNew + ", nodeConnectorIdOld="
                    + nodeConnectorIdOld + ", fcNodeConnectorOld=" + fcNodeConnectorOld + ", interfaceName='"
                    + interfaceName + '\'' + '}';
        }
    }

    public List<ListenableFuture<Void>> addState(InstanceIdentifier<FlowCapableNodeConnector> key,
                                                        String interfaceName,
                                                        FlowCapableNodeConnector fcNodeConnectorNew) {
        boolean unableToProcess = false;
        //Retrieve Port No from nodeConnectorId
        NodeConnectorId nodeConnectorId = InstanceIdentifier.keyOf(key.firstIdentifierOf(NodeConnector.class)).getId();
        long portNo = directTunnelUtils.getPortNumberFromNodeConnectorId(nodeConnectorId);
        if (portNo == ITMConstants.INVALID_PORT_NO) {
            LOG.trace("Cannot derive port number, not proceeding with Interface State "
                    + "addition for interface: {}", interfaceName);
            return null;
        }

        LOG.info("adding interface state to Oper DS for interface: {}", interfaceName);
        List<ListenableFuture<Void>> futures = new ArrayList<>();

        Interface.OperStatus operStatus = Interface.OperStatus.Up;
        Interface.AdminStatus adminStatus = Interface.AdminStatus.Up;

        // Fetch the interface/Tunnel from config DS if exists
        TunnelEndPointInfo tunnelEndPointInfo = dpnTepStateCache.getTunnelEndPointInfoFromCache(interfaceName);

        if (tunnelEndPointInfo != null) {
            DpnTepInterfaceInfo dpnTepConfigInfo = dpnTepStateCache.getDpnTepInterface(
                    new BigInteger(tunnelEndPointInfo.getSrcEndPointInfo()),
                    new BigInteger(tunnelEndPointInfo.getDstEndPointInfo()));
            if (dpnTepConfigInfo != null) {
                futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx -> {
                    StateTunnelList stateTnl = addStateEntry(tunnelEndPointInfo, interfaceName,
                            operStatus, adminStatus, nodeConnectorId);

                    // SF419 This will be onl tunnel If so not required
                    // If this interface is a tunnel interface, create the tunnel ingress flow,
                    // and start tunnel monitoring
                    handleTunnelMonitoringAddition(nodeConnectorId, stateTnl.getIfIndex(),
                            dpnTepConfigInfo, interfaceName, portNo);
                }));
            } else {
                LOG.error("DpnTepINfo is NULL while addState for interface {} ", interfaceName);
                unableToProcess = true;
            }
        } else {
            LOG.error("TunnelEndPointInfo is NULL while addState for interface {} ", interfaceName);
            unableToProcess = true;
        }
        if (unableToProcess) {
            LOG.debug(" Unable to process the NodeConnector ADD event for {} as Config not available."
                    + "Hence parking it", interfaceName);
            NodeConnectorInfo nodeConnectorInfo = new NodeConnectorInfoBuilder().setNodeConnectorId(key)
                                     .setNodeConnector(fcNodeConnectorNew).build();
            unprocessedNCCache.add(interfaceName, nodeConnectorInfo);
        }
        return futures;
    }

    public void handleTunnelMonitoringAddition(NodeConnectorId nodeConnectorId,
                                                      Integer ifindex, DpnTepInterfaceInfo dpnTepConfigInfo,
                                                      String interfaceName, long portNo) {
        BigInteger dpId = directTunnelUtils.getDpnFromNodeConnectorId(nodeConnectorId);
        directTunnelUtils.makeTunnelIngressFlow(dpnTepConfigInfo, dpId, portNo, interfaceName,
                ifindex, NwConstants.ADD_FLOW);
    }

    private StateTunnelList addStateEntry(TunnelEndPointInfo tunnelEndPointInfo, String interfaceName,
                                          Interface.OperStatus operStatus, Interface.AdminStatus adminStatus,
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
        final long portNo = directTunnelUtils.getPortNumberFromNodeConnectorId(nodeConnectorId);
        DpnTepInterfaceInfo dpnTepInfo = dpnTepStateCache.getDpnTepInterface(
                       new BigInteger(tunnelEndPointInfo.getSrcEndPointInfo()),
                        new BigInteger(tunnelEndPointInfo.getDstEndPointInfo()));
        LOG.debug("Source Dpn TEP Interface Info {}" , dpnTepInfo);
        tunnelType = dpnTepInfo.getTunnelType();

        final DstInfoBuilder dstInfoBuilder = new DstInfoBuilder();
        SrcInfoBuilder srcInfoBuilder = new SrcInfoBuilder();

        srcInfoBuilder.setTepDeviceId(tunnelEndPointInfo.getSrcEndPointInfo());

        /*DPNTEPsInfo srcDpnTePsInfo = (DPNTEPsInfo) DataStoreCache.get(ITMConstants.DPN_TEPs_Info_CACHE_NAME,
            new BigInteger(tunnelEndPointInfo.getSrcDpnId()));*/

        Optional<DPNTEPsInfo> srcDpnTepsInfo = dpntePsInfoCache.getDPNTepFromDPNId(new BigInteger(tunnelEndPointInfo
                .getSrcEndPointInfo()));

        LOG.debug("Source Dpn TEP Info {}", srcDpnTepsInfo);
        TunnelEndPoints srcEndPtInfo = srcDpnTepsInfo.get().getTunnelEndPoints().get(0);
        srcInfoBuilder.setTepIp(srcEndPtInfo.getIpAddress());
        //As ITM Direct Tunnels deals with only Internal Tunnels.
        //Relook at this when it deals with external as well
        srcInfoBuilder.setTepDeviceType(TepTypeInternal.class);

        /*dstInfoBuilder.setTepDeviceId(tunnelEndPointInfo.getDstDpnId());
        DPNTEPsInfo dstDpnTePsInfo = (DPNTEPsInfo) DataStoreCache.get(ITMConstants.DPN_TEPs_Info_CACHE_NAME,
            new BigInteger(tunnelEndPointInfo.getDstDpnId()));*/

        Optional<DPNTEPsInfo> dstDpnTePsInfo = dpntePsInfoCache.getDPNTepFromDPNId(new BigInteger(tunnelEndPointInfo
                .getDstEndPointInfo()));

        LOG.debug("Dest Dpn TEP Info {}", dstDpnTePsInfo);
        TunnelEndPoints dstEndPtInfo = dstDpnTePsInfo.get().getTunnelEndPoints().get(0);
        dstInfoBuilder.setTepIp(dstEndPtInfo.getIpAddress());
        //As ITM Direct Tunnels deals with only Internal Tunnels.
        //Relook at this when it deals with external as well
        dstInfoBuilder.setTepDeviceType(TepTypeInternal.class);

        // ITM Direct Tunnels NOT SETTING THE TEP TYPe coz its not available. CHECK IF REQUIRED
        TunnelOperStatus tunnelOperStatus = directTunnelUtils.convertInterfaceToTunnelOperState(operStatus);
        boolean tunnelState = (operStatus.equals(Interface.OperStatus.Up)) ? (true) : (false);

        StateTunnelListKey tlKey = new StateTunnelListKey(interfaceName);
        stlBuilder.setKey(tlKey)
                .setOperState(tunnelOperStatus).setTunnelState(tunnelState)
                .setDstInfo(dstInfoBuilder.build()).setSrcInfo(srcInfoBuilder.build()).setTransportType(tunnelType)
                .setPortNumber(String.valueOf(portNo));
        // ITM DIRECT TUnnels CHECK ifIndex is required ??
        ifIndex = directTunnelUtils.allocateId(ITMConstants.ITM_IDPOOL_NAME, interfaceName);
        createLportTagInterfaceMap(interfaceName, ifIndex);

        stlBuilder.setIfIndex(ifIndex);
        InstanceIdentifier<StateTunnelList> stListId = ItmUtils.buildStateTunnelListId(tlKey);
        LOG.trace("Batching the Creation of tunnel_state: {} for Id: {}", stlBuilder.build(), stListId);
        ITMBatchingUtils.write(stListId, stlBuilder.build(), ITMBatchingUtils.EntityType.DEFAULT_OPERATIONAL);

        return stlBuilder.build();
    }

    public void createLportTagInterfaceMap(String infName, Integer ifIndex) {
        LOG.debug("creating lport tag to interface map for {}",infName);
        InstanceIdentifier<IfIndexTunnel> id = InstanceIdentifier.builder(IfIndexesTunnelMap.class)
                .child(IfIndexTunnel.class, new IfIndexTunnelKey(ifIndex)).build();
        IfIndexTunnel ifIndexInterface = new IfIndexTunnelBuilder().setIfIndex(ifIndex)
                .setKey(new IfIndexTunnelKey(ifIndex)).setInterfaceName(infName).build();
        ITMBatchingUtils.write(id, ifIndexInterface, ITMBatchingUtils.EntityType.DEFAULT_OPERATIONAL);
    }

    public List<ListenableFuture<Void>> updateState(InstanceIdentifier<FlowCapableNodeConnector> key,
                                                           String interfaceName,
                                                           FlowCapableNodeConnector flowCapableNodeConnectorNew,
                                                           FlowCapableNodeConnector flowCapableNodeConnectorOld) {
        LOG.debug("Updating interface state for port: {}", interfaceName);

        // SF 419 Hardware updates can be ignored
        Interface.OperStatus operStatusNew = getOpState(flowCapableNodeConnectorNew);
        MacAddress macAddressNew = flowCapableNodeConnectorNew.getHardwareAddress();

        Interface.OperStatus operStatusOld = getOpState(flowCapableNodeConnectorOld);
        MacAddress macAddressOld = flowCapableNodeConnectorOld.getHardwareAddress();

        boolean opstateModified = false;
        boolean hardwareAddressModified = false;
        if (!operStatusNew.equals(operStatusOld)) {
            opstateModified = true;
        }
        if (!macAddressNew.equals(macAddressOld)) {
            hardwareAddressModified = true;
        }

        if (!opstateModified && !hardwareAddressModified) {
            LOG.debug("If State entry for port: {} Not Modified.", interfaceName);
            return Collections.emptyList();
        }

        DpnTepInterfaceInfo dpnTepInfo = dpnTepStateCache.getTunnelFromCache(interfaceName);

        // For monitoring enabled tunnels, skip opstate updation
        if (!modifyTunnelOpState(dpnTepInfo, opstateModified)) {
            LOG.debug("skip Tunnel-state updation for monitoring enabled tunnel interface {}", interfaceName);
            opstateModified = false;
        }

        if (!opstateModified && !hardwareAddressModified) {
            LOG.debug("If State entry for port: {} Not Modified.", interfaceName);
            return Collections.emptyList();
        }
        if (opstateModified) {
            return Collections.singletonList(txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx -> {
                // modify the attributes in interface operational DS
                handleInterfaceStateUpdates(dpnTepInfo, tx, true, interfaceName,
                        flowCapableNodeConnectorNew.getName(), operStatusNew);

            }));
        } else {
            return Collections.singletonList(txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx -> {
                // modify the attributes in interface operational DS
                handleInterfaceStateUpdates(dpnTepInfo, tx, false, interfaceName,
                        flowCapableNodeConnectorNew.getName(), operStatusNew);

            }));
        }
    }

    public void updateInterfaceStateOnNodeRemove(String interfaceName,
                                                        FlowCapableNodeConnector flowCapableNodeConnector,
                                                        WriteTransaction transaction) {
        LOG.debug("Updating interface oper-status to UNKNOWN for : {}", interfaceName);
        DpnTepInterfaceInfo dpnTepInfo = dpnTepStateCache.getTunnelFromCache(interfaceName);

        handleInterfaceStateUpdates(dpnTepInfo,transaction, true, interfaceName, flowCapableNodeConnector.getName(),
                Interface.OperStatus.Unknown);
    }

    public Interface.OperStatus getOpState(FlowCapableNodeConnector flowCapableNodeConnector) {
        Interface.OperStatus operStatus =
                (flowCapableNodeConnector.getState().isLive()
                        && !flowCapableNodeConnector.getConfiguration().isPORTDOWN())
                        ? Interface.OperStatus.Up : Interface.OperStatus.Down;
        return operStatus;
    }

    public void handleInterfaceStateUpdates(DpnTepInterfaceInfo dpnTepInfo,
                                                   WriteTransaction transaction, boolean opStateModified,
                                                   String interfaceName,
                                                   String portName, Interface.OperStatus opState) {
        if (dpnTepInfo == null && !interfaceName.equals(portName)) {
            return;
        }
        LOG.debug("updating interface state entry for {}", interfaceName);
        InstanceIdentifier<StateTunnelList> tnlStateId = ItmUtils.buildStateTunnelListId(
                new StateTunnelListKey(interfaceName));
        StateTunnelListBuilder stateTnlBuilder = new StateTunnelListBuilder();
        stateTnlBuilder.setKey(new StateTunnelListKey(interfaceName));
        if (modifyOpState(dpnTepInfo, opStateModified)) {
            LOG.debug("updating interface oper status as {} for {}", opState.name(), interfaceName);
            boolean tunnelState = (opState.equals(Interface.OperStatus.Up)) ? (true) : (false);
            stateTnlBuilder.setTunnelState(tunnelState);
            stateTnlBuilder.setOperState(directTunnelUtils.convertInterfaceToTunnelOperState(opState));
        }
        transaction.merge(LogicalDatastoreType.OPERATIONAL, tnlStateId, stateTnlBuilder.build(), false);
    }

    public boolean modifyOpState(DpnTepInterfaceInfo dpnTepInterfaceInfo,
                                        boolean opStateModified) {
        return (opStateModified && (dpnTepInterfaceInfo == null || dpnTepInterfaceInfo.isMonitoringEnabled()));
    }

    public boolean modifyTunnelOpState(DpnTepInterfaceInfo dpnTepInterfaceInfo,
                                              boolean opStateModified) {
        if (!dpnTepInterfaceInfo.isMonitoringEnabled()) {
            return modifyOpState(dpnTepInterfaceInfo, opStateModified);
        }
        return false;
    }

    public List<ListenableFuture<Void>> removeInterfaceStateConfiguration(NodeConnectorId nodeConnectorIdNew,
                                                                          NodeConnectorId nodeConnectorIdOld,
                                                                          String interfaceName,
                                                                          FlowCapableNodeConnector
                                                                          fcNodeConnectorOld,
                                                                          String parentInterface) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();

        NodeConnectorId nodeConnectorId = (nodeConnectorIdOld != null && !nodeConnectorIdNew.equals(nodeConnectorIdOld))
                ? nodeConnectorIdOld : nodeConnectorIdNew;

        BigInteger dpId = DirectTunnelUtils.getDpnFromNodeConnectorId(nodeConnectorId);
        futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx -> {
        // In a genuine port delete scenario, the reason will be there in the incoming event, for all remaining
        // cases treat the event as DPN disconnect, if old and new ports are same. Else, this is a VM migration
        // scenario, and should be treated as port removal.
            if (fcNodeConnectorOld.getReason() != PortReason.Delete && nodeConnectorIdNew.equals(nodeConnectorIdOld)) {
                //Remove event is because of connection lost between controller and switch, or switch shutdown.
                // Hence, dont remove the interface but set the status as "unknown"
                updateInterfaceStateOnNodeRemove(interfaceName, fcNodeConnectorOld, tx);
            } else {
                LOG.debug("removing interface state for interface: {}", interfaceName);
                directTunnelUtils.deleteTunnelStateEntry(interfaceName, tx);
                DpnTepInterfaceInfo dpnTepInfo = dpnTepStateCache.getTunnelFromCache(interfaceName);
                if (dpnTepInfo != null) {
                    //SF 419 This will only be tunnel interface
                    directTunnelUtils.removeLportTagInterfaceMap(tx, interfaceName);
                    long portNo = DirectTunnelUtils.getPortNumberFromNodeConnectorId(nodeConnectorId);
                    directTunnelUtils.makeTunnelIngressFlow(dpnTepInfo, dpId, portNo, interfaceName,
                        -1, NwConstants.DEL_FLOW);
                } else {
                    LOG.error("DPNTEPInfo is null for Tunnel Interface {}", interfaceName);
                }
            }
        }));
        return futures;
    }
}
