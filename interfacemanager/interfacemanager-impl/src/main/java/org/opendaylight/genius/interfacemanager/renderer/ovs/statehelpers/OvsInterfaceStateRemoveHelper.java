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
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.commons.AlivenessMonitorUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.AlivenessMonitorService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class OvsInterfaceStateRemoveHelper {
    private static final Logger LOG = LoggerFactory.getLogger(OvsInterfaceStateRemoveHelper.class);

    public static List<ListenableFuture<Void>> removeState(IdManagerService idManager, IMdsalApiManager mdsalApiManager,
                                                           AlivenessMonitorService alivenessMonitorService,
                                                           InstanceIdentifier<FlowCapableNodeConnector> key,
                                                           DataBroker dataBroker, String portName, FlowCapableNodeConnector fcNodeConnectorOld) {
        LOG.debug("Removing interface-state for port: {}", portName);
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();

        InstanceIdentifier<Interface> ifStateId = IfmUtil.buildStateInterfaceId(portName);

        // delete the port entry from interface operational DS
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface interfaceState =
                InterfaceManagerCommonUtils.getInterfaceStateFromOperDS(portName, dataBroker);
        transaction.delete(LogicalDatastoreType.OPERATIONAL, ifStateId);

        InterfaceKey interfaceKey = new InterfaceKey(portName);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface iface =
                InterfaceManagerCommonUtils.getInterfaceFromConfigDS(interfaceKey, dataBroker);

        NodeConnectorId nodeConnectorId = InstanceIdentifier.keyOf(key.firstIdentifierOf(NodeConnector.class)).getId();
        BigInteger dpId = new BigInteger(IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId));
        // If this interface is a tunnel interface, remove the tunnel ingress flow and stop lldp monitoring
        if(iface != null) {
            IfTunnel tunnel = iface.getAugmentation(IfTunnel.class);
            if (tunnel != null) {
                long portNo = Long.valueOf(IfmUtil.getPortNoFromNodeConnectorId(nodeConnectorId));
                InterfaceManagerCommonUtils.makeTunnelIngressFlow(futures, mdsalApiManager, tunnel, dpId, portNo, iface, -1,
                        NwConstants.DEL_FLOW);
                futures.add(transaction.submit());
                AlivenessMonitorUtils.stopLLDPMonitoring(alivenessMonitorService, dataBroker, iface);
                return futures;
            }
        }

        InterfaceParentEntryKey interfaceParentEntryKey = new InterfaceParentEntryKey(portName);
        InterfaceParentEntry interfaceParentEntry =
                InterfaceMetaUtils.getInterfaceParentEntryFromConfigDS(interfaceParentEntryKey, dataBroker);
        if (interfaceParentEntry == null || interfaceParentEntry.getInterfaceChildEntry() == null) {
            futures.add(transaction.submit());
            return futures;
        }

        //FIXME: If the no. of child entries exceeds 100, perform txn updates in batches of 100.
        InterfaceChildEntry higherlayerChild = interfaceParentEntry.getInterfaceChildEntry().get(0);
        InstanceIdentifier<Interface>
                higerLayerChildIfStateId = IfmUtil.buildStateInterfaceId(higherlayerChild.getChildInterface());
        Interface higherLayerIfChildState = InterfaceManagerCommonUtils.getInterfaceStateFromOperDS(higherlayerChild.getChildInterface(), dataBroker);
        if (interfaceState != null && higherLayerIfChildState != null) {
            transaction.delete(LogicalDatastoreType.OPERATIONAL, higerLayerChildIfStateId);
            FlowBasedServicesUtils.removeIngressFlow(higherLayerIfChildState.getName(), dpId, transaction);
        }
        // If this interface maps to a Vlan trunk entity, operational states of all the vlan-trunk-members
        // should also be created here.
        InterfaceParentEntryKey higherLayerParentEntryKey = new InterfaceParentEntryKey(higherlayerChild.getChildInterface());
        InterfaceParentEntry higherLayerParent =
                InterfaceMetaUtils.getInterfaceParentEntryFromConfigDS(higherLayerParentEntryKey, dataBroker);

        if(higherLayerParent != null && higherLayerParent.getInterfaceChildEntry() != null) {
            for (InterfaceChildEntry interfaceChildEntry : higherLayerParent.getInterfaceChildEntry()) {
                InstanceIdentifier<Interface> ifChildStateId =
                        IfmUtil.buildStateInterfaceId(interfaceChildEntry.getChildInterface());
                Interface childInterfaceState = InterfaceManagerCommonUtils.getInterfaceStateFromOperDS(interfaceChildEntry.getChildInterface(), dataBroker);
                if (childInterfaceState != null) {
                    transaction.delete(LogicalDatastoreType.OPERATIONAL, ifChildStateId);
                    FlowBasedServicesUtils.removeIngressFlow(childInterfaceState.getName(), dpId, transaction);
                }
            }
        }


       /* Below code will be needed if we want to update the vlan-trunk in the of-port.
       NodeConnectorId nodeConnectorId = InstanceIdentifier.keyOf(key.firstIdentifierOf(NodeConnector.class)).getId();
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

        InstanceIdentifier<?> bridgeIid = bridgeRefEntry.getBridgeReference().getValue();
        InstanceIdentifier<TerminationPoint> tpIid = SouthboundUtils.createTerminationPointInstanceIdentifier(
                InstanceIdentifier.keyOf(bridgeIid.firstIdentifierOf(Node.class)), interfaceOld.getName());
        t.delete(LogicalDatastoreType.CONFIGURATION, tpIid); */

        futures.add(transaction.submit());
        return futures;
    }
}