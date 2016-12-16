/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil;

import org.opendaylight.genius.mdsalutil.actions.ActionDrop;
import org.opendaylight.genius.mdsalutil.actions.ActionGroup;
import org.opendaylight.genius.mdsalutil.actions.ActionLearn;
import org.opendaylight.genius.mdsalutil.actions.ActionLoadIpToSpa;
import org.opendaylight.genius.mdsalutil.actions.ActionLoadMacToSha;
import org.opendaylight.genius.mdsalutil.actions.ActionMoveShaToTha;
import org.opendaylight.genius.mdsalutil.actions.ActionMoveSourceDestinationEth;
import org.opendaylight.genius.mdsalutil.actions.ActionMoveSourceDestinationIp;
import org.opendaylight.genius.mdsalutil.actions.ActionMoveSpaToTpa;
import org.opendaylight.genius.mdsalutil.actions.ActionNxConntrack;
import org.opendaylight.genius.mdsalutil.actions.ActionNxLoadInPort;
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
import org.opendaylight.genius.mdsalutil.actions.ActionSetArpOp;
import org.opendaylight.genius.mdsalutil.actions.ActionSetDestinationIp;
import org.opendaylight.genius.mdsalutil.actions.ActionSetFieldDscp;
import org.opendaylight.genius.mdsalutil.actions.ActionSetFieldEthernetDestination;
import org.opendaylight.genius.mdsalutil.actions.ActionSetFieldEthernetSource;
import org.opendaylight.genius.mdsalutil.actions.ActionSetFieldMplsLabel;
import org.opendaylight.genius.mdsalutil.actions.ActionSetFieldPbbIsid;
import org.opendaylight.genius.mdsalutil.actions.ActionSetFieldTunnelId;
import org.opendaylight.genius.mdsalutil.actions.ActionSetFieldVlanVid;
import org.opendaylight.genius.mdsalutil.actions.ActionSetIcmpType;
import org.opendaylight.genius.mdsalutil.actions.ActionSetSourceIp;
import org.opendaylight.genius.mdsalutil.actions.ActionSetTcpDestinationPort;
import org.opendaylight.genius.mdsalutil.actions.ActionSetTcpSourcePort;
import org.opendaylight.genius.mdsalutil.actions.ActionSetTunnelDestinationIp;
import org.opendaylight.genius.mdsalutil.actions.ActionSetTunnelSourceIp;
import org.opendaylight.genius.mdsalutil.actions.ActionSetUdpDestinationPort;
import org.opendaylight.genius.mdsalutil.actions.ActionSetUdpProtocol;
import org.opendaylight.genius.mdsalutil.actions.ActionSetUdpSourcePort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg6;


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
                return new ActionSetFieldMplsLabel(actionInfo).buildAction(newActionKey);
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
                return new ActionSetFieldPbbIsid(actionInfo).buildAction(newActionKey);
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
                return new ActionSetFieldVlanVid(actionInfo).buildAction(newActionKey);
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
                return new ActionSetFieldTunnelId(actionInfo).buildAction(newActionKey);
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
                return new ActionSetTunnelSourceIp(actionInfo).buildAction(newActionKey);
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
                return new ActionSetTunnelDestinationIp(actionInfo).buildAction(newActionKey);
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
                return new ActionSetFieldEthernetDestination(actionInfo).buildAction(newActionKey);
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
                return new ActionLearn(actionInfo).buildAction(newActionKey);
            }
        }
    },

    @Deprecated
    set_udp_destination_port {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            if (actionInfo instanceof ActionSetUdpDestinationPort) {
                return ((ActionSetUdpDestinationPort) actionInfo).buildAction(newActionKey);
            } else {
                return new ActionSetUdpDestinationPort(actionInfo).buildAction(newActionKey);
            }
        }
    },

    @Deprecated
    set_udp_source_port {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            if (actionInfo instanceof ActionSetUdpSourcePort) {
                return ((ActionSetUdpSourcePort) actionInfo).buildAction(newActionKey);
            } else {
                return new ActionSetUdpSourcePort(actionInfo).buildAction(newActionKey);
            }
        }
    },

    @Deprecated
    set_tcp_destination_port {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            if (actionInfo instanceof ActionSetTcpDestinationPort) {
                return ((ActionSetTcpDestinationPort) actionInfo).buildAction(newActionKey);
            } else {
                return new ActionSetTcpDestinationPort(actionInfo).buildAction(newActionKey);
            }
        }
    },

    @Deprecated
    set_tcp_source_port {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            if (actionInfo instanceof ActionSetTcpSourcePort) {
                return ((ActionSetTcpSourcePort) actionInfo).buildAction(newActionKey);
            } else {
                return new ActionSetTcpSourcePort(actionInfo).buildAction(newActionKey);
            }
        }
    },

    @Deprecated
    set_source_ip {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            if (actionInfo instanceof ActionSetSourceIp) {
                return ((ActionSetSourceIp) actionInfo).buildAction(newActionKey);
            } else {
                return new ActionSetSourceIp(actionInfo).buildAction(newActionKey);
            }
        }
    },

    @Deprecated
    set_destination_ip {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            if (actionInfo instanceof ActionSetDestinationIp) {
                return ((ActionSetDestinationIp) actionInfo).buildAction(newActionKey);
            } else {
                return new ActionSetDestinationIp(actionInfo).buildAction(newActionKey);
            }
        }
    },

    @Deprecated
    set_field_dscp {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            return ((ActionSetFieldDscp) actionInfo).buildAction(newActionKey);
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
                return new ActionSetFieldEthernetSource(actionInfo).buildAction(newActionKey);
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
                return new ActionNxResubmit(actionInfo).buildAction(newActionKey);
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

    @Deprecated
    move_src_dst_ip {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            if (actionInfo instanceof ActionMoveSourceDestinationIp) {
                return ((ActionMoveSourceDestinationIp) actionInfo).buildAction(newActionKey);
            } else {
                return new ActionMoveSourceDestinationIp(actionInfo).buildAction(newActionKey);
            }
        }
    },

    @Deprecated
    move_src_dst_eth {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            if (actionInfo instanceof ActionMoveSourceDestinationEth) {
                return ((ActionMoveSourceDestinationEth) actionInfo).buildAction(newActionKey);
            } else {
                return new ActionMoveSourceDestinationEth(actionInfo).buildAction(newActionKey);
            }
        }
    },

    @Deprecated
    set_icmp_type {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            if (actionInfo instanceof ActionSetIcmpType) {
                return ((ActionSetIcmpType) actionInfo).buildAction(newActionKey);
            } else {
                return new ActionSetIcmpType(actionInfo).buildAction(newActionKey);
            }
        }
    },

    @Deprecated
    nx_load_in_port {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            if (actionInfo instanceof ActionNxLoadInPort) {
                return ((ActionNxLoadInPort) actionInfo).buildAction(newActionKey);
            } else {
                return new ActionNxLoadInPort(actionInfo).buildAction(newActionKey);
            }
        }
    },

    @Deprecated
    load_mac_to_sha {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            if (actionInfo instanceof ActionLoadMacToSha) {
                return ((ActionLoadMacToSha) actionInfo).buildAction(newActionKey);
            } else {
                return new ActionLoadMacToSha(actionInfo).buildAction(newActionKey);
            }
        }
    },

    @Deprecated
    load_ip_to_spa {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            if (actionInfo instanceof ActionLoadIpToSpa) {
                return ((ActionLoadIpToSpa) actionInfo).buildAction(newActionKey);
            } else {
                return new ActionLoadIpToSpa(actionInfo).buildAction(newActionKey);
            }
        }
    },

    @Deprecated
    move_sha_to_tha {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            if (actionInfo instanceof ActionMoveShaToTha) {
                return ((ActionMoveShaToTha) actionInfo).buildAction(newActionKey);
            } else {
                return new ActionMoveShaToTha(actionInfo).buildAction(newActionKey);
            }
        }
    },

    @Deprecated
    move_spa_to_tpa {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            if (actionInfo instanceof ActionMoveSpaToTpa) {
                return ((ActionMoveSpaToTpa) actionInfo).buildAction(newActionKey);
            } else {
                return new ActionMoveSpaToTpa(actionInfo).buildAction(newActionKey);
            }
        }
    },

    @Deprecated
    set_arp_op {
        @Override
        public Action buildAction(int newActionKey, ActionInfo actionInfo) {
            if (actionInfo instanceof ActionSetArpOp) {
                return ((ActionSetArpOp) actionInfo).buildAction(newActionKey);
            } else {
                return new ActionSetArpOp(actionInfo).buildAction(newActionKey);
            }
        }
    };

    public abstract Action buildAction(int newActionKey, ActionInfo actionInfo);
}
