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
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Caches DataObjects of a particular type. The cache is updated by a DataTreeChangeListener.
 */
public interface DataObjectCache<V extends DataObject> extends AutoCloseable {
    @Override
    // Overridden to squash the exception
    void close();

    /**
     * Gets the DataObject for the given path. If there's no DataObject cached, it will be read from the provider
     * and put in the cache if it exists.
     *
     * @param path identifies the subtree to query
     * @return if the data at the supplied path exists, returns an Optional object containing the data; otherwise,
     *         returns Optional#absent()
     * @throws ReadFailedException if that data isn't cached and the read to fetch it fails
     */
    @Nonnull
    Optional<V> get(@Nonnull InstanceIdentifier<V> path) throws ReadFailedException;

    /**
     * Gets all DataObjects currently in the cache.
     *
     * @return the DataObjects currently in the cache
     */
    @Nonnull
    Collection<V> getAllPresent();
}
