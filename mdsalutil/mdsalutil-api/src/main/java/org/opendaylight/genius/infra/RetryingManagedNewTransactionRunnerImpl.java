/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra;

import com.google.common.annotations.Beta;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Objects;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.OptimisticLockFailedException;
import org.opendaylight.infrautils.utils.function.CheckedConsumer;

/**
 * Implementation of {@link ManagedNewTransactionRunner} with automatic transparent retries.
 *
 * <h3>Details about the threading model used by this class</h3>
 *
 * <p>This class runs the first attempt to call the delegated {@link ManagedNewTransactionRunner},
 * which typically is a {@link ManagedNewTransactionRunnerImpl} which safely invokes {@link WriteTransaction#submit()},
 * in the using application's thread (like a {@link MoreExecutors#directExecutor()} would, if this were an
 * {@link Executor}, which it's not).
 *
 * <p>Any retry attempts required, if that <code>submit()</code> (eventually) fails with an
 * {@link OptimisticLockFailedException}, are run in the calling thread of that eventual future completion by a
 * {@link MoreExecutors#directExecutor()} implicit in the constructor which does not require you to specify an
 * explicit Executor argument.  Normally that will be an internal thread from the respective DataBroker implementation,
 * not your application's thread anymore, because that meanwhile could well be off doing something else!  Normally,
 * that is not a problem, because retries "should" be relatively uncommon, and (re)issuing some DataBroker
 * <code>put()</code> or <code>delete()</code> and <code>re-submit()</code> <i>should</i> be fast.
 *
 * <p>If this default is not suitable (e.g. for particularly slow try/retry code), then you can specify
 * another {@link Executor} to be used for the retries by using the alternative constructor.
 *
 * @author Michael Vorburger.ch &amp; Stephen Kitt, with input from Tom Pantelis re. catchingAsync &amp; direct Executor
 */
@Beta
// Do *NOT* mark this as @Singleton, because users choose Impl; and as long as this in API, because of https://wiki.opendaylight.org/view/BestPractices/DI_Guidelines#Nota_Bene
public class RetryingManagedNewTransactionRunnerImpl implements ManagedNewTransactionRunner {

    // NB: The RetryingManagedNewTransactionRunnerTest is in mdsalutil-testutils's src/test, not this project's

    private static final int DEFAULT_RETRIES = 3; // duplicated in SingleTransactionDataBroker

    private final int maxRetries;

    private final ManagedNewTransactionRunner delegate;

    private final Executor executor;

    /**
     * Constructor.
     * Please see the class level documentation above for more details about the threading model used.
     * This uses the default of 3 retries, which is typically suitable.
     *
     * @param delegate the {@link ManagedNewTransactionRunner} to run the first attempt and retries (if any) in
     */
    @Inject
    public RetryingManagedNewTransactionRunnerImpl(ManagedNewTransactionRunner delegate) {
        this(delegate, MoreExecutors.directExecutor(), DEFAULT_RETRIES);
    }

    /**
     * Constructor.
     * Please see the class level documentation above for more details about the threading model used.
     *
     * @param delegate the {@link ManagedNewTransactionRunner} to run the first attempt and retries (if any) in
     * @param maxRetries the maximum number of retry attempts
     */
    public RetryingManagedNewTransactionRunnerImpl(ManagedNewTransactionRunner delegate, int maxRetries) {
        this(delegate, MoreExecutors.directExecutor(), maxRetries);
    }

    /**
     * Constructor.
     * Please see the class level documentation above for more details about the threading model used.
     *
     * @param delegate the {@link ManagedNewTransactionRunner} to run the first attempt and retries (if any) in
     * @param executor the {@link Executor} to asynchronously run any retry attempts in
     * @param maxRetries the maximum number of retry attempts
     */
    public RetryingManagedNewTransactionRunnerImpl(ManagedNewTransactionRunner delegate, Executor executor,
            int maxRetries) {
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
}
