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
import org.opendaylight.genius.utils.batching.TestableResourceBatchingManager;

/**
 * {@link DataStoreJobCoordinator} companion class with wait method, for tests.
 *
 * @see TestableResourceBatchingManager
 *
 * @author Michael Vorburger
 */
public class TestDataStoreJobCoordinator {

    /**
     * Waits, by blocking calling thread, for all previously enqueued jobs to be processed.
     *
     * <p>THIS METHOD IS ONLY INTENDED FOR TESTS, AND SHOULD NOT BE CALLED IN PRODUCTION CODE.
     */
    public void waitForAllJobs(Duration timeout) {
        DataStoreJobCoordinator dataStoreJobCoordinator = DataStoreJobCoordinator.getInstance();

        dataStoreJobCoordinator.jobQueueMap.values().forEach(map -> map.values().forEach(jobQueue ->
            Awaitility.await(TestDataStoreJobCoordinator.class.getName())
            .atMost(timeout.toNanos(), TimeUnit.NANOSECONDS).until(() ->
                jobQueue.getWaitingEntries().isEmpty())));

        dataStoreJobCoordinator.fjPool.awaitQuiescence(timeout.toNanos(), TimeUnit.NANOSECONDS);
    }

    public void waitForAllJobs() {
        waitForAllJobs(Duration.ofSeconds(5));
    }
}
