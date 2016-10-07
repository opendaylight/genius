/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.interfaces.testutils.matchers;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.mockito.ArgumentMatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Instructions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;


/**
 * Implements a matcher to be used in conjunction with Mockito's ArgumentCaptor.
 *
 * Example of usage:
 *
 *  <pre>
 * {@code
 * IMdsalApiManager mdsalMgrMock;
 * sut.methodThatInstalls2Flows(mdsalMgrMock)
 * ArgumentCaptor<Flow> argumentCaptor = ArgumentCaptor.forClass(Flow.class);
 * verify(mdsalMgrMock, times(2)).installFlow(argumentCaptor.capture());
 * List<Flow> installedFlowsCaptured = argumentCaptor.getAllValues();
 * assert (installedFlowsCaptured.size() == 2);
 * Flow expectedFlow1 = buildExpectedFlow1();
 * assert (new FlowMatcher(expectedFlow1).matches(installedFlowsCaptured.get(0)));
 * Flow expectedFlow2 = buildExpectedFlow2();
 * assert (new FlowMatcher(expectedFlow2).matches(installedFlowsCaptured.get(1)));
 * }
 * </pre>
 */
public class FlowMatcher extends ArgumentMatcher<Flow> {


    Flow expectedFlow;

    public FlowMatcher(Flow expectedFlow) {
        this.expectedFlow = expectedFlow;
    }

    public boolean sameInstruction(Instruction inst1, Instruction inst2) {
        return inst1.getInstruction()
                .getImplementedInterface()
                .getSimpleName()
                .equals(inst2.getInstruction()
                        .getImplementedInterface()
                        .getSimpleName());
    }

    public boolean containsInstruction(Instruction inst1, List<Instruction> inst2List) {
        for ( Instruction inst2 : inst2List) {
            if ( sameInstruction(inst1, inst2 )) {
                return true;
            }
        }
        return false;
    }

    public boolean sameInstructions(Instructions inst1, Instructions inst2) {
        if ( inst1.getInstruction().size() != inst2.getInstruction().size() ) {
            return false;
        }

        for ( Instruction inst : inst1.getInstruction() ) {
            if ( !containsInstruction(inst, inst2.getInstruction())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean matches(Object flow) {
        if ( ! ( flow instanceof Flow ) ) {
            return false;
        }
        Flow actualFlow = (Flow) flow;

        boolean result =
            (actualFlow.getId() != null)
            && actualFlow.getId().equals(expectedFlow.getId() )
            && actualFlow.getTableId() == expectedFlow.getTableId()
            && StringUtils.equals(actualFlow.getFlowName(), expectedFlow.getFlowName() )
            && sameInstructions(actualFlow.getInstructions(), expectedFlow.getInstructions())
            && actualFlow.getMatch().equals(expectedFlow.getMatch());

        return result;
    }

}
