/*
 * Copyright © 2016 Red Hat, Inc. and others.
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfEthDstCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegMoveNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.NxRegMove;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.NxRegMoveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.nx.reg.move.DstBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.nx.reg.move.SrcBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcOfEthSrcCaseBuilder;
import org.opendaylight.yangtools.yang.common.Empty;

/**
 * Move source/destination Ethernet action.
 */
public class ActionMoveSourceDestinationEth extends ActionInfo {

    public ActionMoveSourceDestinationEth() {
        this(0);
    }

    public ActionMoveSourceDestinationEth(int actionKey) {
        super(actionKey);
    }

    @Override
    public Action buildAction() {
        return buildAction(getActionKey());
    }

    @Override
    public Action buildAction(int newActionKey) {
        ActionBuilder ab = new ActionBuilder();
        NxRegMove regMove = new NxRegMoveBuilder()
            .setSrc(new SrcBuilder()
                .setSrcChoice(new SrcOfEthSrcCaseBuilder().setOfEthSrc(Empty.getInstance()).build())
                .setStart(0)
                .build())
            .setDst(new DstBuilder()
                .setDstChoice(new DstOfEthDstCaseBuilder().setOfEthDst(Empty.getInstance()).build())
                .setStart(0)
                .setEnd(47)
                .build())
            .build();
        ab.setAction(new NxActionRegMoveNodesNodeTableFlowApplyActionsCaseBuilder().setNxRegMove(regMove).build());
        ab.withKey(new ActionKey(newActionKey));
        return ab.build();
    }
}
