/*
 * Copyright © 2018 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra;

import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

class NonSubmitCancelableDatastoreWriteTransaction<D extends Datastore> extends DatastoreTransaction<D>
        implements DatastoreWriteTransaction<D> {
    // Temporarily package protected for TransactionAdapter
    final WriteTransaction delegate;

    NonSubmitCancelableDatastoreWriteTransaction(WriteTransaction realTx) {
        this.delegate = realTx;
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
