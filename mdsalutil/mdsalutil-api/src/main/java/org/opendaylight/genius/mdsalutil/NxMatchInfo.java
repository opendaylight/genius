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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yangtools.util.EvenMoreObjects;

/**
 *  This class defines the nicira extension matches.
 */
public class NxMatchInfo implements Serializable, MatchInfoBase {

    private static final long serialVersionUID = 1L;

    private final NxMatchFieldType matchField;
    private final long[] alMatchValues;
    private final BigInteger[] bigIntValues;
    private final String[] asMatchValues;

    public NxMatchInfo(NxMatchFieldType matchField, long[] alMatchValues) {
        this.matchField = matchField;
        this.alMatchValues = alMatchValues;
        this.bigIntValues = null;
        this.asMatchValues = null;
    }

    public NxMatchInfo(NxMatchFieldType matchField, BigInteger[] alBigMatchValues) {
        this.matchField = matchField;
        this.alMatchValues = null;
        this.bigIntValues = alBigMatchValues;
        this.asMatchValues = null;
    }

    public NxMatchInfo(NxMatchFieldType matchField, String[] alStringMatchValues) {
        this.matchField = matchField;
        this.alMatchValues = null;
        this.bigIntValues = null;
        this.asMatchValues = alStringMatchValues;
    }

    @Override
    public void createInnerMatchBuilder(Map<Class<?>, Object> mapMatchBuilder) {
        this.matchField.createInnerMatchBuilder(this, mapMatchBuilder);
    }

    @Override
    public void setMatch(MatchBuilder matchBuilder, Map<Class<?>, Object> mapMatchBuilder) {
        this.matchField.setMatch(matchBuilder, this, mapMatchBuilder);
    }

    public NxMatchFieldType getMatchField() {
        return matchField;
    }

    public long[] getMatchValues() {
        return alMatchValues;
    }

    public BigInteger[] getBigMatchValues() {
        return bigIntValues;
    }

    public String[] getStringMatchValues() {
        return asMatchValues;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).omitNullValues().add("matchField", matchField)
                .add("matchValues", Arrays.toString(alMatchValues)).add("bigMatchValues", bigIntValues)
                .add("stringMatchValues", Arrays.deepToString(asMatchValues)).toString();
    }

    @Override
    public int hashCode() {
        // BEWARE, Caveat Emptor: Array ([]) type fields must use
        // Arrays.hashCode(). deepHashCode() would have to be used for nested
        // arrays.
        return Objects.hash(matchField, Arrays.hashCode(alMatchValues),
                Arrays.hashCode(bigIntValues), Arrays.hashCode(asMatchValues));
    }

    @Override
    public boolean equals(Object obj) {
        // BEWARE, Caveat Emptor: Array ([]) type fields must use
        // Arrays.equals(). deepEquals() would have to be used for nested
        // arrays. Use == only for primitive types; if ever changing
        // those field types, must change to Objects.equals.
        return EvenMoreObjects.equalsHelper(this, obj,
            (self, other) -> Objects.equals(self.matchField, other.matchField)
                          && Arrays.equals(self.alMatchValues, other.alMatchValues)
                          && Arrays.equals(self.bigIntValues, other.bigIntValues)
                          && Arrays.equals(self.asMatchValues, other.asMatchValues));
    }

}
