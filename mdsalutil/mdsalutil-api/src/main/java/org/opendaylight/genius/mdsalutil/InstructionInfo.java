/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil;

import com.google.common.base.MoreObjects;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.opendaylight.genius.utils.MoreObjects2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;

public class InstructionInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private final InstructionType m_instructionType;

    private long[] m_alInstructionValues;
    private BigInteger[] m_alBigInstructionValues;
    private List<ActionInfo> m_actionInfos;

    // This constructor should be used incase of clearAction
    public InstructionInfo(InstructionType instructionType) {
        m_instructionType = instructionType;
    }

    public InstructionInfo(InstructionType instructionType, long[] instructionValues) {
        m_instructionType = instructionType;
        m_alInstructionValues = instructionValues;
    }

    public InstructionInfo(InstructionType instructionType, BigInteger[] instructionValues) {
        m_instructionType = instructionType;
        m_alBigInstructionValues = instructionValues;
    }

    public InstructionInfo(InstructionType instructionType, List<ActionInfo> actionInfos) {
        m_instructionType = instructionType;
        m_actionInfos = actionInfos;
    }

    public Instruction buildInstruction(int instructionKey) {
        return m_instructionType.buildInstruction(this, instructionKey);
    }

    public InstructionType getInstructionType() {
        return m_instructionType;
    }

    public long[] getInstructionValues() {
        return m_alInstructionValues;
    }

    public BigInteger[] getBigInstructionValues() {
        return m_alBigInstructionValues;
    }

    public List<ActionInfo> getActionInfos() {
        return m_actionInfos;
    }

    public void setInstructionValues(long[] m_alInstructionValues) {
        this.m_alInstructionValues = m_alInstructionValues;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).omitNullValues().add("instructionType", m_instructionType)
                .add("instructionValues", Arrays.toString(m_alInstructionValues))
                .add("bigInstructionValues", Arrays.deepToString(m_alBigInstructionValues))
                .add("actionInfos", m_actionInfos).toString();
    }

    @Override
    public int hashCode() {
        // BEWARE, Caveat Emptor: Array ([]) type fields must use
        // Arrays.hashCode(). deepHashCode() would have to be used for nested
        // arrays.
        return Objects.hash(m_instructionType, Arrays.hashCode(m_alInstructionValues),
                Arrays.hashCode(m_alBigInstructionValues), m_actionInfos);
    }

    @Override
    public boolean equals(Object obj) {
        // BEWARE, Caveat Emptor: Array ([]) type fields must use
        // Arrays.equals(). deepEquals() would have to be used for nested
        // arrays. Use == only for primitive types; if ever changing
        // those field types, must change to Objects.equals.
        return MoreObjects2.equalsHelper(this, obj,
            (self, other) -> Objects.equals(self.m_instructionType, other.m_instructionType)
                          && Arrays.equals(self.m_alInstructionValues, other.m_alInstructionValues)
                          && Arrays.equals(self.m_alBigInstructionValues, other.m_alBigInstructionValues)
                          && Objects.equals(self.m_actionInfos, other.m_actionInfos));
    }
}
