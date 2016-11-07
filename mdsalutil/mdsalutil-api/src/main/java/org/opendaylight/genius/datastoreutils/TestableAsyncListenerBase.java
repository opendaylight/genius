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
 * @author Michael Vorburger
 */
public class TestableAsyncListenerBase implements TestableAsyncListener {

    private final AtomicBoolean hasConsumedEvents = new AtomicBoolean();

    @Override
    public boolean hasConsumedEvents() {
        // TODO weakCompareAndSet ?
        return hasConsumedEvents.compareAndSet(true, false);
    }

    protected void consumedEvents() {
        // TODO weakCompareAndSet ?  is just set faster and better, or slower?
        hasConsumedEvents.compareAndSet(false, true);
    }

}
