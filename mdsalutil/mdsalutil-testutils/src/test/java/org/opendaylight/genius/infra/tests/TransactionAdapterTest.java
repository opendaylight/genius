/*
 * Copyright © 2018 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra.tests;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.TOP_FOO_KEY;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.path;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.topLevelList;

import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.DataBrokerTestModule;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.genius.datastoreutils.testutils.DataBrokerFailuresImpl;
import org.opendaylight.genius.infra.Datastore;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.infra.TransactionAdapter;
import org.opendaylight.infrautils.testutils.LogCaptureRule;
import org.opendaylight.infrautils.testutils.LogRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeComplexUsesAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeComplexUsesAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.complex.from.grouping.ContainerWithUsesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Test for {@link TransactionAdapter}.
 */
// This is a test for a deprecated class
@SuppressWarnings("deprecation")
public class TransactionAdapterTest {

    private static final InstanceIdentifier<TopLevelList> TEST_PATH = path(TOP_FOO_KEY);

    public @Rule
        LogRule logRule = new LogRule();
    public @Rule
        LogCaptureRule logCaptureRule = new LogCaptureRule();

    private SingleTransactionDataBroker singleTransactionDataBroker;
    private ManagedNewTransactionRunner managedNewTransactionRunner;

    private ManagedNewTransactionRunner createManagedNewTransactionRunnerToTest(DataBroker dataBroker) {
        return new ManagedNewTransactionRunnerImpl(dataBroker);
    }

    @Before
    public void beforeTest() {
        DataBrokerFailuresImpl testableDataBroker =
            new DataBrokerFailuresImpl(new DataBrokerTestModule(true).getDataBroker());
        managedNewTransactionRunner = createManagedNewTransactionRunnerToTest(testableDataBroker);
        singleTransactionDataBroker = new SingleTransactionDataBroker(testableDataBroker);
    }

    @Test
    public void testAdaptedWriteTransactionPutsSuccessfully() throws Exception {
        TopLevelList data = newTestDataObject();
        managedNewTransactionRunner.callWithNewWriteOnlyTransactionAndSubmit(Datastore.OPERATIONAL,
            writeTx -> TransactionAdapter.toWriteTransaction(writeTx).put(LogicalDatastoreType.OPERATIONAL,
                    TEST_PATH, data)).get();
        assertEquals(data, singleTransactionDataBroker.syncRead(OPERATIONAL, TEST_PATH));
    }

    @Test
    public void testAdaptedReadWriteTransactionPutsSuccessfully() throws Exception {
        TopLevelList data = newTestDataObject();
        managedNewTransactionRunner.callWithNewReadWriteTransactionAndSubmit(Datastore.OPERATIONAL,
            writeTx -> TransactionAdapter.toReadWriteTransaction(writeTx).put(LogicalDatastoreType.OPERATIONAL,
                    TEST_PATH, data)).get();
        assertEquals(data, singleTransactionDataBroker.syncRead(OPERATIONAL, TEST_PATH));
    }

    @Test
    public void testAdaptedWriteTransactionFailsOnInvalidDatastore() throws Exception {
        assertTrue(assertThrows(ExecutionException.class, () -> {
            managedNewTransactionRunner.callWithNewWriteOnlyTransactionAndSubmit(Datastore.OPERATIONAL,
                writeTx -> TransactionAdapter.toWriteTransaction(writeTx).put(CONFIGURATION, TEST_PATH,
                    newTestDataObject())).get();
            fail("This should have led to an ExecutionException!");
        }).getCause() instanceof IllegalArgumentException);
        assertThat(singleTransactionDataBroker.syncReadOptional(OPERATIONAL, TEST_PATH)).isAbsent();
    }

    @Test
    public void testAdaptedReadWriteTransactionFailsOnInvalidDatastore() throws Exception {
        assertTrue(assertThrows(ExecutionException.class, () -> {
            managedNewTransactionRunner.callWithNewReadWriteTransactionAndSubmit(Datastore.OPERATIONAL,
                writeTx -> TransactionAdapter.toReadWriteTransaction(writeTx).put(CONFIGURATION, TEST_PATH,
                    newTestDataObject())).get();
            fail("This should have led to an ExecutionException!");
        }).getCause() instanceof IllegalArgumentException);
        assertThat(singleTransactionDataBroker.syncReadOptional(OPERATIONAL, TEST_PATH)).isAbsent();
    }

    @Test
    public void testAdaptedWriteTransactionCannotCommit() {
        assertThrows(ExecutionException.class,
            () -> managedNewTransactionRunner.callWithNewWriteOnlyTransactionAndSubmit(Datastore.OPERATIONAL,
                tx -> TransactionAdapter.toWriteTransaction(tx).commit()).get());
    }

    @Test
    public void testAdaptedReadWriteTransactionCannotCommit() {
        assertThrows(ExecutionException.class,
            () -> managedNewTransactionRunner.callWithNewReadWriteTransactionAndSubmit(Datastore.OPERATIONAL,
                tx -> TransactionAdapter.toReadWriteTransaction(tx).commit()).get());
    }

    @Test
    public void testAdaptedWriteTransactionCannotCancel() {
        assertThrows(ExecutionException.class,
            () -> managedNewTransactionRunner.callWithNewWriteOnlyTransactionAndSubmit(Datastore.OPERATIONAL,
                tx -> TransactionAdapter.toWriteTransaction(tx).cancel()).get());
    }

    @Test
    public void testAdaptedReadWriteTransactionCannotCancel() {
        assertThrows(ExecutionException.class,
            () -> managedNewTransactionRunner.callWithNewReadWriteTransactionAndSubmit(Datastore.OPERATIONAL,
                tx -> TransactionAdapter.toReadWriteTransaction(tx).cancel()).get());
    }

    private TopLevelList newTestDataObject() {
        TreeComplexUsesAugment fooAugment = new TreeComplexUsesAugmentBuilder()
            .setContainerWithUses(new ContainerWithUsesBuilder().setLeafFromGrouping("foo").build()).build();
        return topLevelList(TOP_FOO_KEY, fooAugment);
    }

}
