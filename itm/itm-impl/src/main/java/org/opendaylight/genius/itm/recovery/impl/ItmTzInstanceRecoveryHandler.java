/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.recovery.impl;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.listeners.DataTreeEventCallbackRegistrar;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.srm.ServiceRecoveryInterface;
import org.opendaylight.genius.srm.ServiceRecoveryRegistry;
import org.opendaylight.genius.tools.mdsal.listener.AbstractSyncDataTreeChangeListener;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelsState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.srm.types.rev170711.GeniusItmTz;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ItmTzInstanceRecoveryHandler extends
        AbstractSyncDataTreeChangeListener<StateTunnelList> implements ServiceRecoveryInterface {

    private static final Logger LOG = LoggerFactory.getLogger(ItmTzInstanceRecoveryHandler.class);

    private final JobCoordinator jobCoordinator;
    private final DataBroker dataBroker;
    private final ManagedNewTransactionRunner txRunner;
    private final EntityOwnershipUtils entityOwnershipUtils;
    private final DataTreeEventCallbackRegistrar eventCallbacks;
    private final EntityOwnershipService entityOwnershipService;

    @Inject
    public ItmTzInstanceRecoveryHandler(DataBroker dataBroker,
                                        JobCoordinator jobCoordinator,
                                        ServiceRecoveryRegistry serviceRecoveryRegistry,
                                        EntityOwnershipUtils entityOwnershipUtils,
                                        EntityOwnershipService entityOwnershipService,
                                        DataTreeEventCallbackRegistrar eventCallbacks) {
        super(dataBroker, LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.create(TunnelsState.class).child(StateTunnelList.class));
        this.dataBroker = dataBroker;
        this.jobCoordinator = jobCoordinator;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.entityOwnershipUtils = entityOwnershipUtils;
        this.entityOwnershipService = entityOwnershipService;
        serviceRecoveryRegistry.registerServiceRecoveryRegistry(getServiceRegistryKey(), this);
        this.eventCallbacks = eventCallbacks;
    }

    private String getServiceRegistryKey() {
        return GeniusItmTz.class.toString();
    }

    @Override
    public void recoverService(String entityId) {
        if (!entityOwnershipUtils.isEntityOwner(ITMConstants.ITM_CONFIG_ENTITY, ITMConstants.ITM_CONFIG_ENTITY)) {
            return;
        }
        LOG.info("Trigerred recovery of ITM Instance - TZ Name {}", entityId);
        try {
            recoverTransportZone(entityId);
        } catch (InterruptedException e) {
            LOG.error("ITM instance transportzone has not recovered", e);
        }
    }

    private void recoverTransportZone(String entityId) throws InterruptedException {
        //List of Internel tunnels
        List<String> tunnelList = ItmUtils.getInternalTunnelInterfaces(dataBroker);
        LOG.debug("List of tunnel interfaces: {}" , tunnelList);
        InstanceIdentifier<TransportZone> tzII = ItmUtils.getTZInstanceIdentifier(entityId);
        TransportZone tz = ItmUtils.getTransportZoneFromConfigDS(entityId , dataBroker);
        if (tz != null) {
            LOG.trace("deleting transportzone instance {}", entityId);
            jobCoordinator.enqueueJob(entityId, () -> Collections.singletonList(
                txRunner.callWithNewWriteOnlyTransactionAndSubmit(
                    tx -> tx.delete(LogicalDatastoreType.CONFIGURATION, tzII))), ITMConstants.JOB_MAX_RETRIES);
            AtomicInteger eventCallbackCount = new AtomicInteger(0);
            if (!tunnelList.isEmpty()) {
                tunnelList.forEach(tunnelInterface -> {
                    StateTunnelListKey tlKey = new StateTunnelListKey(tunnelInterface);
                    LOG.trace("TunnelStateKey: {} for interface: {}", tlKey, tunnelInterface);
                    InstanceIdentifier<StateTunnelList> stListId = ItmUtils.buildStateTunnelListId(tlKey);
                    eventCallbacks.onRemove(LogicalDatastoreType.OPERATIONAL, stListId, (unused) -> {
                        LOG.trace("callback event for a delete {} interface instance", stListId);
                        // recreating the transportZone
                        recreateTZ(entityId, tz, tzII, tunnelList.size(), eventCallbackCount);
                        return DataTreeEventCallbackRegistrar.NextAction.UNREGISTER;
                    }, Duration.ofMillis(5000), (id) -> {
                            LOG.trace("event callback timed out for {} tunnel interface ", tunnelInterface);
                            recreateTZ(entityId, tz, tzII, tunnelList.size(), eventCallbackCount); });
                });
            } else {
                recreateTZ(entityId, tz, tzII, tunnelList.size(), eventCallbackCount);
            }
        }
    }

    //this function will recreate the transportzone instance
    private void recreateTZ(String entityId, TransportZone tz, InstanceIdentifier<TransportZone> tzII,
                            int sizeOfTunnelList, AtomicInteger registeredEvents) {
        registeredEvents.incrementAndGet();
        if (registeredEvents.intValue() == sizeOfTunnelList || sizeOfTunnelList == 0) {
            LOG.trace("recreating transportzone instance {}", entityId);
            jobCoordinator.enqueueJob(entityId, () -> Collections.singletonList(
                txRunner.callWithNewWriteOnlyTransactionAndSubmit(
                    tx -> tx.put(LogicalDatastoreType.CONFIGURATION, tzII, tz))),
                ITMConstants.JOB_MAX_RETRIES);
        } else {
            LOG.trace("{} call back events registered for {} tunnel interfaces",
                    registeredEvents, sizeOfTunnelList);
        }
    }
}