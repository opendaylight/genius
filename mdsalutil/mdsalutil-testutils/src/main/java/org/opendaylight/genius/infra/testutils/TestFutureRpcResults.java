/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra.testutils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import org.opendaylight.genius.infra.FutureRpcResults;
import org.opendaylight.yangtools.yang.common.RpcResult;

/**
 * Assertion utilities for {@link FutureRpcResults}.
 * @deprecated Please use {@link org.opendaylight.genius.tools.mdsal.testutils.TestFutureRpcResults} instead of this!
 * @author Michael Vorburger.ch
 */
@SuppressFBWarnings("BC_UNCONFIRMED_CAST") // see https://wiki.opendaylight.org/view/BestPractices/Coding_Guidelines#Unchecked.2Funconfirmed_cast_from_com.google.common.truth.Subject_to_com.google.common.truth.BooleanSubject_etc.
@Deprecated
public final class TestFutureRpcResults {

    private TestFutureRpcResults() { }

    public static <T> T getResult(Future<RpcResult<T>> futureRpcResult)
            throws InterruptedException, ExecutionException, TimeoutException {
        return org.opendaylight.genius.tools.mdsal.testutils.TestFutureRpcResults.getResult(futureRpcResult);
    }

    public static void assertVoidRpcSuccess(Future<RpcResult<Void>> futureRpcResult)
            throws InterruptedException, ExecutionException, TimeoutException {
        org.opendaylight.genius.tools.mdsal.testutils.TestFutureRpcResults.assertVoidRpcSuccess(futureRpcResult);
    }

    public static <T> void assertRpcErrorWithoutCausesOrMessages(Future<RpcResult<T>> futureRpcResult)
            throws InterruptedException, ExecutionException, TimeoutException {
        org.opendaylight.genius.tools.mdsal.testutils.TestFutureRpcResults
                .assertRpcErrorWithoutCausesOrMessages(futureRpcResult);
    }

    public static <T> void assertRpcErrorCause(Future<RpcResult<T>> futureRpcResult, Class<?> expectedExceptionClass,
            String expectedRpcErrorMessage) throws InterruptedException, ExecutionException, TimeoutException {
        org.opendaylight.genius.tools.mdsal.testutils.TestFutureRpcResults.assertRpcErrorCause(
                futureRpcResult, expectedExceptionClass, expectedRpcErrorMessage);
    }
}
