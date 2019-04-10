/*
 * Copyright © 2016, 2017 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import ch.vorburger.xtendbeans.XtendBeanGenerator;
import org.junit.Test;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfIpDstCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegMoveNodesNodeTableFlowApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.NxRegMove;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcOfIpSrcCase;
import org.opendaylight.yangtools.yang.common.Empty;

/**
 * Test class for {@link ActionMoveSourceDestinationIp}.
 */
public class ActionMoveSourceDestinationIpTest {
    private XtendBeanGenerator generator = new XtendBeanGenerator();

    @Test
    public void actionInfoTest() {
        verifyAction(new ActionMoveSourceDestinationIp().buildAction());
    }

    private void verifyAction(Action action) {
        assertTrue(action.getAction() instanceof NxActionRegMoveNodesNodeTableFlowApplyActionsCase);
        NxActionRegMoveNodesNodeTableFlowApplyActionsCase actionCase =
            (NxActionRegMoveNodesNodeTableFlowApplyActionsCase) action.getAction();
        NxRegMove nxRegMove = actionCase.getNxRegMove();
        assertTrue(nxRegMove.getSrc().getSrcChoice() instanceof SrcOfIpSrcCase);
        SrcOfIpSrcCase srcChoice = (SrcOfIpSrcCase) nxRegMove.getSrc().getSrcChoice();
        assertEquals(Empty.getInstance(),srcChoice.getOfIpSrc());
        assertEquals(0, nxRegMove.getSrc().getStart().intValue());
        assertTrue(nxRegMove.getDst().getDstChoice() instanceof DstOfIpDstCase);
        DstOfIpDstCase dstChoice = (DstOfIpDstCase) nxRegMove.getDst().getDstChoice();
        assertEquals(Empty.getInstance(),dstChoice.getOfIpDst());
        assertEquals(0, nxRegMove.getDst().getStart().intValue());
        assertEquals(31, nxRegMove.getDst().getEnd().intValue());
    }

    @Test
    public void generateAction() {
        ActionInfo actionInfo = new ActionMoveSourceDestinationIp();
        assertEquals("new ActionMoveSourceDestinationIp", generator.getExpression(actionInfo));
    }
}
