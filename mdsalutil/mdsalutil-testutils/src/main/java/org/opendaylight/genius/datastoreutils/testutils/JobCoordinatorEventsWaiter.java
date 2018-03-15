/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.testutils;

import org.awaitility.core.ConditionTimeoutException;

public interface JobCoordinatorEventsWaiter extends AsyncEventsWaiter {
    /**
     * Wait by blocking calling thread until an expected number of jobs have been processed
     * by job coordinator.  Implementations must have some
     * sensible fixed timeout value.  This method is normally called from
     * the JUnit Test main thread.
     *
     * @return true if all the expected number of jobs have been processed, false otherwise
     *
     * @throws ConditionTimeoutException if timed out while waiting
     */
    boolean awaitJobsConsumption(long clearedJobCount) throws ConditionTimeoutException;
}
