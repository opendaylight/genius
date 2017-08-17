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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class NodeUpdateCounter {

    private static final Logger LOG = LoggerFactory.getLogger(NodeUpdateCounter.class);
    private String nodeListEFSCountStr;
    private static HashSet<String> dpnList = new HashSet<>();
    public final PMAgent agent;
    private final Map<String, String> countersMap = new HashMap<>();

    @Inject
    public NodeUpdateCounter(final PMAgent agent) {
        this.agent = agent;
    }

    public void nodeAddedNotification(String node, String hostName) {
        dpnList.add(node);
        sendNodeUpdation(dpnList.size(), hostName);
    }

    public void nodeRemovedNotification(String node, String hostName) {
        dpnList.remove(node);
        sendNodeUpdation(dpnList.size(), hostName);
    }

    private void sendNodeUpdation(Integer count, String hostName) {

        if (hostName != null) {
            nodeListEFSCountStr = "Node_" + hostName + "_NumberOfEFS";
            LOG.debug("NumberOfEFS: {} dpnList.size {}", nodeListEFSCountStr, count);

            countersMap.put("NumberOfEFS:" + nodeListEFSCountStr, "" + count);
            agent.connectToPMAgent(countersMap);
        } else {
            LOG.error("Hostname is null upon NumberOfEFS counter");
        }
    }

    public boolean isDpnConnectedLocal(String node) {
        return dpnList.contains(node);
    }
}
