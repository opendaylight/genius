/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.testutils;

/**
 * Allows tests to wait for asynchronous event processing to be done.
 *
 * @author Michael Vorburger
 */
public interface AsyncEventsWaiter extends AutoCloseable {

    void awaitEventsConsumption(int howMany);

    /**
     * Deprecated single event await.
     * @deprecated Use {@link #awaitEventsConsumption(1)} instead!
     */
    @Deprecated
    default void awaitEventsConsumption() {
        awaitEventsConsumption(1);
    }

}
