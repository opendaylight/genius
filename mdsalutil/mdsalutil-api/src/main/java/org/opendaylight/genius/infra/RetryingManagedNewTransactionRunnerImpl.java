/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Objects;
import java.util.concurrent.Executor;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.OptimisticLockFailedException;
import org.opendaylight.infrautils.utils.function.CheckedConsumer;
import org.opendaylight.infrautils.utils.function.CheckedFunction;
import org.opendaylight.mdsal.common.api.CommitInfo;

/**
 * Implementation of {@link ManagedNewTransactionRunner} with automatic transparent retries.
 * This is a package local private internal class; end-users use the {@link RetryingManagedNewTransactionRunner}.
 * @see RetryingManagedNewTransactionRunner
 *
 * @author Michael Vorburger.ch &amp; Stephen Kitt, with input from Tom Pantelis re. catchingAsync &amp; direct Executor
 */
// intentionally package local
class RetryingManagedNewTransactionRunnerImpl implements ManagedNewTransactionRunner {

    // NB: The RetryingManagedNewTransactionRunnerTest is in mdsalutil-testutils's src/test, not this project's

    private static final int DEFAULT_RETRIES = 3; // duplicated in SingleTransactionDataBroker

    private final int maxRetries;

    private final ManagedNewTransactionRunner delegate;

    private final Executor executor;

    RetryingManagedNewTransactionRunnerImpl(ManagedNewTransactionRunner delegate) {
        this(delegate, MoreExecutors.directExecutor(), DEFAULT_RETRIES);
    }

    RetryingManagedNewTransactionRunnerImpl(ManagedNewTransactionRunner delegate, int maxRetries) {
        this(delegate, MoreExecutors.directExecutor(), maxRetries);
    }

    RetryingManagedNewTransactionRunnerImpl(ManagedNewTransactionRunner delegate, Executor executor, int maxRetries) {
        this.delegate = delegate;
        this.executor = executor;
        this.maxRetries = maxRetries;
    }

    @Override
    public <E extends Exception> ListenableFuture<Void>
        callWithNewWriteOnlyTransactionAndSubmit(CheckedConsumer<WriteTransaction, E> txRunner) {
        return callWithNewWriteOnlyTransactionAndSubmit(txRunner, maxRetries);
    }

    private <E extends Exception> ListenableFuture<Void>
        callWithNewWriteOnlyTransactionAndSubmit(CheckedConsumer<WriteTransaction, E> txRunner, final int tries) {

        ListenableFuture<Void> future = Objects.requireNonNull(
                 delegate.callWithNewWriteOnlyTransactionAndSubmit(txRunner),
                "delegate.callWithNewWriteOnlyTransactionAndSubmit() == null");
        return Futures.catchingAsync(future, OptimisticLockFailedException.class, optimisticLockFailedException -> {
            // as per AsyncWriteTransaction.submit()'s JavaDoc re. retries
            if (tries - 1 > 0) {
                return callWithNewWriteOnlyTransactionAndSubmit(txRunner, tries - 1);
            } else {
                // out of retries, so propagate the OptimisticLockFailedException
                throw optimisticLockFailedException;
            }
        }, executor);
    }

    @Override
    public <D extends Datastore, E extends Exception> FluentFuture<? extends CommitInfo>
        callWithNewWriteOnlyTransactionAndSubmit(Class<D> datastoreType,
            CheckedConsumer<DatastoreWriteTransaction<D>, E> txRunner) {
        return callWithNewWriteOnlyTransactionAndSubmit(datastoreType, txRunner, maxRetries);
    }

    private <D extends Datastore, E extends Exception> FluentFuture<? extends CommitInfo>
        callWithNewWriteOnlyTransactionAndSubmit(Class<D> datastoreType,
            CheckedConsumer<DatastoreWriteTransaction<D>, E> txRunner, int tries) {

        FluentFuture<? extends CommitInfo> future = Objects.requireNonNull(
                delegate.callWithNewWriteOnlyTransactionAndSubmit(datastoreType, txRunner),
                "delegate.callWithNewWriteOnlyTransactionAndSubmit() == null");
        return future.catchingAsync(OptimisticLockFailedException.class, optimisticLockFailedException -> {
            // as per AsyncWriteTransaction.submit()'s JavaDoc re. retries
            if (tries - 1 > 0) {
                return (ListenableFuture) callWithNewWriteOnlyTransactionAndSubmit(datastoreType, txRunner, tries - 1);
            } else {
                // out of retries, so propagate the OptimisticLockFailedException
                throw optimisticLockFailedException;
            }
        }, executor);
    }

    @Override
    public <E extends Exception> ListenableFuture<Void> callWithNewReadWriteTransactionAndSubmit(
            CheckedConsumer<ReadWriteTransaction, E> txRunner) {
        return callWithNewReadWriteTransactionAndSubmit(txRunner, maxRetries);
    }

    private <E extends Exception> ListenableFuture<Void> callWithNewReadWriteTransactionAndSubmit(
            CheckedConsumer<ReadWriteTransaction, E> txRunner, final int tries) {
        ListenableFuture<Void> future = Objects.requireNonNull(
                 delegate.callWithNewReadWriteTransactionAndSubmit(txRunner),
                "delegate.callWithNewReadWriteTransactionAndSubmit() == null");
        return Futures.catchingAsync(future, OptimisticLockFailedException.class, optimisticLockFailedException -> {
            if (tries - 1 > 0) {
                return callWithNewReadWriteTransactionAndSubmit(txRunner, tries - 1);
            } else {
                throw optimisticLockFailedException;
            }
        }, executor);
    }

    @Override
    public <D extends Datastore, E extends Exception> FluentFuture<? extends CommitInfo>
        callWithNewReadWriteTransactionAndSubmit(
            Class<D> datastoreType, CheckedConsumer<DatastoreReadWriteTransaction<D>, E> txRunner) {
        return callWithNewReadWriteTransactionAndSubmit(datastoreType, txRunner, maxRetries);
    }

    private <D extends Datastore, E extends Exception> FluentFuture<? extends CommitInfo>
        callWithNewReadWriteTransactionAndSubmit(Class<D> datastoreType,
            CheckedConsumer<DatastoreReadWriteTransaction<D>, E> txRunner, int tries) {

        FluentFuture<? extends CommitInfo> future = Objects.requireNonNull(
                delegate.callWithNewReadWriteTransactionAndSubmit(datastoreType, txRunner),
                "delegate.callWithNewWriteOnlyTransactionAndSubmit() == null");
        return future.catchingAsync(OptimisticLockFailedException.class, optimisticLockFailedException -> {
            // as per AsyncWriteTransaction.submit()'s JavaDoc re. retries
            if (tries - 1 > 0) {
                return (ListenableFuture) callWithNewReadWriteTransactionAndSubmit(datastoreType, txRunner, tries - 1);
            } else {
                // out of retries, so propagate the OptimisticLockFailedException
                throw optimisticLockFailedException;
            }
        }, executor);
    }

    @Override
    public <E extends Exception, R> ListenableFuture<R> applyWithNewReadWriteTransactionAndSubmit(
            CheckedFunction<ReadWriteTransaction, R, E> txRunner) {
        return applyWithNewReadWriteTransactionAndSubmit(txRunner, maxRetries);
    }

    private <R, E extends Exception> ListenableFuture<R> applyWithNewReadWriteTransactionAndSubmit(
            CheckedFunction<ReadWriteTransaction, R, E> txRunner, int tries) {
        ListenableFuture<R> future = Objects.requireNonNull(
                delegate.applyWithNewReadWriteTransactionAndSubmit(txRunner),
                "delegate.callWithNewReadWriteTransactionAndSubmit() == null");
        return Futures.catchingAsync(future, OptimisticLockFailedException.class, optimisticLockFailedException -> {
            if (tries - 1 > 0) {
                return applyWithNewReadWriteTransactionAndSubmit(txRunner, tries - 1);
            } else {
                throw optimisticLockFailedException;
            }
        }, executor);
    }

    @Override
    public <D extends Datastore, E extends Exception, R> FluentFuture<R> applyWithNewReadWriteTransactionAndSubmit(
            Class<D> datastoreType, CheckedFunction<DatastoreReadWriteTransaction<D>, R, E> txRunner) {
        return applyWithNewReadWriteTransactionAndSubmit(datastoreType, txRunner, maxRetries);
    }

    private <D extends Datastore, E extends Exception, R> FluentFuture<R> applyWithNewReadWriteTransactionAndSubmit(
            Class<D> datastoreType, CheckedFunction<DatastoreReadWriteTransaction<D>, R, E> txRunner, int tries) {
        FluentFuture<R> future = Objects.requireNonNull(
                delegate.applyWithNewReadWriteTransactionAndSubmit(datastoreType, txRunner),
                "delegate.callWithNewReadWriteTransactionAndSubmit() == null");
        return future.catchingAsync(OptimisticLockFailedException.class, optimisticLockFailedException -> {
            if (tries - 1 > 0) {
                return applyWithNewReadWriteTransactionAndSubmit(datastoreType, txRunner, tries - 1);
            } else {
                throw optimisticLockFailedException;
            }
        }, executor);
    }
}
