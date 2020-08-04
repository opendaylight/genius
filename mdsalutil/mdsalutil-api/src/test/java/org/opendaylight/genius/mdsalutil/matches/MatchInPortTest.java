/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.matches;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.opendaylight.yangtools.yang.common.Uint64;

public class MatchInPortTest {

    @Test
    public void testEqualsAndHashCode() {
        new EqualsTester()
            .addEqualityGroup(newMatchInPort1(), newMatchInPort1())
            .addEqualityGroup(newMatchInPort2(), newMatchInPort2())
            .testEquals();
    }

    private static MatchInPort newMatchInPort1() {
        return new MatchInPort(Uint64.ONE, 123);
    }

    private static MatchInPort newMatchInPort2() {
        return new MatchInPort(Uint64.valueOf(10), 456);
    }
}
