/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.fcapsapp.performancecounter;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.opendaylight.genius.fcapsapp.FcapsConstants;
import org.opendaylight.infrautils.metrics.Counter;
import org.opendaylight.infrautils.metrics.MetricProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.opendaylight.genius.fcapsapp.FcapsConstants.MODULENAME;

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
        this.metricProvider =metricProvider;
    }

    public void nodeAddedNotification(String node, String hostName) {
        dpnList.add(node);
        Counter  counter =metricProvider.newCounter(this,getCounterName(node));
        counter.increment();
        counterMap.put(getCounterName(node),counter);
    }

    public void nodeRemovedNotification(String node, String hostName) {
        dpnList.remove(node);
        if(counterMap.containsKey(getCounterName(node))){
            counterMap.get(getCounterName(node)).close();
            counterMap.remove(getCounterName(node));
        }
    }


    private String getCounterName(String dpnId){
        String dpnName=MODULENAME + FcapsConstants.ENTITY_TYPE_OFSWITCH + "switchid= " + dpnId + "/switchespernode";
        return dpnName;
    }

    public boolean isDpnConnectedLocal(String node) {
        return dpnList.contains(node);
    }
}
