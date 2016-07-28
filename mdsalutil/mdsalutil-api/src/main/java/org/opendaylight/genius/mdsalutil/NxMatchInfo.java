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

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;

/**
 *  This class defines the nicira extension matches.
 */
public class NxMatchInfo implements Serializable, MatchInfoBase {

    private static final long serialVersionUID = 1L;

    private final NxMatchFieldType m_matchField;
    private final long[] m_alMatchValues;
    private final BigInteger[] m_aBigIntValues;
    private final String[] m_asMatchValues;

    public NxMatchInfo(NxMatchFieldType matchField, long[] alMatchValues) {
        m_matchField = matchField;
        m_alMatchValues = alMatchValues;
        m_aBigIntValues = null;
        m_asMatchValues = null;
    }

    public NxMatchInfo(NxMatchFieldType matchField, BigInteger[] alBigMatchValues) {
        m_matchField = matchField;
        m_alMatchValues = null;
        m_aBigIntValues = alBigMatchValues;
        m_asMatchValues = null;
    }

    public NxMatchInfo(NxMatchFieldType matchField, String[] alStringMatchValues) {
        m_matchField = matchField;
        m_alMatchValues = null;
        m_aBigIntValues = null;
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
}
