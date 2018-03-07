/*
 * Copyright © 2018 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.cache;

import com.google.common.base.Optional;
import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Pass-through data object cache.
 */
public class NoopDataObjectCache<V extends DataObject> implements DataObjectCache<V> {
    private final SingleTransactionDataBroker broker;
    private final LogicalDatastoreType datastoreType;

    public NoopDataObjectCache(DataBroker dataBroker, LogicalDatastoreType datastoreType) {
        this.broker = new SingleTransactionDataBroker(dataBroker);
        this.datastoreType = datastoreType;
    }

    @Nonnull
    @Override
    public Optional<V> get(@Nonnull InstanceIdentifier<V> path) throws ReadFailedException {
        return broker.syncReadOptional(datastoreType, path);
    }

    @Nonnull
    @Override
    public Collection<V> getAllPresent() {
        return Collections.emptySet();
    }

    @Override
    public void close() {
        // Nothing to do here
    }
}
