/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.actions;

import java.math.BigInteger;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.ActionType;
import org.opendaylight.genius.mdsalutil.NWUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxArpShaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegLoadNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.NxRegLoadBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.nx.reg.load.DstBuilder;

/**
 * Load MAC address to SHA (Sender Hardware Address)
 * <p>
 * Media address of the sender. In an ARP request this field is used to
 * indicate the address of the host sending the request. In an ARP reply
 * this field is used to indicate the address of the host that the request
 * was looking for.
 */
public class ActionLoadMacToSha extends ActionInfo {
    private final MacAddress address;

    public ActionLoadMacToSha(MacAddress address) {
        this(0, address);
    }

    public ActionLoadMacToSha(int actionKey, MacAddress address) {
        super(ActionType.load_mac_to_sha, new String[] {address.getValue()}, actionKey);
        this.address = address;
    }

    @Deprecated
    public ActionLoadMacToSha(ActionInfo actionInfo) {
        this(actionInfo.getActionKey(), new MacAddress(actionInfo.getActionValues()[0]));
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
                        .setDstChoice(new DstNxArpShaCaseBuilder().setNxArpSha(true).build())
                        .setStart(0)
                        .setEnd(47)
                        .build())
                    .setValue(BigInteger.valueOf(NWUtil.macToLong(address)))
                    .build())
                .build())
            .setKey(new ActionKey(newActionKey))
            .build();
    }

    public MacAddress getAddress() {
        return address;
    }
}
