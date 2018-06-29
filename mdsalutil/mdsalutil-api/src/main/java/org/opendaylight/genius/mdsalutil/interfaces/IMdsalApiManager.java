/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.interfaces;

import java.math.BigInteger;

import org.opendaylight.genius.infra.Datastore.Configuration;
import org.opendaylight.genius.infra.TypedReadTransaction;
import org.opendaylight.genius.infra.TypedReadWriteTransaction;
import org.opendaylight.genius.infra.TypedWriteTransaction;
import org.opendaylight.genius.mdsalutil.FlowEntity;
import org.opendaylight.genius.mdsalutil.GroupEntity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.Bucket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;

public interface IMdsalApiManager {

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

    /**
     * Remove a group using the given transaction.
     *
     * @param tx The transaction to use.
     * @param groupEntity The group to remove.
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
     * Check if the given OF group exists in the given DPN.
     *
     * @param tx The transaction to use.
     * @param dpId The DPN identifier.
     * @param groupId The group identifier.
     * @return {@code true} if the group exists, {@code false} otherwise.
     */
    boolean groupExists(TypedReadTransaction<Configuration> tx, BigInteger dpId, long groupId);

    /**
     * Adds the given bucket.
     *
     * @param tx The transaction to use.
     * @param dpId The DPN identifier.
     * @param groupId The group identifier.
     * @param bucket The bucket.
     */
    void addBucket(TypedReadWriteTransaction<Configuration> tx, BigInteger dpId, long groupId, Bucket bucket);

    /**
     * Removes the given bucket.
     *
     * @param tx The transaction to use.
     * @param dpId The DPN identifier.
     * @param groupId The group identifier.
     * @param bucketId The bucket identifier.
     */
    void removeBucket(TypedReadWriteTransaction<Configuration> tx, BigInteger dpId, long groupId, long bucketId);
}
