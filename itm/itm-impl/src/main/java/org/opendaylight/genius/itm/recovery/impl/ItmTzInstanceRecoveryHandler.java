/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.recovery.impl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.genius.datastoreutils.listeners.DataTreeEventCallbackRegistrar;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.cache.DpnTepStateCache;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipService;
import org.opendaylight.serviceutils.srm.ServiceRecoveryInterface;
import org.opendaylight.serviceutils.srm.ServiceRecoveryRegistry;
import org.opendaylight.serviceutils.tools.listener.AbstractSyncDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelsState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.DpnsTeps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.dpns.teps.RemoteDpns;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.serviceutils.srm.types.rev180626.GeniusItmTz;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint64;
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
    private final IInterfaceManager interfaceManager;
    private final DpnTepStateCache dpnTepStateCache;

    @Inject
    public ItmTzInstanceRecoveryHandler(DataBroker dataBroker,
                                        JobCoordinator jobCoordinator,
                                        ServiceRecoveryRegistry serviceRecoveryRegistry,
                                        EntityOwnershipUtils entityOwnershipUtils,
                                        EntityOwnershipService entityOwnershipService,
                                        DataTreeEventCallbackRegistrar eventCallbacks,
                                        IInterfaceManager interfaceManager,
                                        DpnTepStateCache dpnTepStateCache) {
        super(dataBroker, LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.create(TunnelsState.class).child(StateTunnelList.class));
        this.dataBroker = dataBroker;
        this.jobCoordinator = jobCoordinator;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.entityOwnershipUtils = entityOwnershipUtils;
        this.entityOwnershipService = entityOwnershipService;
        serviceRecoveryRegistry.registerServiceRecoveryRegistry(getServiceRegistryKey(), this);
        this.eventCallbacks = eventCallbacks;
        this.interfaceManager = interfaceManager;
        this.dpnTepStateCache = dpnTepStateCache;

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
        List<String> tunnelList = new ArrayList<>();
        if (interfaceManager.isItmDirectTunnelsEnabled()) {
            Collection<DpnsTeps> dpnsTeps = dpnTepStateCache.getAllPresent();
            List<Uint64> listOfDpnIds = ItmUtils.getDpIdFromTransportzone(dataBroker, entityId);
            for (DpnsTeps dpnTep : dpnsTeps) {
                List<RemoteDpns> rmtdpns = dpnTep.getRemoteDpns();
                for (RemoteDpns remoteDpn : rmtdpns) {
                    if (listOfDpnIds.contains(remoteDpn.getDestinationDpnId())) {
                        tunnelList.add(remoteDpn.getTunnelName());
                    }
                }
            }
            LOG.trace("List of tunnels to be recovered : {}", tunnelList);
        } else {
            //List of Internal tunnels
            tunnelList.addAll(ItmUtils.getInternalTunnelInterfaces(dataBroker));
        }
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
                        LOG.trace("on removal of {}, event callback triggered", stListId);
                        // recreating the transportZone
                        recreateTZ(entityId, tz, tzII, tunnelList.size(), eventCallbackCount);
                        return DataTreeEventCallbackRegistrar.NextAction.UNREGISTER;
                    }, Duration.ofMillis(5000), (id) -> {
                            LOG.trace("event callback timed out for {} tunnel interface ", tunnelInterface);
                            recreateTZ(entityId, tz, tzII, tunnelList.size(), eventCallbackCount); });
                });
            } else {
                LOG.trace("List of tunnels to be recovered is empty, still recreate transportzone {}",entityId);
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
                    tx -> tx.merge(LogicalDatastoreType.CONFIGURATION, tzII, tz))),
                ITMConstants.JOB_MAX_RETRIES);
        }
    }
}