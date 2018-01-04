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
import java.util.concurrent.Callable;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.itm.impl.ITMBatchingUtils;
import org.opendaylight.genius.itm.impl.ItmTepUtils;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.NodeIdTypeHwvtep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.NodeIdTypeOvsdb;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tunnel.zones.TunnelZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tunnel.zones.tunnel.zone.Vteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeBase;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VtepRemoveWorker implements Callable<List<ListenableFuture<Void>>> {
    private static final Logger LOG = LoggerFactory.getLogger(VtepRemoveWorker.class);
    private final TunnelZone zone;
    private final DataBroker dataBroker;
    private final ItmTepUtils itmTepUtils;
    private final Vteps vtep;

    public VtepRemoveWorker(final DataBroker broker, final ItmTepUtils itmTepUtils,
                            final Vteps vtep, final TunnelZone zone) {
        this.dataBroker = broker ;
        this.itmTepUtils = itmTepUtils;
        this.vtep = vtep;
        this.zone = zone;
    }

    private CheckedFuture<Void, TransactionCommitFailedException> removeOvsdbTunnel() {
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        String vtepIpStr = String.valueOf(vtep.getIpAddress().getValue());
        String ifName = ItmUtils.getTrunkInterfaceName(null, vtep.getNodeId(), vtepIpStr,
            zone.getTunnelType().getName());

        InstanceIdentifier<TerminationPoint> tpIid =
            itmTepUtils.createTerminationPointInstanceIdentifier(vtep.getNodeId(), ifName);
        ITMBatchingUtils.delete(tpIid, ITMBatchingUtils.EntityType.TOPOLOGY_CONFIG);
        return tx.submit();
    }

    private CheckedFuture<Void, TransactionCommitFailedException> removeHwvtepTunnel() {
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        Class<? extends InterfaceTypeBase> ifType = itmTepUtils.TUNNEL_INTERFACE_TYPE_MAP.get(zone.getTunnelType());
        return tx.submit();
    }

    @Override
    public List<ListenableFuture<Void>> call() throws Exception {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        if (NodeIdTypeOvsdb.class.equals(vtep.getVtepNodeIdType())) {
            futures.add(removeOvsdbTunnel());
        } else if (NodeIdTypeHwvtep.class.equals(vtep.getVtepNodeIdType())) {
            futures.add(removeHwvtepTunnel());
        }
        return futures ;
    }

}
