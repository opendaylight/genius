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
import org.awaitility.core.ConditionTimeoutException;
import org.opendaylight.genius.datastoreutils.testutils.AsyncEventsWaiter;

/**
 * {@link ResourceBatchingManager} companion class with method waiting for all
 * previously enqueued jobs to be processed, for tests.
 *
 * @see TestDataStoreJobCoordinator
 *
 * @author Michael Vorburger
 */
public class TestableResourceBatchingManager implements AsyncEventsWaiter {

    protected boolean awaitEventsConsumption(Duration timeout) {
        ResourceBatchingManager resourceBatchingManager = ResourceBatchingManager.getInstance();

        // As far as I understand how the ResourceBatchingManager works,
        // it's probably fine to check EITHER the resourceHandlerMapper
        // OR the resourceBatchingThreadMapper, no need for both?

        boolean isNotEmpty = resourceBatchingManager.resourceHandlerMapper.values().stream()
                .anyMatch(pair -> !pair.getLeft().isEmpty());
        if (!isNotEmpty) {
            return false;
        }

        isNotEmpty = resourceBatchingManager.resourceBatchingThreadMapper.values().stream()
                    .anyMatch(scheduledThreadPoolExecutor -> !scheduledThreadPoolExecutor.getQueue().isEmpty());
        if (!isNotEmpty) {
            return false;
        }

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
        return true;
    }

    @Override
    public boolean awaitEventsConsumption() throws ConditionTimeoutException {
        return awaitEventsConsumption(Duration.ofSeconds(5));
    }
}
