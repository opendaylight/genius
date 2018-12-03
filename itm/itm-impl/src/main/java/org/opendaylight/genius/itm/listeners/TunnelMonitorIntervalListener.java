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
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.cache.DpnTepStateCache;
import org.opendaylight.genius.itm.cache.OvsBridgeRefEntryCache;
import org.opendaylight.genius.itm.confighelpers.ItmMonitorIntervalWorker;
import org.opendaylight.genius.itm.confighelpers.ItmMonitorWorker;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.serviceutils.tools.mdsal.listener.AbstractSyncDataTreeChangeListener;
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
    private final DirectTunnelUtils directTunnelUtils;
    private final IInterfaceManager interfaceManager;
    private final DpnTepStateCache dpnTepStateCache;
    private final OvsBridgeRefEntryCache ovsBridgeRefEntryCache;

    @Inject
    public TunnelMonitorIntervalListener(DataBroker dataBroker, JobCoordinator jobCoordinator,
                                         final DirectTunnelUtils directTunnelUtils,
                                         final DpnTepStateCache dpnTepStateCache,
                                         final OvsBridgeRefEntryCache ovsBridgeRefEntryCache,
                                         final IInterfaceManager interfaceManager) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(TunnelMonitorInterval.class));
        this.broker = dataBroker;
        this.jobCoordinator = jobCoordinator;
        this.directTunnelUtils = directTunnelUtils;
        this.dpnTepStateCache = dpnTepStateCache;
        this.interfaceManager = interfaceManager;
        this.ovsBridgeRefEntryCache = ovsBridgeRefEntryCache;
    }

    @Override
    public void remove(@Nonnull InstanceIdentifier<TunnelMonitorInterval> instanceIdentifier,
                       @Nonnull TunnelMonitorInterval dataObjectModification) {
        LOG.debug("remove TunnelMonitorIntervalListener called with {}", dataObjectModification.getInterval());
        InstanceIdentifier<TransportZones> path = InstanceIdentifier.builder(TransportZones.class).build();
        Optional<TransportZones> transportZonesOptional = ItmUtils
                .read(LogicalDatastoreType.CONFIGURATION, path, broker);
        if (transportZonesOptional.isPresent()) {
            for (TransportZone tzone : transportZonesOptional.get().nonnullTransportZone()) {
                //if you remove configuration, the last configured interval is only set i.e no change
                LOG.debug("Remove:Calling TunnelMonitorIntervalWorker with tzone = {} and {}", tzone.getZoneName(),
                          dataObjectModification.getInterval());
                if (interfaceManager.isItmDirectTunnelsEnabled()) {
                    ItmMonitorWorker monitorWorker = new ItmMonitorWorker(tzone.getZoneName(),
                        dataObjectModification.getInterval(),null, broker, directTunnelUtils,
                        dpnTepStateCache, ovsBridgeRefEntryCache);
                    jobCoordinator.enqueueJob(tzone.getZoneName(), monitorWorker);
                } else {
                    ItmMonitorIntervalWorker toggleWorker = new ItmMonitorIntervalWorker(tzone.getZoneName(),
                        dataObjectModification.getInterval(), broker);
                    jobCoordinator.enqueueJob(tzone.getZoneName(), toggleWorker);
                }
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
            for (TransportZone tzone : transportZonesOptional.get().nonnullTransportZone()) {
                LOG.debug("Update:Calling TunnelMonitorIntervalWorker with tzone = {} and {}", tzone.getZoneName(),
                          dataObjectModificationAfter.getInterval());
                if (interfaceManager.isItmDirectTunnelsEnabled()) {
                    ItmMonitorWorker monitorWorker = new ItmMonitorWorker(tzone.getZoneName(),
                        dataObjectModificationAfter.getInterval(), null, broker, directTunnelUtils,
                        dpnTepStateCache, ovsBridgeRefEntryCache);
                    jobCoordinator.enqueueJob(tzone.getZoneName(), monitorWorker);
                } else {
                    ItmMonitorIntervalWorker intervalWorker = new ItmMonitorIntervalWorker(tzone.getZoneName(),
                        dataObjectModificationAfter.getInterval(), broker);
                    jobCoordinator.enqueueJob(tzone.getZoneName(), intervalWorker);
                }
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
            for (TransportZone tzone : transportZonesOptional.get().nonnullTransportZone()) {
                LOG.debug("Add:Calling TunnelMonitorIntervalWorker with tzone = {} and {}", tzone.getZoneName(),
                          dataObjectModification.getInterval());
                if (interfaceManager.isItmDirectTunnelsEnabled()) {
                    ItmMonitorWorker toggleWorker = new ItmMonitorWorker(tzone.getZoneName(),
                        dataObjectModification.getInterval(), null, broker, directTunnelUtils,
                        dpnTepStateCache, ovsBridgeRefEntryCache);
                    jobCoordinator.enqueueJob(tzone.getZoneName(), toggleWorker);
                } else {
                    ItmMonitorIntervalWorker intervalWorker = new ItmMonitorIntervalWorker(tzone.getZoneName(),
                        dataObjectModification.getInterval(), broker);
                    //conversion to milliseconds done while writing to i/f-mgr config DS
                    jobCoordinator.enqueueJob(tzone.getZoneName(), intervalWorker);
                }
            }
        }
    }
}
