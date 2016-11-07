/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils;

/**
 * Event/s listener which offer testability in asynchronous usage scenarios.
 *
 * @author Michael Vorburger
 */
public interface TestableAsyncListener {

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
    boolean hasConsumedEvents();

}
