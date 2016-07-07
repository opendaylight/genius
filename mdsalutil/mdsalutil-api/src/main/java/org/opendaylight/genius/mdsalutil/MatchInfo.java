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
import java.util.Map;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;

public class MatchInfo implements Serializable, MatchInfoBase {
    private static final long serialVersionUID = 1L;

    private final MatchFieldType m_matchField;
    private long[] m_alMatchValues;
    private BigInteger[] m_aBigIntValues;
    private String[] m_asMatchValues;

    public MatchInfo(MatchFieldType matchField, long[] alMatchValues) {
        m_matchField = matchField;
        m_alMatchValues = alMatchValues;
    }

    public MatchInfo(MatchFieldType matchField, BigInteger[] alBigMatchValues) {
        m_matchField = matchField;
        m_aBigIntValues = alBigMatchValues;
    }

    public MatchInfo(MatchFieldType matchField, String[] alStringMatchValues) {
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

    public MatchFieldType getMatchField() {
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
                .add("matchValues", Arrays.toString(m_alMatchValues))
                .add("bigMatchValues", Arrays.deepToString(m_aBigIntValues))
                .add("stringMatchValues", Arrays.deepToString(m_asMatchValues)).toString();
    }
}
