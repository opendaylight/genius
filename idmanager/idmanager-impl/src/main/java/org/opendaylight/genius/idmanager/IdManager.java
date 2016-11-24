/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.idmanager;

import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.idmanager.ReleasedIdHolder.DelayedIdEntry;
import org.opendaylight.genius.idmanager.jobs.CleanUpJob;
import org.opendaylight.genius.idmanager.jobs.IdHolderSyncJob;
import org.opendaylight.genius.idmanager.jobs.LocalPoolCreateJob;
import org.opendaylight.genius.idmanager.jobs.LocalPoolDeleteJob;
import org.opendaylight.genius.idmanager.jobs.UpdateIdEntryJob;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.utils.cache.CacheUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdRangeInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdRangeOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdRangeOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.CreateIdPoolInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.DeleteIdPoolInput;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.LockManagerService;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class IdManager implements IdManagerService {

    private static final Logger LOG = LoggerFactory.getLogger(IdManager.class);
    private static final long DEFAULT_IDLE_TIME = 24 * 60 * 60;

    private final DataBroker broker;
    private final LockManagerService lockManager;
    private final IdUtils idUtils;

    private final ConcurrentMap<String, IdLocalPool> localPool;
    private final Timer cleanJobTimer = new Timer();

    @Inject
    public IdManager(DataBroker db, LockManagerService lockManager, IdUtils idUtils) {
        this.broker = db;
        this.lockManager = lockManager;
        this.idUtils = idUtils;

        CacheUtil.createCache(IdUtils.ID_POOL_CACHE);
        localPool = (ConcurrentMap<String, IdLocalPool>) CacheUtil.getCache(IdUtils.ID_POOL_CACHE);
        populateCache();
    }

    @PostConstruct
    public void start() {
        LOG.info("{} start", getClass().getSimpleName());
    }

    @PreDestroy
    public void close() throws Exception {
        LOG.info("{} close", getClass().getSimpleName());
    }

    private void populateCache() {
        // If IP changes during reboot, then there will be orphaned child pools.
        InstanceIdentifier<IdPools> idPoolsInstance = idUtils.getIdPools();
        Optional<IdPools> idPoolsOptional = MDSALUtil.read(broker, LogicalDatastoreType.CONFIGURATION, idPoolsInstance);
        if (!idPoolsOptional.isPresent()) {
            return;
        }
        IdPools idPools = idPoolsOptional.get();
        List<IdPool> idPoolList = idPools.getIdPool();
        idPoolList
                .parallelStream()
                .filter(idPool -> idPool.getParentPoolName() != null
                        && !idPool.getParentPoolName().isEmpty()
                        && idUtils.getLocalPoolName(idPool.getParentPoolName())
                                .equals(idPool.getPoolName()))
                .forEach(
                    idPool -> updateLocalIdPoolCache(idPool,
                        idPool.getParentPoolName()));
    }

    public boolean updateLocalIdPoolCache(IdPool idPool, String parentPoolName) {
        AvailableIdsHolder availableIdsHolder = idPool.getAvailableIdsHolder();
        AvailableIdHolder availableIdHolder = new AvailableIdHolder(idUtils, availableIdsHolder.getStart(),
                availableIdsHolder.getEnd());
        availableIdHolder.setCur(availableIdsHolder.getCursor());
        ReleasedIdsHolder releasedIdsHolder = idPool.getReleasedIdsHolder();
        ReleasedIdHolder releasedIdHolder = new ReleasedIdHolder(idUtils, releasedIdsHolder.getDelayedTimeSec());
        releasedIdHolder.setAvailableIdCount(releasedIdsHolder.getAvailableIdCount());
        List<DelayedIdEntries> delayedEntries = releasedIdsHolder.getDelayedIdEntries();
        List<DelayedIdEntry> delayedIdEntryInCache = new ArrayList<>();
        if (delayedEntries != null) {
            delayedIdEntryInCache = delayedEntries
                    .parallelStream()
                    .map(delayedIdEntry -> new DelayedIdEntry(delayedIdEntry
                            .getId(), delayedIdEntry.getReadyTimeSec()))
                            .sorted((idEntry1, idEntry2) -> Long.compare(idEntry1.getReadyTimeSec(),
                                    idEntry2.getReadyTimeSec())).collect(Collectors.toList());
        }
        releasedIdHolder.setDelayedEntries(delayedIdEntryInCache);

        IdLocalPool idLocalPool = new IdLocalPool(idUtils, idPool.getPoolName());
        idLocalPool.setAvailableIds(availableIdHolder);
        idLocalPool.setReleasedIds(releasedIdHolder);
        localPool.put(parentPoolName, idLocalPool);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Populating cache for {} with {}", idLocalPool.getPoolName(), idLocalPool);
        }
        return true;
    }

    @Override
    public Future<RpcResult<Void>> createIdPool(CreateIdPoolInput input) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("createIdPool called with input {}", input);
        }
        String poolName = input.getPoolName();
        long low = input.getLow();
        long high = input.getHigh();
        long blockSize = idUtils.computeBlockSize(low, high);
        RpcResultBuilder<Void> createIdPoolRpcBuilder;
        idUtils.lockPool(lockManager, poolName);
        try {
            WriteTransaction tx = broker.newWriteOnlyTransaction();
            poolName = poolName.intern();
            IdPool idPool;
            idPool = createGlobalPool(tx, poolName, low, high, blockSize);
            String localPoolName = idUtils.getLocalPoolName(poolName);
            IdLocalPool idLocalPool = localPool.get(poolName);
            if (idLocalPool == null) {
                createLocalPool(tx, localPoolName, idPool);
                idUtils.updateChildPool(tx, idPool.getPoolName(), localPoolName);
            }
            submitTransaction(tx);
            createIdPoolRpcBuilder = RpcResultBuilder.success();
        } catch (Exception ex) {
            LOG.error("Creation of Id Pool {} failed due to {}", poolName, ex);
            createIdPoolRpcBuilder = RpcResultBuilder.failed();
            createIdPoolRpcBuilder.withError(ErrorType.APPLICATION, ex.getMessage());
        } finally {
            idUtils.unlockPool(lockManager, poolName);
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
        String localPoolName = idUtils.getLocalPoolName(poolName);
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
        String localPoolName = idUtils.getLocalPoolName(poolName);
        RpcResultBuilder<AllocateIdRangeOutput> allocateIdRangeRpcBuilder;
        List<Long> newIdValuesList = new ArrayList<>();
        AllocateIdRangeOutputBuilder output = new AllocateIdRangeOutputBuilder();
        try {
            newIdValuesList = allocateIdFromLocalPool(poolName, localPoolName, idKey, size);
            Collections.sort(newIdValuesList);
            output.setIdValues(newIdValuesList);
            allocateIdRangeRpcBuilder = RpcResultBuilder.success();
            allocateIdRangeRpcBuilder.withResult(output.build());
        } catch (NullPointerException e) {
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
            InstanceIdentifier<IdPool> idPoolToBeDeleted = idUtils.getIdPoolInstance(poolName);
            poolName = poolName.intern();
            synchronized (poolName) {
                IdPool idPool = getIdPool(idPoolToBeDeleted);
                List<ChildPools> childPoolList = idPool.getChildPools();
                if (childPoolList != null) {
                    childPoolList.parallelStream().forEach(childPool -> deletePool(childPool.getChildPoolName()));
                }
                MDSALUtil.syncDelete(broker, LogicalDatastoreType.CONFIGURATION, idPoolToBeDeleted);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Deleted id pool {}", poolName);
                }
            }
            deleteIdPoolRpcBuilder = RpcResultBuilder.success();
        } catch (Exception ex) {
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
            releaseIdFromLocalPool(poolName, idUtils.getLocalPoolName(poolName), idKey);
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
            LOG.debug("Allocating id from local pool {}. Parent pool {}. Idkey {}", localPoolName, parentPoolName,
                    idKey);
        }
        long newIdValue = -1;
        List<Long> newIdValuesList = new ArrayList<>();
        localPoolName = localPoolName.intern();
        InstanceIdentifier<IdPool> parentIdPoolInstanceIdentifier = idUtils.getIdPoolInstance(parentPoolName);
        InstanceIdentifier<IdEntries> existingId = idUtils.getIdEntry(parentIdPoolInstanceIdentifier, idKey);
        Optional<IdEntries> existingIdEntry = MDSALUtil.read(broker, LogicalDatastoreType.CONFIGURATION, existingId);
        if (existingIdEntry.isPresent()) {
            newIdValuesList = existingIdEntry.get().getIdValue();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Existing ids {} for the key {} ", newIdValuesList, idKey);
            }
            return newIdValuesList;
        }
        IdLocalPool localIdPool = localPool.get(parentPoolName);
        if (localIdPool == null) {
            idUtils.lockPool(lockManager, parentPoolName);
            try {
                WriteTransaction tx = broker.newWriteOnlyTransaction();
                IdPool parentIdPool = getIdPool(parentIdPoolInstanceIdentifier);
                localIdPool = createLocalPool(tx, localPoolName, parentIdPool); // Return localIdPool.....
                submitTransaction(tx);
            } finally {
                idUtils.unlockPool(lockManager, parentPoolName);
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Got pool {}", localIdPool);
        }
        if (size == 1) {
            newIdValue = getIdFromLocalPoolCache(localIdPool, parentPoolName);
            newIdValuesList.add(newIdValue);
        } else {
            IdPool parentIdPool = getIdPool(parentIdPoolInstanceIdentifier);
            long totalAvailableIdCount = localIdPool.getAvailableIds().getAvailableIdCount()
                    + localIdPool.getReleasedIds().getAvailableIdCount();
            AvailableIdsHolderBuilder availableParentIds = idUtils.getAvailableIdsHolderBuilder(parentIdPool);
            ReleasedIdsHolderBuilder releasedParentIds = idUtils.getReleaseIdsHolderBuilder(parentIdPool);
            totalAvailableIdCount = totalAvailableIdCount + releasedParentIds.getAvailableIdCount()
                    + idUtils.getAvailableIdsCount(availableParentIds);
            if (totalAvailableIdCount > size) {
                while (size > 0) {
                    try {
                        newIdValue = getIdFromLocalPoolCache(localIdPool, parentPoolName);
                    } catch (RuntimeException e) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Releasing IDs to pool {}", localPoolName);
                        }
                        // Releasing the IDs added in newIdValuesList since a null list would be returned now, as the
                        // requested size of list IDs exceeds the number of available IDs.
                        updateDelayedEntriesInLocalCache(newIdValuesList, parentPoolName, localIdPool);
                    }
                    newIdValuesList.add(newIdValue);
                    size--;
                }
            } else {
                return null;
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("The newIdValues {} for the idKey {}", newIdValuesList, idKey);
        }
        UpdateIdEntryJob job = new UpdateIdEntryJob(parentPoolName, localPoolName, idKey, newIdValuesList, broker, idUtils);
        DataStoreJobCoordinator.getInstance().enqueueJob(parentPoolName, job, IdUtils.RETRY_COUNT);
        return newIdValuesList;
    }

    private Long getIdFromLocalPoolCache(IdLocalPool localIdPool, String parentPoolName) {
        while (true) {
            IdHolder releasedIds = localIdPool.getReleasedIds();
            Optional<Long> releasedId = releasedIds.allocateId();
            if (releasedId.isPresent()) {
                IdHolderSyncJob poolSyncJob = new IdHolderSyncJob(localIdPool.getPoolName(), releasedIds, broker, idUtils);
                DataStoreJobCoordinator.getInstance().enqueueJob(localIdPool.getPoolName(), poolSyncJob, IdUtils.RETRY_COUNT);
                return releasedId.get();
            }
            IdHolder availableIds = localIdPool.getAvailableIds();
            if (availableIds != null) {
                Optional<Long> availableId = availableIds.allocateId();
                if (availableId.isPresent()) {
                    IdHolderSyncJob poolSyncJob = new IdHolderSyncJob(localIdPool.getPoolName(), availableIds, broker, idUtils);
                    DataStoreJobCoordinator.getInstance().enqueueJob(localIdPool.getPoolName(), poolSyncJob, IdUtils.RETRY_COUNT);
                    return availableId.get();
                }
            }
            long idCount = getIdBlockFromParentPool(parentPoolName, localIdPool);
            if (idCount <= 0) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Unable to allocate Id block from global pool");
                }
                throw new RuntimeException(String.format("Ids exhausted for pool : %s", parentPoolName));
            }
        }
    }

    /**
     * Changes made to availableIds and releasedIds will not be persisted to the datastore.
     */
    private long getIdBlockFromParentPool(String parentPoolName, IdLocalPool localIdPool) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Allocating block of id from parent pool {}", parentPoolName);
        }
        InstanceIdentifier<IdPool> idPoolInstanceIdentifier = idUtils.getIdPoolInstance(parentPoolName);
        parentPoolName = parentPoolName.intern();
        idUtils.lockPool(lockManager, parentPoolName);
        try {
            WriteTransaction tx = broker.newWriteOnlyTransaction();
            IdPool parentIdPool = getIdPool(idPoolInstanceIdentifier);
            long idCount = allocateIdBlockFromParentPool(localIdPool, parentIdPool, tx);
            submitTransaction(tx);
            return idCount;
        } finally {
            idUtils.unlockPool(lockManager, parentPoolName);
        }
    }

    private long allocateIdBlockFromParentPool(IdLocalPool localPoolCache, IdPool parentIdPool, WriteTransaction tx) {
        long idCount = -1;
        ReleasedIdsHolderBuilder releasedIdsBuilderParent = idUtils.getReleaseIdsHolderBuilder(parentIdPool);
        while (true) {
            idCount = allocateIdBlockFromReleasedIdsHolder(localPoolCache, releasedIdsBuilderParent, parentIdPool, tx);
            if (idCount > 0) {
                return idCount;
            }
            idCount = allocateIdBlockFromAvailableIdsHolder(localPoolCache, parentIdPool, tx);
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
        // Sorting the child pools on last accessed time so that the pool that
        // was not accessed for a long time comes first.
        Collections.sort(childPoolsList,
            (childPool1, childPool2) -> childPool1.getLastAccessTime().compareTo(childPool2.getLastAccessTime()));
        long currentTime = System.currentTimeMillis() / 1000;
        for (ChildPools childPools : childPoolsList) {
            if (childPools.getLastAccessTime() + DEFAULT_IDLE_TIME > currentTime) {
                break;
            }
            if (!childPools.getChildPoolName().equals(idUtils.getLocalPoolName(parentIdPool.getPoolName()))) {
                InstanceIdentifier<IdPool> idPoolInstanceIdentifier = idUtils.getIdPoolInstance(childPools.getChildPoolName());
                IdPool otherChildPool = getIdPool(idPoolInstanceIdentifier);
                ReleasedIdsHolderBuilder releasedIds = idUtils.getReleaseIdsHolderBuilder(otherChildPool);
                AvailableIdsHolderBuilder availableIds = idUtils.getAvailableIdsHolderBuilder(otherChildPool);
                long totalAvailableIdCount = releasedIds.getDelayedIdEntries().size() + idUtils.getAvailableIdsCount(availableIds);
                List<DelayedIdEntries> delayedIdEntriesChild = releasedIds.getDelayedIdEntries();
                List<DelayedIdEntries> delayedIdEntriesParent = releasedIdsBuilderParent.getDelayedIdEntries();
                if (delayedIdEntriesParent == null) {
                    delayedIdEntriesParent = new LinkedList<>();
                }
                delayedIdEntriesParent.addAll(delayedIdEntriesChild);
                delayedIdEntriesChild.removeAll(delayedIdEntriesChild);
                while (idUtils.isIdAvailable(availableIds)) {
                    long cursor = availableIds.getCursor() + 1;
                    delayedIdEntriesParent.add(idUtils.createDelayedIdEntry(cursor, currentTime));
                    availableIds.setCursor(cursor);
                }
                long count = releasedIdsBuilderParent.getAvailableIdCount() + totalAvailableIdCount;
                releasedIdsBuilderParent.setDelayedIdEntries(delayedIdEntriesParent).setAvailableIdCount(count);
                MDSALUtil.syncUpdate(broker, LogicalDatastoreType.CONFIGURATION, idPoolInstanceIdentifier,
                        new IdPoolBuilder().setKey(new IdPoolKey(otherChildPool.getPoolName()))
                                .setAvailableIdsHolder(availableIds.build()).setReleasedIdsHolder(releasedIds.build())
                                .build());
                return totalAvailableIdCount;
            }
        }
        return 0;
    }

    private long allocateIdBlockFromReleasedIdsHolder(IdLocalPool localIdPool,
            ReleasedIdsHolderBuilder releasedIdsBuilderParent, IdPool parentIdPool, WriteTransaction tx) {
        if (releasedIdsBuilderParent.getAvailableIdCount() == 0) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Ids unavailable in releasedIds of parent pool {}", parentIdPool);
            }
            return 0;
        }
        List<DelayedIdEntries> delayedIdEntriesParent = releasedIdsBuilderParent.getDelayedIdEntries();
        int idCount = Math.min(delayedIdEntriesParent.size(), parentIdPool.getBlockSize());
        List<DelayedIdEntries> idEntriesToBeRemoved = delayedIdEntriesParent.subList(0, idCount);
        ReleasedIdHolder releasedIds = (ReleasedIdHolder) localIdPool.getReleasedIds();
        List<DelayedIdEntry> delayedIdEntriesLocalCache = releasedIds.getDelayedEntries();
        delayedIdEntriesLocalCache = idEntriesToBeRemoved
                .parallelStream()
                .map(delayedIdEntry -> new DelayedIdEntry(delayedIdEntry
                        .getId(), delayedIdEntry.getReadyTimeSec()))
                .sorted((idEntry1, idEntry2) -> Long.compare(idEntry1.getReadyTimeSec(),
                        idEntry2.getReadyTimeSec())).collect(Collectors.toList());
        releasedIds.setDelayedEntries(delayedIdEntriesLocalCache);
        releasedIds.setAvailableIdCount(releasedIds.getAvailableIdCount() + idCount);
        localIdPool.setReleasedIds(releasedIds);
        delayedIdEntriesParent.removeAll(idEntriesToBeRemoved);
        releasedIdsBuilderParent.setDelayedIdEntries(delayedIdEntriesParent);
        InstanceIdentifier<ReleasedIdsHolder> releasedIdsHolderInstanceIdentifier = InstanceIdentifier
                .builder(IdPools.class).child(IdPool.class,
                        new IdPoolKey(parentIdPool.getPoolName())).child(ReleasedIdsHolder.class).build();
        releasedIdsBuilderParent.setAvailableIdCount(releasedIdsBuilderParent.getAvailableIdCount() - idCount);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Allocated {} ids from releasedIds of parent pool {}", idCount, parentIdPool);
        }
        tx.merge(LogicalDatastoreType.CONFIGURATION, releasedIdsHolderInstanceIdentifier,
                releasedIdsBuilderParent.build(), true);
        return idCount;
    }

    private long allocateIdBlockFromAvailableIdsHolder(IdLocalPool localIdPool, IdPool parentIdPool,
            WriteTransaction tx) {
        long idCount = 0;
        AvailableIdsHolderBuilder availableIdsBuilderParent = idUtils.getAvailableIdsHolderBuilder(parentIdPool);
        long end = availableIdsBuilderParent.getEnd();
        long cur = availableIdsBuilderParent.getCursor();
        if (!idUtils.isIdAvailable(availableIdsBuilderParent)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Ids exhausted in parent pool {}", parentIdPool);
            }
            return idCount;
        }
        // Update availableIdsHolder of Local Pool
        idCount = Math.min(end - cur, parentIdPool.getBlockSize());
        AvailableIdHolder availableIds = new AvailableIdHolder(idUtils, cur + 1, cur + idCount);
        localIdPool.setAvailableIds(availableIds);
        // Update availableIdsHolder of Global Pool
        InstanceIdentifier<AvailableIdsHolder> availableIdsHolderInstanceIdentifier = InstanceIdentifier
                .builder(IdPools.class).child(IdPool.class,
                        new IdPoolKey(parentIdPool.getPoolName())).child(AvailableIdsHolder.class).build();
        availableIdsBuilderParent.setCursor(cur + idCount);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Allocated {} ids from availableIds of global pool {}", idCount, parentIdPool);
        }
        tx.merge(LogicalDatastoreType.CONFIGURATION, availableIdsHolderInstanceIdentifier,
                availableIdsBuilderParent.build(), true);
        return idCount;
    }

    private void releaseIdFromLocalPool(String parentPoolName, String localPoolName, String idKey) {
        localPoolName = localPoolName.intern();
        InstanceIdentifier<IdPool> parentIdPoolInstanceIdentifier = idUtils.getIdPoolInstance(parentPoolName);
        IdPool parentIdPool = getIdPool(parentIdPoolInstanceIdentifier);
        List<IdEntries> idEntries = parentIdPool.getIdEntries();
        List<IdEntries> newIdEntries = idEntries;
        if (idEntries == null) {
            throw new RuntimeException("Id Entries does not exist");
        }
        InstanceIdentifier<IdEntries> existingId = idUtils.getIdEntry(parentIdPoolInstanceIdentifier, idKey);
        Optional<IdEntries> existingIdEntryObject = MDSALUtil.read(broker, LogicalDatastoreType.CONFIGURATION, existingId);
        if (!existingIdEntryObject.isPresent()) {
            throw new RuntimeException(
                    String.format("Specified Id key %s does not exist in id pool %s", idKey, parentPoolName));
        }
        IdEntries existingIdEntry = existingIdEntryObject.get();
        List<Long> idValuesList = existingIdEntry.getIdValue();
        IdLocalPool localIdPoolCache = localPool.get(parentPoolName);
        boolean isRemoved = newIdEntries.remove(existingIdEntry);
        if (LOG.isDebugEnabled()) {
            LOG.debug("The entry {} is removed {}", existingIdEntry, isRemoved);
        }
        updateDelayedEntriesInLocalCache(idValuesList, parentPoolName, localIdPoolCache);
        IdHolderSyncJob poolSyncJob = new IdHolderSyncJob(localPoolName, localIdPoolCache.getReleasedIds(), broker, idUtils);
        DataStoreJobCoordinator.getInstance().enqueueJob(localPoolName, poolSyncJob, IdUtils.RETRY_COUNT);
        scheduleCleanUpTask(localIdPoolCache, parentPoolName, parentIdPool.getBlockSize());
        if (LOG.isDebugEnabled()) {
            LOG.debug("Released id ({}, {}) from pool {}", idKey, idValuesList, localPoolName);
        }
        //Updating id entries in the parent pool. This will be used for restart scenario
        UpdateIdEntryJob job = new UpdateIdEntryJob(parentPoolName, localPoolName, idKey, null, broker, idUtils);
        DataStoreJobCoordinator.getInstance().enqueueJob(parentPoolName, job, IdUtils.RETRY_COUNT);
    }

    private void scheduleCleanUpTask(final IdLocalPool localIdPoolCache,
            final String parentPoolName, final int blockSize) {
        TimerTask scheduledTask = new TimerTask() {
            @Override
            public void run() {
                CleanUpJob job = new CleanUpJob(localIdPoolCache, broker, parentPoolName, blockSize, lockManager, idUtils);
                DataStoreJobCoordinator.getInstance().enqueueJob(localIdPoolCache.getPoolName(), job, IdUtils.RETRY_COUNT);
            }
        };
        cleanJobTimer.schedule(scheduledTask, IdUtils.DEFAULT_DELAY_TIME * 1000);
    }

    private IdPool createGlobalPool(WriteTransaction tx, String poolName, long low, long high, long blockSize) {
        IdPool idPool;
        InstanceIdentifier<IdPool> idPoolInstanceIdentifier = idUtils.getIdPoolInstance(poolName);
        Optional<IdPool> existingIdPool = MDSALUtil.read(broker, CONFIGURATION, idPoolInstanceIdentifier);
        if (!existingIdPool.isPresent()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Creating new global pool {}", poolName);
            }
            idPool = idUtils.createGlobalPool(poolName, low, high, blockSize);
            tx.put(LogicalDatastoreType.CONFIGURATION, idPoolInstanceIdentifier, idPool, true);
        } else {
            idPool = existingIdPool.get();
            if (LOG.isDebugEnabled()) {
                LOG.debug("GlobalPool exists {}", idPool);
            }
        }
        return idPool;
    }

    IdLocalPool createLocalPool(WriteTransaction tx, String localPoolName, IdPool idPool) {
        localPoolName = localPoolName.intern();
        IdLocalPool idLocalPool = new IdLocalPool(idUtils, localPoolName);
        allocateIdBlockFromParentPool(idLocalPool, idPool, tx);
        String parentPool = idPool.getPoolName();
        localPool.put(parentPool, idLocalPool);
        LocalPoolCreateJob job = new LocalPoolCreateJob(idLocalPool, broker, idPool.getPoolName(),
                idPool.getBlockSize(), idUtils);
        DataStoreJobCoordinator.getInstance().enqueueJob(localPoolName, job, IdUtils.RETRY_COUNT);
        return idLocalPool;
    }

    private void deletePool(String poolName) {
        LocalPoolDeleteJob job = new LocalPoolDeleteJob(poolName, broker, idUtils);
        DataStoreJobCoordinator.getInstance().enqueueJob(poolName, job, IdUtils.RETRY_COUNT);
    }

    private IdPool getIdPool(InstanceIdentifier<IdPool> idPoolInstanceIdentifier) {
        Optional<IdPool> idPool = MDSALUtil.read(broker, CONFIGURATION, idPoolInstanceIdentifier);
        if (!idPool.isPresent()) {
            throw new NoSuchElementException(String.format("Specified pool %s does not exist" , idPool));
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("GetIdPool : Read id pool {} ", idPool);
        }
        return idPool.get();
    }

    public void poolDeleted(String parentPoolName, String poolName) {
        IdLocalPool idLocalPool = localPool.get(parentPoolName);
        if (idLocalPool != null) {
            if (idLocalPool.getPoolName().equals(poolName)) {
                localPool.remove(parentPoolName);
            }
        }
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

    private void updateDelayedEntriesInLocalCache(List<Long> idsList, String parentPoolName,
            IdLocalPool localPoolCache) {
        for (long idValue : idsList) {
            localPoolCache.getReleasedIds().addId(idValue);
        }
        localPool.put(parentPoolName, localPoolCache);
    }
}
