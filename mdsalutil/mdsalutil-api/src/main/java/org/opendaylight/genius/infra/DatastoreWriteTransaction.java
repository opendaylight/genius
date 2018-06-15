/*
 * Copyright © 2018 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra;

import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public interface DatastoreWriteTransaction<D extends Datastore> extends WriteTransaction {
    @Override
    @Deprecated
    <T extends DataObject> void put(LogicalDatastoreType store, InstanceIdentifier<T> path, T data);

    <T extends DataObject> void put(InstanceIdentifier<T> path, T data);

    @Override
    @Deprecated
    <T extends DataObject> void put(LogicalDatastoreType store, InstanceIdentifier<T> path, T data,
            boolean createMissingParents);

    <T extends DataObject> void put(InstanceIdentifier<T> path, T data, boolean createMissingParents);

    @Override
    @Deprecated
    <T extends DataObject> void merge(LogicalDatastoreType store, InstanceIdentifier<T> path, T data);

    <T extends DataObject> void merge(InstanceIdentifier<T> path, T data);

    @Override
    @Deprecated
    <T extends DataObject> void merge(LogicalDatastoreType store, InstanceIdentifier<T> path, T data,
            boolean createMissingParents);

    <T extends DataObject> void merge(InstanceIdentifier<T> path, T data, boolean createMissingParents);

    @Override
    @Deprecated
    void delete(LogicalDatastoreType store, InstanceIdentifier<?> path);

    void delete(InstanceIdentifier<?> path);
}
