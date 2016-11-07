/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.tests;

import static org.junit.Assert.assertTrue;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.Test;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.datastoreutils.TestDataStoreJobCoordinator;

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
    public void testWait() {
        DataStoreJobCoordinator dataStoreJobCoordinator = DataStoreJobCoordinator.getInstance();
        TestCallable testCallable = new TestCallable();
        dataStoreJobCoordinator.enqueueJob(getClass().getName().toString(), testCallable);
        new TestDataStoreJobCoordinator().waitForAllJobs();
        assertTrue(testCallable.wasCalled);
    }

}
