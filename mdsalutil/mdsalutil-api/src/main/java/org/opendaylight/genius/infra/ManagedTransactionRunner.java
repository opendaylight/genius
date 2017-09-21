/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra;

import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;

/**
 * Managed transactions utility to simplify running code with existing elsewhere managed transactions.
 *
 * <p>This would typically be used in code in "inner" code which "just needs to have a transaction
 * from somewhere".  The submission of the transaction is, intentionally, not exposed here (because
 * it's a "managed" transaction), and must be dealt with elsewhere.  Specifically, "top level" code
 * most likely should be using {@link ManagedNewTransactionRunner} instead of this.
 *
 * @see ManagedNewTransactionRunner
 */
public interface ManagedTransactionRunner {

    /**
     * Invokes a consumer with a <b>EXISTING</b> {@link WriteTransaction}.
     * Where this Tx is obtained from is implementation specific.  One possible implementation
     * is one using {@link ThreadLocal} magic to find such a transaction.
     *
     * <p>The passed in WriteTransaction should not (cannot) be
     * {@link WriteTransaction#cancel()}, {@link WriteTransaction#commit()} or
     * {@link WriteTransaction#submit()}  (it will throw an {@link UnsupportedOperationException}).
     *
     * @see ManagedNewTransactionRunner#callWithNewWriteOnlyTransactionAndSubmit(CheckedConsumer)
     *
     * @param txRunner the {@link CheckedConsumer} that needs an existing write only transaction
     *
     * @throws Exception rethrown from txRunner, automatically marking the transaction as cancelled
     */
    void callWithWriteOnlyTransaction(CheckedConsumer<WriteTransaction> txRunner) throws Exception;

    // TODO ReadWriteTransaction & ReadOnlyTransaction as in ManagedNewTransactionRunner
    // The impl likely can make some optimizations?  If you're running under a thread local ReadWriteTransaction,
    // then e.g. using callWithNewWriteOnlyTransactionAndSubmit can just re-use that ReadWriteTransaction, not fail.

}
