/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra.tests;

import static org.junit.Assert.assertEquals;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.OptimisticLockFailedException;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.RetryingManagedNewTransactionRunner;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;

/**
 * Test for {@link RetryingManagedNewTransactionRunner}.
 * Note that this test (intentionally) extends the {@link ManagedNewTransactionRunnerImplTest}.
 *
 * @author Michael Vorburger.ch
 */
public class RetryingManagedNewTransactionRunnerTest extends ManagedNewTransactionRunnerImplTest {

    @Override
    protected ManagedNewTransactionRunner createManagedNewTransactionRunnerToTest(DataBroker dataBroker) {
        return new RetryingManagedNewTransactionRunner(dataBroker);
    }

    @Override
    public void testCallWithNewWriteOnlyTransactionOptimisticLockFailedException() throws Exception {
        // contrary to the super() test implementation for (just) ManagedNewTransactionRunnerImpl, in the parent class
        // here we expect the x2 OptimisticLockFailedException to be retried, and then eventually succeed:
        testableDataBroker.failSubmits(2, new OptimisticLockFailedException("bada boum bam!"));
        TopLevelList data = newTestDataObject();
        managedNewTransactionRunner.callWithNewWriteOnlyTransactionAndSubmit(
            writeTx -> writeTx.put(LogicalDatastoreType.OPERATIONAL, TEST_PATH, data)).get();
        assertEquals(data, singleTransactionDataBroker.syncRead(OPERATIONAL, TEST_PATH));
    }

    @Override
    public void testCallWithNewReadWriteTransactionOptimisticLockFailedException() throws Exception {
        // contrary to the super() test implementation for (just) ManagedNewTransactionRunnerImpl, in the parent class
        // here we expect the x2 OptimisticLockFailedException to be retried, and then eventually succeed:
        testableDataBroker.failSubmits(2, new OptimisticLockFailedException("bada boum bam!"));
        TopLevelList data = newTestDataObject();
        managedNewTransactionRunner.callWithNewReadWriteTransactionAndSubmit(
            writeTx -> writeTx.put(LogicalDatastoreType.OPERATIONAL, TEST_PATH, data)).get();
        assertEquals(data, singleTransactionDataBroker.syncRead(OPERATIONAL, TEST_PATH));
    }

    @Override
    public void testApplyWithNewReadWriteTransactionOptimisticLockFailedException() throws Exception {
        // contrary to the super() test implementation for (just) ManagedNewTransactionRunnerImpl, in the parent class
        // here we expect the x2 OptimisticLockFailedException to be retried, and then eventually succeed:
        testableDataBroker.failSubmits(2, new OptimisticLockFailedException("bada boum bam!"));
        TopLevelList data = newTestDataObject();
        assertEquals(1, (long) managedNewTransactionRunner.applyWithNewReadWriteTransactionAndSubmit(
            writeTx -> {
                writeTx.put(LogicalDatastoreType.OPERATIONAL, TEST_PATH, data);
                return 1;
            }).get());
        assertEquals(data, singleTransactionDataBroker.syncRead(OPERATIONAL, TEST_PATH));
    }
}
