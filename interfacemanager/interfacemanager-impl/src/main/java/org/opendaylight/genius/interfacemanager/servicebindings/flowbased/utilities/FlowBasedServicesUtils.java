/*
 * Copyright © 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.InstructionInfo;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.MatchInfo;
import org.opendaylight.genius.mdsalutil.MatchInfoBase;
import org.opendaylight.genius.mdsalutil.MetaDataUtil;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.mdsalutil.actions.ActionDrop;
import org.opendaylight.genius.mdsalutil.instructions.InstructionApplyActions;
import org.opendaylight.genius.mdsalutil.matches.MatchInPort;
import org.opendaylight.genius.mdsalutil.matches.MatchMetadata;
import org.opendaylight.genius.mdsalutil.matches.MatchVlanVid;
import org.opendaylight.genius.mdsalutil.nxmatches.NxMatchRegister;
import org.opendaylight.genius.utils.ServiceIndex;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.WriteActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.WriteMetadataCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfExternal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.SplitHorizon;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.BoundServicesStateList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceBindings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeEgress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeIngress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceTypeFlowBased;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.StypeOpenflow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.StypeOpenflowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.bound.services.state.list.BoundServicesState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.bound.services.state.list.BoundServicesStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.bound.services.state.list.BoundServicesStateKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfoKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServicesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServicesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg6;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FlowBasedServicesUtils {
    private static final Logger LOG = LoggerFactory.getLogger(FlowBasedServicesUtils.class);
    private static final int DEFAULT_DISPATCHER_PRIORITY = 10;

    private FlowBasedServicesUtils() {
    }

    public enum ServiceMode {
        INGRESS, EGRESS
    }

    public static final ImmutableBiMap<ServiceMode, Class<? extends ServiceModeBase>>
        SERVICE_MODE_MAP = new ImmutableBiMap.Builder<ServiceMode, Class<? extends ServiceModeBase>>()
            .put(ServiceMode.EGRESS, ServiceModeEgress.class).put(ServiceMode.INGRESS, ServiceModeIngress.class)
            .build();

    // To keep the mapping between Tunnel Types and Tunnel Interfaces
    public static final Collection<String> INTERFACE_TYPE_BASED_SERVICE_BINDING_KEYWORDS =
            ImmutableSet.of(org.opendaylight.genius.interfacemanager.globals.IfmConstants.ALL_VXLAN_INTERNAL,
                    org.opendaylight.genius.interfacemanager.globals.IfmConstants.ALL_VXLAN_EXTERNAL,
                    org.opendaylight.genius.interfacemanager.globals.IfmConstants.ALL_MPLS_OVER_GRE);

    public static ServicesInfo getServicesInfoForInterface(ReadTransaction tx, String interfaceName,
            Class<? extends ServiceModeBase> serviceMode) throws ReadFailedException {
        ServicesInfoKey servicesInfoKey = new ServicesInfoKey(interfaceName, serviceMode);
        InstanceIdentifier.InstanceIdentifierBuilder<ServicesInfo> servicesInfoIdentifierBuilder = InstanceIdentifier
                .builder(ServiceBindings.class).child(ServicesInfo.class, servicesInfoKey);
        return tx.read(LogicalDatastoreType.CONFIGURATION, servicesInfoIdentifierBuilder.build()).checkedGet().orNull();
    }

    public static NodeConnectorId getNodeConnectorIdFromInterface(String interfaceName,
            InterfaceManagerCommonUtils interfaceManagerCommonUtils) {
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.interfaces.rev140508.interfaces.state.Interface ifState = interfaceManagerCommonUtils
                .getInterfaceState(interfaceName);
        if (ifState != null) {
            List<String> ofportIds = ifState.getLowerLayerIf();
            return new NodeConnectorId(ofportIds.get(0));
        }
        return null;
    }

    public static NodeConnectorId getNodeConnectorIdFromInterface(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.interfaces.rev140508.interfaces.state.Interface ifState) {
        if (ifState != null) {
            List<String> ofportIds = ifState.getLowerLayerIf();
            return new NodeConnectorId(ofportIds.get(0));
        }
        return null;
    }

    @Nullable
    public static BigInteger getDpnIdFromInterface(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.interfaces.rev140508.interfaces.state.Interface ifState) {
        if (ifState != null) {
            List<String> ofportIds = ifState.getLowerLayerIf();
            NodeConnectorId nodeConnectorId = new NodeConnectorId(ofportIds.get(0));
            return IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
        }
        return null;
    }

    public static List<MatchInfo> getMatchInfoForVlanPortAtIngressTable(BigInteger dpId, long portNo, Interface iface) {
        List<MatchInfo> matches = new ArrayList<>();
        matches.add(new MatchInPort(dpId, portNo));
        int vlanId = 0;
        IfL2vlan l2vlan = iface.augmentation(IfL2vlan.class);
        if (l2vlan != null) {
            vlanId = l2vlan.getVlanId() == null ? 0 : l2vlan.getVlanId().getValue();
        }
        if (vlanId >= 0  && l2vlan.getL2vlanMode() != IfL2vlan.L2vlanMode.Transparent) {
            matches.add(new MatchVlanVid(vlanId));
        }
        return matches;
    }

    @Nonnull
    public static List<MatchInfo> getMatchInfoForTunnelPortAtIngressTable(BigInteger dpId, long portNo) {
        List<MatchInfo> matches = new ArrayList<>();
        matches.add(new MatchInPort(dpId, portNo));
        return matches;
    }

    public static List<MatchInfo> getMatchInfoForDispatcherTable(int interfaceTag, short servicePriority) {
        List<MatchInfo> matches = new ArrayList<>();
        matches.add(new MatchMetadata(MetaDataUtil.getMetaDataForLPortDispatcher(interfaceTag, servicePriority),
                MetaDataUtil.getMetaDataMaskForLPortDispatcher()));
        return matches;
    }

    public static List<MatchInfoBase> getMatchInfoForEgressDispatcherTable(int interfaceTag, short serviceIndex) {
        List<MatchInfoBase> matches = new ArrayList<>();
        matches.add(new NxMatchRegister(NxmNxReg6.class,
                MetaDataUtil.getReg6ValueForLPortDispatcher(interfaceTag, serviceIndex)));
        return matches;
    }

    public static void installInterfaceIngressFlow(BigInteger dpId, Interface iface, BoundServices boundServiceNew,
            WriteTransaction writeTransaction, List<MatchInfo> matches, int lportTag, short tableId) {
        List<Instruction> instructions = boundServiceNew.augmentation(StypeOpenflow.class).getInstruction();

        int serviceInstructionsSize = instructions != null ? instructions.size() : 0;
        List<Instruction> instructionSet = new ArrayList<>();
        int vlanId = 0;
        IfL2vlan l2vlan = iface.augmentation(IfL2vlan.class);
        if (l2vlan != null && l2vlan.getVlanId() != null) {
            vlanId = l2vlan.getVlanId().getValue();
        }
        if (vlanId != 0) {
            // incrementing instructionSize and using it as actionKey. Because
            // it won't clash with any other instructions
            int actionKey = ++serviceInstructionsSize;
            instructionSet.add(MDSALUtil.buildAndGetPopVlanActionInstruction(actionKey, ++serviceInstructionsSize));
        }

        if (lportTag != 0L) {
            BigInteger[] metadataValues = IfmUtil.mergeOpenflowMetadataWriteInstructions(instructions);
            short index = boundServiceNew.getServicePriority();
            BigInteger metadata = MetaDataUtil.getMetaDataForLPortDispatcher(lportTag, ++index, metadataValues[0],
                    isExternal(iface));
            BigInteger metadataMask = MetaDataUtil.getMetaDataMaskForLPortDispatcher(
                    MetaDataUtil.METADATA_MASK_SERVICE_INDEX, MetaDataUtil.METADATA_MASK_LPORT_TAG_SH_FLAG,
                    metadataValues[1]);
            instructionSet.add(
                    MDSALUtil.buildAndGetWriteMetadaInstruction(metadata, metadataMask, ++serviceInstructionsSize));
        }

        if (instructions != null && !instructions.isEmpty()) {
            for (Instruction info : instructions) {
                // Skip meta data write as that is handled already
                if (info.getInstruction() instanceof WriteMetadataCase) {
                    continue;
                } else if (info.getInstruction() instanceof WriteActionsCase) {
                    info = MDSALUtil.buildWriteActionsInstruction(ActionConverterUtil.convertServiceActionToFlowAction(
                            ((WriteActionsCase) info.getInstruction()).getWriteActions().getAction()));
                } else if (info.getInstruction() instanceof ApplyActionsCase) {
                    info = MDSALUtil.buildApplyActionsInstruction(ActionConverterUtil.convertServiceActionToFlowAction(
                            ((ApplyActionsCase) info.getInstruction()).getApplyActions().getAction()));
                }
                instructionSet.add(info);
            }
        }

        String serviceRef = boundServiceNew.getServiceName();
        String flowRef = getFlowRef(dpId, NwConstants.VLAN_INTERFACE_INGRESS_TABLE, iface.getName(),
                boundServiceNew.getServicePriority());
        StypeOpenflow stypeOpenflow = boundServiceNew.augmentation(StypeOpenflow.class);
        Flow ingressFlow = MDSALUtil.buildFlowNew(tableId, flowRef, stypeOpenflow.getFlowPriority(), serviceRef, 0, 0,
                stypeOpenflow.getFlowCookie(), matches, instructionSet);
        installFlow(dpId, ingressFlow, writeTransaction);
    }

    public static void installFlow(BigInteger dpId, Flow flow, WriteTransaction writeTransaction) {
        FlowKey flowKey = new FlowKey(new FlowId(flow.getId()));
        Node nodeDpn = buildInventoryDpnNode(dpId);
        InstanceIdentifier<Flow> flowInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.key()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(flow.getTableId())).child(Flow.class, flowKey).build();

        writeTransaction.put(LogicalDatastoreType.CONFIGURATION, flowInstanceId, flow, true);
    }

    private static Node buildInventoryDpnNode(BigInteger dpnId) {
        NodeId nodeId = new NodeId("openflow:" + dpnId);
        return new NodeBuilder().setId(nodeId).withKey(new NodeKey(nodeId)).build();
    }

    public static void installLPortDispatcherFlow(BigInteger dpId, BoundServices boundService, String interfaceName,
            WriteTransaction writeTransaction, int interfaceTag, short currentServiceIndex, short nextServiceIndex) {
        String serviceRef = boundService.getServiceName();
        List<MatchInfo> matches = FlowBasedServicesUtils.getMatchInfoForDispatcherTable(interfaceTag,
                currentServiceIndex);

        // Get the metadata and mask from the service's write metadata
        // instruction
        StypeOpenflow stypeOpenFlow = boundService.augmentation(StypeOpenflow.class);
        List<Instruction> serviceInstructions = stypeOpenFlow.getInstruction();
        int instructionSize = serviceInstructions != null ? serviceInstructions.size() : 0;
        BigInteger[] metadataValues = IfmUtil.mergeOpenflowMetadataWriteInstructions(serviceInstructions);
        BigInteger metadata = MetaDataUtil.getMetaDataForLPortDispatcher(interfaceTag, nextServiceIndex,
                metadataValues[0]);
        BigInteger metadataMask = MetaDataUtil.getWriteMetaDataMaskForDispatcherTable();

        // build the final instruction for LPort Dispatcher table flow entry
        List<Instruction> instructions = new ArrayList<>();
        instructions.add(MDSALUtil.buildAndGetWriteMetadaInstruction(metadata, metadataMask, ++instructionSize));
        if (serviceInstructions != null && !serviceInstructions.isEmpty()) {
            for (Instruction info : serviceInstructions) {
                // Skip meta data write as that is handled already
                if (info.getInstruction() instanceof WriteMetadataCase) {
                    continue;
                } else if (info.getInstruction() instanceof WriteActionsCase) {
                    info = MDSALUtil.buildWriteActionsInstruction(ActionConverterUtil.convertServiceActionToFlowAction(
                            ((WriteActionsCase) info.getInstruction()).getWriteActions().getAction()));
                } else if (info.getInstruction() instanceof ApplyActionsCase) {
                    info = MDSALUtil.buildApplyActionsInstruction(ActionConverterUtil.convertServiceActionToFlowAction(
                            ((ApplyActionsCase) info.getInstruction()).getApplyActions().getAction()));
                }
                instructions.add(info);
            }
        }

        // build the flow and install it
        String flowRef = getFlowRef(dpId, NwConstants.LPORT_DISPATCHER_TABLE, interfaceName,
                currentServiceIndex);
        Flow ingressFlow = MDSALUtil.buildFlowNew(NwConstants.LPORT_DISPATCHER_TABLE, flowRef,
                DEFAULT_DISPATCHER_PRIORITY, serviceRef, 0, 0, stypeOpenFlow.getFlowCookie(), matches,
                instructions);
        LOG.debug("Installing LPort Dispatcher Flow on DPN {}, for interface {}, with flowRef {}", dpId,
            interfaceName, flowRef);
        installFlow(dpId, ingressFlow, writeTransaction);
    }

    public static void installEgressDispatcherFlows(BigInteger dpId, BoundServices boundService, String interfaceName,
            WriteTransaction writeTransaction, int interfaceTag, short currentServiceIndex, short nextServiceIndex,
            Interface iface) {
        LOG.debug("Installing Egress Dispatcher Flows on dpn : {}, for interface : {}", dpId, interfaceName);
        installEgressDispatcherFlow(dpId, boundService, interfaceName, writeTransaction, interfaceTag,
                currentServiceIndex, nextServiceIndex);

        // Install Split Horizon drop flow only for the default egress service -
        // this flow drops traffic targeted to external interfaces if they
        // arrived
        // from an external interface (marked with the SH bit)
        if (boundService.getServicePriority() == ServiceIndex.getIndex(NwConstants.DEFAULT_EGRESS_SERVICE_NAME,
                NwConstants.DEFAULT_EGRESS_SERVICE_INDEX)) {
            installEgressDispatcherSplitHorizonFlow(dpId, boundService, interfaceName, writeTransaction, interfaceTag,
                    currentServiceIndex, iface);
        }
    }

    private static void installEgressDispatcherFlow(BigInteger dpId, BoundServices boundService, String interfaceName,
            WriteTransaction writeTransaction, int interfaceTag, short currentServiceIndex, short nextServiceIndex) {

        // Get the metadata and mask from the service's write metadata instruction
        StypeOpenflow stypeOpenflow = boundService.augmentation(StypeOpenflow.class);
        if (stypeOpenflow == null) {
            LOG.warn("Could not install egress dispatcher flow, missing service openflow configuration");
            return;
        }
        List<Instruction> serviceInstructions = stypeOpenflow.getInstruction() != null
                ? stypeOpenflow.getInstruction()
                : Collections.emptyList();

        // build the final instruction for LPort Dispatcher table flow entry
        List<Action> finalApplyActions = new ArrayList<>();
        List<Instruction> instructions = new ArrayList<>();
        if (boundService.getServicePriority() != ServiceIndex.getIndex(NwConstants.DEFAULT_EGRESS_SERVICE_NAME,
                NwConstants.DEFAULT_EGRESS_SERVICE_INDEX)) {
            BigInteger[] metadataValues = IfmUtil.mergeOpenflowMetadataWriteInstructions(serviceInstructions);
            BigInteger metadataMask = MetaDataUtil.getWriteMetaDataMaskForEgressDispatcherTable();
            instructions.add(MDSALUtil.buildAndGetWriteMetadaInstruction(metadataValues[0], metadataMask,
                    instructions.size()));
            finalApplyActions.add(MDSALUtil.createSetReg6Action(finalApplyActions.size(), 0, 31,
                    MetaDataUtil.getReg6ValueForLPortDispatcher(interfaceTag, nextServiceIndex)));
        }

        final int applyActionsOffset = finalApplyActions.size();
        for (Instruction info : serviceInstructions) {
            if (info.getInstruction() instanceof WriteActionsCase) {
                List<Action> writeActions = ActionConverterUtil.convertServiceActionToFlowAction(
                        ((WriteActionsCase) info.getInstruction()).getWriteActions().getAction());
                instructions.add(MDSALUtil.buildWriteActionsInstruction(writeActions, instructions.size()));
            } else if (info.getInstruction() instanceof ApplyActionsCase) {
                List<Action> applyActions = ActionConverterUtil.convertServiceActionToFlowAction(
                        ((ApplyActionsCase) info.getInstruction()).getApplyActions().getAction(),
                        applyActionsOffset);
                finalApplyActions.addAll(applyActions);
            } else if (!(info.getInstruction() instanceof WriteMetadataCase)) {
                // Skip meta data write as that is handled already
                instructions.add(MDSALUtil.buildInstruction(info, instructions.size()));
            }
        }
        if (!finalApplyActions.isEmpty()) {
            instructions.add(MDSALUtil.buildApplyActionsInstruction(finalApplyActions, instructions.size()));
        }

        // build the flow and install it
        String serviceRef = boundService.getServiceName();
        List<? extends MatchInfoBase> matches = FlowBasedServicesUtils
                .getMatchInfoForEgressDispatcherTable(interfaceTag, currentServiceIndex);
        String flowRef = getFlowRef(dpId, NwConstants.EGRESS_LPORT_DISPATCHER_TABLE, interfaceName,
                currentServiceIndex);
        Flow egressFlow = MDSALUtil.buildFlowNew(NwConstants.EGRESS_LPORT_DISPATCHER_TABLE, flowRef,
                boundService.getServicePriority(), serviceRef, 0, 0, stypeOpenflow.getFlowCookie(), matches,
                instructions);
        LOG.debug("Installing Egress Dispatcher Flow for interface : {}, with flow-ref : {}", interfaceName, flowRef);
        installFlow(dpId, egressFlow, writeTransaction);
    }

    public static void installEgressDispatcherSplitHorizonFlow(BigInteger dpId, BoundServices boundService,
            String interfaceName, WriteTransaction writeTransaction, int interfaceTag, short currentServiceIndex,
            Interface iface) {
        // only install split horizon drop flows for external interfaces
        if (!isExternal(iface)) {
            return;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Installing split horizon drop flow for external interface {} on dpId {}", interfaceName, dpId);
        }

        BigInteger shFlagSet = BigInteger.ONE; // BigInteger.ONE is used for
                                                // checking the Split-Horizon
                                                // flag
        List<MatchInfoBase> shMatches = FlowBasedServicesUtils.getMatchInfoForEgressDispatcherTable(interfaceTag,
                currentServiceIndex);
        shMatches.add(new MatchMetadata(shFlagSet, MetaDataUtil.METADATA_MASK_SH_FLAG));
        List<InstructionInfo> shInstructions = new ArrayList<>();
        List<ActionInfo> actionsInfos = new ArrayList<>();
        actionsInfos.add(new ActionDrop());
        shInstructions.add(new InstructionApplyActions(actionsInfos));

        String flowRef = getSplitHorizonFlowRef(dpId, NwConstants.EGRESS_LPORT_DISPATCHER_TABLE, interfaceName,
                shFlagSet);
        String serviceRef = boundService.getServiceName();
        // This must be higher priority than the egress flow
        int splitHorizonFlowPriority = boundService.getServicePriority() + 1;
        StypeOpenflow stypeOpenFlow = boundService.augmentation(StypeOpenflow.class);
        Flow egressSplitHorizonFlow = MDSALUtil.buildFlow(NwConstants.EGRESS_LPORT_DISPATCHER_TABLE, flowRef,
                splitHorizonFlowPriority, serviceRef, 0, 0, stypeOpenFlow.getFlowCookie(), shMatches, shInstructions);

        installFlow(dpId, egressSplitHorizonFlow, writeTransaction);
    }

    public static BoundServices getBoundServices(String serviceName, short servicePriority, int flowPriority,
            BigInteger cookie, List<Instruction> instructions) {
        StypeOpenflowBuilder augBuilder = new StypeOpenflowBuilder().setFlowCookie(cookie).setFlowPriority(flowPriority)
                .setInstruction(instructions);
        return new BoundServicesBuilder().withKey(new BoundServicesKey(servicePriority)).setServiceName(serviceName)
                .setServicePriority(servicePriority).setServiceType(ServiceTypeFlowBased.class)
                .addAugmentation(StypeOpenflow.class, augBuilder.build()).build();
    }

    public static InstanceIdentifier<BoundServices> buildServiceId(String interfaceName, short serviceIndex) {
        return InstanceIdentifier.builder(ServiceBindings.class)
                .child(ServicesInfo.class, new ServicesInfoKey(interfaceName, ServiceModeIngress.class))
                .child(BoundServices.class, new BoundServicesKey(serviceIndex)).build();
    }

    public static InstanceIdentifier<BoundServices> buildServiceId(String interfaceName, short serviceIndex,
            Class<? extends ServiceModeBase> serviceMode) {
        return InstanceIdentifier.builder(ServiceBindings.class)
                .child(ServicesInfo.class, new ServicesInfoKey(interfaceName, serviceMode))
                .child(BoundServices.class, new BoundServicesKey(serviceIndex)).build();
    }

    public static InstanceIdentifier<BoundServices> buildDefaultServiceId(String interfaceName) {
        return buildServiceId(interfaceName, ServiceIndex.getIndex(NwConstants.DEFAULT_EGRESS_SERVICE_NAME,
                NwConstants.DEFAULT_EGRESS_SERVICE_INDEX), ServiceModeEgress.class);
    }

    public static ListenableFuture<Void> bindDefaultEgressDispatcherService(ManagedNewTransactionRunner txRunner,
            Interface interfaceInfo, String portNo, String interfaceName, int ifIndex) {
        List<Instruction> instructions =
                IfmUtil.getEgressInstructionsForInterface(interfaceInfo, portNo, null, true, ifIndex, 0);
        return bindDefaultEgressDispatcherService(txRunner, interfaceName, instructions);
    }

    public static ListenableFuture<Void> bindDefaultEgressDispatcherService(ManagedNewTransactionRunner txRunner,
            Interface interfaceInfo, String interfaceName, int ifIndex, long groupId) {
        List<Instruction> instructions =
             IfmUtil.getEgressInstructionsForInterface(interfaceInfo, StringUtils.EMPTY, null, true, ifIndex, groupId);
        return bindDefaultEgressDispatcherService(txRunner, interfaceName, instructions);
    }

    public static ListenableFuture<Void> bindDefaultEgressDispatcherService(ManagedNewTransactionRunner txRunner,
            String interfaceName, List<Instruction> instructions) {
        return txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx -> {
            int priority = ServiceIndex.getIndex(NwConstants.DEFAULT_EGRESS_SERVICE_NAME,
                    NwConstants.DEFAULT_EGRESS_SERVICE_INDEX);
            BoundServices
                    serviceInfo =
                    getBoundServices(String.format("%s.%s", "default", interfaceName),
                            ServiceIndex.getIndex(NwConstants.DEFAULT_EGRESS_SERVICE_NAME,
                                    NwConstants.DEFAULT_EGRESS_SERVICE_INDEX),
                            priority, NwConstants.EGRESS_DISPATCHER_TABLE_COOKIE, instructions);
            IfmUtil.bindService(tx, interfaceName, serviceInfo, ServiceModeEgress.class);
        });
    }

    public static void removeIngressFlow(String interfaceName, BigInteger dpId, ManagedNewTransactionRunner txRunner,
            List<ListenableFuture<Void>> futures) {
        if (dpId == null) {
            return;
        }
        LOG.debug("Removing Ingress Flows for {}", interfaceName);
        futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx -> {
            String flowKeyStr = getFlowRef(IfmConstants.VLAN_INTERFACE_INGRESS_TABLE, dpId, interfaceName);
            FlowKey flowKey = new FlowKey(new FlowId(flowKeyStr));
            Node nodeDpn = buildInventoryDpnNode(dpId);
            InstanceIdentifier<Flow> flowInstanceId = InstanceIdentifier.builder(Nodes.class)
                    .child(Node.class, nodeDpn.key()).augmentation(FlowCapableNode.class)
                    .child(Table.class, new TableKey(NwConstants.VLAN_INTERFACE_INGRESS_TABLE)).child(Flow.class,
                            flowKey)
                    .build();

            tx.delete(LogicalDatastoreType.CONFIGURATION, flowInstanceId);
        }));
    }

    public static void removeIngressFlow(String name, BoundServices serviceOld, BigInteger dpId,
            WriteTransaction writeTransaction) {
        String flowKeyStr = getFlowRef(dpId, NwConstants.VLAN_INTERFACE_INGRESS_TABLE, name,
                serviceOld.getServicePriority());
        LOG.debug("Removing Ingress Flow {}", flowKeyStr);
        FlowKey flowKey = new FlowKey(new FlowId(flowKeyStr));
        Node nodeDpn = buildInventoryDpnNode(dpId);
        InstanceIdentifier<Flow> flowInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.key()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(NwConstants.VLAN_INTERFACE_INGRESS_TABLE)).child(Flow.class, flowKey)
                .build();

        writeTransaction.delete(LogicalDatastoreType.CONFIGURATION, flowInstanceId);
    }

    public static void removeLPortDispatcherFlow(BigInteger dpId, String iface, BoundServices boundServicesOld,
            WriteTransaction writeTransaction, short currentServiceIndex) {
        LOG.debug("Removing LPort Dispatcher Flows {}, {}", dpId, iface);

        boundServicesOld.augmentation(StypeOpenflow.class);
        // build the flow and install it
        String flowRef = getFlowRef(dpId, NwConstants.LPORT_DISPATCHER_TABLE, iface,
                currentServiceIndex);
        FlowKey flowKey = new FlowKey(new FlowId(flowRef));
        Node nodeDpn = buildInventoryDpnNode(dpId);
        InstanceIdentifier<Flow> flowInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.key()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(NwConstants.LPORT_DISPATCHER_TABLE)).child(Flow.class, flowKey)
                .build();

        writeTransaction.delete(LogicalDatastoreType.CONFIGURATION, flowInstanceId);
    }

    public static void removeEgressDispatcherFlows(BigInteger dpId, String iface,
            WriteTransaction writeTransaction, short currentServiceIndex) {
        LOG.debug("Removing Egress Dispatcher Flows {}, {}", dpId, iface);
        removeEgressDispatcherFlow(dpId, iface, writeTransaction, currentServiceIndex);
        removeEgressSplitHorizonDispatcherFlow(dpId, iface, writeTransaction);
    }

    private static void removeEgressDispatcherFlow(BigInteger dpId, String iface, WriteTransaction writeTransaction,
            short currentServiceIndex) {
        // build the flow and install it
        String flowRef = getFlowRef(dpId, NwConstants.EGRESS_LPORT_DISPATCHER_TABLE, iface,
                currentServiceIndex);
        FlowKey flowKey = new FlowKey(new FlowId(flowRef));
        Node nodeDpn = buildInventoryDpnNode(dpId);
        InstanceIdentifier<Flow> flowInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.key()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(NwConstants.EGRESS_LPORT_DISPATCHER_TABLE)).child(Flow.class, flowKey)
                .build();

        writeTransaction.delete(LogicalDatastoreType.CONFIGURATION, flowInstanceId);
    }

    public static void removeEgressSplitHorizonDispatcherFlow(BigInteger dpId, String iface,
            WriteTransaction writeTransaction) {
        // BigInteger.ONE is used for checking the Split-Horizon flag
        BigInteger shFlagSet = BigInteger.ONE;
        String shFlowRef = getSplitHorizonFlowRef(dpId, NwConstants.EGRESS_LPORT_DISPATCHER_TABLE, iface,
                shFlagSet);
        FlowKey shFlowKey = new FlowKey(new FlowId(shFlowRef));
        Node nodeDpn = buildInventoryDpnNode(dpId);
        InstanceIdentifier<Flow> shFlowInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.key()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(NwConstants.EGRESS_LPORT_DISPATCHER_TABLE))
                .child(Flow.class, shFlowKey).build();

        writeTransaction.delete(LogicalDatastoreType.CONFIGURATION, shFlowInstanceId);
    }

    public static String getFlowRef(short tableId, BigInteger dpnId, String infName) {
        return String.format("%d:%s:%s", tableId, dpnId, infName);
    }

    private static String getFlowRef(BigInteger dpnId, short tableId, String iface, short currentServiceIndex) {
        return String.valueOf(dpnId) + NwConstants.FLOWID_SEPARATOR + tableId + NwConstants.FLOWID_SEPARATOR + iface
                + NwConstants.FLOWID_SEPARATOR + currentServiceIndex;
    }

    private static String getSplitHorizonFlowRef(BigInteger dpnId, short tableId, String iface, BigInteger shFlag) {
        return String.valueOf(dpnId) + NwConstants.FLOWID_SEPARATOR + tableId + NwConstants.FLOWID_SEPARATOR + iface
                + NwConstants.FLOWID_SEPARATOR + shFlag.toString();
    }

    /**
     * This utility method returns an array of ServiceInfo in which index 0 will
     * have the immediate lower priority service and index 1 will have the
     * immediate higher priority service among the list of existing
     * serviceInfos.
     *
     * @param serviceInfos
     *            list of services bound
     * @param currentServiceInfo
     *            current service bound
     * @return array bound services
     */
    public static BoundServices[] getHighAndLowPriorityService(List<BoundServices> serviceInfos,
            BoundServices currentServiceInfo) {
        if (serviceInfos == null || serviceInfos.isEmpty()) {
            return new BoundServices[] { null, null };
        }

        // This will be used to hold the immediate higher service priority with respect to the currentServiceInfo
        BoundServices higher = null;
        // This will be used to hold the immediate lower service priority with respect to the currentServiceInfo
        BoundServices lower = null;

        List<BoundServices> availableServiceInfos = new ArrayList<>(serviceInfos);
        availableServiceInfos.sort(Comparator.comparing(BoundServices::getServicePriority));
        for (BoundServices availableServiceInfo : availableServiceInfos) {
            if (currentServiceInfo.getServicePriority() < availableServiceInfo.getServicePriority()) {
                lower = availableServiceInfo;
                break;
            } else {
                higher = availableServiceInfo;
            }
        }
        return new BoundServices[] { lower, higher };
    }

    public static BoundServices getHighestPriorityService(List<BoundServices> serviceInfos) {
        List<BoundServices> availableServiceInfos = new ArrayList<>(serviceInfos);
        if (availableServiceInfos.isEmpty()) {
            return null;
        }
        BoundServices highPriorityService = availableServiceInfos.get(0);
        availableServiceInfos.remove(0);
        for (BoundServices availableServiceInfo : availableServiceInfos) {
            if (availableServiceInfo.getServicePriority() < highPriorityService.getServicePriority()) {
                highPriorityService = availableServiceInfo;
            }
        }
        return highPriorityService;
    }

    public static void installLportIngressFlow(BigInteger dpId, long portNo, Interface iface,
            List<ListenableFuture<Void>> futures, ManagedNewTransactionRunner txRunner, int lportTag) {
        int vlanId = 0;
        boolean isVlanTransparent = false;

        IfL2vlan l2vlan = iface.augmentation(IfL2vlan.class);
        if (l2vlan != null) {
            vlanId = l2vlan.getVlanId() == null ? 0 : l2vlan.getVlanId().getValue();
            isVlanTransparent = l2vlan.getL2vlanMode() == IfL2vlan.L2vlanMode.Transparent;
        }
        int instructionKey = 0;

        List<Instruction> instructions = new ArrayList<>();

        final SplitHorizon splitHorizon = iface.augmentation(SplitHorizon.class);
        boolean overrideSplitHorizonProtection = splitHorizon != null
                && splitHorizon.isOverrideSplitHorizonProtection();
        int actionKey = -1;
        List<Action> actions = new ArrayList<>();
        if (vlanId != 0 && !isVlanTransparent) {
            actions.add(MDSALUtil.createPopVlanAction(++actionKey));
        }
        if (overrideSplitHorizonProtection) {
            actions.add(MDSALUtil.createNxOfInPortAction(++actionKey, 0));
        }
        if (!actions.isEmpty()) {
            instructions.add(MDSALUtil.buildApplyActionsInstruction(actions, instructionKey++));
        }
        BigInteger metadata = MetaDataUtil.getMetaDataForLPortDispatcher(lportTag, (short) 0, BigInteger.ZERO,
                isExternal(iface));
        BigInteger metadataMask = MetaDataUtil
                .getMetaDataMaskForLPortDispatcher(MetaDataUtil.METADATA_MASK_LPORT_TAG_SH_FLAG);
        instructions.add(MDSALUtil.buildAndGetWriteMetadaInstruction(metadata, metadataMask, instructionKey++));
        instructions
                .add(MDSALUtil.buildAndGetGotoTableInstruction(NwConstants.LPORT_DISPATCHER_TABLE, instructionKey++));
        int priority = isVlanTransparent ? 1
                : vlanId == 0 ? IfmConstants.FLOW_PRIORITY_FOR_UNTAGGED_VLAN : IfmConstants.FLOW_HIGH_PRIORITY;
        String flowRef = getFlowRef(IfmConstants.VLAN_INTERFACE_INGRESS_TABLE, dpId, iface.getName());
        List<MatchInfo> matches = getMatchInfoForVlanPortAtIngressTable(dpId, portNo, iface);
        Flow ingressFlow = MDSALUtil.buildFlowNew(IfmConstants.VLAN_INTERFACE_INGRESS_TABLE, flowRef, priority, flowRef,
                0, 0, NwConstants.VLAN_TABLE_COOKIE, matches, instructions);
        LOG.debug("Installing ingress flow {} for {}", flowRef, iface.getName());
        futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx -> installFlow(dpId, ingressFlow, tx)));
    }

    public static BoundServicesState buildBoundServicesState(
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface
            interfaceState, Class<? extends ServiceModeBase> serviceMode) {
        NodeConnectorId nodeConnectorId = IfmUtil.getNodeConnectorIdFromInterface(interfaceState);
        BigInteger dpId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
        long portNo = IfmUtil.getPortNumberFromNodeConnectorId(nodeConnectorId);
        BoundServicesStateKey boundServicesStateKey = new BoundServicesStateKey(interfaceState.getName(), serviceMode);
        return new BoundServicesStateBuilder().setDpid(dpId).setIfIndex(interfaceState.getIfIndex())
            .setInterfaceName(interfaceState.getName()).setInterfaceType(interfaceState.getType()).setPortNo(portNo)
            .setServiceMode(serviceMode).withKey(boundServicesStateKey).build();
    }

    public static BoundServicesState getBoundServicesState(ReadTransaction tx,
                                                           String interfaceName,
                                                           Class<? extends ServiceModeBase> serviceMode)
            throws ReadFailedException {
        InstanceIdentifier<BoundServicesState> id = InstanceIdentifier.builder(BoundServicesStateList.class)
            .child(BoundServicesState.class, new BoundServicesStateKey(interfaceName, serviceMode)).build();
        return tx.read(LogicalDatastoreType.OPERATIONAL, id).checkedGet().orNull();
    }

    public static void addBoundServicesState(WriteTransaction tx, String interfaceName,
                                             BoundServicesState interfaceBoundServicesState) {
        LOG.info("adding bound-service state information for interface : {}, service-mode : {}",
            interfaceBoundServicesState.getInterfaceName(), interfaceBoundServicesState.getServiceMode().getName());
        InstanceIdentifier<BoundServicesState> id = InstanceIdentifier.builder(BoundServicesStateList.class)
            .child(BoundServicesState.class, new BoundServicesStateKey(interfaceName,
                interfaceBoundServicesState.getServiceMode())).build();
        tx.put(LogicalDatastoreType.OPERATIONAL, id, interfaceBoundServicesState,
                WriteTransaction.CREATE_MISSING_PARENTS);
    }

    public static  void removeBoundServicesState(WriteTransaction tx,
                                                 String interfaceName, Class<? extends ServiceModeBase> serviceMode) {
        LOG.info("remove bound-service state information for interface : {}, service-mode : {}", interfaceName,
            serviceMode.getName());
        InstanceIdentifier<BoundServicesState> id = InstanceIdentifier.builder(BoundServicesStateList.class)
            .child(BoundServicesState.class, new BoundServicesStateKey(interfaceName, serviceMode)).build();
        tx.delete(LogicalDatastoreType.OPERATIONAL, id);
    }

    public static boolean isInterfaceTypeBasedServiceBinding(String interfaceName) {
        return INTERFACE_TYPE_BASED_SERVICE_BINDING_KEYWORDS.contains(interfaceName);
    }

    private static boolean isExternal(Interface iface) {
        if (iface == null) {
            return false;
        }
        IfExternal ifExternal = iface.augmentation(IfExternal.class);
        return ifExternal != null && Boolean.TRUE.equals(ifExternal.isExternal());
    }
}
