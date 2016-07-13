/*
 * Copyright (c) 2016 RedHat Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.yangtools.concepts.Builder;

/**
 * Builder for MatchInfoBase sub types.
 */
public abstract class AbstractMatchInfoBaseBuilder<T extends MatchInfoBase> implements Builder<T> {

    protected List<Long> matchValues = new ArrayList<>();
    protected List<BigInteger> bigMatchValues = new ArrayList<>();
    protected List<String> stringMatchValues = new ArrayList<>();

    public abstract T build();

    public List<Long> getMatchValues() {
        return matchValues;
    }

    public List<BigInteger> getBigMatchValues() {
        return bigMatchValues;
    }

    public List<String> getStringMatchValues() {
        return stringMatchValues;
    }

}
