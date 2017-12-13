/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.cache;

import com.google.common.base.Optional;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Caches DataObjects of a particular type.
 *
 * @author Thomas Pantelis
 */
public class DataObjectCache<V extends DataObject> {
    private final SingleTransactionDataBroker broker;
    private final LogicalDatastoreType datastoreType;
    private final ConcurrentMap<InstanceIdentifier<V>, Optional<V>> cache = new ConcurrentHashMap<>();

    public DataObjectCache(DataBroker dataBroker, LogicalDatastoreType datastoreType) {
        this.broker = new SingleTransactionDataBroker(dataBroker);
        this.datastoreType = datastoreType;
    }

    @Nonnull
    public Optional<V> getIfPresent(@Nonnull InstanceIdentifier<V> path) {
        Optional<V> optional = cache.get(path);
        return optional != null ? optional : Optional.absent();
    }

    @Nonnull
    public Optional<V> get(@Nonnull InstanceIdentifier<V> path) throws ReadFailedException {
        Optional<V> optional = cache.get(path);
        if (optional != null) {
            return optional;
        }

        optional = broker.syncReadOptional(datastoreType, path);
        cache.put(path, optional);
        return optional;
    }

    @Nonnull
    public Collection<V> getAll() {
        return cache.values().stream().flatMap(optional -> optional.isPresent()
                ? Stream.of(optional.get()) : Stream.empty()).collect(Collectors.toList());
    }

    public void remove(@Nonnull InstanceIdentifier<V> path) {
        cache.remove(path);
    }

    public void put(@Nonnull InstanceIdentifier<V> path, @Nonnull V value) {
        cache.put(path, Optional.of(value));
    }
}
