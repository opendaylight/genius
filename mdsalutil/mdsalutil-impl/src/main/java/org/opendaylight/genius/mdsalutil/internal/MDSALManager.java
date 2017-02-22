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
public class MDSALManager extends AbstractLifecycle implements IMdsalApiManager{

    private static final long FIXED_DELAY_IN_MILLISECONDS = 5000;
    private static final Logger s_logger = LoggerFactory.getLogger(MDSALManager.class);

    private final DataBroker m_dataBroker;

    private final PacketProcessingService m_packetProcessingService;
    private final ConcurrentMap<FlowInfoKey, Runnable> flowMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<GroupInfoKey, Runnable> groupMap = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    /**
     * Writes the flows and Groups to the MD SAL DataStore
     * which will be sent to the openflowplugin for installing flows/groups on the switch.
     * Other modules of VPN service that wants to install flows / groups on the switch
     * uses this utility
     *
     * @param db - dataBroker reference
     * @param pktProcService- PacketProcessingService for sending the packet outs
     */
    @Inject
    public MDSALManager(DataBroker db, PacketProcessingService pktProcService) {
        m_dataBroker = db;
        m_packetProcessingService = pktProcService;
        s_logger.info( "MDSAL Manager Initialized ") ;
    }

    @Override
    protected void start() throws Exception {
        s_logger.info("{} start", getClass().getSimpleName());
        registerListener(m_dataBroker);
    }

    @Override
    protected void stop() throws Exception {
        s_logger.info("{} stop", getClass().getSimpleName());
    }

    private void registerListener(DataBroker db) {
        try {
            FlowListener flowListener = new FlowListener();
            GroupListener groupListener = new GroupListener();
            FlowBatchingUtils.registerWithBatchManager(new MdSalUtilBatchHandler(), db);
            flowListener.registerListener(LogicalDatastoreType.OPERATIONAL, db);
            groupListener.registerListener(LogicalDatastoreType.OPERATIONAL, db);
        } catch (final Exception e) {
            s_logger.error("GroupEventHandler: DataChange listener registration fail!", e);
            throw new IllegalStateException("GroupEventHandler: registration Listener failed.", e);
        }
    }

    private InstanceIdentifier<Group> getWildCardGroupPath() {
        return InstanceIdentifier.create(Nodes.class).child(Node.class).augmentation(FlowCapableNode.class).child(Group.class);
    }

    private InstanceIdentifier<Flow> getWildCardFlowPath() {
        return InstanceIdentifier.create(Nodes.class).child(Node.class).augmentation(FlowCapableNode.class).child(Table.class).child(Flow.class);
    }

    public CheckedFuture<Void, TransactionCommitFailedException> installFlowInternal(FlowEntity flowEntity) {
            WriteTransaction tx = m_dataBroker.newWriteOnlyTransaction();
            s_logger.trace("InstallFlow for flowEntity {} ", flowEntity);

            writeFlowEntityInternal(flowEntity, tx);

            CheckedFuture<Void,TransactionCommitFailedException> submitFuture  = tx.submit();

            Futures.addCallback(submitFuture, new FutureCallback<Void>() {

                @Override
                public void onSuccess(final Void result) {
                    // Commited successfully
                    s_logger.debug( "Install Flow -- Committedsuccessfully ") ;
                }

                @Override
                public void onFailure(final Throwable t) {
                    // Transaction failed

                    if(t instanceof OptimisticLockFailedException) {
                        // Failed because of concurrent transaction modifying same data
                        s_logger.error( "Install Flow -- Failed because of concurrent transaction modifying same data ") ;
                    } else {
                        // Some other type of TransactionCommitFailedException
                        s_logger.error( "Install Flow -- Some other type of TransactionCommitFailedException " + t) ;
                    }
                }
            });

            return submitFuture;
    }

    public void writeFlowEntityInternal(FlowEntity flowEntity, WriteTransaction tx) {
        if (flowEntity.getCookie() == null) {
            flowEntity.setCookie(new BigInteger("0110000", 16));
        }

        FlowKey flowKey = new FlowKey( new FlowId(flowEntity.getFlowId()) );

        FlowBuilder flowbld = flowEntity.getFlowBuilder();

        Node nodeDpn = buildDpnNode(flowEntity.getDpnId());
        InstanceIdentifier<Flow> flowInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(flowEntity.getTableId())).child(Flow.class,flowKey).build();

        tx.put(LogicalDatastoreType.CONFIGURATION, flowInstanceId, flowbld.build(),true );
    }

    public CheckedFuture<Void,TransactionCommitFailedException> installFlowInternal(BigInteger dpId, Flow flow) {
        WriteTransaction tx = m_dataBroker.newWriteOnlyTransaction();
        writeFlowInternal(dpId, flow, tx);
        return tx.submit();
    }

    public void writeFlowInternal(BigInteger dpId, Flow flow, WriteTransaction tx) {
        FlowKey flowKey = new FlowKey( new FlowId(flow.getId()) );
        Node nodeDpn = buildDpnNode(dpId);
        InstanceIdentifier<Flow> flowInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(flow.getTableId())).child(Flow.class,flowKey).build();
        tx.put(LogicalDatastoreType.CONFIGURATION, flowInstanceId, flow, true);
    }

    public void batchedAddFlowInternal(BigInteger dpId, Flow flow) {
        FlowKey flowKey = new FlowKey( new FlowId(flow.getId()) );
        Node nodeDpn = buildDpnNode(dpId);
        InstanceIdentifier<Flow> flowInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(flow.getTableId())).child(Flow.class,flowKey).build();
        FlowBatchingUtils.write(flowInstanceId, flow);
    }

    public void batchedRemoveFlowInternal(BigInteger dpId, Flow flow) {
        FlowKey flowKey = new FlowKey( new FlowId(flow.getId()) );
        Node nodeDpn = buildDpnNode(dpId);
        InstanceIdentifier<Flow> flowInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(flow.getTableId())).child(Flow.class,flowKey).build();
        FlowBatchingUtils.delete(flowInstanceId);
    }

    public CheckedFuture<Void, TransactionCommitFailedException> installGroupInternal(GroupEntity groupEntity) {
            WriteTransaction tx = m_dataBroker.newWriteOnlyTransaction();
            writeGroupEntityInternal(groupEntity, tx);

            CheckedFuture<Void,TransactionCommitFailedException> submitFuture  = tx.submit();

            Futures.addCallback(submitFuture, new FutureCallback<Void>() {
                @Override
                public void onSuccess(final Void result) {
                    // Commited successfully
                    s_logger.debug( "Install Group -- Committedsuccessfully ") ;
                }

                @Override
                public void onFailure(final Throwable t) {
                    // Transaction failed

                    if(t instanceof OptimisticLockFailedException) {
                        // Failed because of concurrent transaction modifying same data
                        s_logger.error( "Install Group -- Failed because of concurrent transaction modifying same data ") ;
                    } else {
                        // Some other type of TransactionCommitFailedException
                        s_logger.error( "Install Group -- Some other type of TransactionCommitFailedException " + t) ;
                    }
                }
            });

            return submitFuture;
    }

    public void writeGroupEntityInternal(GroupEntity groupEntity, WriteTransaction tx) {
        Group group = groupEntity.getGroupBuilder().build();

        Node nodeDpn = buildDpnNode(groupEntity.getDpnId());

        InstanceIdentifier<Group> groupInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Group.class, new GroupKey(new GroupId(groupEntity.getGroupId()))).build();

        tx.put(LogicalDatastoreType.CONFIGURATION, groupInstanceId, group, true);
    }

    public void writeGroupInternal(BigInteger dpId, Group group, WriteTransaction tx) {

        Node nodeDpn = buildDpnNode(dpId);
        long groupId = group.getGroupId().getValue();
        InstanceIdentifier<Group> groupInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Group.class, new GroupKey(new GroupId(groupId))).build();

        tx.put(LogicalDatastoreType.CONFIGURATION, groupInstanceId, group, true);
    }

    public void deleteGroupInternal(BigInteger dpId, Group group, WriteTransaction tx) {

        Node nodeDpn = buildDpnNode(dpId);
        long groupId = group.getGroupId().getValue();
        InstanceIdentifier<Group> groupInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Group.class, new GroupKey(new GroupId(groupId))).build();

        tx.delete(LogicalDatastoreType.CONFIGURATION, groupInstanceId);
    }

    public CheckedFuture<Void, TransactionCommitFailedException> removeFlowInternal(FlowEntity flowEntity) {
            WriteTransaction tx = m_dataBroker.newWriteOnlyTransaction();
            deleteFlowEntityInternal(flowEntity, tx);

            CheckedFuture<Void,TransactionCommitFailedException> submitFuture  = tx.submit();

            Futures.addCallback(submitFuture, new FutureCallback<Void>() {
                @Override
                public void onSuccess(final Void result) {
                    // Commited successfully
                    s_logger.debug( "Delete Flow -- Committedsuccessfully ") ;
                }

                @Override
                public void onFailure(final Throwable t) {
                    // Transaction failed
                    if(t instanceof OptimisticLockFailedException) {
                        // Failed because of concurrent transaction modifying same data
                        s_logger.error( "Delete Flow -- Failed because of concurrent transaction modifying same data ") ;
                    } else {
                        // Some other type of TransactionCommitFailedException
                        s_logger.error( "Delete Flow -- Some other type of TransactionCommitFailedException " + t) ;
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


        tx.delete(LogicalDatastoreType.CONFIGURATION,flowInstanceId);
    }

    public CheckedFuture<Void,TransactionCommitFailedException> removeFlowNewInternal(BigInteger dpnId, Flow flowEntity) {
        s_logger.debug("Remove flow {}",flowEntity);
        WriteTransaction  tx = m_dataBroker.newWriteOnlyTransaction();
        deleteFlowInternal(dpnId, flowEntity, tx);
        return tx.submit();
    }

    public void deleteFlowInternal(BigInteger dpId, Flow flow, WriteTransaction tx) {
        Node nodeDpn = buildDpnNode(dpId);
        FlowKey flowKey = new FlowKey(flow.getId());
        InstanceIdentifier<Flow> flowInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(flow.getTableId())).child(Flow.class, flowKey).build();
        tx.delete(LogicalDatastoreType.CONFIGURATION,flowInstanceId );
    }

    public CheckedFuture<Void, TransactionCommitFailedException> removeGroupInternal(GroupEntity groupEntity) {
            WriteTransaction tx = m_dataBroker.newWriteOnlyTransaction();
            removeGroupEntityInternal(groupEntity, tx);

            CheckedFuture<Void,TransactionCommitFailedException> submitFuture  = tx.submit();

            Futures.addCallback(submitFuture, new FutureCallback<Void>() {
                @Override
                public void onSuccess(final Void result) {
                    // Commited successfully
                    s_logger.debug( "Install Group -- Committedsuccessfully ") ;
                }

                @Override
                public void onFailure(final Throwable t) {
                    // Transaction failed
                    if(t instanceof OptimisticLockFailedException) {
                        // Failed because of concurrent transaction modifying same data
                        s_logger.error( "Install Group -- Failed because of concurrent transaction modifying same data ") ;
                    } else {
                        // Some other type of TransactionCommitFailedException
                        s_logger.error( "Install Group -- Some other type of TransactionCommitFailedException " + t) ;
                    }
                }
            });

            return submitFuture;
    }

    public void removeGroupEntityInternal(GroupEntity groupEntity, WriteTransaction tx) {
        Node nodeDpn = buildDpnNode(groupEntity.getDpnId());
        InstanceIdentifier<Group> groupInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Group.class, new GroupKey(new GroupId(groupEntity.getGroupId()))).build();

        tx.delete(LogicalDatastoreType.CONFIGURATION,groupInstanceId );
    }

    public void modifyGroupInternal(GroupEntity groupEntity) {

        installGroup(groupEntity);
    }

    public void sendPacketOutInternal(BigInteger dpnId, int groupId, byte[] payload) {

        List<ActionInfo> actionInfos = new ArrayList<>();
        actionInfos.add(new ActionGroup(groupId));

        sendPacketOutWithActions(dpnId, groupId, payload, actionInfos);
    }

    public void sendPacketOutWithActionsInternal(BigInteger dpnId, long groupId, byte[] payload, List<ActionInfo> actionInfos) {

        m_packetProcessingService.transmitPacket(MDSALUtil.getPacketOut(actionInfos, payload, dpnId,
                getNodeConnRef("openflow:" + dpnId, "0xfffffffd")));
    }

    public void sendARPPacketOutWithActionsInternal(BigInteger dpnId, byte[] payload, List<ActionInfo> actions) {
        m_packetProcessingService.transmitPacket(MDSALUtil.getPacketOut(actions, payload, dpnId,
                getNodeConnRef("openflow:" + dpnId, "0xfffffffd")));
    }

    protected InstanceIdentifier<Node> nodeToInstanceId(Node node) {
        return InstanceIdentifier.builder(Nodes.class).child(Node.class, node.getKey()).toInstance();
    }

    protected static NodeConnectorRef getNodeConnRef(final String nodeId, final String port) {
        StringBuilder _stringBuilder = new StringBuilder(nodeId);
        StringBuilder _append = _stringBuilder.append(":");
        StringBuilder sBuild = _append.append(port);
        String _string = sBuild.toString();
        NodeConnectorId _nodeConnectorId = new NodeConnectorId(_string);
        NodeConnectorKey _nodeConnectorKey = new NodeConnectorKey(_nodeConnectorId);
        NodeConnectorKey nConKey = _nodeConnectorKey;
        InstanceIdentifierBuilder<Nodes> _builder = InstanceIdentifier.builder(Nodes.class);
        NodeId _nodeId = new NodeId(nodeId);
        NodeKey _nodeKey = new NodeKey(_nodeId);
        InstanceIdentifierBuilder<Node> _child = _builder.child(Node.class, _nodeKey);
        InstanceIdentifierBuilder<NodeConnector> _child_1 = _child.child(
                NodeConnector.class, nConKey);
        InstanceIdentifier<NodeConnector> path = _child_1.toInstance();
        NodeConnectorRef _nodeConnectorRef = new NodeConnectorRef(path);
        return _nodeConnectorRef;
    }

    protected Node buildDpnNode(BigInteger dpnId) {
        NodeId nodeId = new NodeId("openflow:" + dpnId);
        Node nodeDpn = new NodeBuilder().setId(nodeId).setKey(new NodeKey(nodeId)).build();

        return nodeDpn;
    }

    public void syncSetUpFlowInternal(FlowEntity flowEntity, long delay, boolean isRemove) {
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("syncSetUpFlow for flowEntity {} ", flowEntity);
        }
        if (flowEntity.getCookie() == null) {
            flowEntity.setCookie(new BigInteger("0110000", 16));
        }
        Flow flow = flowEntity.getFlowBuilder().build();
        String flowId = flowEntity.getFlowId();
        BigInteger dpId = flowEntity.getDpnId();
        FlowKey flowKey = new FlowKey( new FlowId(flowId));
        Node nodeDpn = buildDpnNode(dpId);
        InstanceIdentifier<Flow> flowInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(flow.getTableId())).child(Flow.class, flowKey).build();
        if (isRemove) {
            MDSALUtil.syncDelete(m_dataBroker, LogicalDatastoreType.CONFIGURATION, flowInstanceId);
        } else {
            MDSALUtil.syncWrite(m_dataBroker, LogicalDatastoreType.CONFIGURATION, flowInstanceId, flow);
        }
    }

    public void syncSetUpGroupInternal(GroupEntity groupEntity, long delayTime, boolean isRemove) {
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("syncSetUpGroup for groupEntity {} ", groupEntity);
        }
        Group group = groupEntity.getGroupBuilder().build();
        BigInteger dpId = groupEntity.getDpnId();
        Node nodeDpn = buildDpnNode(dpId);
        long groupId = groupEntity.getGroupId();
        GroupKey groupKey = new GroupKey(new GroupId(groupId));
        InstanceIdentifier<Group> groupInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Group.class, groupKey).build();
        if (isRemove) {
            MDSALUtil.syncDelete(m_dataBroker, LogicalDatastoreType.CONFIGURATION, groupInstanceId);
        } else {
            MDSALUtil.syncWrite(m_dataBroker, LogicalDatastoreType.CONFIGURATION, groupInstanceId, group);
        }
    }

    public void syncSetUpGroupInternal(BigInteger dpId, Group group, long delayTime, boolean isRemove) {
        s_logger.trace("syncSetUpGroup for group {} ", group);
        Node nodeDpn = buildDpnNode(dpId);
        long groupId = group.getGroupId().getValue();
        GroupKey groupKey = new GroupKey(new GroupId(groupId));
        InstanceIdentifier<Group> groupInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Group.class, groupKey).build();
        if (isRemove) {
            MDSALUtil.syncDelete(m_dataBroker, LogicalDatastoreType.CONFIGURATION, groupInstanceId);
        } else {
            MDSALUtil.syncWrite(m_dataBroker, LogicalDatastoreType.CONFIGURATION, groupInstanceId, group);
        }
    }

    class GroupListener extends AsyncClusteredDataChangeListenerBase<Group,GroupListener> {

        public GroupListener() {
            super(Group.class,GroupListener.class);
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
            return InstanceIdentifier.create(Nodes.class).child(Node.class).augmentation(FlowCapableNode.class).child(Group.class);
        }

        @Override
        protected ClusteredDataChangeListener getDataChangeListener() {
            return GroupListener.this;
        }

        @Override
        protected DataChangeScope getDataChangeScope() {
            return DataChangeScope.SUBTREE;
        }
    }

    class FlowListener extends AsyncClusteredDataChangeListenerBase<Flow,FlowListener> {

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
            return InstanceIdentifier.create(Nodes.class).child(Node.class).augmentation(FlowCapableNode.class).child(Table.class).child(Flow.class);
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
    public void sendPacketOutWithActions(BigInteger dpnId, long groupId,
                                         byte[] payload, List<ActionInfo> actionInfos) {
        sendPacketOutWithActionsInternal(dpnId, groupId, payload, actionInfos);
    }


    @Override
    public void sendARPPacketOutWithActions(BigInteger dpnId, byte[] payload,
                                            List<ActionInfo> actionInfo) {
        sendARPPacketOutWithActionsInternal(dpnId, payload, actionInfo);
    }

    @Override
    public void syncRemoveFlow(FlowEntity flowEntity, long delayTime) {
        syncSetUpFlowInternal(flowEntity,  delayTime, true);
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
}
