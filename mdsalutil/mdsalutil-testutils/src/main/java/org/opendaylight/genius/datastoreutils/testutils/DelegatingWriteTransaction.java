/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.testutils;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

// Intentionally package local
class DelegatingWriteTransaction implements WriteTransaction {

    private final WriteTransaction delegate;

    DelegatingWriteTransaction(WriteTransaction delegate) {
        this.delegate = delegate;
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
    public boolean cancel() {
        return delegate.cancel();
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
    public CheckedFuture<Void, TransactionCommitFailedException> submit() {
        return delegate.submit();
    }

    @Override
    public void delete(LogicalDatastoreType store, InstanceIdentifier<?> path) {
        delegate.delete(store, path);
    }

    @Override
    public ListenableFuture<RpcResult<TransactionStatus>> commit() {
        return delegate.commit();
    }

    @Override
    public Object getIdentifier() {
        return delegate.getIdentifier();
    }

}
