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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.mdsalutil.FlowEntity;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.mdsal.binding.testutils.AssertDataObjects;
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

    private List<FlowEntity> getFlows() {
        if (flows == null) {
            flows = new ArrayList<>();
        }
        return flows;
    }

    public synchronized void assertFlows(Iterable<FlowEntity> expectedFlows) {
        checkNonEmptyFlows(expectedFlows);
        // TODO Support Iterable <-> List directly within XtendBeanGenerator
        List<FlowEntity> expectedFlowsAsNewArrayList = Lists.newArrayList(expectedFlows);
        AssertDataObjects.assertEqualBeans(expectedFlowsAsNewArrayList, flows);
    }


    private void checkNonEmptyFlows(Iterable<FlowEntity> expectedFlows) {
        if (!Iterables.isEmpty(expectedFlows)) {
            List<FlowEntity> flows = getFlows();
            assertTrue("No Flows created (bean wiring may be broken?)", !flows.isEmpty());
        }
    }

    public synchronized void assertFlowsInAnyOrder(Iterable<FlowEntity> expectedFlows) {
        checkNonEmptyFlows(expectedFlows);
        // TODO Support Iterable <-> List directly within XtendBeanGenerator
        List<FlowEntity> expectedFlowsAsNewArrayList = Lists.newArrayList(expectedFlows);

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
            LOG.info("expectedFlows = {}", expectedFlowsAsNewArrayList);
            LOG.info("flows = {}",flows);
            assertThat(flows).containsExactlyElementsIn(expectedFlowsAsNewArrayList);
        } catch (AssertionError e) {
            // The point of this is basically just that our assertEqualBeans output,
            // in case of a comparison failure, is *A LOT* more clearly readable
            // than what G Truth (or Hamcrest) can do based on toString.
            assertEqualBeans(expectedFlowsAsNewArrayList, flows);
        }
    }

    @Override
    public synchronized void installFlow(FlowEntity flowEntity) {
        getFlows().add(flowEntity);
    }

    @Override
    public synchronized CheckedFuture<Void, TransactionCommitFailedException> removeFlow(BigInteger dpnId,
            FlowEntity flowEntity) {
        getFlows().remove(flowEntity);
        return Futures.immediateCheckedFuture(null);
    }

}
