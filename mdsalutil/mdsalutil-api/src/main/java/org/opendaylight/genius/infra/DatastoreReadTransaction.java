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
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public interface DatastoreReadTransaction<D extends Datastore> extends ReadTransaction {
    @Override
    @Deprecated
    <T extends DataObject> CheckedFuture<Optional<T>, ReadFailedException> read(
            LogicalDatastoreType store, InstanceIdentifier<T> path);

    <T extends DataObject> CheckedFuture<Optional<T>, ReadFailedException> read(InstanceIdentifier<T> path);
}
