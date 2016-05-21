/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.renderer.ovs.confighelpers;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.commons.AlivenessMonitorUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.genius.interfacemanager.renderer.ovs.utilities.SouthboundUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.AlivenessMonitorService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge._interface.info.BridgeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge._interface.info.BridgeEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge._interface.info.bridge.entry.BridgeInterfaceEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge._interface.info.bridge.entry.BridgeInterfaceEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge.ref.info.BridgeRefEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge.ref.info.BridgeRefEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class OvsInterfaceConfigRemoveHelper {
    private static final Logger LOG = LoggerFactory.getLogger(OvsInterfaceConfigRemoveHelper.class);

    public static List<ListenableFuture<Void>> removeConfiguration(DataBroker dataBroker, AlivenessMonitorService alivenessMonitorService,
                                                                   Interface interfaceOld,
                                                                   IdManagerService idManager,
                                                                   IMdsalApiManager mdsalApiManager,
                                                                   ParentRefs parentRefs) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        WriteTransaction t = dataBroker.newWriteOnlyTransaction();

        IfTunnel ifTunnel = interfaceOld.getAugmentation(IfTunnel.class);
        if (ifTunnel != null) {
            removeTunnelConfiguration(alivenessMonitorService, parentRefs, dataBroker, interfaceOld,
                    idManager, mdsalApiManager, futures);
        }else {
            removeVlanConfiguration(dataBroker, parentRefs, interfaceOld, t, idManager);
            futures.add(t.submit());
        }
        return futures;
    }

    private static void removeVlanConfiguration(DataBroker dataBroker, ParentRefs parentRefs, Interface interfaceOld,
                                                WriteTransaction transaction, IdManagerService idManagerService) {
        IfL2vlan ifL2vlan = interfaceOld.getAugmentation(IfL2vlan.class);
        if (parentRefs == null || ifL2vlan == null ||
                (IfL2vlan.L2vlanMode.Trunk != ifL2vlan.getL2vlanMode() &&
                        IfL2vlan.L2vlanMode.Transparent != ifL2vlan.getL2vlanMode())) {
            return;
        }
        LOG.debug("removing vlan configuration for {}",interfaceOld.getName());
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface ifState =
                InterfaceManagerCommonUtils.getInterfaceStateFromOperDS(interfaceOld.getName(), dataBroker);
        if (ifState == null) {
            LOG.debug("could not fetch interface state corresponding to {}",interfaceOld.getName());
            //return;
        }

        InterfaceManagerCommonUtils.deleteInterfaceStateInformation(interfaceOld.getName(), transaction, idManagerService);

        BigInteger dpId = IfmUtil.getDpnFromInterface(ifState);
        FlowBasedServicesUtils.removeIngressFlow(interfaceOld.getName(), dpId, transaction);
        InterfaceManagerCommonUtils.deleteParentInterfaceEntry(transaction, parentRefs.getParentInterface());
        // For Vlan-Trunk Interface, remove the trunk-member operstates as well...

        InterfaceParentEntry interfaceParentEntry =
                InterfaceMetaUtils.getInterfaceParentEntryFromConfigDS(interfaceOld.getName(), dataBroker);
        if (interfaceParentEntry == null || interfaceParentEntry.getInterfaceChildEntry() == null) {
            return;
        }

        //FIXME: If the no. of child entries exceeds 100, perform txn updates in batches of 100.
        for (InterfaceChildEntry interfaceChildEntry : interfaceParentEntry.getInterfaceChildEntry()) {
            LOG.debug("removing interface state for  vlan trunk member {}",interfaceChildEntry.getChildInterface());
            InterfaceManagerCommonUtils.deleteInterfaceStateInformation(interfaceChildEntry.getChildInterface(), transaction, idManagerService);
            FlowBasedServicesUtils.removeIngressFlow(interfaceChildEntry.getChildInterface(), dpId, transaction);
        }
    }

    private static void removeTunnelConfiguration(AlivenessMonitorService alivenessMonitorService, ParentRefs parentRefs,
                                                  DataBroker dataBroker, Interface interfaceOld,
                                                  IdManagerService idManager, IMdsalApiManager mdsalApiManager,
                                                  List<ListenableFuture<Void>> futures) {
        LOG.debug("removing tunnel configuration for {}",interfaceOld.getName());
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        BigInteger dpId = null;
        if (parentRefs != null) {
            dpId = parentRefs.getDatapathNodeIdentifier();
        }

        if (dpId == null) {
            return;
        }

        BridgeRefEntry bridgeRefEntry =
                InterfaceMetaUtils.getBridgeRefEntryFromOperDS(dpId, dataBroker);
        if (bridgeRefEntry != null) {
            SouthboundUtils.removeTerminationEndPoint(futures, dataBroker, bridgeRefEntry.getBridgeReference().getValue(), interfaceOld.getName());
            // delete tunnel ingress flow
            removeTunnelIngressFlow(futures, dataBroker, interfaceOld, mdsalApiManager, dpId);
        }

        // delete bridge to tunnel interface mappings
        BridgeEntryKey bridgeEntryKey = new BridgeEntryKey(dpId);
        InstanceIdentifier<BridgeEntry> bridgeEntryIid = InterfaceMetaUtils.getBridgeEntryIdentifier(bridgeEntryKey);
        BridgeEntry bridgeEntry = InterfaceMetaUtils.getBridgeEntryFromConfigDS(bridgeEntryIid, dataBroker);
        if (bridgeEntry == null) {
            LOG.debug("Bridge Entry not present for dpn: {}", dpId);
            return;
        }

        List<BridgeInterfaceEntry> bridgeInterfaceEntries = bridgeEntry.getBridgeInterfaceEntry();
        if (bridgeInterfaceEntries == null) {
            LOG.debug("Bridge Interface Entries not present for dpn : {}", dpId);
            return;
        }
        InterfaceMetaUtils.deleteBridgeInterfaceEntry(bridgeEntryKey, bridgeInterfaceEntries, bridgeEntryIid, transaction, interfaceOld);
        InterfaceMetaUtils.removeLportTagInterfaceMap(idManager, transaction, interfaceOld.getName());
        futures.add(transaction.submit());
        // stop LLDP monitoring for the tunnel interface
        AlivenessMonitorUtils.stopLLDPMonitoring(alivenessMonitorService, dataBroker, interfaceOld);
    }

    public static void removeTunnelIngressFlow(List<ListenableFuture<Void>> futures,
                                               DataBroker dataBroker, Interface interfaceOld, IMdsalApiManager mdsalApiManager, BigInteger dpId){
        NodeConnectorId ncId = IfmUtil.getNodeConnectorIdFromInterface(interfaceOld, dataBroker);
        if(ncId == null){
            LOG.debug("Node Connector Id is null. Skipping remove tunnel ingress flow.");
        }else{
            long portNo = Long.valueOf(IfmUtil.getPortNoFromNodeConnectorId(ncId));
            InterfaceManagerCommonUtils.makeTunnelIngressFlow(futures, mdsalApiManager,
                    interfaceOld.getAugmentation(IfTunnel.class),
                    dpId, portNo, interfaceOld, -1,
                    NwConstants.DEL_FLOW);
        }
    }
}