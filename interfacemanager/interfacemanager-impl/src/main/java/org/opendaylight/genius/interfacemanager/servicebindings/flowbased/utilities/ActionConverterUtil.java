/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.bound.services.instruction.instruction.apply.actions._case.apply.actions.action.action.ServiceBindingNxActionConntrackApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.bound.services.instruction.instruction.apply.actions._case.apply.actions.action.action.ServiceBindingNxActionDecNshTtlApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.bound.services.instruction.instruction.apply.actions._case.apply.actions.action.action.ServiceBindingNxActionDecapApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.bound.services.instruction.instruction.apply.actions._case.apply.actions.action.action.ServiceBindingNxActionEncapApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.bound.services.instruction.instruction.apply.actions._case.apply.actions.action.action.ServiceBindingNxActionLearnApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.bound.services.instruction.instruction.apply.actions._case.apply.actions.action.action.ServiceBindingNxActionMultipathApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.bound.services.instruction.instruction.apply.actions._case.apply.actions.action.action.ServiceBindingNxActionOutputRegApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.bound.services.instruction.instruction.apply.actions._case.apply.actions.action.action.ServiceBindingNxActionRegLoadApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.bound.services.instruction.instruction.apply.actions._case.apply.actions.action.action.ServiceBindingNxActionRegMoveApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.bound.services.instruction.instruction.apply.actions._case.apply.actions.action.action.ServiceBindingNxActionResubmitApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.bound.services.instruction.instruction.write.actions._case.write.actions.action.action.ServiceBindingNxActionConntrackWriteActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.bound.services.instruction.instruction.write.actions._case.write.actions.action.action.ServiceBindingNxActionDecNshTtlWriteActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.bound.services.instruction.instruction.write.actions._case.write.actions.action.action.ServiceBindingNxActionDecapWriteActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.bound.services.instruction.instruction.write.actions._case.write.actions.action.action.ServiceBindingNxActionEncapWriteActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.bound.services.instruction.instruction.write.actions._case.write.actions.action.action.ServiceBindingNxActionLearnWriteActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.bound.services.instruction.instruction.write.actions._case.write.actions.action.action.ServiceBindingNxActionMultipathWriteActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.bound.services.instruction.instruction.write.actions._case.write.actions.action.action.ServiceBindingNxActionOutputRegWriteActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.bound.services.instruction.instruction.write.actions._case.write.actions.action.action.ServiceBindingNxActionRegLoadWriteActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.bound.services.instruction.instruction.write.actions._case.write.actions.action.action.ServiceBindingNxActionRegMoveWriteActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.bound.services.instruction.instruction.write.actions._case.write.actions.action.action.ServiceBindingNxActionResubmitWriteActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionConntrackNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionDecNshTtlNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionDecapNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionEncapNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionLearnNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionMultipathNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionOutputRegNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegLoadNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegMoveNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionResubmitNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.write.actions._case.write.actions.action.action.NxActionConntrackNodesNodeTableFlowWriteActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.write.actions._case.write.actions.action.action.NxActionDecNshTtlNodesNodeTableFlowWriteActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.write.actions._case.write.actions.action.action.NxActionDecapNodesNodeTableFlowWriteActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.write.actions._case.write.actions.action.action.NxActionEncapNodesNodeTableFlowWriteActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.write.actions._case.write.actions.action.action.NxActionLearnNodesNodeTableFlowWriteActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.write.actions._case.write.actions.action.action.NxActionMultipathNodesNodeTableFlowWriteActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.write.actions._case.write.actions.action.action.NxActionOutputRegNodesNodeTableFlowWriteActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.write.actions._case.write.actions.action.action.NxActionRegLoadNodesNodeTableFlowWriteActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.write.actions._case.write.actions.action.action.NxActionRegMoveNodesNodeTableFlowWriteActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.write.actions._case.write.actions.action.action.NxActionResubmitNodesNodeTableFlowWriteActionsCaseBuilder;

public final class ActionConverterUtil {
    private static final Map<Class<? extends Action>, Function<Action, Action>> SERVICE_TO_OF =
            ImmutableMap.<Class<? extends Action>, Function<Action, Action>>builder()
            .put(ServiceBindingNxActionConntrackApplyActionsCase.class,
                input -> new NxActionConntrackNodesNodeTableFlowApplyActionsCaseBuilder(
                    (ServiceBindingNxActionConntrackApplyActionsCase) input).build())
            .put(ServiceBindingNxActionLearnApplyActionsCase.class,
                input -> new NxActionLearnNodesNodeTableFlowApplyActionsCaseBuilder(
                    (ServiceBindingNxActionLearnApplyActionsCase) input).build())
            .put(ServiceBindingNxActionMultipathApplyActionsCase.class,
                input -> new NxActionMultipathNodesNodeTableFlowApplyActionsCaseBuilder(
                    (ServiceBindingNxActionMultipathApplyActionsCase) input).build())
            .put(ServiceBindingNxActionOutputRegApplyActionsCase.class,
                input -> new NxActionOutputRegNodesNodeTableFlowApplyActionsCaseBuilder(
                    (ServiceBindingNxActionOutputRegApplyActionsCase) input).build())
            .put(ServiceBindingNxActionDecapApplyActionsCase.class,
                input -> new NxActionDecapNodesNodeTableFlowApplyActionsCaseBuilder(
                    (ServiceBindingNxActionDecapApplyActionsCase) input).build())
            .put(ServiceBindingNxActionEncapApplyActionsCase.class,
                input -> new NxActionEncapNodesNodeTableFlowApplyActionsCaseBuilder(
                    (ServiceBindingNxActionEncapApplyActionsCase) input).build())
            .put(ServiceBindingNxActionRegLoadApplyActionsCase.class,
                input -> new NxActionRegLoadNodesNodeTableFlowApplyActionsCaseBuilder(
                    (ServiceBindingNxActionRegLoadApplyActionsCase) input).build())
            .put(ServiceBindingNxActionRegMoveApplyActionsCase.class,
                input -> new NxActionRegMoveNodesNodeTableFlowApplyActionsCaseBuilder(
                    (ServiceBindingNxActionRegMoveApplyActionsCase) input).build())
            .put(ServiceBindingNxActionResubmitApplyActionsCase.class,
                input -> new NxActionResubmitNodesNodeTableFlowApplyActionsCaseBuilder(
                    (ServiceBindingNxActionResubmitApplyActionsCase) input).build())
            .put(ServiceBindingNxActionDecNshTtlApplyActionsCase.class,
                input -> new NxActionDecNshTtlNodesNodeTableFlowApplyActionsCaseBuilder(
                    (ServiceBindingNxActionDecNshTtlApplyActionsCase) input).build())
            .put(ServiceBindingNxActionConntrackWriteActionsCase.class,
                input -> new NxActionConntrackNodesNodeTableFlowWriteActionsCaseBuilder(
                    (ServiceBindingNxActionConntrackWriteActionsCase) input).build())
            .put(ServiceBindingNxActionLearnWriteActionsCase.class,
                input -> new NxActionLearnNodesNodeTableFlowWriteActionsCaseBuilder(
                    (ServiceBindingNxActionLearnWriteActionsCase) input).build())
            .put(ServiceBindingNxActionMultipathWriteActionsCase.class,
                input -> new NxActionMultipathNodesNodeTableFlowWriteActionsCaseBuilder(
                    (ServiceBindingNxActionMultipathWriteActionsCase) input).build())
            .put(ServiceBindingNxActionOutputRegWriteActionsCase.class,
                input -> new NxActionOutputRegNodesNodeTableFlowWriteActionsCaseBuilder(
                    (ServiceBindingNxActionOutputRegWriteActionsCase) input).build())
            .put(ServiceBindingNxActionDecapWriteActionsCase.class,
                input -> new NxActionDecapNodesNodeTableFlowWriteActionsCaseBuilder(
                    (ServiceBindingNxActionDecapWriteActionsCase) input).build())
            .put(ServiceBindingNxActionEncapWriteActionsCase.class,
                input -> new NxActionEncapNodesNodeTableFlowWriteActionsCaseBuilder(
                    (ServiceBindingNxActionEncapWriteActionsCase) input).build())
            .put(ServiceBindingNxActionRegLoadWriteActionsCase.class,
                input -> new NxActionRegLoadNodesNodeTableFlowWriteActionsCaseBuilder(
                    (ServiceBindingNxActionRegLoadWriteActionsCase) input).build())
            .put(ServiceBindingNxActionRegMoveWriteActionsCase.class,
                input -> new NxActionRegMoveNodesNodeTableFlowWriteActionsCaseBuilder(
                    (ServiceBindingNxActionRegMoveWriteActionsCase) input).build())
            .put(ServiceBindingNxActionResubmitWriteActionsCase.class,
                input -> new NxActionResubmitNodesNodeTableFlowWriteActionsCaseBuilder(
                    (ServiceBindingNxActionResubmitWriteActionsCase) input).build())
            .put(ServiceBindingNxActionDecNshTtlWriteActionsCase.class,
                input -> new NxActionDecNshTtlNodesNodeTableFlowWriteActionsCaseBuilder(
                    (ServiceBindingNxActionDecNshTtlWriteActionsCase) input).build())
            .build();

    private ActionConverterUtil() {

    }

    public static List<org.opendaylight.yang.gen.v1.urn.opendaylight.action
        .types.rev131112.action.list.Action> convertServiceActionToFlowAction(
            List<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112
                .action.list.Action> inActionList) {
        return convertServiceActionToFlowAction(inActionList, 0);
    }

    /**
     * Convert service binding actions to flow actions, applying an offset to
     * its order.
     *
     * @param inActionList the service binding actions.
     * @param keyOffset the offset.
     * @return the flow actions.
     */
    public static List<org.opendaylight.yang.gen.v1.urn.opendaylight.action
            .types.rev131112.action.list.Action> convertServiceActionToFlowAction(
            List<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112
                    .action.list.Action> inActionList,
            int keyOffset) {
        List<org.opendaylight.yang.gen.v1.urn.opendaylight
                .action.types.rev131112.action.list.Action> outActionList = new ArrayList<>();
        if (inActionList != null) {
            for (org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112
                     .action.list.Action inAction : inActionList) {
                outActionList.add(
                        new org.opendaylight.yang.gen.v1.urn.opendaylight
                                .action.types.rev131112.action.list.ActionBuilder()
                                .setAction(convertServiceActionToFlowAction(inAction.getAction()))
                                .withKey(new ActionKey(inAction.key().getOrder() + keyOffset))
                                .build());
            }
        }
        return outActionList;
    }

    public static Action convertServiceActionToFlowAction(Action inAction) {
        final Function<Action, Action> func = SERVICE_TO_OF.get(inAction.implementedInterface());
        return func == null ? inAction : func.apply(inAction);
    }
}
