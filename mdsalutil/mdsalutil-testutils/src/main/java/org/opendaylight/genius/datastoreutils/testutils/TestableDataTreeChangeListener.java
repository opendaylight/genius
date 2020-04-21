/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.testutils;

import java.util.Collection;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;

/**
 * DataTreeChangeListener useful for testing in asynchronous scenarios.
 *
 * @author Michael Vorburger.ch
 */
@SuppressWarnings("rawtypes")
public class TestableDataTreeChangeListener
        extends AbstractTestableListener
        implements DataTreeChangeListener {

    @Override
    public void onDataTreeChanged(Collection changes) {
        consumedEvents(changes.size());
    }

}
