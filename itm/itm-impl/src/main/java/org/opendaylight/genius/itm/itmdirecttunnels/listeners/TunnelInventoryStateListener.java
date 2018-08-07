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
import java.util.concurrent.Callable;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.itm.cache.DPNTEPsInfoCache;
import org.opendaylight.genius.itm.cache.DpnTepStateCache;
import org.opendaylight.genius.itm.cache.TunnelStateCache;
import org.opendaylight.genius.itm.cache.UnprocessedNodeConnectorCache;
import org.opendaylight.genius.itm.cache.UnprocessedNodeConnectorEndPointCache;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.genius.itm.utils.DpnTepInterfaceInfo;
import org.opendaylight.genius.itm.utils.NodeConnectorInfo;
import org.opendaylight.genius.itm.utils.NodeConnectorInfoBuilder;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortReason;
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
public class TunnelInventoryStateListener extends AbstractTunnelListenerBase<FlowCapableNodeConnector> {

    private static final Logger LOG = LoggerFactory.getLogger(TunnelInventoryStateListener.class);

    private final JobCoordinator coordinator;
    private final ManagedNewTransactionRunner txRunner;
    private final TunnelStateCache tunnelStateCache;

    public TunnelInventoryStateListener(final DataBroker dataBroker,
                                        final JobCoordinator coordinator,
                                        final EntityOwnershipUtils entityOwnershipUtils,
                                        final TunnelStateCache tunnelStateCache,
                                        final DpnTepStateCache dpnTepStateCache,
                                        final DPNTEPsInfoCache dpntePsInfoCache,
                                        final UnprocessedNodeConnectorCache unprocessedNCCache,
                                        final UnprocessedNodeConnectorEndPointCache
                                                unprocessedNodeConnectorEndPointCache,
                                        final DirectTunnelUtils directTunnelUtils) {
        super(dataBroker, LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(Nodes.class).child(Node.class)
                .child(NodeConnector.class).augmentation(FlowCapableNodeConnector.class), dpnTepStateCache,
                dpntePsInfoCache, unprocessedNCCache,
                unprocessedNodeConnectorEndPointCache, entityOwnershipUtils, directTunnelUtils);
        this.coordinator = coordinator;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.tunnelStateCache = tunnelStateCache;
        super.register();
    }

    @Override
    public void remove(@Nonnull InstanceIdentifier<FlowCapableNodeConnector> key,
                       @Nonnull FlowCapableNodeConnector flowCapableNodeConnectorOld) {
        String portName = flowCapableNodeConnectorOld.getName();
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
        if (!entityOwner()) {
            return;
        }
        LOG.debug("Received NodeConnector Remove Event: {}, {}", key, flowCapableNodeConnectorOld);
        NodeConnectorId nodeConnectorId = InstanceIdentifier.keyOf(key.firstIdentifierOf(NodeConnector.class)).getId();
        remove(nodeConnectorId, null, flowCapableNodeConnectorOld, portName);
    }

    private void remove(NodeConnectorId nodeConnectorIdNew, NodeConnectorId nodeConnectorIdOld,
                        FlowCapableNodeConnector fcNodeConnectorNew, String portName) {
        LOG.debug("InterfaceInventoryState REMOVE for {}", portName);
        TunnelInterfaceStateRemoveWorker portStateRemoveWorker = new TunnelInterfaceStateRemoveWorker(
                nodeConnectorIdNew, nodeConnectorIdOld, fcNodeConnectorNew, portName, portName);
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
        if (fcNodeConnectorNew.getReason() == PortReason.Delete || !entityOwner()) {
            return;
        }
        LOG.debug("Received NodeConnector Update Event: {}, {}, {}", key, fcNodeConnectorOld, fcNodeConnectorNew);

        TunnelInterfaceStateUpdateWorker portStateUpdateWorker = new TunnelInterfaceStateUpdateWorker(key,
                fcNodeConnectorOld, fcNodeConnectorNew, portName);
        coordinator.enqueueJob(portName, portStateUpdateWorker, ITMConstants.JOB_MAX_RETRIES);
    }

    @Override
    public void add(@Nonnull InstanceIdentifier<FlowCapableNodeConnector> key,
                    @Nonnull FlowCapableNodeConnector fcNodeConnectorNew) {
        String portName = fcNodeConnectorNew.getName();
        LOG.debug("InterfaceInventoryState ADD for {}", portName);
        // Return if its not tunnel port and if its not Internal
        if (!DirectTunnelUtils.TUNNEL_PORT_PREDICATE.test(portName)) {
            LOG.debug("Node Connector Add {} Interface is not a tunnel I/f, so no-op", portName);
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
        if (DirectTunnelUtils.TUNNEL_PORT_PREDICATE.test(portName) && dpnTepStateCache.isInternal(portName)) {
            //NodeConnectorId nodeConnectorId =
            // InstanceIdentifier.keyOf(key.firstIdentifierOf(NodeConnector.class)).getId();
            InterfaceStateAddWorker ifStateAddWorker = new InterfaceStateAddWorker(key,
                    fcNodeConnectorNew, portName);
            coordinator.enqueueJob(portName, ifStateAddWorker, ITMConstants.JOB_MAX_RETRIES);
        }
    }

    private List<ListenableFuture<Void>> updateState(InstanceIdentifier<FlowCapableNodeConnector> key,
                                                     String interfaceName,
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
            LOG.debug("skipping Tunnel-state update for monitoring enabled tunnel interface {}", interfaceName);
            opstateModified = false;
        }

        if (!opstateModified && !hardwareAddressModified) {
            LOG.debug("If State entry for port: {} Not Modified.", interfaceName);
            return Collections.emptyList();
        }
        if (opstateModified) {
            return Collections.singletonList(txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx -> {
                // modify the attributes in interface operational DS
                handleInterfaceStateUpdates(dpnTepInfo, tx, true, interfaceName, flowCapableNodeConnectorNew.getName(),
                        operStatusNew);

            }));
        } else {
            return Collections.singletonList(txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx -> {
                // modify the attributes in interface operational DS
                handleInterfaceStateUpdates(dpnTepInfo, tx, false, interfaceName,
                        flowCapableNodeConnectorNew.getName(), operStatusNew);

            }));
        }
    }

    private void updateInterfaceStateOnNodeRemove(String interfaceName,
                                                  FlowCapableNodeConnector flowCapableNodeConnector,
                                                  WriteTransaction transaction) {
        LOG.debug("Updating interface oper-status to UNKNOWN for : {}", interfaceName);
        DpnTepInterfaceInfo dpnTepInfo = dpnTepStateCache.getTunnelFromCache(interfaceName);

        handleInterfaceStateUpdates(dpnTepInfo, transaction, true, interfaceName, flowCapableNodeConnector.getName(),
                Interface.OperStatus.Unknown);
    }

    private Interface.OperStatus getOpState(FlowCapableNodeConnector flowCapableNodeConnector) {
        return flowCapableNodeConnector.getState().isLive()
                && !flowCapableNodeConnector.getConfiguration().isPORTDOWN()
                ? Interface.OperStatus.Up : Interface.OperStatus.Down;
    }

    private void handleInterfaceStateUpdates(DpnTepInterfaceInfo dpnTepInfo, WriteTransaction transaction,
                                             boolean opStateModified, String interfaceName, String portName,
                                             Interface.OperStatus opState) {
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
            boolean tunnelState = opState.equals(Interface.OperStatus.Up);
            stateTnlBuilder.setTunnelState(tunnelState);
            stateTnlBuilder.setOperState(DirectTunnelUtils.convertInterfaceToTunnelOperState(opState));
        }
        transaction.merge(LogicalDatastoreType.OPERATIONAL, tnlStateId, stateTnlBuilder.build(), false);
    }

    private boolean modifyOpState(DpnTepInterfaceInfo dpnTepInterfaceInfo, boolean opStateModified) {
        return opStateModified && (dpnTepInterfaceInfo != null);
    }

    private boolean modifyTunnelOpState(DpnTepInterfaceInfo dpnTepInterfaceInfo, boolean opStateModified) {
        return !dpnTepInterfaceInfo.isMonitoringEnabled() && modifyOpState(dpnTepInterfaceInfo, opStateModified);
    }

    private List<ListenableFuture<Void>> removeInterfaceStateConfiguration(NodeConnectorId nodeConnectorIdNew,
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
            if (fcNodeConnectorOld.getReason() != PortReason.Delete) {
                //Remove event is because of connection lost between controller and switch, or switch shutdown.
                // Hence, dont remove the interface but set the status as "unknown"
                updateInterfaceStateOnNodeRemove(interfaceName, fcNodeConnectorOld, tx);
            } else {
                LOG.debug("removing interface state for interface: {}", interfaceName);
                directTunnelUtils.deleteTunnelStateEntry(interfaceName);
                DpnTepInterfaceInfo dpnTepInfo = dpnTepStateCache.getTunnelFromCache(interfaceName);
                if (dpnTepInfo != null) {
                    //SF 419 This will only be tunnel interface
                    directTunnelUtils.removeLportTagInterfaceMap(interfaceName);
                    long portNo = DirectTunnelUtils.getPortNumberFromNodeConnectorId(nodeConnectorId);
                    directTunnelUtils.makeTunnelIngressFlow(dpnTepInfo, dpId, portNo, interfaceName, -1,
                            NwConstants.DEL_FLOW);
                } else {
                    LOG.error("DPNTEPInfo is null for Tunnel Interface {}", interfaceName);
                }
            }
        }));
        return futures;
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
            // If another renderer(for eg : OVS) needs to be supported, check can be performed here
            // to call the respective helpers.
            return addState(key, interfaceName, fcNodeConnectorNew);
        }

        @Override
        public String toString() {
            return "InterfaceStateAddWorker{fcNodeConnectorIdentifier=" + key + ", fcNodeConnectorNew="
                    + fcNodeConnectorNew + ", interfaceName='" + interfaceName + '\'' + '}';
        }
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
        public Object call() throws Exception {
            // If another renderer(for eg : OVS) needs to be supported, check can be performed here
            // to call the respective helpers.
            return updateState(key, interfaceName, fcNodeConnectorNew, fcNodeConnectorOld);
        }

        @Override
        public String toString() {
            return "TunnelInterfaceStateUpdateWorker{key=" + key + ", fcNodeConnectorOld=" + fcNodeConnectorOld
                    + ", fcNodeConnectorNew=" + fcNodeConnectorNew + ", interfaceName='" + interfaceName + '\'' + '}';
        }
    }

    private class TunnelInterfaceStateRemoveWorker implements Callable {
        private final NodeConnectorId nodeConnectorIdNew;
        private final NodeConnectorId nodeConnectorIdOld;
        private final FlowCapableNodeConnector fcNodeConnectorOld;
        private final String interfaceName;
        private final String parentInterface;

        TunnelInterfaceStateRemoveWorker(NodeConnectorId nodeConnectorIdNew,
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
            // If another renderer(for eg : OVS) needs to be supported, check can be performed here
            // to call the respective helpers.
            return removeInterfaceStateConfiguration(nodeConnectorIdNew, nodeConnectorIdOld, interfaceName,
                    fcNodeConnectorOld, parentInterface);
        }

        @Override
        public String toString() {
            return "TunnelInterfaceStateRemoveWorker{nodeConnectorIdNew=" + nodeConnectorIdNew + ", nodeConnectorIdOld="
                    + nodeConnectorIdOld + ", fcNodeConnectorOld=" + fcNodeConnectorOld + ", interfaceName='"
                    + interfaceName + '\'' + '}';
        }
    }
}
