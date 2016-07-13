/*
 * Copyright (c) 2016 Red Hat and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil;

import com.google.common.primitives.Longs;
import java.math.BigInteger;

/**
 * Builder for MatchInfo.
 * This class, even if not directly called from anywhere statically, is needed
 * by the XtendBeanGenerator in order to be able to generate code which creates
 * MatchInfo instances.
 */
public class MatchInfoBuilder extends AbstractMatchInfoBaseBuilder<MatchInfo> {

    private MatchFieldType matchField;

    @Override
    public MatchInfo build() {
        if (matchField == null) {
            throw new IllegalStateException("matchField must be set");
        } else if (!matchValues.isEmpty() && bigMatchValues.isEmpty() && stringMatchValues.isEmpty()) {
            return new MatchInfo(matchField, Longs.toArray(matchValues));
        } else if (matchValues.isEmpty() && !bigMatchValues.isEmpty() && stringMatchValues.isEmpty()) {
            return new MatchInfo(matchField, bigMatchValues.toArray(new BigInteger[] {}));
        } else if (matchValues.isEmpty() && bigMatchValues.isEmpty() && !stringMatchValues.isEmpty()) {
            return new MatchInfo(matchField, stringMatchValues.toArray(new String[] {}));
        } else {
            throw new IllegalStateException("Can only use either matchValues or bigMatchValues or stringMatchValues");
        }
    }

    public MatchFieldType getMatchField() {
        return matchField;
    }

    public void setMatchField(MatchFieldType matchField) {
        this.matchField = matchField;
    }

}
