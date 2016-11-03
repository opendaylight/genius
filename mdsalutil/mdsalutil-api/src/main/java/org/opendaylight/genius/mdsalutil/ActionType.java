/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.opendaylight.genius.mdsalutil.NwConstants.LearnFlowModsType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DropActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.GroupActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopMplsActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopPbbActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopVlanActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushMplsActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushPbbActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushVlanActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.drop.action._case.DropAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.drop.action._case.DropActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.group.action._case.GroupActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.pop.mpls.action._case.PopMplsActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.pop.pbb.action._case.PopPbbActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.pop.vlan.action._case.PopVlanActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.push.mpls.action._case.PushMplsActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.push.pbb.action._case.PushPbbActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.push.vlan.action._case.PushVlanActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.field._case.SetFieldBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Icmpv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.ProtocolMatchFieldsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.TunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.UdpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.protocol.match.fields.PbbBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.vlan.match.fields.VlanIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.add.group.input.buckets.bucket.action.action.NxActionResubmitRpcAddGroupCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.DstChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxArpShaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxArpThaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxOfInPortCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxRegCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfArpOpCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfArpSpaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfArpTpaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfEthDstCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfIpDstCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.flow.mod.spec.flow.mod.spec.FlowModAddMatchFromFieldCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.flow.mod.spec.flow.mod.spec.FlowModAddMatchFromValueCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.flow.mod.spec.flow.mod.spec.FlowModCopyFieldIntoFieldCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.flow.mod.spec.flow.mod.spec.FlowModCopyValueIntoFieldCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.flow.mod.spec.flow.mod.spec.FlowModOutputToPortCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.flow.mod.spec.flow.mod.spec.flow.mod.add.match.from.field._case.FlowModAddMatchFromFieldBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.flow.mod.spec.flow.mod.spec.flow.mod.add.match.from.value._case.FlowModAddMatchFromValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.flow.mod.spec.flow.mod.spec.flow.mod.copy.field.into.field._case.FlowModCopyFieldIntoFieldBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.flow.mod.spec.flow.mod.spec.flow.mod.copy.value.into.field._case.FlowModCopyValueIntoFieldBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.flow.mod.spec.flow.mod.spec.flow.mod.output.to.port._case.FlowModOutputToPortBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.group.buckets.bucket.action.action.NxActionRegLoadNodesNodeGroupBucketsBucketActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.group.buckets.bucket.action.action.NxActionRegMoveNodesNodeGroupBucketsBucketActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionConntrackNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionLearnNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegLoadNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegMoveNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.conntrack.grouping.NxConntrackBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.learn.grouping.NxLearnBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.learn.grouping.nx.learn.FlowMods;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.learn.grouping.nx.learn.FlowModsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.NxRegLoad;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.NxRegLoadBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.nx.reg.load.Dst;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.nx.reg.load.DstBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.NxRegMove;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.NxRegMoveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.nx.reg.move.SrcBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.resubmit.grouping.NxResubmitBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.SrcChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcNxArpShaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcOfArpSpaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcOfEthSrcCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcOfIpSrcCaseBuilder;

import com.google.common.net.InetAddresses;


public enum ActionType {
    group {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            long groupId = Long.parseLong(actionInfo.getActionValues()[0]);

            return new ActionBuilder().setAction(
                            new GroupActionCaseBuilder().setGroupAction(
                                    new GroupActionBuilder().setGroupId(groupId).build()).build())
                    .setKey(new ActionKey(newActionKey)).build();
        }
    },

    output {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            String[] actionValues = actionInfo.getActionValues();
            String port = actionValues[0];
            int maxLength = 0;

            if (actionValues.length == 2) {
                maxLength = Integer.valueOf(actionValues[1]);
            }

            return new ActionBuilder().setAction(
                    new OutputActionCaseBuilder().setOutputAction(
                            new OutputActionBuilder().setMaxLength(Integer.valueOf(maxLength))
                                            .setOutputNodeConnector(new Uri(port)).build()).build())
                    .setKey(new ActionKey(newActionKey)).build();
        }
    },

    pop_mpls {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            return new ActionBuilder().setAction(
                    new PopMplsActionCaseBuilder().setPopMplsAction(
                            new PopMplsActionBuilder().setEthernetType(
                                    Integer.valueOf(NwConstants.ETHTYPE_IPV4)).build()).build())

                    .setKey(new ActionKey(newActionKey)).build();
        }
    },

    pop_pbb {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            return new ActionBuilder()
                    .setAction(new PopPbbActionCaseBuilder().setPopPbbAction(new PopPbbActionBuilder().build()).build())
                    .setKey(new ActionKey(newActionKey)).build();
        }
    },

    pop_vlan {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            return new ActionBuilder().setAction(
                    new PopVlanActionCaseBuilder().setPopVlanAction(new PopVlanActionBuilder().build()).build())
                    .setKey(new ActionKey(newActionKey)).build();
        }
    },

    push_mpls {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            return new ActionBuilder().setAction(new PushMplsActionCaseBuilder().setPushMplsAction(
                                    new PushMplsActionBuilder().setEthernetType(
                                            Integer.valueOf(NwConstants.ETHTYPE_MPLS_UC)).build()).build())
                    .setKey(new ActionKey(newActionKey)).build();
        }
    },

    push_pbb {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            return new ActionBuilder().setAction(
                    new PushPbbActionCaseBuilder().setPushPbbAction(
                                    new PushPbbActionBuilder()
                                            .setEthernetType(Integer.valueOf(NwConstants.ETHTYPE_PBB)).build()).build())
                    .setKey(new ActionKey(newActionKey)).build();
        }
    },

    push_vlan {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            return new ActionBuilder().setAction(
                    new PushVlanActionCaseBuilder().setPushVlanAction(
                                    new PushVlanActionBuilder().setEthernetType(
                                            Integer.valueOf(NwConstants.ETHTYPE_802_1Q)).build()).build())
                    .setKey(new ActionKey(newActionKey)).build();
        }
    },

    set_field_mpls_label {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            String[] actionValues = actionInfo.getActionValues();
            long label = Long.valueOf(actionValues[0]);

            return new ActionBuilder().setAction(
                    new SetFieldCaseBuilder().setSetField(new SetFieldBuilder().setProtocolMatchFields(
                                            new ProtocolMatchFieldsBuilder().setMplsLabel(label).build()).build())
                                    .build()).setKey(new ActionKey(newActionKey)).build();
        }
    },

    set_field_pbb_isid {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            String[] actionValues = actionInfo.getActionValues();
            long label = Long.valueOf(actionValues[0]);

            return new ActionBuilder().setAction(
                    new SetFieldCaseBuilder().setSetField(
                            new SetFieldBuilder().setProtocolMatchFields(
                                            new ProtocolMatchFieldsBuilder().setPbb(
                                                    new PbbBuilder().setPbbIsid(label).build()).build()).build())
                                    .build()).setKey(new ActionKey(newActionKey)).build();
        }
    },

    set_field_vlan_vid {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            String[] actionValues = actionInfo.getActionValues();
            int vlanId = Integer.valueOf(actionValues[0]);

            return new ActionBuilder().setAction(
                    new SetFieldCaseBuilder().setSetField(
                            new SetFieldBuilder().setVlanMatch(
                                    new VlanMatchBuilder().setVlanId(
                                                    new VlanIdBuilder().setVlanId(new VlanId(vlanId))
                                                            .setVlanIdPresent(true).build()).build()).build()).build())
                    .setKey(new ActionKey(newActionKey)).build();
        }
    },

    set_field_tunnel_id {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            BigInteger [] actionValues = actionInfo.getBigActionValues();
            if (actionValues.length == 2) {
                return new ActionBuilder().setAction(
                    new SetFieldCaseBuilder().setSetField(
                        new SetFieldBuilder()
                            .setTunnel(new TunnelBuilder().setTunnelId(actionValues[0])
                                           .setTunnelMask(actionValues[1]).build()).build())
                        .build())
                    .setKey(new ActionKey(newActionKey)).build();
            } else {
                return new ActionBuilder().setAction(
                    new SetFieldCaseBuilder().setSetField(
                        new SetFieldBuilder()
                            .setTunnel(new TunnelBuilder().setTunnelId(actionValues[0])
                                           .build()).build())
                        .build())
                    .setKey(new ActionKey(newActionKey)).build();
            }

        }

    },

    set_field_eth_dest {

        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            String[] actionValues = actionInfo.getActionValues();
            MacAddress mac = new MacAddress(actionValues[0]);

            return new ActionBuilder().setAction(
                    new SetFieldCaseBuilder().setSetField(
                            new SetFieldBuilder().setEthernetMatch(
                                    new EthernetMatchBuilder().setEthernetDestination(
                                                    new EthernetDestinationBuilder().setAddress(mac).build()).build())
                                            .build()).build()).setKey(new ActionKey(newActionKey)).build();

        }

    },

    set_udp_protocol {

        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            return new ActionBuilder().setAction(
                    new SetFieldCaseBuilder().setSetField(
                            new SetFieldBuilder().setIpMatch(
                                    new IpMatchBuilder().setIpProtocol((short) 17).build()).
                                    build()).build()).setKey(new ActionKey(newActionKey)).build();

        }

    },
    punt_to_controller {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            ActionBuilder ab = new ActionBuilder();
            OutputActionBuilder output = new OutputActionBuilder();
            output.setMaxLength(0xffff);
            Uri value = new Uri(OutputPortValues.CONTROLLER.toString());
            output.setOutputNodeConnector(value);
            ab.setAction(new OutputActionCaseBuilder().setOutputAction(output.build()).build());
            ab.setKey(new ActionKey(newActionKey));
            return ab.build();
        }

    },
    learn {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            String[] actionValues = actionInfo.getActionValues();
            String[][] actionValuesMatrix = actionInfo.getActionValuesMatrix();
            NxLearnBuilder learnBuilder = new NxLearnBuilder();
            learnBuilder.setIdleTimeout(Integer.parseInt(actionValues[0]));
            learnBuilder.setHardTimeout(Integer.parseInt(actionValues[1]));
            learnBuilder.setPriority(Integer.parseInt(actionValues[2]));
            learnBuilder.setCookie(BigInteger.valueOf(Long.valueOf(actionValues[3])));
            learnBuilder.setFlags(Integer.parseInt(actionValues[4]));
            learnBuilder.setTableId(Short.parseShort(actionValues[5]));
            learnBuilder.setFinIdleTimeout(Integer.parseInt(actionValues[6]));
            learnBuilder.setFinHardTimeout(Integer.parseInt(actionValues[7]));

            List<FlowMods> flowModsList = new ArrayList<>();
            for(String[] values : actionValuesMatrix){
                if(LearnFlowModsType.MATCH_FROM_FIELD.name().equals(values[0])){
                    FlowModAddMatchFromFieldBuilder builder = new FlowModAddMatchFromFieldBuilder();
                    builder.setSrcField(Long.decode(values[1]));
                    builder.setSrcOfs(0);
                    builder.setDstField(Long.decode(values[2]));
                    builder.setDstOfs(0);
                    builder.setFlowModNumBits(Integer.parseInt(values[3]));

                    FlowModsBuilder flowModsBuilder = new FlowModsBuilder();
                    FlowModAddMatchFromFieldCaseBuilder caseBuilder = new FlowModAddMatchFromFieldCaseBuilder();
                    caseBuilder.setFlowModAddMatchFromField(builder.build());
                    flowModsBuilder.setFlowModSpec(caseBuilder.build());
                    flowModsList.add(flowModsBuilder.build());
                } else if (LearnFlowModsType.MATCH_FROM_VALUE.name().equals(values[0])){
                    FlowModAddMatchFromValueBuilder builder = new FlowModAddMatchFromValueBuilder();
                    builder.setValue(Integer.parseInt(values[1]));
                    builder.setSrcField(Long.decode(values[2]));
                    builder.setSrcOfs(0);
                    builder.setFlowModNumBits(Integer.parseInt(values[3]));

                    FlowModsBuilder flowModsBuilder = new FlowModsBuilder();
                    FlowModAddMatchFromValueCaseBuilder caseBuilder = new FlowModAddMatchFromValueCaseBuilder();
                    caseBuilder.setFlowModAddMatchFromValue(builder.build());
                    flowModsBuilder.setFlowModSpec(caseBuilder.build());
                    flowModsList.add(flowModsBuilder.build());
                } else if (LearnFlowModsType.COPY_FROM_FIELD.name().equals(values[0])){
                    FlowModCopyFieldIntoFieldBuilder builder = new FlowModCopyFieldIntoFieldBuilder();
                    builder.setSrcField(Long.decode(values[1]));
                    builder.setSrcOfs(0);
                    builder.setDstField(Long.decode(values[2]));
                    builder.setDstOfs(0);
                    builder.setFlowModNumBits(Integer.parseInt(values[3]));

                    FlowModsBuilder flowModsBuilder = new FlowModsBuilder();
                    FlowModCopyFieldIntoFieldCaseBuilder caseBuilder = new FlowModCopyFieldIntoFieldCaseBuilder();
                    caseBuilder.setFlowModCopyFieldIntoField(builder.build());
                    flowModsBuilder.setFlowModSpec(caseBuilder.build());
                    flowModsList.add(flowModsBuilder.build());
                } else if (LearnFlowModsType.COPY_FROM_VALUE.name().equals(values[0])){
                    FlowModCopyValueIntoFieldBuilder builder = new FlowModCopyValueIntoFieldBuilder();
                    builder.setValue(Integer.parseInt(values[1]));
                    builder.setDstField(Long.decode(values[2]));
                    builder.setDstOfs(0);
                    builder.setFlowModNumBits(Integer.parseInt(values[3]));

                    FlowModsBuilder flowModsBuilder = new FlowModsBuilder();
                    FlowModCopyValueIntoFieldCaseBuilder caseBuilder = new FlowModCopyValueIntoFieldCaseBuilder();
                    caseBuilder.setFlowModCopyValueIntoField(builder.build());
                    flowModsBuilder.setFlowModSpec(caseBuilder.build());
                    flowModsList.add(flowModsBuilder.build());
                } else if (LearnFlowModsType.OUTPUT_TO_PORT.name().equals(values[0])){
                    FlowModOutputToPortBuilder builder = new FlowModOutputToPortBuilder();
                    builder.setSrcField(Long.decode(values[1]));
                    builder.setSrcOfs(0);
                    builder.setFlowModNumBits(Integer.parseInt(values[2]));

                    FlowModsBuilder flowModsBuilder = new FlowModsBuilder();
                    FlowModOutputToPortCaseBuilder caseBuilder = new FlowModOutputToPortCaseBuilder();
                    caseBuilder.setFlowModOutputToPort(builder.build());
                    flowModsBuilder.setFlowModSpec(caseBuilder.build());
                    flowModsList.add(flowModsBuilder.build());
                }
            }
            learnBuilder.setFlowMods(flowModsList);

            ActionBuilder abExt = new ActionBuilder();
            abExt.setKey(new ActionKey(newActionKey));

            abExt.setAction(new NxActionLearnNodesNodeTableFlowApplyActionsCaseBuilder()
                    .setNxLearn(learnBuilder.build()).build());
            return abExt.build();
        }
    },
    set_udp_destination_port {

        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            String[] actionValues = actionInfo.getActionValues();
            Integer portNumber = new Integer(actionValues[0]);

            return new ActionBuilder().setAction(
                    new SetFieldCaseBuilder().setSetField(
                            new SetFieldBuilder().setLayer4Match(
                                    new UdpMatchBuilder().setUdpDestinationPort(
                                            new PortNumber(portNumber)).build())
                            .build()).build()).setKey(new ActionKey(newActionKey)).build();

        }

    },
    set_udp_source_port {

        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            String[] actionValues = actionInfo.getActionValues();
            Integer portNumber = new Integer(actionValues[0]);

            return new ActionBuilder().setAction(
                    new SetFieldCaseBuilder().setSetField(
                            new SetFieldBuilder().setLayer4Match(
                                    new UdpMatchBuilder().setUdpSourcePort(
                                            new PortNumber(portNumber)).build())
                            .build()).build()).setKey(new ActionKey(newActionKey)).build();

        }

    },
    set_tcp_destination_port {

        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            String[] actionValues = actionInfo.getActionValues();
            Integer portNumber = new Integer(actionValues[0]);

            return new ActionBuilder().setAction(
                    new SetFieldCaseBuilder().setSetField(
                            new SetFieldBuilder().setLayer4Match(
                                    new TcpMatchBuilder().setTcpDestinationPort(
                                            new PortNumber(portNumber)).build())
                            .build()).build()).setKey(new ActionKey(newActionKey)).build();

        }

    },
    set_tcp_source_port {

        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            String[] actionValues = actionInfo.getActionValues();
            Integer portNumber = new Integer(actionValues[0]);

            return new ActionBuilder().setAction(
                    new SetFieldCaseBuilder().setSetField(
                            new SetFieldBuilder().setLayer4Match(
                                    new TcpMatchBuilder().setTcpSourcePort(
                                            new PortNumber(portNumber)).build())
                            .build()).build()).setKey(new ActionKey(newActionKey)).build();

        }

    },
    set_source_ip {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            String[] actionValues = actionInfo.getActionValues();
            String sourceIp = actionValues[0];
            String sourceMask = (actionValues.length > 1) ? actionValues[1] : "32";
            String source = sourceIp + "/" + sourceMask;
            return new ActionBuilder().setAction(
                                        new SetFieldCaseBuilder().setSetField(
                                                new SetFieldBuilder().setLayer3Match(
                                                        new Ipv4MatchBuilder().setIpv4Source(
                                                                new Ipv4Prefix(source)).build()).
                                                                build()).build()).setKey(new ActionKey(newActionKey)).build();


        }

    },
    set_destination_ip {

        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            String[] actionValues = actionInfo.getActionValues();
            String destIp = actionValues[0];
            String destMask = (actionValues.length > 1) ? actionValues[1] : "32";
            String destination = destIp + "/" + destMask;
            return new ActionBuilder().setAction(
                    new SetFieldCaseBuilder().setSetField(
                            new SetFieldBuilder().setLayer3Match(
                                    new Ipv4MatchBuilder().setIpv4Destination(
                                            new Ipv4Prefix(destination)).build())
                                            .build()).build()).setKey(new ActionKey(newActionKey)).build();

        }

    },
    set_field_eth_src {

        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            String[] actionValues = actionInfo.getActionValues();
            MacAddress mac = new MacAddress(actionValues[0]);

            return new ActionBuilder().setAction(
                    new SetFieldCaseBuilder().setSetField(
                            new SetFieldBuilder().setEthernetMatch(
                                    new EthernetMatchBuilder().setEthernetSource(
                                                    new EthernetSourceBuilder().setAddress(mac).build()).build())
                                            .build()).build()).setKey(new ActionKey(newActionKey)).build();

        }
    },

    drop_action {

        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            DropActionBuilder dab = new DropActionBuilder();
            DropAction dropAction = dab.build();
            ActionBuilder ab = new ActionBuilder();
            ab.setAction(new DropActionCaseBuilder().setDropAction(dropAction).build());
            ab.setKey(new ActionKey(newActionKey)).build();
            return ab.build();
        }
    },

    nx_resubmit {

        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            String[] actionValues = actionInfo.getActionValues();
            NxResubmitBuilder nxarsb = new NxResubmitBuilder();
            nxarsb.setTable(Short.parseShort(actionValues[0]));
            ActionBuilder ab = new ActionBuilder();
            ab.setAction(new NxActionResubmitRpcAddGroupCaseBuilder().setNxResubmit(nxarsb.build()).build());
            ab.setKey(new ActionKey(newActionKey));
            return ab.build();
        }
    },

    nx_load_reg_6 {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            String[] actionValues = actionInfo.getActionValues();
            NxRegLoadBuilder nxRegLoadBuilder = new NxRegLoadBuilder();
            Dst dst =  new DstBuilder()
                    .setDstChoice(new DstNxRegCaseBuilder().setNxReg(NxmNxReg6.class).build())
                    .setStart(Integer.valueOf(actionValues[0]))
                    .setEnd(Integer.valueOf(actionValues[1]))
                    .build();
            nxRegLoadBuilder.setDst(dst);
            nxRegLoadBuilder.setValue(new BigInteger(actionValues[2]));
            ActionBuilder ab = new ActionBuilder();
            ab.setAction(new NxActionRegLoadNodesNodeTableFlowApplyActionsCaseBuilder().setNxRegLoad(nxRegLoadBuilder.build()).build());
            ab.setKey(new ActionKey(actionInfo.getActionKey()));
            return ab.build();
        }
    },

    goto_table {

        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            new ActionBuilder();
            return null;
        }
    },
    nx_conntrack {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            String[] actionValues = actionInfo.getActionValues();
            Integer flags = new Integer(actionValues[0]);
            Long zoneSrc = new Long(actionValues[1]);
            Integer conntrackZone = new Integer(actionValues[2]);
            Short recircTable = new Short(actionValues[3]);
            NxConntrackBuilder ctb = new NxConntrackBuilder()
                    .setFlags(flags)
                    .setZoneSrc(zoneSrc)
                    .setConntrackZone(conntrackZone)
                    .setRecircTable(recircTable);
            ActionBuilder ab = new ActionBuilder();
            ab.setAction(new NxActionConntrackNodesNodeTableFlowApplyActionsCaseBuilder()
                .setNxConntrack(ctb.build()).build());
            ab.setKey(new ActionKey(newActionKey));
            return ab.build();

        }

    },
    move_src_dst_ip {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {

            ActionBuilder ab = new ActionBuilder();
            NxRegMove regMove = new NxRegMoveBuilder()
                    .setSrc(new SrcBuilder().setSrcChoice(new SrcOfIpSrcCaseBuilder().setOfIpSrc(Boolean.TRUE).build())
                            .setStart(0).build())
                    .setDst(new org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.nx.reg.move.DstBuilder()
                            .setDstChoice(new DstOfIpDstCaseBuilder().setOfIpDst(Boolean.TRUE).build()).setStart(0)
                            .setEnd(31).build())
                    .build();
            ab.setAction(new NxActionRegMoveNodesNodeTableFlowApplyActionsCaseBuilder().setNxRegMove(regMove).build());
            ab.setKey(new ActionKey(newActionKey));
            return ab.build();
        }
    },

    move_src_dst_eth {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            ActionBuilder ab = new ActionBuilder();
            NxRegMove regMove = new NxRegMoveBuilder().setSrc(new SrcBuilder()
                    .setSrcChoice(new SrcOfEthSrcCaseBuilder().setOfEthSrc(Boolean.TRUE).build()).setStart(0).build())
                    .setDst(new org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.nx.reg.move.DstBuilder()
                            .setDstChoice(new DstOfEthDstCaseBuilder().setOfEthDst(Boolean.TRUE).build()).setStart(0)
                            .setEnd(47).build())
                    .build();
            ab.setAction(new NxActionRegMoveNodesNodeTableFlowApplyActionsCaseBuilder().setNxRegMove(regMove).build());
            ab.setKey(new ActionKey(newActionKey));
            return ab.build();
        }
    },

    set_icmp_type {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            String[] actionValues = actionInfo.getActionValues();

            ActionBuilder ab = new ActionBuilder();
            Icmpv4MatchBuilder icmpb = new Icmpv4MatchBuilder().setIcmpv4Type(Short.parseShort(actionValues[0]));
            ab.setAction(new SetFieldCaseBuilder()
                    .setSetField(new SetFieldBuilder().setIcmpv4Match(icmpb.build()).build()).build());
            ab.setKey(new ActionKey(newActionKey));
            return ab.build();
        }

    },
    nx_load_in_port {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            BigInteger[] actionValues = actionInfo.getBigActionValues();
            NxRegLoad rb = new NxRegLoadBuilder()
                    .setDst(new DstBuilder()
                            .setDstChoice(new DstNxOfInPortCaseBuilder().setOfInPort(Boolean.TRUE).build())
                            .setStart(Integer.valueOf(0)).setEnd(Integer.valueOf(15)).build())
                    .setValue(actionValues[0]).build();
            ActionBuilder ab = new ActionBuilder();
            ab.setKey(new ActionKey(newActionKey));
            ab.setOrder(newActionKey);
            ab.setAction(new NxActionRegLoadNodesNodeTableFlowApplyActionsCaseBuilder().setNxRegLoad(rb).build());
            return ab.build();
        }
    },

    /**
     * Load macAddress to SHA(Sender Hardware Address)
     * <p>
     * Media address of the sender. In an ARP request this field is used to
     * indicate the address of the host sending the request. In an ARP reply
     * this field is used to indicate the address of the host that the request
     * was looking for.
     *
     */
    load_mac_to_sha {

        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            final MacAddress mac = new MacAddress(
                    actionInfo.getActionValues()[0]);
            return new ActionBuilder().setKey(new ActionKey(newActionKey))
                    .setAction(nxLoadRegAction(new DstNxArpShaCaseBuilder()
                    .setNxArpSha(Boolean.TRUE).build(), BigInteger.valueOf(NWUtil.macToLong(mac)), 47, false))
                    .build();
        }
    },

    /**
     * Load IP Address to SPA(Sender Protocol Address)
     * <p>
     * IP address of the sender. In an ARP request this field is used to
     * indicate the address of the host sending the request. In an ARP reply
     * this field is used to indicate the address of the host that the request
     * was looking for
     */
    load_ip_to_spa {

        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {

            final String ipAddress = (String) actionInfo.getActionValues()[0];
            final long ipl = InetAddresses
                    .coerceToInteger(InetAddresses.forString(ipAddress)) & 0xffffffffL;
            return new ActionBuilder().setKey(new ActionKey(newActionKey))
                    .setAction(nxLoadRegAction(new DstOfArpSpaCaseBuilder()
                            .setOfArpSpa(Boolean.TRUE).build(), BigInteger.valueOf(ipl))).build();

        }

    },

    /**
     * Move Source Hardware address to Destination address, to where the ARP
     * response need to be addressed to.
     *
     */
    move_sha_to_tha {

        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            return new ActionBuilder().setKey(new ActionKey(newActionKey))
                    .setAction(nxMoveRegAction(
                            new SrcNxArpShaCaseBuilder()
                                    .setNxArpSha(Boolean.TRUE).build(),
                            new DstNxArpThaCaseBuilder()
                                    .setNxArpTha(Boolean.TRUE).build(),
                            47, false))
                    .build();
        }
    },

    /**
     *
     * Move Source IP address to Destination IP address, to where the ARP
     * response need to be addressed to.
     *
     */
    move_spa_to_tpa {

        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            return new ActionBuilder().setKey(new ActionKey(newActionKey))
                    .setAction(nxMoveRegAction(
                            new SrcOfArpSpaCaseBuilder()
                                    .setOfArpSpa(Boolean.TRUE).build(),
                            new DstOfArpTpaCaseBuilder()
                                    .setOfArpTpa(Boolean.TRUE).build()))
                    .build();
        }
    },

    /**
     * Set ARP Operation Type that is Request or Replay.
     */
    set_arp_op {

        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
 
            final int val = Integer.parseInt(actionInfo.getActionValues()[0]);
            return new ActionBuilder().setKey(new ActionKey(newActionKey))
                    .setAction(nxLoadRegAction(
                            new DstOfArpOpCaseBuilder().setOfArpOp(Boolean.TRUE)
                                    .build(),
                            BigInteger.valueOf(val), 15, false))
                    .build();
        }

    };

    private static final int RADIX_HEX = 16;

    public abstract Action buildAction(int newActionKey, ActionInfo actionInfo);

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action nxLoadRegAction(
            DstChoice dstChoice, BigInteger value) {
        return nxLoadRegAction(dstChoice, value, 31, false);

    }

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action nxMoveRegAction(
            final SrcChoice srcChoice, final DstChoice dstChoice,
            final int endOffset, final boolean groupBucket) {
        final NxRegMove reg = new NxRegMoveBuilder()
                .setSrc(new SrcBuilder().setSrcChoice(srcChoice)
                        .setStart(Integer.valueOf(0))
                        .setEnd(Integer.valueOf(endOffset)).build())
                .setDst(new org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.nx.reg.move.DstBuilder()
                        .setDstChoice(dstChoice).setStart(Integer.valueOf(0))
                        .setEnd(Integer.valueOf(endOffset)).build())
                .build();
        if (groupBucket) {
            return new NxActionRegMoveNodesNodeGroupBucketsBucketActionsCaseBuilder()
                    .setNxRegMove(reg).build();
        } else {
            return new NxActionRegMoveNodesNodeTableFlowApplyActionsCaseBuilder()
                    .setNxRegMove(reg).build();
        }
    }

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action nxMoveRegAction(
            final SrcChoice srcChoice, final DstChoice dstChoice) {
        return nxMoveRegAction(srcChoice, dstChoice, 31, false);
    }

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action nxLoadRegAction(
            final DstChoice dstChoice, final BigInteger value,
            final int endOffset, final boolean groupBucket) {
        final NxRegLoad reg = new NxRegLoadBuilder()
                .setDst(new DstBuilder().setDstChoice(dstChoice)
                        .setStart(Integer.valueOf(0))
                        .setEnd(Integer.valueOf(endOffset)).build())
                .setValue(value).build();
        if (groupBucket) {
            return new NxActionRegLoadNodesNodeGroupBucketsBucketActionsCaseBuilder()
                    .setNxRegLoad(reg).build();
        } else {
            return new NxActionRegLoadNodesNodeTableFlowApplyActionsCaseBuilder()
                    .setNxRegLoad(reg).build();
        }
    }

}