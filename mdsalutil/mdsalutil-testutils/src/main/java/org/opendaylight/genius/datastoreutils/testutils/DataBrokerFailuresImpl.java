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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ForwardingDataBroker;
import org.opendaylight.controller.md.sal.binding.api.ForwardingReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.ForwardingWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yangtools.yang.common.RpcResult;

/**
 * DataBroker with methods to simulate failures, useful for tests.
 *
 * <p>If you use this from a Guice-based component test, consider just binding it
 * via the {@link DataBrokerFailuresModule}.
 *
 * @author Michael Vorburger.ch
 */
@SuppressWarnings("deprecation")
public class DataBrokerFailuresImpl extends ForwardingDataBroker implements DataBrokerFailures {

    private final DataBroker delegate;
    private volatile @Nullable TransactionCommitFailedException submitException;
    private final AtomicInteger howManyFailingSubmits = new AtomicInteger();
    private boolean submitAndThrowException = false;

    public DataBrokerFailuresImpl(DataBroker delegate) {
        this.delegate = delegate;
    }

    @Override
    protected DataBroker delegate() {
        return delegate;
    }

    @Override
    public void failSubmits(TransactionCommitFailedException exception) {
        unfailSubmits();
        this.submitException = Objects.requireNonNull(exception, "exception == null");
    }

    @Override
    public void failSubmits(int howManyTimes, TransactionCommitFailedException exception) {
        howManyFailingSubmits.set(howManyTimes);
        this.submitException = Objects.requireNonNull(exception, "exception == null");
    }

    @Override
    public void unfailSubmits() {
        this.submitException = null;
        howManyFailingSubmits.set(-1);
    }


    @Override
    public void submitButFails(TransactionCommitFailedException exception) {
        unfailSubmits();
        this.submitException = Objects.requireNonNull(exception, "exception == null");
        this.submitAndThrowException = true;
    }

    private void update() {
        if (howManyFailingSubmits.decrementAndGet() == -1) {
            this.submitException = null;
        }
    }

    @Override
    public ReadWriteTransaction newReadWriteTransaction() {
        return new ForwardingReadWriteTransaction(delegate.newReadWriteTransaction()) {
            @Override
            public CheckedFuture<Void, TransactionCommitFailedException> submit() {
                update();
                if (submitException == null) {
                    return super.submit();
                } else {
                    if (submitAndThrowException) {
                        try {
                            super.submit().get();
                        } catch (InterruptedException | ExecutionException e) {
                            return Futures.immediateFailedCheckedFuture(submitException);
                        }
                    }
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
        return new ForwardingWriteTransaction(delegate.newWriteOnlyTransaction()) {
            @Override
            public CheckedFuture<Void, TransactionCommitFailedException> submit() {
                update();
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
