/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Trait with default implementation of TestableAsyncListener.
 *
 * @author Michael Vorburger, with AtomicBoolean usage feedback from Vratko Polák
 */
public class TestableAsyncListenerBase implements TestableAsyncListener {

    // see http://docs.oracle.com/javase/8/docs/api/java/util/concurrent/atomic/package-summary.html
    private final AtomicBoolean hasConsumedEvents = new AtomicBoolean();

    @Override
    public boolean hasConsumedEvents() {
        // Using compareAndSet() here even though weakCompareAndSet()
        // may be faster, because this is called from the "test" code,
        // so the test reliability is more important than the business speed penalty.
        return hasConsumedEvents.compareAndSet(true, false);
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
