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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.OptimisticLockFailedException;

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
 * @author Michael Vorburger.ch & Stephen Kitt
 */
public class RetryingManagedNewTransactionRunner implements ManagedNewTransactionRunner {

    // TODO Cover this with a test; either integrate with ManagedNewTransactionRunnerImplTest or new one (subclass?), using DataBrokerFailures from https://git.opendaylight.org/gerrit/#/c/63120/

    private static final int MAX_RETRIES = 3;

    private final ManagedNewTransactionRunner delegate;

    private final Executor executor;

    /**
     * Constructor.
     * Please see the class level documentation above for more details about the threading model used.
     *
     * @param delegate the {@link ManagedNewTransactionRunner} to run the first attempt and retries (if any) in
     */
    public RetryingManagedNewTransactionRunner(ManagedNewTransactionRunner delegate) {
        this(delegate, MoreExecutors.directExecutor());
    }

    /**
     * Constructor.
     * Please see the class level documentation above for more details about the threading model used.
     *
     * @param delegate the {@link ManagedNewTransactionRunner} to run the first attempt and retries (if any) in
     * @param executor the {@link Executor} to asynchronously run any retry attempts in
     */
    public RetryingManagedNewTransactionRunner(ManagedNewTransactionRunner delegate, Executor executor) {
        this.delegate = delegate;
        this.executor = executor;
    }

    @Override
    public ListenableFuture<Void> callWithNewWriteOnlyTransactionAndSubmit(CheckedConsumer<WriteTransaction> txRunner) {
        return callWithNewWriteOnlyTransactionAndSubmit(txRunner, MAX_RETRIES);
    }

    private ListenableFuture<Void> callWithNewWriteOnlyTransactionAndSubmit(CheckedConsumer<WriteTransaction> txRunner,
            final int tries) {
        return Futures.catchingAsync(delegate.callWithNewWriteOnlyTransactionAndSubmit(txRunner),
                ExecutionException.class,
            executionException -> {

                if (executionException.getCause() instanceof OptimisticLockFailedException) {
                    // as per AsyncWriteTransaction.submit()'s JavaDoc re. retries:
                    if (tries - 1 > 0) {
                        return callWithNewWriteOnlyTransactionAndSubmit(txRunner, tries - 1);
                    } else {
                        // out of retries, propagate OptimisticLockFailedException
                        // TODO Log warn or error here?
                        throw executionException;
                    }
                } else {
                    // failed due to another type of TransactionCommitFailedException.
                    throw executionException;
                }
            }, executor);
    }

    @Override
    public ListenableFuture<Void> callWithNewReadWriteTransactionAndSubmit(
            CheckedConsumer<ReadWriteTransaction> txRunner) {
        return callWithNewReadWriteTransactionAndSubmit(txRunner, MAX_RETRIES);
    }

    private ListenableFuture<Void> callWithNewReadWriteTransactionAndSubmit(
            CheckedConsumer<ReadWriteTransaction> txRunner, final int tries) {
        return Futures.catchingAsync(delegate.callWithNewReadWriteTransactionAndSubmit(txRunner),
                ExecutionException.class,
            executionException -> {

                if (executionException.getCause() instanceof OptimisticLockFailedException) {
                    if (tries - 1 > 0) {
                        return callWithNewReadWriteTransactionAndSubmit(txRunner, tries - 1);
                    } else {
                        throw executionException;
                    }
                } else {
                    throw executionException;
                }
            }, executor);
    }

}
