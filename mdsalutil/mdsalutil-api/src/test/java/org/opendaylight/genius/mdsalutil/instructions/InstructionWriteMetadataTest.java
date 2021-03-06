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
import org.opendaylight.genius.mdsalutil.tests.UintXtendBeanGenerator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.WriteMetadataCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.write.metadata._case.WriteMetadata;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yangtools.yang.common.Uint64;

/**
 * Test for {@link InstructionWriteMetadata}.
 */
public class InstructionWriteMetadataTest {

    private static final InstructionWriteMetadata INSTRUCTION_INFO
        = new InstructionWriteMetadata(Uint64.ONE, Uint64.valueOf(10));

    @Test
    public void newInstruction() {
        verifyInstructionInfo(INSTRUCTION_INFO);
    }

    private static void verifyInstructionInfo(InstructionInfo instructionInfo) {
        Instruction instruction = instructionInfo.buildInstruction(2);
        assertEquals(2, instruction.key().getOrder().intValue());
        assertTrue(instruction.getInstruction() instanceof WriteMetadataCase);
        WriteMetadataCase writeMetadataCase = (WriteMetadataCase) instruction.getInstruction();
        WriteMetadata writeMetadata = writeMetadataCase.getWriteMetadata();
        assertEquals(Uint64.ONE, writeMetadata.getMetadata());
        assertEquals(Uint64.valueOf(10), writeMetadata.getMetadataMask());
    }

    @Test
    public void xtendBeanGenerator() {
        XtendBeanGenerator generator = new UintXtendBeanGenerator();
        assertEquals("new InstructionWriteMetadata((u64)1, (u64)10)", generator.getExpression(INSTRUCTION_INFO));
    }
}
