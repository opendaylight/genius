/*
 * Copyright © 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.instructions;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ClearActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;

/**
 * Clear actions instruction.
 */
public class InstructionClearActions extends AbstractInstructionInfoImpl {
    private static final long serialVersionUID = 1L;

    @Override
    public Instruction buildInstruction(int instructionKey) {
        return new InstructionBuilder()
                .setInstruction(new ClearActionsCaseBuilder().build())
                .setKey(new InstructionKey(instructionKey))
                .build();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return 17;
    }

    @Override
    public String toString() {
        return "InstructionClearActions";
    }
}
