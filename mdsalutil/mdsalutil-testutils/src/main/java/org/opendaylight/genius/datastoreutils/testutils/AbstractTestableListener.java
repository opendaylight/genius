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
import org.hamcrest.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event listener which offer testability in asynchronous usage scenarios.
 *
 * @author Michael Vorburger.ch
 */
public abstract class AbstractTestableListener implements AsyncEventsWaiter {

    // intentionally logging as interface type instead of implementation, clearer for developers
    private static final Logger LOG = LoggerFactory.getLogger(AbstractTestableListener.class);

    // see http://docs.oracle.com/javase/8/docs/api/java/util/concurrent/atomic/package-summary.html
    private final AtomicInteger numberOfConsumedEvents = new AtomicInteger();

    private boolean isClosed = false;

    @Override
    public boolean awaitEventsConsumption() {
        return awaitEventsConsumption(Duration.ofSeconds(30));
    }

    public boolean awaitEventsConsumption(Duration timeout) {
        return awaitEventsConsumption(timeout.toMillis(), MILLISECONDS);
    }

    protected boolean awaitEventsConsumption(long timeout, TimeUnit unit) {
        checkIfClosed();
        if (numberOfConsumedEvents.get() == 0) {
            return false;
        }
        LOG.info("awaitEventsConsumption() starting...");
        try {
            Awaitility.await("TestableListener").atMost(timeout, unit)
                .pollDelay(0, MILLISECONDS)
                .conditionEvaluationListener(condition -> LOG.info(
                    "awaitEventsConsumption: Elapsed time {}s, remaining time {}s; numberOfConsumedEvents: {}",
                        condition.getElapsedTimeInMS() / 1000, condition.getRemainingTimeInMS() / 1000,
                        condition.getValue()))
                // could be optimized to fail fast and terminate if ever negative
                .untilAtomic(numberOfConsumedEvents, Matchers.equalTo(0));
            LOG.info("... awaitEventsConsumption() completed OK");
            return true;
        // Do NOT catch (ConditionTimeoutException e) {} here
        // to LOG() & re-throw e here; else it leads to "double logging",
        // the test catching this should log (e.g. using the LogRule)
        } finally {
            numberOfConsumedEvents.set(0);
        }
    }

    private void checkIfClosed() {
        if (isClosed) {
            throw new IllegalStateException("close() called; not usable anymore - test must create new instance");
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
    protected void consumedEvents(int howMany) {
        checkIfClosed();
        LOG.info("consumedEvents({}), now at {}", howMany, numberOfConsumedEvents.addAndGet(howMany));
    }

    @Override
    public void close() throws IllegalStateException {
        isClosed = true;
        if (numberOfConsumedEvents.getAndSet(0) != 0) {
            throw new IllegalStateException("Test forgot an awaitEventsConsumption()");
        }
    }

}
