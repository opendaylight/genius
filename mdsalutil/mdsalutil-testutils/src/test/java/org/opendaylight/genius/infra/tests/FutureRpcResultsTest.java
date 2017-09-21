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
import org.junit.Test;
import org.opendaylight.genius.infra.FutureRpcResults;
import org.opendaylight.genius.infra.testutils.TestFutureRpcResults;
import org.opendaylight.yangtools.yang.common.RpcResult;

/**
 * Unit Test for {@link FutureRpcResults}.
 *
 * @author Michael Vorburger.ch
 */
public class FutureRpcResultsTest {

    @Test
    public void testFromListenableFutureSuccess() throws Exception {
        Future<RpcResult<String>> future = FutureRpcResults.fromListenableFuture(() -> immediateFuture("hello, world"));
        assertThat(TestFutureRpcResults.getResult(future)).isEqualTo("hello, world");
    }

    @Test
    public void testFromListenableFutureFailed() throws Exception {
        TestFutureRpcResults.assertRpcErrorCause(
                fromListenableFuture(() -> immediateFailedFuture(new IllegalArgumentException())),
                IllegalArgumentException.class);
    }

    @Test
    public void testFromListenableFutureException() throws Exception {
        TestFutureRpcResults.assertRpcErrorCause(fromListenableFuture(() -> {
            throw new IllegalArgumentException();
        }), IllegalArgumentException.class);
    }

}
