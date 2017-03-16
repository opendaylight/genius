/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.interfaces.testutils;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.mdsal.binding.testutils.AssertDataObjects.assertEqualBeans;
import static org.opendaylight.yangtools.testutils.mockito.MoreAnswers.realOrException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.mdsalutil.FlowEntity;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.infrautils.utils.CompareUtil;
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
 */
public abstract class TestIMdsalApiManager implements IMdsalApiManager {

    private static final Logger LOG = LoggerFactory.getLogger(TestIMdsalApiManager.class);

    private List<FlowEntity> flows;

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
            // We LOG the expected and actual flow in case of a failed assertion
            // because, even though that is typically just a HUGE String that's
            // hard to read (the diff printed subsequently by assertEqualBeans
            // is, much, more readable), there are cases when looking more closely
            // at the full toString() output of the flows is still useful, so:
            LOG.warn("assert fail; expected flows: {}", sortedExpectedFlows);
            LOG.warn("assert fail; actual flows  : {}", sortedFlows);
            // The point of this is basically just that our assertEqualBeans output,
            // in case of a comparison failure, is *A LOT* more clearly readable
            // than what G Truth (or Hamcrest) can do based on toString.
            assertEqualBeans(sortedExpectedFlows, sortedFlows);
        }
    }

    private List<FlowEntity> sortFlows(Iterable<FlowEntity> flows) {
        List<FlowEntity> sortedFlows = Lists.newArrayList(flows);
        Collections.sort(sortedFlows,
            (flow1, flow2) -> CompareUtil.safeCompareTo(flow1.getTableId(), flow2.getTableId()));
            // TODO next also compare to getPriority() etc.
        return sortedFlows;
    }

    @Override
    public synchronized void installFlow(FlowEntity flowEntity) {
        getOrNewFlows().add(flowEntity);
    }

    @Override
    public synchronized CheckedFuture<Void, TransactionCommitFailedException> removeFlow(BigInteger dpnId,
            FlowEntity flowEntity) {
        getOrNewFlows().remove(flowEntity);
        return Futures.immediateCheckedFuture(null);
    }

    @Override
    public void batchedAddFlow(BigInteger dpId, FlowEntity flowEntity) {
        getOrNewFlows().add(flowEntity);
    }

    @Override
    public void batchedRemoveFlow(BigInteger dpId, FlowEntity flowEntity) {
        getOrNewFlows().remove(flowEntity);
    }

}
