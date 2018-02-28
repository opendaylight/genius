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
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.itm.cache.DPNTEPsInfoCache;
import org.opendaylight.genius.itm.confighelpers.ItmExternalTunnelAddWorker;
import org.opendaylight.genius.itm.confighelpers.ItmInternalTunnelAddWorker;
import org.opendaylight.genius.itm.confighelpers.ItmInternalTunnelDeleteWorker;
import org.opendaylight.genius.itm.confighelpers.ItmTepAddWorker;
import org.opendaylight.genius.itm.confighelpers.ItmTepRemoveWorker;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.impl.TunnelMonitoringConfig;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.genius.srm.ServiceRecoveryInterface;
import org.opendaylight.genius.srm.ServiceRecoveryRegistry;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
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
    private String tzName;
    private IMdsalApiManager imdsalApiManager;
    private TransportZone transportZone;

    @Inject
    public ItmTepInstanceRecoveryHandler(DataBroker dataBroker,
                                         ItmConfig itmConfig,
                                         IMdsalApiManager imdsalApiMgr,
                                         JobCoordinator jobCoordinator,
                                         TunnelMonitoringConfig tunnelMonitoringConfig,
                                         DPNTEPsInfoCache dpntePsInfoCache,
                                         ServiceRecoveryRegistry serviceRecoveryRegistry) {
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

        serviceRecoveryRegistry.registerServiceRecoveryRegistry(buildServiceRegistryKey(), this);
    }

    private String buildServiceRegistryKey() {
        return GeniusItmTep.class.toString();
    }

    @Override
    public void recoverService(String entityId) {
        LOG.info("Trigerred recovery of ITM Instance - Tep {}", entityId);
        try {
            recoverTep(entityId);
        } catch (InterruptedException e) {
            LOG.debug("ITM Instance tep not recovered.");
        }
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

    //Extract tep info from Config DS and it will return DpnId,Portname,VlanId,ipAddress,prefix,GatewayIp,
    // Transportzonename,tunneltype
    private List<String> extractTepInfo(String transportzoneName, String ipAddress) {

        List<String> result = new ArrayList<>();

        transportZone = ItmUtils.getTransportZoneFromConfigDS(transportzoneName, dataBroker);

        for (Subnets sub : transportZone.getSubnets()) {
            if (sub.getVteps() == null || sub.getVteps().isEmpty()) {
                LOG.error("Transport Zone {} subnet {} has no vteps", transportZone.getZoneName(), sub.getPrefix());
                continue;
            }
            for (Vteps vtep : sub.getVteps()) {
                if (ipAddress == vtep.getIpAddress().toString()) {
                    result.add(vtep.getDpnId().toString());
                    result.add(vtep.getPortname());
                    result.add(sub.getVlanId().toString());
                    result.add(ipAddress);
                    result.add(new String(sub.getPrefix().getValue()));
                    result.add(new String(sub.getGatewayIp().getValue()));
                    result.add(transportzoneName);
                    result.add((transportZone.getTunnelType()).toString());
                }
            }
        }
        return result;
    }

    private DPNTEPsInfo extractDPNTepsInfo(String entityId) {
        String[] params = entityId.split(":");
        if (params.length < 2) {
            LOG.error("Not enough arguments..Exiting...");
            return null;
        } else if (params.length > 8) {
            LOG.info("Ignoring extra parameter and proceeding...");
        }

        // ToDo:- Need to add more validations
        this.tzName = params[0];
        String ipAddress = params[1];

        List<String> result = extractTepInfo(tzName, ipAddress);

        List<TzMembership> zones = ItmUtils.createTransportZoneMembership(tzName);
        String tepDetail = StringUtils.join(result,",");
        LOG.trace("Recovering TEP with parameters DPN ID: {}, Port Np: {}, vlanId: {}, ipaddress: {}, IpPrefix: {}, "
                        + "Gateway IP: {}, TransportZone: {}, TunnelType: {} - ", tepDetail);
        //OfTunnels is false byDefault
        //DpnId,Portname,VlanId,ipAddress,prefix,GatewayIp,Transportzonename,tunneltype
        BigInteger dpnId = new BigInteger(result.get(0));
        String portName = result.get(2);
        String vlanId = result.get(3);
        IpPrefix prefix =  new IpPrefix(result.get(3).toCharArray());
        IpAddress gwAddress = new IpAddress(result.get(4).toCharArray());
        //String tunType = result.get(5);

        TunnelEndPoints tunnelEndPoints = ItmUtils.createTunnelEndPoints(dpnId, new IpAddress(ipAddress.toCharArray()),
                portName, false , Integer.parseInt(vlanId), prefix , gwAddress, zones, transportZone.getTunnelType(),
                itmConfig.getDefaultTunnelTos());
        List<TunnelEndPoints> teps  = new ArrayList<>();
        teps.add(tunnelEndPoints);
        return ItmUtils.createDPNTepInfo(dpnId,teps);
    }
}