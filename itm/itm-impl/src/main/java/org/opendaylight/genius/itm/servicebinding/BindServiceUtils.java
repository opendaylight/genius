/*
 * Copyright (c) 2020 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.servicebinding;

import static org.opendaylight.mdsal.binding.util.Datastore.CONFIGURATION;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.MetaDataUtil;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.mdsalutil.actions.ActionGroup;
import org.opendaylight.genius.mdsalutil.actions.ActionNxResubmit;
import org.opendaylight.genius.mdsalutil.actions.ActionOutput;
import org.opendaylight.genius.mdsalutil.actions.ActionRegLoad;
import org.opendaylight.genius.mdsalutil.actions.ActionSetFieldTunnelId;
import org.opendaylight.genius.utils.ServiceIndex;
import org.opendaylight.mdsal.binding.util.Datastore;
import org.opendaylight.mdsal.binding.util.ManagedNewTransactionRunner;
import org.opendaylight.mdsal.binding.util.TypedWriteTransaction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceBindings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeEgress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceTypeFlowBased;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.StypeOpenflowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfoKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServicesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServicesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg6;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BindServiceUtils {

    private static final Logger LOG = LoggerFactory.getLogger(BindServiceUtils.class);

    private BindServiceUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static void bindDefaultEgressDispatcherService(ManagedNewTransactionRunner txRunner,
                                                          List<ListenableFuture<Void>> futures, String tunType,
                                                          String portNo, String interfaceName, Uint16 ifIndex) {
        Map<InstructionKey, Instruction> instructions =
                getEgressInstructionsForInterface(tunType, portNo, null, true, ifIndex, 0);
        bindDefaultEgressDispatcherService(txRunner, futures, interfaceName, instructions);
    }

    public static void bindDefaultEgressDispatcherService(ManagedNewTransactionRunner txRunner,
                                                          List<ListenableFuture<Void>> futures,
                                                          String interfaceName,
                                                          Map<InstructionKey, Instruction> instructions) {
        futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION, tx -> {
            int priority = ServiceIndex.getIndex(NwConstants.DEFAULT_EGRESS_SERVICE_NAME,
                    NwConstants.DEFAULT_EGRESS_SERVICE_INDEX);
            BoundServices serviceInfo =
                    getBoundServices(String.format("%s.%s", "default", interfaceName),
                            ServiceIndex.getIndex(NwConstants.DEFAULT_EGRESS_SERVICE_NAME,
                                    NwConstants.DEFAULT_EGRESS_SERVICE_INDEX),
                            priority, NwConstants.EGRESS_DISPATCHER_TABLE_COOKIE, instructions);
            bindService(tx, interfaceName, serviceInfo, ServiceModeEgress.class);
        }));
    }

    public static Map<InstructionKey, Instruction> getEgressInstructionsForInterface(String tunType,String portNo,
                                                                                     Long tunnelKey,
                                                                                     boolean isDefaultEgress,
                                                                                     Uint16 ifIndex, long groupId) {
        Map<InstructionKey, Instruction> instructions = new HashMap<>();
        List<Action> actionList = MDSALUtil.buildActions(
                getEgressActionInfosForInterface(tunType, portNo, tunnelKey, 0,
                        isDefaultEgress, ifIndex, groupId));
        Instruction inst = MDSALUtil.buildApplyActionsInstruction(actionList);
        instructions.put(inst.key(), inst);
        return instructions;
    }

    public static List<ActionInfo> getEgressActionInfosForInterface(String tunType, String portNo,
                                                                    Long tunnelKey,
                                                                    int actionKeyStart, boolean isDefaultEgress,
                                                                    Uint16 ifIndex, long groupId) {
        List<ActionInfo> result = new ArrayList<>();
        switch (tunType) {
            case "MPLS_OVER_GRE":
                // fall through
            case "GRE_TRUNK_INTERFACE":
                if (!isDefaultEgress) {
                    // TODO tunnel_id to encode GRE key, once it is supported
                    // Until then, tunnel_id should be "cleaned", otherwise it
                    // stores the value coming from a VXLAN tunnel
                    if (tunnelKey == null) {
                        tunnelKey = 0L;
                    }
                    result.add(new ActionSetFieldTunnelId(actionKeyStart++, Uint64.valueOf(tunnelKey)));
                    addEgressActionInfosForInterface(ifIndex, actionKeyStart, result);
                } else {
                    result.add(new ActionOutput(actionKeyStart++, new Uri(portNo)));
                }
                break;
                // fall through
            case "VXLAN_TRUNK_INTERFACE":
                if (!isDefaultEgress) {
                    if (tunnelKey != null) {
                        result.add(new ActionSetFieldTunnelId(actionKeyStart++, Uint64.valueOf(tunnelKey)));
                    }
                    addEgressActionInfosForInterface(ifIndex, actionKeyStart, result);
                } else {
                    result.add(new ActionOutput(actionKeyStart++, new Uri(portNo)));
                }
                break;
            case "VLAN_INTERFACE":
                LOG.error("VLAN swicth case");
                if (isDefaultEgress) {
                    result.add(new ActionOutput(actionKeyStart++, new Uri(portNo)));
                } else {
                    addEgressActionInfosForInterface(ifIndex, actionKeyStart, result);
                }
                break;
            case "LOGICAL_GROUP_INTERFACE":
                if (isDefaultEgress) {
                    result.add(new ActionGroup(groupId));
                } else {
                    addEgressActionInfosForInterface(ifIndex, actionKeyStart, result);
                }
                break;

            default:
                LOG.warn("Interface Type {} not handled yet", tunType);
                break;
        }
        return result;
    }

    public static void addEgressActionInfosForInterface(Uint16 ifIndex, int actionKeyStart, List<ActionInfo> result) {
        long regValue = MetaDataUtil.getReg6ValueForLPortDispatcher(ifIndex.intValue(),
                NwConstants.DEFAULT_SERVICE_INDEX);
        result.add(new ActionRegLoad(actionKeyStart++, NxmNxReg6.class, ITMConstants.REG6_START_INDEX,
                ITMConstants.REG6_END_INDEX, regValue));
        result.add(new ActionNxResubmit(actionKeyStart, NwConstants.EGRESS_LPORT_DISPATCHER_TABLE));
    }

    public static BoundServices getBoundServices(String serviceName, short servicePriority, int flowPriority,
                                                 Uint64 cookie, Map<InstructionKey, Instruction> instructions) {
        StypeOpenflowBuilder augBuilder = new StypeOpenflowBuilder().setFlowCookie(cookie)
                .setFlowPriority(Uint16.valueOf(flowPriority))
                .setInstruction(instructions);
        return new BoundServicesBuilder()
                .withKey(new BoundServicesKey(Uint8.valueOf(servicePriority)))
                .setServiceName(serviceName)
                .setServicePriority(Uint8.valueOf(servicePriority))
                .setServiceType(ServiceTypeFlowBased.class)
                .addAugmentation(augBuilder.build()).build();
    }

    public static void bindService(TypedWriteTransaction<Datastore.Configuration> tx, String interfaceName,
                                   BoundServices serviceInfo,
                                   Class<? extends ServiceModeBase> serviceMode) {
        LOG.info("Binding Service {} for : {}", serviceInfo.getServiceName(), interfaceName);
        InstanceIdentifier<BoundServices> boundServicesInstanceIdentifier = buildBoundServicesIId(
                serviceInfo.getServicePriority(), interfaceName, serviceMode);
        tx.mergeParentStructurePut(boundServicesInstanceIdentifier, serviceInfo);
    }

    public static InstanceIdentifier<BoundServices> buildBoundServicesIId(Uint8 servicePriority, String interfaceName,
                                                                      Class<? extends ServiceModeBase> serviceMode) {
        return InstanceIdentifier.builder(ServiceBindings.class)
                .child(ServicesInfo.class, new ServicesInfoKey(interfaceName, serviceMode))
                .child(BoundServices.class, new BoundServicesKey(servicePriority)).build();
    }

    public static void unbindService(List<ListenableFuture<Void>> futures,
                                     ManagedNewTransactionRunner txRunner,
                                     String interfaceName) {
        LOG.info("Unbinding service for : {}", interfaceName);
        futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION,
            tx -> tx.delete(buildDefaultServiceId(interfaceName))));
    }

    public static InstanceIdentifier<BoundServices> buildDefaultServiceId(String interfaceName) {
        return buildServiceId(interfaceName, ServiceIndex.getIndex(NwConstants.DEFAULT_EGRESS_SERVICE_NAME,
                NwConstants.DEFAULT_EGRESS_SERVICE_INDEX), ServiceModeEgress.class);
    }

    public static InstanceIdentifier<BoundServices> buildServiceId(String interfaceName, short serviceIndex,
                                                                   Class<? extends ServiceModeBase> serviceMode) {
        return InstanceIdentifier.builder(ServiceBindings.class)
                .child(ServicesInfo.class, new ServicesInfoKey(interfaceName, serviceMode))
                .child(BoundServices.class, new BoundServicesKey(Uint8.valueOf(serviceIndex))).build();
    }
}
