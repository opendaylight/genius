/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.renderer.ovs.statehelpers;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class OvsInterfaceStateUpdateHelper {
    private static final Logger LOG = LoggerFactory.getLogger(OvsInterfaceStateUpdateHelper.class);

    private final DataBroker dataBroker;
    private final InterfaceManagerCommonUtils interfaceManagerCommonUtils;
    private final AlivenessMonitorUtils alivenessMonitorUtils;

    @Inject
    public OvsInterfaceStateUpdateHelper(DataBroker dataBroker, AlivenessMonitorUtils alivenessMonitorUtils,
            InterfaceManagerCommonUtils interfaceManagerCommonUtils) {
        this.dataBroker = dataBroker;
        this.interfaceManagerCommonUtils =  interfaceManagerCommonUtils;
        this.alivenessMonitorUtils = alivenessMonitorUtils;
    }

    public List<ListenableFuture<Void>> updateState(String interfaceName,
            FlowCapableNodeConnector flowCapableNodeConnectorNew,
            FlowCapableNodeConnector flowCapableNodeConnectorOld) {
        LOG.debug("Updating interface state information for interface: {}", interfaceName);

        Interface.OperStatus operStatusNew = InterfaceManagerCommonUtils.getOpState(flowCapableNodeConnectorNew);
        MacAddress macAddressNew = flowCapableNodeConnectorNew.getHardwareAddress();

        Interface.OperStatus operStatusOld = InterfaceManagerCommonUtils.getOpState(flowCapableNodeConnectorOld);
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
            return Collections.emptyList();
        }

        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
            .interfaces.rev140508.interfaces.Interface iface = interfaceManagerCommonUtils
                .getInterfaceFromConfigDS(interfaceName);

        // For monitoring enabled tunnels, skip opstate update
        if (isTunnelInterface(iface) && !modifyTunnelOpState(iface, opstateModified)) {
            LOG.debug("skip interface-state updation for monitoring enabled tunnel interface {}", interfaceName);
            opstateModified = false;
        }

        if (!opstateModified && !hardwareAddressModified) {
            LOG.debug("If State entry for port: {} Not Modified.", interfaceName);
            return Collections.emptyList();
        }
        InterfaceBuilder ifaceBuilder = new InterfaceBuilder();
        if (hardwareAddressModified) {
            LOG.debug("Hw-Address Modified for Port: {}", interfaceName);
            PhysAddress physAddress = new PhysAddress(macAddressNew.getValue());
            ifaceBuilder.setPhysAddress(physAddress);
        }
        // modify the attributes in interface operational DS
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        handleInterfaceStateUpdates(iface, transaction, ifaceBuilder, opstateModified, interfaceName,
                flowCapableNodeConnectorNew.getName(), operStatusNew);

        // start/stop monitoring based on opState
        if (isTunnelInterface(iface) && opstateModified) {
            handleTunnelMonitoringUpdates(iface.getAugmentation(IfTunnel.class), iface.getName(), operStatusNew);
        }

        return Collections.singletonList(transaction.submit());
    }

    public void updateInterfaceStateOnNodeRemove(String interfaceName,
            FlowCapableNodeConnector flowCapableNodeConnector, WriteTransaction transaction) {
        LOG.debug("Updating interface oper-status to UNKNOWN for : {}", interfaceName);

        InterfaceBuilder ifaceBuilder = new InterfaceBuilder();
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.interfaces.rev140508.interfaces.Interface iface = interfaceManagerCommonUtils
                .getInterfaceFromConfigDS(interfaceName);
        handleInterfaceStateUpdates(iface, transaction, ifaceBuilder, true, interfaceName,
                flowCapableNodeConnector.getName(), Interface.OperStatus.Unknown);
        if (InterfaceManagerCommonUtils.isTunnelInterface(iface)) {
            handleTunnelMonitoringUpdates(iface.getAugmentation(IfTunnel.class), interfaceName,
                    Interface.OperStatus.Unknown);
        }
    }

    public static void handleInterfaceStateUpdates(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.interfaces.rev140508.interfaces.Interface iface,
            WriteTransaction transaction, InterfaceBuilder ifaceBuilder, boolean opStateModified,
            String interfaceName, String portName, Interface.OperStatus opState) {
        // if interface config DS is null, do the update only for the
        // lower-layer-interfaces
        // which have no corresponding config entries
        if (iface == null && !interfaceName.equals(portName)) {
            return;
        }
        LOG.debug("updating interface state entry for {}", interfaceName);
        InstanceIdentifier<Interface> ifStateId = IfmUtil.buildStateInterfaceId(interfaceName);
        ifaceBuilder.setKey(new InterfaceKey(interfaceName));
        if (modifyOpState(iface, opStateModified)) {
            LOG.debug("updating interface oper status as {} for {}", opState.name(), interfaceName);
            ifaceBuilder.setOperStatus(opState);
        }
        transaction.merge(LogicalDatastoreType.OPERATIONAL, ifStateId, ifaceBuilder.build(), false);
    }

    public void handleTunnelMonitoringUpdates(IfTunnel ifTunnel,
            String interfaceName, Interface.OperStatus operStatus) {
        LOG.debug("handling tunnel monitoring updates for {} due to opstate modification", interfaceName);
        if (operStatus == Interface.OperStatus.Down || operStatus == Interface.OperStatus.Unknown) {
            alivenessMonitorUtils.stopLLDPMonitoring(ifTunnel, interfaceName);
        } else {
            alivenessMonitorUtils.startLLDPMonitoring(ifTunnel, interfaceName);
        }
    }

    public static boolean modifyOpState(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
                .interfaces.rev140508.interfaces.Interface iface,
            boolean opStateModified) {
        return opStateModified && (iface == null || iface.isEnabled());
    }

    public static boolean isTunnelInterface(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.interfaces.rev140508.interfaces.Interface iface) {
        return iface != null && iface.getAugmentation(IfTunnel.class) != null;
    }

    public static boolean modifyTunnelOpState(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.interfaces.rev140508.interfaces.Interface iface,
            boolean opStateModified) {
        if (!iface.getAugmentation(IfTunnel.class).isMonitorEnabled()) {
            return modifyOpState(iface, opStateModified);
        }
        return false;
    }
}
