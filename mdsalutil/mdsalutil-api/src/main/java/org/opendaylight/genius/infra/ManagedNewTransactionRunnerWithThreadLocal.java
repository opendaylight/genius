/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Optional;
import javax.inject.Inject;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;

/**
 * Implementation of {@link ManagedNewTransactionRunner} which makes it possible to use the
 * {@link ThreadLocalTransactionRunner} implementation of {@link ManagedTransactionRunner}
 * in code being called further down the call stack trace during callWith* methods.
 *
 * <p>NB: You do not have to use this to get "managed transactions" - application code can (and, for better performance,
 * ideally should) very well also simply "pass through" the transaction obtained in the ManagedNewTransactionRunner;
 * this is just a convenience to work with existing legacy code where introducing such passing through refactoring
 * may initially be too invasive.
 *
 * @author Michael Vorburger.ch
 */
// Do *NOT* mark this as @Singleton, even though it technically is, as long as this in API, because of https://wiki.opendaylight.org/view/BestPractices/DI_Guidelines#Nota_Bene
public class ManagedNewTransactionRunnerWithThreadLocal extends ManagedNewTransactionRunnerImpl {

    private static final ThreadLocal<WriteTransaction> THREAD_LOCAL_WRITE_TX = new ThreadLocal<>();

    // intentionally package-local only; MUST stay like this, to ensure thread local does not get externally corrupted
    static Optional<WriteTransaction> getThreadLocalWriteTransaction() {
        return Optional.ofNullable(THREAD_LOCAL_WRITE_TX.get());
    }

    @Inject
    public ManagedNewTransactionRunnerWithThreadLocal(DataBroker broker) {
        super(broker);
    }

    @Override
    public ListenableFuture<Void> callWithNewWriteOnlyTransactionAndSubmit(CheckedConsumer<WriteTransaction> txCnsmr) {
        return super.callWithNewWriteOnlyTransactionAndSubmit(input -> {
            try {
                if (THREAD_LOCAL_WRITE_TX.get() != null) {
                    throw new IllegalStateException("Cannot nest (instead use the non-ThreadLocal implementation, "
                            + "and pass through WriteTransaction)");
                }
                THREAD_LOCAL_WRITE_TX.set(input);
                txCnsmr.accept(input);
            } finally {
                THREAD_LOCAL_WRITE_TX.remove();
            }
        });
    }

}
