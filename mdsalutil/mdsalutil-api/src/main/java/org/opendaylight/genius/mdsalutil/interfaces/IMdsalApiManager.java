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

    public void installFlow(FlowEntity flowEntity);

    /**
     * Add a Flow to Configuration DS using transaction
     *
     * @param flowEntity
     *            Flow Entity
     * @param tx
     *            transaction
     */
    public void addFlowToTx(FlowEntity flowEntity, WriteTransaction tx);

    public CheckedFuture<Void,TransactionCommitFailedException> installFlow(BigInteger dpId, Flow flowEntity);

    /**
     * Add a Flow to Configuration DS for specify DPN using transaction
     *
     * @param dpId
     *            dpn Id
     * @param flow
     *            Flow
     * @param tx
     *            transaction
     */
    public void addFlowToTx(BigInteger dpId, Flow flow, WriteTransaction tx);

    public CheckedFuture<Void,TransactionCommitFailedException> removeFlow(BigInteger dpId, Flow flowEntity);

    public CheckedFuture<Void,TransactionCommitFailedException> removeFlow(BigInteger dpId, FlowEntity flowEntity);

    /**
     * Remove a Flow from Configuration DS for specify DPN using transaction
     *
     * @param dpId
     *            dpn Id
     * @param flow
     *            Flow
     * @param tx
     *            transaction
     */
    public void removeFlowToTx(BigInteger dpId, Flow flow, WriteTransaction tx);

    public CheckedFuture<Void,TransactionCommitFailedException> installFlow(BigInteger dpId, FlowEntity flowEntity);

    public void removeFlow(FlowEntity flowEntity);

    /**
     * Remove a Flow from Configuration DS using tx
     *
     * @param flowEntity
     *            Flow Entity
     * @param tx
     *            transaction
     */
    public void removeFlowToTx(FlowEntity flowEntity, WriteTransaction tx);

    public void installGroup(GroupEntity groupEntity);

    /**
     * Add a Group to Configuration DS using tx
     *
     * @param groupEntity
     *            groupEntity
     * @param tx
     *            transaction
     */
    public void addGroupToTx(GroupEntity groupEntity, WriteTransaction tx);

    /**
     * Add a Group to Configuration DS for specify DPN using transaction
     *
     * @param dpId
     *            dpn Id
     * @param group
     *            group
     * @param tx
     *            transaction
     */
    public void addGroupToTx(BigInteger dpId, Group group, WriteTransaction tx);

    public void modifyGroup(GroupEntity groupEntity);

    public void removeGroup(GroupEntity groupEntity);

    /**
     * Remove a Group from Configuration DS by using transaction
     *
     * @param groupEntity
     *            groupEntity
     * @param tx
     *            transaction
     */
    public void removeGroupToTx(GroupEntity groupEntity, WriteTransaction tx);

    /**
     * Remove a Group from Configuration DS for specify DPN using transaction
     *
     * @param dpId
     *            dpn Id
     * @param group
     *            group
     * @param tx
     *            transaction
     */
    public void removeGroupToTx(BigInteger dpId, Group group, WriteTransaction tx);

    public void sendPacketOut(BigInteger dpnId, int groupId, byte[] payload);

    public void sendPacketOutWithActions(BigInteger dpnId, long groupId, byte[] payload, List<ActionInfo> actionInfos);

    public void sendARPPacketOutWithActions(BigInteger dpnId, byte[] payload, List<ActionInfo> action_info);

    /**
     * API to remove the flow on Data Plane Node synchronously. It internally waits for
     * Flow Change Notification to confirm flow delete request is being sent with-in delayTime.
     *
     * @param flowEntity
     * @param delayTime
     */
    public void syncRemoveFlow(FlowEntity flowEntity, long delayTime);
    public void syncInstallFlow(FlowEntity flowEntity, long delayTime);

    /**
     * API to install the Group on Data Plane Node synchronously. It internally waits for
     * Group Change Notification to confirm group mod request is being sent with-in delayTime
     *
     * @param groupEntity
     * @param delayTime
     */
    public void syncInstallGroup(GroupEntity groupEntity, long delayTime);

    public void syncInstallGroup(BigInteger dpId, Group group, long delayTime);

    /**
     * API to remove the Group on Data Plane Node synchronously. It internally waits for
     * Group Change Notification to confirm group delete request is being sent.
     *
     * @param groupEntity
     */
    public void syncRemoveGroup(GroupEntity groupEntity);

    public void syncRemoveGroup(BigInteger dpId, Group groupEntity);
}
