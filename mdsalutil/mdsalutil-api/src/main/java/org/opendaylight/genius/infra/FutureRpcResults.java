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
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.common.OperationFailedException;
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

    // TODO Once matured in genius, this class could be proposed to org.opendaylight.yangtools.yang.common
    // (This was proposed in Oct on yangtools-dev list, but there little interest due to plans to change RpcResult.)

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
     * @return a new FutureRpcResultBuilder
     */
    @CheckReturnValue
    public static <I, O> FutureRpcResultBuilder<I, O> fromListenableFuture(Logger logger, String rpcMethodName,
            @Nullable I input, Callable<ListenableFuture<O>> callable) {
        return new FutureRpcResultBuilder<>(logger, rpcMethodName, input, callable);
    }

    @CheckReturnValue
    public static <I, O> FutureRpcResultBuilder<I, O> fromBuilder(Logger logger, String rpcMethodName,
            @Nullable I input, Callable<Builder<O>> builder) {
        Callable<ListenableFuture<O>> callable = () -> Futures.immediateFuture(builder.call().build());
        return fromListenableFuture(logger, rpcMethodName, input, callable);
    }

    public enum LogLevel { ERROR, WARN, INFO, DEBUG, TRACE }

    @NotThreadSafe
    public static class FutureRpcResultBuilder<I, O> implements Builder<Future<RpcResult<O>>> {

        private final Logger logger;
        private final String rpcMethodName;
        @Nullable private final I input;
        private final Callable<ListenableFuture<O>> callable;
        private Function<Throwable, String> rpcErrorMessageFunction = e -> e.getMessage();
        private Consumer<O> onSuccessConsumer;
        private Optional<Consumer<Throwable>> optOnFailureInsteadLogConsumer = Optional.empty();
        private Optional<Consumer<Throwable>> optOnFailureAfterLogConsumer = Optional.empty();
        private LogLevel onFailureLogLevel = LogLevel.ERROR;

        private FutureRpcResultBuilder(Logger logger, String rpcMethodName, I input,
                Callable<ListenableFuture<O>> callable) {
            this.logger = logger;
            this.rpcMethodName = rpcMethodName;
            this.input = input;
            this.callable = callable;
            // Default methods which can be overwritten by users:
            this.onSuccessConsumer = result -> {
                logger.debug("RPC {}() successful; input = {}, output = {}", rpcMethodName, input, result);
            };
        }

        @Override
        @CheckReturnValue
        @SuppressWarnings("checkstyle:IllegalCatch")
        public Future<RpcResult<O>> build() {
            SettableFuture<RpcResult<O>> futureRpcResult = SettableFuture.create();
            try {
                logger.trace("RPC {}() entered; input = {}", rpcMethodName, input);
                ListenableFuture<O> output = callable.call();
                Futures.addCallback(output, new FutureCallback<O>() {
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
            optOnFailureInsteadLogConsumer.orElseGet(() -> new DefaultOnFailureInsteadLogConsumer()).accept(cause);
            RpcResultBuilder<O> rpcResultBuilder = RpcResultBuilder.<O>failed().withError(
                    RpcError.ErrorType.APPLICATION, rpcErrorMessageFunction.apply(cause), cause);
            // IdManager's buildFailedRpcResultFuture() had this, and it seems a nice idea in general:
            if (cause instanceof OperationFailedException) {
                rpcResultBuilder.withRpcErrors(((OperationFailedException) cause).getErrorList());
            }
            return rpcResultBuilder.build();
        }

        /**
         * Sets a custom on-failure action, for a given exception.
         * By default, the action is to LOG input and exception at the {@link #onFailureLogLevel(LogLevel)}.
         * Note that the Consumer you set here will be called <b>instead of</b>, and <b>not in addition to</b>, the
         * default one which does the log; so if you want to do custom processing and logging (or not), you have to
         * yourself explicitly do so in the Consumer argument passed here.
         */
        public FutureRpcResultBuilder<I,O> onFailureInsteadLog(Consumer<Throwable> newOnFailureInsteadLogConsumer) {
            this.optOnFailureInsteadLogConsumer.ifPresent(c -> {
                throw new IllegalStateException("onFailureInsteadLog can only be set once");
            });
            this.optOnFailureAfterLogConsumer.ifPresent(c -> {
                throw new IllegalStateException("onFailureAfterLog and onFailureInsteadLog are mutually exclusive");
            });
            this.optOnFailureInsteadLogConsumer = Optional.of(newOnFailureInsteadLogConsumer);
            return this;
        }

        public FutureRpcResultBuilder<I,O> onFailureAfterLog(Consumer<Throwable> newOnFailureAfterLogConsumer) {
            this.optOnFailureAfterLogConsumer.ifPresent(c -> {
                throw new IllegalStateException("onFailureAfterLog can only be set once");
            });
            this.optOnFailureInsteadLogConsumer.ifPresent(c -> {
                throw new IllegalStateException("onFailureAfterLog and onFailureInsteadLog are mutually exclusive");
            });
            this.optOnFailureAfterLogConsumer = Optional.of(newOnFailureAfterLogConsumer);
            return this;
        }

        /**
         * Sets a custom on-failure slf4j logging level, in case of an exception.
         * By default, it is LOG.error.
         */
        public FutureRpcResultBuilder<I,O> onFailureLogLevel(LogLevel level) {
            this.onFailureLogLevel = level;
            return this;
        }

        /**
         * Set a custom {@link RpcError} message function, for a given exception.
         * By default, the message is just {@link Throwable#getMessage()}.
         */
        public FutureRpcResultBuilder<I,O> withRpcErrorMessage(Function<Throwable, String> newRpcErrorMessageFunction) {
            this.rpcErrorMessageFunction = newRpcErrorMessageFunction;
            return this;
        }

        /**
         * Sets a custom on-success action, for a given output.
         * By default, the action is to LOG.debug both input and output.
         * Note that the Consumer you set here will be called <b>instead of</b>, and <b>not in addition to</b>, the
         * default one which does the log; so if you want to do custom processing and logging (or not), you have to
         * yourself explicitly do so in the Consumer argument passed here.
         */
        public FutureRpcResultBuilder<I,O> onSuccess(Consumer<O> newOnSuccessFunction) {
            this.onSuccessConsumer = newOnSuccessFunction;
            return this;
        }

        private class DefaultOnFailureInsteadLogConsumer implements Consumer<Throwable> {
            @Override
            public void accept(Throwable throwable) {
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
                optOnFailureAfterLogConsumer.ifPresent(consumer -> consumer.accept(throwable));
            }
        }
    }
}
