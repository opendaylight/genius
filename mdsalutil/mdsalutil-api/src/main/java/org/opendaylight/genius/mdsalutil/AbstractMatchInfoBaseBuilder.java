/*
 * Copyright (c) 2016 RedHat Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil;

import java.math.BigInteger;
import org.opendaylight.yangtools.concepts.Builder;

/**
 * Builder for MatchInfoBase sub types.
 */
public abstract class AbstractMatchInfoBaseBuilder<T extends MatchInfoBase> implements Builder<T> {

    // Don't use List here, but array, as in ActionInfo
    // Otherwise (with List) it's impossible for XtendBeanGenerator to deal w. null/empty List default values correctly
    protected long[] matchValues;
    protected BigInteger[] bigMatchValues;
    protected String[] stringMatchValues;

    public abstract T build();

    public long[] getMatchValues() {
        return matchValues;
    }

    public void setMatchValues(long[] matchValues) {
        this.matchValues = matchValues;
    }

    public BigInteger[] getBigMatchValues() {
        return bigMatchValues;
    }

    public void setBigMatchValues(BigInteger[] bigMatchValues) {
        this.bigMatchValues = bigMatchValues;
    }

    public String[] getStringMatchValues() {
        return stringMatchValues;
    }

    public void setStringMatchValues(String[] stringMatchValues) {
        this.stringMatchValues = stringMatchValues;
    }

}
