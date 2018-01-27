/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.scaling.workers;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.scaling.renderer.ovs.confighelpers.OvsTunnelConfigRemoveHelper;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.DpnTepInterfaceInfo;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.ItmScaleUtils;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.TunnelUtils;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.tunnel.end.points.TzMembership;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TepInternalTunnelDeleteWorker {
    private static final Logger logger = LoggerFactory.getLogger(TepInternalTunnelDeleteWorker.class) ;

    public static List<ListenableFuture<Void>> deleteTunnels(DataBroker dataBroker, IdManagerService idManagerService,
                                                             IMdsalApiManager mdsalManager,
                                                             List<DPNTEPsInfo> dpnTepsList,
                                                             List<DPNTEPsInfo> meshedDpnList) {
        logger.trace( "TEPs to be deleted {} ", dpnTepsList);
        try {
            if (dpnTepsList == null || dpnTepsList.size() == 0) {
                logger.debug("no vtep to delete");
                return Collections.emptyList();
            }

            if (meshedDpnList == null || meshedDpnList.size() == 0) {
                logger.debug("No Meshed Vteps");
                return Collections.emptyList();
            }
            WriteTransaction t = dataBroker.newWriteOnlyTransaction();
            for (DPNTEPsInfo srcDpn : dpnTepsList) {
                logger.trace("Processing srcDpn {}", srcDpn);
                List<TunnelEndPoints> meshedEndPtCache =
                        new ArrayList<>(ItmUtils.getTEPsForDpn(srcDpn.getDPNID(), meshedDpnList));
                if(meshedEndPtCache == null ) {
                    logger.debug("No Tunnel End Point configured for this DPN {}", srcDpn.getDPNID());
                    continue ;
                }
                logger.debug( "Entries in meshEndPointCache {} ", meshedEndPtCache.size() );
                for (TunnelEndPoints srcTep : srcDpn.getTunnelEndPoints()) {
                    logger.trace("Processing srcTep {}", srcTep);
                    List<TzMembership> srcTZones = srcTep.getTzMembership();

                    // run through all other DPNS other than srcDpn
                    for (DPNTEPsInfo dstDpn : meshedDpnList) {
                        if (!srcDpn.getDPNID().equals(dstDpn.getDPNID())) {
                            for (TunnelEndPoints dstTep : dstDpn.getTunnelEndPoints()) {
                                logger.trace("Processing dstTep {}", dstTep);
                                if (!ItmUtils.getIntersection(dstTep.getTzMembership(), srcTZones).isEmpty()) {
                                    if( checkIfTrunkExists(dstDpn.getDPNID(), srcDpn.getDPNID(), srcTep.getTunnelType(),dataBroker)) {
                                        // remove all trunk interfaces
                                        logger.trace("Invoking removeTrunkInterface between source TEP {} , Destination TEP {} " ,srcTep , dstTep);
                                        removeTrunkInterface(dataBroker, idManagerService, mdsalManager,
                                                srcTep, dstTep, srcDpn.getDPNID(), dstDpn.getDPNID(), t);
                                    }
                                }
                            }
                        }
                    }

                    // removing vtep / dpn from Tunnels OpDs.
                    InstanceIdentifier<TunnelEndPoints> tepPath =
                            InstanceIdentifier.builder(DpnEndpoints.class).child(DPNTEPsInfo.class, srcDpn.getKey())
                                    .child(TunnelEndPoints.class, srcTep.getKey()).build();

                    logger.trace("Tep Removal of TEP {} from DPNTEPSINFO CONFIG DS with Key {} ",
                            srcTep, srcTep.getKey());
                    t.delete(LogicalDatastoreType.CONFIGURATION, tepPath);
                    // remove the tep from the cache
                    meshedEndPtCache.remove(srcTep) ;
                    Class<? extends TunnelMonitoringTypeBase> monitorProtocol = ItmUtils.determineMonitorProtocol(dataBroker);
                    InstanceIdentifier<DPNTEPsInfo> dpnPath =
                            InstanceIdentifier.builder(DpnEndpoints.class).child(DPNTEPsInfo.class, srcDpn.getKey())
                                    .build();
                    if( meshedEndPtCache.isEmpty()) {
                        // remove dpn if no vteps exist on dpn
                        if(monitorProtocol.isAssignableFrom(TunnelMonitoringTypeLldp.class)) {
                            logger.debug("Removing Terminating Service Table Flow ");
                            ItmUtils.setUpOrRemoveTerminatingServiceTable(srcDpn.getDPNID(), mdsalManager, false);
                        }
                        logger.trace("DPN Removal from DPNTEPSINFO CONFIG DS {}", srcDpn.getDPNID());
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
                    }
                }
                    // SF419 Remove the DPNSTEPs DS
                    logger.debug("Deleting TEP Interface information CONFIGURATION datastore with DPNs-Teps " +
                            "for source Dpn", srcDpn.getDPNID());
                 // Clean up the DPN TEPs State DS
                 ItmScaleUtils.removeTepFromDpnTepInterfaceConfigDS(srcDpn.getDPNID());
                    // ITM DIRECT TUNNELS Clean up the container when its empty -- How ???
            }
            return Collections.singletonList(t.submit());
        } catch (Exception e1) {
            logger.error("Exception while deleting teps in the list {}, exception ", dpnTepsList, e1);
        }
        return Collections.emptyList();
    }

    private static void removeTrunkInterface(DataBroker dataBroker, IdManagerService idManagerService,
                                             IMdsalApiManager mdsalManager,
                                             TunnelEndPoints srcTep, TunnelEndPoints dstTep, BigInteger srcDpnId,
                                             BigInteger dstDpnId, WriteTransaction t) {
        String trunkfwdIfName =
                ItmUtils.getTrunkInterfaceName(srcTep.getInterfaceName(),
                        srcTep.getIpAddress().getIpv4Address().getValue(),
                        dstTep.getIpAddress().getIpv4Address().getValue(),
                        srcTep.getTunnelType().getName());
        logger.trace("Removing forward Trunk Interface {}", trunkfwdIfName);
        ParentRefs parentRefs = new ParentRefsBuilder().setDatapathNodeIdentifier(srcDpnId).build();
        Interface iface = TunnelUtils.getInterfaceFromConfigDS(trunkfwdIfName,
                dataBroker);
        // ITM DIRECT TUNNELS -- Call the OVS Worker directly
        if (iface != null) {
            OvsTunnelConfigRemoveHelper.removeConfiguration(dataBroker, iface, idManagerService,
                    mdsalManager, parentRefs);
        }
        String trunkRevIfName =
                ItmUtils.getTrunkInterfaceName(dstTep.getInterfaceName(),
                        dstTep.getIpAddress().getIpv4Address().getValue(),
                        srcTep.getIpAddress().getIpv4Address().getValue(),
                        srcTep.getTunnelType().getName());
        parentRefs = new ParentRefsBuilder().setDatapathNodeIdentifier(dstDpnId).build();
        iface = TunnelUtils.getInterfaceFromConfigDS(trunkRevIfName,
                dataBroker);

        if (iface != null) {
            logger.trace("Removing Reverse Trunk Interface {}", trunkRevIfName);
            OvsTunnelConfigRemoveHelper.removeConfiguration(dataBroker, iface, idManagerService,
                    mdsalManager, parentRefs);
        }
    }

    private static boolean checkIfTrunkExists(BigInteger srcDpnId, BigInteger dstDpnId, Class<? extends TunnelTypeBase> tunType, DataBroker dataBroker) {
        boolean existsFlag = false ;
        DpnTepInterfaceInfo dpnTepInterfaceInfo = ItmScaleUtils.getDpnTepInterfaceFromCache(srcDpnId,dstDpnId);
        if (dpnTepInterfaceInfo != null) {
            if (dpnTepInterfaceInfo.getTunnelName() != null)
                existsFlag = true;
        }
        return existsFlag;
    }
}
