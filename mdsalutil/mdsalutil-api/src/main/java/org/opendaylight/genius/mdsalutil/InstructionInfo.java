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

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;

public class InstructionInfo extends AbstractActionInfoList implements Serializable {

    private static final long serialVersionUID = 1L;

    private final InstructionType m_instructionType;
    private final long[] m_alInstructionValues;
    private final BigInteger[] m_alBigInstructionValues;

    // This constructor should be used incase of clearAction
    public InstructionInfo(InstructionType instructionType) {
        super(null);
        m_instructionType = instructionType;
        m_alInstructionValues = null;
        m_alBigInstructionValues = null;
    }

    public InstructionInfo(InstructionType instructionType, long[] instructionValues) {
        super(null);
        m_instructionType = instructionType;
        m_alInstructionValues = instructionValues;
        m_alBigInstructionValues = null;
    }

    public InstructionInfo(InstructionType instructionType, BigInteger[] instructionValues) {
        super(null);
        m_instructionType = instructionType;
        m_alInstructionValues = null;
        m_alBigInstructionValues = instructionValues;
    }

    public InstructionInfo(InstructionType instructionType, List<ActionInfo> actionInfos) {
        super(actionInfos);
        m_instructionType = instructionType;
        m_alInstructionValues = null;
        m_alBigInstructionValues = null;
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

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).omitNullValues().add("instructionType", m_instructionType)
                .add("instructionValues", Arrays.toString(m_alInstructionValues))
                .add("bigInstructionValues", Arrays.deepToString(m_alBigInstructionValues))
                .add("actionInfos", getActionInfos()).toString();
    }
}
