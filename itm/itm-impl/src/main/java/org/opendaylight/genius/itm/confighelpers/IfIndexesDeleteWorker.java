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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelIfIndexes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel._if.indexes.SrcTep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel._if.indexes.SrcTepKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel._if.indexes.src.tep.DstTep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel._if.indexes.src.tep.DstTepKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.tunnel.zones.TunnelZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.tunnel.zones.tunnel.zone.Vteps;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IfIndexesDeleteWorker implements Callable<List<ListenableFuture<Void>>> {
    private static final Logger LOG = LoggerFactory.getLogger(IfIndexesDeleteWorker.class);
    private final TunnelZone zone;
    private final Map<String, String> deletedMap;
    private final DataBroker dataBroker;
    private final Vteps vtep;
    private final IInterfaceManager ifManager;

    public IfIndexesDeleteWorker(DataBroker broker, final IInterfaceManager iInterfaceManager,
                                 final Vteps vtep, final TunnelZone zone, Map<String, String> deletedMap) {
        this.dataBroker = broker;
        this.ifManager = iInterfaceManager;
        this.vtep = vtep;
        this.zone = zone;
        this.deletedMap = deletedMap;
    }

    private CheckedFuture<Void, TransactionCommitFailedException> deleteIfIndexes() {
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        InstanceIdentifier<SrcTep> srcTepIid = InstanceIdentifier.create(TunnelIfIndexes.class)
            .child(SrcTep.class, new SrcTepKey(vtep.getNodeId()));
        for(Vteps dstVtep : zone.getVteps()) {
            if (!dstVtep.getNodeId().equals(vtep.getNodeId())) {
                InstanceIdentifier<DstTep> dstTepIid = InstanceIdentifier.create(TunnelIfIndexes.class)
                    .child(SrcTep.class, new SrcTepKey(dstVtep.getNodeId()))
                    .child(DstTep.class, new DstTepKey(vtep.getNodeId()));
                // Release ifIndex for tunnel from this node to other
                String ifName = ItmUtils.getTrunkInterfaceName(null, vtep.getNodeId(), dstVtep.getNodeId(),
                    zone.getTunnelType().getName());
                ifManager.releaseIfIndex(ifName);

                /*
                 * Release ifIndex for tunnel from other to this. If destVtep is in list of
                 * deleted, it will get cleared out, nothing else to do here.
                 */
                if(deletedMap != null && deletedMap.get(dstVtep.getNodeId()) != null) {
                    deleteDestIfIndexes(dstTepIid, vtep, dstVtep);
                }
            }
        }
        ITMBatchingUtils.delete(srcTepIid, EntityType.DEFAULT_OPERATIONAL);
        return tx.submit();
    }

    private void deleteDestIfIndexes(InstanceIdentifier<DstTep> dstTepIid, Vteps vtep, Vteps dstVtep) {
        String ifName = ItmUtils.getTrunkInterfaceName(null, dstVtep.getNodeId(), vtep.getNodeId(),
            zone.getTunnelType().getName());
        ifManager.releaseIfIndex(ifName);
        ITMBatchingUtils.delete(dstTepIid, EntityType.DEFAULT_OPERATIONAL);
    }

    @Override
    public List<ListenableFuture<Void>> call() throws Exception {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        futures.add(deleteIfIndexes());
        return futures;
    }

}
