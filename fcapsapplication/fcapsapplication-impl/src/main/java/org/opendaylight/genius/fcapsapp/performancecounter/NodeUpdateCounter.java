/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.fcapsapp.performancecounter;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.Integer;
import java.lang.String;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class NodeUpdateCounter {

    private static final Logger LOG = LoggerFactory.getLogger(NodeUpdateCounter.class);
    private String nodeListEFSCountStr;
    private static HashSet<String> dpnList = new HashSet<>();
    public final PMAgent pMAgent;
    Map<String, String> counter_map = new HashMap<>();

    @Inject
    public NodeUpdateCounter(final PMAgent pMAgent) {
        this.pMAgent = pMAgent;
    }

    public void nodeAddedNotification(String sNode,String hostName) {
        dpnList.add(sNode);
        sendNodeUpdation(dpnList.size(),hostName);
    }

    public void nodeRemovedNotification(String sNode,String hostName) {
        dpnList.remove(sNode);
        sendNodeUpdation(dpnList.size(), hostName);
    }

    private void sendNodeUpdation(Integer count,String hostName) {

        if (hostName != null) {
            nodeListEFSCountStr = "Node_" + hostName + "_NumberOfEFS";
            LOG.debug("NumberOfEFS:" + nodeListEFSCountStr + " dpnList.size " + count);

            counter_map.put("NumberOfEFS:" + nodeListEFSCountStr, "" + count);
            pMAgent.connectToPMAgent(counter_map);
        } else
            LOG.error("Hostname is null upon NumberOfEFS counter");
    }

    public boolean isDpnConnectedLocal(String sNode) {
        return dpnList.contains(sNode);
    }
}