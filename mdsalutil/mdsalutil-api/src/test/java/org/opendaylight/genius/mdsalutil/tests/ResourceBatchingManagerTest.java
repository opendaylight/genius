/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.mdsalutil.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.LinkedBlockingQueue;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.utils.batching.DefaultBatchHandler;
import org.opendaylight.genius.utils.batching.ResourceBatchingManager;
import org.opendaylight.genius.utils.hwvtep.HwvtepSouthboundConstants;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ResourceBatchingManagerTest extends AbstractConcurrentDataBrokerTest {

    private static final Integer BATCH_SIZE = 100;
    private static final Integer BATCH_INTERVAL_IN_MILLIS = 10000;

    private static final String NODE1_ID = "node1";
    private static final String NODE2_ID = "node2";
    private static final Node NODE1 = buildNode(NODE1_ID);
    private static final Node NODE2 = buildNode(NODE2_ID);
    private static final InstanceIdentifier<Node> NODE1_IID = buildNodeIid(NODE1_ID);
    private static final InstanceIdentifier<Node> NODE2_IID = buildNodeIid(NODE2_ID);

    private final String resourceType = "config.topology";

    private ResourceBatchingManager batchingManager;
    private DataBroker dataBroker;

    @Before
    public void registerResource() {
        dataBroker = getDataBroker();
        batchingManager = ResourceBatchingManager.getInstance();
        DefaultBatchHandler batchHandler = new DefaultBatchHandler(dataBroker, LogicalDatastoreType.CONFIGURATION,
                BATCH_SIZE, BATCH_INTERVAL_IN_MILLIS);
        batchingManager.registerBatchableResource(resourceType, new LinkedBlockingQueue<>(), batchHandler);
    }

    @Test
    public void testAddAndDeleteNode() throws ReadFailedException {
        batchingManager.put(resourceType, NODE1_IID, NODE1);
        assertTrue("Put operation failed", batchingManager.read(resourceType, NODE1_IID).isPresent());

        batchingManager.delete(resourceType, NODE1_IID);
        assertFalse("Delete operation failed", batchingManager.read(resourceType, NODE1_IID).isPresent());

        batchingManager.put(resourceType, NODE1_IID, NODE1);
        assertTrue("Delete followed by Put operation failed",
                batchingManager.read(resourceType, NODE1_IID).isPresent());
    }

    private static InstanceIdentifier<Node> buildNodeIid(String nodeId) {
        InstanceIdentifier<Node> path = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(new NodeId(nodeId)));
        return path;
    }

    private static Node buildNode(String nodeId) {
        return new NodeBuilder().setNodeId(new NodeId(nodeId)).build();
    }
}
