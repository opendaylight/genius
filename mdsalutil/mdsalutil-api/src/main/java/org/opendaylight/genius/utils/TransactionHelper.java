/*
 * Copyright © 2017 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.utils;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.function.Consumer;
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
        // Utility class
    }

    /**
     * Calls the given function with a single, write-only transaction, and ensures the transaction is closed
     * appropriately before returning.
     *
     * @param broker The broker.
     * @param function The function which needs a transaction.
     * @return The futures returned by the function.
     */
    public static List<ListenableFuture<Void>> applyWriteOnlyTransaction(DataBroker broker,
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

    /**
     * Calls the given function with a single, write-only transaction, and ensures the transaction is closed
     * appropriately before returning.
     *
     * @param broker The broker.
     * @param function The function which needs a transaction.
     */
    public static void consumeWriteOnlyTransaction(DataBroker broker, Consumer<WriteTransaction> function) {
        WriteTransactionWrapper tx = new WriteTransactionWrapper(broker.newWriteOnlyTransaction());
        try {
            function.accept(tx);
        } finally {
            if (!tx.isSubmitted()) {
                tx.cancel();
            }
        }
    }

    private static class WriteTransactionWrapper implements WriteTransaction {
        private final WriteTransaction tx;
        private boolean submitted = false;

        private WriteTransactionWrapper(WriteTransaction tx) {
            this.tx = tx;
        }

        @Override
        public <T extends DataObject> void put(LogicalDatastoreType store, InstanceIdentifier<T> path, T data) {
            tx.put(store, path, data);
        }

        @Override
        public <T extends DataObject> void put(LogicalDatastoreType store, InstanceIdentifier<T> path, T data,
                boolean createMissingParents) {
            tx.put(store, path, data, createMissingParents);
        }

        @Override
        public <T extends DataObject> void merge(LogicalDatastoreType store, InstanceIdentifier<T> path, T data) {
            tx.merge(store, path, data);
        }

        @Override
        public <T extends DataObject> void merge(LogicalDatastoreType store, InstanceIdentifier<T> path, T data,
                boolean createMissingParents) {
            tx.merge(store, path, data, createMissingParents);
        }

        @Override
        public void delete(LogicalDatastoreType store, InstanceIdentifier<?> path) {
            tx.delete(store, path);
        }

        @Override
        public boolean cancel() {
            return tx.cancel();
        }

        @Override
        public CheckedFuture<Void, TransactionCommitFailedException> submit() {
            this.submitted = true;
            return tx.submit();
        }

        @Override
        @Deprecated
        public ListenableFuture<RpcResult<TransactionStatus>> commit() {
            this.submitted = true;
            return tx.commit();
        }

        @Override
        @Nonnull
        public Object getIdentifier() {
            return tx.getIdentifier();
        }

        private boolean isSubmitted() {
            return submitted;
        }
    }
}
