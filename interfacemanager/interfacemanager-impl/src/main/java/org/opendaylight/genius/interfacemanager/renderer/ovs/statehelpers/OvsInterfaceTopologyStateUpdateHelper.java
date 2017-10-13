/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.renderer.ovs.statehelpers;

import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.genius.interfacemanager.renderer.ovs.utilities.SouthboundUtils;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge._interface.info.BridgeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceBfdStatus;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsInterfaceTopologyStateUpdateHelper {
    private static final Logger LOG = LoggerFactory.getLogger(OvsInterfaceTopologyStateUpdateHelper.class);

    /*
     * This code is used to handle only a dpnId change scenario for a particular
     * change, which is not expected to happen in usual cases.
     */
    public static List<ListenableFuture<Void>> updateBridgeRefEntry(
            InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIid, OvsdbBridgeAugmentation bridgeNew,
            OvsdbBridgeAugmentation bridgeOld, DataBroker dataBroker) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();

        BigInteger dpnIdNew = IfmUtil.getDpnId(bridgeNew.getDatapathId());
        BigInteger dpnIdOld = IfmUtil.getDpnId(bridgeOld.getDatapathId());

        LOG.debug("updating bridge references for bridge: {}, dpnNew: {}, dpnOld: {}", bridgeNew,
                dpnIdNew, dpnIdOld);
        // delete bridge reference entry for the old dpn in interface meta
        // operational DS
        InterfaceMetaUtils.deleteBridgeRefEntry(dpnIdOld, writeTransaction);

        // create bridge reference entry in interface meta operational DS
        InterfaceMetaUtils.createBridgeRefEntry(dpnIdNew, bridgeIid, writeTransaction);

        // handle pre-provisioning of tunnels for the newly connected dpn
        BridgeEntry bridgeEntry = InterfaceMetaUtils.getBridgeEntryFromConfigDS(dpnIdNew, dataBroker);
        if (bridgeEntry == null) {
            futures.add(writeTransaction.submit());
            return futures;
        }
        SouthboundUtils.addAllPortsToBridge(bridgeEntry, dataBroker, bridgeIid, bridgeNew);

        futures.add(writeTransaction.submit());
        return futures;
    }

    public static List<ListenableFuture<Void>> updateTunnelState(final DataBroker dataBroker,
            EntityOwnershipUtils entityOwnershipUtils, OvsdbTerminationPointAugmentation terminationPointNew) {
        final Interface.OperStatus interfaceBfdStatus = getTunnelOpState(terminationPointNew.getInterfaceBfdStatus());
        final String interfaceName = terminationPointNew.getName();
        InterfaceManagerCommonUtils.addBfdStateToCache(interfaceName, interfaceBfdStatus);
        if (!entityOwnershipUtils.isEntityOwner(IfmConstants.INTERFACE_CONFIG_ENTITY,
                IfmConstants.INTERFACE_CONFIG_ENTITY)) {
            return Collections.emptyList();
        }

        DataStoreJobCoordinator jobCoordinator = DataStoreJobCoordinator.getInstance();
        jobCoordinator.enqueueJob(interfaceName, () -> {
            // update opstate of interface if TEP has gone down/up as a result
            // of BFD monitoring
            final Interface interfaceState = InterfaceManagerCommonUtils
                    .getInterfaceStateFromOperDS(terminationPointNew.getName(), dataBroker);
            if (interfaceState != null && interfaceState.getOperStatus() != Interface.OperStatus.Unknown
                    && interfaceState.getOperStatus() != interfaceBfdStatus) {
                LOG.debug("updating tunnel state for interface {} as {}", interfaceName, interfaceBfdStatus);
                WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
                InterfaceManagerCommonUtils.updateOpState(transaction, interfaceName, interfaceBfdStatus);
                return Collections.singletonList(transaction.submit());
            }
            return Collections.emptyList();
        });
        return Collections.emptyList();
    }

    private static Interface.OperStatus getTunnelOpState(List<InterfaceBfdStatus> tunnelBfdStatus) {
        Interface.OperStatus livenessState = Interface.OperStatus.Down;
        if (tunnelBfdStatus != null && !tunnelBfdStatus.isEmpty()) {
            for (InterfaceBfdStatus bfdState : tunnelBfdStatus) {
                if (bfdState.getBfdStatusKey().equalsIgnoreCase(SouthboundUtils.BFD_OP_STATE)) {
                    String bfdOpState = bfdState.getBfdStatusValue();
                    livenessState = SouthboundUtils.BFD_STATE_UP.equalsIgnoreCase(bfdOpState) ? Interface.OperStatus.Up
                            : Interface.OperStatus.Down;
                    break;
                }
            }
        }
        return livenessState;
    }
}
