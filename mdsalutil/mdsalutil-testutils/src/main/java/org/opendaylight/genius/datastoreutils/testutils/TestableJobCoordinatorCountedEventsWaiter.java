/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.testutils;

import java.util.concurrent.atomic.AtomicLong;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.awaitility.core.ConditionTimeoutException;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinatorMonitor;

@Singleton
public class TestableJobCoordinatorCountedEventsWaiter extends AbstractTestableJobCoordinatorEventsWaiter
        implements JobCoordinatorCountedEventsWaiter {

    //
    // This utility helps in unit tests where the same test have a sequence of jobs being enqueued
    // in a step by step manner, and there are intermediate verification steps.
    // clearedJobCountTillLastCall will store the number of jobs enqueued till the last call for
    // awaitJobsConsumption() was executed. This is required to mainta
    //
    private final AtomicLong clearedJobCountTillLastCall = new AtomicLong(0L);

    @Inject
    public TestableJobCoordinatorCountedEventsWaiter(JobCoordinatorMonitor jobCoordinatorMonitor) {
        super(jobCoordinatorMonitor);
    }

    @Override
    public boolean awaitJobsConsumption(long newAdditionalClearedJobCountSinceLastCall)
            throws ConditionTimeoutException {
        return awaitJobsConsumption(jobCoordinatorMonitor::getClearedTaskCount,
                buildCumulativeJobCount(newAdditionalClearedJobCountSinceLastCall));
    }

    private long buildCumulativeJobCount(long newAdditionalClearedJobCountSinceLastCall) {
        return clearedJobCountTillLastCall.addAndGet(newAdditionalClearedJobCountSinceLastCall);
    }
}
