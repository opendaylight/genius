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
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.itm.impl.ITMBatchingUtils;
import org.opendaylight.genius.itm.impl.ITMBatchingUtils.EntityType;
import org.opendaylight.genius.itm.impl.ItmFlowUtils;
import org.opendaylight.genius.itm.impl.ItmTepUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tep.states.TepState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TepStateUpdateWorker implements Callable<List<ListenableFuture<Void>>> {
    private static final Logger LOG = LoggerFactory.getLogger(TepStateUpdateWorker.class);
    private final DataBroker dataBroker;
    private final ItmFlowUtils itmFlowUtils;
    private final ItmTepUtils itmTepUtils;
    private final InstanceIdentifier<OvsdbTerminationPointAugmentation> iid;
    private final OvsdbTerminationPointAugmentation tp;

    public TepStateUpdateWorker(final DataBroker dataBroker,
                                final ItmTepUtils itmTepUtils,
                                final ItmFlowUtils itmFlowUtils,
                                final InstanceIdentifier<OvsdbTerminationPointAugmentation> identifier,
                                final OvsdbTerminationPointAugmentation tp) {
        this.dataBroker = dataBroker;
        this.itmFlowUtils = itmFlowUtils;
        this.itmTepUtils = itmTepUtils;
        this.iid = identifier;
        this.tp = tp;
    }

    private CheckedFuture<Void, TransactionCommitFailedException> createTepStateOvs() {
        TepState tepState = itmTepUtils.createTepState(dataBroker, iid, tp);
        InstanceIdentifier<TepState> iid = itmTepUtils.createTepStateIdentifier(tepState.getKey().getTepIfName());
        LOG.debug("Creating TepState: {}", tepState);
        ITMBatchingUtils.write(iid, tepState, EntityType.DEFAULT_OPERATIONAL);
        itmFlowUtils.addTunnelIngressFlows(tepState);
        return dataBroker.newWriteOnlyTransaction().submit();
    }

    @Override
    public List<ListenableFuture<Void>> call() throws Exception {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        futures.add(createTepStateOvs());
        return futures ;
    }

}
