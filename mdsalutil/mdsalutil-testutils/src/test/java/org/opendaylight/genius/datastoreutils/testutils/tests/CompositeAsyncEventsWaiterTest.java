/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.testutils.tests;

import static com.google.common.truth.Truth.assertThat;

import org.awaitility.core.ConditionTimeoutException;
import org.junit.After;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.opendaylight.genius.datastoreutils.testutils.AsyncEventsWaiter;
import org.opendaylight.genius.datastoreutils.testutils.CompositeAsyncEventsWaiter;

/**
 * Unit Test for {@link CompositeAsyncEventsWaiter}.
 *
 * @author Michael Vorburger.ch
 */
@FixMethodOrder(MethodSorters.JVM)
public class CompositeAsyncEventsWaiterTest {

    TestAsyncEventsWaiter first = new TestAsyncEventsWaiter();
    TestAsyncEventsWaiter second = new TestAsyncEventsWaiter();

    CompositeAsyncEventsWaiter composite0 = new CompositeAsyncEventsWaiter();
    CompositeAsyncEventsWaiter composite1 = new CompositeAsyncEventsWaiter(first);
    CompositeAsyncEventsWaiter composite2 = new CompositeAsyncEventsWaiter(first, second);

    @After public void closeAll() {
        composite0.close();
        composite1.close();
        composite2.close();
    }

    @Ignore // This doesn't work like intended; exception from @After can't be expected in @Test
    @Test(expected = IllegalStateException.class) public void testAfterCloseAll() {
        first.hasConsumed = true;
    }

    @Test public void testEmpty() {
        assertThat(composite0.awaitEventsConsumption()).isFalse();
    }

    @Test public void testSingleFalse() {
        assertThat(composite1.awaitEventsConsumption()).isFalse();
        assertThat(first.rounds).isEqualTo(1);
    }

    @Test public void testSingleTrue() {
        first.hasConsumed = true;
        assertThat(composite1.awaitEventsConsumption()).isTrue();
        assertThat(first.rounds).isEqualTo(2);
    }

    @Test public void testTwoFalseFalse() {
        assertThat(composite2.awaitEventsConsumption()).isFalse();
        assertThat(first.rounds).isEqualTo(1);
        assertThat(second.rounds).isEqualTo(1);
    }

    @Test public void testTwoTrueFalse() {
        first.hasConsumed = true;
        assertThat(composite2.awaitEventsConsumption()).isTrue();
        assertThat(first.rounds).isEqualTo(1);
        assertThat(second.rounds).isEqualTo(1);
    }

    @Test public void testTwoFalseTrue() {
        second.hasConsumed = true;
        assertThat(composite2.awaitEventsConsumption()).isTrue();
        assertThat(first.rounds).isEqualTo(2);
        assertThat(second.rounds).isEqualTo(2);
    }

    @Test public void testTwoFalseTrueKeepFirst1() {
        second.hasConsumed = true;
        second.keepConsumedUpToRound = 1;
        assertThat(composite2.awaitEventsConsumption()).isTrue();
        assertThat(first.rounds).isEqualTo(3);
        assertThat(second.rounds).isEqualTo(3);
    }

    @Test public void testTwoFalseTrueKeepSecond1() {
        second.hasConsumed = true;
        second.keepConsumedUpToRound = 1;
        assertThat(composite2.awaitEventsConsumption()).isTrue();
        assertThat(first.rounds).isEqualTo(3);
        assertThat(second.rounds).isEqualTo(3);
    }

    @Test public void testTwoTrueTrueKeepFirst1() {
        first.hasConsumed = true;
        second.hasConsumed = true;
        first.keepConsumedUpToRound = 1;
        assertThat(composite2.awaitEventsConsumption()).isTrue();
        assertThat(first.rounds).isEqualTo(2);
        assertThat(second.rounds).isEqualTo(2);
    }

    @Test public void testTwoTrueTrueKeepSecond1() {
        first.hasConsumed = true;
        second.hasConsumed = true;
        second.keepConsumedUpToRound = 1;
        assertThat(composite2.awaitEventsConsumption()).isTrue();
        assertThat(first.rounds).isEqualTo(3);
        assertThat(second.rounds).isEqualTo(3);
    }

    /**
     * While no correct implementation of AsyncEventsWaiter
     * would continuously return return as this fake for the
     * test does (it would flip back to false after read,
     * even though another thread could set it back to true
     * then), there is a the possibility of two, correctly
     * implemented, AsyncEventsWaiter/s "turning each other
     * on" constantly.  The CompositeAsyncEventsWaiter should
     * not infinitely loop and hang in such a case, but detect
     * it, through an upper bound of number of repetions.
     */
    @Test(expected = ConditionTimeoutException.class)
    public void testNeverInfiniteLoop() {
        AsyncEventsWaiter neverEndingWaiter = () -> true;
        CompositeAsyncEventsWaiter neverEndingComposite
            = new CompositeAsyncEventsWaiter(neverEndingWaiter, neverEndingWaiter);
        assertThat(neverEndingComposite.awaitEventsConsumption()).isTrue();
        neverEndingComposite.close();
    }
}
