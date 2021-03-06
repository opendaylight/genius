/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.renderer.ovs.confighelpers;

import static org.opendaylight.mdsal.binding.util.Datastore.CONFIGURATION;
import static org.opendaylight.mdsal.binding.util.Datastore.OPERATIONAL;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.aries.blueprint.annotation.service.Reference;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.genius.cloudscaler.api.TombstonedNodeManager;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.commons.AlivenessMonitorUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.genius.interfacemanager.renderer.ovs.utilities.SouthboundUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.util.Datastore.Configuration;
import org.opendaylight.mdsal.binding.util.Datastore.Operational;
import org.opendaylight.mdsal.binding.util.ManagedNewTransactionRunner;
import org.opendaylight.mdsal.binding.util.ManagedNewTransactionRunnerImpl;
import org.opendaylight.mdsal.binding.util.TypedReadWriteTransaction;
import org.opendaylight.mdsal.binding.util.TypedWriteTransaction;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge._interface.info.BridgeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge._interface.info.BridgeEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge._interface.info.bridge.entry.BridgeInterfaceEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge._interface.info.bridge.entry.BridgeInterfaceEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeLogicalGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class OvsInterfaceConfigRemoveHelper {
    private static final Logger LOG = LoggerFactory.getLogger(OvsInterfaceConfigRemoveHelper.class);
    private static final Logger EVENT_LOGGER = LoggerFactory.getLogger("GeniusEventLogger");

    private final DataBroker dataBroker;
    private final ManagedNewTransactionRunner txRunner;
    private final IMdsalApiManager mdsalApiManager;
    private final JobCoordinator coordinator;
    private final InterfaceManagerCommonUtils interfaceManagerCommonUtils;
    private final AlivenessMonitorUtils alivenessMonitorUtils;
    private final InterfaceMetaUtils interfaceMetaUtils;
    private final SouthboundUtils southboundUtils;
    private final TombstonedNodeManager tombstonedNodeManager;

    @Inject
    public OvsInterfaceConfigRemoveHelper(@Reference DataBroker dataBroker,
                                          AlivenessMonitorUtils alivenessMonitorUtils,
                                          @Reference IMdsalApiManager mdsalApiManager,
                                          @Reference JobCoordinator coordinator,
                                          InterfaceManagerCommonUtils interfaceManagerCommonUtils,
                                          InterfaceMetaUtils interfaceMetaUtils,
                                          SouthboundUtils southboundUtils,
                                          TombstonedNodeManager tombstonedNodeManager) {
        this.dataBroker = dataBroker;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.mdsalApiManager = mdsalApiManager;
        this.coordinator = coordinator;
        this.interfaceManagerCommonUtils = interfaceManagerCommonUtils;
        this.alivenessMonitorUtils = alivenessMonitorUtils;
        this.interfaceMetaUtils = interfaceMetaUtils;
        this.southboundUtils = southboundUtils;
        this.tombstonedNodeManager = tombstonedNodeManager;
    }

    public List<? extends ListenableFuture<?>> removeConfiguration(Interface interfaceOld, ParentRefs parentRefs) {
        List<ListenableFuture<?>> futures = new ArrayList<>();
        futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(OPERATIONAL, operTx -> {
            IfTunnel ifTunnel = interfaceOld.augmentation(IfTunnel.class);
            if (ifTunnel != null) {
                futures.add(txRunner.callWithNewReadWriteTransactionAndSubmit(CONFIGURATION,
                    confTx -> removeTunnelConfiguration(parentRefs, interfaceOld.getName(), ifTunnel, operTx, confTx)));
            } else {
                removeVlanConfiguration(parentRefs, interfaceOld.getName(),
                        interfaceOld.augmentation(IfL2vlan.class), operTx, futures);
            }
        }));
        return futures;
    }

    private void removeVlanConfiguration(ParentRefs parentRefs, String interfaceName,
            IfL2vlan ifL2vlan, TypedWriteTransaction<Operational> tx,
            List<ListenableFuture<?>> futures) throws ReadFailedException {

        if (parentRefs == null || ifL2vlan == null || IfL2vlan.L2vlanMode.Trunk != ifL2vlan.getL2vlanMode()
                && IfL2vlan.L2vlanMode.Transparent != ifL2vlan.getL2vlanMode()) {
            return;
        }
        LOG.info("removing vlan configuration for interface {}", interfaceName);
        interfaceManagerCommonUtils.deleteInterfaceStateInformation(interfaceName, tx);

        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.interfaces.rev140508.interfaces.state.Interface ifState = interfaceManagerCommonUtils
                .getInterfaceState(interfaceName);

        if (ifState == null) {
            LOG.debug("could not fetch interface state corresponding to {}, probably already removed as part of port "
                    + "removal event, proceeding with remaining config cleanups", interfaceName);
        }

        cleanUpInterfaceWithUnknownState(interfaceName, parentRefs, null, tx);
        Uint64 dpId = IfmUtil.getDpnFromInterface(ifState);
        EVENT_LOGGER.debug("IFM-OvsInterfaceConfig,REMOVE {}", interfaceName);
        FlowBasedServicesUtils.removeIngressFlow(interfaceName, dpId, txRunner, futures);

        interfaceManagerCommonUtils.deleteParentInterfaceEntry(parentRefs.getParentInterface());

        // For Vlan-Trunk Interface, remove the trunk-member operstates as
        // well...

        InterfaceParentEntry interfaceParentEntry = interfaceMetaUtils
                .getInterfaceParentEntryFromConfigDS(interfaceName);
        if (interfaceParentEntry == null || interfaceParentEntry.getInterfaceChildEntry() == null) {
            return;
        }

        VlanMemberStateRemoveWorker vlanMemberStateRemoveWorker = new VlanMemberStateRemoveWorker(txRunner,
                interfaceManagerCommonUtils, dpId, interfaceName, interfaceParentEntry);
        coordinator.enqueueJob(interfaceName, vlanMemberStateRemoveWorker, IfmConstants.JOB_MAX_RETRIES);

    }

    private void removeTunnelConfiguration(ParentRefs parentRefs, String interfaceName, IfTunnel ifTunnel,
            TypedWriteTransaction<Operational> operTx, TypedReadWriteTransaction<Configuration> confTx)
            throws ExecutionException, InterruptedException, ReadFailedException {
        LOG.info("removing tunnel configuration for interface {}", interfaceName);
        Uint64 dpId = null;
        if (parentRefs != null) {
            dpId = parentRefs.getDatapathNodeIdentifier();
        }

        if (dpId == null) {
            return;
        }

        OvsdbBridgeRef ovsdbBridgeRef = interfaceMetaUtils.getOvsdbBridgeRef(dpId);
        BridgeEntryKey bridgeEntryKey = new BridgeEntryKey(dpId);
        InstanceIdentifier<BridgeEntry> bridgeEntryIid = InterfaceMetaUtils.getBridgeEntryIdentifier(bridgeEntryKey);
        BridgeEntry bridgeEntry = interfaceMetaUtils.getBridgeEntryFromConfigDS(bridgeEntryIid);
        if (bridgeEntry == null) {
            LOG.debug("Bridge Entry not present for dpn: {}", dpId);
            return;
        }
        @Nullable Map<BridgeInterfaceEntryKey, BridgeInterfaceEntry> bridgeInterfaceEntries =
                bridgeEntry.getBridgeInterfaceEntry();
        if (bridgeInterfaceEntries == null) {
            LOG.debug("Bridge Interface Entries not present for dpn : {}", dpId);
            return;
        }
        String tunnelName = SouthboundUtils.isOfTunnel(ifTunnel) ? SouthboundUtils.generateOfTunnelName(dpId, ifTunnel)
                : interfaceName;
        boolean deleteTunnel = canDeleteTunnelPort(bridgeInterfaceEntries, ifTunnel);
        if (ovsdbBridgeRef != null && deleteTunnel) {
            southboundUtils.removeTerminationEndPoint(ovsdbBridgeRef.getValue(), tunnelName);
        }
        if (SouthboundUtils.isOfTunnel(ifTunnel)) {
            if (deleteTunnel) {
                interfaceManagerCommonUtils.deleteParentInterfaceEntry(tunnelName);
            } else {
                interfaceManagerCommonUtils.deleteInterfaceChildEntry(tunnelName, interfaceName);
            }
        }

        // delete tunnel ingress flow
        removeTunnelIngressFlow(confTx, interfaceName, ifTunnel, dpId);

        // delete bridge to tunnel interface mappings
        interfaceMetaUtils.deleteBridgeInterfaceEntry(bridgeEntryKey, bridgeInterfaceEntries, bridgeEntryIid,
                interfaceName);
        int lportTag = interfaceMetaUtils.removeLportTagInterfaceMap(operTx, interfaceName);
        cleanUpInterfaceWithUnknownState(interfaceName, parentRefs, ifTunnel, operTx);
        // stop LLDP monitoring for the tunnel interface
        alivenessMonitorUtils.stopLLDPMonitoring(ifTunnel, interfaceName);

        if (ifTunnel.getTunnelInterfaceType().isAssignableFrom(TunnelTypeVxlan.class)) {
            removeMultipleVxlanTunnelsConfiguration(interfaceName, parentRefs);

        } else if (ifTunnel.getTunnelInterfaceType().isAssignableFrom(TunnelTypeLogicalGroup.class)) {
            removeLogicalTunnelGroup(dpId, interfaceName, lportTag, operTx, confTx);
        }
    }

    private static boolean canDeleteTunnelPort(Map<BridgeInterfaceEntryKey, BridgeInterfaceEntry>
                                                       bridgeInterfaceEntries, IfTunnel ifTunnel) {
        return !SouthboundUtils.isOfTunnel(ifTunnel) || bridgeInterfaceEntries == null
                || bridgeInterfaceEntries.size() <= 1;
    }

    public void removeTunnelIngressFlow(TypedReadWriteTransaction<Configuration> confTx,
        String interfaceName, IfTunnel ifTunnel, Uint64 dpId) throws ExecutionException, InterruptedException {
        NodeConnectorId ncId = FlowBasedServicesUtils.getNodeConnectorIdFromInterface(interfaceName,
                interfaceManagerCommonUtils);
        if (ncId == null) {
            LOG.debug("Node Connector Id is null. Skipping remove tunnel ingress flow.");
        } else {
            interfaceManagerCommonUtils.removeTunnelIngressFlow(confTx, ifTunnel, dpId, interfaceName);

            IfmUtil.unbindService(txRunner, coordinator, interfaceName,
                    FlowBasedServicesUtils.buildDefaultServiceId(interfaceName));
        }
    }

    // if the node is shutdown, there will be stale interface state entries,
    // with unknown op-state, clear them.
    public org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
        .ietf.interfaces.rev140508.interfaces.state.Interface cleanUpInterfaceWithUnknownState(
            String interfaceName, ParentRefs parentRefs, IfTunnel ifTunnel,
            TypedWriteTransaction<Operational> transaction) throws ReadFailedException {
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508
            .interfaces.state.Interface ifState = interfaceManagerCommonUtils.getInterfaceState(interfaceName);
        if (ifState != null && ifState
                .getOperStatus() == org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus.Unknown
                || tombstonedNodeManager.isDpnTombstoned(parentRefs.getDatapathNodeIdentifier())) {
            String staleInterface = ifTunnel != null ? interfaceName : parentRefs.getParentInterface();
            LOG.debug("cleaning up parent-interface for {}, in the case oper-status is UNKNOWN or the DPN:{}"
                    + " is tombstoned", interfaceName, parentRefs.getDatapathNodeIdentifier());
            interfaceManagerCommonUtils.deleteInterfaceStateInformation(staleInterface, transaction);
        }
        return ifState;
    }

    private static class VlanMemberStateRemoveWorker implements Callable<List<? extends ListenableFuture<?>>> {
        private final ManagedNewTransactionRunner txRunner;
        private final InterfaceManagerCommonUtils interfaceManagerCommonUtils;
        private final Uint64 dpId;
        private final String interfaceName;
        private final InterfaceParentEntry interfaceParentEntry;

        VlanMemberStateRemoveWorker(ManagedNewTransactionRunner txRunner,
                InterfaceManagerCommonUtils interfaceManagerCommonUtils,
                Uint64 dpId, String interfaceName, InterfaceParentEntry interfaceParentEntry) {
            this.txRunner = txRunner;
            this.interfaceManagerCommonUtils = interfaceManagerCommonUtils;
            this.dpId = dpId;
            this.interfaceName = interfaceName;
            this.interfaceParentEntry = interfaceParentEntry;
        }

        @Override
        public List<? extends ListenableFuture<?>> call() {
            List<ListenableFuture<?>> futures = new ArrayList<>();
            futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(OPERATIONAL, tx -> {
                // FIXME: If the no. of child entries exceeds 100, perform txn
                // updates in batches of 100.
                for (InterfaceChildEntry interfaceChildEntry : interfaceParentEntry
                        .nonnullInterfaceChildEntry().values()) {
                    LOG.debug("removing interface state for vlan trunk member {}",
                            interfaceChildEntry.getChildInterface());
                    interfaceManagerCommonUtils.deleteInterfaceStateInformation(interfaceChildEntry.getChildInterface(),
                            tx);
                    FlowBasedServicesUtils.removeIngressFlow(interfaceChildEntry.getChildInterface(), dpId, txRunner,
                            futures);
                }
            }));
            return futures;
        }

        @Override
        public String toString() {
            return "VlanMemberStateRemoveWorker [dpId=" + dpId + ", interfaceName=" + interfaceName
                    + ", interfaceParentEntry=" + interfaceParentEntry + "]";
        }
    }

    private void removeLogicalTunnelSelectGroup(TypedReadWriteTransaction<Configuration> tx,
            Uint64 srcDpnId, String interfaceName, int lportTag) throws ExecutionException, InterruptedException {
        long groupId = IfmUtil.getLogicalTunnelSelectGroupId(lportTag);
        LOG.debug("MULTIPLE_VxLAN_TUNNELS: group id {} removed for {} srcDpnId {}",
                groupId, interfaceName, srcDpnId);
        mdsalApiManager.removeGroup(tx, srcDpnId, groupId);
    }

    private void removeLogicalTunnelGroup(Uint64 dpnId, String ifaceName, int lportTag,
            TypedWriteTransaction<Operational> operTx, TypedReadWriteTransaction<Configuration> confTx)
            throws ExecutionException, InterruptedException {
        LOG.debug("MULTIPLE_VxLAN_TUNNELS: unbind & delete Interface State for logic tunnel group {}", ifaceName);
        IfmUtil.unbindService(txRunner, coordinator, ifaceName,
                FlowBasedServicesUtils.buildDefaultServiceId(ifaceName));
        interfaceManagerCommonUtils.deleteInterfaceStateInformation(ifaceName, operTx);
        interfaceManagerCommonUtils.deleteParentInterfaceEntry(ifaceName);
        removeLogicalTunnelSelectGroup(confTx, dpnId, ifaceName, lportTag);
    }

    private void removeMultipleVxlanTunnelsConfiguration(String ifaceName, ParentRefs parentRef) {
        //Remove the individual tunnel from interface-child-info model of the tunnel group members
        String parentInterface = parentRef.getParentInterface();
        if (parentInterface == null) {
            return;
        }
        LOG.debug("MULTIPLE_VxLAN_TUNNELS: removeMultipleVxlanTunnelsConfiguration for {} in logical group {}",
                    ifaceName, parentInterface);
        interfaceManagerCommonUtils.deleteInterfaceChildEntry(parentInterface, ifaceName);
    }
}
