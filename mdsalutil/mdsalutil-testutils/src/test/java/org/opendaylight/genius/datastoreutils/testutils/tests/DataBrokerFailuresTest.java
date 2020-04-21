/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.testutils.tests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.runners.MethodSorters.NAME_ASCENDING;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mockito;
import org.opendaylight.genius.datastoreutils.testutils.DataBrokerFailures;
import org.opendaylight.genius.datastoreutils.testutils.DataBrokerFailuresModule;
import org.opendaylight.infrautils.inject.guice.testutils.AnnotationsModule;
import org.opendaylight.infrautils.inject.guice.testutils.GuiceRule;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.OptimisticLockFailedException;
import org.opendaylight.mdsal.common.api.TransactionCommitFailedException;

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

    @Before
    public void setup() {

    }

    @Test
    public void testFailReadWriteTransactionSubmit() throws TimeoutException, InterruptedException {
        dbFailures.failSubmits(new OptimisticLockFailedException("bada boum bam!"));
        checkSubmitFails();
        // Now make sure that it still fails, and not just once:
        checkSubmitFails();
        // and still:
        checkSubmitFails();
    }

    private void checkSubmitFails() throws TimeoutException, InterruptedException {
        try {
            dataBroker.newReadWriteTransaction().commit().get(5, TimeUnit.SECONDS);
            fail("This should have lead to a TransactionCommitFailedException!");
        } catch (ExecutionException e) {
            assertTrue("Expected TransactionCommitFailedException",
                    e.getCause() instanceof TransactionCommitFailedException);
        }
    }

    @Test
    public void testFailReadWriteTransactionSubmitNext()
            throws TimeoutException, InterruptedException, ExecutionException {
        // This must pass (the failSubmits from previous test cannot affect this)
        // (It's a completely new instance of DataBroker & DataBrokerFailures anyways, but just to be to sure.)
        dataBroker.newReadWriteTransaction().commit().get(5, TimeUnit.SECONDS);
    }

    @Test
    public void testFailTwoReadWriteTransactionSubmit()
            throws TimeoutException, InterruptedException, ExecutionException {
        dbFailures.failSubmits(2, new OptimisticLockFailedException("bada boum bam!"));
        checkSubmitFails();
        // Now make sure that it still fails again a 2nd time, and not just once:
        checkSubmitFails();
        // But now it should pass.. because we specified howManyTimes = 2 above
        dataBroker.newReadWriteTransaction().commit().get(5, TimeUnit.SECONDS);
        dataBroker.newWriteOnlyTransaction().commit().get(5, TimeUnit.SECONDS);
        dataBroker.newReadWriteTransaction().commit().get(5, TimeUnit.SECONDS);
    }

    @Test(expected = OptimisticLockFailedException.class)
    @SuppressWarnings("checkstyle:AvoidHidingCauseException")
    public void testFailWriteTransactionSubmit()
            throws TimeoutException, InterruptedException, TransactionCommitFailedException {
        dbFailures.failSubmits(new OptimisticLockFailedException("bada boum bam!"));
        try {
            dataBroker.newWriteOnlyTransaction().commit().get(5, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            assertTrue("Expected TransactionCommitFailedException",
                    e.getCause() instanceof TransactionCommitFailedException);
            throw (TransactionCommitFailedException)e.getCause();
        }
    }

    @Test
    public void testUnfailSubmits() throws TimeoutException, InterruptedException, ExecutionException {
        dbFailures.failSubmits(new OptimisticLockFailedException("bada boum bam!"));
        checkSubmitFails();
        dbFailures.unfailSubmits();
        dataBroker.newReadWriteTransaction().commit().get(5, TimeUnit.SECONDS);
        dataBroker.newWriteOnlyTransaction().commit().get(5, TimeUnit.SECONDS);
        dataBroker.newReadWriteTransaction().commit().get(5, TimeUnit.SECONDS);
    }

    @Test
    public void testFailButSubmitsAnywaysReadWriteTransaction() throws TimeoutException, InterruptedException {
        dbFailures.failButSubmitsAnyways();
        checkSubmitFails();
    }

    // TODO make this work for TransactionChain as well ...

}
