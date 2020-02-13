/*
 * Copyright © 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.internal;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.BucketInfo;
import org.opendaylight.genius.mdsalutil.FlowEntity;
import org.opendaylight.genius.mdsalutil.GroupEntity;
import org.opendaylight.genius.mdsalutil.InstructionInfo;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.MatchInfo;
import org.opendaylight.genius.mdsalutil.actions.ActionOutput;
import org.opendaylight.genius.mdsalutil.actions.ActionPuntToController;
import org.opendaylight.genius.mdsalutil.actions.ActionPushVlan;
import org.opendaylight.genius.mdsalutil.actions.ActionSetFieldVlanVid;
import org.opendaylight.genius.mdsalutil.instructions.InstructionWriteActions;
import org.opendaylight.genius.mdsalutil.matches.MatchTunnelId;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint64;

@RunWith(MockitoJUnitRunner.class)
public class MdSalUtilTest extends AbstractConcurrentDataBrokerTest {

    DataBroker dataBroker;
    @Mock
    PacketProcessingService ppS;
    MDSALManager mdSalMgr = null;
    MockFlowForwarder flowFwder = null;
    MockGroupForwarder grpFwder = null;
    private static final String NODE_ID = "openflow:1";

    @Before
    public void setUp() throws Exception {
        dataBroker = getDataBroker();
        mdSalMgr = new MDSALManager(dataBroker, ppS);
        flowFwder = new MockFlowForwarder(dataBroker);
        grpFwder = new MockGroupForwarder(dataBroker);

        NodeKey s1Key = new NodeKey(new NodeId("openflow:1"));
        addFlowCapableNode(s1Key);
    }

    @Test
    public void testInstallFlow() throws Exception {
        String dpnId = "openflow:1";
        String tableId1 = "12";

        // Install Flow 1
        FlowEntity testFlow1 = createFlowEntity(dpnId, tableId1);
        mdSalMgr.installFlowInternal(testFlow1).get();
        flowFwder.awaitDataChangeCount(1);

        // Install FLow 2
        String tableId2 = "13";
        FlowEntity testFlow2 = createFlowEntity(dpnId, tableId2);
        mdSalMgr.installFlowInternal(testFlow2).get();
        flowFwder.awaitDataChangeCount(2);
    }

    @Test
    public void testRemoveFlow() throws Exception {
        String dpnId = "openflow:1";
        String tableId = "13";
        FlowEntity testFlow = createFlowEntity(dpnId, tableId);

        // To test RemoveFlow add and then delete Flows
        mdSalMgr.installFlowInternal(testFlow).get();
        flowFwder.awaitDataChangeCount(1);
        mdSalMgr.removeFlowInternal(testFlow).get();
        flowFwder.awaitDataChangeCount(0);
    }

    @Test
    public void testInstallGroup() throws Exception {
        // Install Group 1
        String inport = "2";
        int vlanid = 100;
        GroupEntity grpEntity1 = createGroupEntity(NODE_ID, inport, vlanid);

        mdSalMgr.installGroupInternal(grpEntity1).get();
        grpFwder.awaitDataChangeCount(1);

        // Install Group 2
        inport = "3";
        vlanid = 100;
        GroupEntity grpEntity2 = createGroupEntity(NODE_ID, inport, vlanid);
        mdSalMgr.installGroupInternal(grpEntity2).get();
        grpFwder.awaitDataChangeCount(2);
    }

    @Test
    public void testRemoveGroup() throws Exception {
        String inport = "2";
        int vlanid = 100;
        GroupEntity grpEntity = createGroupEntity(NODE_ID, inport, vlanid);
        // To test RemoveGroup add and then delete Group
        mdSalMgr.installGroupInternal(grpEntity).get();
        grpFwder.awaitDataChangeCount(1);
        mdSalMgr.removeGroupInternal(grpEntity.getDpnId(), grpEntity.getGroupId()).get();
        grpFwder.awaitDataChangeCount(0);
    }

    public void addFlowCapableNode(NodeKey nodeKey) throws ExecutionException, InterruptedException {
        Nodes nodes = new NodesBuilder().setNode(Collections.emptyList()).build();
        final InstanceIdentifier<Node> flowNodeIdentifier = InstanceIdentifier.create(Nodes.class).child(Node.class,
                nodeKey);

        FlowCapableNodeBuilder fcnBuilder = new FlowCapableNodeBuilder();
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.withKey(nodeKey);
        nodeBuilder.addAugmentation(FlowCapableNode.class, fcnBuilder.build());

        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(Nodes.class), nodes);
        writeTx.put(LogicalDatastoreType.OPERATIONAL, flowNodeIdentifier, nodeBuilder.build());
        writeTx.put(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(Nodes.class), nodes);
        writeTx.put(LogicalDatastoreType.CONFIGURATION, flowNodeIdentifier, nodeBuilder.build());
        assertCommit(writeTx.submit());
    }

    // Methods to test the install Flow and Group
    public FlowEntity createFlowEntity(String dpnId, String tableId) {
        Uint64 dpId;
        final int serviceId = 0;

        List<ActionInfo> listActionInfo = new ArrayList<>();
        listActionInfo.add(new ActionPuntToController());

        dpId = Uint64.valueOf(dpnId.split(":")[1]);

        List<MatchInfo> mkMatches = new ArrayList<>();
        final BigInteger cookie = new BigInteger("9000000", 16);

        short shortTableId = Short.parseShort(tableId);

        mkMatches.add(new MatchTunnelId(Uint64.ZERO));

        List<InstructionInfo> mkInstructions = new ArrayList<>();
        mkInstructions.add(new InstructionWriteActions(listActionInfo));

        FlowEntity terminatingServiceTableFlowEntity = MDSALUtil.buildFlowEntity(dpId, shortTableId,
                getFlowRef(shortTableId, serviceId), 5, "Terminating Service Flow Entry: " + serviceId, 0, 0,
                Uint64.valueOf(cookie.add(BigInteger.valueOf(serviceId))), null, null);

        return terminatingServiceTableFlowEntity;
    }

    private String getFlowRef(short termSvcTable, int svcId) {
        return String.valueOf(termSvcTable) + svcId;
    }

    public GroupEntity createGroupEntity(String nodeid, String inport, int vlanid) {
        List<BucketInfo> listBucketInfo = new ArrayList<>();
        List<ActionInfo> listActionInfo = new ArrayList<>();
        if (vlanid > 0) {
            listActionInfo.add(new ActionPushVlan());
            listActionInfo.add(new ActionSetFieldVlanVid(vlanid));
        }
        listActionInfo.add(new ActionOutput(new Uri(inport), 65535));
        listBucketInfo.add(new BucketInfo(listActionInfo));

        String groupName = "Test Group";
        Uint64 dpnId = Uint64.valueOf(nodeid.split(":")[1]);

        long id = getUniqueValue(nodeid, inport);
        return MDSALUtil.buildGroupEntity(dpnId, id, groupName, GroupTypes.GroupIndirect, listBucketInfo);
    }

    private static long getUniqueValue(String nodeId, String inport) {
        Long nodeIdL = Long.valueOf(nodeId.split(":")[1]);
        Long inportL = Long.valueOf(inport);
        long sdSet = nodeIdL * 10 + inportL;

        return sdSet;
    }
}
