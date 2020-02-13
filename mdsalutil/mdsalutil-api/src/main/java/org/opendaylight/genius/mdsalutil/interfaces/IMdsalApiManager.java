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
import java.util.concurrent.ExecutionException;
import org.opendaylight.genius.infra.Datastore.Configuration;
import org.opendaylight.genius.infra.TypedReadWriteTransaction;
import org.opendaylight.genius.infra.TypedWriteTransaction;
import org.opendaylight.genius.mdsalutil.FlowEntity;
import org.opendaylight.genius.mdsalutil.GroupEntity;
import org.opendaylight.mdsal.common.api.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.Bucket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yangtools.yang.common.Uint64;

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
     * @deprecated Use {@link #addFlow(TypedWriteTransaction, Uint64, Flow)}.
     */
    @Deprecated
    CheckedFuture<Void,TransactionCommitFailedException> installFlow(Uint64 dpId, Flow flowEntity);

    /**
     * Adds a flow.
     *
     * @deprecated Use {@link #addFlow(TypedWriteTransaction, FlowEntity)}.
     */
    @Deprecated
    CheckedFuture<Void,TransactionCommitFailedException> installFlow(Uint64 dpId, FlowEntity flowEntity);

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
    void addFlow(TypedWriteTransaction<Configuration> tx, Uint64 dpId, Flow flow);

    /**
     * Removes a flow.
     *
     * @deprecated Use {@link #removeFlow(TypedReadWriteTransaction, Uint64, String, short)}.
     */
    @Deprecated
    ListenableFuture<Void> removeFlow(Uint64 dpId, short tableId, FlowId flowId);

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
     * @deprecated Use {@link #removeFlow(TypedReadWriteTransaction, Uint64, Flow)}.
     */
    @Deprecated
    CheckedFuture<Void,TransactionCommitFailedException> removeFlow(Uint64 dpId, Flow flowEntity);

    /**
     * Removes the given flow.
     *
     * @param tx The transaction to use.
     * @param dpId The DPN identifier.
     * @param flow The flow.
     */
    void removeFlow(TypedReadWriteTransaction<Configuration> tx, Uint64 dpId, Flow flow)
        throws ExecutionException, InterruptedException;

    /**
     * Removes the given flow.
     *
     * @param tx The transaction to use.
     * @param flowEntity The flow entity.
     */
    void removeFlow(TypedReadWriteTransaction<Configuration> tx, FlowEntity flowEntity)
        throws ExecutionException, InterruptedException;

    /**
     * Removes the given flow.
     *
     * @param tx The transaction to use.
     * @param dpId The DPN identifier.
     * @param flowKey The flow key.
     * @param tableId The table identifier.
     */
    void removeFlow(TypedReadWriteTransaction<Configuration> tx, Uint64 dpId, FlowKey flowKey, short tableId)
        throws ExecutionException, InterruptedException;

    /**
     * Removes the given flow.
     *
     * @param tx The transaction to use.
     * @param dpId The DPN identifier.
     * @param flowId The flow identifier.
     * @param tableId The table identifier.
     */
    void removeFlow(TypedReadWriteTransaction<Configuration> tx, Uint64 dpId, String flowId, short tableId)
        throws ExecutionException, InterruptedException;

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
    void addGroup(TypedWriteTransaction<Configuration> tx, Uint64 dpId, Group group);

    /**
     * Remove a group.
     *
     * @deprecated Use {@link #removeGroup(TypedReadWriteTransaction, GroupEntity)}
     */
    @Deprecated
    void removeGroup(GroupEntity groupEntity);

    /**
     * Remove a group using the given transaction.
     *
     * @param tx The transaction to use.
     * @param groupEntity The group to remove.
     */
    void removeGroup(TypedReadWriteTransaction<Configuration> tx, GroupEntity groupEntity)
        throws ExecutionException, InterruptedException;

    /**
     * Remove a group using the given transaction.
     *
     * @param tx The transaction to use.
     * @param dpId The DPN identifier.
     * @param group The group to remove.
     */
    void removeGroup(TypedReadWriteTransaction<Configuration> tx, Uint64 dpId, Group group)
        throws ExecutionException, InterruptedException;

    /**
     * Remove a group using the given transaction.
     *
     * @param tx The transaction to use.
     * @param dpId The DPN identifier.
     * @param groupId The group identifier of the group to remove.
     */
    void removeGroup(TypedReadWriteTransaction<Configuration> tx, Uint64 dpId, long groupId)
        throws ExecutionException, InterruptedException;

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
    boolean groupExists(Uint64 dpId, long groupId);

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
     * Installs a group.
     *
     * @deprecated Use {@link #addGroup(TypedWriteTransaction, GroupEntity)}.
     */
    @Deprecated
    void syncInstallGroup(GroupEntity groupEntity);

    /**
     * API to remove the Group on Data Plane Node synchronously. It internally waits for
     * Group Change Notification to confirm group delete request is being sent.
     * @deprecated Use {@link #removeGroup(TypedReadWriteTransaction, GroupEntity)}.
     */
    @Deprecated
    void syncRemoveGroup(GroupEntity groupEntity);

    void addBucket(TypedReadWriteTransaction<Configuration> tx, Uint64 dpId, long groupId, Bucket bucket)
            throws ExecutionException, InterruptedException;

    void removeBucket(TypedReadWriteTransaction<Configuration> tx, Uint64 dpId, long groupId, long bucketId)
        throws ExecutionException, InterruptedException;
}
