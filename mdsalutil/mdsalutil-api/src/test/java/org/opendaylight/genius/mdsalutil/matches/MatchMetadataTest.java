/*
 * Copyright © 2017 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.matches;

import java.math.BigInteger;
import org.junit.Test;
import org.opendaylight.genius.mdsalutil.MatchInfo;

/**
 * Test for {@link MatchMetadata}.
 */
public class MatchMetadataTest {
    @Test
    public void backwardsCompatibleMatch() {

    }

    @Test
    public void newMatch() {
        verifyMatch(new MatchMetadata(BigInteger.ONE, BigInteger.TEN));
    }

    private void verifyMatch(MatchInfo matchInfo) {

    }
}
