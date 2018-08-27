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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.Function;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.OptimisticLockFailedException;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.infrautils.utils.function.InterruptibleCheckedConsumer;
import org.opendaylight.infrautils.utils.function.InterruptibleCheckedFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOG = LoggerFactory.getLogger(RetryingManagedNewTransactionRunnerImpl.class);

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
    public <D extends Datastore, E extends Exception, R> R applyWithNewReadOnlyTransactionAndClose(
            Class<D> datastoreType, InterruptibleCheckedFunction<TypedReadTransaction<D>, R, E> txFunction)
            throws E, InterruptedException {
        LOG.warn("The retrying transaction manager doesn't implement retry semantics with read-only transactions");
        return delegate.applyWithNewReadOnlyTransactionAndClose(datastoreType, txFunction);
    }

    @Override
    public <D extends Datastore, E extends Exception, R> FluentFuture<R> applyWithNewReadWriteTransactionAndSubmit(
            Class<D> datastoreType, InterruptibleCheckedFunction<TypedReadWriteTransaction<D>, R, E> txFunction) {
        return applyWithNewReadWriteTransactionAndSubmit(datastoreType, txFunction, maxRetries);
    }

    private <D extends Datastore, E extends Exception, R> FluentFuture<R> applyWithNewReadWriteTransactionAndSubmit(
            Class<D> datastoreType, InterruptibleCheckedFunction<TypedReadWriteTransaction<D>, R, E> txRunner,
            int tries) {
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

    @Override
    public <R> R applyWithNewTransactionChainAndClose(Function<ManagedTransactionChain, R> chainConsumer) {
        throw new UnsupportedOperationException("The retrying transaction manager doesn't support transaction chains");
    }

    @Override
    public <D extends Datastore, E extends Exception> void callWithNewReadOnlyTransactionAndClose(
            Class<D> datastoreType, InterruptibleCheckedConsumer<TypedReadTransaction<D>, E> txConsumer)
            throws E, InterruptedException {
        LOG.warn("The retrying transaction manager doesn't implement retry semantics with read-only transactions");
        delegate.callWithNewReadOnlyTransactionAndClose(datastoreType, txConsumer);
    }

    @Override
    public <E extends Exception> ListenableFuture<Void> callWithNewReadWriteTransactionAndSubmit(
        InterruptibleCheckedConsumer<ReadWriteTransaction, E> txConsumer) {
        return callWithNewReadWriteTransactionAndSubmit(txConsumer, maxRetries);
    }

    private <E extends Exception> ListenableFuture<Void> callWithNewReadWriteTransactionAndSubmit(
        InterruptibleCheckedConsumer<ReadWriteTransaction, E> txRunner, final int tries) {
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
    public <D extends Datastore, E extends Exception> FluentFuture<Void>
        callWithNewReadWriteTransactionAndSubmit(
            Class<D> datastoreType, InterruptibleCheckedConsumer<TypedReadWriteTransaction<D>, E> txConsumer) {
        return callWithNewReadWriteTransactionAndSubmit(datastoreType, txConsumer, maxRetries);
    }

    private <D extends Datastore, E extends Exception> FluentFuture<Void>
        callWithNewReadWriteTransactionAndSubmit(Class<D> datastoreType,
            InterruptibleCheckedConsumer<TypedReadWriteTransaction<D>, E> txRunner, int tries) {

        return Objects.requireNonNull(delegate.callWithNewReadWriteTransactionAndSubmit(datastoreType, txRunner),
            "delegate.callWithNewWriteOnlyTransactionAndSubmit() == null")
            .catchingAsync(OptimisticLockFailedException.class, optimisticLockFailedException -> {
                // as per AsyncWriteTransaction.submit()'s JavaDoc re. retries
                if (tries - 1 > 0) {
                    return callWithNewReadWriteTransactionAndSubmit(datastoreType, txRunner, tries - 1);
                } else {
                    // out of retries, so propagate the OptimisticLockFailedException
                    throw optimisticLockFailedException;
                }
            }, executor);
    }

    @Override
    public <E extends Exception> ListenableFuture<Void>
        callWithNewWriteOnlyTransactionAndSubmit(InterruptibleCheckedConsumer<WriteTransaction, E> txConsumer) {
        return callWithNewWriteOnlyTransactionAndSubmit(txConsumer, maxRetries);
    }

    private <E extends Exception> ListenableFuture<Void> callWithNewWriteOnlyTransactionAndSubmit(
        InterruptibleCheckedConsumer<WriteTransaction, E> txRunner, final int tries) {

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
    public <D extends Datastore, E extends Exception> FluentFuture<Void>
        callWithNewWriteOnlyTransactionAndSubmit(Class<D> datastoreType,
            InterruptibleCheckedConsumer<TypedWriteTransaction<D>, E> txConsumer) {
        return callWithNewWriteOnlyTransactionAndSubmit(datastoreType, txConsumer, maxRetries);
    }

    private <D extends Datastore, E extends Exception> FluentFuture<Void>
        callWithNewWriteOnlyTransactionAndSubmit(Class<D> datastoreType,
            InterruptibleCheckedConsumer<TypedWriteTransaction<D>, E> txRunner, int tries) {

        return Objects.requireNonNull(delegate.callWithNewWriteOnlyTransactionAndSubmit(datastoreType, txRunner),
                "delegate.callWithNewWriteOnlyTransactionAndSubmit() == null")
                .catchingAsync(OptimisticLockFailedException.class, optimisticLockFailedException -> {
                    // as per AsyncWriteTransaction.submit()'s JavaDoc re. retries
                    if (tries - 1 > 0) {
                        return callWithNewWriteOnlyTransactionAndSubmit(datastoreType, txRunner, tries - 1);
                    } else {
                        // out of retries, so propagate the OptimisticLockFailedException
                        throw optimisticLockFailedException;
                    }
                }, executor);
    }
}
