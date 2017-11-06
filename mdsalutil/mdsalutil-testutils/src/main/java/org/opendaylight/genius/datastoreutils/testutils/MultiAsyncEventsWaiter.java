/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.testutils;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.awaitility.core.ConditionTimeoutException;

/**
 * An AsyncEventsWaiter which can await multiple other AsyncEventsWaiters.
 *
 * @author Michael Vorburger.ch
 */
public class MultiAsyncEventsWaiter implements AsyncEventsWaiter {

    private final List<AsyncEventsWaiter> waiters;

    public MultiAsyncEventsWaiter(List<AsyncEventsWaiter> waiters) {
        this.waiters = ImmutableList.copyOf(waiters);
    }

    @Override
    public boolean awaitEventsConsumption() throws ConditionTimeoutException {
        boolean anyEventsConsumed = false;
        do {
            for (AsyncEventsWaiter waiter : waiters) {
                anyEventsConsumed |= waiter.awaitEventsConsumption();
            }
        } while (!anyEventsConsumed);
        // Returning true isn't strictly correct here, but that's OK,because tests
        // ignore this return value; the important thing is just that all other child
        // AsyncEventsWaiter implementations in waiters return the correct value.
        // (In the unlikely case that this will be a problem in the future because you
        // want to "stack" several "layers" of AsyncEventsWaiter, then fix up the loop
        // above to "remember" where it has indeed consumed any events or not.
        return true;
    }

    @Override
    public void close() throws Exception {
        for (AsyncEventsWaiter waiter : waiters) {
            waiter.close();
        }
    }

}
