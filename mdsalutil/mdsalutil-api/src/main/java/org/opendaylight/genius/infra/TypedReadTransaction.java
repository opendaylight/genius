/*
 * Copyright © 2018 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra;

import com.google.common.util.concurrent.FluentFuture;
import java.util.Optional;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.Transaction;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


/**
 * Read transaction which is specific to a single logical datastore (configuration or operational). Designed for use
 * with {@link ManagedNewTransactionRunner} (it doesn’t support explicit cancel or commit operations).
 *
 * @see ReadTransaction
 *
 * @param <D> The logical datastore handled by the transaction.
 */
public interface TypedReadTransaction<D extends Datastore>
        extends Transaction {
    /**
     * Reads an object from the given path.
     *
     * @see ReadTransaction#read(org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType,
     * InstanceIdentifier)
     *
     * @param path The path to read from.
     * @param <T> The type of the expected object.
     * @return A future providing access to the result of the read, when it’s available, or any error encountered.
     */
    <T extends DataObject> FluentFuture<Optional<T>> read(InstanceIdentifier<T> path);

    /**
     * Determines if an object exists at the given path.
     *
     * @see ReadTransaction#exists(org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType,
     * InstanceIdentifier)
     *
     * @param path The path to read from.
     * @return A future providing access to the result of the check, when it’s available, or any error encountered.
     */
    FluentFuture<Boolean> exists(InstanceIdentifier<?> path);
}
