/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.utils.hwvtep.internal;

import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Singleton;
import org.opendaylight.genius.utils.hwvtep.HwvtepNodeHACache;
import org.opendaylight.genius.utils.hwvtep.HwvtepSouthboundConstants;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Implementation of HwvtepNodeHACache.
 *
 * @author Thomas Pantelis
 */
@Singleton
public class HwvtepNodeHACacheImpl implements HwvtepNodeHACache {
    private final ConcurrentHashMap<InstanceIdentifier<Node>, Set<InstanceIdentifier<Node>>> parentToChildMap =
            new ConcurrentHashMap<>();

    private final ConcurrentHashMap<InstanceIdentifier<Node>, InstanceIdentifier<Node>> childToParentMap =
            new ConcurrentHashMap<>();

    private final Set<String> childNodeIds = ConcurrentHashMap.newKeySet();

    private final ConcurrentHashMap<String, Boolean> connectedNodes = new ConcurrentHashMap<>();

    @Override
    public void addChild(InstanceIdentifier<Node> parentId, InstanceIdentifier<Node> childId) {
        if (parentId == null || childId == null) {
            return;
        }

        parentToChildMap.computeIfAbsent(parentId, key -> ConcurrentHashMap.newKeySet()).add(childId);
        childToParentMap.put(childId, parentId);
        String childNodeId = childId.firstKeyOf(Node.class).getNodeId().getValue();
        childNodeIds.add(childNodeId);
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
                return childNodeIds.contains(globalNodeId);
            }
        }
        return enabled;
    }

    @Override
    public  boolean isHAParentNode(InstanceIdentifier<Node> nodeId) {
        return parentToChildMap.containsKey(nodeId);
    }

    @Override
    public Set<InstanceIdentifier<Node>> getChildrenForHANode(InstanceIdentifier<Node> parentId) {
        Set<InstanceIdentifier<Node>> children = parentId != null ? parentToChildMap.get(parentId) : null;
        return children != null ? children : Collections.emptySet();
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
    public InstanceIdentifier<Node> getParent(InstanceIdentifier<Node> childId) {
        return childId != null ? childToParentMap.get(childId) : null;
    }

    @Override
    public void removeParent(InstanceIdentifier<Node> parentId) {
        if (parentId == null) {
            return;
        }

        if (parentToChildMap.get(parentId) != null) {
            Set<InstanceIdentifier<Node>> childs = parentToChildMap.get(parentId);
            for (InstanceIdentifier<Node> child : childs) {
                childToParentMap.remove(child);
                String childNodeId = child.firstKeyOf(Node.class).getNodeId().getValue();
                childNodeIds.remove(childNodeId);
            }
        }
        parentToChildMap.remove(parentId);
    }

    @Override
    public void updateConnectedNodeStatus(InstanceIdentifier<Node> iid) {
        String nodeId = iid.firstKeyOf(Node.class).getNodeId().getValue();
        connectedNodes.put(nodeId, true);
    }

    @Override
    public void updateDisconnectedNodeStatus(InstanceIdentifier<Node> iid) {
        String nodeId = iid.firstKeyOf(Node.class).getNodeId().getValue();
        connectedNodes.put(nodeId, false);
    }

    @Override
    public Map<String, Boolean> getNodeConnectionStatuses() {
        return ImmutableMap.copyOf(connectedNodes);
    }
}
