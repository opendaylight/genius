/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
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

public class ActionConverterUtil {
  private static final Logger LOG = LoggerFactory.getLogger(ActionConverterUtil.class);
  private static final Map<Class<? extends Action>, Class<?>> serviceActionToOfActionMap = new HashMap();
  private static final String buildMethod = "build";
  private static final String augmentationMethod = "getAugmentation";
  private static final String getImplMethod = "getImplementedInterface";

  static {
    serviceActionToOfActionMap.put(ServiceBindingNxActionConntrackApplyActionsCase.class, NxActionConntrackNodesNodeTableFlowApplyActionsCaseBuilder.class);
    serviceActionToOfActionMap.put(ServiceBindingNxActionLearnApplyActionsCase.class, NxActionLearnNodesNodeTableFlowApplyActionsCaseBuilder.class);
    serviceActionToOfActionMap.put(ServiceBindingNxActionMultipathApplyActionsCase.class, NxActionMultipathNodesNodeTableFlowApplyActionsCaseBuilder.class);
    serviceActionToOfActionMap.put(ServiceBindingNxActionOutputRegApplyActionsCase.class, NxActionOutputRegNodesNodeTableFlowApplyActionsCaseBuilder.class);
    serviceActionToOfActionMap.put(ServiceBindingNxActionPopNshApplyActionsCase.class, NxActionPopNshNodesNodeTableFlowApplyActionsCaseBuilder.class);
    serviceActionToOfActionMap.put(ServiceBindingNxActionPushNshApplyActionsCase.class, NxActionPushNshNodesNodeTableFlowApplyActionsCaseBuilder.class);
    serviceActionToOfActionMap.put(ServiceBindingNxActionRegLoadApplyActionsCase.class, NxActionRegLoadNodesNodeTableFlowApplyActionsCaseBuilder.class);
    serviceActionToOfActionMap.put(ServiceBindingNxActionRegMoveApplyActionsCase.class, NxActionRegMoveNodesNodeTableFlowApplyActionsCaseBuilder.class);
    serviceActionToOfActionMap.put(ServiceBindingNxActionResubmitApplyActionsCase.class, NxActionResubmitNodesNodeTableFlowApplyActionsCaseBuilder.class);
    serviceActionToOfActionMap.put(ServiceBindingNxActionConntrackWriteActionsCase.class, NxActionConntrackNodesNodeTableFlowWriteActionsCaseBuilder.class);
    serviceActionToOfActionMap.put(ServiceBindingNxActionLearnWriteActionsCase.class, NxActionLearnNodesNodeTableFlowWriteActionsCaseBuilder.class);
    serviceActionToOfActionMap.put(ServiceBindingNxActionMultipathWriteActionsCase.class, NxActionMultipathNodesNodeTableFlowWriteActionsCaseBuilder.class);
    serviceActionToOfActionMap.put(ServiceBindingNxActionOutputRegWriteActionsCase.class, NxActionOutputRegNodesNodeTableFlowWriteActionsCaseBuilder.class);
    serviceActionToOfActionMap.put(ServiceBindingNxActionPopNshWriteActionsCase.class, NxActionPopNshNodesNodeTableFlowWriteActionsCaseBuilder.class);
    serviceActionToOfActionMap.put(ServiceBindingNxActionPushNshWriteActionsCase.class, NxActionPushNshNodesNodeTableFlowWriteActionsCaseBuilder.class);
    serviceActionToOfActionMap.put(ServiceBindingNxActionRegLoadWriteActionsCase.class, NxActionRegLoadNodesNodeTableFlowWriteActionsCaseBuilder.class);
    serviceActionToOfActionMap.put(ServiceBindingNxActionRegMoveWriteActionsCase.class, NxActionRegMoveNodesNodeTableFlowWriteActionsCaseBuilder.class);
    serviceActionToOfActionMap.put(ServiceBindingNxActionResubmitWriteActionsCase.class, NxActionResubmitNodesNodeTableFlowWriteActionsCaseBuilder.class);
  }

  public static List<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action> convertServiceActionToFlowAction
      (List<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action> inActionList) {
    List<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action> outActionList = new ArrayList<>();
    if (inActionList != null) {
      for (org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action inAction : inActionList) {
        outActionList.add(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder()
                .setAction(convertServiceActionToFlowAction(inAction.getAction()))
                .setKey(inAction.getKey())
                .build());
      }
    }
    return outActionList;
  }

  public static Action convertServiceActionToFlowAction(Action inAction) {
    Class ofActionClass = serviceActionToOfActionMap.get(inAction.getImplementedInterface());
    if (ofActionClass != null) {
      try {
        Method build = ofActionClass.getDeclaredMethod(buildMethod);
        Object ofActionObj = ofActionClass.newInstance();
        ofActionObj = build.invoke(copy(inAction, ofActionObj));
        LOG.info("Converted {} action to {} action", inAction.getImplementedInterface(),
                 ((Action)ofActionObj).getImplementedInterface());
        inAction = (Action)ofActionObj;
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
    for (int i = 0; i < gettersAndSetters.length; i++) {
      String methodName = gettersAndSetters[i].getName();
      gettersAndSetters[i].setAccessible(true);
      try{
        if(methodName.equals(augmentationMethod) || methodName.equals(getImplMethod)) {
          continue;
        }
        if(methodName.startsWith("get")){
          dest = dest.getClass().getMethod(methodName.replaceFirst("get", "set"), gettersAndSetters[i].getReturnType())
                                .invoke(dest, gettersAndSetters[i].invoke(src, null));
        }else if(methodName.startsWith("is") ){
          dest = dest.getClass().getMethod(methodName.replaceFirst("is", "set"), gettersAndSetters[i].getReturnType())
                                .invoke(dest, gettersAndSetters[i].invoke(src, null));
        }
      }catch (NoSuchMethodException e) {
        LOG.error("Method {} does not exist in {}", methodName, src.getClass(), e);
      }catch (IllegalArgumentException e) {
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
