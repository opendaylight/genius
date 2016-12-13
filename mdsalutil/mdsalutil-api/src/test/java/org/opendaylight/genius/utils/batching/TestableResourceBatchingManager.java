/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.utils.batching;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.opendaylight.genius.datastoreutils.TestDataStoreJobCoordinator;

/**
 * {@link ResourceBatchingManager} companion class with wait method, for tests.
 *
 * @see TestDataStoreJobCoordinator
 *
 * @author Michael Vorburger
 */
public class TestableResourceBatchingManager {

    /**
     * Waits, by blocking calling thread, for all previously enqueued jobs to be processed.
     *
     * <p>THIS METHOD IS ONLY INTENDED FOR TESTS, AND SHOULD NOT BE CALLED IN PRODUCTION CODE.
     */
    public void waitForAllJobs(Duration timeout) {
        ResourceBatchingManager resourceBatchingManager = ResourceBatchingManager.getInstance();
        resourceBatchingManager.resourceHandlerMapper.values().forEach(pair -> {
            Awaitility.await(TestableResourceBatchingManager.class.getName())
                .atMost(timeout.toNanos(), TimeUnit.NANOSECONDS)
                .until(() -> pair.getLeft().isEmpty());
        });

        resourceBatchingManager.resourceBatchingThreadMapper.values().forEach(scheduledThreadPoolExecutor -> {
            Awaitility.await(TestableResourceBatchingManager.class.getName())
                .atMost(timeout.toNanos(), TimeUnit.NANOSECONDS)
                .until(() -> scheduledThreadPoolExecutor.getQueue().isEmpty());
        });
    }

    public void waitForAllJobs() {
        waitForAllJobs(Duration.ofSeconds(5));
    }
}
