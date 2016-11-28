/*
 * Copyright © 2016 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfMplsLabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegMoveNodesNodeTableFlowApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.NxRegMove;

/**
 * Test for {@link ActionRegMove}.
 */
public class ActionRegMoveTest {
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
