/*
 * Copyright (c) 2016, 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.interfaces.testutils;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opendaylight.mdsal.binding.testutils.AssertDataObjects.assertEqualBeans;
import static org.opendaylight.yangtools.testutils.mockito.MoreAnswers.realOrException;

import ch.vorburger.xtendbeans.XtendBeanGenerator;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.junit.ComparisonFailure;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.mdsalutil.FlowEntity;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fake IMdsalApiManager useful for tests.
 *
 * <p>Read e.g.
 * http://googletesting.blogspot.ch/2013/07/testing-on-toilet-know-your-test-doubles.html
 * and http://martinfowler.com/articles/mocksArentStubs.html for more background.
 *
 * <p>This class is abstract just to save reading lines and typing keystrokes to
 * manually implement a bunch of methods we're not yet interested in.  Create instances
 * of it using it's static {@link #newInstance()} method.
 *
 * @author Michael Vorburger
 * @autor Faseela K
 */
public abstract class TestIMdsalApiManager implements IMdsalApiManager {

    private static final Logger LOG = LoggerFactory.getLogger(TestIMdsalApiManager.class);

    private List<FlowEntity> flows;
    private List<Group> groups;

    public static TestIMdsalApiManager newInstance() {
        return Mockito.mock(TestIMdsalApiManager.class, realOrException());
    }

    /**
     * Get list of installed flows.
     * Prefer the {@link #assertFlows(Iterable)} instead of using this and checking yourself.
     * @return immutable copy of list of flows
     */
    public synchronized List<FlowEntity> getFlows() {
        return ImmutableList.copyOf(getOrNewFlows());
    }

    private synchronized List<FlowEntity> getOrNewFlows() {
        if (flows == null) {
            flows = new ArrayList<>();
        }
        return flows;
    }

    private synchronized List<Group> getOrNewGroups() {
        if (groups == null) {
            groups = new ArrayList<>();
        }
        return groups;
    }

    public synchronized void assertFlows(Iterable<FlowEntity> expectedFlows) {
        checkNonEmptyFlows(expectedFlows);
        List<FlowEntity> nonNullFlows = getOrNewFlows();
        if (!Iterables.isEmpty(expectedFlows)) {
            assertTrue("No Flows created (bean wiring may be broken?)", !nonNullFlows.isEmpty());
        }
        // TODO Support Iterable <-> List directly within XtendBeanGenerator
        List<FlowEntity> expectedFlowsAsNewArrayList = Lists.newArrayList(expectedFlows);
        assertEqualBeans(expectedFlowsAsNewArrayList, nonNullFlows);
    }


    private synchronized void checkNonEmptyFlows(Iterable<FlowEntity> expectedFlows) {
        if (!Iterables.isEmpty(expectedFlows)) {
            assertTrue("No Flows created (bean wiring may be broken?)", !getOrNewFlows().isEmpty());
        }
    }

    // ComparisonException doesn’t allow us to keep the cause (which we don’t care about anyway)
    @SuppressWarnings("checkstyle:AvoidHidingCauseException")
    public synchronized void assertFlowsInAnyOrder(Iterable<FlowEntity> expectedFlows) {
        checkNonEmptyFlows(expectedFlows);
        // TODO Support Iterable <-> List directly within XtendBeanGenerator
        List<FlowEntity> expectedFlowsAsNewArrayList = Lists.newArrayList(expectedFlows);

        List<FlowEntity> sortedFlows = sortFlows(flows);
        List<FlowEntity> sortedExpectedFlows = sortFlows(expectedFlowsAsNewArrayList);

        // FYI: This containsExactlyElementsIn() assumes that FlowEntity, and everything in it,
        // has correctly working equals() implementations.  assertEqualBeans() does not assume
        // that, and would work even without equals, because it only uses property reflection.
        // Normally this will lead to the same result, but if one day it doesn't (because of
        // a bug in an equals() implementation somewhere), then it's worth to keep this diff
        // in mind.

        // FTR: This use of G Truth and then catch AssertionError and using assertEqualBeans iff NOK
        // (thus discarding the message from G Truth) is a bit of a hack, but it works well...
        // If you're tempted to improve this, please remember that correctly re-implementing
        // containsExactlyElementsIn (or Hamcrest's similar containsInAnyOrder) isn't a 1 line
        // trivia... e.g. a.containsAll(b) && b.containsAll(a) isn't sufficient, because it
        // won't work for duplicates (which we frequently have here); and ordering before is
        // not viable because FlowEntity is not Comparable, and Comparator based on hashCode
        // is not a good idea (different instances can have same hashCode), and e.g. on
        // System#identityHashCode even less so.
        try {
            assertThat(sortedFlows).containsExactlyElementsIn(sortedExpectedFlows);
        } catch (AssertionError e) {
            // We LOG the AssertionError just for clarity why containsExactlyElementsIn() failed
            LOG.warn("assert containsExactlyElementsIn() failed", e);
            // We LOG the expected and actual flow in case of a failed assertion
            // because, even though that is typically just a HUGE String that's
            // hard to read (the diff printed subsequently by assertEqualBeans
            // is, much, more readable), there are cases when looking more closely
            // at the full toString() output of the flows is still useful, so:
            // TIP: Use e.g. 'wdiff -n expected.txt actual.txt | colordiff' to compare these!
            LOG.warn("assert failed [order ignored!]; expected flows: {}", sortedExpectedFlows);
            LOG.warn("assert failed [order ignored!]; actual flows  : {}", sortedFlows);
            // The point of now also doing assertEqualBeans() is just that its output,
            // in case of a comparison failure, is *A LOT* more clearly readable
            // than what G Truth (or Hamcrest) can do based on toString.
            assertEqualBeans(sortedExpectedFlows, sortedFlows);
            if (sortedExpectedFlows.toString().equals(sortedFlows.toString())
                    && !sortedExpectedFlows.equals(sortedFlows)) {
                fail("Suspected toString, missing getter, equals (hashCode) bug in FlowEntity related class!!! :-(");
            }
            throw new ComparisonFailure(
                    "assertEqualBeans() MUST fail - given that the assertThat.containsExactlyElementsIn() just failed!"
                    // Beware, we're using XtendBeanGenerator instead of XtendYangBeanGenerator like in
                    // AssertDataObjects, but for FlowEntity it's the same... it only makes a difference for DataObjects
                    + " What is missing in: " + new XtendBeanGenerator().getExpression(sortedFlows),
                    sortedExpectedFlows.toString(), sortedFlows.toString());
            // If this ^^^ occurs, then there is probably a bug in ch.vorburger.xtendbeans
        }
    }

    private List<FlowEntity> sortFlows(Iterable<FlowEntity> flowsToSort) {
        List<FlowEntity> sortedFlows = Lists.newArrayList(flowsToSort);
        sortedFlows.sort((flow1, flow2) -> ComparisonChain.start()
                .compare(flow1.getTableId(), flow2.getTableId())
                .compare(flow1.getPriority(), flow2.getPriority())
                .compare(flow1.getFlowId(), flow2.getFlowId())
                .result());
        return sortedFlows;
    }

    @Override
    public synchronized CheckedFuture<Void, TransactionCommitFailedException> installFlow(FlowEntity flowEntity) {
        getOrNewFlows().add(flowEntity);
        return Futures.immediateCheckedFuture(null);
    }

    @Override
    public synchronized CheckedFuture<Void, TransactionCommitFailedException> installFlow(BigInteger dpId,
            FlowEntity flowEntity) {
        // TODO should dpId be considered here? how? Copy clone FlowEntity and change its dpId?
        return installFlow(flowEntity);
    }

    @Override
    public synchronized CheckedFuture<Void, TransactionCommitFailedException> removeFlow(BigInteger dpnId,
            FlowEntity flowEntity) {
        // TODO should dpId be considered here? how? Copy clone FlowEntity and change its dpId?
        getOrNewFlows().remove(flowEntity);
        return Futures.immediateCheckedFuture(null);
    }

    @Override
    public synchronized void batchedAddFlow(BigInteger dpId, FlowEntity flowEntity) {
        getOrNewFlows().add(flowEntity);
    }

    @Override
    public synchronized void batchedRemoveFlow(BigInteger dpId, FlowEntity flowEntity) {
        getOrNewFlows().remove(flowEntity);
    }

    @Override
    public void syncInstallGroup(BigInteger dpId, Group group, long delayTime) {
        getOrNewGroups().add(group);
    }

    @Override
    public void syncInstallGroup(BigInteger dpId, Group group) {
        getOrNewGroups().add(group);
    }

    @Override
    public void syncRemoveGroup(BigInteger dpId, Group groupEntity) {
        getOrNewGroups().remove(groupEntity);
    }

    @Override
    public void addFlowToTx(FlowEntity flowEntity, WriteTransaction tx) {
        getOrNewFlows().add(flowEntity);
    }

    @Override
    public void removeFlowToTx(FlowEntity flowEntity, WriteTransaction tx) {
        getOrNewFlows().remove(flowEntity);
    }
}
