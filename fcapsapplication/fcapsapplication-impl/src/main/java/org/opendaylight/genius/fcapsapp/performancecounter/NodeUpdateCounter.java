/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.fcapsapp.performancecounter;

import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.genius.fcapsapp.FcapsUtil;
import org.opendaylight.infrautils.metrics.Counter;
import org.opendaylight.infrautils.metrics.MetricProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class NodeUpdateCounter {

    private static final Logger LOG = LoggerFactory.getLogger(NodeUpdateCounter.class);
    private String nodeListEFSCountStr;
    private static HashSet<String> dpnList = new HashSet<>();
    public final PMAgent agent;
    private final MetricProvider metricProvider;
    private Map<String,Counter> counterMap = new ConcurrentHashMap<>();
    private String counterName = "switchespernode";

    @Inject
    public NodeUpdateCounter(final PMAgent agent, final MetricProvider metricProvider) {
        this.agent = agent;
        this.metricProvider = metricProvider;
    }

    public void nodeAddedNotification(String node, String hostName) {
        dpnList.add(node);
        String counterKey = FcapsUtil.getCounterName(String.valueOf(hostName),counterName)
                .replace("switchid","hostid");
        counterMap.computeIfAbsent(counterKey,
                (counter -> metricProvider.newCounter(this, counterKey))).increment();
    }

    public void nodeRemovedNotification(String node, String hostName) {
        dpnList.remove(node);
        String counterKey = FcapsUtil.getCounterName(String.valueOf(hostName),counterName)
                .replace("switchid","hostid");
        if (counterMap.containsKey(counterKey)) {
            counterMap.get(counterKey).close();
            counterMap.remove(counterKey);
        }
    }

    public boolean isDpnConnectedLocal(String node) {
        return dpnList.contains(node);
    }
}
