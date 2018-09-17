/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.itmdirecttunnels.listeners;

import static org.opendaylight.genius.infra.Datastore.CONFIGURATION;
import static org.opendaylight.genius.infra.Datastore.OPERATIONAL;

import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.infra.Datastore.Operational;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.infra.TypedWriteTransaction;
import org.opendaylight.genius.itm.cache.DPNTEPsInfoCache;
import org.opendaylight.genius.itm.cache.DpnTepStateCache;
import org.opendaylight.genius.itm.cache.TunnelStateCache;
import org.opendaylight.genius.itm.cache.UnprocessedNodeConnectorCache;
import org.opendaylight.genius.itm.cache.UnprocessedNodeConnectorEndPointCache;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.genius.itm.itmdirecttunnels.workers.TunnelStateAddWorker;
import org.opendaylight.genius.itm.itmdirecttunnels.workers.TunnelStateAddWorkerForNodeConnector;
import org.opendaylight.genius.itm.utils.DpnTepInterfaceInfo;
import org.opendaylight.genius.itm.utils.NodeConnectorInfo;
import org.opendaylight.genius.itm.utils.NodeConnectorInfoBuilder;
import org.opendaylight.genius.itm.utils.TunnelEndPointInfo;
import org.opendaylight.genius.itm.utils.TunnelStateInfo;
import org.opendaylight.genius.itm.utils.TunnelStateInfoBuilder;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.serviceutils.tools.mdsal.listener.AbstractClusteredSyncDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortReason;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelListKey;
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

    private final JobCoordinator coordinator;
    private final ManagedNewTransactionRunner txRunner;
    private final TunnelStateCache tunnelStateCache;
    private final DpnTepStateCache dpnTepStateCache;
    private final DPNTEPsInfoCache dpntePsInfoCache;
    private final UnprocessedNodeConnectorCache unprocessedNCCache;
    private final UnprocessedNodeConnectorEndPointCache unprocessedNodeConnectorEndPointCache;
    private final DirectTunnelUtils directTunnelUtils;

    public TunnelInventoryStateListener(final DataBroker dataBroker,
                                        final JobCoordinator coordinator,
                                        final TunnelStateCache tunnelStateCache,
                                        final DpnTepStateCache dpnTepStateCache,
                                        final DPNTEPsInfoCache dpntePsInfoCache,
                                        final UnprocessedNodeConnectorCache unprocessedNCCache,
                                        final UnprocessedNodeConnectorEndPointCache
                                            unprocessedNodeConnectorEndPointCache,
                                        final DirectTunnelUtils directTunnelUtils) {
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
        super.register();
    }

    @Override
    public void remove(@Nonnull InstanceIdentifier<FlowCapableNodeConnector> key,
                       @Nonnull FlowCapableNodeConnector flowCapableNodeConnector) {
        String portName = flowCapableNodeConnector.getName();
        LOG.debug("InterfaceInventoryState Remove for {}", portName);
        // ITM Direct Tunnels Return if its not tunnel port and if its not Internal
        if (!DirectTunnelUtils.TUNNEL_PORT_PREDICATE.test(portName)) {
            LOG.debug("Node Connector Remove - {} Interface is not a tunnel I/f, so no-op", portName);
            return;
        } else {
            try {
                if (!tunnelStateCache.isInternalBasedOnState(portName)) {
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
        LOG.debug("InterfaceInventoryState REMOVE for {}", portName);
        TunnelInterfaceStateRemoveWorker portStateRemoveWorker = new TunnelInterfaceStateRemoveWorker(nodeConnectorId,
                fcNodeConnectorNew, portName);
        coordinator.enqueueJob(portName, portStateRemoveWorker, ITMConstants.JOB_MAX_RETRIES);
    }

    @Override
    public void update(@Nonnull InstanceIdentifier<FlowCapableNodeConnector> key,
                       @Nonnull FlowCapableNodeConnector fcNodeConnectorOld,
                       @Nonnull FlowCapableNodeConnector fcNodeConnectorNew) {
        String portName = fcNodeConnectorNew.getName();
        if (DirectTunnelUtils.TUNNEL_PORT_PREDICATE.test(portName)) {
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
        coordinator.enqueueJob(portName, portStateUpdateWorker, ITMConstants.JOB_MAX_RETRIES);
    }

    @Override
    public void add(@Nonnull InstanceIdentifier<FlowCapableNodeConnector> key,
                    @Nonnull FlowCapableNodeConnector fcNodeConnectorNew) {
        LOG.info("Received NodeConnector Add Event: {}, {}", key, fcNodeConnectorNew);
        String portName = fcNodeConnectorNew.getName();
        NodeConnectorInfo nodeConnectorInfo = new NodeConnectorInfoBuilder().setNodeConnectorId(key)
            .setNodeConnector(fcNodeConnectorNew).build();
        TunnelStateInfo tunnelStateInfo = null;
        DpnTepInterfaceInfo dpnTepInfo = null;
        TunnelEndPointInfo tunnelEndPtInfo = null;
        DPNTEPsInfo srcDpnTepsInfo = null;
        DPNTEPsInfo dstDpnTepsInfo = null;
        // Return if its not tunnel port and if its not Internal
        if (!DirectTunnelUtils.TUNNEL_PORT_PREDICATE.test(portName)) {
            LOG.debug("Node Connector Add {} Interface is not a tunnel I/f, so no-op", portName);
            return;
        }
        try {
            directTunnelUtils.getTunnelLocks().lock(portName);
            if (!dpnTepStateCache.isConfigAvailable(portName)) {
                // Park the notification
                LOG.info("Unable to process the NodeConnector ADD event for {} as Config not available."
                    + "Hence parking it", portName);
                unprocessedNCCache.add(portName,
                    new TunnelStateInfoBuilder().setNodeConnectorInfo(nodeConnectorInfo).build());
                return;
            } else if (!dpnTepStateCache.isInternal(portName)) {
                LOG.debug("{} Interface is not a internal tunnel I/f, so no-op", portName);
                return;
            }
        } finally {
            directTunnelUtils.getTunnelLocks().unlock(portName);
        }

        if (DirectTunnelUtils.TUNNEL_PORT_PREDICATE.test(portName) && dpnTepStateCache.isInternal(portName)) {
            tunnelEndPtInfo = dpnTepStateCache.getTunnelEndPointInfoFromCache(portName);
            TunnelStateInfoBuilder builder = new TunnelStateInfoBuilder().setNodeConnectorInfo(nodeConnectorInfo);
            dpntePsInfoCache.getDPNTepFromDPNId(new BigInteger(tunnelEndPtInfo.getSrcEndPointInfo()))
                .ifPresent(builder::setSrcDpnTepsInfo);
            dpntePsInfoCache.getDPNTepFromDPNId(new BigInteger(tunnelEndPtInfo.getDstEndPointInfo()))
                .ifPresent(builder::setDstDpnTepsInfo);
            tunnelStateInfo = builder.setTunnelEndPointInfo(tunnelEndPtInfo)
                .setDpnTepInterfaceInfo(dpnTepStateCache.getTunnelFromCache(portName)).build();

            if (tunnelStateInfo.getSrcDpnTepsInfo() == null) {
                directTunnelUtils.getTunnelLocks().lock(tunnelEndPtInfo.getSrcEndPointInfo());
                LOG.info("Source DPNTepsInfo is null for tunnel {}. Hence Parking with key {}",
                        portName, tunnelEndPtInfo.getSrcEndPointInfo());
                unprocessedNodeConnectorEndPointCache.add(tunnelEndPtInfo.getSrcEndPointInfo(), tunnelStateInfo);
                directTunnelUtils.getTunnelLocks().unlock(tunnelEndPtInfo.getSrcEndPointInfo());
            }
            if (tunnelStateInfo.getDstDpnTepsInfo() == null) {
                directTunnelUtils.getTunnelLocks().lock(tunnelEndPtInfo.getDstEndPointInfo());
                LOG.info("Destination DPNTepsInfo is null for tunnel {}. Hence Parking with key {}",
                        portName, tunnelEndPtInfo.getDstEndPointInfo());
                unprocessedNodeConnectorEndPointCache.add(tunnelEndPtInfo.getDstEndPointInfo(), tunnelStateInfo);
                directTunnelUtils.getTunnelLocks().unlock(tunnelEndPtInfo.getDstEndPointInfo());
            }
        }

        if (tunnelEndPtInfo != null && tunnelStateInfo.getSrcDpnTepsInfo() != null
            && tunnelStateInfo.getDstDpnTepsInfo() != null && directTunnelUtils.isEntityOwner()) {
            coordinator.enqueueJob(portName,
                new TunnelStateAddWorkerForNodeConnector(new TunnelStateAddWorker(directTunnelUtils, txRunner),
                    tunnelStateInfo), ITMConstants.JOB_MAX_RETRIES);
        }
    }

    private List<ListenableFuture<Void>> updateState(String interfaceName,
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

            }));
        } else {
            return Collections.singletonList(txRunner.callWithNewWriteOnlyTransactionAndSubmit(OPERATIONAL, tx -> {
                // modify the attributes in interface operational DS
                handleInterfaceStateUpdates(tx, dpnTepInfo, false, interfaceName,
                        flowCapableNodeConnectorNew.getName(), operStatusNew);

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
        if (dpnTepInfo == null && !interfaceName.equals(portName)) {
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
        return opStateModified && (dpnTepInterfaceInfo != null);
    }

    private boolean modifyTunnelOpState(DpnTepInterfaceInfo dpnTepInterfaceInfo, boolean opStateModified) {
        return !dpnTepInterfaceInfo.isMonitoringEnabled() && modifyOpState(dpnTepInterfaceInfo, opStateModified);
    }

    private List<ListenableFuture<Void>> removeInterfaceStateConfiguration(NodeConnectorId nodeConnectorId,
                                                                           String interfaceName,
                                                                           FlowCapableNodeConnector
                                                                                   flowCapableNodeConnector) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();

        BigInteger dpId = DirectTunnelUtils.getDpnFromNodeConnectorId(nodeConnectorId);
        // In a genuine port delete scenario, the reason will be there in the incoming event, for all remaining
        // cases treat the event as DPN disconnect, if old and new ports are same. Else, this is a VM migration
        // scenario, and should be treated as port removal.
        if (flowCapableNodeConnector.getReason() != PortReason.Delete) {
            //Remove event is because of connection lost between controller and switch, or switch shutdown.
            // Hence, dont remove the interface but set the status as "unknown"
            futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(OPERATIONAL,
                tx -> updateInterfaceStateOnNodeRemove(tx, interfaceName, flowCapableNodeConnector)));
        } else {
            LOG.debug("removing interface state for interface: {}", interfaceName);
            directTunnelUtils.deleteTunnelStateEntry(interfaceName);
            futures.add(txRunner.callWithNewReadWriteTransactionAndSubmit(CONFIGURATION, tx -> {
                directTunnelUtils.removeLportTagInterfaceMap(interfaceName);
                directTunnelUtils.removeTunnelIngressFlow(tx, dpId, interfaceName);
            }));
        }
        return futures;
    }

    private class TunnelInterfaceStateUpdateWorker implements Callable {
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
        public Object call() {
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

    private class TunnelInterfaceStateRemoveWorker implements Callable {
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
        public Object call() {
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

/*    private void addTepInfoToUnprocessedCache(String portName, TunnelEndPointInfo tunnelEndPointInfo,
                                              TunnelStateInfo tunnelStateInfo, String srcDst) {
        directTunnelUtils.getTunnelLocks().lock(tunnelEndPointInfo.getSrcEndPointInfo());
        LOG.debug("{} DPNTepsInfo is null for tunnel {}. Hence Parking with key {}", srcDst, portName,
            tunnelEndPointInfo.getSrcEndPointInfo());
        unprocessedNodeConnectorEndPointCache.add(tunnelEndPointInfo.getSrcEndPointInfo(), tunnelStateInfo);
        directTunnelUtils.getTunnelLocks().unlock(tunnelEndPointInfo.getSrcEndPointInfo());
    }*/
}
