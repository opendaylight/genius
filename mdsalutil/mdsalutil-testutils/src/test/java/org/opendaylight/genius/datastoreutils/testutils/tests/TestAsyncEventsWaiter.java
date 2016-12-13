/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.testutils.tests;

import org.awaitility.core.ConditionTimeoutException;
import org.opendaylight.genius.datastoreutils.testutils.AsyncEventsWaiter;

/**
 * AsyncEventsWaiter for unit tests.
 *
 * @author Michael Vorburger.ch
 */
class TestAsyncEventsWaiter implements AsyncEventsWaiter {

    int rounds;
    boolean hasConsumed;
    int keepConsumedUpToRound = 0;

    @Override
    public boolean awaitEventsConsumption() throws ConditionTimeoutException {
        boolean previousHasConsumed = hasConsumed;
        if (++rounds == keepConsumedUpToRound + 1) {
            hasConsumed = false;
        }
        return previousHasConsumed;
    }
}
