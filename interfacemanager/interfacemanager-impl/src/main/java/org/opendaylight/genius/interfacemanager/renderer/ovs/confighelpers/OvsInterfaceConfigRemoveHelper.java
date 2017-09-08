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
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.commons.AlivenessMonitorUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.genius.interfacemanager.renderer.ovs.utilities.SouthboundUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.AlivenessMonitorService;
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

public class OvsInterfaceConfigRemoveHelper {
    private static final Logger LOG = LoggerFactory.getLogger(OvsInterfaceConfigRemoveHelper.class);

    public static List<ListenableFuture<Void>> removeConfiguration(DataBroker dataBroker,
            AlivenessMonitorService alivenessMonitorService, Interface interfaceOld, IdManagerService idManager,
            IMdsalApiManager mdsalApiManager, ParentRefs parentRefs) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        WriteTransaction defaultOperationalShardTransaction = dataBroker.newWriteOnlyTransaction();
        WriteTransaction defaultConfigShardTransaction = dataBroker.newWriteOnlyTransaction();

        IfTunnel ifTunnel = interfaceOld.getAugmentation(IfTunnel.class);
        if (ifTunnel != null) {
            removeTunnelConfiguration(alivenessMonitorService, parentRefs, dataBroker, interfaceOld.getName(), ifTunnel,
                    idManager, mdsalApiManager, defaultOperationalShardTransaction
            );
        } else {
            removeVlanConfiguration(dataBroker, parentRefs, interfaceOld.getName(),
                    interfaceOld.getAugmentation(IfL2vlan.class), defaultOperationalShardTransaction,
                    futures, idManager);
        }
        futures.add(defaultConfigShardTransaction.submit());
        futures.add(defaultOperationalShardTransaction.submit());
        return futures;
    }

    private static void removeVlanConfiguration(DataBroker dataBroker, ParentRefs parentRefs, String interfaceName,
            IfL2vlan ifL2vlan, WriteTransaction defaultOperationalShardTransaction,
            List<ListenableFuture<Void>> futures, IdManagerService idManagerService) {
        if (parentRefs == null || ifL2vlan == null || IfL2vlan.L2vlanMode.Trunk != ifL2vlan.getL2vlanMode()
                && IfL2vlan.L2vlanMode.Transparent != ifL2vlan.getL2vlanMode()) {
            return;
        }
        LOG.info("removing vlan configuration for interface {}", interfaceName);
        InterfaceManagerCommonUtils.deleteInterfaceStateInformation(interfaceName, defaultOperationalShardTransaction,
                idManagerService);

        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.interfaces.rev140508.interfaces.state.Interface ifState = InterfaceManagerCommonUtils
                .getInterfaceState(interfaceName, dataBroker);

        if (ifState == null) {
            LOG.debug("could not fetch interface state corresponding to {}, probably already removed as part of port "
                    + "removal event, proceeding with remaining config cleanups", interfaceName);
        }

        cleanUpInterfaceWithUnknownState(interfaceName, parentRefs, null, dataBroker,
                defaultOperationalShardTransaction, idManagerService);
        BigInteger dpId = IfmUtil.getDpnFromInterface(ifState);
        FlowBasedServicesUtils.removeIngressFlow(interfaceName, dpId, dataBroker, futures);

        InterfaceManagerCommonUtils.deleteParentInterfaceEntry(parentRefs.getParentInterface());

        // For Vlan-Trunk Interface, remove the trunk-member operstates as
        // well...

        InterfaceParentEntry interfaceParentEntry = InterfaceMetaUtils
                .getInterfaceParentEntryFromConfigDS(interfaceName, dataBroker);
        if (interfaceParentEntry == null || interfaceParentEntry.getInterfaceChildEntry() == null) {
            return;
        }

        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
        VlanMemberStateRemoveWorker vlanMemberStateRemoveWorker = new VlanMemberStateRemoveWorker(dataBroker,
                idManagerService, dpId, interfaceName, interfaceParentEntry);
        coordinator.enqueueJob(interfaceName, vlanMemberStateRemoveWorker, IfmConstants.JOB_MAX_RETRIES);

    }

    private static void removeTunnelConfiguration(AlivenessMonitorService alivenessMonitorService,
            ParentRefs parentRefs, DataBroker dataBroker, String interfaceName, IfTunnel ifTunnel,
            IdManagerService idManager, IMdsalApiManager mdsalApiManager,
            WriteTransaction defaultOperationalShardTransaction) {
        LOG.info("removing tunnel configuration for interface {}", interfaceName);
        BigInteger dpId = null;
        if (parentRefs != null) {
            dpId = parentRefs.getDatapathNodeIdentifier();
        }

        if (dpId == null) {
            return;
        }

        OvsdbBridgeRef ovsdbBridgeRef = InterfaceMetaUtils.getOvsdbBridgeRef(dpId, dataBroker);
        BridgeEntryKey bridgeEntryKey = new BridgeEntryKey(dpId);
        InstanceIdentifier<BridgeEntry> bridgeEntryIid = InterfaceMetaUtils.getBridgeEntryIdentifier(bridgeEntryKey);
        BridgeEntry bridgeEntry = InterfaceMetaUtils.getBridgeEntryFromConfigDS(bridgeEntryIid, dataBroker);
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
            SouthboundUtils.removeTerminationEndPoint(ovsdbBridgeRef.getValue(), tunnelName);
        }
        if (SouthboundUtils.isOfTunnel(ifTunnel)) {
            if (deleteTunnel) {
                InterfaceManagerCommonUtils.deleteParentInterfaceEntry(tunnelName);
            } else {
                InterfaceManagerCommonUtils.deleteInterfaceChildEntry(tunnelName, interfaceName);
            }
        }

        // delete tunnel ingress flow
        removeTunnelIngressFlow(dataBroker, interfaceName, ifTunnel, mdsalApiManager, dpId);

        // delete bridge to tunnel interface mappings
        InterfaceMetaUtils.deleteBridgeInterfaceEntry(bridgeEntryKey, bridgeInterfaceEntries, bridgeEntryIid,
                interfaceName);
        int lportTag = InterfaceMetaUtils.removeLportTagInterfaceMap(idManager, defaultOperationalShardTransaction,
                                                                     interfaceName);
        cleanUpInterfaceWithUnknownState(interfaceName, parentRefs, ifTunnel, dataBroker,
                defaultOperationalShardTransaction, idManager);
        // stop LLDP monitoring for the tunnel interface
        AlivenessMonitorUtils.stopLLDPMonitoring(alivenessMonitorService, dataBroker, ifTunnel, interfaceName);

        if (ifTunnel.getTunnelInterfaceType().isAssignableFrom(TunnelTypeVxlan.class)) {
            removeMultipleVxlanTunnelsConfiguration(interfaceName, parentRefs);

        } else if (ifTunnel.getTunnelInterfaceType().isAssignableFrom(TunnelTypeLogicalGroup.class)) {
            removeLogicalTunnelGroup(dpId, interfaceName, lportTag, idManager, mdsalApiManager, dataBroker,
                                     defaultOperationalShardTransaction);
        }
    }

    private static boolean canDeleteTunnelPort(List<BridgeInterfaceEntry> bridgeInterfaceEntries, IfTunnel ifTunnel) {
        return !SouthboundUtils.isOfTunnel(ifTunnel) || bridgeInterfaceEntries == null
                || bridgeInterfaceEntries.size() <= 1;
    }

    public static void removeTunnelIngressFlow(DataBroker dataBroker,
            String interfaceName, IfTunnel ifTunnel, IMdsalApiManager mdsalApiManager, BigInteger dpId) {
        NodeConnectorId ncId = IfmUtil.getNodeConnectorIdFromInterface(interfaceName, dataBroker);
        if (ncId == null) {
            LOG.debug("Node Connector Id is null. Skipping remove tunnel ingress flow.");
        } else {
            long portNo = IfmUtil.getPortNumberFromNodeConnectorId(ncId);
            InterfaceManagerCommonUtils.makeTunnelIngressFlow(mdsalApiManager, ifTunnel, dpId, portNo,
                    interfaceName, -1, NwConstants.DEL_FLOW);
            FlowBasedServicesUtils.unbindDefaultEgressDispatcherService(dataBroker, interfaceName);
        }
    }

    // if the node is shutdown, there will be stale interface state entries,
    // with unknown op-state, clear them.
    public static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
        .ietf.interfaces.rev140508.interfaces.state.Interface cleanUpInterfaceWithUnknownState(
            String interfaceName, ParentRefs parentRefs, IfTunnel ifTunnel, DataBroker dataBroker,
            WriteTransaction transaction, IdManagerService idManagerService) {
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508
            .interfaces.state.Interface ifState = InterfaceManagerCommonUtils
                .getInterfaceState(interfaceName, dataBroker);
        if (ifState != null && ifState
                .getOperStatus() == org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus.Unknown) {
            String staleInterface = ifTunnel != null ? interfaceName : parentRefs.getParentInterface();
            LOG.debug("cleaning up parent-interface for {}, since the oper-status is UNKNOWN", interfaceName);
            InterfaceManagerCommonUtils.deleteInterfaceStateInformation(staleInterface, transaction, idManagerService);
        }
        return ifState;
    }

    private static class VlanMemberStateRemoveWorker implements Callable<List<ListenableFuture<Void>>> {
        private final DataBroker dataBroker;
        private final IdManagerService idManager;
        private final BigInteger dpId;
        private final String interfaceName;
        private final InterfaceParentEntry interfaceParentEntry;

        VlanMemberStateRemoveWorker(DataBroker dataBroker, IdManagerService idManager, BigInteger dpId,
                String interfaceName, InterfaceParentEntry interfaceParentEntry) {
            super();
            this.dataBroker = dataBroker;
            this.idManager = idManager;
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
                InterfaceManagerCommonUtils.deleteInterfaceStateInformation(interfaceChildEntry.getChildInterface(),
                        operShardTransaction, idManager);
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

    private static void removeLogicalTunnelSelectGroup(BigInteger srcDpnId, String interfaceName, int lportTag,
                                                       IMdsalApiManager mdsalManager) {
        long groupId = IfmUtil.getLogicalTunnelSelectGroupId(lportTag);
        Group group = MDSALUtil.buildGroup(groupId, interfaceName, GroupTypes.GroupSelect,
                                           MDSALUtil.buildBucketLists(Collections.emptyList()));
        LOG.debug("MULTIPLE_VxLAN_TUNNELS: group id {} removed for {} srcDpnId {}",
                group.getGroupId().getValue(), interfaceName, srcDpnId);
        mdsalManager.syncRemoveGroup(srcDpnId, group);
    }

    private static void removeLogicalTunnelGroup(BigInteger dpnId, String ifaceName, int lportTag,
            IdManagerService idManager, IMdsalApiManager mdsalApiManager, DataBroker dataBroker, WriteTransaction tx) {
        LOG.debug("MULTIPLE_VxLAN_TUNNELS: unbind & delete Interface State for logic tunnel group {}", ifaceName);
        FlowBasedServicesUtils.unbindDefaultEgressDispatcherService(dataBroker, ifaceName);
        InterfaceManagerCommonUtils.deleteInterfaceStateInformation(ifaceName, tx, idManager);
        InterfaceManagerCommonUtils.deleteParentInterfaceEntry(ifaceName);
        removeLogicalTunnelSelectGroup(dpnId, ifaceName, lportTag, mdsalApiManager);
    }

    private static void removeMultipleVxlanTunnelsConfiguration(String ifaceName, ParentRefs parentRef) {
        //Remove the individual tunnel from interface-child-info model of the tunnel group members
        String parentInterface = parentRef.getParentInterface();
        if (parentInterface == null) {
            return;
        }
        LOG.debug("MULTIPLE_VxLAN_TUNNELS: removeMultipleVxlanTunnelsConfiguration for {} in logical group {}",
                    ifaceName, parentInterface);
        InterfaceManagerCommonUtils.deleteInterfaceChildEntry(parentInterface, ifaceName);
    }
}
