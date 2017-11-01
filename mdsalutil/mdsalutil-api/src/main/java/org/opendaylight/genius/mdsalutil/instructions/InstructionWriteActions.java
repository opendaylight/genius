/*
 * Copyright © 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.instructions;

import java.util.Collections;
import java.util.List;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.ActionInfoList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.WriteActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.write.actions._case.WriteActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;

/**
 * Write actions instruction.
 */
public class InstructionWriteActions extends AbstractInstructionInfoImpl {

    private final ActionInfoList actions;

    public InstructionWriteActions(List<ActionInfo> actionInfos) {
        this.actions = new ActionInfoList(actionInfos);
    }

    @Override
    public Instruction buildInstruction(int instructionKey) {
        return new InstructionBuilder()
                .setInstruction(new WriteActionsCaseBuilder()
                        .setWriteActions(new WriteActionsBuilder()
                                .setAction(actions.buildActions())
                                .build()
                        )
                        .build()
                )
                .setKey(new InstructionKey(instructionKey))
                .build();
    }

    public List<ActionInfo> getActionInfos() {
        // This is required because,
        // even though actions is final and should never be null,
        // the xtendbeans library creates instances of this class
        // via a reflection-based trick (to determine default values)
        // and logs a confusing NPE warning during tests, if we don't guard:
        if (actions != null) {
            return actions.getActionInfos();
        } else {
            return Collections.emptyList();
        }

    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        InstructionWriteActions that = (InstructionWriteActions) other;

        return actions.equals(that.actions);
    }

    @Override
    public int hashCode() {
        return actions.hashCode();
    }

    @Override
    public String toString() {
        return "InstructionWriteActions[" + actions + "]";
    }

}
