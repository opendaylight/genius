/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.testutils;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.hamcrest.Matchers;
import org.opendaylight.genius.datastoreutils.AsyncEventsCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event listener which offer testability in asynchronous usage scenarios.
 *
 * @author Michael Vorburger.ch
 */
public abstract class AbstractTestableListener implements AsyncEventsWaiter, AsyncEventsCounter {

    // TODO rename to (non-abstract class) AsyncEventsTracker, and adjust AbstractTestableListenerTest
    // not sure if need to @Deprecated this class extending it, for backward compat, or just remove?

    // intentionally logging as interface type instead of implementation, clearer for developers
    private static final Logger LOG = LoggerFactory.getLogger(AsyncEventsWaiter.class);

    // see http://docs.oracle.com/javase/8/docs/api/java/util/concurrent/atomic/package-summary.html
    private final AtomicInteger numberOfConsumedEvents = new AtomicInteger();

    @Override
    public boolean awaitEventsConsumption() {
        return awaitEventsConsumption(Duration.ofSeconds(3));
    }

    public boolean awaitEventsConsumption(Duration timeout) {
        return awaitEventsConsumption(timeout.toMillis(), MILLISECONDS);
    }

    protected boolean awaitEventsConsumption(long timeout, TimeUnit unit) {
        if (numberOfConsumedEvents.get() == 0) {
            return false;
        }
        LOG.info("awaitEventsConsumption() starting...");
        try {
            Awaitility.await("TestableListener").atMost(timeout, unit)
                .pollDelay(0, MILLISECONDS)
                // could be optimized to fail fast and terminate if ever negative
                .untilAtomic(numberOfConsumedEvents, Matchers.equalTo(0));
            LOG.info("... awaitEventsConsumption() completed OK");
            return true;
        } catch (ConditionTimeoutException e) {
            LOG.warn("... awaitEventsConsumption() completed NOK", e);
            throw e;
        } finally {
            numberOfConsumedEvents.set(0);
        }
    }

    /**
     * Signal that a testable listener has consumed an event.
     * This is normally called either from the JUnit Test main thread
     * if the event listener is synchronous, or from a background
     * thread if the event listener is asynchronous.
     *
     * @param howMany number of events consumed; may be negative, to decrement
     */
    @Override
    // TODO or protected, if not @Override
    public void consumedEvents(int howMany) {
        LOG.info("consumedEvents({}), now at {}", howMany, numberOfConsumedEvents.addAndGet(howMany));
    }

}
