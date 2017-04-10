/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.mdsalutil.internal;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.OptimisticLockFailedException;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.datastoreutils.AsyncClusteredDataTreeChangeListenerBase;
import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.FlowEntity;
import org.opendaylight.genius.mdsalutil.FlowInfoKey;
import org.opendaylight.genius.mdsalutil.GroupEntity;
import org.opendaylight.genius.mdsalutil.GroupInfoKey;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.actions.ActionGroup;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.infrautils.inject.AbstractLifecycle;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class MDSALManager extends AbstractLifecycle implements IMdsalApiManager {

    private static final long FIXED_DELAY_IN_MILLISECONDS = 5000;
    private static final Logger LOG = LoggerFactory.getLogger(MDSALManager.class);

    private final DataBroker dataBroker;

    private final PacketProcessingService packetProcessingService;
    private final ConcurrentMap<FlowInfoKey, Runnable> flowMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<GroupInfoKey, Runnable> groupMap = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final SingleTransactionDataBroker singleTxDb;

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
    @Inject
    public MDSALManager(DataBroker db, PacketProcessingService pktProcService) {
        this.dataBroker = db;
        this.packetProcessingService = pktProcService;
        singleTxDb = new SingleTransactionDataBroker(dataBroker);
        LOG.info("MDSAL Manager Initialized ");
    }

    @Override
    protected void start() throws Exception {
        LOG.info("{} start", getClass().getSimpleName());
        registerListener(dataBroker);
    }

    @Override
    protected void stop() throws Exception {
        LOG.info("{} stop", getClass().getSimpleName());
    }

    private void registerListener(DataBroker db) {
        FlowListener flowListener = new FlowListener();
        GroupListener groupListener = new GroupListener();
        FlowBatchingUtils.registerWithBatchManager(new MdSalUtilBatchHandler(), db);
        flowListener.registerListener(LogicalDatastoreType.OPERATIONAL, db);
        groupListener.registerListener(LogicalDatastoreType.OPERATIONAL, db);
    }

    private InstanceIdentifier<Group> getWildCardGroupPath() {
        return InstanceIdentifier.create(Nodes.class).child(Node.class).augmentation(FlowCapableNode.class)
                .child(Group.class);
    }

    private InstanceIdentifier<Flow> getWildCardFlowPath() {
        return InstanceIdentifier.create(Nodes.class).child(Node.class).augmentation(FlowCapableNode.class)
                .child(Table.class).child(Flow.class);
    }

    public CheckedFuture<Void, TransactionCommitFailedException> installFlowInternal(FlowEntity flowEntity) {
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        LOG.trace("InstallFlow for flowEntity {} ", flowEntity);

        writeFlowEntityInternal(flowEntity, tx);

        CheckedFuture<Void, TransactionCommitFailedException> submitFuture = tx.submit();

        Futures.addCallback(submitFuture, new FutureCallback<Void>() {

            @Override
            public void onSuccess(final Void result) {
                // Committed successfully
                LOG.debug("Install Flow -- Committedsuccessfully ");
            }

            @Override
            public void onFailure(final Throwable throwable) {
                // Transaction failed

                if (throwable instanceof OptimisticLockFailedException) {
                    // Failed because of concurrent transaction modifying same
                    // data
                    LOG.error("Install Flow -- Failed because of concurrent transaction modifying same data");
                } else {
                    // Some other type of TransactionCommitFailedException
                    LOG.error("Install Flow -- Some other type of TransactionCommitFailedException", throwable);
                }
            }
        });

        return submitFuture;
    }

    public CheckedFuture<Void, TransactionCommitFailedException> installFlowInternal(BigInteger dpId, Flow flow) {
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        writeFlowInternal(dpId, flow, tx);
        return tx.submit();
    }

    public void writeFlowEntityInternal(FlowEntity flowEntity, WriteTransaction tx) {
        if (flowEntity.getCookie() == null) {
            flowEntity.setCookie(new BigInteger("0110000", 16));
        }

        FlowKey flowKey = new FlowKey(new FlowId(flowEntity.getFlowId()));

        FlowBuilder flowbld = flowEntity.getFlowBuilder();

        Node nodeDpn = buildDpnNode(flowEntity.getDpnId());
        InstanceIdentifier<Flow> flowInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(flowEntity.getTableId())).child(Flow.class, flowKey).build();

        tx.put(LogicalDatastoreType.CONFIGURATION, flowInstanceId, flowbld.build(), true);
    }

    public void writeFlowInternal(BigInteger dpId, Flow flow, WriteTransaction tx) {
        FlowKey flowKey = new FlowKey(new FlowId(flow.getId()));
        Node nodeDpn = buildDpnNode(dpId);
        InstanceIdentifier<Flow> flowInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(flow.getTableId())).child(Flow.class, flowKey).build();
        tx.put(LogicalDatastoreType.CONFIGURATION, flowInstanceId, flow, true);
    }

    public void batchedAddFlowInternal(BigInteger dpId, Flow flow) {
        FlowKey flowKey = new FlowKey(new FlowId(flow.getId()));
        Node nodeDpn = buildDpnNode(dpId);
        InstanceIdentifier<Flow> flowInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(flow.getTableId())).child(Flow.class, flowKey).build();
        FlowBatchingUtils.write(flowInstanceId, flow);
    }

    public void batchedRemoveFlowInternal(BigInteger dpId, Flow flow) {
        FlowKey flowKey = new FlowKey(new FlowId(flow.getId()));
        Node nodeDpn = buildDpnNode(dpId);
        InstanceIdentifier<Flow> flowInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(flow.getTableId())).child(Flow.class, flowKey).build();
        FlowBatchingUtils.delete(flowInstanceId);
    }

    public CheckedFuture<Void, TransactionCommitFailedException> installGroupInternal(GroupEntity groupEntity) {
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        writeGroupEntityInternal(groupEntity, tx);

        CheckedFuture<Void, TransactionCommitFailedException> submitFuture = tx.submit();

        Futures.addCallback(submitFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                // Committed successfully
                LOG.debug("Install Group -- Committedsuccessfully ");
            }

            @Override
            public void onFailure(final Throwable throwable) {
                // Transaction failed

                if (throwable instanceof OptimisticLockFailedException) {
                    // Failed because of concurrent transaction modifying same
                    // data
                    LOG.error("Install Group -- Failed because of concurrent transaction modifying same data");
                } else {
                    // Some other type of TransactionCommitFailedException
                    LOG.error("Install Group -- Some other type of TransactionCommitFailedException", throwable);
                }
            }
        });

        return submitFuture;
    }

    public void writeGroupEntityInternal(GroupEntity groupEntity, WriteTransaction tx) {
        Group group = groupEntity.getGroupBuilder().build();

        Node nodeDpn = buildDpnNode(groupEntity.getDpnId());

        InstanceIdentifier<Group> groupInstanceId = buildGroupInstanceIdentifier(groupEntity.getGroupId(), nodeDpn);

        tx.put(LogicalDatastoreType.CONFIGURATION, groupInstanceId, group, true);
    }

    public void writeGroupInternal(BigInteger dpId, Group group, WriteTransaction tx) {

        Node nodeDpn = buildDpnNode(dpId);
        long groupId = group.getGroupId().getValue();
        InstanceIdentifier<Group> groupInstanceId = buildGroupInstanceIdentifier(groupId, nodeDpn);

        tx.put(LogicalDatastoreType.CONFIGURATION, groupInstanceId, group, true);
    }

    public void deleteGroupInternal(BigInteger dpId, Group group, WriteTransaction tx) {

        Node nodeDpn = buildDpnNode(dpId);
        long groupId = group.getGroupId().getValue();
        InstanceIdentifier<Group> groupInstanceId = buildGroupInstanceIdentifier(groupId, nodeDpn);
        Optional<Group>existingGroupId = MDSALUtil.read(dataBroker, LogicalDatastoreType.CONFIGURATION,
                groupInstanceId);
        if (existingGroupId.isPresent()) {
            tx.delete(LogicalDatastoreType.CONFIGURATION, groupInstanceId);
        }
    }

    public CheckedFuture<Void, TransactionCommitFailedException> removeFlowInternal(FlowEntity flowEntity) {
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        deleteFlowEntityInternal(flowEntity, tx);

        CheckedFuture<Void, TransactionCommitFailedException> submitFuture = tx.submit();

        Futures.addCallback(submitFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                // Committed successfully
                LOG.debug("Delete Flow -- Committedsuccessfully ");
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

        });

        return submitFuture;
    }

    public void deleteFlowEntityInternal(FlowEntity flowEntity, WriteTransaction tx) {
        Node nodeDpn = buildDpnNode(flowEntity.getDpnId());
        FlowKey flowKey = new FlowKey(new FlowId(flowEntity.getFlowId()));
        InstanceIdentifier<Flow> flowInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(flowEntity.getTableId())).child(Flow.class, flowKey).build();

        tx.delete(LogicalDatastoreType.CONFIGURATION, flowInstanceId);
    }

    public CheckedFuture<Void, TransactionCommitFailedException> removeFlowNewInternal(BigInteger dpnId,
            Flow flowEntity) {
        LOG.debug("Remove flow {}", flowEntity);
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        deleteFlowInternal(dpnId, flowEntity, tx);
        return tx.submit();
    }

    public void deleteFlowInternal(BigInteger dpId, Flow flow, WriteTransaction tx) {
        Node nodeDpn = buildDpnNode(dpId);
        FlowKey flowKey = new FlowKey(flow.getId());
        InstanceIdentifier<Flow> flowInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(flow.getTableId())).child(Flow.class, flowKey).build();
        tx.delete(LogicalDatastoreType.CONFIGURATION, flowInstanceId);
    }

    public CheckedFuture<Void, TransactionCommitFailedException> removeGroupInternal(GroupEntity groupEntity) {
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        removeGroupEntityInternal(groupEntity, tx);

        CheckedFuture<Void, TransactionCommitFailedException> submitFuture = tx.submit();

        Futures.addCallback(submitFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                // Committed successfully
                LOG.debug("Install Group -- Committedsuccessfully ");
            }

            @Override
            public void onFailure(final Throwable throwable) {
                // Transaction failed
                if (throwable instanceof OptimisticLockFailedException) {
                    // Failed because of concurrent transaction modifying same
                    // data
                    LOG.error("Install Group -- Failed because of concurrent transaction modifying same data");
                } else {
                    // Some other type of TransactionCommitFailedException
                    LOG.error("Install Group -- Some other type of TransactionCommitFailedException", throwable);
                }
            }
        });

        return submitFuture;
    }

    public void removeGroupEntityInternal(GroupEntity groupEntity, WriteTransaction tx) {
        Node nodeDpn = buildDpnNode(groupEntity.getDpnId());
        InstanceIdentifier<Group> groupInstanceId = buildGroupInstanceIdentifier(groupEntity.getGroupId(), nodeDpn);

        tx.delete(LogicalDatastoreType.CONFIGURATION, groupInstanceId);
    }

    public void modifyGroupInternal(GroupEntity groupEntity) {

        installGroup(groupEntity);
    }

    public void sendPacketOutInternal(BigInteger dpnId, int groupId, byte[] payload) {

        List<ActionInfo> actionInfos = new ArrayList<>();
        actionInfos.add(new ActionGroup(groupId));

        sendPacketOutWithActions(dpnId, groupId, payload, actionInfos);
    }

    public void sendPacketOutWithActionsInternal(BigInteger dpnId, long groupId, byte[] payload,
            List<ActionInfo> actionInfos) {

        packetProcessingService.transmitPacket(
                MDSALUtil.getPacketOut(actionInfos, payload, dpnId, getNodeConnRef("openflow:" + dpnId, "0xfffffffd")));
    }

    public void sendARPPacketOutWithActionsInternal(BigInteger dpnId, byte[] payload, List<ActionInfo> actions) {
        packetProcessingService.transmitPacket(
                MDSALUtil.getPacketOut(actions, payload, dpnId, getNodeConnRef("openflow:" + dpnId, "0xfffffffd")));
    }

    protected InstanceIdentifier<Node> nodeToInstanceId(Node node) {
        return InstanceIdentifier.builder(Nodes.class).child(Node.class, node.getKey()).toInstance();
    }

    protected static NodeConnectorRef getNodeConnRef(final String nodeId, final String port) {
        StringBuilder stringBuilder = new StringBuilder(nodeId);
        StringBuilder append = stringBuilder.append(":");
        StringBuilder build = append.append(port);
        String string = build.toString();
        NodeConnectorId nodeConnectorId = new NodeConnectorId(string);
        NodeConnectorKey nodeConnectorKey = new NodeConnectorKey(nodeConnectorId);
        NodeConnectorKey connectorKey = nodeConnectorKey;
        InstanceIdentifierBuilder<Nodes> builder = InstanceIdentifier.builder(Nodes.class);

        NodeKey nodeKey = new NodeKey(new NodeId(nodeId));
        InstanceIdentifierBuilder<Node> child = builder.child(Node.class, nodeKey);
        InstanceIdentifierBuilder<NodeConnector> anotherChild = child.child(NodeConnector.class, connectorKey);
        InstanceIdentifier<NodeConnector> path = anotherChild.toInstance();
        NodeConnectorRef nodeConnectorRef = new NodeConnectorRef(path);
        return nodeConnectorRef;
    }

    protected Node buildDpnNode(BigInteger dpnId) {
        NodeId nodeId = new NodeId("openflow:" + dpnId);
        Node nodeDpn = new NodeBuilder().setId(nodeId).setKey(new NodeKey(nodeId)).build();

        return nodeDpn;
    }

    public void syncSetUpFlowInternal(FlowEntity flowEntity, long delay, boolean isRemove) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("syncSetUpFlow for flowEntity {} ", flowEntity);
        }
        if (flowEntity.getCookie() == null) {
            flowEntity.setCookie(new BigInteger("0110000", 16));
        }
        Flow flow = flowEntity.getFlowBuilder().build();
        String flowId = flowEntity.getFlowId();
        BigInteger dpId = flowEntity.getDpnId();
        FlowKey flowKey = new FlowKey(new FlowId(flowId));
        Node nodeDpn = buildDpnNode(dpId);
        InstanceIdentifier<Flow> flowInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(flow.getTableId())).child(Flow.class, flowKey).build();
        if (isRemove) {
            MDSALUtil.syncDelete(dataBroker, LogicalDatastoreType.CONFIGURATION, flowInstanceId);
        } else {
            MDSALUtil.syncWrite(dataBroker, LogicalDatastoreType.CONFIGURATION, flowInstanceId, flow);
        }
    }

    public void syncSetUpGroupInternal(GroupEntity groupEntity, long delayTime, boolean isRemove) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("syncSetUpGroup for groupEntity {} ", groupEntity);
        }
        Group group = groupEntity.getGroupBuilder().build();
        BigInteger dpId = groupEntity.getDpnId();
        Node nodeDpn = buildDpnNode(dpId);
        long groupId = groupEntity.getGroupId();
        InstanceIdentifier<Group> groupInstanceId = buildGroupInstanceIdentifier(groupEntity.getGroupId(), nodeDpn);
        if (isRemove) {
            MDSALUtil.syncDelete(dataBroker, LogicalDatastoreType.CONFIGURATION, groupInstanceId);
        } else {
            MDSALUtil.syncWrite(dataBroker, LogicalDatastoreType.CONFIGURATION, groupInstanceId, group);
        }
    }

    public void syncSetUpGroupInternal(BigInteger dpId, Group group, long delayTime, boolean isRemove) {
        LOG.trace("syncSetUpGroup for group {} ", group);
        Node nodeDpn = buildDpnNode(dpId);
        long groupId = group.getGroupId().getValue();
        InstanceIdentifier<Group> groupInstanceId = buildGroupInstanceIdentifier(groupId, nodeDpn);
        if (isRemove) {
            MDSALUtil.syncDelete(dataBroker, LogicalDatastoreType.CONFIGURATION, groupInstanceId);
        } else {
            MDSALUtil.syncWrite(dataBroker, LogicalDatastoreType.CONFIGURATION, groupInstanceId, group);
        }
    }

    class GroupListener extends AsyncClusteredDataTreeChangeListenerBase<Group, GroupListener> {

        GroupListener() {
            super(Group.class, GroupListener.class);
        }

        @Override
        protected void remove(InstanceIdentifier<Group> identifier, Group del) {
            BigInteger dpId = getDpnFromString(identifier.firstKeyOf(Node.class, NodeKey.class).getId().getValue());
            executeNotifyTaskIfRequired(dpId, del);
        }

        private void executeNotifyTaskIfRequired(BigInteger dpId, Group group) {
            GroupInfoKey groupKey = new GroupInfoKey(dpId, group.getGroupId().getValue());
            Runnable notifyTask = groupMap.remove(groupKey);
            if (notifyTask == null) {
                return;
            }
            executorService.execute(notifyTask);
        }

        @Override
        protected void update(InstanceIdentifier<Group> identifier, Group original, Group update) {
            BigInteger dpId = getDpnFromString(identifier.firstKeyOf(Node.class, NodeKey.class).getId().getValue());
            executeNotifyTaskIfRequired(dpId, update);
        }

        @Override
        protected void add(InstanceIdentifier<Group> identifier, Group add) {
            BigInteger dpId = getDpnFromString(identifier.firstKeyOf(Node.class, NodeKey.class).getId().getValue());
            executeNotifyTaskIfRequired(dpId, add);
        }

        @Override
        protected InstanceIdentifier<Group> getWildCardPath() {
            return InstanceIdentifier.create(Nodes.class).child(Node.class).augmentation(FlowCapableNode.class)
                    .child(Group.class);
        }

        @Override
        protected GroupListener getDataTreeChangeListener() {
            return GroupListener.this;
        }
    }

    class FlowListener extends AsyncClusteredDataTreeChangeListenerBase<Flow, FlowListener> {

        FlowListener() {
            super(Flow.class, FlowListener.class);
        }

        @Override
        protected void remove(InstanceIdentifier<Flow> identifier, Flow del) {
            BigInteger dpId = getDpnFromString(identifier.firstKeyOf(Node.class, NodeKey.class).getId().getValue());
            notifyTaskIfRequired(dpId, del);
        }

        private void notifyTaskIfRequired(BigInteger dpId, Flow flow) {
            FlowInfoKey flowKey = new FlowInfoKey(dpId, flow.getTableId(), flow.getMatch(), flow.getId().getValue());
            Runnable notifyTask = flowMap.remove(flowKey);
            if (notifyTask == null) {
                return;
            }
            executorService.execute(notifyTask);
        }

        @Override
        protected void update(InstanceIdentifier<Flow> identifier, Flow original, Flow update) {
        }

        @Override
        protected void add(InstanceIdentifier<Flow> identifier, Flow add) {
            BigInteger dpId = getDpnFromString(identifier.firstKeyOf(Node.class, NodeKey.class).getId().getValue());
            notifyTaskIfRequired(dpId, add);
        }

        @Override
        protected InstanceIdentifier<Flow> getWildCardPath() {
            return InstanceIdentifier.create(Nodes.class).child(Node.class).augmentation(FlowCapableNode.class)
                    .child(Table.class).child(Flow.class);
        }

        @Override
        protected FlowListener getDataTreeChangeListener() {
            return FlowListener.this;
        }
    }

    private BigInteger getDpnFromString(String dpnString) {
        String[] split = dpnString.split(":");
        return new BigInteger(split[1]);
    }

    @Override
    public void installFlow(FlowEntity flowEntity) {
        installFlowInternal(flowEntity);
    }

    @Override
    public CheckedFuture<Void, TransactionCommitFailedException> installFlow(BigInteger dpId, Flow flowEntity) {
        return installFlowInternal(dpId, flowEntity);
    }

    @Override
    public CheckedFuture<Void, TransactionCommitFailedException> installFlow(BigInteger dpId, FlowEntity flowEntity) {
        return installFlowInternal(dpId, flowEntity.getFlowBuilder().build());
    }

    @Override
    public CheckedFuture<Void, TransactionCommitFailedException> removeFlow(BigInteger dpId, Flow flowEntity) {
        return removeFlowNewInternal(dpId, flowEntity);
    }

    @Override
    public CheckedFuture<Void, TransactionCommitFailedException> removeFlow(BigInteger dpId, FlowEntity flowEntity) {
        return removeFlowNewInternal(dpId, flowEntity.getFlowBuilder().build());
    }

    @Override
    public void removeFlow(FlowEntity flowEntity) {
        removeFlowInternal(flowEntity);
    }

    @Override
    public void installGroup(GroupEntity groupEntity) {
        installGroupInternal(groupEntity);
    }

    @Override
    public void modifyGroup(GroupEntity groupEntity) {
        modifyGroupInternal(groupEntity);
    }

    @Override
    public void removeGroup(GroupEntity groupEntity) {
        removeGroupInternal(groupEntity);
    }

    @Override
    public void sendPacketOut(BigInteger dpnId, int groupId, byte[] payload) {
        sendPacketOutInternal(dpnId, groupId, payload);
    }

    @Override
    public void sendPacketOutWithActions(BigInteger dpnId, long groupId, byte[] payload, List<ActionInfo> actionInfos) {
        sendPacketOutWithActionsInternal(dpnId, groupId, payload, actionInfos);
    }

    @Override
    public void sendARPPacketOutWithActions(BigInteger dpnId, byte[] payload, List<ActionInfo> actionInfo) {
        sendARPPacketOutWithActionsInternal(dpnId, payload, actionInfo);
    }

    @Override
    public void syncRemoveFlow(FlowEntity flowEntity, long delayTime) {
        syncSetUpFlowInternal(flowEntity, delayTime, true);
    }

    @Override
    public void syncInstallFlow(FlowEntity flowEntity, long delayTime) {
        syncSetUpFlowInternal(flowEntity, delayTime, false);
    }

    @Override
    public void syncInstallGroup(GroupEntity groupEntity, long delayTime) {
        syncSetUpGroupInternal(groupEntity, delayTime, false);
    }

    @Override
    public void syncInstallGroup(BigInteger dpId, Group group, long delayTime) {
        syncSetUpGroupInternal(dpId, group, delayTime, false);
    }

    @Override
    public void syncRemoveGroup(GroupEntity groupEntity) {
        syncSetUpGroupInternal(groupEntity, FIXED_DELAY_IN_MILLISECONDS, true);
    }

    @Override
    public void syncRemoveGroup(BigInteger dpId, Group group) {
        syncSetUpGroupInternal(dpId, group, FIXED_DELAY_IN_MILLISECONDS, true);
    }

    @Override
    public void addFlowToTx(FlowEntity flowEntity, WriteTransaction tx) {
        writeFlowEntityInternal(flowEntity, tx);
    }

    @Override
    public void addFlowToTx(BigInteger dpId, Flow flow, WriteTransaction tx) {
        writeFlowInternal(dpId, flow, tx);
    }

    @Override
    public void removeFlowToTx(BigInteger dpId, Flow flow, WriteTransaction tx) {
        deleteFlowInternal(dpId, flow, tx);
    }

    @Override
    public void removeFlowToTx(FlowEntity flowEntity, WriteTransaction tx) {
        deleteFlowEntityInternal(flowEntity, tx);
    }

    @Override
    public void addGroupToTx(GroupEntity groupEntity, WriteTransaction tx) {
        writeGroupEntityInternal(groupEntity, tx);
    }

    @Override
    public void addGroupToTx(BigInteger dpId, Group group, WriteTransaction tx) {
        writeGroupInternal(dpId, group, tx);
    }

    @Override
    public void removeGroupToTx(GroupEntity groupEntity, WriteTransaction tx) {
        removeGroupEntityInternal(groupEntity, tx);
    }

    @Override
    public void removeGroupToTx(BigInteger dpId, Group group, WriteTransaction tx) {
        deleteGroupInternal(dpId, group, tx);
    }

    @Override
    public void batchedAddFlow(BigInteger dpId, FlowEntity flowEntity) {
        batchedAddFlowInternal(dpId, flowEntity.getFlowBuilder().build());
    }

    @Override
    public void batchedRemoveFlow(BigInteger dpId, FlowEntity flowEntity) {
        batchedRemoveFlowInternal(dpId, flowEntity.getFlowBuilder().build());
    }

    @Override
    public void addBucketToTx(BigInteger dpId, long groupId, Bucket bucket, WriteTransaction tx) {
        addBucket(dpId, groupId, bucket, tx);
    }

    @Override
    public void removeBucketToTx(BigInteger dpId, long groupId, long bucketId, WriteTransaction tx) {
        deleteBucket(dpId, groupId, bucketId, tx);
    }

    public void deleteBucket(BigInteger dpId, long groupId, long bucketId, WriteTransaction tx) {
        Node nodeDpn = buildDpnNode(dpId);
        InstanceIdentifier<Bucket> bucketInstanceId = buildBucketInstanceIdentifier(groupId, bucketId, nodeDpn);

        if (groupExists(nodeDpn, groupId)) {
            tx.delete(LogicalDatastoreType.CONFIGURATION, bucketInstanceId);
        }
    }

    public void addBucket(BigInteger dpId, long groupId, Bucket bucket, WriteTransaction tx) {
        Node nodeDpn = buildDpnNode(dpId);
        InstanceIdentifier<Bucket> bucketInstanceId = buildBucketInstanceIdentifier(groupId,
                bucket.getBucketId().getValue(), nodeDpn);

        if (groupExists(nodeDpn, groupId)) {
            tx.put(LogicalDatastoreType.CONFIGURATION, bucketInstanceId, bucket);
        }
    }

    @Override
    public boolean groupExists(BigInteger dpId, long groupId) {
        return groupExists(buildDpnNode(dpId), groupId);
    }

    private  boolean groupExists(Node nodeDpn, long groupId) {
        InstanceIdentifier<Group> groupInstanceId = buildGroupInstanceIdentifier(groupId, nodeDpn);
        try {
            Optional<Group> groupOptional = singleTxDb.syncReadOptional(LogicalDatastoreType.CONFIGURATION,
                    groupInstanceId);
            return groupOptional.isPresent();
        } catch (ReadFailedException e) {
            LOG.warn("Exception while reading group {} for Node {}", nodeDpn.getKey(), groupId);
        }
        return false;
    }

    private InstanceIdentifier<Group> buildGroupInstanceIdentifier(long groupId, Node nodeDpn) {
        InstanceIdentifier<Group> groupInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Group.class, new GroupKey(new GroupId(groupId))).build();
        return groupInstanceId;
    }

    private InstanceIdentifier<Bucket> buildBucketInstanceIdentifier(long groupId, long bucketId,
            Node nodeDpn) {
        InstanceIdentifier<Bucket> bucketInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Group.class, new GroupKey(new GroupId(groupId)))
                .child(Buckets.class)
                .child(Bucket.class, new BucketKey(new BucketId(bucketId))).build();
        return bucketInstanceId;
    }
}
