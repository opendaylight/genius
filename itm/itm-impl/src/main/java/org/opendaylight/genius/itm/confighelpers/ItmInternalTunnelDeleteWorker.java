/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.confighelpers;

import static java.util.Collections.singletonList;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.cache.DpnTepStateCache;
import org.opendaylight.genius.itm.cache.OvsBridgeEntryCache;
import org.opendaylight.genius.itm.cache.OvsBridgeRefEntryCache;
import org.opendaylight.genius.itm.cache.TunnelStateCache;
import org.opendaylight.genius.itm.impl.ITMBatchingUtils;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.impl.TunnelMonitoringConfig;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.genius.itm.utils.DpnTepInterfaceInfo;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeLldp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeLogicalGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.bridge.tunnel.info.OvsBridgeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.bridge.tunnel.info.OvsBridgeEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.bridge.tunnel.info.ovs.bridge.entry.OvsBridgeTunnelEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.bridge.tunnel.info.ovs.bridge.entry.OvsBridgeTunnelEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.ovs.bridge.ref.info.OvsBridgeRefEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelOperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.tunnel.end.points.TzMembership;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnelKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.OperationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItmInternalTunnelDeleteWorker {

    private static final Logger LOG = LoggerFactory.getLogger(ItmInternalTunnelDeleteWorker.class) ;

    private final DataBroker dataBroker;
    private final ManagedNewTransactionRunner txRunner;
    private final JobCoordinator jobCoordinator;
    private final TunnelMonitoringConfig tunnelMonitoringConfig;
    private final IInterfaceManager interfaceManager;
    private final DpnTepStateCache dpnTepStateCache;
    private final OvsBridgeEntryCache ovsBridgeEntryCache;
    private final OvsBridgeRefEntryCache ovsBridgeRefEntryCache;
    private final TunnelStateCache tunnelStateCache;
    private final DirectTunnelUtils directTunnelUtils;

    public ItmInternalTunnelDeleteWorker(DataBroker dataBroker, JobCoordinator jobCoordinator,
                                         TunnelMonitoringConfig tunnelMonitoringConfig,
                                         IInterfaceManager interfaceManager, DpnTepStateCache dpnTepStateCache,
                                         OvsBridgeEntryCache ovsBridgeEntryCache,
                                         OvsBridgeRefEntryCache ovsBridgeRefEntryCache,
                                         TunnelStateCache tunnelStateCache,
                                         DirectTunnelUtils directTunnelUtils) {
        this.dataBroker = dataBroker;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.jobCoordinator = jobCoordinator;
        this.tunnelMonitoringConfig = tunnelMonitoringConfig;
        this.interfaceManager = interfaceManager;
        this.dpnTepStateCache = dpnTepStateCache;
        this.ovsBridgeEntryCache = ovsBridgeEntryCache;
        this.ovsBridgeRefEntryCache = ovsBridgeRefEntryCache;
        this.tunnelStateCache = tunnelStateCache;
        this.directTunnelUtils = directTunnelUtils;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public List<ListenableFuture<Void>> deleteTunnels(IMdsalApiManager mdsalManager,
            Collection<DPNTEPsInfo> dpnTepsList, Collection<DPNTEPsInfo> meshedDpnList) {
        LOG.trace("TEPs to be deleted {} " , dpnTepsList);
        return Collections.singletonList(txRunner.callWithNewWriteOnlyTransactionAndSubmit(writeTransaction -> {
            if (dpnTepsList == null || dpnTepsList.size() == 0) {
                LOG.debug("no vtep to delete");
                return;
            }

            if (meshedDpnList == null || meshedDpnList.size() == 0) {
                LOG.debug("No Meshed Vteps");
                return;
            }
            for (DPNTEPsInfo srcDpn : dpnTepsList) {
                LOG.trace("Processing srcDpn {}", srcDpn);

                List<TunnelEndPoints> meshedEndPtCache = ItmUtils.getTEPsForDpn(srcDpn.getDPNID(), meshedDpnList);
                if (meshedEndPtCache == null) {
                    LOG.debug("No Tunnel End Point configured for this DPN {}", srcDpn.getDPNID());
                    continue;
                }
                LOG.debug("Entries in meshEndPointCache {} for DPN Id{} ", meshedEndPtCache.size(), srcDpn.getDPNID());
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
                                    if (ItmUtils.getIntersection(dstTep.getTzMembership(),
                                            originalTzMembership).size() == 1) {
                                        if (interfaceManager.isItmDirectTunnelsEnabled()) {
                                            if (checkIfTepInterfaceExists(dstDpn.getDPNID(), srcDpn.getDPNID())) {
                                                // remove all trunk interfaces
                                                LOG.trace("Invoking removeTrunkInterface between source TEP {} , "
                                                        + "Destination TEP {} " ,srcTep , dstTep);
                                                removeTunnelInterfaceFromOvsdb(srcTep, dstTep, srcDpn.getDPNID(),
                                                        dstDpn.getDPNID(), writeTransaction);

                                            }

                                        } else {
                                            if (checkIfTrunkExists(dstDpn.getDPNID(), srcDpn.getDPNID(),
                                                    srcTep.getTunnelType(), dataBroker)) {
                                                // remove all trunk interfaces
                                                LOG.trace("Invoking removeTrunkInterface between source TEP {} , "
                                                        + "Destination TEP {} ", srcTep, dstTep);
                                                removeTrunkInterface(srcTep, dstTep, srcDpn.getDPNID(),
                                                        dstDpn.getDPNID(), writeTransaction);
                                            }
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
                                    LOG.debug("Removing TZ list {} from Existing TZ list {} ", srcTZones,
                                            dstTep.getTzMembership());
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

                        LOG.trace("Tep Removal of TEP {} from DPNTEPSINFO CONFIG DS with Key {} ", srcTep,
                                srcTep.getKey());
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
                    if (interfaceManager.isItmDirectTunnelsEnabled()) {
                        // SF419 Remove the DPNSTEPs DS
                        LOG.debug("Deleting TEP Interface information from Config datastore with DPNs-Teps "
                                + "for source Dpn {}", srcDpn.getDPNID());
                        // Clean up the DPN TEPs State DS
                        dpnTepStateCache.removeTepFromDpnTepInterfaceConfigDS(srcDpn.getDPNID());
                    }
                }
            }
        }));
    }

    private void removeTrunkInterface(TunnelEndPoints srcTep, TunnelEndPoints dstTep,
                                      BigInteger srcDpnId, BigInteger dstDpnId, WriteTransaction transaction) {
        String trunkfwdIfName = ItmUtils.getTrunkInterfaceName(srcTep.getInterfaceName(),
                new String(srcTep.getIpAddress().getValue()),
                new String(dstTep.getIpAddress().getValue()),
                srcTep.getTunnelType().getName());
        LOG.trace("Removing forward Trunk Interface " + trunkfwdIfName);
        InstanceIdentifier<Interface> trunkIdentifier = ItmUtils.buildId(trunkfwdIfName);
        LOG.debug(" Removing Trunk Interface Name - {} , Id - {} from Config DS ",
                trunkfwdIfName, trunkIdentifier) ;
        transaction.delete(LogicalDatastoreType.CONFIGURATION, trunkIdentifier);
        ItmUtils.ITM_CACHE.removeInterface(trunkfwdIfName);
        // also update itm-state ds -- Delete the forward tunnel-interface from the tunnel list
        InstanceIdentifier<InternalTunnel> path = InstanceIdentifier.create(TunnelList.class)
                .child(InternalTunnel.class, new InternalTunnelKey(dstDpnId, srcDpnId, srcTep.getTunnelType()));
        transaction.delete(LogicalDatastoreType.CONFIGURATION,path) ;
        ItmUtils.ITM_CACHE.removeInternalTunnel(trunkfwdIfName);
        // Release the Ids for the forward trunk interface Name
        ItmUtils.releaseIdForTrunkInterfaceName(srcTep.getInterfaceName(),
                new String(srcTep.getIpAddress().getValue()),
                new String(dstTep.getIpAddress().getValue()),
                srcTep.getTunnelType().getName());
        removeLogicalGroupTunnel(srcDpnId, dstDpnId);

        String trunkRevIfName = ItmUtils.getTrunkInterfaceName(dstTep.getInterfaceName(),
                new String(dstTep.getIpAddress().getValue()),
                new String(srcTep.getIpAddress().getValue()),
                srcTep.getTunnelType().getName());
        LOG.trace("Removing Reverse Trunk Interface {}", trunkRevIfName);
        trunkIdentifier = ItmUtils.buildId(trunkRevIfName);
        LOG.debug(" Removing Trunk Interface Name - {} , Id - {} from Config DS ",
                trunkRevIfName, trunkIdentifier) ;
        transaction.delete(LogicalDatastoreType.CONFIGURATION, trunkIdentifier);
        ItmUtils.ITM_CACHE.removeInternalTunnel(trunkRevIfName);
        // also update itm-state ds -- Delete the reverse tunnel-interface from the tunnel list
        path = InstanceIdentifier.create(TunnelList.class)
                .child(InternalTunnel.class, new InternalTunnelKey(srcDpnId, dstDpnId, dstTep.getTunnelType()));
        transaction.delete(LogicalDatastoreType.CONFIGURATION,path) ;

        // Release the Ids for the reverse trunk interface Name
        ItmUtils.releaseIdForTrunkInterfaceName(dstTep.getInterfaceName(),
                new String(dstTep.getIpAddress().getValue()),
                new String(srcTep.getIpAddress().getValue()),
                dstTep.getTunnelType().getName());
        removeLogicalGroupTunnel(dstDpnId, srcDpnId);
    }

    private static boolean checkIfTrunkExists(BigInteger srcDpnId, BigInteger dstDpnId,
                                              Class<? extends TunnelTypeBase> tunType, DataBroker dataBroker) {
        InstanceIdentifier<InternalTunnel> path = InstanceIdentifier.create(TunnelList.class)
                .child(InternalTunnel.class, new InternalTunnelKey(dstDpnId, srcDpnId, tunType));
        return ItmUtils.read(LogicalDatastoreType.CONFIGURATION,path, dataBroker).isPresent();
    }

    private void removeLogicalGroupTunnel(BigInteger srcDpnId, BigInteger dstDpnId) {
        boolean tunnelAggregationEnabled = ItmTunnelAggregationHelper.isTunnelAggregationEnabled();
        if (!tunnelAggregationEnabled) {
            return;
        }
        String logicTunnelName = ItmUtils.getLogicalTunnelGroupName(srcDpnId, dstDpnId);
        ItmTunnelAggregationDeleteWorker addWorker =
                new ItmTunnelAggregationDeleteWorker(logicTunnelName, srcDpnId, dstDpnId, dataBroker);
        jobCoordinator.enqueueJob(logicTunnelName, addWorker);
    }

    private static class ItmTunnelAggregationDeleteWorker implements Callable<List<ListenableFuture<Void>>> {

        private final String logicTunnelName;
        private final BigInteger srcDpnId;
        private final BigInteger dstDpnId;
        private final ManagedNewTransactionRunner txRunner;

        ItmTunnelAggregationDeleteWorker(String groupName, BigInteger srcDpnId, BigInteger dstDpnId, DataBroker db) {
            this.logicTunnelName = groupName;
            this.srcDpnId = srcDpnId;
            this.dstDpnId = dstDpnId;
            this.txRunner = new ManagedNewTransactionRunnerImpl(db);
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            Collection<InternalTunnel> tunnels = ItmUtils.ITM_CACHE.getAllInternalTunnel();
            if (tunnels == null) {
                return Collections.emptyList();
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
                return singletonList(txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx -> {
                    LOG.debug("MULTIPLE_VxLAN_TUNNELS: remove the logical tunnel group {} because a last tunnel"
                        + " interface on srcDpnId {} dstDpnId {} is removed", logicTunnelName, srcDpnId, dstDpnId);
                    InstanceIdentifier<Interface> trunkIdentifier = ItmUtils.buildId(logicTunnelName);
                    tx.delete(LogicalDatastoreType.CONFIGURATION, trunkIdentifier);
                    ItmUtils.ITM_CACHE.removeInterface(logicTunnelName);
                    InstanceIdentifier<InternalTunnel> path = InstanceIdentifier.create(TunnelList.class)
                            .child(InternalTunnel.class,
                                    new InternalTunnelKey(dstDpnId, srcDpnId, TunnelTypeLogicalGroup.class));
                    tx.delete(LogicalDatastoreType.CONFIGURATION, path);
                    ItmUtils.ITM_CACHE.removeInternalTunnel(logicTunnelName);
                }));
            } else if (!emptyTunnelGroup) {
                LOG.debug("MULTIPLE_VxLAN_TUNNELS: not last tunnel in logical tunnel group {}", logicTunnelName);
            }
            return Collections.emptyList();
        }
    }

    private void removeTunnelInterfaceFromOvsdb(TunnelEndPoints srcTep, TunnelEndPoints dstTep, BigInteger srcDpnId,
                                                BigInteger dstDpnId, WriteTransaction writeTransaction) {
        String trunkfwdIfName = ItmUtils.getTrunkInterfaceName(srcTep.getInterfaceName(),
                srcTep.getIpAddress().getIpv4Address().getValue(),
                dstTep.getIpAddress().getIpv4Address().getValue(),
                srcTep.getTunnelType().getName());
        LOG.trace("Removing forward Trunk Interface {}", trunkfwdIfName);
        ParentRefs parentRefs = new ParentRefsBuilder().setDatapathNodeIdentifier(srcDpnId).build();
        Interface iface = dpnTepStateCache.getInterfaceFromCache(trunkfwdIfName);
        // ITM DIRECT TUNNELS -- Call the OVS Worker directly
        if (iface != null) {
            try {
                removeConfiguration(iface, parentRefs, writeTransaction);
            } catch (ExecutionException | InterruptedException | OperationFailedException e) {
                LOG.error("Cannot Delete Tunnel {} as OVS Bridge Entry is NULL ", iface.getName(), e);
            }
        }
        String trunkRevIfName = ItmUtils.getTrunkInterfaceName(dstTep.getInterfaceName(),
                dstTep.getIpAddress().getIpv4Address().getValue(),
                srcTep.getIpAddress().getIpv4Address().getValue(),
                srcTep.getTunnelType().getName());
        parentRefs = new ParentRefsBuilder().setDatapathNodeIdentifier(dstDpnId).build();
        iface = dpnTepStateCache.getInterfaceFromCache(trunkRevIfName);

        if (iface != null) {
            try {
                LOG.trace("Removing Reverse Trunk Interface {}", trunkRevIfName);
                removeConfiguration(iface, parentRefs, writeTransaction);
            } catch (ExecutionException | InterruptedException | OperationFailedException e) {
                LOG.error("Cannot Delete Tunnel {} as OVS Bridge Entry is NULL ", iface.getName(), e);
            }
        }
    }

    private boolean checkIfTepInterfaceExists(BigInteger srcDpnId, BigInteger dstDpnId) {
        DpnTepInterfaceInfo dpnTepInterfaceInfo = dpnTepStateCache.getDpnTepInterface(srcDpnId,dstDpnId);
        if (dpnTepInterfaceInfo != null) {
            if (dpnTepInterfaceInfo.getTunnelName() != null) {
                return true;
            }
        }
        return false;
    }

    private void removeConfiguration(Interface interfaceOld, ParentRefs parentRefs, WriteTransaction
            writeTransaction) throws ExecutionException, InterruptedException, OperationFailedException {
        IfTunnel ifTunnel = interfaceOld.getAugmentation(IfTunnel.class);
        if (ifTunnel != null) {
            // Check if the same transaction can be used across Config and operational shards
            removeTunnelConfiguration(parentRefs, interfaceOld.getName(), ifTunnel, writeTransaction);
        }
    }

    private void removeTunnelConfiguration(ParentRefs parentRefs, String interfaceName, IfTunnel ifTunnel,
                                           WriteTransaction transaction)
            throws ExecutionException, InterruptedException, OperationFailedException {

        LOG.info("removing tunnel configuration for {}", interfaceName);
        BigInteger dpId = null;
        if (parentRefs != null) {
            dpId = parentRefs.getDatapathNodeIdentifier();
        }

        if (dpId == null) {
            return;
        }

        Optional<OvsBridgeRefEntry> ovsBridgeRefEntryOptional = ovsBridgeRefEntryCache.get(dpId);
        Optional<OvsBridgeEntry> ovsBridgeEntryOptional;
        OvsdbBridgeRef ovsdbBridgeRef = null;
        if (ovsBridgeRefEntryOptional.isPresent()) {
            ovsdbBridgeRef = ovsBridgeRefEntryOptional.get().getOvsBridgeReference();
        } else {
            ovsBridgeEntryOptional = ovsBridgeEntryCache.get(dpId);
            if (ovsBridgeEntryOptional.isPresent()) {
                ovsdbBridgeRef = ovsBridgeEntryOptional.get().getOvsBridgeReference();
            }
        }

        if (ovsdbBridgeRef != null) {
            removeTerminationEndPoint(ovsdbBridgeRef.getValue(), interfaceName);
        }

        // delete tunnel ingress flow
        removeTunnelIngressFlow(interfaceName, dpId);

        // delete bridge to tunnel interface mappings
        OvsBridgeEntryKey bridgeEntryKey = new OvsBridgeEntryKey(dpId);
        InstanceIdentifier<OvsBridgeEntry> bridgeEntryIid =
                DirectTunnelUtils.getOvsBridgeEntryIdentifier(bridgeEntryKey);

        ovsBridgeEntryOptional = ovsBridgeEntryCache.get(dpId);
        if (ovsBridgeEntryOptional.isPresent()) {
            List<OvsBridgeTunnelEntry> bridgeTunnelEntries = ovsBridgeEntryOptional.get().getOvsBridgeTunnelEntry();
            deleteBridgeInterfaceEntry(bridgeEntryKey, bridgeTunnelEntries, bridgeEntryIid, interfaceName);
            // IfIndex needs to be removed only during State Clean up not Config
            // TunnelMetaUtils.removeLportTagInterfaceMap(idManager, defaultOperationalShardTransaction, interfaceName);
            cleanUpInterfaceWithUnknownState(interfaceName, parentRefs, ifTunnel, transaction);
            directTunnelUtils.removeLportTagInterfaceMap(interfaceName);
        }
    }

    private void removeTerminationEndPoint(InstanceIdentifier<?> bridgeIid, String interfaceName) {
        LOG.debug("removing termination point for {}", interfaceName);
        InstanceIdentifier<TerminationPoint> tpIid = DirectTunnelUtils.createTerminationPointInstanceIdentifier(
                InstanceIdentifier.keyOf(bridgeIid.firstIdentifierOf(Node.class)), interfaceName);
        ITMBatchingUtils.delete(tpIid, ITMBatchingUtils.EntityType.TOPOLOGY_CONFIG);
    }

    private void removeTunnelIngressFlow(String interfaceName, BigInteger dpId) throws ReadFailedException {
        long portNo = tunnelStateCache.getNodeConnectorIdFromInterface(interfaceName);
        DpnTepInterfaceInfo dpnTepInfo = dpnTepStateCache.getTunnelFromCache(interfaceName);
        directTunnelUtils.makeTunnelIngressFlow(dpnTepInfo, dpId, portNo,interfaceName , -1, NwConstants.DEL_FLOW);
    }

    // if the node is shutdown, there will be stale interface state entries,
    // with unknown op-state, clear them.
    private void cleanUpInterfaceWithUnknownState(String interfaceName, ParentRefs parentRefs, IfTunnel ifTunnel,
                                                  WriteTransaction transaction) throws ReadFailedException {
        Optional<StateTunnelList> stateTunnelList =
                tunnelStateCache.get(tunnelStateCache.getStateTunnelListIdentifier(interfaceName));
        if (stateTunnelList.isPresent() && stateTunnelList.get().getOperState() == TunnelOperStatus.Unknown) {
            String staleInterface = ifTunnel != null ? interfaceName : parentRefs.getParentInterface();
            LOG.debug("cleaning up parent-interface for {}, since the oper-status is UNKNOWN", interfaceName);
            directTunnelUtils.deleteTunnelStateEntry(staleInterface);
        }
    }

    private void deleteBridgeInterfaceEntry(OvsBridgeEntryKey bridgeEntryKey,
                                            List<OvsBridgeTunnelEntry> bridgeTunnelEntries,
                                            InstanceIdentifier<OvsBridgeEntry> bridgeEntryIid,
                                            String interfaceName) {
        OvsBridgeTunnelEntryKey bridgeTunnelEntryKey = new OvsBridgeTunnelEntryKey(interfaceName);
        InstanceIdentifier<OvsBridgeTunnelEntry> bridgeTunnelEntryIid =
                DirectTunnelUtils.getBridgeTunnelEntryIdentifier(bridgeEntryKey, bridgeTunnelEntryKey);
        ITMBatchingUtils.delete(bridgeTunnelEntryIid, ITMBatchingUtils.EntityType.DEFAULT_CONFIG);
        if (bridgeTunnelEntries.size() <= 1) {
            ITMBatchingUtils.delete(bridgeEntryIid, ITMBatchingUtils.EntityType.DEFAULT_CONFIG);
        }
    }
}
