/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.testutils.tests;

import java.time.Duration;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.After;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.opendaylight.genius.datastoreutils.testutils.AbstractTestableListener;

/**
 * Unit test for {@link AbstractTestableListener}.
 *
 * @author Michael Vorburger
 */
@FixMethodOrder(MethodSorters.JVM)
public class AbstractTestableListenerTest {

    private static final Duration TIMEOUT_50MS = Duration.ofMillis(50);

    VisibleAbstractTestableListener abstractTestableListener = new VisibleAbstractTestableListener();

    @After
    public void afterCloseAsyncEventsWaiter() throws Exception {
        abstractTestableListener.close();
    }

    @Test
    public void passingAwaitEventsConsumption0() {
        abstractTestableListener.awaitEventsConsumption(0, TIMEOUT_50MS);
    }

    @Test
    public void passingAwaitEventsConsumption0_0() {
        abstractTestableListener.awaitEventsConsumption(0, TIMEOUT_50MS);
        abstractTestableListener.awaitEventsConsumption(0, TIMEOUT_50MS);
    }

    @Test
    public void passingAwaitEventsConsumption1() {
        abstractTestableListener.consumedEvents(1);
        abstractTestableListener.awaitEventsConsumption(1, TIMEOUT_50MS);
    }

    @Test
    public void passingAwaitEventsConsumption1_1() {
        abstractTestableListener.consumedEvents(1);
        abstractTestableListener.awaitEventsConsumption(1, TIMEOUT_50MS);
        abstractTestableListener.consumedEvents(1);
        abstractTestableListener.awaitEventsConsumption(1, TIMEOUT_50MS);
    }

    @Test(expected = ConditionTimeoutException.class)
    public void failingAwaitEventsConsumption1() {
        abstractTestableListener.awaitEventsConsumption(1, TIMEOUT_50MS);
    }

    @Test
    public void passingAwaitEventsConsumption11() {
        abstractTestableListener.consumedEvents(1);
        abstractTestableListener.consumedEvents(1);
        abstractTestableListener.awaitEventsConsumption(2, TIMEOUT_50MS);
    }

    @Test
    @Ignore // TODO HELP! I don't really understand why this, just sometimes, fails.. it should be deterministic.
    public void passingAwaitEventsConsumption11_withVeryShortTimeout() {
        abstractTestableListener.consumedEvents(1);
        abstractTestableListener.consumedEvents(1);
        abstractTestableListener.awaitEventsConsumption(2, Duration.ofMillis(5));
    }

    @Test
    public void passingAwaitEventsConsumption11_11() {
        abstractTestableListener.consumedEvents(1);
        abstractTestableListener.consumedEvents(1);
        abstractTestableListener.awaitEventsConsumption(2, TIMEOUT_50MS);
        abstractTestableListener.consumedEvents(1);
        abstractTestableListener.consumedEvents(1);
        abstractTestableListener.awaitEventsConsumption(2, TIMEOUT_50MS);
    }

    @Test
    public void passingAwaitEventsConsumption2() {
        abstractTestableListener.consumedEvents(2);
        abstractTestableListener.awaitEventsConsumption(2, TIMEOUT_50MS);
    }

    @Test
    public void passingAwaitEventsConsumption2_2() {
        abstractTestableListener.consumedEvents(2);
        abstractTestableListener.awaitEventsConsumption(2, TIMEOUT_50MS);
        abstractTestableListener.consumedEvents(2);
        abstractTestableListener.awaitEventsConsumption(2, TIMEOUT_50MS);
    }

    @Test(expected = ConditionTimeoutException.class)
    public void failingAwaitEventsConsumption2() {
        abstractTestableListener.consumedEvents(1);
        abstractTestableListener.awaitEventsConsumption(2, TIMEOUT_50MS);
    }

    @Test
    public void passingAwaitEventsConsumptionClose0() throws Exception {
        abstractTestableListener.close();
    }

    @Test
    public void passingAwaitEventsConsumptionClose1() throws Exception {
        abstractTestableListener.consumedEvents(1);
        abstractTestableListener.awaitEventsConsumption(1, TIMEOUT_50MS);
        abstractTestableListener.close();
    }

    @Test(expected = IllegalStateException.class)
    public void failingAwaitEventsConsumptionClose1() throws Exception {
        abstractTestableListener.consumedEvents(1);
        abstractTestableListener.close();
    }

    @Test
    public void passingAwaitEventsConsumptionClose1andAnother() throws Exception {
        abstractTestableListener.consumedEvents(1);
        try {
            abstractTestableListener.close();
        } catch (IllegalStateException e) {
            // OK, expected
        }
        // Another subsequent test should not be impacted
        abstractTestableListener.consumedEvents(1);
        abstractTestableListener.awaitEventsConsumption(1, TIMEOUT_50MS);
    }

    private class VisibleAbstractTestableListener extends AbstractTestableListener {
        @Override // make protected method visible for this test
        protected void consumedEvents(int howMany) {
            super.consumedEvents(howMany);
        }
    }
}
