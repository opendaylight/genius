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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.TOP_FOO_KEY;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.USES_ONE_KEY;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.complexUsesAugment;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.path;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.topLevelList;
import static org.opendaylight.mdsal.common.api.LogicalDatastoreType.OPERATIONAL;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Uninterruptibles;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.controller.md.sal.binding.test.ConstantSchemaAbstractDataBrokerTest;
import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.genius.datastoreutils.listeners.DataTreeEventCallbackRegistrar;
import org.opendaylight.genius.datastoreutils.listeners.DataTreeEventCallbackRegistrar.NextAction;
import org.opendaylight.genius.datastoreutils.listeners.internal.DataTreeEventCallbackRegistrarImpl;
import org.opendaylight.genius.infra.RetryingManagedNewTransactionRunner;
import org.opendaylight.infrautils.testutils.LogCaptureRule;
import org.opendaylight.infrautils.testutils.LogRule;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.spec.reflect.BindingReflections;
import org.opendaylight.mdsal.common.api.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeComplexUsesAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.TwoLevelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test for {@link DataTreeEventCallbackRegistrarImpl}.
 *
 * @author Michael Vorburger.ch
 */
public class DataTreeEventCallbackRegistrarTest {

    // TODO add similar tests as for onAdd() also for onUpdate() and onDelete() and onAddOrUpdate()

    private static final Logger LOG = LoggerFactory.getLogger(DataTreeEventCallbackRegistrarTest.class);

    private static final InstanceIdentifier<TopLevelList> FOO_PATH = path(TOP_FOO_KEY);
    private static final TopLevelList FOO_DATA = topLevelList(TOP_FOO_KEY, complexUsesAugment(USES_ONE_KEY));

    public @Rule LogRule logRule = new LogRule();
    public @Rule LogCaptureRule logCaptureRule = new LogCaptureRule();

    private final DataBroker db;
    private final SingleTransactionDataBroker db1;

    public DataTreeEventCallbackRegistrarTest() throws Exception {
        // Argument true to make sure we use the multi-threaded DataTreeChangeListenerExecutor
        // because otherwise we hit a deadlock :( with this test!
        ConstantSchemaAbstractDataBrokerTest dataBrokerTest = new ConstantSchemaAbstractDataBrokerTest(true) {
            @Override
            protected Set<YangModuleInfo> getModuleInfos() throws Exception {
                return ImmutableSet.of(BindingReflections.getModuleInfo(TwoLevelList.class),
                        BindingReflections.getModuleInfo(TreeComplexUsesAugment.class));
            }
        };

        dataBrokerTest.setup();
        db = dataBrokerTest.getDataBroker();
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

    @SuppressWarnings("unchecked")
    private void checkAdd(NextAction nextAction) throws TransactionCommitFailedException {
        DataBroker spiedDataBroker = spy(db);

        ListenerRegistration<?> mockListenerReg = mock(ListenerRegistration.class);
        doAnswer(invocation -> {
            ListenerRegistration<?> realReg = db.registerDataTreeChangeListener(
                invocation.getArgument(0),
                invocation.getArgument(1));
            doAnswer(ignored -> {
                realReg.close();
                return null;
            }).when(mockListenerReg).close();
            return mockListenerReg;
        }).when(spiedDataBroker).registerDataTreeChangeListener(any(), any());

        DataTreeEventCallbackRegistrar dataTreeEventCallbackRegistrar =
                new DataTreeEventCallbackRegistrarImpl(spiedDataBroker);

        AtomicBoolean added = new AtomicBoolean(false);
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

        if (nextAction.equals(NextAction.UNREGISTER)) {
            verify(mockListenerReg).close();
        } else {
            added.set(false);
            db1.syncDelete(OPERATIONAL, FOO_PATH);

            db1.syncWrite(OPERATIONAL, FOO_PATH, FOO_DATA);
            await().untilTrue(added);
            verify(mockListenerReg, never()).close();
        }
    }

    @Test
    public void testAddWithTimeoutWhichExpires() throws InterruptedException {
        DataBroker spiedDataBroker = spy(db);

        ListenerRegistration<?> mockListenerReg = mock(ListenerRegistration.class);
        doReturn(mockListenerReg).when(spiedDataBroker).registerDataTreeChangeListener(any(), any());

        DataTreeEventCallbackRegistrar dataTreeEventCallbackRegistrar =
                new DataTreeEventCallbackRegistrarImpl(spiedDataBroker);

        AtomicBoolean timedOut = new AtomicBoolean(false);
        dataTreeEventCallbackRegistrar.onAdd(OPERATIONAL, FOO_PATH, topLevelList -> { /* NOOP */ },
                Duration.ofMillis(50), iid -> {
                if (iid.equals(new DataTreeIdentifier<>(OPERATIONAL, FOO_PATH))) {
                    timedOut.set(true);
                }
            }
        );

        Thread.sleep(75);
        await().untilTrue(timedOut);
        verify(mockListenerReg).close();
    }

    @Test
    public void testAddWithTimeoutNeverHits() throws TransactionCommitFailedException, InterruptedException {
        AtomicBoolean added = new AtomicBoolean(false);
        AtomicBoolean timedOut = new AtomicBoolean(false);
        DataTreeEventCallbackRegistrar dataTreeEventCallbackRegistrar = new DataTreeEventCallbackRegistrarImpl(db);
        dataTreeEventCallbackRegistrar.onAdd(OPERATIONAL, FOO_PATH, topLevelList -> {
            added.set(true);
        }, Duration.ofMillis(3000), iid -> timedOut.set(true));

        // This test is timing sensitive, and a too low timeout value (above), or slow machine, could make this fail :(
        db1.syncWrite(OPERATIONAL, FOO_PATH, FOO_DATA);
        await().untilTrue(added);
        await().untilFalse(timedOut);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testExceptionInCallbackMustBeLogged() throws TransactionCommitFailedException, InterruptedException {
        logCaptureRule.expectLastErrorMessageContains("Error invoking worker");

        DataBroker spiedDataBroker = spy(db);
        final DataTreeChangeListener mockListener = mock(DataTreeChangeListener.class, "TestListener");
        doAnswer(invocation -> db.registerDataTreeChangeListener(invocation.getArgument(0),
                mockListener)).when(spiedDataBroker).registerDataTreeChangeListener(any(), any());

        AtomicBoolean added = new AtomicBoolean(false);
        DataTreeEventCallbackRegistrar dataTreeEventCallbackRegistrar =
                new DataTreeEventCallbackRegistrarImpl(spiedDataBroker);
        dataTreeEventCallbackRegistrar.onAdd(OPERATIONAL, FOO_PATH,
            (Function<TopLevelList, NextAction>) topLevelList -> {
                added.set(true);
                throw new IllegalStateException("TEST");
            });

        ArgumentCaptor<DataTreeChangeListener> realListener = ArgumentCaptor.forClass(DataTreeChangeListener.class);
        verify(spiedDataBroker).registerDataTreeChangeListener(any(), realListener.capture());

        AtomicBoolean onDataTreeChangeDone = new AtomicBoolean(false);
        doAnswer(invocation -> {
            try {
                realListener.getValue().onDataTreeChanged(invocation.getArgument(0));
            } finally {
                onDataTreeChangeDone.set(true);
            }
            return null;
        }).when(mockListener).onDataTreeChanged(anyCollection());

        db1.syncWrite(OPERATIONAL, FOO_PATH, FOO_DATA);
        await().untilTrue(added);
        await().untilTrue(onDataTreeChangeDone);
    }

    @Test
    public void testTimeoutCallbackNotInvokedWhileHandlingChangeNotificationForUnregister() {
        testTimeoutCallbackNotInvokedWhileHandlingChangeNotification(NextAction.UNREGISTER);
    }

    @Test
    public void testTimeoutCallbackIsInvokedWhileHandlingChangeNotificationForCallAgain() {
        testTimeoutCallbackNotInvokedWhileHandlingChangeNotification(NextAction.CALL_AGAIN);
    }

    private void testTimeoutCallbackNotInvokedWhileHandlingChangeNotification(NextAction nextAction) {
        Duration timeout = Duration.ofMillis(10);

        ScheduledExecutorService mockScheduler = mock(ScheduledExecutorService.class);
        ScheduledFuture<?> mockScheduledFuture = mock(ScheduledFuture.class);
        doReturn(mockScheduledFuture).when(mockScheduler).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));

        ListeningScheduledExecutorService directExecutorService = MoreExecutors.listeningDecorator(mockScheduler);

        DataTreeEventCallbackRegistrar callbackRegistrar =
                new DataTreeEventCallbackRegistrarImpl(db, directExecutorService);

        CountDownLatch inChangeCallback = new CountDownLatch(1);
        CountDownLatch changeCallbackContinue = new CountDownLatch(1);
        AtomicBoolean updated = new AtomicBoolean(false);
        AtomicBoolean timedOut = new AtomicBoolean(false);
        callbackRegistrar.onAdd(OPERATIONAL, FOO_PATH, dataObject -> {
            inChangeCallback.countDown();
            Uninterruptibles.awaitUninterruptibly(changeCallbackContinue);

            // Sleep a bit for the timeout task - see below.
            Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
            updated.set(true);
            return nextAction;
        }, timeout, id -> timedOut.set(true));

        ArgumentCaptor<Runnable> timerTask = ArgumentCaptor.forClass(Runnable.class);
        verify(mockScheduler).schedule(timerTask.capture(), eq(timeout.toMillis()), eq(TimeUnit.MILLISECONDS));

        new RetryingManagedNewTransactionRunner(db, 1).callWithNewWriteOnlyTransactionAndSubmit(
            tx -> tx.put(OPERATIONAL, FOO_PATH, FOO_DATA, true));

        // Wait for the change notification callback to be invoked.

        assertThat(Uninterruptibles.awaitUninterruptibly(inChangeCallback, 5, TimeUnit.SECONDS)).isTrue();

        // Now artificially fire the timeout task on a separate thread.

        CountDownLatch timerTaskDone = new CountDownLatch(1);
        new Thread(() -> {
            // We have to tell the notification change callback to continue prior to invoking the timeout task as
            // the latter should block internally in DataTreeEventCallbackRegistrarImpl while the change notification
            // is still in progress. The change callback sleeps a bit to give the timeout task plenty of time to
            // complete if it didn't block.
            changeCallbackContinue.countDown();
            timerTask.getValue().run();
            timerTaskDone.countDown();
        }).start();

        await().timeout(5, TimeUnit.SECONDS).untilTrue(updated);

        assertThat(Uninterruptibles.awaitUninterruptibly(timerTaskDone, 5, TimeUnit.SECONDS)).isTrue();

        if (nextAction.equals(NextAction.UNREGISTER)) {
            assertThat(timedOut.get()).isFalse();
            verify(mockScheduledFuture).cancel(anyBoolean());
        } else {
            assertThat(timedOut.get()).isTrue();
            verify(mockScheduledFuture, never()).cancel(anyBoolean());
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testChangeCallbackNotInvokedAfterTimeout() {
        Duration timeout = Duration.ofMillis(10);

        ScheduledExecutorService mockScheduler = mock(ScheduledExecutorService.class);
        ScheduledFuture<?> mockScheduledFuture = mock(ScheduledFuture.class);
        doReturn(mockScheduledFuture).when(mockScheduler).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));

        ListeningScheduledExecutorService directExecutorService = MoreExecutors.listeningDecorator(mockScheduler);

        DataBroker spiedDataBroker = spy(db);

        final DataTreeChangeListener mockListener = mock(DataTreeChangeListener.class);
        doAnswer(invocation -> {
            db.registerDataTreeChangeListener(invocation.getArgument(0), mockListener);
            return mock(ListenerRegistration.class);
        }).when(spiedDataBroker).registerDataTreeChangeListener(any(), any());

        DataTreeEventCallbackRegistrar callbackRegistrar =
                new DataTreeEventCallbackRegistrarImpl(spiedDataBroker, directExecutorService);

        AtomicBoolean updated = new AtomicBoolean(false);
        AtomicBoolean timedOut = new AtomicBoolean(false);
        callbackRegistrar.onAdd(OPERATIONAL, FOO_PATH, dataObject -> {
            updated.set(true);
            return NextAction.UNREGISTER;
        }, timeout, id -> timedOut.set(true));

        ArgumentCaptor<Runnable> timerTask = ArgumentCaptor.forClass(Runnable.class);
        verify(mockScheduler).schedule(timerTask.capture(), eq(timeout.toMillis()), eq(TimeUnit.MILLISECONDS));

        ArgumentCaptor<DataTreeChangeListener> realListener = ArgumentCaptor.forClass(DataTreeChangeListener.class);
        verify(spiedDataBroker).registerDataTreeChangeListener(any(), realListener.capture());

        timerTask.getValue().run();
        assertThat(timedOut.get()).isTrue();

        AtomicBoolean onDataTreeChangeDone = new AtomicBoolean(false);
        doAnswer(invocation -> {
            try {
                realListener.getValue().onDataTreeChanged(invocation.getArgument(0));
            } finally {
                onDataTreeChangeDone.set(true);
            }
            return null;
        }).when(mockListener).onDataTreeChanged(anyCollection());

        new RetryingManagedNewTransactionRunner(db, 1).callWithNewWriteOnlyTransactionAndSubmit(
            tx -> tx.put(OPERATIONAL, FOO_PATH, FOO_DATA, true));

        await().untilTrue(onDataTreeChangeDone);
        assertThat(updated.get()).isFalse();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testChangeCallbackOccursImmediatelyAfterRegistration() {
        ScheduledExecutorService mockScheduler = mock(ScheduledExecutorService.class);
        ScheduledFuture<?> mockScheduledFuture = mock(ScheduledFuture.class);
        doReturn(mockScheduledFuture).when(mockScheduler).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));

        DataBroker spiedDataBroker = spy(db);

        AtomicBoolean updated = new AtomicBoolean(false);
        ListenerRegistration<?> mockListenerReg = mock(ListenerRegistration.class);
        doAnswer(invocation -> {
            DataTreeChangeListener<?> listener = invocation.getArgument(1);
            db.registerDataTreeChangeListener(invocation.getArgument(0), listener);
            db1.syncWrite(OPERATIONAL, FOO_PATH, FOO_DATA);
            await().untilTrue(updated);
            return mockListenerReg;
        }).when(spiedDataBroker).registerDataTreeChangeListener(any(), any());

        DataTreeEventCallbackRegistrar callbackRegistrar =
                new DataTreeEventCallbackRegistrarImpl(spiedDataBroker, mockScheduler);

        callbackRegistrar.onAdd(OPERATIONAL, FOO_PATH, dataObject -> {
            updated.set(true);
            return NextAction.UNREGISTER;
        }, Duration.ofMillis(10), id -> { });

        await().untilTrue(updated);
        verify(mockListenerReg).close();
        verify(mockScheduledFuture).cancel(anyBoolean());
    }
}
