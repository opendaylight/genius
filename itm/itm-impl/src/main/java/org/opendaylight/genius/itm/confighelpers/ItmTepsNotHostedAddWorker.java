/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.confighelpers;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.util.Datastore;
import org.opendaylight.mdsal.binding.util.ManagedNewTransactionRunner;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.not.hosted.transport.zones.tepsinnothostedtransportzone.UnknownVteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.not.hosted.transport.zones.tepsinnothostedtransportzone.UnknownVtepsKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItmTepsNotHostedAddWorker implements Callable<List<? extends ListenableFuture<?>>> {

    private static final Logger LOG = LoggerFactory.getLogger(ItmTepsNotHostedAddWorker.class);
    private final Map<UnknownVtepsKey, UnknownVteps> vtepsList;
    private final  String tzName;
    private final ManagedNewTransactionRunner txRunner;

    public ItmTepsNotHostedAddWorker(Map<UnknownVtepsKey, UnknownVteps> vtepsList, String tzName, DataBroker broker,
                                     ManagedNewTransactionRunner txRunner) {
        this.vtepsList = vtepsList;
        this.tzName = tzName;
        this.txRunner = txRunner;
    }

    @Override
    public List<? extends ListenableFuture<?>> call() throws Exception {
        LOG.trace("Add TEP into TepsNotHosted list task is picked from DataStoreJobCoordinator for execution.");

        // Add TEP to TepsNotHosted list.
        return Collections.singletonList(txRunner.callWithNewReadWriteTransactionAndSubmit(Datastore.OPERATIONAL,
            tx -> OvsdbTepAddConfigHelper
                    .addVtepIntoTepsNotHosted(vtepsList, tzName, tx)));
    }
}
