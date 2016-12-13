/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.testutils.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.Test;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.datastoreutils.TestDataStoreJobCoordinator;
import org.opendaylight.genius.datastoreutils.testutils.AbstractTestableListener;

/**
 * Unit test for DataStoreJobCoordinator.
 *
 * @author Michael Vorburger
 */
public class DataStoreJobCoordinatorTest {

    private static class TestCallable implements Callable<List<ListenableFuture<Void>>> {

        boolean wasCalled = false;

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            wasCalled = true;
            return null;
        }
    }

    @Test
    public void testAwaitOfJobUsingTestDataStoreJobCoordinator() {
        DataStoreJobCoordinator dataStoreJobCoordinator = DataStoreJobCoordinator.getInstance();
        TestCallable testCallable = new TestCallable();
        dataStoreJobCoordinator.enqueueJob(getClass().getName().toString(), testCallable);
        TestDataStoreJobCoordinator testDataStoreJobCoordinator = new TestDataStoreJobCoordinator();
        // This assert of the AsyncEventsWaiter is not reliable enough,
        // because if DataStoreJobCoordinator processes the job fast enough,
        // then we're already too late here...
        // assertTrue(testDataStoreJobCoordinator.awaitEventsConsumption());
        testDataStoreJobCoordinator.awaitEventsConsumption();
        assertTrue(testCallable.wasCalled);
        testDataStoreJobCoordinator.close();
    }

    @Test
    public void testEmptyUsingTestDataStoreJobCoordinator() {
        TestDataStoreJobCoordinator testDataStoreJobCoordinator = new TestDataStoreJobCoordinator();
        assertFalse(testDataStoreJobCoordinator.awaitEventsConsumption());
        testDataStoreJobCoordinator.close();
    }

    @Test
    public void testUsingAsyncEventsCounter() {
        // TODO use AsyncEventsTracker instead AbstractTestableListener
        AbstractTestableListener asyncEventsTracker = new AbstractTestableListener() {
        };
        DataStoreJobCoordinator dataStoreJobCoordinator = DataStoreJobCoordinator.getInstance();
        dataStoreJobCoordinator.setAsyncEventsCounter(asyncEventsTracker);
        TestCallable testCallable = new TestCallable();
        dataStoreJobCoordinator.enqueueJob(getClass().getName().toString(), testCallable);
        asyncEventsTracker.awaitEventsConsumption();
        assertTrue(testCallable.wasCalled);
    }
}
