/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableBiMap;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.mdsalutil.MatchFieldType;
import org.opendaylight.genius.mdsalutil.MatchInfo;
import org.opendaylight.genius.mdsalutil.MatchInfoBase;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.ActionType;
import org.opendaylight.genius.mdsalutil.InstructionInfo;
import org.opendaylight.genius.mdsalutil.InstructionType;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.MetaDataUtil;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.mdsalutil.NxMatchInfo;
import org.opendaylight.genius.mdsalutil.NxMatchFieldType;
import org.opendaylight.genius.utils.ServiceIndex;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.WriteMetadataCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.StypeOpenflow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.StypeOpenflowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceTypeFlowBased;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceBindings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeEgress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeIngress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfoKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServicesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServicesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.SplitHorizon;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfExternal;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowBasedServicesUtils {
    private static final Logger LOG = LoggerFactory.getLogger(FlowBasedServicesUtils.class);

    public enum ServiceMode  {
        INGRESS,
        EGRESS;
    }

    public static final ImmutableBiMap SERVICE_MODE_MAP =
            new ImmutableBiMap.Builder<ServiceMode, Class<? extends ServiceModeBase>>()
                    .put(ServiceMode.EGRESS, ServiceModeEgress.class)
                    .put(ServiceMode.INGRESS, ServiceModeIngress.class)
                    .build();

    public static ServicesInfo getServicesInfoForInterface(String interfaceName, Class<? extends ServiceModeBase> serviceMode,
                                                           DataBroker dataBroker) {
        ServicesInfoKey servicesInfoKey = new ServicesInfoKey(interfaceName,serviceMode);
        InstanceIdentifier.InstanceIdentifierBuilder<ServicesInfo> servicesInfoIdentifierBuilder =
                InstanceIdentifier.builder(ServiceBindings.class).child(ServicesInfo.class, servicesInfoKey);
        Optional<ServicesInfo> servicesInfoOptional = IfmUtil.read(LogicalDatastoreType.CONFIGURATION,
                servicesInfoIdentifierBuilder.build(), dataBroker);

        if (servicesInfoOptional.isPresent()) {
            return servicesInfoOptional.get();
        }

        return null;
    }

    public static NodeConnectorId getNodeConnectorIdFromInterface(String interfaceName, DataBroker dataBroker) {
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface ifState =
                InterfaceManagerCommonUtils.getInterfaceStateFromOperDS(interfaceName, dataBroker);
        if(ifState != null) {
            List<String> ofportIds = ifState.getLowerLayerIf();
            return new NodeConnectorId(ofportIds.get(0));
        }
        return null;
    }

    public static NodeConnectorId getNodeConnectorIdFromInterface(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface ifState) {
        if(ifState != null) {
            List<String> ofportIds = ifState.getLowerLayerIf();
            return new NodeConnectorId(ofportIds.get(0));
        }
        return null;
    }

    public static BigInteger getDpnIdFromInterface(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface ifState) {
        NodeConnectorId nodeConnectorId = null;
        if(ifState != null) {
            List<String> ofportIds = ifState.getLowerLayerIf();
            nodeConnectorId = new NodeConnectorId(ofportIds.get(0));
        }
        return IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
    }

    public static List<MatchInfo> getMatchInfoForVlanPortAtIngressTable(BigInteger dpId, long portNo, Interface iface) {
        List<MatchInfo> matches = new ArrayList<>();
        matches.add(new MatchInfo(MatchFieldType.in_port, new BigInteger[] {dpId, BigInteger.valueOf(portNo)}));
        int vlanId = 0;
        IfL2vlan l2vlan = iface.getAugmentation(IfL2vlan.class);
        if(l2vlan != null && l2vlan.getL2vlanMode() != IfL2vlan.L2vlanMode.Transparent){
            vlanId = l2vlan.getVlanId() == null ? 0 : l2vlan.getVlanId().getValue();
        }
        if (vlanId > 0) {
            matches.add(new MatchInfo(MatchFieldType.vlan_vid, new long[]{vlanId}));
        }
        return matches;
    }

    public static List<MatchInfo> getMatchInfoForTunnelPortAtIngressTable(BigInteger dpId, long portNo) {
        List<MatchInfo> matches = new ArrayList<>();
        matches.add(new MatchInfo(MatchFieldType.in_port, new BigInteger[]{dpId, BigInteger.valueOf(portNo)}));
        return matches;
    }

    public static List<MatchInfo> getMatchInfoForDispatcherTable(BigInteger dpId,
                                                                 int interfaceTag, short servicePriority) {
        List<MatchInfo> matches = new ArrayList<>();
        matches.add(new MatchInfo(MatchFieldType.metadata, new BigInteger[] {
                MetaDataUtil.getMetaDataForLPortDispatcher(interfaceTag, servicePriority),
                MetaDataUtil.getMetaDataMaskForLPortDispatcher() }));
        return matches;
    }

    public static List<NxMatchInfo> getMatchInfoForEgressDispatcherTable(int interfaceTag, short serviceIndex) {
        List<NxMatchInfo> matches = new ArrayList<>();
        matches.add(new NxMatchInfo(NxMatchFieldType.nxm_reg_6, new long[] {
                MetaDataUtil.getReg6ValueForLPortDispatcher(interfaceTag, serviceIndex)}));
        return matches;
    }

    public static void installInterfaceIngressFlow(BigInteger dpId, Interface iface,
                                                   BoundServices boundServiceNew,
                                                   WriteTransaction t,
                                                   List<MatchInfo> matches, int lportTag, short tableId) {
        List<Instruction> instructions = boundServiceNew.getAugmentation(StypeOpenflow.class).getInstruction();

        int serviceInstructionsSize = instructions.size();
        List<Instruction> instructionSet = new ArrayList<>();
        int vlanId = 0;
        IfL2vlan l2vlan = iface.getAugmentation(IfL2vlan.class);
        if(l2vlan != null && l2vlan.getVlanId() != null){
            vlanId = l2vlan.getVlanId().getValue();
        }
        if (vlanId != 0) {
            // incrementing instructionSize and using it as actionKey. Because it won't clash with any other instructions
            int actionKey = ++serviceInstructionsSize;
            instructionSet.add(MDSALUtil.buildAndGetPopVlanActionInstruction(actionKey, ++serviceInstructionsSize));
        }

        if (lportTag != 0L) {
            BigInteger[] metadataValues = IfmUtil.mergeOpenflowMetadataWriteInstructions(instructions);
            short sIndex = boundServiceNew.getServicePriority();
            BigInteger metadata = MetaDataUtil.getMetaDataForLPortDispatcher(lportTag,
                    ++sIndex, metadataValues[0], isExternal(iface));
            BigInteger metadataMask = MetaDataUtil.getMetaDataMaskForLPortDispatcher(
                    MetaDataUtil.METADATA_MASK_SERVICE_INDEX,
                    MetaDataUtil.METADATA_MASK_LPORT_TAG_SH_FLAG, metadataValues[1]);
            instructionSet.add(MDSALUtil.buildAndGetWriteMetadaInstruction(metadata, metadataMask,
                    ++serviceInstructionsSize));
        }

        if (instructions != null && !instructions.isEmpty()) {
            for (Instruction info : instructions) {
                // Skip meta data write as that is handled already
                if (info.getInstruction() instanceof WriteMetadataCase) {
                    continue;
                }
                instructionSet.add(info);
            }
        }

        String serviceRef = boundServiceNew.getServiceName();
        String flowRef = getFlowRef(dpId, NwConstants.VLAN_INTERFACE_INGRESS_TABLE, iface.getName(),
                boundServiceNew, boundServiceNew.getServicePriority());
        StypeOpenflow stypeOpenflow = boundServiceNew.getAugmentation(StypeOpenflow.class);
        Flow ingressFlow = MDSALUtil.buildFlowNew(tableId, flowRef,
                stypeOpenflow.getFlowPriority(), serviceRef, 0, 0,
                stypeOpenflow.getFlowCookie(), matches, instructionSet);
        installFlow(dpId, ingressFlow, t);
    }

    public static void installFlow(BigInteger dpId, Flow flow, WriteTransaction t) {
        FlowKey flowKey = new FlowKey(new FlowId(flow.getId()));
        Node nodeDpn = buildInventoryDpnNode(dpId);
        InstanceIdentifier<Flow> flowInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(flow.getTableId())).child(Flow.class,flowKey).build();

        t.put(LogicalDatastoreType.CONFIGURATION, flowInstanceId, flow, true);
    }

    public static void removeFlow(String flowRef, BigInteger dpId, WriteTransaction t) {
        LOG.debug("Removing Ingress Flows");
        FlowKey flowKey = new FlowKey(new FlowId(flowRef));
        Node nodeDpn = buildInventoryDpnNode(dpId);
        InstanceIdentifier<Flow> flowInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(NwConstants.VLAN_INTERFACE_INGRESS_TABLE)).child(Flow.class, flowKey).build();

        t.delete(LogicalDatastoreType.CONFIGURATION, flowInstanceId);
    }

    private static Node buildInventoryDpnNode(BigInteger dpnId) {
        NodeId nodeId = new NodeId("openflow:" + dpnId);
        Node nodeDpn = new NodeBuilder().setId(nodeId).setKey(new NodeKey(nodeId)).build();

        return nodeDpn;
    }

    public static void installLPortDispatcherFlow(BigInteger dpId, BoundServices boundService, String interfaceName,
                                                  WriteTransaction t, int interfaceTag, short currentServiceIndex, short nextServiceIndex) {
        LOG.debug("Installing LPort Dispatcher Flow {}, {}", dpId, interfaceName);
        String serviceRef = boundService.getServiceName();
        List<MatchInfo> matches = FlowBasedServicesUtils.getMatchInfoForDispatcherTable(dpId,
                interfaceTag, currentServiceIndex);

        // Get the metadata and mask from the service's write metadata instruction
        StypeOpenflow stypeOpenFlow = boundService.getAugmentation(StypeOpenflow.class);
        List<Instruction> serviceInstructions = stypeOpenFlow.getInstruction();
        int instructionSize = serviceInstructions.size();
        BigInteger[] metadataValues = IfmUtil.mergeOpenflowMetadataWriteInstructions(serviceInstructions);
        BigInteger metadata = MetaDataUtil.getMetaDataForLPortDispatcher(interfaceTag, nextServiceIndex, metadataValues[0]);
        BigInteger metadataMask = MetaDataUtil.getWriteMetaDataMaskForDispatcherTable();

        // build the final instruction for LPort Dispatcher table flow entry
        List<Instruction> instructions = new ArrayList<>();
        instructions.add(MDSALUtil.buildAndGetWriteMetadaInstruction(metadata, metadataMask, ++instructionSize));
        if (serviceInstructions != null && !serviceInstructions.isEmpty()) {
            for (Instruction info : serviceInstructions) {
                // Skip meta data write as that is handled already
                if (info.getInstruction() instanceof WriteMetadataCase) {
                    continue;
                }
                instructions.add(info);
            }
        }

        // build the flow and install it
        String flowRef = getFlowRef(dpId, NwConstants.LPORT_DISPATCHER_TABLE, interfaceName, boundService, currentServiceIndex);
        Flow ingressFlow = MDSALUtil.buildFlowNew(NwConstants.LPORT_DISPATCHER_TABLE, flowRef,
                boundService.getServicePriority(), serviceRef, 0, 0, stypeOpenFlow.getFlowCookie(), matches, instructions);
        installFlow(dpId, ingressFlow, t);
    }

    public static void installEgressDispatcherFlows(BigInteger dpId, BoundServices boundService, String interfaceName,
                                                  WriteTransaction t, int interfaceTag, short currentServiceIndex,
                                                  short nextServiceIndex, Interface iface) {
        LOG.debug("Installing Egress Dispatcher Flows {}, {}", dpId, interfaceName);
        installEgressDispatcherFlow(dpId, boundService, interfaceName, t, interfaceTag, currentServiceIndex, nextServiceIndex);

        // Install Split Horizon drop flows only for the default egress service
        if (boundService.getServicePriority() == ServiceIndex.getIndex(NwConstants.DEFAULT_EGRESS_SERVICE_NAME, NwConstants.DEFAULT_EGRESS_SERVICE_INDEX)) {
            installEgressDispatcherSplitHorizonFlow(dpId, boundService, interfaceName, t, interfaceTag, currentServiceIndex, iface);
        }
    }

    private static void installEgressDispatcherFlow(BigInteger dpId, BoundServices boundService, String interfaceName,
        WriteTransaction t, int interfaceTag, short currentServiceIndex, short nextServiceIndex) {
        String serviceRef = boundService.getServiceName();
        List<? extends MatchInfoBase> matches;
        matches = FlowBasedServicesUtils.getMatchInfoForEgressDispatcherTable(interfaceTag, currentServiceIndex);

        // Get the metadata and mask from the service's write metadata instruction
        StypeOpenflow stypeOpenFlow = boundService.getAugmentation(StypeOpenflow.class);
        List<Instruction> serviceInstructions = stypeOpenFlow.getInstruction();
        int instructionSize = serviceInstructions.size();

        // build the final instruction for LPort Dispatcher table flow entry
        List<Instruction> instructions = new ArrayList<Instruction>();
        if(boundService.getServicePriority() != ServiceIndex.getIndex(NwConstants.DEFAULT_EGRESS_SERVICE_NAME, NwConstants.DEFAULT_EGRESS_SERVICE_INDEX)) {
            BigInteger[] metadataValues = IfmUtil.mergeOpenflowMetadataWriteInstructions(serviceInstructions);
            BigInteger metadata = MetaDataUtil.getMetaDataForLPortDispatcher(interfaceTag, nextServiceIndex, metadataValues[0]);
            BigInteger metadataMask = MetaDataUtil.getWriteMetaDataMaskForDispatcherTable();
            instructions.add(MDSALUtil.buildAndGetWriteMetadaInstruction(metadata, metadataMask, ++instructionSize));
            instructions.add(MDSALUtil.buildAndGetSetReg6ActionInstruction(0, ++instructionSize, 0, 31,
                    MetaDataUtil.getReg6ValueForLPortDispatcher(interfaceTag, nextServiceIndex)));
        }
        if (serviceInstructions != null && !serviceInstructions.isEmpty()) {
            for (Instruction info : serviceInstructions) {
                // Skip meta data write as that is handled already
                if (info.getInstruction() instanceof WriteMetadataCase) {
                    continue;
                }
                instructions.add(info);
            }
        }

        // build the flow and install it
        String flowRef = getFlowRef(dpId, NwConstants.EGRESS_LPORT_DISPATCHER_TABLE, interfaceName, boundService, currentServiceIndex);
        Flow egressFlow = MDSALUtil.buildFlowNew(NwConstants.EGRESS_LPORT_DISPATCHER_TABLE, flowRef,
                boundService.getServicePriority(), serviceRef, 0, 0, stypeOpenFlow.getFlowCookie(), matches, instructions);
        installFlow(dpId, egressFlow, t);
    }

    public static void installEgressDispatcherSplitHorizonFlow(BigInteger dpId, BoundServices boundService, String interfaceName,
            WriteTransaction t, int interfaceTag, short currentServiceIndex, Interface iface) {
        // only install split horizon drop flows for external interfaces
        if (!isExternal(iface)) {
            return;
        }

        BigInteger shFlagSet = BigInteger.ONE; // BigInteger.ONE is used for checking the Split-Horizon flag
        StypeOpenflow stypeOpenFlow = boundService.getAugmentation(StypeOpenflow.class);
        List<MatchInfoBase> shMatches = new ArrayList<>();
        shMatches.add(new MatchInfo(MatchFieldType.metadata, new BigInteger[] { shFlagSet, MetaDataUtil.METADATA_MASK_SH_FLAG }));
        shMatches.add(new NxMatchInfo(NxMatchFieldType.nxm_reg_6, new long[] {
                MetaDataUtil.getReg6ValueForLPortDispatcher(interfaceTag, currentServiceIndex)}));
        List<InstructionInfo> shInstructions = new ArrayList<>();
        List<ActionInfo> actionsInfos = new ArrayList<>();
        actionsInfos.add(new ActionInfo(ActionType.drop_action, new String[] {}));
        shInstructions.add(new InstructionInfo(InstructionType.apply_actions, actionsInfos));

        String flowRef = getSplitHorizonFlowRef(dpId, NwConstants.EGRESS_LPORT_DISPATCHER_TABLE, interfaceName, currentServiceIndex, shFlagSet);
        String serviceRef = boundService.getServiceName();
        int splitHorizonFlowPriority = boundService.getServicePriority() + 1; // this must be higher priority than the egress flow
        Flow egressSplitHorizonFlow = MDSALUtil.buildFlow(NwConstants.EGRESS_LPORT_DISPATCHER_TABLE, flowRef,
                splitHorizonFlowPriority, serviceRef, 0, 0, stypeOpenFlow.getFlowCookie(), shMatches, shInstructions);

        installFlow(dpId, egressSplitHorizonFlow, t);
    }

    public static BoundServices getBoundServices(String serviceName, short servicePriority, int flowPriority,
                                                 BigInteger cookie, List<Instruction> instructions) {
        StypeOpenflowBuilder augBuilder = new StypeOpenflowBuilder().setFlowCookie(cookie).setFlowPriority(flowPriority).setInstruction(instructions);
        return new BoundServicesBuilder().setKey(new BoundServicesKey(servicePriority))
                .setServiceName(serviceName).setServicePriority(servicePriority)
                .setServiceType(ServiceTypeFlowBased.class).addAugmentation(StypeOpenflow.class, augBuilder.build()).build();
    }

    public static InstanceIdentifier<BoundServices> buildServiceId(String interfaceName, short serviceIndex) {
        return InstanceIdentifier.builder(ServiceBindings.class).child(ServicesInfo.class,
                new ServicesInfoKey(interfaceName, ServiceModeIngress.class))
                .child(BoundServices.class, new BoundServicesKey(serviceIndex)).build();
    }

    public static InstanceIdentifier<BoundServices> buildServiceId(String interfaceName, short serviceIndex, Class<? extends ServiceModeBase> serviceMode) {
        return InstanceIdentifier.builder(ServiceBindings.class).child(ServicesInfo.class,
                new ServicesInfoKey(interfaceName, serviceMode))
                .child(BoundServices.class, new BoundServicesKey(serviceIndex)).build();
    }

    public static void unbindDefaultEgressDispatcherService(DataBroker dataBroker, String interfaceName, String parentInterface) {
        IfmUtil.unbindService(dataBroker, interfaceName, buildServiceId(interfaceName,
                ServiceIndex.getIndex(NwConstants.DEFAULT_EGRESS_SERVICE_NAME, NwConstants.DEFAULT_EGRESS_SERVICE_INDEX),
                ServiceModeEgress.class), parentInterface);
    }

    public static void bindDefaultEgressDispatcherService(DataBroker dataBroker, List<ListenableFuture<Void>> futures,
                                                          Interface interfaceInfo, String portNo,
                                                          String interfaceName, int ifIndex) {
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        int priority = ServiceIndex.getIndex(NwConstants.DEFAULT_EGRESS_SERVICE_NAME, NwConstants.DEFAULT_EGRESS_SERVICE_INDEX);
        List<Instruction> instructions = IfmUtil.getEgressInstructionsForInterface(interfaceInfo, portNo, null, true, ifIndex);
        BoundServices
                serviceInfo =
                getBoundServices(String.format("%s.%s", "default", interfaceName),
                        ServiceIndex.getIndex(NwConstants.DEFAULT_EGRESS_SERVICE_NAME, NwConstants.DEFAULT_EGRESS_SERVICE_INDEX), priority,
                        NwConstants.EGRESS_DISPATCHER_TABLE_COOKIE, instructions);
        IfmUtil.bindService(tx, interfaceName, serviceInfo, ServiceModeEgress.class);
        futures.add(tx.submit());
    }

    public static void removeIngressFlow(String name, BoundServices serviceOld, BigInteger dpId, WriteTransaction t) {
        LOG.debug("Removing Ingress Flows");
        String flowKeyStr = getFlowRef(dpId, NwConstants.VLAN_INTERFACE_INGRESS_TABLE, name, serviceOld, serviceOld.getServicePriority());
        FlowKey flowKey = new FlowKey(new FlowId(flowKeyStr));
        Node nodeDpn = buildInventoryDpnNode(dpId);
        InstanceIdentifier<Flow> flowInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(NwConstants.VLAN_INTERFACE_INGRESS_TABLE)).child(Flow.class, flowKey).build();

        t.delete(LogicalDatastoreType.CONFIGURATION, flowInstanceId);
    }

    public static void removeLPortDispatcherFlow(BigInteger dpId, String iface, BoundServices boundServicesOld, WriteTransaction t, short currentServiceIndex) {
        LOG.debug("Removing LPort Dispatcher Flows {}, {}", dpId, iface);

        boundServicesOld.getAugmentation(StypeOpenflow.class);
        // build the flow and install it
        String flowRef = getFlowRef(dpId, NwConstants.LPORT_DISPATCHER_TABLE, iface, boundServicesOld, currentServiceIndex);
        FlowKey flowKey = new FlowKey(new FlowId(flowRef));
        Node nodeDpn = buildInventoryDpnNode(dpId);
        InstanceIdentifier<Flow> flowInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(NwConstants.LPORT_DISPATCHER_TABLE)).child(Flow.class, flowKey).build();

        t.delete(LogicalDatastoreType.CONFIGURATION, flowInstanceId);
    }

    public static void removeEgressDispatcherFlows(BigInteger dpId, String iface, BoundServices boundServicesOld, WriteTransaction t, short currentServiceIndex) {
        LOG.debug("Removing Egress Dispatcher Flows {}, {}", dpId, iface);
        removeEgressDispatcherFlow(dpId, iface, t, currentServiceIndex, boundServicesOld);
        removeEgressSplitHorizonDispatcherFlow(dpId, iface, t, currentServiceIndex);
    }

    private static void removeEgressDispatcherFlow(BigInteger dpId, String iface, WriteTransaction t,
            short currentServiceIndex, BoundServices boundServicesOld) {
        // build the flow and install it
        String flowRef = getFlowRef(dpId, NwConstants.EGRESS_LPORT_DISPATCHER_TABLE, iface, boundServicesOld, currentServiceIndex);
        FlowKey flowKey = new FlowKey(new FlowId(flowRef));
        Node nodeDpn = buildInventoryDpnNode(dpId);
        InstanceIdentifier<Flow> flowInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(NwConstants.EGRESS_LPORT_DISPATCHER_TABLE)).child(Flow.class, flowKey).build();

        t.delete(LogicalDatastoreType.CONFIGURATION, flowInstanceId);
    }

    public static void removeEgressSplitHorizonDispatcherFlow(BigInteger dpId, String iface, WriteTransaction t, short currentServiceIndex) {
        BigInteger shFlagSet = BigInteger.ONE; // BigInteger.ONE is used for checking the Split-Horizon flag
        String shFlowRef = getSplitHorizonFlowRef(dpId, NwConstants.EGRESS_LPORT_DISPATCHER_TABLE, iface, currentServiceIndex, shFlagSet);
        FlowKey shFlowKey = new FlowKey(new FlowId(shFlowRef));
        Node nodeDpn = buildInventoryDpnNode(dpId);
        InstanceIdentifier<Flow> shFlowInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(NwConstants.EGRESS_LPORT_DISPATCHER_TABLE)).child(Flow.class, shFlowKey).build();

        t.delete(LogicalDatastoreType.CONFIGURATION, shFlowInstanceId);
    }

    private static String getFlowRef(BigInteger dpnId, short tableId, String iface, BoundServices service, short currentServiceIndex) {
        return new StringBuffer().append(dpnId).append(tableId).append(NwConstants.FLOWID_SEPARATOR)
                .append(iface).append(NwConstants.FLOWID_SEPARATOR).append(currentServiceIndex).toString();
    }

    private static String getSplitHorizonFlowRef(BigInteger dpnId, short tableId, String iface, short currentServiceIndex, BigInteger shFlag) {
        return new StringBuffer().append(dpnId).append(tableId).append(NwConstants.FLOWID_SEPARATOR)
                .append(iface).append(NwConstants.FLOWID_SEPARATOR).append(shFlag.toString()).toString();
    }
    /**
     * This util method returns an array of ServiceInfo in which index 0 will
     * have the immediate lower priority service and index 1 will have the
     * immediate higher priority service among the list of existing serviceInfos
     *
     * @param serviceInfos
     * @param currentServiceInfo
     * @return
     */
    public static BoundServices[] getHighAndLowPriorityService(
            List<BoundServices> serviceInfos, BoundServices currentServiceInfo) {
        BoundServices higher = null; // this will be used to hold the immediate higher service priority with respect to the currentServiceInfo
        BoundServices lower = null; // this will be used to hold the immediate lower service priority with respect to the currentServiceInfo
        if (serviceInfos == null || serviceInfos.isEmpty()) {
            return new BoundServices[]{lower, higher};
        }
        List <BoundServices> availableServiceInfos = new ArrayList<>(serviceInfos);
        Collections.sort(availableServiceInfos, new Comparator<BoundServices>() {
            @Override
            public int compare(BoundServices serviceInfo1, BoundServices serviceInfo2) {
                return serviceInfo1.getServicePriority().compareTo(serviceInfo2.getServicePriority());
            }
        });
        for (BoundServices availableServiceInfo: availableServiceInfos) {
            if (currentServiceInfo.getServicePriority() < availableServiceInfo.getServicePriority()) {
                lower = availableServiceInfo;
                break;
            } else {
                higher = availableServiceInfo;
            }
        }
        return new BoundServices[]{lower,higher};
    }

    public static BoundServices getHighestPriorityService(List<BoundServices> serviceInfos) {
        List <BoundServices> availableServiceInfos = new ArrayList<>(serviceInfos);
        if (availableServiceInfos.isEmpty()) {
            return null;
        }
        BoundServices highPriorityService = availableServiceInfos.get(0);
        availableServiceInfos.remove(0);
        for (BoundServices availableServiceInfo: availableServiceInfos) {
            if (availableServiceInfo.getServicePriority() < highPriorityService.getServicePriority()) {
                highPriorityService = availableServiceInfo;
            }
        }
        return highPriorityService;
    }

    public static void installLportIngressFlow(BigInteger dpId, long portNo, Interface iface,
                                               List<ListenableFuture<Void>> futures, DataBroker dataBroker,
                                               int lportTag) {
        int vlanId = 0;
        boolean isVlanTransparent = false;
        WriteTransaction  inventoryConfigShardTransaction = dataBroker.newWriteOnlyTransaction();
        List<MatchInfo> matches = getMatchInfoForVlanPortAtIngressTable(dpId, portNo, iface);
        IfL2vlan l2vlan = iface.getAugmentation(IfL2vlan.class);
        if(l2vlan != null){
            vlanId = l2vlan.getVlanId() == null ? 0 : l2vlan.getVlanId().getValue();
            isVlanTransparent = l2vlan.getL2vlanMode() == IfL2vlan.L2vlanMode.Transparent;
        }
        int instructionKey = 0;
        BigInteger metadata = MetaDataUtil.getMetaDataForLPortDispatcher(lportTag, (short) 0, BigInteger.ZERO, isExternal(iface));
        BigInteger metadataMask = MetaDataUtil.getMetaDataMaskForLPortDispatcher(MetaDataUtil.METADATA_MASK_LPORT_TAG_SH_FLAG);
        List<Instruction> instructions = new ArrayList<>();

        final SplitHorizon splitHorizon = iface.getAugmentation(SplitHorizon.class);
        boolean overrideSplitHorizonProtection = (splitHorizon != null && splitHorizon.isOverrideSplitHorizonProtection());
        int actionKey = 0;
        List<Action> actions = new ArrayList<>();
        if (vlanId != 0 && !isVlanTransparent) {
            actions.add(MDSALUtil.createPopVlanAction(actionKey++));
        }
        if (overrideSplitHorizonProtection) {
            actions.add(MDSALUtil.createNxOfInPortAction(actionKey++,0));
        }
        if (actions.size() != 0) {
            instructions.add(MDSALUtil.buildApplyActionsInstruction(actions,instructionKey++));
        }

        instructions.add(MDSALUtil.buildAndGetWriteMetadaInstruction(metadata, metadataMask, instructionKey++));
        instructions.add(MDSALUtil.buildAndGetGotoTableInstruction(NwConstants.LPORT_DISPATCHER_TABLE, instructionKey++));
        int priority =  isVlanTransparent ? 1 : vlanId == 0 ? IfmConstants.FLOW_PRIORITY_FOR_UNTAGGED_VLAN : IfmConstants.FLOW_HIGH_PRIORITY;
        String flowRef = getFlowRef(IfmConstants.VLAN_INTERFACE_INGRESS_TABLE, dpId, iface.getName());
        Flow ingressFlow = MDSALUtil.buildFlowNew(IfmConstants.VLAN_INTERFACE_INGRESS_TABLE, flowRef, priority, flowRef, 0, 0,
                NwConstants.VLAN_TABLE_COOKIE, matches, instructions);
        installFlow(dpId, ingressFlow, inventoryConfigShardTransaction);
        futures.add(inventoryConfigShardTransaction.submit());
    }

    public static String getFlowRef(short tableId, BigInteger dpnId, String infName) {
        return String.format("%d:%s:%s", tableId, dpnId, infName);
    }

    public static void removeIngressFlow(String interfaceName, BigInteger dpId, DataBroker dataBroker,
                                         List<ListenableFuture<Void>> futures) {
        if(dpId == null){
            return;
        }
        LOG.debug("Removing Ingress Flows for {}", interfaceName);
        WriteTransaction t = dataBroker.newWriteOnlyTransaction();
        String flowKeyStr = getFlowRef(IfmConstants.VLAN_INTERFACE_INGRESS_TABLE, dpId, interfaceName);
        FlowKey flowKey = new FlowKey(new FlowId(flowKeyStr));
        Node nodeDpn = buildInventoryDpnNode(dpId);
        InstanceIdentifier<Flow> flowInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(NwConstants.VLAN_INTERFACE_INGRESS_TABLE)).child(Flow.class, flowKey).build();

        t.delete(LogicalDatastoreType.CONFIGURATION, flowInstanceId);
        futures.add(t.submit());
    }

    private static boolean isExternal(Interface iface) {
        if (iface == null) {
            return false;
        }
        IfExternal ifExternal = iface.getAugmentation(IfExternal.class);
        return ifExternal != null && Boolean.TRUE.equals(ifExternal.isExternal());
    }
}
