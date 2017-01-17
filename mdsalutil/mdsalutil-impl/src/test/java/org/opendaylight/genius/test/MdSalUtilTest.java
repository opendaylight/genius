/*
 * Copyright © 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.genius.mdsalutil.actions.ActionOutput;
import org.opendaylight.genius.mdsalutil.actions.ActionPuntToController;
import org.opendaylight.genius.mdsalutil.actions.ActionPushVlan;
import org.opendaylight.genius.mdsalutil.actions.ActionSetFieldVlanVid;
import org.opendaylight.genius.mdsalutil.instructions.InstructionWriteActions;
import org.opendaylight.genius.mdsalutil.matches.MatchTunnelId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.FlowEntity;
import org.opendaylight.genius.mdsalutil.GroupEntity;
import org.opendaylight.genius.mdsalutil.BucketInfo;
import org.opendaylight.genius.mdsalutil.InstructionInfo;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.MatchInfo;
import org.opendaylight.genius.mdsalutil.internal.MDSALManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
//@RunWith(PowerMockRunner.class)
@PrepareForTest(MDSALUtil.class)
public class MdSalUtilTest extends AbstractDataBrokerTest {
     DataBroker dataBroker;
     @Mock PacketProcessingService ppS ;
     MDSALManager mdSalMgr = null ;
     MockFlowForwarder flowFwder = null ;
     MockGroupForwarder grpFwder = null ;
     private static final String Nodeid = "openflow:1";

     @Before
        public void setUp() throws Exception {
            dataBroker = getDataBroker() ;
            mdSalMgr = new MDSALManager( dataBroker, ppS);
            flowFwder = new MockFlowForwarder( dataBroker );
            grpFwder = new MockGroupForwarder( dataBroker ) ;

            PowerMockito.mockStatic(MDSALUtil.class) ;

            NodeKey s1Key = new NodeKey(new NodeId("openflow:1"));
            addFlowCapableNode(s1Key);
        }

        @Test
        public void testInstallFlow() {
            String dpnId = "openflow:1";
            String tableId1 = "12";

            //Install Flow 1
            FlowEntity testFlow1 = createFlowEntity(dpnId, tableId1) ;
            mdSalMgr.installFlow(testFlow1);
            assertEquals(1, flowFwder.getDataChgCount());

            // Install FLow 2
            String tableId2 = "13" ;
             FlowEntity testFlow2 = createFlowEntity(dpnId, tableId2) ;
             mdSalMgr.installFlow(testFlow2);
             assertEquals(2, flowFwder.getDataChgCount());
        }

        @Test
        public void testRemoveFlow() {
            String dpnId = "openflow:1";
            String tableId = "13" ;
            FlowEntity testFlow = createFlowEntity(dpnId, tableId) ;

            // To test RemoveFlow add and then delete Flows
            mdSalMgr.installFlow(testFlow) ;
            assertEquals(1, flowFwder.getDataChgCount());
            mdSalMgr.removeFlow(testFlow);
            assertEquals(0, flowFwder.getDataChgCount());
        }

        @Test
        public void testInstallGroup() {
            // Install Group 1
            String inport = "2" ;
            int vlanid = 100 ;
            GroupEntity grpEntity1 = createGroupEntity(Nodeid, inport, vlanid) ;

             mdSalMgr.installGroup(grpEntity1);
             assertEquals(1, grpFwder.getDataChgCount());

             // Install Group 2
                inport = "3" ;
                vlanid = 100 ;
                GroupEntity grpEntity2 = createGroupEntity(Nodeid, inport, vlanid) ;
                mdSalMgr.installGroup(grpEntity2);
                assertEquals(2, grpFwder.getDataChgCount());
        }

        @Test
        public void testRemoveGroup() {
            String inport = "2" ;
            int vlanid = 100 ;
            GroupEntity grpEntity = createGroupEntity(Nodeid, inport, vlanid) ;
            // To test RemoveGroup  add and then delete Group
            mdSalMgr.installGroup(grpEntity);
            assertEquals(1, grpFwder.getDataChgCount());
            mdSalMgr.removeGroup(grpEntity);
            assertEquals(0, grpFwder.getDataChgCount());
        }

        public void addFlowCapableNode(NodeKey nodeKey) throws ExecutionException, InterruptedException {
            Nodes nodes = new NodesBuilder().setNode(Collections.emptyList()).build();
            InstanceIdentifier<Node> flowNodeIdentifier = InstanceIdentifier.create(Nodes.class)
                    .child(Node.class, nodeKey);

            FlowCapableNodeBuilder fcnBuilder = new FlowCapableNodeBuilder();
            NodeBuilder nodeBuilder = new NodeBuilder();
            nodeBuilder.setKey(nodeKey);
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

            BigInteger dpId;
            int SERVICE_ID = 0;
            FlowEntity terminatingServiceTableFlowEntity = null;

            List<ActionInfo> listActionInfo = new ArrayList<>();
            listActionInfo.add(new ActionPuntToController());

            try {
                dpId = new BigInteger(dpnId.split(":")[1]);

                List<MatchInfo> mkMatches = new ArrayList<>();
                BigInteger COOKIE = new BigInteger("9000000", 16);

                short s_tableId = Short.parseShort(tableId) ;

                mkMatches.add(new MatchTunnelId(new BigInteger("0000000000000000", 16)));

                List<InstructionInfo> mkInstructions = new ArrayList<>();
                mkInstructions.add(new InstructionWriteActions(listActionInfo));

                terminatingServiceTableFlowEntity = MDSALUtil
                        .buildFlowEntity(
                                dpId,
                                s_tableId,
                                getFlowRef(s_tableId,
                                        SERVICE_ID), 5, "Terminating Service Flow Entry: " + SERVICE_ID,
                                0, 0, COOKIE
                                        .add(BigInteger.valueOf(SERVICE_ID)),
                                        null, null);
                } catch (Exception e) {
                    //throw new Exception(e) ;
              }

            return terminatingServiceTableFlowEntity;
        }

        private String getFlowRef(short termSvcTable, int svcId) {
            return String.valueOf(termSvcTable) + svcId;
        }

        public GroupEntity createGroupEntity(String Nodeid, String inport, int vlanid) {
            GroupEntity groupEntity;
            long id = getUniqueValue(Nodeid, inport);
            List<BucketInfo> listBucketInfo = new ArrayList<>();
            List<ActionInfo> listActionInfo = new ArrayList<>();
            if (vlanid > 0) {
                listActionInfo.add(new ActionPushVlan());
                listActionInfo.add(new ActionSetFieldVlanVid(vlanid));
            }
            listActionInfo.add(new ActionOutput(new Uri(inport), 65535));
            listBucketInfo.add(new BucketInfo(listActionInfo));

            String groupName = "Test Group";
            BigInteger dpnId = new BigInteger(Nodeid.split(":")[1]);
            groupEntity = MDSALUtil.buildGroupEntity(dpnId, id, groupName, GroupTypes.GroupIndirect,
                    listBucketInfo);

            return groupEntity;
        }

        private static long getUniqueValue(String nodeId, String inport) {

            Long nodeIdL = Long.valueOf(nodeId.split(":")[1]);
            Long inportL = Long.valueOf(inport);
                long sd_set;
                sd_set = nodeIdL * 10 + inportL;

                return sd_set;
    }

}
