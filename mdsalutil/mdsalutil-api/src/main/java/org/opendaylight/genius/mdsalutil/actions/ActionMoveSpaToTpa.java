/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.actions;

import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfArpTpaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegMoveNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.NxRegMoveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.nx.reg.move.DstBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.nx.reg.move.SrcBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcOfArpSpaCaseBuilder;
import org.opendaylight.yangtools.yang.common.Empty;

/**
 * Move Source IP address to Destination IP address, to where the ARP
 * response need to be addressed to.
 */
public class ActionMoveSpaToTpa extends ActionInfo {

    public ActionMoveSpaToTpa() {
        this(0);
    }

    public ActionMoveSpaToTpa(int actionKey) {
        super(actionKey);
    }

    @Override
    public Action buildAction() {
        return buildAction(getActionKey());
    }

    @Override
    public Action buildAction(int newActionKey) {
        // Length of the SHA is 6 byte, hence the end offset bit is 47
        return new ActionBuilder()
            .setAction(new NxActionRegMoveNodesNodeTableFlowApplyActionsCaseBuilder()
                .setNxRegMove(new NxRegMoveBuilder()
                    .setSrc(new SrcBuilder()
                        .setSrcChoice(new SrcOfArpSpaCaseBuilder().setOfArpSpa(Empty.getInstance()).build())
                        .setStart(0)
                        .setEnd(31)
                        .build())
                    .setDst(new DstBuilder()
                        .setDstChoice(new DstOfArpTpaCaseBuilder().setOfArpTpa(Empty.getInstance()).build())
                        .setStart(0)
                        .setEnd(31)
                        .build())
                    .build())
                .build())
            .withKey(new ActionKey(newActionKey))
            .build();
    }
}
