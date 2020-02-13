/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.lockmanager.tests;

import static org.opendaylight.serviceutils.tools.mdsal.testutils.TestFutureRpcResults.assertRpcErrorCause;
import static org.opendaylight.serviceutils.tools.mdsal.testutils.TestFutureRpcResults.assertRpcErrorWithoutCausesOrMessages;
import static org.opendaylight.serviceutils.tools.mdsal.testutils.TestFutureRpcResults.assertRpcSuccess;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import javax.inject.Inject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.genius.datastoreutils.testutils.DataBrokerFailures;
import org.opendaylight.genius.datastoreutils.testutils.DataBrokerFailuresModule;
import org.opendaylight.genius.lockmanager.impl.LockManagerServiceImpl;
import org.opendaylight.genius.lockmanager.impl.LockManagerUtils;
import org.opendaylight.infrautils.inject.guice.testutils.GuiceRule;
import org.opendaylight.infrautils.testutils.LogCaptureRule;
import org.opendaylight.infrautils.testutils.LogRule;
import org.opendaylight.mdsal.common.api.OptimisticLockFailedException;
import org.opendaylight.mdsal.common.api.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.LockInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.LockInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.LockManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.TimeUnits;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.TryLockInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.TryLockInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.UnlockInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.UnlockInputBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test for {@link LockManagerServiceImpl}.
 *
 * @author Michael Vorburger.ch
 */
public class LockManagerTest extends AbstractConcurrentDataBrokerTest {

    private static final Logger LOG = LoggerFactory.getLogger(LockManagerTest.class);

    public @Rule LogRule logRule = new LogRule();
    public @Rule LogCaptureRule logCaptureRule = new LogCaptureRule();
    public @Rule MethodRule guice = new GuiceRule(LockManagerTestModule.class, DataBrokerFailuresModule.class);

    @Inject DataBrokerFailures dbFailureSimulator;
    @Inject LockManagerService lockManager;
    @Inject LockManagerUtils lockManagerUtils;

    @Test
    public void testLockAndUnLock() throws InterruptedException, ExecutionException, TimeoutException {
        LockInput lockInput = new LockInputBuilder().setLockName("testLock").build();
        assertRpcSuccess(lockManager.lock(lockInput));

        UnlockInput unlockInput = new UnlockInputBuilder().setLockName("testLock").build();
        assertRpcSuccess(lockManager.unlock(unlockInput));
    }

    @Test
    public void testUnLockOfUnknownShouldNotFail() throws InterruptedException, ExecutionException, TimeoutException {
        UnlockInput unlockInput = new UnlockInputBuilder().setLockName("unknownLock").build();
        assertRpcSuccess(lockManager.unlock(unlockInput));
    }

    @Test
    // test re-lock of already locked key.
    // lock() RPC will infinitely retry, and it will only come out when the key is unlocked
    public void testLockAndReLockSameAgain() throws InterruptedException, ExecutionException, TimeoutException {
        LockInput lockInput = new LockInputBuilder().setLockName("testLock").build();
        assertRpcSuccess(lockManager.lock(lockInput));
        runUnlockTimerTask("testLock", 3000);

        // This will retry infinitely since the other lock is not released!
        // After 5 seconds, the parallel thread will unlock the key, and the below TC will pass
        assertRpcSuccess(lockManager.lock(lockInput));
    }

    @Test
    // test re-lock of already locked key using tryLock() RPC.
    // tryLock() RPC will retry only specific number of times, and it will only return after that
    public void testTryLock() throws InterruptedException, ExecutionException, TimeoutException {
        String uniqueId = lockManagerUtils.getBladeId() + ":2";
        logCaptureRule.expectError("Failed to get lock testTryLock owner " + uniqueId + " after 3 retries");

        TryLockInput lockInput = new TryLockInputBuilder().setLockName("testTryLock").setTime(3L)
                .setTimeUnit(TimeUnits.Seconds).build();
        assertRpcSuccess(lockManager.tryLock(lockInput));

        // The second acquireLock request will retry for 3 seconds
        // and since the first lock is not unlocked, the request will fail.
        lockInput = new TryLockInputBuilder().setLockName("testTryLock").setTime(3000L)
                .setTimeUnit(TimeUnits.Milliseconds).build();
        assertRpcErrorWithoutCausesOrMessages(lockManager.tryLock(lockInput));

        // Try to unlock the key in a separate thread before retry expires, and see
        // if lock gets acquired.
        runUnlockTimerTask("testTryLock", 2000);

        lockInput = new TryLockInputBuilder().setLockName("testTryLock").setTime(4000000L)
                .setTimeUnit(TimeUnits.Microseconds).build();
        assertRpcSuccess(lockManager.tryLock(lockInput));
    }

    @Test
    public void test3sOptimisticLockFailedExceptionOnLock()
            throws InterruptedException, ExecutionException, TimeoutException {
        dbFailureSimulator.failSubmits(new OptimisticLockFailedException("bada boum bam!"));
        LockInput lockInput = new LockInputBuilder().setLockName("testLock").build();
        runUnfailSubmitsTimerTask(3000); // see other tests above
        assertRpcSuccess(lockManager.lock(lockInput));
    }

    @Test
    public void testAskTimeOutException() throws InterruptedException, ExecutionException, TimeoutException {
        String lockName = "testLock";
        logCaptureRule.expectError("Unable to acquire lock for " + lockName + ", try 1", 1);
        dbFailureSimulator.failButSubmitsAnyways();
        LockInput lockInput = new LockInputBuilder().setLockName(lockName).build();
        assertRpcErrorCause(lockManager.lock(lockInput), TransactionCommitFailedException.class,
                "caused by simulated AskTimeoutException");
    }

    @Test
    public void testEternalTransactionCommitFailedExceptionOnLock()
            throws InterruptedException, ExecutionException, TimeoutException {
        logCaptureRule.expectError("RPC lock() failed; input = LockInput{_lockName=testLock, augmentation=[]}");
        dbFailureSimulator.failSubmits(new TransactionCommitFailedException("bada boum bam!"));
        LockInput lockInput = new LockInputBuilder().setLockName("testLock").build();
        assertRpcErrorCause(lockManager.lock(lockInput), TransactionCommitFailedException.class, "bada boum bam!");
    }

    // TODO testEternalReadFailedExceptionOnLock() throws InterruptedException, ExecutionException, TimeoutException {

    // TODO test3sOptimisticLockFailedExceptionOnUnLock()
    // TODO testEternalReadFailedExceptionOnUnLock()
    // TODO testEternalTransactionCommitFailedExceptionOnUnLock()

    // TODO test3sOptimisticLockFailedExceptionOnTryLock()
    // TODO testEternalReadFailedExceptionOnTryLock()
    // TODO testEternalTransactionCommitFailedExceptionOnTryLock()

    private void runUnlockTimerTask(String lockKey, long delay) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                UnlockInput unlockInput = new UnlockInputBuilder().setLockName(lockKey).build();
                try {
                    assertRpcSuccess(lockManager.unlock(unlockInput));
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    LOG.error("runUnlockTimerTask() failed", e);
                    // throw new RuntimeException(e) is useless here, as this in a BG Thread, and it would go nowhere
                }
            }
        }, delay);
    }

    private void runUnfailSubmitsTimerTask(long delay) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                dbFailureSimulator.unfailSubmits();
            }
        }, delay);
    }

}
