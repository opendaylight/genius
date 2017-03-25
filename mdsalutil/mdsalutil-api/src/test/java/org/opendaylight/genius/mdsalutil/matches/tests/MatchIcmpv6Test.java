/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.matches.tests;

import static org.junit.Assert.assertEquals;

import ch.vorburger.xtendbeans.XtendBeanGenerator;
import org.junit.Test;
import org.opendaylight.genius.mdsalutil.matches.MatchIcmpv6;

public class MatchIcmpv6Test {

    XtendBeanGenerator generator = new XtendBeanGenerator();

    @Test
    public void xtendBeanGeneratorTwoNonDefaultValues() {
        assertEquals("new MatchIcmpv6(1 as short, 2 as short)",
                generator.getExpression(new MatchIcmpv6((short) 1, (short)2)));
    }

    @Test
    public void xtendBeanGeneratorTwoDefaultValues() {
        assertEquals("new MatchIcmpv6",
                generator.getExpression(new MatchIcmpv6((short) 0, (short)0)));
    }

    @Test
    public void xtendBeanGeneratorOneDefaultValue() {
        assertEquals("new MatchIcmpv6(0 as short, 3 as short)",
                generator.getExpression(new MatchIcmpv6((short) 0, (short)3)));
    }

    @Test
    public void xtendBeanGeneratorOtherDefaultValue() {
        assertEquals("new MatchIcmpv6(3 as short, 0 as short)",
                generator.getExpression(new MatchIcmpv6((short) 3, (short)0)));
    }

}
