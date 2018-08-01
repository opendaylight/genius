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
import java.math.BigInteger;
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
import org.opendaylight.genius.itm.cache.OvsBridgeEntryCache;
import org.opendaylight.genius.itm.cache.UnprocessedNodeConnectorCache;
import org.opendaylight.genius.itm.cache.UnprocessedNodeConnectorEndPointCache;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.bridge.tunnel.info.OvsBridgeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.bridge.tunnel.info.OvsBridgeEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.bridge.tunnel.info.OvsBridgeEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.bridge.tunnel.info.ovs.bridge.entry.OvsBridgeTunnelEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.ovs.bridge.ref.info.OvsBridgeRefEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.ovs.bridge.ref.info.OvsBridgeRefEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.ovs.bridge.ref.info.OvsBridgeRefEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TunnelTopologyStateListener extends AbstractTunnelListenerBase<OvsdbBridgeAugmentation> {

    private static final Logger LOG = LoggerFactory.getLogger(TunnelTopologyStateListener.class);

    private final JobCoordinator coordinator;
    private final ManagedNewTransactionRunner txRunner;
    private final DirectTunnelUtils directTunnelUtils;
    private final OvsBridgeEntryCache ovsBridgeEntryCache;

    public TunnelTopologyStateListener(final DataBroker dataBroker,
                                       final JobCoordinator coordinator,
                                       final EntityOwnershipUtils entityOwnershipUtils,
                                       final DirectTunnelUtils directTunnelUtils,
                                       final DpnTepStateCache dpnTepStateCache,
                                       final DPNTEPsInfoCache dpntePsInfoCache,
                                       final OvsBridgeEntryCache ovsBridgeEntryCache,
                                       final UnprocessedNodeConnectorCache unprocessedNodeConnectorCache,
                                       final UnprocessedNodeConnectorEndPointCache
                                               unprocessedNodeConnectorEndPointCache)  {
        super(dataBroker, LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.create(NetworkTopology.class).child(Topology.class).child(Node.class)
                        .augmentation(OvsdbBridgeAugmentation.class), dpnTepStateCache, dpntePsInfoCache,
                unprocessedNodeConnectorCache, unprocessedNodeConnectorEndPointCache,
                entityOwnershipUtils, directTunnelUtils);
        this.coordinator = coordinator;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.directTunnelUtils = directTunnelUtils;
        this.ovsBridgeEntryCache = ovsBridgeEntryCache;
        super.register();
    }

    @Override
    public void remove(@Nonnull InstanceIdentifier<OvsdbBridgeAugmentation> identifier,
                       @Nonnull OvsdbBridgeAugmentation bridgeOld) {
        if (entityOwner()) {
            LOG.debug("Received Remove DataChange Notification for identifier: {}, ovsdbBridgeAugmentation: {}",
                    identifier, bridgeOld);
            TunnelRendererStateRemoveWorker rendererStateRemoveWorker =
                    new TunnelRendererStateRemoveWorker(identifier, bridgeOld);
            coordinator.enqueueJob(bridgeOld.getBridgeName().getValue(), rendererStateRemoveWorker,
                    ITMConstants.JOB_MAX_RETRIES);
        }
    }

    @Override
    public void update(@Nonnull InstanceIdentifier<OvsdbBridgeAugmentation> identifier,
                       @Nonnull OvsdbBridgeAugmentation bridgeOld, @Nonnull OvsdbBridgeAugmentation bridgeNew) {

        if (!entityOwner()) {
            return;
        }
        LOG.debug("Received Update DataChange Notification for identifier: {}, + ovsdbBridgeAugmentation old: {},"
                + " new: {}.", identifier, bridgeOld, bridgeNew);

        DatapathId oldDpid = bridgeOld.getDatapathId();
        DatapathId newDpid = bridgeNew.getDatapathId();
        if (oldDpid == null && newDpid != null) {
            TunnelRendererStateAddWorker rendererStateAddWorker =
                    new TunnelRendererStateAddWorker(identifier, bridgeNew);
            coordinator.enqueueJob(bridgeNew.getBridgeName().getValue(), rendererStateAddWorker,
                    ITMConstants.JOB_MAX_RETRIES);
        } else if (oldDpid != null && !oldDpid.equals(newDpid)) {
            TunnelRendererStateUpdateWorker rendererStateAddWorker =
                    new TunnelRendererStateUpdateWorker(identifier, bridgeNew, bridgeOld);
            coordinator.enqueueJob(bridgeNew.getBridgeName().getValue(), rendererStateAddWorker,
                    ITMConstants.JOB_MAX_RETRIES);
        }
    }

    @Override
    public void add(@Nonnull InstanceIdentifier<OvsdbBridgeAugmentation> identifier,
                    @Nonnull OvsdbBridgeAugmentation bridgeNew) {
        if (entityOwner()) {
            LOG.debug("Received Add DataChange Notification for identifier: {}, ovsdbBridgeAugmentation: {}",
                    identifier, bridgeNew);
            TunnelRendererStateAddWorker rendererStateAddWorker =
                    new TunnelRendererStateAddWorker(identifier, bridgeNew);
            coordinator.enqueueJob(bridgeNew.getBridgeName().getValue(), rendererStateAddWorker,
                    ITMConstants.JOB_MAX_RETRIES);
        }
    }

    /*
     *  This code is used to handle only a dpnId change scenario for a particular change,
     * which is not expected to happen in usual cases.
     */
    private List<ListenableFuture<Void>> updateOvsBridgeRefEntry(InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIid,
                                                                OvsdbBridgeAugmentation bridgeNew,
                                                                OvsdbBridgeAugmentation bridgeOld) {

        return Collections.singletonList(txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx -> {
            BigInteger dpnIdNew = directTunnelUtils.getDpnId(bridgeNew.getDatapathId());
            BigInteger dpnIdOld = directTunnelUtils.getDpnId(bridgeOld.getDatapathId());

            LOG.debug("updating bridge references for bridge: {}, dpnNew: {}, dpnOld: {}", bridgeNew,
                    dpnIdNew, dpnIdOld);
            //delete bridge reference entry for the old dpn in interface meta operational DS
            deleteOvsBridgeRefEntry(dpnIdOld, tx);

            // create bridge reference entry in interface meta operational DS
            createOvsBridgeRefEntry(dpnIdNew, bridgeIid, tx);

            // handle pre-provisioning of tunnels for the newly connected dpn
            Optional<OvsBridgeEntry> bridgeEntry = null;
            try {
                bridgeEntry = ovsBridgeEntryCache.get(dpnIdNew);
                if (bridgeEntry.isPresent()) {
                    addAllPortsToBridge(bridgeEntry.get(), bridgeIid, bridgeNew);
                }
            } catch (ReadFailedException e) {
                LOG.debug("OVSDB Bridge is not present for DPN {}", dpnIdNew);
            }
        }));
    }

    public List<ListenableFuture<Void>> removePortFromBridge(InstanceIdentifier<OvsdbBridgeAugmentation>
                                                                            bridgeIid,
                                                                    OvsdbBridgeAugmentation bridgeOld) {
        BigInteger dpnId = directTunnelUtils.getDpnId(bridgeOld.getDatapathId());
        if (dpnId == null) {
            LOG.warn("Got Null DPID for Bridge: {}", bridgeOld);
            return Collections.emptyList();
        }
        return Collections.singletonList(txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx -> {
            LOG.debug("removing bridge references for bridge: {}, dpn: {}", bridgeOld, dpnId);
            //delete bridge reference entry in interface meta operational DS
            deleteOvsBridgeRefEntry(dpnId, tx);

            // the bridge reference is copied to dpn-tunnel interfaces map, so that whenever a northbound delete
            // happens when bridge is not connected, we need the bridge reference to clean up the topology config DS
            addBridgeRefToBridgeTunnelEntry(dpnId, new OvsdbBridgeRef(bridgeIid), tx);
        }));
    }

    private void createOvsBridgeRefEntry(BigInteger dpnId, InstanceIdentifier<?> bridgeIid,
                                               WriteTransaction tx) {
        LOG.debug("Creating bridge ref entry for dpn: {} bridge: {}",
                dpnId, bridgeIid);
        OvsBridgeRefEntryKey bridgeRefEntryKey = new OvsBridgeRefEntryKey(dpnId);
        InstanceIdentifier<OvsBridgeRefEntry> bridgeEntryId =
                DirectTunnelUtils.getOvsBridgeRefEntryIdentifier(bridgeRefEntryKey);
        OvsBridgeRefEntryBuilder tunnelDpnBridgeEntryBuilder =
                new OvsBridgeRefEntryBuilder().setKey(bridgeRefEntryKey).setDpid(dpnId)
                        .setOvsBridgeReference(new OvsdbBridgeRef(bridgeIid));
        tx.put(LogicalDatastoreType.OPERATIONAL, bridgeEntryId, tunnelDpnBridgeEntryBuilder.build(), true);
    }

    private void deleteOvsBridgeRefEntry(BigInteger dpnId,
                                               WriteTransaction tx) {
        LOG.debug("Deleting bridge ref entry for dpn: {}",
                dpnId);
        OvsBridgeRefEntryKey bridgeRefEntryKey = new OvsBridgeRefEntryKey(dpnId);
        InstanceIdentifier<OvsBridgeRefEntry> bridgeEntryId =
                DirectTunnelUtils.getOvsBridgeRefEntryIdentifier(bridgeRefEntryKey);
        tx.delete(LogicalDatastoreType.OPERATIONAL, bridgeEntryId);
    }

    private void addBridgeRefToBridgeTunnelEntry(BigInteger dpId, OvsdbBridgeRef ovsdbBridgeRef, WriteTransaction tx) {
        OvsBridgeEntryKey bridgeEntryKey = new OvsBridgeEntryKey(dpId);
        InstanceIdentifier<OvsBridgeEntry> bridgeEntryInstanceIdentifier =
                DirectTunnelUtils.getOvsBridgeEntryIdentifier(bridgeEntryKey);

        OvsBridgeEntryBuilder bridgeEntryBuilder = new OvsBridgeEntryBuilder().setKey(bridgeEntryKey)
                .setOvsBridgeReference(ovsdbBridgeRef);
        tx.merge(LogicalDatastoreType.CONFIGURATION, bridgeEntryInstanceIdentifier, bridgeEntryBuilder.build(), true);
    }

    /*
     * Add all tunnels ports corresponding to the bridge to the topology config
     * DS
     */
    private void addAllPortsToBridge(OvsBridgeEntry bridgeEntry, InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIid,
                                     OvsdbBridgeAugmentation bridgeNew) {
        String bridgeName = bridgeNew.getBridgeName().getValue();
        LOG.debug("adding all ports to bridge: {}", bridgeName);
        List<OvsBridgeTunnelEntry> bridgeInterfaceEntries = bridgeEntry.getOvsBridgeTunnelEntry();
        if (bridgeInterfaceEntries != null) {
            for (OvsBridgeTunnelEntry bridgeInterfaceEntry : bridgeInterfaceEntries) {
                String portName = bridgeInterfaceEntry.getTunnelName();
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                        .ietf.interfaces.rev140508.interfaces.Interface iface =
                        dpnTepStateCache.getInterfaceFromCache(portName);
                if (iface != null) {
                    IfTunnel ifTunnel = iface.getAugmentation(IfTunnel.class);
                    if (ifTunnel != null) {
                        directTunnelUtils.addTunnelPortToBridge(ifTunnel, bridgeIid, iface, portName);
                    }
                } else {
                    LOG.debug("Interface {} not found in config DS", portName);
                }
            }
        }
    }

    private class TunnelRendererStateAddWorker implements Callable<List<ListenableFuture<Void>>> {
        private final InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIid;
        private final OvsdbBridgeAugmentation bridgeNew;

        TunnelRendererStateAddWorker(InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIid,
                                     OvsdbBridgeAugmentation bridgeNew) {
            this.bridgeIid = bridgeIid;
            this.bridgeNew = bridgeNew;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            // If another renderer(for eg : OVS) needs to be supported, check can be performed here
            // to call the respective helpers.
            if (bridgeNew.getDatapathId() == null) {
                LOG.info("DataPathId found as null for Bridge Augmentation: {}... returning...", bridgeNew);
                return Collections.emptyList();
            }

            BigInteger dpnId = directTunnelUtils.getDpnId(bridgeNew.getDatapathId());
            LOG.debug("adding bridge references for bridge: {}, dpn: {}", bridgeNew, dpnId);

            // create bridge reference entry in interface meta operational DS
            return Collections.singletonList(txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx -> {
                createOvsBridgeRefEntry(dpnId, bridgeIid, tx);
                // handle pre-provisioning of tunnels for the newly connected dpn
                Optional<OvsBridgeEntry> bridgeEntry = ovsBridgeEntryCache.get(dpnId);
                if (!bridgeEntry.isPresent()) {
                    LOG.debug("Bridge entry not found in config DS for dpn: {}", dpnId);
                } else {
                    addAllPortsToBridge(bridgeEntry.get(), bridgeIid, bridgeNew);
                }
            }));
        }
    }

    private class TunnelRendererStateRemoveWorker implements Callable<List<ListenableFuture<Void>>> {
        private final InstanceIdentifier<OvsdbBridgeAugmentation> instanceIdentifier;
        private final OvsdbBridgeAugmentation bridgeNew;

        TunnelRendererStateRemoveWorker(InstanceIdentifier<OvsdbBridgeAugmentation> instanceIdentifier,
                                        OvsdbBridgeAugmentation bridgeNew) {
            this.instanceIdentifier = instanceIdentifier;
            this.bridgeNew = bridgeNew;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            // If another renderer needs to be supported, check can be performed here
            // to call the respective helpers.
            return removePortFromBridge(instanceIdentifier, bridgeNew);
        }
    }

    private class TunnelRendererStateUpdateWorker implements Callable<List<ListenableFuture<Void>>> {
        private final InstanceIdentifier<OvsdbBridgeAugmentation> instanceIdentifier;
        private final OvsdbBridgeAugmentation bridgeNew;
        private final OvsdbBridgeAugmentation bridgeOld;

        TunnelRendererStateUpdateWorker(InstanceIdentifier<OvsdbBridgeAugmentation> instanceIdentifier,
                                        OvsdbBridgeAugmentation bridgeNew, OvsdbBridgeAugmentation bridgeOld) {
            this.instanceIdentifier = instanceIdentifier;
            this.bridgeNew = bridgeNew;
            this.bridgeOld = bridgeOld;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            // If another renderer(for eg : OVS) needs to be supported, check can be performed here
            // to call the respective helpers.
            return updateOvsBridgeRefEntry(instanceIdentifier, bridgeNew, bridgeOld);
        }
    }
}