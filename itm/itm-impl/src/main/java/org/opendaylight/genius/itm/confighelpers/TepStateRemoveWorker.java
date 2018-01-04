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
import org.opendaylight.genius.itm.impl.ITMBatchingUtils.EntityType;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tep.states.TepState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TepStateRemoveWorker implements Callable<List<ListenableFuture<Void>>> {
    private static final Logger LOG = LoggerFactory.getLogger(TepStateRemoveWorker.class);
    private final DataBroker dataBroker;
    private final InstanceIdentifier<OvsdbTerminationPointAugmentation> iid;

    public TepStateRemoveWorker(final DataBroker dataBroker,
                                final InstanceIdentifier<OvsdbTerminationPointAugmentation> identifier) {
        this.dataBroker = dataBroker;
        this.iid = identifier;
    }

    private CheckedFuture<Void, TransactionCommitFailedException> removeTepState() {
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        String nodeId = iid.firstKeyOf(Node.class).getNodeId().getValue();
        InstanceIdentifier<TepState> tsIid = ItmUtils.createTepStateIdentifier(nodeId);
        LOG.debug("Deleting TepState: {}", nodeId);
        ITMBatchingUtils.delete(tsIid, EntityType.DEFAULT_OPERATIONAL);
        return tx.submit();
    }

    @Override
    public List<ListenableFuture<Void>> call() throws Exception {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        futures.add(removeTepState());
        return futures ;
    }

    @Override
    public String toString() {
        return "TepStateRemoveWorker  { " + "TerminationPointIid: " + iid + " }" ;
    }
}
