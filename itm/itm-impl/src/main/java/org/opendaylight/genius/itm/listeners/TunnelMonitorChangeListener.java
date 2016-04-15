/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
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
import org.opendaylight.genius.itm.confighelpers.HwVtep;
import org.opendaylight.genius.itm.confighelpers.ItmMonitorToggleWorker;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev151102.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.Subnets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.DeviceVteps;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class TunnelMonitorChangeListener  extends AsyncDataTreeChangeListenerBase<TunnelMonitorEnabled, TunnelMonitorChangeListener>
                implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(TunnelMonitorChangeListener.class);
    private final DataBroker broker;
   // private final IInterfaceManager interfaceManager;

    public TunnelMonitorChangeListener(final DataBroker db) {
        super(TunnelMonitorEnabled.class, TunnelMonitorChangeListener.class);
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

    @Override
    protected InstanceIdentifier<TunnelMonitorEnabled> getWildCardPath() {
        return InstanceIdentifier.create(TunnelMonitorEnabled.class);
    }

    @Override
    protected void remove(InstanceIdentifier<TunnelMonitorEnabled> key, TunnelMonitorEnabled dataObjectModification) {
        List<HwVtep> hwVteps = new ArrayList<HwVtep>();
        Boolean hwVtepsExist = false;
        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
        InstanceIdentifier<TransportZones> path = InstanceIdentifier.builder(TransportZones.class).build();
        Optional<TransportZones> tZonesOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, path, broker);
        if (tZonesOptional.isPresent()) {
            TransportZones tZones = tZonesOptional.get();
            for (TransportZone tzone : tZones.getTransportZone()) {
                hwVtepsExist = false;
                hwVteps = new ArrayList<HwVtep>();
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
                LOG.debug("Remove:Calling TunnelMonitorToggleWorker with tzone = {} and {}",tzone.getZoneName(),dataObjectModification.isEnabled());
                ItmMonitorToggleWorker toggleWorker = new ItmMonitorToggleWorker(hwVteps, tzone.getZoneName(),
                                false, broker, hwVtepsExist);
                coordinator.enqueueJob(tzone.getZoneName(), toggleWorker);
            }
        }
    }


    @Override
    protected void update(InstanceIdentifier<TunnelMonitorEnabled> key,
                          TunnelMonitorEnabled dataObjectModificationBefore,
                          TunnelMonitorEnabled dataObjectModificationAfter) {
        LOG.debug("update TunnelMonitorChangeListener called with {}",dataObjectModificationAfter.isEnabled());
        List<HwVtep> hwVteps = new ArrayList<HwVtep>();
        Boolean hwVtepsExist = false;
        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
        InstanceIdentifier<TransportZones> path = InstanceIdentifier.builder(TransportZones.class).build();
        Optional<TransportZones> tZonesOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, path, broker);
        if (tZonesOptional.isPresent()) {
            TransportZones tZones = tZonesOptional.get();
            for (TransportZone tzone : tZones.getTransportZone()) {
                hwVtepsExist = false;
                hwVteps = new ArrayList<HwVtep>();
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
                ItmMonitorToggleWorker toggleWorker = new ItmMonitorToggleWorker(hwVteps, tzone.getZoneName(),
                                dataObjectModificationAfter.isEnabled(), broker, hwVtepsExist);
                coordinator.enqueueJob(tzone.getZoneName(), toggleWorker);
            }
        }
    }

    @Override
    protected void add(InstanceIdentifier<TunnelMonitorEnabled> key, TunnelMonitorEnabled dataObjectModification) {
        LOG.debug("add TunnelMonitorChangeListener called with {}",dataObjectModification.isEnabled());
        List<HwVtep> hwVteps = new ArrayList<HwVtep>();
        Boolean hwVtepsExist = false;
        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
        InstanceIdentifier<TransportZones> path = InstanceIdentifier.builder(TransportZones.class).build();
        Optional<TransportZones> tZonesOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, path, broker);
        if (tZonesOptional.isPresent()) {
            TransportZones tZones = tZonesOptional.get();
            for (TransportZone tzone : tZones.getTransportZone()) {
                hwVtepsExist = false;
                hwVteps = new ArrayList<HwVtep>();
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
                LOG.debug("Add:Calling TunnelMonitorToggleWorker with tzone = {} and {}",tzone.getZoneName(),dataObjectModification.isEnabled());
                ItmMonitorToggleWorker toggleWorker = new ItmMonitorToggleWorker(hwVteps, tzone.getZoneName(),
                                dataObjectModification.isEnabled(), broker, hwVtepsExist);
                coordinator.enqueueJob(tzone.getZoneName(), toggleWorker);
            }
        }
    }

    @Override
    protected TunnelMonitorChangeListener getDataTreeChangeListener() {
        return TunnelMonitorChangeListener.this;
    }

}
