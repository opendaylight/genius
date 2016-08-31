/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.utils.hwvtep;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class HwvtepHACache {
    static ConcurrentHashMap<InstanceIdentifier<Node>,Set<InstanceIdentifier<Node>>> haParentToChildMap = new ConcurrentHashMap<>();
    static ConcurrentHashMap<InstanceIdentifier<Node>,InstanceIdentifier<Node>> haChildToParentMap = new ConcurrentHashMap<>();
    static ConcurrentHashMap<String,Boolean> haChildNodeIds = new ConcurrentHashMap<>();

    static ConcurrentHashMap<String,Boolean> connectedNodes = new ConcurrentHashMap<>();
    static final int MAX_EVENT_BUFFER_SIZE = 500000;
    static final int EVENT_DRAIN_BUFFER_SIZE = 100000;

    static LinkedBlockingQueue<Pair<Long,String>> eventTimeStamps = new LinkedBlockingQueue<>(MAX_EVENT_BUFFER_SIZE);

    public static void updateNodeStatus(InstanceIdentifier<Node> iid, boolean connected) {
        String nodeId = iid.firstKeyOf(Node.class).getNodeId().getValue();
        int idx = nodeId.indexOf("uuid/");
        if (idx > 0) {
            nodeId = nodeId.substring(idx + "uuid/".length());
        }
        connectedNodes.put(nodeId, connected);

        String event = nodeId + " " + (connected ? "connected" : "disconneced");
        addEvent(event);
    }

    public static void addEvent(String event) {
        Long eventTime = System.currentTimeMillis();
        if (!eventTimeStamps.offer(new ImmutablePair<>(eventTime, event))) {
            Collection<? super Pair<Long, String>> list = new ArrayList<>();
            eventTimeStamps.drainTo(list, EVENT_DRAIN_BUFFER_SIZE);
        }
    }

    public static ConcurrentHashMap<String,Boolean> getConnectedNodes() {
        return new ConcurrentHashMap<>(connectedNodes);
    }

    public static ArrayList<Pair<Long,String>> getNodeEvents() {
        ArrayList<Pair<Long,String>> result = new ArrayList<>();
        Iterator<Pair<Long, String>> it = eventTimeStamps.iterator();
        while (it.hasNext()) {
            result.add(it.next());
        }
        return result;
    }

    public static boolean isHAParentNode(InstanceIdentifier<Node> node) {
        return haParentToChildMap.containsKey(node);
    }

    public static boolean isHAChildNode(InstanceIdentifier<Node> node) {
        return isHAEnabledDevice(node);
    }

    public static Set<InstanceIdentifier<Node>> getChildrenForHANode(InstanceIdentifier<Node> parent) {
        if (parent != null && haParentToChildMap.containsKey(parent)) {
            return new HashSet(haParentToChildMap.get(parent));
        } else {
            return Collections.EMPTY_SET;
        }
    }

    public static Set<InstanceIdentifier<Node>> getHAParentNodes() {
        return haParentToChildMap.keySet();
    }

    public static Set<InstanceIdentifier<Node>> getHAChildNodes() {
        return haChildToParentMap.keySet();
    }

    public static Set<InstanceIdentifier<Node>> getHAEnabledDevices() {
        return haChildToParentMap.keySet();
    }

    public static InstanceIdentifier<Node> getParent(InstanceIdentifier<Node> child) {
        if (child != null) {
            return haChildToParentMap.get(child);
        }
        return null;
    }

    public static synchronized void cleanupParent(InstanceIdentifier<Node> parent) {
        if (parent == null) {
            return;
        }

        if (haParentToChildMap.get(parent) != null) {
            Set<InstanceIdentifier<Node>> childs = haParentToChildMap.get(parent);
            for (InstanceIdentifier<Node> child : childs) {
                haChildToParentMap.remove(child);
                String childNodeId = child.firstKeyOf(Node.class).getNodeId().getValue();
                haChildNodeIds.remove(childNodeId);
            }
        }
        haParentToChildMap.remove(parent);
    }

    public static synchronized void addChild(InstanceIdentifier<Node> parent, InstanceIdentifier<Node> child) {
        if (parent == null || child == null) {
            return;
        }
        if (haParentToChildMap.get(parent) == null) {
            haParentToChildMap.put(parent, new HashSet<InstanceIdentifier<Node>>());
        }
        haParentToChildMap.get(parent).add(child);
        haChildToParentMap.put(child, parent);
        String childNodeId = child.firstKeyOf(Node.class).getNodeId().getValue();
        haChildNodeIds.put(childNodeId, Boolean.TRUE);
        addEvent(childNodeId+" ha child added to cache");
    }

    public static boolean isHAEnabledDevice(InstanceIdentifier<?> iid) {
        boolean enabled = haChildToParentMap.containsKey(iid);
        if (!enabled) {
            String nodeId = iid.firstKeyOf(Node.class).getNodeId().getValue();
            int idx = nodeId.indexOf(HwvtepSouthboundConstants.PHYSICALSWITCH);
            if (idx > 0) {
                nodeId = nodeId.substring(0, idx);
                return haChildNodeIds.containsKey(nodeId);
            }
        }
        return enabled;
    }
}
