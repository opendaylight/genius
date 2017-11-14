/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.renderer.ovs.statehelpers;

import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.commons.AlivenessMonitorUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
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

@Singleton
public final class OvsInterfaceStateAddHelper {
    private static final Logger LOG = LoggerFactory.getLogger(OvsInterfaceStateAddHelper.class);

    private final ManagedNewTransactionRunner txRunner;
    private final InterfaceManagerCommonUtils interfaceManagerCommonUtils;
    private final AlivenessMonitorUtils alivenessMonitorUtils;

    @Inject
    public OvsInterfaceStateAddHelper(DataBroker dataBroker, AlivenessMonitorUtils alivenessMonitorUtils,
            InterfaceManagerCommonUtils interfaceManagerCommonUtils) {
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.interfaceManagerCommonUtils = interfaceManagerCommonUtils;
        this.alivenessMonitorUtils = alivenessMonitorUtils;
    }

    public List<ListenableFuture<Void>> addState(String interfaceName, Interface parentInterface) {
        if (parentInterface.getLowerLayerIf() == null || parentInterface.getLowerLayerIf().isEmpty()) {
            LOG.trace("Cannot obtain lower layer if, not proceeding with Interface State addition for interface: {}",
                    interfaceName);
        }
        NodeConnectorId nodeConnectorId = new NodeConnectorId(parentInterface.getLowerLayerIf().get(0));
        PhysAddress physAddress = parentInterface.getPhysAddress();
        long portNo = IfmUtil.getPortNumberFromNodeConnectorId(nodeConnectorId);
        return addState(nodeConnectorId, interfaceName, portNo, physAddress);
    }



    public List<ListenableFuture<Void>> addState(NodeConnectorId nodeConnectorId, String interfaceName,
            FlowCapableNodeConnector fcNodeConnectorNew) {
        long portNo = IfmUtil.getPortNumberFromNodeConnectorId(nodeConnectorId);
        PhysAddress physAddress = IfmUtil.getPhyAddress(portNo, fcNodeConnectorNew);
        return addState(nodeConnectorId, interfaceName, portNo, physAddress);
    }

    private List<ListenableFuture<Void>> addState(NodeConnectorId nodeConnectorId, String interfaceName,
            long portNo, PhysAddress physAddress) {
        LOG.info("Adding Interface State to Oper DS for interface: {}", interfaceName);

        if (portNo == IfmConstants.INVALID_PORT_NO) {
            LOG.trace("Cannot derive port number, not proceeding with Interface State " + "addition for interface: {}",
                    interfaceName);
            return null;
        }

        List<ListenableFuture<Void>> futures = new ArrayList<>();

        Interface.OperStatus operStatus = Interface.OperStatus.Up;
        Interface.AdminStatus adminStatus = Interface.AdminStatus.Up;

        // Fetch the interface from config DS if exists
        InterfaceKey interfaceKey = new InterfaceKey(interfaceName);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
            .interfaces.rev140508.interfaces.Interface iface = interfaceManagerCommonUtils
                .getInterfaceFromConfigDS(interfaceKey);

        if (InterfaceManagerCommonUtils.isTunnelPort(interfaceName)
                && !validateTunnelPortAttributes(nodeConnectorId, iface)) {
            return futures;
        }

        futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx -> {
            Interface ifState = interfaceManagerCommonUtils.addStateEntry(iface, interfaceName,
                    tx, physAddress, operStatus, adminStatus, nodeConnectorId);

            // If this interface is a tunnel interface, create the tunnel ingress
            // flow,and start tunnel monitoring
            if (InterfaceManagerCommonUtils.isTunnelInterface(iface)) {
                handleTunnelMonitoringAddition(futures, nodeConnectorId, ifState.getIfIndex(), iface, interfaceName,
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
                FlowBasedServicesUtils.bindDefaultEgressDispatcherService(txRunner, futures, iface,
                        Long.toString(portNo), interfaceName, ifState.getIfIndex());
            }
        }));

        return futures;
    }

    public void handleTunnelMonitoringAddition(List<ListenableFuture<Void>> futures, NodeConnectorId nodeConnectorId,
            Integer ifIndex,
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                    .ietf.interfaces.rev140508.interfaces.Interface interfaceInfo, String interfaceName, long portNo) {
        BigInteger dpId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
        interfaceManagerCommonUtils.addTunnelIngressFlow(
                interfaceInfo.getAugmentation(IfTunnel.class), dpId, portNo, interfaceName, ifIndex);
        FlowBasedServicesUtils.bindDefaultEgressDispatcherService(txRunner, futures, interfaceInfo,
                Long.toString(portNo), interfaceName, ifIndex);
        alivenessMonitorUtils.startLLDPMonitoring(interfaceInfo.getAugmentation(IfTunnel.class), interfaceName);
    }

    public static boolean validateTunnelPortAttributes(NodeConnectorId nodeConnectorId,
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
