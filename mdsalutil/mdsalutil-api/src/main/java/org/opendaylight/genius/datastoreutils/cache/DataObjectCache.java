/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.cache;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Caches DataObjects of a particular type. The cache is updated by a DataTreeChangeListener.
 *
 * @author Thomas Pantelis
 */
public class DataObjectCache<V extends DataObject> implements AutoCloseable {
    private final SingleTransactionDataBroker broker;
    private final LogicalDatastoreType datastoreType;
    private final LoadingCache<InstanceIdentifier<V>, Optional<V>> cache;
    private ListenerRegistration<?> listenerRegistration;

    public DataObjectCache(DataBroker dataBroker, LogicalDatastoreType datastoreType,
            InstanceIdentifier<V> listetenerRegistrationPath) {
        this.broker = new SingleTransactionDataBroker(dataBroker);
        this.datastoreType = datastoreType;

        cache = CacheBuilder.newBuilder().build(new CacheLoader<InstanceIdentifier<V>, Optional<V>>() {
            @Override
            public Optional<V> load(InstanceIdentifier<V> path) throws ReadFailedException {
                return read(path);
            }
        });

        ClusteredDataTreeChangeListener<V> dataObjectListener = (ClusteredDataTreeChangeListener<V>) changes -> {
            for (DataTreeModification<V> dataTreeModification : changes) {
                DataObjectModification<V> rootNode = dataTreeModification.getRootNode();
                switch (rootNode.getModificationType()) {
                    case WRITE:
                    case SUBTREE_MODIFIED:
                        cache.put(dataTreeModification.getRootPath().getRootIdentifier(),
                                Optional.of(rootNode.getDataAfter()));
                        break;
                    case DELETE:
                        cache.invalidate(dataTreeModification.getRootPath().getRootIdentifier());
                        break;
                    default:
                        break;
                }
            }
        };

        listenerRegistration = dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<>(
                datastoreType, listetenerRegistrationPath), dataObjectListener);
    }

    @Override
    public void close() {
        listenerRegistration.close();
    }

    /**
     * Gets the DataObject for the given path. If there's no DataObject cached, it will be read from the data store
     * and put in the cache if it exists.
     *
     * @param path identifies the subtree to query
     * @return if the data at the supplied path exists, returns an Optional object containing the data; otherwise,
     *         returns Optional#absent()
     * @throws ReadFailedException if that data isn't cached and the read to fetch it fails
     */
    @Nonnull
    // The ExecutionException should be ReadFailedException.
    @SuppressFBWarnings("BC_UNCONFIRMED_CAST_OF_RETURN_VALUE")
    public Optional<V> get(@Nonnull InstanceIdentifier<V> path) throws ReadFailedException {
        try {
            return cache.get(path);
        } catch (ExecutionException e) {
            throw (ReadFailedException) e.getCause();
        }
    }

    /**
     * Gets all DataObjects currently in the cache.
     *
     * @return the DataObjects currently in the cache
     */
    @Nonnull
    public Collection<V> getAllPresent() {
        return cache.asMap().values().stream().flatMap(optional -> optional.isPresent()
                ? Stream.of(optional.get()) : Stream.empty()).collect(Collectors.toList());
    }

    private Optional<V> read(InstanceIdentifier<V> path) throws ReadFailedException {
        return broker.syncReadOptional(datastoreType, path);
    }
}
