/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra;

import com.google.common.annotations.Beta;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.function.Function;
import javax.inject.Inject;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.infrautils.utils.function.InterruptibleCheckedConsumer;
import org.opendaylight.infrautils.utils.function.InterruptibleCheckedFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link ManagedNewTransactionRunner}. This is based on {@link ManagedTransactionFactoryImpl} but
 * re-implements operations based on read-write transactions to cancel transactions which don't end up making any
 * changes to the datastore.
 */
@Beta
// Do *NOT* mark this as @Singleton, because users choose Impl; and as long as this in API, because of https://wiki.opendaylight.org/view/BestPractices/DI_Guidelines#Nota_Bene
public class ManagedNewTransactionRunnerImpl extends ManagedTransactionFactoryImpl
        implements ManagedNewTransactionRunner {

    private static final Logger LOG = LoggerFactory.getLogger(ManagedNewTransactionRunnerImpl.class);

    private final DataBroker broker;

    @Inject
    public ManagedNewTransactionRunnerImpl(DataBroker broker) {
        super(broker);
        this.broker = broker;
    }

    @Override
    public <E extends Exception> ListenableFuture<Void>
            callWithNewWriteOnlyTransactionAndSubmit(InterruptibleCheckedConsumer<WriteTransaction, E> txConsumer) {
        WriteTransaction realTx = broker.newWriteOnlyTransaction();
        WriteTransaction wrappedTx = new NonSubmitCancelableWriteTransaction(realTx);
        return tryCatchCancel(() -> {
            txConsumer.accept(wrappedTx);
            return realTx.commit().transform(v -> null, MoreExecutors.directExecutor());
        }, realTx);
    }

    @Override
    public <E extends Exception> ListenableFuture<Void>
            callWithNewReadWriteTransactionAndSubmit(InterruptibleCheckedConsumer<ReadWriteTransaction, E> txConsumer) {
        ReadWriteTransaction realTx = broker.newReadWriteTransaction();
        WriteTrackingReadWriteTransaction wrappedTx = new NonSubmitCancelableReadWriteTransaction(realTx);
        return tryCatchCancel(() -> {
            txConsumer.accept(wrappedTx);
            return commit(realTx, null, wrappedTx);
        }, realTx);
    }

    @Override
    public <D extends Datastore, E extends Exception> FluentFuture<Void>
        callWithNewReadWriteTransactionAndSubmit(Class<D> datastoreType,
            InterruptibleCheckedConsumer<TypedReadWriteTransaction<D>, E> txConsumer) {
        ReadWriteTransaction realTx = broker.newReadWriteTransaction();
        WriteTrackingTypedReadWriteTransactionImpl<D> wrappedTx =
            new WriteTrackingTypedReadWriteTransactionImpl<>(datastoreType, realTx);
        return tryCatchCancel(() -> {
            txConsumer.accept(wrappedTx);
            return commit(realTx, null, wrappedTx);
        }, realTx);
    }

    @Override
    public <D extends Datastore, E extends Exception, R> FluentFuture<R> applyWithNewReadWriteTransactionAndSubmit(
            Class<D> datastoreType, InterruptibleCheckedFunction<TypedReadWriteTransaction<D>, R, E> txFunction) {
        ReadWriteTransaction realTx = broker.newReadWriteTransaction();
        WriteTrackingTypedReadWriteTransactionImpl<D> wrappedTx =
                new WriteTrackingTypedReadWriteTransactionImpl<>(datastoreType, realTx);
        return tryCatchCancel(() -> commit(realTx, txFunction.apply(wrappedTx), wrappedTx), realTx);
    }

    @Override
    public <R> R applyWithNewTransactionChainAndClose(Function<ManagedTransactionChain, R> chainConsumer) {
        try (BindingTransactionChain realTxChain = broker.createTransactionChain(new TransactionChainListener() {
            @Override
            public void onTransactionChainFailed(TransactionChain<?, ?> chain, AsyncTransaction<?, ?> transaction,
                Throwable cause) {
                LOG.error("Error handling a transaction chain", cause);
            }

            @Override
            public void onTransactionChainSuccessful(TransactionChain<?, ?> chain) {
                // Nothing to do
            }
        })) {
            return chainConsumer.apply(new ManagedTransactionChainImpl(realTxChain));
        }
    }

    private <R> FluentFuture<R> commit(ReadWriteTransaction realTx, R result, WriteTrackingTransaction wrappedTx) {
        if (wrappedTx.isWritten()) {
            // The transaction contains changes, commit it
            return realTx.commit().transform(v -> result, MoreExecutors.directExecutor());
        } else {
            // The transaction only handled reads, cancel it
            realTx.cancel();
            return FluentFuture.from(Futures.immediateFuture(result));
        }
    }
}
