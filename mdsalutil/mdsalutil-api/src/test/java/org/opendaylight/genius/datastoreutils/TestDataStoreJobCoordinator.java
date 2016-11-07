/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;

/**
 * DataStoreJobCoordinator companion class with wait method.
 *
 * @author Michael Vorburger
 */
public class TestDataStoreJobCoordinator {

    // TODO Later when DataStoreJobCoordinator is de-static-ified and DI'd then this could become a subclass..

    /**
     * Waits, by blocking calling thread, for all previously enqueued jobs to be processed.
     *
     * <p>THIS METHOD IS ONLY INTENDED FOR TESTS, AND SHOULD NOT BE CALLED IN PRODUCTION CODE.
     */
    public static void waitForAllJobs(Duration duration) {
        DataStoreJobCoordinator dataStoreJobCoordinator = DataStoreJobCoordinator.getInstance();

        dataStoreJobCoordinator.jobQueueMap.values().forEach(map -> map.values().forEach(jobQueue ->
            Awaitility.await(TestDataStoreJobCoordinator.class.getName())
            .atMost(duration.get(ChronoUnit.SECONDS), TimeUnit.SECONDS).until(() ->
                jobQueue.getWaitingEntries().isEmpty())));

        dataStoreJobCoordinator.fjPool.awaitQuiescence(duration.get(ChronoUnit.SECONDS), TimeUnit.SECONDS);
    }

    public static void waitForAllJobs() {
        waitForAllJobs(Duration.ofSeconds(5));
    }
}
