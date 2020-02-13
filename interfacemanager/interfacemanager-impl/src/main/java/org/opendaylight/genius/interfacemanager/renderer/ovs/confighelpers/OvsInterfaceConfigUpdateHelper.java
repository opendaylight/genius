/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.renderer.ovs.confighelpers;

import static org.opendaylight.genius.infra.Datastore.CONFIGURATION;
import static org.opendaylight.genius.infra.Datastore.OPERATIONAL;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.aries.blueprint.annotation.service.Reference;
import org.opendaylight.genius.infra.Datastore.Configuration;
import org.opendaylight.genius.infra.Datastore.Operational;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.infra.TypedWriteTransaction;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.commons.AlivenessMonitorUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.genius.interfacemanager.renderer.ovs.utilities.SouthboundUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge.ref.info.BridgeRefEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class OvsInterfaceConfigUpdateHelper {
    private static final Logger LOG = LoggerFactory.getLogger(OvsInterfaceConfigUpdateHelper.class);
    private static final Logger EVENT_LOGGER = LoggerFactory.getLogger("GeniusEventLogger");

    private final ManagedNewTransactionRunner txRunner;
    private final JobCoordinator coordinator;
    private final InterfaceManagerCommonUtils interfaceManagerCommonUtils;
    private final AlivenessMonitorUtils alivenessMonitorUtils;
    private final OvsInterfaceConfigRemoveHelper ovsInterfaceConfigRemoveHelper;
    private final OvsInterfaceConfigAddHelper ovsInterfaceConfigAddHelper;
    private final InterfaceMetaUtils interfaceMetaUtils;

    @Inject
    public OvsInterfaceConfigUpdateHelper(@Reference DataBroker dataBroker,
                                          @Reference JobCoordinator coordinator,
                                          InterfaceManagerCommonUtils interfaceManagerCommonUtils,
                                          AlivenessMonitorUtils alivenessMonitorUtils,
                                          OvsInterfaceConfigRemoveHelper ovsInterfaceConfigRemoveHelper,
                                          OvsInterfaceConfigAddHelper ovsInterfaceConfigAddHelper,
                                          InterfaceMetaUtils interfaceMetaUtils) {
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.coordinator = coordinator;
        this.interfaceManagerCommonUtils = interfaceManagerCommonUtils;
        this.alivenessMonitorUtils = alivenessMonitorUtils;
        this.ovsInterfaceConfigRemoveHelper = ovsInterfaceConfigRemoveHelper;
        this.ovsInterfaceConfigAddHelper = ovsInterfaceConfigAddHelper;
        this.interfaceMetaUtils = interfaceMetaUtils;
    }

    public List<ListenableFuture<Void>> updateConfiguration(Interface interfaceNew, Interface interfaceOld) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        // If any of the port attributes are modified, treat it as a delete and
        // recreate scenario
        if (portAttributesModified(interfaceOld, interfaceNew)) {
            LOG.info("port attributes modified, requires a delete and recreate of {} configuration", interfaceNew
                    .getName());
            futures.addAll(ovsInterfaceConfigRemoveHelper.removeConfiguration(interfaceOld,
                    interfaceOld.augmentation(ParentRefs.class)));
            futures.addAll(ovsInterfaceConfigAddHelper.addConfiguration(interfaceNew.augmentation(ParentRefs.class),
                    interfaceNew));
            return futures;
        }

        // If there is no operational state entry for the interface, treat it as
        // create
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.interfaces.rev140508.interfaces.state.Interface ifState = interfaceManagerCommonUtils
                .getInterfaceState(interfaceNew.getName());
        if (ifState == null) {
            futures.addAll(ovsInterfaceConfigAddHelper.addConfiguration(interfaceNew.augmentation(ParentRefs.class),
                    interfaceNew));
            return futures;
        }

        if (tunnelMonitoringAttributesModified(interfaceOld, interfaceNew)) {
            futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION,
                tx -> handleTunnelMonitorUpdates(tx, interfaceNew, interfaceOld)));
        } else if (!Objects.equals(interfaceNew.isEnabled(), interfaceOld.isEnabled())) {
            EVENT_LOGGER.debug("IFM-OvsInterfaceConfig,UPDATE {}", interfaceNew.getName());
            futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(OPERATIONAL,
                tx -> handleInterfaceAdminStateUpdates(tx, interfaceNew, ifState)));
        }

        return futures;
    }

    private static boolean portAttributesModified(Interface interfaceOld, Interface interfaceNew) {
        ParentRefs parentRefsOld = interfaceOld.augmentation(ParentRefs.class);
        ParentRefs parentRefsNew = interfaceNew.augmentation(ParentRefs.class);
        if (checkAugmentations(parentRefsOld, parentRefsNew)) {
            return true;
        }

        IfL2vlan ifL2vlanOld = interfaceOld.augmentation(IfL2vlan.class);
        IfL2vlan ifL2vlanNew = interfaceNew.augmentation(IfL2vlan.class);
        if (checkAugmentations(ifL2vlanOld, ifL2vlanNew)) {
            return true;
        }

        IfTunnel ifTunnelOld = interfaceOld.augmentation(IfTunnel.class);
        IfTunnel ifTunnelNew = interfaceNew.augmentation(IfTunnel.class);
        if (checkAugmentations(ifTunnelOld, ifTunnelNew)) {
            if (!Objects.equals(ifTunnelNew.getTunnelDestination(), ifTunnelOld.getTunnelDestination())
                    || !Objects.equals(ifTunnelNew.getTunnelSource(), ifTunnelOld.getTunnelSource())
                    || ifTunnelNew.getTunnelGateway() != null && ifTunnelOld.getTunnelGateway() != null
                            && !ifTunnelNew.getTunnelGateway().equals(ifTunnelOld.getTunnelGateway())) {
                return true;
            }
        }

        return false;
    }

    private static boolean tunnelMonitoringAttributesModified(Interface interfaceOld, Interface interfaceNew) {
        IfTunnel ifTunnelOld = interfaceOld.augmentation(IfTunnel.class);
        IfTunnel ifTunnelNew = interfaceNew.augmentation(IfTunnel.class);
        return checkAugmentations(ifTunnelOld, ifTunnelNew);
    }

    /*
     * if the tunnel monitoring attributes have changed, handle it based on the
     * tunnel type. As of now internal vxlan tunnels use LLDP monitoring and
     * external tunnels use BFD monitoring.
     */
    private void handleTunnelMonitorUpdates(TypedWriteTransaction<Configuration> transaction,
            Interface interfaceNew, Interface interfaceOld) {
        LOG.debug("tunnel monitoring attributes modified for interface {}", interfaceNew.getName());
        // update termination point on switch, if switch is connected
        BridgeRefEntry bridgeRefEntry = interfaceMetaUtils.getBridgeReferenceForInterface(interfaceNew);
        IfTunnel ifTunnel = interfaceNew.augmentation(IfTunnel.class);
        if (SouthboundUtils.isMonitorProtocolBfd(ifTunnel)
                && interfaceMetaUtils.bridgeExists(bridgeRefEntry)) {
            SouthboundUtils.updateBfdParamtersForTerminationPoint(bridgeRefEntry.getBridgeReference().getValue(),
                    interfaceNew.augmentation(IfTunnel.class), interfaceNew.getName(), transaction);
        } else {
            // update lldp tunnel monitoring attributes for an internal vxlan
            // tunnel interface
            alivenessMonitorUtils.handleTunnelMonitorUpdates(interfaceOld, interfaceNew);
        }
    }

    private void handleInterfaceAdminStateUpdates(TypedWriteTransaction<Operational> transaction,
            Interface interfaceNew,
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.interfaces.rev140508.interfaces.state.Interface ifState) {
        IfL2vlan ifL2vlan = interfaceNew.augmentation(IfL2vlan.class);
        if (ifL2vlan == null || IfL2vlan.L2vlanMode.Trunk != ifL2vlan.getL2vlanMode()
                && IfL2vlan.L2vlanMode.Transparent != ifL2vlan.getL2vlanMode()) {
            return;
        }
        LOG.info("admin-state modified for interface {}", interfaceNew.getName());
        OperStatus operStatus = interfaceManagerCommonUtils.updateStateEntry(interfaceNew, transaction , ifState);
        InterfaceParentEntryKey interfaceParentEntryKey = new InterfaceParentEntryKey(interfaceNew.getName());
        InterfaceParentEntry interfaceParentEntry = interfaceMetaUtils
                .getInterfaceParentEntryFromConfigDS(interfaceParentEntryKey);
        if (interfaceParentEntry == null || interfaceParentEntry.getInterfaceChildEntry() == null) {
            return;
        }

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
            return Collections.singletonList(txRunner.callWithNewWriteOnlyTransactionAndSubmit(OPERATIONAL, tx -> {
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
