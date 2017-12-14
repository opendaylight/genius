/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.renderer.ovs.confighelpers;

import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.commons.AlivenessMonitorUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.genius.interfacemanager.renderer.ovs.utilities.SouthboundUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge._interface.info.BridgeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge._interface.info.BridgeEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge._interface.info.bridge.entry.BridgeInterfaceEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeLogicalGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class OvsInterfaceConfigRemoveHelper {
    private static final Logger LOG = LoggerFactory.getLogger(OvsInterfaceConfigRemoveHelper.class);

    private final DataBroker dataBroker;
    private final IMdsalApiManager mdsalApiManager;
    private final IdManagerService idManager;
    private final JobCoordinator coordinator;
    private final InterfaceManagerCommonUtils interfaceManagerCommonUtils;
    private final AlivenessMonitorUtils alivenessMonitorUtils;
    private final InterfaceMetaUtils interfaceMetaUtils;
    private final SouthboundUtils southboundUtils;

    @Inject
    public OvsInterfaceConfigRemoveHelper(DataBroker dataBroker, AlivenessMonitorUtils alivenessMonitorUtils,
            IMdsalApiManager mdsalApiManager, IdManagerService idManager, JobCoordinator coordinator,
            InterfaceManagerCommonUtils interfaceManagerCommonUtils, InterfaceMetaUtils interfaceMetaUtils,
            SouthboundUtils southboundUtils) {
        this.dataBroker = dataBroker;
        this.mdsalApiManager = mdsalApiManager;
        this.idManager = idManager;
        this.coordinator = coordinator;
        this.interfaceManagerCommonUtils = interfaceManagerCommonUtils;
        this.alivenessMonitorUtils = alivenessMonitorUtils;
        this.interfaceMetaUtils = interfaceMetaUtils;
        this.southboundUtils = southboundUtils;
    }

    public List<ListenableFuture<Void>> removeConfiguration(Interface interfaceOld, ParentRefs parentRefs) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        WriteTransaction defaultOperationalShardTransaction = dataBroker.newWriteOnlyTransaction();
        WriteTransaction defaultConfigShardTransaction = dataBroker.newWriteOnlyTransaction();

        IfTunnel ifTunnel = interfaceOld.getAugmentation(IfTunnel.class);
        if (ifTunnel != null) {
            removeTunnelConfiguration(parentRefs, interfaceOld.getName(), ifTunnel, defaultOperationalShardTransaction);
        } else {
            removeVlanConfiguration(parentRefs, interfaceOld.getName(),
                    interfaceOld.getAugmentation(IfL2vlan.class), defaultOperationalShardTransaction,
                    futures);
        }
        futures.add(defaultConfigShardTransaction.submit());
        futures.add(defaultOperationalShardTransaction.submit());
        return futures;
    }

    private void removeVlanConfiguration(ParentRefs parentRefs, String interfaceName,
            IfL2vlan ifL2vlan, WriteTransaction defaultOperationalShardTransaction,
            List<ListenableFuture<Void>> futures) {
        if (parentRefs == null || ifL2vlan == null || IfL2vlan.L2vlanMode.Trunk != ifL2vlan.getL2vlanMode()
                && IfL2vlan.L2vlanMode.Transparent != ifL2vlan.getL2vlanMode()) {
            return;
        }
        LOG.info("removing vlan configuration for interface {}", interfaceName);
        interfaceManagerCommonUtils.deleteInterfaceStateInformation(interfaceName, defaultOperationalShardTransaction);

        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.interfaces.rev140508.interfaces.state.Interface ifState = interfaceManagerCommonUtils
                .getInterfaceState(interfaceName);

        if (ifState == null) {
            LOG.debug("could not fetch interface state corresponding to {}, probably already removed as part of port "
                    + "removal event, proceeding with remaining config cleanups", interfaceName);
        }

        cleanUpInterfaceWithUnknownState(interfaceName, parentRefs, null,
                defaultOperationalShardTransaction);
        BigInteger dpId = IfmUtil.getDpnFromInterface(ifState);
        FlowBasedServicesUtils.removeIngressFlow(interfaceName, dpId, dataBroker, futures);

        interfaceManagerCommonUtils.deleteParentInterfaceEntry(parentRefs.getParentInterface());

        // For Vlan-Trunk Interface, remove the trunk-member operstates as
        // well...

        InterfaceParentEntry interfaceParentEntry = interfaceMetaUtils
                .getInterfaceParentEntryFromConfigDS(interfaceName);
        if (interfaceParentEntry == null || interfaceParentEntry.getInterfaceChildEntry() == null) {
            return;
        }

        VlanMemberStateRemoveWorker vlanMemberStateRemoveWorker = new VlanMemberStateRemoveWorker(dataBroker,
                interfaceManagerCommonUtils, dpId, interfaceName, interfaceParentEntry);
        coordinator.enqueueJob(interfaceName, vlanMemberStateRemoveWorker, IfmConstants.JOB_MAX_RETRIES);

    }

    private void removeTunnelConfiguration(ParentRefs parentRefs, String interfaceName, IfTunnel ifTunnel,
            WriteTransaction defaultOperationalShardTransaction) {
        LOG.info("removing tunnel configuration for interface {}", interfaceName);
        BigInteger dpId = null;
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
        List<BridgeInterfaceEntry> bridgeInterfaceEntries = bridgeEntry.getBridgeInterfaceEntry();
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
        removeTunnelIngressFlow(interfaceName, ifTunnel, dpId);

        // delete bridge to tunnel interface mappings
        interfaceMetaUtils.deleteBridgeInterfaceEntry(bridgeEntryKey, bridgeInterfaceEntries, bridgeEntryIid,
                interfaceName);
        int lportTag = interfaceMetaUtils.removeLportTagInterfaceMap(defaultOperationalShardTransaction, interfaceName);
        cleanUpInterfaceWithUnknownState(interfaceName, parentRefs, ifTunnel,
                defaultOperationalShardTransaction);
        // stop LLDP monitoring for the tunnel interface
        alivenessMonitorUtils.stopLLDPMonitoring(ifTunnel, interfaceName);

        if (ifTunnel.getTunnelInterfaceType().isAssignableFrom(TunnelTypeVxlan.class)) {
            removeMultipleVxlanTunnelsConfiguration(interfaceName, parentRefs);

        } else if (ifTunnel.getTunnelInterfaceType().isAssignableFrom(TunnelTypeLogicalGroup.class)) {
            removeLogicalTunnelGroup(dpId, interfaceName, lportTag, defaultOperationalShardTransaction);
        }
    }

    private static boolean canDeleteTunnelPort(List<BridgeInterfaceEntry> bridgeInterfaceEntries, IfTunnel ifTunnel) {
        return !SouthboundUtils.isOfTunnel(ifTunnel) || bridgeInterfaceEntries == null
                || bridgeInterfaceEntries.size() <= 1;
    }

    public void removeTunnelIngressFlow(String interfaceName, IfTunnel ifTunnel, BigInteger dpId) {
        NodeConnectorId ncId = FlowBasedServicesUtils.getNodeConnectorIdFromInterface(interfaceName,
                interfaceManagerCommonUtils);
        if (ncId == null) {
            LOG.debug("Node Connector Id is null. Skipping remove tunnel ingress flow.");
        } else {
            interfaceManagerCommonUtils.removeTunnelIngressFlow(ifTunnel, dpId, interfaceName);

            IfmUtil.unbindService(dataBroker, coordinator, interfaceName,
                    FlowBasedServicesUtils.buildDefaultServiceId(interfaceName));
        }
    }

    // if the node is shutdown, there will be stale interface state entries,
    // with unknown op-state, clear them.
    public org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
        .ietf.interfaces.rev140508.interfaces.state.Interface cleanUpInterfaceWithUnknownState(
            String interfaceName, ParentRefs parentRefs, IfTunnel ifTunnel, WriteTransaction transaction) {
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508
            .interfaces.state.Interface ifState = interfaceManagerCommonUtils.getInterfaceState(interfaceName);
        if (ifState != null && ifState
                .getOperStatus() == org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus.Unknown) {
            String staleInterface = ifTunnel != null ? interfaceName : parentRefs.getParentInterface();
            LOG.debug("cleaning up parent-interface for {}, since the oper-status is UNKNOWN", interfaceName);
            interfaceManagerCommonUtils.deleteInterfaceStateInformation(staleInterface, transaction);
        }
        return ifState;
    }

    private static class VlanMemberStateRemoveWorker implements Callable<List<ListenableFuture<Void>>> {
        private final DataBroker dataBroker;
        private final InterfaceManagerCommonUtils interfaceManagerCommonUtils;
        private final BigInteger dpId;
        private final String interfaceName;
        private final InterfaceParentEntry interfaceParentEntry;

        VlanMemberStateRemoveWorker(DataBroker dataBroker, InterfaceManagerCommonUtils interfaceManagerCommonUtils,
                BigInteger dpId, String interfaceName, InterfaceParentEntry interfaceParentEntry) {
            this.dataBroker = dataBroker;
            this.interfaceManagerCommonUtils = interfaceManagerCommonUtils;
            this.dpId = dpId;
            this.interfaceName = interfaceName;
            this.interfaceParentEntry = interfaceParentEntry;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            List<ListenableFuture<Void>> futures = new ArrayList<>();
            WriteTransaction operShardTransaction = dataBroker.newWriteOnlyTransaction();
            // FIXME: If the no. of child entries exceeds 100, perform txn
            // updates in batches of 100.
            for (InterfaceChildEntry interfaceChildEntry : interfaceParentEntry.getInterfaceChildEntry()) {
                LOG.debug("removing interface state for vlan trunk member {}", interfaceChildEntry.getChildInterface());
                interfaceManagerCommonUtils.deleteInterfaceStateInformation(interfaceChildEntry.getChildInterface(),
                        operShardTransaction);
                FlowBasedServicesUtils.removeIngressFlow(interfaceChildEntry.getChildInterface(), dpId, dataBroker,
                        futures);
            }
            futures.add(operShardTransaction.submit());
            return futures;
        }

        @Override
        public String toString() {
            return "VlanMemberStateRemoveWorker [dpId=" + dpId + ", interfaceName=" + interfaceName
                    + ", interfaceParentEntry=" + interfaceParentEntry + "]";
        }
    }

    private void removeLogicalTunnelSelectGroup(BigInteger srcDpnId, String interfaceName, int lportTag) {
        long groupId = IfmUtil.getLogicalTunnelSelectGroupId(lportTag);
        Group group = MDSALUtil.buildGroup(groupId, interfaceName, GroupTypes.GroupSelect,
                                           MDSALUtil.buildBucketLists(Collections.emptyList()));
        LOG.debug("MULTIPLE_VxLAN_TUNNELS: group id {} removed for {} srcDpnId {}",
                group.getGroupId().getValue(), interfaceName, srcDpnId);
        mdsalApiManager.syncRemoveGroup(srcDpnId, group);
    }

    private void removeLogicalTunnelGroup(BigInteger dpnId, String ifaceName, int lportTag, WriteTransaction tx) {
        LOG.debug("MULTIPLE_VxLAN_TUNNELS: unbind & delete Interface State for logic tunnel group {}", ifaceName);
        IfmUtil.unbindService(dataBroker, coordinator, ifaceName,
                FlowBasedServicesUtils.buildDefaultServiceId(ifaceName));
        interfaceManagerCommonUtils.deleteInterfaceStateInformation(ifaceName, tx);
        interfaceManagerCommonUtils.deleteParentInterfaceEntry(ifaceName);
        removeLogicalTunnelSelectGroup(dpnId, ifaceName, lportTag);
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
