/*
 * Copyright (c) 2017 Hewlett Packard Enterprise, Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.confighelpers;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.interfacemanager.globals.IfmConstants;
import org.opendaylight.genius.interfacemanager.globals.InterfaceInfo;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.MetaDataUtil;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.mdsalutil.actions.ActionNxResubmit;
import org.opendaylight.genius.mdsalutil.actions.ActionRegLoad;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Tunnel;
//import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.CreateIdPoolInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.CreateIdPoolInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.InterfaceChildInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeLogicalGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.itm.config.TunnelAggregation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.Bucket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg6;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ItmTunnelAggregationHelper {

    public static final int ADD_TUNNEL = 0;
    public static final int DEL_TUNNEL = 1;
    public static final int MOD_TUNNEL = 2;
    public static final int MOD_GROUP_TUNNEL = 3;
    public static final int DEFAULT_WEIGHT = 1;

    private static final Logger logger = LoggerFactory.getLogger(ItmTunnelAggregationHelper.class);
    private static IMdsalApiManager mdsalManager;
    private static IdManagerService idManagerService;
    private static IInterfaceManager interfaceManager;
    private static boolean tunnelAggregationEnabled;

    @Inject
    public ItmTunnelAggregationHelper(IdManagerService idManager, ItmConfig itmConfig,
                                      IMdsalApiManager mdsalMngr, IInterfaceManager interfaceMngr) {
        initTunnelAggregationConfig(itmConfig);
        idManagerService = idManager;
        mdsalManager = mdsalMngr;
        interfaceManager = interfaceMngr;
    }

    @PostConstruct
    public void start() {
        createGroupIdPool(idManagerService);
    }

    public static boolean isTunnelAggregationEnabled() {
        return tunnelAggregationEnabled;
    }

    public static long getLogicalTunnelSelectGroupId(String ifaceName) {
        long groupId = 0;
        AllocateIdInput getIdInput = new AllocateIdInputBuilder()
                .setPoolName(ITMConstants.VXLAN_GROUP_POOL_NAME).setIdKey(ifaceName).build();
        try {
            Future<RpcResult<AllocateIdOutput>> result = idManagerService.allocateId(getIdInput);
            RpcResult<AllocateIdOutput> rpcResult = result.get();
            groupId = rpcResult.getResult().getIdValue();
        } catch (NullPointerException | InterruptedException | ExecutionException e) {
            logger.trace("",e);
        }
        return groupId;
    }

    public static void createLogicalTunnelSelectGroup(BigInteger srcDpnId, String interfaceName) {
        long groupId = getLogicalTunnelSelectGroupId(interfaceName);
        if (groupId == 0) {
            logger.warn("MULTIPLE_VxLAN_TUNNELS: group id was not allocated for {} srcDpnId {}",
                          interfaceName, srcDpnId);
            return;
        }
        logger.debug("MULTIPLE_VxLAN_TUNNELS: id {} allocated for the logical select group {} srcDpnId {}",
                      groupId, interfaceName, srcDpnId);
        Group group = MDSALUtil.buildGroup(groupId, interfaceName, GroupTypes.GroupSelect,
                                           MDSALUtil.buildBucketLists(Collections.emptyList()));
        mdsalManager.syncInstallGroup(srcDpnId, group, ITMConstants.DELAY_TIME_IN_MILLISECOND);
    }

    public static void updateLogicalTunnelSelectGroup(InterfaceParentEntry entry, DataBroker broker) {
        String groupName = entry.getParentInterface();
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface iface =
                ItmUtils.getInterface(groupName, interfaceManager);
        if (iface == null || !iface.getType().isAssignableFrom(Tunnel.class)) {
            return;
        }
        IfTunnel ifTunnel = iface.getAugmentation(IfTunnel.class);
        if (!ifTunnel.getTunnelInterfaceType().isAssignableFrom(TunnelTypeLogicalGroup.class)) {
            return;
        }
        long groupId = getLogicalTunnelSelectGroupId(groupName);
        logger.debug("MULTIPLE_VxLAN_TUNNELS: updateLogicalTunnelSelectGroup id {} groupName {}", groupId, groupName);
        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
        TunnelAggregationUpdateWorker worker =
                new TunnelAggregationUpdateWorker(null, iface, entry, MOD_GROUP_TUNNEL, broker);
        coordinator.enqueueJob(groupName, worker);
    }

    public static void updateLogicalTunnelState(Interface ifaceState, int tunnelAction, DataBroker broker) {
        boolean tunnelAggregationEnabled = isTunnelAggregationEnabled();
        if (!tunnelAggregationEnabled || ifaceState == null) {
            logger.debug("MULTIPLE_VxLAN_TUNNELS: updateLogicalTunnelState - wrong configuration -"
                    + " tunnelAggregationEnabled {} ifaceState {}", tunnelAggregationEnabled, ifaceState);
            return;
        }
        String ifName = ifaceState.getName();
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface iface =
                ItmUtils.getInterface(ifName, interfaceManager);
        IfTunnel ifTunnel = iface != null ? iface.getAugmentation(IfTunnel.class) : null;
        if (iface == null || ifTunnel == null) {
            logger.debug("MULTIPLE_VxLAN_TUNNELS: updateLogicalTunnelState - not tunnel interface {}", ifName);
            return;
        }
        String groupName = null;
        if (ifTunnel.getTunnelInterfaceType().isAssignableFrom(TunnelTypeLogicalGroup.class)) {
            groupName = ifaceState.getName();
        } else {
            ParentRefs parentRefs = iface.getAugmentation(ParentRefs.class);
            if (ifTunnel.getTunnelInterfaceType().isAssignableFrom(TunnelTypeVxlan.class) && parentRefs != null) {
                groupName = parentRefs.getParentInterface();
            }
        }
        if (groupName != null) {
            DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
            TunnelAggregationUpdateWorker worker =
                    new TunnelAggregationUpdateWorker(ifaceState, iface, null, tunnelAction, broker);
            coordinator.enqueueJob(groupName, worker);
        }
    }

    private void createGroupIdPool(IdManagerService idManager) {
        CreateIdPoolInput createPool = new CreateIdPoolInputBuilder()
            .setPoolName(ITMConstants.VXLAN_GROUP_POOL_NAME)
            .setLow(ITMConstants.VXLAN_GROUP_POOL_START)
            .setHigh(ITMConstants.VXLAN_GROUP_POOL_END)
            .build();
        try {
            Future<RpcResult<Void>> result = idManager.createIdPool(createPool);
            if (result != null && result.get().isSuccessful()) {
                logger.debug("MULTIPLE_VxLAN_TUNNELS: created GroupIdPool");
            } else {
                logger.error("MULTIPLE_VxLAN_TUNNELS: unable to create GroupIdPool");
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error("MULTIPLE_VxLAN_TUNNELS: failed to create pool for tunnel aggregation service", e);
        }
    }

    private void initTunnelAggregationConfig(ItmConfig itmConfig) {
        // Load balancing of VxLAN feature is guarded by a global configuration option in the ITM,
        // only when the feature is enabled, the logical tunnel interfaces should be created.
        boolean tunnelAggregationConfigEnabled = false;
        List<TunnelAggregation> tunnelsConfig = itmConfig != null ? itmConfig.getTunnelAggregation() : null;
        if (tunnelsConfig != null) {
            for (TunnelAggregation tnlCfg : tunnelsConfig) {
                Class<? extends TunnelTypeBase> tunType = ItmUtils.getTunnelType(tnlCfg.getKey().getTunnelType());
                if (tunType.isAssignableFrom(TunnelTypeVxlan.class)) {
                    tunnelAggregationConfigEnabled = tnlCfg.isEnabled();
                    logger.debug("MULTIPLE_VxLAN_TUNNELS: tunnelAggregationEnabled {}", tunnelAggregationConfigEnabled);
                    break;
                }
            }
        }
        ItmTunnelAggregationHelper.tunnelAggregationEnabled = tunnelAggregationConfigEnabled;
    }

    private static Bucket createBucket(IfTunnel ifTunnel, ParentRefs parentRefs, Integer ifIndex,
                                       int bucketId, int portNumber) {
        // from SPEC: each tunnel member can be assigned a weight field that will be applied on its
        // corresponding bucket in the OF select group. If a weight was not defined, the bucket weight will
        // be configured with a default value of 1
        Integer portWeight = ifTunnel.getWeight() != null ? ifTunnel.getWeight() : DEFAULT_WEIGHT;
        int actionKey = 0;
        BigInteger lportTag = MetaDataUtil.getLportTagForReg6(ifIndex);
        List<ActionInfo> listActionInfo = new ArrayList<ActionInfo>();
        listActionInfo.add(new ActionRegLoad(actionKey++, NxmNxReg6.class, ITMConstants.REG6_START_INDEX,
                ITMConstants.REG6_END_INDEX, lportTag.longValue()));
        listActionInfo.add(new ActionNxResubmit(actionKey++, NwConstants.EGRESS_LPORT_DISPATCHER_TABLE));
        Bucket buckt = MDSALUtil.buildBucket(MDSALUtil.buildActions(listActionInfo), portWeight, bucketId,
                                             portNumber, MDSALUtil.WATCH_GROUP);
        return buckt;
    }

    private static void updateTunnelAggregationGroup(InterfaceParentEntry entry) {
        String groupName = entry.getParentInterface();
        InternalTunnel groupInternalTunnel = ItmUtils.itmCache.getInternalTunnel(groupName);
        if (groupInternalTunnel == null) {
            logger.debug("MULTIPLE_VxLAN_TUNNELS: {} not found in internal tunnels list", groupName);
            return;
        }
        long groupId = getLogicalTunnelSelectGroupId(groupName);
        BigInteger srcDpnId = groupInternalTunnel.getSourceDPN();
        List<Bucket> listBuckets = new ArrayList<>();
        List<InterfaceChildEntry> interfaceChildEntries = entry.getInterfaceChildEntry();
        for (InterfaceChildEntry interfaceChildEntry : interfaceChildEntries) {
            String curChildName = interfaceChildEntry.getChildInterface();
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface
                        childIface = ItmUtils.getInterface(curChildName, interfaceManager);
            IfTunnel ifTunnel = childIface != null ? childIface.getAugmentation(IfTunnel.class) : null;
            if (ifTunnel == null || !ifTunnel.getTunnelInterfaceType().isAssignableFrom(TunnelTypeVxlan.class)) {
                continue;
            }
            ParentRefs parentRefs = childIface.getAugmentation(ParentRefs.class);
            if (parentRefs == null) {
                continue;
            }
            InterfaceInfo ifInfo = interfaceManager.getInterfaceInfoFromOperationalDSCache(curChildName);
            if (ifInfo == null) {
                ifInfo = interfaceManager.getInterfaceInfoFromOperationalDataStore(curChildName);
                if (ifInfo == null) {
                    logger.debug("MULTIPLE_VxLAN_TUNNELS: interface state not found for {} in groupId {}",
                            curChildName, groupId);
                    continue;
                }
            }
            int bucketId = interfaceChildEntries.indexOf(interfaceChildEntry);
            logger.debug("MULTIPLE_VxLAN_TUNNELS: updateTunnelAggregationGroup - groupId {} bucketId {}",
                    groupId, bucketId);
            Bucket buckt = createBucket(ifTunnel, parentRefs, ifInfo.getInterfaceTag(), bucketId, ifInfo.getPortNo());
            listBuckets.add(buckt);
        }
        if (!listBuckets.isEmpty()) {
            Group group = MDSALUtil.buildGroup(groupId, groupName, GroupTypes.GroupSelect,
                                               MDSALUtil.buildBucketLists(listBuckets));
            mdsalManager.syncInstallGroup(srcDpnId, group, ITMConstants.DELAY_TIME_IN_MILLISECOND);
        }
    }

    private static void updateTunnelAggregationGroupBucket(Interface ifaceState, IfTunnel ifTunnel,
                                                           ParentRefs parentRefs, InterfaceParentEntry groupParentEntry,
                                                           int action, WriteTransaction tx) {
        String groupName = parentRefs.getParentInterface();
        long groupId = getLogicalTunnelSelectGroupId(groupName);
        String ifaceName = ifaceState.getName();
        InterfaceChildEntry childEntry = new InterfaceChildEntryBuilder().setChildInterface(ifaceName)
                .setKey(new InterfaceChildEntryKey(ifaceName)).build();
        List<InterfaceChildEntry> interfaceChildEntries = groupParentEntry.getInterfaceChildEntry();
        int bucketId = interfaceChildEntries.indexOf(childEntry);
        if (bucketId == -1) {
            return;
        }
        String lowerLayerIf = ifaceState.getLowerLayerIf().get(0); // openflow:dpnid:portnum
        String[] split = lowerLayerIf.split(IfmConstants.OF_URI_SEPARATOR);
        BigInteger srcDpnId = new BigInteger(split[1]);
        int portNumber = Integer.parseInt(split[2]);
        if (action == ADD_TUNNEL) {
            Bucket buckt = createBucket(ifTunnel, parentRefs, ifaceState.getIfIndex(), bucketId, portNumber);
            logger.debug("MULTIPLE_VxLAN_TUNNELS: add bucketId {} to groupId {}", bucketId, groupId);
            mdsalManager.addBucketToTx(srcDpnId, groupId, buckt, tx);
        } else {
            logger.debug("MULTIPLE_VxLAN_TUNNELS: remove bucketId {} from groupId {}", bucketId, groupId);
            mdsalManager.removeBucketToTx(srcDpnId, groupId, bucketId, tx);
        }
    }

    private static void updateLogicalTunnelGroupOperStatus(String logicalGroupName, Interface ifaceState,
                                                           InterfaceParentEntry groupParentEntry,
                                                           DataBroker broker, WriteTransaction tx) {
        if (groupParentEntry == null) {
            logger.debug("MULTIPLE_VxLAN_TUNNELS: uninitialized parent entry {}", logicalGroupName);
            return;
        }
        OperStatus groupNewState = OperStatus.Down;
        List<InterfaceChildEntry> interfaceChildEntries = groupParentEntry.getInterfaceChildEntry();
        for (InterfaceChildEntry interfaceChildEntry : interfaceChildEntries) {
            String curChildInterface = interfaceChildEntry.getChildInterface();
            if (curChildInterface.equals(ifaceState.getName())) {
                if (ifaceState.getOperStatus() == OperStatus.Up) {
                    groupNewState = OperStatus.Up;
                    break;
                }
                continue;
            }
            InterfaceInfo ifInfo = interfaceManager.getInterfaceInfoFromOperationalDSCache(curChildInterface);
            if (ifInfo != null && ifInfo.getOpState() == InterfaceInfo.InterfaceOpState.UP) {
                groupNewState = OperStatus.Up;
                break;
            }
        }
        if (logicalGroupName.equals(ifaceState.getName())) { //the current interface is logical group itself
            if (ifaceState.getOperStatus() != groupNewState) {
                updateInterfaceOperStatus(logicalGroupName, ifaceState, groupNewState, tx);
            }
        } else {
            InterfaceInfo ifLogicInfo = interfaceManager.getInterfaceInfoFromOperationalDSCache(logicalGroupName);
            if (ifLogicInfo != null &&
                (ifLogicInfo.getOpState() == InterfaceInfo.InterfaceOpState.UP && groupNewState == OperStatus.Down) ||
                (ifLogicInfo.getOpState() == InterfaceInfo.InterfaceOpState.DOWN && groupNewState == OperStatus.Up)) {

                InstanceIdentifier<Interface> idGroup = ItmUtils.buildStateInterfaceId(logicalGroupName);
                Optional<Interface> ifStateGroup = ItmUtils.read(LogicalDatastoreType.OPERATIONAL, idGroup, broker);
                if (ifStateGroup.isPresent()) {
                    Interface ifStateLogicGroup = ifStateGroup.get();
                    updateInterfaceOperStatus(logicalGroupName, ifStateLogicGroup, groupNewState, tx);
                }
            }
        }
    }

    private static void updateInterfaceOperStatus(String logicGroupName, Interface ifaceLogicGroupState,
                                                  OperStatus st, WriteTransaction tx) {
        logger.debug("MULTIPLE_VxLAN_TUNNELS: updateInterfaceOperStatus to {} for logical group {}",
                st.toString(), logicGroupName);
        InstanceIdentifier<Interface> idLogicGroup = ItmUtils.buildStateInterfaceId(logicGroupName);
        InterfaceBuilder ifaceBuilderChild = new InterfaceBuilder(ifaceLogicGroupState);
        ifaceBuilderChild.setOperStatus(st);
        tx.merge(LogicalDatastoreType.OPERATIONAL, idLogicGroup, ifaceBuilderChild.build(), true);
    }

    private static class TunnelAggregationUpdateWorker implements Callable<List<ListenableFuture<Void>>> {

        private final Interface  ifaceState;
        private final DataBroker dataBroker;
        private final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface ifaceConfig;
        private final int ifaceAction;
        private final InterfaceParentEntry parentEntry;

        TunnelAggregationUpdateWorker(Interface ifState,
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface iface,
                InterfaceParentEntry entry, int action, DataBroker broker) {
            ifaceState  = ifState;
            ifaceConfig = iface;
            ifaceAction = action;
            dataBroker  = broker;
            parentEntry = entry;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            List<ListenableFuture<Void>> futures = new ArrayList<>();
            if (ifaceAction == MOD_GROUP_TUNNEL) {
                updateTunnelAggregationGroup(parentEntry);
                return futures;
            }
            IfTunnel ifTunnel = ifaceConfig != null ? ifaceConfig.getAugmentation(IfTunnel.class) : null;
            if (ifTunnel == null) {
                logger.debug("MULTIPLE_VxLAN_TUNNELS: not tunnel interface {}", ifaceConfig.getName());
                return futures;
            }
            WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
            if (ifTunnel.getTunnelInterfaceType().isAssignableFrom(TunnelTypeLogicalGroup.class)) {
                String logicalGroupName = ifaceState.getName();
                InterfaceParentEntry groupParentEntry = getInterfaceParentEntry(logicalGroupName);
                updateLogicalTunnelGroupOperStatus(logicalGroupName, ifaceState, groupParentEntry, dataBroker, tx);
                futures.add(tx.submit());
                return futures;
            }
            if (!ifTunnel.getTunnelInterfaceType().isAssignableFrom(TunnelTypeVxlan.class)) {
                logger.debug("MULTIPLE_VxLAN_TUNNELS: wrong tunnel type {}", ifTunnel.getTunnelInterfaceType());
                return futures;
            }
            ParentRefs parentRefs = ifaceConfig.getAugmentation(ParentRefs.class);
            if (parentRefs == null) {
                logger.debug("MULTIPLE_VxLAN_TUNNELS: not updated parent ref for {}", ifaceConfig.getName());
                return futures;
            }
            String groupName = parentRefs.getParentInterface();
            InterfaceParentEntry groupEntry = parentEntry == null ? getInterfaceParentEntry(groupName) : parentEntry;
            if (ifaceAction == ADD_TUNNEL || ifaceAction == DEL_TUNNEL) {
                updateTunnelAggregationGroupBucket(ifaceState, ifTunnel, parentRefs, groupEntry, ifaceAction, tx);
            }
            updateLogicalTunnelGroupOperStatus(groupName, ifaceState, groupEntry, dataBroker, tx);
            futures.add(tx.submit());
            return futures;
        }

        private InterfaceParentEntry getInterfaceParentEntry(String logicalGroupName) {
            InterfaceParentEntryKey interfaceParentEntryKey = new InterfaceParentEntryKey(logicalGroupName);
            InstanceIdentifier.InstanceIdentifierBuilder<InterfaceParentEntry> intfIdBuilder =
                    InstanceIdentifier.builder(InterfaceChildInfo.class)
                            .child(InterfaceParentEntry.class, interfaceParentEntryKey);
            InstanceIdentifier<InterfaceParentEntry> intfId = intfIdBuilder.build();
            Optional<InterfaceParentEntry> groupChildInfo =
                    ItmUtils.read(LogicalDatastoreType.CONFIGURATION, intfId, dataBroker);
            return groupChildInfo.isPresent() ? groupChildInfo.get() : null;
        }
    }
}
