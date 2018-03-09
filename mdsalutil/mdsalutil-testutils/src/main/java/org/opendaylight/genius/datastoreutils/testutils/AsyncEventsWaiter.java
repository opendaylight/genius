/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.testutils;

import org.awaitility.core.ConditionTimeoutException;

/**
 * Allows tests to wait for asynchronous event processing to be done.
 *
 * @author Michael Vorburger.ch
 */
public interface AsyncEventsWaiter extends AutoCloseable {

    /**
     * Wait by blocking calling thread until pending events have been processed
     * by other threads in the background.  Implementations must have some
     * sensible fixed timeout value.  This method is normally called from
     * the JUnit Test main thread.
     *
     * @return true if anything was pending to be processed and has been
     *         processed, false if nothing needed to be
     *
     * @throws ConditionTimeoutException if timed out while waiting
     */
    boolean awaitEventsConsumption() throws ConditionTimeoutException;
}
