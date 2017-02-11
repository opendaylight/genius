/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.interfaces.testutils;

import static org.opendaylight.yangtools.testutils.mockito.MoreAnswers.realOrException;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.mockito.Mockito;
import org.opendaylight.genius.mdsalutil.FlowEntity;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;

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

    @Override
    public void installFlow(FlowEntity flowEntity) {
        getFlows().add(flowEntity);
    }
    @Override
    public void batchedAddFlow(BigInteger dpId, FlowEntity flowEntity){
        getOrNewFlows().add(flowEntity);
    }
    @Override
    public void batchedRemoveFlow(BigInteger dpId, FlowEntity flowEntity){
        getOrNewFlows().remove(flowEntity);
    }

}
