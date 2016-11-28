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
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.globals.InterfaceInfo;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.mdsalutil.FlowEntity;
import org.opendaylight.genius.mdsalutil.InstructionInfo;
import org.opendaylight.genius.mdsalutil.InstructionType;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.MatchFieldType;
import org.opendaylight.genius.mdsalutil.MatchInfo;
import org.opendaylight.genius.mdsalutil.MetaDataUtil;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.DpnToInterfaceList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.dpn.to._interface.list.DpnToInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.dpn.to._interface.list.DpnToInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.dpn.to._interface.list.dpn.to._interface.InterfaceNameEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.dpn.to._interface.list.dpn.to._interface.InterfaceNameEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.dpn.to._interface.list.dpn.to._interface.InterfaceNameEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeMplsOverGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InterfaceManagerCommonUtils {
    private static final Logger LOG = LoggerFactory.getLogger(InterfaceManagerCommonUtils.class);
    private static ConcurrentHashMap<String, Interface> interfaceConfigMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> interfaceStateMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus> bfdStateMap =
            new ConcurrentHashMap<>();

    private static final String NOVA_OR_TUNNEL_PORT_REGEX = "(tap|vhu)[0-9a-f]{8}-[0-9a-f]{2}|tun[0-9a-f]{11}";
    private static final Pattern pattern = Pattern.compile(NOVA_OR_TUNNEL_PORT_REGEX);

    public static NodeConnector getNodeConnectorFromInventoryOperDS(NodeConnectorId nodeConnectorId,
            DataBroker dataBroker) {
        NodeId nodeId = IfmUtil.getNodeIdFromNodeConnectorId(nodeConnectorId);
        InstanceIdentifier<NodeConnector> ncIdentifier = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(nodeId))
                .child(NodeConnector.class, new NodeConnectorKey(nodeConnectorId)).build();

        return IfmUtil.read(LogicalDatastoreType.OPERATIONAL, ncIdentifier, dataBroker).orNull();
    }

    public static boolean isNodePresent(DataBroker dataBroker, NodeConnectorId nodeConnectorId) {
        NodeId nodeID = IfmUtil.getNodeIdFromNodeConnectorId(nodeConnectorId);
        InstanceIdentifier<Node> nodeInstanceIdentifier = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(nodeID)).build();
        return IfmUtil.read(LogicalDatastoreType.OPERATIONAL, nodeInstanceIdentifier, dataBroker).isPresent();
    }

    public static InstanceIdentifier<Interface> getInterfaceIdentifier(InterfaceKey interfaceKey) {
        InstanceIdentifier.InstanceIdentifierBuilder<Interface> interfaceInstanceIdentifierBuilder = InstanceIdentifier
                .builder(Interfaces.class).child(Interface.class, interfaceKey);
        return interfaceInstanceIdentifierBuilder.build();
    }

    public static List<Interface> getAllTunnelInterfaces(DataBroker dataBroker,
            InterfaceInfo.InterfaceType interfaceType) {
        List<Interface> vxlanList = new ArrayList<>();
        InstanceIdentifier<Interfaces> interfacesInstanceIdentifier = InstanceIdentifier.builder(Interfaces.class)
                .build();
        Optional<Interfaces> interfacesOptional = IfmUtil.read(LogicalDatastoreType.CONFIGURATION,
                interfacesInstanceIdentifier, dataBroker);
        if (!interfacesOptional.isPresent()) {
            return vxlanList;
        }
        Interfaces interfaces = interfacesOptional.get();
        List<Interface> interfacesList = interfaces.getInterface();
        for (Interface iface : interfacesList) {
            if (IfmUtil.getInterfaceType(iface) == InterfaceInfo.InterfaceType.VXLAN_TRUNK_INTERFACE
                    && iface.getAugmentation(IfTunnel.class).isInternal()) {
                vxlanList.add(iface);
            }
        }
        return vxlanList;
    }

    /**
     * Searches for an interface by its name
     * @param interfaceName name of the interface to search for
     * @param dataBroker data tree store to start searching for the interface
     * @return the Interface object
     *
     */
    public static Interface getInterfaceFromConfigDS (String interfaceName, DataBroker dataBroker) {
        Interface iface = interfaceConfigMap.get(interfaceName);
        if (iface != null) {
            return iface;
        }
        InstanceIdentifier<Interface> interfaceId = getInterfaceIdentifier(new InterfaceKey(interfaceName));
        Optional<Interface> interfaceOptional = IfmUtil.read(LogicalDatastoreType.CONFIGURATION, interfaceId,
                dataBroker);
        if (interfaceOptional.isPresent()) {
            iface = interfaceOptional.get();
            interfaceConfigMap.put(iface.getName(), iface);
        }
        return iface;
    }

    @Deprecated
    public static Interface getInterfaceFromConfigDS (InterfaceKey interfaceKey, DataBroker dataBroker) {
        return getInterfaceFromConfigDS(interfaceKey.getName(), dataBroker);
    }

    public static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface getInterfaceStateFromOperDS(
            String interfaceName, DataBroker dataBroker) {
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface ifState = interfaceStateMap
                .get(interfaceName);
        if (ifState != null) {
            return ifState;
        }
        Optional<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> ifStateOptional = IfmUtil
                .read(LogicalDatastoreType.OPERATIONAL, IfmUtil.buildStateInterfaceId(interfaceName), dataBroker);
        if (ifStateOptional.isPresent()) {
            ifState = ifStateOptional.get();
            interfaceStateMap.put(ifState.getName(), ifState);
        }
        return ifState;
    }

    @Deprecated
    public static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface getInterfaceStateFromOperDS(
            InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> ifStateId,
            DataBroker dataBroker) {
        return IfmUtil.read(LogicalDatastoreType.OPERATIONAL, ifStateId, dataBroker).orNull();
    }

    public static void makeTunnelIngressFlow(List<ListenableFuture<Void>> futures, IMdsalApiManager mdsalApiManager,
            IfTunnel tunnel, BigInteger dpnId, long portNo, String interfaceName, int ifIndex, int addOrRemoveFlow) {
        LOG.debug("make tunnel ingress flow for {}", interfaceName);
        String flowRef = InterfaceManagerCommonUtils.getTunnelInterfaceFlowRef(dpnId,
                NwConstants.VLAN_INTERFACE_INGRESS_TABLE, interfaceName);
        List<MatchInfo> matches = new ArrayList<>();
        List<InstructionInfo> mkInstructions = new ArrayList<>();
        if (NwConstants.ADD_FLOW == addOrRemoveFlow) {
            matches.add(new MatchInfo(MatchFieldType.in_port, new BigInteger[] { dpnId, BigInteger.valueOf(portNo) }));
            mkInstructions.add(new InstructionInfo(InstructionType.write_metadata,
                    new BigInteger[] { MetaDataUtil.getLportTagMetaData(ifIndex).or(BigInteger.ONE),
                            MetaDataUtil.METADATA_MASK_LPORT_TAG_SH_FLAG }));
            short tableId = (tunnel.getTunnelInterfaceType().isAssignableFrom(TunnelTypeMplsOverGre.class))
                    ? NwConstants.L3_LFIB_TABLE
                    : tunnel.isInternal() ? NwConstants.INTERNAL_TUNNEL_TABLE : NwConstants.DHCP_TABLE_EXTERNAL_TUNNEL;
            mkInstructions.add(new InstructionInfo(InstructionType.goto_table, new long[] { tableId }));
        }

        FlowEntity flowEntity = MDSALUtil.buildFlowEntity(dpnId, NwConstants.VLAN_INTERFACE_INGRESS_TABLE, flowRef,
                IfmConstants.DEFAULT_FLOW_PRIORITY, interfaceName, 0, 0, NwConstants.COOKIE_VM_INGRESS_TABLE, matches,
                mkInstructions);
        if (NwConstants.ADD_FLOW == addOrRemoveFlow) {
            futures.add(mdsalApiManager.installFlow(dpnId, flowEntity));
        } else {
            futures.add(mdsalApiManager.removeFlow(dpnId, flowEntity));
        }
    }

    public static String getTunnelInterfaceFlowRef(BigInteger dpnId, short tableId, String ifName) {
        return String.valueOf(dpnId) + tableId + ifName;
    }

    public static void setOpStateForInterface(DataBroker broker, String interfaceName,
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus opStatus) {
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> interfaceId = IfmUtil
                .buildStateInterfaceId(interfaceName);
        InterfaceBuilder ifaceBuilder = new InterfaceBuilder()
                .setKey(new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey(
                        interfaceName));
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface interfaceData = ifaceBuilder
                .setOperStatus(opStatus).build();
        MDSALUtil.syncUpdate(broker, LogicalDatastoreType.OPERATIONAL, interfaceId, interfaceData);
    }

    public static void createInterfaceChildEntry(WriteTransaction t, String parentInterface, String childInterface) {
        InterfaceParentEntryKey interfaceParentEntryKey = new InterfaceParentEntryKey(parentInterface);
        InterfaceChildEntryKey interfaceChildEntryKey = new InterfaceChildEntryKey(childInterface);
        InstanceIdentifier<InterfaceChildEntry> intfId = InterfaceMetaUtils
                .getInterfaceChildEntryIdentifier(interfaceParentEntryKey, interfaceChildEntryKey);
        InterfaceChildEntryBuilder entryBuilder = new InterfaceChildEntryBuilder().setKey(interfaceChildEntryKey)
                .setChildInterface(childInterface);
        t.put(LogicalDatastoreType.CONFIGURATION, intfId, entryBuilder.build(), true);
    }

    public static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus updateStateEntry(
            Interface interfaceNew, DataBroker dataBroker, WriteTransaction transaction,
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface ifState) {
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus operStatus;
        if (!interfaceNew.isEnabled()) {
            operStatus = org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus.Down;
        } else {
            String ncStr = ifState.getLowerLayerIf().get(0);
            NodeConnectorId nodeConnectorId = new NodeConnectorId(ncStr);
            NodeConnector nodeConnector = InterfaceManagerCommonUtils
                    .getNodeConnectorFromInventoryOperDS(nodeConnectorId, dataBroker);
            FlowCapableNodeConnector flowCapableNodeConnector = nodeConnector
                    .getAugmentation(FlowCapableNodeConnector.class);
            // State state = flowCapableNodeConnector.getState();
            operStatus = flowCapableNodeConnector == null
                    ? org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus.Down
                    : org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus.Up;
        }

        String ifName = interfaceNew.getName();
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> ifStateId = IfmUtil
                .buildStateInterfaceId(interfaceNew.getName());
        InterfaceBuilder ifaceBuilder = new InterfaceBuilder();
        ifaceBuilder.setOperStatus(operStatus);
        ifaceBuilder.setKey(IfmUtil.getStateInterfaceKeyFromName(ifName));
        transaction.merge(LogicalDatastoreType.OPERATIONAL, ifStateId, ifaceBuilder.build());
        return operStatus;
    }

    public static void updateOperStatus(String interfaceName,
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus operStatus,
            WriteTransaction transaction) {
        LOG.debug("updating operational status for interface {}", interfaceName);
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> ifChildStateId = IfmUtil
                .buildStateInterfaceId(interfaceName);
        InterfaceBuilder ifaceBuilderChild = new InterfaceBuilder();
        ifaceBuilderChild.setOperStatus(operStatus);
        ifaceBuilderChild.setKey(IfmUtil.getStateInterfaceKeyFromName(interfaceName));
        transaction.merge(LogicalDatastoreType.OPERATIONAL, ifChildStateId, ifaceBuilderChild.build());
    }

    public static void addStateEntry(String interfaceName, DataBroker dataBroker,
                                     IdManagerService idManager, List<ListenableFuture<Void>> futures,
                                     org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface ifState) {
        WriteTransaction interfaceOperShardTransaction = dataBroker.newWriteOnlyTransaction();
        addStateEntry(interfaceName, dataBroker, interfaceOperShardTransaction, idManager, futures, ifState);
        futures.add(interfaceOperShardTransaction.submit());
    }

    public static void addStateEntry(String interfaceName, DataBroker dataBroker, WriteTransaction interfaceOperShardTransaction,
                                     IdManagerService idManager, List<ListenableFuture<Void>> futures,
                                     org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface ifState) {
        // allocate lport tag and create interface-if-index map.
        // This is done even if interface-state is not present, so that there is
        // no throttling
        // on id allocation even when multiple southbound port_up events come in
        // one shot
        Integer ifIndex = IfmUtil.allocateId(idManager, IfmConstants.IFM_IDPOOL_NAME, interfaceName);
        InterfaceMetaUtils.createLportTagInterfaceMap(interfaceOperShardTransaction, interfaceName, ifIndex);
        if (ifState == null) {
            LOG.debug("could not retrieve interface state corresponding to {}", interfaceName);
            return;
        }
        LOG.debug("adding interface state for {}", interfaceName);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus operStatus = ifState
                .getOperStatus();
        PhysAddress physAddress = ifState.getPhysAddress();
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.AdminStatus adminStatus = ifState
                .getAdminStatus();
        NodeConnectorId nodeConnectorId = new NodeConnectorId(ifState.getLowerLayerIf().get(0));
        InterfaceKey interfaceKey = new InterfaceKey(interfaceName);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface interfaceInfo = InterfaceManagerCommonUtils
                .getInterfaceFromConfigDS(interfaceKey, dataBroker);

        if (interfaceInfo != null && !interfaceInfo.isEnabled()) {
            operStatus = org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus.Down;
        }

        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> ifStateId = IfmUtil
                .buildStateInterfaceId(interfaceName);
        List<String> childLowerLayerIfList = new ArrayList<>();
        childLowerLayerIfList.add(0, nodeConnectorId.getValue());
        InterfaceBuilder ifaceBuilder = new InterfaceBuilder().setAdminStatus(adminStatus).setOperStatus(operStatus)
                .setPhysAddress(physAddress).setLowerLayerIf(childLowerLayerIfList);
        ifaceBuilder.setIfIndex(ifIndex);

        if (interfaceInfo != null) {
            ifaceBuilder.setType(interfaceInfo.getType());
        }
        ifaceBuilder.setKey(IfmUtil.getStateInterfaceKeyFromName(interfaceName));
        interfaceOperShardTransaction.put(LogicalDatastoreType.OPERATIONAL, ifStateId, ifaceBuilder.build(), true);

        // install ingress flow
        BigInteger dpId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
        long portNo = Long.valueOf(IfmUtil.getPortNoFromNodeConnectorId(nodeConnectorId));
        if (interfaceInfo != null && interfaceInfo.isEnabled() && ifState
                .getOperStatus() == org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus.Up) {
            FlowBasedServicesUtils.installLportIngressFlow(dpId, portNo, interfaceInfo, futures, dataBroker, ifIndex);
            FlowBasedServicesUtils.bindDefaultEgressDispatcherService(dataBroker, futures, interfaceInfo, Long.toString(portNo), interfaceName, ifIndex);
        }

        // Update the DpnToInterfaceList OpDS
        createOrUpdateDpnToInterface(dpId, interfaceName,interfaceOperShardTransaction);
    }

    public static boolean checkIfBfdStateIsDown(String interfaceName){
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus operStatus =
                InterfaceManagerCommonUtils.getBfdStateFromCache(interfaceName);
        return (operStatus == org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus.Down);
    }

    public static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface addStateEntry(
            Interface interfaceInfo, String interfaceName, WriteTransaction transaction, IdManagerService idManager,
            PhysAddress physAddress, OperStatus operStatus, AdminStatus adminStatus, NodeConnectorId nodeConnectorId) {
        LOG.debug("adding interface state for {}", interfaceName);
        InterfaceBuilder ifaceBuilder = new InterfaceBuilder();
        Integer ifIndex;
        if (interfaceInfo != null) {
            if(!interfaceInfo.isEnabled() || (InterfaceManagerCommonUtils.isTunnelInterface(interfaceInfo) &&
                    checkIfBfdStateIsDown(interfaceInfo.getName()))){
                operStatus = org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus.Down;
            }

            ifaceBuilder.setType(interfaceInfo.getType());
            // retrieve if-index only for northbound configured interfaces
            ifIndex = IfmUtil.allocateId(idManager, IfmConstants.IFM_IDPOOL_NAME, interfaceName);
            ifaceBuilder.setIfIndex(ifIndex);
            InterfaceMetaUtils.createLportTagInterfaceMap(transaction, interfaceName, ifIndex);
        }
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> ifStateId = IfmUtil
                .buildStateInterfaceId(interfaceName);
        List<String> childLowerLayerIfList = new ArrayList<>();
        childLowerLayerIfList.add(0, nodeConnectorId.getValue());
        ifaceBuilder.setAdminStatus(adminStatus).setOperStatus(operStatus).setPhysAddress(physAddress)
                .setLowerLayerIf(childLowerLayerIfList);
        ifaceBuilder.setKey(IfmUtil.getStateInterfaceKeyFromName(interfaceName));
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface ifState = ifaceBuilder
                .build();
        transaction.put(LogicalDatastoreType.OPERATIONAL, ifStateId, ifState, true);

        BigInteger dpId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
        // Update the DpnToInterfaceList OpDS
        createOrUpdateDpnToInterface(dpId, interfaceName, transaction);
        return ifState;
    }

    public static void deleteStateEntry(String interfaceName, WriteTransaction transaction) {
        LOG.debug("removing interface state entry for {}", interfaceName);
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> ifChildStateId = IfmUtil
                .buildStateInterfaceId(interfaceName);
        transaction.delete(LogicalDatastoreType.OPERATIONAL, ifChildStateId);
    }

    public static void deleteInterfaceStateInformation(String interfaceName, WriteTransaction transaction,
            IdManagerService idManagerService) {
        LOG.debug("removing interface state information for {}", interfaceName);
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> ifStateId = IfmUtil
                .buildStateInterfaceId(interfaceName);
        transaction.delete(LogicalDatastoreType.OPERATIONAL, ifStateId);
        InterfaceMetaUtils.removeLportTagInterfaceMap(idManagerService, transaction, interfaceName);
    }

    // For trunk interfaces, binding to a parent interface which is already
    // bound to another trunk interface should not
    // be allowed
    public static boolean createInterfaceChildEntryIfNotPresent(DataBroker dataBroker, WriteTransaction t,
            String parentInterface, String childInterface) {
        InterfaceParentEntryKey interfaceParentEntryKey = new InterfaceParentEntryKey(parentInterface);
        InstanceIdentifier<InterfaceParentEntry> interfaceParentEntryIdentifier = InterfaceMetaUtils
                .getInterfaceParentEntryIdentifier(interfaceParentEntryKey);
        InterfaceParentEntry interfaceParentEntry = InterfaceMetaUtils
                .getInterfaceParentEntryFromConfigDS(interfaceParentEntryIdentifier, dataBroker);

        if (interfaceParentEntry != null) {
            if (!Objects.equals(parentInterface, interfaceParentEntry.getParentInterface())) {
                LOG.error("Trying to bind the same parent interface {} to multiple trunk interfaces. ", parentInterface);
            } else {
                LOG.trace("Child entry for interface {} already exists", childInterface);
            }
            return false;
        }

        LOG.info("First vlan trunk {} bound on parent-interface {}", childInterface, parentInterface);
        InterfaceChildEntryKey interfaceChildEntryKey = new InterfaceChildEntryKey(childInterface);
        InstanceIdentifier<InterfaceChildEntry> intfId = InterfaceMetaUtils
                .getInterfaceChildEntryIdentifier(interfaceParentEntryKey, interfaceChildEntryKey);
        InterfaceChildEntryBuilder entryBuilder = new InterfaceChildEntryBuilder().setKey(interfaceChildEntryKey)
                .setChildInterface(childInterface);
        t.put(LogicalDatastoreType.CONFIGURATION, intfId, entryBuilder.build(), true);
        return true;
    }

    public static boolean deleteParentInterfaceEntry(WriteTransaction t, String parentInterface) {
        InterfaceParentEntryKey interfaceParentEntryKey = new InterfaceParentEntryKey(parentInterface);
        InstanceIdentifier<InterfaceParentEntry> interfaceParentEntryIdentifier = InterfaceMetaUtils
                .getInterfaceParentEntryIdentifier(interfaceParentEntryKey);
        t.delete(LogicalDatastoreType.CONFIGURATION, interfaceParentEntryIdentifier);
        return true;
    }

    /*
     * update operational state of interface based on events like tunnel
     * monitoring
     */
    public static void updateOpState(WriteTransaction transaction, String interfaceName,
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus operStatus) {
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> ifStateId = IfmUtil
                .buildStateInterfaceId(interfaceName);
        LOG.debug("updating tep interface state as {} for {}", operStatus.name(), interfaceName);
        InterfaceBuilder ifaceBuilder = new InterfaceBuilder().setOperStatus(operStatus);
        ifaceBuilder.setKey(IfmUtil.getStateInterfaceKeyFromName(interfaceName));
        transaction.merge(LogicalDatastoreType.OPERATIONAL, ifStateId, ifaceBuilder.build(), false);
    }

    public static boolean isTunnelInterface(Interface interfaceInfo) {
        return interfaceInfo != null && interfaceInfo.getAugmentation(IfTunnel.class) != null;
    }

    public static boolean isVlanInterface(Interface interfaceInfo) {
        return interfaceInfo != null && interfaceInfo.getAugmentation(IfL2vlan.class) != null;
    }

    // Cache Util methods
    public static void addInterfaceToCache(Interface iface) {
        interfaceConfigMap.put(iface.getName(), iface);
    }

    public static void removeFromInterfaceCache(Interface iface) {
        interfaceConfigMap.remove(iface.getName());
    }

    public static void addInterfaceStateToCache(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface iface) {
        interfaceStateMap.put(iface.getName(), iface);
    }

    public static void removeFromInterfaceStateCache(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface iface) {
        interfaceStateMap.remove(iface.getName());
    }

    public static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus getBfdStateFromCache(String interfaceName) {
        return bfdStateMap.get(interfaceName);
    }

    public static void addBfdStateToCache(String interfaceName,
                                          org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus operStatus) {
        bfdStateMap.put(interfaceName, operStatus);
    }

    public static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus removeBfdStateFromCache(String interfaceName){
        return bfdStateMap.remove(interfaceName);
    }

    public static boolean isNovaOrTunnelPort(String portName) {

        Matcher matcher = pattern.matcher(portName);
        return matcher.matches();
    }


    public static void createOrUpdateDpnToInterface(BigInteger dpId, String infName, WriteTransaction transaction) {
        DpnToInterfaceKey dpnToInterfaceKey = new DpnToInterfaceKey(dpId);
        InterfaceNameEntryKey interfaceNameEntryKey = new InterfaceNameEntryKey(infName);
        InstanceIdentifier<InterfaceNameEntry> intfid = InstanceIdentifier.builder(DpnToInterfaceList.class)
                                                        .child(DpnToInterface.class, dpnToInterfaceKey)
                                                        .child(InterfaceNameEntry.class, interfaceNameEntryKey)
                                                        .build();
        InterfaceNameEntryBuilder entryBuilder = new InterfaceNameEntryBuilder().setKey(interfaceNameEntryKey).setInterfaceName(infName);
        transaction.put(LogicalDatastoreType.OPERATIONAL, intfid, entryBuilder.build(), true);
    }

    public static void deleteDpnToInterface(DataBroker dataBroker, BigInteger dpId, String infName, WriteTransaction transaction) {
        DpnToInterfaceKey dpnToInterfaceKey = new DpnToInterfaceKey(dpId);
        InstanceIdentifier<DpnToInterface> dpnToInterfaceId = InstanceIdentifier.builder(DpnToInterfaceList.class)
                .child(DpnToInterface.class, dpnToInterfaceKey).build();
        Optional<DpnToInterface> dpnToInterfaceOptional = IfmUtil.read(LogicalDatastoreType.OPERATIONAL, dpnToInterfaceId, dataBroker);
        if (!dpnToInterfaceOptional.isPresent()) {
            LOG.debug("DPN {} is already removed from the Operational DS", dpId);
            return;
        }

        List<InterfaceNameEntry> interfaceNameEntries = dpnToInterfaceOptional.get().getInterfaceNameEntry();
        InterfaceNameEntryKey interfaceNameEntryKey = new InterfaceNameEntryKey(infName);
        InstanceIdentifier<InterfaceNameEntry> intfid = InstanceIdentifier.builder(DpnToInterfaceList.class)
                .child(DpnToInterface.class, dpnToInterfaceKey)
                .child(InterfaceNameEntry.class, interfaceNameEntryKey)
                .build();
        transaction.delete(LogicalDatastoreType.OPERATIONAL, intfid);

        if (interfaceNameEntries.size() <=1) {
            transaction.delete(LogicalDatastoreType.OPERATIONAL, dpnToInterfaceId);
        }
    }

}
