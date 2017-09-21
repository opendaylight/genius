/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra;

import static org.opendaylight.genius.infra.ManagedNewTransactionRunnerWithThreadLocal.getThreadLocalWriteTransaction;

import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;

/**
 * Implementation of {@link ManagedTransactionRunner} suitable for use (only)
 * with {@link ManagedNewTransactionRunnerWithThreadLocal}.
 *
 * @author Michael Vorburger.ch
 */
public class ThreadLocalTransactionRunner implements ManagedTransactionRunner {

    @Override
    public void callWithWriteOnlyTransaction(CheckedConsumer<WriteTransaction> txRunner) throws Exception {
        txRunner.accept(getThreadLocalWriteTransaction().orElseThrow(() -> new IllegalStateException(
                "Must use " + ManagedNewTransactionRunnerWithThreadLocal.class.getName())));
    }

}
