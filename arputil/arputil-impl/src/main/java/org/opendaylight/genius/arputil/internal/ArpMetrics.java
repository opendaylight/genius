/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.arputil.internal;

import com.codahale.metrics.Counter;
import org.opendaylight.infrautils.metrics.MetricProvider;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class ArpMetrics {

    private MetricProvider metricProvider;
    private Map<ArpUtilCounters,Counter> metricsMap = new HashMap<>();

    @Inject
    public ArpMetrics(MetricProvider metricProvider) {
        this.metricProvider = metricProvider;
    }

    @PostConstruct
    public void start() {
        for (ArpUtilCounters arpCounter : ArpUtilCounters.values()) {
            metricsMap.put(arpCounter,this.metricProvider.newCounter(this, arpCounter.name()));
        }
    }

    public void inc(ArpUtilCounters counter) {
        metricsMap.get(counter).inc();
    }

    public long get(ArpUtilCounters counter) {
        return metricsMap.get(counter).getCount();
    }
}