/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.utils.hwvtep;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@Deprecated
public class HwvtepHACache implements HwvtepNodeHACache {
    private static final int MAX_EVENT_BUFFER_SIZE = 500000;
    private static final int EVENT_DRAIN_BUFFER_SIZE = 100000;

    private static HwvtepHACache instance = new HwvtepHACache();

    private final ConcurrentHashMap<InstanceIdentifier<Node>, Set<InstanceIdentifier<Node>>>
        parentToChildMap = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<InstanceIdentifier<Node>, InstanceIdentifier<Node>>
        childToParentMap = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Boolean> childNodeIds = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Boolean> connectedNodes = new ConcurrentHashMap<>();

    private final LinkedBlockingQueue<DebugEvent> debugEvents = new LinkedBlockingQueue<>(MAX_EVENT_BUFFER_SIZE);

    public static HwvtepHACache getInstance() {
        return instance;
    }

    @Override
    public synchronized void addChild(InstanceIdentifier<Node> parent, InstanceIdentifier<Node> child) {
        if (parent == null || child == null) {
            return;
        }

        parentToChildMap.computeIfAbsent(parent, key -> new HashSet<>()).add(child);
        childToParentMap.put(child, parent);
        String childNodeId = child.firstKeyOf(Node.class).getNodeId().getValue();
        childNodeIds.put(childNodeId, Boolean.TRUE);
        addDebugEvent(new NodeEvent.ChildAddedEvent(childNodeId));
    }

    @Override
    public boolean isHAEnabledDevice(InstanceIdentifier<?> iid) {
        if (iid == null) {
            return false;
        }
        boolean enabled = childToParentMap.containsKey(iid.firstIdentifierOf(Node.class));
        if (!enabled) {
            String psNodeId = iid.firstKeyOf(Node.class).getNodeId().getValue();
            int idx = psNodeId.indexOf(HwvtepSouthboundConstants.PSWITCH_URI_PREFIX);
            if (idx > 0) {
                String globalNodeId = psNodeId.substring(0, idx - 1);
                return childNodeIds.containsKey(globalNodeId);
            }
        }
        return enabled;
    }

    @Override
    public  boolean isHAParentNode(InstanceIdentifier<Node> node) {
        return parentToChildMap.containsKey(node);
    }

    @Override
    public Set<InstanceIdentifier<Node>> getChildrenForHANode(InstanceIdentifier<Node> parent) {
        if (parent != null && parentToChildMap.containsKey(parent)) {
            return new HashSet<>(parentToChildMap.get(parent));
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public Set<InstanceIdentifier<Node>> getHAParentNodes() {
        return parentToChildMap.keySet();
    }

    @Override
    public Set<InstanceIdentifier<Node>> getHAChildNodes() {
        return childToParentMap.keySet();
    }

    @Override
    public InstanceIdentifier<Node> getParent(InstanceIdentifier<Node> child) {
        if (child != null) {
            return childToParentMap.get(child);
        }
        return null;
    }

    @Override
    public void removeParent(InstanceIdentifier<Node> parent) {
        cleanupParent(parent);
    }

    public synchronized void cleanupParent(InstanceIdentifier<Node> parent) {
        if (parent == null) {
            return;
        }

        if (parentToChildMap.get(parent) != null) {
            Set<InstanceIdentifier<Node>> childs = parentToChildMap.get(parent);
            for (InstanceIdentifier<Node> child : childs) {
                childToParentMap.remove(child);
                String childNodeId = child.firstKeyOf(Node.class).getNodeId().getValue();
                childNodeIds.remove(childNodeId);
            }
        }
        parentToChildMap.remove(parent);
    }

    @Override
    public void updateConnectedNodeStatus(InstanceIdentifier<Node> iid) {
        String nodeId = iid.firstKeyOf(Node.class).getNodeId().getValue();
        connectedNodes.put(nodeId, true);
        DebugEvent event = new NodeEvent.NodeConnectedEvent(nodeId);
        addDebugEvent(event);
    }

    @Override
    public void updateDisconnectedNodeStatus(InstanceIdentifier<Node> iid) {
        String nodeId = iid.firstKeyOf(Node.class).getNodeId().getValue();
        connectedNodes.put(nodeId, false);
        DebugEvent event = new NodeEvent.NodeDisconnectedEvent(nodeId);
        addDebugEvent(event);
    }

    public Map<String, Boolean> getConnectedNodes() {
        return ImmutableMap.copyOf(connectedNodes);
    }

    public void addDebugEvent(DebugEvent debugEvent) {
        //Try adding the event to event queue
        if (!debugEvents.offer(debugEvent)) {
            //buffer is exhausted
            Collection<DebugEvent> list = new ArrayList<>();
            //do not clear all events , make some place by clearing few old events
            debugEvents.drainTo(list, EVENT_DRAIN_BUFFER_SIZE);
            addDebugEvent(debugEvent);
        }
    }

    public List<DebugEvent> getNodeEvents() {
        return ImmutableList.copyOf(debugEvents);
    }

    @Override
    public Map<String, Boolean> getNodeConnectionStatuses() {
        return getConnectedNodes();
    }
}
