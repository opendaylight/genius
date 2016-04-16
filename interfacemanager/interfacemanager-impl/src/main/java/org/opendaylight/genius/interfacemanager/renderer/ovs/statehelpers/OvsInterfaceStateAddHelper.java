/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
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
                                                        IMdsalApiManager mdsalApiManager, AlivenessMonitorService alivenessMonitorService,
                                                        NodeConnectorId nodeConnectorId, String portName, FlowCapableNodeConnector fcNodeConnectorNew) {
        LOG.debug("Adding Interface State to Oper DS for port: {}", portName);
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();

        //Retrieve PbyAddress & OperState from the DataObject
        PhysAddress physAddress = new PhysAddress(fcNodeConnectorNew.getHardwareAddress().getValue());
        /*FIXME
        State state = fcNodeConnectorNew.getState();
        Interface.OperStatus operStatus =
                fcNodeConnectorNew == null ? Interface.OperStatus.Down : Interface.OperStatus.Up;
        Interface.AdminStatus adminStatus = state.isBlocked() ? Interface.AdminStatus.Down : Interface.AdminStatus.Up;
        */
        Interface.OperStatus operStatus = Interface.OperStatus.Up;
        Interface.AdminStatus adminStatus = Interface.AdminStatus.Up;

        // Fetch the interface name corresponding to the port Name
        InterfaceKey interfaceKey = new InterfaceKey(portName);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface iface =
                InterfaceManagerCommonUtils.getInterfaceFromConfigDS(interfaceKey, dataBroker);

        Interface ifState = InterfaceManagerCommonUtils.addStateEntry(iface, portName, transaction, idManager,
                physAddress, operStatus, adminStatus, nodeConnectorId);
        // If this interface is a tunnel interface, create the tunnel ingress flow
        if(iface != null) {
            IfTunnel tunnel = iface.getAugmentation(IfTunnel.class);
            if (tunnel != null) {
                BigInteger dpId = new BigInteger(IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId));
                long portNo = Long.valueOf(IfmUtil.getPortNoFromNodeConnectorId(nodeConnectorId));
                InterfaceManagerCommonUtils.makeTunnelIngressFlow(futures, mdsalApiManager, tunnel, dpId, portNo, iface,
                        ifState.getIfIndex(), NwConstants.ADD_FLOW);
                futures.add(transaction.submit());
                AlivenessMonitorUtils.startLLDPMonitoring(alivenessMonitorService, dataBroker, iface);
                return futures;
            }
        }

        // For all other interfaces except tunnel interfaces, interface name won't be same as port name.
        // In that case fetch the interface corresponding to the portName, and update the state accordingly
        InterfaceParentEntryKey interfaceParentEntryKey = new InterfaceParentEntryKey(portName);
        InterfaceParentEntry interfaceParentEntry =
                InterfaceMetaUtils.getInterfaceParentEntryFromConfigDS(interfaceParentEntryKey, dataBroker);
        if (interfaceParentEntry == null || interfaceParentEntry.getInterfaceChildEntry() == null) {
            futures.add(transaction.submit());
            return futures;
        }

        //FIXME: If the no. of child entries exceeds 100, perform txn updates in batches of 100.
        //List<Trunks> trunks = new ArrayList<>();

        String higherlayerChild = interfaceParentEntry.getInterfaceChildEntry().get(0).getChildInterface();
        InterfaceManagerCommonUtils.addStateEntry(higherlayerChild, transaction, dataBroker, idManager,
                ifState);

        // If this interface maps to a Vlan trunk entity, operational states of all the vlan-trunk-members
        // should also be created here.
        InterfaceParentEntryKey higherLayerParentEntryKey = new InterfaceParentEntryKey(higherlayerChild);
        InterfaceParentEntry higherLayerParent =
                InterfaceMetaUtils.getInterfaceParentEntryFromConfigDS(higherLayerParentEntryKey, dataBroker);
        if(higherLayerParent != null && higherLayerParent.getInterfaceChildEntry() != null) {
            for (InterfaceChildEntry interfaceChildEntry : higherLayerParent.getInterfaceChildEntry()){
                InterfaceManagerCommonUtils.addStateEntry(interfaceChildEntry.getChildInterface(), transaction, dataBroker, idManager,
                        ifState);
            }
        }
        /** Below code will be needed if we want to update the vlan-trunks on the of-port
         if (trunks.isEmpty()) {
         futures.add(t.submit());
         return futures;
         }

         BigInteger dpId = new BigInteger(IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId));

         BridgeRefEntryKey BridgeRefEntryKey = new BridgeRefEntryKey(dpId);
         InstanceIdentifier<BridgeRefEntry> dpnBridgeEntryIid =
         InterfaceMetaUtils.getBridgeRefEntryIdentifier(BridgeRefEntryKey);
         BridgeRefEntry bridgeRefEntry =
         InterfaceMetaUtils.getBridgeRefEntryFromOperDS(dpnBridgeEntryIid, dataBroker);
         if (bridgeRefEntry == null) {
         futures.add(t.submit());
         return futures;
         }

         InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIid =
         (InstanceIdentifier<OvsdbBridgeAugmentation>)bridgeRefEntry.getBridgeReference().getValue();
         VlanTrunkSouthboundUtils.addTerminationPointWithTrunks(bridgeIid, trunks, iface.getName(), t);
         */

        futures.add(transaction.submit());
        return futures;
    }
}