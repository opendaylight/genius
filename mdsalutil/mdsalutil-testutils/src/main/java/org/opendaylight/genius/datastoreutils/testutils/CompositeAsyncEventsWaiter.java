/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.testutils;

import com.google.common.collect.Lists;
import java.util.List;
import org.awaitility.core.ConditionTimeoutException;

/**
 * AsyncEventsWaiter composed of other AsyncEventsWaiter/s.
 *
 * @author Michael Vorburger
 */
public class CompositeAsyncEventsWaiter implements AsyncEventsWaiter {

    private static final int MAX_LOOPS = 23;

    private final List<AsyncEventsWaiter> asyncEventsWaiters;

    public CompositeAsyncEventsWaiter(List<AsyncEventsWaiter> asyncEventsWaiters) {
        this.asyncEventsWaiters = asyncEventsWaiters;
    }

    public CompositeAsyncEventsWaiter(AsyncEventsWaiter... asyncEventsWaiters) {
        this(Lists.newArrayList(asyncEventsWaiters));
    }

    @Override
    public boolean awaitEventsConsumption() throws ConditionTimeoutException {
        int rerunNumber = 0;
        boolean everConsumed = false;
        boolean consumed = false;
        do {
            for (AsyncEventsWaiter asyncEventsWaiter : asyncEventsWaiters) {
                consumed = asyncEventsWaiter.awaitEventsConsumption();
                if (consumed && !everConsumed) {
                    everConsumed = true;
                }
            }
            if (++rerunNumber == MAX_LOOPS) {
                throw new ConditionTimeoutException("Inifinite retry loop");
            }
        }
        while (consumed);
        return everConsumed;
    }

}
