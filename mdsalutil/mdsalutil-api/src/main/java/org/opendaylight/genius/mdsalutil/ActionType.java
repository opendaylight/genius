/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil;

import com.google.common.net.InetAddresses;
import java.math.BigInteger;
import org.opendaylight.genius.mdsalutil.actions.ActionDrop;
import org.opendaylight.genius.mdsalutil.actions.ActionGroup;
import org.opendaylight.genius.mdsalutil.actions.ActionLearn;
import org.opendaylight.genius.mdsalutil.actions.ActionNxConntrack;
import org.opendaylight.genius.mdsalutil.actions.ActionNxResubmit;
import org.opendaylight.genius.mdsalutil.actions.ActionOutput;
import org.opendaylight.genius.mdsalutil.actions.ActionPopMpls;
import org.opendaylight.genius.mdsalutil.actions.ActionPopPbb;
import org.opendaylight.genius.mdsalutil.actions.ActionPopVlan;
import org.opendaylight.genius.mdsalutil.actions.ActionPuntToController;
import org.opendaylight.genius.mdsalutil.actions.ActionPushMpls;
import org.opendaylight.genius.mdsalutil.actions.ActionPushPbb;
import org.opendaylight.genius.mdsalutil.actions.ActionPushVlan;
import org.opendaylight.genius.mdsalutil.actions.ActionRegLoad;
import org.opendaylight.genius.mdsalutil.actions.ActionRegMove;
import org.opendaylight.genius.mdsalutil.actions.ActionSetFieldEthernetDestination;
import org.opendaylight.genius.mdsalutil.actions.ActionSetFieldEthernetSource;
import org.opendaylight.genius.mdsalutil.actions.ActionSetFieldMplsLabel;
import org.opendaylight.genius.mdsalutil.actions.ActionSetFieldPbbIsid;
import org.opendaylight.genius.mdsalutil.actions.ActionSetFieldTunnelId;
import org.opendaylight.genius.mdsalutil.actions.ActionSetFieldVlanVid;
import org.opendaylight.genius.mdsalutil.actions.ActionSetTunnelDestinationIp;
import org.opendaylight.genius.mdsalutil.actions.ActionSetTunnelSourceIp;
import org.opendaylight.genius.mdsalutil.actions.ActionSetUdpProtocol;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.field._case.SetFieldBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Icmpv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.UdpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.DstChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxArpShaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxArpThaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxOfInPortCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfArpOpCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfArpSpaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfArpTpaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfEthDstCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfIpDstCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.group.buckets.bucket.action.action.NxActionRegLoadNodesNodeGroupBucketsBucketActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegLoadNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.NxRegLoadBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.nx.reg.load.DstBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.group.buckets.bucket.action.action.NxActionRegMoveNodesNodeGroupBucketsBucketActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegMoveNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.NxRegLoad;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.NxRegMove;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.NxRegMoveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.nx.reg.move.SrcBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.SrcChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcNxArpShaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcOfArpSpaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcOfEthSrcCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcOfIpSrcCaseBuilder;


public enum ActionType {
    @Deprecated
    group {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            if (actionInfo instanceof ActionGroup) {
                return ((ActionGroup) actionInfo).buildAction(newActionKey);
            } else {
                // TODO Migrate all users to ActionGroup
                return new ActionGroup(actionInfo.getActionValues()).buildAction(newActionKey);
            }
        }
    },

    @Deprecated
    output {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            if (actionInfo instanceof ActionOutput) {
                return ((ActionOutput) actionInfo).buildAction(newActionKey);
            } else {
                // TODO Migrate all users to ActionOutput
                return new ActionOutput(actionInfo.getActionValues()).buildAction(newActionKey);
            }
        }
    },

    @Deprecated
    pop_mpls {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            if (actionInfo instanceof ActionPopMpls) {
                return ((ActionPopMpls) actionInfo).buildAction(newActionKey);
            } else {
                // TODO Migrate all users to ActionPopMpls
                return new ActionPopMpls().buildAction(newActionKey);
            }
        }
    },

    @Deprecated
    pop_pbb {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            if (actionInfo instanceof ActionPopPbb) {
                return ((ActionPopPbb) actionInfo).buildAction(newActionKey);
            } else {
                // TODO Migrate all users to ActionPopPbb
                return new ActionPopPbb().buildAction(newActionKey);
            }
        }
    },

    @Deprecated
    pop_vlan {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            if (actionInfo instanceof ActionPopVlan) {
                return ((ActionPopVlan) actionInfo).buildAction(newActionKey);
            } else {
                // TODO Migrate all users to ActionPopVlan
                return new ActionPopVlan().buildAction(newActionKey);
            }
        }
    },

    @Deprecated
    push_mpls {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            if (actionInfo instanceof ActionPushMpls) {
                return ((ActionPushMpls) actionInfo).buildAction(newActionKey);
            } else {
                // TODO Migrate all users to ActionPushMpls
                return new ActionPushMpls().buildAction(newActionKey);
            }
        }
    },

    @Deprecated
    push_pbb {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            if (actionInfo instanceof ActionPushPbb) {
                return ((ActionPushPbb) actionInfo).buildAction(newActionKey);
            } else {
                // TODO Migrate all users to ActionPushPbb
                return new ActionPushPbb().buildAction(newActionKey);
            }
        }
    },

    @Deprecated
    push_vlan {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            if (actionInfo instanceof ActionPushVlan) {
                return ((ActionPushVlan) actionInfo).buildAction(newActionKey);
            } else {
                // TODO Migrate all users to ActionPushVlan
                return new ActionPushVlan().buildAction(newActionKey);
            }
        }
    },

    @Deprecated
    set_field_mpls_label {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            if (actionInfo instanceof ActionSetFieldMplsLabel) {
                return ((ActionSetFieldMplsLabel) actionInfo).buildAction(newActionKey);
            } else {
                // TODO Migrate all users to ActionSetFieldMplsLabel
                return new ActionSetFieldMplsLabel(actionInfo.getActionValues()).buildAction(newActionKey);
            }
        }
    },

    @Deprecated
    set_field_pbb_isid {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            if (actionInfo instanceof ActionSetFieldPbbIsid) {
                return ((ActionSetFieldPbbIsid) actionInfo).buildAction(newActionKey);
            } else {
                // TODO Migrate all users to ActionSetFieldPbbIsid
                return new ActionSetFieldPbbIsid(actionInfo.getActionValues()).buildAction(newActionKey);
            }
        }
    },

    @Deprecated
    set_field_vlan_vid {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            if (actionInfo instanceof ActionSetFieldVlanVid) {
                return ((ActionSetFieldVlanVid) actionInfo).buildAction(newActionKey);
            } else {
                // TODO Migrate all users to ActionSetFieldVlanVid
                return new ActionSetFieldVlanVid(actionInfo.getActionValues()).buildAction(newActionKey);
            }
        }
    },

    @Deprecated
    set_field_tunnel_id {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            if (actionInfo instanceof ActionSetFieldTunnelId) {
                return ((ActionSetFieldTunnelId) actionInfo).buildAction(newActionKey);
            } else {
                // TODO Migrate all users to ActionSetFieldTunnelId
                return new ActionSetFieldTunnelId(actionInfo.getBigActionValues()).buildAction(newActionKey);
            }
        }
    },

    @Deprecated
    set_tunnel_src_ip {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            if (actionInfo instanceof ActionSetTunnelSourceIp) {
                return ((ActionSetTunnelSourceIp) actionInfo).buildAction(newActionKey);
            } else {
                // TODO Migrate all users to ActionSetTunnelSourceIp
                return new ActionSetTunnelSourceIp(actionInfo.getBigActionValues()).buildAction(newActionKey);
            }
        }
    },

    @Deprecated
    set_tunnel_dest_ip {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            if (actionInfo instanceof ActionSetTunnelDestinationIp) {
                return ((ActionSetTunnelDestinationIp) actionInfo).buildAction(newActionKey);
            } else {
                // TODO Migrate all users to ActionSetTunnelDestinationIp
                return new ActionSetTunnelDestinationIp(actionInfo.getBigActionValues()).buildAction(newActionKey);
            }
        }
    },

    @Deprecated
    set_field_eth_dest {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            if (actionInfo instanceof ActionSetFieldEthernetDestination) {
                return ((ActionSetFieldEthernetDestination) actionInfo).buildAction(newActionKey);
            } else {
                // TODO Migrate all users to ActionSetFieldEthernetDestination
                return new ActionSetFieldEthernetDestination(actionInfo.getActionValues()).buildAction(newActionKey);
            }
        }
    },

    @Deprecated
    set_udp_protocol {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            if (actionInfo instanceof ActionSetUdpProtocol) {
                return ((ActionSetUdpProtocol) actionInfo).buildAction(newActionKey);
            } else {
                return new ActionSetUdpProtocol().buildAction(newActionKey);
            }
        }
    },

    @Deprecated
    punt_to_controller {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            if (actionInfo instanceof ActionPuntToController) {
                return ((ActionPuntToController) actionInfo).buildAction(newActionKey);
            } else {
                return new ActionPuntToController().buildAction(newActionKey);
            }
        }
    },

    @Deprecated
    learn {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            if (actionInfo instanceof ActionLearn) {
                return ((ActionLearn) actionInfo).buildAction(newActionKey);
            } else {
                // TODO Migrate all users to ActionLearn
                return new ActionLearn(actionInfo.getActionValues(), actionInfo.getActionValuesMatrix()).buildAction(
                    newActionKey);
            }
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

    @Deprecated
    set_field_eth_src {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            if (actionInfo instanceof ActionSetFieldEthernetSource) {
                return ((ActionSetFieldEthernetSource) actionInfo).buildAction(newActionKey);
            } else {
                // TODO Migrate all users to ActionSetFieldEthernetSource
                return new ActionSetFieldEthernetSource(actionInfo.getActionValues()).buildAction(newActionKey);
            }
        }
    },

    @Deprecated
    drop_action {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            if (actionInfo instanceof ActionDrop) {
                return ((ActionDrop) actionInfo).buildAction(newActionKey);
            } else {
                // TODO Migrate all users to ActionDrop
                return new ActionDrop().buildAction(newActionKey);
            }
        }
    },

    @Deprecated
    nx_resubmit {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            if (actionInfo instanceof ActionNxResubmit) {
                return ((ActionNxResubmit) actionInfo).buildAction(newActionKey);
            } else {
                // TODO Migrate all users to ActionNxResubmit
                return new ActionNxResubmit(actionInfo.getActionValues()).buildAction(newActionKey);
            }
        }
    },

    @Deprecated
    nx_load_reg_6 {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            // TODO Migrate all users to ActionRegLoad
            return new ActionRegLoad(NxmNxReg6.class, actionInfo.getActionValues()).buildAction(newActionKey);
        }
    },

    @Deprecated
    nx_load_reg {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            if (actionInfo instanceof ActionRegLoad) {
                return ((ActionRegLoad) actionInfo).buildAction(newActionKey);
            }
            throw new IllegalStateException(
                    "nx_load_reg with an ActionInfo that's not ActionRegLoad but " + actionInfo.getClass());
        }
    },

    @Deprecated
    nx_reg_move_mpls_label {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            if (actionInfo instanceof ActionRegMove) {
                return ((ActionRegMove) actionInfo).buildAction(newActionKey);
            }
            throw new IllegalStateException(
                    "nx_reg_move_mpls_label with an ActionInfo that's not ActionRegMove but " + actionInfo.getClass());
        }
    },

    @Deprecated
    goto_table {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            return null;
        }
    },

    @Deprecated
    nx_conntrack {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            if (actionInfo instanceof ActionNxConntrack) {
                return ((ActionNxConntrack) actionInfo).buildAction(newActionKey);
            } else {
                return new ActionNxConntrack(actionInfo.getActionValues()).buildAction(newActionKey);
            }
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
                            .setStart(0).setEnd(15).build())
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

            final String ipAddress = actionInfo.getActionValues()[0];
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
                            47, false)) //Length of the SHA is 6Byte, hence the end offset bit is 47
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
                            BigInteger.valueOf(val), 15, false)) // The length of ARP operation field is 2Byte, hence end offset bit is 15
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
                        .setStart(0)
                        .setEnd(endOffset).build())
                .setDst(new org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.nx.reg.move.DstBuilder()
                        .setDstChoice(dstChoice).setStart(0)
                        .setEnd(endOffset).build())
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
                        .setStart(0)
                        .setEnd(endOffset).build())
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
