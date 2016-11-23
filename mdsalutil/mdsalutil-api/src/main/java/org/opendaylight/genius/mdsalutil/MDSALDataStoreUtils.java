/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class MDSALDataStoreUtils {

    /**
     * Deprecated read.
     * @deprecated Use {@link SingleTransactionDataBroker#syncReadOptional(
     * DataBroker, LogicalDatastoreType, InstanceIdentifier)}
     */
    @Deprecated
    public static <T extends DataObject> Optional<T> read(final DataBroker broker,
            final LogicalDatastoreType datastoreType, InstanceIdentifier<T> path) {
        try {
            return SingleTransactionDataBroker.syncReadOptional(broker, datastoreType, path);
        } catch (ReadFailedException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T extends DataObject> void asyncWrite(final DataBroker broker,
           final LogicalDatastoreType datastoreType, InstanceIdentifier<T> path, T data,
           FutureCallback<Void> callback) {
        WriteTransaction tx = broker.newWriteOnlyTransaction();
        tx.put(datastoreType, path, data, true);
        Futures.addCallback(tx.submit(), callback);
    }

    public static <T extends DataObject> void asyncUpdate(final DataBroker broker,
            final LogicalDatastoreType datastoreType, InstanceIdentifier<T> path, T data,
            FutureCallback<Void> callback) {
        WriteTransaction tx = broker.newWriteOnlyTransaction();
        tx.merge(datastoreType, path, data, true);
        Futures.addCallback(tx.submit(), callback);
    }

    public static <T extends DataObject> void asyncRemove(final DataBroker broker,
            final LogicalDatastoreType datastoreType, InstanceIdentifier<T> path, FutureCallback<Void> callback) {
        WriteTransaction tx = broker.newWriteOnlyTransaction();
        tx.delete(datastoreType, path);
        Futures.addCallback(tx.submit(), callback);
    }

}
