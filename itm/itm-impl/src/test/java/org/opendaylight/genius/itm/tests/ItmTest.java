/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.tests;

import javax.inject.Inject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.opendaylight.genius.datastoreutils.testutils.JobCoordinatorEventsWaiter;
import org.opendaylight.genius.datastoreutils.testutils.JobCoordinatorTestModule;
import org.opendaylight.genius.datastoreutils.testutils.TestableDataTreeChangeListenerModule;
import org.opendaylight.genius.itm.impl.ItmTestUtils;
import org.opendaylight.genius.utils.cache.CacheTestUtil;
import org.opendaylight.infrautils.caches.testutils.CacheModule;
import org.opendaylight.infrautils.inject.guice.testutils.GuiceRule;
import org.opendaylight.infrautils.testutils.LogCaptureRule;
import org.opendaylight.infrautils.testutils.LogRule;

/**
 * Component tests for ITM.
 */
public class ItmTest {

    public @Rule LogRule logRule = new LogRule();
    public @Rule LogCaptureRule logCaptureRule = new LogCaptureRule();
    public @Rule MethodRule guice = new GuiceRule(ItmTestModule.class,  TestableDataTreeChangeListenerModule.class,
            JobCoordinatorTestModule.class, CacheModule.class);

    private @Inject JobCoordinatorEventsWaiter coordinatorEventsWaiter;

    @Before
    public void before() {
        // so that any @Test methods here which do something real
        // are isolated from any other test that run before this ItmTest
        clearCaches();
    }

    @After
    public void after() {
        // so that any other *Test which will run after us has a clean slate again
        clearCaches();
    }

    private void clearCaches() {
        // Explicitly clear stupid static caches
        // (TODO which really need to be de-static-ified instead of doing this..)
        ItmTestUtils.clearAllItmCaches();
        CacheTestUtil.clearAllCaches();
    }

    @Test
    public void testWiring() {
        // wait for default-TZ creation task (which runs on start-up) to get completed
        coordinatorEventsWaiter.awaitEventsConsumption();
        // do nothing; still valuable to check that ItmTestModule works and everything comes up
    }
}
