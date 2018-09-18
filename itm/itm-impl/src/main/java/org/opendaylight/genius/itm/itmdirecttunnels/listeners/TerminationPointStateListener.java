/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.itmdirecttunnels.listeners;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.itm.cache.BfdStateCache;
import org.opendaylight.genius.itm.cache.DpnTepStateCache;
import org.opendaylight.genius.itm.cache.TunnelStateCache;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.serviceutils.tools.mdsal.listener.AbstractClusteredSyncDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelOperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceBfdStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TerminationPointStateListener
        extends AbstractClusteredSyncDataTreeChangeListener<OvsdbTerminationPointAugmentation> {

    private static final Logger LOG = LoggerFactory.getLogger(TerminationPointStateListener.class);

    private final ManagedNewTransactionRunner txRunner;
    private final EntityOwnershipUtils entityOwnershipUtils;
    private final JobCoordinator coordinator;
    private final BfdStateCache bfdStateCache;
    private final DpnTepStateCache dpnTepStateCache;
    private final TunnelStateCache tunnelStateCache;

    public TerminationPointStateListener(final DataBroker dataBroker, final EntityOwnershipUtils entityOwnershipUtils,
                                         final JobCoordinator coordinator, final BfdStateCache bfdStateCache,
                                         final DpnTepStateCache dpnTepStateCache,
                                         final TunnelStateCache tunnelStateCache) {
        super(dataBroker, LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.create(NetworkTopology.class).child(Topology.class).child(Node.class)
                        .child(TerminationPoint.class).augmentation(OvsdbTerminationPointAugmentation.class));
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.entityOwnershipUtils = entityOwnershipUtils;
        this.coordinator = coordinator;
        this.bfdStateCache = bfdStateCache;
        this.dpnTepStateCache = dpnTepStateCache;
        this.tunnelStateCache = tunnelStateCache;
        super.register();
    }

    @Override
    public void remove(@Nonnull InstanceIdentifier<OvsdbTerminationPointAugmentation> identifier,
                       @Nonnull OvsdbTerminationPointAugmentation tpOld) {
        if (DirectTunnelUtils.TUNNEL_PORT_PREDICATE.test(tpOld.getName())
                && dpnTepStateCache.isInternal(tpOld.getName())) {
            LOG.debug("Received remove DataChange Notification for ovsdb termination point {}", tpOld.getName());
            if (tpOld.getInterfaceBfdStatus() != null) {
                LOG.debug("Received termination point removed notification with bfd status values {}", tpOld.getName());
                RendererTunnelStateRemoveWorker rendererStateRemoveWorker = new RendererTunnelStateRemoveWorker(tpOld);
                coordinator.enqueueJob(tpOld.getName(), rendererStateRemoveWorker);
            }
        }
    }

    @Override
    public void update(@Nonnull InstanceIdentifier<OvsdbTerminationPointAugmentation> identifier,
                       @Nonnull OvsdbTerminationPointAugmentation tpOld,
                       @Nonnull OvsdbTerminationPointAugmentation tpNew) {
        if (DirectTunnelUtils.TUNNEL_PORT_PREDICATE.test(tpNew.getName())
                && dpnTepStateCache.isInternal(tpNew.getName())) {
            LOG.debug("Received Update DataChange Notification for ovsdb termination point {}", tpNew.getName());
            if (DirectTunnelUtils.changeInBfdMonitoringDetected(tpOld, tpNew)
                    || DirectTunnelUtils.ifBfdStatusNotEqual(tpOld, tpNew)) {
                LOG.info("Bfd Status changed for ovsdb termination point identifier: {},  old: {}, new: {}",
                        identifier, tpOld, tpNew);
                RendererTunnelStateUpdateWorker rendererStateAddWorker = new RendererTunnelStateUpdateWorker(tpNew);
                coordinator.enqueueJob(tpNew.getName(), rendererStateAddWorker, ITMConstants.JOB_MAX_RETRIES);
            }
        }
    }

    @Override
    public void add(@Nonnull InstanceIdentifier<OvsdbTerminationPointAugmentation> identifier,
                    @Nonnull OvsdbTerminationPointAugmentation tpNew) {
        if (DirectTunnelUtils.TUNNEL_PORT_PREDICATE.test(tpNew.getName())
                && dpnTepStateCache.isInternal(tpNew.getName())) {
            LOG.debug("Received add DataChange Notification for ovsdb termination point {}", tpNew.getName());
            if (tpNew.getInterfaceBfdStatus() != null  && !tpNew.getInterfaceBfdStatus().isEmpty()) {
                LOG.debug("Received termination point added notification with bfd status values {}", tpNew.getName());
                RendererTunnelStateUpdateWorker rendererStateUpdateWorker = new RendererTunnelStateUpdateWorker(tpNew);
                coordinator.enqueueJob(tpNew.getName(), rendererStateUpdateWorker, ITMConstants.JOB_MAX_RETRIES);
            }
        }
    }

    private List<ListenableFuture<Void>> updateTunnelState(OvsdbTerminationPointAugmentation terminationPointNew) {
        final String interfaceName = terminationPointNew.getName();
        final Interface.OperStatus interfaceBfdStatus = getTunnelOpState(terminationPointNew);
        TunnelOperStatus tunnelState = DirectTunnelUtils.convertInterfaceToTunnelOperState(interfaceBfdStatus);
        bfdStateCache.add(interfaceName, interfaceBfdStatus);
        if (!entityOwnershipUtils.isEntityOwner(ITMConstants.ITM_CONFIG_ENTITY, ITMConstants.ITM_CONFIG_ENTITY)) {
            return Collections.emptyList();
        }

        coordinator.enqueueJob(interfaceName, () -> {
            // update opstate of interface if TEP has gone down/up as a result of BFD monitoring
            Optional<StateTunnelList> stateTnl = tunnelStateCache.get(tunnelStateCache
                    .getStateTunnelListIdentifier(interfaceName));
            if (stateTnl.isPresent() && stateTnl.get().getOperState() != TunnelOperStatus.Unknown
                    && stateTnl.get().getOperState() != tunnelState) {
                LOG.debug("updating tunnel state for interface {} as {}", interfaceName,
                        tunnelState);
                return Collections.singletonList(txRunner.callWithNewWriteOnlyTransactionAndSubmit(
                    tx -> updateOpState(tx, interfaceName, tunnelState)));
            }
            return Collections.emptyList();
        });
        return Collections.emptyList();
    }

    private Interface.OperStatus getTunnelOpState(OvsdbTerminationPointAugmentation terminationPoint) {
        if (!DirectTunnelUtils.bfdMonitoringEnabled(terminationPoint.getInterfaceBfd())) {
            return Interface.OperStatus.Up;
        }
        List<InterfaceBfdStatus> tunnelBfdStatus = terminationPoint.getInterfaceBfdStatus();
        if (tunnelBfdStatus != null && !tunnelBfdStatus.isEmpty()) {
            for (InterfaceBfdStatus bfdState : tunnelBfdStatus) {
                if (bfdState.getBfdStatusKey().equalsIgnoreCase(DirectTunnelUtils.BFD_OP_STATE)) {
                    String bfdOpState = bfdState.getBfdStatusValue();
                    return DirectTunnelUtils.BFD_STATE_UP.equalsIgnoreCase(bfdOpState)
                            ? Interface.OperStatus.Up : Interface.OperStatus.Down;
                }
            }
        }
        return Interface.OperStatus.Down;
    }

    /*
     * update operational state of interface based on events like tunnel
     * monitoring
     */
    private static void updateOpState(WriteTransaction transaction, String interfaceName, TunnelOperStatus operStatus) {
        StateTunnelListKey stateTnlKey = new StateTunnelListKey(interfaceName);
        InstanceIdentifier<StateTunnelList> stateTnlII = ItmUtils.buildStateTunnelListId(stateTnlKey);
        LOG.debug("updating tep interface state as {} for {}", operStatus.name(), interfaceName);
        StateTunnelListBuilder stateTnlBuilder = new StateTunnelListBuilder().setKey(stateTnlKey);
        stateTnlBuilder.setOperState(operStatus);
        transaction.merge(LogicalDatastoreType.OPERATIONAL, stateTnlII, stateTnlBuilder.build(), false);
    }

    private class RendererTunnelStateUpdateWorker implements Callable<List<ListenableFuture<Void>>> {
        private final OvsdbTerminationPointAugmentation terminationPointNew;

        RendererTunnelStateUpdateWorker(OvsdbTerminationPointAugmentation tpNew) {
            this.terminationPointNew = tpNew;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            // If another renderer(for eg : OVS) needs to be supported, check can be performed here
            // to call the respective helpers.
            return updateTunnelState(terminationPointNew);
        }
    }

    private class RendererTunnelStateRemoveWorker implements Callable<List<ListenableFuture<Void>>> {
        private final OvsdbTerminationPointAugmentation terminationPointOld;

        RendererTunnelStateRemoveWorker(OvsdbTerminationPointAugmentation tpNew) {
            this.terminationPointOld = tpNew;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            LOG.debug("Removing bfd state from cache, if any, for {}", terminationPointOld.getName());
            bfdStateCache.remove(terminationPointOld.getName());
            return Collections.emptyList();
        }
    }
}
