/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra.tests;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.Futures.immediateFailedFuture;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static org.opendaylight.genius.infra.FutureRpcResults.fromListenableFuture;

import java.util.concurrent.Future;
import org.junit.Rule;
import org.junit.Test;
import org.opendaylight.genius.infra.FutureRpcResults;
import org.opendaylight.genius.infra.testutils.TestFutureRpcResults;
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

    @Test
    public void testListenableFutureSuccess() throws Exception {
        Future<RpcResult<String>> future = FutureRpcResults.fromListenableFuture(
                LOG, "testListenableFutureSuccess", null, () -> immediateFuture("hello, world")).build();
        assertThat(TestFutureRpcResults.getResult(future)).isEqualTo("hello, world");
    }

    @Test
    public void testFailedListenableFuture() throws Exception {
        TestFutureRpcResults.assertRpcErrorCause(fromListenableFuture(LOG, "testFailedListenableFuture", null, () ->
                immediateFailedFuture(new IllegalArgumentException("boum"))).build(),
                    IllegalArgumentException.class, "boum");
    }

    @Test
    public void testFromListenableFutureException() throws Exception {
        TestFutureRpcResults.assertRpcErrorCause(fromListenableFuture(
            LOG, "testFromListenableFutureException", null, () -> {
                throw new IllegalArgumentException("bam");
            }).build(), IllegalArgumentException.class, "bam");
    }

    @Test
    public void testFromListenableFutureExceptionCustomMessage() throws Exception {
        TestFutureRpcResults.assertRpcErrorCause(fromListenableFuture(LOG, "assertRpcErrorCause", null, () -> {
            throw new IllegalArgumentException("bam");
        }).withRpcErrorMessage(e -> "tra la la").build(), IllegalArgumentException.class, "tra la la");
    }

}
