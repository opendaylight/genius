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
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.AlivenessMonitorService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class OvsInterfaceStateRemoveHelper {
    private static final Logger LOG = LoggerFactory.getLogger(OvsInterfaceStateRemoveHelper.class);

    public static List<ListenableFuture<Void>> removeInterfaceStateConfiguration(IdManagerService idManager, IMdsalApiManager mdsalApiManager,
                                                                                 AlivenessMonitorService alivenessMonitorService,
                                                                                 NodeConnectorId nodeConnectorIdNew, NodeConnectorId nodeConnectorIdOld,
                                                                                 DataBroker dataBroker, String interfaceName,
                                                                                 FlowCapableNodeConnector fcNodeConnectorOld,
                                                                                 boolean isNodePresent,
                                                                                 String parentInterface) {
        LOG.debug("Removing interface-state information for interface: {} {}", interfaceName, isNodePresent);
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        WriteTransaction defaultOperationalShardTransaction = dataBroker.newWriteOnlyTransaction();

        //VM Migration: Use old nodeConnectorId to delete the interface entry
        NodeConnectorId nodeConnectorId = nodeConnectorIdOld != null && !nodeConnectorIdNew.equals(nodeConnectorIdOld) ?
                nodeConnectorIdOld : nodeConnectorIdNew;
        // delete the port entry from interface operational DS
        BigInteger dpId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);

        //VM Migration: Update the interface state to unknown only if remove event received for same switch
        if(!isNodePresent && nodeConnectorIdNew.equals(nodeConnectorIdOld)){
            //Remove event is because of connection lost between controller and switch, or switch shutdown.
            // Hence, dont remove the interface but set the status as "unknown"
            OvsInterfaceStateUpdateHelper.updateInterfaceStateOnNodeRemove(interfaceName, fcNodeConnectorOld, dataBroker,
                    alivenessMonitorService, defaultOperationalShardTransaction);
        }else{
            InterfaceManagerCommonUtils.deleteStateEntry(interfaceName, defaultOperationalShardTransaction);
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface iface =
                    InterfaceManagerCommonUtils.getInterfaceFromConfigDS(interfaceName, dataBroker);

            if(iface != null) {
                // If this interface is a tunnel interface, remove the tunnel ingress flow and stop lldp monitoring
                if (InterfaceManagerCommonUtils.isTunnelInterface(iface)) {
                    InterfaceMetaUtils.removeLportTagInterfaceMap(idManager, defaultOperationalShardTransaction, interfaceName);
                    handleTunnelMonitoringRemoval(alivenessMonitorService, mdsalApiManager, dataBroker, dpId,
                            iface.getName(), iface.getAugmentation(IfTunnel.class), defaultOperationalShardTransaction,
                            nodeConnectorId, futures);
                    return futures;
                }
            }
            // remove ingress flow only for northbound configured interfaces
            // skip this check for non-unique ports(Ex: br-int,br-ex)
            if(iface != null || (iface == null && !interfaceName.contains(fcNodeConnectorOld.getName()))) {
                FlowBasedServicesUtils.removeIngressFlow(interfaceName, dpId, dataBroker, futures);
            }

            // Delete the Vpn Interface from DpnToInterface Op DS.
            InterfaceManagerCommonUtils.deleteDpnToInterface(dataBroker, dpId, interfaceName, defaultOperationalShardTransaction);
        }
        futures.add(defaultOperationalShardTransaction.submit());
        return futures;
    }

    public static void handleTunnelMonitoringRemoval(AlivenessMonitorService alivenessMonitorService, IMdsalApiManager mdsalApiManager,
                                                     DataBroker dataBroker, BigInteger dpId, String interfaceName,
                                                     IfTunnel ifTunnel, WriteTransaction transaction,
                                                     NodeConnectorId nodeConnectorId, List<ListenableFuture<Void>> futures){
        long portNo = IfmUtil.getPortNumberFromNodeConnectorId(nodeConnectorId);
        InterfaceManagerCommonUtils.makeTunnelIngressFlow(futures, mdsalApiManager, ifTunnel, dpId, portNo, interfaceName, -1,
                NwConstants.DEL_FLOW);
        futures.add(transaction.submit());
        AlivenessMonitorUtils.stopLLDPMonitoring(alivenessMonitorService, dataBroker, ifTunnel, interfaceName);
    }
}