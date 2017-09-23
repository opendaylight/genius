/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra.tests;

import static com.google.common.util.concurrent.Futures.immediateFuture;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.infra.CheckedConsumer;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.RetryingManagedNewTransactionRunner;
import org.opendaylight.infrautils.testutils.LogCaptureRule;
import org.opendaylight.infrautils.testutils.LogRule;

/**
 * Unit test for {@link RetryingManagedNewTransactionRunner}.
 *
 * @author Michael Vorburger.ch
 */
public class RetryingManagedNewTransactionRunnerTest {

    // private static final Logger LOG = LoggerFactory.getLogger(RetryingManagedNewTransactionRunnerTest.class);

    // private final DataBroker mockDataBroker = Mockito.mock(DataBroker.class);
    private final ManagedNewTransactionRunner wrappedMgdNewTxRunner = mock(ManagedNewTransactionRunner.class);
    // private @Inject DataBrokerFailures dbFailures;
    // private @Inject DataBroker dataBroker;

    // public @Rule MethodRule guice = new GuiceRule(
    //        new DataBrokerFailuresModule(mockDataBroker), new AnnotationsModule());

    public @Rule LogRule logRule = new LogRule();
    public @Rule LogCaptureRule logCaptureRule = new LogCaptureRule();

    private RetryingManagedNewTransactionRunner retryingManagedNewTransactionRunner;

    @Before
    public void beforeTest() {
        retryingManagedNewTransactionRunner = new RetryingManagedNewTransactionRunner(wrappedMgdNewTxRunner);
    }

    @Test
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED") // Mockito & @CheckReturnValue don't go well together
    public void testCallWithNewWriteOnlyTransactionAndSubmitEmptySuccessfully() throws Exception {
        @SuppressWarnings("unchecked")
        CheckedConsumer<WriteTransaction> writeTxLambda = mock(CheckedConsumer.class);
        when(wrappedMgdNewTxRunner.callWithNewWriteOnlyTransactionAndSubmit(any())).thenReturn(immediateFuture(null));
        retryingManagedNewTransactionRunner.callWithNewWriteOnlyTransactionAndSubmit(writeTxLambda).get();
        verify(wrappedMgdNewTxRunner).callWithNewWriteOnlyTransactionAndSubmit(writeTxLambda);
    }

    // TODO same as all of above for callWithNewReadWriteTransactionAndSubmit

}
