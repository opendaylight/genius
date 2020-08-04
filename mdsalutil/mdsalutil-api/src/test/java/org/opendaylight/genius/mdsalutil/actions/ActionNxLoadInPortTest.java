/*
 * Copyright © 2016, 2017 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import ch.vorburger.xtendbeans.XtendBeanGenerator;
import org.junit.Test;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.tests.UintXtendBeanGenerator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxOfInPortCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegLoadNodesNodeTableFlowApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.NxRegLoad;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.common.Uint64;

/**
 * Test for {@link ActionNxLoadInPort}.
 */
public class ActionNxLoadInPortTest {
    private static final Uint64 VALUE = Uint64.valueOf(10).intern();

    private final XtendBeanGenerator generator = new UintXtendBeanGenerator();

    @Test
    public void actionInfoTest() {
        verifyAction(new ActionNxLoadInPort(VALUE).buildAction());
    }

    private static void verifyAction(Action action) {
        assertTrue(action.getAction() instanceof NxActionRegLoadNodesNodeTableFlowApplyActionsCase);
        NxActionRegLoadNodesNodeTableFlowApplyActionsCase actionCase =
            (NxActionRegLoadNodesNodeTableFlowApplyActionsCase) action.getAction();
        assertNotNull(actionCase.getNxRegLoad());
        NxRegLoad nxRegLoad = actionCase.getNxRegLoad();
        assertTrue(nxRegLoad.getDst().getDstChoice() instanceof DstNxOfInPortCase);
        DstNxOfInPortCase dstNxOfInPortCase = (DstNxOfInPortCase) nxRegLoad.getDst().getDstChoice();
        assertEquals(Empty.getInstance(),dstNxOfInPortCase.getOfInPort());
        assertEquals(0, nxRegLoad.getDst().getStart().intValue());
        assertEquals(15, nxRegLoad.getDst().getEnd().intValue());
        assertEquals(VALUE, nxRegLoad.getValue());
    }

    @Test
    public void generateAction() {
        ActionInfo actionInfo = new ActionNxLoadInPort(VALUE);
        assertEquals(
            "new ActionNxLoadInPort(0, (u64)" + VALUE + ")", generator.getExpression(actionInfo));
    }
}
