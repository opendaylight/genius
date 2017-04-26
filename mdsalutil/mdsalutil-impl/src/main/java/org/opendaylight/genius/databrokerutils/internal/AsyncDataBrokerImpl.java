/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.databrokerutils.internal;

import java.util.function.Function;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.genius.databrokerutils.AsyncDataBroker;
import org.opendaylight.genius.databrokerutils.AsyncReadOnlyTransaction;
import org.opendaylight.genius.databrokerutils.AsyncReadTransaction;
import org.opendaylight.genius.databrokerutils.AsyncReadWriteTransaction;
import org.opendaylight.genius.databrokerutils.infra.CompletionStage2;

/**
 * Implementation of {@link AsyncDataBroker}.
 *
 * @author Michael Vorburger.ch
 */
@Singleton
public class AsyncDataBrokerImpl implements AsyncDataBroker {

    private final DataBroker dataBroker;

    @Inject
    public AsyncDataBrokerImpl(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    @Override
    public AsyncReadOnlyTransaction newAsyncReadOnlyTransaction() {
        return new AsyncReadOnlyTransactionImpl(dataBroker.newReadOnlyTransaction());
    }

    @Override
    public AsyncReadWriteTransaction newAsyncReadWriteTransaction() {
        return new AsyncReadWriteTransactionImpl(dataBroker.newReadWriteTransaction());
    }

    @Override
    public AsyncReadTransaction asAsyncReadTransaction(ReadTransaction tx) {
        return new AsyncReadTransactionImpl(tx);
    }

    @Override
    @SuppressWarnings("resource")
    public CompletionStage2<?> withNewSingleAsyncReadOnlyTransaction(
            Function<AsyncReadTransaction, CompletionStage2<?>> lambda) {
        final AsyncReadOnlyTransaction tx = newAsyncReadOnlyTransaction();
        CompletionStage2<?> future = lambda.apply(tx);
        future.thenAccept(v -> tx.close());
        return future;
    }

    @Override
    public CompletionStage2<?> withNewSingleAsyncReadWriteTransaction(
            Function<AsyncReadWriteTransaction, CompletionStage2<?>> lambda) {
        final AsyncReadWriteTransaction tx = newAsyncReadWriteTransaction();
        CompletionStage2<?> future = lambda.apply(tx);
        future.thenAccept(v -> tx.submit());
        return future;
    }

}
