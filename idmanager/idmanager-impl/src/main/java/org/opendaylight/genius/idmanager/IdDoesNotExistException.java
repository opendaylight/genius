/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.idmanager;

public class IdDoesNotExistException extends IdManagerException {

    private static final long serialVersionUID = 1L;

    public IdDoesNotExistException(String idPool, String idKey) {
        super("Id entry for " + idKey + " doesn't exist in the pool"
                + idPool);
    }

}
