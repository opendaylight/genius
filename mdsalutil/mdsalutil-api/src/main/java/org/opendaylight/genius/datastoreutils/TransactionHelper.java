/*
 * Copyright © 2017 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

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
    public static List<ListenableFuture<Void>> callWithWriteOnlyTransaction(DataBroker broker,
            Function<WriteTransaction, List<ListenableFuture<Void>>> function) {
        WriteTransactionWrapper tx = new WriteTransactionWrapper(broker.newWriteOnlyTransaction());
        try {
            return function.apply(tx);
        } finally {
            if (!tx.isSubmitted()) {
                tx.cancel();
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

        private boolean isSubmitted() {
            return submitted;
        }
    }
}
