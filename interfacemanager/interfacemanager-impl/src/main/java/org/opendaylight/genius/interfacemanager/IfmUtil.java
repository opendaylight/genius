/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager;

import static org.opendaylight.genius.infra.Datastore.CONFIGURATION;
import static org.opendaylight.genius.interfacemanager.globals.InterfaceInfo.InterfaceType.GRE_TRUNK_INTERFACE;
import static org.opendaylight.genius.interfacemanager.globals.InterfaceInfo.InterfaceType.LOGICAL_GROUP_INTERFACE;
import static org.opendaylight.genius.interfacemanager.globals.InterfaceInfo.InterfaceType.MPLS_OVER_GRE;
import static org.opendaylight.genius.interfacemanager.globals.InterfaceInfo.InterfaceType.VLAN_INTERFACE;
import static org.opendaylight.genius.interfacemanager.globals.InterfaceInfo.InterfaceType.VXLAN_TRUNK_INTERFACE;
import static org.opendaylight.mdsal.binding.api.WriteTransaction.CREATE_MISSING_PARENTS;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListenableFuture;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import org.apache.commons.lang3.BooleanUtils;
import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.genius.infra.Datastore.Configuration;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.TypedWriteTransaction;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.globals.InterfaceInfo;
import org.opendaylight.genius.interfacemanager.globals.VlanInterfaceInfo;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.MetaDataUtil;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.mdsalutil.actions.ActionGroup;
import org.opendaylight.genius.mdsalutil.actions.ActionNxResubmit;
import org.opendaylight.genius.mdsalutil.actions.ActionOutput;
import org.opendaylight.genius.mdsalutil.actions.ActionPushVlan;
import org.opendaylight.genius.mdsalutil.actions.ActionRegLoad;
import org.opendaylight.genius.mdsalutil.actions.ActionSetFieldTunnelId;
import org.opendaylight.genius.mdsalutil.actions.ActionSetFieldVlanVid;
import org.opendaylight.genius.mdsalutil.actions.ActionSetTunnelDestinationIp;
import org.opendaylight.genius.mdsalutil.actions.ActionSetTunnelSourceIp;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev170119.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev170119.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.WriteMetadataCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.write.metadata._case.WriteMetadata;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdPools;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.ReleaseIdInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.ReleaseIdInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.ReleaseIdOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPool;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPoolKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan.L2vlanMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeLogicalGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeMplsOverGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlanGpe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceBindings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfoKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServicesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathId;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class IfmUtil {
    private static final Logger LOG = LoggerFactory.getLogger(IfmUtil.class);
    private static final Pattern GENERATE_MAC_PATTERN = Pattern.compile("(.{2})");
    private static final int INVALID_ID = 0;

    private IfmUtil() {
        throw new IllegalStateException("Utility class");
    }

    private static final ImmutableMap<Class<? extends TunnelTypeBase>, InterfaceInfo.InterfaceType>
        TUNNEL_TYPE_MAP = new ImmutableMap.Builder<Class<? extends TunnelTypeBase>, InterfaceInfo.InterfaceType>()
            .put(TunnelTypeGre.class, GRE_TRUNK_INTERFACE).put(TunnelTypeMplsOverGre.class, MPLS_OVER_GRE)
            .put(TunnelTypeVxlan.class, VXLAN_TRUNK_INTERFACE).put(TunnelTypeVxlanGpe.class, VXLAN_TRUNK_INTERFACE)
            .put(TunnelTypeLogicalGroup.class, LOGICAL_GROUP_INTERFACE)
            .build();

    public static Uint64 getDpnFromNodeConnectorId(NodeConnectorId portId) {
        return Uint64.valueOf(getDpnStringFromNodeConnectorId(portId));
    }

    public static String getDpnStringFromNodeConnectorId(NodeConnectorId portId) {
        /*
         * NodeConnectorId is of form 'openflow:dpnid:portnum'
         */
        return portId.getValue().split(IfmConstants.OF_URI_SEPARATOR)[1];
    }

    public static Uint64 getDpnFromInterface(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.interfaces.rev140508.interfaces.state.Interface ifState) {
        NodeConnectorId ncId = getNodeConnectorIdFromInterface(ifState);
        if (ncId != null) {
            return getDpnFromNodeConnectorId(ncId);
        }
        return null;
    }

    public static String getPortNoFromNodeConnectorId(NodeConnectorId portId) {
        /*
         * NodeConnectorId is of form 'openflow:dpnid:portnum'
         */
        String[] split = portId.getValue().split(IfmConstants.OF_URI_SEPARATOR);
        return split[2];
    }

    public static Long getPortNumberFromNodeConnectorId(NodeConnectorId portId) {
        String portNo = getPortNoFromNodeConnectorId(portId);
        try {
            return Long.valueOf(portNo);
        } catch (NumberFormatException ex) {
            LOG.trace("Unable to retrieve port number from nodeconnector id for {}", portId);
        }
        return IfmConstants.INVALID_PORT_NO;
    }

    public static NodeId buildDpnNodeId(Uint64 dpnId) {
        return new NodeId(IfmConstants.OF_URI_PREFIX + dpnId);
    }

    public static InstanceIdentifier<Interface> buildId(String interfaceName) {
        // TODO Make this generic and move to AbstractDataChangeListener or
        // Utils.
        InstanceIdentifierBuilder<Interface> idBuilder = InstanceIdentifier.builder(Interfaces.class)
                .child(Interface.class, new InterfaceKey(interfaceName));
        return idBuilder.build();
    }

    public static InstanceIdentifier<org.opendaylight.yang.gen.v1.urn
        .ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> buildStateInterfaceId(
            String interfaceName) {
        InstanceIdentifierBuilder<org.opendaylight.yang.gen.v1.urn
            .ietf.params.xml.ns.yang.ietf.interfaces.rev140508
                .interfaces.state.Interface> idBuilder = InstanceIdentifier
                .builder(InterfacesState.class)
                .child(org.opendaylight.yang.gen.v1.urn
                        .ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.class,
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                            .ietf.interfaces.rev140508.interfaces.state.InterfaceKey(
                                interfaceName));
        return idBuilder.build();
    }

    public static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
        .ietf.interfaces.rev140508.interfaces.state.InterfaceKey getStateInterfaceKeyFromName(
            String name) {
        return new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.interfaces.rev140508.interfaces.state.InterfaceKey(
                name);
    }

    public static InstanceIdentifier<IdPool> getPoolId(String poolName) {
        return InstanceIdentifier.builder(IdPools.class).child(IdPool.class, new IdPoolKey(poolName)).build();
    }

    public static long getGroupId(int ifIndex, InterfaceInfo.InterfaceType infType) {
        if (infType == LOGICAL_GROUP_INTERFACE) {
            return getLogicalTunnelSelectGroupId(ifIndex);
        }
        return 0;
    }

    /**
     * Synchronous blocking read from data store.
     *
     * @deprecated Use
     * {@link SingleTransactionDataBroker#syncReadOptional(DataBroker, LogicalDatastoreType, InstanceIdentifier)}
     *             instead of this.
     */
    @Deprecated
    public static <T extends DataObject> Optional<T> read(LogicalDatastoreType datastoreType,
            InstanceIdentifier<T> path, DataBroker broker) {
        try (ReadTransaction tx = broker.newReadOnlyTransaction()) {
            return tx.read(datastoreType, path).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Cannot read identifier", e);
            throw new RuntimeException(e);
        }
    }

    public static List<Action> getEgressActionsForInterface(String interfaceName, Long tunnelKey, Integer actionKey,
            InterfaceManagerCommonUtils interfaceUtils, Boolean isDefaultEgress) {
        List<ActionInfo> listActionInfo = getEgressActionInfosForInterface(interfaceName, tunnelKey,
                actionKey == null ? 0 : actionKey, interfaceUtils, isDefaultEgress);
        List<Action> actionsList = new ArrayList<>();
        for (ActionInfo actionInfo : listActionInfo) {
            actionsList.add(actionInfo.buildAction());
        }
        return actionsList;
    }

    public static List<Instruction> getEgressInstructionsForInterface(String interfaceName, Long tunnelKey,
            InterfaceManagerCommonUtils interfaceUtils, Boolean isDefaultEgress) {
        List<Instruction> instructions = new ArrayList<>();
        List<Action> actionList = MDSALUtil.buildActions(
                getEgressActionInfosForInterface(interfaceName, tunnelKey, 0, interfaceUtils, isDefaultEgress));
        instructions.add(MDSALUtil.buildApplyActionsInstruction(actionList));
        return instructions;
    }

    public static List<Instruction> getEgressInstructionsForInterface(Interface interfaceInfo, String portNo,
                                                                      Long tunnelKey, boolean isDefaultEgress,
                                                                      int ifIndex, long groupId) {
        List<Instruction> instructions = new ArrayList<>();
        InterfaceInfo.InterfaceType ifaceType = getInterfaceType(interfaceInfo);
        List<Action> actionList = MDSALUtil.buildActions(
                getEgressActionInfosForInterface(interfaceInfo, portNo, ifaceType, tunnelKey, 0,
                                                 isDefaultEgress, ifIndex, groupId));
        instructions.add(MDSALUtil.buildApplyActionsInstruction(actionList));
        return instructions;
    }

    public static List<ActionInfo> getEgressActionInfosForInterface(String interfaceName, int actionKeyStart,
            InterfaceManagerCommonUtils interfaceUtils, Boolean isDefaultEgress) {
        return getEgressActionInfosForInterface(interfaceName, null, actionKeyStart, interfaceUtils, isDefaultEgress);
    }

    /**
     * Returns a list of Actions to be taken when sending a packet over an
     * interface.
     *
     * @param interfaceName
     *            name of the interface
     * @param tunnelKey
     *            Optional.
     * @param actionKeyStart
     *            action key
     * @param interfaceUtils
     *            InterfaceManagerCommonUtils
     * @return list of actions
     */
    public static List<ActionInfo> getEgressActionInfosForInterface(String interfaceName, Long tunnelKey,
            int actionKeyStart, InterfaceManagerCommonUtils interfaceUtils, Boolean isDefaultEgress) {
        Interface interfaceInfo = interfaceUtils.getInterfaceFromConfigDS(new InterfaceKey(interfaceName));
        if (interfaceInfo == null) {
            throw new NullPointerException("Interface information not present in config DS for " + interfaceName);
        }
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.interfaces.rev140508.interfaces.state.Interface ifState =
            interfaceUtils.getInterfaceState(interfaceName);
        if (ifState == null) {
            throw new NullPointerException("Interface information not present in oper DS for " + interfaceName);
        }
        String lowerLayerIf = ifState.getLowerLayerIf().get(0);
        NodeConnectorId nodeConnectorId = new NodeConnectorId(lowerLayerIf);
        String portNo = IfmUtil.getPortNoFromNodeConnectorId(nodeConnectorId);

        InterfaceInfo.InterfaceType ifaceType = getInterfaceType(interfaceInfo);
        return getEgressActionInfosForInterface(interfaceInfo, portNo, ifaceType, tunnelKey, actionKeyStart,
                isDefaultEgress, ifState.getIfIndex(), 0);
    }

    /**
     * Returns the list of egress actions for a given interface.
     *
     * @param interfaceInfo the interface to look up
     * @param portNo port number
     * @param ifaceType the type of the interface
     * @param tunnelKey the tunnel key
     * @param actionKeyStart the start for the first key assigned for the new actions
     * @param isDefaultEgress if it is the default egress
     * @param ifIndex interface index
     * @param groupId group Id
     * @return list of actions for the interface
     */
    // The following suppression is for javac, not for checkstyle
    @SuppressWarnings("fallthrough")
    @SuppressFBWarnings("SF_SWITCH_FALLTHROUGH")
    public static List<ActionInfo> getEgressActionInfosForInterface(Interface interfaceInfo, String portNo,
            InterfaceInfo.InterfaceType ifaceType, Long tunnelKey, int actionKeyStart, boolean isDefaultEgress,
            int ifIndex, long groupId) {
        List<ActionInfo> result = new ArrayList<>();
        switch (ifaceType) {
            case MPLS_OVER_GRE:
                // fall through
            case GRE_TRUNK_INTERFACE:
                if (!isDefaultEgress) {
                    // TODO tunnel_id to encode GRE key, once it is supported
                    // Until then, tunnel_id should be "cleaned", otherwise it
                    // stores the value coming from a VXLAN tunnel
                    if (tunnelKey == null) {
                        tunnelKey = 0L;
                    }
                }
                // fall through
            case VXLAN_TRUNK_INTERFACE:
                if (!isDefaultEgress) {
                    if (tunnelKey != null) {
                        result.add(new ActionSetFieldTunnelId(actionKeyStart++, Uint64.valueOf(tunnelKey)));
                    }
                } else {
                    // For OF Tunnels default egress actions need to set tunnelIps
                    IfTunnel ifTunnel = interfaceInfo.augmentation(IfTunnel.class);
                    if (BooleanUtils.isTrue(ifTunnel.isTunnelRemoteIpFlow()
                            && ifTunnel.getTunnelDestination() != null)) {
                        result.add(new ActionSetTunnelDestinationIp(actionKeyStart++, ifTunnel.getTunnelDestination()));
                    }
                    if (BooleanUtils.isTrue(ifTunnel.isTunnelSourceIpFlow()
                            && ifTunnel.getTunnelSource() != null)) {
                        result.add(new ActionSetTunnelSourceIp(actionKeyStart++, ifTunnel.getTunnelSource()));
                    }
                }
                // fall through
            case VLAN_INTERFACE:
                if (isDefaultEgress) {
                    IfL2vlan vlanIface = interfaceInfo.augmentation(IfL2vlan.class);
                    LOG.trace("get egress actions for l2vlan interface: {}", vlanIface);
                    boolean isVlanTransparent = false;
                    int vlanVid = 0;
                    if (vlanIface != null) {
                        vlanVid = vlanIface.getVlanId() == null ? 0 : vlanIface.getVlanId().getValue().toJava();
                        isVlanTransparent = vlanIface.getL2vlanMode() == IfL2vlan.L2vlanMode.Transparent;
                    }
                    if (vlanVid != 0 && !isVlanTransparent) {
                        result.add(new ActionPushVlan(actionKeyStart++));
                        result.add(new ActionSetFieldVlanVid(actionKeyStart++, vlanVid));
                    }
                    result.add(new ActionOutput(actionKeyStart++, new Uri(portNo)));
                } else {
                    addEgressActionInfosForInterface(ifIndex, actionKeyStart, result);
                }
                break;
            case LOGICAL_GROUP_INTERFACE:
                if (isDefaultEgress) {
                    result.add(new ActionGroup(groupId));
                } else {
                    addEgressActionInfosForInterface(ifIndex, actionKeyStart, result);
                }
                break;

            default:
                LOG.warn("Interface Type {} not handled yet", ifaceType);
                break;
        }
        return result;
    }

    public static void addEgressActionInfosForInterface(int ifIndex, int actionKeyStart, List<ActionInfo> result) {
        long regValue = MetaDataUtil.getReg6ValueForLPortDispatcher(ifIndex, NwConstants.DEFAULT_SERVICE_INDEX);
        result.add(new ActionRegLoad(actionKeyStart++, NxmNxReg6.class, IfmConstants.REG6_START_INDEX,
                IfmConstants.REG6_END_INDEX, regValue));
        result.add(new ActionNxResubmit(actionKeyStart, NwConstants.EGRESS_LPORT_DISPATCHER_TABLE));
    }

    public static NodeId getNodeIdFromNodeConnectorId(NodeConnectorId ncId) {
        return new NodeId(ncId.getValue().substring(0, ncId.getValue().lastIndexOf(':')));
    }

    public static Uint64[] mergeOpenflowMetadataWriteInstructions(List<Instruction> instructions) {
        Uint64 metadata = Uint64.ZERO;
        Uint64 metadataMask = Uint64.ZERO;
        if (instructions != null && !instructions.isEmpty()) {
            // check if metadata write instruction is present
            for (Instruction instruction : instructions) {
                org.opendaylight.yang.gen.v1.urn.opendaylight.flow
                    .types.rev131026.instruction.Instruction actualInstruction = instruction
                        .getInstruction();
                if (actualInstruction instanceof WriteMetadataCase) {
                    WriteMetadataCase writeMetaDataInstruction = (WriteMetadataCase) actualInstruction;
                    WriteMetadata availableMetaData = writeMetaDataInstruction.getWriteMetadata();
                    metadata = Uint64.fromLongBits(metadata.longValue() | availableMetaData.getMetadata().longValue());
                    metadataMask = Uint64.fromLongBits(metadataMask.longValue()
                        | availableMetaData.getMetadataMask().longValue());
                }
            }
        }
        return new Uint64[] { metadata, metadataMask };
    }

    public static Integer allocateId(IdManagerService idManager, String poolName, String idKey) {
        AllocateIdInput getIdInput = new AllocateIdInputBuilder().setPoolName(poolName).setIdKey(idKey).build();
        try {
            Future<RpcResult<AllocateIdOutput>> result = idManager.allocateId(getIdInput);
            RpcResult<AllocateIdOutput> rpcResult = result.get();
            if (rpcResult.isSuccessful()) {
                return rpcResult.getResult().getIdValue().intValue();
            } else {
                LOG.warn("RPC Call to Get Unique Id returned with Errors {}", rpcResult.getErrors());
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Exception when getting Unique Id", e);
        }
        return INVALID_ID;
    }

    public static void releaseId(IdManagerService idManager, String poolName, String idKey) {
        ReleaseIdInput idInput = new ReleaseIdInputBuilder().setPoolName(poolName).setIdKey(idKey).build();
        try {
            ListenableFuture<RpcResult<ReleaseIdOutput>> result = idManager.releaseId(idInput);
            RpcResult<ReleaseIdOutput> rpcResult = result.get();
            if (!rpcResult.isSuccessful()) {
                LOG.warn("RPC Call to release Id with Key {} returned with Errors {}", idKey, rpcResult.getErrors());
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Exception when releasing Id for key {}", idKey, e);
        }
    }

    public static Uint64 getDpnId(DatapathId datapathId) {
        if (datapathId != null) {
            // Adding logs for a random issue spotted during datapath id
            // conversion
            String dpIdStr = datapathId.getValue().replace(":", "");
            return Uint64.valueOf(dpIdStr, 16);
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

    public static InterfaceInfo.InterfaceType getInterfaceType(Interface iface) {
        InterfaceInfo.InterfaceType interfaceType = org.opendaylight
                .genius.interfacemanager.globals.InterfaceInfo.InterfaceType.UNKNOWN_INTERFACE;
        Class<? extends org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.interfaces.rev140508.InterfaceType> ifType = iface
                .getType();

        if (ifType.isAssignableFrom(L2vlan.class)) {
            interfaceType = VLAN_INTERFACE;
        } else if (ifType.isAssignableFrom(Tunnel.class)) {
            IfTunnel ifTunnel = iface.augmentation(IfTunnel.class);
            Class<? extends org.opendaylight.yang.gen.v1.urn.opendaylight
                    .genius.interfacemanager.rev160406.TunnelTypeBase> tunnelType = ifTunnel
                    .getTunnelInterfaceType();
            interfaceType = tunnelType.isAssignableFrom(TunnelTypeLogicalGroup.class)
                    ? InterfaceInfo.InterfaceType.LOGICAL_GROUP_INTERFACE :  TUNNEL_TYPE_MAP.get(tunnelType);
        }
        return interfaceType;
    }

    public static VlanInterfaceInfo getVlanInterfaceInfo(Interface iface, Uint64 dpId) {
        short vlanId = 0;
        String portName = null;
        IfL2vlan vlanIface = iface.augmentation(IfL2vlan.class);
        ParentRefs parentRefs = iface.augmentation(ParentRefs.class);
        if (parentRefs != null && parentRefs.getParentInterface() != null) {
            portName = parentRefs.getParentInterface();
        } else {
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

    public static Uint64 getDeadBeefBytesForMac() {
        final long raw = IfmConstants.DEAD_BEEF_MAC_PREFIX.longValue() & 0xFFFFFFFFL;
        return Uint64.fromLongBits(raw << 16);
    }

    public static Uint64 fillPortNumberToMac(long portNumber) {
        return Uint64.fromLongBits(portNumber & 0xFFFF);
    }

    public static String generateMacAddress(long portNo) {
        final long raw = getDeadBeefBytesForMac().longValue() | fillPortNumberToMac(portNo).longValue();
        return GENERATE_MAC_PATTERN.matcher(Long.toUnsignedString(raw))
                .replaceAll("$1" + IfmConstants.MAC_SEPARATOR)
                .substring(0, IfmConstants.MAC_STRING_LENGTH);
    }

    public static PhysAddress getPhyAddress(long portNo, FlowCapableNodeConnector flowCapableNodeConnector) {
        String southboundMacAddress = flowCapableNodeConnector.getHardwareAddress().getValue();
        if (IfmConstants.INVALID_MAC.equals(southboundMacAddress)) {
            LOG.debug("Invalid MAC Address received for {}, generating MAC Address",
                    flowCapableNodeConnector.getName());
            southboundMacAddress = generateMacAddress(portNo);
        }
        return new PhysAddress(southboundMacAddress);
    }

    public static void updateInterfaceParentRef(TypedWriteTransaction<Configuration> tx, String interfaceName,
            String parentInterface) {
        InstanceIdentifier<ParentRefs> parentRefIdentifier = InstanceIdentifier.builder(Interfaces.class)
                .child(Interface.class, new InterfaceKey(interfaceName)).augmentation(ParentRefs.class).build();
        ParentRefs parentRefs = new ParentRefsBuilder().setParentInterface(parentInterface).build();
        tx.merge(parentRefIdentifier, parentRefs);
        LOG.debug(
                "Updating parentRefInterface for interfaceName {}. "
                        + "interfaceKey {}, with parentRef augmentation pointing to {}",
                interfaceName, new InterfaceKey(interfaceName), parentInterface);
    }

    public static InstanceIdentifier<BoundServices> buildBoundServicesIId(short servicePriority, String interfaceName,
            Class<? extends ServiceModeBase> serviceMode) {
        return InstanceIdentifier.builder(ServiceBindings.class)
                .child(ServicesInfo.class, new ServicesInfoKey(interfaceName, serviceMode))
                .child(BoundServices.class, new BoundServicesKey(servicePriority)).build();
    }

    public static void bindService(TypedWriteTransaction<Configuration> tx, String interfaceName,
        BoundServices serviceInfo, Class<? extends ServiceModeBase> serviceMode) {
        LOG.info("Binding Service {} for : {}", serviceInfo.getServiceName(), interfaceName);
        InstanceIdentifier<BoundServices> boundServicesInstanceIdentifier = buildBoundServicesIId(
            serviceInfo.getServicePriority().toJava(), interfaceName, serviceMode);
        tx.put(boundServicesInstanceIdentifier, serviceInfo, CREATE_MISSING_PARENTS);
    }

    public static void unbindService(ManagedNewTransactionRunner txRunner, JobCoordinator coordinator,
            String interfaceName, InstanceIdentifier<BoundServices> boundServicesInstanceIdentifier) {
        coordinator.enqueueJob(interfaceName, () -> Collections.singletonList(
                txRunner.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION,
                    tx -> unbindService(tx, interfaceName, boundServicesInstanceIdentifier))));
    }

    public static void unbindService(TypedWriteTransaction<Configuration> tx, String interfaceName,
            InstanceIdentifier<BoundServices> boundServicesInstanceIdentifier) {
        LOG.info("Unbinding Service from : {}", interfaceName);
        tx.delete(boundServicesInstanceIdentifier);
    }

    public static long getLogicalTunnelSelectGroupId(int lportTag) {
        return org.opendaylight.genius.interfacemanager.globals.IfmConstants.VXLAN_GROUPID_MIN + lportTag;
    }
}
