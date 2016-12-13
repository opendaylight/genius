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
import org.opendaylight.genius.datastoreutils.testutils.infra.AutoCloseableModule;
import org.opendaylight.genius.utils.batching.TestableResourceBatchingManager;

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
        install(new AutoCloseableModule());
    }

    @Provides
    @Singleton
    protected AsyncEventsWaiter getCompoundAsyncEventsWaiter(Injector injector) {
        AsyncEventsWaiter testableDataTreeChangeListener =
                injector.getInstance(TestableDataTreeChangeListenerModule.class)
                .getTestableDataTreeChangeListener(injector);

        TestDataStoreJobCoordinator testTestDataStoreJobCoordinator =
                injector.getInstance(TestDataStoreJobCoordinator.class);

        TestableResourceBatchingManager testableResourceBatchingManager =
                injector.getInstance(TestableResourceBatchingManager.class);

        return new AsyncEventsWaiter() {
            @Override
            public void awaitEventsConsumption(int howMany) {
                testableDataTreeChangeListener.awaitEventsConsumption(howMany);
                testTestDataStoreJobCoordinator.waitForAllJobs();
                testableResourceBatchingManager.waitForAllJobs();
            }

            @Override
            public void close() throws Exception {
                testableDataTreeChangeListener.close();
            }
        };
    }

}
