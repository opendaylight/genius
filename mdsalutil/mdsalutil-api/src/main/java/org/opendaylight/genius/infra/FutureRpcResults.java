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

/**
 * Utility to simplify correctly handling transformation of Future of RpcResult to return.
 *
 * @author Michael Vorburger.ch
 */
public final class FutureRpcResults {

    // TODO Once matured in genius, this class could perhaps be proposed to org.opendaylight.yangtools.yang.common

    private FutureRpcResults() {}

    @CheckReturnValue
    public static <I, O> FutureRpcResultBuilder<I, O> fromListenableFuture(Logger logger, @Nullable I input,
            Callable<ListenableFuture<O>> callable) {
        return new FutureRpcResultBuilder<>(logger, input, callable);
    }

    public static class FutureRpcResultBuilder<I, O> implements Builder<Future<RpcResult<O>>> {

        private final @Nullable I input;
        private final Callable<ListenableFuture<O>> callable;
        private Function<Throwable, String> rpcErrorMessageFunction = e -> e.getMessage();
        private Consumer<O> onSuccessFunction;

        private FutureRpcResultBuilder(Logger logger, I input, Callable<ListenableFuture<O>> callable) {
            this.callable = callable;
            this.input = input;
            this.onSuccessFunction = result -> {
                if (logger.isDebugEnabled()) {
                    logger.debug("RPC Success; input = {}, output = {}", input, result);
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
                        onSuccessFunction.accept(result);
                        futureRpcResult.set(RpcResultBuilder.success(result).build());
                    }

                    @Override
                    public void onFailure(Throwable cause) {
                        RpcResultBuilder<O> rpcResultBuilder = RpcResultBuilder.<O>failed().withError(
                                RpcError.ErrorType.APPLICATION, rpcErrorMessageFunction.apply(cause), cause);
                        futureRpcResult.set(rpcResultBuilder.build());
                    }
                }, MoreExecutors.directExecutor());

                return futureRpcResult;

            } catch (Exception cause) {
                RpcResultBuilder<O> rpcResultBuilder = RpcResultBuilder.<O>failed().withError(
                        RpcError.ErrorType.APPLICATION, rpcErrorMessageFunction.apply(cause), cause);
                return Futures.immediateFuture(rpcResultBuilder.build());
            }
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
            this.onSuccessFunction = newOnSuccessFunction;
            return this;
        }
    }

}
