/*
 * Copyright © 2018 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@Deprecated
public final class TransactionAdapter {
    private TransactionAdapter() {
    }

    public static ReadWriteTransaction toReadWriteTransaction(
            TypedReadWriteTransaction<? extends Datastore> datastoreTx) {
        if (datastoreTx instanceof TypedReadWriteTransactionImpl) {
            TypedReadWriteTransactionImpl nonSubmitCancelableDatastoreReadWriteTransaction =
                    (TypedReadWriteTransactionImpl) datastoreTx;
            return new ReadWriteTransactionAdapter(nonSubmitCancelableDatastoreReadWriteTransaction.datastoreType,
                    nonSubmitCancelableDatastoreReadWriteTransaction.delegate);
        }
        throw new IllegalArgumentException(
                "Unsupported TypedWriteTransaction implementation " + datastoreTx.getClass());
    }

    public static WriteTransaction toWriteTransaction(TypedWriteTransaction<? extends Datastore> datastoreTx) {
        if (datastoreTx instanceof TypedWriteTransactionImpl) {
            TypedWriteTransactionImpl nonSubmitCancelableDatastoreWriteTransaction =
                    (TypedWriteTransactionImpl) datastoreTx;
            return new WriteTransactionAdapter(nonSubmitCancelableDatastoreWriteTransaction.datastoreType,
                    nonSubmitCancelableDatastoreWriteTransaction.delegate);
        }
        throw new IllegalArgumentException(
                "Unsupported TypedWriteTransaction implementation " + datastoreTx.getClass());
    }

    // We want to subclass this class, even though it has a private constructor
    @SuppressWarnings("FinalClass")
    private static class WriteTransactionAdapter extends NonSubmitCancelableWriteTransaction {
        final LogicalDatastoreType datastoreType;

        private WriteTransactionAdapter(LogicalDatastoreType datastoreType, WriteTransaction delegate) {
            super(delegate);
            this.datastoreType = datastoreType;
        }

        @Override
        public <T extends DataObject> void put(LogicalDatastoreType store, InstanceIdentifier<T> path, T data) {
            Preconditions.checkArgument(datastoreType.equals(store), "Invalid datastore %s used instead of %s", store,
                    datastoreType);
            super.put(store, path, data);
        }

        @Override
        public <T extends DataObject> void put(LogicalDatastoreType store, InstanceIdentifier<T> path, T data,
                boolean createMissingParents) {
            Preconditions.checkArgument(datastoreType.equals(store), "Invalid datastore %s used instead of %s", store,
                    datastoreType);
            super.put(store, path, data, createMissingParents);
        }

        @Override
        public <T extends DataObject> void merge(LogicalDatastoreType store, InstanceIdentifier<T> path, T data) {
            Preconditions.checkArgument(datastoreType.equals(store), "Invalid datastore %s used instead of %s", store,
                    datastoreType);
            super.merge(store, path, data);
        }

        @Override
        public <T extends DataObject> void merge(LogicalDatastoreType store, InstanceIdentifier<T> path, T data,
                boolean createMissingParents) {
            Preconditions.checkArgument(datastoreType.equals(store), "Invalid datastore %s used instead of %s", store,
                    datastoreType);
            super.merge(store, path, data, createMissingParents);
        }

        @Override
        public void delete(LogicalDatastoreType store, InstanceIdentifier<?> path) {
            Preconditions.checkArgument(datastoreType.equals(store), "Invalid datastore %s used instead of %s", store,
                    datastoreType);
            super.delete(store, path);
        }
    }

    private static final class ReadWriteTransactionAdapter extends WriteTransactionAdapter
            implements ReadWriteTransaction {
        private final ReadWriteTransaction delegate;

        private ReadWriteTransactionAdapter(LogicalDatastoreType datastoreType, ReadWriteTransaction delegate) {
            super(datastoreType, delegate);
            this.delegate = delegate;
        }

        @Override
        public <T extends DataObject> CheckedFuture<Optional<T>, ReadFailedException> read(LogicalDatastoreType store,
                InstanceIdentifier<T> path) {
            Preconditions.checkArgument(datastoreType.equals(store), "Invalid datastore %s used instead of %s", store,
                    datastoreType);
            return delegate.read(datastoreType, path);
        }
    }
}
