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
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.opendaylight.genius.datastoreutils.TestDataStoreJobCoordinator;

/**
 * Guice wiring module which binds a combination of (first)
 * {@link TestDataStoreJobCoordinator} and (then) a
 * {@link TestableDataTreeChangeListener} like
 * {@link TestableDataTreeChangeListenerModule} as an {@link AsyncEventsWaiter}.
 *
 * @author Michael Vorburger.ch
 */
public class TestableJobCoordinatorDataTreeChangeListenerModule extends AbstractModule {

    @Override
    protected void configure() {
    }

    @Provides
    @Singleton
    protected AsyncEventsWaiter getCompoundAsyncEventsWaiter(Injector injector) {
        TestableDataTreeChangeListenerModule testableDataTreeChangeListenerModule = injector
                .getInstance(TestableDataTreeChangeListenerModule.class);

        TestDataStoreJobCoordinator testTestDataStoreJobCoordinator = injector
                .getInstance(TestDataStoreJobCoordinator.class);

        AsyncEventsWaiter testableDataTreeChangeListener = testableDataTreeChangeListenerModule
                .getTestableDataTreeChangeListener(injector);

        return () -> {
            testTestDataStoreJobCoordinator.waitForAllJobs();
            testableDataTreeChangeListener.awaitEventsConsumption();
        };
    }

}
