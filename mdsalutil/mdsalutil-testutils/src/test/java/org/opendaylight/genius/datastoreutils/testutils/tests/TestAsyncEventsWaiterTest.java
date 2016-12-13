/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.testutils.tests;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

/**
 * Unit test for {@link TestAsyncEventsWaiter}.
 *
 * @author Michael Vorburger.ch
 */
public class TestAsyncEventsWaiterTest {

    TestAsyncEventsWaiter waiter = new TestAsyncEventsWaiter();

    @Test public void testInitial() {
        assertThat(waiter.awaitEventsConsumption()).isEqualTo(false);
        assertThat(waiter.rounds).isEqualTo(1);
    }

    @Test public void testHasConsume() {
        waiter.hasConsumed = true;
        assertThat(waiter.awaitEventsConsumption()).isEqualTo(true);
        assertThat(waiter.awaitEventsConsumption()).isEqualTo(false);
        assertThat(waiter.rounds).isEqualTo(2);
    }

    @Test public void testKeepOnRound() {
        waiter.hasConsumed = true;
        waiter.keepConsumedUpToRound = 1;
        assertThat(waiter.awaitEventsConsumption()).isEqualTo(true);
        assertThat(waiter.awaitEventsConsumption()).isEqualTo(true);
        assertThat(waiter.awaitEventsConsumption()).isEqualTo(false);
        assertThat(waiter.rounds).isEqualTo(3);
    }

    @Test public void testKeepUntilRound2() {
        waiter.hasConsumed = true;
        waiter.keepConsumedUpToRound = 2;
        assertThat(waiter.awaitEventsConsumption()).isEqualTo(true);
        assertThat(waiter.awaitEventsConsumption()).isEqualTo(true);
        assertThat(waiter.awaitEventsConsumption()).isEqualTo(true);
        assertThat(waiter.awaitEventsConsumption()).isEqualTo(false);
        assertThat(waiter.rounds).isEqualTo(4);
    }

}
