/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.recovery.impl;
import java.math.BigInteger;
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
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.impl.TunnelMonitoringConfig;
import org.opendaylight.genius.itm.recovery.ItmServiceRecoveryInterface;
import org.opendaylight.genius.itm.recovery.registry.ItmServiceRecoveryRegistry;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.tunnel.end.points.TzMembership;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.srm.types.rev170711.GeniusItmTep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ItmTepInstanceRecoveryHandler implements ItmServiceRecoveryInterface {

    private static final Logger LOG = LoggerFactory.getLogger(ItmTepInstanceRecoveryHandler.class);

    private final JobCoordinator jobCoordinator;
    private final ItmInternalTunnelAddWorker itmInternalTunnelAddWorker;
    private final ItmExternalTunnelAddWorker itmExternalTunnelAddWorker;
    private final DPNTEPsInfoCache dpntePsInfoCache;
    private final DataBroker dataBroker;
    private final ItmInternalTunnelDeleteWorker itmInternalTunnelDeleteWorker;
    private final ItmConfig itmConfig;
    private String tzName;
    private IMdsalApiManager imdsalApiManager;

    @Inject
    public ItmTepInstanceRecoveryHandler(DataBroker dataBroker,
                                         ItmConfig itmConfig,
                                         IMdsalApiManager imdsalApiMgr,
                                         JobCoordinator jobCoordinator,
                                         TunnelMonitoringConfig tunnelMonitoringConfig,
                                         DPNTEPsInfoCache dpntePsInfoCache,
                                         ItmServiceRecoveryRegistry itmServiceRecoveryRegistry) {
        this.dataBroker = dataBroker;
        this.itmConfig = itmConfig;
        this.imdsalApiManager = imdsalApiMgr;
        this.jobCoordinator = jobCoordinator;
        this.dpntePsInfoCache = dpntePsInfoCache;
        this.itmInternalTunnelAddWorker = new ItmInternalTunnelAddWorker(dataBroker, jobCoordinator,
                tunnelMonitoringConfig);
        this.itmExternalTunnelAddWorker = new ItmExternalTunnelAddWorker(dataBroker, itmConfig,
                dpntePsInfoCache);
        this.itmInternalTunnelDeleteWorker = new ItmInternalTunnelDeleteWorker(dataBroker, jobCoordinator,
                tunnelMonitoringConfig);

        itmServiceRecoveryRegistry.registerServiceRecoveryRegistry(buildServiceRegistryKey(), this);
    }

    private String buildServiceRegistryKey() {
        return GeniusItmTep.class.toString();
    }

    @Override
    public void recoverService(String entityId) throws InterruptedException {
        LOG.info("Trigerred recovery of ITM Instance - Tep {}", entityId);
        recoverTep(entityId);
    }

    private void recoverTep(String entityId) throws InterruptedException {
        // TO DO after discussing how to represent a TEP entity
        List<DPNTEPsInfo> tepsToRecover = new ArrayList<>();
        tepsToRecover.add(extractDPNTepsInfo(entityId));
        if (tepsToRecover != null) {
            // Delete the transportZone and re create it
            // Get the transport zone from the transport zone name
            TransportZone oldTz = ItmUtils
                    .getTransportZoneFromConfigDS(ItmUtils.getTransportZoneIdentifierFromName(tzName), dataBroker);
            ItmTepRemoveWorker tepRemoveWorker = new ItmTepRemoveWorker(tepsToRecover, null, oldTz,
                    dataBroker, imdsalApiManager, itmInternalTunnelDeleteWorker, dpntePsInfoCache);
            LOG.trace("Submitting Tep Remove to DJC");
            jobCoordinator.enqueueJob(tzName, tepRemoveWorker);
            // ITM is not able to work with back to back delete and create so sleep is included
            Thread.sleep(5000);
            ItmTepAddWorker tepAddWorker = new ItmTepAddWorker(tepsToRecover,null, dataBroker,
                    imdsalApiManager, itmConfig, itmInternalTunnelAddWorker, itmExternalTunnelAddWorker,
                    dpntePsInfoCache);
            LOG.trace("Submitting Tep Add to DJC");
            jobCoordinator.enqueueJob(tzName, tepAddWorker);
        }
    }

    private DPNTEPsInfo extractDPNTepsInfo(String entityId) {
        String[] params = entityId.split(":");
        if (params.length < 8) {
            LOG.error("Not enough arguments..Exiting...");
            return null;
        } else if (params.length > 8) {
            LOG.info("Ignoring extra parameter and proceeding...");
        }

        // ToDo:- Need to add more validations
        String dpnId = params[0];
        String portNo = params[1];
        String vlanId = params[2];
        String ipAddress = params[3];
        String ipPrefix = params[4];
        String gatewayIP = params[5];
        String tunnelType = params[7];
        this.tzName = params[6];

        Class<? extends TunnelTypeBase> tunType = ItmUtils.convertStringToTunnelType(tunnelType);
        List<TzMembership> zones = ItmUtils.createTransportZoneMembership(tzName);
        LOG.trace("Recovering TEP with parameters DPN ID: {}, Port Np: {}, vlanId: {}, ipaddress: {}, IpPrefix: {}, "
                        + "Gateway IP: {}, TransportZone: {}, TunnelType- {}, ", dpnId, portNo, vlanId, ipAddress,
                ipPrefix, gatewayIP, tzName, tunnelType);
        //OfTunnels is false byDefault
        TunnelEndPoints tunnelEndPoints = ItmUtils.createTunnelEndPoints(new BigInteger(dpnId),
                new IpAddress(ipAddress.toCharArray()), null,false , Integer.parseInt(vlanId),
                new IpPrefix(ipPrefix.toCharArray()), new IpAddress(gatewayIP.toCharArray()),
                zones, tunType, itmConfig.getDefaultTunnelTos());
        List<TunnelEndPoints> teps  = new ArrayList<>();
        teps.add(tunnelEndPoints);
        return ItmUtils.createDPNTepInfo(new BigInteger(dpnId),teps);
    }
}