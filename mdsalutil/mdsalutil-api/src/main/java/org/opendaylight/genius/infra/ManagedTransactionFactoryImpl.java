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
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.TransactionFactory;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.infrautils.utils.function.InterruptibleCheckedConsumer;
import org.opendaylight.infrautils.utils.function.InterruptibleCheckedFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ManagedTransactionFactoryImpl implements ManagedTransactionFactory {
    private static final Logger LOG = LoggerFactory.getLogger(ManagedTransactionFactoryImpl.class);

    private final TransactionFactory transactionFactory;

    ManagedTransactionFactoryImpl(
        TransactionFactory transactionFactory) {
        this.transactionFactory = transactionFactory;
    }

    @Override
    @CheckReturnValue
    @SuppressWarnings("checkstyle:IllegalCatch")
    public <D extends Datastore, E extends Exception, R>
        FluentFuture<R> applyWithNewReadWriteTransactionAndSubmit(Class<D> datastoreType,
            InterruptibleCheckedFunction<TypedReadWriteTransaction<D>, R, E> txRunner) {
        ReadWriteTransaction realTx = transactionFactory.newReadWriteTransaction();
        TypedReadWriteTransaction<D> wrappedTx = new TypedReadWriteTransactionImpl<>(datastoreType, realTx);
        try {
            R result = txRunner.apply(wrappedTx);
            return realTx.commit().transform(v -> result, MoreExecutors.directExecutor());
            // catch Exception for both the <E extends Exception> thrown by accept() as well as any RuntimeException
        } catch (Exception e) {
            if (!realTx.cancel()) {
                LOG.error("Transaction.cancel() returned false, which should never happen here");
            }
            return FluentFuture.from(immediateFailedFuture(e));
        }
    }

    @Override
    @CheckReturnValue
    @SuppressWarnings("checkstyle:IllegalCatch")
    public <D extends Datastore, E extends Exception>
        FluentFuture<Void> callWithNewReadWriteTransactionAndSubmit(Class<D> datastoreType,
            InterruptibleCheckedConsumer<TypedReadWriteTransaction<D>, E> txRunner) {
        ReadWriteTransaction realTx = transactionFactory.newReadWriteTransaction();
        TypedReadWriteTransaction<D> wrappedTx = new TypedReadWriteTransactionImpl<>(datastoreType, realTx);
        try {
            txRunner.accept(wrappedTx);
            return realTx.commit().transform(commitInfo -> null, MoreExecutors.directExecutor());
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
            Class<D> datastoreType, InterruptibleCheckedConsumer<TypedWriteTransaction<D>, E> txRunner) {
        WriteTransaction realTx = transactionFactory.newWriteOnlyTransaction();
        TypedWriteTransaction<D> wrappedTx =
            new TypedWriteTransactionImpl<>(datastoreType, realTx);
        try {
            txRunner.accept(wrappedTx);
            return realTx.commit().transform(commitInfo -> null, MoreExecutors.directExecutor());
            // catch Exception for both the <E extends Exception> thrown by accept() as well as any RuntimeException
        } catch (Exception e) {
            if (!realTx.cancel()) {
                LOG.error("Transaction.cancel() return false - this should never happen (here)");
            }
            return FluentFuture.from(immediateFailedFuture(e));
        }
    }
}
