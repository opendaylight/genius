/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.interfaces;

import java.math.BigInteger;
import java.util.List;

import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.FlowEntity;
import org.opendaylight.genius.mdsalutil.GroupEntity;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;

public interface IMdsalApiManager {

    void installFlow(FlowEntity flowEntity);

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

    CheckedFuture<Void,TransactionCommitFailedException> installFlow(BigInteger dpId, Flow flowEntity);

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

    CheckedFuture<Void,TransactionCommitFailedException> installFlow(BigInteger dpId, FlowEntity flowEntity);

    void removeFlow(FlowEntity flowEntity);

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
     * Add a Group using batched transaction
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

    void sendPacketOut(BigInteger dpnId, int groupId, byte[] payload);

    void sendPacketOutWithActions(BigInteger dpnId, long groupId, byte[] payload, List<ActionInfo> actionInfos);

    void sendARPPacketOutWithActions(BigInteger dpnId, byte[] payload, List<ActionInfo> action_info);

    /**
     * API to remove the flow on Data Plane Node synchronously. It internally waits for
     * Flow Change Notification to confirm flow delete request is being sent with-in delayTime.
     *
     * @param flowEntity
     * @param delayTime
     */
    void syncRemoveFlow(FlowEntity flowEntity, long delayTime);
    void syncInstallFlow(FlowEntity flowEntity, long delayTime);

    /**
     * API to install the Group on Data Plane Node synchronously. It internally waits for
     * Group Change Notification to confirm group mod request is being sent with-in delayTime
     *
     * @param groupEntity
     * @param delayTime
     */
    void syncInstallGroup(GroupEntity groupEntity, long delayTime);

    void syncInstallGroup(BigInteger dpId, Group group, long delayTime);

    /**
     * API to remove the Group on Data Plane Node synchronously. It internally waits for
     * Group Change Notification to confirm group delete request is being sent.
     *
     * @param groupEntity
     */
    void syncRemoveGroup(GroupEntity groupEntity);

    void syncRemoveGroup(BigInteger dpId, Group groupEntity);
}
