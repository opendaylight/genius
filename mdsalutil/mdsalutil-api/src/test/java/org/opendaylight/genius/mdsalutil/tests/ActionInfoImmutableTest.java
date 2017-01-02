/*
 * Copyright © 2016, 2017 Red Hat and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.tests;

import static org.junit.Assert.assertEquals;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.FlowEntity;
import org.opendaylight.genius.mdsalutil.actions.ActionNxConntrack;
import org.opendaylight.genius.mdsalutil.instructions.InstructionApplyActions;

public class ActionInfoImmutableTest {

    @Test
    public void actionInfoActionKeyDoesNotMagicallyChangeOnFlowEntityGetFlowBuilder() {
        FlowEntity flowEntity = new FlowEntity(BigInteger.valueOf(123L));
        flowEntity.setFlowId("TEST");
        flowEntity.setCookie(BigInteger.valueOf(110100480L));
        ActionInfo actionInfo = new ActionNxConntrack(27, 1, 0, 0, (short) 255);
        List<ActionInfo> actionInfos = new ArrayList<>();
        actionInfos.add(actionInfo);
        flowEntity.getInstructionInfoList().add(new InstructionApplyActions(actionInfos));
        assertEquals(27, ((InstructionApplyActions) flowEntity.getInstructionInfoList().get(0)).getActionInfos().get(
                0).getActionKey());

        flowEntity.getFlowBuilder();
        assertEquals(27, ((InstructionApplyActions) flowEntity.getInstructionInfoList().get(0)).getActionInfos().get(
                0).getActionKey());
        flowEntity.getFlowBuilder();
        assertEquals(27, ((InstructionApplyActions) flowEntity.getInstructionInfoList().get(0)).getActionInfos().get(
                0).getActionKey());
    }

}
