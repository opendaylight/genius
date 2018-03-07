/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.cache;

import static java.util.Collections.emptySet;

import com.google.common.base.Optional;
import java.util.Collection;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * {@link DataObjectCache} which does not cache the DataObject.
 *
 * <p>This is used by code which has configuration knobs to enable or disable a DataObjectCache.
 *
 * <p>
 * Its {@link #added(InstanceIdentifier, DataObject)} and
 * {@link #removed(InstanceIdentifier, DataObject)} methods will never be
 * called.
 *
 * @author Michael Vorburger.ch
 */
public class NoopDataObjectCache<V extends DataObject> extends DataObjectCache<V> {

    public NoopDataObjectCache(DataBroker dataBroker, LogicalDatastoreType datastoreType) {
        super(dataBroker, datastoreType);
    }

    @Override
    public void close() {
        // we do *NOT* super(...);
    }

    /**
     * This will always read from the data store, not cached.
     *
     * {@inheritDoc}
     */
    @Override
    public Optional<V> get(InstanceIdentifier<V> path) throws ReadFailedException {
        return cacheFunction.get(path);
    }

    /**
     * This will always return an empty Collection.
     *
     * {@inheritDoc}
     */
    @Override
    public Collection<V> getAllPresent() {
        return emptySet();
    }

}
