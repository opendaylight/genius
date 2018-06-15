/*
 * Copyright © 2018 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FluentFuture;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

class NonSubmitCancelableDatastoreWriteTransaction<D extends Datastore> extends DatastoreTransaction<D>
        implements DatastoreWriteTransaction<D> {
    private final WriteTransaction delegate;

    NonSubmitCancelableDatastoreWriteTransaction(WriteTransaction realTx) {
        this.delegate = realTx;
    }

    @Override
    public <T extends DataObject> void put(LogicalDatastoreType store, InstanceIdentifier<T> path, T data) {
        Preconditions.checkArgument(getDatastoreType().equals(store), "Invalid datastore %s used instead of %s", store,
                getDatastoreType());
        put(path, data);
    }

    @Override
    public <T extends DataObject> void put(InstanceIdentifier<T> path, T data) {
        delegate.put(getDatastoreType(), path, data);
    }

    @Override
    public <T extends DataObject> void put(LogicalDatastoreType store, InstanceIdentifier<T> path, T data,
            boolean createMissingParents) {
        Preconditions.checkArgument(getDatastoreType().equals(store), "Invalid datastore %s used instead of %s", store,
                getDatastoreType());
        put(path, data, createMissingParents);
    }

    @Override
    public <T extends DataObject> void put(InstanceIdentifier<T> path, T data, boolean createMissingParents) {
        delegate.put(getDatastoreType(), path, data, createMissingParents);
    }

    @Override
    public <T extends DataObject> void merge(LogicalDatastoreType store, InstanceIdentifier<T> path, T data) {
        Preconditions.checkArgument(getDatastoreType().equals(store), "Invalid datastore %s used instead of %s", store,
                getDatastoreType());
        merge(path, data);
    }

    @Override
    public <T extends DataObject> void merge(InstanceIdentifier<T> path, T data) {
        delegate.merge(getDatastoreType(), path, data);
    }

    @Override
    public <T extends DataObject> void merge(LogicalDatastoreType store, InstanceIdentifier<T> path, T data,
            boolean createMissingParents) {
        Preconditions.checkArgument(getDatastoreType().equals(store), "Invalid datastore %s used instead of %s", store,
                getDatastoreType());
        merge(path, data, createMissingParents);
    }

    @Override
    public <T extends DataObject> void merge(InstanceIdentifier<T> path, T data, boolean createMissingParents) {
        delegate.merge(getDatastoreType(), path, data, createMissingParents);
    }

    @Override
    public boolean cancel() {
        throw new UnsupportedOperationException("cancel() cannot be used inside a Managed[New]TransactionRunner");
    }

    @Override
    public void delete(LogicalDatastoreType store, InstanceIdentifier<?> path) {
        Preconditions.checkArgument(getDatastoreType().equals(store), "Invalid datastore %s used instead of %s", store,
                getDatastoreType());
        delete(path);
    }

    @Override
    public void delete(InstanceIdentifier<?> path) {
        delegate.delete(getDatastoreType(), path);
    }

    @Override
    public FluentFuture<? extends CommitInfo> commit() {
        throw new UnsupportedOperationException("commit() cannot be used inside a Managed[New]TransactionRunner");
    }

    @Override
    @Nonnull
    public Object getIdentifier() {
        return delegate.getIdentifier();
    }
}
