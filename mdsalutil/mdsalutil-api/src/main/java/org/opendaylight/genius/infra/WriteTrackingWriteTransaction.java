/*
 * Copyright © 2018 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra;

import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.binding.spi.ForwardingWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Write transaction which keeps track of writes.
 */
class WriteTrackingWriteTransaction extends ForwardingWriteTransaction implements WriteTrackingTransaction {
    // This is volatile to ensure we get the latest value; transactions aren't supposed to be used in multiple threads,
    // but the cost here is tiny (one read penalty at the end of a transaction) so we play it safe
    private volatile boolean written;

    WriteTrackingWriteTransaction(WriteTransaction delegate) {
        super(delegate);
    }

    @Override
    public <T extends DataObject> void put(LogicalDatastoreType store, InstanceIdentifier<T> path, T data) {
        super.put(store, path, data);
        written = true;
    }

    @Override
    public <T extends DataObject> void mergeParentStructurePut(LogicalDatastoreType store, InstanceIdentifier<T> path,
                                                               T data) {
        super.mergeParentStructurePut(store, path, data);
        written = true;
    }

    @Override
    public <T extends DataObject> void merge(LogicalDatastoreType store, InstanceIdentifier<T> path, T data) {
        super.merge(store, path, data);
        written = true;
    }

    @Override
    public <T extends DataObject> void mergeParentStructureMerge(LogicalDatastoreType store, InstanceIdentifier<T> path,
                                                                 T data) {
        super.mergeParentStructureMerge(store, path, data);
        written = true;
    }

    @Override
    public void delete(LogicalDatastoreType store, InstanceIdentifier<?> path) {
        super.delete(store, path);
        written = true;
    }

    @Override
    public boolean isWritten() {
        return written;
    }
}
