/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra;

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
import org.slf4j.event.Level;

/**
 * Utility to simplify correctly handling transformation of Future of RpcResult to return.
 *
 * @author Michael Vorburger.ch
 */
public final class FutureRpcResults {

    // TODO Once matured in genius, this class could perhaps be proposed to org.opendaylight.yangtools.yang.common

    private FutureRpcResults() {}

    /**
     * Create a Builder for a ListenableFuture to Future&lt;RpcResult&lt;O&gt;&gt; transformer.
     *
     * @param logger the slf4j Logger of the caller
     * @param input the RPC input DataObject of the caller (may be null)
     * @param callable the Callable (typically lambda) creating a ListenableFuture, may throw any Exception
     *
     * @return a new Builder
     */
    @CheckReturnValue
    public static <I, O> FutureRpcResultBuilder<I, O> fromListenableFuture(Logger logger, String rpcMethodName,
            @Nullable I input, Callable<ListenableFuture<O>> callable) {
        return new FutureRpcResultBuilder<>(logger, rpcMethodName, input, callable);
    }

    public static class FutureRpcResultBuilder<I, O> implements Builder<Future<RpcResult<O>>> {

        private final @Nullable I input;
        private final Callable<ListenableFuture<O>> callable;
        private Function<Throwable, String> rpcErrorMessageFunction = e -> e.getMessage();
        private Consumer<O> onSuccessConsumer;
        private Consumer<Throwable> onFailureConsumer;
        private Level onFailureLogLevel = Level.ERROR;

        private FutureRpcResultBuilder(Logger logger, String rpcMethodName, I input,
                Callable<ListenableFuture<O>> callable) {
            this.input = input;
            this.callable = callable;
            // Default methods which can be overwritten by users:
            this.onSuccessConsumer = result -> {
                logger.debug("RPC {}() successful; input = {}, output = {}", rpcMethodName, input, result);
            };
            this.onFailureConsumer = throwable -> {
                switch (onFailureLogLevel) {
                    case TRACE:
                        logger.trace("RPC {}() failed; input = {}", rpcMethodName, input, throwable);
                        break;
                    case DEBUG:
                        logger.debug("RPC {}() failed; input = {}", rpcMethodName, input, throwable);
                        break;
                    case INFO:
                        logger.info("RPC {}() failed; input = {}", rpcMethodName, input, throwable);
                        break;
                    case WARN:
                        logger.warn("RPC {}() failed; input = {}", rpcMethodName, input, throwable);
                        break;
                    default: // including ERROR
                        logger.error("RPC {}() failed; input = {}", rpcMethodName, input, throwable);
                        break;
                }
            };
        }

        @Override
        @CheckReturnValue
        @SuppressWarnings("checkstyle:IllegalCatch")
        public Future<RpcResult<O>> build() {
            SettableFuture<RpcResult<O>> futureRpcResult = SettableFuture.create();
            try {
                Futures.addCallback(callable.call(), new FutureCallback<O>() {
                    @Override
                    public void onSuccess(O result) {
                        onSuccessConsumer.accept(result);
                        futureRpcResult.set(RpcResultBuilder.success(result).build());
                    }

                    @Override
                    public void onFailure(Throwable cause) {
                        futureRpcResult.set(getRpcResultOnFailure(cause));
                    }
                }, MoreExecutors.directExecutor());

                return futureRpcResult;

            } catch (Exception cause) {
                return Futures.immediateFuture(getRpcResultOnFailure(cause));
            }
        }

        private RpcResult<O> getRpcResultOnFailure(Throwable cause) {
            onFailureConsumer.accept(cause);
            RpcResultBuilder<O> rpcResultBuilder = RpcResultBuilder.<O>failed().withError(
                    RpcError.ErrorType.APPLICATION, rpcErrorMessageFunction.apply(cause), cause);
            return rpcResultBuilder.build();
        }

        /**
         * Sets a custom on-failure action, for a given exception.
         * By default, the action is to LOG.error input and exception.
         */
        public FutureRpcResultBuilder<I,O> onFailure(Consumer<Throwable> newOnFailureFunction) {
            this.onFailureConsumer = newOnFailureFunction;
            return this;
        }

        public FutureRpcResultBuilder<I,O> onFailureLog(Level level) {
            this.onFailureLogLevel = level;
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
