/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.cache;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.PreDestroy;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.infrautils.caches.CacheProvider;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Caches DataObjects of a particular type. The cache is updated by a DataTreeChangeListener.
 *
 * @author Thomas Pantelis
 */
public class DataObjectCache<K, V extends DataObject> implements AutoCloseable {

    private final SingleTransactionDataBroker broker;
    private final LoadingCache<K, Optional<V>> cache;
    private final ListenerRegistration<?> listenerRegistration;

    /**
     * Constructor.
     *
     * @param dataObjectClass the DataObject class to cache
     * @param dataBroker the DataBroker
     * @param datastoreType the LogicalDatastoreType
     * @param listetenerRegistrationPath the yang path for which register the listener
     * @param cacheProvider the CacheProvider used to instantiate the Cache
     * @param keyFunction the function used to convert or extract the key instance on change notification
     * @param instanceIdFunction the function used to convert a key instance to an InstanceIdentifier on read
     */
    public DataObjectCache(Class<V> dataObjectClass, DataBroker dataBroker, LogicalDatastoreType datastoreType,
            InstanceIdentifier<V> listetenerRegistrationPath, CacheProvider cacheProvider,
            BiFunction<InstanceIdentifier<V>, V, K> keyFunction,
            Function<K, InstanceIdentifier<V>> instanceIdFunction) {
        Objects.requireNonNull(keyFunction);
        Objects.requireNonNull(instanceIdFunction);
        this.broker = new SingleTransactionDataBroker(Objects.requireNonNull(dataBroker));

        requireNonNull(cacheProvider, "cacheProvider");
        cache = CacheBuilder.newBuilder().build(new CacheLoader<K, Optional<V>>() {
            @Override
            public Optional<V> load(K key) throws ReadFailedException {
                return broker.syncReadOptional(datastoreType, instanceIdFunction.apply(key));
            }
        });

        ClusteredDataTreeChangeListener<V> dataObjectListener = changes -> {
            for (DataTreeModification<V> dataTreeModification : changes) {
                DataObjectModification<V> rootNode = dataTreeModification.getRootNode();
                InstanceIdentifier<V> path = dataTreeModification.getRootPath().getRootIdentifier();
                switch (rootNode.getModificationType()) {
                    case WRITE:
                    case SUBTREE_MODIFIED:
                        V dataAfter = rootNode.getDataAfter();
                        cache.put(keyFunction.apply(path, dataAfter), Optional.of(dataAfter));
                        added(path, dataAfter);
                        break;
                    case DELETE:
                        V dataBefore = rootNode.getDataBefore();
                        cache.invalidate(keyFunction.apply(path, dataBefore));
                        removed(path, dataBefore);
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
    @PreDestroy
    public void close() {
        listenerRegistration.close();
        cache.cleanUp();
    }

    /**
     * Gets the DataObject for the given key. If there's no DataObject cached, it will be read from the data store
     * and put in the cache if it exists.
     *
     * @param key identifies the DataObject to query
     * @return if the data for the supplied key exists, returns an Optional object containing the data; otherwise,
     *         returns Optional#absent()
     * @throws ReadFailedException if that data isn't cached and the read to fetch it fails
     */
    @Nonnull
    // The ExecutionException cause should be a ReadFailedException - ok to cast.
    @SuppressFBWarnings("BC_UNCONFIRMED_CAST_OF_RETURN_VALUE")
    @SuppressWarnings("checkstyle:AvoidHidingCauseException")
    public Optional<V> get(@Nonnull K key) throws ReadFailedException {
        try {
            return cache.get(key);
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

    protected void added(InstanceIdentifier<V> path, V dataObject) {
    }

    protected void removed(InstanceIdentifier<V> path, V dataObject) {
    }
}
