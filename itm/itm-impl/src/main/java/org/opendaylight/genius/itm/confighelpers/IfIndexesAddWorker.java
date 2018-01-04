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
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.impl.ITMBatchingUtils;
import org.opendaylight.genius.itm.impl.ITMBatchingUtils.EntityType;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelIfIndexes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelIfIndexesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel._if.indexes.SrcTep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel._if.indexes.SrcTepBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel._if.indexes.SrcTepKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel._if.indexes.src.tep.DstTep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel._if.indexes.src.tep.DstTepBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel._if.indexes.src.tep.DstTepKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.NodeIdTypeHwvtep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.NodeIdTypeOvsdb;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.tunnel.zones.TunnelZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.tunnel.zones.tunnel.zone.Vteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeBase;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IfIndexesAddWorker implements Callable<List<ListenableFuture<Void>>> {
    private static final Logger LOG = LoggerFactory.getLogger(IfIndexesAddWorker.class);
    private final TunnelZone zone;
    private final Map<String, String> addedMap;
    private final DataBroker dataBroker;
    private final Vteps vtep;
    private final IInterfaceManager ifManager;

    public IfIndexesAddWorker(DataBroker broker, final IInterfaceManager iInterfaceManager,
                              final Vteps vtep, final TunnelZone zone, Map<String, String> addedMap) {
        this.dataBroker = broker;
        this.ifManager = iInterfaceManager;
        this.vtep = vtep;
        this.zone = zone;
        this.addedMap = addedMap;
    }

    private CheckedFuture<Void, TransactionCommitFailedException> createIfIndexes() {
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        SrcTepBuilder srcTepBuilder = new SrcTepBuilder();
        srcTepBuilder.setSrcTepNodeId(vtep.getNodeId());
        List<DstTep> dstTepList = new ArrayList<>();
        for(Vteps dstVtep : zone.getVteps()) {
            if (!dstVtep.getNodeId().equals(vtep.getNodeId())) {
                DstTepBuilder dstTepBuilder = new DstTepBuilder();
                dstTepBuilder.setDstTepNodeId(dstVtep.getNodeId());
                String ifName = ItmUtils.getTrunkInterfaceName(null, vtep.getNodeId(), dstVtep.getNodeId(),
                    zone.getTunnelType().getName());
                dstTepBuilder.setTunnelIfName(ifName);
                long ifIndex = ifManager.allocateIfIndex(ifName).longValue();
                if (ifIndex == 0) {
                    LOG.error("Unable to get ifIndex for tunnel from {} to {}", vtep.getNodeId(), dstVtep.getNodeId());
                }
                dstTepBuilder.setTunnelIfIndex(ifIndex);
                dstTepList.add(dstTepBuilder.build());

                /*
                 * Get ifIndex for tunnel from other to this. If destVtep is in list of
                 * added, it will get added, nothing else to do here.
                 */
                if (addedMap != null && addedMap.get(dstVtep.getNodeId()) != null) {
                    addDestIfIndexes(dstVtep, vtep);
                }
            }
        }
        srcTepBuilder.setDstTep(dstTepList);
        InstanceIdentifier<SrcTep> srcTepIid = InstanceIdentifier.create(TunnelIfIndexes.class)
            .child(SrcTep.class, new SrcTepKey(vtep.getNodeId()));
        ITMBatchingUtils.write(srcTepIid, srcTepBuilder.build(), EntityType.DEFAULT_OPERATIONAL);
        return tx.submit();
    }

    private void addDestIfIndexes(Vteps srcVtep, Vteps dstVtep) {
        DstTepBuilder dstTepBuilder = new DstTepBuilder();
        dstTepBuilder.setDstTepNodeId(dstVtep.getNodeId());
        String ifName = ItmUtils.getTrunkInterfaceName(null, vtep.getNodeId(), dstVtep.getNodeId(),
            zone.getTunnelType().getName());
        dstTepBuilder.setTunnelIfName(ifName);
        long ifIndex = ifManager.allocateIfIndex(ifName).longValue();
        if (ifIndex == 0) {
            LOG.error("Unable to get ifIndex for tunnel from {} to {}", vtep.getNodeId(), dstVtep.getNodeId());
        }
        dstTepBuilder.setTunnelIfIndex(ifIndex);
        InstanceIdentifier<DstTep> dstTepIid = InstanceIdentifier.create(TunnelIfIndexes.class)
            .child(SrcTep.class, new SrcTepKey(srcVtep.getNodeId()))
            .child(DstTep.class, new DstTepKey(dstVtep.getNodeId()));
        ITMBatchingUtils.update(dstTepIid, dstTepBuilder.build(), EntityType.DEFAULT_OPERATIONAL);
    }


    @Override
    public List<ListenableFuture<Void>> call() throws Exception {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        futures.add(createIfIndexes());
        return futures;
    }

}
