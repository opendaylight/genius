/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.internal;

import static org.opendaylight.infrautils.utils.concurrent.Executors.newListeningSingleThreadExecutor;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.genius.mdsalutil.FlowEntity;
import org.opendaylight.genius.mdsalutil.FlowInfoKey;
import org.opendaylight.genius.mdsalutil.GroupEntity;
import org.opendaylight.genius.mdsalutil.GroupInfoKey;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.infrautils.inject.AbstractLifecycle;
import org.opendaylight.infrautils.utils.concurrent.Executors;
import org.opendaylight.infrautils.utils.concurrent.NamedLocks;
import org.opendaylight.infrautils.utils.concurrent.NamedSimpleReentrantLock.Acquired;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.util.Datastore;
import org.opendaylight.mdsal.binding.util.Datastore.Configuration;
import org.opendaylight.mdsal.binding.util.RetryingManagedNewTransactionRunner;
import org.opendaylight.mdsal.binding.util.TypedReadTransaction;
import org.opendaylight.mdsal.binding.util.TypedReadWriteTransaction;
import org.opendaylight.mdsal.binding.util.TypedWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.OptimisticLockFailedException;
import org.opendaylight.serviceutils.tools.listener.AbstractClusteredAsyncDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.BucketId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.Buckets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.Bucket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.BucketKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class MDSALManager extends AbstractLifecycle implements IMdsalApiManager {
    private static final class FlowLockKey {
        private final Uint64 dpId;
        private final FlowKey flowKey;
        private final short tableId;

        FlowLockKey(Uint64 dpId, short tableId, FlowKey flowKey) {
            this.dpId = dpId;
            this.tableId = tableId;
            this.flowKey = flowKey;
        }

        @Override
        public int hashCode() {
            return 31 * Short.hashCode(tableId) + Objects.hash(dpId, flowKey);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof FlowLockKey)) {
                return false;
            }
            final FlowLockKey other = (FlowLockKey) obj;
            return tableId == other.tableId && Objects.equals(dpId, other.dpId)
                    && Objects.equals(flowKey, other.flowKey);
        }
    }

    private static final class GroupLockKey {
        private final Uint64 dpId;
        private final long groupId;

        GroupLockKey(long groupId, Uint64 dpId) {
            this.groupId = groupId;
            this.dpId = dpId;
        }

        @Override
        public int hashCode() {
            return 31 * Long.hashCode(groupId) + Objects.hashCode(dpId);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof GroupLockKey)) {
                return false;
            }
            final GroupLockKey other = (GroupLockKey) obj;
            return groupId == other.groupId && Objects.equals(dpId, other.dpId);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(MDSALManager.class);
    private static final NamedLocks<FlowLockKey> FLOW_LOCKS = new NamedLocks<>();
    private static final NamedLocks<GroupLockKey> GROUP_LOCKS = new NamedLocks<>();

    private final DataBroker dataBroker;
    private final RetryingManagedNewTransactionRunner txRunner;
    private final FlowBatchingUtils flowBatchingUtils = new FlowBatchingUtils();

    private final ConcurrentMap<FlowInfoKey, Runnable> flowMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<GroupInfoKey, Runnable> groupMap = new ConcurrentHashMap<>();
    private final ExecutorService executorService = newListeningSingleThreadExecutor("genius-MDSALManager", LOG);
    private final SingleTransactionDataBroker singleTxDb;
    private final FlowListener flowListener = new FlowListener();
    private final FlowConfigListener flowConfigListener = new FlowConfigListener();
    private final GroupListener groupListener = new GroupListener();

    /**
     * Writes the flows and Groups to the MD SAL DataStore which will be sent to
     * the openflowplugin for installing flows/groups on the switch. Other
     * modules of VPN service that wants to install flows / groups on the switch
     * uses this utility
     *
     * @param db
     *            dataBroker reference
     * @param pktProcService
     *            PacketProcessingService for sending the packet outs
     */
    @Deprecated
    public MDSALManager(DataBroker db, PacketProcessingService pktProcService) {
        this(db);
    }

    @Inject
    public MDSALManager(DataBroker db) {
        this.dataBroker = db;
        this.txRunner = new RetryingManagedNewTransactionRunner(db);
        singleTxDb = new SingleTransactionDataBroker(dataBroker);
        LOG.info("MDSAL Manager Initialized ");
    }

    @Override
    protected void start() {
        LOG.info("{} start", getClass().getSimpleName());

        int batchSize = Integer.getInteger("batch.size", 1000);
        int batchInterval = Integer.getInteger("batch.wait.time", 500);

        flowBatchingUtils.registerWithBatchManager(new MdSalUtilBatchHandler(dataBroker, batchSize, batchInterval));
    }

    @Override
    protected void stop() {
        LOG.info("{} stop", getClass().getSimpleName());

        flowListener.close();
        flowConfigListener.close();
        groupListener.close();
    }

    @VisibleForTesting
    FluentFuture<?> installFlowInternal(FlowEntity flowEntity) {
        return addCallBackForInstallFlowAndReturn(txRunner
            .callWithNewWriteOnlyTransactionAndSubmit(Datastore.CONFIGURATION,
                tx -> writeFlowEntityInternal(flowEntity, tx)));
    }

    private FluentFuture<?> installFlowInternal(Uint64 dpId, Flow flow) {
        return txRunner.callWithNewWriteOnlyTransactionAndSubmit(Datastore.CONFIGURATION,
            tx -> writeFlowInternal(dpId, flow, tx));
    }

    private static void writeFlowEntityInternal(FlowEntity flowEntity,
            TypedWriteTransaction<Datastore.Configuration> tx) {
        FlowKey flowKey = new FlowKey(new FlowId(flowEntity.getFlowId()));
        FlowBuilder flowbld = flowEntity.getFlowBuilder();
        InstanceIdentifier<Flow> flowInstanceId = buildFlowInstanceIdentifier(flowEntity.getDpnId(),
                flowEntity.getTableId(), flowKey);
        tx.mergeParentStructurePut(flowInstanceId, flowbld.build());
    }

    private static void writeFlowInternal(Uint64 dpId, Flow flow,
            TypedWriteTransaction<Datastore.Configuration> tx) {
        FlowKey flowKey = new FlowKey(new FlowId(flow.getId()));
        InstanceIdentifier<Flow> flowInstanceId = buildFlowInstanceIdentifier(dpId,
                                                            flow.getTableId().toJava(), flowKey);
        tx.mergeParentStructurePut(flowInstanceId, flow);
    }

    @VisibleForTesting
    FluentFuture<?> installGroupInternal(GroupEntity groupEntity) {
        return addCallBackForInstallGroupAndReturn(txRunner
            .callWithNewWriteOnlyTransactionAndSubmit(Datastore.CONFIGURATION,
                tx -> writeGroupEntityInternal(groupEntity, tx)));
    }

    private static void writeGroupEntityInternal(GroupEntity groupEntity,
            TypedWriteTransaction<Datastore.Configuration> tx) {
        Group group = groupEntity.getGroupBuilder().build();
        Node nodeDpn = buildDpnNode(groupEntity.getDpnId());
        InstanceIdentifier<Group> groupInstanceId = buildGroupInstanceIdentifier(groupEntity.getGroupId(), nodeDpn);
        tx.mergeParentStructurePut(groupInstanceId, group);
    }

    @VisibleForTesting
    FluentFuture<?> removeFlowInternal(FlowEntity flowEntity) {
        return addCallBackForDeleteFlowAndReturn(txRunner
                .callWithNewWriteOnlyTransactionAndSubmit(Datastore.CONFIGURATION,
                    tx -> deleteFlowEntityInternal(flowEntity, tx)));
    }

    private void deleteFlowEntityInternal(FlowEntity flowEntity, TypedWriteTransaction<Datastore.Configuration> tx) {
        Uint64 dpId = flowEntity.getDpnId();
        short tableId = flowEntity.getTableId();
        FlowKey flowKey = new FlowKey(new FlowId(flowEntity.getFlowId()));
        deleteFlow(dpId, tableId, flowKey, tx);
    }

    private void deleteFlow(Uint64 dpId, short tableId, FlowKey flowKey,
                            TypedWriteTransaction<Datastore.Configuration> tx) {
        if (flowExists(dpId, tableId, flowKey)) {
            InstanceIdentifier<Flow> flowInstanceId = buildFlowInstanceIdentifier(dpId, tableId, flowKey);
            tx.delete(flowInstanceId);
        } else {
            LOG.debug("Flow {} does not exist for dpn {}", flowKey, dpId);
        }
    }

    private FluentFuture<?> removeFlowNewInternal(Uint64 dpnId, Flow flowEntity) {
        LOG.debug("Remove flow {}", flowEntity);
        return txRunner.callWithNewWriteOnlyTransactionAndSubmit(Datastore.CONFIGURATION,
            tx -> {
                FlowKey flowKey = new FlowKey(flowEntity.getId());
                short tableId = flowEntity.getTableId().toJava();
                deleteFlow(dpnId, tableId, flowKey, tx);
            });
    }

    @VisibleForTesting
    FluentFuture<?> removeGroupInternal(Uint64 dpnId, long groupId) {
        return addCallBackForInstallGroupAndReturn(txRunner
            .callWithNewWriteOnlyTransactionAndSubmit(Datastore.CONFIGURATION,
                tx -> removeGroupInternal(dpnId, groupId, tx)));
    }

    private void removeGroupInternal(Uint64 dpnId, long groupId,
                                     TypedWriteTransaction<Datastore.Configuration> tx) {
        Node nodeDpn = buildDpnNode(dpnId);
        if (groupExists(nodeDpn, groupId)) {
            InstanceIdentifier<Group> groupInstanceId = buildGroupInstanceIdentifier(groupId, nodeDpn);
            tx.delete(groupInstanceId);
        } else {
            LOG.debug("Group {} does not exist for dpn {}", groupId, dpnId);
        }
    }

    private static Node buildDpnNode(Uint64 dpnId) {
        NodeId nodeId = new NodeId("openflow:" + dpnId);
        Node nodeDpn = new NodeBuilder().setId(nodeId).withKey(new NodeKey(nodeId)).build();

        return nodeDpn;
    }

    private void syncSetUpFlowInternal(FlowEntity flowEntity, boolean isRemove) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("syncSetUpFlow for flowEntity {} ", flowEntity);
        }
        Flow flow = flowEntity.getFlowBuilder().build();
        String flowId = flowEntity.getFlowId();
        short tableId = flowEntity.getTableId();
        Uint64 dpId = flowEntity.getDpnId();
        FlowKey flowKey = new FlowKey(new FlowId(flowId));
        InstanceIdentifier<Flow> flowInstanceId = buildFlowInstanceIdentifier(dpId, tableId, flowKey);

        if (isRemove) {
            try (Acquired lock = FLOW_LOCKS.acquire(new FlowLockKey(dpId, tableId, flowKey))) {
                if (flowExists(dpId, tableId, flowKey)) {
                    MDSALUtil.syncDelete(dataBroker, LogicalDatastoreType.CONFIGURATION, flowInstanceId);
                } else {
                    LOG.debug("Flow {} does not exist for dpn {}", flowKey, dpId);
                }
            }
        } else {
            MDSALUtil.syncWrite(dataBroker, LogicalDatastoreType.CONFIGURATION, flowInstanceId, flow);
        }
    }

    private void syncSetUpGroupInternal(GroupEntity groupEntity, boolean isRemove) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("syncSetUpGroup for groupEntity {} ", groupEntity);
        }
        Group group = groupEntity.getGroupBuilder().build();
        Uint64 dpId = groupEntity.getDpnId();
        long groupId = groupEntity.getGroupId();
        InstanceIdentifier<Group> groupInstanceId = buildGroupInstanceIdentifier(groupId, buildDpnNode(dpId));

        if (isRemove) {
            try (Acquired lock = GROUP_LOCKS.acquire(new GroupLockKey(groupId, dpId))) {
                if (groupExists(dpId, groupId)) {
                    MDSALUtil.syncDelete(dataBroker, LogicalDatastoreType.CONFIGURATION, groupInstanceId);
                } else {
                    LOG.debug("Group {} does not exist for dpn {}", groupId, dpId);
                }
            }
        } else {
            MDSALUtil.syncWrite(dataBroker, LogicalDatastoreType.CONFIGURATION, groupInstanceId, group);
        }
    }

    private class GroupListener extends AbstractClusteredAsyncDataTreeChangeListener<Group> {

        GroupListener() {
            super(dataBroker, LogicalDatastoreType.OPERATIONAL,InstanceIdentifier.create(Nodes.class).child(Node.class)
                    .augmentation(FlowCapableNode.class).child(Group.class),
                    Executors.newSingleThreadExecutor("GroupListener", LOG));
        }

        @Override
        public void remove(InstanceIdentifier<Group> identifier, Group del) {
            Uint64 dpId = getDpnFromString(identifier.firstKeyOf(Node.class).getId().getValue());
            executeNotifyTaskIfRequired(dpId, del);
        }

        private void executeNotifyTaskIfRequired(Uint64 dpId, Group group) {
            GroupInfoKey groupKey = new GroupInfoKey(dpId, group.getGroupId().getValue().toJava());
            Runnable notifyTask = groupMap.remove(groupKey);
            if (notifyTask == null) {
                return;
            }
            executorService.execute(notifyTask);
        }

        @Override
        public void update(InstanceIdentifier<Group> identifier, Group original, Group update) {
            Uint64 dpId = getDpnFromString(identifier.firstKeyOf(Node.class).getId().getValue());
            executeNotifyTaskIfRequired(dpId, update);
        }

        @Override
        public void add(InstanceIdentifier<Group> identifier, Group add) {
            Uint64 dpId = getDpnFromString(identifier.firstKeyOf(Node.class).getId().getValue());
            executeNotifyTaskIfRequired(dpId, add);
        }
    }

    private class FlowListener extends AbstractClusteredAsyncDataTreeChangeListener<Flow> {

        FlowListener() {
            super(dataBroker, LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(Nodes.class).child(Node.class)
                    .augmentation(FlowCapableNode.class).child(Table.class).child(Flow.class),
                    Executors.newSingleThreadExecutor("FlowListener", LOG));
        }

        @Override
        public void remove(InstanceIdentifier<Flow> identifier, Flow del) {
            Uint64 dpId = getDpnFromString(identifier.firstKeyOf(Node.class).getId().getValue());
            notifyTaskIfRequired(dpId, del);
        }

        private void notifyTaskIfRequired(Uint64 dpId, Flow flow) {
            FlowInfoKey flowKey = new FlowInfoKey(dpId, flow.getTableId().toJava(),
                                                  flow.getMatch(), flow.getId().getValue());
            Runnable notifyTask = flowMap.remove(flowKey);
            if (notifyTask == null) {
                return;
            }
            executorService.execute(notifyTask);
        }

        @Override
        public void update(InstanceIdentifier<Flow> identifier, Flow original, Flow update) {
        }

        @Override
        public void add(InstanceIdentifier<Flow> identifier, Flow add) {
            Uint64 dpId = getDpnFromString(identifier.firstKeyOf(Node.class).getId().getValue());
            notifyTaskIfRequired(dpId, add);
        }

    }

    private class FlowConfigListener extends AbstractClusteredAsyncDataTreeChangeListener<Flow> {
        private final Logger flowLog = LoggerFactory.getLogger(FlowConfigListener.class);

        FlowConfigListener() {
            super(dataBroker, LogicalDatastoreType.OPERATIONAL,InstanceIdentifier.create(Nodes.class).child(Node.class)
                    .augmentation(FlowCapableNode.class).child(Table.class).child(Flow.class),
                    Executors.newSingleThreadExecutor("FlowConfigListener", LOG));
        }

        @Override
        public void remove(InstanceIdentifier<Flow> identifier, Flow del) {
            Uint64 dpId = getDpnFromString(identifier.firstKeyOf(Node.class).getId().getValue());
            flowLog.trace("FlowId {} deleted from Table {} on DPN {}",
                del.getId().getValue(), del.getTableId(), dpId);
        }

        @Override
        public void update(InstanceIdentifier<Flow> identifier, Flow original, Flow update) {
        }

        @Override
        public void add(InstanceIdentifier<Flow> identifier, Flow add) {
            Uint64 dpId = getDpnFromString(identifier.firstKeyOf(Node.class).getId().getValue());
            flowLog.debug("FlowId {} added to Table {} on DPN {}",
                add.getId().getValue(), add.getTableId(), dpId);
        }
    }

    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "https://github.com/spotbugs/spotbugs/issues/811")
    private static Uint64 getDpnFromString(String dpnString) {
        String[] split = dpnString.split(":");
        return Uint64.valueOf(split[1]);
    }

    @Override
    public FluentFuture<?> installFlow(FlowEntity flowEntity) {
        return installFlowInternal(flowEntity);
    }

    @Override
    public FluentFuture<?> installFlow(Uint64 dpId, Flow flowEntity) {
        return installFlowInternal(dpId, flowEntity);
    }

    @Override
    public FluentFuture<?> installFlow(Uint64 dpId, FlowEntity flowEntity) {
        return installFlowInternal(dpId, flowEntity.getFlowBuilder().build());
    }

    @Override
    public ListenableFuture<?> removeFlow(Uint64 dpId, short tableId, FlowId flowId) {
        ListenableFuture<?> future = txRunner.callWithNewWriteOnlyTransactionAndSubmit(Datastore.CONFIGURATION,
            tx -> deleteFlow(dpId, tableId, new FlowKey(flowId), tx));

        Futures.addCallback(future, new FutureCallback<Object>() {
            @Override
            public void onSuccess(final Object result) {
                // Committed successfully
                LOG.debug("Delete Flow -- Committed successfully");
            }

            @Override
            public void onFailure(final Throwable throwable) {
                // Transaction failed
                if (throwable instanceof OptimisticLockFailedException) {
                    // Failed because of concurrent transaction modifying same
                    // data
                    LOG.error("Delete Flow -- Failed because of concurrent transaction modifying same data");
                } else {
                    // Some other type of TransactionCommitFailedException
                    LOG.error("Delete Flow -- Some other type of TransactionCommitFailedException", throwable);
                }
            }

        }, MoreExecutors.directExecutor());

        return future;
    }

    @Override
    public FluentFuture<?> removeFlow(Uint64 dpId, Flow flowEntity) {
        return removeFlowNewInternal(dpId, flowEntity);
    }

    @Override
    public FluentFuture<?> removeFlow(FlowEntity flowEntity) {
        return removeFlowInternal(flowEntity);
    }

    @Override
    public void removeFlow(TypedReadWriteTransaction<Configuration> tx, FlowEntity flowEntity)
            throws ExecutionException, InterruptedException {
        removeFlow(tx, flowEntity.getDpnId(), flowEntity.getFlowId(), flowEntity.getTableId());
    }

    @Override
    public void removeFlow(TypedReadWriteTransaction<Configuration> tx, Uint64 dpId, Flow flow)
            throws ExecutionException, InterruptedException {
        removeFlow(tx, dpId, flow.key(), flow.getTableId().toJava());
    }

    @Override
    public void removeFlow(TypedReadWriteTransaction<Configuration> tx, Uint64 dpId, String flowId, short tableId)
            throws ExecutionException, InterruptedException {
        removeFlow(tx, dpId, new FlowKey(new FlowId(flowId)), tableId);
    }

    @Override
    public void removeFlow(TypedReadWriteTransaction<Configuration> tx, Uint64 dpId, FlowKey flowKey,
            short tableId) throws ExecutionException, InterruptedException {
        InstanceIdentifier<Flow> flowInstanceIdentifier = buildFlowInstanceIdentifier(dpId, tableId, flowKey);
        if (tx.read(flowInstanceIdentifier).get().isPresent()) {
            tx.delete(flowInstanceIdentifier);
        }
    }

    @Override
    public void removeGroup(GroupEntity groupEntity) {
        removeGroupInternal(groupEntity.getDpnId(), groupEntity.getGroupId());
    }

    @Override
    public void removeGroup(TypedReadWriteTransaction<Configuration> tx, GroupEntity groupEntity)
            throws ExecutionException, InterruptedException {
        removeGroup(tx, groupEntity.getDpnId(), groupEntity.getGroupId());
    }

    @Override
    public void removeGroup(TypedReadWriteTransaction<Configuration> tx, Uint64 dpId, Group group)
            throws ExecutionException, InterruptedException {
        removeGroup(tx, dpId, group.getGroupId().getValue().toJava());
    }

    @Override
    public void removeGroup(TypedReadWriteTransaction<Configuration> tx, Uint64 dpId, long groupId)
            throws ExecutionException, InterruptedException {
        Node nodeDpn = buildDpnNode(dpId);
        InstanceIdentifier<Group> groupInstanceId = buildGroupInstanceIdentifier(groupId, nodeDpn);
        if (tx.read(groupInstanceId).get().isPresent()) {
            tx.delete(groupInstanceId);
        } else {
            LOG.debug("Group {} does not exist for dpn {}", groupId, dpId);
        }
    }

    @Override
    public void syncRemoveFlow(FlowEntity flowEntity, long delayTime) {
        syncSetUpFlowInternal(flowEntity, true);
    }

    @Override
    public void syncRemoveFlow(FlowEntity flowEntity) {
        syncSetUpFlowInternal(flowEntity, true);
    }

    @Override
    public void syncInstallFlow(FlowEntity flowEntity, long delayTime) {
        syncSetUpFlowInternal(flowEntity, false);
    }

    @Override
    public void syncInstallFlow(FlowEntity flowEntity) {
        syncSetUpFlowInternal(flowEntity, false);
    }

    @Override
    public void syncInstallGroup(GroupEntity groupEntity) {
        syncSetUpGroupInternal(groupEntity, false);
    }

    @Override
    public void syncRemoveGroup(GroupEntity groupEntity) {
        syncSetUpGroupInternal(groupEntity, true);
    }

    @Override
    public void addFlow(TypedWriteTransaction<Configuration> tx, FlowEntity flowEntity) {
        addFlow(tx, flowEntity.getDpnId(), flowEntity.getFlowBuilder().build());
    }

    @Override
    public void addFlow(TypedWriteTransaction<Configuration> tx, Uint64 dpId, Flow flow) {
        InstanceIdentifier<Flow> flowInstanceId = buildFlowInstanceIdentifier(dpId,
                                                            flow.getTableId().toJava(), flow.key());
        tx.mergeParentStructurePut(flowInstanceId, flow);
    }

    @Override
    public void addGroup(TypedWriteTransaction<Configuration> tx, GroupEntity groupEntity) {
        addGroup(tx, groupEntity.getDpnId(), groupEntity.getGroupBuilder().build());
    }

    @Override
    public void addGroup(TypedWriteTransaction<Configuration> tx, Uint64 dpId, Group group) {
        Node nodeDpn = buildDpnNode(dpId);
        long groupId = group.getGroupId().getValue().toJava();
        InstanceIdentifier<Group> groupInstanceId = buildGroupInstanceIdentifier(groupId, nodeDpn);
        tx.mergeParentStructurePut(groupInstanceId, group);
    }

    @Override
    public void addBucket(TypedReadWriteTransaction<Configuration> tx, Uint64 dpId, long groupId, Bucket bucket)
            throws ExecutionException, InterruptedException {
        Node nodeDpn = buildDpnNode(dpId);
        if (groupExists(tx, nodeDpn, groupId)) {
            InstanceIdentifier<Bucket> bucketInstanceId = buildBucketInstanceIdentifier(groupId,
                bucket.getBucketId().getValue().toJava(), nodeDpn);
            tx.put(bucketInstanceId, bucket);
        }
    }

    @Override
    public void removeBucket(TypedReadWriteTransaction<Configuration> tx, Uint64 dpId, long groupId, long bucketId)
            throws ExecutionException, InterruptedException {
        Node nodeDpn = buildDpnNode(dpId);
        if (groupExists(tx, nodeDpn, groupId)) {
            InstanceIdentifier<Bucket> bucketInstanceId = buildBucketInstanceIdentifier(groupId, bucketId, nodeDpn);
            tx.delete(bucketInstanceId);
        } else {
            LOG.debug("Group {} does not exist for dpn {}", groupId, dpId);
        }
    }

    @Override
    public boolean groupExists(Uint64 dpId, long groupId) {
        return groupExists(buildDpnNode(dpId), groupId);
    }

    private boolean groupExists(Node nodeDpn, long groupId) {
        InstanceIdentifier<Group> groupInstanceId = buildGroupInstanceIdentifier(groupId, nodeDpn);
        try {
            return singleTxDb.syncReadOptional(LogicalDatastoreType.CONFIGURATION, groupInstanceId).isPresent();
        } catch (ExecutionException | InterruptedException e) {
            LOG.warn("Exception while reading group {} for Node {}", groupId, nodeDpn.key());
        }
        return false;
    }

    private static boolean groupExists(TypedReadTransaction<Configuration> tx, Node nodeDpn, long groupId)
           throws ExecutionException, InterruptedException {
        return tx.exists(buildGroupInstanceIdentifier(groupId, nodeDpn)).get();
    }

    private static InstanceIdentifier<Group> buildGroupInstanceIdentifier(long groupId, Node nodeDpn) {
        InstanceIdentifier<Group> groupInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.key()).augmentation(FlowCapableNode.class)
                .child(Group.class, new GroupKey(new GroupId(groupId))).build();
        return groupInstanceId;
    }

    private boolean flowExists(Uint64 dpId, short tableId, FlowKey flowKey) {
        InstanceIdentifier<Flow> flowInstanceId = buildFlowInstanceIdentifier(dpId, tableId, flowKey);
        try {
            Optional<Flow> flowOptional = singleTxDb.syncReadOptional(LogicalDatastoreType.CONFIGURATION,
                    flowInstanceId);
            return flowOptional.isPresent();
        } catch (ExecutionException | InterruptedException e) {
            LOG.warn("Exception while reading flow {} for dpn {}", flowKey, dpId);
        }
        return false;
    }

    private static InstanceIdentifier<Flow> buildFlowInstanceIdentifier(Uint64 dpnId, short tableId,
            FlowKey flowKey) {
        InstanceIdentifier<Flow> flowInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, buildDpnNode(dpnId).key()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(tableId)).child(Flow.class, flowKey).build();
        return flowInstanceId;
    }

    private static InstanceIdentifier<Bucket> buildBucketInstanceIdentifier(long groupId, long bucketId,
            Node nodeDpn) {
        InstanceIdentifier<Bucket> bucketInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.key()).augmentation(FlowCapableNode.class)
                .child(Group.class, new GroupKey(new GroupId(groupId)))
                .child(Buckets.class)
                .child(Bucket.class, new BucketKey(new BucketId(bucketId))).build();
        return bucketInstanceId;
    }

    private static FluentFuture<?> addCallBackForDeleteFlowAndReturn(FluentFuture<?> fluentFuture) {
        return callBack(fluentFuture, "Delete Flow");
    }

    private static FluentFuture<?> addCallBackForInstallFlowAndReturn(FluentFuture<?> fluentFuture) {
        return callBack(fluentFuture, "Install Flow");
    }

    private static FluentFuture<?> addCallBackForInstallGroupAndReturn(FluentFuture<?> fluentFuture) {
        return callBack(fluentFuture, "Install Group");
    }

    // Generic for handling callbacks
    private static FluentFuture<?> callBack(FluentFuture<?> fluentFuture, String log) {
        fluentFuture.addCallback(new FutureCallback<Object>() {
            @Override
            public void onSuccess(final Object result) {
                // Committed successfully
                LOG.debug("{} -- Committedsuccessfully ", log);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                // Transaction failed

                if (throwable instanceof OptimisticLockFailedException) {
                    // Failed because of concurrent transaction modifying same
                    // data
                    LOG.error("{} -- Failed because of concurrent transaction modifying same data", log);
                } else {
                    // Some other type of TransactionCommitFailedException
                    LOG.error("{} -- Some other type of TransactionCommitFailedException",log, throwable);
                }
            }
        }, MoreExecutors.directExecutor());
        return fluentFuture;
    }
}
