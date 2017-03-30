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
        assertEquals(
            // with xtendbeans v1.2.3 instead of 1.2.2 it will finally correctly directly gen. this, if no Builder:
                // "new MatchIcmpv6(1 as short, 2 as short)",
            // in the mean time, it generates this (after fixing missing getters)
                "(new MatchIcmpv6Builder => [\n"
                + "    code = 2 as short\n"
                + "    type = 1 as short\n"
                + "]).build()",
                generator.getExpression(new MatchIcmpv6((short) 1, (short)2)));
    }

    @Test
    public void xtendBeanGeneratorTwoDefaultValues() {
        assertEquals(
            // with xtendbeans v1.2.3 instead of 1.2.2 it will finally correctly directly gen. this, if no Builder:
                // "new MatchIcmpv6(0 as short, 0 as short)",
            // in the mean time, it generates this (after fixing missing getters)
                "(new MatchIcmpv6Builder\n).build()",
                generator.getExpression(new MatchIcmpv6((short) 0, (short)0)));
    }

    @Test
    public void xtendBeanGeneratorOneDefaultValue() {
        assertEquals(
            // with xtendbeans v1.2.3 instead of 1.2.2 it will finally correctly directly gen. this, if no Builder:
                // "new MatchIcmpv6(0 as short, 3 as short)",
            // in the mean time, it generates this (after fixing missing getters)
                "(new MatchIcmpv6Builder => [\n"
                + "    code = 3 as short\n"
                + "]).build()",
                generator.getExpression(new MatchIcmpv6((short) 0, (short)3)));
    }

    @Test
    public void xtendBeanGeneratorOtherDefaultValue() {
        assertEquals(
            // with xtendbeans v1.2.3 instead of 1.2.2 it will finally correctly directly gen. this, if no Builder:
                // "new MatchIcmpv6(3 as short, 0 as short)",
            // in the mean time, it generates this (after fixing missing getters)
                "(new MatchIcmpv6Builder => [\n"
                + "    type = 3 as short\n"
                + "]).build()",
                generator.getExpression(new MatchIcmpv6((short) 3, (short)0)));
    }

}
