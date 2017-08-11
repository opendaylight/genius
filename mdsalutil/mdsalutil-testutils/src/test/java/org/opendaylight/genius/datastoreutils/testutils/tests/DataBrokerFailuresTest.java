/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.testutils.tests;

import static org.junit.Assert.fail;
import static org.junit.runners.MethodSorters.NAME_ASCENDING;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

import javax.inject.Inject;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.OptimisticLockFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.datastoreutils.testutils.DataBrokerFailures;
import org.opendaylight.genius.datastoreutils.testutils.DataBrokerFailuresModule;
import org.opendaylight.infrautils.inject.guice.testutils.AnnotationsModule;
import org.opendaylight.infrautils.inject.guice.testutils.GuiceRule;

/**
 * Unit test for DataBrokerFailuresImpl.
 *
 * @author Michael Vorburger.ch
 */
@FixMethodOrder(NAME_ASCENDING)
public class DataBrokerFailuresTest {

    private final DataBroker mockDataBroker = Mockito.mock(DataBroker.class, RETURNS_DEEP_STUBS);

    public @Rule MethodRule guice = new GuiceRule(
            new DataBrokerFailuresModule(mockDataBroker), new AnnotationsModule());

    @Inject DataBrokerFailures dbFailures;
    @Inject DataBroker dataBroker;

    @Test
    public void testFailReadWriteTransactionSubmit() throws TransactionCommitFailedException {
        dbFailures.failSubmits(new OptimisticLockFailedException("bada boum bam!"));
        checkSubmitFails();
        // Now make sure that it still fails, and not just once:
        checkSubmitFails();
        // and still:
        checkSubmitFails();
    }

    private void checkSubmitFails() {
        try {
            dataBroker.newReadWriteTransaction().submit().checkedGet();
            fail("This should have lead to a TransactionCommitFailedException!");
        } catch (TransactionCommitFailedException e) {
            // as expected!
        }
    }

    @Test
    public void testFailReadWriteTransactionSubmitNext() throws TransactionCommitFailedException {
        // This must pass (the failSubmits from previous test cannot affect this)
        // (It's a completely new instance of DataBroker & DataBrokerFailures anyways, but just to be to sure.)
        dataBroker.newReadWriteTransaction().submit().checkedGet();
    }

    @Test
    public void testFailTwoReadWriteTransactionSubmit() throws TransactionCommitFailedException {
        dbFailures.failSubmits(2, new OptimisticLockFailedException("bada boum bam!"));
        checkSubmitFails();
        // Now make sure that it still fails again a 2nd time, and not just once:
        checkSubmitFails();
        // But now it should pass.. because we specified howManyTimes = 2 above
        dataBroker.newReadWriteTransaction().submit().checkedGet();
        dataBroker.newWriteOnlyTransaction().submit().checkedGet();
        dataBroker.newReadWriteTransaction().submit().checkedGet();
    }

    @Test(expected = OptimisticLockFailedException.class)
    public void testFailWriteTransactionSubmit() throws TransactionCommitFailedException {
        dbFailures.failSubmits(new OptimisticLockFailedException("bada boum bam!"));
        dataBroker.newWriteOnlyTransaction().submit().checkedGet();
    }

    @Test
    public void testUnfailSubmits() throws TransactionCommitFailedException {
        dbFailures.failSubmits(new OptimisticLockFailedException("bada boum bam!"));
        checkSubmitFails();
        dbFailures.unfailSubmits();
        dataBroker.newReadWriteTransaction().submit().checkedGet();
        dataBroker.newWriteOnlyTransaction().submit().checkedGet();
        dataBroker.newReadWriteTransaction().submit().checkedGet();
    }

    @Test
    public void testFailButSubmitsAnywaysReadWriteTransaction() {
        dbFailures.failButSubmitsAnyways();
        checkSubmitFails();
    }

    // TODO make this work for TransactionChain as well ...

}
