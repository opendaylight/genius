/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.interfaces.testutils;

import static org.opendaylight.mdsal.binding.testutils.AssertDataObjects.assertEqualBeans;

import java.util.ArrayList;
import java.util.Comparator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;

/**
 * This class is a place holder for various openflow flow/group related test utilities.
 *
 * @author Faseela K
 */
public class FlowAssertTestUtils {

    // The problem we're fixing here is that, in MD-SAL binding v1, YANG lists are represented
    // as Java lists but they don't preserve order (unless they specify “ordered-by user”).
    // YANG keyed lists in particular are backed by maps, so you can store such a list in the
    // MD-SAL and get it back in a different order.
    // When comparing beans involving such lists, we need to sort the lists before comparing
    // them.
    // Hence, we rebuild instances of Flow, and sort the affected lists
    // in the augmentations.
    public void assertFlowsInAnyOrder(Flow expected, Flow actual) {
        // Re-create the flows to avoid re-ordering problems
        assertEqualBeans(rebuildFlow(expected), rebuildFlow(actual));
    }

    private Flow rebuildFlow(Flow flow) {
        FlowBuilder flowBuilder = new FlowBuilder(flow);
        InstructionsBuilder instructionsBuilder = new InstructionsBuilder();
        if (flow.getInstructions() != null) {
            ArrayList<Instruction> instructionList =
                new ArrayList<>(flow.getInstructions().nonnullInstruction());
            instructionList.sort(Comparator.comparing(o -> o.key().toString()));
            instructionsBuilder.setInstruction(instructionList);
            flowBuilder.setInstructions(instructionsBuilder.build());
        }
        return flowBuilder.build();
    }
}
