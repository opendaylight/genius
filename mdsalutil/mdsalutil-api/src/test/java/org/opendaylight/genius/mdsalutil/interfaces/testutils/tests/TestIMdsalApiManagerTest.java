/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.interfaces.testutils.tests;

import com.google.common.collect.ImmutableList;
import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.opendaylight.genius.mdsalutil.FlowEntity;
import org.opendaylight.genius.mdsalutil.FlowEntityBuilder;
import org.opendaylight.genius.mdsalutil.instructions.InstructionClearActions;
import org.opendaylight.genius.mdsalutil.instructions.InstructionGotoTable;
import org.opendaylight.genius.mdsalutil.interfaces.testutils.TestIMdsalApiManager;
import org.opendaylight.genius.mdsalutil.matches.MatchArpSpa;
import org.opendaylight.yangtools.yang.common.Uint64;

/**
 * Unit test for {@link TestIMdsalApiManager}.
 * @author Michael Vorburger.ch
 */
public class TestIMdsalApiManagerTest {

    private final TestIMdsalApiManager mdsalApiManager = TestIMdsalApiManager.newInstance();

    @Test
    public void testEqualsAndHashCode() {
        new EqualsTester()
            .addEqualityGroup(getNewFlow1(), getNewFlow1())
            .addEqualityGroup(getNewFlow2(), getNewFlow2())
            .testEquals();
    }

    @Test
    public void testAssertFlowsInAnyOrder() {
        mdsalApiManager.installFlow(getNewFlow1());
        mdsalApiManager.installFlow(getNewFlow2());
        mdsalApiManager.assertFlows(ImmutableList.of(getNewFlow2(), getNewFlow1()));
        mdsalApiManager.assertFlowsInAnyOrder(ImmutableList.of(getNewFlow2(), getNewFlow1()));
    }

    private static FlowEntity getNewFlow1() {
        return new FlowEntityBuilder()
            .setDpnId(Uint64.valueOf(123))
            .setTableId((short) 1)
            .setCookie(Uint64.valueOf(456789))
            .setFlowId("ThisIsFlow1")
            .setHardTimeOut(456)
            .setIdleTimeOut(789)
            .setPriority(1)
            .setSendFlowRemFlag(true)
            .setStrictFlag(true)
            .addInstructionInfoList(
                new InstructionClearActions(), new InstructionGotoTable((short) 2))
            .addMatchInfoList(new MatchArpSpa("192.168.1.1", "24"))
        .build();
    }

    private static FlowEntity getNewFlow2() {
        return new FlowEntityBuilder()
                .setDpnId(Uint64.valueOf(321))
                .setTableId((short) 2)
                .setCookie(Uint64.valueOf(987654))
                .setFlowId("ThisIsFlow2")
                .setHardTimeOut(654)
                .setIdleTimeOut(987)
                .setPriority(2)
                .setSendFlowRemFlag(false)
                .setStrictFlag(false)
                .addInstructionInfoList(
                    new InstructionGotoTable((short) 1), new InstructionClearActions())
                .addMatchInfoList(new MatchArpSpa("10.11.12.30", "24"))
            .build();
    }

}
