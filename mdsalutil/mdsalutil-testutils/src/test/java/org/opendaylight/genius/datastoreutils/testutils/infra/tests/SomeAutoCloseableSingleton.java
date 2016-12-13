/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.testutils.infra.tests;

import javax.inject.Singleton;

/**
 * Example Closeable.
 *
 * @author Michael Vorburger
 */
@Singleton
public class SomeAutoCloseableSingleton implements AutoCloseable {

    boolean wasClosed = false;

    @Override
    public void close() {
        wasClosed = true;
    }

}
