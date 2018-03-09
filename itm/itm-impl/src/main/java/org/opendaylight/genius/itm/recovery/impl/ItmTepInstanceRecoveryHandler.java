/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.recovery.impl;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.itm.cache.DPNTEPsInfoCache;
import org.opendaylight.genius.itm.confighelpers.ItmExternalTunnelAddWorker;
import org.opendaylight.genius.itm.confighelpers.ItmInternalTunnelAddWorker;
import org.opendaylight.genius.itm.confighelpers.ItmInternalTunnelDeleteWorker;
import org.opendaylight.genius.itm.confighelpers.ItmTepAddWorker;
import org.opendaylight.genius.itm.confighelpers.ItmTepRemoveWorker;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.impl.TunnelMonitoringConfig;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.genius.srm.ServiceRecoveryInterface;
import org.opendaylight.genius.srm.ServiceRecoveryRegistry;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.tunnel.end.points.TzMembership;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.Subnets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.Vteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.srm.types.rev170711.GeniusItmTep;
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
    private final EntityOwnershipService entityOwnershipService;
    private final IMdsalApiManager imdsalApiManager;
    private String tzName;
    private TransportZone transportZone;

    @Inject
    public ItmTepInstanceRecoveryHandler(DataBroker dataBroker,
                                         ItmConfig itmConfig,
                                         IMdsalApiManager imdsalApiMgr,
                                         JobCoordinator jobCoordinator,
                                         TunnelMonitoringConfig tunnelMonitoringConfig,
                                         DPNTEPsInfoCache dpntePsInfoCache,
                                         ServiceRecoveryRegistry serviceRecoveryRegistry,
                                         EntityOwnershipUtils entityOwnershipUtils,
                                         EntityOwnershipService entityOwnershipService) {
        this.dataBroker = dataBroker;
        this.itmConfig = itmConfig;
        this.imdsalApiManager = imdsalApiMgr;
        this.jobCoordinator = jobCoordinator;
        this.dpntePsInfoCache = dpntePsInfoCache;
        this.entityOwnershipUtils = entityOwnershipUtils;
        this.entityOwnershipService = entityOwnershipService;
        this.itmInternalTunnelAddWorker = new ItmInternalTunnelAddWorker(dataBroker, jobCoordinator,
                tunnelMonitoringConfig, itmConfig);
        this.itmExternalTunnelAddWorker = new ItmExternalTunnelAddWorker(dataBroker, itmConfig,
                dpntePsInfoCache);
        this.itmInternalTunnelDeleteWorker = new ItmInternalTunnelDeleteWorker(dataBroker, jobCoordinator,
                tunnelMonitoringConfig);
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
            LOG.debug("ITM Instance tep not recovered.");
        }
    }

    private void recoverTep(String entityId) throws InterruptedException {
        List<DPNTEPsInfo> tepsToRecover = new ArrayList<>();
        DPNTEPsInfo dpnTepsToRecover = extractDPNTepsInfo(entityId);
        if (dpnTepsToRecover == null) {
            LOG.error("Please Enter appropriate arguments for Tep Recovery.");
            return;
        } else {
            tepsToRecover.add(dpnTepsToRecover);
            // Delete the transportZone and re create it
            // Get the transport zone from the transport zone name
            TransportZone oldTz = ItmUtils.getTransportZoneFromConfigDS(tzName, dataBroker);
            if (oldTz != null) {
                ItmTepRemoveWorker tepRemoveWorker = new ItmTepRemoveWorker(tepsToRecover, null, oldTz,
                        dataBroker, imdsalApiManager, itmInternalTunnelDeleteWorker, dpntePsInfoCache);
                LOG.trace("Submitting Tep Remove to DJC");
                jobCoordinator.enqueueJob(tzName, tepRemoveWorker);
                ItmTepAddWorker tepAddWorker = new ItmTepAddWorker(tepsToRecover, null, dataBroker,
                        imdsalApiManager, itmConfig, itmInternalTunnelAddWorker, itmExternalTunnelAddWorker,
                        dpntePsInfoCache);
                LOG.trace("Submitting Tep Add to DJC");
                jobCoordinator.enqueueJob(tzName, tepAddWorker);
            }
        }
    }

    private DPNTEPsInfo extractDPNTepsInfo(String entityId) {

        String[] params = entityId.split(":");
        if (params.length < 2) {
            LOG.error("Not enough arguments..Exiting...");
            return null;
        } else if (params.length > 2) {
            LOG.info("Ignoring extra parameter and proceeding...");
        }

        // ToDo:- Need to add more validations
        this.tzName = params[0];
        String ipAddress = params[1];

        transportZone = ItmUtils.getTransportZoneFromConfigDS(tzName, dataBroker);
        if (transportZone == null) {
            LOG.error("Transportzone name is not valid.");
            return null;
        }

        for (Subnets sub : transportZone.getSubnets()) {
            if (sub.getVteps() == null || sub.getVteps().isEmpty()) {
                LOG.error("Transport Zone {} subnet {} has no vteps", transportZone.getZoneName(), sub.getPrefix());
            }
            for (Vteps vtep : sub.getVteps()) {
                if (ipAddress.equals(vtep.getIpAddress().getIpv4Address().getValue())) {

                    List<TzMembership> zones = ItmUtils.createTransportZoneMembership(tzName);
                    LOG.trace("Match Found for TEP");

                    //OfTunnels is false byDefault
                    TunnelEndPoints tunnelEndPoints = ItmUtils.createTunnelEndPoints(vtep.getDpnId(),
                            new IpAddress(ipAddress.toCharArray()), vtep.getPortname(), false, sub.getVlanId(),
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