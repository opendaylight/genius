/*
 * Copyright © 2017 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra;

import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.binding.api.ForwardingReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;

/**
 * ReadWriteTransaction which cannot be {@link ReadWriteTransaction#cancel()},
 * {@link ReadWriteTransaction#commit()} or {@link ReadWriteTransaction#submit()}.
 */
@SuppressWarnings("deprecation")
// intentionally package local, for now
class NonSubmitCancelableReadWriteTransaction extends ForwardingReadWriteTransaction {

    NonSubmitCancelableReadWriteTransaction(ReadWriteTransaction delegate) {
        super(delegate);
    }

    @Override
    public boolean cancel() {
        throw new UnsupportedOperationException("cancel() cannot be used inside a Managed[New]TransactionRunner");
    }

    @Override
    public CheckedFuture<Void, TransactionCommitFailedException> submit() {
        throw new UnsupportedOperationException("submit() cannot be used inside a Managed[New]TransactionRunner");
    }
}
