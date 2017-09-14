/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.testutils.tests;

import static org.junit.runners.MethodSorters.NAME_ASCENDING;

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

    private final DataBroker mockDataBroker = Mockito.mock(DataBroker.class);

    public @Rule MethodRule guice = new GuiceRule(
            new DataBrokerFailuresModule(mockDataBroker), new AnnotationsModule());

    @Inject DataBrokerFailures dbFailures;
    @Inject DataBroker dataBroker;

    @Test(expected = OptimisticLockFailedException.class)
    public void testFailReadWriteTransactionSubmit() throws TransactionCommitFailedException {
        dbFailures.failSubmits(new OptimisticLockFailedException("bada boum bam!"));
        dataBroker.newReadWriteTransaction().submit().checkedGet();
    }

    @Test
    public void testFailReadWriteTransactionSubmitNext() {
        // This must pass (the failSubmits from previous test cannot affect this)
    }

    @Test(expected = OptimisticLockFailedException.class)
    public void testFailWriteTransactionSubmit() throws TransactionCommitFailedException {
        dbFailures.failSubmits(new OptimisticLockFailedException("bada boum bam!"));
        dataBroker.newWriteOnlyTransaction().submit().checkedGet();
    }

    public void testUnfailSubmits() throws TransactionCommitFailedException {
        dbFailures.failSubmits(new OptimisticLockFailedException("bada boum bam!"));
        dbFailures.unfailSubmits();
        dataBroker.newWriteOnlyTransaction().submit().checkedGet();
        dataBroker.newReadWriteTransaction().submit().checkedGet();
    }

    // TODO make this work for TransactionChain as well ...

}
