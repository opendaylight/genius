/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.listeners;

import com.google.common.base.Optional;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncDataTreeChangeListenerBase;
import org.opendaylight.genius.itm.cache.BfdStateCache;
import org.opendaylight.genius.itm.cache.DpnTepStateCache;
import org.opendaylight.genius.itm.cache.OvsBridgeEntryCache;
import org.opendaylight.genius.itm.cache.OvsBridgeRefEntryCache;
import org.opendaylight.genius.itm.cache.TunnelStateCache;
import org.opendaylight.genius.itm.cache.OvsBridgeRefEntryCache;
import org.opendaylight.genius.itm.confighelpers.ItmMonitorToggleWorker;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorParams;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class TunnelMonitorChangeListener
        extends AsyncDataTreeChangeListenerBase<TunnelMonitorParams, TunnelMonitorChangeListener> {

    private static final Logger LOG = LoggerFactory.getLogger(TunnelMonitorChangeListener.class);

    private final DataBroker broker;
    private final JobCoordinator jobCoordinator;
    private final BfdStateCache bfdStateCache;
    private final DirectTunnelUtils directTunnelUtils;
    private final IInterfaceManager iInterfaceManager;
    private final DpnTepStateCache dpnTepStateCache;
    private final OvsBridgeRefEntryCache ovsBridgeRefEntryCache;

    @Inject
    public TunnelMonitorChangeListener(final DataBroker dataBroker, JobCoordinator jobCoordinator,
                                       final BfdStateCache bfdStateCache,
                                       final DirectTunnelUtils directTunnelUtils,
                                       final DpnTepStateCache dpnTepStateCache,
                                       final OvsBridgeRefEntryCache ovsBridgeRefEntryCache,
                                       final IInterfaceManager interfaceManager) {
        super(TunnelMonitorParams.class, TunnelMonitorChangeListener.class);
        this.broker = dataBroker;
        this.jobCoordinator = jobCoordinator;
        this.bfdStateCache = bfdStateCache;
        this.directTunnelUtils = directTunnelUtils;
        this.dpnTepStateCache = dpnTepStateCache;
        this.iInterfaceManager = interfaceManager;
        this.ovsBridgeRefEntryCache = ovsBridgeRefEntryCache;
    }

    @PostConstruct
    public void start() throws  Exception {
        registerListener(LogicalDatastoreType.CONFIGURATION, this.broker);
        LOG.info("Tunnel Monitor listeners Started");
    }

    @Override
    @PreDestroy
    public void close() {
        LOG.info("Tunnel Monitor listeners Closed");
    }

    @Override protected InstanceIdentifier<TunnelMonitorParams> getWildCardPath() {
        return InstanceIdentifier.create(TunnelMonitorParams.class);
    }

    @Override
    protected void remove(InstanceIdentifier<TunnelMonitorParams> key, TunnelMonitorParams dataObjectModification) {
        InstanceIdentifier<TransportZones> path = InstanceIdentifier.builder(TransportZones.class).build();
        Optional<TransportZones> transportZonesOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION,
                path, broker);
        Class<? extends TunnelMonitoringTypeBase> monitorProtocol = dataObjectModification.getMonitorProtocol();
        if (monitorProtocol == null) {
            monitorProtocol = ITMConstants.DEFAULT_MONITOR_PROTOCOL;
        }
        if (transportZonesOptional.isPresent()) {
            TransportZones transportZones = transportZonesOptional.get();
            for (TransportZone tzone : transportZones.getTransportZone()) {
                LOG.debug("Remove - TunnelMonitorToggleWorker with tzone = {}, Enable = {}, MonitorProtocol = {}",
                        tzone.getZoneName(),dataObjectModification.isEnabled(), monitorProtocol);
                ItmMonitorToggleWorker toggleWorker = new ItmMonitorToggleWorker(tzone.getZoneName(),
                        false,monitorProtocol, broker, bfdStateCache, directTunnelUtils, dpnTepStateCache, iInterfaceManager, ovsBridgeRefEntryCache);
                jobCoordinator.enqueueJob(tzone.getZoneName(), toggleWorker);
            }
        }
    }

    @Override
    protected void update(InstanceIdentifier<TunnelMonitorParams> key,
                                    TunnelMonitorParams dataObjectModificationBefore,
                                    TunnelMonitorParams dataObjectModificationAfter) {
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
            TransportZones tzones = transportZonesOptional.get();
            for (TransportZone tzone : tzones.getTransportZone()) {
                LOG.debug("Update - TunnelMonitorToggleWorker with tzone = {}, Enable = {}, MonitorProtocol = {}",
                        tzone.getZoneName(),dataObjectModificationAfter.isEnabled(), monitorProtocol);
                ItmMonitorToggleWorker toggleWorker = new ItmMonitorToggleWorker(tzone.getZoneName(),
                        dataObjectModificationAfter.isEnabled(), monitorProtocol, broker, bfdStateCache, directTunnelUtils, dpnTepStateCache, iInterfaceManager, ovsBridgeRefEntryCache);
                jobCoordinator.enqueueJob(tzone.getZoneName(), toggleWorker);
            }
        }
    }

    @Override
    protected void add(InstanceIdentifier<TunnelMonitorParams> key, TunnelMonitorParams dataObjectModification) {
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
            TransportZones tzones = transportZonesOptional.get();
            for (TransportZone tzone : tzones.getTransportZone()) {
                LOG.debug("Add: TunnelMonitorToggleWorker with tzone = {} monitoringEnabled {} and "
                        + "monitoringProtocol {}",tzone.getZoneName(),dataObjectModification.isEnabled(),
                        dataObjectModification.getMonitorProtocol());
                ItmMonitorToggleWorker toggleWorker = new ItmMonitorToggleWorker(tzone.getZoneName(),
                        dataObjectModification.isEnabled(), monitorProtocol, broker, bfdStateCache, directTunnelUtils, dpnTepStateCache, iInterfaceManager, ovsBridgeRefEntryCache);
                jobCoordinator.enqueueJob(tzone.getZoneName(), toggleWorker);
            }
        }
    }

    @Override protected TunnelMonitorChangeListener getDataTreeChangeListener() {
        return TunnelMonitorChangeListener.this;
    }
}
