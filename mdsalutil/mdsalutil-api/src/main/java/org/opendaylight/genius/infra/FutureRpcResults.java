/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra;

import com.google.common.annotations.Beta;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.infrautils.utils.StackTraces;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;

/**
 * Utility to simplify correctly handling transformation of Future of RpcResult to return.
 *
 * @author Michael Vorburger.ch
 */
@Beta
public final class FutureRpcResults {

    // NB: The FutureRpcResultsTest unit test for this util is in mdsalutil-testutils's src/test, not this project's

    // TODO Once matured in genius, this class could be proposed to org.opendaylight.yangtools.yang.common
    // (This was proposed in Oct on yangtools-dev list, but there little interest due to plans to change RpcResult.)

    private FutureRpcResults() {}

    /**
     * Create a Builder for a ListenableFuture to Future&lt;RpcResult&lt;O&gt;&gt; transformer. By default, the future
     * will log success or failure, with configurable log levels; the caller can also add handlers for success and/or
     * failure.
     *
     * <p>The RPC's method name is automatically obtained using {@link StackTraces}.  This has some cost, which in
     * the overall scheme of a typical RPC is typically negligible, but on a highly optimized fast path could
     * theoretically be an issue; if you see this method as a hot spot in a profiler, then (only) use the
     * alternative signature where you manually pass the String rpcMethodName.
     *
     * @param logger the slf4j Logger of the caller
     * @param input the RPC input DataObject of the caller (may be null)
     * @param callable the Callable (typically lambda) creating a ListenableFuture.  Note that the
     *        functional interface Callable's call() method declares throws Exception, so your lambda
     *        does not have to do any exception handling (specifically it does NOT have to catch and
     *        wrap any exception into a failed Future); this utility does that for you.
     *
     * @return a new Builder
     */
    @CheckReturnValue
    public static <I, O> FutureRpcResultBuilder<I, O> fromListenableFuture(Logger logger,
            @Nullable I input, Callable<ListenableFuture<O>> callable) {
        return new FutureRpcResultBuilder<>(org.opendaylight.genius.tools.mdsal.rpc
                .FutureRpcResults.fromListenableFuture(logger, input, callable));
    }

    /**
     * Create a Builder for a ListenableFuture to Future&lt;RpcResult&lt;O&gt;&gt; transformer. By default, the future
     * will log success or failure, with configurable log levels; the caller can also add handlers for success and/or
     * failure.
     *
     * @param logger the slf4j Logger of the caller
     * @param rpcMethodName Java method name (without "()") of the RPC operation, used for logging
     * @param input the RPC input DataObject of the caller (may be null)
     * @param callable the Callable (typically lambda) creating a ListenableFuture.  Note that the
     *        functional interface Callable's call() method declares throws Exception, so your lambda
     *        does not have to do any exception handling (specifically it does NOT have to catch and
     *        wrap any exception into a failed Future); this utility does that for you.
     *
     * @return a new FutureRpcResultBuilder
     */
    @CheckReturnValue
    public static <I, O> FutureRpcResultBuilder<I, O> fromListenableFuture(Logger logger, String rpcMethodName,
            @Nullable I input, Callable<ListenableFuture<O>> callable) {
        return new FutureRpcResultBuilder<>(org.opendaylight.genius.tools.mdsal.rpc.FutureRpcResults
                .fromListenableFuture(logger, rpcMethodName, input, callable));
    }

    @CheckReturnValue
    public static <I, O> FutureRpcResultBuilder<I, O> fromBuilder(Logger logger, String rpcMethodName,
            @Nullable I input, Callable<Builder<O>> builder) {
        return new FutureRpcResultBuilder<>(org.opendaylight.genius.tools.mdsal.rpc
                .FutureRpcResults.fromBuilder(logger, rpcMethodName, input, builder));
    }

    @CheckReturnValue
    public static <I, O> FutureRpcResultBuilder<I, O> fromBuilder(Logger logger, @Nullable I input,
            Callable<Builder<O>> builder) {
        return new FutureRpcResultBuilder<>(org.opendaylight.genius.tools.mdsal.rpc
                .FutureRpcResults.fromBuilder(logger, input, builder));
    }

    @NotThreadSafe
    public static final class FutureRpcResultBuilder<I, O> implements Builder<Future<RpcResult<O>>> {

        private final org.opendaylight.genius.tools.mdsal.rpc.FutureRpcResults.FutureRpcResultBuilder delegate;

        private FutureRpcResultBuilder(
                org.opendaylight.genius.tools.mdsal.rpc.FutureRpcResults.FutureRpcResultBuilder delegate) {
            this.delegate = delegate;
        }

        @Override
        @CheckReturnValue
        public Future<RpcResult<O>> build() {
            return this.delegate.build();
        }

        /**
         * Sets a custom on-failure action, for a given exception.
         */
        public FutureRpcResultBuilder<I,O> onFailure(Consumer<Throwable> newOnFailureConsumer) {
            delegate.onFailure(newOnFailureConsumer);
            return this;
        }

        /**
         * Sets a custom on-failure SLF4J logging level, in case of an exception. The log message mentions the RPC
         * method name, the provided input, the exception and its stack trace (depending on logger settings).
         * By default, it is {@code LOG.error}. Setting {@code NONE} will disable this logging.
         */
        public FutureRpcResultBuilder<I,O> onFailureLogLevel(
                org.opendaylight.genius.tools.mdsal.rpc.FutureRpcResults.LogLevel level) {
            delegate.onFailureLogLevel(level);
            return this;
        }

        /**
         * Set a custom {@link RpcError} message function, for a given exception.
         * By default, the message is just {@link Throwable#getMessage()}.
         */
        public FutureRpcResultBuilder<I,O> withRpcErrorMessage(Function<Throwable, String> newRpcErrorMessageFunction) {
            delegate.withRpcErrorMessage(newRpcErrorMessageFunction);
            return this;
        }

        /**
         * Sets a custom on-success action, for a given output.
         */
        public FutureRpcResultBuilder<I,O> onSuccess(Consumer<O> newOnSuccessFunction) {
            delegate.onSuccess(newOnSuccessFunction);
            return this;
        }

    }
}
