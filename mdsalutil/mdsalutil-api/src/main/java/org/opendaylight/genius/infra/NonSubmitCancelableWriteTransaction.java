/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra;

import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.binding.api.ForwardingWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;

/**
 * WriteTransaction which cannot be {@link WriteTransaction#cancel()},
 * {@link WriteTransaction#commit()} or {@link WriteTransaction#submit()}.
 *
 * @author Michael Vorburger.ch
 */
@SuppressWarnings("deprecation")
// intentionally package local, for now
class NonSubmitCancelableWriteTransaction extends ForwardingWriteTransaction {

    // TODO if we could finally reach consensus on https://git.opendaylight.org/gerrit/#/c/46684/, then this could probably be removed?
    // see also https://git.opendaylight.org/gerrit/#/c/46335/ for the earlier take on it - controller, mdsal, controller, mdsal... ;-)

    NonSubmitCancelableWriteTransaction(WriteTransaction delegate) {
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
