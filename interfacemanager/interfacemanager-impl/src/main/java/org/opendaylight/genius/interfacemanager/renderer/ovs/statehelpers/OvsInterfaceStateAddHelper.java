/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.renderer.ovs.statehelpers;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.commons.AlivenessMonitorUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.AlivenessMonitorService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This worker is responsible for adding the openflow-interfaces/of-port-info
 * container in odl-interface-openflow yang. Where applicable: Create the
 * entries in Interface-State OperDS. Create the entries in Inventory OperDS.
 */

public class OvsInterfaceStateAddHelper {
    private static final Logger LOG = LoggerFactory.getLogger(OvsInterfaceStateAddHelper.class);

    public static List<ListenableFuture<Void>> addState(DataBroker dataBroker, ManagedNewTransactionRunner txRunner,
            IdManagerService idManager,
            IMdsalApiManager mdsalApiManager, AlivenessMonitorService alivenessMonitorService,
            NodeConnectorId nodeConnectorId, String interfaceName, FlowCapableNodeConnector fcNodeConnectorNew) {
        // Retrieve Port No from nodeConnectorId
        long portNo = IfmUtil.getPortNumberFromNodeConnectorId(nodeConnectorId);
        if (portNo == IfmConstants.INVALID_PORT_NO) {
            LOG.trace("Cannot derive port number, not proceeding with Interface State " + "addition for interface: {}",
                    interfaceName);
            return null;
        }

        // Retrieve PbyAddress & OperState from the DataObject
        LOG.info("adding interface state to Oper DS for interface: {}", interfaceName);
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx -> {
            PhysAddress physAddress = IfmUtil.getPhyAddress(portNo, fcNodeConnectorNew);

            Interface.OperStatus operStatus = Interface.OperStatus.Up;
            Interface.AdminStatus adminStatus = Interface.AdminStatus.Up;

            // Fetch the interface from config DS if exists
            InterfaceKey interfaceKey = new InterfaceKey(interfaceName);
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
                    .interfaces.rev140508.interfaces.Interface iface = InterfaceManagerCommonUtils
                    .getInterfaceFromConfigDS(interfaceKey, dataBroker);

            if (InterfaceManagerCommonUtils.isTunnelPort(interfaceName)
                    && !validateTunnelPortAttributes(nodeConnectorId, iface)) {
                return;
            }

            Interface ifState = InterfaceManagerCommonUtils.addStateEntry(iface, interfaceName,
                    tx, idManager, physAddress, operStatus, adminStatus, nodeConnectorId);

            // If this interface is a tunnel interface, create the tunnel ingress
            // flow,and start tunnel monitoring
            if (InterfaceManagerCommonUtils.isTunnelInterface(iface)) {
                handleTunnelMonitoringAddition(futures, dataBroker, txRunner, mdsalApiManager, alivenessMonitorService,
                        nodeConnectorId, ifState.getIfIndex(), iface, interfaceName,
                        portNo);
                return;
            }

            // install ingress flow if this is an l2vlan interface
            if (InterfaceManagerCommonUtils.isVlanInterface(iface) && iface.isEnabled() && ifState
                    .getOperStatus() == org.opendaylight.yang.gen.v1.urn
                    .ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus.Up) {
                BigInteger dpId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
                FlowBasedServicesUtils.installLportIngressFlow(dpId, portNo, iface, futures, txRunner,
                        ifState.getIfIndex());
                futures.add(FlowBasedServicesUtils.bindDefaultEgressDispatcherService(txRunner, iface,
                        Long.toString(portNo), interfaceName, ifState.getIfIndex()));
            }
        }));

        return futures;
    }

    private static void handleTunnelMonitoringAddition(List<ListenableFuture<Void>> futures, DataBroker dataBroker,
            ManagedNewTransactionRunner txRunner,
            IMdsalApiManager mdsalApiManager, AlivenessMonitorService alivenessMonitorService,
            NodeConnectorId nodeConnectorId, Integer ifIndex,
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                    .ietf.interfaces.rev140508.interfaces.Interface interfaceInfo,
            String interfaceName, long portNo) {
        BigInteger dpId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
        InterfaceManagerCommonUtils.makeTunnelIngressFlow(futures, mdsalApiManager,
                interfaceInfo.getAugmentation(IfTunnel.class), dpId, portNo, interfaceName, ifIndex,
                NwConstants.ADD_FLOW);
        ListenableFuture<Void> future =
                FlowBasedServicesUtils.bindDefaultEgressDispatcherService(txRunner, interfaceInfo,
                        Long.toString(portNo), interfaceName, ifIndex);
        future.addListener(() -> AlivenessMonitorUtils.startLLDPMonitoring(alivenessMonitorService, dataBroker,
                interfaceInfo.getAugmentation(IfTunnel.class), interfaceName), MoreExecutors.directExecutor());
        futures.add(future);
    }

    private static boolean validateTunnelPortAttributes(NodeConnectorId nodeConnectorId,
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                    .ietf.interfaces.rev140508.interfaces.Interface iface) {
        BigInteger currentDpnId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
        if (iface != null) {
            ParentRefs parentRefs = iface.getAugmentation(ParentRefs.class);
            if (!currentDpnId.equals(parentRefs.getDatapathNodeIdentifier())) {
                LOG.warn(
                        "Received tunnel state add notification for tunnel {} from dpn {} where as "
                                + "the northbound configured dpn is {}",
                        iface.getName(), currentDpnId, parentRefs.getDatapathNodeIdentifier());
                return false;
            }
        }
        return true;
    }
}
