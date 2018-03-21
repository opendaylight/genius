/*
 * Copyright © 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.utils.metrics;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.opendaylight.infrautils.metrics.MetricProvider;

/**
 * Metrics for datastore operations.
 * @Deprecated Please use {@link org.opendaylight.genius.tools.mdsal.metrics.DataStoreMetrics} instead of this.
 */
@Deprecated
@SuppressFBWarnings("NM_SAME_SIMPLE_NAME_AS_SUPERCLASS")
public class DataStoreMetrics extends org.opendaylight.genius.tools.mdsal.metrics.DataStoreMetrics {

    public DataStoreMetrics(MetricProvider metricProvider, Class<?> clazz) {
        super(metricProvider, clazz);
    }
}
