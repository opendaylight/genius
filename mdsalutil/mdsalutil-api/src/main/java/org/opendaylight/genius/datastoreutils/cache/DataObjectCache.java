/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.cache;

import com.google.common.base.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Caches DataObjects of a particular type.
 *
 * @author Thomas Pantelis
 */
public class DataObjectCache<V extends DataObject> {
    private static final Logger LOG = LoggerFactory.getLogger(DataObjectCache.class);

    private final SingleTransactionDataBroker broker;
    private final LogicalDatastoreType datastoreType;
    private final ConcurrentMap<InstanceIdentifier<V>, V> cache = new ConcurrentHashMap<>();

    public DataObjectCache(DataBroker dataBroker, LogicalDatastoreType datastoreType) {
        this.broker = new SingleTransactionDataBroker(dataBroker);
        this.datastoreType = datastoreType;
    }

    @Nonnull
    public Optional<V> getIfPresent(@Nonnull InstanceIdentifier<V> path) {
        return Optional.fromNullable(cache.get(path));
    }

    @Nonnull
    public Optional<V> get(@Nonnull InstanceIdentifier<V> path) {
        Optional<V> optional = getIfPresent(path);
        if (optional.isPresent()) {
            return optional;
        }

        optional = read(path);
        if (optional.isPresent()) {
            put(path, optional.get());
        }

        return optional;
    }

    public void remove(@Nonnull InstanceIdentifier<V> path) {
        cache.remove(path);
    }

    public void put(@Nonnull InstanceIdentifier<V> path, @Nonnull V value) {
        cache.put(path, value);
    }

    private Optional<V> read(InstanceIdentifier<V> path) {
        try {
            return broker.syncReadOptional(datastoreType, path);
        } catch (ReadFailedException e) {
            LOG.warn("Read of {} failed", path, e);
            return Optional.absent();
        }
    }
}
