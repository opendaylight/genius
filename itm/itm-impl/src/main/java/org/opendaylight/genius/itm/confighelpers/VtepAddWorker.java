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
import org.opendaylight.genius.itm.impl.ItmTepUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.NodeIdTypeOvsdb;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.TunnelInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tunnel.info.SrcTep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tunnel.info.SrcTepBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tunnel.info.SrcTepKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tunnel.info.src.tep.DstTep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tunnel.info.src.tep.DstTepBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tunnel.info.src.tep.DstTepKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tunnel.zones.TunnelZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tunnel.zones.tunnel.zone.Vteps;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VtepAddWorker implements Callable<List<ListenableFuture<Void>>> {
    private static final Logger LOG = LoggerFactory.getLogger(VtepAddWorker.class);
    private final TunnelZone zone;
    private final Map<String, String> addedMap;
    private final DataBroker dataBroker;
    private final ItmTepUtils itmTepUtils;
    private final Vteps vtep;
    private final Class<? extends TunnelTypeBase> tunnelType;

    public VtepAddWorker(final DataBroker broker, final ItmTepUtils itmTepUtils, final Vteps vtep,
                         final Class<? extends TunnelTypeBase> tunnelType, final TunnelZone zone,
                         final Map<String, String> addedMap) {
        this.dataBroker = broker ;
        this.itmTepUtils = itmTepUtils;
        this.vtep = vtep;
        this.tunnelType = tunnelType;
        this.zone = zone;
        this.addedMap = addedMap;
    }

    private CheckedFuture<Void, TransactionCommitFailedException> createOvsdbVtep() {
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        List<DstTep> dstTepList = new ArrayList<>();
        for (Vteps dstVtep : zone.getVteps()) {
            if (!dstVtep.getNodeId().equals(vtep.getNodeId())) {
                //itmFlowUtils.addTunnelFlows(vtep, dstTep);
                /*
                 * Get ifIndex for tunnel from other to this. If destVtep is in list of
                 * added, it will get added, nothing else to do here.
                 */
                if (NodeIdTypeOvsdb.class.equals(dstVtep.getVtepNodeIdType())
                    && addedMap != null && addedMap.get(dstVtep.getNodeId()) == null) {
                    DstTep peerToSelfDestTep = addDestTunnelInfo(dstVtep);
                    createOvsdbTunnel(dstVtep, peerToSelfDestTep);
                    //itmFlowUtils.addTunnelFlows(dstTep, vtep);
                }
                DstTep dstTep = buildDstTep(dstVtep);
                dstTepList.add(dstTep);
                createOvsdbTunnel(vtep, dstTep);
            }
        }
        createSrcTep(dstTepList);
        return tx.submit();
    }

    private void createSrcTep(List<DstTep> dstTepList) {
        SrcTepBuilder srcTepBuilder = new SrcTepBuilder();
        srcTepBuilder.setSrcTepNodeId(vtep.getNodeId());
        srcTepBuilder.setDstTep(dstTepList);
        InstanceIdentifier<SrcTep> srcTepIid = InstanceIdentifier.create(TunnelInfo.class)
            .child(SrcTep.class, new SrcTepKey(vtep.getNodeId()));
        LOG.debug("Adding tunnelInfo for srcTep: {}", vtep.getNodeId());
        ITMBatchingUtils.write(srcTepIid, srcTepBuilder.build(), EntityType.DEFAULT_CONFIG);
    }

    private void createOvsdbTunnel(Vteps srcVtep, DstTep dstTep) {
        TerminationPoint tp = itmTepUtils.createOvsdbTerminationPoint(srcVtep, dstTep, zone);
        InstanceIdentifier<TerminationPoint> tpIid =
            itmTepUtils.createTerminationPointInstanceIdentifier(srcVtep.getNodeId(), tp.getTpId().getValue());
        LOG.debug("Adding Ovsdb Tunnel {} from {} to {}",
            tp.getTpId().getValue(), srcVtep.getNodeId(), dstTep.getDstTepNodeId());
        ITMBatchingUtils.write(tpIid, tp, ITMBatchingUtils.EntityType.TOPOLOGY_CONFIG);
    }

    private DstTep addDestTunnelInfo(Vteps dstVtep) {
        DstTep dstTep = buildDstTep(dstVtep, vtep);
        InstanceIdentifier<DstTep> dstTepIid = InstanceIdentifier.create(TunnelInfo.class)
            .child(SrcTep.class, new SrcTepKey(dstVtep.getNodeId()))
            .child(DstTep.class, new DstTepKey(dstTep.getTunnelIfName()));
        LOG.debug("Adding tunnelInfo to srcTep: {} for dstVtep: {}", dstVtep.getNodeId(), vtep.getNodeId());
        ITMBatchingUtils.update(dstTepIid, dstTep, EntityType.DEFAULT_CONFIG);
        return dstTep;
    }

    private DstTep buildDstTep(Vteps dstVtep) {
        return buildDstTep(vtep, dstVtep);
    }

    private DstTep buildDstTep(Vteps srcVtep, Vteps dstVtep) {
        String ifName = itmTepUtils.getVtepInterfaceName(srcVtep, dstVtep, zone);
        String portName = itmTepUtils.getVtepPortName(srcVtep, dstVtep, zone);
        DstTepBuilder dstTepBuilder = new DstTepBuilder();
        dstTepBuilder.setDstTepNodeId(dstVtep.getNodeId());
        dstTepBuilder.setTepNodeType(dstVtep.getVtepNodeIdType());
        dstTepBuilder.setTunnelIp(dstVtep.getIpAddress());
        dstTepBuilder.setTunnelIfName(ifName);
        dstTepBuilder.setTunnelPortName(portName);
        dstTepBuilder.setTepTunnelType(itmTepUtils.getTunnelType(srcVtep, dstVtep, zone));
        Integer intIfIndex = itmTepUtils.allocateIfIndex(ifName);
        long ifIndex = intIfIndex.longValue();
        if (ifIndex == 0) {
            LOG.error("Unable to get ifIndex for tunnel from {} to {}", srcVtep.getNodeId(), dstVtep.getNodeId());
        }
        dstTepBuilder.setTunnelIfIndex(ifIndex);
        //TODO: Revisit logic to get groupId for tunnels
        long groupId = itmTepUtils.getGroupId(intIfIndex);//ifManager.getLogicalTunnelSelectGroupId(intIfIndex);
        dstTepBuilder.setTunnelOutGroupId(groupId);
        return dstTepBuilder.build();
    }

    @Override
    public List<ListenableFuture<Void>> call() throws Exception {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        if (NodeIdTypeOvsdb.class.equals(vtep.getVtepNodeIdType())) {
            futures.add(createOvsdbVtep());
        }
        return futures ;
    }

}
