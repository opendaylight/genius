/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.lockmanager.tests;

import static com.google.common.truth.Truth.assertThat;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.genius.lockmanager.LockManager;
import org.opendaylight.genius.lockmanager.LockManagerUtils;
import org.opendaylight.infrautils.inject.guice.testutils.GuiceRule;
import org.opendaylight.infrautils.testutils.LogCaptureRule;
import org.opendaylight.infrautils.testutils.LogRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.LockInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.LockInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.LockManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.TimeUnits;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.TryLockInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.TryLockInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.UnlockInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.UnlockInputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;

/**
 * Test for {@link LockManager}.
 * @author Michael Vorburger.ch
 */
public class LockManagerTest extends AbstractConcurrentDataBrokerTest {

    public @Rule LogRule logRule = new LogRule();
    public @Rule LogCaptureRule logCaptureRule = new LogCaptureRule();
    public @Rule MethodRule guice = new GuiceRule(new LockManagerTestModule());

    @Inject DataBroker dataBroker;
    @Inject LockManagerService lockManager;
    @Inject LockManagerUtils lockManagerUtils;

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
    // test re-lock of already locked key.
    // lock() RPC will infinitely retry, and it will only come out when the key is unlocked
    public void testLockAndReLockSameAgain() throws InterruptedException, ExecutionException, TimeoutException {
        LockInput lockInput = new LockInputBuilder().setLockName("testLock").build();
        assertSuccessfulFutureRpcResult(lockManager.lock(lockInput));
        runUnlockTimerTask("testLock", 3000);

        // This will retry infinitely since the other lock is not released!
        // After 5 seconds, the parallel thread will unlock the key, and the below TC will pass
        assertSuccessfulFutureRpcResult(lockManager.lock(lockInput));
    }

    @Test
    // test re-lock of already locked key using tryLock() RPC.
    // tryLock() RPC will retry only specific number of times, and it will only return after that
    public void testTryLock() throws InterruptedException, ExecutionException, TimeoutException {
        String uniqueId = lockManagerUtils.getBladeId() + ":2";
        logCaptureRule.expectError("Failed to get lock testTryLock owner " + uniqueId);

        TryLockInput lockInput = new TryLockInputBuilder().setLockName("testTryLock").setTime(3L)
            .setTimeUnit(TimeUnits.Seconds).build();
        assertSuccessfulFutureRpcResult(lockManager.tryLock(lockInput));

        // The second acquireLock request will retry for 3 seconds
        // and since the first lock is not unlocked, the request will fail.
        lockInput = new TryLockInputBuilder().setLockName("testTryLock").setTime(3000L)
            .setTimeUnit(TimeUnits.Milliseconds).build();
        assertFailedFutureRpcResult(lockManager.tryLock(lockInput));

        // Try to unlock the key in a separate thread before retry expires, and see
        // if lock gets acquired.
        runUnlockTimerTask("testTryLock", 2000);

        lockInput = new TryLockInputBuilder().setLockName("testTryLock").setTime(4000000L)
            .setTimeUnit(TimeUnits.Microseconds).build();
        assertSuccessfulFutureRpcResult(lockManager.tryLock(lockInput));
    }


    private void assertSuccessfulFutureRpcResult(Future<RpcResult<Void>> futureRpcResult)
            throws InterruptedException, ExecutionException, TimeoutException {
        assertThat(futureRpcResult.get(5, TimeUnit.SECONDS).isSuccessful()).isTrue();
        assertThat(futureRpcResult.get(5, TimeUnit.SECONDS).getErrors()).isEmpty();
    }

    private void assertFailedFutureRpcResult(Future<RpcResult<Void>> futureRpcResult)
            throws InterruptedException, ExecutionException, TimeoutException {
        assertThat(futureRpcResult.get(5, TimeUnit.SECONDS).isSuccessful()).isFalse();
    }

    private void runUnlockTimerTask(String lockKey, long delay) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                UnlockInput unlockInput = new UnlockInputBuilder().setLockName(lockKey).build();
                try {
                    assertSuccessfulFutureRpcResult(lockManager.unlock(unlockInput));
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    throw new RuntimeException(e);
                }
            }
        }, delay);
    }
}
