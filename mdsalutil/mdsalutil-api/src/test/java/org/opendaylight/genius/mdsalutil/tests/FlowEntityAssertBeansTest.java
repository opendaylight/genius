/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.tests;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.opendaylight.mdsal.binding.testutils.AssertDataObjects.assertEqualBeans;

import ch.vorburger.xtendbeans.XtendBeanGenerator;
import org.junit.ComparisonFailure;
import org.junit.Test;
import org.opendaylight.genius.mdsalutil.FlowEntity;
import org.opendaylight.genius.mdsalutil.FlowEntityBuilder;
import org.opendaylight.genius.mdsalutil.matches.MatchIpv4Source;
import org.opendaylight.yangtools.yang.common.Uint64;

/**
 * Tests non-regression of FlowEntity related assertions.
 * See <a href="https://bugs.opendaylight.org/show_bug.cgi?id=8800">Bug 8800</a>.
 * @author Michael Vorburger.ch
 */
public class FlowEntityAssertBeansTest {

    // FLOW1 & FLOW2 differ by a minor variance in their MatchIpv4Source (NB .1 vs .2)
    private static final FlowEntity FLOW1 = new FlowEntityBuilder()
            .addMatchInfoList(new MatchIpv4Source("10.0.0.1", "32")).setFlowId("A").setTableId((short) 1)
            .setDpnId(Uint64.ONE).build();
    private static final FlowEntity FLOW2 = new FlowEntityBuilder()
            .addMatchInfoList(new MatchIpv4Source("10.0.0.2", "32")).setFlowId("A").setTableId((short) 1)
            .setDpnId(Uint64.ONE).build();

    @Test(expected = ComparisonFailure.class)
    public void testFlowEntityAssertEquals() {
        assertEquals(FLOW1.toString(), FLOW2.toString());
    }

    @Test
    public void testXtendBeanGenerator() {
        XtendBeanGenerator generator = new UintXtendBeanGenerator();
        assertThat(FLOW1.toString()).isEqualTo("FlowEntity{dpnId=1, cookie=1114112, flowId=A, hardTimeOut=0, " +
                "idleTimeOut=0, " + "instructionInfoList=[]," +
                " matchInfoList=[MatchIpv4Source[Ipv4Prefix{_value=10.0.0.1/32}]]," +
                " priority=0, sendFlowRemFlag=false, strictFlag=false, tableId=1}");
    }

}
