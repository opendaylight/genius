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
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Read-write transaction which keeps track of writes.
 */
class WriteTrackingReadWriteTransaction extends WriteTrackingWriteTransaction implements ReadWriteTransaction {

    WriteTrackingReadWriteTransaction(ReadWriteTransaction delegate) {
        super(delegate);
    }

    @Override
    public <T extends DataObject> CheckedFuture<Optional<T>, ReadFailedException> read(LogicalDatastoreType store,
            InstanceIdentifier<T> path) {
        return ((ReadWriteTransaction) delegate()).read(store, path);
    }

    @Override
    public CheckedFuture<Boolean, ReadFailedException> exists(LogicalDatastoreType store,
            InstanceIdentifier<?> path) {
        return ((ReadWriteTransaction) delegate()).exists(store, path);
    }
}
