package org.opendaylight.genius.arputil.test;

import com.codahale.metrics.*;
import org.junit.Before;
import org.mockito.Mockito;
import org.opendaylight.genius.arputil.internal.ArpUtilImpl;
import org.opendaylight.infrautils.metrics.MetricProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;

import static org.opendaylight.yangtools.testutils.mockito.MoreAnswers.realOrException;

public abstract class TestMetricProvider implements MetricProvider {

    private MetricRegistry registry;

    public static TestMetricProvider newInstance() {
        TestMetricProvider testMetricProvider = Mockito.mock(TestMetricProvider.class, realOrException());
        testMetricProvider.createRegistry();
        return testMetricProvider;
    }

    public void createRegistry() {
        registry = new MetricRegistry();
    }

    @Override
    public Counter newCounter(Object anchor, String id) {
        return registry.counter(id);
    }
}
