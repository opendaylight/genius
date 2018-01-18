/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.fcapsapp.performancecounter;

import java.util.HashSet;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.infrautils.metrics.Counter;
import org.opendaylight.infrautils.metrics.Labeled;
import org.opendaylight.infrautils.metrics.MetricDescriptor;
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
    private final Labeled<Labeled<Labeled<Counter>>> packetInCounter;
    private Counter counter;

    @Inject
    public NodeUpdateCounter(final PMAgent agent, final MetricProvider metricProvider) {
        this.agent = agent;
        this.metricProvider = metricProvider;
        packetInCounter =  metricProvider.newCounter(MetricDescriptor.builder().anchor(this)
                .project("genius").module("fcapsapplication")
                .id("entitycounter").build(), "entitytype", "hostid","name");
    }

    public void nodeAddedNotification(String node, String hostName) {
        dpnList.add(node);
        counter = packetInCounter.label("OFSwitch").label(hostName).label("switchespernode");
        counter.increment();
    }

    public void nodeRemovedNotification(String node, String hostName) {
        dpnList.remove(node);
        counter = packetInCounter.label("OFSwitch").label(hostName).label("switchespernode");
        counter.close();
    }

    public boolean isDpnConnectedLocal(String node) {
        return dpnList.contains(node);
    }
}
