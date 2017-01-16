/*
 * Copyright © 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.instructions;

import org.opendaylight.genius.mdsalutil.InstructionInfo;
import org.opendaylight.genius.mdsalutil.InstructionType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.GoToTableCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.go.to.table._case.GoToTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;

/**
 * Goto table instruction.
 */
public class InstructionGotoTable extends InstructionInfo {
    private final short tableId;

    public InstructionGotoTable(short tableId) {
        super(InstructionType.goto_table, new long[] {tableId});
        this.tableId = tableId;
    }

    @Override
    public Instruction buildInstruction(int instructionKey) {
        return new InstructionBuilder()
                .setInstruction(new GoToTableCaseBuilder()
                        .setGoToTable(new GoToTableBuilder()
                                .setTableId(tableId)
                                .build()
                        )
                        .build()
                )
                .setKey(new InstructionKey(instructionKey))
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InstructionGotoTable that = (InstructionGotoTable) o;

        return tableId == that.tableId;
    }

    @Override
    public int hashCode() {
        return (int) tableId;
    }
}
