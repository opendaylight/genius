/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.lockmanager;

import org.opendaylight.infrautils.utils.function.CheckedCallable;

public interface LockManager {

    <E extends Exception> void runUnderLock(String lockName, CheckedRunnable<E> runnable)
            throws E, LockManagerException;

    <T, E extends Exception> T runUnderLock(String lockName, CheckedCallable<T, E> callable)
            throws E, LockManagerException;

    // TODO Replace by infrautils' when https://git.opendaylight.org/gerrit/#/c/65297/ is merged
    @FunctionalInterface
    interface CheckedRunnable<E extends Exception> {
        void run() throws E;
    }

}
