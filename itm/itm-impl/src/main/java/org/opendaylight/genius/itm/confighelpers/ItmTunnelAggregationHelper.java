/*
 * Copyright (c) 2017 Hewlett Packard Enterprise, Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.confighelpers;

import static org.opendaylight.controller.md.sal.binding.api.WriteTransaction.CREATE_MISSING_PARENTS;
import static org.opendaylight.genius.infra.Datastore.CONFIGURATION;
import static org.opendaylight.genius.infra.Datastore.OPERATIONAL;
import static org.opendaylight.genius.itm.impl.ItmUtils.nullToEmpty;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.infra.Datastore.Configuration;
import org.opendaylight.genius.infra.Datastore.Operational;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.infra.TypedReadTransaction;
import org.opendaylight.genius.infra.TypedReadWriteTransaction;
import org.opendaylight.genius.infra.TypedWriteTransaction;
import org.opendaylight.genius.interfacemanager.globals.IfmConstants;
import org.opendaylight.genius.interfacemanager.globals.InterfaceInfo;
import org.opendaylight.genius.interfacemanager.globals.InterfaceInfo.InterfaceAdminState;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev170119.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
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
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
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
    private static boolean tunnelAggregationEnabled;

    private final IInterfaceManager interfaceManager;
    private final IMdsalApiManager mdsalManager;
    private final JobCoordinator jobCoordinator;

    @Inject
    public ItmTunnelAggregationHelper(final IInterfaceManager interfaceMngr,
                                      final IMdsalApiManager mdsalMngr,
                                      final ItmConfig itmConfig,
                                      final JobCoordinator jobCoordinator) {
        this.interfaceManager = interfaceMngr;
        this.mdsalManager = mdsalMngr;
        this.jobCoordinator = jobCoordinator;
        initTunnelAggregationConfig(itmConfig);
    }

    public static boolean isTunnelAggregationEnabled() {
        return tunnelAggregationEnabled;
    }

    public void createLogicalTunnelSelectGroup(TypedWriteTransaction<Configuration> tx,
        BigInteger srcDpnId, String interfaceName, int lportTag) {
        Group group = prepareLogicalTunnelSelectGroup(interfaceName, lportTag);
        LOG.debug("MULTIPLE_VxLAN_TUNNELS: group id {} installed for {} srcDpnId {}",
                group.getGroupId().getValue(), interfaceName, srcDpnId);
        mdsalManager.addGroup(tx, srcDpnId, group);
    }

    public void updateLogicalTunnelSelectGroup(InterfaceParentEntry entry, DataBroker broker) {
        String logicTunnelName = entry.getParentInterface();
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508
                    .interfaces.Interface ifaceConfig = ItmUtils.getInterface(logicTunnelName, interfaceManager);
        if (ifaceConfig == null || !ifaceConfig.getType().isAssignableFrom(Tunnel.class)) {
            return;
        }
        IfTunnel ifTunnel = ifaceConfig.augmentation(IfTunnel.class);
        if (!ifTunnel.getTunnelInterfaceType().isAssignableFrom(TunnelTypeLogicalGroup.class)) {
            return;
        }
        LOG.debug("MULTIPLE_VxLAN_TUNNELS: updateLogicalTunnelSelectGroup name {}", logicTunnelName);
        TunnelAggregationUpdateWorker worker =
                new TunnelAggregationUpdateWorker(null, null, ifaceConfig, entry, MOD_GROUP_TUNNEL, broker);
        jobCoordinator.enqueueJob(logicTunnelName, worker);
    }

    public void updateLogicalTunnelState(Interface ifaceState, int tunnelAction, DataBroker broker) {
        updateLogicalTunnelState(null, ifaceState, tunnelAction, broker);
    }

    public void updateLogicalTunnelState(Interface ifStateOrigin, Interface ifStateUpdated,
                                                int tunnelAction, DataBroker broker) {
        if (!tunnelAggregationEnabled || ifStateUpdated == null) {
            LOG.debug("MULTIPLE_VxLAN_TUNNELS: updateLogicalTunnelState - wrong configuration -"
                    + " tunnelAggregationEnabled {} ifStateUpdated {}", tunnelAggregationEnabled, ifStateUpdated);
            return;
        }
        String ifName = ifStateUpdated.getName();
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface iface =
                ItmUtils.getInterface(ifName, interfaceManager);
        IfTunnel ifTunnel = iface != null ? iface.augmentation(IfTunnel.class) : null;
        if (iface == null || ifTunnel == null) {
            LOG.debug("MULTIPLE_VxLAN_TUNNELS: updateLogicalTunnelState - not tunnel interface {}", ifName);
            return;
        }
        String logicTunnelName = null;
        if (ifTunnel.getTunnelInterfaceType().isAssignableFrom(TunnelTypeLogicalGroup.class)) {
            logicTunnelName = ifStateUpdated.getName();
        } else {
            ParentRefs parentRefs = iface.augmentation(ParentRefs.class);
            if (ifTunnel.getTunnelInterfaceType().isAssignableFrom(TunnelTypeVxlan.class) && parentRefs != null) {
                logicTunnelName = parentRefs.getParentInterface();
            }
        }
        if (logicTunnelName != null) {
            TunnelAggregationUpdateWorker worker =
                    new TunnelAggregationUpdateWorker(ifStateOrigin, ifStateUpdated, iface, null, tunnelAction, broker);
            jobCoordinator.enqueueJob(logicTunnelName, worker);
        }
    }

    private void initTunnelAggregationConfig(ItmConfig itmConfig) {
        // Load balancing of VxLAN feature is guarded by a global configuration option in the ITM,
        // only when the feature is enabled, the logical tunnel interfaces should be created.
        boolean tunnelAggregationConfigEnabled = false;
        List<TunnelAggregation> tunnelsConfig = itmConfig != null ? itmConfig.getTunnelAggregation() : null;
        if (tunnelsConfig != null) {
            for (TunnelAggregation tnlCfg : tunnelsConfig) {
                Class<? extends TunnelTypeBase> tunType = ItmUtils.getTunnelType(tnlCfg.key().getTunnelType());
                if (tunType.isAssignableFrom(TunnelTypeVxlan.class)) {
                    tunnelAggregationConfigEnabled = tnlCfg.isEnabled();
                    LOG.info("MULTIPLE_VxLAN_TUNNELS: tunnelAggregationEnabled {}", tunnelAggregationConfigEnabled);
                    break;
                }
            }
        }
        tunnelAggregationEnabled = tunnelAggregationConfigEnabled;
    }

    private Group prepareLogicalTunnelSelectGroup(String interfaceName, int lportTag) {
        long groupId = interfaceManager.getLogicalTunnelSelectGroupId(lportTag);
        return MDSALUtil.buildGroup(groupId, interfaceName, GroupTypes.GroupSelect,
                                    MDSALUtil.buildBucketLists(Collections.emptyList()));
    }

    private Bucket createBucket(String interfaceName, IfTunnel ifTunnel, int bucketId, int portNumber) {
        List<ActionInfo> listActionInfo = nullToEmpty(interfaceManager.getInterfaceEgressActions(interfaceName));
        if (listActionInfo.isEmpty()) {
            LOG.warn("MULTIPLE_VxLAN_TUNNELS: could not build Egress bucket for {}", interfaceName);
        }
        Integer portWeight = ifTunnel.getWeight() != null ? ifTunnel.getWeight() : DEFAULT_WEIGHT;
        return MDSALUtil.buildBucket(MDSALUtil.buildActions(listActionInfo), portWeight, bucketId,
                                     portNumber, MDSALUtil.WATCH_GROUP);
    }

    private void updateTunnelAggregationGroup(
        TypedWriteTransaction<Configuration> tx,
        InterfaceParentEntry parentEntry) {
        String logicTunnelName = parentEntry.getParentInterface();
        InternalTunnel logicInternalTunnel = ItmUtils.ITM_CACHE.getInternalTunnel(logicTunnelName);
        if (logicInternalTunnel == null) {
            LOG.debug("MULTIPLE_VxLAN_TUNNELS: {} not found in internal tunnels list", logicTunnelName);
            return;
        }
        InterfaceInfo ifLogicTunnel = interfaceManager.getInterfaceInfoFromOperationalDataStore(logicTunnelName);
        long groupId = ifLogicTunnel != null
                ? interfaceManager.getLogicalTunnelSelectGroupId(ifLogicTunnel.getInterfaceTag()) : INVALID_ID;
        BigInteger srcDpnId = logicInternalTunnel.getSourceDPN();
        List<Bucket> listBuckets = new ArrayList<>();
        List<InterfaceChildEntry> interfaceChildEntries = parentEntry.getInterfaceChildEntry();
        if (interfaceChildEntries == null || interfaceChildEntries.isEmpty()) {
            LOG.debug("MULTIPLE_VxLAN_TUNNELS: empty child list in group {}", parentEntry.getParentInterface());
            return;
        }
        for (InterfaceChildEntry interfaceChildEntry : interfaceChildEntries) {
            String curChildName = interfaceChildEntry.getChildInterface();
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface
                        childIface = ItmUtils.getInterface(curChildName, interfaceManager);
            IfTunnel ifTunnel = childIface != null ? childIface.augmentation(IfTunnel.class) : null;
            if (ifTunnel == null || !ifTunnel.getTunnelInterfaceType().isAssignableFrom(TunnelTypeVxlan.class)) {
                LOG.debug("MULTIPLE_VxLAN_TUNNELS: not tunnel interface {} found in group {}",
                        curChildName, logicTunnelName);
                continue;
            }
            ParentRefs parentRefs = childIface.augmentation(ParentRefs.class);
            if (parentRefs == null) {
                LOG.debug("MULTIPLE_VxLAN_TUNNELS: parent refs not specified for interface {} in group {}",
                        curChildName, logicTunnelName);
                continue;
            }
            InterfaceInfo ifInfo = interfaceManager.getInterfaceInfoFromOperationalDataStore(curChildName);
            if (ifInfo == null) {
                LOG.debug("MULTIPLE_VxLAN_TUNNELS: interface state not found for {} in groupId {}",
                        curChildName, groupId);
                continue;
            }
            int bucketId = interfaceChildEntries.indexOf(interfaceChildEntry);
            LOG.debug("MULTIPLE_VxLAN_TUNNELS: updateTunnelAggregationGroup - add bucketId {} to groupId {}",
                    bucketId, groupId);
            listBuckets.add(createBucket(curChildName, ifTunnel, bucketId, ifInfo.getPortNo()));
        }
        if (!listBuckets.isEmpty()) {
            Group group = MDSALUtil.buildGroup(groupId, logicTunnelName, GroupTypes.GroupSelect,
                                               MDSALUtil.buildBucketLists(listBuckets));
            mdsalManager.addGroup(tx, srcDpnId, group);
        }
    }

    private void updateTunnelAggregationGroupBucket(Interface ifaceState, IfTunnel ifTunnel,
                                                    ParentRefs parentRefs, InterfaceParentEntry groupParentEntry,
                                                    int action, TypedReadWriteTransaction<Configuration> tx)
            throws ExecutionException, InterruptedException {
        String logicTunnelName = parentRefs.getParentInterface();
        List<InterfaceChildEntry> interfaceChildEntries = groupParentEntry.getInterfaceChildEntry();
        if (interfaceChildEntries == null) {
            LOG.debug("MULTIPLE_VxLAN_TUNNELS: empty child list in group {}", groupParentEntry.getParentInterface());
            return;
        }
        String ifaceName = ifaceState.getName();
        InterfaceChildEntry childEntry = new InterfaceChildEntryBuilder().setChildInterface(ifaceName)
                .withKey(new InterfaceChildEntryKey(ifaceName)).build();
        int bucketId = interfaceChildEntries.indexOf(childEntry);
        if (bucketId == -1) {
            LOG.debug("MULTIPLE_VxLAN_TUNNELS: wrong child id for {} in group {}", ifaceName,
                    groupParentEntry.getParentInterface());
            return;
        }
        InterfaceInfo ifLogicTunnel = interfaceManager.getInterfaceInfoFromOperationalDataStore(logicTunnelName);
        long groupId = ifLogicTunnel != null
                ? interfaceManager.getLogicalTunnelSelectGroupId(ifLogicTunnel.getInterfaceTag()) : INVALID_ID;
        if (groupId == INVALID_ID) {
            LOG.warn("MULTIPLE_VxLAN_TUNNELS: unknown group id for logic tunnel {}", logicTunnelName);
            return;
        }
        String lowerLayerIf = ifaceState.getLowerLayerIf().get(0); // openflow:dpnid:portnum
        String[] split = lowerLayerIf.split(IfmConstants.OF_URI_SEPARATOR);
        BigInteger srcDpnId = new BigInteger(split[1]);
        int portNumber = Integer.parseInt(split[2]);
        if (action == ADD_TUNNEL) {
            if (!mdsalManager.groupExists(srcDpnId, groupId)) {
                createLogicalTunnelSelectGroup(tx, srcDpnId, logicTunnelName, ifLogicTunnel.getInterfaceTag());
            }
            Bucket buckt = createBucket(ifaceName, ifTunnel, bucketId, portNumber);
            LOG.debug("MULTIPLE_VxLAN_TUNNELS: add bucketId {} to groupId {}", bucketId, groupId);
            mdsalManager.addBucket(tx, srcDpnId, groupId, buckt);
        } else {
            LOG.debug("MULTIPLE_VxLAN_TUNNELS: remove bucketId {} from groupId {}", bucketId, groupId);
            mdsalManager.removeBucket(tx, srcDpnId, groupId, bucketId);
        }
    }

    private void updateLogicalTunnelGroupOperStatus(String logicalTunnelIfaceName, Interface ifaceState,
            InterfaceParentEntry parentEntry, TypedReadWriteTransaction<Operational> tx)
        throws ExecutionException, InterruptedException {
        if (parentEntry == null) {
            LOG.debug("MULTIPLE_VxLAN_TUNNELS: uninitialized parent entry {}", logicalTunnelIfaceName);
            return;
        }
        OperStatus newOperStatus = getAggregatedOperStatus(ifaceState, parentEntry);
        if (logicalTunnelIfaceName.equals(ifaceState.getName())) { //the current interface is logical tunnel itself
            if (ifaceState.getOperStatus() != newOperStatus) {
                updateInterfaceOperStatus(logicalTunnelIfaceName, ifaceState, newOperStatus, tx);
            }
        } else {
            InterfaceInfo ifLogicInfo =
                    interfaceManager.getInterfaceInfoFromOperationalDataStore(logicalTunnelIfaceName);
            if (isLogicalTunnelStateUpdateNeeded(newOperStatus, ifLogicInfo)) {
                InstanceIdentifier<Interface> id = ItmUtils.buildStateInterfaceId(logicalTunnelIfaceName);
                Optional<Interface> ifState = tx.read(id).get();
                if (ifState.isPresent()) {
                    Interface ifStateLogicTunnel = ifState.get();
                    updateInterfaceOperStatus(logicalTunnelIfaceName, ifStateLogicTunnel, newOperStatus, tx);
                }
            }
        }
    }

    private boolean isLogicalTunnelStateUpdateNeeded(OperStatus newOperStatus, InterfaceInfo ifLogicInfo) {
        return ifLogicInfo != null && (ifLogicInfo.getOpState() == InterfaceInfo.InterfaceOpState.UP
                && newOperStatus == OperStatus.Down
                || ifLogicInfo.getOpState() == InterfaceInfo.InterfaceOpState.DOWN && newOperStatus == OperStatus.Up);
    }

    private OperStatus getAggregatedOperStatus(Interface ifaceState, InterfaceParentEntry parentEntry) {
        String logicalTunnelName = parentEntry.getParentInterface();
        if (!Objects.equals(logicalTunnelName, ifaceState.getName()) && ifaceState.getOperStatus() == OperStatus.Up) {
            return OperStatus.Up;
        }

        List<InterfaceChildEntry> interfaceChildEntries = parentEntry.getInterfaceChildEntry();
        if (interfaceChildEntries == null || interfaceChildEntries.isEmpty()) {
            LOG.debug("MULTIPLE_VxLAN_TUNNELS: OperStatus is Down, because of the empty child list in group {}",
                    parentEntry.getParentInterface());
            return OperStatus.Down;
        }
        for (InterfaceChildEntry interfaceChildEntry : interfaceChildEntries) {
            String curChildInterface = interfaceChildEntry.getChildInterface();
            if (!Objects.equals(curChildInterface, ifaceState.getName())) {
                InterfaceInfo ifInfo = interfaceManager.getInterfaceInfoFromOperationalDataStore(curChildInterface);
                if (ifInfo != null && InterfaceInfo.InterfaceOpState.UP.equals(ifInfo.getOpState())) {
                    return OperStatus.Up;
                }
            }
        }
        return OperStatus.Down;
    }

    private void updateInterfaceOperStatus(String ifaceName, Interface ifaceState,
                                           OperStatus st, TypedWriteTransaction<Operational> tx) {
        LOG.debug("MULTIPLE_VxLAN_TUNNELS: update OperStatus to be {} for {}", st.toString(), ifaceName);
        InstanceIdentifier<Interface> idLogicGroup = ItmUtils.buildStateInterfaceId(ifaceName);
        InterfaceBuilder ifaceBuilderChild = new InterfaceBuilder(ifaceState);
        ifaceBuilderChild.setOperStatus(st);
        tx.merge(idLogicGroup, ifaceBuilderChild.build(), CREATE_MISSING_PARENTS);
    }

    private void updateLogicalTunnelAdminStatus(String logicalTunnelName, Interface ifOrigin,
            Interface ifUpdated, InterfaceParentEntry parentEntry, TypedWriteTransaction<Operational> tx) {

        if (ifOrigin == null || ifUpdated == null || ifOrigin.getAdminStatus() == ifUpdated.getAdminStatus()) {
            return;
        }
        List<InterfaceChildEntry> interfaceChildEntries = parentEntry.getInterfaceChildEntry();
        if (interfaceChildEntries == null || interfaceChildEntries.isEmpty()) {
            LOG.debug("MULTIPLE_VxLAN_TUNNELS: empty child list in group {}", logicalTunnelName);
            return;
        }
        for (InterfaceChildEntry interfaceChildEntry : interfaceChildEntries) {
            String curChildInterface = interfaceChildEntry.getChildInterface();
            updateInterfaceAdminStatus(curChildInterface, ifUpdated.getAdminStatus(), tx);
        }
    }

    private void updateInterfaceAdminStatus(String logicalTunnelName, Interface ifState,
        TypedWriteTransaction<Operational> tx) {
        InterfaceInfo ifLogicTunnelInfo = interfaceManager.getInterfaceInfoFromOperationalDataStore(logicalTunnelName);
        if (ifLogicTunnelInfo == null) {
            return;
        }
        if (ifState.getAdminStatus() == AdminStatus.Up
                && ifLogicTunnelInfo.getAdminState() != InterfaceAdminState.ENABLED) {
            updateInterfaceAdminStatus(ifState.getName(), AdminStatus.Down, tx);
        }
    }

    private void updateInterfaceAdminStatus(String ifaceName, AdminStatus st, TypedWriteTransaction<Operational> tx) {
        LOG.debug("MULTIPLE_VxLAN_TUNNELS: update AdminStatus to be {} for {}", st.toString(), ifaceName);
        InstanceIdentifier<Interface> id = ItmUtils.buildStateInterfaceId(ifaceName);
        InterfaceBuilder ifaceBuilderChild = new InterfaceBuilder();
        ifaceBuilderChild.withKey(new InterfaceKey(ifaceName));
        ifaceBuilderChild.setAdminStatus(st);
        tx.merge(id, ifaceBuilderChild.build(), CREATE_MISSING_PARENTS);
    }

    private class TunnelAggregationUpdateWorker implements Callable<List<ListenableFuture<Void>>> {

        private final Interface ifStateOrigin;
        private final Interface ifStateUpdated;
        private final ManagedNewTransactionRunner txRunner;
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
            this.txRunner = new ManagedNewTransactionRunnerImpl(broker);
            this.parentEntry = entry;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            List<ListenableFuture<Void>> futures = new ArrayList<>();
            futures.add(txRunner.callWithNewReadWriteTransactionAndSubmit(CONFIGURATION, confTx -> {
                if (ifaceAction == MOD_GROUP_TUNNEL) {
                    updateTunnelAggregationGroup(confTx, parentEntry);
                    return;
                }
                IfTunnel ifTunnel = ifaceConfig != null ? ifaceConfig.augmentation(IfTunnel.class) : null;
                if (ifTunnel == null) {
                    LOG.debug("MULTIPLE_VxLAN_TUNNELS: not tunnel interface {}", ifaceConfig.getName());
                    return;
                }
                if (ifTunnel.getTunnelInterfaceType().isAssignableFrom(TunnelTypeLogicalGroup.class)) {
                    String logicTunnelIfaceName = ifStateUpdated.getName();
                    futures.add(txRunner.callWithNewReadWriteTransactionAndSubmit(OPERATIONAL, operTx -> {
                        final InterfaceParentEntry interfaceParentEntry =
                            getInterfaceParentEntry(logicTunnelIfaceName, confTx);
                        updateLogicalTunnelGroupOperStatus(logicTunnelIfaceName, ifStateUpdated,
                            interfaceParentEntry,
                            operTx);
                        updateLogicalTunnelAdminStatus(logicTunnelIfaceName, ifStateOrigin, ifStateUpdated,
                            interfaceParentEntry, operTx);
                    }));
                    return;
                }
                if (!ifTunnel.getTunnelInterfaceType().isAssignableFrom(TunnelTypeVxlan.class)) {
                    LOG.debug("MULTIPLE_VxLAN_TUNNELS: wrong tunnel type {}", ifTunnel.getTunnelInterfaceType());
                    return;
                }
                ParentRefs parentRefs = ifaceConfig.augmentation(ParentRefs.class);
                if (parentRefs == null) {
                    LOG.debug("MULTIPLE_VxLAN_TUNNELS: not updated parent ref for {}", ifaceConfig.getName());
                    return;
                }
                String logicTunnelIfaceName = parentRefs.getParentInterface();
                InterfaceParentEntry groupEntry = getInterfaceParentEntry(logicTunnelIfaceName, confTx);
                if (groupEntry == null) {
                    LOG.debug("MULTIPLE_VxLAN_TUNNELS: not found InterfaceParentEntry for {}",
                        logicTunnelIfaceName);
                    return;
                }
                if (ifaceAction == ADD_TUNNEL) {
                    futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(OPERATIONAL,
                        operTx -> updateInterfaceAdminStatus(logicTunnelIfaceName, ifStateUpdated, operTx)));
                    updateTunnelAggregationGroupBucket(ifStateUpdated, ifTunnel, parentRefs, groupEntry,
                        ifaceAction,
                        confTx);
                } else if (ifaceAction == DEL_TUNNEL) {
                    updateTunnelAggregationGroupBucket(ifStateUpdated, ifTunnel, parentRefs, groupEntry,
                        ifaceAction,
                        confTx);
                }
                futures.add(txRunner.callWithNewReadWriteTransactionAndSubmit(OPERATIONAL,
                    operTx -> updateLogicalTunnelGroupOperStatus(logicTunnelIfaceName, ifStateUpdated, groupEntry,
                        operTx)));
            }));
            return futures;
        }

        private InterfaceParentEntry getInterfaceParentEntry(String logicalGroupName,
            TypedReadTransaction<Configuration> tx) throws ExecutionException, InterruptedException {
            InterfaceParentEntryKey interfaceParentEntryKey = new InterfaceParentEntryKey(logicalGroupName);
            InstanceIdentifier.InstanceIdentifierBuilder<InterfaceParentEntry> intfIdBuilder =
                    InstanceIdentifier.builder(InterfaceChildInfo.class)
                            .child(InterfaceParentEntry.class, interfaceParentEntryKey);
            InstanceIdentifier<InterfaceParentEntry> intfId = intfIdBuilder.build();
            return tx.read(intfId).get().orNull();
        }
    }
}
