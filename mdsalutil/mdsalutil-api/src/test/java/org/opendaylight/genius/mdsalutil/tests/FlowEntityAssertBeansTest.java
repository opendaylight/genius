/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.tests;

import static com.google.common.truth.Truth.assertThat;
import static org.opendaylight.mdsal.binding.testutils.AssertDataObjects.assertEqualBeans;

import ch.vorburger.xtendbeans.XtendBeanGenerator;
import java.math.BigInteger;
import org.junit.ComparisonFailure;
import org.junit.Test;
import org.opendaylight.genius.mdsalutil.FlowEntity;
import org.opendaylight.genius.mdsalutil.FlowEntityBuilder;
import org.opendaylight.genius.mdsalutil.matches.MatchIpv4Source;

/**
 * Tests non-regression of FlowEntity related assertions.
 * See <a href="Bug 8800">https://bugs.opendaylight.org/show_bug.cgi?id=8800</a>
 * @author Michael Vorburger.ch
 */
public class FlowEntityAssertBeansTest {

    // FLOW1 & FLOW2 differ by a minor variance in their MatchIpv4Source (NB .1 vs .2)
    private static final FlowEntity FLOW1 = new FlowEntityBuilder()
            .addMatchInfoList(new MatchIpv4Source("10.0.0.1", "32")).setFlowId("A").setTableId((short) 1)
            .setDpnId(BigInteger.ONE).build();
    private static final FlowEntity FLOW2 = new FlowEntityBuilder()
            .addMatchInfoList(new MatchIpv4Source("10.0.0.2", "32")).setFlowId("A").setTableId((short) 1)
            .setDpnId(BigInteger.ONE).build();

    @Test(expected = ComparisonFailure.class)
    public void testFlowEntityAssertEqualBeans() {
        assertEqualBeans(FLOW1, FLOW2);
    }

    @Test
    public void testXtendBeanGenerator() {
        XtendBeanGenerator generator = new XtendBeanGenerator();
        assertThat(generator.getExpression(FLOW1)).isEqualTo("(new FlowEntityBuilder => [\n"
                + "    cookie = 1114112bi\n"
                + "    dpnId = 1bi\n"
                + "    flowId = \"A\"\n"
                + "    hardTimeOut = 0\n"
                + "    idleTimeOut = 0\n"
                + "    instructionInfoList = #[\n"
                + "    ]\n"
                + "    matchInfoList = #[\n"
                + "        new MatchIpv4Source(new Ipv4Prefix(\"10.0.0.1/32\"))\n"
                + "    ]\n"
                + "    priority = 0\n"
                + "    sendFlowRemFlag = false\n"
                + "    strictFlag = false\n"
                + "    tableId = 1 as short\n"
                + "]).build()");
    }

}
