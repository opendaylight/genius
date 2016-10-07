/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.interfaces.testutils.matchers;

import org.mockito.ArgumentMatcher;
import org.opendaylight.genius.mdsalutil.FlowEntity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;

/**
 * Implements a matcher to be used in conjunction with Mockito's ArgumentCaptor.
 *
 * Example of usage:
 *
 * <pre>
 * {@code
 * IMdsalApiManager mdsalMgrMock;
 * sut.methodThatInstalls2Flows(mdsalMgrMock)
 * ArgumentCaptor<FlowEntity> argumentCaptor = ArgumentCaptor.forClass(FlowEntity.class);
 * verify(mdsalMgrMock, times(2)).installFlow(argumentCaptor.capture());
 * List<FlowEntity> installedFlowsCaptured = argumentCaptor.getAllValues();
 * assert (installedFlowsCaptured.size() == 2);
 * FlowEntity expectedFlow1 = buildExpectedFlow1();
 * assert (new FlowEntityMatcher(expectedFlow1).matches(installedFlowsCaptured.get(0)));
 * FlowEntity expectedFlow1 = buildExpectedFlow2();
 * assert (new FlowEntityMatcher(expectedFlow1).matches(installedFlowsCaptured.get(1)));
 * }
 * </pre>
 */
public class FlowEntityMatcher  extends ArgumentMatcher<FlowEntity> {
    FlowEntity expectedFlow;

    public FlowEntityMatcher(FlowEntity expectedFlow) {
        this.expectedFlow = expectedFlow;
    }

    @Override
    public boolean matches(Object flow) {
        if (!(flow instanceof FlowEntity)) {
            return false;
        }

        FlowEntity actualFlow = (FlowEntity) flow;
        FlowBuilder actualFlowBuilder = actualFlow.getFlowBuilder();
        FlowBuilder expectedFlowBuilder = expectedFlow.getFlowBuilder();

        boolean result =
            actualFlow.getDpnId() == expectedFlow.getDpnId()
            && actualFlow.getPriority() == expectedFlow.getPriority()
            && actualFlowBuilder.getMatch().equals(expectedFlowBuilder.getMatch())
            && actualFlowBuilder.getInstructions().equals(expectedFlowBuilder.getInstructions());

        return result;
    }
}
