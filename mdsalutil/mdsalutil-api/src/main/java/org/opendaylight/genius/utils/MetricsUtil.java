/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.utils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.infrautils.caches.Cache;
import org.opendaylight.infrautils.caches.CacheConfigBuilder;
import org.opendaylight.infrautils.caches.CachePolicyBuilder;
import org.opendaylight.infrautils.caches.CacheProvider;
import org.opendaylight.infrautils.metrics.Counter;
import org.opendaylight.infrautils.metrics.MetricProvider;

@Singleton
public final class MetricsUtil {

    private static MetricsUtil instance;

    private final Cache<Pair<Object, String>, Counter> cache;

    @Inject
    public MetricsUtil(MetricProvider metricProvider, CacheProvider cacheProvider) {
        this.cache = cacheProvider.newCache(
            new CacheConfigBuilder<Pair<Object, String>, Counter>()
                .anchor(this)
                .cacheFunction(pair -> metricProvider.newCounter(pair.getLeft(), pair.getRight()))
                .build(),
                new CachePolicyBuilder().maxEntries(20000).build());
    }

    @SuppressFBWarnings
    public static void increment(Object context, String counter) {
        instance.cache.get(ImmutablePair.of(context, counter)).increment();
    }

    @SuppressFBWarnings
    public static void incrementAdded(Object context, String counter) {
        instance.cache.get(ImmutablePair.of(context, counter + ".add")).increment();
    }

    @SuppressFBWarnings
    public static void incrementUpdated(Object context, String counter) {
        instance.cache.get(ImmutablePair.of(context, counter + ".update")).increment();
    }

    @SuppressFBWarnings
    public static void incrementDeleted(Object context, String counter) {
        instance.cache.get(ImmutablePair.of(context, counter + ".del")).increment();
    }
}
