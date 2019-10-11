/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.testutils;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ForwardingDataBroker;
import org.opendaylight.controller.md.sal.binding.api.ForwardingReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.ForwardingWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DataBroker with methods to simulate failures, useful for tests.
 *
 * <p>If you use this from a Guice-based component test, consider just binding it
 * via the {@link DataBrokerFailuresModule}.
 *
 * @author Michael Vorburger.ch
 */
public class DataBrokerFailuresImpl extends ForwardingDataBroker implements DataBrokerFailures {

    private static final Logger LOG = LoggerFactory.getLogger(DataBrokerFailuresImpl.class);

    private final DataBroker delegate;
    private volatile @Nullable ReadFailedException readException;
    private volatile @Nullable TransactionCommitFailedException submitException;
    private final AtomicInteger howManyFailingReads = new AtomicInteger();
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
    public void failReads(ReadFailedException exception) {
        unfailReads();
        readException = Objects.requireNonNull(exception, "exception == null");
    }

    @Override
    public void failReads(int howManyTimes, ReadFailedException exception) {
        unfailReads();
        howManyFailingReads.set(howManyTimes);
        readException = Objects.requireNonNull(exception, "exception == null");
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
    public void unfailReads() {
        readException = null;
        howManyFailingReads.set(-1);
    }

    @Override
    public void unfailSubmits() {
        this.submitException = null;
        howManyFailingSubmits.set(-1);
        this.submitAndThrowException = false;
    }

    @Override
    public void failButSubmitsAnyways() {
        unfailSubmits();
        this.submitException = new TransactionCommitFailedException("caused by simulated AskTimeoutException");
        this.submitAndThrowException = true;
    }

    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "https://github.com/spotbugs/spotbugs/issues/811")
    private FluentFuture<? extends CommitInfo> handleCommit(Supplier<FluentFuture<? extends CommitInfo>> commitMethod) {
        if (howManyFailingSubmits.decrementAndGet() == -1) {
            submitException = null;
        }
        if (submitException == null) {
            return commitMethod.get();
        } else {
            if (submitAndThrowException) {
                try {
                    commitMethod.get().get();
                } catch (InterruptedException | ExecutionException e) {
                    LOG.warn("Exception while waiting for submitted transaction", e);
                }
            }
            return FluentFuture.from(Futures.immediateFailedFuture(submitException));
        }
    }

    public <T extends DataObject> CheckedFuture<Optional<T>, ReadFailedException> handleRead(
            BiFunction<LogicalDatastoreType, InstanceIdentifier<T>, CheckedFuture<Optional<T>, ReadFailedException>>
                readMethod,
            LogicalDatastoreType store, InstanceIdentifier<T> path) {
        if (howManyFailingReads.decrementAndGet() == -1) {
            readException = null;
        }
        if (readException == null) {
            return readMethod.apply(store, path);
        } else {
            return Futures.immediateFailedCheckedFuture(readException);
        }
    }

    @Override
    public ReadWriteTransaction newReadWriteTransaction() {
        return new ForwardingReadWriteTransaction(delegate.newReadWriteTransaction()) {
            @Override
            public <T extends DataObject> CheckedFuture<Optional<T>, ReadFailedException> read(
                    LogicalDatastoreType store, InstanceIdentifier<T> path) {
                return handleRead(super::read, store, path);
            }

            @Override
            public FluentFuture<? extends CommitInfo> commit() {
                return handleCommit(super::commit);
            }
        };
    }

    @Override
    public WriteTransaction newWriteOnlyTransaction() {
        return new ForwardingWriteTransaction(delegate.newWriteOnlyTransaction()) {
            @Override
            public FluentFuture<? extends CommitInfo> commit() {
                return handleCommit(super::commit);
            }
        };
    }

}
