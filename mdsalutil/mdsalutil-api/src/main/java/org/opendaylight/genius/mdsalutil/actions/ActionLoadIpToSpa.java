/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.actions;

import com.google.common.net.InetAddresses;
import java.math.BigInteger;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.ActionType;
import org.opendaylight.genius.mdsalutil.NWUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice
    .grouping.DstChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxArpShaCaseBuilder;

import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice
    .grouping.dst.choice.DstOfArpSpaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegLoadNodesNodeTableFlowApplyActionsCaseBuilder;

import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg
    .load.grouping.NxRegLoad;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.NxRegLoadBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.nx.reg.load.DstBuilder;


/**
 * Load IP Address to SPA (Sender Protocol Address).
 * <p>
 * IP address of the sender. In an ARP request this field is used to
 * indicate the address of the host sending the request. In an ARP reply
 * this field is used to indicate the address of the host that the request
 * was looking for.
 */
public class ActionLoadIpToSpa extends ActionInfo {
    private final String address;

    public ActionLoadIpToSpa(String address) {
        this(0, address);
    }

    public ActionLoadIpToSpa(int actionKey, String address) {
        super(ActionType.load_ip_to_spa, new String[] {address}, actionKey);
        this.address = address;
    }

    @Deprecated
    public ActionLoadIpToSpa(ActionInfo actionInfo) {
        this(actionInfo.getActionKey(), actionInfo.getActionValues()[0]);
    }

    @Override
    public Action buildAction() {
        return buildAction(getActionKey());
    }

    public Action buildAction(int newActionKey) {
        return new ActionBuilder()
            .setAction(new NxActionRegLoadNodesNodeTableFlowApplyActionsCaseBuilder()
                .setNxRegLoad(new NxRegLoadBuilder()
                    .setDst(new DstBuilder()
                        .setDstChoice(new DstOfArpSpaCaseBuilder().setOfArpSpa(true).build())
                        .setStart(0)
                        .setEnd(31)
                        .build())
                    .setValue(BigInteger.valueOf(
                        InetAddresses.coerceToInteger(InetAddresses.forString(address)) & 0xffffffffL))
                    .build())
                .build())
            .setKey(new ActionKey(newActionKey))
            .build();
    }

    public String getAddress() {
        return address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ActionLoadIpToSpa that = (ActionLoadIpToSpa) o;

        return address != null ? address.equals(that.address) : that.address == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (address != null ? address.hashCode() : 0);
        return result;
    }
}
