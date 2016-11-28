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

import ch.vorburger.xtendbeans.XtendBeanGenerator;
import org.junit.Test;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxRegCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegLoadNodesNodeTableFlowApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.NxRegLoad;

/**
 * Test class for {@link ActionRegLoad}.
 */
public class ActionRegLoadTest {
    private XtendBeanGenerator generator = new XtendBeanGenerator();

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
    public void generateActionRegLoad() {
        ActionInfo actionRegLoad = new ActionRegLoad(NxmNxReg1.class, 0, 2, 255);
        actionRegLoad.buildAction();
        assertEquals(
                "new ActionRegLoad(0, org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match" +
                        ".rev140421.NxmNxReg1, 0, 2, 255L)\n",
                generator.getExpression(actionRegLoad));
    }
}
