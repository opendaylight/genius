/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
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
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeLldp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeLogicalGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.tunnel.end.points.TzMembership;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnelKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItmInternalTunnelDeleteWorker {
    private static final Logger LOG = LoggerFactory.getLogger(ItmInternalTunnelDeleteWorker.class) ;

    @SuppressWarnings("checkstyle:IllegalCatch")
    public static List<ListenableFuture<Void>> deleteTunnels(DataBroker dataBroker, IdManagerService idManagerService,
                                                             IMdsalApiManager mdsalManager,
                                                             List<DPNTEPsInfo> dpnTepsList,
                                                             List<DPNTEPsInfo> meshedDpnList) {
        LOG.trace("TEPs to be deleted {} " , dpnTepsList);
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        try {
            if (dpnTepsList == null || dpnTepsList.size() == 0) {
                LOG.debug("no vtep to delete");
                return futures ;
            }

            if (meshedDpnList == null || meshedDpnList.size() == 0) {
                LOG.debug("No Meshed Vteps");
                return futures ;
            }
            for (DPNTEPsInfo srcDpn : dpnTepsList) {
                LOG.trace("Processing srcDpn {}", srcDpn);

                List<TunnelEndPoints> meshedEndPtCache = ItmUtils.getTEPsForDpn(srcDpn.getDPNID(), meshedDpnList);
                if (meshedEndPtCache == null) {
                    LOG.debug("No Tunnel End Point configured for this DPN {}", srcDpn.getDPNID());
                    continue ;
                }
                LOG.debug("Entries in meshEndPointCache {} for DPN Id {}", meshedEndPtCache.size(), srcDpn.getDPNID());
                for (TunnelEndPoints srcTep : srcDpn.getTunnelEndPoints()) {
                    LOG.trace("Processing srcTep {}", srcTep);
                    List<TzMembership> srcTZones = srcTep.getTzMembership();
                    boolean tepDeleteFlag = false;
                    // First, take care of tunnel removal, so run through all other DPNS other than srcDpn
                    // In the tep received from Delete DCN, the membership list will always be 1
                    // as the DCN is at transport zone level
                    // Hence if a tunnel is shared across TZs, compare the original membership list between end points
                    // to decide if tunnel to be deleted.
                    for (DPNTEPsInfo dstDpn : meshedDpnList) {
                        if (!srcDpn.getDPNID().equals(dstDpn.getDPNID())) {
                            for (TunnelEndPoints dstTep : dstDpn.getTunnelEndPoints()) {
                                if (!ItmUtils.getIntersection(dstTep.getTzMembership(), srcTZones).isEmpty()) {
                                    List<TzMembership> originalTzMembership =
                                            ItmUtils.getOriginalTzMembership(srcTep, srcDpn.getDPNID(), meshedDpnList);
                                    if (ItmUtils.getIntersection(dstTep.getTzMembership(), originalTzMembership).size()
                                            == 1) {
                                        if (checkIfTrunkExists(dstDpn.getDPNID(), srcDpn.getDPNID(),
                                                srcTep.getTunnelType(), dataBroker)) {
                                            // remove all trunk interfaces
                                            LOG.trace("Invoking removeTrunkInterface between source TEP {} , "
                                                    + "Destination TEP {} ", srcTep, dstTep);
                                            removeTrunkInterface(dataBroker, idManagerService, srcTep, dstTep, srcDpn
                                                    .getDPNID(), dstDpn.getDPNID(), writeTransaction, futures);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    for (DPNTEPsInfo dstDpn : meshedDpnList) {
                        // Second, take care of Tep TZ membership and identify if tep can be removed
                        if (srcDpn.getDPNID().equals(dstDpn.getDPNID())) {
                            // Same DPN, so remove the TZ membership
                            for (TunnelEndPoints dstTep : dstDpn.getTunnelEndPoints()) {
                                if (dstTep.getIpAddress().equals(srcTep.getIpAddress())) {
                                    // Remove the deleted TZ membership from the TEP
                                    LOG.debug("Removing TZ list {} from Existing TZ list {} ",
                                            srcTZones, dstTep.getTzMembership());
                                    List<TzMembership> updatedList =
                                            ItmUtils.removeTransportZoneMembership(dstTep, srcTZones);
                                    if (updatedList.isEmpty()) {
                                        LOG.debug(" This TEP can be deleted {}", srcTep);
                                        tepDeleteFlag = true;
                                    } else {
                                        TunnelEndPointsBuilder modifiedTepBld = new TunnelEndPointsBuilder(dstTep);
                                        modifiedTepBld.setTzMembership(updatedList);
                                        TunnelEndPoints modifiedTep = modifiedTepBld.build() ;
                                        InstanceIdentifier<TunnelEndPoints> tepPath = InstanceIdentifier
                                                .builder(DpnEndpoints.class)
                                                .child(DPNTEPsInfo.class, dstDpn.getKey())
                                                .child(TunnelEndPoints.class, dstTep.getKey()).build();

                                        LOG.debug(" Store the modified Tep in DS {} ", modifiedTep);
                                        writeTransaction.put(LogicalDatastoreType.CONFIGURATION, tepPath, modifiedTep);
                                    }
                                }
                            }
                        }
                    }
                    if (tepDeleteFlag) {
                        // Third, removing vtep / dpn from Tunnels OpDs.
                        InstanceIdentifier<TunnelEndPoints> tepPath =
                                InstanceIdentifier.builder(DpnEndpoints.class).child(DPNTEPsInfo.class, srcDpn.getKey())
                                        .child(TunnelEndPoints.class, srcTep.getKey()).build();

                        LOG.trace("Tep Removal of TEP {} from DPNTEPSINFO CONFIG DS with Key {}",
                                srcTep, srcTep.getKey());
                        writeTransaction.delete(LogicalDatastoreType.CONFIGURATION, tepPath);
                        // remove the tep from the cache
                        meshedEndPtCache.remove(srcTep);
                        Class<? extends TunnelMonitoringTypeBase> monitorProtocol =
                                ItmUtils.determineMonitorProtocol(dataBroker);
                        InstanceIdentifier<DPNTEPsInfo> dpnPath =
                                InstanceIdentifier.builder(DpnEndpoints.class).child(DPNTEPsInfo.class, srcDpn.getKey())
                                        .build();

                        if (meshedEndPtCache.isEmpty()) {
                            // remove dpn if no vteps exist on dpn
                            if (monitorProtocol.isAssignableFrom(TunnelMonitoringTypeLldp.class)) {
                                LOG.debug("Removing Terminating Service Table Flow ");
                                ItmUtils.setUpOrRemoveTerminatingServiceTable(srcDpn.getDPNID(), mdsalManager, false);
                            }
                            LOG.trace("DPN Removal from DPNTEPSINFO CONFIG DS {}", srcDpn.getDPNID());
                            writeTransaction.delete(LogicalDatastoreType.CONFIGURATION, dpnPath);
                            InstanceIdentifier<DpnEndpoints> tnlContainerPath =
                                    InstanceIdentifier.builder(DpnEndpoints.class).build();
                            Optional<DpnEndpoints> containerOptional =
                                    ItmUtils.read(LogicalDatastoreType.CONFIGURATION,
                                            tnlContainerPath, dataBroker);
                            // remove container if no DPNs are present
                            if (containerOptional.isPresent()) {
                                DpnEndpoints deps = containerOptional.get();
                                if (deps.getDPNTEPsInfo() == null || deps.getDPNTEPsInfo().isEmpty()) {
                                    LOG.trace("Container Removal from DPNTEPSINFO CONFIG DS");
                                    writeTransaction.delete(LogicalDatastoreType.CONFIGURATION, tnlContainerPath);
                                }
                            }
                        }
                    }
                }
            }
            futures.add(writeTransaction.submit());
        } catch (Exception e1) {
            LOG.error("exception while deleting tep", e1);
        }
        return futures ;
    }

    private static void removeTrunkInterface(DataBroker dataBroker, IdManagerService idManagerService,
                                             TunnelEndPoints srcTep, TunnelEndPoints dstTep, BigInteger srcDpnId,
                                             BigInteger dstDpnId, WriteTransaction transaction,
                                             List<ListenableFuture<Void>> futures) {
        String trunkfwdIfName = ItmUtils.getTrunkInterfaceName(idManagerService, srcTep.getInterfaceName(),
                new String(srcTep.getIpAddress().getValue()),
                new String(dstTep.getIpAddress().getValue()),
                srcTep.getTunnelType().getName());
        LOG.trace("Removing forward Trunk Interface {}", trunkfwdIfName);
        InstanceIdentifier<Interface> trunkIdentifier = ItmUtils.buildId(trunkfwdIfName);
        LOG.debug(" Removing Trunk Interface Name - {} , Id - {} from Config DS ",
                trunkfwdIfName, trunkIdentifier) ;
        transaction.delete(LogicalDatastoreType.CONFIGURATION, trunkIdentifier);
        ItmUtils.itmCache.removeInterface(trunkfwdIfName);
        // also update itm-state ds -- Delete the forward tunnel-interface from the tunnel list
        InstanceIdentifier<InternalTunnel> path = InstanceIdentifier.create(TunnelList.class)
                .child(InternalTunnel.class, new InternalTunnelKey(dstDpnId, srcDpnId, srcTep.getTunnelType()));
        transaction.delete(LogicalDatastoreType.CONFIGURATION,path) ;
        ItmUtils.itmCache.removeInternalTunnel(trunkfwdIfName);
        // Release the Ids for the forward trunk interface Name
        ItmUtils.releaseIdForTrunkInterfaceName(idManagerService,srcTep.getInterfaceName(),
                new String(srcTep.getIpAddress().getValue()),
                new String(dstTep.getIpAddress().getValue()),
                srcTep.getTunnelType().getName());
        removeLogicalGroupTunnel(srcDpnId, dstDpnId, dataBroker);

        String trunkRevIfName = ItmUtils.getTrunkInterfaceName(idManagerService, dstTep.getInterfaceName(),
                new String(dstTep.getIpAddress().getValue()),
                new String(srcTep.getIpAddress().getValue()),
                srcTep.getTunnelType().getName());
        LOG.trace("Removing Reverse Trunk Interface {}", trunkRevIfName);
        trunkIdentifier = ItmUtils.buildId(trunkRevIfName);
        LOG.debug(" Removing Trunk Interface Name - {} , Id - {} from Config DS ",
                trunkRevIfName, trunkIdentifier) ;
        transaction.delete(LogicalDatastoreType.CONFIGURATION, trunkIdentifier);
        ItmUtils.itmCache.removeInternalTunnel(trunkRevIfName);
        // also update itm-state ds -- Delete the reverse tunnel-interface from the tunnel list
        path = InstanceIdentifier.create(TunnelList.class)
                .child(InternalTunnel.class, new InternalTunnelKey(srcDpnId, dstDpnId, dstTep.getTunnelType()));
        transaction.delete(LogicalDatastoreType.CONFIGURATION,path) ;

        // Release the Ids for the reverse trunk interface Name
        ItmUtils.releaseIdForTrunkInterfaceName(idManagerService, dstTep.getInterfaceName(),
                new String(dstTep.getIpAddress().getValue()),
                new String(srcTep.getIpAddress().getValue()),
                dstTep.getTunnelType().getName());
        removeLogicalGroupTunnel(dstDpnId, srcDpnId, dataBroker);
    }

    private static boolean checkIfTrunkExists(BigInteger srcDpnId, BigInteger dstDpnId,
                                              Class<? extends TunnelTypeBase> tunType, DataBroker dataBroker) {
        InstanceIdentifier<InternalTunnel> path = InstanceIdentifier.create(TunnelList.class)
                .child(InternalTunnel.class, new InternalTunnelKey(dstDpnId, srcDpnId, tunType));
        return ItmUtils.read(LogicalDatastoreType.CONFIGURATION,path, dataBroker).isPresent();
    }

    private static void removeLogicalGroupTunnel(BigInteger srcDpnId, BigInteger dstDpnId,
                                                 DataBroker dataBroker) {
        boolean tunnelAggregationEnabled = ItmTunnelAggregationHelper.isTunnelAggregationEnabled();
        if (!tunnelAggregationEnabled) {
            return;
        }
        String logicTunnelName = ItmUtils.getLogicalTunnelGroupName(srcDpnId, dstDpnId);
        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
        ItmTunnelAggregationDeleteWorker addWorker =
                new ItmTunnelAggregationDeleteWorker(logicTunnelName, srcDpnId, dstDpnId, dataBroker);
        coordinator.enqueueJob(logicTunnelName, addWorker);
    }

    private static class ItmTunnelAggregationDeleteWorker implements Callable<List<ListenableFuture<Void>>> {

        private final String logicTunnelName;
        private final BigInteger srcDpnId;
        private final BigInteger dstDpnId;
        private final DataBroker dataBroker;

        ItmTunnelAggregationDeleteWorker(String groupName, BigInteger srcDpnId, BigInteger dstDpnId, DataBroker db) {
            this.logicTunnelName = groupName;
            this.srcDpnId = srcDpnId;
            this.dstDpnId = dstDpnId;
            this.dataBroker = db;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            List<ListenableFuture<Void>> futures = new ArrayList<>();
            Collection<InternalTunnel> tunnels = ItmUtils.itmCache.getAllInternalTunnel();
            if (tunnels == null) {
                return futures;
            }
            //The logical tunnel interface be removed only when the last tunnel interface on each OVS is deleted
            boolean emptyTunnelGroup = true;
            boolean foundLogicGroupIface = false;
            for (InternalTunnel tunl : tunnels) {
                if (tunl.getSourceDPN().equals(srcDpnId) && tunl.getDestinationDPN().equals(dstDpnId)) {
                    if (tunl.getTransportType().isAssignableFrom(TunnelTypeVxlan.class)
                            && tunl.getTunnelInterfaceNames() != null && !tunl.getTunnelInterfaceNames().isEmpty()) {
                        emptyTunnelGroup = false;
                        break;
                    } else if (tunl.getTransportType().isAssignableFrom(TunnelTypeLogicalGroup.class)) {
                        foundLogicGroupIface = true;
                    }
                }
            }
            if (emptyTunnelGroup && foundLogicGroupIface) {
                WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
                LOG.debug("MULTIPLE_VxLAN_TUNNELS: remove the logical tunnel group {} because a last tunnel"
                    + " interface on srcDpnId {} dstDpnId {} is removed", logicTunnelName, srcDpnId, dstDpnId);
                InstanceIdentifier<Interface> trunkIdentifier = ItmUtils.buildId(logicTunnelName);
                tx.delete(LogicalDatastoreType.CONFIGURATION, trunkIdentifier);
                ItmUtils.itmCache.removeInterface(logicTunnelName);
                InstanceIdentifier<InternalTunnel> path = InstanceIdentifier.create(TunnelList.class)
                        .child(InternalTunnel.class,
                                new InternalTunnelKey(dstDpnId, srcDpnId, TunnelTypeLogicalGroup.class));
                tx.delete(LogicalDatastoreType.CONFIGURATION, path);
                ItmUtils.itmCache.removeInternalTunnel(logicTunnelName);
                futures.add(tx.submit());
            } else if (!emptyTunnelGroup) {
                LOG.debug("MULTIPLE_VxLAN_TUNNELS: not last tunnel in logical tunnel group {}", logicTunnelName);
            }
            return futures;
        }
    }
}
