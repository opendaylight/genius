/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.interfaces;

import com.google.common.util.concurrent.CheckedFuture;

import java.math.BigInteger;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.FlowEntity;
import org.opendaylight.genius.mdsalutil.GroupEntity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.Bucket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;

public interface IMdsalApiManager {

    CheckedFuture<Void,TransactionCommitFailedException> installFlow(FlowEntity flowEntity);

    CheckedFuture<Void,TransactionCommitFailedException> installFlow(BigInteger dpId, Flow flowEntity);

    CheckedFuture<Void,TransactionCommitFailedException> installFlow(BigInteger dpId, FlowEntity flowEntity);

    /**
     * Add a Flow to batched transaction.
     * This is used to batch multiple ConfigDS changes in a single transaction.
     *
     * @param flowEntity
     *            Flow being added
     * @param tx
     *            batched transaction
     */
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
     */
    void addFlowToTx(BigInteger dpId, Flow flow, WriteTransaction tx);

    void batchedAddFlow(BigInteger dpId, FlowEntity flowEntity);

    void batchedRemoveFlow(BigInteger dpId, FlowEntity flowEntity);

    CheckedFuture<Void,TransactionCommitFailedException> removeFlow(FlowEntity flowEntity);

    CheckedFuture<Void,TransactionCommitFailedException> removeFlow(BigInteger dpId, Flow flowEntity);

    CheckedFuture<Void,TransactionCommitFailedException> removeFlow(BigInteger dpId, FlowEntity flowEntity);

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
     */
    void removeFlowToTx(BigInteger dpId, Flow flow, WriteTransaction tx);

    /**
     *  Remove a Flow using batched transaction.
     *  This is used to batch multiple ConfigDS changes in a single transaction
     *
     * @param flowEntity
     *            Flow being removed
     * @param tx
     *            batched transaction
     */
    void removeFlowToTx(FlowEntity flowEntity, WriteTransaction tx);

    void installGroup(GroupEntity groupEntity);

    /**
     * Add a Group using batched transaction.
     * This is used to batch multiple ConfigDS changes in a single transaction
     *
     * @param groupEntity
     *            group being added
     * @param tx
     *            batched transaction
     */
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
     */
    void addGroupToTx(BigInteger dpId, Group group, WriteTransaction tx);

    void modifyGroup(GroupEntity groupEntity);

    void removeGroup(GroupEntity groupEntity);

    /**
     * Remove a Group using batched transaction
     * This is used to batch multiple ConfigDS changes in a single transaction.
     *
     * @param groupEntity
     *            group being removed
     * @param tx
     *            batched transaction
     */
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
     */
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
    boolean groupExists(BigInteger dpId, long groupId);

    void sendPacketOut(BigInteger dpnId, int groupId, byte[] payload);

    /**
     * Send packet out with actions.
     *
     * @deprecated Use {@link #sendPacketOutWithActions(BigInteger, byte[], List)}.
     */
    @Deprecated
    void sendPacketOutWithActions(BigInteger dpnId, long groupId, byte[] payload, List<ActionInfo> actionInfos);

    void sendPacketOutWithActions(BigInteger dpnId, byte[] payload, List<ActionInfo> actionInfos);

    void sendARPPacketOutWithActions(BigInteger dpnId, byte[] payload, List<ActionInfo> actionInfo);

    /**
     * API to remove the flow on Data Plane Node synchronously. It internally waits for
     * Flow Change Notification to confirm flow delete request is being sent with-in delayTime.
     *
     * @deprecated Use {@link #syncRemoveFlow(FlowEntity)}.
     */
    @Deprecated
    void syncRemoveFlow(FlowEntity flowEntity, long delayTime);

    void syncRemoveFlow(FlowEntity flowEntity);

    /**
     * Install a flow.
     *
     * @deprecated Use {@link #syncInstallFlow(FlowEntity)}.
     */
    @Deprecated
    void syncInstallFlow(FlowEntity flowEntity, long delayTime);

    void syncInstallFlow(FlowEntity flowEntity);

    /**
     * API to install the Group on Data Plane Node synchronously. It internally waits for
     * Group Change Notification to confirm group mod request is being sent with-in delayTime
     *
     * @deprecated Use {@link #syncInstallGroup(GroupEntity)}.
     */
    @Deprecated
    void syncInstallGroup(GroupEntity groupEntity, long delayTime);

    void syncInstallGroup(GroupEntity groupEntity);

    /**
     * Install a group.
     *
     * @deprecated Use {@link #syncInstallGroup(BigInteger, Group)}.
     */
    @Deprecated
    void syncInstallGroup(BigInteger dpId, Group group, long delayTime);

    void syncInstallGroup(BigInteger dpId, Group group);

    /**
     * API to remove the Group on Data Plane Node synchronously. It internally waits for
     * Group Change Notification to confirm group delete request is being sent.
     */
    void syncRemoveGroup(GroupEntity groupEntity);

    void syncRemoveGroup(BigInteger dpId, Group groupEntity);

    void addBucketToTx(BigInteger dpId, long groupId, Bucket bucket, WriteTransaction tx);

    void removeBucketToTx(BigInteger dpId, long groupId, long bucketId, WriteTransaction tx);
}
