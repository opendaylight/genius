/*
 * Copyright (c) 2019 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.cloudscaler.tests;

import static org.junit.Assert.assertTrue;
import static org.opendaylight.mdsal.common.api.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.mdsal.common.api.LogicalDatastoreType.OPERATIONAL;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.opendaylight.genius.cloudscaler.api.ScaleInConstants;
import org.opendaylight.genius.cloudscaler.rpcservice.CloudscalerRpcServiceImpl;
import org.opendaylight.genius.cloudscaler.rpcservice.ComputeNodeManager;
import org.opendaylight.genius.cloudscaler.rpcservice.TombstonedNodeManagerImpl;
import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.genius.datastoreutils.testutils.JobCoordinatorTestModule;
import org.opendaylight.infrautils.inject.guice.testutils.GuiceRule;
import org.opendaylight.infrautils.testutils.LogRule;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.mdsal.common.api.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.cloudscaler.rpcs.rev171220.ComputeNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.cloudscaler.rpcs.rev171220.ScaleinComputesRecoverInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.cloudscaler.rpcs.rev171220.ScaleinComputesRecoverOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.cloudscaler.rpcs.rev171220.ScaleinComputesStartInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.cloudscaler.rpcs.rev171220.ScaleinComputesStartOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.cloudscaler.rpcs.rev171220.compute.nodes.ComputeNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.cloudscaler.rpcs.rev171220.compute.nodes.ComputeNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.cloudscaler.rpcs.rev171220.compute.nodes.ComputeNodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.BridgeRefInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge.ref.info.BridgeRefEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge.ref.info.BridgeRefEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge.ref.info.BridgeRefEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeOtherConfigsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudScalerServiceTest {

    private static final Logger LOG = LoggerFactory.getLogger(CloudScalerServiceTest.class);

    private static final String NODEID1  = "ovsdb://uuid/2de70a99-29a5-4f2a-b87e-96d6ed57e411/bridge/br-int";
    private static final String NODEID2  = "ovsdb://uuid/2de70a99-29a5-4f2a-b87e-96d6ed57e412/bridge/br-int";
    private static final String NODEID3  = "ovsdb://uuid/2de70a99-29a5-4f2a-b87e-96d6ed57e413/bridge/br-int";

    private static final Uint64 DPN1 = Uint64.ONE;
    private static final Uint64 DPN2 = Uint64.valueOf(2);
    private static final Uint64 DPN3 = Uint64.valueOf(3);
    private static final Uint64 DPN4 = Uint64.valueOf(4);

    private static final String DPN1_DATAPATHID = new String("00:00:00:00:00:00:00:01");
    private static final String DPN2_DATAPATHID = new String("00:00:00:00:00:00:00:02");
    private static final String DPN3_DATAPATHID = new String("00:00:00:00:00:00:00:03");
    private static final String DPN4_DATAPATHID = new String("00:00:00:00:00:00:00:04");

    private static final String COMPUTE1 = new String("COMPUTE1");
    private static final String COMPUTE2 = new String("COMPUTE2");
    private static final String COMPUTE3 = new String("COMPUTE3");

    private static final InstanceIdentifier<BridgeRefEntry> DPN1_BRIDGE_REF
        = InstanceIdentifier.builder(BridgeRefInfo.class)
        .child(BridgeRefEntry.class, new BridgeRefEntryKey(DPN1)).build();

    private static final InstanceIdentifier<BridgeRefEntry> DPN2_BRIDGE_REF
        = InstanceIdentifier.builder(BridgeRefInfo.class)
        .child(BridgeRefEntry.class, new BridgeRefEntryKey(DPN2)).build();

    private static final InstanceIdentifier<BridgeRefEntry> DPN3_BRIDGE_REF
        = InstanceIdentifier.builder(BridgeRefInfo.class)
        .child(BridgeRefEntry.class, new BridgeRefEntryKey(DPN3)).build();

    private static ConditionFactory AWAITER = Awaitility.await("TestableListener")
        .atMost(30, TimeUnit.SECONDS)
        .pollInterval(100, TimeUnit.MILLISECONDS);

    public @Rule LogRule logRule = new LogRule();
    public @Rule MethodRule guice = new GuiceRule(CloudScalerServiceTestModule.class, JobCoordinatorTestModule.class);

    private @Inject CloudscalerRpcServiceImpl scaleInRpcManager;
    private @Inject TombstonedNodeManagerImpl tombstonedNodeManager;
    private @Inject SingleTransactionDataBroker dataBroker;
    private @Inject ComputeNodeManager computeNodeManager;

    public CloudScalerServiceTest() {
    }

    private InstanceIdentifier<ComputeNode> buildComputeNodeIid(String computeName) {
        return InstanceIdentifier.builder(ComputeNodes.class)
                .child(ComputeNode.class, new ComputeNodeKey(computeName))
                .build();
    }

    private InstanceIdentifier<Node> buildNodeId(String nodeId) {
        return InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId("ovsdb:1")))
                .child(Node.class, new NodeKey(new NodeId(nodeId)));
    }

    private Node buildNode(String nodeId, String computeName, String datapathid) {
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(new NodeId(nodeId));
        OvsdbBridgeAugmentationBuilder bridge = new OvsdbBridgeAugmentationBuilder();
        BridgeOtherConfigsBuilder otherConfigsBuilder = new BridgeOtherConfigsBuilder();
        otherConfigsBuilder.setBridgeOtherConfigKey("dp-desc");
        otherConfigsBuilder.setBridgeOtherConfigValue(computeName);
        bridge.setDatapathId(new DatapathId(datapathid));
        bridge.setBridgeOtherConfigs(Lists.newArrayList(otherConfigsBuilder.build()));
        nodeBuilder.addAugmentation(OvsdbBridgeAugmentation.class, bridge.build());
        return nodeBuilder.build();
    }

    private InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node>
        buildOpenflowNodeIid(Uint64 dpnid) {
        return InstanceIdentifier.builder(Nodes.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class,
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey(
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId(
                                "openflow:" + dpnid.toString()))).build();
    }

    private org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node buildOpenflowNode(
            Uint64 dpnId) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder nodeBuilder
                = new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder();
        nodeBuilder.setId(new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId(
                "openflow:" + dpnId.toString()));
        return nodeBuilder.build();
    }

    @Before public void before() throws TransactionCommitFailedException {
        dataBroker.syncWrite(OPERATIONAL, DPN1_BRIDGE_REF, buildBridgeRefEntry(DPN1, NODEID1));
        dataBroker.syncWrite(OPERATIONAL, DPN2_BRIDGE_REF, buildBridgeRefEntry(DPN2, NODEID2));
        dataBroker.syncWrite(OPERATIONAL, DPN3_BRIDGE_REF, buildBridgeRefEntry(DPN3, NODEID3));

        dataBroker.syncWrite(CONFIGURATION, buildNodeId(NODEID1), buildNode(NODEID1, COMPUTE1, DPN1_DATAPATHID));
        dataBroker.syncWrite(CONFIGURATION, buildNodeId(NODEID2), buildNode(NODEID2, COMPUTE2, DPN2_DATAPATHID));
        dataBroker.syncWrite(CONFIGURATION, buildNodeId(NODEID3), buildNode(NODEID3, COMPUTE3, DPN3_DATAPATHID));

        dataBroker.syncWrite(CONFIGURATION, buildOpenflowNodeIid(DPN1), buildOpenflowNode(DPN1));
        dataBroker.syncWrite(CONFIGURATION, buildOpenflowNodeIid(DPN2), buildOpenflowNode(DPN2));
        dataBroker.syncWrite(CONFIGURATION, buildOpenflowNodeIid(DPN3), buildOpenflowNode(DPN3));
    }

    @Test public void testBridgeAdd()
            throws ExecutionException, InterruptedException, TransactionCommitFailedException {

        dataBroker.syncWrite(OPERATIONAL, buildNodeId(NODEID1), buildNode(NODEID1, COMPUTE1, DPN1_DATAPATHID));
        dataBroker.syncWrite(OPERATIONAL, buildNodeId(NODEID2), buildNode(NODEID2, COMPUTE2, DPN2_DATAPATHID));
        dataBroker.syncWrite(OPERATIONAL, buildNodeId(NODEID3), buildNode(NODEID3, COMPUTE3, DPN3_DATAPATHID));

        AWAITER.until(() -> dataBroker.syncReadOptional(CONFIGURATION, buildComputeNodeIid(COMPUTE1)).isPresent());
    }

    @Test public void testScaleinComputesStartRpc()
            throws ExecutionException, InterruptedException, TransactionCommitFailedException {
        testBridgeAdd();
        ListenableFuture<RpcResult<ScaleinComputesStartOutput>> ft = scaleInRpcManager.scaleinComputesStart(
                new ScaleinComputesStartInputBuilder().setScaleinComputeNames(
                        Lists.newArrayList(COMPUTE1, COMPUTE2)).build());
        assertTrue("Scalein computes start rpc should return success code ", ft.get().isSuccessful());

        AWAITER.until(() -> dataBroker.syncReadOptional(
                CONFIGURATION, buildComputeNodeIid(COMPUTE1)).get().isTombstoned());
        AWAITER.until(() -> dataBroker.syncReadOptional(
                CONFIGURATION, buildComputeNodeIid(COMPUTE2)).get().isTombstoned());
    }

    private ComputeNode buildComputeNode(String nodeid1, BigInteger dpn1, String compute1) {
        return new ComputeNodeBuilder()
                .setComputeName(compute1)
                .setDpnid(dpn1)
                .setNodeid(nodeid1)
                .build();
    }

    @Test public void testScaleinComputesRecoverRpc()
            throws ExecutionException, InterruptedException, TransactionCommitFailedException {

        testScaleinComputesStartRpc();

        ListenableFuture<RpcResult<ScaleinComputesRecoverOutput>> ft = scaleInRpcManager.scaleinComputesRecover(
                new ScaleinComputesRecoverInputBuilder().setRecoverComputeNames(Lists.newArrayList(COMPUTE1)).build());
        assertTrue("scalein computes recover rpc should return success code", ft.get().isSuccessful());

        AWAITER.until(() -> !dataBroker.syncReadOptional(
                CONFIGURATION, buildComputeNodeIid(COMPUTE1)).get().isTombstoned());
        AWAITER.until(() -> dataBroker.syncReadOptional(
                CONFIGURATION, buildComputeNodeIid(COMPUTE2)).get().isTombstoned());
    }

    @Test public void testIsDpnTombstonedApi()
            throws ExecutionException, InterruptedException, TransactionCommitFailedException {

        testScaleinComputesStartRpc();
        assertTrue("Dpn 1 should be marked as tombstoned", tombstonedNodeManager.isDpnTombstoned(DPN1));
        assertTrue("Dpn 2 should be marked as tombstoned", tombstonedNodeManager.isDpnTombstoned(DPN2));
    }

    @Test public void testfilterTombstoned()
            throws ExecutionException, InterruptedException, TransactionCommitFailedException, ReadFailedException {

        testScaleinComputesStartRpc();
        List<Uint64> filtered = tombstonedNodeManager.filterTombStoned(Lists.newArrayList(DPN1, DPN2, DPN3, DPN4));
        assertTrue("Dpn 1 and 2 should be filtered ",
                Sets.difference(Sets.newHashSet(DPN3, DPN4), Sets.newHashSet(filtered)).isEmpty());
    }

    @Test public void testRecoveryCallback()
            throws ExecutionException, InterruptedException, TransactionCommitFailedException {

        Set<Uint64> nodesRecoverd = new HashSet<>();
        tombstonedNodeManager.addOnRecoveryCallback((dpnId) -> {
            nodesRecoverd.add(dpnId);
            return null;
        });
        testScaleinComputesRecoverRpc();
        AWAITER.until(() -> nodesRecoverd.contains(DPN1));
    }

    private BridgeRefEntry buildBridgeRefEntry(Uint64 dpnId, String nodeId) {
        return new BridgeRefEntryBuilder()
            .setDpid(dpnId)
            .setBridgeReference(new OvsdbBridgeRef(buildNodeIid(nodeId)))
            .build();
    }

    public static InstanceIdentifier<Node> buildNodeIid(String nodeId) {
        return InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(ScaleInConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(new NodeId(nodeId))).build();
    }
}
