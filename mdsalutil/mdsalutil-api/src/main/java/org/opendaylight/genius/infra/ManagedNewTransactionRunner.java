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
import io.netty.util.concurrent.Future;
import java.util.concurrent.CompletionStage;
import javax.annotation.CheckReturnValue;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;

/**
 * Managed transactions utility to simplify handling of new transactions and ensure they are always closed.
 *
 * <p>This should typically be used (only) in code which really must be creating its own new transactions,
 * such as RPC entry points, or background jobs.
 */
@Beta
public interface ManagedNewTransactionRunner {

    /**
     * Invokes a consumer with a <b>NEW</b> {@link WriteTransaction}, and then submits that transaction and
     * returns the Future from that submission, or cancels it if an exception was thrown and returns a failed
     * future with that exception. Thus when this method returns, that transaction is guaranteed to have
     * been either submitted or cancelled, and will never "leak" and waste memory.
     *
     * <p>The consumer should not (cannot) itself use
     * {@link WriteTransaction#cancel()}, {@link WriteTransaction#commit()} or
     * {@link WriteTransaction#submit()} (it will throw an {@link UnsupportedOperationException}).
     *
     * <p>This is an asynchronous API, like {@link DataBroker}'s own;
     * when returning from this method, the operation of the Transaction may well still be ongoing in the background,
     * or pending;
     * calling code therefore <b>must</b> handle the returned future, e.g. by passing it onwards (return),
     * or by itself adding callback listeners to it using {@link Futures}' methods, or by transforming it into a
     * {@link CompletionStage} using infrautils' ListenableFutures and chaining on
     * that, or at the very least simply by using
     * ListenableFutures's addErrorLogging()
     * (but better NOT by using the blocking {@link Future#get()} on it).
     *
     * @param txRunner the {@link CheckedConsumer} that needs a new write only transaction
     *
     * @return the {@link ListenableFuture} returned by {@link WriteTransaction#submit()},
     *         or a failed future with an application specific exception (not from submit())
     */
    @CheckReturnValue
    ListenableFuture<Void> callWithNewWriteOnlyTransactionAndSubmit(CheckedConsumer<WriteTransaction> txRunner);

    // TODO ListenableFuture<Void> callWithNewReadWriteTransactionAndSubmit(CheckedConsumer<ReadWriteTransaction> ...);

    // TODO void callWithNewReadOnlyTransactionAndClose(CheckedConsumer<ReadOnlyTransaction> txRunner);

}
