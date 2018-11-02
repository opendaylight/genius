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

import com.google.common.base.Strings;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.infra.Datastore.Configuration;
import org.opendaylight.genius.infra.Datastore.Operational;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.infra.TypedWriteTransaction;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.commons.AlivenessMonitorUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.genius.interfacemanager.renderer.ovs.statehelpers.OvsInterfaceStateAddHelper;
import org.opendaylight.genius.interfacemanager.renderer.ovs.utilities.SouthboundUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
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

@Singleton
public final class OvsInterfaceConfigAddHelper {
    private static final Logger LOG = LoggerFactory.getLogger(OvsInterfaceConfigAddHelper.class);

    private final ManagedNewTransactionRunner txRunner;
    private final IMdsalApiManager mdsalApiManager;
    private final JobCoordinator coordinator;
    private final AlivenessMonitorUtils alivenessMonitorUtils;
    private final InterfaceManagerCommonUtils interfaceManagerCommonUtils;
    private final OvsInterfaceStateAddHelper ovsInterfaceStateAddHelper;
    private final InterfaceMetaUtils interfaceMetaUtils;
    private final SouthboundUtils southboundUtils;

    @Inject
    public OvsInterfaceConfigAddHelper(DataBroker dataBroker, AlivenessMonitorUtils alivenessMonitorUtils,
            IMdsalApiManager mdsalApiManager, JobCoordinator coordinator,
            InterfaceManagerCommonUtils interfaceManagerCommonUtils,
            OvsInterfaceStateAddHelper ovsInterfaceStateAddHelper,
            InterfaceMetaUtils interfaceMetaUtils, SouthboundUtils southboundUtils) {
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.alivenessMonitorUtils = alivenessMonitorUtils;
        this.mdsalApiManager = mdsalApiManager;
        this.coordinator = coordinator;
        this.interfaceManagerCommonUtils = interfaceManagerCommonUtils;
        this.ovsInterfaceStateAddHelper = ovsInterfaceStateAddHelper;
        this.interfaceMetaUtils = interfaceMetaUtils;
        this.southboundUtils = southboundUtils;
    }

    public List<ListenableFuture<Void>> addConfiguration(ParentRefs parentRefs, Interface interfaceNew) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        // TODO Disentangle the transactions
        futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION, configTx -> {
            futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(OPERATIONAL, operTx -> {
                IfTunnel ifTunnel = interfaceNew.augmentation(IfTunnel.class);
                if (ifTunnel != null) {
                    addTunnelConfiguration(parentRefs, interfaceNew, ifTunnel, configTx, operTx, futures);
                } else {
                    addVlanConfiguration(interfaceNew, parentRefs, configTx, operTx, futures);
                }
            }));
        }));
        return futures;
    }

    private void addVlanConfiguration(Interface interfaceNew, ParentRefs parentRefs,
        TypedWriteTransaction<Configuration> confTx, TypedWriteTransaction<Operational> operTx,
        List<ListenableFuture<Void>> futures) {
        IfL2vlan ifL2vlan = interfaceNew.augmentation(IfL2vlan.class);
        if (ifL2vlan == null || IfL2vlan.L2vlanMode.Trunk != ifL2vlan.getL2vlanMode()
                && IfL2vlan.L2vlanMode.Transparent != ifL2vlan.getL2vlanMode()) {
            return;
        }
        if (!interfaceManagerCommonUtils.createInterfaceChildEntryIfNotPresent(confTx,
                parentRefs.getParentInterface(), interfaceNew.getName(), ifL2vlan.getL2vlanMode())) {
            return;
        }
        LOG.info("adding vlan configuration for interface {}", interfaceNew.getName());
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.interfaces.rev180220.interfaces.state.Interface ifState = interfaceManagerCommonUtils
                .getInterfaceState(parentRefs.getParentInterface());

        interfaceManagerCommonUtils.addStateEntry(operTx, interfaceNew.getName(), futures, ifState);
    }

    private void addTunnelConfiguration(ParentRefs parentRefs, Interface interfaceNew, IfTunnel ifTunnel,
            TypedWriteTransaction<Configuration> confTx, TypedWriteTransaction<Operational> operTx,
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
            futures.add(addLogicalTunnelGroup(interfaceNew, operTx, confTx));
            return;
        }

        boolean createTunnelPort = true;
        final String tunnelName;
        if (SouthboundUtils.isOfTunnel(ifTunnel)) {
            BridgeEntry bridgeEntry = interfaceMetaUtils.getBridgeEntryFromConfigDS(dpId);
            createTunnelPort = bridgeEntry == null
                    || bridgeEntry.getBridgeInterfaceEntry() == null
                    || bridgeEntry.getBridgeInterfaceEntry().isEmpty();
            tunnelName = SouthboundUtils.generateOfTunnelName(dpId, ifTunnel);
            interfaceManagerCommonUtils.createInterfaceChildEntry(confTx, tunnelName, interfaceNew.getName());
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state
                    .Interface
                    interfaceState = interfaceManagerCommonUtils.getInterfaceState(tunnelName);
            if (interfaceState != null) {
                coordinator.enqueueJob(tunnelName, () -> ovsInterfaceStateAddHelper.addState(interfaceNew.getName(),
                        interfaceState));
            }
        } else {
            tunnelName = interfaceNew.getName();
        }

        String parentInterface = parentRefs.getParentInterface();
        if (ifTunnel.getTunnelInterfaceType().isAssignableFrom(TunnelTypeVxlan.class)
                && !Strings.isNullOrEmpty(parentInterface)) {
            LOG.debug("MULTIPLE_VxLAN_TUNNELS: createInterfaceChildEntry for {} in logical group {}",
                    tunnelName, parentInterface);
            interfaceManagerCommonUtils.createInterfaceChildEntry(confTx, parentInterface, tunnelName);
        }
        LOG.debug("creating bridge interfaceEntry in ConfigDS {}", dpId);
        interfaceMetaUtils.createBridgeInterfaceEntryInConfigDS(dpId, interfaceNew.getName());

        // create bridge on switch, if switch is connected
        BridgeRefEntry bridgeRefEntry = interfaceMetaUtils.getBridgeRefEntryFromOperDS(dpId);
        if (bridgeRefEntry != null && bridgeRefEntry.getBridgeReference() != null) {
            LOG.debug("creating bridge interface on dpn {}", dpId);
            InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIid =
                    (InstanceIdentifier<OvsdbBridgeAugmentation>) bridgeRefEntry
                    .getBridgeReference().getValue();
            if (createTunnelPort) {
                southboundUtils.addPortToBridge(bridgeIid, interfaceNew, tunnelName);
            }

            // if TEP is already configured on switch, start LLDP monitoring and
            // program tunnel ingress flow
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.interfaces.rev180220.interfaces.state.Interface ifState = interfaceManagerCommonUtils
                    .getInterfaceState(interfaceNew.getName());
            if (ifState != null) {
                NodeConnectorId ncId = IfmUtil.getNodeConnectorIdFromInterface(ifState);
                if (ncId != null) {
                    long portNo = IfmUtil.getPortNumberFromNodeConnectorId(ncId);
                    interfaceManagerCommonUtils.addTunnelIngressFlow(confTx, ifTunnel, dpId, portNo,
                            interfaceNew.getName(), ifState.getIfIndex());
                    ListenableFuture<Void> future =
                            FlowBasedServicesUtils.bindDefaultEgressDispatcherService(txRunner, interfaceNew,
                                    Long.toString(portNo), interfaceNew.getName(), ifState.getIfIndex());
                    futures.add(future);
                    Futures.addCallback(future, new FutureCallback<Void>() {
                        @Override
                        public void onSuccess(@Nullable Void result) {
                            // start LLDP monitoring for the tunnel interface
                            alivenessMonitorUtils.startLLDPMonitoring(ifTunnel, interfaceNew.getName());
                        }

                        @Override
                        public void onFailure(@Nonnull Throwable throwable) {
                            LOG.error("Unable to add tunnel monitoring", throwable);
                        }
                    }, MoreExecutors.directExecutor());
                }
            }
        }
    }

    private long createLogicalTunnelSelectGroup(TypedWriteTransaction<Configuration> tx,
        BigInteger srcDpnId, String interfaceName, int lportTag) {
        long groupId = IfmUtil.getLogicalTunnelSelectGroupId(lportTag);
        Group group = MDSALUtil.buildGroup(groupId, interfaceName, GroupTypes.GroupSelect,
                                           MDSALUtil.buildBucketLists(Collections.emptyList()));
        LOG.debug("MULTIPLE_VxLAN_TUNNELS: group id {} installed for {} srcDpnId {}",
                group.getGroupId().getValue(), interfaceName, srcDpnId);
        mdsalApiManager.addGroup(tx, srcDpnId, group);
        return groupId;
    }

    private ListenableFuture<Void> addLogicalTunnelGroup(Interface itfNew, TypedWriteTransaction<Operational> operTx,
        TypedWriteTransaction<Configuration> confTx) {
        String ifaceName = itfNew.getName();
        LOG.debug("MULTIPLE_VxLAN_TUNNELS: adding Interface State for logic tunnel group {}", ifaceName);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state
            .Interface ifState = interfaceManagerCommonUtils.addStateEntry(itfNew, ifaceName, operTx,
                    null /*physAddress*/, org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
                    .interfaces.rev180220.interfaces.state.Interface.OperStatus.Up,
                    org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
                    .interfaces.rev180220.interfaces.state.Interface.AdminStatus.Up,
                    null /*nodeConnectorId*/);
        long groupId = createLogicalTunnelSelectGroup(confTx, IfmUtil.getDpnFromInterface(ifState),
                                                      itfNew.getName(), ifState.getIfIndex());
        return FlowBasedServicesUtils.bindDefaultEgressDispatcherService(txRunner, itfNew,
                                                                  ifaceName, ifState.getIfIndex(), groupId);
    }

}
