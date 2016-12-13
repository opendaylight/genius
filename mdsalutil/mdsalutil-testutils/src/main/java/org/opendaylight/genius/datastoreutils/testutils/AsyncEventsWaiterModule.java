/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.testutils;

import com.google.inject.Injector;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.datastoreutils.TestDataStoreJobCoordinator;
import org.opendaylight.genius.utils.batching.ResourceBatchingManager;

/**
 * Guice wiring module which binds {@link AsyncEventsWaiter} to a combination of
 * {@link TestableDataTreeChangeListener} (like {@link TestableDataTreeChangeListenerModule}), and
 * {@link TestDataStoreJobCoordinator} and link TestableResourceBatchingManager}.
 *
 * @author Michael Vorburger.ch
 */
public class AsyncEventsWaiterModule extends TestableDataTreeChangeListenerModule {

    @Override
    protected void configure() {
        super.configure();
    }

    @Override
    protected AsyncEventsWaiter getRealAsyncEventsWaiter(Injector injector) {
        return getCompoundAsyncEventsWaiter(injector);
    }

    protected AsyncEventsWaiter getCompoundAsyncEventsWaiter(Injector injector) {
        TestableDataTreeChangeListener testableDataTreeChangeListener = getTestableDataTreeChangeListener(injector);
        DataStoreJobCoordinator.getInstance().setAsyncEventsCounter(testableDataTreeChangeListener);
        ResourceBatchingManager.getInstance().setAsyncEventsCounter(testableDataTreeChangeListener);
        return testableDataTreeChangeListener;
/*
        TestDataStoreJobCoordinator testTestDataStoreJobCoordinator =
                injector.getInstance(TestDataStoreJobCoordinator.class);

        TestableResourceBatchingManager testableResourceBatchingManager =
                injector.getInstance(TestableResourceBatchingManager.class);

        return new CompositeAsyncEventsWaiter(
                testableDataTreeChangeListener, testTestDataStoreJobCoordinator, testableResourceBatchingManager);
*/
    }

}
