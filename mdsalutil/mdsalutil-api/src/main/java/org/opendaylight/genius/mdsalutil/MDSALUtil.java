/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.mdsalutil;

import com.google.common.base.Optional;
import com.google.common.net.InetAddresses;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.controller.liblldp.HexEncode;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.genius.mdsalutil.actions.ActionDrop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopVlanActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.pop.vlan.action._case.PopVlanActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.field._case.SetFieldBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Instructions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.GoToTableCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.WriteActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.WriteActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.WriteMetadataCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.go.to.table._case.GoToTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.write.actions._case.WriteActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.write.actions._case.WriteActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.write.metadata._case.WriteMetadataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.BucketId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.Buckets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.BucketsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.Bucket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.BucketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.BucketKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.TunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxOfInPortCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxRegCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegLoadNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.NxRegLoad;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.NxRegLoadBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.nx.reg.load.Dst;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.nx.reg.load.DstBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInputBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This class needs to be mocked
@SuppressWarnings({ "checkstyle:AbbreviationAsWordInName", "checkstyle:FinalClass" })
public class MDSALUtil {

    private static final Logger LOG = LoggerFactory.getLogger(MDSALUtil.class);

    public enum MdsalOp {  CREATION_OP, UPDATE_OP, REMOVAL_OP }

    public static final String NODE_PREFIX = "openflow";
    public static final int GROUP_WEIGHT = 0;
    public static final long WATCH_PORT = 0xffffffffL;
    public static final long WATCH_GROUP = 0xffffffffL;
    public static final String SEPARATOR = ":";
    private static final Buckets EMPTY_BUCKETS = new BucketsBuilder().build();
    private static final Instructions EMPTY_INSTRUCTIONS = new InstructionsBuilder().setInstruction(
            new ArrayList<>()).build();
    private static final Match EMPTY_MATCHES = new MatchBuilder().build();

    private MDSALUtil() { }

    public static FlowEntity buildFlowEntity(BigInteger dpnId, short tableId, String flowId, int priority,
            String flowName, int idleTimeOut, int hardTimeOut, BigInteger cookie,
            List<? extends MatchInfoBase> listMatchInfoBase, List<InstructionInfo> listInstructionInfo) {

        FlowEntityBuilder builder = new FlowEntityBuilder()
            .setDpnId(dpnId)
            .setTableId(tableId)
            .setFlowId(flowId)
            .setPriority(priority)
            .setFlowName(flowName)
            .setIdleTimeOut(idleTimeOut)
            .setHardTimeOut(hardTimeOut)
            .setCookie(cookie);
        if (listMatchInfoBase != null) {
            builder.addAllMatchInfoList(listMatchInfoBase);
        }
        if (listInstructionInfo != null) {
            builder.addAllInstructionInfoList(listInstructionInfo);
        }
        return builder.build();
    }

    // TODO: CHECK IF THIS IS USED
    public static Flow buildFlow(short tableId, String flowId, int priority, String flowName, int idleTimeOut,
            int hardTimeOut, BigInteger cookie, List<? extends MatchInfoBase> listMatchInfoBase,
            List<InstructionInfo> listInstructionInfo) {
        return MDSALUtil.buildFlow(tableId, flowId, priority, flowName, idleTimeOut, hardTimeOut, cookie,
                listMatchInfoBase, listInstructionInfo, true);
    }

    public static Flow buildFlow(short tableId, String flowId, int priority, String flowName, int idleTimeOut,
            int hardTimeOut, BigInteger cookie, List<? extends MatchInfoBase>  listMatchInfoBase,
            List<InstructionInfo> listInstructionInfo, boolean isStrict) {
        FlowKey key = new FlowKey(new FlowId(flowId));
        return new FlowBuilder().setMatch(buildMatches(listMatchInfoBase)).setKey(key)
                .setPriority(priority).setInstructions(buildInstructions(listInstructionInfo))
                .setBarrier(false).setInstallHw(true).setHardTimeout(hardTimeOut).setIdleTimeout(idleTimeOut)
                .setFlowName(flowName).setTableId(tableId).setStrict(isStrict)
                .setCookie(new FlowCookie(cookie)).build();
    }

    public static Flow buildFlow(short tableId, String flowId) {
        return new FlowBuilder().setTableId(tableId).setId(new FlowId(flowId)).build();
    }

    public static Flow buildFlowNew(short tableId, String flowId, int priority, String flowName, int idleTimeOut,
            int hardTimeOut, BigInteger cookie, List<? extends MatchInfoBase> listMatchInfoBase,
            List<Instruction> listInstructionInfo) {
        return MDSALUtil.buildFlowNew(tableId, flowId, priority, flowName, idleTimeOut, hardTimeOut, cookie,
                listMatchInfoBase, listInstructionInfo, true);
    }

    private static Flow buildFlowNew(short tableId, String flowId, int priority, String flowName, int idleTimeOut,
                                  int hardTimeOut, BigInteger cookie, List<? extends MatchInfoBase>  listMatchInfoBase,
                                  List<Instruction> listInstructionInfo, boolean isStrict) {
        FlowKey key = new FlowKey(new FlowId(flowId));
        return new FlowBuilder().setMatch(buildMatches(listMatchInfoBase)).setKey(key)
                .setPriority(priority)
                .setInstructions(new InstructionsBuilder().setInstruction(listInstructionInfo).build())
                .setBarrier(false).setInstallHw(true).setHardTimeout(hardTimeOut).setIdleTimeout(idleTimeOut)
                .setFlowName(flowName).setTableId(tableId).setStrict(isStrict)
                .setCookie(new FlowCookie(cookie)).build();
    }

    public static GroupEntity buildGroupEntity(BigInteger dpnId, long groupId, String groupName, GroupTypes groupType,
            List<BucketInfo> listBucketInfo) {

        GroupEntityBuilder groupEntity = new GroupEntityBuilder();
        groupEntity.setDpnId(dpnId);
        groupEntity.setGroupId(groupId);
        groupEntity.setGroupName(groupName);
        groupEntity.setGroupType(groupType);
        groupEntity.setBucketInfoList(listBucketInfo);
        return groupEntity.build();
    }

    public static Group buildGroup(long groupId, String groupName, GroupTypes groupType, Buckets buckets) {
        GroupId groupIdentifier = new GroupId(groupId);
        return new GroupBuilder().setGroupId(groupIdentifier).setKey(new GroupKey(groupIdentifier))
                .setGroupName(groupName).setGroupType(groupType).setBuckets(buckets).build();
    }

    public static TransmitPacketInput getPacketOutDefault(List<ActionInfo> actionInfos, byte[] payload,
            BigInteger dpnId) {
        return new TransmitPacketInputBuilder()
                .setAction(buildActions(actionInfos))
                .setPayload(payload)
                .setNode(
                        new NodeRef(InstanceIdentifier.builder(Nodes.class)
                                .child(Node.class, new NodeKey(new NodeId("openflow:" + dpnId))).toInstance()))
                .setIngress(getDefaultNodeConnRef(dpnId)).setEgress(getDefaultNodeConnRef(dpnId)).build();
    }

    public static TransmitPacketInput getPacketOutFromController(List<ActionInfo> actionInfos, byte[] payload,
            long dpnId, NodeConnectorRef egress) {
        return new TransmitPacketInputBuilder()
                .setAction(buildActions(actionInfos))
                .setPayload(payload)
                .setNode(
                        new NodeRef(InstanceIdentifier.builder(Nodes.class)
                                .child(Node.class, new NodeKey(new NodeId("openflow:" + dpnId))).toInstance()))
                .setEgress(egress).build();
    }

    public static TransmitPacketInput getPacketOut(List<ActionInfo> actionInfos, byte[] payload, long dpnId,
            NodeConnectorRef ingress) {
        return new TransmitPacketInputBuilder()
                .setAction(buildActions(actionInfos))
                .setPayload(payload)
                .setNode(
                        new NodeRef(InstanceIdentifier.builder(Nodes.class)
                                .child(Node.class, new NodeKey(new NodeId("openflow:" + dpnId))).toInstance()))
                .setIngress(ingress).setEgress(ingress).build();
    }

    public static TransmitPacketInput getPacketOut(List<ActionInfo> actionInfos, byte[] payload, BigInteger dpnId,
            NodeConnectorRef nodeConnRef) {
        // TODO Auto-generated method stub
        return null;
    }

    public static TransmitPacketInput getPacketOut(List<Action> actions, byte[] payload, BigInteger dpnId) {
        NodeConnectorRef ncRef = getDefaultNodeConnRef(dpnId);
        return new TransmitPacketInputBuilder()
                .setAction(actions)
                .setPayload(payload)
                .setNode(
                        new NodeRef(InstanceIdentifier.builder(Nodes.class)
                                .child(Node.class, new NodeKey(new NodeId("openflow:" + dpnId))).toInstance()))
                .setIngress(ncRef).setEgress(ncRef).build();
    }

    public static Action retrieveSetTunnelIdAction(BigInteger tunnelId, int actionKey) {
        return new ActionBuilder().setAction(
                new SetFieldCaseBuilder().setSetField(new SetFieldBuilder().setTunnel(new TunnelBuilder()
                .setTunnelId(tunnelId).build()).build())
                .build()).setKey(new ActionKey(actionKey)).build();
    }

    public static List<Action> buildActions(List<ActionInfo> actions) {
        List<Action> actionsList = new ArrayList<>();
        for (ActionInfo actionInfo : actions) {
            actionsList.add(actionInfo.buildAction());
        }
        return actionsList;
    }

    public static String longToIp(long ip, long mask) {
        return ((ip & 0xFF000000) >> 3 * 8) + "."
               + ((ip & 0x00FF0000) >> 2 * 8) + "."
               + ((ip & 0x0000FF00) >>     8) + "."
               + (ip & 0x000000FF)
               + (mask == 0 ? "" : "/" + mask);
    }

    public static BigInteger getBigIntIpFromIpAddress(IpAddress ipAddr) {
        String ipString = ipAddr.getIpv4Address().getValue();
        int ipInt = InetAddresses.coerceToInteger(InetAddresses.forString(ipString));
        return BigInteger.valueOf(ipInt & 0xffffffffL);
    }


    public static Bucket buildBucket(List<Action> actionsList, int weight, int bucketId, long watchPort,
            long watchGroup) {
        return new BucketBuilder().setAction(actionsList).setWeight(weight).setWatchGroup(watchGroup)
                .setWatchPort(watchPort).setBucketId(new BucketId(Long.valueOf(bucketId)))
                .setKey(new BucketKey(new BucketId(Long.valueOf(bucketId)))).build();
    }

    public static Buckets buildBucketLists(List<Bucket> bucketList) {
        return new BucketsBuilder().setBucket(bucketList).build();
    }

    protected static Buckets buildBuckets(List<BucketInfo> listBucketInfo) {
        long index = 0;
        if (listBucketInfo != null) {
            BucketsBuilder bucketsBuilder = new BucketsBuilder();
            List<Bucket> bucketList = new ArrayList<>();

            for (BucketInfo bucketInfo : listBucketInfo) {
                BucketBuilder bucketBuilder = new BucketBuilder();
                bucketBuilder.setAction(bucketInfo.buildActions());
                bucketBuilder.setWeight(bucketInfo.getWeight());
                bucketBuilder.setBucketId(new BucketId(index++));
                bucketBuilder.setWeight(bucketInfo.getWeight()).setWatchPort(bucketInfo.getWatchPort())
                        .setWatchGroup(bucketInfo.getWatchGroup());
                bucketList.add(bucketBuilder.build());
            }

            bucketsBuilder.setBucket(bucketList);
            return bucketsBuilder.build();
        }

        return EMPTY_BUCKETS;
    }

    public static Instructions buildInstructions(List<InstructionInfo> listInstructionInfo) {
        if (listInstructionInfo != null) {
            List<Instruction> instructions = new ArrayList<>();
            int instructionKey = 0;

            for (InstructionInfo instructionInfo : listInstructionInfo) {
                instructions.add(instructionInfo.buildInstruction(instructionKey));
                instructionKey++;
            }

            return new InstructionsBuilder().setInstruction(instructions).build();
        }

        return EMPTY_INSTRUCTIONS;
    }

    public static Match buildMatches(List<? extends MatchInfoBase> listMatchInfoBase) {
        if (listMatchInfoBase != null) {
            MatchBuilder matchBuilder = new MatchBuilder();
            Map<Class<?>, Object> mapMatchBuilder = new HashMap<>();

            for (MatchInfoBase matchInfoBase : listMatchInfoBase) {
                matchInfoBase.createInnerMatchBuilder(mapMatchBuilder);
            }

            for (MatchInfoBase matchInfoBase : listMatchInfoBase) {
                matchInfoBase.setMatch(matchBuilder, mapMatchBuilder);
            }

            return matchBuilder.build();
        }

        return EMPTY_MATCHES;
    }

    // TODO: Check the port const
    public static NodeConnectorRef getDefaultNodeConnRef(BigInteger dpId) {
        return getNodeConnRef(NODE_PREFIX + SEPARATOR + dpId, "0xfffffffd");
    }

    public static NodeConnectorRef getNodeConnRef(BigInteger dpId, String port) {
        return getNodeConnRef(NODE_PREFIX + SEPARATOR + dpId, port);
    }

    public static NodeConnectorRef getNodeConnRef(String nodeId, String port) {
        StringBuilder sb = new StringBuilder();
        sb.append(nodeId);
        sb.append(SEPARATOR);
        sb.append(port);
        String nodeConnectorKeyAsString = sb.toString();
        NodeConnectorId nodeConnectorId = new NodeConnectorId(nodeConnectorKeyAsString);
        NodeConnectorKey nodeConnectorKey = new NodeConnectorKey(nodeConnectorId);

        NodeKey nodeKey = new NodeKey(new NodeId(nodeId));
        InstanceIdentifierBuilder<Node> nodeInstanceIdentifierBuilder
            = InstanceIdentifier.builder(Nodes.class).child(Node.class, nodeKey);
        InstanceIdentifierBuilder<NodeConnector> nodeConnectorInstanceIdentifierBuilder
            = nodeInstanceIdentifierBuilder.child(NodeConnector.class, nodeConnectorKey);
        InstanceIdentifier<NodeConnector> nodeConnectorInstanceIdentifier
            = nodeConnectorInstanceIdentifierBuilder.toInstance();
        NodeConnectorRef nodeConnectorRef = new NodeConnectorRef(nodeConnectorInstanceIdentifier);
        return nodeConnectorRef;
    }

    public static BigInteger getDpnIdFromNodeName(NodeId nodeId) {
        return getDpnIdFromNodeName(nodeId.getValue());
    }

    public static BigInteger getDpnIdFromNodeName(String mdsalNodeName) {
        String dpId = mdsalNodeName.substring(mdsalNodeName.lastIndexOf(':') + 1);
        return new BigInteger(dpId);
    }

    public static long getOfPortNumberFromPortName(NodeConnectorId nodeConnectorId) {
        return getOfPortNumberFromPortName(nodeConnectorId.getValue());
    }

    public static long getOfPortNumberFromPortName(String mdsalPortName) {
        String portNumber = mdsalPortName.substring(mdsalPortName.lastIndexOf(':') + 1);
        return Long.parseLong(portNumber);
    }

    public static long getDpnIdFromPortName(NodeConnectorId nodeConnectorId) {
        if (nodeConnectorId == null || nodeConnectorId.getValue() == null) {
            return -1;
        }
        try {
            String ofPortName = nodeConnectorId.getValue();
            return Long.parseLong(ofPortName.substring(ofPortName.indexOf(':') + 1,
                    ofPortName.lastIndexOf(':')));
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            LOG.error("NodeConnectorId not of expected format openflow:dpnid:portnum");
            return -1;
        }
    }

    public static BigInteger getDpnId(String datapathId) {
        if (datapathId != null) {
            String dpIdStr = datapathId.replace(":", "");
            BigInteger dpnId =  new BigInteger(dpIdStr, 16);
            return dpnId;
        }
        return null;
    }

    public static Instruction buildAndGetPopVlanActionInstruction(int actionKey, int instructionKey) {
        Action popVlanAction = new ActionBuilder().setAction(
                new PopVlanActionCaseBuilder().setPopVlanAction(new PopVlanActionBuilder().build()).build())
                .setKey(new ActionKey(actionKey)).build();
        List<Action> listAction = new ArrayList<>();
        listAction.add(popVlanAction);
        return buildApplyActionsInstruction(listAction, instructionKey);
    }


    /**
     * Create action to set REG6 to the given value.
     *
     * @param actionKey the action key.
     * @param startOffSet the start offset.
     * @param endOffSet the end offset.
     * @param value the value.
     * @return the action.
     */
    public static Action createSetReg6Action(int actionKey, int startOffSet, int endOffSet, long value) {
        NxRegLoadBuilder nxRegLoadBuilder = new NxRegLoadBuilder();
        Dst dst =  new DstBuilder()
                .setDstChoice(new DstNxRegCaseBuilder().setNxReg(NxmNxReg6.class).build())
                .setStart(startOffSet)
                .setEnd(endOffSet)
                .build();
        nxRegLoadBuilder.setDst(dst);
        nxRegLoadBuilder.setValue(new BigInteger(Long.toString(value)));
        ActionBuilder ab = new ActionBuilder();
        ab.setAction(new NxActionRegLoadNodesNodeTableFlowApplyActionsCaseBuilder()
                .setNxRegLoad(nxRegLoadBuilder.build()).build());
        ab.setKey(new ActionKey(actionKey));
        return ab.build();
    }

    public static Instruction buildAndGetSetReg6ActionInstruction(int actionKey, int instructionKey,
                                                                  int startOffSet, int endOffSet, long value) {
        return buildApplyActionsInstruction(
                Collections.singletonList(createSetReg6Action(actionKey, startOffSet, endOffSet, value)),
                instructionKey);
    }

    public static Instruction buildApplyActionsInstruction(List<Action> actions) {
        return buildApplyActionsInstruction(actions, 0);
    }

    public static Instruction buildApplyActionsInstruction(List<Action> listAction, int instructionKey) {
        ApplyActions applyActions = new ApplyActionsBuilder().setAction(listAction).build();
        ApplyActionsCase applyActionsCase = new ApplyActionsCaseBuilder().setApplyActions(applyActions).build();
        InstructionBuilder instructionBuilder = new InstructionBuilder();

        instructionBuilder.setInstruction(applyActionsCase);
        instructionBuilder.setKey(new InstructionKey(instructionKey));
        return instructionBuilder.build();
    }

    public static Instruction buildWriteActionsInstruction(List<Action> actions) {
        return buildWriteActionsInstruction(actions, 0);
    }

    /**
     * Build write actions instruction with the given actions and key.
     *
     * @param actions the actions.
     * @param instructionKey the instruction key.
     * @return the instruction.
     */
    public static Instruction buildWriteActionsInstruction(List<Action> actions, int instructionKey) {
        WriteActions writeActions = new WriteActionsBuilder().setAction(actions).build();
        WriteActionsCase writeActionsCase = new WriteActionsCaseBuilder().setWriteActions(writeActions).build();
        InstructionBuilder instructionBuilder = new InstructionBuilder();

        instructionBuilder.setInstruction(writeActionsCase);
        instructionBuilder.setKey(new InstructionKey(instructionKey));
        return instructionBuilder.build();
    }

    public static Instruction buildInstruction(Instruction instruction, int instructionKey) {
        return new InstructionBuilder(instruction).setKey(new InstructionKey(instructionKey)).build();
    }

    public static List<Instruction> buildInstructionsDrop() {
        return buildInstructionsDrop(0);
    }

    public static List<Instruction> buildInstructionsDrop(int instructionKey) {
        List<Instruction> mkInstructions = new ArrayList<>();
        List<Action> actionsInfos = new ArrayList<>();
        actionsInfos.add(new ActionDrop().buildAction());
        mkInstructions.add(getWriteActionsInstruction(actionsInfos, instructionKey));
        return mkInstructions;
    }

    /**
     * Build write actions instruction with the given actions and key.
     *
     * @param listAction the actions.
     * @param instructionKey the instruction key.
     * @return the instruction.
     * @deprecated Use buildWriteActionsInstruction
     */
    @Deprecated
    public static Instruction getWriteActionsInstruction(List<Action> listAction, int instructionKey) {
        return buildWriteActionsInstruction(listAction, instructionKey);
    }

    public static Instruction buildAndGetWriteMetadaInstruction(BigInteger metadata,
                                                                BigInteger mask, int instructionKey) {
        return new InstructionBuilder()
                .setInstruction(
                        new WriteMetadataCaseBuilder().setWriteMetadata(
                                new WriteMetadataBuilder().setMetadata(metadata).setMetadataMask(mask).build())
                                .build()).setKey(new InstructionKey(instructionKey)).build();
    }

    public static Instruction buildAndGetGotoTableInstruction(short tableId, int instructionKey) {
        return new InstructionBuilder()
            .setInstruction(
                new GoToTableCaseBuilder().setGoToTable(
                    new GoToTableBuilder().setTableId(tableId).build()).build())
            .setKey(new InstructionKey(instructionKey)).build();
    }

    /**
     * Deprecated read.
     * @deprecated Use {@link SingleTransactionDataBroker#syncReadOptionalAndTreatReadFailedExceptionAsAbsentOptional(
     * DataBroker, LogicalDatastoreType, InstanceIdentifier)}
     */
    @Deprecated
    public static <T extends DataObject> Optional<T> read(LogicalDatastoreType datastoreType,
                                                          InstanceIdentifier<T> path, DataBroker broker) {
        return SingleTransactionDataBroker
                .syncReadOptionalAndTreatReadFailedExceptionAsAbsentOptional(broker, datastoreType, path);
    }

    /**
     * Deprecated read.
     * @deprecated Use {@link SingleTransactionDataBroker#syncReadOptional(
     * DataBroker, LogicalDatastoreType, InstanceIdentifier)}
     */
    @Deprecated
    public static <T extends DataObject> Optional<T> read(DataBroker broker, LogicalDatastoreType datastoreType,
                                                          InstanceIdentifier<T> path) {
        try {
            return SingleTransactionDataBroker.syncReadOptional(broker, datastoreType, path);
        } catch (ReadFailedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Deprecated write.
     *
     * @deprecated Use
     *             {@link SingleTransactionDataBroker#syncWrite(
     *                     DataBroker, LogicalDatastoreType, InstanceIdentifier, DataObject)}
     */
    @Deprecated
    public static <T extends DataObject> void syncWrite(DataBroker broker,
                                                        LogicalDatastoreType datastoreType, InstanceIdentifier<T> path,
                                                        T data) {
        try {
            SingleTransactionDataBroker.syncWrite(broker, datastoreType, path, data);
        } catch (TransactionCommitFailedException e) {
            LOG.error("Error writing to datastore (path, data) : ({}, {})", path, data);
            throw new RuntimeException(e);
        }
    }

    /**
     * Deprecated update.
     *
     * @deprecated Use
     *             {@link SingleTransactionDataBroker#syncUpdate(
     *                          DataBroker, LogicalDatastoreType, InstanceIdentifier, DataObject)}
     */
    @Deprecated
    public static <T extends DataObject> void syncUpdate(DataBroker broker,
                                                         LogicalDatastoreType datastoreType, InstanceIdentifier<T> path,
                                                         T data) {
        try {
            SingleTransactionDataBroker.syncUpdate(broker, datastoreType, path, data);
        } catch (TransactionCommitFailedException e) {
            LOG.error("Error writing to datastore (path, data) : ({}, {})", path, data);
            throw new RuntimeException(e);
        }
    }

    /**
     * Deprecated delete.
     *
     * @deprecated Use
     *             {@link SingleTransactionDataBroker#syncDelete(DataBroker, LogicalDatastoreType, InstanceIdentifier)}
     */
    @Deprecated
    public static <T extends DataObject> void syncDelete(DataBroker broker,
            LogicalDatastoreType datastoreType, InstanceIdentifier<T> path) {
        try {
            SingleTransactionDataBroker.syncDelete(broker, datastoreType, path);
        } catch (TransactionCommitFailedException e) {
            LOG.error("Error deleting from datastore (path) : ({})", path, e);
            throw new RuntimeException(e);
        }
    }

    // "Consider returning a zero length array rather than null" - too late to change behavior plus it's deprecated.
    @SuppressFBWarnings("PZLA_PREFER_ZERO_LENGTH_ARRAYS")
    public static byte[] getMacAddressForNodeConnector(DataBroker broker,
            InstanceIdentifier<NodeConnector> nodeConnectorId)  {
        Optional<NodeConnector> optNc = MDSALDataStoreUtils.read(broker,
                LogicalDatastoreType.OPERATIONAL, nodeConnectorId);
        if (optNc.isPresent()) {
            NodeConnector nc = optNc.get();
            FlowCapableNodeConnector fcnc = nc.getAugmentation(FlowCapableNodeConnector.class);
            MacAddress macAddress = fcnc.getHardwareAddress();
            return HexEncode.bytesFromHexString(macAddress.getValue());
        }
        return null;
    }

    public static NodeId getNodeIdFromNodeConnectorId(NodeConnectorId ncId) {
        return new NodeId(ncId.getValue().substring(0,
                ncId.getValue().lastIndexOf(':')));
    }

    public static String getInterfaceName(NodeConnectorRef ref, DataBroker dataBroker) {
        NodeConnectorId nodeConnectorId = getNodeConnectorId(dataBroker, ref);
        NodeId nodeId = getNodeIdFromNodeConnectorId(nodeConnectorId);
        InstanceIdentifier<NodeConnector> ncIdentifier = InstanceIdentifier
                .builder(Nodes.class)
                .child(Node.class, new NodeKey(nodeId))
                .child(NodeConnector.class,
                        new NodeConnectorKey(nodeConnectorId)).build();
        return read(dataBroker, LogicalDatastoreType.OPERATIONAL, ncIdentifier).toJavaUtil().map(
            nc -> nc.getAugmentation(FlowCapableNodeConnector.class)).map(FlowCapableNodeConnector::getName).orElse(
                null);
    }

    public static NodeConnectorId getNodeConnectorId(DataBroker dataBroker,
            NodeConnectorRef ref) {
        return ((Optional<NodeConnector>) read(dataBroker, LogicalDatastoreType.OPERATIONAL,
                ref.getValue())).toJavaUtil().map(NodeConnector::getId).orElse(null);
    }

    public static Action createNxOfInPortAction(final int actionKey, final int inPortVal) {
        NxRegLoad regLoad = new NxRegLoadBuilder()
                .setDst(new DstBuilder().setDstChoice(new DstNxOfInPortCaseBuilder().setOfInPort(Boolean.TRUE).build())
                        .setStart(0).setEnd(15).build())
                .setValue(BigInteger.valueOf(inPortVal)).build();
        ActionBuilder abExt = new ActionBuilder();
        abExt.setKey(new ActionKey(actionKey));
        abExt.setOrder(actionKey);
        abExt.setAction(new NxActionRegLoadNodesNodeTableFlowApplyActionsCaseBuilder().setNxRegLoad(regLoad).build());
        return abExt.build();
    }

    public static Action createPopVlanAction(final int actionKey) {
        return new ActionBuilder().setAction(
               new PopVlanActionCaseBuilder().setPopVlanAction(new PopVlanActionBuilder().build()).build())
                .setKey(new ActionKey(actionKey)).setOrder(actionKey).build();
    }

}

