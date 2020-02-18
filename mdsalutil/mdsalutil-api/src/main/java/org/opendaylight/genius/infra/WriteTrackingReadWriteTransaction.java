/*
 * Copyright © 2018 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra;

import java.util.Optional;

import com.google.common.util.concurrent.FluentFuture;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.ReadFailedException;
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
    public <T extends DataObject> FluentFuture<Optional<T>> read(LogicalDatastoreType store,
            InstanceIdentifier<T> path) {
        return ((ReadWriteTransaction) delegate()).read(store, path);
    }

    @Override
    public FluentFuture<Boolean> exists(LogicalDatastoreType store, InstanceIdentifier<?> path) {
        return ((ReadWriteTransaction) delegate()).exists(store, path);
    }
}
