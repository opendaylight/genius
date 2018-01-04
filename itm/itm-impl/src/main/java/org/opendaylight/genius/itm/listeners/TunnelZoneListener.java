/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.listeners;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.listeners.AbstractSyncDataTreeChangeListener;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.confighelpers.VtepAddWorker;
import org.opendaylight.genius.itm.confighelpers.VtepRemoveWorker;
import org.opendaylight.genius.itm.impl.ItmFlowUtils;
import org.opendaylight.genius.itm.impl.ItmTepUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.TunnelZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tunnel.zones.TunnelZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tunnel.zones.tunnel.zone.Vteps;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class TunnelZoneListener extends AbstractSyncDataTreeChangeListener<TunnelZone> {

    private static final Logger LOG = LoggerFactory.getLogger(TunnelZoneListener.class);

    private final DataBroker dataBroker;
    private final ItmTepUtils itmTepUtils;
    private final ItmFlowUtils itmFlowUtils;
    private final IInterfaceManager ifManager;
    private final JobCoordinator jobCoordinator;
    private final ItmConfig itmConfig;

    @Inject
    public TunnelZoneListener(final DataBroker dataBroker, final ItmConfig itmConfig,
                              final ItmTepUtils itmTepUtils, final ItmFlowUtils itmFlowUtils,
                              final IInterfaceManager interfaceManager, final JobCoordinator jobCoordinator) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION,
            InstanceIdentifier.create(TunnelZones.class).child(TunnelZone.class));
        this.dataBroker = dataBroker;
        this.itmTepUtils = itmTepUtils;
        this.itmFlowUtils = itmFlowUtils;
        this.ifManager = interfaceManager;
        this.jobCoordinator = jobCoordinator;
        this.itmConfig = itmConfig;
    }

    @Override
    public void add(@Nonnull TunnelZone tunnelZone) {
        LOG.trace("Received Transport Zone Add Event: {}", tunnelZone);
        if (tunnelZone.getVteps() != null) {
            Set<Class<? extends TunnelTypeBase>> zoneTunnelTypes = getZoneTunnelTypes(tunnelZone);
            tunnelZone.getVteps().parallelStream().forEach(vtep
                -> addNode(vtep, tunnelZone, null, zoneTunnelTypes));
        }
    }

    @Override
    public void remove(@Nonnull TunnelZone tunnelZone) {
        LOG.trace("Received Transport Zone Delete Event: {}", tunnelZone);
        if (tunnelZone.getVteps() != null) {
            Set<Class<? extends TunnelTypeBase>> zoneTunnelTypes = getZoneTunnelTypes(tunnelZone);
            tunnelZone.getVteps().parallelStream().forEach(vtep
                -> deleteNode(vtep, tunnelZone, null, zoneTunnelTypes));
        }
    }

    public void update(@Nonnull TunnelZone original, @Nonnull TunnelZone update) {
        if (update.isOptionOfTunnel()) {
            LOG.error("Updating zone of type OfTunnel not supported yet");
        }
        LOG.trace("Received Transport Zone Update Event: Old = {}, Updated = {}", original, update);
        List<Vteps> added = new ArrayList<>();
        List<Vteps> deleted = new ArrayList<>();
        Optional<List<Vteps>> updateOptional = Optional.ofNullable(update.getVteps());
        Optional<List<Vteps>> originalOptional = Optional.ofNullable(original.getVteps());

        updateOptional.ifPresent(vteps -> added.addAll(vteps));
        originalOptional.ifPresent(vteps -> deleted.addAll(vteps));

        originalOptional.ifPresent(vteps -> added.removeAll(vteps));
        updateOptional.ifPresent(vteps -> deleted.removeAll(vteps));
        Map<String, String> deletedMap =
            deleted.stream().collect(Collectors.toMap(Vteps::getNodeId, Vteps::getNodeId));
        Map<String, String> addedMap =
            added.stream().collect(Collectors.toMap(Vteps::getNodeId, Vteps::getNodeId));
        LOG.trace("vtepsAdded={}, vtepsDeleted={}", added, deleted);
        deleted.parallelStream().forEach(vtep -> deleteNode(vtep, original, deletedMap, null));
        added.parallelStream().forEach(vtep -> addNode(vtep, update, addedMap, null));
    }


    private void addNode(Vteps vtep, TunnelZone zone, Map<String, String> addedMap,
                         Set<Class<? extends TunnelTypeBase>> zoneTunnelTypes) {
        if (zoneTunnelTypes != null) {
            // OfTunnels use case
            for (Class<? extends TunnelTypeBase> tunnelType : zoneTunnelTypes) {
                addVtep(vtep, zone, addedMap, tunnelType);
            }
        } else {
            addVtep(vtep, zone, addedMap, null);
        }
    }


    private void deleteNode(Vteps vtep, TunnelZone zone, Map<String, String> deletedMap,
                            Set<Class<? extends TunnelTypeBase>> zoneTunnelTypes) {
        if (zoneTunnelTypes != null) {
            for (Class<? extends TunnelTypeBase> tunnelType : zoneTunnelTypes) {
                removeVtep(vtep, zone, deletedMap, tunnelType);
            }
        } else {
            removeVtep(vtep, zone, deletedMap, null);
        }
    }

    private void addVtep(Vteps vtep, TunnelZone zone, Map<String, String> addedMap,
                         Class<? extends TunnelTypeBase> tunnelType) {
        LOG.debug("Adding Vtep", vtep.getNodeId());
        /*
        if (NodeIdTypeOvsdb.class.equals(vtep.getVtepNodeIdType())) {
            TunnelInfoAddWorker tunnelInfoAddWorker =
                new TunnelInfoAddWorker(dataBroker, itmTepUtils, vtep, zone, addedMap);
            jobCoordinator.enqueueJob(zone.getTunnelZoneName(), tunnelInfoAddWorker);
        }
        */
        VtepAddWorker vtepAddWorker =
            new VtepAddWorker(dataBroker, itmTepUtils, vtep, tunnelType, zone, addedMap);
        jobCoordinator.enqueueJob(zone.getTunnelZoneName(), vtepAddWorker);
    }

    private void removeVtep(Vteps vtep, TunnelZone zone, Map<String, String> deletedMap,
                            Class<? extends TunnelTypeBase> tunnelType) {
        LOG.debug("Removing vtep: {}", vtep.getNodeId());
        VtepRemoveWorker vtepRemoveWorker =
            new VtepRemoveWorker(dataBroker, itmTepUtils, itmFlowUtils, vtep, tunnelType, zone, deletedMap);
        jobCoordinator.enqueueJob(zone.getTunnelZoneName(), vtepRemoveWorker);
        /*
        String tepPortName = itmTepUtils.getVtepPortName(vtep, tunnelType);

        TepStateRemoveWorker tepStateRemoveWorker =
            new TepStateRemoveWorker(dataBroker, itmTepUtils, tepPortName);
        jobCoordinator.enqueueJob(zone.getTunnelZoneName(), tepStateRemoveWorker);
        */
        /*
        if (NodeIdTypeOvsdb.class.equals(vtep.getVtepNodeIdType())) {
            TunnelInfoDeleteWorker tunnelInfoDeleteWorker =
                new TunnelInfoDeleteWorker(dataBroker, itmTepUtils, vtep, zone, deletedMap);
            jobCoordinator.enqueueJob(zone.getTunnelZoneName(), tunnelInfoDeleteWorker);
        }
        */
    }


    private Set<Class<? extends TunnelTypeBase>> getZoneTunnelTypes(TunnelZone zone) {
        if (zone.isOptionOfTunnel()) {
            Set<Class<? extends TunnelTypeBase>> zoneTunnelTypes =
                zone.getVteps().stream().map(Vteps::getVtepTunnelType).filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            zoneTunnelTypes.add(zone.getTunnelType());
            return zoneTunnelTypes;
        }
        return null;
    }
}
