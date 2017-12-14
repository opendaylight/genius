/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.bound.services.instruction.instruction.apply.actions._case.apply.actions.action.action.ServiceBindingNxActionConntrackApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.bound.services.instruction.instruction.apply.actions._case.apply.actions.action.action.ServiceBindingNxActionLearnApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.bound.services.instruction.instruction.apply.actions._case.apply.actions.action.action.ServiceBindingNxActionMultipathApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.bound.services.instruction.instruction.apply.actions._case.apply.actions.action.action.ServiceBindingNxActionOutputRegApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.bound.services.instruction.instruction.apply.actions._case.apply.actions.action.action.ServiceBindingNxActionPopNshApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.bound.services.instruction.instruction.apply.actions._case.apply.actions.action.action.ServiceBindingNxActionPushNshApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.bound.services.instruction.instruction.apply.actions._case.apply.actions.action.action.ServiceBindingNxActionRegLoadApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.bound.services.instruction.instruction.apply.actions._case.apply.actions.action.action.ServiceBindingNxActionRegMoveApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.bound.services.instruction.instruction.apply.actions._case.apply.actions.action.action.ServiceBindingNxActionResubmitApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.bound.services.instruction.instruction.write.actions._case.write.actions.action.action.ServiceBindingNxActionConntrackWriteActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.bound.services.instruction.instruction.write.actions._case.write.actions.action.action.ServiceBindingNxActionLearnWriteActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.bound.services.instruction.instruction.write.actions._case.write.actions.action.action.ServiceBindingNxActionMultipathWriteActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.bound.services.instruction.instruction.write.actions._case.write.actions.action.action.ServiceBindingNxActionOutputRegWriteActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.bound.services.instruction.instruction.write.actions._case.write.actions.action.action.ServiceBindingNxActionPopNshWriteActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.bound.services.instruction.instruction.write.actions._case.write.actions.action.action.ServiceBindingNxActionPushNshWriteActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.bound.services.instruction.instruction.write.actions._case.write.actions.action.action.ServiceBindingNxActionRegLoadWriteActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.bound.services.instruction.instruction.write.actions._case.write.actions.action.action.ServiceBindingNxActionRegMoveWriteActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.bound.services.instruction.instruction.write.actions._case.write.actions.action.action.ServiceBindingNxActionResubmitWriteActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionConntrackNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionLearnNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionMultipathNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionOutputRegNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionPopNshNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionPushNshNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegLoadNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegMoveNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionResubmitNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.write.actions._case.write.actions.action.action.NxActionConntrackNodesNodeTableFlowWriteActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.write.actions._case.write.actions.action.action.NxActionLearnNodesNodeTableFlowWriteActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.write.actions._case.write.actions.action.action.NxActionMultipathNodesNodeTableFlowWriteActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.write.actions._case.write.actions.action.action.NxActionOutputRegNodesNodeTableFlowWriteActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.write.actions._case.write.actions.action.action.NxActionPopNshNodesNodeTableFlowWriteActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.write.actions._case.write.actions.action.action.NxActionPushNshNodesNodeTableFlowWriteActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.write.actions._case.write.actions.action.action.NxActionRegLoadNodesNodeTableFlowWriteActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.write.actions._case.write.actions.action.action.NxActionRegMoveNodesNodeTableFlowWriteActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.write.actions._case.write.actions.action.action.NxActionResubmitNodesNodeTableFlowWriteActionsCaseBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ActionConverterUtil {

    private static final Logger LOG = LoggerFactory.getLogger(ActionConverterUtil.class);
    private static final Map<Class<? extends Action>, Class<?>> SERVICE_ACTION_TO_OF_ACTION_MAP = new HashMap<>();
    private static final String BUILD_METHOD = "build";
    private static final String AUGMENTATION_METHOD = "getAugmentation";
    private static final String GET_IMPL_METHOD = "getImplementedInterface";

    static {
        SERVICE_ACTION_TO_OF_ACTION_MAP.put(ServiceBindingNxActionConntrackApplyActionsCase.class,
                NxActionConntrackNodesNodeTableFlowApplyActionsCaseBuilder.class);
        SERVICE_ACTION_TO_OF_ACTION_MAP.put(ServiceBindingNxActionLearnApplyActionsCase.class,
                NxActionLearnNodesNodeTableFlowApplyActionsCaseBuilder.class);
        SERVICE_ACTION_TO_OF_ACTION_MAP.put(ServiceBindingNxActionMultipathApplyActionsCase.class,
                NxActionMultipathNodesNodeTableFlowApplyActionsCaseBuilder.class);
        SERVICE_ACTION_TO_OF_ACTION_MAP.put(ServiceBindingNxActionOutputRegApplyActionsCase.class,
                NxActionOutputRegNodesNodeTableFlowApplyActionsCaseBuilder.class);
        SERVICE_ACTION_TO_OF_ACTION_MAP.put(ServiceBindingNxActionPopNshApplyActionsCase.class,
                NxActionPopNshNodesNodeTableFlowApplyActionsCaseBuilder.class);
        SERVICE_ACTION_TO_OF_ACTION_MAP.put(ServiceBindingNxActionPushNshApplyActionsCase.class,
                NxActionPushNshNodesNodeTableFlowApplyActionsCaseBuilder.class);
        SERVICE_ACTION_TO_OF_ACTION_MAP.put(ServiceBindingNxActionRegLoadApplyActionsCase.class,
                NxActionRegLoadNodesNodeTableFlowApplyActionsCaseBuilder.class);
        SERVICE_ACTION_TO_OF_ACTION_MAP.put(ServiceBindingNxActionRegMoveApplyActionsCase.class,
                NxActionRegMoveNodesNodeTableFlowApplyActionsCaseBuilder.class);
        SERVICE_ACTION_TO_OF_ACTION_MAP.put(ServiceBindingNxActionResubmitApplyActionsCase.class,
                NxActionResubmitNodesNodeTableFlowApplyActionsCaseBuilder.class);
        SERVICE_ACTION_TO_OF_ACTION_MAP.put(ServiceBindingNxActionConntrackWriteActionsCase.class,
                NxActionConntrackNodesNodeTableFlowWriteActionsCaseBuilder.class);
        SERVICE_ACTION_TO_OF_ACTION_MAP.put(ServiceBindingNxActionLearnWriteActionsCase.class,
                NxActionLearnNodesNodeTableFlowWriteActionsCaseBuilder.class);
        SERVICE_ACTION_TO_OF_ACTION_MAP.put(ServiceBindingNxActionMultipathWriteActionsCase.class,
                NxActionMultipathNodesNodeTableFlowWriteActionsCaseBuilder.class);
        SERVICE_ACTION_TO_OF_ACTION_MAP.put(ServiceBindingNxActionOutputRegWriteActionsCase.class,
                NxActionOutputRegNodesNodeTableFlowWriteActionsCaseBuilder.class);
        SERVICE_ACTION_TO_OF_ACTION_MAP.put(ServiceBindingNxActionPopNshWriteActionsCase.class,
                NxActionPopNshNodesNodeTableFlowWriteActionsCaseBuilder.class);
        SERVICE_ACTION_TO_OF_ACTION_MAP.put(ServiceBindingNxActionPushNshWriteActionsCase.class,
                NxActionPushNshNodesNodeTableFlowWriteActionsCaseBuilder.class);
        SERVICE_ACTION_TO_OF_ACTION_MAP.put(ServiceBindingNxActionRegLoadWriteActionsCase.class,
                NxActionRegLoadNodesNodeTableFlowWriteActionsCaseBuilder.class);
        SERVICE_ACTION_TO_OF_ACTION_MAP.put(ServiceBindingNxActionRegMoveWriteActionsCase.class,
                NxActionRegMoveNodesNodeTableFlowWriteActionsCaseBuilder.class);
        SERVICE_ACTION_TO_OF_ACTION_MAP.put(ServiceBindingNxActionResubmitWriteActionsCase.class,
                NxActionResubmitNodesNodeTableFlowWriteActionsCaseBuilder.class);
    }

    private ActionConverterUtil() { }

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
                                .setKey(new ActionKey(inAction.getKey().getOrder() + keyOffset))
                                .build());
            }
        }
        return outActionList;
    }

    public static Action convertServiceActionToFlowAction(Action inAction) {
        Class ofActionClass = SERVICE_ACTION_TO_OF_ACTION_MAP.get(inAction.getImplementedInterface());
        if (ofActionClass != null) {
            try {
                Method build = ofActionClass.getDeclaredMethod(BUILD_METHOD);
                Object ofActionObj = ofActionClass.newInstance();
                ofActionObj = build.invoke(copy(inAction, ofActionObj));
                LOG.debug("Converted {} action to {} action", inAction.getImplementedInterface(),
                        ((Action) ofActionObj).getImplementedInterface());
                inAction = (Action) ofActionObj;
            } catch (InstantiationException e) {
                LOG.error("Failed to instantiate OF action class {}", ofActionClass);
            } catch (IllegalAccessException e) {
                LOG.error("Cannot access some fields in {}", ofActionClass);
            } catch (NoSuchMethodException e) {
                LOG.error("Method build does not exist in {}", ofActionClass);
            } catch (InvocationTargetException e) {
                LOG.error("Method build invocation failed in {}", ofActionClass);
            }
        }
        return inAction;
    }

    private static Object copy(Object src, Object dest) {
        Method[] gettersAndSetters = src.getClass().getDeclaredMethods();
        for (Method gettersAndSetter : gettersAndSetters) {
            String methodName = gettersAndSetter.getName();
            gettersAndSetter.setAccessible(true);
            try {
                if (methodName.equals(AUGMENTATION_METHOD) || methodName.equals(GET_IMPL_METHOD)) {
                    continue;
                }
                if (methodName.startsWith("get")) {
                    dest = dest.getClass()
                            .getMethod(methodName.replaceFirst("get", "set"), gettersAndSetter.getReturnType())
                            .invoke(dest, gettersAndSetter.invoke(src, null));
                } else if (methodName.startsWith("is")) {
                    dest = dest.getClass()
                            .getMethod(methodName.replaceFirst("is", "set"), gettersAndSetter.getReturnType())
                            .invoke(dest, gettersAndSetter.invoke(src, null));
                }
            } catch (NoSuchMethodException e) {
                LOG.error("Method {} does not exist in {}", methodName, src.getClass(), e);
            } catch (IllegalArgumentException e) {
                LOG.error("Method {} invocation failed in {} due to illegal argument.", methodName, dest.getClass(), e);
            } catch (InvocationTargetException e) {
                LOG.error("Method {} invocation failed in {}", methodName, dest.getClass(), e);
            } catch (IllegalAccessException e) {
                LOG.error("Cannot access method {} in {}", methodName, dest.getClass(), e);
            }
        }
        return dest;
    }
}
