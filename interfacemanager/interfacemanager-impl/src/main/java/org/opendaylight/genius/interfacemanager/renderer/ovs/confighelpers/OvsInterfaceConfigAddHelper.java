/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.renderer.ovs.confighelpers;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.commons.AlivenessMonitorUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.genius.interfacemanager.renderer.ovs.utilities.SouthboundUtils;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.AlivenessMonitorService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge._interface.info.BridgeEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge._interface.info.bridge.entry.BridgeInterfaceEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge.ref.info.BridgeRefEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge.ref.info.BridgeRefEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class OvsInterfaceConfigAddHelper {
    private static final Logger LOG = LoggerFactory.getLogger(OvsInterfaceConfigAddHelper.class);

    public static List<ListenableFuture<Void>> addConfiguration(DataBroker dataBroker, ParentRefs parentRefs,
                                                                Interface interfaceNew, IdManagerService idManager,
                                                                AlivenessMonitorService alivenessMonitorService,
                                                                IMdsalApiManager mdsalApiManager) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();

        IfTunnel ifTunnel = interfaceNew.getAugmentation(IfTunnel.class);
        if (ifTunnel != null) {
            addTunnelConfiguration(dataBroker, parentRefs, interfaceNew, idManager, alivenessMonitorService,
                    mdsalApiManager, futures);
            return futures;
        }

        addVlanConfiguration(interfaceNew, parentRefs, dataBroker, idManager, futures);
        return futures;
    }

    private static void addVlanConfiguration(Interface interfaceNew, ParentRefs parentRefs, DataBroker dataBroker, IdManagerService idManager,
                                             List<ListenableFuture<Void>> futures) {
        IfL2vlan ifL2vlan = interfaceNew.getAugmentation(IfL2vlan.class);
        if (ifL2vlan == null || (IfL2vlan.L2vlanMode.Trunk != ifL2vlan.getL2vlanMode() && IfL2vlan.L2vlanMode.Transparent != ifL2vlan.getL2vlanMode())) {
            return;
        }
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        if(!InterfaceManagerCommonUtils.createInterfaceChildEntryIfNotPresent(dataBroker, transaction,
                parentRefs.getParentInterface(), interfaceNew.getName())){
            return;
        }
        LOG.debug("adding vlan configuration for {}",interfaceNew.getName());

        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface ifState =
                InterfaceManagerCommonUtils.getInterfaceStateFromOperDS(parentRefs.getParentInterface(), dataBroker);

        if (ifState == null) {
            LOG.debug("could not retrieve interface state corresponding to {}",interfaceNew.getName());
            futures.add(transaction.submit());
            return;
        }

        InterfaceManagerCommonUtils.addStateEntry(interfaceNew.getName(), transaction, dataBroker, idManager, ifState);

        InterfaceParentEntryKey interfaceParentEntryKey = new InterfaceParentEntryKey(interfaceNew.getName());
        InterfaceParentEntry interfaceParentEntry =
                InterfaceMetaUtils.getInterfaceParentEntryFromConfigDS(interfaceParentEntryKey, dataBroker);
        if (interfaceParentEntry == null || interfaceParentEntry.getInterfaceChildEntry() == null) {
            LOG.debug("could not retrieve interface parent info for {}",interfaceNew.getName());
            futures.add(transaction.submit());
            return;
        }

        //FIXME: If the no. of child entries exceeds 100, perform txn updates in batches of 100.
        for (InterfaceChildEntry interfaceChildEntry : interfaceParentEntry.getInterfaceChildEntry()) {
            InterfaceManagerCommonUtils.addStateEntry(interfaceChildEntry.getChildInterface(), transaction, dataBroker, idManager,ifState);
        }
        futures.add(transaction.submit());
    }

    private static void addTunnelConfiguration(DataBroker dataBroker, ParentRefs parentRefs,
                                               Interface interfaceNew, IdManagerService idManager,
                                               AlivenessMonitorService alivenessMonitorService,
                                               IMdsalApiManager mdsalApiManager,
                                               List<ListenableFuture<Void>> futures) {
        LOG.debug("adding tunnel configuration for {}", interfaceNew.getName());
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        if (parentRefs == null) {
            LOG.warn("ParentRefs for interface: {} Not Found. " +
                    "Creation of Tunnel OF-Port not supported when dpid not provided.", interfaceNew.getName());
            return;
        }

        BigInteger dpId = parentRefs.getDatapathNodeIdentifier();
        if (dpId == null) {
            LOG.warn("dpid for interface: {} Not Found. No DPID provided. " +
                    "Creation of OF-Port not supported.", interfaceNew.getName());
            return;
        }

        BridgeEntryKey bridgeEntryKey = new BridgeEntryKey(dpId);
        BridgeInterfaceEntryKey bridgeInterfaceEntryKey = new BridgeInterfaceEntryKey(interfaceNew.getName());

        LOG.debug("creating bridge interfaceEntry in ConfigDS {}", bridgeEntryKey);
        InterfaceMetaUtils.createBridgeInterfaceEntryInConfigDS(bridgeEntryKey, bridgeInterfaceEntryKey,
                interfaceNew.getName(), transaction);
        futures.add(transaction.submit());

        // create bridge on switch, if switch is connected
        BridgeRefEntryKey BridgeRefEntryKey = new BridgeRefEntryKey(dpId);
        InstanceIdentifier<BridgeRefEntry> dpnBridgeEntryIid =
                InterfaceMetaUtils.getBridgeRefEntryIdentifier(BridgeRefEntryKey);
        BridgeRefEntry bridgeRefEntry =
                InterfaceMetaUtils.getBridgeRefEntryFromOperDS(dpnBridgeEntryIid, dataBroker);
        if(bridgeRefEntry != null && bridgeRefEntry.getBridgeReference() != null) {
            LOG.debug("creating bridge interface on dpn {}", bridgeEntryKey);
            InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIid =
                    (InstanceIdentifier<OvsdbBridgeAugmentation>) bridgeRefEntry.getBridgeReference().getValue();
            Optional<OvsdbBridgeAugmentation> bridgeNodeOptional =
                    IfmUtil.read(LogicalDatastoreType.OPERATIONAL, bridgeIid, dataBroker);
            if (bridgeNodeOptional.isPresent()) {
                OvsdbBridgeAugmentation ovsdbBridgeAugmentation = bridgeNodeOptional.get();
                String bridgeName = ovsdbBridgeAugmentation.getBridgeName().getValue();
                SouthboundUtils.addPortToBridge(bridgeIid, interfaceNew,
                        ovsdbBridgeAugmentation, bridgeName, interfaceNew.getName(), dataBroker, futures);

                // if TEP is already configured on switch, start LLDP monitoring and program tunnel ingress flow
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface ifState =
                        InterfaceManagerCommonUtils.getInterfaceStateFromOperDS(interfaceNew.getName(), dataBroker);
                if(ifState != null){
                    NodeConnectorId ncId = IfmUtil.getNodeConnectorIdFromInterface(ifState);
                    if(ncId != null) {
                        long portNo = Long.valueOf(IfmUtil.getPortNoFromNodeConnectorId(ncId));
                        IfTunnel ifTunnel = interfaceNew.getAugmentation(IfTunnel.class);
                        InterfaceManagerCommonUtils.makeTunnelIngressFlow(futures, mdsalApiManager, ifTunnel,
                                dpId, portNo, interfaceNew.getName(),
                                ifState.getIfIndex(), NwConstants.ADD_FLOW);
                        // start LLDP monitoring for the tunnel interface
                        AlivenessMonitorUtils.startLLDPMonitoring(alivenessMonitorService, dataBroker,
                                ifTunnel, interfaceNew.getName());
                    }
                }
            }
        }
    }
}
