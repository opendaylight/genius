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
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.commons.AlivenessMonitorUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.AlivenessMonitorService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class OvsInterfaceStateUpdateHelper {
    private static final Logger LOG = LoggerFactory.getLogger(OvsInterfaceStateUpdateHelper.class);

    public static List<ListenableFuture<Void>> updateState(InstanceIdentifier<FlowCapableNodeConnector> key,
                                                           AlivenessMonitorService alivenessMonitorService,
                                                           DataBroker dataBroker, String interfaceName,
                                                           FlowCapableNodeConnector flowCapableNodeConnectorNew,
                                                           FlowCapableNodeConnector flowCapableNodeConnectorOld) {
        LOG.debug("Update of Interface State for port: {}", interfaceName);
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();

        Interface.OperStatus operStatusNew = getOpState(flowCapableNodeConnectorNew);
        MacAddress macAddressNew = flowCapableNodeConnectorNew.getHardwareAddress();

        Interface.OperStatus operStatusOld = getOpState(flowCapableNodeConnectorOld);
        MacAddress macAddressOld = flowCapableNodeConnectorOld.getHardwareAddress();

        boolean opstateModified = false;
        boolean hardwareAddressModified = false;
        if (!operStatusNew.equals(operStatusOld)) {
            opstateModified = true;
        }
        if (!macAddressNew.equals(macAddressOld)) {
            hardwareAddressModified = true;
        }

        if (!opstateModified && !hardwareAddressModified) {
            LOG.debug("If State entry for port: {} Not Modified.", interfaceName);
            return futures;
        }

        InterfaceBuilder ifaceBuilder = new InterfaceBuilder();
        if (hardwareAddressModified) {
            LOG.debug("Hw-Address Modified for Port: {}", interfaceName);
            PhysAddress physAddress = new PhysAddress(macAddressNew.getValue());
            ifaceBuilder.setPhysAddress(physAddress);
        }

        // modify the attributes in interface operational DS
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface iface =
                handleInterfaceStateUpdates(interfaceName, transaction, dataBroker,
                        ifaceBuilder, opstateModified, flowCapableNodeConnectorNew.getName(), operStatusNew);

        // start/stop monitoring based on opState
        if(modifyTunnel(iface, opstateModified)){
            handleTunnelMonitoringUpdates(alivenessMonitorService, dataBroker, iface.getAugmentation(IfTunnel.class),
                    iface.getName(), operStatusNew);
        }

        futures.add(transaction.submit());
        return futures;
    }

    public static void updateInterfaceStateOnNodeRemove(String interfaceName, FlowCapableNodeConnector flowCapableNodeConnector,
                                                        DataBroker dataBroker, AlivenessMonitorService alivenessMonitorService,
                                                        WriteTransaction transaction){
        LOG.debug("Updating interface oper-status to UNKNOWN for : {}", interfaceName);

        InterfaceBuilder ifaceBuilder = new InterfaceBuilder();
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface iface =
                handleInterfaceStateUpdates(interfaceName,transaction, dataBroker,
                        ifaceBuilder, true, flowCapableNodeConnector.getName(),
                        Interface.OperStatus.Unknown);
        if (InterfaceManagerCommonUtils.isTunnelInterface(iface)){
            handleTunnelMonitoringUpdates(alivenessMonitorService, dataBroker, iface.getAugmentation(IfTunnel.class),
                    interfaceName, Interface.OperStatus.Unknown);
        }
    }

    public static Interface.OperStatus getOpState(FlowCapableNodeConnector flowCapableNodeConnector){
        Interface.OperStatus operStatus =
                (flowCapableNodeConnector.getState().isLive() &&
                        !flowCapableNodeConnector.getConfiguration().isPORTDOWN())
                        ? Interface.OperStatus.Up: Interface.OperStatus.Down;
        return operStatus;
    }

    public static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface
    handleInterfaceStateUpdates(String interfaceName, WriteTransaction transaction,
                                DataBroker dataBroker, InterfaceBuilder ifaceBuilder, boolean opStateModified,
                                String portName,
                                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus opState){
        LOG.debug("updating interface state entry for {}", interfaceName);
        InstanceIdentifier<Interface> ifStateId = IfmUtil.buildStateInterfaceId(interfaceName);
        ifaceBuilder.setKey(new InterfaceKey(interfaceName));
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface iface =
                InterfaceManagerCommonUtils.getInterfaceFromConfigDS(interfaceName, dataBroker);
        // if interface config DS is null, do the update only for the lower-layer-interfaces
        // which have no corresponding config entries
        if(iface == null && interfaceName != portName){
            return null;
        }
        if (modifyOpState(iface, opStateModified)) {
            LOG.debug("updating interface oper status as {} for {}", opState.name(), interfaceName);
            ifaceBuilder.setOperStatus(opState);
        }
        transaction.merge(LogicalDatastoreType.OPERATIONAL, ifStateId, ifaceBuilder.build());

        return iface;
    }

    public static void handleTunnelMonitoringUpdates(AlivenessMonitorService alivenessMonitorService, DataBroker dataBroker,
                                                     IfTunnel ifTunnel, String interfaceName,
                                                     Interface.OperStatus operStatus){

        LOG.debug("handling tunnel monitoring updates for {} due to opstate modification", interfaceName);
        if (operStatus == Interface.OperStatus.Down || operStatus == Interface.OperStatus.Unknown)
            AlivenessMonitorUtils.stopLLDPMonitoring(alivenessMonitorService, dataBroker, ifTunnel, interfaceName);
        else
            AlivenessMonitorUtils.startLLDPMonitoring(alivenessMonitorService, dataBroker, ifTunnel, interfaceName);
    }

    public static boolean modifyOpState(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface iface,
                                        boolean opStateModified){
        return (opStateModified && (iface == null || iface != null && iface.isEnabled()));
    }

    public static boolean modifyTunnel(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface iface,
                                       boolean opStateModified){
        return modifyOpState(iface, opStateModified) && iface != null && iface.getAugmentation(IfTunnel.class) != null;
    }
}
