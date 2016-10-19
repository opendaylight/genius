/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.idmanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdRangeInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdRangeOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdRangeOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.CreateIdPoolInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.DeleteIdPoolInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.GetExistingIdFromPoolInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.GetExistingIdFromPoolOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.GetExistingIdFromPoolOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdPools;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.ReleaseIdInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPool;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPoolBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPoolKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.AvailableIdsHolder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.AvailableIdsHolderBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.ChildPools;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.IdEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.ReleasedIdsHolder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.ReleasedIdsHolderBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.released.ids.DelayedIdEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.released.ids.DelayedIdEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.LockManagerService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;

public class IdManager implements IdManagerService, AutoCloseable{
    private static final Logger LOG = LoggerFactory.getLogger(IdManager.class);

    private static final long DEFAULT_IDLE_TIME = 24 * 60 * 60;

    private ListenerRegistration<DataChangeListener> listenerRegistration;
    private final DataBroker broker;
    private LockManagerService lockManager;

    @Override
    public void close() throws Exception {
        if (listenerRegistration != null) {
            try {
                listenerRegistration.close();
            } catch (final Exception e) {
                LOG.error("Error when cleaning up DataChangeListener.", e);
            }
            listenerRegistration = null;
        }
        LOG.info("IDManager Closed");
    }

    public IdManager(DataBroker db, LockManagerService lockManager) {
        broker = db;
        this.lockManager = lockManager;
    }

    /**
     * Deprecated constructor.
     * @deprecated Use {@link IdManager#IdManager(DataBroker, LockManagerService)} instead.
     */
    @Deprecated
    public IdManager(final DataBroker db) {
        broker = db;
    }

    /**
     * Deprecated LockManagerService setter injection.
     * @deprecated Use {@link IdManager#IdManager(DataBroker, LockManagerService)} instead.
     */
    @Deprecated
    public void setLockManager(LockManagerService lockManager) {
        this.lockManager = lockManager;
    }

    @Override
    public Future<RpcResult<Void>> createIdPool(CreateIdPoolInput input) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("createIdPool called with input {}", input);
        }
        String poolName = input.getPoolName();
        long low = input.getLow();
        long high = input.getHigh();
        long blockSize = IdUtils.computeBlockSize(low, high);
        RpcResultBuilder<Void> createIdPoolRpcBuilder;
        IdUtils.lockPool(lockManager, poolName);
        try {
            WriteTransaction tx = broker.newWriteOnlyTransaction();
            poolName = poolName.intern();
            IdPool idPool;
            idPool = createGlobalPool(tx, poolName, low, high, blockSize);
            String localPoolName = IdUtils.getLocalPoolName(poolName);
            createLocalPool(tx, localPoolName, idPool);
            submitTransaction(tx);
            createIdPoolRpcBuilder = RpcResultBuilder.success();
        } catch (Exception ex) {
            LOG.error("Creation of Id Pool {} failed due to {}", poolName, ex);
            createIdPoolRpcBuilder = RpcResultBuilder.failed();
            createIdPoolRpcBuilder.withError(ErrorType.APPLICATION, ex.getMessage());
        } finally {
            IdUtils.unlockPool(lockManager, poolName);
        }
        return Futures.immediateFuture(createIdPoolRpcBuilder.build());
    }

    @Override
    public Future<RpcResult<AllocateIdOutput>> allocateId(AllocateIdInput input) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("AllocateId called with input {}", input);
        }
        String idKey = input.getIdKey();
        String poolName = input.getPoolName();
        String localPoolName = IdUtils.getLocalPoolName(poolName);
        RpcResultBuilder<AllocateIdOutput> allocateIdRpcBuilder;
        long newIdValue = -1;
        AllocateIdOutputBuilder output = new AllocateIdOutputBuilder();
        try {
            //allocateIdFromLocalPool method returns a list of IDs with one element. This element is obtatined by get(0)
            newIdValue = allocateIdFromLocalPool(poolName, localPoolName, idKey, 1).get(0);
            output.setIdValue(newIdValue);
            allocateIdRpcBuilder = RpcResultBuilder.success();
            allocateIdRpcBuilder.withResult(output.build());
        } catch (Exception ex) {
            LOG.error("Allocate id in pool {} failed due to {}", poolName, ex);
            allocateIdRpcBuilder = RpcResultBuilder.failed();
            allocateIdRpcBuilder.withError(ErrorType.APPLICATION, ex.getMessage());
        }
        return Futures.immediateFuture(allocateIdRpcBuilder.build());
    }

    @Override
    public Future<RpcResult<AllocateIdRangeOutput>> allocateIdRange(AllocateIdRangeInput input) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("AllocateIdRange called with input {}", input);
        }
        String idKey = input.getIdKey();
        String poolName = input.getPoolName();
        long size = input.getSize();
        String localPoolName = IdUtils.getLocalPoolName(poolName);
        RpcResultBuilder<AllocateIdRangeOutput> allocateIdRangeRpcBuilder;
        List<Long> newIdValuesList = new ArrayList<>();
        AllocateIdRangeOutputBuilder output = new AllocateIdRangeOutputBuilder();
        try {
            newIdValuesList = allocateIdFromLocalPool(poolName, localPoolName, idKey, size);
            Collections.sort(newIdValuesList);
            output.setIdValues(newIdValuesList);
            allocateIdRangeRpcBuilder = RpcResultBuilder.success();
            allocateIdRangeRpcBuilder.withResult(output.build());
        } catch (NullPointerException e){
            LOG.error("Not enough Ids available in the pool {} for requested size {}", poolName, size);
            allocateIdRangeRpcBuilder = RpcResultBuilder.failed();
            allocateIdRangeRpcBuilder.withError(ErrorType.APPLICATION, e.getMessage());
        } catch (Exception ex) {
            LOG.error("Allocate id range in pool {} failed due to {}", poolName, ex);
            allocateIdRangeRpcBuilder = RpcResultBuilder.failed();
            allocateIdRangeRpcBuilder.withError(ErrorType.APPLICATION, ex.getMessage());
        }
        return Futures.immediateFuture(allocateIdRangeRpcBuilder.build());
    }

    @Override
    public Future<RpcResult<Void>> deleteIdPool(DeleteIdPoolInput input) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("DeleteIdPool called with input {}", input);
        }
        String poolName = input.getPoolName();
        RpcResultBuilder<Void> deleteIdPoolRpcBuilder;
        try {
            InstanceIdentifier<IdPool> idPoolToBeDeleted = IdUtils.getIdPoolInstance(poolName);
            poolName = poolName.intern();
            synchronized(poolName) {
                IdPool idPool = getIdPool(idPoolToBeDeleted);
                List<ChildPools> childPoolList = idPool.getChildPools();
                if (childPoolList != null) {
                    for (ChildPools childPoolName : childPoolList) {
                        deletePool(childPoolName.getChildPoolName());
                    }
                }
                MDSALUtil.syncDelete(broker, LogicalDatastoreType.CONFIGURATION, idPoolToBeDeleted);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Deleted id pool {}", poolName);
                }
            }
            deleteIdPoolRpcBuilder = RpcResultBuilder.success();
        }
        catch (Exception ex) {
            LOG.error("Delete id in pool {} failed due to {}", poolName, ex);
            deleteIdPoolRpcBuilder = RpcResultBuilder.failed();
            deleteIdPoolRpcBuilder.withError(ErrorType.APPLICATION, ex.getMessage());
        }
        return Futures.immediateFuture(deleteIdPoolRpcBuilder.build());
    }

    @Override
    public Future<RpcResult<Void>> releaseId(ReleaseIdInput input) {
        String poolName = input.getPoolName();
        String idKey = input.getIdKey();
        RpcResultBuilder<Void> releaseIdRpcBuilder;
        try {
            releaseIdFromLocalPool(IdUtils.getLocalPoolName(poolName), idKey);
            releaseIdRpcBuilder = RpcResultBuilder.success();
        } catch (Exception ex) {
            LOG.error("Release id {} from pool {} failed due to {}", idKey, poolName, ex);
            releaseIdRpcBuilder = RpcResultBuilder.failed();
            releaseIdRpcBuilder.withError(ErrorType.APPLICATION, ex.getMessage());
        }
        return Futures.immediateFuture(releaseIdRpcBuilder.build());
    }

    private List<Long> allocateIdFromLocalPool(String parentPoolName, String localPoolName, String idKey, long size) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Allocating id from local pool {}. Parent pool {}. Idkey {}", localPoolName, parentPoolName, idKey);
        }
        long newIdValue = -1;
        List<Long> newIdValuesList = new ArrayList<>();
        InstanceIdentifier<IdPool> localIdPoolInstanceIdentifier = IdUtils.getIdPoolInstance(localPoolName);
        localPoolName = localPoolName.intern();
        synchronized (localPoolName) {
            InstanceIdentifier<IdPool> parentIdPoolInstanceIdentifier = IdUtils.getIdPoolInstance(parentPoolName);
            IdPool parentIdPool = getIdPool(parentIdPoolInstanceIdentifier);
            List<IdEntries> idEntries = parentIdPool.getIdEntries();
            if (idEntries == null) {
                idEntries = new LinkedList<>();
            } else {
                InstanceIdentifier<IdEntries> existingId = IdUtils.getIdEntry(parentIdPoolInstanceIdentifier, idKey);
                Optional<IdEntries> existingIdEntry = MDSALUtil.read(broker, LogicalDatastoreType.CONFIGURATION, existingId);
                if (existingIdEntry.isPresent()) {
                    newIdValuesList = existingIdEntry.get().getIdValue();
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Existing ids {} for the key {} ", newIdValuesList, idKey);
                    }
                    return newIdValuesList;
                }
            }
            WriteTransaction tx = broker.newWriteOnlyTransaction();
            IdPool localPool = null;
            try {
                localPool = getIdPool(localIdPoolInstanceIdentifier);
            } catch (NoSuchElementException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Creating local pool {} since it was not present", localPoolName);
                }
                localPool = IdUtils.createLocalIdPool(localPoolName, parentIdPool);
                updateChildPool(tx, localPool.getParentPoolName(), localPoolName);
            }
            IdEntries newIdEntry;
            AvailableIdsHolderBuilder availableIds = IdUtils.getAvailableIdsHolderBuilder(localPool);
            ReleasedIdsHolderBuilder releasedIds = IdUtils.getReleaseIdsHolderBuilder(localPool);
            //Calling cleanupExcessIds since there could be excessive ids.
            cleanupExcessIds(availableIds, releasedIds, parentPoolName, localPool.getBlockSize());

            long totalAvailableIdCount = releasedIds.getAvailableIdCount() + IdUtils.getAvailableIdsCount(availableIds);
            AvailableIdsHolderBuilder availableParentIds = IdUtils.getAvailableIdsHolderBuilder(parentIdPool);
            ReleasedIdsHolderBuilder releasedParentIds = IdUtils.getReleaseIdsHolderBuilder(parentIdPool);
            totalAvailableIdCount = totalAvailableIdCount + releasedParentIds.getAvailableIdCount()
                    + IdUtils.getAvailableIdsCount(availableParentIds);
            if (totalAvailableIdCount > size) {
                while (size > 0) {
                    try {
                        newIdValue = getIdFromPool(localPool, availableIds, releasedIds);
                    } catch (RuntimeException e) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Releasing IDs to pool {}", localPoolName);
                        }
                        //Releasing the IDs added in newIdValuesList since a null list would be returned now, as the
                        //requested size of list IDs exceeds the number of available IDs.
                        updateDelayedEntries(availableIds, releasedIds, newIdValuesList, parentPoolName,
                                localPool, localIdPoolInstanceIdentifier, tx);
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Released ids ({}, {}) from local pool {}", idKey, newIdValuesList, localPoolName);
                        }
                        submitTransaction(tx);
                        return null;
                    }
                    newIdValuesList.add(newIdValue);
                    size--;
                }
            } else {
                return null;
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("The newIdValues {} for the idKey {}", newIdValuesList, idKey);
            }
            newIdEntry = IdUtils.createIdEntries(idKey, newIdValuesList);
            idEntries.add(newIdEntry);
            if (LOG.isDebugEnabled()) {
                LOG.debug("The availablelIds are {}", availableIds.build());
            }
            localPool = new IdPoolBuilder(localPool).setAvailableIdsHolder(availableIds.build())
                    .setReleasedIdsHolder(releasedIds.build()).build();
            tx.put(LogicalDatastoreType.CONFIGURATION, localIdPoolInstanceIdentifier, localPool, true);
            updateChildPool(tx, localPool.getParentPoolName(), localPoolName);
            //Updating id entries in the parent pool. This will be used for restart scenario
            tx.merge(LogicalDatastoreType.CONFIGURATION, IdUtils.getIdEntriesInstanceIdentifier(parentPoolName, idKey), newIdEntry);
            submitTransaction(tx);
        }
        return newIdValuesList;
    }

    private void updateDelayedEntries(AvailableIdsHolderBuilder availableIds, ReleasedIdsHolderBuilder releasedIds,
                                      List<Long> idsList, String parentPoolName, IdPool localPool,
                                      InstanceIdentifier<IdPool> localIdPoolInstanceIdentifier, WriteTransaction tx){
        long delayTime = System.currentTimeMillis() / 1000 + releasedIds.getDelayedTimeSec();
        List<DelayedIdEntries> delayedIdEntries = releasedIds.getDelayedIdEntries();
        if (delayedIdEntries == null) {
            delayedIdEntries = new LinkedList<>();
        }
        for(long idValue : idsList) {
            DelayedIdEntries delayedIdEntry = IdUtils.createDelayedIdEntry(idValue, delayTime);
            delayedIdEntries.add(delayedIdEntry);
        }

        long availableIdCount = releasedIds
                .getAvailableIdCount() == null ? 0
                : releasedIds.getAvailableIdCount();
        releasedIds.setDelayedIdEntries(delayedIdEntries);
        releasedIds.setAvailableIdCount(availableIdCount);
        //Calling cleanupExcessIds since there could be excessive ids.
        cleanupExcessIds(availableIds, releasedIds, parentPoolName, localPool.getBlockSize());
        localPool = new IdPoolBuilder(localPool)
                .setAvailableIdsHolder(availableIds.build())
                .setReleasedIdsHolder(releasedIds.build()).build();
        tx.put(LogicalDatastoreType.CONFIGURATION, localIdPoolInstanceIdentifier, localPool, true);
    }

    private long getIdFromPool(IdPool pool, AvailableIdsHolderBuilder availableIds, ReleasedIdsHolderBuilder releasedIds) {
        long newIdValue = -1;
        while (true) {
            newIdValue = IdUtils.getIdFromReleaseIdsIfAvailable(releasedIds);
            if (newIdValue != -1) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Retrieved id value {} from released id holder", newIdValue);
                }
                return newIdValue;
            }
            newIdValue = IdUtils.getIdFromAvailableIds(availableIds);
            if (newIdValue != -1) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Creating a new id {} for the pool: {} ", newIdValue, pool.getPoolName());
                }
                return newIdValue;
            }
            long idCount = getIdBlockFromParentPool(pool.getParentPoolName(), availableIds, releasedIds);
            if (idCount <= 0) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Unable to allocate Id block from global pool");
                }
                throw new RuntimeException(String.format("Ids exhausted for pool : %s", pool.getPoolName()));
            }
        }
    }

    /**
     * Changes made to releaseIds and AvailableIds are not persisted.
     * @param availableIds
     * @param releasedIds
     * @param parentPoolName
     * @param blockSize
     */
    private void cleanupExcessIds(AvailableIdsHolderBuilder availableIds, ReleasedIdsHolderBuilder releasedIds, String parentPoolName, int blockSize) {
        IdUtils.processDelayList(releasedIds);
        long totalAvailableIdCount = releasedIds.getAvailableIdCount() + IdUtils.getAvailableIdsCount(availableIds);
        if (totalAvailableIdCount > blockSize * 2) {
            parentPoolName = parentPoolName.intern();
            InstanceIdentifier<ReleasedIdsHolder> releasedIdInstanceIdentifier = IdUtils.getReleasedIdsHolderInstance(parentPoolName);
            IdUtils.lockPool(lockManager, parentPoolName);
            try {
                Optional<ReleasedIdsHolder> releasedIdsHolder = MDSALUtil.read(broker, LogicalDatastoreType.CONFIGURATION, releasedIdInstanceIdentifier);
                ReleasedIdsHolderBuilder releasedIdsParent;
                if (!releasedIdsHolder.isPresent()) {
                    LOG.error("ReleasedIds not present in parent pool. Unable to cleanup excess ids");
                    return;
                }
                releasedIdsParent = new ReleasedIdsHolderBuilder(releasedIdsHolder.get());
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Releasing excesss Ids from local pool");
                }
                IdUtils.freeExcessAvailableIds(releasedIds, releasedIdsParent, blockSize);
                MDSALUtil.syncWrite(broker, LogicalDatastoreType.CONFIGURATION, releasedIdInstanceIdentifier, releasedIdsParent.build());
            } finally {
                IdUtils.unlockPool(lockManager, parentPoolName);
            }
        }
    }

    /**
     * Changes made to availableIds and releasedIds will not be persisted to the datastore
     * @param parentPoolName
     * @param availableIdsBuilder
     * @param releasedIdsBuilder
     * @return
     */
    private long getIdBlockFromParentPool(String parentPoolName,
            AvailableIdsHolderBuilder availableIdsBuilder, ReleasedIdsHolderBuilder releasedIdsBuilder) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Allocating block of id from parent pool {}", parentPoolName);
        }
        InstanceIdentifier<IdPool> idPoolInstanceIdentifier = IdUtils.getIdPoolInstance(parentPoolName);
        parentPoolName = parentPoolName.intern();
        IdUtils.lockPool(lockManager, parentPoolName);
        try {
            WriteTransaction tx = broker.newWriteOnlyTransaction();
            IdPool parentIdPool = getIdPool(idPoolInstanceIdentifier);
            long idCount = allocateIdBlockFromParentPool(availableIdsBuilder, releasedIdsBuilder, parentIdPool, tx);
            submitTransaction(tx);
            return idCount;
        }
        finally {
            IdUtils.unlockPool(lockManager, parentPoolName);
        }
    }

    /**
     * Changes made to availableIds and releasedIds will not be persisted to the datastore
     * @param parentPoolName
     * @param availableIdsBuilder
     * @param releasedIdsBuilder
     * @return
     */
    private long allocateIdBlockFromParentPool(AvailableIdsHolderBuilder availableIdsBuilder,
            ReleasedIdsHolderBuilder releasedIdsBuilder, IdPool parentIdPool, WriteTransaction tx) {
        long idCount = -1;
        ReleasedIdsHolderBuilder releasedIdsBuilderParent = IdUtils.getReleaseIdsHolderBuilder(parentIdPool);
        while (true) {
            idCount = allocateIdBlockFromReleasedIdsHolder(releasedIdsBuilder, releasedIdsBuilderParent, parentIdPool, tx);
            if (idCount > 0) {
                return idCount;
            }
            idCount = allocateIdBlockFromAvailableIdsHolder(availableIdsBuilder, parentIdPool, tx);
            if (idCount > 0) {
                return idCount;
            }
            idCount = getIdsFromOtherChildPools(releasedIdsBuilderParent, parentIdPool);
            if (idCount <= 0) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Unable to allocate Id block from global pool");
                }
                throw new RuntimeException(String.format("Ids exhausted for pool : %s", parentIdPool.getPoolName()));
            }
        }
    }

    private long getIdsFromOtherChildPools(ReleasedIdsHolderBuilder releasedIdsBuilderParent, IdPool parentIdPool) {
        List<ChildPools> childPoolsList = parentIdPool.getChildPools();
        // Sorting the child pools on last accessed time so that the pool that was not accessed for a long time comes first.
        Collections.sort(childPoolsList, (childPool1, childPool2) -> childPool1.getLastAccessTime().compareTo(childPool2.getLastAccessTime()));
        long currentTime = System.currentTimeMillis() / 1000;
        for (ChildPools childPools : childPoolsList) {
            if (childPools.getLastAccessTime() + DEFAULT_IDLE_TIME < currentTime) {
                break;
            }
            if (!childPools.getChildPoolName().equals(IdUtils.getLocalPoolName(parentIdPool.getPoolName()))) {
                InstanceIdentifier<IdPool> idPoolInstanceIdentifier = IdUtils.getIdPoolInstance(childPools.getChildPoolName());
                IdPool otherChildPool = getIdPool(idPoolInstanceIdentifier);
                ReleasedIdsHolderBuilder releasedIds = IdUtils.getReleaseIdsHolderBuilder(otherChildPool);
                AvailableIdsHolderBuilder availableIds = IdUtils.getAvailableIdsHolderBuilder(otherChildPool);
                long totalAvailableIdCount = releasedIds.getDelayedIdEntries().size() + IdUtils.getAvailableIdsCount(availableIds);
                List<DelayedIdEntries> delayedIdEntriesChild = releasedIds.getDelayedIdEntries();
                List<DelayedIdEntries> delayedIdEntriesParent = releasedIdsBuilderParent.getDelayedIdEntries();
                if (delayedIdEntriesParent == null) {
                    delayedIdEntriesParent = new LinkedList<>();
                }
                delayedIdEntriesParent.addAll(delayedIdEntriesChild);
                delayedIdEntriesChild.removeAll(delayedIdEntriesChild);
                while (IdUtils.isIdAvailable(availableIds)) {
                    long cursor = availableIds.getCursor() + 1;
                    delayedIdEntriesParent.add(new DelayedIdEntriesBuilder().setId(cursor).setReadyTimeSec(System.currentTimeMillis()).build());
                    availableIds.setCursor(cursor);
                }
                long count = releasedIdsBuilderParent.getAvailableIdCount() + totalAvailableIdCount;
                releasedIdsBuilderParent.setDelayedIdEntries(delayedIdEntriesParent).setAvailableIdCount(count);
                MDSALUtil.syncWrite(broker, LogicalDatastoreType.CONFIGURATION, idPoolInstanceIdentifier,
                        new IdPoolBuilder(otherChildPool).setAvailableIdsHolder(availableIds.build()).setReleasedIdsHolder(releasedIds.build()).build());
                return totalAvailableIdCount;
            }
        }
        return 0;
    }

    private long allocateIdBlockFromReleasedIdsHolder(ReleasedIdsHolderBuilder releasedIdsBuilderChild, ReleasedIdsHolderBuilder releasedIdsBuilderParent, IdPool parentIdPool, WriteTransaction tx) {
        if (releasedIdsBuilderParent.getAvailableIdCount() == 0) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Ids unavailable in releasedIds of parent pool {}", parentIdPool);
            }
            return 0;
        }
        List<DelayedIdEntries> delayedIdEntriesParent = releasedIdsBuilderParent.getDelayedIdEntries();
        List<DelayedIdEntries> delayedIdEntriesChild = releasedIdsBuilderChild.getDelayedIdEntries();
        if (delayedIdEntriesChild == null) {
            delayedIdEntriesChild = new LinkedList<>();
        }
        int idCount = Math.min(delayedIdEntriesParent.size(), parentIdPool.getBlockSize());
        List<DelayedIdEntries> idEntriesToBeRemoved = delayedIdEntriesParent.subList(0, idCount);
        delayedIdEntriesChild.addAll(0, idEntriesToBeRemoved);
        delayedIdEntriesParent.removeAll(idEntriesToBeRemoved);
        releasedIdsBuilderParent.setDelayedIdEntries(delayedIdEntriesParent);
        releasedIdsBuilderChild.setDelayedIdEntries(delayedIdEntriesChild);
        releasedIdsBuilderChild.setAvailableIdCount(releasedIdsBuilderChild.getAvailableIdCount() + idCount);
        InstanceIdentifier<ReleasedIdsHolder> releasedIdsHolderInstanceIdentifier = InstanceIdentifier
                .builder(IdPools.class).child(IdPool.class,
                        new IdPoolKey(parentIdPool.getPoolName())).child(ReleasedIdsHolder.class).build();
        releasedIdsBuilderParent.setAvailableIdCount(releasedIdsBuilderParent.getAvailableIdCount() - idCount);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Allocated {} ids from releasedIds of parent pool {}", idCount, parentIdPool);
        }
        tx.merge(LogicalDatastoreType.CONFIGURATION, releasedIdsHolderInstanceIdentifier, releasedIdsBuilderParent.build(), true);
        return idCount;
    }

    private long allocateIdBlockFromAvailableIdsHolder(AvailableIdsHolderBuilder availableIdsBuilder, IdPool parentIdPool, WriteTransaction tx) {
        long idCount = 0;
        AvailableIdsHolderBuilder availableIdsBuilderParent = IdUtils.getAvailableIdsHolderBuilder(parentIdPool);
        long end = availableIdsBuilderParent.getEnd();
        long cur = availableIdsBuilderParent.getCursor();
        if (!IdUtils.isIdAvailable(availableIdsBuilderParent)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Ids exhausted in parent pool {}", parentIdPool);
            }
            return idCount;
        }
        // Update availableIdsHolder of Local Pool
        availableIdsBuilder.setStart(cur + 1);
        idCount = Math.min(end - cur, parentIdPool.getBlockSize());
        availableIdsBuilder.setEnd(cur + idCount);
        availableIdsBuilder.setCursor(cur);
        // Update availableIdsHolder of Global Pool
        InstanceIdentifier<AvailableIdsHolder> availableIdsHolderInstanceIdentifier = InstanceIdentifier
                .builder(IdPools.class).child(IdPool.class,
                        new IdPoolKey(parentIdPool.getPoolName())).child(AvailableIdsHolder.class).build();
        availableIdsBuilderParent.setCursor(cur + idCount);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Allocated {} ids from availableIds of global pool {}", idCount, parentIdPool);
        }
        tx.merge(LogicalDatastoreType.CONFIGURATION, availableIdsHolderInstanceIdentifier, availableIdsBuilderParent.build(), true);
        return idCount;
    }

    private void releaseIdFromLocalPool(String poolName, String idKey) {
        InstanceIdentifier<IdPool> localIdPoolInstanceIdentifier = IdUtils.getIdPoolInstance(poolName);
        poolName = poolName.intern();
        synchronized (poolName) {
            IdPool localPool = getIdPool(localIdPoolInstanceIdentifier);
            String parentPoolName = localPool.getParentPoolName();
            InstanceIdentifier<IdPool> parentIdPoolInstanceIdentifier = IdUtils.getIdPoolInstance(parentPoolName);
            IdPool parentIdPool = getIdPool(parentIdPoolInstanceIdentifier);
            List<IdEntries> idEntries = parentIdPool.getIdEntries();
            List<IdEntries> newIdEntries = idEntries;
            if (idEntries == null) {
                throw new RuntimeException("Id Entries does not exist");
            }
            InstanceIdentifier<IdEntries> existingId = IdUtils.getIdEntry(parentIdPoolInstanceIdentifier, idKey);
            Optional<IdEntries> existingIdEntryObject = MDSALUtil.read(broker, LogicalDatastoreType.CONFIGURATION, existingId);
            if (!existingIdEntryObject.isPresent()) {
                throw new RuntimeException(String.format("Specified Id key %s does not exist in id pool %s", idKey, poolName));
            }
            IdEntries existingIdEntry = existingIdEntryObject.get();
            List<Long> idValuesList = existingIdEntry.getIdValue();
            boolean isRemoved = newIdEntries.remove(existingIdEntry);
            if (LOG.isDebugEnabled()) {
                LOG.debug("The entry {} is removed {}", existingIdEntry, isRemoved);
            }
            ReleasedIdsHolderBuilder releasedIds = IdUtils.getReleaseIdsHolderBuilder(localPool);
            AvailableIdsHolderBuilder availableIds = IdUtils.getAvailableIdsHolderBuilder(localPool);
            WriteTransaction tx = broker.newWriteOnlyTransaction();
            updateDelayedEntries(availableIds, releasedIds, idValuesList, parentPoolName,
                    localPool, localIdPoolInstanceIdentifier, tx);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Released ids ({}, {}) from pool {}", idKey, idValuesList, poolName);
            }
            //Updating id entries in the parent pool. This will be used for restart scenario
            tx.delete(LogicalDatastoreType.CONFIGURATION, IdUtils.getIdEntriesInstanceIdentifier(parentPoolName, idKey));
            submitTransaction(tx);
        }
    }

    private IdPool createGlobalPool(WriteTransaction tx, String poolName, long low, long high,
                                    long blockSize) {
        IdPool idPool;
        InstanceIdentifier<IdPool> idPoolInstanceIdentifier = IdUtils.getIdPoolInstance(poolName);
        Optional<IdPool> existingIdPool = MDSALUtil.read(broker, LogicalDatastoreType.CONFIGURATION, idPoolInstanceIdentifier);
        if (!existingIdPool.isPresent()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Creating new global pool {}", poolName);
            }
            idPool = IdUtils.createGlobalPool(poolName, low, high, blockSize);
            tx.put(LogicalDatastoreType.CONFIGURATION, idPoolInstanceIdentifier, idPool, true);
        }
        else {
            idPool = existingIdPool.get();
            if (LOG.isDebugEnabled()) {
                LOG.debug("GlobalPool exists {}", idPool);
            }
        }
        return idPool;
    }

    private boolean createLocalPool(WriteTransaction tx, String localPoolName, IdPool idPool) {
        localPoolName = localPoolName.intern();
        synchronized (localPoolName) {
            InstanceIdentifier<IdPool> localIdPoolInstanceIdentifier = IdUtils.getIdPoolInstance(localPoolName);
            Optional<IdPool> localIdPool = MDSALUtil.read(broker, LogicalDatastoreType.CONFIGURATION, localIdPoolInstanceIdentifier);
            if (!localIdPool.isPresent()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Creating new local pool");
                }
                IdPool newLocalIdPool = IdUtils.createLocalIdPool(localPoolName, idPool);
                ReleasedIdsHolderBuilder releasedIdsBuilder = IdUtils.getReleaseIdsHolderBuilder(newLocalIdPool);
                AvailableIdsHolderBuilder availableIdsBuilder = IdUtils.getAvailableIdsHolderBuilder(newLocalIdPool);
                allocateIdBlockFromParentPool(availableIdsBuilder, releasedIdsBuilder, idPool, tx);
                newLocalIdPool = new IdPoolBuilder(newLocalIdPool).setAvailableIdsHolder(availableIdsBuilder.build())
                        .setReleasedIdsHolder(releasedIdsBuilder.build()).build();
                tx.put(LogicalDatastoreType.CONFIGURATION, localIdPoolInstanceIdentifier, newLocalIdPool, true);
                updateChildPool(tx, idPool.getPoolName(), localPoolName);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Local pool created {}", newLocalIdPool);
                }
                return true;
            }
        }
        return false;
    }

    private void deletePool(String poolName) {
        InstanceIdentifier<IdPool> idPoolToBeDeleted = IdUtils.getIdPoolInstance(poolName);
        synchronized (poolName) {
            Optional<IdPool> idPool = MDSALUtil.read(broker, LogicalDatastoreType.CONFIGURATION, idPoolToBeDeleted);
            if (idPool.isPresent()) {
                MDSALUtil.syncDelete(broker, LogicalDatastoreType.CONFIGURATION, idPoolToBeDeleted);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Deleted local pool {}", poolName);
                }
            }
        }
    }

    private IdPool getIdPool(InstanceIdentifier<IdPool> idPoolInstanceIdentifier) {
        Optional<IdPool> idPool = MDSALUtil.read(broker, LogicalDatastoreType.CONFIGURATION, idPoolInstanceIdentifier);
        if (!idPool.isPresent()) {
            throw new NoSuchElementException(String.format("Specified pool %s does not exist" , idPool));
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("GetIdPool : Read id pool {} ", idPool);
        }
        return idPool.get();
    }

    private void updateChildPool(WriteTransaction tx, String poolName, String localPoolName) {
        ChildPools childPool = IdUtils.createChildPool(localPoolName);
        InstanceIdentifier<ChildPools> childPoolInstanceIdentifier = IdUtils.getChildPoolsInstanceIdentifier(poolName, localPoolName);
        tx.merge(LogicalDatastoreType.CONFIGURATION, childPoolInstanceIdentifier, childPool, true);
    }


    private void submitTransaction(WriteTransaction tx) {
        CheckedFuture<Void, TransactionCommitFailedException> futures = tx.submit();
        try {
            futures.get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error writing to datastore tx", tx);
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public Future<RpcResult<GetExistingIdFromPoolOutput>> getExistingIdFromPool(GetExistingIdFromPoolInput input) {
        String parentPoolName = input.getPoolName();
        String idKey = input.getIdKey();
        String localPoolName = IdUtils.getLocalPoolName(parentPoolName);
        Long idValue = 0L;
        RpcResultBuilder<GetExistingIdFromPoolOutput> getExistingIdRpcBuilder;
        GetExistingIdFromPoolOutputBuilder output = new GetExistingIdFromPoolOutputBuilder();
        synchronized (localPoolName) {
            try {
                InstanceIdentifier<IdPool> parentIdPoolInstanceIdentifier = IdUtils.getIdPoolInstance(parentPoolName);
                IdPool parentIdPool = getIdPool(parentIdPoolInstanceIdentifier);
                List<IdEntries> idEntries = parentIdPool.getIdEntries();
                if (idEntries == null) {
                    LOG.error("IdPool {} is empty", parentPoolName);
                    getExistingIdRpcBuilder = RpcResultBuilder.failed();
                    getExistingIdRpcBuilder.withError(ErrorType.APPLICATION, "IdPool " + parentPoolName + " is empty.");
                } else {
                    InstanceIdentifier<IdEntries> existingId = IdUtils.getIdEntry(parentIdPoolInstanceIdentifier, idKey);
                    Optional<IdEntries> existingIdEntry = MDSALUtil.read(broker, LogicalDatastoreType.CONFIGURATION, existingId);
                    if (existingIdEntry.isPresent()) {
                        idValue = existingIdEntry.get().getIdValue().get(0);
                        LOG.debug("Existing id {} for the key {} ", idValue, idKey);
                        output.setIdValue(idValue);
                        getExistingIdRpcBuilder = RpcResultBuilder.success();
                        getExistingIdRpcBuilder.withResult(output.build());
                    } else {
                        LOG.error("No Id present in pool {} for key {}", parentPoolName, idKey);
                        getExistingIdRpcBuilder = RpcResultBuilder.failed();
                        getExistingIdRpcBuilder.withError(ErrorType.APPLICATION, "No Id present in pool "+ parentPoolName+" for key " + idKey);
                    }
                }
            } catch (Exception e) {
                LOG.error("Error while fetching existing Id for key {} from pool {}. E {}", idKey, parentPoolName, e.getMessage());
                getExistingIdRpcBuilder = RpcResultBuilder.failed();
                getExistingIdRpcBuilder.withError(ErrorType.APPLICATION, e.getMessage());
            }
        }
        return Futures.immediateFuture(getExistingIdRpcBuilder.build());
    }
}
