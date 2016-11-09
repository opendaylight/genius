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
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncDataTreeChangeListenerBase;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.itm.confighelpers.HwVtep;
import org.opendaylight.genius.itm.confighelpers.ItmMonitorIntervalWorker;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorInterval;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class TunnelMonitorIntervalListener  extends AsyncDataTreeChangeListenerBase<TunnelMonitorInterval, TunnelMonitorIntervalListener>
        implements  AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(TunnelMonitorIntervalListener.class);
    private ListenerRegistration<DataChangeListener> monitorIntervalListenerRegistration;
    private final DataBroker broker;

    public TunnelMonitorIntervalListener(DataBroker db) {
        super(TunnelMonitorInterval.class, TunnelMonitorIntervalListener.class);
        broker = db;
    }

    @Override protected InstanceIdentifier<TunnelMonitorInterval> getWildCardPath() {
        return InstanceIdentifier.create(TunnelMonitorInterval.class);
    }

    @Override
    protected void remove(InstanceIdentifier<TunnelMonitorInterval> key, TunnelMonitorInterval dataObjectModification) {
        LOG.debug("remove TunnelMonitorIntervalListener called with {}",dataObjectModification.getInterval());
        List<HwVtep> hwVteps = new ArrayList<>();
        Boolean hwVtepsExist = false;
        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
        InstanceIdentifier<TransportZones> path = InstanceIdentifier.builder(TransportZones.class).build();
        Optional<TransportZones> tZonesOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, path, broker);
        if (tZonesOptional.isPresent()) {
            TransportZones tZones = tZonesOptional.get();
            for (TransportZone tzone : tZones.getTransportZone()) {
              /*  hwVtepsExist = false;
                hwVteps = new ArrayList<HwVtep>();
                if (tzone.getSubnets() != null && !tzone.getSubnets().isEmpty()) {
                    for (Subnets sub : tzone.getSubnets()) {
                        if (sub.getDeviceVteps() != null && !sub.getDeviceVteps().isEmpty()) {
                            hwVtepsExist = true;
                            LOG.debug("Remove:Calling TunnelMonitorIntervalWorker with tzone = {} and hwtepExist",tzone.getZoneName());
                            for (DeviceVteps deviceVtep : sub.getDeviceVteps()) {
                                HwVtep hwVtep = ItmUtils.createHwVtepObject(deviceVtep.getTopologyId(), deviceVtep.getNodeId(),
                                                deviceVtep.getIpAddress(), sub.getPrefix(), sub.getGatewayIp(), sub.getVlanId(),
                                                tzone.getTunnelType(), tzone);
                                hwVteps.add(hwVtep);
                            }
                        }
                    }
                }*/
                //if you remove configuration, the last configured interval is only set i.e no change
                LOG.debug("Remove:Calling TunnelMonitorIntervalWorker with tzone = {} and {}",tzone.getZoneName(),dataObjectModification.getInterval());
                ItmMonitorIntervalWorker toggleWorker = new ItmMonitorIntervalWorker(hwVteps, tzone.getZoneName(),
                        dataObjectModification.getInterval(), broker, hwVtepsExist);
                coordinator.enqueueJob(tzone.getZoneName(), toggleWorker);
            }
        }
    }

    @Override protected void update(InstanceIdentifier<TunnelMonitorInterval> key,
                                    TunnelMonitorInterval dataObjectModificationBefore,
                                    TunnelMonitorInterval dataObjectModificationAfter) {
        LOG.debug("update TunnelMonitorIntervalListener called with {}",dataObjectModificationAfter.getInterval());
        List<HwVtep> hwVteps = new ArrayList<>();
        Boolean hwVtepsExist = false;
        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
        InstanceIdentifier<TransportZones> path = InstanceIdentifier.builder(TransportZones.class).build();
        Optional<TransportZones> tZonesOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, path, broker);
        if (tZonesOptional.isPresent()) {
            TransportZones tZones = tZonesOptional.get();
            for (TransportZone tzone : tZones.getTransportZone()) {
                /*hwVtepsExist = false;
                hwVteps = new ArrayList<HwVtep>();
                if (tzone.getSubnets() != null && !tzone.getSubnets().isEmpty()) {
                    for (Subnets sub : tzone.getSubnets()) {
                        if (sub.getDeviceVteps() != null && !sub.getDeviceVteps().isEmpty()) {
                            hwVtepsExist = true;//gets set to true only if this particular tzone has
                            LOG.debug("Update:Calling TunnelMonitorIntervalWorker with tzone = {} and hwtepExist",tzone.getZoneName());
                            for (DeviceVteps deviceVtep : sub.getDeviceVteps()) {
                                HwVtep hwVtep = ItmUtils.createHwVtepObject(deviceVtep.getTopologyId(), deviceVtep.getNodeId(),
                                                deviceVtep.getIpAddress(), sub.getPrefix(), sub.getGatewayIp(), sub.getVlanId(),
                                                tzone.getTunnelType(), tzone);
                                hwVteps.add(hwVtep);
                            }
                        }
                    }
                }*/
                LOG.debug("Update:Calling TunnelMonitorIntervalWorker with tzone = {} and {}",tzone.getZoneName(),dataObjectModificationAfter.getInterval());
                ItmMonitorIntervalWorker intervalWorker = new ItmMonitorIntervalWorker(hwVteps, tzone.getZoneName(),
                        dataObjectModificationAfter.getInterval(), broker, hwVtepsExist);
                coordinator.enqueueJob(tzone.getZoneName(), intervalWorker);
            }
        }
    }

    @Override
    protected void add(InstanceIdentifier<TunnelMonitorInterval> key, TunnelMonitorInterval dataObjectModification) {
        LOG.debug("Add TunnelMonitorIntervalListener called with {}",dataObjectModification.getInterval());
        List<HwVtep> hwVteps = new ArrayList<>();
        Boolean hwVtepsExist = false;
        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
        InstanceIdentifier<TransportZones> path = InstanceIdentifier.builder(TransportZones.class).build();
        Optional<TransportZones> tZonesOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, path, broker);
        if (tZonesOptional.isPresent()) {
            TransportZones tZones = tZonesOptional.get();
            for (TransportZone tzone : tZones.getTransportZone()) {
                /*hwVtepsExist = false;
                hwVteps = new ArrayList<HwVtep>();
                if (tzone.getSubnets() != null && !tzone.getSubnets().isEmpty()) {
                    for (Subnets sub : tzone.getSubnets()) {
                        if (sub.getDeviceVteps() != null && !sub.getDeviceVteps().isEmpty()) {
                            hwVtepsExist = true;
                            LOG.debug("Add:Calling TunnelMonitorIntervalWorker with tzone = {} and hwtepExist",tzone.getZoneName());
                            for (DeviceVteps deviceVtep : sub.getDeviceVteps()) {
                                HwVtep hwVtep = ItmUtils.createHwVtepObject(deviceVtep.getTopologyId(), deviceVtep.getNodeId(),
                                                deviceVtep.getIpAddress(), sub.getPrefix(), sub.getGatewayIp(), sub.getVlanId(),
                                                tzone.getTunnelType(), tzone);
                                hwVteps.add(hwVtep);
                            }
                        }
                    }
                }*/
                LOG.debug("Add:Calling TunnelMonitorIntervalWorker with tzone = {} and {}",tzone.getZoneName(),dataObjectModification.getInterval());
                ItmMonitorIntervalWorker intervalWorker = new ItmMonitorIntervalWorker(hwVteps, tzone.getZoneName(),
                        dataObjectModification.getInterval(), broker, hwVtepsExist);
                //conversion to milliseconds done while writing to i/f-mgr config DS
                coordinator.enqueueJob(tzone.getZoneName(), intervalWorker);
            }
        }
    }

    @Override protected TunnelMonitorIntervalListener getDataTreeChangeListener() {
        return TunnelMonitorIntervalListener.this;
    }
}
