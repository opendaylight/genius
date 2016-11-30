/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.listeners;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncDataTreeChangeListenerBase;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.itm.confighelpers.ItmMonitorToggleWorker;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorParams;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TunnelMonitorChangeListener  extends AsyncDataTreeChangeListenerBase<TunnelMonitorParams, TunnelMonitorChangeListener>
        implements  AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(TunnelMonitorChangeListener.class);
    private final DataBroker broker;
    // private final IInterfaceManager interfaceManager;

    @Inject
    public TunnelMonitorChangeListener(final DataBroker dataBroker) {
        super(TunnelMonitorParams.class, TunnelMonitorChangeListener.class);
        this.broker = dataBroker;
        // interfaceManager = ifManager;
        // registerListener(db);
    }

    @PostConstruct
    public void start() throws  Exception {
        registerListener(LogicalDatastoreType.CONFIGURATION, this.broker);
        LOG.info("Tunnel Monitor listeners Started");
    }

    /* private void registerListener(final DataBroker db) {
         try {
             TunnelMonitorChangeListener monitorEnabledChangeListener = new TunnelMonitorChangeListener();
             monitorEnabledListenerRegistration = db.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                     monitorEnabledChangeListener.getWildCardPath(), monitorEnabledChangeListener, DataChangeScope.SUBTREE);
         } catch (final Exception e) {
             LOG.error("ITM Monitor Interfaces DataChange listener registration fail!", e);
             throw new IllegalStateException("ITM Monitor registration Listener failed.", e);
         }
     }
 */    @Override
    @PreDestroy
    public void close() throws Exception {
       /* if (monitorEnabledListenerRegistration != null) {
            try {
                monitorEnabledListenerRegistration.close();
            } catch (final Exception e) {
                LOG.error("Error when cleaning up DataChangeListener.", e);
            }
            monitorEnabledListenerRegistration = null;
        }

        if (monitorIntervalListenerRegistration != null) {
            try {
                monitorIntervalListenerRegistration.close();
            } catch (final Exception e) {
                LOG.error("Error when cleaning up DataChangeListener.", e);
            }
            monitorIntervalListenerRegistration = null;
        }
*/
        LOG.info("Tunnel Monitor listeners Closed");
    }

    @Override protected InstanceIdentifier<TunnelMonitorParams> getWildCardPath() {
        return InstanceIdentifier.create(TunnelMonitorParams.class);
    }

    @Override
    protected void remove(InstanceIdentifier<TunnelMonitorParams> key, TunnelMonitorParams dataObjectModification) {
        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
        InstanceIdentifier<TransportZones> path = InstanceIdentifier.builder(TransportZones.class).build();
        Optional<TransportZones> tZonesOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, path, broker);
        Class<? extends TunnelMonitoringTypeBase> monitorProtocol = dataObjectModification.getMonitorProtocol();
        if(monitorProtocol==null)
            monitorProtocol = ITMConstants.DEFAULT_MONITOR_PROTOCOL;
        if (tZonesOptional.isPresent()) {
            TransportZones tZones = tZonesOptional.get();
            for (TransportZone tzone : tZones.getTransportZone()) {
                LOG.debug("Remove - TunnelMonitorToggleWorker with tzone = {}, Enable = {}, MonitorProtocol = {}",tzone.getZoneName(),dataObjectModification.isEnabled(), monitorProtocol);
                ItmMonitorToggleWorker toggleWorker = new ItmMonitorToggleWorker(tzone.getZoneName(),
                        false,monitorProtocol, broker);
                coordinator.enqueueJob(tzone.getZoneName(), toggleWorker);
            }
        }
    }


    @Override protected void update(InstanceIdentifier<TunnelMonitorParams> key,
                                    TunnelMonitorParams dataObjectModificationBefore,
                                    TunnelMonitorParams dataObjectModificationAfter) {
        LOG.debug("update TunnelMonitorChangeListener called with {}",dataObjectModificationAfter.isEnabled());
        Class<? extends TunnelMonitoringTypeBase> monitorProtocol_before = dataObjectModificationBefore.getMonitorProtocol();
        Class<? extends TunnelMonitoringTypeBase> monitorProtocol_after = dataObjectModificationAfter.getMonitorProtocol();
        Class<? extends TunnelMonitoringTypeBase> monitorProtocol = ITMConstants.DEFAULT_MONITOR_PROTOCOL;
        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
        InstanceIdentifier<TransportZones> path = InstanceIdentifier.builder(TransportZones.class).build();
        Optional<TransportZones> tZonesOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, path, broker);
        if(monitorProtocol_after!=null )
            monitorProtocol = dataObjectModificationAfter.getMonitorProtocol();
        if(monitorProtocol_before!=null && monitorProtocol_after!=null)
        {
            LOG.debug("TunnelMonitorChangeListener Update : Existing_MonitorProtocol {}, New_MonitorProtocol {} ",monitorProtocol_before.getName(), monitorProtocol_after.getName());
            if(!monitorProtocol_after.getName().equalsIgnoreCase(monitorProtocol_before.getName()))
                LOG.error("Updation of monitor protocol not allowed");

        }
        if (tZonesOptional.isPresent()) {
            TransportZones tZones = tZonesOptional.get();
            for (TransportZone tzone : tZones.getTransportZone()) {
                LOG.debug("Update - TunnelMonitorToggleWorker with tzone = {}, Enable = {}, MonitorProtocol = {}",tzone.getZoneName(),dataObjectModificationAfter.isEnabled(), monitorProtocol);
                ItmMonitorToggleWorker toggleWorker = new ItmMonitorToggleWorker(tzone.getZoneName(),
                        dataObjectModificationAfter.isEnabled(), monitorProtocol, broker);
                coordinator.enqueueJob(tzone.getZoneName(), toggleWorker);
            }
        }
    }

    @Override
    protected void add(InstanceIdentifier<TunnelMonitorParams> key, TunnelMonitorParams dataObjectModification) {
        LOG.debug("Add - TunnelMonitorToggleWorker with Enable = {}, MonitorProtocol = {}",dataObjectModification.isEnabled(), dataObjectModification.getMonitorProtocol());
        Class<? extends TunnelMonitoringTypeBase> monitorProtocol = dataObjectModification.getMonitorProtocol();
        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
        InstanceIdentifier<TransportZones> path = InstanceIdentifier.builder(TransportZones.class).build();
        Optional<TransportZones> tZonesOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, path, broker);
        if(monitorProtocol==null)
            monitorProtocol = ITMConstants.DEFAULT_MONITOR_PROTOCOL;
        if (tZonesOptional.isPresent()) {
            TransportZones tZones = tZonesOptional.get();
            for (TransportZone tzone : tZones.getTransportZone()) {
                LOG.debug("Add: TunnelMonitorToggleWorker with tzone = {} monitoringEnabled {} and monitoringProtocol {}",tzone.getZoneName(),dataObjectModification.isEnabled(), dataObjectModification.getMonitorProtocol());
                ItmMonitorToggleWorker toggleWorker = new ItmMonitorToggleWorker(tzone.getZoneName(),
                        dataObjectModification.isEnabled(), monitorProtocol, broker);
                coordinator.enqueueJob(tzone.getZoneName(), toggleWorker);
            }
        }
    }

    @Override protected TunnelMonitorChangeListener getDataTreeChangeListener() {
        return TunnelMonitorChangeListener.this;
    }

}
