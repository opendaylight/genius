/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra;

import com.google.common.annotations.Beta;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;

/**
 * Utility to simplify correctly handling transformation of Future of RpcResult to return.
 *
 * @author Michael Vorburger.ch
 */
@Beta
public final class FutureRpcResults {

    // NB: The FutureRpcResultsTest unit test for this util is in mdsalutil-testutils's src/test, not this project's

    // TODO Once matured in genius, this class should be proposed to org.opendaylight.yangtools.yang.common

    private FutureRpcResults() {}

    /**
     * Create a Builder for a ListenableFuture to Future&lt;RpcResult&lt;O&gt;&gt; transformer.
     *
     * @param logger the slf4j Logger of the caller
     * @param rpcMethodName Java method name (without "()") of the RPC operation, used for logging
     * @param input the RPC input DataObject of the caller (may be null)
     * @param callable the Callable (typically lambda) creating a ListenableFuture.  Note that the
     *        functional interface Callable's call() method declares throws Exception, so your lambda
     *        does not have to do any exception handling (specifically it does NOT have to catch and
     *        wrap any exception into a failed Future); this utility does that for you.
     *
     * @return a new Builder
     */
    @CheckReturnValue
    public static <I, O> FutureRpcResultBuilder<I, O> fromListenableFuture(Logger logger, String rpcMethodName,
            @Nullable I input, Callable<ListenableFuture<O>> callable) {
        return new FutureRpcResultBuilder<>(logger, rpcMethodName, input, callable);
    }

    public enum LogLevel {
        ERROR, WARN, INFO, DEBUG, TRACE, NONE;

        public void log(Logger logger, String format, Object... arguments) {
            switch(this) {
                case NONE:
                    break;
                case TRACE:
                    logger.trace(format, arguments);
                    break;
                case DEBUG:
                    logger.debug(format, arguments);
                    break;
                case INFO:
                    logger.info(format, arguments);
                    break;
                case WARN:
                    logger.warn(format, arguments);
                    break;
                default: // including ERROR
                    logger.error(format, arguments);
                    break;
            }
        }
    }

    public static class FutureRpcResultBuilder<I, O> implements Builder<Future<RpcResult<O>>> {

        @Nullable private final I input;
        private final Callable<ListenableFuture<O>> callable;
        private Function<Throwable, String> rpcErrorMessageFunction = Throwable::getMessage;
        private Consumer<O> onSuccessConsumer;
        private Consumer<Throwable> onFailureConsumer;
        private final Logger logger;
        private final String rpcMethodName;
        private LogLevel onFailureLogLevel = LogLevel.ERROR;
        private LogLevel onSuccessLogLevel = LogLevel.DEBUG;

        private FutureRpcResultBuilder(Logger logger, String rpcMethodName, @Nullable I input,
                Callable<ListenableFuture<O>> callable) {
            this.input = input;
            this.callable = callable;
            this.logger = logger;
            this.rpcMethodName = rpcMethodName;
            // Default methods which can be overridden by users:
            this.onSuccessConsumer = result -> {};
            this.onFailureConsumer = throwable -> {};
        }

        @Override
        @CheckReturnValue
        @SuppressWarnings("checkstyle:IllegalCatch")
        public Future<RpcResult<O>> build() {
            SettableFuture<RpcResult<O>> futureRpcResult = SettableFuture.create();
            FutureCallback<O> callback = new FutureCallback<O>() {
                @Override
                public void onSuccess(O result) {
                    onSuccessLogLevel.log(logger, "RPC {}() successful; input = {}, output = {}", rpcMethodName,
                            input, result);
                    onSuccessConsumer.accept(result);
                    futureRpcResult.set(RpcResultBuilder.success(result).build());
                }

                @Override
                public void onFailure(Throwable cause) {
                    onFailureLogLevel.log(logger, "RPC {}() failed; input = {}", rpcMethodName, input, cause);
                    onFailureConsumer.accept(cause);
                    futureRpcResult.set(RpcResultBuilder.<O>failed().withError(
                            RpcError.ErrorType.APPLICATION, rpcErrorMessageFunction.apply(cause), cause).build());
                }
            };
            try {
                Futures.addCallback(callable.call(), callback, MoreExecutors.directExecutor());
            } catch (Exception cause) {
                callback.onFailure(cause);
            }

            return futureRpcResult;
        }

        /**
         * Sets a custom on-failure action, for a given exception.
         * By default, the action is to LOG input and exception at the {@link #onFailureLogLevel(LogLevel)}.
         */
        public FutureRpcResultBuilder<I,O> onFailure(Consumer<Throwable> newOnFailureConsumer) {
            this.onFailureConsumer = newOnFailureConsumer;
            return this;
        }

        /**
         * Sets a custom on-failure SLF4J logging level, in case of an exception.
         * By default, it is LOG.error. Setting {@code NONE} will disable logging.
         */
        public FutureRpcResultBuilder<I,O> onFailureLogLevel(LogLevel level) {
            this.onFailureLogLevel = level;
            return this;
        }

        /**
         * Sets a custom on-success SLF4J logging level, in case of an exception.
         * By default, it is LOG.debug. Setting {@code NONE} will disable logging.
         */
        public FutureRpcResultBuilder<I,O> onSuccessLogLevel(LogLevel level) {
            this.onSuccessLogLevel = level;
            return this;
        }

        /**
         * Set a custom {@link RpcError} message (function), for a given exception.
         * By default, the message is just {@link Throwable#getMessage()}.
         */
        public FutureRpcResultBuilder<I,O> withRpcErrorMessage(Function<Throwable, String> newRpcErrorMessageFunction) {
            this.rpcErrorMessageFunction = newRpcErrorMessageFunction;
            return this;
        }

        /**
         * Sets a custom on-success action, for a given output.
         * By default, the action is to LOG.debug both input and output.
         */
        public FutureRpcResultBuilder<I,O> onSuccess(Consumer<O> newOnSuccessFunction) {
            this.onSuccessConsumer = newOnSuccessFunction;
            return this;
        }
    }

}
