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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.listeners.DataTreeEventCallbackRegistrar;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.cache.DPNTEPsInfoCache;
import org.opendaylight.genius.itm.cache.DpnTepStateCache;
import org.opendaylight.genius.itm.cache.OvsBridgeEntryCache;
import org.opendaylight.genius.itm.cache.OvsBridgeRefEntryCache;
import org.opendaylight.genius.itm.cache.TunnelStateCache;
import org.opendaylight.genius.itm.confighelpers.ItmExternalTunnelAddWorker;
import org.opendaylight.genius.itm.confighelpers.ItmInternalTunnelAddWorker;
import org.opendaylight.genius.itm.confighelpers.ItmInternalTunnelDeleteWorker;
import org.opendaylight.genius.itm.confighelpers.ItmTepAddWorker;
import org.opendaylight.genius.itm.confighelpers.ItmTepRemoveWorker;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.impl.TunnelMonitoringConfig;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.genius.srm.ServiceRecoveryInterface;
import org.opendaylight.genius.srm.ServiceRecoveryRegistry;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.tunnel.end.points.TzMembership;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.Subnets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.Vteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.srm.types.rev170711.GeniusItmTep;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ItmTepInstanceRecoveryHandler implements ServiceRecoveryInterface {

    private static final Logger LOG = LoggerFactory.getLogger(ItmTepInstanceRecoveryHandler.class);

    private final JobCoordinator jobCoordinator;
    private final ItmInternalTunnelAddWorker itmInternalTunnelAddWorker;
    private final ItmExternalTunnelAddWorker itmExternalTunnelAddWorker;
    private final DPNTEPsInfoCache dpntePsInfoCache;
    private final DataBroker dataBroker;
    private final ItmInternalTunnelDeleteWorker itmInternalTunnelDeleteWorker;
    private final ItmConfig itmConfig;
    private final EntityOwnershipUtils entityOwnershipUtils;
    private final IMdsalApiManager imdsalApiManager;
    private final DataTreeEventCallbackRegistrar eventCallbacks;

    @Inject
    public ItmTepInstanceRecoveryHandler(DataBroker dataBroker,
                                         ItmConfig itmConfig,
                                         IMdsalApiManager imdsalApiMgr,
                                         JobCoordinator jobCoordinator,
                                         TunnelMonitoringConfig tunnelMonitoringConfig,
                                         DPNTEPsInfoCache dpntePsInfoCache, TunnelStateCache tunnelStateCache,
                                         DirectTunnelUtils directTunnelUtils, DpnTepStateCache dpnTepStateCache,
                                         OvsBridgeEntryCache ovsBridgeEntryCache,
                                         OvsBridgeRefEntryCache ovsBridgeRefEntryCache,
                                         IInterfaceManager interfaceManager,
                                         ServiceRecoveryRegistry serviceRecoveryRegistry,
                                         EntityOwnershipUtils entityOwnershipUtils,
                                         DataTreeEventCallbackRegistrar eventCallbacks) {
        this.dataBroker = dataBroker;
        this.itmConfig = itmConfig;
        this.imdsalApiManager = imdsalApiMgr;
        this.jobCoordinator = jobCoordinator;
        this.dpntePsInfoCache = dpntePsInfoCache;
        this.entityOwnershipUtils = entityOwnershipUtils;
        this.eventCallbacks = eventCallbacks;
        this.itmInternalTunnelAddWorker = new ItmInternalTunnelAddWorker(dataBroker, jobCoordinator,
                tunnelMonitoringConfig, itmConfig, directTunnelUtils, interfaceManager, ovsBridgeRefEntryCache);
        this.itmExternalTunnelAddWorker = new ItmExternalTunnelAddWorker(dataBroker, itmConfig,
                dpntePsInfoCache);
        this.itmInternalTunnelDeleteWorker = new ItmInternalTunnelDeleteWorker(dataBroker, jobCoordinator,
                tunnelMonitoringConfig, interfaceManager, dpnTepStateCache, ovsBridgeEntryCache,
                ovsBridgeRefEntryCache, tunnelStateCache,
                directTunnelUtils);
        serviceRecoveryRegistry.registerServiceRecoveryRegistry(getServiceRegistryKey(), this);
    }

    private String getServiceRegistryKey() {
        return GeniusItmTep.class.toString();
    }

    @Override
    public void recoverService(String entityId) {
        if (!entityOwnershipUtils.isEntityOwner(ITMConstants.ITM_CONFIG_ENTITY, ITMConstants.ITM_CONFIG_ENTITY)) {
            return;
        }
        LOG.info("Trigerred recovery of ITM Instance - Tep {}", entityId);
        try {
            recoverTep(entityId);
        } catch (InterruptedException e) {
            LOG.error("ITM Instance tep has not been recovered.", e);
        }
    }

    private void recoverTep(String entityId) throws InterruptedException {
        List<DPNTEPsInfo> tepsToRecover = new ArrayList<>();
        String[] params = entityId.split(":");
        if (params.length < 2) {
            LOG.error("Not enough arguments..Exiting...");
        } else if (params.length > 2) {
            LOG.info("Ignoring extra parameter and proceeding...");
        }
        String tzName = params[0];
        String ipAddress = params[1];
        TransportZone oldTz = ItmUtils.getTransportZoneFromConfigDS(tzName, dataBroker);
        DPNTEPsInfo dpnTepsToRecover = extractDPNTepsInfo(tzName, ipAddress, oldTz);
        if (dpnTepsToRecover == null) {
            LOG.error("Please Enter appropriate arguments for Tep Recovery.");
            return;
        } else {
            tepsToRecover.add(dpnTepsToRecover);
            //List of Internel tunnels
            List<InternalTunnel> tunnelList = ItmUtils.getInternalTunnelsFromCache(dataBroker);
            List<String> interfaceListToRecover = new ArrayList<>();
            LOG.debug("List of tunnel interfaces: {}" , tunnelList);

            if (oldTz != null) {
                LOG.trace("Deleting transportzone {}", tzName);
                ItmTepRemoveWorker tepRemoveWorker = new ItmTepRemoveWorker(tepsToRecover, null, oldTz,
                        dataBroker, imdsalApiManager, itmInternalTunnelDeleteWorker, dpntePsInfoCache);
                jobCoordinator.enqueueJob(tzName, tepRemoveWorker);
                AtomicInteger eventCallbackCount = new AtomicInteger(0);
                AtomicInteger eventRegistrationCount = new AtomicInteger(0);
                tunnelList.stream().filter(internalTunnel -> internalTunnel
                        .getDestinationDPN().equals(dpnTepsToRecover.getDPNID()) || internalTunnel.getSourceDPN()
                        .equals(dpnTepsToRecover.getDPNID())).forEach(internalTunnel -> {
                            eventRegistrationCount.incrementAndGet();
                            interfaceListToRecover.add(String.valueOf(internalTunnel.getTunnelInterfaceNames())); });

                if (!interfaceListToRecover.isEmpty()) {
                    interfaceListToRecover.forEach(interfaceName -> {
                        StateTunnelListKey tlKey = new StateTunnelListKey(interfaceName);
                        LOG.trace("TunnelStateKey: {} for interface: {}", tlKey, interfaceName);
                        InstanceIdentifier<StateTunnelList> stListId = ItmUtils.buildStateTunnelListId(tlKey);
                        eventCallbacks.onRemove(LogicalDatastoreType.OPERATIONAL, stListId, (unused) -> {
                            LOG.trace("callback event for a delete {} interface instance....", stListId);
                            // recreating the transportZone
                            recreateTEP(tzName, tepsToRecover, eventCallbackCount, interfaceListToRecover.size());
                            return DataTreeEventCallbackRegistrar.NextAction.UNREGISTER;
                        }, Duration.ofMillis(5000), (id) -> {
                                LOG.trace("event callback timed out for {} tunnel interface ", interfaceName);
                                recreateTEP(tzName, tepsToRecover, eventCallbackCount, interfaceListToRecover.size());
                            });
                    });
                } else {
                    recreateTEP(tzName, tepsToRecover, eventCallbackCount, interfaceListToRecover.size());
                }
            }
        }
    }


    private void recreateTEP(String tzName, List tepts, AtomicInteger eventCallbackCount, int registeredEventSize) {
        eventCallbackCount.incrementAndGet();
        if (eventCallbackCount.intValue() == registeredEventSize || registeredEventSize == 0) {
            LOG.info("Re-creating TEP {}", tzName);
            ItmTepAddWorker tepAddWorker = new ItmTepAddWorker(tepts, null, dataBroker,
                    imdsalApiManager, itmConfig, itmInternalTunnelAddWorker, itmExternalTunnelAddWorker,
                    dpntePsInfoCache);
            jobCoordinator.enqueueJob(tzName, tepAddWorker);
        } else {
            LOG.trace("{} call back events registered for {} tunnel interfaces",
                    registeredEventSize, eventCallbackCount);
        }
    }

    private DPNTEPsInfo extractDPNTepsInfo(String tzName, String ipAddress, TransportZone transportZone) {

        if (transportZone == null) {
            LOG.error("Transportzone name {} is not valid.", tzName);
            return null;
        }

        for (Subnets sub : transportZone.getSubnets()) {
            if (sub.getVteps() == null || sub.getVteps().isEmpty()) {
                LOG.error("Transport Zone {} subnet {} has no vteps", transportZone.getZoneName(), sub.getPrefix());
            }
            for (Vteps vtep : sub.getVteps()) {
                if (ipAddress.equals(String.valueOf(vtep.getIpAddress().getValue()))) {

                    List<TzMembership> zones = ItmUtils.createTransportZoneMembership(tzName);
                    LOG.trace("Transportzone {} found match for tep {} to be recovered", transportZone.getZoneName(),
                            ipAddress);

                    //OfTunnels is false byDefault
                    TunnelEndPoints tunnelEndPoints = ItmUtils.createTunnelEndPoints(vtep.getDpnId(),
                        IpAddressBuilder.getDefaultInstance(ipAddress), vtep.getPortname(), false, sub.getVlanId(),
                            sub.getPrefix(), sub.getGatewayIp(), zones,transportZone.getTunnelType(),
                            itmConfig.getDefaultTunnelTos());

                    List<TunnelEndPoints> teps = new ArrayList<>();
                    teps.add(tunnelEndPoints);
                    return ItmUtils.createDPNTepInfo(vtep.getDpnId(), teps);
                }
            }
        }
        return null;
    }
}