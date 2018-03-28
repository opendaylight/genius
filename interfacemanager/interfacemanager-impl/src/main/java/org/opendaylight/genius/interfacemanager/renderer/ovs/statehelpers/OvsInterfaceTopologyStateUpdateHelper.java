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
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.InterfacemgrProvider;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.genius.interfacemanager.renderer.ovs.utilities.SouthboundUtils;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge._interface.info.BridgeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceBfdStatus;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class OvsInterfaceTopologyStateUpdateHelper {
    private static final Logger LOG = LoggerFactory.getLogger(OvsInterfaceTopologyStateUpdateHelper.class);

    private final DataBroker dataBroker;
    private final EntityOwnershipUtils entityOwnershipUtils;
    private final JobCoordinator coordinator;
    private final InterfaceManagerCommonUtils interfaceManagerCommonUtils;
    private final InterfaceMetaUtils interfaceMetaUtils;
    private final SouthboundUtils southboundUtils;
    private final InterfacemgrProvider interfacemgrProvider;

    @Inject
    public OvsInterfaceTopologyStateUpdateHelper(DataBroker dataBroker, EntityOwnershipUtils entityOwnershipUtils,
            JobCoordinator coordinator, InterfaceManagerCommonUtils interfaceManagerCommonUtils,
            InterfaceMetaUtils interfaceMetaUtils, SouthboundUtils southboundUtils,
            InterfacemgrProvider interfacemgrProvider) {
        this.dataBroker = dataBroker;
        this.entityOwnershipUtils = entityOwnershipUtils;
        this.coordinator = coordinator;
        this.interfaceManagerCommonUtils = interfaceManagerCommonUtils;
        this.interfaceMetaUtils = interfaceMetaUtils;
        this.southboundUtils = southboundUtils;
        this.interfacemgrProvider = interfacemgrProvider;
    }

    /*
     * This code is used to handle only a dpnId change scenario for a particular
     * change, which is not expected to happen in usual cases.
     */
    public List<ListenableFuture<Void>> updateBridgeRefEntry(InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIid,
                                                             OvsdbBridgeAugmentation bridgeNew,
                                                             OvsdbBridgeAugmentation bridgeOld) {
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
        BridgeEntry bridgeEntry = interfaceMetaUtils.getBridgeEntryFromConfigDS(dpnIdNew);
        if (bridgeEntry == null) {
            futures.add(writeTransaction.submit());
            return futures;
        }
        southboundUtils.addAllPortsToBridge(bridgeEntry, interfaceManagerCommonUtils, bridgeIid, bridgeNew);

        futures.add(writeTransaction.submit());
        return futures;
    }

    public List<ListenableFuture<Void>> updateTunnelState(OvsdbTerminationPointAugmentation terminationPointNew) {
        final Interface.OperStatus interfaceBfdStatus = getTunnelOpState(terminationPointNew);
        final String interfaceName = terminationPointNew.getName();
        interfaceManagerCommonUtils.addBfdStateToCache(interfaceName, interfaceBfdStatus);
        if (!entityOwnershipUtils.isEntityOwner(IfmConstants.INTERFACE_CONFIG_ENTITY,
                IfmConstants.INTERFACE_CONFIG_ENTITY)) {
            return Collections.emptyList();
        }

        coordinator.enqueueJob(interfaceName, () -> {
            // update opstate of interface if TEP has gone down/up as a result
            // of BFD monitoring
            final Interface interfaceState = interfaceManagerCommonUtils
                    .getInterfaceStateFromOperDS(terminationPointNew.getName());
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

    private static Interface.OperStatus getTunnelOpState(OvsdbTerminationPointAugmentation terminationPoint) {
        if (!SouthboundUtils.bfdMonitoringEnabled(terminationPoint.getInterfaceBfd())) {
            return Interface.OperStatus.Up;
        }
        InterfaceBfdStatus tunnelBfdStatus = terminationPoint.getInterfaceBfdStatus().get(0);
        if (tunnelBfdStatus != null) {
            return SouthboundUtils.BFD_STATE_UP.equalsIgnoreCase(tunnelBfdStatus.getBfdStatusValue())
                    && SouthboundUtils.BFD_OP_STATE.equalsIgnoreCase(tunnelBfdStatus.getBfdStatusKey())
                    ? Interface.OperStatus.Up : Interface.OperStatus.Down;
        }
        return Interface.OperStatus.Down;
    }
}
