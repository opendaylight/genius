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
 * DataTreeChangeListener which decorates a TestableDataTreeChangeListener
 * but decrements instead of increments its consumed events.
 *
 * @author Michael Vorburger.ch
 */
@SuppressWarnings("rawtypes")
public class DecrementingTestableDataTreeChangeDecoratorListener implements DataTreeChangeListener {

    private final TestableDataTreeChangeListener delegate;

    public DecrementingTestableDataTreeChangeDecoratorListener(TestableDataTreeChangeListener delegate) {
        this.delegate = delegate;
    }

    @Override
    public void onDataTreeChanged(Collection changes) {
        delegate.consumedEvents(- changes.size());
    }

}
