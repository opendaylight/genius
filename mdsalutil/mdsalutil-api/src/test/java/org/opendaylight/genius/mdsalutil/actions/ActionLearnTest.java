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
    @Test
    public void backwardsCompatibleActions() {
        int idleTimeout = 2;
        int hardTimeout = 3;
        int priority = 4;
        int cookie = 5;
        int flags = 6;
        short tableId = (short) 7;
        int finIdleTimeout = 8;
        int finHardTimeout = 9;
        Action action = ActionType.learn.buildAction(1,
            new ActionInfo(
                ActionType.learn,
                new String[] {
                    Integer.toString(idleTimeout),
                    Integer.toString(hardTimeout),
                    Integer.toString(priority),
                    Integer.toString(cookie),
                    Integer.toString(flags),
                    Short.toString(tableId),
                    Integer.toString(finIdleTimeout),
                    Integer.toString(finHardTimeout)
                },
                new String[][] {
                    {NwConstants.LearnFlowModsType.MATCH_FROM_FIELD.name(), "1", "2", "3"},
                    {NwConstants.LearnFlowModsType.MATCH_FROM_VALUE.name(), "1", "2", "3"},
                    {NwConstants.LearnFlowModsType.COPY_FROM_FIELD.name(), "1", "2", "3"},
                    {NwConstants.LearnFlowModsType.COPY_FROM_VALUE.name(), "1", "2", "3"},
                    {NwConstants.LearnFlowModsType.OUTPUT_TO_PORT.name(), "1", "2"}
                }));
        assertTrue(action.getAction() instanceof NxActionLearnNodesNodeTableFlowApplyActionsCase);
        NxActionLearnNodesNodeTableFlowApplyActionsCase nxAction =
            (NxActionLearnNodesNodeTableFlowApplyActionsCase) action.getAction();
        NxLearn nxLearn = nxAction.getNxLearn();
        assertEquals(idleTimeout, nxLearn.getIdleTimeout().intValue());
        assertEquals(hardTimeout, nxLearn.getHardTimeout().intValue());
        assertEquals(priority, nxLearn.getPriority().intValue());
        assertEquals(cookie, nxLearn.getCookie().intValue());
        assertEquals(flags, nxLearn.getFlags().intValue());
        assertEquals(tableId, nxLearn.getTableId().shortValue());
        assertEquals(finIdleTimeout, nxLearn.getFinIdleTimeout().intValue());
        assertEquals(finHardTimeout, nxLearn.getFinHardTimeout().intValue());

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
                assertEquals(1, flowModAddMatchFromValue.getValue().intValue());
                assertEquals(0, flowModAddMatchFromValue.getSrcOfs().intValue());
                assertEquals(2, flowModAddMatchFromValue.getSrcField().longValue());
                assertEquals(3, flowModAddMatchFromValue.getFlowModNumBits().intValue());
                nbChecked++;
            } else if (flowMods.getFlowModSpec() instanceof FlowModCopyFieldIntoFieldCase) {
                FlowModCopyFieldIntoFieldCase flowModCopyFieldIntoFieldCase =
                    (FlowModCopyFieldIntoFieldCase) flowMods.getFlowModSpec();
                FlowModCopyFieldIntoField flowModCopyFieldIntoField =
                    flowModCopyFieldIntoFieldCase.getFlowModCopyFieldIntoField();
                assertEquals(0, flowModCopyFieldIntoField.getSrcOfs().intValue());
                assertEquals(1, flowModCopyFieldIntoField.getSrcField().longValue());
                assertEquals(0, flowModCopyFieldIntoField.getDstOfs().intValue());
                assertEquals(2, flowModCopyFieldIntoField.getDstField().longValue());
                assertEquals(3, flowModCopyFieldIntoField.getFlowModNumBits().intValue());
                nbChecked++;
            } else if (flowMods.getFlowModSpec() instanceof FlowModCopyValueIntoFieldCase) {
                FlowModCopyValueIntoFieldCase flowModCopyValueIntoFieldCase =
                    (FlowModCopyValueIntoFieldCase) flowMods.getFlowModSpec();
                FlowModCopyValueIntoField flowModCopyValueIntoField =
                    flowModCopyValueIntoFieldCase.getFlowModCopyValueIntoField();
                assertEquals(1, flowModCopyValueIntoField.getValue().intValue());
                assertEquals(0, flowModCopyValueIntoField.getDstOfs().intValue());
                assertEquals(2, flowModCopyValueIntoField.getDstField().longValue());
                assertEquals(3, flowModCopyValueIntoField.getFlowModNumBits().intValue());
                nbChecked++;
            } else if (flowMods.getFlowModSpec() instanceof FlowModOutputToPortCase) {
                FlowModOutputToPortCase flowModOutputToPortCase = (FlowModOutputToPortCase) flowMods.getFlowModSpec();
                FlowModOutputToPort flowModCopyFieldIntoField = flowModOutputToPortCase.getFlowModOutputToPort();
                assertEquals(0, flowModCopyFieldIntoField.getSrcOfs().intValue());
                assertEquals(1, flowModCopyFieldIntoField.getSrcField().longValue());
                assertEquals(2, flowModCopyFieldIntoField.getFlowModNumBits().intValue());
                nbChecked++;
            }
        }
        assertEquals(5, nbChecked);
    }
}
