/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.itmdirecttunnels.listeners;

import java.util.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.itm.cache.DpnTepStateCache;
import org.opendaylight.genius.itm.cache.OvsBridgeEntryCache;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.serviceutils.tools.mdsal.listener.AbstractClusteredSyncDataTreeChangeListener;
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
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TunnelTopologyStateListener extends AbstractClusteredSyncDataTreeChangeListener<OvsdbBridgeAugmentation> {

    private static final Logger LOG = LoggerFactory.getLogger(TunnelTopologyStateListener.class);
    private static final Logger EVENT_LOGGER = LoggerFactory.getLogger("GeniusEventLogger");

    private final JobCoordinator coordinator;
    private final ManagedNewTransactionRunner txRunner;
    private final DirectTunnelUtils directTunnelUtils;
    private final OvsBridgeEntryCache ovsBridgeEntryCache;
    protected final DpnTepStateCache dpnTepStateCache;

    public TunnelTopologyStateListener(final DataBroker dataBroker,
                                       final JobCoordinator coordinator,
                                       final DirectTunnelUtils directTunnelUtils,
                                       final DpnTepStateCache dpnTepStateCache,
                                       final OvsBridgeEntryCache ovsBridgeEntryCache)  {
        super(dataBroker, LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.create(NetworkTopology.class).child(Topology.class).child(Node.class)
                        .augmentation(OvsdbBridgeAugmentation.class));
        this.coordinator = coordinator;
        this.dpnTepStateCache = dpnTepStateCache;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.directTunnelUtils = directTunnelUtils;
        this.ovsBridgeEntryCache = ovsBridgeEntryCache;
        super.register();
    }

    @Override
    public void remove(@NonNull InstanceIdentifier<OvsdbBridgeAugmentation> identifier,
                       @NonNull OvsdbBridgeAugmentation bridgeOld) {
        EVENT_LOGGER.debug("ITM-TunnelTopologyState, REMOVE DTCN received");
        if (directTunnelUtils.isEntityOwner()) {
            LOG.debug("Received Remove DataChange Notification for identifier: {}, ovsdbBridgeAugmentation: {}",
                    identifier, bridgeOld);
            TunnelRendererStateRemoveWorker rendererStateRemoveWorker =
                    new TunnelRendererStateRemoveWorker(identifier, bridgeOld);
            coordinator.enqueueJob(bridgeOld.getBridgeName().getValue(), rendererStateRemoveWorker,
                    ITMConstants.JOB_MAX_RETRIES);
        }
    }

    @Override
    public void update(@NonNull InstanceIdentifier<OvsdbBridgeAugmentation> identifier,
                       @NonNull OvsdbBridgeAugmentation bridgeOld, @NonNull OvsdbBridgeAugmentation bridgeNew) {
        EVENT_LOGGER.debug("ITM-TunnelTopologyState, UPDATE DTCN received");

        if (!directTunnelUtils.isEntityOwner()) {
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
    public void add(@NonNull InstanceIdentifier<OvsdbBridgeAugmentation> identifier,
                    @NonNull OvsdbBridgeAugmentation bridgeNew) {
        EVENT_LOGGER.debug("ITM-TunnelTopologyState, ADD DTCN received");
        if (directTunnelUtils.isEntityOwner()) {
            LOG.debug("Received Add DataChange Notification for identifier: {}, ovsdbBridgeAugmentation: {}",
                    identifier, bridgeNew);
            TunnelRendererStateAddWorker rendererStateAddWorker =
                    new TunnelRendererStateAddWorker(identifier, bridgeNew);
            coordinator.enqueueJob(bridgeNew.getBridgeName().getValue(), rendererStateAddWorker,
                    ITMConstants.JOB_MAX_RETRIES);
        }
    }

    /*
     * This code is used to handle only a dpnId change scenario for a particular change,
     * which is not expected to happen in usual cases.
     */
    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "https://github.com/spotbugs/spotbugs/issues/811")
    private List<ListenableFuture<Void>> updateOvsBridgeRefEntry(InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIid,
                                                                 OvsdbBridgeAugmentation bridgeNew,
                                                                 OvsdbBridgeAugmentation bridgeOld) {

        return Collections.singletonList(txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx -> {
            Uint64 dpnIdNew = directTunnelUtils.getDpnId(bridgeNew.getDatapathId());
            Uint64 dpnIdOld = directTunnelUtils.getDpnId(bridgeOld.getDatapathId());

            LOG.debug("updating bridge references for bridge: {}, dpnNew: {}, dpnOld: {}", bridgeNew,
                    dpnIdNew, dpnIdOld);
            //delete bridge reference entry for the old dpn in interface meta operational DS
            deleteOvsBridgeRefEntry(dpnIdOld, tx);

            // create bridge reference entry in interface meta operational DS
            createOvsBridgeRefEntry(dpnIdNew, bridgeIid, tx);

            // handle pre-provisioning of tunnels for the newly connected dpn
            Optional<OvsBridgeEntry> bridgeEntry = null;
            bridgeEntry = ovsBridgeEntryCache.get(dpnIdNew);
            if (bridgeEntry.isPresent()) {
                addAllPortsToBridge(bridgeEntry.get(), bridgeIid, bridgeNew);
            }
        }));
    }

    public List<ListenableFuture<Void>> removePortFromBridge(InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIid,
                                                             OvsdbBridgeAugmentation bridgeOld) {
        Uint64 dpnId = directTunnelUtils.getDpnId(bridgeOld.getDatapathId());
        if (dpnId == null) {
            LOG.warn("Got Null DPID for Bridge: {}", bridgeOld);
            return Collections.emptyList();
        }
        return Collections.singletonList(txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx -> {
            LOG.debug("removing bridge references for bridge: {}, dpn: {}", bridgeOld, dpnId);
            EVENT_LOGGER.debug("ITM-TunnelTopologyState, REMOVE {} completed", bridgeOld.getBridgeName().getValue());
            //delete bridge reference entry in interface meta operational DS
            deleteOvsBridgeRefEntry(dpnId, tx);

            // the bridge reference is copied to dpn-tunnel interfaces map, so that whenever a northbound delete
            // happens when bridge is not connected, we need the bridge reference to clean up the topology config DS
            addBridgeRefToBridgeTunnelEntry(dpnId, new OvsdbBridgeRef(bridgeIid), tx);
        }));
    }

    private void createOvsBridgeRefEntry(Uint64 dpnId, InstanceIdentifier<?> bridgeIid, WriteTransaction tx) {
        LOG.debug("Creating bridge ref entry for dpn: {} bridge: {}",
                dpnId, bridgeIid);
        OvsBridgeRefEntryKey bridgeRefEntryKey = new OvsBridgeRefEntryKey(dpnId);
        InstanceIdentifier<OvsBridgeRefEntry> bridgeEntryId =
                DirectTunnelUtils.getOvsBridgeRefEntryIdentifier(bridgeRefEntryKey);
        OvsBridgeRefEntryBuilder tunnelDpnBridgeEntryBuilder =
                new OvsBridgeRefEntryBuilder().withKey(bridgeRefEntryKey).setDpid(dpnId)
                        .setOvsBridgeReference(new OvsdbBridgeRef(bridgeIid));
        tx.put(LogicalDatastoreType.OPERATIONAL, bridgeEntryId, tunnelDpnBridgeEntryBuilder.build(), true);
    }

    private void deleteOvsBridgeRefEntry(Uint64 dpnId, WriteTransaction tx) {
        LOG.debug("Deleting bridge ref entry for dpn: {}",
                dpnId);
        OvsBridgeRefEntryKey bridgeRefEntryKey = new OvsBridgeRefEntryKey(dpnId);
        InstanceIdentifier<OvsBridgeRefEntry> bridgeEntryId =
                DirectTunnelUtils.getOvsBridgeRefEntryIdentifier(bridgeRefEntryKey);
        tx.delete(LogicalDatastoreType.OPERATIONAL, bridgeEntryId);
    }

    private void addBridgeRefToBridgeTunnelEntry(Uint64 dpId, OvsdbBridgeRef ovsdbBridgeRef, WriteTransaction tx) {
        OvsBridgeEntryKey bridgeEntryKey = new OvsBridgeEntryKey(dpId);
        InstanceIdentifier<OvsBridgeEntry> bridgeEntryInstanceIdentifier =
                DirectTunnelUtils.getOvsBridgeEntryIdentifier(bridgeEntryKey);

        OvsBridgeEntryBuilder bridgeEntryBuilder = new OvsBridgeEntryBuilder().withKey(bridgeEntryKey)
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
                    IfTunnel ifTunnel = iface.augmentation(IfTunnel.class);
                    if (ifTunnel != null) {
                        directTunnelUtils.addTunnelPortToBridge(ifTunnel, bridgeIid, iface, portName);
                    }
                } else {
                    LOG.debug("Interface {} not found in config DS", portName);
                }
            }
            EVENT_LOGGER.debug("ITM-TunnelTopologyState, ADD port on {} completed", bridgeName);
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

            Uint64 dpnId = directTunnelUtils.getDpnId(bridgeNew.getDatapathId());
            LOG.debug("adding bridge references for bridge: {}, dpn: {}", bridgeNew, dpnId);
            EVENT_LOGGER.debug("TunnelTopologyState, ADD bridge {} for {}", bridgeNew.getBridgeName(), dpnId);

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
