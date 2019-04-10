/*
 * Copyright © 2017 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.instructions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import ch.vorburger.xtendbeans.XtendBeanGenerator;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.opendaylight.genius.mdsalutil.InstructionInfo;
import org.opendaylight.genius.mdsalutil.actions.ActionGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.GroupActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;

/**
 * Test for {@link InstructionApplyActions}.
 */
public class InstructionApplyActionsTest {

    private static final InstructionApplyActions INSTRUCTION_INFO
        = new InstructionApplyActions(Collections.singletonList(new ActionGroup(1L)));

    @Test
    public void newInstruction() {
        verifyInstructionInfo(INSTRUCTION_INFO);
    }

    private void verifyInstructionInfo(InstructionInfo instructionInfo) {
        Instruction instruction = instructionInfo.buildInstruction(2);
        assertEquals(2, instruction.key().getOrder().intValue());
        assertTrue(instruction.getInstruction() instanceof ApplyActionsCase);
        ApplyActionsCase applyActionsCase = (ApplyActionsCase) instruction.getInstruction();
        List<Action> actions = applyActionsCase.getApplyActions().getAction();
        assertEquals(1, actions.size());
        Action action = actions.get(0);
        assertTrue(action.getAction() instanceof GroupActionCase);
    }

    @Test
    public void xtendBeanGenerator() {
        XtendBeanGenerator generator = new XtendBeanGenerator();
        assertEquals("new InstructionApplyActions(#[\n"
                + "    (new ActionGroupBuilder => [\n"
                + "        groupId = 1L\n"
                + "    ]).build()\n"
                + "])", generator.getExpression(INSTR/AbstractTestableListener.javaUCTION_INFO));
    }

}
