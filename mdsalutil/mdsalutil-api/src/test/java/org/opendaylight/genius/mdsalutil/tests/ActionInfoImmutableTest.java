/*
 * Copyright (c) 2016 Red Hat and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.ActionType;
import org.opendaylight.genius.mdsalutil.FlowEntity;
import org.opendaylight.genius.mdsalutil.InstructionInfo;
import org.opendaylight.genius.mdsalutil.InstructionType;
import org.opendaylight.genius.mdsalutil.actions.ActionRegLoad;
import org.opendaylight.genius.mdsalutil.actions.ActionRegMove;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxRegCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfMplsLabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegLoadNodesNodeTableFlowApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegMoveNodesNodeTableFlowApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.NxRegLoad;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.NxRegMove;

public class ActionInfoImmutableTest {

    @Test
    public void actionInfoActionKeyDoesNotMagicallyChangeOnFlowEntityGetFlowBuilder() {
        FlowEntity flowEntity = new FlowEntity(BigInteger.valueOf(123L));
        flowEntity.setFlowId("TEST");
        flowEntity.setCookie(BigInteger.valueOf(110100480L));
        ActionInfo actionInfo = new ActionInfo(ActionType.nx_conntrack, new String[] { "1", "0", "0", "255" }, 27);
        List<ActionInfo> actionInfos = new ArrayList<>();
        actionInfos.add(actionInfo);
        flowEntity.getInstructionInfoList().add(new InstructionInfo(InstructionType.apply_actions, actionInfos));
        assertEquals(27, flowEntity.getInstructionInfoList().get(0).getActionInfos().get(0).getActionKey());

        flowEntity.getFlowBuilder();
        assertEquals(27, flowEntity.getInstructionInfoList().get(0).getActionInfos().get(0).getActionKey());
        flowEntity.getFlowBuilder();
        assertEquals(27, flowEntity.getInstructionInfoList().get(0).getActionInfos().get(0).getActionKey());
    }

    @Test
    public void actionInfoTestForRegLoadAction() {
        ActionInfo actionInfo = new ActionRegLoad(1, NxmNxReg6.class, 0, 31, 100);
        Action action = actionInfo.buildAction();
        assertTrue(action.getAction() instanceof NxActionRegLoadNodesNodeTableFlowApplyActionsCase);
        NxActionRegLoadNodesNodeTableFlowApplyActionsCase actionsCase = (NxActionRegLoadNodesNodeTableFlowApplyActionsCase) action.getAction();
        NxRegLoad nxRegLoad = actionsCase.getNxRegLoad();
        assertTrue(nxRegLoad.getDst().getDstChoice() instanceof DstNxRegCase);
        DstNxRegCase dstNxRegCase = (DstNxRegCase) nxRegLoad.getDst().getDstChoice();
        assertEquals(NxmNxReg6.class, dstNxRegCase.getNxReg());
        assertEquals((Integer) 0, nxRegLoad.getDst().getStart());
        assertEquals((Integer) 31, nxRegLoad.getDst().getEnd());
        assertEquals(100, nxRegLoad.getValue().longValue());
    }

    @Test
    public void actionInfoTestForRegMoveToMplsAction() {
        ActionInfo actionInfo = new ActionRegMove(1, NxmNxReg1.class, 0, 31);
        Action action = actionInfo.buildAction();
        assertTrue(action.getAction() instanceof NxActionRegMoveNodesNodeTableFlowApplyActionsCase);
        NxActionRegMoveNodesNodeTableFlowApplyActionsCase actionsCase = (NxActionRegMoveNodesNodeTableFlowApplyActionsCase) action.getAction();
        NxRegMove nxRegMove = actionsCase.getNxRegMove();
        assertTrue(nxRegMove.getDst().getDstChoice() instanceof DstOfMplsLabelCase);
        assertEquals((Integer) 0, nxRegMove.getDst().getStart());
        assertEquals((Integer) 31, nxRegMove.getDst().getEnd());
    }

}
