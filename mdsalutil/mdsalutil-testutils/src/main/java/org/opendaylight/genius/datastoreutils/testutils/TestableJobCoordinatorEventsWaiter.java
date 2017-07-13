/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.testutils;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.is;

import javax.inject.Inject;
import javax.inject.Singleton;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinatorMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class TestableJobCoordinatorEventsWaiter implements JobCoordinatorEventsWaiter {

    private static final Logger LOG = LoggerFactory.getLogger(TestableJobCoordinatorEventsWaiter.class);

    private final JobCoordinatorMonitor infrautilsJobCoordinatorMonitor;

    @Inject
    public TestableJobCoordinatorEventsWaiter(JobCoordinatorMonitor infrautilsJobCoordinatorMonitor) {
        super();
        this.infrautilsJobCoordinatorMonitor = infrautilsJobCoordinatorMonitor;
    }

    @Override
    public boolean awaitEventsConsumption() throws ConditionTimeoutException {
        Awaitility.await("TestableJobCoordinatorEventsWaiter")
            .atMost(30, SECONDS)
            .pollDelay(0, MILLISECONDS)
            .conditionEvaluationListener(condition -> LOG.info(
                    "awaitEventsConsumption: Elapsed time {}s, remaining time {}s; incompleteTaskCount: {}",
                        condition.getElapsedTimeInMS() / 1000, condition.getRemainingTimeInMS() / 1000,
                        condition.getValue()))
            .until(() -> infrautilsJobCoordinatorMonitor.getIncompleteTaskCount(), is(0L));
        return true;
    }

    @Override
    public void close() throws Exception {
    }
}
