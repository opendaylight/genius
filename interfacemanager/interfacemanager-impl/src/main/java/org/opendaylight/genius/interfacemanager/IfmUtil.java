/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableBiMap;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.globals.InterfaceInfo;
import org.opendaylight.genius.interfacemanager.globals.VlanInterfaceInfo;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.ActionType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.WriteMetadataCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.write.metadata._case.WriteMetadata;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPool;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPoolKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan.L2vlanMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class IfmUtil {
    private static final Logger LOG = LoggerFactory.getLogger(IfmUtil.class);
    private static final int INVALID_ID = 0;

    public static final ImmutableBiMap<Class<? extends TunnelTypeBase>, InterfaceInfo.InterfaceType> TUNNEL_TYPE_MAP =
            new ImmutableBiMap.Builder<Class<? extends TunnelTypeBase>, InterfaceInfo.InterfaceType>()
                    .put(TunnelTypeGre.class, InterfaceInfo.InterfaceType.GRE_TRUNK_INTERFACE)
                    .put(TunnelTypeMplsOverGre.class, InterfaceInfo.InterfaceType.MPLS_OVER_GRE)
                    .put(TunnelTypeVxlan.class, InterfaceInfo.InterfaceType.VXLAN_TRUNK_INTERFACE)
                    .build();


    public static String getDpnFromNodeConnectorId(NodeConnectorId portId) {
        /*
         * NodeConnectorId is of form 'openflow:dpnid:portnum'
         */
        String[] split = portId.getValue().split(IfmConstants.OF_URI_SEPARATOR);
        return split[1];
    }

    public static BigInteger getDpnFromInterface(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface ifState){
        NodeConnectorId ncId = getNodeConnectorIdFromInterface(ifState);
        if(ncId != null){
            return new BigInteger(getDpnFromNodeConnectorId(ncId));
        }
        return null;
    }
    public static String getPortNoFromInterfaceName(String ifaceName, DataBroker broker) {
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface ifState =
                InterfaceManagerCommonUtils.getInterfaceStateFromOperDS(ifaceName, broker);

        if(ifState == null){
            throw new NullPointerException("Interface information not present in oper DS for " +ifaceName);
        }
        String lowerLayerIf = ifState.getLowerLayerIf().get(0);
        NodeConnectorId nodeConnectorId = new NodeConnectorId(lowerLayerIf);
        String portNo = IfmUtil.getPortNoFromNodeConnectorId(nodeConnectorId);

        return portNo;
    }

    public static String getPortNoFromNodeConnectorId(NodeConnectorId portId) {
        /*
         * NodeConnectorId is of form 'openflow:dpnid:portnum'
         */
        String[] split = portId.getValue().split(IfmConstants.OF_URI_SEPARATOR);
        return split[2];
    }

    public static NodeId buildDpnNodeId(BigInteger dpnId) {
        return new NodeId(IfmConstants.OF_URI_PREFIX + dpnId);
    }

    public static InstanceIdentifier<Interface> buildId(String interfaceName) {
        //TODO Make this generic and move to AbstractDataChangeListener or Utils.
        InstanceIdentifierBuilder<Interface> idBuilder =
                InstanceIdentifier.builder(Interfaces.class).child(Interface.class, new InterfaceKey(interfaceName));
        InstanceIdentifier<Interface> id = idBuilder.build();
        return id;
    }

    public static InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> buildStateInterfaceId(String interfaceName) {
        InstanceIdentifierBuilder<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> idBuilder =
                InstanceIdentifier.builder(InterfacesState.class)
                        .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.class,
                                new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey(interfaceName));
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> id = idBuilder.build();
        return id;
    }

    public static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey getStateInterfaceKeyFromName(
            String name) {
        return new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey(name);
    }

    public static InstanceIdentifier<IdPool> getPoolId(String poolName){
        InstanceIdentifier.InstanceIdentifierBuilder<IdPool> idBuilder =
                InstanceIdentifier.builder(IdPools.class).child(IdPool.class, new IdPoolKey(poolName));
        InstanceIdentifier<IdPool> id = idBuilder.build();
        return id;
    }

    public static List<String> getPortNameAndSuffixFromInterfaceName(String intfName) {
        List<String> strList = new ArrayList<>(2);
        int index = intfName.indexOf(":");
        if (index != -1) {
            strList.add(0, intfName.substring(0, index));
            strList.add(1, intfName.substring(index));
        }
        return strList;
    }

    public static long getGroupId(long ifIndex, InterfaceInfo.InterfaceType infType) {
        if (infType == InterfaceInfo.InterfaceType.LOGICAL_GROUP_INTERFACE) {
            return ifIndex + IfmConstants.LOGICAL_GROUP_START;
        }
        else if (infType == InterfaceInfo.InterfaceType.VLAN_INTERFACE) {
            return ifIndex + IfmConstants.VLAN_GROUP_START;
        } else {
            return ifIndex + IfmConstants.TRUNK_GROUP_START;
        }
    }

    public static List<String> getDpIdPortNameAndSuffixFromInterfaceName(String intfName) {
        List<String> strList = new ArrayList<>(3);
        int index1 = intfName.indexOf(":");
        if (index1 != -1) {
            int index2 = intfName.indexOf(":", index1 + 1 );
            strList.add(0, intfName.substring(0, index1));
            if (index2 != -1) {
                strList.add(1, intfName.substring(index1, index2));
                strList.add(2, intfName.substring(index2));
            } else {
                strList.add(1, intfName.substring(index1));
                strList.add(2, "");
            }
        }
        return strList;
    }

    public static <T extends DataObject> Optional<T> read(LogicalDatastoreType datastoreType,
                                                          InstanceIdentifier<T> path, DataBroker broker) {

        ReadOnlyTransaction tx = broker.newReadOnlyTransaction();

        Optional<T> result = Optional.absent();
        try {
            result = tx.read(datastoreType, path).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    public static List<Action> getEgressActionsForInterface(String interfaceName, Long tunnelKey, DataBroker dataBroker) {
        List<ActionInfo> listActionInfo = getEgressActionInfosForInterface(interfaceName, tunnelKey, 0, dataBroker);
        List<Action> actionsList = new ArrayList<>();
        for (ActionInfo actionInfo : listActionInfo) {
            actionsList.add(actionInfo.buildAction());
        }
        return actionsList;
    }

    public static List<ActionInfo> getEgressActionInfosForInterface(String     interfaceName,
                                                                    int        actionKeyStart,
                                                                    DataBroker dataBroker) {
        return getEgressActionInfosForInterface(interfaceName, null, actionKeyStart, dataBroker);
    }

    /**
     * Returns a list of Actions to be taken when sending a packet over an interface
     *
     * @param interfaceName
     * @param tunnelKey Optional.
     * @param actionKeyStart
     * @param dataBroker
     * @return
     */
    public static List<ActionInfo> getEgressActionInfosForInterface(String     interfaceName,
                                                                    Long       tunnelKey,
                                                                    int        actionKeyStart,
                                                                    DataBroker dataBroker) {
        List<ActionInfo> result = new ArrayList<ActionInfo>();
        Interface interfaceInfo = InterfaceManagerCommonUtils.getInterfaceFromConfigDS(new InterfaceKey(interfaceName),
                dataBroker);
        if(interfaceInfo == null){
            throw new NullPointerException("Interface information not present in config DS for " +interfaceName);
        }
        String portNo = IfmUtil.getPortNoFromInterfaceName(interfaceName, dataBroker);

        InterfaceInfo.InterfaceType ifaceType = getInterfaceType(interfaceInfo);

        switch (ifaceType ) {
            case VLAN_INTERFACE:
                IfL2vlan vlanIface = interfaceInfo.getAugmentation(IfL2vlan.class);
                LOG.trace("L2Vlan: {}",vlanIface);
                boolean isVlanTransparent = false;
                long vlanVid = 0;
                if (vlanIface != null) {
                    vlanVid = vlanIface.getVlanId() == null ? 0 : vlanIface.getVlanId().getValue();
                    isVlanTransparent = vlanIface.getL2vlanMode() == IfL2vlan.L2vlanMode.Transparent;
                }
                if (vlanVid != 0 && !isVlanTransparent) {
                    result.add(new ActionInfo(ActionType.push_vlan, new String[] {}, actionKeyStart));
                    actionKeyStart++;
                    result.add(new ActionInfo(ActionType.set_field_vlan_vid,
                            new String[] { Long.toString(vlanVid) }, actionKeyStart));
                    actionKeyStart++;
                }
                result.add(new ActionInfo(ActionType.output, new String[] {portNo}, actionKeyStart));
                actionKeyStart++;
                break;
            case MPLS_OVER_GRE:
            case VXLAN_TRUNK_INTERFACE:
            case GRE_TRUNK_INTERFACE:
                if(tunnelKey != null) {
                    result.add(new ActionInfo(ActionType.set_field_tunnel_id,
                            new BigInteger[] { BigInteger.valueOf(tunnelKey.longValue()) },
                            actionKeyStart) );
                    actionKeyStart++;
                }

                result.add(new ActionInfo(ActionType.output, new String[] { portNo}, actionKeyStart));
                actionKeyStart++;
                break;

            default:
                LOG.warn("Interface Type {} not handled yet", ifaceType);
                break;
        }

        return result;
    }


    public static NodeId getNodeIdFromNodeConnectorId(NodeConnectorId ncId) {
        return new NodeId(ncId.getValue().substring(0,ncId.getValue().lastIndexOf(":")));
    }

    public static BigInteger[] mergeOpenflowMetadataWriteInstructions(List<Instruction> instructions) {
        BigInteger metadata = new BigInteger("0", 16);
        BigInteger metadataMask = new BigInteger("0", 16);
        if (instructions != null && !instructions.isEmpty()) {
            // check if metadata write instruction is present
            for (Instruction instruction : instructions) {
                org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.Instruction actualInstruction = instruction.getInstruction();
                if (actualInstruction instanceof WriteMetadataCase) {
                    WriteMetadataCase writeMetaDataInstruction = (WriteMetadataCase) actualInstruction ;
                    WriteMetadata availableMetaData = writeMetaDataInstruction.getWriteMetadata();
                    metadata = metadata.or(availableMetaData.getMetadata());
                    metadataMask = metadataMask.or(availableMetaData.getMetadataMask());
                }
            }
        }
        return new BigInteger[] { metadata, metadataMask };
    }

    public static Integer allocateId(IdManagerService idManager, String poolName, String idKey) {
        AllocateIdInput getIdInput = new AllocateIdInputBuilder()
                .setPoolName(poolName)
                .setIdKey(idKey).build();
        try {
            Future<RpcResult<AllocateIdOutput>> result = idManager.allocateId(getIdInput);
            RpcResult<AllocateIdOutput> rpcResult = result.get();
            if(rpcResult.isSuccessful()) {
                return rpcResult.getResult().getIdValue().intValue();
            } else {
                LOG.warn("RPC Call to Get Unique Id returned with Errors {}", rpcResult.getErrors());
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Exception when getting Unique Id",e);
        }
        return INVALID_ID;
    }

    public static void releaseId(IdManagerService idManager, String poolName, String idKey) {
        ReleaseIdInput idInput = new ReleaseIdInputBuilder()
                .setPoolName(poolName)
                .setIdKey(idKey).build();
        try {
            Future<RpcResult<Void>> result = idManager.releaseId(idInput);
            RpcResult<Void> rpcResult = result.get();
            if(!rpcResult.isSuccessful()) {
                LOG.warn("RPC Call to release Id {} with Key {} returned with Errors {}",
                        idKey, rpcResult.getErrors());
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Exception when releasing Id for key {}", idKey, e);
        }
    }

    public static BigInteger getDpnId(DatapathId datapathId){
        if (datapathId != null) {
            // Adding logs for a random issue spotted during datapath id conversion
            String dpIdStr = datapathId.getValue().replace(":", "");
            BigInteger dpnId =  new BigInteger(dpIdStr, 16);
            return dpnId;
        }
        return null;
    }

    public static NodeConnectorId getNodeConnectorIdFromInterface(String interfaceName, DataBroker dataBroker) {
        return FlowBasedServicesUtils.getNodeConnectorIdFromInterface(interfaceName, dataBroker);
    }

    public static String getPortName(DataBroker dataBroker, NodeConnectorId ncId){
        InstanceIdentifier<NodeConnector> ncIdentifier =
                InstanceIdentifier.builder(Nodes.class)
                        .child(Node.class, new NodeKey(getNodeIdFromNodeConnectorId(ncId)))
                        .child(NodeConnector.class, new NodeConnectorKey(ncId)).build();
        Optional<NodeConnector> optNc = read(LogicalDatastoreType.OPERATIONAL, ncIdentifier, dataBroker);
        if(optNc.isPresent()) {
            NodeConnector nc = optNc.get();
            FlowCapableNodeConnector fcnc = nc.getAugmentation(FlowCapableNodeConnector.class);
            return fcnc.getName();
        }
        return null;
    }

    public static NodeConnectorId getNodeConnectorIdFromInterface(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface ifState){
        if(ifState != null) {
            List<String> ofportIds = ifState.getLowerLayerIf();
            return new NodeConnectorId(ofportIds.get(0));
        }
        return null;
    }

    public static boolean isTunnelType(Interface iface,org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface ifState) {
        boolean isTunnelInterface = InterfaceManagerCommonUtils.isTunnelInterface(iface);
        if (!isTunnelInterface && ifState != null) {
            isTunnelInterface = IfmUtil.isTunnelType(ifState.getType());
        }
        return isTunnelInterface;
    }

    public static boolean isTunnelType(Class<? extends InterfaceType> ifType) {
        if( (ifType != null) && (ifType.isAssignableFrom(Tunnel.class)) ) {
            return true;
        }
        return false;
    }

    public static InterfaceInfo.InterfaceType getInterfaceType(Interface iface) {
        InterfaceInfo.InterfaceType interfaceType =
                org.opendaylight.genius.interfacemanager.globals.InterfaceInfo.InterfaceType.UNKNOWN_INTERFACE;
        Class<? extends org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType> ifType = iface.getType();

        if (ifType.isAssignableFrom(L2vlan.class)) {
            interfaceType =  org.opendaylight.genius.interfacemanager.globals.InterfaceInfo.InterfaceType.VLAN_INTERFACE;
        } else if (ifType.isAssignableFrom(Tunnel.class)) {
            IfTunnel ifTunnel = iface.getAugmentation(IfTunnel.class);
            Class<? extends  org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase> tunnelType = ifTunnel.getTunnelInterfaceType();
            interfaceType = TUNNEL_TYPE_MAP.get(tunnelType);
            /*if (tunnelType.isAssignableFrom(TunnelTypeVxlan.class)) {
                interfaceType = InterfaceInfo.InterfaceType.VXLAN_TRUNK_INTERFACE;
            } else if (tunnelType.isAssignableFrom(TunnelTypeGre.class)) {
                interfaceType = InterfaceInfo.InterfaceType.GRE_TRUNK_INTERFACE;
            } else if(tunnelType.isAssignableFrom(TunnelTypeMplsOverGre.class)){
                interfaceType = InterfaceInfo.InterfaceType.MPLS_OVER_GRE;
            } else if(tunnelType.isAssignableFrom(TunnelTypeMplsOverUdp.class)){
                interfaceType = InterfaceInfo.InterfaceType.MPLS_OVER_UDP;
            }*/
        }
        // TODO: Check if the below condition is still needed/valid
        //else if (ifType.isAssignableFrom(InterfaceGroup.class)) {
        //    interfaceType =  org.opendaylight.vpnservice.interfacemgr.globals.InterfaceInfo.InterfaceType.LOGICAL_GROUP_INTERFACE;
        //}
        return interfaceType;
    }

    public static VlanInterfaceInfo getVlanInterfaceInfo(String interfaceName, Interface iface, BigInteger dpId) {

        short vlanId = 0;
        String portName = null;
        IfL2vlan vlanIface = iface.getAugmentation(IfL2vlan.class);
        ParentRefs parentRefs = iface.getAugmentation(ParentRefs.class);
        if (parentRefs != null && parentRefs.getParentInterface() != null) {
            portName = parentRefs.getParentInterface();
        }else {
            LOG.warn("Portname set to null since parentRef is Null");
        }
        VlanInterfaceInfo vlanInterfaceInfo = new VlanInterfaceInfo(dpId, portName, vlanId);

        if (vlanIface != null) {
            vlanId = vlanIface.getVlanId() == null ? 0 : vlanIface.getVlanId().getValue().shortValue();
            L2vlanMode l2VlanMode = vlanIface.getL2vlanMode();

            if (l2VlanMode == L2vlanMode.Transparent) {
                vlanInterfaceInfo.setVlanTransparent(true);
            }
            if (l2VlanMode == L2vlanMode.NativeUntagged) {
                vlanInterfaceInfo.setUntaggedVlan(true);
            }
            vlanInterfaceInfo.setVlanId(vlanId);

        }
        return vlanInterfaceInfo;
    }
}
