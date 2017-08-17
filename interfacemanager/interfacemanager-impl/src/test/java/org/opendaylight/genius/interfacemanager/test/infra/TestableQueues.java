/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.test.infra;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.Queue;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;

/**
 * Utility for tests to be able await a {@link Queue} becoming empty.
 *
 * @author Michael Vorburger.ch
 */
public final class TestableQueues {

    // TODO remove when https://git.opendaylight.org/gerrit/#/c/61927/ is in infrautils

    /**
     * Await for a {@link Queue} to be empty.
     *
     * @param queue the Queue to await becoming empty
     * @param timeout timeout as a number
     * @param timeoutUnit unit of timeout
     */
    public static void awaitEmpty(Queue<?> queue, long timeout, TimeUnit timeoutUnit) {
        try {
            Awaitility.await("TestableQueues.awaitEmpty()")
                .pollDelay(0, MILLISECONDS)
                .pollInterval(100, MILLISECONDS)
                .atMost(timeout, timeoutUnit)
                // .conditionEvaluationListener(condition -> LOG.info(...))
                .until(() -> queue.peek() == null);
        } catch (ConditionTimeoutException e) {
            throw new AssertionError("Awaited Queue to become empty, but it STILL contains: " + queue.toString(), e);
        }
    }

}
