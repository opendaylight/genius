/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.testutils;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;

/**
 * Configures a DataBroker to simulate failures, useful for tests.
 *
 * @author Michael Vorburger.ch
 */
public interface DataBrokerFailures {

    /**
     * Fails all future Transaction submits.
     *
     * @param exception
     *            an Exception to throw from a {@link WriteTransaction#submit()}
     *            (also {@link ReadWriteTransaction#submit()}) method
     */
    void failSubmits(TransactionCommitFailedException exception);

    /**
     * Fails N future Transaction submits.
     *
     * @param howManyTimes
     *               how many times to throw the passed exception, until it resets
     *
     * @param exception
     *            an Exception to throw from a {@link WriteTransaction#submit()}
     *            (also {@link ReadWriteTransaction#submit()}) method
     */
    void failSubmits(int howManyTimes, TransactionCommitFailedException exception);

    /**
     * To simulate scenarios where even though the transaction throws a
     * TransactionCommitFailedException (caused by
     * akka.pattern.AskTimeoutException) it eventually succeeds. These timeouts
     * are typically seen in scaled cluster environments under load. The new
     * tell-based protocol, which will soon be enabled by default (c/61002),
     * adds internal retries for transactions, making the application not to
     * handle such scenarios.
     */
    void failButSubmitsAnyways();

    /**
     * Resets any earlier {@link #unfailSubmits()}.
     */
    void unfailSubmits();
}
