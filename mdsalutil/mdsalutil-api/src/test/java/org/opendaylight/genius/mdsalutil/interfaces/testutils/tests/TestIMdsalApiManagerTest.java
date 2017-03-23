/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.interfaces.testutils.tests;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.testing.EqualsTester;
import java.math.BigInteger;
import java.util.List;
import org.junit.ComparisonFailure;
import org.junit.Test;
import org.opendaylight.genius.mdsalutil.FlowEntity;
import org.opendaylight.genius.mdsalutil.InstructionInfo;
import org.opendaylight.genius.mdsalutil.MatchInfoBase;
import org.opendaylight.genius.mdsalutil.instructions.InstructionClearActions;
import org.opendaylight.genius.mdsalutil.instructions.InstructionGotoTable;
import org.opendaylight.genius.mdsalutil.interfaces.testutils.TestIMdsalApiManager;
import org.opendaylight.genius.mdsalutil.matches.MatchArpSpa;
import org.opendaylight.genius.mdsalutil.matches.MatchIpProtocol;
import org.opendaylight.genius.mdsalutil.matches.MatchVlanVid;

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
        mdsalApiManager.assertFlowsInAnyOrder(ImmutableList.of(getNewFlow2(), getNewFlow1()));
        mdsalApiManager.assertFlows(ImmutableList.of(getNewFlow1(), getNewFlow2()));
    }

    @Test(expected = ComparisonFailure.class)
    public void testFailingAssertion() {
        mdsalApiManager.installFlow(getNewFlow1());
        mdsalApiManager.assertFlows(ImmutableList.of(getNewFlow2()));
    }

    @Test(expected = ComparisonFailure.class)
    public void testFailingAssertionInAnyOrder() {
        mdsalApiManager.installFlow(getNewFlow1());
        mdsalApiManager.assertFlowsInAnyOrder(ImmutableList.of(getNewFlow2()));
    }

    @Test
    public void testInstallingIdenticalFlowTwice() {
        mdsalApiManager.installFlow(getNewFlow1());
        mdsalApiManager.installFlow(getNewFlow2());
        mdsalApiManager.installFlow(getNewFlow1());
        mdsalApiManager.assertFlowsInAnyOrder(ImmutableList.of(getNewFlow2(), getNewFlow1()));
        mdsalApiManager.assertFlows(ImmutableList.of(getNewFlow1(), getNewFlow2()));
    }

    @Test
    public void testDeepEqualsNotLogicalKey() {
        assertThat(getNewFlow1()).isNotEqualTo(getNewFlowWithEqualLogicalIdentityTo1());
    }

    @Test
    public void testInstallingSimilarFlowWithEqualLogicalIdentitiyTwice() {
        mdsalApiManager.installFlow(getNewFlow1());
        mdsalApiManager.installFlow(getNewFlow2());
        mdsalApiManager.installFlow(getNewFlowWithEqualLogicalIdentityTo1());
        mdsalApiManager.assertFlowsInAnyOrder(ImmutableList.of(getNewFlow2(), getNewFlowWithEqualLogicalIdentityTo1()));
        mdsalApiManager.assertFlows(ImmutableList.of(getNewFlowWithEqualLogicalIdentityTo1(), getNewFlow2()));
    }

    @Test
    public void testInstallingSimilarFlowWithEqualLogicalIdentitiyButDifferentDpnIdTwice() {
        mdsalApiManager.installFlow(getNewFlow1(456));
        mdsalApiManager.installFlow(getNewFlow2());
        mdsalApiManager.installFlow(getNewFlowWithEqualLogicalIdentityTo1());
        mdsalApiManager.assertFlowsInAnyOrder(
                ImmutableList.of(getNewFlow2(), getNewFlow1(456), getNewFlowWithEqualLogicalIdentityTo1()));
        mdsalApiManager.assertFlows(
                ImmutableList.of(getNewFlow1(456), getNewFlow2(), getNewFlowWithEqualLogicalIdentityTo1()));
    }

    @Test
    public void testInstallingNearIdenticalDifferentDpnFlowTwice() {
        mdsalApiManager.installFlow(getNewFlow1(123));
        mdsalApiManager.installFlow(getNewFlow2());
        mdsalApiManager.installFlow(getNewFlow1(456));
        mdsalApiManager.assertFlowsInAnyOrder(ImmutableList.of(getNewFlow1(456), getNewFlow2(), getNewFlow1(123)));
        mdsalApiManager.assertFlows(ImmutableList.of(getNewFlow1(123), getNewFlow2(), getNewFlow1(456)));
    }

    private FlowEntity getNewFlow1() {
        return getNewFlow1(123);
    }

    private FlowEntity getNewFlow1(long dpnId) {
        FlowEntity flow1 = new FlowEntity(dpnId);
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
                new MatchVlanVid(17),
                new MatchArpSpa("192.168.1.1", "24"));
        flow1.setMatchInfoList(matchInfos);
        return flow1;
    }

    /**
     * Get flow with the same "logical key" as the {@link #getNewFlow1()}.
     * It is not Java equals() to flow1; but, see OpenFlow specification: <i>"A flow table
     * entry is identified by its match fields and priority: the match fields
     * and priority taken together identify a unique flow entry in the flow
     * table."</i>
     */
    private FlowEntity getNewFlowWithEqualLogicalIdentityTo1() {
        FlowEntity flow1 = new FlowEntity(123);
        flow1.setTableId(1);
        flow1.setCookie(BigInteger.valueOf(987654));
        flow1.setFlowId("ThisIsFlow1-modified");
        flow1.setHardTimeOut(789);
        flow1.setIdleTimeOut(456);
        flow1.setPriority(1);
        flow1.setSendFlowRemFlag(false);
        flow1.setStrictFlag(false);
        List<InstructionInfo> instructionInfos = ImmutableList.of(
                new InstructionClearActions(), new InstructionGotoTable((short) 7));
        flow1.setInstructionInfoList(instructionInfos);
        List<MatchInfoBase> matchInfos = ImmutableList.of(
                // NB: Order of matches intentionally inversed
                new MatchVlanVid(17),
                new MatchArpSpa("192.168.1.1", "24"));
        flow1.setMatchInfoList(matchInfos);
        return flow1;
    }

    private FlowEntity getNewFlow2() {
        FlowEntity flow2 = new FlowEntity(321);
        flow2.setTableId(2);
        flow2.setCookie(BigInteger.valueOf(987654));
        flow2.setFlowId("ThisIsFlow2");
        flow2.setHardTimeOut(654);
        flow2.setIdleTimeOut(987);
        flow2.setPriority(2);
        flow2.setSendFlowRemFlag(false);
        flow2.setStrictFlag(false);
        List<InstructionInfo> instructionInfos = ImmutableList.of(
                new InstructionGotoTable((short) 1), new InstructionClearActions());
        flow2.setInstructionInfoList(instructionInfos);
        List<MatchInfoBase> matchInfos = ImmutableList.of(MatchIpProtocol.ICMPV6);
        flow2.setMatchInfoList(matchInfos);
        return flow2;
    }

}
