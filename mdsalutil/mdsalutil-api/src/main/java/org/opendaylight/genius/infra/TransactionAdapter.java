/*
 * Copyright © 2018 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;

@Deprecated
public class TransactionAdapter {
    public static ReadWriteTransaction toReadWriteTransaction(
            DatastoreReadWriteTransaction<? extends Datastore> datastoreTx) {
        if (datastoreTx instanceof NonSubmitCancelableDatastoreReadWriteTransaction) {
            return ((NonSubmitCancelableDatastoreReadWriteTransaction) datastoreTx).delegate;
        }
        throw new IllegalArgumentException(
                "Unsupported DatastoreWriteTransaction implementation " + datastoreTx.getClass());
    }

    public static WriteTransaction toWriteTransaction(DatastoreWriteTransaction<? extends Datastore> datastoreTx) {
        if (datastoreTx instanceof NonSubmitCancelableDatastoreWriteTransaction) {
            return ((NonSubmitCancelableDatastoreWriteTransaction) datastoreTx).delegate;
        }
        throw new IllegalArgumentException(
                "Unsupported DatastoreWriteTransaction implementation " + datastoreTx.getClass());
    }
}
