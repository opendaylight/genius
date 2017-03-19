/*
 * Copyright (c) 2017 Hewlett Packard Enterprise, Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.confighelpers;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.interfacemanager.globals.IfmConstants;
import org.opendaylight.genius.interfacemanager.globals.InterfaceInfo;
import org.opendaylight.genius.interfacemanager.globals.InterfaceInfo.InterfaceAdminState;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.CreateIdPoolInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.CreateIdPoolInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.InterfaceChildInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntryKey;
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
    public static final long INVALID_ID = 0;

    private static final Logger LOG = LoggerFactory.getLogger(ItmTunnelAggregationHelper.class);
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
        long groupId = INVALID_ID;
        AllocateIdInput getIdInput = new AllocateIdInputBuilder()
                .setPoolName(IfmConstants.VXLAN_GROUP_POOL_NAME).setIdKey(ifaceName).build();
        try {
            Future<RpcResult<AllocateIdOutput>> result = idManagerService.allocateId(getIdInput);
            RpcResult<AllocateIdOutput> rpcResult = result.get();
            groupId = rpcResult.getResult().getIdValue();
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("MULTIPLE_VxLAN_TUNNELS: Exception while creating group id for {}", ifaceName, e);
        }
        return groupId;
    }

    public static void createLogicalTunnelSelectGroup(BigInteger srcDpnId, String interfaceName) {
        long groupId = getLogicalTunnelSelectGroupId(interfaceName);
        if (groupId == INVALID_ID) {
            LOG.warn("MULTIPLE_VxLAN_TUNNELS: group id was not allocated for {} srcDpnId {}",
                          interfaceName, srcDpnId);
            return;
        }
        LOG.debug("MULTIPLE_VxLAN_TUNNELS: id {} allocated for the logical select group {} srcDpnId {}",
                      groupId, interfaceName, srcDpnId);
        Group group = MDSALUtil.buildGroup(groupId, interfaceName, GroupTypes.GroupSelect,
                                           MDSALUtil.buildBucketLists(Collections.emptyList()));
        mdsalManager.syncInstallGroup(srcDpnId, group, ITMConstants.DELAY_TIME_IN_MILLISECOND);
    }

    public static void updateLogicalTunnelSelectGroup(InterfaceParentEntry entry, DataBroker broker) {
        String logicTunnelName = entry.getParentInterface();
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508
                    .interfaces.Interface ifaceConfig = ItmUtils.getInterface(logicTunnelName, interfaceManager);
        if (ifaceConfig == null || !ifaceConfig.getType().isAssignableFrom(Tunnel.class)) {
            return;
        }
        IfTunnel ifTunnel = ifaceConfig.getAugmentation(IfTunnel.class);
        if (!ifTunnel.getTunnelInterfaceType().isAssignableFrom(TunnelTypeLogicalGroup.class)) {
            return;
        }
        LOG.debug("MULTIPLE_VxLAN_TUNNELS: updateLogicalTunnelSelectGroup name {}", logicTunnelName);
        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
        TunnelAggregationUpdateWorker worker =
                new TunnelAggregationUpdateWorker(null, null, ifaceConfig, entry, MOD_GROUP_TUNNEL, broker);
        coordinator.enqueueJob(logicTunnelName, worker);
    }

    public static void updateLogicalTunnelState(Interface ifaceState, int tunnelAction, DataBroker broker) {
        updateLogicalTunnelState(null, ifaceState, tunnelAction, broker);
    }

    public static void updateLogicalTunnelState(Interface ifStateOrigin, Interface ifStateUpdated,
                                                int tunnelAction, DataBroker broker) {
        boolean tunnelAggregationEnabled = isTunnelAggregationEnabled();
        if (!tunnelAggregationEnabled || ifStateUpdated == null) {
            LOG.debug("MULTIPLE_VxLAN_TUNNELS: updateLogicalTunnelState - wrong configuration -"
                    + " tunnelAggregationEnabled {} ifStateUpdated {}", tunnelAggregationEnabled, ifStateUpdated);
            return;
        }
        String ifName = ifStateUpdated.getName();
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface iface =
                ItmUtils.getInterface(ifName, interfaceManager);
        IfTunnel ifTunnel = iface != null ? iface.getAugmentation(IfTunnel.class) : null;
        if (iface == null || ifTunnel == null) {
            LOG.debug("MULTIPLE_VxLAN_TUNNELS: updateLogicalTunnelState - not tunnel interface {}", ifName);
            return;
        }
        String logicTunnelName = null;
        if (ifTunnel.getTunnelInterfaceType().isAssignableFrom(TunnelTypeLogicalGroup.class)) {
            logicTunnelName = ifStateUpdated.getName();
        } else {
            ParentRefs parentRefs = iface.getAugmentation(ParentRefs.class);
            if (ifTunnel.getTunnelInterfaceType().isAssignableFrom(TunnelTypeVxlan.class) && parentRefs != null) {
                logicTunnelName = parentRefs.getParentInterface();
            }
        }
        if (logicTunnelName != null) {
            DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
            TunnelAggregationUpdateWorker worker =
                    new TunnelAggregationUpdateWorker(ifStateOrigin, ifStateUpdated, iface, null, tunnelAction, broker);
            coordinator.enqueueJob(logicTunnelName, worker);
        }
    }

    private void createGroupIdPool(IdManagerService idManager) {
        CreateIdPoolInput createPool = new CreateIdPoolInputBuilder()
            .setPoolName(IfmConstants.VXLAN_GROUP_POOL_NAME)
            .setLow(ITMConstants.VXLAN_GROUP_POOL_START)
            .setHigh(ITMConstants.VXLAN_GROUP_POOL_END)
            .build();
        try {
            Future<RpcResult<Void>> result = idManager.createIdPool(createPool);
            if (result != null && result.get().isSuccessful()) {
                LOG.debug("MULTIPLE_VxLAN_TUNNELS: created GroupIdPool");
            } else {
                LOG.error("MULTIPLE_VxLAN_TUNNELS: unable to create GroupIdPool");
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("MULTIPLE_VxLAN_TUNNELS: failed to create pool for tunnel aggregation service", e);
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
                    LOG.info("MULTIPLE_VxLAN_TUNNELS: tunnelAggregationEnabled {}", tunnelAggregationConfigEnabled);
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
        String logicTunnelName = entry.getParentInterface();
        InternalTunnel logicInternalTunnel = ItmUtils.itmCache.getInternalTunnel(logicTunnelName);
        if (logicInternalTunnel == null) {
            LOG.debug("MULTIPLE_VxLAN_TUNNELS: {} not found in internal tunnels list", logicTunnelName);
            return;
        }
        long groupId = getLogicalTunnelSelectGroupId(logicTunnelName);
        if (groupId == INVALID_ID) {
            LOG.debug("MULTIPLE_VxLAN_TUNNELS: group id was not allocated for {}", logicTunnelName);
            return;
        }
        BigInteger srcDpnId = logicInternalTunnel.getSourceDPN();
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
                    LOG.debug("MULTIPLE_VxLAN_TUNNELS: interface state not found for {} in groupId {}",
                            curChildName, groupId);
                    continue;
                }
            }
            int bucketId = interfaceChildEntries.indexOf(interfaceChildEntry);
            LOG.debug("MULTIPLE_VxLAN_TUNNELS: updateTunnelAggregationGroup - groupId {} bucketId {}",
                    groupId, bucketId);
            Bucket buckt = createBucket(ifTunnel, parentRefs, ifInfo.getInterfaceTag(), bucketId, ifInfo.getPortNo());
            listBuckets.add(buckt);
        }
        if (!listBuckets.isEmpty()) {
            Group group = MDSALUtil.buildGroup(groupId, logicTunnelName, GroupTypes.GroupSelect,
                                               MDSALUtil.buildBucketLists(listBuckets));
            mdsalManager.syncInstallGroup(srcDpnId, group, ITMConstants.DELAY_TIME_IN_MILLISECOND);
        }
    }

    private static void updateTunnelAggregationGroupBucket(Interface ifaceState, IfTunnel ifTunnel,
                                                           ParentRefs parentRefs, InterfaceParentEntry groupParentEntry,
                                                           int action, WriteTransaction tx) {
        String logicTunnelName = parentRefs.getParentInterface();
        long groupId = getLogicalTunnelSelectGroupId(logicTunnelName);
        if (groupId == INVALID_ID) {
            LOG.debug("MULTIPLE_VxLAN_TUNNELS: group id was not allocated for {}", logicTunnelName);
            return;
        }
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
            LOG.debug("MULTIPLE_VxLAN_TUNNELS: add bucketId {} to groupId {}", bucketId, groupId);
            mdsalManager.addBucketToTx(srcDpnId, groupId, buckt, tx);
        } else {
            LOG.debug("MULTIPLE_VxLAN_TUNNELS: remove bucketId {} from groupId {}", bucketId, groupId);
            mdsalManager.removeBucketToTx(srcDpnId, groupId, bucketId, tx);
        }
    }

    private static void updateLogicalTunnelGroupOperStatus(String logicalTunnelIfaceName, Interface ifaceState,
                                                           InterfaceParentEntry parentEntry,
                                                           DataBroker broker, WriteTransaction tx) {
        if (parentEntry == null) {
            LOG.debug("MULTIPLE_VxLAN_TUNNELS: uninitialized parent entry {}", logicalTunnelIfaceName);
            return;
        }
        OperStatus newOperStatus = OperStatus.Down;
        List<InterfaceChildEntry> interfaceChildEntries = parentEntry.getInterfaceChildEntry();
        for (InterfaceChildEntry interfaceChildEntry : interfaceChildEntries) {
            String curChildInterface = interfaceChildEntry.getChildInterface();
            if (curChildInterface.equals(ifaceState.getName())) {
                if (ifaceState.getOperStatus() == OperStatus.Up) {
                    newOperStatus = OperStatus.Up;
                    break;
                }
                continue;
            }
            InterfaceInfo ifInfo = interfaceManager.getInterfaceInfoFromOperationalDSCache(curChildInterface);
            if (ifInfo != null && ifInfo.getOpState() == InterfaceInfo.InterfaceOpState.UP) {
                newOperStatus = OperStatus.Up;
                break;
            }
        }
        if (logicalTunnelIfaceName.equals(ifaceState.getName())) { //the current interface is logical tunnel itself
            if (ifaceState.getOperStatus() != newOperStatus) {
                updateInterfaceOperStatus(logicalTunnelIfaceName, ifaceState, newOperStatus, tx);
            }
        } else {
            InterfaceInfo ifLogicInfo = interfaceManager.getInterfaceInfoFromOperationalDSCache(logicalTunnelIfaceName);
            if (ifLogicInfo != null
                    && ((ifLogicInfo.getOpState() == InterfaceInfo.InterfaceOpState.UP
                            && newOperStatus == OperStatus.Down)
                    || (ifLogicInfo.getOpState() == InterfaceInfo.InterfaceOpState.DOWN
                            && newOperStatus == OperStatus.Up))) {
                InstanceIdentifier<Interface> id = ItmUtils.buildStateInterfaceId(logicalTunnelIfaceName);
                Optional<Interface> ifState = ItmUtils.read(LogicalDatastoreType.OPERATIONAL, id, broker);
                if (ifState.isPresent()) {
                    Interface ifStateLogicTunnel = ifState.get();
                    updateInterfaceOperStatus(logicalTunnelIfaceName, ifStateLogicTunnel, newOperStatus, tx);
                }
            }
        }
    }

    private static void updateInterfaceOperStatus(String ifaceName, Interface ifaceState,
                                                  OperStatus st, WriteTransaction tx) {
        LOG.debug("MULTIPLE_VxLAN_TUNNELS: update OperStatus to be {} for {}", st.toString(), ifaceName);
        InstanceIdentifier<Interface> idLogicGroup = ItmUtils.buildStateInterfaceId(ifaceName);
        InterfaceBuilder ifaceBuilderChild = new InterfaceBuilder(ifaceState);
        ifaceBuilderChild.setOperStatus(st);
        tx.merge(LogicalDatastoreType.OPERATIONAL, idLogicGroup, ifaceBuilderChild.build(), true);
    }

    private static void updateLogicalTunnelAdminStatus(String logicalTunnelName, Interface ifOrigin,
            Interface ifUpdated, InterfaceParentEntry parentEntry, WriteTransaction tx) {

        if (ifOrigin == null || ifUpdated == null || ifOrigin.getAdminStatus() == ifUpdated.getAdminStatus()) {
            return;
        }
        List<InterfaceChildEntry> interfaceChildEntries = parentEntry.getInterfaceChildEntry();
        for (InterfaceChildEntry interfaceChildEntry : interfaceChildEntries) {
            String curChildInterface = interfaceChildEntry.getChildInterface();
            updateInterfaceAdminStatus(curChildInterface, ifUpdated.getAdminStatus(), tx);
        }
    }

    private static void updateInterfaceAdminStatus(String logicalTunnelName, Interface ifState,
            WriteTransaction tx) {
        InterfaceInfo ifLogicTunnelInfo = interfaceManager.getInterfaceInfoFromOperationalDSCache(logicalTunnelName);
        if (ifLogicTunnelInfo == null) {
            return;
        }
        if (ifState.getAdminStatus() == AdminStatus.Up
                && ifLogicTunnelInfo.getAdminState() != InterfaceAdminState.ENABLED) {
            updateInterfaceAdminStatus(ifState.getName(), AdminStatus.Down, tx);
        }
    }

    private static void updateInterfaceAdminStatus(String ifaceName, AdminStatus st, WriteTransaction tx) {
        LOG.debug("MULTIPLE_VxLAN_TUNNELS: update AdminStatus to be {} for {}", st.toString(), ifaceName);
        InstanceIdentifier<Interface> id = ItmUtils.buildStateInterfaceId(ifaceName);
        InterfaceBuilder ifaceBuilderChild = new InterfaceBuilder();
        ifaceBuilderChild.setKey(new InterfaceKey(ifaceName));
        ifaceBuilderChild.setAdminStatus(st);
        tx.merge(LogicalDatastoreType.OPERATIONAL, id, ifaceBuilderChild.build(), true);
    }

    private static class TunnelAggregationUpdateWorker implements Callable<List<ListenableFuture<Void>>> {

        private final Interface ifStateOrigin;
        private final Interface ifStateUpdated;
        private final DataBroker dataBroker;
        private final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
                                                    .interfaces.rev140508.interfaces.Interface ifaceConfig;
        private final int ifaceAction;
        private final InterfaceParentEntry parentEntry;

        TunnelAggregationUpdateWorker(Interface ifStateOrig, Interface ifStateUpdated,
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508
                .interfaces.Interface iface, InterfaceParentEntry entry, int action, DataBroker broker) {
            this.ifStateOrigin = ifStateOrig;
            this.ifStateUpdated = ifStateUpdated;
            this.ifaceConfig = iface;
            this.ifaceAction = action;
            this.dataBroker  = broker;
            this.parentEntry = entry;
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
                LOG.debug("MULTIPLE_VxLAN_TUNNELS: not tunnel interface {}", ifaceConfig.getName());
                return futures;
            }
            WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
            if (ifTunnel.getTunnelInterfaceType().isAssignableFrom(TunnelTypeLogicalGroup.class)) {
                String logicTunnelIfaceName = ifStateUpdated.getName();
                InterfaceParentEntry parentEntry = getInterfaceParentEntry(logicTunnelIfaceName);
                updateLogicalTunnelGroupOperStatus(logicTunnelIfaceName, ifStateUpdated, parentEntry, dataBroker, tx);
                updateLogicalTunnelAdminStatus(logicTunnelIfaceName, ifStateOrigin, ifStateUpdated,
                                                    parentEntry, tx);
                futures.add(tx.submit());
                return futures;
            }
            if (!ifTunnel.getTunnelInterfaceType().isAssignableFrom(TunnelTypeVxlan.class)) {
                LOG.debug("MULTIPLE_VxLAN_TUNNELS: wrong tunnel type {}", ifTunnel.getTunnelInterfaceType());
                return futures;
            }
            ParentRefs parentRefs = ifaceConfig.getAugmentation(ParentRefs.class);
            if (parentRefs == null) {
                LOG.debug("MULTIPLE_VxLAN_TUNNELS: not updated parent ref for {}", ifaceConfig.getName());
                return futures;
            }
            String logicTunnelIfaceName = parentRefs.getParentInterface();
            InterfaceParentEntry groupEntry = getInterfaceParentEntry(logicTunnelIfaceName);
            if (ifaceAction == ADD_TUNNEL) {
                updateInterfaceAdminStatus(logicTunnelIfaceName, ifStateUpdated, tx);
                updateTunnelAggregationGroupBucket(ifStateUpdated, ifTunnel, parentRefs, groupEntry, ifaceAction, tx);
            } else if (ifaceAction == DEL_TUNNEL) {
                updateTunnelAggregationGroupBucket(ifStateUpdated, ifTunnel, parentRefs, groupEntry, ifaceAction, tx);
            }
            updateLogicalTunnelGroupOperStatus(logicTunnelIfaceName, ifStateUpdated, groupEntry, dataBroker, tx);
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
