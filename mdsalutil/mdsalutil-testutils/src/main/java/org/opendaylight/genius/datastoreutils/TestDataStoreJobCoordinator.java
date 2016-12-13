/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.opendaylight.genius.datastoreutils.testutils.AsyncEventsWaiter;
import org.opendaylight.genius.utils.batching.TestableResourceBatchingManager;

/**
 * {@link DataStoreJobCoordinator} companion class with method waiting for all
 * previously enqueued jobs to be processed, for tests.
 *
 * @see TestableResourceBatchingManager
 *
 * @author Michael Vorburger
 */
public class TestDataStoreJobCoordinator implements AsyncEventsWaiter {

    protected boolean awaitEventsConsumption(Duration timeout) {
        DataStoreJobCoordinator dataStoreJobCoordinator = DataStoreJobCoordinator.getInstance();

        boolean isNotEmpty = dataStoreJobCoordinator.jobQueueMap.values().stream().anyMatch(map ->
            map.values().stream().anyMatch(jobQueue ->
                !jobQueue.getWaitingEntries().isEmpty()));
        if (!isNotEmpty) {
            return false;
        }

        dataStoreJobCoordinator.jobQueueMap.values().forEach(map -> map.values().forEach(jobQueue ->
            Awaitility.await(TestDataStoreJobCoordinator.class.getName())
            .atMost(timeout.toNanos(), TimeUnit.NANOSECONDS).until(() ->
                jobQueue.getWaitingEntries().isEmpty())));

        dataStoreJobCoordinator.fjPool.awaitQuiescence(timeout.toNanos(), TimeUnit.NANOSECONDS);

        return true;
    }

    @Override
    public boolean awaitEventsConsumption() throws ConditionTimeoutException {
        return awaitEventsConsumption(Duration.ofSeconds(5));
    }
}
