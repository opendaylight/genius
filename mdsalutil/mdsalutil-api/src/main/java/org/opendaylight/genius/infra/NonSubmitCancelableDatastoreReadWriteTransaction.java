/*
 * Copyright © 2018 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

class NonSubmitCancelableDatastoreReadWriteTransaction<D extends Datastore> extends DatastoreTransaction<D>
        implements DatastoreReadWriteTransaction<D> {
    final ReadWriteTransaction delegate;

    NonSubmitCancelableDatastoreReadWriteTransaction(ReadWriteTransaction realTx) {
        this.delegate = realTx;
    }

    @Override
    public <T extends DataObject> CheckedFuture<Optional<T>, ReadFailedException> read(InstanceIdentifier<T> path) {
        return delegate.read(getDatastoreType(), path);
    }

    @Override
    public <T extends DataObject> void put(InstanceIdentifier<T> path, T data) {
        delegate.put(getDatastoreType(), path, data);
    }

    @Override
    public <T extends DataObject> void put(InstanceIdentifier<T> path, T data, boolean createMissingParents) {
        delegate.put(getDatastoreType(), path, data, createMissingParents);
    }

    @Override
    public <T extends DataObject> void merge(InstanceIdentifier<T> path, T data) {
        delegate.merge(getDatastoreType(), path, data);
    }

    @Override
    public <T extends DataObject> void merge(InstanceIdentifier<T> path, T data, boolean createMissingParents) {
        delegate.merge(getDatastoreType(), path, data, createMissingParents);
    }

    @Override
    public void delete(InstanceIdentifier<?> path) {
        delegate.delete(getDatastoreType(), path);
    }

    @Override
    @Nonnull
    public Object getIdentifier() {
        return delegate.getIdentifier();
    }
}
