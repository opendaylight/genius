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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event listener which offer testability in asynchronous usage scenarios.
 *
 * @author Michael Vorburger
 */
public abstract class AbstractTestableListener implements AsyncEventsWaiter {

    // intentionally logging as interface type instead of implementation, clearer for developers
    private static final Logger LOG = LoggerFactory.getLogger(AsyncEventsWaiter.class);

    // see http://docs.oracle.com/javase/8/docs/api/java/util/concurrent/atomic/package-summary.html
    private final AtomicInteger numberOfConsumedEvents = new AtomicInteger();

    /**
     * Wait by blocking calling thread until (at least) <code>howMany</code> events have been consumed, for
     * max. 500ms (time out).  This is normally called from the JUnit Test main thread.
     */
    @Override
    public void awaitEventsConsumption(int howMany) {
        awaitEventsConsumption(howMany, Duration.ofSeconds(3));
    }

    public void awaitEventsConsumption(int howMany, Duration timeout) {
        awaitEventsConsumption(howMany, timeout.toMillis(), MILLISECONDS);
    }

    protected void awaitEventsConsumption(int howMany, long timeout, TimeUnit unit) {
        LOG.info("awaitEventsConsumption({}) starting...", howMany);
        try {
            Awaitility.await("TestableListener").atMost(timeout, unit)
                .pollDelay(0, MILLISECONDS)
                // TODO could be optimized to terminate if howMany OR MORE, but fail if more
                .untilAtomic(numberOfConsumedEvents, Matchers.equalTo(howMany));
            LOG.info("... awaitEventsConsumption({}) completed OK", howMany);
        } catch (ConditionTimeoutException e) {
            LOG.warn("... awaitEventsConsumption({}) completed NOK", howMany, e);
            throw e;
        } finally {
            numberOfConsumedEvents.set(0);
        }
    }

    @Override
    @Deprecated
    public void awaitEventsConsumption() {
        try {
            Awaitility.await("TestableListener").atMost(500, MILLISECONDS)
                .pollDelay(0, MILLISECONDS)
                .untilAtomic(numberOfConsumedEvents, Matchers.greaterThan(0));
        } catch (ConditionTimeoutException e) {
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
     */
    protected void consumedEvents(int howMany) {
        LOG.info("consumedEvents({})", numberOfConsumedEvents.addAndGet(howMany));
    }

    @Override
    public void close() throws Exception {
        try {
            final int localNumberOfConsumedEvents = numberOfConsumedEvents.get();
            if (localNumberOfConsumedEvents != 0) {
                throw new IllegalStateException(
                        "Test forgot an awaitEventsConsumption(" + localNumberOfConsumedEvents + ")");
            }
        } finally {
            numberOfConsumedEvents.set(0);
            LOG.info("close(0)");
        }
    }

}
