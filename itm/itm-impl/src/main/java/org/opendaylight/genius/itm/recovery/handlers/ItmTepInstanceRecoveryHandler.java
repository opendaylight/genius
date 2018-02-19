package org.opendaylight.genius.itm.recovery.handlers;

import java.math.*;
import java.util.*;
import javax.inject.*;
import org.opendaylight.controller.md.sal.binding.api.*;
import org.opendaylight.genius.itm.cache.*;
import org.opendaylight.genius.itm.confighelpers.*;
import org.opendaylight.genius.itm.impl.*;
import org.opendaylight.genius.itm.recovery.*;
import org.opendaylight.genius.itm.recovery.registry.*;
import org.opendaylight.genius.mdsalutil.interfaces.*;
import org.opendaylight.infrautils.jobcoordinator.*;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.tunnel.end.points.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.srm.types.rev170711.*;
import org.slf4j.*;

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
    private IdManagerService idManagerService;
    private IMdsalApiManager iMdsalApiManager;

    @Inject
    public ItmTepInstanceRecoveryHandler(DataBroker dataBroker,
                                        ItmConfig itmConfig,
                                        IdManagerService idMgrService,
                                        IMdsalApiManager iMdsalApiMgr,
                                        JobCoordinator jobCoordinator,
                                        ItmInternalTunnelAddWorker itmInternalTunnelAddWorker,
                                        ItmExternalTunnelAddWorker itmExternalTunnelAddWorker,
                                        DPNTEPsInfoCache dpntePsInfoCache,
                                        ItmInternalTunnelDeleteWorker itmInternalTunnelDeleteWorker,
                                        ItmServiceRecoveryRegistry itmServiceRecoveryRegistry){
        this.dataBroker = dataBroker;
        this.itmConfig = itmConfig;
        this.idManagerService = idMgrService;
        this.iMdsalApiManager = iMdsalApiMgr;
        this.jobCoordinator = jobCoordinator;
        this.itmInternalTunnelAddWorker = itmInternalTunnelAddWorker;
        this.itmExternalTunnelAddWorker = itmExternalTunnelAddWorker;
        this.dpntePsInfoCache = dpntePsInfoCache;
        this.itmInternalTunnelDeleteWorker = itmInternalTunnelDeleteWorker;
        itmServiceRecoveryRegistry.registerServiceRecoveryRegistry(buildServiceRegistryKey(), this);
    }

    private String buildServiceRegistryKey() {
        return GeniusItm.class.toString();
    }

    @Override
    public void recover(String entityId) {
        LOG.info("Trigerred recovery of ITM Instance - Tep {}", entityId);
        recoverTep(entityId);
    }

    private void recoverTep(String entityId) {
        // TO DO after discussing how to represent a TEP entity
        List<DPNTEPsInfo> tepsToRecover = new ArrayList<>();
        tepsToRecover.add(extractDPNTepsInfo(entityId));
        if(tepsToRecover != null) {
            // Delete the transportZone and re create it
            // Get the transport zone from the transport zone name
            TransportZone oldTz = ItmUtils.getTransportZoneFromConfigDS(ItmUtils.getTransportZoneIdentifierFromName(tzName),dataBroker);
            ItmTepRemoveWorker tepRemoveWorker = new ItmTepRemoveWorker(tepsToRecover, null, oldTz,
                    dataBroker, iMdsalApiManager, itmInternalTunnelDeleteWorker, dpntePsInfoCache);
            LOG.trace( "Submitting Tep Remove to DJC");
            jobCoordinator.enqueueJob(tzName, tepRemoveWorker);
            ItmTepAddWorker tepAddWorker = new ItmTepAddWorker(tepsToRecover,null, dataBroker,
                    iMdsalApiManager, itmConfig, itmInternalTunnelAddWorker, itmExternalTunnelAddWorker, dpntePsInfoCache);
            LOG.trace( "Submitting Tep Add to DJC");
            jobCoordinator.enqueueJob(tzName, tepAddWorker);
        }
    }

    private DPNTEPsInfo extractDPNTepsInfo(String entityId) {
        String[] params = entityId.split(":");
        if (params.length < 8){
            LOG.error( "Not enough arguments..Exiting...");
            return null;
        } else if (params.length > 8){
            LOG.info( "Ignoring extra parameter and proceeding...");
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
        LOG.trace( "Recovering TEP with parameters DPN ID: {}, Port Np: {}, vlanId: {}, ipaddress: {}, IpPrefix: {}, " +
                        "Gateway IP: {}, TransportZone: {}, TunnelType- {}, ", dpnId, portNo, vlanId, ipAddress, ipPrefix,
                gatewayIP, tzName, tunnelType);
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