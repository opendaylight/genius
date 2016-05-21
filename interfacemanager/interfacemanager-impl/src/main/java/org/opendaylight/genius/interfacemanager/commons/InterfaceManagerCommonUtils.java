/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.interfacemanager.commons;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.globals.InterfaceInfo;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.mdsalutil.*;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeMplsOverGre;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class InterfaceManagerCommonUtils {
    private static final Logger LOG = LoggerFactory.getLogger(InterfaceManagerCommonUtils.class);
    public static NodeConnector getNodeConnectorFromInventoryOperDS(NodeConnectorId nodeConnectorId,
                                                                    DataBroker dataBroker) {
        NodeId nodeId = IfmUtil.getNodeIdFromNodeConnectorId(nodeConnectorId);
        InstanceIdentifier<NodeConnector> ncIdentifier = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(nodeId))
                .child(NodeConnector.class, new NodeConnectorKey(nodeConnectorId)).build();

        Optional<NodeConnector> nodeConnectorOptional = IfmUtil.read(LogicalDatastoreType.OPERATIONAL,
                ncIdentifier, dataBroker);
        if (!nodeConnectorOptional.isPresent()) {
            return null;
        }
        return nodeConnectorOptional.get();
    }

    public static InstanceIdentifier<Interface> getInterfaceIdentifier(InterfaceKey interfaceKey) {
        InstanceIdentifier.InstanceIdentifierBuilder<Interface> interfaceInstanceIdentifierBuilder =
                InstanceIdentifier.builder(Interfaces.class).child(Interface.class, interfaceKey);
        return interfaceInstanceIdentifierBuilder.build();
    }

    public static List<Interface> getAllTunnelInterfaces(DataBroker dataBroker, InterfaceInfo.InterfaceType interfaceType) {
        List<Interface> vxlanList = new ArrayList<Interface>();
        InstanceIdentifier<Interfaces> interfacesInstanceIdentifier =  InstanceIdentifier.builder(Interfaces.class).build();
        Optional<Interfaces> interfacesOptional  = IfmUtil.read(LogicalDatastoreType.CONFIGURATION, interfacesInstanceIdentifier, dataBroker);
        if (!interfacesOptional.isPresent()) {
            return vxlanList;
        }
        Interfaces interfaces = interfacesOptional.get();
        List<Interface> interfacesList = interfaces.getInterface();
        for (Interface iface : interfacesList) {
            if(IfmUtil.getInterfaceType(iface) == InterfaceInfo.InterfaceType.VXLAN_TRUNK_INTERFACE &&
                    iface.getAugmentation(IfTunnel.class).isInternal()) {
                vxlanList.add(iface);
            }
        }
        return vxlanList;
    }

    public static Interface getInterfaceFromConfigDS(String interfaceName, DataBroker dataBroker) {
        InterfaceKey interfaceKey = new InterfaceKey(interfaceName);
        return getInterfaceFromConfigDS(interfaceKey, dataBroker);
    }

    public static Interface getInterfaceFromConfigDS(InterfaceKey interfaceKey, DataBroker dataBroker) {
        InstanceIdentifier<Interface> interfaceId = getInterfaceIdentifier(interfaceKey);
        Optional<Interface> interfaceOptional = IfmUtil.read(LogicalDatastoreType.CONFIGURATION, interfaceId, dataBroker);
        if (!interfaceOptional.isPresent()) {
            return null;
        }

        return interfaceOptional.get();
    }

    public static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface getInterfaceStateFromOperDS(String interfaceName, DataBroker dataBroker) {
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> ifStateId =
                IfmUtil.buildStateInterfaceId(interfaceName);
        return getInterfaceStateFromOperDS(ifStateId, dataBroker);
    }

    public static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface getInterfaceStateFromOperDS
            (InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> ifStateId, DataBroker dataBroker) {
        Optional<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> ifStateOptional =
                IfmUtil.read(LogicalDatastoreType.OPERATIONAL, ifStateId, dataBroker);
        if (!ifStateOptional.isPresent()) {
            return null;
        }

        return ifStateOptional.get();
    }
    public static void makeTunnelIngressFlow(List<ListenableFuture<Void>> futures, IMdsalApiManager mdsalApiManager,
                                             IfTunnel tunnel, BigInteger dpnId, long portNo, Interface iface, int ifIndex, int addOrRemoveFlow) {
        LOG.debug("make tunnel ingress flow for {}",iface.getName());
        String flowRef = InterfaceManagerCommonUtils.getTunnelInterfaceFlowRef(dpnId, NwConstants.VLAN_INTERFACE_INGRESS_TABLE, iface.getName());
        List<MatchInfo> matches = new ArrayList<MatchInfo>();
        List<InstructionInfo> mkInstructions = new ArrayList<InstructionInfo>();
        if (NwConstants.ADD_FLOW == addOrRemoveFlow) {
            matches.add(new MatchInfo(MatchFieldType.in_port, new BigInteger[] {
                    dpnId, BigInteger.valueOf(portNo) }));
            mkInstructions.add(new InstructionInfo(
                    InstructionType.write_metadata, new BigInteger[] {
                    MetaDataUtil.getLportTagMetaData(ifIndex).or(BigInteger.ONE),
                    MetaDataUtil.METADATA_MASK_LPORT_TAG_SH_FLAG}));
            short tableId = (tunnel.getTunnelInterfaceType().isAssignableFrom(TunnelTypeMplsOverGre.class)) ? NwConstants.L3_LFIB_TABLE :
                    tunnel.isInternal() ? NwConstants.INTERNAL_TUNNEL_TABLE : NwConstants.DHCP_TABLE_EXTERNAL_TUNNEL;
            mkInstructions.add(new InstructionInfo(InstructionType.goto_table, new long[] {tableId}));
        }

        BigInteger COOKIE_VM_INGRESS_TABLE = new BigInteger("8000001", 16);
        FlowEntity flowEntity = MDSALUtil.buildFlowEntity(dpnId, NwConstants.VLAN_INTERFACE_INGRESS_TABLE, flowRef,
                IfmConstants.DEFAULT_FLOW_PRIORITY, iface.getName(), 0, 0, COOKIE_VM_INGRESS_TABLE, matches, mkInstructions);
        if (NwConstants.ADD_FLOW == addOrRemoveFlow) {
            futures.add(mdsalApiManager.installFlow(dpnId, flowEntity));
        } else {
            futures.add(mdsalApiManager.removeFlow(dpnId, flowEntity));
        }
    }
    public static String getTunnelInterfaceFlowRef(BigInteger dpnId, short tableId, String ifName) {
        return new StringBuilder().append(dpnId).append(tableId).append(ifName).toString();
    }

    public static void setOpStateForInterface(DataBroker broker, String interfaceName, org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus opStatus) {
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> interfaceId = IfmUtil.buildStateInterfaceId(interfaceName);
        InterfaceBuilder ifaceBuilder = new InterfaceBuilder().setKey(new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey(interfaceName));
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface interfaceData = ifaceBuilder.setOperStatus(opStatus).build();
        MDSALUtil.syncUpdate(broker, LogicalDatastoreType.OPERATIONAL, interfaceId, interfaceData);
    }

    public static void createInterfaceChildEntry( WriteTransaction t,
                                                  String parentInterface, String childInterface){
        InterfaceParentEntryKey interfaceParentEntryKey = new InterfaceParentEntryKey(parentInterface);
        InterfaceChildEntryKey interfaceChildEntryKey = new InterfaceChildEntryKey(childInterface);
        InstanceIdentifier<InterfaceChildEntry> intfId =
                InterfaceMetaUtils.getInterfaceChildEntryIdentifier(interfaceParentEntryKey, interfaceChildEntryKey);
        InterfaceChildEntryBuilder entryBuilder = new InterfaceChildEntryBuilder().setKey(interfaceChildEntryKey)
                .setChildInterface(childInterface);
        t.put(LogicalDatastoreType.CONFIGURATION, intfId, entryBuilder.build(),true);
    }

    public static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus
    updateStateEntry(Interface interfaceNew, DataBroker dataBroker, WriteTransaction transaction,
                     org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface ifState) {
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus operStatus;
        if (!interfaceNew.isEnabled()) {
            operStatus = org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus.Down;
        } else {
            String ncStr = ifState.getLowerLayerIf().get(0);
            NodeConnectorId nodeConnectorId = new NodeConnectorId(ncStr);
            NodeConnector nodeConnector =
                    InterfaceManagerCommonUtils.getNodeConnectorFromInventoryOperDS(nodeConnectorId, dataBroker);
            FlowCapableNodeConnector flowCapableNodeConnector =
                    nodeConnector.getAugmentation(FlowCapableNodeConnector.class);
            //State state = flowCapableNodeConnector.getState();
            operStatus = flowCapableNodeConnector == null ? org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus.Down : org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus.Up;
        }

        String ifName = interfaceNew.getName();
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> ifStateId =
                IfmUtil.buildStateInterfaceId(interfaceNew.getName());
        InterfaceBuilder ifaceBuilder = new InterfaceBuilder();
        ifaceBuilder.setOperStatus(operStatus);
        ifaceBuilder.setKey(IfmUtil.getStateInterfaceKeyFromName(ifName));
        transaction.merge(LogicalDatastoreType.OPERATIONAL, ifStateId, ifaceBuilder.build());
        return operStatus;
    }

    public static void updateOperStatus(String interfaceName, org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus operStatus,
                                        WriteTransaction transaction) {
        LOG.debug("updating operational status for interface {}",interfaceName);
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> ifChildStateId =
                IfmUtil.buildStateInterfaceId(interfaceName);
        InterfaceBuilder ifaceBuilderChild = new InterfaceBuilder();
        ifaceBuilderChild.setOperStatus(operStatus);
        ifaceBuilderChild.setKey(IfmUtil.getStateInterfaceKeyFromName(interfaceName));
        transaction.merge(LogicalDatastoreType.OPERATIONAL, ifChildStateId, ifaceBuilderChild.build());
    }

    public static void addStateEntry(String interfaceName, WriteTransaction transaction, DataBroker dataBroker, IdManagerService idManager,
                                     org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface ifState) {
        LOG.debug("adding interface state for {}",interfaceName);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus operStatus = ifState.getOperStatus();
        PhysAddress physAddress = ifState.getPhysAddress();
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.AdminStatus adminStatus = ifState.getAdminStatus();
        NodeConnectorId nodeConnectorId = new NodeConnectorId(ifState.getLowerLayerIf().get(0));
        InterfaceKey interfaceKey = new InterfaceKey(interfaceName);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface interfaceInfo =
                InterfaceManagerCommonUtils.getInterfaceFromConfigDS(interfaceKey, dataBroker);

        if (interfaceInfo != null && !interfaceInfo.isEnabled()) {
            operStatus = org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus.Down;
        }

        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> ifStateId =
                IfmUtil.buildStateInterfaceId(interfaceName);
        List<String> childLowerLayerIfList = new ArrayList<>();
        childLowerLayerIfList.add(0, nodeConnectorId.getValue());
        InterfaceBuilder ifaceBuilder = new InterfaceBuilder().setAdminStatus(adminStatus)
                .setOperStatus(operStatus).setPhysAddress(physAddress).setLowerLayerIf(childLowerLayerIfList);

        Integer ifIndex = IfmUtil.allocateId(idManager, IfmConstants.IFM_IDPOOL_NAME, interfaceName);
        ifaceBuilder.setIfIndex(ifIndex);

        if(interfaceInfo != null){
            ifaceBuilder.setType(interfaceInfo.getType());
        }
        ifaceBuilder.setKey(IfmUtil.getStateInterfaceKeyFromName(interfaceName));
        transaction.put(LogicalDatastoreType.OPERATIONAL, ifStateId, ifaceBuilder.build(), true);

        // create lportTag Interface Map
        InterfaceMetaUtils.createLportTagInterfaceMap(transaction, interfaceName, ifIndex);

        // install ingress flow
        BigInteger dpId = new BigInteger(IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId));
        long portNo = Long.valueOf(IfmUtil.getPortNoFromNodeConnectorId(nodeConnectorId));
        if(interfaceInfo != null && interfaceInfo.isEnabled() &&
                ifState.getOperStatus() == org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus.Up) {
            List<MatchInfo> matches = FlowBasedServicesUtils.getMatchInfoForVlanPortAtIngressTable(dpId, portNo, interfaceInfo);
            FlowBasedServicesUtils.installVlanFlow(dpId, portNo, interfaceInfo, transaction, matches, ifIndex);
        }
    }

    public static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface addStateEntry(Interface interfaceInfo, String portName, WriteTransaction transaction, IdManagerService idManager,
                                                                                                                                              PhysAddress physAddress, org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus operStatus,
                                                                                                                                              org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.AdminStatus adminStatus,
                                                                                                                                              NodeConnectorId nodeConnectorId) {
        LOG.debug("adding interface state for {}",portName);
        if (interfaceInfo != null && !interfaceInfo.isEnabled()) {
            operStatus = org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus.Down;
        }
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> ifStateId =
                IfmUtil.buildStateInterfaceId(portName);
        List<String> childLowerLayerIfList = new ArrayList<>();
        childLowerLayerIfList.add(0, nodeConnectorId.getValue());
        InterfaceBuilder ifaceBuilder = new InterfaceBuilder().setAdminStatus(adminStatus)
                .setOperStatus(operStatus).setPhysAddress(physAddress).setLowerLayerIf(childLowerLayerIfList);

        Integer ifIndex = IfmUtil.allocateId(idManager, IfmConstants.IFM_IDPOOL_NAME, portName);
        ifaceBuilder.setIfIndex(ifIndex);

        if(interfaceInfo != null){
            ifaceBuilder.setType(interfaceInfo.getType());
        }
        ifaceBuilder.setKey(IfmUtil.getStateInterfaceKeyFromName(portName));
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface ifState = ifaceBuilder.build();
        transaction.put(LogicalDatastoreType.OPERATIONAL, ifStateId, ifState , true);

        // allocate lport tag and set in if-index
        InterfaceMetaUtils.createLportTagInterfaceMap(transaction, portName, ifIndex);

        // install ingress flow if this is an l2vlan interface
        if(InterfaceManagerCommonUtils.isVlanInterface(interfaceInfo) && interfaceInfo.isEnabled() &&
                ifState.getOperStatus() == org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus.Up) {
            BigInteger dpId = new BigInteger(IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId));
            long portNo = Long.valueOf(IfmUtil.getPortNoFromNodeConnectorId(nodeConnectorId));
            List<MatchInfo> matches = FlowBasedServicesUtils.getMatchInfoForVlanPortAtIngressTable(dpId, portNo, interfaceInfo);
            FlowBasedServicesUtils.installVlanFlow(dpId, portNo, interfaceInfo, transaction, matches, ifIndex);
        }
        return ifState;
    }

    public static void deleteStateEntry(String interfaceName, WriteTransaction transaction) {
        LOG.debug("removing interface state entry for {}",interfaceName);
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> ifChildStateId =
                IfmUtil.buildStateInterfaceId(interfaceName);
        transaction.delete(LogicalDatastoreType.OPERATIONAL, ifChildStateId);
    }

    public static void deleteInterfaceStateInformation(String interfaceName, WriteTransaction transaction, IdManagerService idManagerService) {
        LOG.debug("removing interface state information for {}",interfaceName);
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> ifChildStateId =
                IfmUtil.buildStateInterfaceId(interfaceName);
        transaction.delete(LogicalDatastoreType.OPERATIONAL, ifChildStateId);
        InterfaceMetaUtils.removeLportTagInterfaceMap(idManagerService, transaction, interfaceName);
    }

    // For trunk interfaces, binding to a parent interface which is already bound to another trunk interface should not
    // be allowed
    public static boolean createInterfaceChildEntryIfNotPresent( DataBroker dataBroker, WriteTransaction t,
                                                                 String parentInterface, String childInterface){
        InterfaceParentEntryKey interfaceParentEntryKey = new InterfaceParentEntryKey(parentInterface);
        InstanceIdentifier<InterfaceParentEntry> interfaceParentEntryIdentifier =
                InterfaceMetaUtils.getInterfaceParentEntryIdentifier(interfaceParentEntryKey);
        InterfaceParentEntry interfaceParentEntry =
                InterfaceMetaUtils.getInterfaceParentEntryFromConfigDS(interfaceParentEntryIdentifier, dataBroker);

        if(interfaceParentEntry != null){
            LOG.error("Trying to bind the same parent interface {} to multiple trunk interfaces. ", parentInterface);
            return false;
        }

        LOG.info("First vlan trunk {} bound on parent-interface {}", childInterface, parentInterface);
        InterfaceChildEntryKey interfaceChildEntryKey = new InterfaceChildEntryKey(childInterface);
        InstanceIdentifier<InterfaceChildEntry> intfId =
                InterfaceMetaUtils.getInterfaceChildEntryIdentifier(interfaceParentEntryKey, interfaceChildEntryKey);
        InterfaceChildEntryBuilder entryBuilder = new InterfaceChildEntryBuilder().setKey(interfaceChildEntryKey)
                .setChildInterface(childInterface);
        t.put(LogicalDatastoreType.CONFIGURATION, intfId, entryBuilder.build(),true);
        return true;
    }

    public static boolean deleteParentInterfaceEntry( WriteTransaction t, String parentInterface){
        InterfaceParentEntryKey interfaceParentEntryKey = new InterfaceParentEntryKey(parentInterface);
        InstanceIdentifier<InterfaceParentEntry> interfaceParentEntryIdentifier = InterfaceMetaUtils.getInterfaceParentEntryIdentifier(interfaceParentEntryKey);
        t.delete(LogicalDatastoreType.CONFIGURATION, interfaceParentEntryIdentifier);
        return true;
    }

    /*
     * update operational state of interface based on events like tunnel monitoring
     */
    public static void updateOpState(WriteTransaction transaction, String interfaceName,
                                     org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus operStatus){
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> ifStateId =
                IfmUtil.buildStateInterfaceId(interfaceName);
        LOG.debug("updating tep interface state as {} for {}", operStatus.name(), interfaceName);
        InterfaceBuilder ifaceBuilder = new InterfaceBuilder().setOperStatus(operStatus);
        ifaceBuilder.setKey(IfmUtil.getStateInterfaceKeyFromName(interfaceName));
        transaction.merge(LogicalDatastoreType.OPERATIONAL, ifStateId, ifaceBuilder.build());
    }

    public static boolean isTunnelInterface(Interface interfaceInfo){
        if(interfaceInfo != null && interfaceInfo.getAugmentation(IfTunnel.class) != null){
            return true;
        }
        return false;
    }

    public static boolean isVlanInterface(Interface interfaceInfo){
        if(interfaceInfo != null && interfaceInfo.getAugmentation(IfL2vlan.class) != null){
            return true;
        }
        return false;
    }

}
