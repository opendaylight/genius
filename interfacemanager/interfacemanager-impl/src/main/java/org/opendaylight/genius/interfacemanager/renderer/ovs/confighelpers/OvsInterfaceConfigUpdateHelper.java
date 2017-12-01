/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.renderer.ovs.confighelpers;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.commons.AlivenessMonitorUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.genius.interfacemanager.renderer.ovs.utilities.SouthboundUtils;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.AlivenessMonitorService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge.ref.info.BridgeRefEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsInterfaceConfigUpdateHelper {
    private static final Logger LOG = LoggerFactory.getLogger(OvsInterfaceConfigUpdateHelper.class);

    public static List<ListenableFuture<Void>> updateConfiguration(DataBroker dataBroker,
            ManagedNewTransactionRunner txRunner,
            AlivenessMonitorService alivenessMonitorService, IdManagerService idManager,
            IMdsalApiManager mdsalApiManager, Interface interfaceNew, Interface interfaceOld) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        // If any of the port attributes are modified, treat it as a delete and
        // recreate scenario
        if (portAttributesModified(interfaceOld, interfaceNew)) {
            LOG.info("port attributes modified, requires a delete and recreate of {} configuration", interfaceNew
                    .getName());
            futures.addAll(
                    OvsInterfaceConfigRemoveHelper.removeConfiguration(dataBroker, txRunner, alivenessMonitorService,
                    interfaceOld, idManager, mdsalApiManager, interfaceOld.getAugmentation(ParentRefs.class)));
            futures.addAll(OvsInterfaceConfigAddHelper.addConfiguration(dataBroker, txRunner,
                    interfaceNew.getAugmentation(ParentRefs.class), interfaceNew, idManager, alivenessMonitorService,
                    mdsalApiManager));
            return futures;
        }

        // If there is no operational state entry for the interface, treat it as
        // create
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.interfaces.rev140508.interfaces.state.Interface ifState = InterfaceManagerCommonUtils
                .getInterfaceState(interfaceNew.getName(), dataBroker);
        if (ifState == null) {
            futures.addAll(OvsInterfaceConfigAddHelper.addConfiguration(dataBroker, txRunner,
                    interfaceNew.getAugmentation(ParentRefs.class), interfaceNew, idManager, alivenessMonitorService,
                    mdsalApiManager));
            return futures;
        }

        futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx -> {
            if (tunnelMonitoringAttributesModified(interfaceOld, interfaceNew)) {
                handleTunnelMonitorUpdates(tx, alivenessMonitorService, interfaceNew, interfaceOld, dataBroker);
            }

            if (interfaceNew.isEnabled() != interfaceOld.isEnabled()) {
                handleInterfaceAdminStateUpdates(tx, interfaceNew, dataBroker, txRunner, ifState);
            }
        }));

        return futures;
    }

    private static boolean portAttributesModified(Interface interfaceOld, Interface interfaceNew) {
        ParentRefs parentRefsOld = interfaceOld.getAugmentation(ParentRefs.class);
        ParentRefs parentRefsNew = interfaceNew.getAugmentation(ParentRefs.class);
        if (checkAugmentations(parentRefsOld, parentRefsNew)) {
            return true;
        }

        IfL2vlan ifL2vlanOld = interfaceOld.getAugmentation(IfL2vlan.class);
        IfL2vlan ifL2vlanNew = interfaceNew.getAugmentation(IfL2vlan.class);
        if (checkAugmentations(ifL2vlanOld, ifL2vlanNew)) {
            return true;
        }

        IfTunnel ifTunnelOld = interfaceOld.getAugmentation(IfTunnel.class);
        IfTunnel ifTunnelNew = interfaceNew.getAugmentation(IfTunnel.class);
        if (checkAugmentations(ifTunnelOld, ifTunnelNew)) {
            if (!ifTunnelNew.getTunnelDestination().equals(ifTunnelOld.getTunnelDestination())
                    || !ifTunnelNew.getTunnelSource().equals(ifTunnelOld.getTunnelSource())
                    || ifTunnelNew.getTunnelGateway() != null && ifTunnelOld.getTunnelGateway() != null
                            && !ifTunnelNew.getTunnelGateway().equals(ifTunnelOld.getTunnelGateway())) {
                return true;
            }
        }

        return false;
    }

    private static boolean tunnelMonitoringAttributesModified(Interface interfaceOld, Interface interfaceNew) {
        IfTunnel ifTunnelOld = interfaceOld.getAugmentation(IfTunnel.class);
        IfTunnel ifTunnelNew = interfaceNew.getAugmentation(IfTunnel.class);
        return checkAugmentations(ifTunnelOld, ifTunnelNew);
    }

    /*
     * if the tunnel monitoring attributes have changed, handle it based on the
     * tunnel type. As of now internal vxlan tunnels use LLDP monitoring and
     * external tunnels use BFD monitoring.
     */
    private static void handleTunnelMonitorUpdates(WriteTransaction transaction,
            AlivenessMonitorService alivenessMonitorService, Interface interfaceNew, Interface interfaceOld,
            DataBroker dataBroker) {
        LOG.debug("tunnel monitoring attributes modified for interface {}", interfaceNew.getName());
        // update termination point on switch, if switch is connected
        BridgeRefEntry bridgeRefEntry = InterfaceMetaUtils.getBridgeReferenceForInterface(interfaceNew, dataBroker);
        IfTunnel ifTunnel = interfaceNew.getAugmentation(IfTunnel.class);
        if (SouthboundUtils.isMonitorProtocolBfd(ifTunnel)
                && InterfaceMetaUtils.bridgeExists(bridgeRefEntry, dataBroker)) {
            SouthboundUtils.updateBfdParamtersForTerminationPoint(bridgeRefEntry.getBridgeReference().getValue(),
                    interfaceNew.getAugmentation(IfTunnel.class), interfaceNew.getName(), transaction);
        } else {
            // update lldp tunnel monitoring attributes for an internal vxlan
            // tunnel interface
            AlivenessMonitorUtils.handleTunnelMonitorUpdates(alivenessMonitorService, dataBroker, interfaceOld,
                    interfaceNew);
        }
    }

    private static void handleInterfaceAdminStateUpdates(WriteTransaction transaction, Interface interfaceNew,
            DataBroker dataBroker, ManagedNewTransactionRunner txRunner,
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                    .ietf.interfaces.rev140508.interfaces.state.Interface ifState) {
        IfL2vlan ifL2vlan = interfaceNew.getAugmentation(IfL2vlan.class);
        if (ifL2vlan == null || IfL2vlan.L2vlanMode.Trunk != ifL2vlan.getL2vlanMode()
                && IfL2vlan.L2vlanMode.Transparent != ifL2vlan.getL2vlanMode()) {
            return;
        }

        OperStatus operStatus = InterfaceManagerCommonUtils.updateStateEntry(interfaceNew, dataBroker, transaction,
            ifState);
        InterfaceParentEntryKey interfaceParentEntryKey = new InterfaceParentEntryKey(interfaceNew.getName());
        InterfaceParentEntry interfaceParentEntry = InterfaceMetaUtils
                .getInterfaceParentEntryFromConfigDS(interfaceParentEntryKey, dataBroker);
        if (interfaceParentEntry == null || interfaceParentEntry.getInterfaceChildEntry() == null) {
            return;
        }

        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
        VlanMemberStateUpdateWorker vlanMemberStateUpdateWorker = new VlanMemberStateUpdateWorker(txRunner,
                operStatus, interfaceParentEntry.getInterfaceChildEntry());
        coordinator.enqueueJob(interfaceNew.getName(), vlanMemberStateUpdateWorker, IfmConstants.JOB_MAX_RETRIES);
    }

    private static <T> boolean checkAugmentations(T oldAug, T newAug) {
        if (oldAug != null && newAug == null || oldAug == null && newAug != null) {
            return true;
        }

        return newAug != null && !newAug.equals(oldAug);
    }

    private static class VlanMemberStateUpdateWorker implements Callable<List<ListenableFuture<Void>>> {

        private final ManagedNewTransactionRunner txRunner;
        private final OperStatus operStatus;
        private final List<InterfaceChildEntry> interfaceChildEntries;

        VlanMemberStateUpdateWorker(ManagedNewTransactionRunner txRunner, OperStatus operStatus,
                List<InterfaceChildEntry> interfaceChildEntries) {
            this.txRunner = txRunner;
            this.operStatus = operStatus;
            this.interfaceChildEntries = interfaceChildEntries;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            return Collections.singletonList(txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx -> {
                for (InterfaceChildEntry interfaceChildEntry : interfaceChildEntries) {
                    InterfaceManagerCommonUtils.updateOperStatus(interfaceChildEntry.getChildInterface(), operStatus,
                            tx);
                }
            }));
        }

        @Override
        public String toString() {
            return "VlanMemberStateUpdateWorker [operStatus=" + operStatus + ", interfaceChildEntries="
                    + interfaceChildEntries + "]";
        }
    }
}
