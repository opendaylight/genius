/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.testutils;

import static org.hamcrest.Matchers.is;

import javax.inject.Singleton;

import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;

@Singleton
public class TestableJobCoordinatorEventsWaiter implements JobCoordinatorEventsWaiter {

    @Override
    public boolean awaitEventsConsumption() throws ConditionTimeoutException {
        Awaitility.await().until(() -> DataStoreJobCoordinator.getInstance().getIncompleteTaskCount(), is(0L));
        return true;
    }

    @Override
    public void close() throws Exception {
    }
}
