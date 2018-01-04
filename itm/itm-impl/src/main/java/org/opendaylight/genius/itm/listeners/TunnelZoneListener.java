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
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.listeners.AbstractSyncDataTreeChangeListener;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.confighelpers.TepStateRemoveWorker;
import org.opendaylight.genius.itm.confighelpers.TunnelInfoAddWorker;
import org.opendaylight.genius.itm.confighelpers.TunnelInfoDeleteWorker;
import org.opendaylight.genius.itm.confighelpers.VtepAddWorker;
import org.opendaylight.genius.itm.confighelpers.VtepRemoveWorker;
import org.opendaylight.genius.itm.impl.ItmTepUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.NodeIdTypeOvsdb;
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
    private final IInterfaceManager ifManager;
    private final JobCoordinator jobCoordinator;
    private final ItmConfig itmConfig;

    @Inject
    public TunnelZoneListener(final DataBroker dataBroker, final ItmConfig itmConfig, final ItmTepUtils itmTepUtils,
                              final IInterfaceManager interfaceManager, final JobCoordinator jobCoordinator) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION,
            InstanceIdentifier.create(TunnelZones.class).child(TunnelZone.class));
        this.dataBroker = dataBroker;
        this.itmTepUtils = itmTepUtils;
        this.ifManager = interfaceManager;
        this.jobCoordinator = jobCoordinator;
        this.itmConfig = itmConfig;
    }

    @Override
    public void add(@Nonnull TunnelZone tunnelZone) {
        LOG.trace("Received Transport Zone Add Event: {}", tunnelZone);
        Optional.ofNullable(tunnelZone.getVteps()).ifPresent(vteps
            -> vteps.parallelStream().forEach(vtep
                -> addVtep(vtep, tunnelZone, null)));
    }

    @Override
    public void remove(@Nonnull TunnelZone tunnelZone) {
        LOG.trace("Received Transport Zone Delete Event: {}", tunnelZone);
        Optional.ofNullable(tunnelZone.getVteps()).ifPresent(vteps
            -> vteps.parallelStream().forEach(vtep
                -> removeVtep(vtep, tunnelZone, null)));
    }

    public void update(@Nonnull TunnelZone original, @Nonnull TunnelZone update) {
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
        deleted.parallelStream().forEach(vtep -> removeVtep(vtep, original, deletedMap));
        added.parallelStream().forEach(vtep -> addVtep(vtep, update, addedMap));

    }


    private void addVtep(Vteps vtep, TunnelZone zone, Map<String, String> addedMap) {
        LOG.debug("Creating tunnel of type{} on {}", zone.getTunnelType().getSimpleName(), vtep.getNodeId());
        if (NodeIdTypeOvsdb.class.equals(vtep.getVtepNodeIdType())) {
            TunnelInfoAddWorker tunnelInfoAddWorker =
                new TunnelInfoAddWorker(dataBroker, itmTepUtils, vtep, zone, addedMap);
            jobCoordinator.enqueueJob(zone.getTunnelZoneName(), tunnelInfoAddWorker);
        }
        VtepAddWorker vtepAddWorker = new VtepAddWorker(dataBroker, itmTepUtils, vtep, zone);
        jobCoordinator.enqueueJob(zone.getTunnelZoneName(), vtepAddWorker);
    }

    private void removeVtep(Vteps vtep, TunnelZone zone, Map<String, String> deletedMap) {
        LOG.debug("Removing tunnel of type{} on {}", zone.getTunnelType().getSimpleName(), vtep.getNodeId());
        VtepRemoveWorker vtepRemoveWorker = new VtepRemoveWorker(dataBroker, itmTepUtils, vtep, zone);
        TepStateRemoveWorker tepStateRemoveWorker =
            new TepStateRemoveWorker(dataBroker, itmTepUtils, vtep.getNodeId());
        jobCoordinator.enqueueJob(zone.getTunnelZoneName(), vtepRemoveWorker);
        jobCoordinator.enqueueJob(zone.getTunnelZoneName(), tepStateRemoveWorker);
        if (NodeIdTypeOvsdb.class.equals(vtep.getVtepNodeIdType())) {
            TunnelInfoDeleteWorker tunnelInfoDeleteWorker =
                new TunnelInfoDeleteWorker(dataBroker, itmTepUtils, vtep, zone, deletedMap);
            jobCoordinator.enqueueJob(zone.getTunnelZoneName(), tunnelInfoDeleteWorker);
        }
    }
}
