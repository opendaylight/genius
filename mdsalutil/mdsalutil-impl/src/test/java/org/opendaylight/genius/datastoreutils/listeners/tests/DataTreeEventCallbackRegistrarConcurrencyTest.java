/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.listeners.tests;

import static com.google.common.truth.Truth.assertThat;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.TOP_FOO_KEY;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.path;
import static org.opendaylight.genius.datastoreutils.listeners.DataTreeEventCallbackRegistrar.NextAction.UNREGISTER;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.opendaylight.genius.datastoreutils.listeners.DataTreeEventCallbackRegistrar;
import org.opendaylight.genius.datastoreutils.listeners.DataTreeEventCallbackRegistrar.NextAction;
import org.opendaylight.genius.datastoreutils.listeners.internal.DataTreeEventCallbackRegistrarImpl;
import org.opendaylight.genius.datastoreutils.testutils.TestableDataBroker;
import org.opendaylight.infrautils.testutils.concurrent.TestableScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests for {@link DataTreeEventCallbackRegistrarImpl} specifically related to
 * lower-level concurrency edge cases.
 *
 * @see DataTreeEventCallbackRegistrarTest
 *
 * @author Michael Vorburger.ch
 */
public class DataTreeEventCallbackRegistrarConcurrencyTest {

    private static final Logger LOG = LoggerFactory.getLogger(DataTreeEventCallbackRegistrarConcurrencyTest.class);

    @Test
    public void testTimeoutCallbackNotInvokedWhileHandlingChangeNotification() {
        checkTimeoutCallbackNotInvokedWhileHandlingChangeNotification(NextAction.UNREGISTER);
    }

    @Test
    public void testTimeoutCallbackEventuallyInvokedWhileHandlingChangeNotification() {
        checkTimeoutCallbackNotInvokedWhileHandlingChangeNotification(NextAction.CALL_AGAIN);
    }

    private void checkTimeoutCallbackNotInvokedWhileHandlingChangeNotification(NextAction nextAction) {
        AtomicBoolean updated = new AtomicBoolean(false);
        AtomicBoolean timedOut = new AtomicBoolean(false);
        TestableDataBroker db = TestableDataBroker.newInstance();
        TestableScheduledExecutorService scheduler = TestableScheduledExecutorService.newInstance();
        DataTreeEventCallbackRegistrar callbackRegistrar = new DataTreeEventCallbackRegistrarImpl(db, scheduler);
        callbackRegistrar.onAdd(OPERATIONAL, path(TOP_FOO_KEY), dataObject -> {
            try {
                updated.wait(10000);
            } catch (InterruptedException e) {
                LOG.error("InterruptedException was not expected", e);
                throw new RuntimeException(e);
            }
            updated.set(true);
            return nextAction;
        }, Duration.ofNanos(1), id -> timedOut.set(true));

        assertThat(updated.get()).isFalse();
        assertThat(timedOut.get()).isFalse();

        db.asyncFireDataTreeChangeListener();
        assertThat(updated.get()).isFalse();
        assertThat(timedOut.get()).isFalse();

        scheduler.runScheduled(); // time out
        assertThat(updated.get()).isFalse();
        assertThat(timedOut.get()).isFalse();

        updated.notifyAll();
        assertThat(updated.get()).isTrue();

        if (nextAction.equals(UNREGISTER)) {
            assertThat(timedOut.get()).isFalse();
        } else {
            assertThat(timedOut.get()).isTrue();
        }
    }

    @Test
    public void testCallbackNotInvokedAnymoreWhenAlreadyInTimeout() {
        AtomicBoolean updated = new AtomicBoolean(false);
        AtomicBoolean timedOut = new AtomicBoolean(false);
        TestableDataBroker db = TestableDataBroker.newInstance();
        TestableScheduledExecutorService scheduler = TestableScheduledExecutorService.newInstance();
        DataTreeEventCallbackRegistrar callbackRegistrar = new DataTreeEventCallbackRegistrarImpl(db, scheduler);
        callbackRegistrar.onAdd(OPERATIONAL, path(TOP_FOO_KEY), dataObject -> {
            updated.set(true);
        }, Duration.ofNanos(1), id -> {
                try {
                    timedOut.wait(10000);
                } catch (InterruptedException e) {
                    LOG.error("InterruptedException was not expected", e);
                    throw new RuntimeException(e);
                }
                timedOut.set(true);
            });

        assertThat(updated.get()).isFalse();
        assertThat(timedOut.get()).isFalse();

        scheduler.runScheduled(); // time out
        assertThat(updated.get()).isFalse();
        assertThat(timedOut.get()).isFalse();

        db.asyncFireDataTreeChangeListener();
        assertThat(updated.get()).isFalse();
        assertThat(timedOut.get()).isFalse();

        timedOut.notifyAll();
        assertThat(updated.get()).isFalse();
        assertThat(timedOut.get()).isTrue();
    }

}
