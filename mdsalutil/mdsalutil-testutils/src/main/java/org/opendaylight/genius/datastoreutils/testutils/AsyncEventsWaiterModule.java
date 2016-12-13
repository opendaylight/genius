/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.testutils;

import com.google.inject.Injector;
import org.opendaylight.genius.datastoreutils.TestDataStoreJobCoordinator;
import org.opendaylight.genius.datastoreutils.testutils.infra.AutoCloseableModule;
import org.opendaylight.genius.utils.batching.TestableResourceBatchingManager;

/**
 * Guice wiring module which binds {@link AsyncEventsWaiter} to a combination of
 * 1. {@link TestableDataTreeChangeListener} (like {@link TestableDataTreeChangeListenerModule}), and (then)
 * 2. {@link TestDataStoreJobCoordinator} and (then)
 * 3. {@link TestableResourceBatchingManager}.
 *
 * <p>The 1/2/3 ordering is important, because that is how code flows,
 * and thus the order in which we wait for async events to get fully processed.
 *
 * @author Michael Vorburger.ch
 */
public class AsyncEventsWaiterModule extends TestableDataTreeChangeListenerModule {

    @Override
    protected void configure() {
        super.configure();
        // TODO remove this when TestableDataTreeChangeListenerModule already has a AutoCloseableModule
        install(new AutoCloseableModule());
    }

    @Override
    protected AsyncEventsWaiter getRealAsyncEventsWaiter(Injector injector) {
        return getCompoundAsyncEventsWaiter(injector);
    }

    protected AsyncEventsWaiter getCompoundAsyncEventsWaiter(Injector injector) {
        TestableDataTreeChangeListener testableDataTreeChangeListener = getTestableDataTreeChangeListener(injector);

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
                // If meanwhile new listeners fired from the
                // DataStoreJobCoordinator or the ResourceBatchingManager
                // then this can't work...
                testableDataTreeChangeListener.awaitEventsConsumption(0);
            }

            @Override
            @SuppressWarnings("deprecation")
            public void awaitEventsConsumption() {
                testableDataTreeChangeListener.awaitEventsConsumption();
                testTestDataStoreJobCoordinator.waitForAllJobs();
                testableResourceBatchingManager.waitForAllJobs();
                testableDataTreeChangeListener.awaitEventsConsumption(0);
            }

            @Override
            public void close() throws Exception {
                testableDataTreeChangeListener.close();
            }
        };
    }

}
