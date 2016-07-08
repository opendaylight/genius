/*
 * Copyright (c) 2016 RedHat Inc. and others.  All rights reserved.
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
import java.util.Map;
import java.util.Objects;
import org.opendaylight.genius.utils.MoreObjects2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;

/**
 *  This class defines the nicira extension matches.
 */
public class NxMatchInfo implements Serializable, MatchInfoBase {

    private static final long serialVersionUID = 1L;

    private final NxMatchFieldType m_matchField;

    private long[] m_alMatchValues;
    private BigInteger[] m_aBigIntValues;
    private String[] m_asMatchValues;

    public NxMatchInfo(NxMatchFieldType matchField, long[] alMatchValues) {
        m_matchField = matchField;
        m_alMatchValues = alMatchValues;
    }

    public NxMatchInfo(NxMatchFieldType matchField, BigInteger[] alBigMatchValues) {
        m_matchField = matchField;
        m_aBigIntValues = alBigMatchValues;
    }

    public NxMatchInfo(NxMatchFieldType matchField, String[] alStringMatchValues) {
        m_matchField = matchField;
        m_asMatchValues = alStringMatchValues;
    }

    @Override
    public void createInnerMatchBuilder(Map<Class<?>, Object> mapMatchBuilder) {
        m_matchField.createInnerMatchBuilder(this, mapMatchBuilder);
    }

    @Override
    public void setMatch(MatchBuilder matchBuilder, Map<Class<?>, Object> mapMatchBuilder) {
        m_matchField.setMatch(matchBuilder, this, mapMatchBuilder);
    }

    public NxMatchFieldType getMatchField() {
        return m_matchField;
    }

    public long[] getMatchValues() {
        return m_alMatchValues;
    }

    public BigInteger[] getBigMatchValues() {
        return m_aBigIntValues;
    }

    public String[] getStringMatchValues() {
        return m_asMatchValues;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).omitNullValues().add("matchField", m_matchField)
                .add("matchValues", Arrays.toString(m_alMatchValues)).add("bigMatchValues", m_aBigIntValues)
                .add("stringMatchValues", Arrays.deepToString(m_asMatchValues)).toString();
    }

    @Override
    public int hashCode() {
        // BEWARE, Caveat Emptor: Array ([]) type fields must use
        // Arrays.hashCode(). deepHashCode() would have to be used for nested
        // arrays.
        return Objects.hash(m_matchField, Arrays.hashCode(m_alMatchValues),
                Arrays.hashCode(m_aBigIntValues), Arrays.hashCode(m_asMatchValues));
    }

    @Override
    public boolean equals(Object obj) {
        // BEWARE, Caveat Emptor: Array ([]) type fields must use
        // Arrays.equals(). deepEquals() would have to be used for nested
        // arrays. Use == only for primitive types; if ever changing
        // those field types, must change to Objects.equals.
        return MoreObjects2.equalsHelper(this, obj,
            (self, other) -> Objects.equals(self.m_matchField, other.m_matchField)
                          && Arrays.equals(self.m_alMatchValues, other.m_alMatchValues)
                          && Arrays.equals(self.m_aBigIntValues, other.m_aBigIntValues)
                          && Arrays.equals(self.m_asMatchValues, other.m_asMatchValues));
    }

}
