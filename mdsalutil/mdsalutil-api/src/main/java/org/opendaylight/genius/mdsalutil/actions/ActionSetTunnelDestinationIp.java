/*
 * Copyright © 2016 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.actions;

import java.math.BigInteger;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxTunIpv4DstCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.group.buckets.bucket.action.action.NxActionRegLoadNodesNodeGroupBucketsBucketActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegLoadNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.NxRegLoadBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.nx.reg.load.Dst;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.nx.reg.load.DstBuilder;
import org.opendaylight.yangtools.yang.common.Empty;

/**
 * Set tunnel destination IP action.
 */
public class ActionSetTunnelDestinationIp extends ActionInfo {

    private final BigInteger destIp;
    private final boolean groupBucket;

    public ActionSetTunnelDestinationIp(BigInteger destIp) {
        this(0, destIp);
    }

    public ActionSetTunnelDestinationIp(IpAddress destIp) {
        this(0, destIp);
    }

    public ActionSetTunnelDestinationIp(int actionKey, BigInteger destIp) {
        super(actionKey);
        this.destIp = destIp;
        this.groupBucket = false;
    }

    public ActionSetTunnelDestinationIp(int actionKey, IpAddress destIp) {
        this(actionKey, MDSALUtil.getBigIntIpFromIpAddress(destIp));
    }

    public BigInteger getDestIp() {
        return destIp;
    }

    public boolean isGroupBucket() {
        return groupBucket;
    }

    @Override
    public Action buildAction() {
        return buildAction(getActionKey());
    }

    @Override
    public Action buildAction(int newActionKey) {
        NxRegLoadBuilder nxRegLoadBuilder = new NxRegLoadBuilder();
        Dst dst = new DstBuilder()
            .setDstChoice(new DstNxTunIpv4DstCaseBuilder().setNxTunIpv4Dst(Empty.getInstance()).build())
            .setStart(0)
            .setEnd(31)
            .build();
        nxRegLoadBuilder.setDst(dst);
        nxRegLoadBuilder.setValue(destIp);
        ActionBuilder ab = new ActionBuilder();

        if (groupBucket) {
            ab.setAction(new NxActionRegLoadNodesNodeGroupBucketsBucketActionsCaseBuilder()
                .setNxRegLoad(nxRegLoadBuilder.build()).build());
        } else {
            ab.setAction(new NxActionRegLoadNodesNodeTableFlowApplyActionsCaseBuilder()
                .setNxRegLoad(nxRegLoadBuilder.build()).build());
        }
        ab.withKey(new ActionKey(newActionKey));
        return ab.build();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        if (!super.equals(other)) {
            return false;
        }

        ActionSetTunnelDestinationIp that = (ActionSetTunnelDestinationIp) other;

        if (groupBucket != that.groupBucket) {
            return false;
        }
        return destIp != null ? destIp.equals(that.destIp) : that.destIp == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (destIp != null ? destIp.hashCode() : 0);
        result = 31 * result + (groupBucket ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ActionSetTunnelDestinationIp [destIp=" + destIp + ", groupBucket=" + groupBucket + ", getActionKey()="
                + getActionKey() + "]";
    }
}
