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
import org.opendaylight.genius.itm.impl.ItmTepUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TepStateRemoveWorker implements Callable<List<ListenableFuture<Void>>> {
    private static final Logger LOG = LoggerFactory.getLogger(TepStateRemoveWorker.class);
    private final DataBroker dataBroker;
    private final ItmTepUtils itmTepUtils;
    private final String tepStateId;

    public TepStateRemoveWorker(final DataBroker dataBroker, final ItmTepUtils itmTepUtils, final String tepStateId) {
        this.dataBroker = dataBroker;
        this.itmTepUtils = itmTepUtils;
        this.tepStateId = tepStateId;
    }

    private CheckedFuture<Void, TransactionCommitFailedException> removeTepState() {
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        itmTepUtils.deleteTepState(tepStateId);
        //TODO: Delete flows
        return tx.submit();
    }

    @Override
    public List<ListenableFuture<Void>> call() throws Exception {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        futures.add(removeTepState());
        return futures ;
    }

}
