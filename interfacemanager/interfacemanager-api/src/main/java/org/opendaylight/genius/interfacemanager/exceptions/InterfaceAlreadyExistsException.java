/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.exceptions;

public class InterfaceAlreadyExistsException extends Exception {

    private static final long serialVersionUID = 1L;

    public InterfaceAlreadyExistsException() {

    }

    public InterfaceAlreadyExistsException(String message) {
        super(message);
    }

}
