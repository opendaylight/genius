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
import java.util.function.Function;
import javax.annotation.CheckReturnValue;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

/**
 * Utility to simplify correctly handling transformation of Future of RpcResult to return.
 *
 * @author Michael Vorburger.ch
 */
public final class FutureRpcResults {

    // TODO Once matured in genius, this class could perhaps be proposed to org.opendaylight.yangtools.yang.common

    private FutureRpcResults() {}

    @CheckReturnValue
    public static <T /* extends DataObject*/> FutureRpcResultBuilder<T> fromListenableFuture(
            Callable<ListenableFuture<T>> callable) {
        return new FutureRpcResultBuilder<>(callable);
    }

    // TODO custom log level (LOG.error)
    // TODO custom message

    public static class FutureRpcResultBuilder<T> implements Builder<Future<RpcResult<T>>> {

        private final Callable<ListenableFuture<T>> callable;
        private Function<Throwable, String> rpcErrorMessageFunction = e -> e.getMessage();

        private FutureRpcResultBuilder(Callable<ListenableFuture<T>> callable) {
            this.callable = callable;
        }

        @Override
        @CheckReturnValue
        @SuppressWarnings("checkstyle:IllegalCatch")
        public Future<RpcResult<T>> build() {
            SettableFuture<RpcResult<T>> futureRpcResult = SettableFuture.create();
            try {
                Futures.addCallback(callable.call(), new FutureCallback<T>() {
                    @Override
                    public void onSuccess(T result) {
                        futureRpcResult.set(RpcResultBuilder.success(result).build());
                    }

                    @Override
                    public void onFailure(Throwable cause) {
                        RpcResultBuilder<T> rpcResultBuilder = RpcResultBuilder.<T>failed().withError(
                                RpcError.ErrorType.APPLICATION, rpcErrorMessageFunction.apply(cause), cause);
                        futureRpcResult.set(rpcResultBuilder.build());
                    }
                }, MoreExecutors.directExecutor());

                return futureRpcResult;

            } catch (Exception cause) {
                RpcResultBuilder<T> rpcResultBuilder = RpcResultBuilder.<T>failed().withError(
                        RpcError.ErrorType.APPLICATION, rpcErrorMessageFunction.apply(cause), cause);
                return Futures.immediateFuture(rpcResultBuilder.build());
            }
        }

        public FutureRpcResultBuilder<T> withRpcErrorMessage(Function<Throwable, String> newRpcErrorMessageFunction) {
            this.rpcErrorMessageFunction = newRpcErrorMessageFunction;
            return this;
        }

    }

}
