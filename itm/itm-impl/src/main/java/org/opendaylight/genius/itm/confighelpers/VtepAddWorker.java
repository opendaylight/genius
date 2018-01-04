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
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.NodeIdTypeHwvtep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.NodeIdTypeOvsdb;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.tunnel.zones.tunnel.zone.Vteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeBase;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VtepAddWorker implements Callable<List<ListenableFuture<Void>>> {
    private static final Logger LOG = LoggerFactory.getLogger(VtepAddWorker.class);
    private DataBroker dataBroker;
    private Vteps vtep;
    private Class<? extends TunnelTypeBase> tunnelType;

    public VtepAddWorker(DataBroker broker, Vteps vtep, Class<? extends TunnelTypeBase> tunnelType) {
        this.dataBroker = broker ;
        this.vtep = vtep;
        this.tunnelType = tunnelType;
    }

    private CheckedFuture<Void, TransactionCommitFailedException> createOvsdbTunnel() {
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        TerminationPoint tp = ItmUtils.createOvsdbTerminationPoint(vtep.getNodeId(),
            String.valueOf(vtep.getIpAddress().getValue()), tunnelType);
        InstanceIdentifier<TerminationPoint> tpIid =
            ItmUtils.createTerminationPointInstanceIdentifier(vtep.getNodeId(), tp.getTpId().getValue());
        ITMBatchingUtils.write(tpIid, tp, ITMBatchingUtils.EntityType.TOPOLOGY_CONFIG);
        return tx.submit();
    }

    private CheckedFuture<Void, TransactionCommitFailedException> createHwvtepTunnel() {
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        Class<? extends InterfaceTypeBase> ifType = ItmUtils.TUNNEL_INTERFACE_TYPE_MAP.get(tunnelType);
        return tx.submit();
    }

    @Override
    public List<ListenableFuture<Void>> call() throws Exception {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        if (NodeIdTypeOvsdb.class.equals(vtep.getVtepNodeIdType())) {
            futures.add(createOvsdbTunnel());
        } else if (NodeIdTypeHwvtep.class.equals(vtep.getVtepNodeIdType())) {
            futures.add(createHwvtepTunnel());
        }
        return futures ;
    }

    @Override
    public String toString() {
        return "VtepAddWorker  { " + "Vtep: " + vtep + "  TunnelType : " + tunnelType.getSimpleName() + " }" ;
    }
}
