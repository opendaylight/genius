/*
 * Copyright © 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.instructions;

import java.math.BigInteger;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.WriteMetadataCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.write.metadata._case.WriteMetadataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;

/**
 * Write metadata instruction.
 */
public class InstructionWriteMetadata extends AbstractInstructionInfoImpl {

    private final BigInteger metadata;
    private final BigInteger mask;

    public InstructionWriteMetadata(BigInteger metadata, BigInteger mask) {
        this.metadata = metadata;
        this.mask = mask;
    }

    @Override
    public Instruction buildInstruction(int instructionKey) {
        return new InstructionBuilder()
                .setInstruction(new WriteMetadataCaseBuilder()
                        .setWriteMetadata(new WriteMetadataBuilder()
                                .setMetadata(metadata)
                                .setMetadataMask(mask)
                                .build()
                        )
                        .build()
                )
                .setKey(new InstructionKey(instructionKey))
                .build();
    }

    public BigInteger getMetadata() {
        return metadata;
    }

    public BigInteger getMask() {
        return mask;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        InstructionWriteMetadata that = (InstructionWriteMetadata) other;

        if (metadata != null ? !metadata.equals(that.metadata) : that.metadata != null) {
            return false;
        }
        return mask != null ? mask.equals(that.mask) : that.mask == null;
    }

    @Override
    public int hashCode() {
        int result = metadata != null ? metadata.hashCode() : 0;
        result = 31 * result + (mask != null ? mask.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "InstructionWriteMetadata[metadata=" + metadata + ", mask=" + mask + "]";
    }
}
