/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.idmanager;

public class IdManagerException extends Exception {

    private static final long serialVersionUID = 1000511034899423819L;

    public IdManagerException(String message) {
        super(message);
    }

    public IdManagerException(String message, Throwable cause) {
        super(message, cause);
    }

    public IdManagerException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
