/*
 * Copyright © 2016 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.actions;

import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxTunIpv4SrcCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.group.buckets.bucket.action.action.NxActionRegLoadNodesNodeGroupBucketsBucketActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegLoadNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.NxRegLoad;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.NxRegLoadBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.nx.reg.load.Dst;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.nx.reg.load.DstBuilder;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint64;

/**
 * Set tunnel source IP action.
 */
public class ActionSetTunnelSourceIp extends ActionInfo {
    private static final Dst NX_REGEX_LOAD_SRC = new DstBuilder()
            .setDstChoice(new DstNxTunIpv4SrcCaseBuilder().setNxTunIpv4Src(Empty.getInstance()).build())
            .setStart(Uint16.ZERO)
            .setEnd(Uint16.valueOf(31).intern())
            .build();

    private final Uint64 sourceIp;
    private final boolean groupBucket;

    public ActionSetTunnelSourceIp(Uint64 sourceIp) {
        this(0, sourceIp);
    }

    public ActionSetTunnelSourceIp(IpAddress sourceIp) {
        this(0, sourceIp);
    }

    public ActionSetTunnelSourceIp(int actionKey, Uint64 sourceIp) {
        super(actionKey);
        this.sourceIp = sourceIp;
        this.groupBucket = false;
    }

    public ActionSetTunnelSourceIp(int actionKey, IpAddress sourceIp) {
        this(actionKey, MDSALUtil.getBigIntIpFromIpAddress(sourceIp));
    }

    public Uint64 getSourceIp() {
        return sourceIp;
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
        final NxRegLoad nxRegLoad = new NxRegLoadBuilder()
                .setDst(NX_REGEX_LOAD_SRC)
                .setValue(sourceIp)
                .build();

        ActionBuilder ab = new ActionBuilder().withKey(new ActionKey(newActionKey));
        if (groupBucket) {
            ab.setAction(new NxActionRegLoadNodesNodeGroupBucketsBucketActionsCaseBuilder()
                .setNxRegLoad(nxRegLoad).build());
        } else {
            ab.setAction(new NxActionRegLoadNodesNodeTableFlowApplyActionsCaseBuilder()
                .setNxRegLoad(nxRegLoad).build());
        }
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

        ActionSetTunnelSourceIp that = (ActionSetTunnelSourceIp) other;

        if (groupBucket != that.groupBucket) {
            return false;
        }
        return sourceIp != null ? sourceIp.equals(that.sourceIp) : that.sourceIp == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (sourceIp != null ? sourceIp.hashCode() : 0);
        result = 31 * result + (groupBucket ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ActionSetTunnelSourceIp [sourceIp=" + sourceIp + ", groupBucket=" + groupBucket + ", getActionKey()="
                + getActionKey() + "]";
    }
}
