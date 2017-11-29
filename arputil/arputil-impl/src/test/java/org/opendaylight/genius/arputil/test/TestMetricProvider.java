/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.arputil.test;

import com.codahale.metrics.*;
import org.mockito.Mockito;
import org.opendaylight.infrautils.metrics.MetricProvider;

import static org.opendaylight.yangtools.testutils.mockito.MoreAnswers.realOrException;

public abstract class TestMetricProvider implements MetricProvider {

    private MetricRegistry registry;

    public static TestMetricProvider newInstance() {
        TestMetricProvider testMetricProvider = Mockito.mock(TestMetricProvider.class, realOrException());
        testMetricProvider.createRegistry();
        return testMetricProvider;
    }

    private void createRegistry() {
        registry = new MetricRegistry();
    }

    @Override
    public Counter newCounter(Object anchor, String id) {
        return registry.counter(id);
    }
}
