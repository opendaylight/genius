/*
 * Copyright © 2017 Red Hat, Inc. and others.
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfMetadataCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegLoadNodesNodeTableFlowApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.NxRegLoad;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint64;

/**
 * Test for {@link ActionNxLoadMetadata}.
 */
public class ActionNxLoadMetadataTest {
    private static final Uint64 VALUE = Uint64.valueOf(10);
    private static final Integer START = 0;
    private static final Integer END = 23;

    private final XtendBeanGenerator generator = new UintXtendBeanGenerator();

    @Test
    public void actionInfoTest() {
        verifyAction(new ActionNxLoadMetadata(VALUE, START, END).buildAction());
    }

    private static void verifyAction(Action action) {
        assertTrue(action.getAction() instanceof NxActionRegLoadNodesNodeTableFlowApplyActionsCase);
        NxActionRegLoadNodesNodeTableFlowApplyActionsCase actionCase =
            (NxActionRegLoadNodesNodeTableFlowApplyActionsCase) action.getAction();
        assertNotNull(actionCase.getNxRegLoad());
        NxRegLoad nxRegLoad = actionCase.getNxRegLoad();
        assertTrue(nxRegLoad.getDst().getDstChoice() instanceof DstOfMetadataCase);
        DstOfMetadataCase dstOfMetadataCase = (DstOfMetadataCase) nxRegLoad.getDst().getDstChoice();
        assertEquals(Empty.getInstance(), dstOfMetadataCase.getOfMetadata());
        assertEquals(Uint16.valueOf(START), nxRegLoad.getDst().getStart());
        assertEquals(Uint16.valueOf(END), nxRegLoad.getDst().getEnd());
        assertEquals(VALUE, nxRegLoad.getValue());
    }

    @Test
    public void generateAction() {
        ActionInfo actionInfo = new ActionNxLoadMetadata(VALUE, START, END);
        assertEquals("new ActionNxLoadMetadata(0, (u64)" + VALUE + ", " + START +  ", " + END + ")",
            generator.getExpression(actionInfo));
    }
}
