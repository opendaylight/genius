/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
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
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class OvsVlanMemberConfigRemoveHelper {
    private static final Logger LOG = LoggerFactory.getLogger(OvsVlanMemberConfigRemoveHelper.class);
    public static List<ListenableFuture<Void>> removeConfiguration(DataBroker dataBroker, ParentRefs parentRefs,
                                                                   Interface interfaceOld, IfL2vlan ifL2vlan,
                                                                   IdManagerService idManager) {
        LOG.debug("remove vlan member configuration {}",interfaceOld.getName());
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        WriteTransaction t = dataBroker.newWriteOnlyTransaction();

        InterfaceParentEntryKey interfaceParentEntryKey = new InterfaceParentEntryKey(parentRefs.getParentInterface());
        InstanceIdentifier<InterfaceParentEntry> interfaceParentEntryIid =
                InterfaceMetaUtils.getInterfaceParentEntryIdentifier(interfaceParentEntryKey);
        InterfaceParentEntry interfaceParentEntry =
                InterfaceMetaUtils.getInterfaceParentEntryFromConfigDS(interfaceParentEntryIid, dataBroker);

        if(interfaceParentEntry == null){
            return futures;
        }

        //Delete the interface child information
        List<InterfaceChildEntry> interfaceChildEntries = interfaceParentEntry.getInterfaceChildEntry();
        InterfaceChildEntryKey interfaceChildEntryKey = new InterfaceChildEntryKey(interfaceOld.getName());
        InstanceIdentifier<InterfaceChildEntry> interfaceChildEntryIid =
                    InterfaceMetaUtils.getInterfaceChildEntryIdentifier(interfaceParentEntryKey, interfaceChildEntryKey);
        t.delete(LogicalDatastoreType.CONFIGURATION, interfaceChildEntryIid);
        //If this is the last child, remove the interface parent info as well.
        if (interfaceChildEntries.size() <= 1) {
            t.delete(LogicalDatastoreType.CONFIGURATION, interfaceParentEntryIid);
        }

        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface ifState =
                InterfaceManagerCommonUtils.getInterfaceStateFromOperDS(parentRefs.getParentInterface(), dataBroker);
        if (ifState != null) {
            /* FIXME -- The below code is needed if vlan-trunks should be updated in the of-port

            String lowerLayerIf = ifState.getLowerLayerIf().get(0);
            NodeConnectorId nodeConnectorId = new NodeConnectorId(lowerLayerIf);
            BigInteger dpId = new BigInteger(IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId));

            BridgeRefEntryKey BridgeRefEntryKey = new BridgeRefEntryKey(dpId);
            InstanceIdentifier<BridgeRefEntry> dpnBridgeEntryIid =
                    InterfaceMetaUtils.getBridgeRefEntryIdentifier(BridgeRefEntryKey);
            BridgeRefEntry bridgeRefEntry =
                    InterfaceMetaUtils.getBridgeRefEntryFromOperDS(dpnBridgeEntryIid, dataBroker);
            if (bridgeRefEntry != null) {
                InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIid =
                        (InstanceIdentifier<OvsdbBridgeAugmentation>)bridgeRefEntry.getBridgeReference().getValue();
                Optional<OvsdbBridgeAugmentation> bridgeNodeOptional =
                        IfmUtil.read(LogicalDatastoreType.OPERATIONAL, bridgeIid, dataBroker);
                if (bridgeNodeOptional.isPresent()) {
                    OvsdbBridgeAugmentation ovsdbBridgeAugmentation = bridgeNodeOptional.get();
                    String bridgeName = ovsdbBridgeAugmentation.getBridgeName().getValue();
                    VlanTrunkSouthboundUtils.updateVlanMemberInTrunk(bridgeIid, ifL2vlan,
                            ovsdbBridgeAugmentation, bridgeName, parentRefs.getParentInterface(), dataBroker, t);
                }
            } */

            LOG.debug("delete vlan member interface state {}",interfaceOld.getName());
            BigInteger dpId = IfmUtil.getDpnFromInterface(ifState);
            InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> ifStateId =
                    IfmUtil.buildStateInterfaceId(interfaceOld.getName());
            t.delete(LogicalDatastoreType.OPERATIONAL, ifStateId);
            FlowBasedServicesUtils.removeIngressFlow(interfaceOld.getName(), dpId, t);
        }

        futures.add(t.submit());
        return futures;
    }
}