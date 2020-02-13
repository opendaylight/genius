/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.renderer.ovs.statehelpers;

import static org.opendaylight.genius.infra.Datastore.OPERATIONAL;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.aries.blueprint.annotation.service.Reference;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.genius.interfacemanager.renderer.ovs.utilities.SouthboundUtils;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge._interface.info.BridgeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceBfdStatus;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class OvsInterfaceTopologyStateUpdateHelper {
    private static final Logger LOG = LoggerFactory.getLogger(OvsInterfaceTopologyStateUpdateHelper.class);
    private static final Logger EVENT_LOGGER = LoggerFactory.getLogger("GeniusEventLogger");

    private final ManagedNewTransactionRunner txRunner;
    private final EntityOwnershipUtils entityOwnershipUtils;
    private final JobCoordinator coordinator;
    private final InterfaceManagerCommonUtils interfaceManagerCommonUtils;
    private final InterfaceMetaUtils interfaceMetaUtils;
    private final SouthboundUtils southboundUtils;

    @Inject
    public OvsInterfaceTopologyStateUpdateHelper(@Reference DataBroker dataBroker,
                                                 EntityOwnershipUtils entityOwnershipUtils,
                                                 @Reference JobCoordinator coordinator,
                                                 InterfaceManagerCommonUtils interfaceManagerCommonUtils,
                                                 InterfaceMetaUtils interfaceMetaUtils,
                                                 SouthboundUtils southboundUtils) {
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.entityOwnershipUtils = entityOwnershipUtils;
        this.coordinator = coordinator;
        this.interfaceManagerCommonUtils = interfaceManagerCommonUtils;
        this.interfaceMetaUtils = interfaceMetaUtils;
        this.southboundUtils = southboundUtils;
    }

    /*
     * This code is used to handle only a dpnId change scenario for a particular
     * change, which is not expected to happen in usual cases.
     */
    public List<ListenableFuture<Void>> updateBridgeRefEntry(InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIid,
                                                             OvsdbBridgeAugmentation bridgeNew,
                                                             OvsdbBridgeAugmentation bridgeOld) {
        return Collections.singletonList(txRunner.callWithNewWriteOnlyTransactionAndSubmit(OPERATIONAL, tx -> {
            Uint64 dpnIdNew = IfmUtil.getDpnId(bridgeNew.getDatapathId());
            Uint64 dpnIdOld = IfmUtil.getDpnId(bridgeOld.getDatapathId());

            LOG.debug("updating bridge references for bridge: {}, dpnNew: {}, dpnOld: {}", bridgeNew,
                    dpnIdNew, dpnIdOld);
            // delete bridge reference entry for the old dpn in interface meta
            // operational DS
            InterfaceMetaUtils.deleteBridgeRefEntry(dpnIdOld, tx);

            // create bridge reference entry in interface meta operational DS
            InterfaceMetaUtils.createBridgeRefEntry(dpnIdNew, bridgeIid, tx);

            // handle pre-provisioning of tunnels for the newly connected dpn
            BridgeEntry bridgeEntry = interfaceMetaUtils.getBridgeEntryFromConfigDS(dpnIdNew);
            if (bridgeEntry != null) {
                southboundUtils.addAllPortsToBridge(bridgeEntry, interfaceManagerCommonUtils, bridgeIid, bridgeNew);
            }
        }));
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
                EVENT_LOGGER.debug("IFM-OvsInterfaceTopologyState,UPDATE {} to STATUS {}", interfaceName,
                        interfaceState.getOperStatus());
                LOG.debug("updating tunnel state for interface {} as {}", interfaceName, interfaceBfdStatus);
                return Collections.singletonList(txRunner.callWithNewWriteOnlyTransactionAndSubmit(OPERATIONAL,
                    tx -> InterfaceManagerCommonUtils.updateOpState(tx, interfaceName, interfaceBfdStatus)));
            }
            return Collections.emptyList();
        });
        return Collections.emptyList();
    }

    private static Interface.OperStatus getTunnelOpState(OvsdbTerminationPointAugmentation terminationPoint) {
        if (!SouthboundUtils.bfdMonitoringEnabled(terminationPoint.getInterfaceBfd())) {
            return Interface.OperStatus.Up;
        }
        Interface.OperStatus livenessState = Interface.OperStatus.Down;
        List<InterfaceBfdStatus> tunnelBfdStatus = terminationPoint.getInterfaceBfdStatus();
        if (tunnelBfdStatus != null && !tunnelBfdStatus.isEmpty()) {
            for (InterfaceBfdStatus bfdState : tunnelBfdStatus) {
                if (SouthboundUtils.BFD_OP_STATE.equalsIgnoreCase(bfdState.getBfdStatusKey())) {
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
