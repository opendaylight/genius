/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.listeners;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncDataTreeChangeListenerBase;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.itm.confighelpers.HwVtep;
import org.opendaylight.genius.itm.confighelpers.ItmMonitorToggleWorker;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorParams;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.Subnets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.DeviceVteps;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TunnelMonitorChangeListener  extends AsyncDataTreeChangeListenerBase<TunnelMonitorParams, TunnelMonitorChangeListener>
        implements  AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(TunnelMonitorChangeListener.class);
    private final DataBroker broker;
    // private final IInterfaceManager interfaceManager;

    public TunnelMonitorChangeListener(final DataBroker db) {
        super(TunnelMonitorParams.class, TunnelMonitorChangeListener.class);
        broker = db;
        // interfaceManager = ifManager;
        // registerListener(db);
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
        List<HwVtep> hwVteps = new ArrayList<>();
        Boolean hwVtepsExist = false;
        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
        InstanceIdentifier<TransportZones> path = InstanceIdentifier.builder(TransportZones.class).build();
        Optional<TransportZones> tZonesOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, path, broker);
        Class<? extends TunnelMonitoringTypeBase> monitorProtocol = dataObjectModification.getMonitorProtocol();
        if (tZonesOptional.isPresent()) {
            TransportZones tZones = tZonesOptional.get();
            for (TransportZone tzone : tZones.getTransportZone()) {
                hwVtepsExist = false;
                hwVteps = new ArrayList<>();
                if (tzone.getSubnets() != null && !tzone.getSubnets().isEmpty()) {
                    for (Subnets sub : tzone.getSubnets()) {
                        if (sub.getDeviceVteps() != null && !sub.getDeviceVteps().isEmpty()) {
                            hwVtepsExist = true;
                            for (DeviceVteps deviceVtep : sub.getDeviceVteps()) {
                                HwVtep hwVtep = ItmUtils.createHwVtepObject(deviceVtep.getTopologyId(), deviceVtep.getNodeId(),
                                        deviceVtep.getIpAddress(), sub.getPrefix(), sub.getGatewayIp(), sub.getVlanId(),
                                        tzone.getTunnelType(), tzone);
                                hwVteps.add(hwVtep);
                            }
                        }
                    }
                }
                if(monitorProtocol==null)
                    monitorProtocol = ITMConstants.DEFAULT_MONITOR_PROTOCOL;
                LOG.debug("Remove:Calling TunnelMonitorToggleWorker with tzone = {} and {}",tzone.getZoneName(),dataObjectModification.isEnabled());
                LOG.debug("Update:Calling TunnelMonitorToggleWorker with monitor protocol = {} ",monitorProtocol);
                ItmMonitorToggleWorker toggleWorker = new ItmMonitorToggleWorker(hwVteps, tzone.getZoneName(),
                        false,monitorProtocol, broker, hwVtepsExist);
                coordinator.enqueueJob(tzone.getZoneName(), toggleWorker);
            }
        }
    }


    @Override protected void update(InstanceIdentifier<TunnelMonitorParams> key,
                                    TunnelMonitorParams dataObjectModificationBefore,
                                    TunnelMonitorParams dataObjectModificationAfter) {
        LOG.debug("update TunnelMonitorChangeListener called with {}",dataObjectModificationAfter.isEnabled());
        List<HwVtep> hwVteps = new ArrayList<>();
        Boolean hwVtepsExist = false;
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
            LOG.debug("Update TunnelMonitorChangeListener: Existing monitor protocol {}",monitorProtocol_before.getName());
            LOG.debug("Update TunnelMonitorChangeListener: New monitor protocol {}",monitorProtocol_after.getName());
            if(!monitorProtocol_after.getName().equalsIgnoreCase(monitorProtocol_before.getName()))
                LOG.error("Updation of monitor protocol not allowed");

        }
        if (tZonesOptional.isPresent()) {
            TransportZones tZones = tZonesOptional.get();
            for (TransportZone tzone : tZones.getTransportZone()) {
                hwVtepsExist = false;
                hwVteps = new ArrayList<>();
                if (tzone.getSubnets() != null && !tzone.getSubnets().isEmpty()) {
                    for (Subnets sub : tzone.getSubnets()) {
                        if (sub.getDeviceVteps() != null && !sub.getDeviceVteps().isEmpty()) {
                            hwVtepsExist = true;//gets set to true only if this particular tzone has
                            LOG.debug("Update:Calling TunnelMonitorToggleWorker with tzone = {} and hwtepExist",tzone.getZoneName());
                            for (DeviceVteps deviceVtep : sub.getDeviceVteps()) {
                                HwVtep hwVtep = ItmUtils.createHwVtepObject(deviceVtep.getTopologyId(), deviceVtep.getNodeId(),
                                        deviceVtep.getIpAddress(), sub.getPrefix(), sub.getGatewayIp(), sub.getVlanId(),
                                        tzone.getTunnelType(), tzone);
                                hwVteps.add(hwVtep);
                            }
                        }
                    }
                }
                LOG.debug("Update:Calling TunnelMonitorToggleWorker with tzone = {} and {}",tzone.getZoneName(),dataObjectModificationAfter.isEnabled());
                LOG.debug("Update:Calling TunnelMonitorToggleWorker with monitor protocol = {} ",monitorProtocol);
                ItmMonitorToggleWorker toggleWorker = new ItmMonitorToggleWorker(hwVteps, tzone.getZoneName(),
                        dataObjectModificationAfter.isEnabled(), monitorProtocol, broker, hwVtepsExist);
                coordinator.enqueueJob(tzone.getZoneName(), toggleWorker);
            }
        }
    }

    @Override
    protected void add(InstanceIdentifier<TunnelMonitorParams> key, TunnelMonitorParams dataObjectModification) {
        LOG.debug("add TunnelMonitorChangeListener called with {}",dataObjectModification.isEnabled());
        LOG.debug("add TunnelMonitorChangeListener called with monitorProtcol {}",dataObjectModification.getMonitorProtocol());
        List<HwVtep> hwVteps = new ArrayList<>();
        Boolean hwVtepsExist = false;
        Class<? extends TunnelMonitoringTypeBase> monitorProtocol = dataObjectModification.getMonitorProtocol();
        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
        InstanceIdentifier<TransportZones> path = InstanceIdentifier.builder(TransportZones.class).build();
        Optional<TransportZones> tZonesOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, path, broker);
        if (tZonesOptional.isPresent()) {
            TransportZones tZones = tZonesOptional.get();
            for (TransportZone tzone : tZones.getTransportZone()) {
                hwVtepsExist = false;
                hwVteps = new ArrayList<>();
                if (tzone.getSubnets() != null && !tzone.getSubnets().isEmpty()) {
                    for (Subnets sub : tzone.getSubnets()) {
                        if (sub.getDeviceVteps() != null && !sub.getDeviceVteps().isEmpty()) {
                            hwVtepsExist = true;
                            for (DeviceVteps deviceVtep : sub.getDeviceVteps()) {
                                HwVtep hwVtep = ItmUtils.createHwVtepObject(deviceVtep.getTopologyId(), deviceVtep.getNodeId(),
                                        deviceVtep.getIpAddress(), sub.getPrefix(), sub.getGatewayIp(), sub.getVlanId(),
                                        tzone.getTunnelType(), tzone);
                                hwVteps.add(hwVtep);
                            }
                        }
                    }
                }
                LOG.debug("Add:Calling TunnelMonitorToggleWorker with tzone = {} monitoringEnabled {} and monitoringProtocol {}",tzone.getZoneName(),dataObjectModification.isEnabled(), dataObjectModification.getMonitorProtocol());
                if(monitorProtocol==null)
                    monitorProtocol = ITMConstants.DEFAULT_MONITOR_PROTOCOL;
                LOG.debug("Add:Calling TunnelMonitorToggleWorker with monitor protocol = {} ",monitorProtocol);
                ItmMonitorToggleWorker toggleWorker = new ItmMonitorToggleWorker(hwVteps, tzone.getZoneName(),
                        dataObjectModification.isEnabled(), monitorProtocol, broker, hwVtepsExist);
                coordinator.enqueueJob(tzone.getZoneName(), toggleWorker);
            }
        }
    }

    @Override protected TunnelMonitorChangeListener getDataTreeChangeListener() {
        return TunnelMonitorChangeListener.this;
    }

}
