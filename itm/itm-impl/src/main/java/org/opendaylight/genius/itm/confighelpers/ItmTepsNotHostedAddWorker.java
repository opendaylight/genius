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
import java.util.concurrent.Callable;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.genius.infra.Datastore;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.not.hosted.transport.zones.tepsinnothostedtransportzone.UnknownVteps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItmTepsNotHostedAddWorker implements Callable<List<ListenableFuture<Void>>> {

    private static final Logger LOG = LoggerFactory.getLogger(ItmTepsNotHostedAddWorker.class);
    private final  List<UnknownVteps> vtepsList;
    private final  String tzName;
    private final ManagedNewTransactionRunner txRunner;

    public ItmTepsNotHostedAddWorker(List<UnknownVteps> vtepsList, String tzName, DataBroker broker,
                                     ManagedNewTransactionRunner txRunner) {
        this.vtepsList = vtepsList;
        this.tzName = tzName;
        this.txRunner = txRunner;
    }

    @Override
    public List<ListenableFuture<Void>> call() throws Exception {
        LOG.trace("Add TEP into TepsNotHosted list task is picked from DataStoreJobCoordinator for execution.");

        // Add TEP to TepsNotHosted list.
        return Collections.singletonList(txRunner.callWithNewReadWriteTransactionAndSubmit(Datastore.OPERATIONAL,
            tx -> OvsdbTepAddConfigHelper
                    .addVtepIntoTepsNotHosted(vtepsList, tzName, tx)));
    }
}
