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
import edu.umd.cs.findbugs.annotations.CheckReturnValue;
import java.util.function.Function;
import javax.inject.Inject;
import org.opendaylight.infrautils.utils.function.InterruptibleCheckedConsumer;
import org.opendaylight.infrautils.utils.function.InterruptibleCheckedFunction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.binding.api.Transaction;
import org.opendaylight.mdsal.binding.api.TransactionChain;
import org.opendaylight.mdsal.binding.api.TransactionChainListener;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation of {@link ManagedNewTransactionRunner}. This is based on {@link ManagedTransactionFactoryImpl} but
 * re-implements operations based on (read-)write transactions to cancel transactions which don't end up making any
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
    @CheckReturnValue
    public <E extends Exception> ListenableFuture<Void>
            callWithNewWriteOnlyTransactionAndSubmit(InterruptibleCheckedConsumer<WriteTransaction, E> txConsumer) {
        return callWithNewTransactionAndSubmit(Datastore.class, broker::newWriteOnlyTransaction,
            (datastoreType, realTx) -> new NonSubmitCancelableWriteTransaction(realTx),
            txConsumer::accept, (realTx, wrappedTx) -> realTx.commit());
    }

    @Override
    @CheckReturnValue
    public <D extends Datastore, E extends Exception> FluentFuture<Void> callWithNewWriteOnlyTransactionAndSubmit(
        Class<D> datastoreType, InterruptibleCheckedConsumer<TypedWriteTransaction<D>, E> txConsumer) {
        return callWithNewTransactionAndSubmit(datastoreType, broker::newWriteOnlyTransaction,
            WriteTrackingTypedWriteTransactionImpl::new, txConsumer::accept, this::commit);
    }

    @Override
    @CheckReturnValue
    public <E extends Exception> ListenableFuture<Void>
            callWithNewReadWriteTransactionAndSubmit(InterruptibleCheckedConsumer<ReadWriteTransaction, E> txConsumer) {
        return callWithNewTransactionAndSubmit(Datastore.class, broker::newReadWriteTransaction,
            (datastoreType, realTx) -> new NonSubmitCancelableReadWriteTransaction(realTx), txConsumer::accept,
            this::commit);
    }

    @Override
    @CheckReturnValue
    public <D extends Datastore, E extends Exception> FluentFuture<Void>
        callWithNewReadWriteTransactionAndSubmit(Class<D> datastoreType,
            InterruptibleCheckedConsumer<TypedReadWriteTransaction<D>, E> txConsumer) {
        return callWithNewTransactionAndSubmit(datastoreType, broker::newReadWriteTransaction,
            WriteTrackingTypedReadWriteTransactionImpl::new, txConsumer::accept, this::commit);
    }

    @Override
    @CheckReturnValue
    public <D extends Datastore, E extends Exception, R> FluentFuture<R> applyWithNewReadWriteTransactionAndSubmit(
            Class<D> datastoreType, InterruptibleCheckedFunction<TypedReadWriteTransaction<D>, R, E> txFunction) {
        return super.applyWithNewTransactionAndSubmit(datastoreType, broker::newReadWriteTransaction,
            WriteTrackingTypedReadWriteTransactionImpl::new, txFunction::apply, this::commit);
    }

    @Override
    public <R> R applyWithNewTransactionChainAndClose(Function<ManagedTransactionChain, R> chainConsumer) {
        try (TransactionChain realTxChain = broker.createTransactionChain(new TransactionChainListener() {
            @Override
            public void onTransactionChainFailed(TransactionChain chain, Transaction transaction,
                Throwable cause) {
                LOG.error("Error handling a transaction chain", cause);
            }

            @Override
            public void onTransactionChainSuccessful(TransactionChain chain) {
                // Nothing to do
            }
        })) {
            return chainConsumer.apply(new ManagedTransactionChainImpl(realTxChain));
        }
    }

    @CheckReturnValue
    private FluentFuture<? extends CommitInfo> commit(WriteTransaction realTx, WriteTrackingTransaction wrappedTx) {
        if (wrappedTx.isWritten()) {
            // The transaction contains changes, commit it
            return realTx.commit();
        } else {
            // The transaction only handled reads, cancel it
            realTx.cancel();
            return FluentFuture.from(Futures.immediateFuture(CommitInfo.empty()));
        }
    }
}
