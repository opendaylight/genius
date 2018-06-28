/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.interfaces;

import com.google.common.util.concurrent.CheckedFuture;

import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.infra.Datastore.Configuration;
import org.opendaylight.genius.infra.TypedReadWriteTransaction;
import org.opendaylight.genius.infra.TypedWriteTransaction;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.FlowEntity;
import org.opendaylight.genius.mdsalutil.GroupEntity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.Bucket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;

public interface IMdsalApiManager {

    /**
     * Adds a flow.
     *
     * @deprecated Use {@link #addFlow(TypedWriteTransaction, FlowEntity)}.
     */
    @Deprecated
    CheckedFuture<Void,TransactionCommitFailedException> installFlow(FlowEntity flowEntity);

    /**
     * Adds a flow.
     *
     * @deprecated Use {@link #addFlow(TypedWriteTransaction, BigInteger, Flow)}.
     */
    @Deprecated
    CheckedFuture<Void,TransactionCommitFailedException> installFlow(BigInteger dpId, Flow flowEntity);

    /**
     * Adds a flow.
     *
     * @deprecated Use {@link #addFlow(TypedWriteTransaction, FlowEntity)}.
     */
    @Deprecated
    CheckedFuture<Void,TransactionCommitFailedException> installFlow(BigInteger dpId, FlowEntity flowEntity);

    /**
     * Add a Flow to batched transaction.
     * This is used to batch multiple ConfigDS changes in a single transaction.
     *
     * @param flowEntity
     *            Flow being added
     * @param tx
     *            batched transaction
     * @deprecated Use {@link #addFlow(TypedWriteTransaction, FlowEntity)}.
     */
    @Deprecated
    void addFlowToTx(FlowEntity flowEntity, WriteTransaction tx);

    /**
     * Add a Flow to batched transaction
     * This is used to batch multiple ConfigDS changes in a single transaction and programming on specific DPN.
     *
     * @param dpId
     *            dpn Id
     * @param flow
     *            Flow being added
     * @param tx
     *            batched transaction
     * @deprecated Use {@link #addFlow(TypedWriteTransaction, BigInteger, Flow)}.
     */
    @Deprecated
    void addFlowToTx(BigInteger dpId, Flow flow, WriteTransaction tx);

    /**
     * Adds the given flow.
     *
     * @param tx The transaction to use.
     * @param flowEntity The flow entity.
     */
    void addFlow(TypedWriteTransaction<Configuration> tx, FlowEntity flowEntity);

    /**
     * Adds the given flow.
     *
     * @param tx The transaction to use.
     * @param dpId The DPN identifier.
     * @param flow The flow.
     */
    void addFlow(TypedWriteTransaction<Configuration> tx, BigInteger dpId, Flow flow);

    /**
     * Adds a flow.
     *
     * @deprecated Use {@link #addFlow(TypedWriteTransaction, FlowEntity)}.
     */
    @Deprecated
    void batchedAddFlow(BigInteger dpId, FlowEntity flowEntity);

    /**
     * Adds a flow.
     *
     * @deprecated Use {@link #addFlow(TypedWriteTransaction, BigInteger, Flow)}.
     */
    @Deprecated
    void batchedAddFlow(BigInteger dpId, Flow flow);

    /**
     * Removes a flow.
     *
     * @deprecated Use {@link #removeFlow(TypedReadWriteTransaction, FlowEntity)}.
     */
    @Deprecated
    void batchedRemoveFlow(BigInteger dpId, FlowEntity flowEntity);

    /**
     * Removes a flow.
     *
     * @deprecated Use {@link #removeFlow(TypedReadWriteTransaction, BigInteger, Flow)}.
     */
    @Deprecated
    void batchedRemoveFlow(BigInteger dpId, Flow flow);

    /**
     * Removes a flow.
     *
     * @deprecated Use {@link #removeFlow(TypedReadWriteTransaction, BigInteger, String, short)}.
     */
    @Deprecated
    ListenableFuture<Void> removeFlow(BigInteger dpId, short tableId, FlowId flowId);

    /**
     * Removes a flow.
     *
     * @deprecated Use {@link #removeFlow(TypedReadWriteTransaction, FlowEntity)}.
     */
    @Deprecated
    CheckedFuture<Void,TransactionCommitFailedException> removeFlow(FlowEntity flowEntity);

    /**
     * Removes a flow.
     *
     * @deprecated Use {@link #removeFlow(TypedReadWriteTransaction, BigInteger, Flow)}.
     */
    @Deprecated
    CheckedFuture<Void,TransactionCommitFailedException> removeFlow(BigInteger dpId, Flow flowEntity);

    /**
     * Removes a flow.
     *
     * @deprecated Use {@link #removeFlow(TypedReadWriteTransaction, FlowEntity)}.
     */
    @Deprecated
    CheckedFuture<Void,TransactionCommitFailedException> removeFlow(BigInteger dpId, FlowEntity flowEntity);

    /**
     * Removes the given flow.
     *
     * @param tx The transaction to use.
     * @param dpId The DPN identifier.
     * @param flow The flow.
     */
    void removeFlow(TypedReadWriteTransaction<Configuration> tx, BigInteger dpId, Flow flow);

    /**
     * Removes the given flow.
     *
     * @param tx The transaction to use.
     * @param flowEntity The flow entity.
     */
    void removeFlow(TypedReadWriteTransaction<Configuration> tx, FlowEntity flowEntity);

    /**
     * Removes the given flow.
     *
     * @param tx The transaction to use.
     * @param dpId The DPN identifier.
     * @param flowKey The flow key.
     * @param tableId The table identifier.
     */
    void removeFlow(TypedReadWriteTransaction<Configuration> tx, BigInteger dpId, FlowKey flowKey, short tableId);

    /**
     * Removes the given flow.
     *
     * @param tx The transaction to use.
     * @param dpId The DPN identifier.
     * @param flowId The flow identifier.
     * @param tableId The table identifier.
     */
    void removeFlow(TypedReadWriteTransaction<Configuration> tx, BigInteger dpId, String flowId, short tableId);

    /**
     * Remove a Flow using batched transaction.
     * This is used to batch multiple ConfigDS changes in a single transaction for removing on specific DPN.
     *
     * @param dpId
     *            dpn Id
     * @param flow
     *            Flow being removed
     * @param tx
     *            batched transaction
     * @deprecated Use {@link #removeFlow(TypedReadWriteTransaction, BigInteger, Flow)}.
     */
    @Deprecated
    void removeFlowToTx(BigInteger dpId, Flow flow, WriteTransaction tx);

    /**
     *  Remove a Flow using batched transaction.
     *  This is used to batch multiple ConfigDS changes in a single transaction
     *
     * @param flowEntity
     *            Flow being removed
     * @param tx
     *            batched transaction
     * @deprecated Use {@link #removeFlow(TypedReadWriteTransaction, BigInteger, Flow)}.
     */
    @Deprecated
    void removeFlowToTx(FlowEntity flowEntity, WriteTransaction tx);

    /**
     * Adds a group.
     *
     * @deprecated Use {@link #addGroup(TypedWriteTransaction, GroupEntity)}.
     */
    @Deprecated
    void installGroup(GroupEntity groupEntity);

    /**
     * Add a Group using batched transaction.
     * This is used to batch multiple ConfigDS changes in a single transaction
     *
     * @param groupEntity
     *            group being added
     * @param tx
     *            batched transaction
     * @deprecated Use {@link #addGroup(TypedWriteTransaction, GroupEntity)}.
     */
    @Deprecated
    void addGroupToTx(GroupEntity groupEntity, WriteTransaction tx);

    /**
     * Add a Group using batched transaction
     * This is used to batch multiple ConfigDS changes in a single transaction and programming on specific DPN.
     *
     * @param dpId
     *            dpn Id
     * @param group
     *            group being added
     * @param tx
     *            batched transaction
     * @deprecated Use {@link #addGroup(TypedWriteTransaction, BigInteger, Group)}.
     */
    @Deprecated
    void addGroupToTx(BigInteger dpId, Group group, WriteTransaction tx);

    /**
     * Adds the given group using the given transaction.
     *
     * @param tx The transaction to use.
     * @param groupEntity The group to add.
     */
    void addGroup(TypedWriteTransaction<Configuration> tx, GroupEntity groupEntity);

    /**
     * Adds the given group using the given transaction.
     *
     * @param tx The transaction to use.
     * @param dpId The DPN identifier.
     * @param group The group to add.
     */
    void addGroup(TypedWriteTransaction<Configuration> tx, BigInteger dpId, Group group);

    @Deprecated
    void modifyGroup(GroupEntity groupEntity);

    /**
     * Remove a group.
     *
     * @deprecated Use {@link #removeGroup(TypedReadWriteTransaction, GroupEntity)}
     */
    @Deprecated
    void removeGroup(GroupEntity groupEntity);

    /**
     * Remove a group.
     *
     * @param dpnId The DP id.
     * @param groupId The group id.
     * @deprecated Use {@link #removeGroup(TypedReadWriteTransaction, BigInteger, long)}.
     */
    @Deprecated
    void removeGroup(BigInteger dpnId, long groupId);

    /**
     * Remove a group, using an existing transaction.
     *
     * @param dpnId The DP id.
     * @param groupId The group id.
     * @param tx The transaction.
     * @deprecated Use {@link #removeGroup(TypedReadWriteTransaction, BigInteger, long)}.
     */
    @Deprecated
    void removeGroup(BigInteger dpnId, long groupId, WriteTransaction tx);

    /**
     * Remove a group using the given transaction.
     *
     * @param tx The transaction to use.
     * @param groupEntity The group to remove.
     * @deprecated Use {@link #removeGroup(TypedReadWriteTransaction, GroupEntity)}.
     */
    void removeGroup(TypedReadWriteTransaction<Configuration> tx, GroupEntity groupEntity);

    /**
     * Remove a group using the given transaction.
     *
     * @param tx The transaction to use.
     * @param dpId The DPN identifier.
     * @param group The group to remove.
     */
    void removeGroup(TypedReadWriteTransaction<Configuration> tx, BigInteger dpId, Group group);

    /**
     * Remove a group using the given transaction.
     *
     * @param tx The transaction to use.
     * @param dpId The DPN identifier.
     * @param groupId The group identifier of the group to remove.
     */
    void removeGroup(TypedReadWriteTransaction<Configuration> tx, BigInteger dpId, long groupId);

    /**
     * Remove a Group using batched transaction
     * This is used to batch multiple ConfigDS changes in a single transaction.
     *
     * @param groupEntity
     *            group being removed
     * @param tx
     *            batched transaction
     *
     * @deprecated Use {@link #removeGroup(TypedReadWriteTransaction, GroupEntity)}
     */
    @Deprecated
    void removeGroupToTx(GroupEntity groupEntity, WriteTransaction tx);

    /**
     * Remove a Group using batched transaction
     * This is used to batch multiple ConfigDS changes in a single transaction.
     *
     * @param dpId
     *            dpn Id
     * @param group
     *            group being removed
     * @param tx
     *            batched transaction
     *
     * @deprecated Use {@link #removeGroup(TypedReadWriteTransaction, BigInteger, Group)}
     */
    @Deprecated
    void removeGroupToTx(BigInteger dpId, Group group, WriteTransaction tx);

    /**
     * Check if OF group exist on DPN.
     *
     * @param dpId
     *            dpn id
     * @param groupId
     *            OF group id
     * @return true if group exists and false otherwise
     */
    @Deprecated
    boolean groupExists(BigInteger dpId, long groupId);

    @Deprecated
    void sendPacketOut(BigInteger dpnId, int groupId, byte[] payload);

    /**
     * Send packet out with actions.
     *
     * @deprecated Use {@link #sendPacketOutWithActions(BigInteger, byte[], List)}.
     */
    @Deprecated
    void sendPacketOutWithActions(BigInteger dpnId, long groupId, byte[] payload, List<ActionInfo> actionInfos);

    @Deprecated
    void sendPacketOutWithActions(BigInteger dpnId, byte[] payload, List<ActionInfo> actionInfos);

    @Deprecated
    void sendARPPacketOutWithActions(BigInteger dpnId, byte[] payload, List<ActionInfo> actionInfo);

    /**
     * API to remove the flow on Data Plane Node synchronously. It internally waits for
     * Flow Change Notification to confirm flow delete request is being sent with-in delayTime.
     *
     * @deprecated Use {@link #removeFlow(TypedReadWriteTransaction, FlowEntity)}.
     */
    @Deprecated
    void syncRemoveFlow(FlowEntity flowEntity, long delayTime);

    /**
     * Removes a flow.
     *
     * @deprecated Use {@link #removeFlow(TypedReadWriteTransaction, FlowEntity)}.
     */
    @Deprecated
    void syncRemoveFlow(FlowEntity flowEntity);

    /**
     * Install a flow.
     *
     * @deprecated Use {@link #addFlow(TypedWriteTransaction, FlowEntity)}.
     */
    @Deprecated
    void syncInstallFlow(FlowEntity flowEntity, long delayTime);

    /**
     * Installs a flow.
     *
     * @deprecated Use {@link #addFlow(TypedWriteTransaction, FlowEntity)}.
     */
    @Deprecated
    void syncInstallFlow(FlowEntity flowEntity);

    /**
     * API to install the Group on Data Plane Node synchronously. It internally waits for
     * Group Change Notification to confirm group mod request is being sent with-in delayTime
     *
     * @deprecated Use {@link #addGroup(TypedWriteTransaction, GroupEntity)}.
     */
    @Deprecated
    void syncInstallGroup(GroupEntity groupEntity, long delayTime);

    /**
     * Installs a group.
     *
     * @deprecated Use {@link #addGroup(TypedWriteTransaction, GroupEntity)}.
     */
    @Deprecated
    void syncInstallGroup(GroupEntity groupEntity);

    /**
     * Install a group.
     *
     * @deprecated Use {@link #addGroup(TypedWriteTransaction, BigInteger, Group)}.
     */
    @Deprecated
    void syncInstallGroup(BigInteger dpId, Group group, long delayTime);

    /**
     * Installs a group.
     *
     * @deprecated Use {@link #addGroup(TypedWriteTransaction, BigInteger, Group)}.
     */
    @Deprecated
    void syncInstallGroup(BigInteger dpId, Group group);

    /**
     * API to remove the Group on Data Plane Node synchronously. It internally waits for
     * Group Change Notification to confirm group delete request is being sent.
     * @deprecated Use {@link #removeGroup(TypedReadWriteTransaction, GroupEntity)}.
     */
    @Deprecated
    void syncRemoveGroup(GroupEntity groupEntity);

    /**
     * Removes a group.
     *
     * @deprecated Use {@link #removeGroup(TypedReadWriteTransaction, GroupEntity)}.
     */
    @Deprecated
    void syncRemoveGroup(BigInteger dpId, Group groupEntity);

    @Deprecated
    void addBucketToTx(BigInteger dpId, long groupId, Bucket bucket, WriteTransaction tx);

    @Deprecated
    void removeBucketToTx(BigInteger dpId, long groupId, long bucketId, WriteTransaction tx);
}
