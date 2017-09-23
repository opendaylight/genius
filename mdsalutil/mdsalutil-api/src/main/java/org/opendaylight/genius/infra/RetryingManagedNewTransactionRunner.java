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
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.OptimisticLockFailedException;

/**
 * Implementation of {@link ManagedNewTransactionRunner} with automatic transparent retries.
 *
 * @author Michael Vorburger.ch & Stephen Kitt
 */
public class RetryingManagedNewTransactionRunner implements ManagedNewTransactionRunner {

    // TODO Cover this with a test; either integrate with ManagedNewTransactionRunnerImplTest or new one (subclass?), using DataBrokerFailures from https://git.opendaylight.org/gerrit/#/c/63120/

    private static final int MAX_RETRIES = 3;

    private final ManagedNewTransactionRunner delegate;

    // TODO I'm not clear yet if the catchingAsync usage is case where "directExecutor is dangerous (in some cases)"
    //   See the discussion in the {@link ListenableFuture#addListener ListenableFuture.addListener} JavaDoc
    //   The documentation's warnings about "lightweight listeners" refer to the work done during AsyncFunction.apply,
    //   not to any work done to complete the returned Future
    //
    // If this is NOK, then what other Executor should we use here instead of a directExecutor?
    private final Executor executor = MoreExecutors.directExecutor();

    public RetryingManagedNewTransactionRunner(ManagedNewTransactionRunner delegate) {
        super();
        this.delegate = delegate;
    }

    @Override
    public ListenableFuture<Void> callWithNewWriteOnlyTransactionAndSubmit(CheckedConsumer<WriteTransaction> txRunner) {
        return callWithNewWriteOnlyTransactionAndSubmit(txRunner, MAX_RETRIES);
    }

    private ListenableFuture<Void> callWithNewWriteOnlyTransactionAndSubmit(CheckedConsumer<WriteTransaction> txRunner,
            final int tries) {
        // TODO TDD to make sure ExecutionException/getCause for OptimisticLockFailedException is correct here...
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
}
