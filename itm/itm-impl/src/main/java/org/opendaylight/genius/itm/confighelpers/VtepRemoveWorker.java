/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.confighelpers;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.itm.impl.ITMBatchingUtils;
import org.opendaylight.genius.itm.impl.ITMBatchingUtils.EntityType;
import org.opendaylight.genius.itm.impl.ItmFlowUtils;
import org.opendaylight.genius.itm.impl.ItmTepUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.TunnelInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tep.states.TepState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tunnel.info.SrcTep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tunnel.info.SrcTepKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tunnel.info.src.tep.DstTep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tunnel.info.src.tep.DstTepKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tunnel.zones.TunnelZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tunnel.zones.tunnel.zone.Vteps;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VtepRemoveWorker implements Callable<List<ListenableFuture<Void>>> {
    private static final Logger LOG = LoggerFactory.getLogger(VtepRemoveWorker.class);
    private final Class<? extends TunnelTypeBase> tunnelType;
    private final TunnelZone zone;
    private final Map<String, String> deletedMap;
    private final DataBroker dataBroker;
    private final ItmTepUtils itmTepUtils;
    private final ItmFlowUtils itmFlowUtils;
    private final Vteps vtep;

    public VtepRemoveWorker(final DataBroker broker, final ItmTepUtils itmTepUtils,
                            final ItmFlowUtils itmFlowUtils,
                            final Vteps vtep, Class<? extends TunnelTypeBase> tunnelType,
                            final TunnelZone zone, Map<String, String> deletedMap) {
        this.dataBroker = broker ;
        this.itmTepUtils = itmTepUtils;
        this.itmFlowUtils = itmFlowUtils;
        this.vtep = vtep;
        this.tunnelType = tunnelType;
        this.zone = zone;
        this.deletedMap = deletedMap;
    }

    private CheckedFuture<Void, TransactionCommitFailedException> removeVtep() {
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        for (Vteps dstVtep : zone.getVteps()) {
            if (!dstVtep.getNodeId().equals(vtep.getNodeId())) {
                removeOvsdbTep(vtep, dstVtep);
                /*
                 * Get ifIndex for tunnel from other to this. If destVtep is in list of
                 * deleted, it will get deleted, nothing else to do here.
                 */
                if (itmTepUtils.isVtepOvs(dstVtep)
                    && deletedMap != null && deletedMap.get(dstVtep.getNodeId()) == null) {
                    removeOvsdbTep(dstVtep, vtep);
                    deleteDstTep(dstVtep);
                }
            }
        }
        deleteSrcTep();
        return tx.submit();
    }

    private void removeOvsdbTep(Vteps srcVtep, Vteps dstVtep) {
        /*
        Class<? extends TunnelTypeBase> tepTunnelType =
            tunnelType == null ? itmTepUtils.getTunnelType(srcVtep, dstVtep, zone) : tunnelType;
            */
        if (!itmTepUtils.isVtepOvs(srcVtep)) {
            // Src is non OVSDB type, return
            LOG.debug("Skip deleting tunnel for: {}", srcVtep.getNodeId());
            return;
        }
        String portName = itmTepUtils.getVtepPortName(srcVtep, dstVtep, zone);
        LOG.debug("Deleting Ovsdb Tunnel {} from {} to {}",
            portName, srcVtep.getNodeId(), dstVtep.getNodeId());
        InstanceIdentifier<TerminationPoint> tpIid = removeOvsdbTunnel(srcVtep.getNodeId(), portName);
        removeTepState(portName);
        /* TODO: For non OfTunnels use interfacename
         * String ifName = itmTepUtils.getVtepInterfaceName(vtep, dstVtep, zone);
        */
        itmTepUtils.releaseIfIndex(portName);
    }

    private InstanceIdentifier<TerminationPoint> removeOvsdbTunnel(String nodeId, String portName) {
        InstanceIdentifier<TerminationPoint> tpIid =
            itmTepUtils.createTerminationPointInstanceIdentifier(nodeId, portName);
        ITMBatchingUtils.delete(tpIid, ITMBatchingUtils.EntityType.TOPOLOGY_CONFIG);
        return tpIid;
    }

    private void removeTepState(String portName) {
        TepState tepState = itmTepUtils.deleteTepState(portName);
        if (tepState != null) {
            itmFlowUtils.removeTunnelFlows(tepState);
            itmFlowUtils.removeTunnelEgressGroup(tepState.getDpnId(), tepState.getTepOptionRemoteIp(),
                itmTepUtils.getGroupId(portName), 0);
        } //TODO: Revisit else when add support for OFTunnels
    }

    private void deleteSrcTep() {
        if (itmTepUtils.isVtepOvs(vtep) && zone.isOptionOfTunnel()) {
            LOG.debug("Deleting tunnelInfo for srcTep: {}", vtep.getNodeId());
            InstanceIdentifier<SrcTep> srcTepIid = InstanceIdentifier.create(TunnelInfo.class)
                .child(SrcTep.class, new SrcTepKey(vtep.getNodeId()));
            ITMBatchingUtils.delete(srcTepIid, EntityType.DEFAULT_CONFIG);
        } else {
            LOG.debug("Skip deleting tunnelInfo for srcTep: {}", vtep.getNodeId());
        }
    }

    private void deleteDstTep(Vteps dstVtep) {
        if (itmTepUtils.isVtepOvs(vtep) && zone.isOptionOfTunnel()) {
            String ifName = itmTepUtils.getVtepInterfaceName(dstVtep, vtep, zone);
            LOG.debug("Deleting DstTep {} from srcTep {} to dstTep {}", ifName, vtep.getNodeId(), dstVtep.getNodeId());
            InstanceIdentifier<DstTep> dstTepIid = InstanceIdentifier.create(TunnelInfo.class)
                .child(SrcTep.class, new SrcTepKey(dstVtep.getNodeId()))
                .child(DstTep.class, new DstTepKey(ifName));
            ITMBatchingUtils.delete(dstTepIid, EntityType.DEFAULT_CONFIG);
            itmTepUtils.releaseIfIndex(ifName);
            itmTepUtils.releaseDestGroupId(dstVtep.getNodeId());
        } else {
            LOG.debug("Skip deleting dstTep {} for srcTep: {}", vtep.getNodeId(), dstVtep.getNodeId());
        }
    }

    @Override
    public List<ListenableFuture<Void>> call() throws Exception {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        futures.add(removeVtep());
        return futures ;
    }

}
