/*
 * Copyright (c) 2019 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.confighelpers;

import static org.opendaylight.genius.infra.Datastore.CONFIGURATION;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.opendaylight.genius.cloudscaler.api.TombstonedNodeManager;
import org.opendaylight.genius.infra.Datastore.Configuration;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.infra.TypedReadWriteTransaction;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.cache.OfDpnTepConfigCache;
import org.opendaylight.genius.itm.cache.OfTepStateCache;
import org.opendaylight.genius.itm.cache.OvsBridgeEntryCache;
import org.opendaylight.genius.itm.cache.OvsBridgeRefEntryCache;
import org.opendaylight.genius.itm.impl.ITMBatchingUtils;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.bridge.tunnel.info.OvsBridgeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.bridge.tunnel.info.OvsBridgeEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.bridge.tunnel.info.ovs.bridge.entry.OvsBridgeTunnelEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.bridge.tunnel.info.ovs.bridge.entry.OvsBridgeTunnelEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.ovs.bridge.ref.info.OvsBridgeRefEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelOperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.tep.config.OfDpnTep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.of.teps.state.OfTep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.OperationFailedException;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItmOfTunnelDeleteWorker {

    private static final Logger LOG = LoggerFactory.getLogger(ItmOfTunnelDeleteWorker.class);
    private static final Logger EVENT_LOGGER = LoggerFactory.getLogger("GeniusEventLogger");

    private final ManagedNewTransactionRunner txRunner;
    private final DataBroker dataBroker;
    private final OfDpnTepConfigCache ofDpnTepConfigCache;
    private final TombstonedNodeManager tombstonedNodeManager;
    private final IInterfaceManager interfaceManager;
    private final DirectTunnelUtils directTunnelUtils;
    private final OvsBridgeRefEntryCache ovsBridgeRefEntryCache;
    private final OvsBridgeEntryCache ovsBridgeEntryCache;
    private final OfTepStateCache ofTepStateCache;

    public ItmOfTunnelDeleteWorker(DataBroker dataBroker,
                                   OfDpnTepConfigCache ofDpnTepConfigCache,
                                   TombstonedNodeManager tombstonedNodeManager,
                                   IInterfaceManager interfaceManager,
                                   DirectTunnelUtils directTunnelUtils,
                                   OvsBridgeRefEntryCache ovsBridgeRefEntryCache,
                                   OvsBridgeEntryCache ovsBridgeEntryCache,
                                   OfTepStateCache ofTepStateCache) {
        this.dataBroker = dataBroker;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.ofDpnTepConfigCache = ofDpnTepConfigCache;
        this.tombstonedNodeManager = tombstonedNodeManager;
        this.interfaceManager = interfaceManager;
        this.directTunnelUtils = directTunnelUtils;
        this.ovsBridgeRefEntryCache = ovsBridgeRefEntryCache;
        this.ovsBridgeEntryCache = ovsBridgeEntryCache;
        this.ofTepStateCache = ofTepStateCache;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public List<ListenableFuture<Void>> deleteOfTeps(Collection<OfDpnTep> ofTepsList) {
        LOG.trace("OFTeps to be deleted {} " , ofTepsList);
        return Collections.singletonList(txRunner.callWithNewReadWriteTransactionAndSubmit(CONFIGURATION, tx -> {
            if (ofTepsList == null || ofTepsList.size() == 0) {
                LOG.debug("no ofteps to delete");
                return;
            }

            for (OfDpnTep srcDpn : ofTepsList) {
                LOG.trace("Processing srcDpn {}", srcDpn);

                Boolean isDpnTombstoned = tombstonedNodeManager.isDpnTombstoned(srcDpn.getSourceDpnId());

                removeOfTepInterfaceFromOvsdb(srcDpn, tx);
                if (isDpnTombstoned) {
                    EVENT_LOGGER.debug("OfTep-DeleteWorker, Tombstoned, DpnTep for {} removed",
                            srcDpn.getSourceDpnId());
                    LOG.trace("Removing tunnelState entry for {} while tombstoned is set true",srcDpn.getSourceDpnId());
                    removeOfTepState(srcDpn);
                }
                LOG.debug("Deleting OFTEP Interface information from Config datastore with DPNs-Tep "
                        + "for source Dpn {}", srcDpn.getSourceDpnId());
                // Clean up the DPNs TEP State DS
                DirectTunnelUtils.removeOfTepFromDpnsTepConfigDS(dataBroker, srcDpn.getSourceDpnId());
                EVENT_LOGGER.debug("OfTep-DeleteWorker, DpnTep for {} removed", srcDpn.getSourceDpnId());
            }
        }));
    }

    private void removeOfTepState(OfDpnTep srcDpn) {
        LOG.trace("Removing ofTepstate for {} with tomstoned enable", srcDpn.getOfPortName());
        directTunnelUtils.deleteOfTepStateEntry(srcDpn.getOfPortName());
    }

    private void removeOfTepInterfaceFromOvsdb(OfDpnTep dpnTep, TypedReadWriteTransaction<Configuration> tx) {
        LOG.trace("Removing ofTep Interface {}", dpnTep.getOfPortName());
        try {
            removeConfiguration(dpnTep, tx);
        } catch (ExecutionException | InterruptedException | OperationFailedException e) {
            LOG.error("Cannot Delete Tunnel {} as OVS Bridge Entry is NULL ", dpnTep.getTunnelType(), e);
        }
    }

    private void removeConfiguration(OfDpnTep dpnsTep, TypedReadWriteTransaction<Configuration> tx)
            throws ExecutionException, InterruptedException, OperationFailedException {
        // Check if the same transaction can be used across Config and operational shards
        removeTunnelConfiguration(dpnsTep, tx);
    }

    private void removeTunnelConfiguration(OfDpnTep dpnTep, TypedReadWriteTransaction<Configuration> tx)
            throws ExecutionException, InterruptedException, OperationFailedException {

        LOG.info("removing ofTep configuration for {}", dpnTep.getOfPortName());

        Optional<OvsBridgeRefEntry> ovsBridgeRefEntry = ovsBridgeRefEntryCache.get(dpnTep.getSourceDpnId());
        Optional<OvsBridgeEntry> ovsBridgeEntryOptional;
        OvsdbBridgeRef ovsdbBridgeRef = null;
        if (ovsBridgeRefEntry.isPresent()) {
            ovsdbBridgeRef = ovsBridgeRefEntry.get().getOvsBridgeReference();
        } else {
            ovsBridgeEntryOptional = ovsBridgeEntryCache.get(dpnTep.getSourceDpnId());
            if (ovsBridgeEntryOptional.isPresent()) {
                ovsdbBridgeRef = ovsBridgeEntryOptional.get().getOvsBridgeReference();
            }
        }

        if (ovsdbBridgeRef != null) {
            removeTerminationEndPoint(ovsdbBridgeRef.getValue(), dpnTep.getOfPortName());
        }

        // delete tunnel ingress flow
        removeOfPortIngressFlow(tx, dpnTep.getOfPortName(), dpnTep.getSourceDpnId());

        // delete bridge to tunnel interface mappings
        OvsBridgeEntryKey bridgeEntryKey = new OvsBridgeEntryKey(dpnTep.getSourceDpnId());
        InstanceIdentifier<OvsBridgeEntry> bridgeEntryIid =
                DirectTunnelUtils.getOvsBridgeEntryIdentifier(bridgeEntryKey);

        ovsBridgeEntryOptional = ovsBridgeEntryCache.get(dpnTep.getSourceDpnId());
        if (ovsBridgeEntryOptional.isPresent()) {
            Map<OvsBridgeTunnelEntryKey, OvsBridgeTunnelEntry> bridgeTunnelEntries = ovsBridgeEntryOptional
                    .get().getOvsBridgeTunnelEntry();
            deleteBridgeInterfaceEntry(bridgeEntryKey,
                    bridgeTunnelEntries.values().stream().collect(Collectors.toList()),
                    bridgeEntryIid, dpnTep.getOfPortName());
            // IfIndex needs to be removed only during State Clean up not Config
            cleanUpOfTepWithUnknownState(dpnTep.getOfPortName(), tx);
            directTunnelUtils.removeLportTagInterfaceMap(dpnTep.getOfPortName());
        }
    }

    private void removeTerminationEndPoint(InstanceIdentifier<?> bridgeIid, String ofPortName) {
        LOG.debug("removing termination point for {}", ofPortName);
        InstanceIdentifier<TerminationPoint> tpIid = DirectTunnelUtils.createTerminationPointInstanceIdentifier(
                InstanceIdentifier.keyOf(bridgeIid.firstIdentifierOf(Node.class)), ofPortName);
        ITMBatchingUtils.delete(tpIid, ITMBatchingUtils.EntityType.TOPOLOGY_CONFIG);
    }

    private void removeOfPortIngressFlow(TypedReadWriteTransaction<Configuration> tx,
                                         String ofTepName,
                                         Uint64 dpId) throws ExecutionException, InterruptedException {
        directTunnelUtils.removeTunnelIngressFlow(tx, dpId, ofTepName);
    }

    private void deleteBridgeInterfaceEntry(OvsBridgeEntryKey bridgeEntryKey,
                                            List<OvsBridgeTunnelEntry> bridgeTunnelEntries,
                                            InstanceIdentifier<OvsBridgeEntry> bridgeEntryIid,
                                            String ofTepName) {
        OvsBridgeTunnelEntryKey bridgeTunnelEntryKey = new OvsBridgeTunnelEntryKey(ofTepName);
        InstanceIdentifier<OvsBridgeTunnelEntry> bridgeTunnelEntryIid =
                DirectTunnelUtils.getBridgeTunnelEntryIdentifier(bridgeEntryKey, bridgeTunnelEntryKey);
        ITMBatchingUtils.delete(bridgeTunnelEntryIid, ITMBatchingUtils.EntityType.DEFAULT_CONFIG);
        if (bridgeTunnelEntries.size() <= 1) {
            ITMBatchingUtils.delete(bridgeEntryIid, ITMBatchingUtils.EntityType.DEFAULT_CONFIG);
        }
    }

    // if the node is shutdown, there will be stale ofTep state entries,
    // with unknown op-state, clear them.
    private void cleanUpOfTepWithUnknownState(String ofTepName, TypedReadWriteTransaction<Configuration> tx)
            throws ReadFailedException {
        Optional<OfTep> ofTepList = ofTepStateCache.get(ofTepName);
        if (ofTepList.isPresent() && ofTepList.get().getOfTepState() == TunnelOperStatus.Unknown) {
            LOG.debug("cleaning up dpnTep for {}, since the oper-status is {}", ofTepName,
                    ofTepList.get().getOfTepState());
            directTunnelUtils.deleteOfTepStateEntry(ofTepName);
        }
    }

}
