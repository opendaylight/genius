/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.testutils.infra;

/**
 * RuntimeException thrown on catch of {@link Exception} from an {@link AutoCloseable#close()}.
 *
 * @author Michael Vorburger
 */
public class AutoCloseableRuntimeException extends RuntimeException {

    private static final long serialVersionUID = -6128472398177458322L;

    public AutoCloseableRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

}
