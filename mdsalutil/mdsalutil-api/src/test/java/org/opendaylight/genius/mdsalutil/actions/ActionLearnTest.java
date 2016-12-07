/*
 * Copyright © 2016 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.Arrays;
import org.junit.Test;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.ActionType;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.flow.mod.spec.flow.mod.spec.FlowModAddMatchFromFieldCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.flow.mod.spec.flow.mod.spec.FlowModAddMatchFromValueCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.flow.mod.spec.flow.mod.spec.FlowModCopyFieldIntoFieldCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.flow.mod.spec.flow.mod.spec.FlowModCopyValueIntoFieldCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.flow.mod.spec.flow.mod.spec.FlowModOutputToPortCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.flow.mod.spec.flow.mod.spec.flow.mod.add.match.from.field._case.FlowModAddMatchFromField;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.flow.mod.spec.flow.mod.spec.flow.mod.add.match.from.value._case.FlowModAddMatchFromValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.flow.mod.spec.flow.mod.spec.flow.mod.copy.field.into.field._case.FlowModCopyFieldIntoField;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.flow.mod.spec.flow.mod.spec.flow.mod.copy.value.into.field._case.FlowModCopyValueIntoField;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.flow.mod.spec.flow.mod.spec.flow.mod.output.to.port._case.FlowModOutputToPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionLearnNodesNodeTableFlowApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.learn.grouping.NxLearn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.learn.grouping.nx.learn.FlowMods;

/**
 * Test class for {@link ActionRegLoad}.
 */
public class ActionLearnTest {
    private static final int IDLE_TIMEOUT = 2;
    private static final int HARD_TIMEOUT = 3;
    private static final int PRIORITY = 4;
    private static final BigInteger COOKIE = BigInteger.valueOf(5);
    private static final int FLAGS = 6;
    private static final short TABLE_ID = (short) 7;
    private static final int FIN_IDLE_TIMEOUT = 8;
    private static final int FIN_HARD_TIMEOUT = 9;

    @Test
    public void backwardsCompatibleActionProcessing() {
        verifyAction(buildOldAction());
    }

    private Action buildOldAction() {
        return ActionType.learn.buildAction(1,
                new ActionInfo(
                    ActionType.learn,
                    new String[] {
                        Integer.toString(IDLE_TIMEOUT),
                        Integer.toString(HARD_TIMEOUT),
                        Integer.toString(PRIORITY),
                        COOKIE.toString(),
                        Integer.toString(FLAGS),
                        Short.toString(TABLE_ID),
                        Integer.toString(FIN_IDLE_TIMEOUT),
                        Integer.toString(FIN_HARD_TIMEOUT)
                    },
                    new String[][] {
                        {NwConstants.LearnFlowModsType.MATCH_FROM_FIELD.name(), "1", "2", "3"},
                        {NwConstants.LearnFlowModsType.MATCH_FROM_VALUE.name(), "4", "5", "6"},
                        {NwConstants.LearnFlowModsType.COPY_FROM_FIELD.name(), "7", "8", "9"},
                        {NwConstants.LearnFlowModsType.COPY_FROM_VALUE.name(), "10", "11", "12"},
                        {NwConstants.LearnFlowModsType.OUTPUT_TO_PORT.name(), "13", "14"}
                    }));
    }

    private void verifyAction(Action action) {
        assertTrue(action.getAction() instanceof NxActionLearnNodesNodeTableFlowApplyActionsCase);
        NxActionLearnNodesNodeTableFlowApplyActionsCase nxAction =
            (NxActionLearnNodesNodeTableFlowApplyActionsCase) action.getAction();
        NxLearn nxLearn = nxAction.getNxLearn();
        assertEquals(IDLE_TIMEOUT, nxLearn.getIdleTimeout().intValue());
        assertEquals(HARD_TIMEOUT, nxLearn.getHardTimeout().intValue());
        assertEquals(PRIORITY, nxLearn.getPriority().intValue());
        assertEquals(COOKIE, nxLearn.getCookie());
        assertEquals(FLAGS, nxLearn.getFlags().intValue());
        assertEquals(TABLE_ID, nxLearn.getTableId().shortValue());
        assertEquals(FIN_IDLE_TIMEOUT, nxLearn.getFinIdleTimeout().intValue());
        assertEquals(FIN_HARD_TIMEOUT, nxLearn.getFinHardTimeout().intValue());

        assertEquals(5, nxLearn.getFlowMods().size());

        int nbChecked = 0;
        for (FlowMods flowMods : nxLearn.getFlowMods()) {
            if (flowMods.getFlowModSpec() instanceof FlowModAddMatchFromFieldCase) {
                FlowModAddMatchFromFieldCase flowModAddMatchFromFieldCase =
                    (FlowModAddMatchFromFieldCase) flowMods.getFlowModSpec();
                FlowModAddMatchFromField flowModAddMatchFromField =
                    flowModAddMatchFromFieldCase.getFlowModAddMatchFromField();
                assertEquals(0, flowModAddMatchFromField.getSrcOfs().intValue());
                assertEquals(1, flowModAddMatchFromField.getSrcField().longValue());
                assertEquals(0, flowModAddMatchFromField.getDstOfs().intValue());
                assertEquals(2, flowModAddMatchFromField.getDstField().longValue());
                assertEquals(3, flowModAddMatchFromField.getFlowModNumBits().intValue());
                nbChecked++;
            } else if (flowMods.getFlowModSpec() instanceof FlowModAddMatchFromValueCase) {
                FlowModAddMatchFromValueCase flowModAddMatchFromValueCase =
                    (FlowModAddMatchFromValueCase) flowMods.getFlowModSpec();
                FlowModAddMatchFromValue flowModAddMatchFromValue =
                    flowModAddMatchFromValueCase.getFlowModAddMatchFromValue();
                assertEquals(4, flowModAddMatchFromValue.getValue().intValue());
                assertEquals(0, flowModAddMatchFromValue.getSrcOfs().intValue());
                assertEquals(5, flowModAddMatchFromValue.getSrcField().longValue());
                assertEquals(6, flowModAddMatchFromValue.getFlowModNumBits().intValue());
                nbChecked++;
            } else if (flowMods.getFlowModSpec() instanceof FlowModCopyFieldIntoFieldCase) {
                FlowModCopyFieldIntoFieldCase flowModCopyFieldIntoFieldCase =
                    (FlowModCopyFieldIntoFieldCase) flowMods.getFlowModSpec();
                FlowModCopyFieldIntoField flowModCopyFieldIntoField =
                    flowModCopyFieldIntoFieldCase.getFlowModCopyFieldIntoField();
                assertEquals(0, flowModCopyFieldIntoField.getSrcOfs().intValue());
                assertEquals(7, flowModCopyFieldIntoField.getSrcField().longValue());
                assertEquals(0, flowModCopyFieldIntoField.getDstOfs().intValue());
                assertEquals(8, flowModCopyFieldIntoField.getDstField().longValue());
                assertEquals(9, flowModCopyFieldIntoField.getFlowModNumBits().intValue());
                nbChecked++;
            } else if (flowMods.getFlowModSpec() instanceof FlowModCopyValueIntoFieldCase) {
                FlowModCopyValueIntoFieldCase flowModCopyValueIntoFieldCase =
                    (FlowModCopyValueIntoFieldCase) flowMods.getFlowModSpec();
                FlowModCopyValueIntoField flowModCopyValueIntoField =
                    flowModCopyValueIntoFieldCase.getFlowModCopyValueIntoField();
                assertEquals(10, flowModCopyValueIntoField.getValue().intValue());
                assertEquals(0, flowModCopyValueIntoField.getDstOfs().intValue());
                assertEquals(11, flowModCopyValueIntoField.getDstField().longValue());
                assertEquals(12, flowModCopyValueIntoField.getFlowModNumBits().intValue());
                nbChecked++;
            } else if (flowMods.getFlowModSpec() instanceof FlowModOutputToPortCase) {
                FlowModOutputToPortCase flowModOutputToPortCase = (FlowModOutputToPortCase) flowMods.getFlowModSpec();
                FlowModOutputToPort flowModCopyFieldIntoField = flowModOutputToPortCase.getFlowModOutputToPort();
                assertEquals(0, flowModCopyFieldIntoField.getSrcOfs().intValue());
                assertEquals(13, flowModCopyFieldIntoField.getSrcField().longValue());
                assertEquals(14, flowModCopyFieldIntoField.getFlowModNumBits().intValue());
                nbChecked++;
            }
        }
        assertEquals(5, nbChecked);
    }

    @Test
    public void actionInfoTestForLearnAction() {
        verifyAction(buildNewAction());
    }

    private Action buildNewAction() {
        return ActionType.learn.buildAction(1, buildActionLearn());
    }

    private ActionLearn buildActionLearn() {
        return new ActionLearn(IDLE_TIMEOUT, HARD_TIMEOUT, PRIORITY, COOKIE, FLAGS, TABLE_ID, FIN_IDLE_TIMEOUT,
            FIN_HARD_TIMEOUT,
            Arrays.asList(
                new ActionLearn.MatchFromField(1, 2, 3),
                new ActionLearn.MatchFromValue(4, 5, 6),
                new ActionLearn.CopyFromField(7, 8, 9),
                new ActionLearn.CopyFromValue(10, 11, 12),
                new ActionLearn.OutputToPort(13, 14)
                ));
    }

    @Test
    public void backwardsCompatibleActions() {
        ActionInfo actionLearn = buildActionLearn();
        verifyAction(new ActionInfo(actionLearn.getActionType(), actionLearn.getActionValues(),
            actionLearn.getActionValuesMatrix()).buildAction());
    }
}
