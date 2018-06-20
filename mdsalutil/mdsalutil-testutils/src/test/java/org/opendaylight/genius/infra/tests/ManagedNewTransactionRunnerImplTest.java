/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra.tests;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.TOP_FOO_KEY;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.path;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.topLevelList;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.DataBrokerTestModule;
import org.opendaylight.controller.md.sal.common.api.data.AsyncWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.OptimisticLockFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.genius.datastoreutils.testutils.DataBrokerFailuresImpl;
import org.opendaylight.genius.infra.Datastore;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.infrautils.testutils.LogCaptureRule;
import org.opendaylight.infrautils.testutils.LogRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeComplexUsesAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeComplexUsesAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.complex.from.grouping.ContainerWithUsesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Test for {@link ManagedNewTransactionRunnerImpl}.
 *
 * @author Michael Vorburger.ch
 * @author Stephen Kitt
 */
public class ManagedNewTransactionRunnerImplTest {

    static final InstanceIdentifier<TopLevelList> TEST_PATH = path(TOP_FOO_KEY);

    public @Rule LogRule logRule = new LogRule();
    public @Rule LogCaptureRule logCaptureRule = new LogCaptureRule();

    DataBrokerFailuresImpl testableDataBroker;
    SingleTransactionDataBroker singleTransactionDataBroker;
    ManagedNewTransactionRunner managedNewTransactionRunner;

    protected ManagedNewTransactionRunner createManagedNewTransactionRunnerToTest(DataBroker dataBroker) {
        return new ManagedNewTransactionRunnerImpl(dataBroker);
    }

    @Before
    public void beforeTest() {
        testableDataBroker = new DataBrokerFailuresImpl(new DataBrokerTestModule(true).getDataBroker());
        managedNewTransactionRunner = createManagedNewTransactionRunnerToTest(testableDataBroker);
        singleTransactionDataBroker = new SingleTransactionDataBroker(testableDataBroker);
    }

    @Test
    public void testCallWithNewWriteOnlyTransactionAndSubmitEmptySuccessfully() throws Exception {
        managedNewTransactionRunner.callWithNewWriteOnlyTransactionAndSubmit(writeTx -> { }).get();
    }

    @Test
    public void testCallWithNewReadWriteTransactionAndSubmitEmptySuccessfully() throws Exception {
        managedNewTransactionRunner.callWithNewReadWriteTransactionAndSubmit(tx -> { }).get();
    }

    @Test
    public void testApplyWithNewReadWriteTransactionAndSubmitEmptySuccessfully() throws Exception {
        assertEquals(1,
            (long) managedNewTransactionRunner.applyWithNewReadWriteTransactionAndSubmit(Datastore.OPERATIONAL,
                tx -> 1).get());
    }

    @Test
    public void testCallWithNewWriteOnlyTransactionAndSubmitPutSuccessfully() throws Exception {
        TopLevelList data = newTestDataObject();
        managedNewTransactionRunner.callWithNewWriteOnlyTransactionAndSubmit(
            writeTx -> writeTx.put(LogicalDatastoreType.OPERATIONAL, TEST_PATH, data)).get();
        assertEquals(data, singleTransactionDataBroker.syncRead(OPERATIONAL, TEST_PATH));
    }

    @Test
    public void testCallWithNewReadWriteTransactionAndSubmitPutSuccessfully() throws Exception {
        TopLevelList data = newTestDataObject();
        managedNewTransactionRunner.callWithNewReadWriteTransactionAndSubmit(
            tx -> tx.put(OPERATIONAL, TEST_PATH, data)).get();
        assertEquals(data, singleTransactionDataBroker.syncRead(OPERATIONAL, TEST_PATH));
    }

    @Test
    public void testApplyWithNewReadWriteTransactionAndSubmitPutSuccessfully() throws Exception {
        TopLevelList data = newTestDataObject();
        assertEquals(1, (long) managedNewTransactionRunner.applyWithNewReadWriteTransactionAndSubmit(
            Datastore.OPERATIONAL,
            tx -> {
                tx.put(TEST_PATH, data);
                return 1;
            }).get());
        assertEquals(data, singleTransactionDataBroker.syncRead(OPERATIONAL, TEST_PATH));
    }

    TopLevelList newTestDataObject() {
        TreeComplexUsesAugment fooAugment = new TreeComplexUsesAugmentBuilder()
                .setContainerWithUses(new ContainerWithUsesBuilder().setLeafFromGrouping("foo").build()).build();
        return topLevelList(TOP_FOO_KEY, fooAugment);
    }

    @Test
    public void testCallWithNewWriteOnlyTransactionAndSubmitPutButLaterException() throws Exception {
        try {
            managedNewTransactionRunner.callWithNewWriteOnlyTransactionAndSubmit(writeTx -> {
                writeTx.put(OPERATIONAL, TEST_PATH, newTestDataObject());
                // We now throw an arbitrary kind of checked (not unchecked!) exception here
                throw new IOException("something didn't quite go as expected...");
            }).get();
            fail("This should have lead to an ExecutionException!");
        } catch (ExecutionException e) {
            assertThat(e.getCause() instanceof IOException).isTrue();
        }
        assertThat(singleTransactionDataBroker.syncReadOptional(OPERATIONAL, TEST_PATH)).isAbsent();
    }

    @Test
    public void testCallWithNewReadWriteTransactionAndSubmitPutButLaterException() throws Exception {
        try {
            managedNewTransactionRunner.callWithNewReadWriteTransactionAndSubmit(writeTx -> {
                writeTx.put(OPERATIONAL, TEST_PATH, newTestDataObject());
                // We now throw an arbitrary kind of checked (not unchecked!) exception here
                throw new IOException("something didn't quite go as expected...");
            }).get();
            fail("This should have led to an ExecutionException!");
        } catch (ExecutionException e) {
            assertThat(e.getCause() instanceof IOException).isTrue();
        }
        assertThat(singleTransactionDataBroker.syncReadOptional(OPERATIONAL, TEST_PATH)).isAbsent();
    }

    @Test
    public void testApplyWithNewReadWriteTransactionAndSubmitPutButLaterException() throws Exception {
        try {
            managedNewTransactionRunner.applyWithNewReadWriteTransactionAndSubmit(Datastore.OPERATIONAL, writeTx -> {
                writeTx.put(TEST_PATH, newTestDataObject());
                // We now throw an arbitrary kind of checked (not unchecked!) exception here
                throw new IOException("something didn't quite go as expected...");
            }).get();
            fail("This should have led to an ExecutionException!");
        } catch (ExecutionException e) {
            assertThat(e.getCause() instanceof IOException).isTrue();
        }
        assertThat(singleTransactionDataBroker.syncReadOptional(OPERATIONAL, TEST_PATH)).isAbsent();
    }

    @Test
    public void testCallWithNewWriteOnlyTransactionCommitFailedException() throws Exception {
        try {
            testableDataBroker.failSubmits(new TransactionCommitFailedException("bada boum bam!"));
            managedNewTransactionRunner.callWithNewWriteOnlyTransactionAndSubmit(
                writeTx -> writeTx.put(LogicalDatastoreType.OPERATIONAL, TEST_PATH, newTestDataObject())).get();
            fail("This should have led to an ExecutionException!");
        } catch (ExecutionException e) {
            assertThat(e.getCause() instanceof TransactionCommitFailedException).isTrue();
        }
        assertThat(singleTransactionDataBroker.syncReadOptional(OPERATIONAL, TEST_PATH)).isAbsent();
    }

    @Test
    public void testCallWithNewReadWriteTransactionCommitFailedException() throws Exception {
        try {
            testableDataBroker.failSubmits(new TransactionCommitFailedException("bada boum bam!"));
            managedNewTransactionRunner.callWithNewReadWriteTransactionAndSubmit(
                writeTx -> writeTx.put(LogicalDatastoreType.OPERATIONAL, TEST_PATH, newTestDataObject())).get();
            fail("This should have led to an ExecutionException!");
        } catch (ExecutionException e) {
            assertThat(e.getCause() instanceof TransactionCommitFailedException).isTrue();
        }
        assertThat(singleTransactionDataBroker.syncReadOptional(OPERATIONAL, TEST_PATH)).isAbsent();
    }

    @Test
    public void testApplyWithNewReadWriteTransactionCommitFailedException() throws Exception {
        try {
            testableDataBroker.failSubmits(new TransactionCommitFailedException("bada boum bam!"));
            managedNewTransactionRunner.applyWithNewReadWriteTransactionAndSubmit(Datastore.OPERATIONAL,
                writeTx -> {
                    writeTx.put(TEST_PATH, newTestDataObject());
                    return 1;
                }).get();
            fail("This should have led to an ExecutionException!");
        } catch (ExecutionException e) {
            assertThat(e.getCause() instanceof TransactionCommitFailedException).isTrue();
        }
        assertThat(singleTransactionDataBroker.syncReadOptional(OPERATIONAL, TEST_PATH)).isAbsent();
    }

    @Test
    public void testCallWithNewWriteOnlyTransactionOptimisticLockFailedException() throws Exception {
        try {
            testableDataBroker.failSubmits(2, new OptimisticLockFailedException("bada boum bam!"));
            managedNewTransactionRunner.callWithNewWriteOnlyTransactionAndSubmit(
                writeTx -> writeTx.put(LogicalDatastoreType.OPERATIONAL, TEST_PATH, newTestDataObject())).get();
            fail("This should have led to an ExecutionException!");
        } catch (ExecutionException e) {
            assertThat(e.getCause() instanceof OptimisticLockFailedException).isTrue();
        }
        assertThat(singleTransactionDataBroker.syncReadOptional(OPERATIONAL, TEST_PATH)).isAbsent();
    }

    @Test
    public void testCallWithNewReadWriteTransactionOptimisticLockFailedException() throws Exception {
        try {
            testableDataBroker.failSubmits(2, new OptimisticLockFailedException("bada boum bam!"));
            managedNewTransactionRunner.callWithNewReadWriteTransactionAndSubmit(
                writeTx -> writeTx.put(LogicalDatastoreType.OPERATIONAL, TEST_PATH, newTestDataObject())).get();
            fail("This should have led to an ExecutionException!");
        } catch (ExecutionException e) {
            assertThat(e.getCause() instanceof OptimisticLockFailedException).isTrue();
        }
        assertThat(singleTransactionDataBroker.syncReadOptional(OPERATIONAL, TEST_PATH)).isAbsent();
    }

    @Test
    public void testApplyWithNewReadWriteTransactionOptimisticLockFailedException() throws Exception {
        try {
            testableDataBroker.failSubmits(2, new OptimisticLockFailedException("bada boum bam!"));
            managedNewTransactionRunner.applyWithNewReadWriteTransactionAndSubmit(Datastore.OPERATIONAL,
                writeTx -> {
                    writeTx.put(TEST_PATH, newTestDataObject());
                    return 1;
                }).get();
            fail("This should have led to an ExecutionException!");
        } catch (ExecutionException e) {
            assertThat(e.getCause() instanceof OptimisticLockFailedException).isTrue();
        }
        assertThat(singleTransactionDataBroker.syncReadOptional(OPERATIONAL, TEST_PATH)).isAbsent();
    }

    @Test(expected = ExecutionException.class)
    public void testCallWithNewWriteOnlyTransactionAndSubmitCannotCommit() throws Exception {
        managedNewTransactionRunner.callWithNewWriteOnlyTransactionAndSubmit(AsyncWriteTransaction::commit).get();
    }

    @Test(expected = ExecutionException.class)
    public void testCallWithNewReadWriteTransactionAndSubmitCannotCommit() throws Exception {
        managedNewTransactionRunner.callWithNewReadWriteTransactionAndSubmit(AsyncWriteTransaction::commit).get();
    }

    @Test(expected = ExecutionException.class)
    public void testCallWithNewWriteOnlyTransactionAndSubmitCannotCancel() throws Exception {
        managedNewTransactionRunner.callWithNewWriteOnlyTransactionAndSubmit(AsyncWriteTransaction::cancel).get();
    }

    @Test(expected = ExecutionException.class)
    public void testCallWithNewReadWriteTransactionAndSubmitCannotCancel() throws Exception {
        managedNewTransactionRunner.callWithNewReadWriteTransactionAndSubmit(AsyncWriteTransaction::cancel).get();
    }
}
