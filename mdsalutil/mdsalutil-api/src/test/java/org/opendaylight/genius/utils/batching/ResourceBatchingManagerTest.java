/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd., Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.utils.batching;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.genius.utils.hwvtep.HwvtepSouthboundConstants;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
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
    private static final Integer BATCH_INTERVAL_IN_MILLIS = 500;

    private static final String NODE1_ID = "node1";
    private static final Node NODE1 = buildNode(NODE1_ID);
    private static final InstanceIdentifier<Node> NODE1_IID = buildNodeIid(NODE1_ID);

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
    public void testReadAfterPutAndDelete() throws InterruptedException, ExecutionException, TimeoutException {
        Optional<Node> optional = batchingManager.read(resourceType, NODE1_IID).get(5, TimeUnit.SECONDS);
        assertFalse("Expected not present", optional.isPresent());

        batchingManager.put(resourceType, NODE1_IID, NODE1);
        optional = batchingManager.read(resourceType, NODE1_IID).get(5, TimeUnit.SECONDS);
        assertTrue("Expected present", optional.isPresent());
        assertEquals("Node", NODE1, optional.get());

        batchingManager.delete(resourceType, NODE1_IID);
        optional = batchingManager.read(resourceType, NODE1_IID).get(5, TimeUnit.SECONDS);
        assertFalse("Expected not present", optional.isPresent());
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