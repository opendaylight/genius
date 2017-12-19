/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.renderer.ovs.confighelpers;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import javax.annotation.CheckReturnValue;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge._interface.info.BridgeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge.ref.info.BridgeRefEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeLogicalGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsInterfaceConfigAddHelper {
    private static final Logger LOG = LoggerFactory.getLogger(OvsInterfaceConfigAddHelper.class);

    public static List<ListenableFuture<Void>> addConfiguration(DataBroker dataBroker,
            ManagedNewTransactionRunner txRunner, ParentRefs parentRefs,
            Interface interfaceNew, IdManagerService idManager, AlivenessMonitorService alivenessMonitorService,
            IMdsalApiManager mdsalApiManager) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        // TODO Disentangle the transactions
        futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(configTx -> {
            futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(operTx -> {
                IfTunnel ifTunnel = interfaceNew.getAugmentation(IfTunnel.class);
                if (ifTunnel != null) {
                    addTunnelConfiguration(dataBroker, txRunner, parentRefs, interfaceNew, idManager,
                            alivenessMonitorService, ifTunnel, mdsalApiManager, operTx, futures);
                } else {
                    addVlanConfiguration(interfaceNew, parentRefs, dataBroker, txRunner, idManager, configTx, operTx,
                            futures);
                }
            }));
        }));
        return futures;
    }

    private static void addVlanConfiguration(Interface interfaceNew, ParentRefs parentRefs, DataBroker dataBroker,
            ManagedNewTransactionRunner txRunner,
            IdManagerService idManager, WriteTransaction defaultConfigShardTransaction,
            WriteTransaction defaultOperShardTransaction, List<ListenableFuture<Void>> futures) {
        IfL2vlan ifL2vlan = interfaceNew.getAugmentation(IfL2vlan.class);
        if (ifL2vlan == null || IfL2vlan.L2vlanMode.Trunk != ifL2vlan.getL2vlanMode()
                && IfL2vlan.L2vlanMode.Transparent != ifL2vlan.getL2vlanMode()) {
            return;
        }
        if (!InterfaceManagerCommonUtils.createInterfaceChildEntryIfNotPresent(dataBroker,
                defaultConfigShardTransaction, parentRefs.getParentInterface(), interfaceNew.getName(),
                ifL2vlan.getL2vlanMode())) {
            return;
        }
        LOG.info("adding vlan configuration for interface {}", interfaceNew.getName());
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.interfaces.rev140508.interfaces.state.Interface ifState = InterfaceManagerCommonUtils
                .getInterfaceState(parentRefs.getParentInterface(), dataBroker);

        InterfaceManagerCommonUtils.addStateEntry(interfaceNew.getName(), dataBroker, txRunner,
                defaultOperShardTransaction, idManager, futures, ifState);

        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
        VlanMemberStateAddWorker vlanMemberStateAddWorker =
                new VlanMemberStateAddWorker(dataBroker, txRunner, idManager, interfaceNew.getName(), ifState);
        coordinator.enqueueJob(interfaceNew.getName(), vlanMemberStateAddWorker, IfmConstants.JOB_MAX_RETRIES);
    }

    private static void addTunnelConfiguration(DataBroker dataBroker, ManagedNewTransactionRunner txRunner,
                                               ParentRefs parentRefs,
                                               Interface interfaceNew, IdManagerService idManager,
                                               AlivenessMonitorService alivenessMonitorService,
                                               IfTunnel ifTunnel, IMdsalApiManager mdsalApiManager,
                                               WriteTransaction defaultOperShardTransaction,
                                               List<ListenableFuture<Void>> futures) {
        if (parentRefs == null) {
            LOG.warn(
                    "ParentRefs for interface: {} Not Found. "
                            + "Creation of Tunnel OF-Port not supported when dpid not provided.",
                    interfaceNew.getName());
            return;
        }

        BigInteger dpId = parentRefs.getDatapathNodeIdentifier();
        if (dpId == null) {
            LOG.warn("dpid for interface: {} Not Found. No DPID provided. " + "Creation of OF-Port not supported.",
                    interfaceNew.getName());
            return;
        }
        LOG.info("adding tunnel configuration for interface {}", interfaceNew.getName());

        if (ifTunnel.getTunnelInterfaceType().isAssignableFrom(TunnelTypeLogicalGroup.class)) {
            futures.add(addLogicalTunnelGroup(interfaceNew, idManager, mdsalApiManager, txRunner,
                                              defaultOperShardTransaction));
            return;
        }
        boolean createTunnelPort = true;
        String tunnelName = interfaceNew.getName();
        if (SouthboundUtils.isOfTunnel(ifTunnel)) {
            BridgeEntry bridgeEntry = InterfaceMetaUtils.getBridgeEntryFromConfigDS(dpId, dataBroker);
            createTunnelPort = bridgeEntry == null || bridgeEntry.getBridgeInterfaceEntry() == null ? true
                    : bridgeEntry.getBridgeInterfaceEntry().isEmpty();
            tunnelName = SouthboundUtils.generateOfTunnelName(dpId, ifTunnel);
            InterfaceManagerCommonUtils.createInterfaceChildEntry(tunnelName, interfaceNew.getName());

            if (InterfaceManagerCommonUtils.getInterfaceStateFromCache(tunnelName) != null) {
                InterfaceManagerCommonUtils.addStateEntry(interfaceNew.getName(), dataBroker, txRunner, idManager,
                        futures, InterfaceManagerCommonUtils.getInterfaceStateFromCache(tunnelName));
            }
        }
        String parentInterface = parentRefs.getParentInterface();
        if (ifTunnel.getTunnelInterfaceType().isAssignableFrom(TunnelTypeVxlan.class)
                && !Strings.isNullOrEmpty(parentInterface)) {
            LOG.debug("MULTIPLE_VxLAN_TUNNELS: createInterfaceChildEntry for {} in logical group {}",
                    tunnelName, parentInterface);
            InterfaceManagerCommonUtils.createInterfaceChildEntry(parentInterface, tunnelName,
                    defaultOperShardTransaction);
        }
        LOG.debug("creating bridge interfaceEntry in ConfigDS {}", dpId);
        InterfaceMetaUtils.createBridgeInterfaceEntryInConfigDS(dpId, interfaceNew.getName());

        // create bridge on switch, if switch is connected
        BridgeRefEntry bridgeRefEntry = InterfaceMetaUtils.getBridgeRefEntryFromOperDS(dpId, dataBroker);
        if (bridgeRefEntry != null && bridgeRefEntry.getBridgeReference() != null) {
            LOG.debug("creating bridge interface on dpn {}", dpId);
            InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIid =
                    (InstanceIdentifier<OvsdbBridgeAugmentation>) bridgeRefEntry
                    .getBridgeReference().getValue();
            if (createTunnelPort) {
                SouthboundUtils.addPortToBridge(bridgeIid, interfaceNew, tunnelName, dataBroker, futures);
            }

            // if TEP is already configured on switch, start LLDP monitoring and
            // program tunnel ingress flow
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.interfaces.rev140508.interfaces.state.Interface ifState = InterfaceManagerCommonUtils
                    .getInterfaceState(interfaceNew.getName(), dataBroker);
            if (ifState != null) {
                NodeConnectorId ncId = IfmUtil.getNodeConnectorIdFromInterface(ifState);
                if (ncId != null) {
                    long portNo = IfmUtil.getPortNumberFromNodeConnectorId(ncId);
                    InterfaceManagerCommonUtils.makeTunnelIngressFlow(futures, mdsalApiManager, ifTunnel, dpId, portNo,
                            interfaceNew.getName(), ifState.getIfIndex(), NwConstants.ADD_FLOW);
                    ListenableFuture<Void> future =
                            FlowBasedServicesUtils.bindDefaultEgressDispatcherService(txRunner, interfaceNew,
                                    Long.toString(portNo), interfaceNew.getName(), ifState.getIfIndex());
                    // start LLDP monitoring for the tunnel interface
                    future.addListener(
                        () -> AlivenessMonitorUtils.startLLDPMonitoring(alivenessMonitorService, dataBroker,
                                ifTunnel, interfaceNew.getName()), MoreExecutors.directExecutor());
                    futures.add(future);
                }
            }
        }
    }

    private static class VlanMemberStateAddWorker implements Callable<List<ListenableFuture<Void>>> {
        private final DataBroker dataBroker;
        private final ManagedNewTransactionRunner txRunner;
        private final IdManagerService idManager;
        private final String interfaceName;
        private final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.interfaces.rev140508.interfaces.state.Interface ifState;

        VlanMemberStateAddWorker(DataBroker dataBroker, ManagedNewTransactionRunner txRunner,
                IdManagerService idManager, String interfaceName,
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.interfaces.rev140508.interfaces.state.Interface ifState) {
            this.dataBroker = dataBroker;
            this.txRunner = txRunner;
            this.idManager = idManager;
            this.interfaceName = interfaceName;
            this.ifState = ifState;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            InterfaceParentEntryKey interfaceParentEntryKey = new InterfaceParentEntryKey(interfaceName);
            InterfaceParentEntry interfaceParentEntry = InterfaceMetaUtils
                    .getInterfaceParentEntryFromConfigDS(interfaceParentEntryKey, dataBroker);
            if (interfaceParentEntry == null || interfaceParentEntry.getInterfaceChildEntry() == null) {
                return null;
            }

            List<ListenableFuture<Void>> futures = new ArrayList<>();
            // FIXME: If the no. of child entries exceeds 100, perform txn
            // updates in batches of 100.
            for (InterfaceChildEntry interfaceChildEntry : interfaceParentEntry.getInterfaceChildEntry()) {
                LOG.debug("adding interface state for vlan trunk member {}", interfaceChildEntry.getChildInterface());
                InterfaceManagerCommonUtils.addStateEntry(interfaceChildEntry.getChildInterface(), dataBroker,
                        txRunner, idManager, futures, ifState);
            }
            return futures;
        }

        @Override
        public String toString() {
            return "VlanMemberStateAddWorker [interfaceName=" + interfaceName + ", ifState=" + ifState + "]";
        }
    }

    private static long createLogicalTunnelSelectGroup(BigInteger srcDpnId, String interfaceName,
                                                int lportTag, IMdsalApiManager mdsalManager) {
        long groupId = IfmUtil.getLogicalTunnelSelectGroupId(lportTag);
        Group group = MDSALUtil.buildGroup(groupId, interfaceName, GroupTypes.GroupSelect,
                                           MDSALUtil.buildBucketLists(Collections.emptyList()));
        LOG.debug("MULTIPLE_VxLAN_TUNNELS: group id {} installed for {} srcDpnId {}",
                group.getGroupId().getValue(), interfaceName, srcDpnId);
        mdsalManager.syncInstallGroup(srcDpnId, group, IfmConstants.DELAY_TIME_IN_MILLISECOND);
        return groupId;
    }

    @CheckReturnValue
    private static ListenableFuture<Void> addLogicalTunnelGroup(Interface itfNew, IdManagerService idManager,
            IMdsalApiManager mdsalApiManager, ManagedNewTransactionRunner txRunner,
            WriteTransaction tx) {
        String ifaceName = itfNew.getName();
        LOG.debug("MULTIPLE_VxLAN_TUNNELS: adding Interface State for logic tunnel group {}", ifaceName);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state
            .Interface ifState = InterfaceManagerCommonUtils.addStateEntry(itfNew, ifaceName, tx,
                    idManager, null /*physAddress*/,
                    org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
                    .interfaces.rev140508.interfaces.state.Interface.OperStatus.Up,
                    org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
                    .interfaces.rev140508.interfaces.state.Interface.AdminStatus.Up,
                    null /*nodeConnectorId*/);
        long groupId = createLogicalTunnelSelectGroup(IfmUtil.getDpnFromInterface(ifState),
                                                      itfNew.getName(), ifState.getIfIndex(), mdsalApiManager);
        return FlowBasedServicesUtils.bindDefaultEgressDispatcherService(txRunner, itfNew, ifaceName,
                                                                         ifState.getIfIndex(), groupId);
    }

}
