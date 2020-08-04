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
import org.junit.Test;
import org.opendaylight.genius.mdsalutil.InstructionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.GoToTableCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;

/**
 * Test for {@link InstructionGotoTable}.
 */
public class InstructionGotoTableTest {

    private static final InstructionGotoTable INSTRUCTION_INFO = new InstructionGotoTable((short) 1);

    @Test
    public void newInstruction() {
        verifyInstructionInfo(INSTRUCTION_INFO);
    }

    private static void verifyInstructionInfo(InstructionInfo instructionInfo) {
        Instruction instruction = instructionInfo.buildInstruction(2);
        assertEquals(2, instruction.key().getOrder().intValue());
        assertTrue(instruction.getInstruction() instanceof GoToTableCase);
        GoToTableCase goToTableCase = (GoToTableCase) instruction.getInstruction();
        assertEquals((short) 1, goToTableCase.getGoToTable().getTableId().shortValue());
    }

    @Test
    public void xtendBeanGenerator() {
        XtendBeanGenerator generator = new XtendBeanGenerator();
        assertEquals("new InstructionGotoTable(1 as short)", generator.getExpression(INSTRUCTION_INFO));
    }
}
