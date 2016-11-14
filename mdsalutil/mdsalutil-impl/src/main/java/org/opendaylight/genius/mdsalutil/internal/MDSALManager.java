/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.mdsalutil.internal;

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
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.OptimisticLockFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.datastoreutils.AsyncClusteredDataChangeListenerBase;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.ActionType;
import org.opendaylight.genius.mdsalutil.FlowEntity;
import org.opendaylight.genius.mdsalutil.FlowInfoKey;
import org.opendaylight.genius.mdsalutil.GroupEntity;
import org.opendaylight.genius.mdsalutil.GroupInfoKey;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.infrautils.inject.AbstractLifecycle;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupId;
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
public class MDSALManager extends AbstractLifecycle implements IMdsalApiManager {
    private static final Logger LOG = LoggerFactory.getLogger(MDSALManager.class);
    private final DataBroker dataBroker;
    private final PacketProcessingService packetProcessingService;
    private final FlowListener flowListener = new FlowListener();
    private final GroupListener groupListener = new GroupListener();

    private final ConcurrentMap<FlowInfoKey, Runnable> flowMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<GroupInfoKey, Runnable> groupMap = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private static final long FIXED_DELAY_IN_MILLISECONDS = 5000;

    /**
     * Writes the flows and Groups to the MD SAL DataStore which will be sent to
     * the openflowplugin for installing flows/groups on the switch. Other
     * modules of VPN service that wants to install flows / groups on the switch
     * uses this utility.
     *
     * @param dataBroker
     *            broker to access the datastore
     * @param packetProcessingService
     *            service to send the packet outs
     */
    @Inject
    public MDSALManager(final DataBroker dataBroker, final PacketProcessingService packetProcessingService) {
        this.dataBroker = dataBroker;
        this.packetProcessingService = packetProcessingService;
    }

    @Override
    protected void start() throws Exception {
        LOG.info("{} start", getClass().getSimpleName());
        registerListener(dataBroker);
    }

    private void registerListener(DataBroker databroker) {
        flowListener.registerListener(LogicalDatastoreType.OPERATIONAL, databroker);
        groupListener.registerListener(LogicalDatastoreType.OPERATIONAL, databroker);
    }

    @Override
    protected void stop() throws Exception {
        LOG.info("{} stop", getClass().getSimpleName());
        flowListener.close();
        groupListener.close();
    }

    private Node buildDpnNode(BigInteger dpnId) {
        NodeId nodeId = new NodeId("openflow:" + dpnId);
        return new NodeBuilder().setId(nodeId).setKey(new NodeKey(nodeId)).build();
    }

    private BigInteger getDpnFromString(String dpnString) {
        String[] split = dpnString.split(":");
        return new BigInteger(split[1]);
    }

    @Override
    public void installFlow(FlowEntity flowEntity) {
        try {
            WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
            LOG.trace("InstallFlow for flowEntity {} ", flowEntity);

            writeFlowEntity(flowEntity, tx);

            CheckedFuture<Void, TransactionCommitFailedException> submitFuture = tx.submit();

            Futures.addCallback(submitFuture, new FutureCallback<Void>() {

                @Override
                public void onSuccess(final Void result) {
                    // Commited successfully
                    LOG.debug("Install Flow -- Committedsuccessfully ");
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    // Transaction failed

                    if (throwable instanceof OptimisticLockFailedException) {
                        // Failed because of concurrent transaction modifying
                        // same data
                        LOG.error("Install Flow -- Failed because of concurrent transaction modifying same data ");
                    } else {
                        // Some other type of TransactionCommitFailedException
                        LOG.error("Install Flow -- Some other type of TransactionCommitFailedException " + throwable);
                    }
                }
            });
        } catch (Exception e) {
            LOG.error("Could not install flow: {}", flowEntity, e);
        }
    }

    @Override
    public CheckedFuture<Void, TransactionCommitFailedException> installFlow(BigInteger dpId, FlowEntity flowEntity) {
        return installFlow(dpId, flowEntity.getFlowBuilder().build());
    }

    @Override
    public CheckedFuture<Void, TransactionCommitFailedException> installFlow(BigInteger dpId, Flow flow) {
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        writeFlow(dpId, flow, tx);
        return tx.submit();
    }

    @Override
    public void installGroup(GroupEntity groupEntity) {
        try {
            WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
            writeGroupEntity(groupEntity, tx);

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
                        // Failed because of concurrent transaction modifying
                        // same data
                        LOG.error("Install Group -- Failed because of concurrent transaction modifying same data ");
                    } else {
                        // Some other type of TransactionCommitFailedException
                        LOG.error("Install Group -- Some other type of TransactionCommitFailedException " + throwable);
                    }
                }
            });
        } catch (Exception e) {
            LOG.error("Could not install Group: {}", groupEntity, e);
            throw e;
        }
    }

    @Override
    public CheckedFuture<Void, TransactionCommitFailedException> removeFlow(BigInteger dpId, FlowEntity flowEntity) {
        return removeFlowNew(dpId, flowEntity.getFlowBuilder().build());
    }

    @Override
    public CheckedFuture<Void, TransactionCommitFailedException> removeFlow(BigInteger dpId, Flow flowEntity) {
        return removeFlowNew(dpId, flowEntity);
    }

    @Override
    public void removeFlow(FlowEntity flowEntity) {
        try {
            WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
            deleteFlowEntity(flowEntity, tx);

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
                        // Failed because of concurrent transaction modifying
                        // same data
                        LOG.error("Delete Flow -- Failed because of concurrent transaction modifying same data ");
                    } else {
                        // Some other type of TransactionCommitFailedException
                        LOG.error("Delete Flow -- Some other type of TransactionCommitFailedException " + throwable);
                    }
                }

            });
        } catch (Exception e) {
            LOG.error("Could not remove Flow: {}", flowEntity, e);
        }
    }

    @Override
    public void removeGroup(GroupEntity groupEntity) {
        try {
            WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
            removeGroupEntity(groupEntity, tx);

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
                        // Failed because of concurrent transaction modifying
                        // same data
                        LOG.error("Install Group -- Failed because of concurrent transaction modifying same data ");
                    } else {
                        // Some other type of TransactionCommitFailedException
                        LOG.error("Install Group -- Some other type of TransactionCommitFailedException " + throwable);
                    }
                }
            });
        } catch (Exception e) {
            LOG.error("Could not remove Group: {}", groupEntity, e);
        }
    }

    @Override
    public void modifyGroup(GroupEntity groupEntity) {
        installGroup(groupEntity);
    }

    @Override
    public void sendPacketOut(BigInteger dpnId, int groupId, byte[] payload) {
        List<ActionInfo> actionInfos = new ArrayList<>();
        actionInfos.add(new ActionInfo(ActionType.group, new String[] { String.valueOf(groupId) }));

        sendPacketOutWithActions(dpnId, groupId, payload, actionInfos);
    }

    @Override
    public void sendPacketOutWithActions(BigInteger dpnId, long groupId, byte[] payload, List<ActionInfo> actionInfos) {
        packetProcessingService.transmitPacket(
                MDSALUtil.getPacketOut(actionInfos, payload, dpnId, getNodeConnRef("openflow:" + dpnId, "0xfffffffd")));
    }

    @Override
    public void sendARPPacketOutWithActions(BigInteger dpnId, byte[] payload, List<ActionInfo> actions) {
        packetProcessingService.transmitPacket(
                MDSALUtil.getPacketOut(actions, payload, dpnId, getNodeConnRef("openflow:" + dpnId, "0xfffffffd")));
    }

    class GroupListener extends AsyncClusteredDataChangeListenerBase<Group, GroupListener> {

        public GroupListener() {
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
        protected ClusteredDataChangeListener getDataChangeListener() {
            return groupListener;
        }

        @Override
        protected DataChangeScope getDataChangeScope() {
            return DataChangeScope.SUBTREE;
        }
    }

    class FlowListener extends AsyncClusteredDataChangeListenerBase<Flow, FlowListener> {

        public FlowListener() {
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
        protected ClusteredDataChangeListener getDataChangeListener() {
            return FlowListener.this;
        }

        @Override
        protected DataChangeScope getDataChangeScope() {
            return DataChangeScope.SUBTREE;
        }
    }

    @Override
    public void addFlowToTx(FlowEntity flowEntity, WriteTransaction tx) {
        writeFlowEntity(flowEntity, tx);
    }

    @Override
    public void addFlowToTx(BigInteger dpId, Flow flow, WriteTransaction tx) {
        writeFlow(dpId, flow, tx);
    }

    @Override
    public void removeFlowToTx(BigInteger dpId, Flow flow, WriteTransaction tx) {
        deleteFlow(dpId, flow, tx);
    }

    @Override
    public void removeFlowToTx(FlowEntity flowEntity, WriteTransaction tx) {
        deleteFlowEntity(flowEntity, tx);
    }

    @Override
    public void addGroupToTx(GroupEntity groupEntity, WriteTransaction tx) {
        writeGroupEntity(groupEntity, tx);
    }

    @Override
    public void addGroupToTx(BigInteger dpId, Group group, WriteTransaction tx) {
        writeGroup(dpId, group, tx);
    }

    @Override
    public void removeGroupToTx(GroupEntity groupEntity, WriteTransaction tx) {
        removeGroupEntity(groupEntity, tx);
    }

    @Override
    public void removeGroupToTx(BigInteger dpId, Group group, WriteTransaction tx) {
        deleteGroup(dpId, group, tx);
    }

    @Override
    public void syncRemoveFlow(FlowEntity flowEntity, long delayTime) {
        syncSetUpFlow(flowEntity, delayTime, true);
    }

    @Override
    public void syncInstallFlow(FlowEntity flowEntity, long delayTime) {
        syncSetUpFlow(flowEntity, delayTime, false);
    }

    @Override
    public void syncInstallGroup(GroupEntity groupEntity, long delayTime) {
        syncSetUpGroup(groupEntity, delayTime, false);
    }

    @Override
    public void syncInstallGroup(BigInteger dpId, Group group, long delayTime) {
        syncSetUpGroup(dpId, group, delayTime, false);
    }

    @Override
    public void syncRemoveGroup(GroupEntity groupEntity) {
        syncSetUpGroup(groupEntity, FIXED_DELAY_IN_MILLISECONDS, true);
    }

    @Override
    public void syncRemoveGroup(BigInteger dpId, Group group) {
        syncSetUpGroup(dpId, group, FIXED_DELAY_IN_MILLISECONDS, true);
    }

    private void writeFlowEntity(FlowEntity flowEntity, WriteTransaction tx) {
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

    private void writeGroupEntity(GroupEntity groupEntity, WriteTransaction tx) {
        Group group = groupEntity.getGroupBuilder().build();

        Node nodeDpn = buildDpnNode(groupEntity.getDpnId());

        InstanceIdentifier<Group> groupInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Group.class, new GroupKey(new GroupId(groupEntity.getGroupId()))).build();

        tx.put(LogicalDatastoreType.CONFIGURATION, groupInstanceId, group, true);
    }

    private void writeGroup(BigInteger dpId, Group group, WriteTransaction tx) {

        Node nodeDpn = buildDpnNode(dpId);
        long groupId = group.getGroupId().getValue();
        InstanceIdentifier<Group> groupInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Group.class, new GroupKey(new GroupId(groupId))).build();

        tx.put(LogicalDatastoreType.CONFIGURATION, groupInstanceId, group, true);
    }

    private void deleteGroup(BigInteger dpId, Group group, WriteTransaction tx) {

        Node nodeDpn = buildDpnNode(dpId);
        long groupId = group.getGroupId().getValue();
        InstanceIdentifier<Group> groupInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Group.class, new GroupKey(new GroupId(groupId))).build();

        tx.delete(LogicalDatastoreType.CONFIGURATION, groupInstanceId);
    }

    private void writeFlow(BigInteger dpId, Flow flow, WriteTransaction tx) {
        FlowKey flowKey = new FlowKey(new FlowId(flow.getId()));
        Node nodeDpn = buildDpnNode(dpId);
        InstanceIdentifier<Flow> flowInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(flow.getTableId())).child(Flow.class, flowKey).build();
        tx.put(LogicalDatastoreType.CONFIGURATION, flowInstanceId, flow, true);
    }

    private void deleteFlowEntity(FlowEntity flowEntity, WriteTransaction tx) {
        Node nodeDpn = buildDpnNode(flowEntity.getDpnId());
        FlowKey flowKey = new FlowKey(new FlowId(flowEntity.getFlowId()));
        InstanceIdentifier<Flow> flowInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(flowEntity.getTableId())).child(Flow.class, flowKey).build();

        tx.delete(LogicalDatastoreType.CONFIGURATION, flowInstanceId);
    }

    private CheckedFuture<Void, TransactionCommitFailedException> removeFlowNew(BigInteger dpnId, Flow flowEntity) {
        LOG.debug("Remove flow {}", flowEntity);
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        deleteFlow(dpnId, flowEntity, tx);
        return tx.submit();
    }

    private void deleteFlow(BigInteger dpId, Flow flow, WriteTransaction tx) {
        Node nodeDpn = buildDpnNode(dpId);
        FlowKey flowKey = new FlowKey(flow.getId());
        InstanceIdentifier<Flow> flowInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(flow.getTableId())).child(Flow.class, flowKey).build();
        tx.delete(LogicalDatastoreType.CONFIGURATION, flowInstanceId);
    }

    private void removeGroupEntity(GroupEntity groupEntity, WriteTransaction tx) {
        Node nodeDpn = buildDpnNode(groupEntity.getDpnId());
        InstanceIdentifier<Group> groupInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Group.class, new GroupKey(new GroupId(groupEntity.getGroupId()))).build();

        tx.delete(LogicalDatastoreType.CONFIGURATION, groupInstanceId);
    }

    private static NodeConnectorRef getNodeConnRef(final String nodeIdName, final String port) {
        StringBuilder stringBuilder = new StringBuilder(nodeIdName);
        StringBuilder append = stringBuilder.append(":");
        StringBuilder sBuild = append.append(port);
        String string = sBuild.toString();
        NodeConnectorId nodeConnectorId = new NodeConnectorId(string);
        NodeConnectorKey nodeConnectorKey = new NodeConnectorKey(nodeConnectorId);
        NodeConnectorKey nConKey = nodeConnectorKey;
        InstanceIdentifierBuilder<Nodes> builder = InstanceIdentifier.builder(Nodes.class);
        NodeId nodeId = new NodeId(nodeIdName);
        NodeKey nodeKey = new NodeKey(nodeId);
        InstanceIdentifierBuilder<Node> child = builder.child(Node.class, nodeKey);
        InstanceIdentifier<NodeConnector> path = child.child(NodeConnector.class, nConKey).toInstance();

        return new NodeConnectorRef(path);
    }

    private void syncSetUpFlow(FlowEntity flowEntity, long delay, boolean isRemove) {
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

    private void syncSetUpGroup(GroupEntity groupEntity, long delayTime, boolean isRemove) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("syncSetUpGroup for groupEntity {} ", groupEntity);
        }
        Group group = groupEntity.getGroupBuilder().build();
        BigInteger dpId = groupEntity.getDpnId();
        Node nodeDpn = buildDpnNode(dpId);
        long groupId = groupEntity.getGroupId();
        GroupKey groupKey = new GroupKey(new GroupId(groupId));
        InstanceIdentifier<Group> groupInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class).child(Group.class, groupKey)
                .build();
        if (isRemove) {
            MDSALUtil.syncDelete(dataBroker, LogicalDatastoreType.CONFIGURATION, groupInstanceId);
        } else {
            MDSALUtil.syncWrite(dataBroker, LogicalDatastoreType.CONFIGURATION, groupInstanceId, group);
        }
    }

    private void syncSetUpGroup(BigInteger dpId, Group group, long delayTime, boolean isRemove) {
        LOG.trace("syncSetUpGroup for group {} ", group);
        Node nodeDpn = buildDpnNode(dpId);
        long groupId = group.getGroupId().getValue();
        GroupKey groupKey = new GroupKey(new GroupId(groupId));
        InstanceIdentifier<Group> groupInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class).child(Group.class, groupKey)
                .build();
        if (isRemove) {
            MDSALUtil.syncDelete(dataBroker, LogicalDatastoreType.CONFIGURATION, groupInstanceId);
        } else {
            MDSALUtil.syncWrite(dataBroker, LogicalDatastoreType.CONFIGURATION, groupInstanceId, group);
        }
    }
}
