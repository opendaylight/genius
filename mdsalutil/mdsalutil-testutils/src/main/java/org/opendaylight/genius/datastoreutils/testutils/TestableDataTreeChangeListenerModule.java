/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.testutils;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.opendaylight.genius.datastoreutils.ChainableDataTreeChangeListener;

/**
 * Guice wiring module which binds a {@link TestableDataTreeChangeListener} as
 * an {@link AsyncEventsWaiter}, and automatically registers all bound
 * {@link ChainableDataTreeChangeListener} to it.
 *
 * @author Michael Vorburger
 */
public class TestableDataTreeChangeListenerModule extends AbstractModule {

    @Override
    protected void configure() {
    }

    @Provides
    @Singleton
    @SuppressWarnings({ "unchecked", "rawtypes" })
    AsyncEventsWaiter getTestableDataTreeChangeListener(Injector injector) {
        TestableDataTreeChangeListener testableDataTreeChangeListener = new TestableDataTreeChangeListener();
        for (Key<?> key : injector.getAllBindings().keySet()) {
            if (ChainableDataTreeChangeListener.class.isAssignableFrom(key.getTypeLiteral().getRawType())) {
                ChainableDataTreeChangeListener chainableListener
                    = (ChainableDataTreeChangeListener) injector.getInstance(key);
                chainableListener.addAfterListener(testableDataTreeChangeListener);
            }
        }
        return testableDataTreeChangeListener;
    }

}
