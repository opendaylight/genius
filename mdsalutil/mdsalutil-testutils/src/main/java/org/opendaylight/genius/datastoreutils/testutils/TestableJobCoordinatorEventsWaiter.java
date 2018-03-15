/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.testutils;

import javax.inject.Inject;
import javax.inject.Singleton;
import org.awaitility.core.ConditionTimeoutException;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinatorMonitor;

@Singleton
public class TestableJobCoordinatorEventsWaiter extends AbstractTestableJobCoordinatorEventsWaiter
        implements JobCoordinatorEventsWaiter {

    @Inject
    public TestableJobCoordinatorEventsWaiter(JobCoordinatorMonitor jobCoordinatorMonitor) {
        super(jobCoordinatorMonitor);
    }

    @Override
    public boolean awaitEventsConsumption() throws ConditionTimeoutException {
        return awaitJobsConsumption(jobCoordinatorMonitor::getIncompleteTaskCount, 0);
    }

    @Override
    public void close() throws Exception {
    }
}
