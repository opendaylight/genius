/*
 * Copyright (c) 2016 Red Hat and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.hwvtep;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.genius.utils.hwvtep.DebugEvent;
import org.opendaylight.genius.utils.hwvtep.HwvtepHACache;
import org.opendaylight.genius.utils.hwvtep.HwvtepSouthboundConstants;
import org.opendaylight.genius.utils.hwvtep.NodeEvent;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HwvtepHACacheTest {

    HwvtepHACache hwvtepHACache = HwvtepHACache.getInstance();

    @Test
    public void testAddHAChild() {

        InstanceIdentifier<Node> parent = newNodeInstanceIdentifier("ha");
        InstanceIdentifier<Node> child1 = newNodeInstanceIdentifier("d1");
        InstanceIdentifier<Node> child2 = newNodeInstanceIdentifier("d1");
        String child1NodeId = child1.firstKeyOf(Node.class).getNodeId().getValue();
        String child2NodeId = child2.firstKeyOf(Node.class).getNodeId().getValue();

        hwvtepHACache.addChild(parent, child1);

        assertTrue(hwvtepHACache.isHAEnabledDevice(child1));
        assertTrue(hwvtepHACache.isHAParentNode(parent));

        hwvtepHACache.addChild(parent, child2);
        assertTrue(hwvtepHACache.isHAEnabledDevice(child1));
        assertTrue(hwvtepHACache.isHAEnabledDevice(child2));
        assertTrue(hwvtepHACache.isHAParentNode(parent));

        assertTrue(hwvtepHACache.getHAChildNodes().contains(child1));
        assertTrue(hwvtepHACache.getHAChildNodes().contains(child2));
        assertTrue(hwvtepHACache.getHAParentNodes().contains(parent));

        assertTrue(hwvtepHACache.getChildrenForHANode(parent).contains(child1));
        assertTrue(hwvtepHACache.getChildrenForHANode(parent).contains(child2));

        List<DebugEvent> events = hwvtepHACache.getNodeEvents();
        assertTrue(events.contains(new NodeEvent.ChildAddedEvent(child1NodeId)));
        assertTrue(events.contains(new NodeEvent.ChildAddedEvent(child2NodeId)));

        hwvtepHACache.updateConnectedNodeStatus(child1);
        assertTrue(hwvtepHACache.getNodeEvents().contains(new NodeEvent.NodeConnectedEvent(child1NodeId)));

        hwvtepHACache.updateDisconnectedNodeStatus(child1);
        assertTrue(hwvtepHACache.getNodeEvents().contains(new NodeEvent.NodeDisconnectedEvent(child1NodeId)));

        hwvtepHACache.cleanupParent(parent);
        assertFalse(hwvtepHACache.isHAEnabledDevice(child1));
        assertFalse(hwvtepHACache.isHAEnabledDevice(child2));
        assertFalse(hwvtepHACache.isHAParentNode(parent));
    }

    public static InstanceIdentifier<Node> newNodeInstanceIdentifier(String id) {
        String nodeString = "hwvtep://uuid/" + java.util.UUID.nameUUIDFromBytes(id.getBytes()).toString();
        NodeId nodeId = new NodeId(new Uri(nodeString));
        NodeKey nodeKey = new NodeKey(nodeId);
        TopologyKey topoKey = new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID);
        return InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, topoKey)
                .child(Node.class, nodeKey)
                .build();
    }
}
