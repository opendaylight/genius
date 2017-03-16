/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.interfaces.testutils.tests;

import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.List;
import org.junit.Test;
import org.opendaylight.genius.mdsalutil.FlowEntity;
import org.opendaylight.genius.mdsalutil.InstructionInfo;
import org.opendaylight.genius.mdsalutil.MatchInfoBase;
import org.opendaylight.genius.mdsalutil.instructions.InstructionClearActions;
import org.opendaylight.genius.mdsalutil.instructions.InstructionGotoTable;
import org.opendaylight.genius.mdsalutil.interfaces.testutils.TestIMdsalApiManager;
import org.opendaylight.genius.mdsalutil.matches.MatchArpSpa;

/**
 * Unit test for {@link TestIMdsalApiManager}.
 * @author Michael Vorburger.ch
 */
public class TestIMdsalApiManagerTest {

    private final TestIMdsalApiManager mdsalApiManager = TestIMdsalApiManager.newInstance();

    @Test
    public void testAssertFlowsInAnyOrder() {
        mdsalApiManager.installFlow(getNewFlow1());
        mdsalApiManager.installFlow(getNewFlow2());
        mdsalApiManager.assertFlows(ImmutableList.of(getNewFlow1(), getNewFlow2()));
        mdsalApiManager.assertFlowsInAnyOrder(ImmutableList.of(getNewFlow2(), getNewFlow1()));
    }

    private FlowEntity getNewFlow1() {
        FlowEntity flow1 = new FlowEntity(123);
        flow1.setTableId(1);
        flow1.setCookie(BigInteger.valueOf(456789));
        flow1.setFlowId("ThisIsFlow1");
        flow1.setHardTimeOut(456);
        flow1.setIdleTimeOut(789);
        flow1.setPriority(1);
        flow1.setSendFlowRemFlag(true);
        flow1.setStrictFlag(true);
        List<InstructionInfo> instructionInfos = ImmutableList.of(
                new InstructionClearActions(), new InstructionGotoTable((short) 2));
        flow1.setInstructionInfoList(instructionInfos);
        List<MatchInfoBase> matchInfos = ImmutableList.of(
                new MatchArpSpa("192.168.1.1", "24"));
        flow1.setMatchInfoList(matchInfos);
        return flow1;
    }

    private FlowEntity getNewFlow2() {
        FlowEntity flow2 = new FlowEntity(321);
        flow2.setTableId(2);
        flow2.setCookie(BigInteger.valueOf(987654));
        flow2.setFlowId("ThisIsflow2");
        flow2.setHardTimeOut(654);
        flow2.setIdleTimeOut(987);
        flow2.setPriority(2);
        flow2.setSendFlowRemFlag(false);
        flow2.setStrictFlag(false);
        List<InstructionInfo> instructionInfos = ImmutableList.of(
                new InstructionGotoTable((short) 1), new InstructionClearActions());
        flow2.setInstructionInfoList(instructionInfos);
        List<MatchInfoBase> matchInfos = ImmutableList.of(
                new MatchArpSpa("10.11.12.30", "24"));
        flow2.setMatchInfoList(matchInfos);
        return flow2;
    }

}
