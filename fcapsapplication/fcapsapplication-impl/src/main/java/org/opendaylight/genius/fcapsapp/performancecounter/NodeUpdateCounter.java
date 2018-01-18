/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.fcapsapp.performancecounter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.genius.fcapsapp.FcapsConstants;
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
    private Map<String,Counter> counterMap = new HashMap<>();

    @Inject
    public NodeUpdateCounter(final PMAgent agent, final MetricProvider metricProvider) {
        this.agent = agent;
        this.metricProvider = metricProvider;
    }

    public void nodeAddedNotification(String node, String hostName) {
        dpnList.add(node);
        Counter counter = metricProvider.newCounter(this,getCounterName(hostName));
        counter.increment();
        counterMap.put(getCounterName(hostName),counter);
    }

    public void nodeRemovedNotification(String node, String hostName) {
        dpnList.remove(node);
        if (counterMap.containsKey(getCounterName(hostName))) {
            counterMap.get(getCounterName(hostName)).close();
            counterMap.remove(getCounterName(hostName));
        }
    }


    private String getCounterName(String dpnId) {
        String dpnName = FcapsConstants.MODULENAME + FcapsConstants.ENTITY_TYPE_OFSWITCH
                + "nodeid=" + dpnId + ".switchespernode";
        return dpnName;
    }

    public boolean isDpnConnectedLocal(String node) {
        return dpnList.contains(node);
    }
}
