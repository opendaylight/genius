/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.renderer.ovs.statehelpers;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.commons.AlivenessMonitorUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.mdsalutil.MatchInfo;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.AlivenessMonitorService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

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
                                                        IMdsalApiManager mdsalApiManager,AlivenessMonitorService alivenessMonitorService,
                                                        NodeConnectorId nodeConnectorId, String interfaceName, FlowCapableNodeConnector fcNodeConnectorNew) {
        LOG.debug("Adding Interface State to Oper DS for interface: {}", interfaceName);
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        WriteTransaction defaultOperationalShardTransaction = dataBroker.newWriteOnlyTransaction();

        //Retrieve PbyAddress & OperState from the DataObject
        long portNo = Long.valueOf(IfmUtil.getPortNoFromNodeConnectorId(nodeConnectorId));
        PhysAddress physAddress = IfmUtil.getPhyAddress(portNo, fcNodeConnectorNew);

        Interface.OperStatus operStatus = Interface.OperStatus.Up;
        Interface.AdminStatus adminStatus = Interface.AdminStatus.Up;

        // Fetch the interface from config DS if exists
        InterfaceKey interfaceKey = new InterfaceKey(interfaceName);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface iface =
                InterfaceManagerCommonUtils.getInterfaceFromConfigDS(interfaceKey, dataBroker);

        Interface ifState = InterfaceManagerCommonUtils.addStateEntry(iface, interfaceName, defaultOperationalShardTransaction, idManager,
                physAddress, operStatus, adminStatus, nodeConnectorId);

        // If this interface is a tunnel interface, create the tunnel ingress flow,and start tunnel monitoring
        if (InterfaceManagerCommonUtils.isTunnelInterface(iface)) {
            handleTunnelMonitoringAddition(futures, dataBroker, mdsalApiManager, alivenessMonitorService,
                    nodeConnectorId, defaultOperationalShardTransaction, ifState.getIfIndex(),
                    iface.getAugmentation(IfTunnel.class), iface, portNo);
            return futures;
        }

        // install ingress flow if this is an l2vlan interface
        if(InterfaceManagerCommonUtils.isVlanInterface(iface) && iface.isEnabled() &&
                ifState.getOperStatus() == org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus.Up) {
            BigInteger dpId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
            FlowBasedServicesUtils.installLportIngressFlow(dpId, portNo, iface, futures, dataBroker, ifState.getIfIndex());
            FlowBasedServicesUtils.bindDefaultEgressDispatcherService(dataBroker, futures, iface, Long.toString(portNo), interfaceName, ifState.getIfIndex());
        }

        futures.add(defaultOperationalShardTransaction.submit());
        return futures;
    }

    public static void handleTunnelMonitoringAddition(List<ListenableFuture<Void>> futures, DataBroker dataBroker,
                                                      IMdsalApiManager mdsalApiManager,AlivenessMonitorService alivenessMonitorService,
                                                      NodeConnectorId nodeConnectorId, WriteTransaction transaction, Integer ifindex,
                                                      IfTunnel ifTunnel, org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface iface, long portNo){
        BigInteger dpId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
        InterfaceManagerCommonUtils.makeTunnelIngressFlow(futures, mdsalApiManager, ifTunnel, dpId, portNo, iface,
                ifindex, NwConstants.ADD_FLOW);
        futures.add(transaction.submit());
        AlivenessMonitorUtils.startLLDPMonitoring(alivenessMonitorService, dataBroker, ifTunnel, iface.getName());
    }
}