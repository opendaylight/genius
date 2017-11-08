/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.lockmanager;

public class LockManagerException extends Exception {
    private static final long serialVersionUID = 1L;

    public LockManagerException(String message, Throwable cause) {
        super(message, cause);
    }

    @Deprecated // TODO Remove this in later refactoring when LockManagerImpl doesn't go through RPC
    public LockManagerException(String message) {
        super(message);
    }

    @Deprecated // TODO Remove this in later refactoring when LockManagerImpl doesn't go through RPC
    public LockManagerException() {
        super();
    }

}
