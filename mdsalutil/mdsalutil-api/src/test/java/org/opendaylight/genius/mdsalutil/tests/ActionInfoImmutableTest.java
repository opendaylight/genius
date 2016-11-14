/*
 * Copyright (c) 2016 Red Hat and others.  All rights reserved.
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
import org.opendaylight.genius.mdsalutil.ActionType;
import org.opendaylight.genius.mdsalutil.FlowEntity;
import org.opendaylight.genius.mdsalutil.InstructionInfo;
import org.opendaylight.genius.mdsalutil.InstructionType;
import org.opendaylight.genius.mdsalutil.NxmRegisters;

public class ActionInfoImmutableTest {

    @Test
    public void actionInfoActionKeyDoesNotMagicallyChangeOnFlowEntityGetFlowBuilder() {
        FlowEntity flowEntity = new FlowEntity(BigInteger.valueOf(123L));
        flowEntity.setFlowId("TEST");
        flowEntity.setCookie(BigInteger.valueOf(110100480L));
        ActionInfo actionInfo = new ActionInfo(ActionType.nx_conntrack, new String[] { "1", "0", "0", "255" }, 27);
        List<ActionInfo> actionInfos = new ArrayList<>();
        actionInfos.add(actionInfo);
        flowEntity.getInstructionInfoList().add(new InstructionInfo(InstructionType.apply_actions, actionInfos));
        assertEquals(27, flowEntity.getInstructionInfoList().get(0).getActionInfos().get(0).getActionKey());

        flowEntity.getFlowBuilder();
        assertEquals(27, flowEntity.getInstructionInfoList().get(0).getActionInfos().get(0).getActionKey());
        flowEntity.getFlowBuilder();
        assertEquals(27, flowEntity.getInstructionInfoList().get(0).getActionInfos().get(0).getActionKey());
    }

    @Test
    public void actionInfoTestForRegLoadAction() {
        FlowEntity flowEntity = new FlowEntity(BigInteger.valueOf(123L));
        flowEntity.setFlowId("TESTFLOWWITHREGLOAD");
        flowEntity.setCookie(BigInteger.valueOf(110100481L));
        ActionInfo actionInfo = new ActionInfo(ActionType.nx_load_reg, new String[] { "0", "31", "100" }, 1, NxmRegisters.REG6);
        List<ActionInfo> actionInfos = new ArrayList<>();
        actionInfos.add(actionInfo);
        flowEntity.getInstructionInfoList().add(new InstructionInfo(InstructionType.apply_actions, actionInfos));
        assertEquals(NxmRegisters.REG6, flowEntity.getInstructionInfoList().get(0).getActionInfos().get(0).getNxmRegister());
        flowEntity.getFlowBuilder();
        assertEquals(NxmRegisters.REG6, flowEntity.getInstructionInfoList().get(0).getActionInfos().get(0).getNxmRegister());
    }

    @Test
    public void actionInfoTestForRegMoveToMplsAction() {
        FlowEntity flowEntity = new FlowEntity(BigInteger.valueOf(123L));
        flowEntity.setFlowId("TESTFLOWWITHREGMOVE");
        flowEntity.setCookie(BigInteger.valueOf(110100481L));
        ActionInfo actionInfo = new ActionInfo(ActionType.nx_reg_move_mpls_label, new String[] { "0", "31" }, 1, NxmRegisters.REG1);
        List<ActionInfo> actionInfos = new ArrayList<>();
        actionInfos.add(actionInfo);
        flowEntity.getInstructionInfoList().add(new InstructionInfo(InstructionType.apply_actions, actionInfos));
        assertEquals(ActionType.nx_reg_move_mpls_label, flowEntity.getInstructionInfoList().get(0).getActionInfos().get(0).getActionType());
        flowEntity.getFlowBuilder();
        assertEquals(ActionType.nx_reg_move_mpls_label, flowEntity.getInstructionInfoList().get(0).getActionInfos().get(0).getActionType());
        assertEquals(NxmRegisters.REG1, flowEntity.getInstructionInfoList().get(0).getActionInfos().get(0).getNxmRegister());
    }

}
