/*
 * Copyright (c) 2016, 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.listeners;

import com.google.common.base.Optional;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.confighelpers.ItmMonitorIntervalWorker;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.tools.mdsal.listener.AbstractSyncDataTreeChangeListener;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorInterval;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class TunnelMonitorIntervalListener extends AbstractSyncDataTreeChangeListener<TunnelMonitorInterval> {

    private static final Logger LOG = LoggerFactory.getLogger(TunnelMonitorIntervalListener.class);

    private final DataBroker broker;
    private final JobCoordinator jobCoordinator;

    @Inject
    public TunnelMonitorIntervalListener(DataBroker dataBroker, JobCoordinator jobCoordinator) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(TunnelMonitorInterval.class));
        this.broker = dataBroker;
        this.jobCoordinator = jobCoordinator;
    }

    @Override
    public void remove(@Nonnull InstanceIdentifier<TunnelMonitorInterval> instanceIdentifier,
                       @Nonnull TunnelMonitorInterval dataObjectModification) {
        LOG.debug("remove TunnelMonitorIntervalListener called with {}", dataObjectModification.getInterval());
        InstanceIdentifier<TransportZones> path = InstanceIdentifier.builder(TransportZones.class).build();
        Optional<TransportZones> transportZonesOptional = ItmUtils
                .read(LogicalDatastoreType.CONFIGURATION, path, broker);
        if (transportZonesOptional.isPresent()) {
            TransportZones tzones = transportZonesOptional.get();
            for (TransportZone tzone : tzones.getTransportZone()) {
                //if you remove configuration, the last configured interval is only set i.e no change
                LOG.debug("Remove:Calling TunnelMonitorIntervalWorker with tzone = {} and {}", tzone.getZoneName(),
                          dataObjectModification.getInterval());
                ItmMonitorIntervalWorker toggleWorker = new ItmMonitorIntervalWorker(tzone.getZoneName(),
                                                                                     dataObjectModification
                                                                                             .getInterval(), broker);
                jobCoordinator.enqueueJob(tzone.getZoneName(), toggleWorker);
            }
        }
    }

    @Override
    public void update(@Nonnull InstanceIdentifier<TunnelMonitorInterval> instanceIdentifier,
                       @Nonnull TunnelMonitorInterval dataObjectModificationBefore,
                       @Nonnull TunnelMonitorInterval dataObjectModificationAfter) {
        LOG.debug("update TunnelMonitorIntervalListener called with {}", dataObjectModificationAfter.getInterval());
        InstanceIdentifier<TransportZones> path = InstanceIdentifier.builder(TransportZones.class).build();
        Optional<TransportZones> transportZonesOptional = ItmUtils
                .read(LogicalDatastoreType.CONFIGURATION, path, broker);
        if (transportZonesOptional.isPresent()) {
            TransportZones tzones = transportZonesOptional.get();
            for (TransportZone tzone : tzones.getTransportZone()) {
                LOG.debug("Update:Calling TunnelMonitorIntervalWorker with tzone = {} and {}", tzone.getZoneName(),
                          dataObjectModificationAfter.getInterval());
                ItmMonitorIntervalWorker intervalWorker = new ItmMonitorIntervalWorker(tzone.getZoneName(),
                                                                                       dataObjectModificationAfter
                                                                                               .getInterval(), broker);
                jobCoordinator.enqueueJob(tzone.getZoneName(), intervalWorker);
            }
        }
    }

    @Override
    public void add(@Nonnull InstanceIdentifier<TunnelMonitorInterval> instanceIdentifier,
                    @Nonnull TunnelMonitorInterval dataObjectModification) {
        LOG.debug("Add TunnelMonitorIntervalListener called with {}", dataObjectModification.getInterval());
        InstanceIdentifier<TransportZones> path = InstanceIdentifier.builder(TransportZones.class).build();
        Optional<TransportZones> transportZonesOptional = ItmUtils
                .read(LogicalDatastoreType.CONFIGURATION, path, broker);
        if (transportZonesOptional.isPresent()) {
            TransportZones tzones = transportZonesOptional.get();
            for (TransportZone tzone : tzones.getTransportZone()) {
                LOG.debug("Add:Calling TunnelMonitorIntervalWorker with tzone = {} and {}", tzone.getZoneName(),
                          dataObjectModification.getInterval());
                ItmMonitorIntervalWorker intervalWorker = new ItmMonitorIntervalWorker(tzone.getZoneName(),
                                                                                       dataObjectModification
                                                                                               .getInterval(), broker);
                //conversion to milliseconds done while writing to i/f-mgr config DS
                jobCoordinator.enqueueJob(tzone.getZoneName(), intervalWorker);
            }
        }
    }
}
