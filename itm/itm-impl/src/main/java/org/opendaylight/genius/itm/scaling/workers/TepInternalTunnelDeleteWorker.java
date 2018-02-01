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
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.cache.DPNTEPsInfoCache;
import org.opendaylight.genius.itm.cache.TunnelStateCache;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.impl.TunnelMonitoringConfig;
import org.opendaylight.genius.itm.scaling.renderer.ovs.confighelpers.OvsTunnelConfigRemoveHelper;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.DpnTepInterfaceInfo;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.ItmScaleUtils;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.TunnelUtils;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeLldp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.tunnel.end.points.TzMembership;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TepInternalTunnelDeleteWorker {
    private static final Logger LOGGER = LoggerFactory.getLogger(TepInternalTunnelDeleteWorker.class) ;

    private TepInternalTunnelDeleteWorker() {
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public static List<ListenableFuture<Void>> deleteTunnels(DataBroker dataBroker, IdManagerService idManagerService,
                                                             IMdsalApiManager mdsalManager,
                                                             List<DPNTEPsInfo> dpnTepsList,
                                                             Collection<DPNTEPsInfo> meshedDpnList,
                                                             TunnelMonitoringConfig tunnelMonitoringConfig,
                                                             DPNTEPsInfoCache dpntePsInfoCache,
                                                             TunnelStateCache tunnelStateCache) {
        LOGGER.trace("TEPs to be deleted {} ", dpnTepsList);
        try {
            if (dpnTepsList == null || dpnTepsList.size() == 0) {
                LOGGER.debug("no vtep to delete");
                return Collections.emptyList();
            }

            if (meshedDpnList == null || meshedDpnList.size() == 0) {
                LOGGER.debug("No Meshed Vteps");
                return Collections.emptyList();
            }
            WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
            for (DPNTEPsInfo srcDpn : dpnTepsList) {
                LOGGER.trace("Processing srcDpn {}", srcDpn);
                List<TunnelEndPoints> meshedEndPtCache =
                        new ArrayList<>(ItmUtils.getTEPsForDpn(srcDpn.getDPNID(), meshedDpnList));
                if (meshedEndPtCache == null) {
                    LOGGER.debug("No Tunnel End Point configured for this DPN {}", srcDpn.getDPNID());
                    continue ;
                }

                LOGGER.debug("Entries in meshEndPointCache {} ", meshedEndPtCache.size());
                for (TunnelEndPoints srcTep : srcDpn.getTunnelEndPoints()) {
                    LOGGER.trace("Processing srcTep {}", srcTep);
                    List<TzMembership> srcTZones = srcTep.getTzMembership();

                    // run through all other DPNS other than srcDpn
                    for (DPNTEPsInfo dstDpn : meshedDpnList) {
                        if (!srcDpn.getDPNID().equals(dstDpn.getDPNID())) {
                            for (TunnelEndPoints dstTep : dstDpn.getTunnelEndPoints()) {
                                LOGGER.trace("Processing dstTep {}", dstTep);
                                if (!ItmUtils.getIntersection(dstTep.getTzMembership(), srcTZones).isEmpty()) {
                                    if (checkIfTrunkExists(dstDpn.getDPNID(), srcDpn.getDPNID(),
                                            srcTep.getTunnelType(), dataBroker)) {
                                        // remove all trunk interfaces
                                        LOGGER.trace("Invoking removeTrunkInterface between source TEP {} , "
                                                + "Destination TEP {} " ,srcTep , dstTep);
                                        removeTrunkInterface(dataBroker, idManagerService, mdsalManager,
                                                srcTep, dstTep, srcDpn.getDPNID(), dstDpn.getDPNID(),
                                                writeTransaction, dpntePsInfoCache, tunnelStateCache,
                                                tunnelMonitoringConfig);
                                    }
                                }
                            }
                        }
                    }

                    // removing vtep / dpn from Tunnels OpDs.
                    InstanceIdentifier<TunnelEndPoints> tepPath =
                            InstanceIdentifier.builder(DpnEndpoints.class).child(DPNTEPsInfo.class, srcDpn.getKey())
                                    .child(TunnelEndPoints.class, srcTep.getKey()).build();

                    LOGGER.trace("Tep Removal of TEP {} from DPNTEPSINFO CONFIG DS with Key {} ",
                            srcTep, srcTep.getKey());
                    writeTransaction.delete(LogicalDatastoreType.CONFIGURATION, tepPath);
                    // remove the tep from the cache
                    meshedEndPtCache.remove(srcTep);
                    Class<? extends TunnelMonitoringTypeBase> monitorProtocol =
                            tunnelMonitoringConfig.getMonitorProtocol();

                    InstanceIdentifier<DPNTEPsInfo> dpnPath =
                            InstanceIdentifier.builder(DpnEndpoints.class).child(DPNTEPsInfo.class, srcDpn.getKey())
                                    .build();
                    if (meshedEndPtCache.isEmpty()) {
                        // remove dpn if no vteps exist on dpn
                        if (monitorProtocol.isAssignableFrom(TunnelMonitoringTypeLldp.class)) {
                            LOGGER.debug("Removing Terminating Service Table Flow ");
                            ItmUtils.setUpOrRemoveTerminatingServiceTable(srcDpn.getDPNID(), mdsalManager, false);
                        }
                        LOGGER.trace("DPN Removal from DPNTEPSINFO CONFIG DS {}", srcDpn.getDPNID());
                        writeTransaction.delete(LogicalDatastoreType.CONFIGURATION, dpnPath);
                        InstanceIdentifier<DpnEndpoints> tnlContainerPath =
                                InstanceIdentifier.builder(DpnEndpoints.class).build();
                        Optional<DpnEndpoints> containerOptional =
                                ItmUtils.read(LogicalDatastoreType.CONFIGURATION, tnlContainerPath, dataBroker);
                        // remove container if no DPNs are present
                        if (containerOptional.isPresent()) {
                            DpnEndpoints deps = containerOptional.get();
                            if (deps.getDPNTEPsInfo() == null || deps.getDPNTEPsInfo().isEmpty()) {
                                LOGGER.trace("Container Removal from DPNTEPSINFO CONFIG DS");
                                writeTransaction.delete(LogicalDatastoreType.CONFIGURATION, tnlContainerPath);
                            }
                        }
                    }
                }
                // SF419 Remove the DPNSTEPs DS
                LOGGER.debug("Deleting TEP Interface information CONFIGURATION datastore with DPNs-Teps "
                        + "for source Dpn", srcDpn.getDPNID());
                // Clean up the DPN TEPs State DS
                ItmScaleUtils.removeTepFromDpnTepInterfaceConfigDS(srcDpn.getDPNID());
                // ITM DIRECT TUNNELS Clean up the container when its empty -- How ???
            }
            return Collections.singletonList(writeTransaction.submit());
        } catch (Exception e1) {
            LOGGER.error("Exception while deleting teps in the list {}, exception ", dpnTepsList, e1);
        }
        return Collections.emptyList();
    }

    private static void removeTrunkInterface(DataBroker dataBroker, IdManagerService idManagerService,
                                             IMdsalApiManager mdsalManager, TunnelEndPoints srcTep,
                                             TunnelEndPoints dstTep, BigInteger srcDpnId, BigInteger dstDpnId,
                                             WriteTransaction writeTransaction, DPNTEPsInfoCache dpntePsInfoCache,
                                             TunnelStateCache tunnelStateCache,
                                             TunnelMonitoringConfig tunnelMonitoringConfig) {
        String trunkfwdIfName = ItmUtils.getTrunkInterfaceName(srcTep.getInterfaceName(),
                srcTep.getIpAddress().getIpv4Address().getValue(),
                dstTep.getIpAddress().getIpv4Address().getValue(),
                srcTep.getTunnelType().getName());
        LOGGER.trace("Removing forward Trunk Interface {}", trunkfwdIfName);
        ParentRefs parentRefs = new ParentRefsBuilder().setDatapathNodeIdentifier(srcDpnId).build();
        Interface iface = TunnelUtils.getInterfaceFromConfigDS(trunkfwdIfName, tunnelMonitoringConfig,
                dpntePsInfoCache);
        // ITM DIRECT TUNNELS -- Call the OVS Worker directly
        if (iface != null) {
            OvsTunnelConfigRemoveHelper.removeConfiguration(dataBroker, iface, idManagerService,
                    mdsalManager, parentRefs, tunnelStateCache);
        }
        String trunkRevIfName = ItmUtils.getTrunkInterfaceName(dstTep.getInterfaceName(),
                dstTep.getIpAddress().getIpv4Address().getValue(),
                srcTep.getIpAddress().getIpv4Address().getValue(),
                srcTep.getTunnelType().getName());
        parentRefs = new ParentRefsBuilder().setDatapathNodeIdentifier(dstDpnId).build();
        iface = TunnelUtils.getInterfaceFromConfigDS(trunkRevIfName, tunnelMonitoringConfig, dpntePsInfoCache);

        if (iface != null) {
            LOGGER.trace("Removing Reverse Trunk Interface {}", trunkRevIfName);
            OvsTunnelConfigRemoveHelper.removeConfiguration(dataBroker, iface, idManagerService, mdsalManager,
                    parentRefs, tunnelStateCache);
        }
    }

    private static boolean checkIfTrunkExists(BigInteger srcDpnId, BigInteger dstDpnId,
                                              Class<? extends TunnelTypeBase> tunType, DataBroker dataBroker) {
        boolean existsFlag = false ;
        DpnTepInterfaceInfo dpnTepInterfaceInfo = ItmScaleUtils.getDpnTepInterfaceFromCache(srcDpnId,dstDpnId);
        if (dpnTepInterfaceInfo != null) {
            if (dpnTepInterfaceInfo.getTunnelName() != null) {
                existsFlag = true;
            }
        }
        return existsFlag;
    }
}
