/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.scaling.renderer.ovs.statehelpers;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collections;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.DpnTepInterfaceInfo;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.ItmScaleUtils;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.TunnelUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelListKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsInterfaceStateUpdateHelper {
    private static final Logger LOG = LoggerFactory.getLogger(OvsInterfaceStateUpdateHelper.class);

    public static List<ListenableFuture<Void>> updateState(InstanceIdentifier<FlowCapableNodeConnector> key,
                                                           DataBroker dataBroker, String interfaceName,
                                                           FlowCapableNodeConnector flowCapableNodeConnectorNew,
                                                           FlowCapableNodeConnector flowCapableNodeConnectorOld) {
        LOG.debug("Updating interface state for port: {}", interfaceName);

        // SF 419 Hardware updates can be ignored
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
            return Collections.emptyList();
        }

        DpnTepInterfaceInfo dpnTepInfo = TunnelUtils.getTunnelFromConfigDS(interfaceName, dataBroker);

        // For monitoring enabled tunnels, skip opstate updation
        if( !modifyTunnelOpState(dpnTepInfo, opstateModified)){
            LOG.debug("skip Tunnel-state updation for monitoring enabled tunnel interface {}", interfaceName);
            opstateModified = false;
        }

        if (!opstateModified && !hardwareAddressModified) {
            LOG.debug("If State entry for port: {} Not Modified.", interfaceName);
            return Collections.emptyList();
        }

        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        // modify the attributes in interface operational DS
        handleInterfaceStateUpdates(dpnTepInfo, transaction, dataBroker,
                        opstateModified, interfaceName, flowCapableNodeConnectorNew.getName(), operStatusNew);

        return Collections.singletonList(transaction.submit());
    }

    public static void updateInterfaceStateOnNodeRemove(String interfaceName,
                                                        FlowCapableNodeConnector flowCapableNodeConnector,
                                                        DataBroker dataBroker, WriteTransaction transaction){
        LOG.debug("Updating interface oper-status to UNKNOWN for : {}", interfaceName);
        DpnTepInterfaceInfo dpnTepInfo = TunnelUtils.getTunnelFromConfigDS(interfaceName, dataBroker);

        handleInterfaceStateUpdates(dpnTepInfo,transaction, dataBroker,
                        true, interfaceName, flowCapableNodeConnector.getName(),
                        Interface.OperStatus.Unknown);
    }

    public static Interface.OperStatus getOpState(FlowCapableNodeConnector flowCapableNodeConnector){
        Interface.OperStatus operStatus =
                (flowCapableNodeConnector.getState().isLive() &&
                        !flowCapableNodeConnector.getConfiguration().isPORTDOWN())
                        ? Interface.OperStatus.Up: Interface.OperStatus.Down;
        return operStatus;
    }

    public static void handleInterfaceStateUpdates(DpnTepInterfaceInfo dpnTepInfo,
                                                   WriteTransaction transaction, DataBroker dataBroker,
                                                   boolean opStateModified, String interfaceName,
                                                   String portName, Interface.OperStatus opState){
        if(dpnTepInfo == null && !interfaceName.equals(portName)) {
            return;
        }
        LOG.debug("updating interface state entry for {}", interfaceName);
        InstanceIdentifier<StateTunnelList> tnlStateId = ItmUtils.buildStateTunnelListId(
                                    new StateTunnelListKey(interfaceName));
        StateTunnelListBuilder stateTnlBuilder = new StateTunnelListBuilder();
        stateTnlBuilder.setKey(new StateTunnelListKey(interfaceName));
        if (modifyOpState(dpnTepInfo, opStateModified)) {
            LOG.debug("updating interface oper status as {} for {}", opState.name(), interfaceName);
            boolean tunnelState = (opState.equals(Interface.OperStatus.Up)) ? (true):(false);
            stateTnlBuilder.setTunnelState(tunnelState);
            stateTnlBuilder.setOperState(ItmScaleUtils.convertInterfaceToTunnelOperState(opState));
        }
        transaction.merge(LogicalDatastoreType.OPERATIONAL, tnlStateId, stateTnlBuilder.build(), false);
    }

    public static boolean modifyOpState(DpnTepInterfaceInfo dpnTepInterfaceInfo,
                                        boolean opStateModified){
        return (opStateModified && (dpnTepInterfaceInfo == null
                || dpnTepInterfaceInfo != null && dpnTepInterfaceInfo.isMonitorEnabled()));
    }

    public static boolean modifyTunnelOpState(DpnTepInterfaceInfo dpnTepInterfaceInfo,
                                              boolean opStateModified){
        if (!dpnTepInterfaceInfo.isMonitorEnabled()) {
            return modifyOpState(dpnTepInterfaceInfo, opStateModified);
        }
        return false;
    }
}
