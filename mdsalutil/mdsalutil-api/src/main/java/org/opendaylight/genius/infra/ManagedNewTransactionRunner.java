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
import edu.umd.cs.findbugs.annotations.CheckReturnValue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.function.Function;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.infrautils.utils.concurrent.ListenableFutures;
import org.opendaylight.infrautils.utils.function.CheckedConsumer;
import org.opendaylight.infrautils.utils.function.InterruptibleCheckedConsumer;
import org.opendaylight.infrautils.utils.function.InterruptibleCheckedFunction;

/**
 * Managed transactions utility to simplify handling of new transactions and ensure they are always closed.
 * Implementation in {@link ManagedNewTransactionRunnerImpl}, alternative implementation of this API with optional
 * retries is {@link RetryingManagedNewTransactionRunner}.
 *
 * <p>This should typically be used (only) directly in code which really must be creating its own new transactions,
 * such as RPC entry points, or background jobs.  Other lower level code "behind" such entry points should
 * just get handed over the transaction provided by this API.
 */
@Beta
public interface ManagedNewTransactionRunner extends ManagedTransactionFactory {

    /**
     * Invokes a consumer with a <b>NEW</b> {@link WriteTransaction}, and then submits that transaction and
     * returns the Future from that submission, or cancels it if an exception was thrown and returns a failed
     * future with that exception. Thus when this method returns, that transaction is guaranteed to have
     * been either submitted or cancelled, and will never "leak" and waste memory.
     *
     * <p>The consumer should not (cannot) itself use
     * {@link WriteTransaction#cancel()}, or
     * {@link WriteTransaction#submit()} (it will throw an {@link UnsupportedOperationException}).
     *
     * <p>This is an asynchronous API, like {@link DataBroker}'s own;
     * when returning from this method, the operation of the Transaction may well still be ongoing in the background,
     * or pending;
     * calling code therefore <b>must</b> handle the returned future, e.g. by passing it onwards (return),
     * or by itself adding callback listeners to it using {@link Futures}' methods, or by transforming it into a
     * {@link CompletionStage} using {@link ListenableFutures#toCompletionStage(ListenableFuture)} and chaining on
     * that, or at the very least simply by using
     * {@link ListenableFutures#addErrorLogging(ListenableFuture, org.slf4j.Logger, String)}
     * (but better NOT by using the blocking {@link Future#get()} on it).
     *
     * @param txConsumer the {@link CheckedConsumer} that needs a new write only transaction
     * @return the {@link ListenableFuture} returned by {@link WriteTransaction#submit()},
     *     or a failed future with an application specific exception (not from submit())
     */
    @CheckReturnValue
    @Deprecated
    <E extends Exception>
        ListenableFuture<Void> callWithNewWriteOnlyTransactionAndSubmit(
            InterruptibleCheckedConsumer<WriteTransaction, E> txConsumer);

    /**
     * Invokes a consumer with a <b>NEW</b> {@link ReadWriteTransaction}, and then submits that transaction and
     * returns the Future from that submission, or cancels it if an exception was thrown and returns a failed
     * future with that exception. Thus when this method returns, that transaction is guaranteed to have
     * been either submitted or cancelled, and will never "leak" and waste memory.
     *
     * <p>The consumer should not (cannot) itself use
     * {@link ReadWriteTransaction#cancel()}, or
     * {@link ReadWriteTransaction#submit()} (it will throw an {@link UnsupportedOperationException}).
     *
     * <p>This is an asynchronous API, like {@link DataBroker}'s own;
     * when returning from this method, the operation of the Transaction may well still be ongoing in the background,
     * or pending;
     * calling code therefore <b>must</b> handle the returned future, e.g. by passing it onwards (return),
     * or by itself adding callback listeners to it using {@link Futures}' methods, or by transforming it into a
     * {@link CompletionStage} using {@link ListenableFutures#toCompletionStage(ListenableFuture)} and chaining on
     * that, or at the very least simply by using
     * {@link ListenableFutures#addErrorLogging(ListenableFuture, org.slf4j.Logger, String)}
     * (but better NOT by using the blocking {@link Future#get()} on it).
     *
     * @param txConsumer the {@link CheckedConsumer} that needs a new read-write transaction
     * @return the {@link ListenableFuture} returned by {@link ReadWriteTransaction#submit()},
     *     or a failed future with an application specific exception (not from submit())
     */
    @CheckReturnValue
    @Deprecated
    <E extends Exception> ListenableFuture<Void>
        callWithNewReadWriteTransactionAndSubmit(InterruptibleCheckedConsumer<ReadWriteTransaction, E> txConsumer);

    /**
     * Invokes a function with a new {@link ManagedTransactionChain}, which is a wrapper around standard transaction
     * chains providing managed semantics. The transaction chain will be closed when the function returns.
     *
     * <p>This is an asynchronous API, like {@link DataBroker}'s own; when this method returns, the transactions in
     * the chain may well still be ongoing in the background, or pending. <strong>It is up to the consumer and
     * caller</strong> to agree on how failure will be handled; for example, the return type can include the futures
     * corresponding to the transactions in the chain. The implementation uses a default transaction chain listener
     * which logs an error if any of the transactions fail.
     *
     * <p>The MD-SAL transaction chain semantics are preserved: each transaction in the chain will see the results of
     * the previous transactions in the chain, even if they haven't been fully committed yet; and any error will result
     * in subsequent transactions in the chain <strong>not</strong> being submitted.
     *
     * @param chainConsumer The {@link InterruptibleCheckedFunction} that will build transactions in the transaction
     *                      chain.
     * @param <R> The type of result returned by the function.
     * @return The result of the function call.
     */
    <R> R applyWithNewTransactionChainAndClose(Function<ManagedTransactionChain, R> chainConsumer);
}
