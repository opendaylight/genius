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

public class MDSALManager implements AutoCloseable {

    private static final Logger s_logger = LoggerFactory.getLogger(MDSALManager.class);

    private DataBroker m_dataBroker;

    private PacketProcessingService m_packetProcessingService;
    private ConcurrentMap<FlowInfoKey, Runnable> flowMap = new ConcurrentHashMap<>();
    private ConcurrentMap<GroupInfoKey, Runnable> groupMap = new ConcurrentHashMap<> ();
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    /**
     * Writes the flows and Groups to the MD SAL DataStore
     * which will be sent to the openflowplugin for installing flows/groups on the switch.
     * Other modules of VPN service that wants to install flows / groups on the switch
     * uses this utility
     *
     * @param db - dataBroker reference
     * @param pktProcService- PacketProcessingService for sending the packet outs
     */
    public MDSALManager(final DataBroker db, PacketProcessingService pktProcService) {
        m_dataBroker = db;
        m_packetProcessingService = pktProcService;
        registerListener(db);
        s_logger.info( "MDSAL Manager Initialized ") ;
    }

    @Override
    public void close() throws Exception {
        s_logger.info("MDSAL Manager Closed");
    }

    private void registerListener(DataBroker db) {
        try {
            FlowListener flowListener = new FlowListener();
            GroupListener groupListener = new GroupListener();
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

    public void installFlow(FlowEntity flowEntity) {

        try {
            WriteTransaction tx = m_dataBroker.newWriteOnlyTransaction();
            s_logger.trace("InstallFlow for flowEntity {} ", flowEntity);

            writeFlowEntity(flowEntity, tx);

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
        } catch (Exception e) {
            s_logger.error("Could not install flow: {}", flowEntity, e);
        }
    }

    public void writeFlowEntity(FlowEntity flowEntity, WriteTransaction tx) {
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

    public CheckedFuture<Void,TransactionCommitFailedException> installFlow(BigInteger dpId, Flow flow) {
        WriteTransaction tx = m_dataBroker.newWriteOnlyTransaction();
        writeFlow(dpId, flow, tx);
        return tx.submit();
    }

    public void writeFlow(BigInteger dpId, Flow flow, WriteTransaction tx) {
        FlowKey flowKey = new FlowKey( new FlowId(flow.getId()) );
        Node nodeDpn = buildDpnNode(dpId);
        InstanceIdentifier<Flow> flowInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(flow.getTableId())).child(Flow.class,flowKey).build();
        tx.put(LogicalDatastoreType.CONFIGURATION, flowInstanceId, flow, true);
    }

    public void installGroup(GroupEntity groupEntity) {
        try {
            WriteTransaction tx = m_dataBroker.newWriteOnlyTransaction();
            writeGroupEntity(groupEntity, tx);

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
        } catch (Exception e) {
            s_logger.error("Could not install Group: {}", groupEntity, e);
            throw e;
        }
    }

    public void writeGroupEntity(GroupEntity groupEntity, WriteTransaction tx) {
        Group group = groupEntity.getGroupBuilder().build();

        Node nodeDpn = buildDpnNode(groupEntity.getDpnId());

        InstanceIdentifier<Group> groupInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Group.class, new GroupKey(new GroupId(groupEntity.getGroupId()))).build();

        tx.put(LogicalDatastoreType.CONFIGURATION, groupInstanceId, group, true);
    }

    public void writeGroup(BigInteger dpId, Group group, WriteTransaction tx) {

        Node nodeDpn = buildDpnNode(dpId);
        long groupId = group.getGroupId().getValue();
        InstanceIdentifier<Group> groupInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Group.class, new GroupKey(new GroupId(groupId))).build();

        tx.put(LogicalDatastoreType.CONFIGURATION, groupInstanceId, group, true);
    }

    public void deleteGroup(BigInteger dpId, Group group, WriteTransaction tx) {

        Node nodeDpn = buildDpnNode(dpId);
        long groupId = group.getGroupId().getValue();
        InstanceIdentifier<Group> groupInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Group.class, new GroupKey(new GroupId(groupId))).build();

        tx.delete(LogicalDatastoreType.CONFIGURATION, groupInstanceId);
    }


    public void removeFlow(FlowEntity flowEntity) {
        try {
            WriteTransaction tx = m_dataBroker.newWriteOnlyTransaction();
            deleteFlowEntity(flowEntity, tx);

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
        } catch (Exception e) {
            s_logger.error("Could not remove Flow: {}", flowEntity, e);
        }
    }

    public void deleteFlowEntity(FlowEntity flowEntity, WriteTransaction tx) {
        Node nodeDpn = buildDpnNode(flowEntity.getDpnId());
        FlowKey flowKey = new FlowKey(new FlowId(flowEntity.getFlowId()));
        InstanceIdentifier<Flow> flowInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(flowEntity.getTableId())).child(Flow.class, flowKey).build();


        tx.delete(LogicalDatastoreType.CONFIGURATION,flowInstanceId);
    }

    public CheckedFuture<Void,TransactionCommitFailedException> removeFlowNew(BigInteger dpnId, Flow flowEntity) {
        s_logger.debug("Remove flow {}",flowEntity);
        WriteTransaction  tx = m_dataBroker.newWriteOnlyTransaction();
        deleteFlow(dpnId, flowEntity, tx);
        return tx.submit();
    }

    public void deleteFlow(BigInteger dpId, Flow flow, WriteTransaction tx) {
        Node nodeDpn = buildDpnNode(dpId);
        FlowKey flowKey = new FlowKey(flow.getId());
        InstanceIdentifier<Flow> flowInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(flow.getTableId())).child(Flow.class, flowKey).build();
        tx.delete(LogicalDatastoreType.CONFIGURATION,flowInstanceId );
    }

    public void removeGroup(GroupEntity groupEntity) {
        try {
            WriteTransaction tx = m_dataBroker.newWriteOnlyTransaction();
            removeGroupEntity(groupEntity, tx);

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
        } catch (Exception e) {
            s_logger.error("Could not remove Group: {}", groupEntity, e);
        }
    }

    public void removeGroupEntity(GroupEntity groupEntity, WriteTransaction tx) {
        Node nodeDpn = buildDpnNode(groupEntity.getDpnId());
        InstanceIdentifier<Group> groupInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Group.class, new GroupKey(new GroupId(groupEntity.getGroupId()))).build();

        tx.delete(LogicalDatastoreType.CONFIGURATION,groupInstanceId );
    }

    public void modifyGroup(GroupEntity groupEntity) {

        installGroup(groupEntity);
    }

    public void sendPacketOut(BigInteger dpnId, int groupId, byte[] payload) {

        List<ActionInfo> actionInfos = new ArrayList<>();
        actionInfos.add(new ActionGroup(groupId));

        sendPacketOutWithActions(dpnId, groupId, payload, actionInfos);
    }

    public void sendPacketOutWithActions(BigInteger dpnId, long groupId, byte[] payload, List<ActionInfo> actionInfos) {

        m_packetProcessingService.transmitPacket(MDSALUtil.getPacketOut(actionInfos, payload, dpnId,
                getNodeConnRef("openflow:" + dpnId, "0xfffffffd")));
    }

    public void sendARPPacketOutWithActions(BigInteger dpnId, byte[] payload, List<ActionInfo> actions) {
        m_packetProcessingService.transmitPacket(MDSALUtil.getPacketOut(actions, payload, dpnId,
                getNodeConnRef("openflow:" + dpnId, "0xfffffffd")));
    }

    public InstanceIdentifier<Node> nodeToInstanceId(Node node) {
        return InstanceIdentifier.builder(Nodes.class).child(Node.class, node.getKey()).toInstance();
    }

    private static NodeConnectorRef getNodeConnRef(final String nodeId, final String port) {
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

    private Node buildDpnNode(BigInteger dpnId) {
        NodeId nodeId = new NodeId("openflow:" + dpnId);
        Node nodeDpn = new NodeBuilder().setId(nodeId).setKey(new NodeKey(nodeId)).build();

        return nodeDpn;
    }

    public void syncSetUpFlow(FlowEntity flowEntity, long delay, boolean isRemove) {
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

    public void syncSetUpGroup(GroupEntity groupEntity, long delayTime, boolean isRemove) {
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

    public void syncSetUpGroup(BigInteger dpId, Group group, long delayTime, boolean isRemove) {
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

}
