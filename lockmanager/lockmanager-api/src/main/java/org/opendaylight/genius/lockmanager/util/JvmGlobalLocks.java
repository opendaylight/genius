/*
 * Copyright (c) 2018 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.lockmanager.util;

import static java.util.Objects.requireNonNull;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.concurrent.locks.ReentrantLock;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;

/**
 * Utility class providing JVM-global locks keyed by String. This class is provided to provide an alternative to the
 * following locking invention:
 * <pre>
 *     String foo;
 *     synchronized (foo.intern()) {
 *         // ...
 *     }
 * </pre>
 *
 * <p>
 * As such this is an extremely bad idea, as it does not make it clear what the locking domain is and what code is
 * actually participating on it. Until we can get proper locking in place, the above horror should be replaced with:
 * <pre>
 *     String foo;
 *     final ReentrantLock = JvnGlobalLocks.getInstance();
 *     lock.lock();
 *     try {
 *         // ...
 *     } finally {
 *         lock.unlock();
 *     }
 * </pre>
 * This class provides a replacement, so that we can separate locking domains.
 *
 * @author Robert Varga
 */
public final class JvmGlobalLocks {
    // We use weakValues() to allow garbage-collection of unheld locks.
    private static final LoadingCache<Object, NamedReentrantLock<?>> LOCKS = CacheBuilder.newBuilder().weakValues()
            .build(new CacheLoader<Object, NamedReentrantLock<?>>() {
                @Override
                public NamedReentrantLock<?> load(final Object key) {
                    return new NamedReentrantLock<>(key);
                }
            });

    private JvmGlobalLocks() {

    }

    /**
     * Return a JVM-global {@link ReentrantLock} for a particular string.
     *
     * @param lockName Name of the lock, must not be null
     * @return A JVM-global reentrant lock.
     * @throws NullPointerException if {@code lockName} is null
     * @deprecated This is provided only for migration purposes until a proper locking scheme is deployed throughout
     *             genius and netvirt.
     */
    @Deprecated
    public static @NonNull ReentrantLock getLockForString(final String lockName) {
        return LOCKS.getUnchecked(requireNonNull(lockName));
    }

    /**
     * Return a JVM-global {@link ReentrantLock} for an identifier.
     *
     * @param identifier Entity identifier, must not be null
     * @return A JVM-global reentrant lock.
     * @throws NullPointerException if {@code identifier} is null
     */
    public static @NonNull ReentrantLock getLockFor(final Identifier<?> identifier) {
        return LOCKS.getUnchecked(requireNonNull(identifier));
    }

    /**
     * Return a JVM-global {@link ReentrantLock} for a particular name.
     *
     * @param resource an {@link Identifiable} resource
     * @return A JVM-global reentrant lock.
     * @throws NullPointerException if {@code resource} is null
     */
    public static @NonNull ReentrantLock getLockFor(final Identifiable<?> resource) {
        return LOCKS.getUnchecked(resource.key());
    }
}
