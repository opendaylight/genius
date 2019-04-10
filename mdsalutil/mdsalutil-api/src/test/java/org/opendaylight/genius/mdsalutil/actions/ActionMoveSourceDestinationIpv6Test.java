/*
 * Copyright © 2017 Red Hat, Inc. and others.
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxIpv6DstCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegMoveNodesNodeTableFlowApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.NxRegMove;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcNxIpv6SrcCase;
import org.opendaylight.yangtools.yang.common.Empty;

/**
 * Test class for {@link ActionMoveSourceDestinationIpv6}.
 */
public class ActionMoveSourceDestinationIpv6Test {
    private XtendBeanGenerator generator = new XtendBeanGenerator();

    @Test
    public void actionInfoTest() {
        verifyAction(new ActionMoveSourceDestinationIpv6().buildAction());
    }

    private void verifyAction(Action action) {
        assertTrue(action.getAction() instanceof NxActionRegMoveNodesNodeTableFlowApplyActionsCase);
        NxActionRegMoveNodesNodeTableFlowApplyActionsCase actionCase =
            (NxActionRegMoveNodesNodeTableFlowApplyActionsCase) action.getAction();
        NxRegMove nxRegMove = actionCase.getNxRegMove();
        assertTrue(nxRegMove.getSrc().getSrcChoice() instanceof SrcNxIpv6SrcCase);
        SrcNxIpv6SrcCase srcChoice = (SrcNxIpv6SrcCase) nxRegMove.getSrc().getSrcChoice();
        assertEquals(Empty.getInstance(),srcChoice.isNxIpv6Src());
        assertEquals(0, nxRegMove.getSrc().getStart().intValue());
        assertTrue(nxRegMove.getDst().getDstChoice() instanceof DstNxIpv6DstCase);
        DstNxIpv6DstCase dstChoice = (DstNxIpv6DstCase) nxRegMove.getDst().getDstChoice();
        assertEquals(Empty.getInstance(), dstChoice.isNxIpv6Dst());
        assertEquals(0, nxRegMove.getDst().getStart().intValue());
        assertEquals(127, nxRegMove.getDst().getEnd().intValue());
    }

    @Test
    public void generateAction() {
        ActionInfo actionInfo = new ActionMoveSourceDestinationIpv6();
        assertEquals("new ActionMoveSourceDestinationIpv6", generator.getExpression(actionInfo));
    }
}
