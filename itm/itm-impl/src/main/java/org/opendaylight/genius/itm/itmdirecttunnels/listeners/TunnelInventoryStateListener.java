/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.itmdirecttunnels.listeners;

import static org.opendaylight.mdsal.binding.util.Datastore.OPERATIONAL;

import com.google.common.util.concurrent.ListenableFuture;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.cache.DPNTEPsInfoCache;
import org.opendaylight.genius.itm.cache.DpnTepStateCache;
import org.opendaylight.genius.itm.cache.OfDpnTepConfigCache;
import org.opendaylight.genius.itm.cache.TunnelStateCache;
import org.opendaylight.genius.itm.cache.UnprocessedNodeConnectorCache;
import org.opendaylight.genius.itm.cache.UnprocessedNodeConnectorEndPointCache;
import org.opendaylight.genius.itm.cache.UnprocessedOFNodeConnectorCache;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.genius.itm.itmdirecttunnels.workers.OfPortStateAddWorker;
import org.opendaylight.genius.itm.itmdirecttunnels.workers.OfPortStateAddWorkerForNodeConnector;
import org.opendaylight.genius.itm.itmdirecttunnels.workers.TunnelStateAddWorker;
import org.opendaylight.genius.itm.itmdirecttunnels.workers.TunnelStateAddWorkerForNodeConnector;
import org.opendaylight.genius.itm.utils.DpnTepInterfaceInfo;
import org.opendaylight.genius.itm.utils.NodeConnectorInfo;
import org.opendaylight.genius.itm.utils.NodeConnectorInfoBuilder;
import org.opendaylight.genius.itm.utils.TunnelEndPointInfo;
import org.opendaylight.genius.itm.utils.TunnelStateInfo;
import org.opendaylight.genius.itm.utils.TunnelStateInfoBuilder;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.infrautils.utils.concurrent.NamedSimpleReentrantLock.Acquired;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.util.Datastore.Operational;
import org.opendaylight.mdsal.binding.util.ManagedNewTransactionRunner;
import org.opendaylight.mdsal.binding.util.ManagedNewTransactionRunnerImpl;
import org.opendaylight.mdsal.binding.util.TypedWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.serviceutils.tools.listener.AbstractClusteredSyncDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortReason;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.tep.config.OfDpnTep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Class is a Data Change Listener for FlowCapableNodeConnector updates.
 * This creates an entry in the tunnels-state OperDS for every node-connector used.
 */
public class TunnelInventoryStateListener extends
    AbstractClusteredSyncDataTreeChangeListener<FlowCapableNodeConnector> {

    private static final Logger LOG = LoggerFactory.getLogger(TunnelInventoryStateListener.class);
    private static final Logger EVENT_LOGGER = LoggerFactory.getLogger("GeniusEventLogger");

    private final JobCoordinator coordinator;
    private final ManagedNewTransactionRunner txRunner;
    private final TunnelStateCache tunnelStateCache;
    private final DpnTepStateCache dpnTepStateCache;
    private final DPNTEPsInfoCache dpntePsInfoCache;
    private final UnprocessedNodeConnectorCache unprocessedNCCache;
    private final UnprocessedNodeConnectorEndPointCache unprocessedNodeConnectorEndPointCache;
    private final DirectTunnelUtils directTunnelUtils;
    private final ConcurrentMap<String, NodeConnectorInfo> meshedMap = new ConcurrentHashMap<>();
    private final UnprocessedOFNodeConnectorCache unprocessedOFNCCache;
    private final OfDpnTepConfigCache ofDpnTepConfigCache;
    private final IInterfaceManager interfaceManager;

    public TunnelInventoryStateListener(final DataBroker dataBroker,
                                        final JobCoordinator coordinator,
                                        final TunnelStateCache tunnelStateCache,
                                        final DpnTepStateCache dpnTepStateCache,
                                        final DPNTEPsInfoCache dpntePsInfoCache,
                                        final UnprocessedNodeConnectorCache unprocessedNCCache,
                                        final UnprocessedNodeConnectorEndPointCache
                                            unprocessedNodeConnectorEndPointCache,
                                        final DirectTunnelUtils directTunnelUtils,
                                        UnprocessedOFNodeConnectorCache unprocessedOFNCCache,
                                        final OfDpnTepConfigCache ofDpnTepConfigCache,
                                        final IInterfaceManager interfaceManager) {
        super(dataBroker, LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(Nodes.class).child(Node.class)
            .child(NodeConnector.class).augmentation(FlowCapableNodeConnector.class));
        this.coordinator = coordinator;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.tunnelStateCache = tunnelStateCache;
        this.dpnTepStateCache = dpnTepStateCache;
        this.dpntePsInfoCache = dpntePsInfoCache;
        this.unprocessedNCCache = unprocessedNCCache;
        this.unprocessedNodeConnectorEndPointCache = unprocessedNodeConnectorEndPointCache;
        this.directTunnelUtils = directTunnelUtils;
        this.unprocessedOFNCCache = unprocessedOFNCCache;
        this.ofDpnTepConfigCache = ofDpnTepConfigCache;
        this.interfaceManager = interfaceManager;
        super.register();
    }

    @Override
    public void remove(@NonNull InstanceIdentifier<FlowCapableNodeConnector> key,
                       @NonNull FlowCapableNodeConnector flowCapableNodeConnector) {
        String portName = flowCapableNodeConnector.getName();
        LOG.debug("InterfaceInventoryState Remove for {}", portName);
        EVENT_LOGGER.debug("ITM-TunnelInventoryState,REMOVE DTCN received for {}",
                flowCapableNodeConnector.getName());
        // ITM Direct Tunnels Return if its not tunnel port and if its not Internal
        if (!DirectTunnelUtils.TUNNEL_PORT_PREDICATE.test(portName) && !portName.startsWith("of")) {
            LOG.debug("Node Connector Remove - {} Interface is not a tunnel I/f, so no-op", portName);
            return;
        } else {
            try {
                if (DirectTunnelUtils.TUNNEL_PORT_PREDICATE.test(portName)
                        && !tunnelStateCache.isInternalBasedOnState(portName)) {
                    LOG.debug("Node Connector Remove {} Interface is not a internal tunnel I/f, so no-op", portName);
                    return;
                }
            } catch (ReadFailedException e) {
                LOG.error("Tunnel {} is not present in operational DS ", portName);
                return;
            }
        }
        if (!directTunnelUtils.isEntityOwner()) {
            return;
        }
        LOG.debug("Received NodeConnector Remove Event: {}, {}", key, flowCapableNodeConnector);
        NodeConnectorId nodeConnectorId = InstanceIdentifier.keyOf(key.firstIdentifierOf(NodeConnector.class)).getId();
        remove(nodeConnectorId, flowCapableNodeConnector, portName);
    }

    private void remove(NodeConnectorId nodeConnectorId,
                        FlowCapableNodeConnector fcNodeConnectorNew, String portName) {
        LOG.debug("TunnelInventoryState REMOVE for {}", portName);
        LOG.debug("InterfaceInventoryState REMOVE for {}", portName);
        EVENT_LOGGER.debug("ITM-TunnelInventoryState Entity Owner, REMOVE {} {}", nodeConnectorId.getValue(),
                portName);
        TunnelInterfaceStateRemoveWorker portStateRemoveWorker = new TunnelInterfaceStateRemoveWorker(nodeConnectorId,
                fcNodeConnectorNew, portName);
        coordinator.enqueueJob(portName, portStateRemoveWorker, ITMConstants.JOB_MAX_RETRIES);
    }

    @Override
    public void update(@NonNull InstanceIdentifier<FlowCapableNodeConnector> key,
                       @NonNull FlowCapableNodeConnector fcNodeConnectorOld,
                       @NonNull FlowCapableNodeConnector fcNodeConnectorNew) {
        EVENT_LOGGER.debug("ITM-TunnelInventoryState,UPDATE DTCN received for {}", fcNodeConnectorOld.getName());
        String portName = fcNodeConnectorNew.getName();
        if (!DirectTunnelUtils.TUNNEL_PORT_PREDICATE.test(portName) && !portName.startsWith("of")) {
            LOG.debug("Node Connector Update - {} Interface is not a tunnel I/f, so no-op", portName);
            return;
        } else if (!dpnTepStateCache.isInternal(portName)) {
            LOG.debug("Node Connector Update {} Interface is not a internal tunnel I/f, so no-op", portName);
            return;
        }

        if (fcNodeConnectorNew.getReason() == PortReason.Delete || !directTunnelUtils.isEntityOwner()) {
            return;
        }
        LOG.debug("Received NodeConnector Update Event: {}, {}, {}", key, fcNodeConnectorOld, fcNodeConnectorNew);

        TunnelInterfaceStateUpdateWorker portStateUpdateWorker =
                new TunnelInterfaceStateUpdateWorker(key, fcNodeConnectorOld, fcNodeConnectorNew, portName);
        EVENT_LOGGER.debug("ITM-TunnelInventoryState Entity Owner, UPDATE {} {} Reason {}",
                fcNodeConnectorNew.getName(), portName, fcNodeConnectorNew.getReason());
        coordinator.enqueueJob(portName, portStateUpdateWorker, ITMConstants.JOB_MAX_RETRIES);
    }

    @Override
    public void add(@NonNull InstanceIdentifier<FlowCapableNodeConnector> key,
                    @NonNull FlowCapableNodeConnector fcNodeConnectorNew) {
        LOG.info("Received NodeConnector Add Event: {}, {}", key, fcNodeConnectorNew);
        EVENT_LOGGER.debug("ITM-TunnelInventoryState,ADD DTCN received for {}", fcNodeConnectorNew.getName());
        String portName = fcNodeConnectorNew.getName();

        // Return if its not tunnel port and if its not Internal
        if (!DirectTunnelUtils.TUNNEL_PORT_PREDICATE.test(portName) && !portName.startsWith("of")) {
            LOG.debug("Node Connector Add {} Interface is not a tunnel I/f, so no-op", portName);
            return;
        }

        if (!directTunnelUtils.isEntityOwner()) {
            LOG.debug("Not an entity owner.");
            return;
        }

        //Optional<OfDpnTep> dpnTepOptional = Optional.ofNullable(null);
        NodeConnectorInfo nodeConnectorInfo =
                new NodeConnectorInfoBuilder().setNodeConnectorId(key).setNodeConnector(fcNodeConnectorNew).build();

        if (portName.startsWith("of") && interfaceManager.isItmOfTunnelsEnabled()) {
            NodeConnectorId nodeConnectorId = InstanceIdentifier.keyOf(key.firstIdentifierOf(NodeConnector.class))
                    .getId();
            Uint64 srcDpn = DirectTunnelUtils.getDpnFromNodeConnectorId(nodeConnectorId);

            OfDpnTep dpntep = null;
            try (Acquired lock = directTunnelUtils.lockTunnel(portName)) {
                try {
                    Optional<OfDpnTep> dpnTepOptional = ofDpnTepConfigCache.get(srcDpn.toJava());
                    if (!dpnTepOptional.isPresent()) {
                        // Park the notification
                        LOG.debug("Unable to process the NodeConnector ADD event for {} as Config not available."
                                + "Hence parking it", portName);
                        unprocessedOFNCCache.add(portName, nodeConnectorInfo);
                        return;
                    } else {
                        dpntep = dpnTepOptional.get();
                    }
                } catch (ReadFailedException e) {
                    LOG.error("unable to get ofDpnTepConfigCache");
                }
            }

            if (dpntep != null) {
                OfPortStateAddWorkerForNodeConnector ifOfStateAddWorker =
                        new OfPortStateAddWorkerForNodeConnector(new OfPortStateAddWorker(directTunnelUtils,
                                dpntep, txRunner), nodeConnectorInfo);
                EVENT_LOGGER.debug("ITM-Of-tepInventoryState Entity Owner,ADD {} {}",
                        nodeConnectorId.getValue(), portName);
                coordinator.enqueueJob(portName, ifOfStateAddWorker, ITMConstants.JOB_MAX_RETRIES);
            }
        } else {
            addTunnelState(nodeConnectorInfo, portName);
        }
    }

    private void addTunnelState(NodeConnectorInfo nodeConnectorInfo, String portName) {

        TunnelStateInfo tunnelStateInfo = null;
        TunnelEndPointInfo tunnelEndPtInfo = null;
        try (Acquired lock = directTunnelUtils.lockTunnel(portName)) {
            if (!dpnTepStateCache.isConfigAvailable(portName)) {
                // Park the notification
                LOG.debug("Unable to process the NodeConnector ADD event for {} as Config not available."
                    + "Hence parking it", portName);
                unprocessedNCCache.add(portName,
                    new TunnelStateInfoBuilder().setNodeConnectorInfo(nodeConnectorInfo).build());
                return;
            } else if (!dpnTepStateCache.isInternal(portName)) {
                LOG.debug("{} Interface is not a internal tunnel I/f, so no-op", portName);
                return;
            }
        }

        // Check if tunnels State has an entry for this interface.
        // If so, then this Inventory Add is due to compute re-connection. Then, ONLY update the state
        // to UP as previously the compute would have disconnected and so the state will be UNKNOWN.
        try {
            long portNo = tunnelStateCache.getNodeConnectorIdFromInterface(portName);
            if (portNo != ITMConstants.INVALID_PORT_NO) {
                coordinator.enqueueJob(portName,
                        new TunnelInterfaceNodeReconnectWorker(portName), ITMConstants.JOB_MAX_RETRIES);
                return;
            }
        } catch (ReadFailedException e) {
            LOG.error("Exception occurred in reconnect for portName {}, reason: {}.",
                     portName, e.getMessage());
        }

        if (DirectTunnelUtils.TUNNEL_PORT_PREDICATE.test(portName) && dpnTepStateCache.isInternal(portName)) {
            tunnelEndPtInfo = dpnTepStateCache.getTunnelEndPointInfoFromCache(portName);
            TunnelStateInfoBuilder builder = new TunnelStateInfoBuilder().setNodeConnectorInfo(nodeConnectorInfo);
            dpntePsInfoCache.getDPNTepFromDPNId(Uint64.valueOf(tunnelEndPtInfo.getSrcEndPointInfo()))
                .ifPresent(builder::setSrcDpnTepsInfo);
            dpntePsInfoCache.getDPNTepFromDPNId(Uint64.valueOf(tunnelEndPtInfo.getDstEndPointInfo()))
                .ifPresent(builder::setDstDpnTepsInfo);
            tunnelStateInfo = builder.setTunnelEndPointInfo(tunnelEndPtInfo)
                .setDpnTepInterfaceInfo(dpnTepStateCache.getTunnelFromCache(portName)).build();

            if (tunnelStateInfo.getSrcDpnTepsInfo() == null) {
                try (Acquired lock = directTunnelUtils.lockTunnel(tunnelEndPtInfo.getSrcEndPointInfo())) {
                    LOG.debug("Source DPNTepsInfo is null for tunnel {}. Hence Parking with key {}",
                        portName, tunnelEndPtInfo.getSrcEndPointInfo());
                    unprocessedNodeConnectorEndPointCache.add(tunnelEndPtInfo.getSrcEndPointInfo(), tunnelStateInfo);
                }
            }
            if (tunnelStateInfo.getDstDpnTepsInfo() == null) {
                try (Acquired lock = directTunnelUtils.lockTunnel(tunnelEndPtInfo.getDstEndPointInfo())) {
                    LOG.debug("Destination DPNTepsInfo is null for tunnel {}. Hence Parking with key {}",
                        portName, tunnelEndPtInfo.getDstEndPointInfo());
                    unprocessedNodeConnectorEndPointCache.add(tunnelEndPtInfo.getDstEndPointInfo(), tunnelStateInfo);
                }
            }
        }

        if (tunnelEndPtInfo != null && tunnelStateInfo.getSrcDpnTepsInfo() != null
            && tunnelStateInfo.getDstDpnTepsInfo() != null) {
            EVENT_LOGGER.debug("ITM-TunnelInventoryState Entity Owner,ADD {}", portName);
            coordinator.enqueueJob(portName,
                new TunnelStateAddWorkerForNodeConnector(new TunnelStateAddWorker(directTunnelUtils, txRunner),
                    tunnelStateInfo), ITMConstants.JOB_MAX_RETRIES);
        }
    }

    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "https://github.com/spotbugs/spotbugs/issues/811")
    private List<? extends ListenableFuture<?>> updateState(String interfaceName,
        FlowCapableNodeConnector flowCapableNodeConnectorNew,
        FlowCapableNodeConnector flowCapableNodeConnectorOld) {
        LOG.debug("Updating interface state for port: {}", interfaceName);

        // Hardware updates can be ignored
        Interface.OperStatus operStatusNew = getOpState(flowCapableNodeConnectorNew);
        MacAddress macAddressNew = flowCapableNodeConnectorNew.getHardwareAddress();

        Interface.OperStatus operStatusOld = getOpState(flowCapableNodeConnectorOld);
        MacAddress macAddressOld = flowCapableNodeConnectorOld.getHardwareAddress();

        boolean opstateModified = false;
        boolean hardwareAddressModified = false;
        if (!operStatusNew.equals(operStatusOld)) {
            opstateModified = true;
        }
        if (!Objects.equals(macAddressNew, macAddressOld)) {
            hardwareAddressModified = true;
        }

        if (!opstateModified && !hardwareAddressModified) {
            LOG.debug("If State entry for port: {} Not Modified.", interfaceName);
            return Collections.emptyList();
        }

        DpnTepInterfaceInfo dpnTepInfo = dpnTepStateCache.getTunnelFromCache(interfaceName);

        // For monitoring enabled tunnels, skip opstate updation
        if (!modifyTunnelOpState(dpnTepInfo, opstateModified)) {
            LOG.debug("skipping Tunnel-state update for monitoring enabled tunnel interface {}", interfaceName);
            opstateModified = false;
        }

        if (!opstateModified && !hardwareAddressModified) {
            LOG.debug("If State entry for port: {} Not Modified.", interfaceName);
            return Collections.emptyList();
        }
        if (opstateModified) {
            return Collections.singletonList(txRunner.callWithNewWriteOnlyTransactionAndSubmit(OPERATIONAL, tx -> {
                // modify the attributes in interface operational DS
                handleInterfaceStateUpdates(tx, dpnTepInfo, true, interfaceName, flowCapableNodeConnectorNew.getName(),
                        operStatusNew);
                EVENT_LOGGER.debug("ITM-TunnelInventoryState, UPDATE {} CHGED {} completed", interfaceName,
                        operStatusNew.getName());
            }));
        } else {
            return Collections.singletonList(txRunner.callWithNewWriteOnlyTransactionAndSubmit(OPERATIONAL, tx -> {
                // modify the attributes in interface operational DS
                handleInterfaceStateUpdates(tx, dpnTepInfo, false, interfaceName,
                        flowCapableNodeConnectorNew.getName(), operStatusNew);
                EVENT_LOGGER.debug("ITM-TunnelInventoryState, UPDATE {} completed", interfaceName);
            }));
        }
    }

    private void updateInterfaceStateOnNodeRemove(TypedWriteTransaction<Operational> tx, String interfaceName,
        FlowCapableNodeConnector flowCapableNodeConnector) {
        LOG.debug("Updating interface oper-status to UNKNOWN for : {}", interfaceName);
        DpnTepInterfaceInfo dpnTepInfo = dpnTepStateCache.getTunnelFromCache(interfaceName);

        handleInterfaceStateUpdates(tx, dpnTepInfo, true, interfaceName, flowCapableNodeConnector.getName(),
                Interface.OperStatus.Unknown);
    }

    private Interface.OperStatus getOpState(FlowCapableNodeConnector flowCapableNodeConnector) {
        return flowCapableNodeConnector.getState().isLive()
                && !flowCapableNodeConnector.getConfiguration().isPORTDOWN()
                ? Interface.OperStatus.Up : Interface.OperStatus.Down;
    }

    private void handleInterfaceStateUpdates(TypedWriteTransaction<Operational> tx,
        DpnTepInterfaceInfo dpnTepInfo,
        boolean opStateModified, String interfaceName, String portName,
        Interface.OperStatus opState) {
        if (dpnTepInfo == null && !portName.startsWith("of") && !interfaceName.equals(portName)) {
            return;
        }
        LOG.debug("updating interface state entry for {}", interfaceName);
        InstanceIdentifier<StateTunnelList> tnlStateId = ItmUtils.buildStateTunnelListId(
                new StateTunnelListKey(interfaceName));
        StateTunnelListBuilder stateTnlBuilder = new StateTunnelListBuilder();
        stateTnlBuilder.withKey(new StateTunnelListKey(interfaceName));
        if (modifyOpState(dpnTepInfo, opStateModified)) {
            LOG.debug("updating interface oper status as {} for {}", opState.name(), interfaceName);
            boolean tunnelState = opState.equals(Interface.OperStatus.Up);
            stateTnlBuilder.setTunnelState(tunnelState);
            stateTnlBuilder.setOperState(DirectTunnelUtils.convertInterfaceToTunnelOperState(opState));
        }
        tx.merge(tnlStateId, stateTnlBuilder.build());
    }

    private boolean modifyOpState(DpnTepInterfaceInfo dpnTepInterfaceInfo, boolean opStateModified) {
        return opStateModified && dpnTepInterfaceInfo != null;
    }

    private boolean modifyTunnelOpState(DpnTepInterfaceInfo dpnTepInterfaceInfo, boolean opStateModified) {
        return !dpnTepInterfaceInfo.isMonitoringEnabled() && modifyOpState(dpnTepInterfaceInfo, opStateModified);
    }

    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "https://github.com/spotbugs/spotbugs/issues/811")
    private List<? extends ListenableFuture<?>> removeInterfaceStateConfiguration(NodeConnectorId nodeConnectorId,
                                                                                  String interfaceName,
                                                                                  FlowCapableNodeConnector
                                                                                      flowCapableNodeConnector) {


        List<ListenableFuture<?>> futures = new ArrayList<>();
        Uint64 dpId = DirectTunnelUtils.getDpnFromNodeConnectorId(nodeConnectorId);
        // In a genuine port delete scenario, the reason will be there in the incoming event, for all remaining
        // cases treat the event as DPN disconnect, if old and new ports are same. Else, this is a VM migration
        // scenario, and should be treated as port removal.
        if (flowCapableNodeConnector.getReason() != PortReason.Delete) {
            //Remove event is because of connection lost between controller and switch, or switch shutdown.
            // Hence, dont remove the interface but set the status as "unknown"

            if (interfaceName.startsWith("of")) {
                LOG.debug("Received remove state for dpid {}", dpId.intValue());
                for (Map.Entry<String, NodeConnectorInfo> entry : meshedMap.entrySet()) {
                    if (!dpId.toString().equals(entry.getKey())) {
                        String fwdTunnel = dpnTepStateCache.getDpnTepInterface(dpId, Uint64.valueOf(entry.getKey()))
                                .getTunnelName();
                        LOG.debug("Fwd Tunnel name for {} : {} is {}", dpId.intValue(), entry.getKey(), fwdTunnel);
                        futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(OPERATIONAL,
                            tx -> updateInterfaceStateOnNodeRemove(tx, fwdTunnel, flowCapableNodeConnector)));
                        String bwdTunnel = dpnTepStateCache.getDpnTepInterface(Uint64.valueOf(entry.getKey()), dpId)
                                .getTunnelName();
                        LOG.debug("Bwd Tunnel name for {} : {} is {}", entry.getKey(), dpId.intValue(), bwdTunnel);
                        futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(OPERATIONAL,
                            tx -> updateInterfaceStateOnNodeRemove(tx, bwdTunnel,
                                    entry.getValue().getNodeConnector())));
                    }
                }
            } else {
                futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(OPERATIONAL,
                    tx -> updateInterfaceStateOnNodeRemove(tx, interfaceName, flowCapableNodeConnector)));
            }
        } else {
            LOG.debug("removing interface state for interface: {}", interfaceName);
            EVENT_LOGGER.debug("ITM-TunnelInventoryState,REMOVE Table 0 flow for {} completed", interfaceName);
            // removing interfaces are already done in delete worker
            meshedMap.remove(dpId.toString());
        }
        return futures;
    }

    private class TunnelInterfaceStateUpdateWorker implements Callable<List<? extends ListenableFuture<?>>> {
        private final InstanceIdentifier<FlowCapableNodeConnector> key;
        private final FlowCapableNodeConnector fcNodeConnectorOld;
        private final FlowCapableNodeConnector fcNodeConnectorNew;
        private final String interfaceName;

        TunnelInterfaceStateUpdateWorker(InstanceIdentifier<FlowCapableNodeConnector> key,
                                         FlowCapableNodeConnector fcNodeConnectorOld,
                                         FlowCapableNodeConnector fcNodeConnectorNew, String portName) {
            this.key = key;
            this.fcNodeConnectorOld = fcNodeConnectorOld;
            this.fcNodeConnectorNew = fcNodeConnectorNew;
            this.interfaceName = portName;
        }

        @Override
        public List<? extends ListenableFuture<?>> call() {
            // If another renderer(for eg : OVS) needs to be supported, check can be performed here
            // to call the respective helpers.
            return updateState(interfaceName, fcNodeConnectorNew, fcNodeConnectorOld);
        }

        @Override
        public String toString() {
            return "TunnelInterfaceStateUpdateWorker{key=" + key + ", fcNodeConnectorOld=" + fcNodeConnectorOld
                    + ", fcNodeConnectorNew=" + fcNodeConnectorNew + ", interfaceName='" + interfaceName + '\'' + '}';
        }
    }

    private class TunnelInterfaceStateRemoveWorker implements Callable<List<? extends ListenableFuture<?>>> {
        private final NodeConnectorId nodeConnectorId;
        private final FlowCapableNodeConnector flowCapableNodeConnector;
        private final String interfaceName;

        TunnelInterfaceStateRemoveWorker(NodeConnectorId nodeConnectorId,
                                         FlowCapableNodeConnector flowCapableNodeConnector,
                                         String interfaceName) {
            this.nodeConnectorId = nodeConnectorId;
            this.flowCapableNodeConnector = flowCapableNodeConnector;
            this.interfaceName = interfaceName;
        }

        @Override
        public List<? extends ListenableFuture<?>> call() {
            // If another renderer(for eg : OVS) needs to be supported, check can be performed here
            // to call the respective helpers.
            return removeInterfaceStateConfiguration(nodeConnectorId, interfaceName, flowCapableNodeConnector);
        }

        @Override
        public String toString() {
            return "TunnelInterfaceStateRemoveWorker{nodeConnectorId=" + nodeConnectorId + ", fcNodeConnector"
                    + flowCapableNodeConnector + ", interfaceName='" + interfaceName + '\'' + '}';
        }
    }

    private class TunnelInterfaceNodeReconnectWorker implements Callable<List<? extends ListenableFuture<?>>> {
        private final String tunnelName;

        TunnelInterfaceNodeReconnectWorker(String tunnelName) {
            this.tunnelName = tunnelName;
        }

        @Override
        public List<? extends ListenableFuture<?>> call() throws Exception {
            // If another renderer(for eg : OVS) needs to be supported, check can be performed here
            // to call the respective helpers.
            EVENT_LOGGER.debug("ITM-TunnelInventoryState, Compute Re-connected, ADD received for {} ", tunnelName);

            return handleInterfaceStateOnReconnect(tunnelName);
        }

        @Override
        public String toString() {
            return "TunnelInterfaceNodeReconnectWorker{tunnelName=" + tunnelName + '\'' + '}';
        }
    }

    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "https://github.com/spotbugs/spotbugs/issues/811")
    private List<? extends ListenableFuture<?>> handleInterfaceStateOnReconnect(String interfaceName) {
        List<ListenableFuture<?>> futures = new ArrayList<>();

        futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(OPERATIONAL, tx -> {
            DpnTepInterfaceInfo dpnTepInfo = dpnTepStateCache.getTunnelFromCache(interfaceName);

            handleInterfaceStateUpdates(tx, dpnTepInfo, true, interfaceName, interfaceName,
                    Interface.OperStatus.Up);
        }));
        return futures;
    }
}
