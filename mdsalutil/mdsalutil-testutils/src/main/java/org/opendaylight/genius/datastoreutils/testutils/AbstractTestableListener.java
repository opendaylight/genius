/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.testutils;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.awaitility.Awaitility;

/**
 * Event listener which offer testability in asynchronous usage scenarios.
 *
 * @author Michael Vorburger, with AtomicBoolean usage feedback from Vratko Polák
 */
public abstract class AbstractTestableListener {

    // see http://docs.oracle.com/javase/8/docs/api/java/util/concurrent/atomic/package-summary.html
    private final AtomicBoolean hasConsumedEvents = new AtomicBoolean();

    /**
     * Check if listener has consumed events.
     *
     * <p>Reading automatically resets the internal flag back to false if it was true when called.
     *
     * <p>This cannot distinguish between one or several events
     * consumed, but this is fine and intentional; because in a typical use
     * case by a test, this method is called immediately after the test did
     * something which it knows causes a listener to react.
     *
     * <p>This is not suitable in a multi-threaded test where several emit events
     * which cause listeners to be called.  This is fine and intentional; because
     * in a typical use case only the main test thread emits the event which is
     * then consumed asynchronously in another thread.  Multi-threaded component
     * testing would use separate listener instances per test.
     *
     * @return true if one or several events have been processed since last called, false if not
     */
    public boolean hasConsumedEvents() {
        // Using compareAndSet() here even though weakCompareAndSet()
        // may be faster, because this is called from the "test" code,
        // so the test reliability is more important than the business speed penalty.
        return hasConsumedEvents.compareAndSet(true, false);
    }

    /**
     * Wait by blocking calling thread until {@link #hasConsumedEvents()}, for
     * max. 500ms.
     */
    public void awaitEventsConsumption() {
        awaitEventsConsumption(500, MILLISECONDS);
    }

    /**
     * Wait by blocking calling thread until {@link #hasConsumedEvents()}, for
     * max. the time passed in the argument.
     */
    public void awaitEventsConsumption(long timeout, TimeUnit unit) {
        Awaitility.await("TestableListener").atMost(timeout, unit).until(() -> this.hasConsumedEvents());
    }

    protected void consumedEvents() {
        // Using lazySet() here instead of compareAndSet(),
        // because this is called from the "production" code,
        // so the speed of the business logic is more important than the latency of the test information.
        // Using compareAndSet(false, true), or weakCompareAndSet(), is not neccessary here,
        // as there is nothing to return.
        hasConsumedEvents.lazySet(true);
    }

}
