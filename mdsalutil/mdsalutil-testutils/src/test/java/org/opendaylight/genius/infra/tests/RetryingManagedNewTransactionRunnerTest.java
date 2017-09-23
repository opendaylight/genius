/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra.tests;

import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.RetryingManagedNewTransactionRunner;

/**
 * Test for {@link RetryingManagedNewTransactionRunner}.
 * Note that this test (intentionally) extends the {@link ManagedNewTransactionRunnerImplTest}.
 *
 * @author Michael Vorburger.ch
 */
public class RetryingManagedNewTransactionRunnerTest extends ManagedNewTransactionRunnerImplTest {

    @Override
    protected ManagedNewTransactionRunner createManagedNewTransactionRunnerToTest(DataBroker dataBroker) {
        return new RetryingManagedNewTransactionRunner(super.createManagedNewTransactionRunnerToTest(dataBroker));
    }

    @Test
    public void testCallWithNewWriteOnlyTransactionAndSubmitReturnFailedFuture() {
        // TODO Implement...
    }

    @Test
    public void testCallWithNewWriteOnlyTransactionAndSubmitLambdaThrowsException() {
        // TODO Implement...
    }

    // TODO same as all of above for callWithNewReadWriteTransactionAndSubmit

}
