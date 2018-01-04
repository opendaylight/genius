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
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.listeners.AbstractSyncDataTreeChangeListener;
import org.opendaylight.genius.itm.confighelpers.VtepAddWorker;
import org.opendaylight.genius.itm.confighelpers.VtepRemoveWorker;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TunnelZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.tunnel.zones.TunnelZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.tunnel.zones.tunnel.zone.Vteps;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class TunnelZoneListener extends AbstractSyncDataTreeChangeListener<TunnelZone> {

    private static final Logger LOG = LoggerFactory.getLogger(TunnelZoneListener.class);

    private final DataBroker dataBroker;
    private final JobCoordinator jobCoordinator;
    private final ItmConfig itmConfig;

    @Inject
    public TunnelZoneListener(final DataBroker dataBroker, final ItmConfig itmConfig, JobCoordinator jobCoordinator) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION,
            InstanceIdentifier.create(TunnelZones.class).child(TunnelZone.class));
        this.dataBroker = dataBroker;
        this.jobCoordinator = jobCoordinator;
        this.itmConfig = itmConfig;
    }

    @Override
    public void add(@Nonnull TunnelZone tunnelZone) {
        LOG.trace("Received Transport Zone Add Event: {}", tunnelZone);
        Optional.ofNullable(tunnelZone.getVteps()).ifPresent(vteps
            -> vteps.parallelStream().forEach(vtep
                -> addVtep(vtep, tunnelZone)));
    }

    @Override
    public void remove(@Nonnull TunnelZone tunnelZone) {
        LOG.trace("Received Transport Zone Delete Event: {}", tunnelZone);
        Optional.ofNullable(tunnelZone.getVteps()).ifPresent(vteps
            -> vteps.parallelStream().forEach(vtep
                -> removeVtep(vtep, tunnelZone)));
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
        LOG.trace("vtepsAdded={}, vtepsDeleted={}", added, deleted);
        deleted.parallelStream().forEach(vtep -> removeVtep(vtep, original));
        added.parallelStream().forEach(vtep -> addVtep(vtep, update));

    }


    private void addVtep(Vteps vtep, TunnelZone zone) {
        LOG.debug("Creating tunnel of type{} on {}", zone.getTunnelType().getSimpleName(), vtep.getNodeId());
        VtepAddWorker vtepAddWorker = new VtepAddWorker(dataBroker, vtep, zone.getTunnelType());
        jobCoordinator.enqueueJob(zone.getTunnelZoneName(), vtepAddWorker);
    }

    private void removeVtep(Vteps vtep, TunnelZone zone) {
        LOG.debug("Removing tunnel of type{} on {}", zone.getTunnelType().getSimpleName(), vtep.getNodeId());
        VtepRemoveWorker vtepRemoveWorker = new VtepRemoveWorker(dataBroker, vtep, zone.getTunnelType());
        jobCoordinator.enqueueJob(zone.getTunnelZoneName(), vtepRemoveWorker);
    }
}
