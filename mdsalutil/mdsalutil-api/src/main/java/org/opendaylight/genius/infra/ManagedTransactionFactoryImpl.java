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
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
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
    @SuppressWarnings("checkstyle:IllegalCatch")
    public <D extends Datastore, E extends Exception, R>
        FluentFuture<R> applyWithNewReadWriteTransactionAndSubmit(Class<D> datastoreType,
            InterruptibleCheckedFunction<TypedReadWriteTransaction<D>, R, E> txFunction) {
        ReadWriteTransaction realTx = transactionFactory.newReadWriteTransaction();
        TypedReadWriteTransaction<D> wrappedTx = new TypedReadWriteTransactionImpl<>(datastoreType, realTx);
        try {
            return commit(realTx, txFunction.apply(wrappedTx));
        } catch (Exception e) {
            // catch Exception for both the <E extends Exception> thrown by accept() as well as any RuntimeException
            if (!realTx.cancel()) {
                LOG.error("Transaction.cancel() returned false, which should never happen here");
            }
            return FluentFuture.from(immediateFailedFuture(e));
        }
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
    @SuppressWarnings("checkstyle:IllegalCatch")
    public <D extends Datastore, E extends Exception>
        FluentFuture<Void> callWithNewReadWriteTransactionAndSubmit(Class<D> datastoreType,
            InterruptibleCheckedConsumer<TypedReadWriteTransaction<D>, E> txConsumer) {
        ReadWriteTransaction realTx = transactionFactory.newReadWriteTransaction();
        TypedReadWriteTransaction<D> wrappedTx = new TypedReadWriteTransactionImpl<>(datastoreType, realTx);
        try {
            txConsumer.accept(wrappedTx);
            return commit(realTx, null);
            // catch Exception for both the <E extends Exception> thrown by accept() as well as any RuntimeException
        } catch (Exception e) {
            if (!realTx.cancel()) {
                LOG.error("Transaction.cancel() returned false, which should never happen here");
            }
            return FluentFuture.from(immediateFailedFuture(e));
        }
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public <D extends Datastore, E extends Exception> FluentFuture<Void> callWithNewWriteOnlyTransactionAndSubmit(
            Class<D> datastoreType, InterruptibleCheckedConsumer<TypedWriteTransaction<D>, E> txConsumer) {
        WriteTransaction realTx = transactionFactory.newWriteOnlyTransaction();
        TypedWriteTransaction<D> wrappedTx =
            new TypedWriteTransactionImpl<>(datastoreType, realTx);
        try {
            txConsumer.accept(wrappedTx);
            return commit(realTx, null);
            // catch Exception for both the <E extends Exception> thrown by accept() as well as any RuntimeException
        } catch (Exception e) {
            if (!realTx.cancel()) {
                LOG.error("Transaction.cancel() return false - this should never happen (here)");
            }
            return FluentFuture.from(immediateFailedFuture(e));
        }
    }

    private <R> FluentFuture<R> commit(WriteTransaction realTx, @Nullable R result) {
        return realTx.commit().transform(v -> result, MoreExecutors.directExecutor());
    }
}
