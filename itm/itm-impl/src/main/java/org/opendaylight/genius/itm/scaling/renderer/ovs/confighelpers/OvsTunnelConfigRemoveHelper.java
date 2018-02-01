/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.scaling.renderer.ovs.confighelpers;

import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.itm.cache.TunnelStateCache;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.DpnTepInterfaceInfo;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.ItmScaleUtils;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.SouthboundUtils;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.TunnelMetaUtils;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.TunnelUtils;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.bridge.tunnel.info.OvsBridgeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.bridge.tunnel.info.OvsBridgeEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.bridge.tunnel.info.ovs.bridge.entry.OvsBridgeTunnelEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelOperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OvsTunnelConfigRemoveHelper {
    private static final Logger LOG = LoggerFactory.getLogger(OvsTunnelConfigRemoveHelper.class);

    private OvsTunnelConfigRemoveHelper() {
    }

    public static List<ListenableFuture<Void>> removeConfiguration(DataBroker dataBroker,
                                                                   Interface interfaceOld,
                                                                   IdManagerService idManager,
                                                                   IMdsalApiManager mdsalApiManager,
                                                                   ParentRefs parentRefs,
                                                                   TunnelStateCache tunnelStateCache) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        WriteTransaction defaultOperationalShardTransaction = dataBroker.newWriteOnlyTransaction();
        WriteTransaction defaultConfigShardTransaction = dataBroker.newWriteOnlyTransaction();

        IfTunnel ifTunnel = interfaceOld.getAugmentation(IfTunnel.class);
        if (ifTunnel != null) {
            removeTunnelConfiguration(parentRefs, dataBroker, interfaceOld.getName(), ifTunnel,
                    idManager, mdsalApiManager, defaultOperationalShardTransaction, defaultConfigShardTransaction,
                    futures, tunnelStateCache);
            futures.add(defaultConfigShardTransaction.submit());
            futures.add(defaultOperationalShardTransaction.submit());
        }
        return futures;
    }

    private static void removeTunnelConfiguration(ParentRefs parentRefs,
                                                  DataBroker dataBroker, String interfaceName, IfTunnel ifTunnel,
                                                  IdManagerService idManager, IMdsalApiManager mdsalApiManager,
                                                  WriteTransaction defaultOperationalShardTransaction,
                                                  WriteTransaction defaultConfigShardTransaction,
                                                  List<ListenableFuture<Void>> futures,
                                                  TunnelStateCache tunnelStateCache) {

        LOG.info("removing tunnel configuration for {}", interfaceName);
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        BigInteger dpId = null;
        if (parentRefs != null) {
            dpId = parentRefs.getDatapathNodeIdentifier();
        }

        if (dpId == null) {
            return;
        }

        OvsdbBridgeRef ovsdbBridgeRef = TunnelMetaUtils.getOvsdbBridgeRef(dpId, dataBroker);
        if (ovsdbBridgeRef != null) {
            SouthboundUtils.removeTerminationEndPoint(dataBroker, ovsdbBridgeRef.getValue(), interfaceName);
        } else {
            LOG.error("Cannot Delete Tunnel {} as OVS Bridge Reference is NULL ",interfaceName);
        }

        // delete tunnel ingress flow
        removeTunnelIngressFlow(futures, dataBroker, interfaceName, ifTunnel, mdsalApiManager, dpId, tunnelStateCache);

        // delete bridge to tunnel interface mappings
        OvsBridgeEntryKey bridgeEntryKey = new OvsBridgeEntryKey(dpId);
        InstanceIdentifier<OvsBridgeEntry> bridgeEntryIid = TunnelMetaUtils.getOvsBridgeEntryIdentifier(bridgeEntryKey);
        OvsBridgeEntry bridgeEntry = TunnelMetaUtils.getOvsBridgeEntryFromConfigDS(bridgeEntryIid, dataBroker);
        if (bridgeEntry == null) {
            LOG.debug("Bridge Entry not present for dpn: {}", dpId);
            return;
        }

        List<OvsBridgeTunnelEntry> bridgeInterfaceEntries = bridgeEntry.getOvsBridgeTunnelEntry();
        if (bridgeInterfaceEntries == null) {
            LOG.debug("Bridge Interface Entries not present for dpn : {}", dpId);
            return;
        }

        TunnelMetaUtils.deleteBridgeInterfaceEntry(bridgeEntryKey, bridgeInterfaceEntries, bridgeEntryIid,
                defaultConfigShardTransaction, interfaceName);
        // IfIndex needs to be removed only during State Clean up not Config
       // TunnelMetaUtils.removeLportTagInterfaceMap(idManager, defaultOperationalShardTransaction, interfaceName);
        cleanUpInterfaceWithUnknownState(interfaceName, parentRefs, ifTunnel, dataBroker,
                defaultOperationalShardTransaction, idManager, tunnelStateCache);
    }

    static void removeTunnelIngressFlow(List<ListenableFuture<Void>> futures, DataBroker dataBroker,
                                        String interfaceName, IfTunnel ifTunnel, IMdsalApiManager mdsalApiManager,
                                        BigInteger dpId, TunnelStateCache tunnelStateCache) {
        long portNo = ItmScaleUtils.getNodeConnectorIdFromInterface(interfaceName, dataBroker, tunnelStateCache);
        DpnTepInterfaceInfo dpnTepInfo = TunnelUtils.getTunnelFromConfigDS(interfaceName, dataBroker);

        TunnelUtils.makeTunnelIngressFlow(futures, mdsalApiManager, dpnTepInfo, dpId, portNo,interfaceName , -1,
                    NwConstants.DEL_FLOW);
    }

    // if the node is shutdown, there will be stale interface state entries,
    // with unknown op-state, clear them.
    static StateTunnelList cleanUpInterfaceWithUnknownState(String interfaceName, ParentRefs parentRefs,
                                                            IfTunnel ifTunnel, DataBroker dataBroker,
                                                            WriteTransaction transaction,
                                                            IdManagerService idManagerService,
                                                            TunnelStateCache tunnelStateCache) {
        StateTunnelList stateTunnelList = TunnelUtils.getTunnelFromOperationalDS(interfaceName, dataBroker,
                tunnelStateCache);
        if (stateTunnelList != null && stateTunnelList.getOperState() == TunnelOperStatus.Unknown) {
            String staleInterface = ifTunnel != null ? interfaceName : parentRefs.getParentInterface();
            LOG.debug("cleaning up parent-interface for {}, since the oper-status is UNKNOWN", interfaceName);
            TunnelUtils.deleteTunnelStateInformation(staleInterface, transaction, idManagerService);
        }
        return stateTunnelList;
    }
}
