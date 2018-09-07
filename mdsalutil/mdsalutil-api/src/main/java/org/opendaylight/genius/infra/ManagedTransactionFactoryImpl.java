/*
 * Copyright © 2018 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra;

import static com.google.common.util.concurrent.Futures.immediateFailedFuture;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import javax.annotation.CheckReturnValue;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.TransactionFactory;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.infrautils.utils.function.InterruptibleCheckedConsumer;
import org.opendaylight.infrautils.utils.function.InterruptibleCheckedFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic implementation of a {@link ManagedTransactionFactory}.
 */
class ManagedTransactionFactoryImpl implements ManagedTransactionFactory {
    private static final Logger LOG = LoggerFactory.getLogger(ManagedTransactionFactoryImpl.class);

    private final TransactionFactory transactionFactory;

    ManagedTransactionFactoryImpl(TransactionFactory transactionFactory) {
        this.transactionFactory = transactionFactory;
    }

    @Override
    public <D extends Datastore, E extends Exception, R> R applyWithNewReadOnlyTransactionAndClose(
            Class<D> datastoreType, InterruptibleCheckedFunction<TypedReadTransaction<D>, R, E> txFunction)
            throws E, InterruptedException {
        try (ReadOnlyTransaction realTx = transactionFactory.newReadOnlyTransaction()) {
            TypedReadTransaction<D> wrappedTx = new TypedReadTransactionImpl<>(datastoreType, realTx);
            return txFunction.apply(wrappedTx);
        }
    }

    @Override
    @CheckReturnValue
    public <D extends Datastore, E extends Exception, R>
        FluentFuture<R> applyWithNewReadWriteTransactionAndSubmit(Class<D> datastoreType,
            InterruptibleCheckedFunction<TypedReadWriteTransaction<D>, R, E> txFunction) {
        return applyWithNewTransactionAndSubmit(datastoreType, transactionFactory::newReadWriteTransaction,
            TypedReadWriteTransactionImpl::new, (realTx, wrappedTx) -> realTx.commit(), txFunction);
    }

    @Override
    public <D extends Datastore, E extends Exception> void callWithNewReadOnlyTransactionAndClose(
            Class<D> datastoreType, InterruptibleCheckedConsumer<TypedReadTransaction<D>, E> txConsumer)
            throws E, InterruptedException {
        try (ReadOnlyTransaction realTx = transactionFactory.newReadOnlyTransaction()) {
            TypedReadTransaction<D> wrappedTx = new TypedReadTransactionImpl<>(datastoreType, realTx);
            txConsumer.accept(wrappedTx);
        }
    }

    @Override
    @CheckReturnValue
    public <D extends Datastore, E extends Exception>
        FluentFuture<Void> callWithNewReadWriteTransactionAndSubmit(Class<D> datastoreType,
            InterruptibleCheckedConsumer<TypedReadWriteTransaction<D>, E> txConsumer) {
        return callWithNewTransactionAndSubmit(datastoreType, transactionFactory::newReadWriteTransaction,
            TypedReadWriteTransactionImpl::new, (realTx, wrappedTx) -> realTx.commit(), txConsumer);
    }

    @Override
    public <D extends Datastore, E extends Exception> FluentFuture<Void> callWithNewWriteOnlyTransactionAndSubmit(
            Class<D> datastoreType, InterruptibleCheckedConsumer<TypedWriteTransaction<D>, E> txConsumer) {
        return callWithNewTransactionAndSubmit(datastoreType, transactionFactory::newWriteOnlyTransaction,
            TypedWriteTransactionImpl::new, (realTx, wrappedTx) -> realTx.commit(), txConsumer);
    }

    <D extends Datastore, T extends WriteTransaction, W, E extends Exception> FluentFuture<Void>
        callWithNewTransactionAndSubmit(
            Class<D> datastoreType, Supplier<T> txSupplier, BiFunction<Class<D>, T, W> txWrapper,
            BiFunction<T, W, FluentFuture<?>> txSubmitter, InterruptibleCheckedConsumer<W, E> txConsumer) {
        return applyWithNewTransactionAndSubmit(datastoreType, txSupplier, txWrapper, txSubmitter, tx -> {
            txConsumer.accept(tx);
            return null;
        });
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    <D extends Datastore, T extends WriteTransaction, W, R, E extends Exception> FluentFuture<R>
        applyWithNewTransactionAndSubmit(
            Class<D> datastoreType, Supplier<T> txSupplier, BiFunction<Class<D>, T, W> txWrapper,
            BiFunction<T, W, FluentFuture<?>> txSubmitter, InterruptibleCheckedFunction<W, R, E> txFunction) {
        T realTx = txSupplier.get();
        W wrappedTx = txWrapper.apply(datastoreType, realTx);
        try {
            // We must store the result before submitting the transaction; if we inline the next line in the
            // transform lambda, that's not guaranteed
            R result = txFunction.apply(wrappedTx);
            return txSubmitter.apply(realTx, wrappedTx).transform(v -> result, MoreExecutors.directExecutor());
        } catch (Exception e) {
            // catch Exception for both the <E extends Exception> thrown by accept() as well as any RuntimeException
            if (!realTx.cancel()) {
                LOG.error("Transaction.cancel() return false - this should never happen (here)");
            }
            return FluentFuture.from(immediateFailedFuture(e));
        }
    }
}
