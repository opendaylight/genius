/*
 * Copyright (c) 2017 - 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.tools.mdsal.testutils;

import static com.google.common.truth.Truth.assertThat;
import static java.util.concurrent.TimeUnit.MINUTES;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import org.opendaylight.genius.tools.mdsal.rpc.FutureRpcResults;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;

/**
 * Assertion utilities for {@link FutureRpcResults}.
 *
 * @author Michael Vorburger.ch
 */
@SuppressFBWarnings("BC_UNCONFIRMED_CAST") // see https://wiki.opendaylight.org/view/BestPractices/Coding_Guidelines#Unchecked.2Funconfirmed_cast_from_com.google.common.truth.Subject_to_com.google.common.truth.BooleanSubject_etc.
public final class TestFutureRpcResults {

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

    public static void assertVoidRpcSuccess(Future<RpcResult<Void>> futureRpcResult)
            throws InterruptedException, ExecutionException, TimeoutException {
        RpcResult<Void> rpcResult = futureRpcResult.get(1, MINUTES);
        assertThat(rpcResult.isSuccessful()).isTrue();
        assertThat(rpcResult.getErrors()).isEmpty();
    }

    public static <O extends DataObject> void assertRpcSuccess(Future<RpcResult<O>> futureRpcResult)
            throws InterruptedException, ExecutionException, TimeoutException {
        RpcResult<O> rpcResult = futureRpcResult.get(1, MINUTES);
        assertThat(rpcResult.isSuccessful()).isTrue();
        assertThat(rpcResult.getErrors()).isEmpty();
    }

    public static <T> void assertRpcErrorWithoutCausesOrMessages(Future<RpcResult<T>> futureRpcResult)
            throws InterruptedException, ExecutionException, TimeoutException {
        RpcResult<T> rpcResult = futureRpcResult.get(1, MINUTES);
        assertThat(rpcResult.isSuccessful()).named("rpcResult.isSuccessful").isFalse();
        assertThat(rpcResult.getErrors()).named("rpcResult.errors").isEmpty();
    }

    public static <T> void assertRpcErrorCause(Future<RpcResult<T>> futureRpcResult, Class<?> expectedExceptionClass,
            String expectedRpcErrorMessage) throws InterruptedException, ExecutionException, TimeoutException {
        assertRpcErrorCause(futureRpcResult.get(1, MINUTES), expectedExceptionClass, expectedRpcErrorMessage);
    }

    private static <T> void assertRpcErrorCause(RpcResult<T> rpcResult, Class<?> expected1stExceptionClass,
            String expected1stRpcErrorMessage) {
        assertThat(rpcResult.isSuccessful()).named("rpcResult.isSuccessful").isFalse();
        Collection<RpcError> errors = rpcResult.getErrors();
        assertThat(errors).named("rpcResult.errors").hasSize(1);
        RpcError error1 = errors.iterator().next();
        assertThat(error1.getErrorType()).named("rpcResult.errors[0].errorType").isEqualTo(ErrorType.APPLICATION);
        assertThat(error1.getMessage()).named("rpcResult.errors[0].message").isEqualTo(expected1stRpcErrorMessage);
        if (error1.getCause() != null) {
            // Check needed because FutureRpcResults does not propagate cause if OperationFailedException
            assertThat(error1.getCause()).named("rpcResult.errors[0].cause").isInstanceOf(expected1stExceptionClass);
        }
    }

}
