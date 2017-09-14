/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.testutils;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Objects;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ForwardingDataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yangtools.yang.common.RpcResult;

/**
 * DataBroker with methods to simulate failures, useful for tests.
 *
 * @author Michael Vorburger.ch
 */
@SuppressWarnings("deprecation")
// Intentionally only package local, as only ever supposed to be used via DataBrokerFailuresModule
class DataBrokerFailuresImpl extends ForwardingDataBroker implements DataBrokerFailures {

    private final DataBroker delegate;
    private @Nullable TransactionCommitFailedException submitException;

    DataBrokerFailuresImpl(DataBroker delegate) {
        this.delegate = delegate;
    }

    @Override
    protected DataBroker delegate() {
        return delegate;
    }

    @Override
    public void failSubmits(@Nullable TransactionCommitFailedException exception) {
        this.submitException = Objects.requireNonNull(exception, "exception == null");
    }

    @Override
    public void unfailSubmits() {
        this.submitException = null;
    }

    @Override
    public ReadWriteTransaction newReadWriteTransaction() {
        return new DelegatingReadWriteTransaction(delegate.newReadWriteTransaction()) {
            @Override
            public CheckedFuture<Void, TransactionCommitFailedException> submit() {
                if (submitException == null) {
                    return super.submit();
                } else {
                    return Futures.immediateFailedCheckedFuture(submitException);
                }
            }

            @Override
            public ListenableFuture<RpcResult<TransactionStatus>> commit() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public WriteTransaction newWriteOnlyTransaction() {
        return new DelegatingWriteTransaction(delegate.newWriteOnlyTransaction()) {
            @Override
            public CheckedFuture<Void, TransactionCommitFailedException> submit() {
                if (submitException == null) {
                    return super.submit();
                } else {
                    return Futures.immediateFailedCheckedFuture(submitException);
                }
            }

            @Override
            public ListenableFuture<RpcResult<TransactionStatus>> commit() {
                throw new UnsupportedOperationException();
            }
        };
    }

}
