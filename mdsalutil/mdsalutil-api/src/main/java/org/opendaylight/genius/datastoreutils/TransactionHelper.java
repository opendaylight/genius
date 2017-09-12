/*
 * Copyright © 2017 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

/**
 * Transaction helper to simplify transaction handling and ensure transactions are closed when no longer necessary.
 */
public final class TransactionHelper {

    private TransactionHelper() {
    }

    /**
     * Calls the given function with a single, write-only transaction, and ensures the transaction is closed
     * appropriately before returning: if the provided function doesn't submit the transaction, this method will
     * cancel it. Thus when this method returns, the transaction is guaranteed to have been submitted or cancelled,
     * and won't leak.
     *
     * @param broker The broker.
     * @param function The function which needs a transaction.
     * @return The futures returned by the function.
     */
    @Nonnull
    public static List<ListenableFuture<Void>> applyWriteOnlyTransaction(@Nonnull DataBroker broker,
            @Nonnull Function<WriteTransaction, List<ListenableFuture<Void>>> function) {
        WriteTransactionWrapper wrapper = new WriteTransactionWrapper(broker.newWriteOnlyTransaction());
        try {
            return function.apply(wrapper);
        } finally {
            if (!wrapper.isSubmitted()) {
                wrapper.cancel();
            }
        }
    }

    /**
     * Calls the given function with a single, write-only transaction, and ensures the transaction is closed
     * appropriately before returning.
     *
     * @param broker The broker.
     * @param function The function which needs a transaction.
     */
    public static void consumeWriteOnlyTransaction(@Nonnull DataBroker broker,
            @Nonnull Consumer<WriteTransaction> function) {
        WriteTransactionWrapper wrapper = new WriteTransactionWrapper(broker.newWriteOnlyTransaction());
        try {
            function.accept(wrapper);
        } finally {
            if (!wrapper.isSubmitted()) {
                wrapper.cancel();
            }
        }
    }

    /**
     * Calls the given function with a single, write-only transaction, and ensures the transaction is closed
     * appropriately before returning. The transaction might be pre-existing, in which case no new transaction is
     * created. The callee needs to follow the usual transaction semantics, and submit the transaction if necessary
     * (this won't actually submit the transaction, we'll wait for the code which created the transaction to submit
     * it).
     *
     * @param broker The broker.
     * @param existingTransaction The pre-existing transaciton.
     * @param function The function which needs a transaction.
     */
    public static void consumePreExistingWriteTransaction(@Nonnull DataBroker broker,
            @Nullable WriteTransaction existingTransaction, @Nonnull Consumer<WriteTransaction> function) {
        WriteTransactionWrapper wrapper =
                existingTransaction == null ? new WriteTransactionWrapper(broker.newWriteOnlyTransaction())
                        : new WriteTransactionComposer(existingTransaction);
        try {
            function.accept(wrapper);
        } finally {
            if (!wrapper.isSubmitted()) {
                wrapper.cancel();
            }
        }
    }

    private static class WriteTransactionWrapper implements WriteTransaction {
        private final WriteTransaction delegate;
        private boolean submitted = false;

        private WriteTransactionWrapper(WriteTransaction tx) {
            this.delegate = tx;
        }

        @Override
        public <T extends DataObject> void put(LogicalDatastoreType store, InstanceIdentifier<T> path, T data) {
            delegate.put(store, path, data);
        }

        @Override
        public <T extends DataObject> void put(LogicalDatastoreType store, InstanceIdentifier<T> path, T data,
                boolean createMissingParents) {
            delegate.put(store, path, data, createMissingParents);
        }

        @Override
        public <T extends DataObject> void merge(LogicalDatastoreType store, InstanceIdentifier<T> path, T data) {
            delegate.merge(store, path, data);
        }

        @Override
        public <T extends DataObject> void merge(LogicalDatastoreType store, InstanceIdentifier<T> path, T data,
                boolean createMissingParents) {
            delegate.merge(store, path, data, createMissingParents);
        }

        @Override
        public void delete(LogicalDatastoreType store, InstanceIdentifier<?> path) {
            delegate.delete(store, path);
        }

        @Override
        public boolean cancel() {
            return delegate.cancel();
        }

        @Override
        public CheckedFuture<Void, TransactionCommitFailedException> submit() {
            CheckedFuture<Void, TransactionCommitFailedException> result = delegate.submit();
            this.submitted = true;
            return result;
        }

        @Override
        @Deprecated
        public ListenableFuture<RpcResult<TransactionStatus>> commit() {
            ListenableFuture<RpcResult<TransactionStatus>> result = delegate.commit();
            this.submitted = true;
            return result;
        }

        @Override
        @Nonnull
        public Object getIdentifier() {
            return delegate.getIdentifier();
        }

        void setSubmitted(boolean submitted) {
            this.submitted = submitted;
        }

        private boolean isSubmitted() {
            return submitted;
        }
    }

    /**
     * This wraps a transaction which is intended to be submitted in code further out than the caller. Submissions here
     * are ignored but flagged, so that the transaction won't be cancelled.
     */
    private static class WriteTransactionComposer extends WriteTransactionWrapper {
        private WriteTransactionComposer(WriteTransaction tx) {
            super(tx);
        }

        @Override
        public CheckedFuture<Void, TransactionCommitFailedException> submit() {
            // Note that this only makes sense as long as we *don't* wrap other wrappers (we mustn't propagate the
            // status to the wrapped transaction)
            setSubmitted(true);
            return Futures.immediateCheckedFuture(null);
        }

        @Override
        public ListenableFuture<RpcResult<TransactionStatus>> commit() {
            // Note that this only makes sense as long as we *don't* wrap other wrappers (we mustn't propagate the
            // status to the wrapped transaction)
            setSubmitted(true);
            return Futures.immediateFuture(RpcResultBuilder.success(TransactionStatus.SUBMITED).build());
        }
    }
}
