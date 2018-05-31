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
import java.math.BigInteger;
import org.junit.Test;
import org.opendaylight.genius.mdsalutil.InstructionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.WriteMetadataCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.write.metadata._case.WriteMetadata;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;

/**
 * Test for {@link InstructionWriteMetadata}.
 */
public class InstructionWriteMetadataTest {

    private static final InstructionWriteMetadata INSTRUCTION_INFO
        = new InstructionWriteMetadata(BigInteger.ONE, BigInteger.TEN);

    @Test
    public void newInstruction() {
        verifyInstructionInfo(INSTRUCTION_INFO);
    }

    private void verifyInstructionInfo(InstructionInfo instructionInfo) {
        Instruction instruction = instructionInfo.buildInstruction(2);
        assertEquals(2, instruction.key().getOrder().intValue());
        assertTrue(instruction.getInstruction() instanceof WriteMetadataCase);
        WriteMetadataCase writeMetadataCase = (WriteMetadataCase) instruction.getInstruction();
        WriteMetadata writeMetadata = writeMetadataCase.getWriteMetadata();
        assertEquals(BigInteger.ONE, writeMetadata.getMetadata());
        assertEquals(BigInteger.TEN, writeMetadata.getMetadataMask());
    }

    @Test
    public void xtendBeanGenerator() {
        XtendBeanGenerator generator = new XtendBeanGenerator();
        assertEquals("new InstructionWriteMetadata(1bi, 10bi)", generator.getExpression(INSTRUCTION_INFO));
    }
}
