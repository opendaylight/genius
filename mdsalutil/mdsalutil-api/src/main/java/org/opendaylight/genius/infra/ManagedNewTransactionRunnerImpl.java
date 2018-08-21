/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra;

import static com.google.common.util.concurrent.Futures.immediateFailedFuture;

import com.google.common.annotations.Beta;
import com.google.common.util.concurrent.ListenableFuture;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link ManagedNewTransactionRunner}.
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
    @SuppressWarnings("checkstyle:IllegalCatch")
    public <E extends Exception> ListenableFuture<Void>
            callWithNewWriteOnlyTransactionAndSubmit(InterruptibleCheckedConsumer<WriteTransaction, E> txCnsmr) {
        WriteTransaction realTx = broker.newWriteOnlyTransaction();
        WriteTransaction wrappedTx = new NonSubmitCancelableWriteTransaction(realTx);
        try {
            txCnsmr.accept(wrappedTx);
            return realTx.submit();
        // catch Exception for both the <E extends Exception> thrown by accept() as well as any RuntimeException
        } catch (Exception e) {
            if (!realTx.cancel()) {
                LOG.error("Transaction.cancel() return false - this should never happen (here)");
            }
            return immediateFailedFuture(e);
        }
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public <E extends Exception> ListenableFuture<Void>
            callWithNewReadWriteTransactionAndSubmit(InterruptibleCheckedConsumer<ReadWriteTransaction, E> txRunner) {
        ReadWriteTransaction realTx = broker.newReadWriteTransaction();
        ReadWriteTransaction wrappedTx = new NonSubmitCancelableReadWriteTransaction(realTx);
        try {
            txRunner.accept(wrappedTx);
            return realTx.submit();
        // catch Exception for both the <E extends Exception> thrown by accept() as well as any RuntimeException
        } catch (Exception e) {
            if (!realTx.cancel()) {
                LOG.error("Transaction.cancel() returned false, which should never happen here");
            }
            return immediateFailedFuture(e);
        }
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
}
