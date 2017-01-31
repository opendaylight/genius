/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.confighelpers;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeLldp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.tunnel.end.points.TzMembership;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnelKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItmInternalTunnelDeleteWorker {
    private static final Logger logger = LoggerFactory.getLogger(ItmInternalTunnelDeleteWorker.class) ;

    public static List<ListenableFuture<Void>> deleteTunnels(DataBroker dataBroker, IdManagerService idManagerService,IMdsalApiManager mdsalManager,
                                                             List<DPNTEPsInfo> dpnTepsList, List<DPNTEPsInfo> meshedDpnList)
    {
        logger.trace( "TEPs to be deleted {} " , dpnTepsList );
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        WriteTransaction t = dataBroker.newWriteOnlyTransaction();
        try {
            if (dpnTepsList == null || dpnTepsList.size() == 0) {
                logger.debug("no vtep to delete");
                return futures ;
            }

            if (meshedDpnList == null || meshedDpnList.size() == 0) {
                logger.debug("No Meshed Vteps");
                return futures ;
            }
            for (DPNTEPsInfo srcDpn : dpnTepsList) {
                logger.trace("Processing srcDpn " + srcDpn);
                List<TunnelEndPoints> meshedEndPtCache =
                        new ArrayList<>(ItmUtils.getTEPsForDpn(srcDpn.getDPNID(), meshedDpnList)) ;
                if(meshedEndPtCache == null ) {
                    logger.debug("No Tunnel End Point configured for this DPN {}", srcDpn.getDPNID());
                    continue ;
                }
                logger.debug( "Entries in meshEndPointCache {} ", meshedEndPtCache.size() );
                for (TunnelEndPoints srcTep : srcDpn.getTunnelEndPoints()) {
                    logger.trace("Processing srcTep " + srcTep);
                    List<TzMembership> srcTZones = srcTep.getTzMembership();

                    // run through all other DPNS other than srcDpn
                    for (DPNTEPsInfo dstDpn : meshedDpnList) {
                        if (!srcDpn.getDPNID().equals(dstDpn.getDPNID())) {
                            for (TunnelEndPoints dstTep : dstDpn.getTunnelEndPoints()) {
                                logger.trace("Processing dstTep " + dstTep);
                                if (!ItmUtils.getIntersection(dstTep.getTzMembership(), srcTZones).isEmpty()) {
                                    if( checkIfTrunkExists(dstDpn.getDPNID(), srcDpn.getDPNID(), srcTep.getTunnelType(),dataBroker)) {
                                        // remove all trunk interfaces
                                        logger.trace("Invoking removeTrunkInterface between source TEP {} , Destination TEP {} " ,srcTep , dstTep);
                                        removeTrunkInterface(dataBroker, idManagerService, srcTep, dstTep, srcDpn.getDPNID(), dstDpn.getDPNID(), t, futures);
                                    }
                                }
                            }
                        }
                    }

                    // removing vtep / dpn from Tunnels OpDs.
                    InstanceIdentifier<TunnelEndPoints> tepPath =
                            InstanceIdentifier.builder(DpnEndpoints.class).child(DPNTEPsInfo.class, srcDpn.getKey())
                                    .child(TunnelEndPoints.class, srcTep.getKey()).build();

                    logger.trace("Tep Removal of TEP {} from DPNTEPSINFO CONFIG DS with Key {} " + srcTep, srcTep.getKey());
                    t.delete(LogicalDatastoreType.CONFIGURATION, tepPath);
                    // remove the tep from the cache
                    meshedEndPtCache.remove(srcTep) ;
                    Class<? extends TunnelMonitoringTypeBase> monitorProtocol = ItmUtils.determineMonitorProtocol(dataBroker);
                    InstanceIdentifier<DPNTEPsInfo> dpnPath =
                            InstanceIdentifier.builder(DpnEndpoints.class).child(DPNTEPsInfo.class, srcDpn.getKey())
                                    .build();
                    /*
                    Optional<DPNTEPsInfo> dpnOptional =
                                    ItmUtils.read(LogicalDatastoreType.CONFIGURATION, dpnPath, dataBroker);
                    if (dpnOptional.isPresent()) {
                    */
                    if( meshedEndPtCache.isEmpty()) {
                        //DPNTEPsInfo dpnRead = dpnOptional.get();
                        // remove dpn if no vteps exist on dpn
                        //  if (dpnRead.getTunnelEndPoints() == null || dpnRead.getTunnelEndPoints().size() == 0) {
                        if(monitorProtocol.isAssignableFrom(TunnelMonitoringTypeLldp.class)) {
                            logger.debug("Removing Terminating Service Table Flow ");
                            ItmUtils.setUpOrRemoveTerminatingServiceTable(srcDpn.getDPNID(), mdsalManager, false);
                        }
                        logger.trace("DPN Removal from DPNTEPSINFO CONFIG DS " + srcDpn.getDPNID());
                        t.delete(LogicalDatastoreType.CONFIGURATION, dpnPath);
                        InstanceIdentifier<DpnEndpoints> tnlContainerPath =
                                InstanceIdentifier.builder(DpnEndpoints.class).build();
                        Optional<DpnEndpoints> containerOptional =
                                ItmUtils.read(LogicalDatastoreType.CONFIGURATION,
                                        tnlContainerPath, dataBroker);
                        // remove container if no DPNs are present
                        if (containerOptional.isPresent()) {
                            DpnEndpoints deps = containerOptional.get();
                            if (deps.getDPNTEPsInfo() == null || deps.getDPNTEPsInfo().isEmpty()) {
                                logger.trace("Container Removal from DPNTEPSINFO CONFIG DS");
                                t.delete(LogicalDatastoreType.CONFIGURATION, tnlContainerPath);
                            }
                        }
                        //}
                    }
                }
            }
            futures.add( t.submit() );
        } catch (Exception e1) {
            logger.error("exception while deleting tep", e1);
        }
        return futures ;
    }

    private static void removeTrunkInterface(DataBroker dataBroker, IdManagerService idManagerService,
                                             TunnelEndPoints srcTep, TunnelEndPoints dstTep, BigInteger srcDpnId, BigInteger dstDpnId,
                                             WriteTransaction t, List<ListenableFuture<Void>> futures) {
        String trunkfwdIfName =
                ItmUtils.getTrunkInterfaceName( idManagerService, srcTep.getInterfaceName(),
                        srcTep.getIpAddress().getIpv4Address().getValue(),
                        dstTep.getIpAddress().getIpv4Address().getValue(),
                        srcTep.getTunnelType());
        logger.trace("Removing forward Trunk Interface " + trunkfwdIfName);
        InstanceIdentifier<Interface> trunkIdentifier = ItmUtils.buildId(trunkfwdIfName);
        logger.debug(  " Removing Trunk Interface Name - {} , Id - {} from Config DS ", trunkfwdIfName, trunkIdentifier ) ;
        t.delete(LogicalDatastoreType.CONFIGURATION, trunkIdentifier);
        ItmUtils.itmCache.removeInterface(trunkfwdIfName);
        // also update itm-state ds -- Delete the forward tunnel-interface from the tunnel list
        InstanceIdentifier<InternalTunnel> path = InstanceIdentifier.create(
                TunnelList.class)
                .child(InternalTunnel.class, new InternalTunnelKey( dstDpnId, srcDpnId, srcTep.getTunnelType()));
        t.delete(LogicalDatastoreType.CONFIGURATION,path) ;
        ItmUtils.itmCache.removeInternalTunnel(trunkfwdIfName);
        // Release the Ids for the forward trunk interface Name
        ItmUtils.releaseIdForTrunkInterfaceName(idManagerService,srcTep.getInterfaceName(), srcTep.getIpAddress()
                .getIpv4Address().getValue(), dstTep.getIpAddress().getIpv4Address()
                .getValue(), srcTep.getTunnelType().getName() );

        String trunkRevIfName =
                ItmUtils.getTrunkInterfaceName( idManagerService, dstTep.getInterfaceName(),
                        dstTep.getIpAddress().getIpv4Address().getValue(),
                        srcTep.getIpAddress().getIpv4Address().getValue(),
                        srcTep.getTunnelType());
        logger.trace("Removing Reverse Trunk Interface " + trunkRevIfName);
        trunkIdentifier = ItmUtils.buildId(trunkRevIfName);
        logger.debug(  " Removing Trunk Interface Name - {} , Id - {} from Config DS ", trunkRevIfName, trunkIdentifier ) ;
        t.delete(LogicalDatastoreType.CONFIGURATION, trunkIdentifier);

        // also update itm-state ds -- Delete the reverse tunnel-interface from the tunnel list
        path = InstanceIdentifier.create(
                TunnelList.class)
                .child(InternalTunnel.class, new InternalTunnelKey(srcDpnId, dstDpnId, dstTep.getTunnelType()));
        t.delete(LogicalDatastoreType.CONFIGURATION,path) ;

        // Release the Ids for the reverse trunk interface Name
        ItmUtils.releaseIdForTrunkInterfaceName(idManagerService, dstTep.getInterfaceName(), dstTep.getIpAddress()
                .getIpv4Address().getValue(), srcTep.getIpAddress().getIpv4Address()
                .getValue(),dstTep.getTunnelType().getName());
    }
    private static boolean checkIfTrunkExists(BigInteger srcDpnId, BigInteger dstDpnId, Class<? extends TunnelTypeBase> tunType, DataBroker dataBroker) {
        InstanceIdentifier<InternalTunnel> path = InstanceIdentifier.create(
                TunnelList.class)
                .child(InternalTunnel.class, new InternalTunnelKey( dstDpnId, srcDpnId, tunType));
        return ItmUtils.read(LogicalDatastoreType.CONFIGURATION,path, dataBroker).isPresent();
    }
}
