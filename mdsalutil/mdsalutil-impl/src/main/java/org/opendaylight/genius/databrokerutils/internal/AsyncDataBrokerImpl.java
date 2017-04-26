/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.databrokerutils.internal;

import java.util.function.Function;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.databrokerutils.AsyncDataBroker;
import org.opendaylight.genius.databrokerutils.AsyncReadTransaction;
import org.opendaylight.genius.databrokerutils.infra.CompletionStage2;

/**
 * Implementation of {@link AsyncDataBroker}.
 *
 * @author Michael Vorburger.ch
 */
public class AsyncDataBrokerImpl implements AsyncDataBroker {

    private final DataBroker dataBroker;

    public AsyncDataBrokerImpl(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    @Override
    public AsyncReadTransaction newAsyncReadOnlyTransaction() {
        return new AsyncReadTransactionImpl(dataBroker.newReadOnlyTransaction());
    }

    @Override
    @SuppressWarnings("resource")
    public CompletionStage2<?> withNewSingleAsyncReadOnlyTransaction(
            Function<AsyncReadTransaction, CompletionStage2<?>> lambda) {
        final AsyncReadTransaction tx = newAsyncReadOnlyTransaction();
        CompletionStage2<?> future = lambda.apply(tx);
        future.thenAccept(v -> tx.close());
        return future;
    }

}
