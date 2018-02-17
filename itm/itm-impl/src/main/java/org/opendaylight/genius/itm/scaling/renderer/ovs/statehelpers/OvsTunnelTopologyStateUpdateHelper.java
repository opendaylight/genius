/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.scaling.renderer.ovs.statehelpers;

import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.itm.cache.DPNTEPsInfoCache;
import org.opendaylight.genius.itm.cache.TunnelStateCache;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.TunnelMonitoringConfig;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.ItmScaleUtils;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.SouthboundUtils;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.TunnelMetaUtils;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.TunnelUtils;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.bridge.tunnel.info.OvsBridgeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelOperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceBfdStatus;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Singleton
public class OvsTunnelTopologyStateUpdateHelper {
    private static final Logger LOG = LoggerFactory.getLogger(OvsTunnelTopologyStateUpdateHelper.class);

    private final DataBroker dataBroker;
    private final JobCoordinator coordinator;
    private final EntityOwnershipUtils entityOwnershipUtils;
    private final DPNTEPsInfoCache dpntePsInfoCache;
    private final TunnelStateCache tunnelStateCache;
    private final TunnelMonitoringConfig tunnelMonitoringConfig;

    @Inject
    public OvsTunnelTopologyStateUpdateHelper(final DataBroker dataBroker, final JobCoordinator coordinator,
                                              final EntityOwnershipUtils entityOwnershipUtils,
                                              final DPNTEPsInfoCache dpntePsInfoCache,
                                              final TunnelStateCache tunnelStateCache,
                                              final TunnelMonitoringConfig tunnelMonitoringConfig) {
        this.dataBroker = dataBroker;
        this.coordinator = coordinator;
        this.entityOwnershipUtils = entityOwnershipUtils;
        this.dpntePsInfoCache = dpntePsInfoCache;
        this.tunnelStateCache = tunnelStateCache;
        this.tunnelMonitoringConfig = tunnelMonitoringConfig;

    }

    /*
     *  This code is used to handle only a dpnId change scenario for a particular change,
     * which is not expected to happen in usual cases.
     */
    public List<ListenableFuture<Void>> updateOvsBridgeRefEntry(InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIid,
                                                                OvsdbBridgeAugmentation bridgeNew,
                                                                OvsdbBridgeAugmentation bridgeOld) {

        List<ListenableFuture<Void>> futures = new ArrayList<>();
        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();

        BigInteger dpnIdNew = ItmScaleUtils.getDpnId(bridgeNew.getDatapathId());
        BigInteger dpnIdOld = ItmScaleUtils.getDpnId(bridgeOld.getDatapathId());

        LOG.debug("updating bridge references for bridge: {}, dpnNew: {}, dpnOld: {}", bridgeNew,
            dpnIdNew, dpnIdOld);
        //delete bridge reference entry for the old dpn in interface meta operational DS
        TunnelMetaUtils.deleteOvsBridgeRefEntry(dpnIdOld, writeTransaction);

        // create bridge reference entry in interface meta operational DS
        TunnelMetaUtils.createOvsBridgeRefEntry(dpnIdNew, bridgeIid, writeTransaction);

        // handle pre-provisioning of tunnels for the newly connected dpn
        OvsBridgeEntry bridgeEntry = TunnelMetaUtils.getOvsBridgeEntryFromConfigDS(dpnIdNew, dataBroker);
        if (bridgeEntry == null) {
            futures.add(writeTransaction.submit());
            return futures;
        }

        SouthboundUtils.addAllPortsToBridge(bridgeEntry, dataBroker, bridgeIid, bridgeNew, futures,
                tunnelMonitoringConfig, dpntePsInfoCache);

        futures.add(writeTransaction.submit());
        return futures;
    }

    public List<ListenableFuture<Void>> updateTunnelState(OvsdbTerminationPointAugmentation terminationPointNew) {
        final String interfaceName = terminationPointNew.getName();
        final Interface.OperStatus interfaceBfdStatus = getTunnelOpState(terminationPointNew);
        TunnelOperStatus tunnelState = ItmScaleUtils.convertInterfaceToTunnelOperState(interfaceBfdStatus);
        TunnelUtils.addBfdStateToCache(interfaceName, interfaceBfdStatus);
        if (!entityOwnershipUtils.isEntityOwner(ITMConstants.ITM_CONFIG_ENTITY, ITMConstants.ITM_CONFIG_ENTITY)) {
            return null;
        }

        coordinator.enqueueJob(interfaceName, () -> {
            // update opstate of interface if TEP has gone down/up as a result of BFD monitoring
            final StateTunnelList stateTnl = TunnelUtils
                .getTunnelFromOperationalDS(interfaceName, dataBroker, tunnelStateCache);
            if (stateTnl != null && stateTnl.getOperState() != TunnelOperStatus.Unknown
                && stateTnl.getOperState() != tunnelState) {
                LOG.debug("updating tunnel state for interface {} as {}", interfaceName,
                    stateTnl.getOperState());
                WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
                TunnelUtils.updateOpState(transaction, interfaceName, tunnelState);
                return Collections.singletonList(transaction.submit());
            }
            return Collections.emptyList();
        });
        return null;
    }

    private static Interface.OperStatus getTunnelOpState(OvsdbTerminationPointAugmentation terminationPoint) {
        if (!SouthboundUtils.bfdMonitoringEnabled(terminationPoint.getInterfaceBfd())) {
            return Interface.OperStatus.Up;
        }
        Interface.OperStatus livenessState = Interface.OperStatus.Down;
        List<InterfaceBfdStatus> tunnelBfdStatus = terminationPoint.getInterfaceBfdStatus();
        if (tunnelBfdStatus != null && !tunnelBfdStatus.isEmpty()) {
            for (InterfaceBfdStatus bfdState : tunnelBfdStatus) {
                if (bfdState.getBfdStatusKey().equalsIgnoreCase(SouthboundUtils.BFD_OP_STATE)) {
                    String bfdOpState = bfdState.getBfdStatusValue();
                    livenessState = SouthboundUtils.BFD_STATE_UP.equalsIgnoreCase(bfdOpState)
                            ? Interface.OperStatus.Up : Interface.OperStatus.Down;
                    break;
                }
            }
        }
        return livenessState;
    }
}
