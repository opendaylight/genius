/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.renderer.ovs.confighelpers;

import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
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
import org.opendaylight.genius.interfacemanager.globals.InterfaceInfo;
import org.opendaylight.genius.interfacemanager.renderer.ovs.utilities.SouthboundUtils;
import org.opendaylight.genius.itm.api.IITMProvider;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsInterfaceConfigAddHelper {
    private static final Logger LOG = LoggerFactory.getLogger(OvsInterfaceConfigAddHelper.class);

    public static List<ListenableFuture<Void>> addConfiguration(DataBroker dataBroker, ParentRefs parentRefs,
                                                                Interface interfaceNew, IdManagerService idManager,
                                                                AlivenessMonitorService alivenessMonitorService,
                                                                IMdsalApiManager mdsalApiManager,
                                                                IITMProvider itmProvider) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        WriteTransaction defaultConfigShardTransaction = dataBroker.newWriteOnlyTransaction();
        WriteTransaction defaultOperShardTransaction = dataBroker.newWriteOnlyTransaction();
        IfTunnel ifTunnel = interfaceNew.getAugmentation(IfTunnel.class);
        if (ifTunnel != null) {
            addTunnelConfiguration(dataBroker, parentRefs, interfaceNew, idManager, alivenessMonitorService, ifTunnel,
                    mdsalApiManager, itmProvider, defaultOperShardTransaction, futures);
        } else {
            addVlanConfiguration(interfaceNew, parentRefs, dataBroker, idManager, defaultConfigShardTransaction,
                    defaultOperShardTransaction, futures);
        }
        futures.add(defaultConfigShardTransaction.submit());
        futures.add(defaultOperShardTransaction.submit());
        return futures;
    }

    private static void addVlanConfiguration(Interface interfaceNew, ParentRefs parentRefs, DataBroker dataBroker, IdManagerService idManager,
                                             WriteTransaction defaultConfigShardTransaction, WriteTransaction defaultOperShardTransaction,
                                             List<ListenableFuture<Void>> futures) {
        IfL2vlan ifL2vlan = interfaceNew.getAugmentation(IfL2vlan.class);
        if (ifL2vlan == null || IfL2vlan.L2vlanMode.Trunk != ifL2vlan.getL2vlanMode() && IfL2vlan.L2vlanMode.Transparent != ifL2vlan.getL2vlanMode()) {
            return;
        }
        if (!InterfaceManagerCommonUtils.createInterfaceChildEntryIfNotPresent(dataBroker, defaultConfigShardTransaction,
                parentRefs.getParentInterface(), interfaceNew.getName(), ifL2vlan.getL2vlanMode())) {
            return;
        }
        LOG.info("adding vlan configuration for interface {}",interfaceNew.getName());
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface ifState =
                InterfaceManagerCommonUtils.getInterfaceStateFromOperDS(parentRefs.getParentInterface(), dataBroker);

        InterfaceManagerCommonUtils.addStateEntry(interfaceNew.getName(), dataBroker, defaultOperShardTransaction,
                idManager, futures, ifState);

        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
        VlanMemberStateAddWorker vlanMemberStateAddWorker = new VlanMemberStateAddWorker(dataBroker, idManager,
                interfaceNew.getName(), ifState);
        coordinator.enqueueJob(interfaceNew.getName(), vlanMemberStateAddWorker, IfmConstants.JOB_MAX_RETRIES);
    }

    private static void addTunnelConfiguration(DataBroker dataBroker, ParentRefs parentRefs,
                                               Interface interfaceNew, IdManagerService idManager,
                                               AlivenessMonitorService alivenessMonitorService,
                                               IfTunnel ifTunnel, IMdsalApiManager mdsalApiManager,
                                               IITMProvider itmProvider,
                                               WriteTransaction defaultOperShardTransaction,
                                               List<ListenableFuture<Void>> futures) {
        if (parentRefs == null) {
            LOG.warn("ParentRefs for interface: {} Not Found. " +
                    "Creation of Tunnel OF-Port not supported when dpid not provided.", interfaceNew.getName());
            return;
        }

        BigInteger dpId = parentRefs.getDatapathNodeIdentifier();
        if (dpId == null) {
            LOG.warn("dpid for interface: {} Not Found. No DPID provided. " +
                    "Creation of OF-Port not supported.", interfaceNew.getName());
            return;
        }
        LOG.info("adding tunnel configuration for interface {}", interfaceNew.getName());

        if (ifTunnel.getTunnelInterfaceType().equals(TunnelTypeLogicalGroup.class) ||
            ifTunnel.getTunnelInterfaceType().equals(TunnelTypeVxlan.class)) {

            addMultipleVxLANtunnelsConfiguration(dataBroker, interfaceNew, idManager,
                                                 ifTunnel, itmProvider, defaultOperShardTransaction);
        }
        boolean createTunnelPort = true;
        String tunnelName = interfaceNew.getName();
        if (SouthboundUtils.isOfTunnel(ifTunnel)) {
            BridgeEntry bridgeEntry = InterfaceMetaUtils.getBridgeEntryFromConfigDS(dpId, dataBroker);
            createTunnelPort = bridgeEntry == null || bridgeEntry.getBridgeInterfaceEntry() == null ?
                    true : bridgeEntry.getBridgeInterfaceEntry().isEmpty();
            tunnelName = SouthboundUtils.generateOfTunnelName(dpId, ifTunnel);
            InterfaceManagerCommonUtils.createInterfaceChildEntry(tunnelName, interfaceNew.getName());
        }

        LOG.debug("creating bridge interfaceEntry in ConfigDS {}", dpId);
        InterfaceMetaUtils.createBridgeInterfaceEntryInConfigDS(dpId, interfaceNew.getName());

        // create bridge on switch, if switch is connected
        BridgeRefEntry bridgeRefEntry =
                InterfaceMetaUtils.getBridgeRefEntryFromOperDS(dpId, dataBroker);
        if(bridgeRefEntry != null && bridgeRefEntry.getBridgeReference() != null) {
            LOG.debug("creating bridge interface on dpn {}", dpId);
            InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIid =
                    (InstanceIdentifier<OvsdbBridgeAugmentation>) bridgeRefEntry.getBridgeReference().getValue();
            if(createTunnelPort) {
                SouthboundUtils.addPortToBridge(bridgeIid, interfaceNew, tunnelName, dataBroker, futures);
            }

            // if TEP is already configured on switch, start LLDP monitoring and program tunnel ingress flow
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface ifState =
                    InterfaceManagerCommonUtils.getInterfaceStateFromOperDS(interfaceNew.getName(), dataBroker);
            if(ifState != null){
                NodeConnectorId ncId = IfmUtil.getNodeConnectorIdFromInterface(ifState);
                if(ncId != null) {
                    long portNo = IfmUtil.getPortNumberFromNodeConnectorId(ncId);
                    InterfaceManagerCommonUtils.makeTunnelIngressFlow(futures, mdsalApiManager, ifTunnel,
                            dpId, portNo, interfaceNew.getName(),
                            ifState.getIfIndex(), NwConstants.ADD_FLOW);
                    // start LLDP monitoring for the tunnel interface
                    AlivenessMonitorUtils.startLLDPMonitoring(alivenessMonitorService, dataBroker,
                            ifTunnel, interfaceNew.getName());
                }
            }
        }
    }

    private static class VlanMemberStateAddWorker implements Callable<List<ListenableFuture<Void>>> {

        private final DataBroker dataBroker;
        private final IdManagerService idManager;
        private final String interfaceName;
        private final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface ifState;

        public VlanMemberStateAddWorker(DataBroker dataBroker, IdManagerService idManager, String interfaceName,
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface ifState) {
            this.dataBroker = dataBroker;
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
            WriteTransaction operShardTransaction = dataBroker.newWriteOnlyTransaction();
            // FIXME: If the no. of child entries exceeds 100, perform txn
            // updates in batches of 100.
            for (InterfaceChildEntry interfaceChildEntry : interfaceParentEntry.getInterfaceChildEntry()) {
                LOG.debug("adding interface state for vlan trunk member {}", interfaceChildEntry.getChildInterface());
                InterfaceManagerCommonUtils.addStateEntry(interfaceChildEntry.getChildInterface(), dataBroker,
                        operShardTransaction, idManager, futures, ifState);
            }

            futures.add(operShardTransaction.submit());
            return futures;
        }

        @Override
        public String toString() {
            return "VlanMemberStateAddWorker [interfaceName=" + interfaceName + ", ifState=" + ifState + "]";
        }
    }

    private static void addMultipleVxLANtunnelsConfiguration(DataBroker dataBroker, Interface itfNew,
                                                      IdManagerService idManager, IfTunnel ifTunnel,
                                                      IITMProvider itmProvider,
                                                      WriteTransaction defaultOperShardTransaction) {
        if (dataBroker == null || itfNew == null || idManager == null || ifTunnel == null || itmProvider == null) {
            LOG.warn("MULTIPLE_VxLAN_TUNNELS: addMultipleVxLANtunnelsConfiguration - not full input info recieved");
            return;
        }
        if (ifTunnel.getTunnelInterfaceType().equals(TunnelTypeLogicalGroup.class)) {

            LOG.debug("MULTIPLE_VxLAN_TUNNELS: adding Interface State for logic tunnel group {}", itfNew.getName());
            InterfaceManagerCommonUtils.addStateEntry(itfNew, itfNew.getName(), defaultOperShardTransaction,
                                                      idManager, null /*physAddress*/,
                                                      org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
                                                      .interfaces.rev140508.interfaces.state.Interface.OperStatus.Up,
                                                      org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
                                                      .interfaces.rev140508.interfaces.state.Interface.AdminStatus.Up,
                                                      null /*nodeConnectorId*/);
            return;
        }
        if (ifTunnel.getTunnelInterfaceType().equals(TunnelTypeVxlan.class)) {
            //Update the interface-child-info model with the list of individual tunnel members
            String parentInterface = null;
            List<Interface> logicTunnelInterfaces = InterfaceManagerCommonUtils.getAllLogicTunnelInterfacesFromCache();
            LOG.debug("MULTIPLE_VxLAN_TUNNELS: getAllLogicTunnelInterfaces size {}", logicTunnelInterfaces.size());
            if (logicTunnelInterfaces.size() == 0) {
                return;
            }
            InternalTunnel tunnelNew = itmProvider.getInternalTunnel(itfNew.getName());
            if (tunnelNew == null) {
                LOG.warn("MULTIPLE_VxLAN_TUNNELS: InternalTunnel for {} doesn't exist", itfNew.getName());
                return;
            }
            for (Interface iface : logicTunnelInterfaces) {
                InternalTunnel tunnelLogic = itmProvider.getInternalTunnel(iface.getName());
                if (tunnelLogic == null) {
                    LOG.debug("MULTIPLE_VxLAN_TUNNELS: InternalTunnel for the logic group {} doesn't exist",
                            iface.getName());
                    continue;
                }
                if (tunnelNew.getSourceDPN().equals(tunnelLogic.getSourceDPN())
                        && tunnelNew.getDestinationDPN().equals(tunnelLogic.getDestinationDPN())) {

                    parentInterface = iface.getName();
                    LOG.debug("MULTIPLE_VxLAN_TUNNELS: logical tunnel group found {}", parentInterface);
                    break;
                }
            }
            if (parentInterface != null) {
                LOG.debug("MULTIPLE_VxLAN_TUNNELS: createInterfaceChildEntry for {} in logical group {}",
                           itfNew.getName(), parentInterface);
                InterfaceManagerCommonUtils.createInterfaceChildEntry(parentInterface, itfNew.getName());
            }
        }
    }
}
