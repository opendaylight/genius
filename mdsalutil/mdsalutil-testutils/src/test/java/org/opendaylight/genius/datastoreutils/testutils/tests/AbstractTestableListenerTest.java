/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.testutils.tests;

import static org.junit.Assert.fail;

import java.time.Duration;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.After;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.opendaylight.genius.datastoreutils.testutils.AbstractTestableListener;
import org.opendaylight.infrautils.testutils.LogRule;

/**
 * Unit test for {@link AbstractTestableListener}.
 *
 * @author Michael Vorburger
 */
@FixMethodOrder(MethodSorters.JVM)
public class AbstractTestableListenerTest {

    private static final Duration TIMEOUT_500MS = Duration.ofMillis(500);

    public @Rule LogRule logRule = new LogRule();

    private final VisibleAbstractTestableListener abstractTestableListener = new VisibleAbstractTestableListener();

    @After
    public void afterCloseAsyncEventsWaiter() {
        abstractTestableListener.close();
    }

    @Test
    public void passingAwaitEventsConsumption0() {
        abstractTestableListener.awaitEventsConsumption(TIMEOUT_500MS);
    }

    @Test
    public void passingAwaitEventsConsumption0_0() {
        abstractTestableListener.awaitEventsConsumption(TIMEOUT_500MS);
        abstractTestableListener.awaitEventsConsumption(TIMEOUT_500MS);
    }

    @Test
    public void passingAwaitEventsConsumption1() {
        abstractTestableListener.consumedEvents(1);
        abstractTestableListener.consumedEvents(-1);
        abstractTestableListener.awaitEventsConsumption(TIMEOUT_500MS);
    }

    @Test
    public void passingAwaitEventsConsumption1_1() {
        abstractTestableListener.consumedEvents(1);
        abstractTestableListener.consumedEvents(-1);
        abstractTestableListener.awaitEventsConsumption(TIMEOUT_500MS);
        abstractTestableListener.consumedEvents(1);
        abstractTestableListener.consumedEvents(-1);
        abstractTestableListener.awaitEventsConsumption(TIMEOUT_500MS);
    }

    @Test
    public void passingAwaitEventsConsumption11_11() {
        abstractTestableListener.consumedEvents(1);
        abstractTestableListener.consumedEvents(1);
        abstractTestableListener.consumedEvents(-1);
        abstractTestableListener.consumedEvents(-1);
        abstractTestableListener.awaitEventsConsumption(TIMEOUT_500MS);
        abstractTestableListener.consumedEvents(1);
        abstractTestableListener.consumedEvents(-1);
        abstractTestableListener.consumedEvents(1);
        abstractTestableListener.consumedEvents(-1);
        abstractTestableListener.awaitEventsConsumption(TIMEOUT_500MS);
    }

    @Test
    public void passingAwaitEventsConsumption2() {
        abstractTestableListener.consumedEvents(2);
        abstractTestableListener.consumedEvents(-1);
        abstractTestableListener.consumedEvents(-1);
        abstractTestableListener.awaitEventsConsumption(TIMEOUT_500MS);
    }

    @Test
    public void passingAwaitEventsConsumption2_2() {
        abstractTestableListener.consumedEvents(2);
        abstractTestableListener.consumedEvents(-2);
        abstractTestableListener.awaitEventsConsumption(TIMEOUT_500MS);
    }

    @Test(expected = ConditionTimeoutException.class)
    public void failingAwaitEventsConsumption2() {
        abstractTestableListener.consumedEvents(1);
        abstractTestableListener.awaitEventsConsumption(TIMEOUT_500MS);
    }

    @Test
    public void passingAwaitEventsConsumptionClose0() {
        abstractTestableListener.close();
    }

    @Test
    public void passingAwaitEventsConsumptionClose1() {
        abstractTestableListener.consumedEvents(1);
        abstractTestableListener.consumedEvents(-1);
        abstractTestableListener.awaitEventsConsumption(TIMEOUT_500MS);
        abstractTestableListener.close();
    }

    @Test(expected = IllegalStateException.class)
    public void failingAwaitEventsConsumptionClose1() {
        abstractTestableListener.consumedEvents(1);
        abstractTestableListener.close();
    }

    @Test
    public void passingAwaitEventsConsumptionClose1andAnother() {
        abstractTestableListener.consumedEvents(1);
        try {
            abstractTestableListener.close();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // OK, expected
        }
        // Another subsequent test should use a new instance; it's not re-usable:
        try {
            abstractTestableListener.consumedEvents(1);
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // OK, expected
        }
        try {
            abstractTestableListener.awaitEventsConsumption(TIMEOUT_500MS);
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // OK, expected
        }
    }

    private class VisibleAbstractTestableListener extends AbstractTestableListener {
        @Override // make protected method visible for this test
        public void consumedEvents(int howMany) {
            super.consumedEvents(howMany);
        }
    }
}
