/*
 * Copyright (c) 2017 - 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.listeners.tests;

import static com.google.common.truth.Truth.assertThat;
import static org.awaitility.Awaitility.await;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.TOP_FOO_KEY;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.USES_ONE_KEY;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.complexUsesAugment;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.path;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.topLevelList;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import org.junit.Rule;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.test.DataBrokerTestModule;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.genius.datastoreutils.listeners.DataTreeEventCallbackRegistrar;
import org.opendaylight.genius.datastoreutils.listeners.DataTreeEventCallbackRegistrar.NextAction;
import org.opendaylight.genius.datastoreutils.listeners.internal.DataTreeEventCallbackRegistrarImpl;
import org.opendaylight.infrautils.testutils.LogCaptureRule;
import org.opendaylight.infrautils.testutils.LogRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test for {@link DataTreeEventCallbackRegistrarImpl}.
 *
 * @author Michael Vorburger.ch
 */
public class DataTreeEventCallbackRegistrarTest {

    // TODO add similar tests as for onAdd() also for onUpdate() and onDelete() and onAddOrUpdate()

    // TODO This test may suffer from hard to reproduce failures related to timing issues on very slow machines
    // We would need the DataBrokerTestModule to make the multi-threaded DataTreeChangeListenerExecutor await-able.

    private static final Logger LOG = LoggerFactory.getLogger(DataTreeEventCallbackRegistrarTest.class);

    private static final InstanceIdentifier<TopLevelList> FOO_PATH = path(TOP_FOO_KEY);
    private static final TopLevelList FOO_DATA = topLevelList(TOP_FOO_KEY, complexUsesAugment(USES_ONE_KEY));

    public @Rule LogRule logRule = new LogRule();
    public @Rule LogCaptureRule logCaptureRule = new LogCaptureRule();

    private final DataBroker db;
    private final SingleTransactionDataBroker db1;

    public DataTreeEventCallbackRegistrarTest() {
        // Argument true to make sure we use the multi-threaded DataTreeChangeListenerExecutor
        // because otherwise we hit a deadlock :( with this test!
        db = new DataBrokerTestModule(true).getDataBroker();
        db1 = new SingleTransactionDataBroker(db);
    }

    @Test
    public void testAddAndUnregister() throws TransactionCommitFailedException {
        checkAdd(NextAction.UNREGISTER);
    }

    @Test
    public void testAddAndKeepRegistered() throws TransactionCommitFailedException {
        checkAdd(NextAction.CALL_AGAIN);
    }

    @Test
    public void testAddOrUpdateAdd() throws TransactionCommitFailedException {
        DataTreeEventCallbackRegistrar dataTreeEventCallbackRegistrar = new DataTreeEventCallbackRegistrarImpl(db);
        AtomicBoolean added = new AtomicBoolean(false);
        dataTreeEventCallbackRegistrar.onAddOrUpdate(OPERATIONAL, FOO_PATH, (first, second) -> {
            if (first == null && second != null) {
                added.set(true);
            }
            return NextAction.UNREGISTER;
        });
        db1.syncWrite(OPERATIONAL, FOO_PATH, FOO_DATA);
        await().untilTrue(added);

    }

    @Test
    public void testAddOrUpdateUpdate() throws TransactionCommitFailedException {
        DataTreeEventCallbackRegistrar dataTreeEventCallbackRegistrar = new DataTreeEventCallbackRegistrarImpl(db);
        AtomicBoolean updated = new AtomicBoolean(false);
        dataTreeEventCallbackRegistrar.onAddOrUpdate(OPERATIONAL, FOO_PATH, (first, second) -> {
            if (first != null && second != null) {
                updated.set(true);
                return NextAction.UNREGISTER;
            }
            return NextAction.CALL_AGAIN;
        });
        db1.syncWrite(OPERATIONAL, FOO_PATH, FOO_DATA);
        db1.syncWrite(OPERATIONAL, FOO_PATH, FOO_DATA);
        await().untilTrue(updated);

    }

    private void checkAdd(NextAction nextAction) throws TransactionCommitFailedException {
        AtomicBoolean added = new AtomicBoolean(false);
        DataTreeEventCallbackRegistrar dataTreeEventCallbackRegistrar = new DataTreeEventCallbackRegistrarImpl(db);
        dataTreeEventCallbackRegistrar.onAdd(OPERATIONAL, FOO_PATH, topLevelList -> {
            if (topLevelList.equals(FOO_DATA)) {
                added.set(true);
            } else {
                LOG.error("Expected: {} but was: {}", FOO_DATA, topLevelList);
                assertThat(topLevelList).isEqualTo(FOO_DATA);
            }
            return nextAction;
        });
        db1.syncWrite(OPERATIONAL, FOO_PATH, FOO_DATA);
        await().untilTrue(added);

        added.set(false);
        db1.syncDelete(OPERATIONAL, FOO_PATH);

        db1.syncWrite(OPERATIONAL, FOO_PATH, FOO_DATA);
        if (nextAction.equals(NextAction.CALL_AGAIN)) {
            await().untilTrue(added);
        } else {
            // TODO see above; this actually isn't really reliable.. it could test "too soon"
            await().untilFalse(added);
        }
    }

    @Test
    public void testAddTimeoutWhichExpires() throws InterruptedException {
        AtomicBoolean timedOut = new AtomicBoolean(false);
        DataTreeEventCallbackRegistrar dataTreeEventCallbackRegistrar = new DataTreeEventCallbackRegistrarImpl(db);
        dataTreeEventCallbackRegistrar.onAdd(OPERATIONAL, FOO_PATH, topLevelList -> { /* NOOP */ },
                Duration.ofMillis(50), iid -> {
                if (iid.equals(new DataTreeIdentifier<>(OPERATIONAL, FOO_PATH))) {
                    timedOut.set(true);
                }
            }
        );
        Thread.sleep(75);
        await().untilTrue(timedOut);
    }

    @Test
    public void testExceptionInCallbackMustBeLogged() throws TransactionCommitFailedException, InterruptedException {
        logCaptureRule.expectLastErrorMessageContains("TestConsumer");
        AtomicBoolean added = new AtomicBoolean(false);
        DataTreeEventCallbackRegistrar dataTreeEventCallbackRegistrar = new DataTreeEventCallbackRegistrarImpl(db);
        dataTreeEventCallbackRegistrar.onAdd(OPERATIONAL, FOO_PATH, new Function<TopLevelList, NextAction>() {

            @Override
            public NextAction apply(TopLevelList topLevelList) {
                added.set(true);
                throw new IllegalStateException("TEST");
            }


            @Override
            public String toString() {
                return "TestConsumer";
            }

        });
        db1.syncWrite(OPERATIONAL, FOO_PATH, FOO_DATA);
        await().untilTrue(added);
        // TODO see above we can remove this once we can await DataBroker listeners
        // The sleep () is required :( so that the throw new IllegalStateException really leads to an ERROR log,
        // because the (easily) await().untilTrue(...) could theoretically complete immediately after added.set(true)
        // but before the throw new IllegalStateException("TEST") and LOG.  To make this more reliable and without sleep
        // would require more work inside DataBroker to be able to await listener event processing.
        Thread.sleep(100);
    }

}
