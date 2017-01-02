/*
 * Copyright © 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil;

import org.opendaylight.genius.mdsalutil.instructions.InstructionApplyActions;
import org.opendaylight.genius.mdsalutil.instructions.InstructionClearActions;
import org.opendaylight.genius.mdsalutil.instructions.InstructionGotoTable;
import org.opendaylight.genius.mdsalutil.instructions.InstructionWriteActions;
import org.opendaylight.genius.mdsalutil.instructions.InstructionWriteMetadata;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;

@Deprecated
public enum InstructionType {
    @Deprecated
    apply_actions {
        @Override
        public Instruction buildInstruction(InstructionInfo instructionInfo, int instructionKey) {
            return new InstructionApplyActions(instructionInfo.getActionInfos()).buildInstruction(instructionKey);
        }
    },

    @Deprecated
    goto_table {
        @Override
        public Instruction buildInstruction(InstructionInfo instructionInfo, int instructionKey) {
            return new InstructionGotoTable((short) instructionInfo.getInstructionValues()[0]).buildInstruction(
                    instructionKey);
        }
    },

    @Deprecated
    write_actions {
        @Override
        public Instruction buildInstruction(InstructionInfo instructionInfo, int instructionKey) {
            return new InstructionWriteActions(instructionInfo.getActionInfos()).buildInstruction(instructionKey);
        }
    },

    @Deprecated
    clear_actions {
        @Override
        public Instruction buildInstruction(InstructionInfo instructionInfo, int instructionKey) {
            return new InstructionClearActions().buildInstruction(instructionKey);
        }
    },

    @Deprecated
    write_metadata {
        @Override
        public Instruction buildInstruction(InstructionInfo instructionInfo, int instructionKey) {
            return new InstructionWriteMetadata(instructionInfo.getBigInstructionValues()[0],
                    instructionInfo.getBigInstructionValues()[1]).buildInstruction(instructionKey);
        }
    };

    @Deprecated
    public abstract Instruction buildInstruction(InstructionInfo instructionInfo, int instructionKey);
}
