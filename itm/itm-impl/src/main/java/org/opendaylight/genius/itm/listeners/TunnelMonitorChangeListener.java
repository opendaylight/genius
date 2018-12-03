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
import org.opendaylight.genius.itm.confighelpers.ItmMonitorToggleWorker;
import org.opendaylight.genius.itm.confighelpers.ItmMonitorWorker;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.serviceutils.tools.mdsal.listener.AbstractSyncDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorParams;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class TunnelMonitorChangeListener
        extends AbstractSyncDataTreeChangeListener<TunnelMonitorParams> {

    private static final Logger LOG = LoggerFactory.getLogger(TunnelMonitorChangeListener.class);

    private final DataBroker broker;
    private final JobCoordinator jobCoordinator;
    private final DirectTunnelUtils directTunnelUtils;
    private final IInterfaceManager interfaceManager;
    private final DpnTepStateCache dpnTepStateCache;
    private final OvsBridgeRefEntryCache ovsBridgeRefEntryCache;


    @Inject
    public TunnelMonitorChangeListener(DataBroker dataBroker, JobCoordinator jobCoordinator,
                                       final DirectTunnelUtils directTunnelUtils,
                                       final DpnTepStateCache dpnTepStateCache,
                                       final OvsBridgeRefEntryCache ovsBridgeRefEntryCache,
                                       final IInterfaceManager interfaceManager) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(TunnelMonitorParams.class));
        this.broker = dataBroker;
        this.jobCoordinator = jobCoordinator;
        this.directTunnelUtils = directTunnelUtils;
        this.dpnTepStateCache = dpnTepStateCache;
        this.interfaceManager = interfaceManager;
        this.ovsBridgeRefEntryCache = ovsBridgeRefEntryCache;
    }

    @Override
    public void remove(@Nonnull InstanceIdentifier<TunnelMonitorParams> instanceIdentifier,
                       @Nonnull TunnelMonitorParams dataObjectModification) {
        InstanceIdentifier<TransportZones> path = InstanceIdentifier.builder(TransportZones.class).build();
        Optional<TransportZones> transportZonesOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION,
                path, broker);
        Class<? extends TunnelMonitoringTypeBase> monitorProtocol = dataObjectModification.getMonitorProtocol();
        if (monitorProtocol == null) {
            monitorProtocol = ITMConstants.DEFAULT_MONITOR_PROTOCOL;
        }
        if (transportZonesOptional.isPresent()) {
            for (TransportZone tzone : transportZonesOptional.get().nonnullTransportZone()) {
                LOG.debug("Remove - TunnelMonitorToggleWorker with tzone = {}, Enable = {}, MonitorProtocol = {}",
                        tzone.getZoneName(),dataObjectModification.isEnabled(), monitorProtocol);
                if (interfaceManager.isItmDirectTunnelsEnabled()) {
                    ItmMonitorWorker toggleWorker = new ItmMonitorWorker(tzone.getZoneName(),
                        Boolean.valueOf(false), monitorProtocol, broker, directTunnelUtils, dpnTepStateCache,
                        ovsBridgeRefEntryCache);
                    jobCoordinator.enqueueJob(tzone.getZoneName(), toggleWorker);
                }
                else {
                    ItmMonitorToggleWorker toggleWorker = new ItmMonitorToggleWorker(tzone.getZoneName(),
                        false, monitorProtocol, broker);
                    jobCoordinator.enqueueJob(tzone.getZoneName(), toggleWorker);
                }

            }
        }
    }

    @Override
    public void update(@Nonnull InstanceIdentifier<TunnelMonitorParams> instanceIdentifier,
                       @Nonnull TunnelMonitorParams dataObjectModificationBefore,
                       @Nonnull TunnelMonitorParams dataObjectModificationAfter) {
        LOG.debug("update TunnelMonitorChangeListener called with {}",dataObjectModificationAfter.isEnabled());
        Class<? extends TunnelMonitoringTypeBase> monitorProtocolBefore =
                dataObjectModificationBefore.getMonitorProtocol();
        Class<? extends TunnelMonitoringTypeBase> monitorProtocolAfter =
                dataObjectModificationAfter.getMonitorProtocol();
        Class<? extends TunnelMonitoringTypeBase> monitorProtocol = ITMConstants.DEFAULT_MONITOR_PROTOCOL;
        InstanceIdentifier<TransportZones> path = InstanceIdentifier.builder(TransportZones.class).build();
        Optional<TransportZones> transportZonesOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION,
                path, broker);
        if (monitorProtocolAfter != null) {
            monitorProtocol = dataObjectModificationAfter.getMonitorProtocol();
        }
        if (monitorProtocolBefore != null && monitorProtocolAfter != null) {
            LOG.debug("TunnelMonitorChangeListener Update : Existing_MonitorProtocol {}, New_MonitorProtocol {} ",
                    monitorProtocolBefore.getName(), monitorProtocolAfter.getName());
            if (!monitorProtocolAfter.getName().equalsIgnoreCase(monitorProtocolBefore.getName())) {
                LOG.error("Updation of monitor protocol not allowed");
            }

        }
        if (transportZonesOptional.isPresent()) {
            for (TransportZone tzone : transportZonesOptional.get().nonnullTransportZone()) {
                LOG.debug("Update - TunnelMonitorToggleWorker with tzone = {}, Enable = {}, MonitorProtocol = {}",
                        tzone.getZoneName(),dataObjectModificationAfter.isEnabled(), monitorProtocol);
                if (interfaceManager.isItmDirectTunnelsEnabled()) {
                    ItmMonitorWorker toggleWorker = new ItmMonitorWorker(tzone.getZoneName(),
                        dataObjectModificationAfter.isEnabled(), monitorProtocol, broker, directTunnelUtils,
                        dpnTepStateCache, ovsBridgeRefEntryCache);
                    jobCoordinator.enqueueJob(tzone.getZoneName(), toggleWorker);

                } else {
                    ItmMonitorToggleWorker toggleWorker = new ItmMonitorToggleWorker(tzone.getZoneName(),
                        dataObjectModificationAfter.isEnabled(), monitorProtocol, broker);
                    jobCoordinator.enqueueJob(tzone.getZoneName(), toggleWorker);
                }
            }
        }
    }

    @Override
    public void add(@Nonnull InstanceIdentifier<TunnelMonitorParams> instanceIdentifier,
                    @Nonnull TunnelMonitorParams dataObjectModification) {
        LOG.debug("Add - TunnelMonitorToggleWorker with Enable = {}, MonitorProtocol = {}",
                dataObjectModification.isEnabled(), dataObjectModification.getMonitorProtocol());
        Class<? extends TunnelMonitoringTypeBase> monitorProtocol = dataObjectModification.getMonitorProtocol();
        InstanceIdentifier<TransportZones> path = InstanceIdentifier.builder(TransportZones.class).build();
        Optional<TransportZones> transportZonesOptional =
                ItmUtils.read(LogicalDatastoreType.CONFIGURATION, path, broker);
        if (monitorProtocol == null) {
            monitorProtocol = ITMConstants.DEFAULT_MONITOR_PROTOCOL;
        }
        if (transportZonesOptional.isPresent()) {
            for (TransportZone tzone : transportZonesOptional.get().nonnullTransportZone()) {
                LOG.debug("Add: TunnelMonitorToggleWorker with tzone = {} monitoringEnabled {} and "
                        + "monitoringProtocol {}",tzone.getZoneName(),dataObjectModification.isEnabled(),
                        dataObjectModification.getMonitorProtocol());
                if (interfaceManager.isItmDirectTunnelsEnabled()) {
                    ItmMonitorWorker toggleWorker = new ItmMonitorWorker(tzone.getZoneName(),
                        dataObjectModification.isEnabled(), monitorProtocol, broker, directTunnelUtils,
                        dpnTepStateCache, ovsBridgeRefEntryCache);
                    jobCoordinator.enqueueJob(tzone.getZoneName(), toggleWorker);
                } else {
                    ItmMonitorToggleWorker toggleWorker = new ItmMonitorToggleWorker(tzone.getZoneName(),
                        dataObjectModification.isEnabled(), monitorProtocol, broker);
                    jobCoordinator.enqueueJob(tzone.getZoneName(), toggleWorker);
                }
            }
        }
    }
}
