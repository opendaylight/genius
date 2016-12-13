/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils;

/**
 * TODO JavaDoc.
 *
 * @author Michael Vorburger
 */
public interface AsyncEventsCounter {

    // TODO JavaDoc from AbstractTestableListener
    void consumedEvents(int howMany);
}
