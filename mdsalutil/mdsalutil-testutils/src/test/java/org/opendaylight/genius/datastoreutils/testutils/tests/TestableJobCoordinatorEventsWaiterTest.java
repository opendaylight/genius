/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.testutils.tests;

import static org.junit.Assert.assertTrue;

import com.google.common.util.concurrent.ListenableFuture;
import com.mycila.guice.ext.closeable.CloseableModule;
import com.mycila.guice.ext.jsr250.Jsr250Module;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.opendaylight.genius.datastoreutils.testutils.JobCoordinatorTestModule;
import org.opendaylight.genius.datastoreutils.testutils.TestableJobCoordinatorCountedEventsWaiter;
import org.opendaylight.genius.datastoreutils.testutils.TestableJobCoordinatorEventsWaiter;
import org.opendaylight.infrautils.inject.guice.testutils.GuiceRule;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.infrautils.testutils.LogCaptureRule;
import org.opendaylight.infrautils.testutils.LogRule;
import org.opendaylight.infrautils.testutils.RunUntilFailureClassRule;
import org.opendaylight.infrautils.testutils.RunUntilFailureRule;

/**
 * Unit test for DataStoreJobCoordinator.
 *
 * @author Michael Vorburger.ch
 */
public class TestableJobCoordinatorEventsWaiterTest {

    private static class TestCallable implements Callable<List<ListenableFuture<Void>>> {

        boolean wasCalled = false;
        AtomicInteger invocationCount = new AtomicInteger();

        @Override
        public List<ListenableFuture<Void>> call() {
            wasCalled = true;
            invocationCount.incrementAndGet();
            return null;
        }
    }

    public @Rule LogRule logRule = new LogRule();

    public @Rule LogCaptureRule logCaptureRule = new LogCaptureRule();

    public @Rule GuiceRule guice = new GuiceRule(CloseableModule.class, Jsr250Module.class,
            JobCoordinatorTestModule.class);

    public static @ClassRule RunUntilFailureClassRule classRepeater = new RunUntilFailureClassRule(13);
    public @Rule RunUntilFailureRule repeater = new RunUntilFailureRule(classRepeater);

    @Inject JobCoordinator jobCoordinator;
    @Inject TestableJobCoordinatorEventsWaiter jobCoordinatorEventsWaiter;
    @Inject
    TestableJobCoordinatorCountedEventsWaiter jobCoordinatorCountedEventsWaiter;

    @Test
    public void testInfrautilsJobCoordinatorUsingTestableJobCoordinatorEventsWaiter() {
        TestCallable testCallable = new TestCallable();
        jobCoordinator.enqueueJob(getClass().getName().toString(), testCallable);
        jobCoordinatorEventsWaiter.awaitEventsConsumption();
        assertTrue(testCallable.wasCalled);
    }

    @Test
    public void testInfrautilsJobCoordinatorUsingTestableJobCoordinatorCountedEventsWaiter() {
        // Enqueue 3 jobs parallelly, and check if awaitEventsConsumption(jobCount) works as expected.
        TestCallable testCallable = new TestCallable();
        jobCoordinator.enqueueJob("key1", testCallable);
        jobCoordinator.enqueueJob("key2", testCallable);
        jobCoordinator.enqueueJob("key3", testCallable);
        jobCoordinatorCountedEventsWaiter.awaitJobsConsumption(3);
        Assert.assertEquals(3, testCallable.invocationCount.get());
    }
}
