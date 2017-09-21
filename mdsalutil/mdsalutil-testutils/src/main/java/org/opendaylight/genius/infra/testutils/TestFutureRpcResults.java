/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra.testutils;

import static com.google.common.truth.Truth.assertThat;
import static java.util.concurrent.TimeUnit.MINUTES;

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import org.opendaylight.genius.infra.FutureRpcResults;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;

/**
 * Assertion utilities for {@link FutureRpcResults}.
 *
 * @author Michael Vorburger.ch
 */
public class TestFutureRpcResults {

    private TestFutureRpcResults() { }

    private static <T> T getResult(RpcResult<T> rpcResult) {
        assertThat(rpcResult.isSuccessful()).named("rpcResult.isSuccessful").isTrue();
        T result = rpcResult.getResult();
        assertThat(result).named("result").isNotNull();
        return result;
    }

    public static <T> T getResult(Future<RpcResult<T>> futureRpcResult)
            throws InterruptedException, ExecutionException, TimeoutException {
        return getResult(futureRpcResult.get(1, MINUTES));
    }

    private static <T> void assertRpcErrorCause(RpcResult<T> rpcResult, Class<?> expectedExceptionClass) {
        assertThat(rpcResult.isSuccessful()).named("rpcResult.isSuccessful").isFalse();
        Collection<RpcError> errors = rpcResult.getErrors();
        assertThat(errors).named("rpcResult.errors").hasSize(1);
        RpcError firstError = errors.iterator().next();
        assertThat(firstError.getErrorType()).named("rpcResult.errors[0].errorType").isEqualTo(ErrorType.APPLICATION);
        assertThat(firstError.getCause()).named("rpcResult.errors[0].cause").isInstanceOf(expectedExceptionClass);
    }

    public static <T> void assertRpcErrorCause(Future<RpcResult<T>> futureRpcResult,
            Class<?> expectedExceptionClass) throws InterruptedException, ExecutionException, TimeoutException {
        assertRpcErrorCause(futureRpcResult.get(1, MINUTES), expectedExceptionClass);
    }

}
