/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.lockmanager.tests;

import static com.google.common.truth.Truth.assertThat;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.infrautils.testutils.LogRule;
import org.opendaylight.lockmanager.LockManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.LockInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.LockInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.LockManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.UnlockInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.UnlockInputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;

/**
 * Test for {@link LockManager}.
 * @author Michael Vorburger.ch
 */
public class LockManagerTest extends AbstractConcurrentDataBrokerTest {

    public final @Rule LogRule logRule = new LogRule();
    private LockManagerService lockManager;

    @Before
    public void setUp() {
        lockManager = new LockManager(getDataBroker());
    }

    @Test
    public void testLockAndUnLock() throws InterruptedException, ExecutionException, TimeoutException {
        LockInput lockInput = new LockInputBuilder().setLockName("testLock").build();
        assertSuccessfulFutureRpcResult(lockManager.lock(lockInput));

        UnlockInput unlockInput = new UnlockInputBuilder().setLockName("testLock").build();
        assertSuccessfulFutureRpcResult(lockManager.unlock(unlockInput));
    }

    @Test
    public void testUnLockOfUnknownShouldNotFail() throws InterruptedException, ExecutionException, TimeoutException {
        UnlockInput unlockInput = new UnlockInputBuilder().setLockName("unknownLock").build();
        assertSuccessfulFutureRpcResult(lockManager.unlock(unlockInput));
    }

    @Test
    @Ignore // TODO make this work... currently it leads to an infinite loop
    // and never gives up, and if it would give up, it would not return an error is expected;
    // so retry seems to be completely broken; FIXME
    public void testLockAndReLockSameAgain() throws InterruptedException, ExecutionException, TimeoutException {
        LockInput lockInput = new LockInputBuilder().setLockName("testLock").build();
        assertSuccessfulFutureRpcResult(lockManager.lock(lockInput));
        assertSuccessfulFutureRpcResult(lockManager.lock(lockInput));
    }

    // TODO testTryLock()

    private void assertSuccessfulFutureRpcResult(Future<RpcResult<Void>> futureRpcResult)
            throws InterruptedException, ExecutionException, TimeoutException {
        assertThat(futureRpcResult.get(5, TimeUnit.SECONDS).isSuccessful()).isTrue();
        assertThat(futureRpcResult.get(5, TimeUnit.SECONDS).getErrors()).isEmpty();
    }
}
