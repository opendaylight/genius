/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.interfaces.testutils;

import static org.junit.Assert.assertTrue;
import static org.opendaylight.yangtools.testutils.mockito.MoreAnswers.realOrException;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.util.concurrent.CheckedFuture;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.mdsalutil.FlowEntity;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.mdsal.binding.testutils.AssertDataObjects;

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

    private List<FlowEntity> flows;

    public static TestIMdsalApiManager newInstance() {
        return Mockito.mock(TestIMdsalApiManager.class, realOrException());
    }

    private synchronized List<FlowEntity> initializeFlows() {
        return Collections.synchronizedList(new ArrayList<>());
    }

    public List<FlowEntity> getFlows() {
        if (flows == null) {
            flows = initializeFlows();
        }
        return flows;
    }

    public void assertFlows(Iterable<FlowEntity> expectedFlows) {
        List<FlowEntity> flows = this.getFlows();
        if (!Iterables.isEmpty(expectedFlows)) {
            assertTrue("No Flows created (bean wiring may be broken?)", !flows.isEmpty());
        }
        // TODO Support Iterable <-> List directly within XtendBeanGenerator
        List<FlowEntity> expectedFlowsAsNewArrayList = Lists.newArrayList(expectedFlows);
        AssertDataObjects.assertEqualBeans(expectedFlowsAsNewArrayList, flows);
    }

    @Override
    public void installFlow(FlowEntity flowEntity) {
        getFlows().add(flowEntity);
    }

    @Override
    public CheckedFuture<Void, TransactionCommitFailedException> removeFlow(BigInteger dpnId, FlowEntity flowEntity) {
        getFlows().remove(flowEntity);
        return null;
    }

}
