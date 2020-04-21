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
import org.opendaylight.genius.datastoreutils.testutils.infra.AutoCloseableModule;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.serviceutils.tools.listener.ChainableDataTreeChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Guice wiring module which binds a {@link TestableDataTreeChangeListener} as
 * an {@link AsyncEventsWaiter}, and automatically registers all bound
 * {@link ChainableDataTreeChangeListener} to it.
 *
 * @author Michael Vorburger.ch
 */
public class TestableDataTreeChangeListenerModule extends AbstractModule {

    private static final Logger LOG = LoggerFactory.getLogger(TestableDataTreeChangeListenerModule.class);

    @Override
    protected void configure() {
        install(new AutoCloseableModule());
    }

    @Provides
    @Singleton
    protected final AsyncEventsWaiter getAsyncEventsWaiter(Injector injector) {
        return getRealAsyncEventsWaiter(injector);
    }

    protected AsyncEventsWaiter getRealAsyncEventsWaiter(Injector injector) {
        return getTestableDataTreeChangeListener(injector);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected TestableDataTreeChangeListener getTestableDataTreeChangeListener(Injector injector) {
        TestableDataTreeChangeListener beforeTestableDataTreeChangeListener = new TestableDataTreeChangeListener();
        DataTreeChangeListener afterTestableDataTreeChangeListener =
                new DecrementingTestableDataTreeChangeDecoratorListener(beforeTestableDataTreeChangeListener);
        for (Key<?> key : injector.getAllBindings().keySet()) {
            if (ChainableDataTreeChangeListener.class.isAssignableFrom(key.getTypeLiteral().getRawType())) {
                ChainableDataTreeChangeListener chainableListener
                    = (ChainableDataTreeChangeListener) injector.getInstance(key);
                chainableListener.addBeforeListener(beforeTestableDataTreeChangeListener);
                chainableListener.addAfterListener(afterTestableDataTreeChangeListener);
                LOG.info("AsyncEventsWaiter: {}", chainableListener);
            }
        }
        return beforeTestableDataTreeChangeListener;
    }

}
