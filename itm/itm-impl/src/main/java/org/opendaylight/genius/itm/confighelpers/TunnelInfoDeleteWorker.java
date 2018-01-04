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
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.itm.impl.ITMBatchingUtils;
import org.opendaylight.genius.itm.impl.ITMBatchingUtils.EntityType;
import org.opendaylight.genius.itm.impl.ItmTepUtils;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.NodeIdTypeOvsdb;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.TunnelInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tunnel.info.SrcTep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tunnel.info.SrcTepKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tunnel.info.src.tep.DstTep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tunnel.info.src.tep.DstTepKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tunnel.zones.TunnelZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tunnel.zones.tunnel.zone.Vteps;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TunnelInfoDeleteWorker implements Callable<List<ListenableFuture<Void>>> {
    private static final Logger LOG = LoggerFactory.getLogger(TunnelInfoDeleteWorker.class);
    private final TunnelZone zone;
    private final Map<String, String> deletedMap;
    private final DataBroker dataBroker;
    private final ItmTepUtils itmTepUtils;
    private final Vteps vtep;

    public TunnelInfoDeleteWorker(DataBroker broker, final ItmTepUtils itmTepUtils,
                                  final Vteps vtep, final TunnelZone zone, Map<String, String> deletedMap) {
        this.dataBroker = broker;
        this.itmTepUtils = itmTepUtils;
        this.vtep = vtep;
        this.zone = zone;
        this.deletedMap = deletedMap;
    }

    private CheckedFuture<Void, TransactionCommitFailedException> deleteIfIndexes() {
        deleteSrcTep();
        for (Vteps dstVtep : zone.getVteps()) {
            if (!dstVtep.getNodeId().equals(vtep.getNodeId())) {
                // Release ifIndex for tunnel from this node to other
                String ifName = itmTepUtils.getVtepInterfaceName(vtep, dstVtep, zone);
                itmTepUtils.releaseIfIndex(ifName);

                /*
                 * Release ifIndex for tunnel from other to this. If destVtep is in list of
                 * deleted, it will get cleared out, nothing else to do here.
                 */
                if (NodeIdTypeOvsdb.class.equals(dstVtep.getVtepNodeIdType())
                        && deletedMap != null && deletedMap.get(dstVtep.getNodeId()) == null) {
                    deleteDstTep(dstVtep);
                }
            }
        }
        return dataBroker.newWriteOnlyTransaction().submit();
    }

    private void deleteSrcTep() {
        InstanceIdentifier<SrcTep> srcTepIid = InstanceIdentifier.create(TunnelInfo.class)
            .child(SrcTep.class, new SrcTepKey(vtep.getNodeId()));
        ITMBatchingUtils.delete(srcTepIid, EntityType.DEFAULT_CONFIG);

    }

    private void deleteDstTep(Vteps dstVtep) {
        InstanceIdentifier<DstTep> dstTepIid = InstanceIdentifier.create(TunnelInfo.class)
            .child(SrcTep.class, new SrcTepKey(dstVtep.getNodeId()))
            .child(DstTep.class, new DstTepKey(vtep.getNodeId()));
        ITMBatchingUtils.delete(dstTepIid, EntityType.DEFAULT_CONFIG);
        String ifName = itmTepUtils.getVtepInterfaceName(dstVtep, vtep, zone);
        itmTepUtils.releaseIfIndex(ifName);
    }

    @Override
    public List<ListenableFuture<Void>> call() throws Exception {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        futures.add(deleteIfIndexes());
        return futures;
    }

}
