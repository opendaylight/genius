/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.tools.mdsal.rpc;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.Futures.immediateFailedFuture;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static org.opendaylight.genius.tools.mdsal.rpc.FutureRpcResults.LogLevel.NONE;

import com.google.common.truth.Truth;
import com.google.common.util.concurrent.Futures;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Rule;
import org.junit.Test;
import org.opendaylight.genius.tools.mdsal.rpc.FutureRpcResults.LogLevel;
import org.opendaylight.genius.tools.mdsal.testutils.TestFutureRpcResults;
import org.opendaylight.infrautils.testutils.LogCaptureRule;
import org.opendaylight.infrautils.testutils.LogRule;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit Test for {@link FutureRpcResults}.
 *
 * @author Michael Vorburger.ch
 */
public class FutureRpcResultsTest {

    private static final Logger LOG = LoggerFactory.getLogger(FutureRpcResultsTest.class);

    public @Rule LogRule logRule = new LogRule();
    public @Rule LogCaptureRule logCaptureRule = new LogCaptureRule();

    @Test
    public void testListenableFutureSuccess() throws Exception {
        Future<RpcResult<String>> future = FutureRpcResults.fromListenableFuture(
                LOG, null, () -> immediateFuture("hello, world")).build();
        Truth.assertThat(TestFutureRpcResults.getResult(future)).isEqualTo("hello, world");
    }

    @Test
    public void testFailedListenableFuture() throws Exception {
        logCaptureRule.expectError("RPC testFailedListenableFuture() failed; input = null");
        TestFutureRpcResults.assertRpcErrorCause(FutureRpcResults.fromListenableFuture(LOG, null, () ->
                immediateFailedFuture(new IllegalArgumentException("boum"))).build(),
                    IllegalArgumentException.class, "boum");
    }

    @Test
    public void testFromListenableFutureException() throws Exception {
        logCaptureRule.expectError("RPC testFromListenableFutureException() failed; input = null");
        TestFutureRpcResults.assertRpcErrorCause(FutureRpcResults.fromListenableFuture(
            LOG, null, () -> {
                throw new IllegalArgumentException("bam");
            }).build(), IllegalArgumentException.class, "bam");
    }

    @Test
    public void testFromListenableFutureExceptionWarnInsteadError() throws Exception {
        TestFutureRpcResults.assertRpcErrorCause(FutureRpcResults.fromListenableFuture(
            LOG, "testFromListenableFutureException", null, () -> {
                throw new IllegalArgumentException("bam");
            }).onFailureLogLevel(LogLevel.WARN).build(), IllegalArgumentException.class, "bam");
    }

    @Test
    public void testFromListenableFutureExceptionNoLog() throws Exception {
        TestFutureRpcResults.assertRpcErrorCause(FutureRpcResults.fromListenableFuture(
            LOG, "testFromListenableFutureException", null, () -> {
                throw new IllegalArgumentException("bam");
            }).onFailureLogLevel(NONE).build(), IllegalArgumentException.class, "bam");
    }

    @Test
    public void testFromListenableFutureExceptionAlsoLog() throws Exception {
        final AtomicBoolean afterLogActionCalled = new AtomicBoolean(false);
        logCaptureRule.expectError("RPC testFromListenableFutureException() failed; input = null");
        TestFutureRpcResults.assertRpcErrorCause(FutureRpcResults.fromListenableFuture(
            LOG, "testFromListenableFutureException", null, () -> {
                throw new IllegalArgumentException("bam");
            }).onFailure(e -> afterLogActionCalled.set(true)).build(), IllegalArgumentException.class, "bam");
        assertThat(afterLogActionCalled.get()).isTrue();
    }

    @Test
    public void testFromListenableFutureExceptionCustomMessage() throws Exception {
        logCaptureRule.expectError("RPC testFromListenableFutureExceptionCustomMessage() failed; input = null");
        TestFutureRpcResults.assertRpcErrorCause(FutureRpcResults.fromListenableFuture(LOG, null, () -> {
            throw new IllegalArgumentException("bam");
        }).withRpcErrorMessage(e -> "tra la la").build(), IllegalArgumentException.class, "tra la la");
    }

    @Test(expected = IllegalStateException.class)
    public void testExtraOnFailureThrowsException() throws Exception {
        FutureRpcResults.fromListenableFuture(LOG, null, () -> Futures.immediateFuture(null)).onFailure(failure -> {
        }).onFailure(failure -> {
        });
    }

    @Test(expected = IllegalStateException.class)
    public void testExtraOnSuccessThrowsException() throws Exception {
        FutureRpcResults.fromListenableFuture(LOG, null, () -> Futures.immediateFuture(null)).onSuccess(result -> {
        }).onSuccess(result -> {
        });
    }

    @Test(expected = IllegalStateException.class)
    public void testExtraWithRpcErrorMessageThrowsException() throws Exception {
        FutureRpcResults.fromListenableFuture(LOG, null, () -> Futures.immediateFuture(null)).withRpcErrorMessage(
            error -> null).withRpcErrorMessage(error -> null);
    }

}
