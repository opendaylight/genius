/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.renderer.ovs.statehelpers;

import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.commons.AlivenessMonitorUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.factory.FlowBasedServicesStateAddable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.factory.FlowBasedServicesStateRendererFactory;
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
 * This worker is responsible for adding the openflow-interfaces/of-port-info container
 * in odl-interface-openflow yang.
 * Where applicable:
 * Create the entries in Interface-State OperDS.
 * Create the entries in Inventory OperDS.
 */

public class OvsInterfaceStateAddHelper {
    private static final Logger LOG = LoggerFactory.getLogger(OvsInterfaceStateAddHelper.class);

    public static List<ListenableFuture<Void>> addState(DataBroker dataBroker, IdManagerService idManager,
            IMdsalApiManager mdsalApiManager, AlivenessMonitorService alivenessMonitorService,
            NodeConnectorId nodeConnectorId, String interfaceName, FlowCapableNodeConnector fcNodeConnectorNew) {
        //Retrieve Port No from nodeConnectorId
        long portNo = IfmUtil.getPortNumberFromNodeConnectorId(nodeConnectorId);
        if (portNo == IfmConstants.INVALID_PORT_NO) {
            LOG.trace("Cannot derive port number, not proceeding with Interface State "
                    + "addition for interface: {}", interfaceName);
            return null;
        }

        //Retrieve PbyAddress & OperState from the DataObject
        LOG.debug("Adding Interface State to Oper DS for interface: {}", interfaceName);
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        WriteTransaction defaultOperationalShardTransaction = dataBroker.newWriteOnlyTransaction();
        PhysAddress physAddress = IfmUtil.getPhyAddress(portNo, fcNodeConnectorNew);

        Interface.OperStatus operStatus = Interface.OperStatus.Up;
        Interface.AdminStatus adminStatus = Interface.AdminStatus.Up;

        // Fetch the interface from config DS if exists
        InterfaceKey interfaceKey = new InterfaceKey(interfaceName);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface iface =
                InterfaceManagerCommonUtils.getInterfaceFromConfigDS(interfaceKey, dataBroker);

        if (InterfaceManagerCommonUtils.isTunnelPort(interfaceName)) {
            if (!validateTunnelPortAttributes(nodeConnectorId, iface)) {
                return futures;
            }
        }

        Interface ifState = InterfaceManagerCommonUtils.addStateEntry(iface, interfaceName,
                defaultOperationalShardTransaction, idManager, physAddress, operStatus, adminStatus, nodeConnectorId);

        // If this interface is a tunnel interface, create the tunnel ingress flow,and start tunnel monitoring
        if (InterfaceManagerCommonUtils.isTunnelInterface(iface)) {
            handleTunnelMonitoringAddition(futures, dataBroker, mdsalApiManager, alivenessMonitorService,
                    nodeConnectorId, defaultOperationalShardTransaction, ifState.getIfIndex(),
                    iface.getAugmentation(IfTunnel.class), interfaceName, portNo);
            return futures;
        }

        // install ingress flow if this is an l2vlan interface
        if (InterfaceManagerCommonUtils.isVlanInterface(iface) && iface.isEnabled() && ifState
                .getOperStatus() == org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus.Up) {
            BigInteger dpId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
            FlowBasedServicesUtils.installLportIngressFlow(dpId, portNo, iface, futures, dataBroker,
                    ifState.getIfIndex());
            FlowBasedServicesUtils.bindDefaultEgressDispatcherService(dataBroker, futures, iface, Long.toString(portNo),
                    interfaceName, ifState.getIfIndex());
        }
        // call the ingress and egress bind service on the interface if there are already services
        // created on the interface.
        DataStoreJobCoordinator.getInstance().enqueueJob(InterfaceManagerCommonUtils.getInterfaceServiceKey(interfaceName),
                () -> {
                    FlowBasedServicesUtils.SERVICE_MODE_MAP.values().stream().forEach(serviceMode ->
                        FlowBasedServicesStateRendererFactory.getFlowBasedServicesStateRendererFactory(serviceMode)
                        .getFlowBasedServicesStateAddRenderer().bindServicesOnInterface(ifState));
                    return Collections.emptyList();
                    }, IfmConstants.JOB_MAX_RETRIES);

        futures.add(defaultOperationalShardTransaction.submit());
        return futures;
    }

    public static void handleTunnelMonitoringAddition(List<ListenableFuture<Void>> futures, DataBroker dataBroker,
            IMdsalApiManager mdsalApiManager, AlivenessMonitorService alivenessMonitorService,
            NodeConnectorId nodeConnectorId, WriteTransaction transaction, Integer ifindex, IfTunnel ifTunnel,
            String interfaceName, long portNo) {
        BigInteger dpId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
        InterfaceManagerCommonUtils.makeTunnelIngressFlow(futures, mdsalApiManager, ifTunnel, dpId, portNo,
                interfaceName, ifindex, NwConstants.ADD_FLOW);
        futures.add(transaction.submit());
        AlivenessMonitorUtils.startLLDPMonitoring(alivenessMonitorService, dataBroker, ifTunnel, interfaceName);
    }

    public static boolean validateTunnelPortAttributes(NodeConnectorId nodeConnectorId,
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface iface) {
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
        } else {
            LOG.warn("Received tunnel state add notification for a tunnel which is not configured {}", iface.getName());
            return false;
        }
        return true;
    }
}
