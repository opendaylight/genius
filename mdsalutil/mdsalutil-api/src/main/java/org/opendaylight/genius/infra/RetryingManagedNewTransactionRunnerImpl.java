/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Objects;
import java.util.concurrent.Executor;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.OptimisticLockFailedException;
import org.opendaylight.infrautils.utils.function.CheckedConsumer;

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

    private static final int MAX_RETRIES = 3;

    private final ManagedNewTransactionRunner delegate;

    private final Executor executor;

    /**
     * Constructor.
     * Please see the class level documentation above for more details about the threading model used.
     *
     * @param delegate the {@link ManagedNewTransactionRunner} to run the first attempt and retries (if any) in
     */
    RetryingManagedNewTransactionRunnerImpl(ManagedNewTransactionRunner delegate) {
        this(delegate, MoreExecutors.directExecutor());
    }

    /**
     * Constructor.
     * Please see the class level documentation above for more details about the threading model used.
     *
     * @param delegate the {@link ManagedNewTransactionRunner} to run the first attempt and retries (if any) in
     * @param executor the {@link Executor} to asynchronously run any retry attempts in
     */
    RetryingManagedNewTransactionRunnerImpl(ManagedNewTransactionRunner delegate, Executor executor) {
        this.delegate = delegate;
        this.executor = executor;
    }

    @Override
    public <E extends Exception> ListenableFuture<Void>
        callWithNewWriteOnlyTransactionAndSubmit(CheckedConsumer<WriteTransaction, E> txRunner) {

        return callWithNewWriteOnlyTransactionAndSubmit(txRunner, MAX_RETRIES);
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
    public <E extends Exception> ListenableFuture<Void> callWithNewReadWriteTransactionAndSubmit(
            CheckedConsumer<ReadWriteTransaction, E> txRunner) {
        return callWithNewReadWriteTransactionAndSubmit(txRunner, MAX_RETRIES);
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
}
