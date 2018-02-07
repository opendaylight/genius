/*
 * Copyright © 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.utils.metrics;

import javax.inject.Inject;

import org.opendaylight.infrautils.metrics.Counter;
import org.opendaylight.infrautils.metrics.MetricDescriptor;
import org.opendaylight.infrautils.metrics.MetricProvider;


public class DataStoreMetrics {

    private final MetricProvider metricProvider;
    private final Class clazz;
    private final Counter added;
    private final Counter updated;
    private final Counter deleted;

    @Inject
    public DataStoreMetrics(MetricProvider metricProvider, Class clazz) {
        this.metricProvider = metricProvider;
        this.clazz = clazz;
        added = initCounter(":added");
        updated = initCounter(":updated");
        deleted = initCounter(":deleted");
    }

    public void incrementAdded() {
        added.increment();
    }

    public void incrementUpdated() {
        updated.increment();
    }

    public void incrementDeleted() {
        deleted.increment();
    }

    private Counter initCounter(String type) {
        String className = clazz.getSimpleName();
        //expects the form org.opendaylight.project.module
        String project = clazz.getName().split("\\.")[2];
        String module = clazz.getName().split("\\.")[3];
        return metricProvider.newCounter(new MetricDescriptor() {
            @Override
            public Object anchor() {
                return this;
            }

            @Override
            public String project() {
                return project;
            }

            @Override
            public String module() {
                return module;
            }

            @Override
            public String id() {
                return className + type;
            }
        });
    }
}
