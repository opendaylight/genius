/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.utils.hwvtep.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.opendaylight.genius.utils.hwvtep.HwvtepSouthboundConstants;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class HwvtepNodeHACacheImplTest {
    private final HwvtepNodeHACacheImpl hwvtepNodeHACacheImpl = new HwvtepNodeHACacheImpl();

    @Test
    public void testAddChild() {
        InstanceIdentifier<Node> parent = newNodeInstanceIdentifier("ha");
        InstanceIdentifier<Node> child1 = newNodeInstanceIdentifier("d1");

        hwvtepNodeHACacheImpl.addChild(parent, child1);

        assertTrue(hwvtepNodeHACacheImpl.isHAEnabledDevice(child1));
        assertTrue(hwvtepNodeHACacheImpl.isHAParentNode(parent));

        InstanceIdentifier<Node> child2 = newNodeInstanceIdentifier("d1");
        hwvtepNodeHACacheImpl.addChild(parent, child2);
        assertTrue(hwvtepNodeHACacheImpl.isHAEnabledDevice(child1));
        assertTrue(hwvtepNodeHACacheImpl.isHAEnabledDevice(child2));
        assertTrue(hwvtepNodeHACacheImpl.isHAParentNode(parent));

        assertEquals(ImmutableSet.of(child1, child2), hwvtepNodeHACacheImpl.getHAChildNodes());
        assertEquals(ImmutableSet.of(parent), hwvtepNodeHACacheImpl.getHAParentNodes());

        assertEquals(ImmutableSet.of(child1, child2), hwvtepNodeHACacheImpl.getChildrenForHANode(parent));

        hwvtepNodeHACacheImpl.removeParent(parent);
        assertFalse(hwvtepNodeHACacheImpl.isHAEnabledDevice(child1));
        assertFalse(hwvtepNodeHACacheImpl.isHAEnabledDevice(child2));
        assertFalse(hwvtepNodeHACacheImpl.isHAParentNode(parent));
    }

    @Test
    public void testNodeConnectionStatus() {
        InstanceIdentifier<Node> node1 = newNodeInstanceIdentifier("node1");
        InstanceIdentifier<Node> node2 = newNodeInstanceIdentifier("node2");

        hwvtepNodeHACacheImpl.updateConnectedNodeStatus(node1);
        assertEquals(ImmutableMap.of("node1", Boolean.TRUE), hwvtepNodeHACacheImpl.getNodeConnectionStatuses());

        hwvtepNodeHACacheImpl.updateConnectedNodeStatus(node2);
        assertEquals(ImmutableMap.of("node1", Boolean.TRUE, "node2", Boolean.TRUE),
                hwvtepNodeHACacheImpl.getNodeConnectionStatuses());

        hwvtepNodeHACacheImpl.updateDisconnectedNodeStatus(node1);
        assertEquals(ImmutableMap.of("node1", Boolean.FALSE, "node2", Boolean.TRUE),
                hwvtepNodeHACacheImpl.getNodeConnectionStatuses());
    }

    private static InstanceIdentifier<Node> newNodeInstanceIdentifier(String id) {
        NodeId nodeId = new NodeId(id);
        NodeKey nodeKey = new NodeKey(nodeId);
        TopologyKey topoKey = new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID);
        return InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, topoKey)
                .child(Node.class, nodeKey)
                .build();
    }
}
