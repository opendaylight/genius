/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.idmanager;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toCollection;
import static org.opendaylight.mdsal.binding.util.Datastore.CONFIGURATION;
import static org.opendaylight.yangtools.yang.binding.CodeHelpers.nonnull;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.aries.blueprint.annotation.service.Reference;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.daexim.DataImportBootReady;
import org.opendaylight.genius.datastoreutils.ExpectedDataObjectNotFoundException;
import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.genius.idmanager.ReleasedIdHolder.DelayedIdEntry;
import org.opendaylight.genius.idmanager.api.IdManagerMonitor;
import org.opendaylight.genius.idmanager.jobs.CleanUpJob;
import org.opendaylight.genius.idmanager.jobs.IdHolderSyncJob;
import org.opendaylight.genius.idmanager.jobs.LocalPoolCreateJob;
import org.opendaylight.genius.idmanager.jobs.LocalPoolDeleteJob;
import org.opendaylight.genius.idmanager.jobs.UpdateIdEntryJob;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.mdsalutil.interfaces.ShardStatusMonitor;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.util.Datastore.Configuration;
import org.opendaylight.mdsal.binding.util.ManagedNewTransactionRunner;
import org.opendaylight.mdsal.binding.util.ManagedNewTransactionRunnerImpl;
import org.opendaylight.mdsal.binding.util.TypedReadWriteTransaction;
import org.opendaylight.mdsal.binding.util.TypedWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.serviceutils.tools.rpc.FutureRpcResults;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdRangeInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdRangeOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdRangeOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.CreateIdPoolInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.CreateIdPoolOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.CreateIdPoolOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.DeleteIdPoolInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.DeleteIdPoolOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdPools;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.ReleaseIdInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.ReleaseIdOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.ReleaseIdOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPool;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPoolBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPoolKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.AvailableIdsHolder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.AvailableIdsHolderBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.ChildPools;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.ChildPoolsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.IdEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.IdEntriesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.ReleasedIdsHolder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.ReleasedIdsHolderBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.released.ids.DelayedIdEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.LockManagerService;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.OperationFailedException;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class IdManager implements IdManagerService, IdManagerMonitor {
    private static final Logger LOG = LoggerFactory.getLogger(IdManager.class);
    private static final long DEFAULT_IDLE_TIME = 24 * 60 * 60;

    private final DataBroker broker;
    private final ManagedNewTransactionRunner txRunner;
    private final SingleTransactionDataBroker singleTxDB;
    private final LockManagerService lockManager;
    private final IdUtils idUtils;
    private final JobCoordinator jobCoordinator;

    private final ConcurrentMap<String, IdLocalPool> localPool;
    private final Timer cleanJobTimer = new Timer();

    @Inject
    public IdManager(DataBroker db, LockManagerService lockManager, IdUtils idUtils,
                     @Reference DataImportBootReady dataImportBootReady,
                     @Reference JobCoordinator jobCoordinator, @Reference ShardStatusMonitor shardStatusMonitor)
                    throws ReadFailedException, InterruptedException {
        this.broker = db;
        this.txRunner = new ManagedNewTransactionRunnerImpl(db);
        this.singleTxDB = new SingleTransactionDataBroker(db);
        this.lockManager = lockManager;
        this.idUtils = idUtils;
        this.jobCoordinator = jobCoordinator;

        // NB: We do not "use" the DataImportBootReady, but it's presence in the OSGi
        // Service Registry is the required "signal" that the Daexim "import on boot"
        // has fully completed (which we want to wait for).  Therefore, making this
        // dependent on that defers the Blueprint initialization, as we'd like to,
        // so that we do not start giving out new IDs before an import went in.
        // Thus, please DO NOT remove the DataImportBootReady argument, even if
        // it appears to be (is) un-used from a Java code PoV!

        this.localPool = new ConcurrentHashMap<>();
        boolean isDatastoreAvailable = false;
        int retryCount = 0;
        int totalRetry = 1000;
        try {
            while (retryCount < totalRetry) {
                isDatastoreAvailable = shardStatusMonitor.getShardStatus(NwConstants.IdManagerShards.getShardList());
                if (isDatastoreAvailable) {
                    LOG.info("IDManager is UP");
                    populateCache();
                    break;
                }
                LOG.error("IdManager: retrying shard status check for the {} time, pending retries {}",
                        ++retryCount, totalRetry - retryCount);
                Thread.sleep(2000);
            }
        } catch (InterruptedException e) {
            LOG.error("IDManager is DOWN, shard status check failed");
        }

        if (!isDatastoreAvailable) {
            LOG.error("IDManager is DOWN, as shards were not available at bundle bringup");
        }
    }

    @Override
    public Map<String, String> getLocalPoolsDetails() {
        Map<String, String> map = new HashMap<>();
        localPool.forEach((key, value) -> map.put(key, value.toString()));
        return map;
    }

    @PostConstruct
    public void start() {
        LOG.info("{} start", getClass().getSimpleName());
    }

    @PreDestroy
    public void close() {
        cleanJobTimer.cancel();

        LOG.info("{} close", getClass().getSimpleName());
    }

    private void populateCache() throws InterruptedException {
        // If IP changes during reboot, then there will be orphaned child pools.
        InstanceIdentifier<IdPools> idPoolsInstance = idUtils.getIdPools();
        Optional<IdPools> idPoolsOptional;
        while (true) {
            try {
                idPoolsOptional = singleTxDB.syncReadOptional(LogicalDatastoreType.CONFIGURATION, idPoolsInstance);
                break;
            } catch (ExecutionException e) {
                LOG.error("Failed to read the id pools due to error. Retrying again...", e);
            }
            Thread.sleep(2000);
        }
        if (!idPoolsOptional.isPresent()) {
            return;
        }
        idPoolsOptional.get().nonnullIdPool().values()
                .stream()
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
        AvailableIdHolder availableIdHolder = new AvailableIdHolder(idUtils, availableIdsHolder.getStart().toJava(),
                availableIdsHolder.getEnd().toJava());
        availableIdHolder.setCur(availableIdsHolder.getCursor());
        ReleasedIdsHolder releasedIdsHolder = idPool.getReleasedIdsHolder();
        ReleasedIdHolder releasedIdHolder = new ReleasedIdHolder(idUtils,
                                                        releasedIdsHolder.getDelayedTimeSec().toJava());
        releasedIdHolder.setAvailableIdCount(releasedIdsHolder.getAvailableIdCount().toJava());
        List<DelayedIdEntry> delayedIdEntryInCache = releasedIdsHolder.nonnullDelayedIdEntries()
                .stream()
                .map(delayedIdEntry -> new DelayedIdEntry(delayedIdEntry
                        .getId().toJava(), delayedIdEntry.getReadyTimeSec().toJava()))
                .sorted(comparing(DelayedIdEntry::getReadyTimeSec))
                .collect(toCollection(ArrayList::new));

        releasedIdHolder.replaceDelayedEntries(delayedIdEntryInCache);

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
    public ListenableFuture<RpcResult<CreateIdPoolOutput>> createIdPool(CreateIdPoolInput input) {
        LOG.info("createIdPool called with input {}", input);
        long low = input.getLow().toJava();
        long high = input.getHigh().toJava();
        long blockSize = idUtils.computeBlockSize(low, high);
        return FutureRpcResults.fromListenableFuture(LOG, "createIdPool", input, () -> {
            String poolName = input.getPoolName().intern();
            try {
                idUtils.lock(lockManager, poolName);
                return Futures.transform(txRunner.callWithNewReadWriteTransactionAndSubmit(CONFIGURATION, confTx -> {
                    IdPool idPool = createGlobalPool(confTx, poolName, low, high, blockSize);
                    String localPoolName = idUtils.getLocalPoolName(poolName);
                    IdLocalPool idLocalPool = localPool.get(poolName);
                    if (idLocalPool == null) {
                        createLocalPool(confTx, localPoolName, idPool);
                        idUtils.updateChildPool(confTx, idPool.getPoolName(), localPoolName);
                    }
                }), unused -> new CreateIdPoolOutputBuilder().build(), MoreExecutors.directExecutor());
            } finally {
                idUtils.unlock(lockManager, poolName);
            }
        }).build();
    }

    @Override
    public ListenableFuture<RpcResult<AllocateIdOutput>> allocateId(AllocateIdInput input) {
        String idKey = input.getIdKey();
        String poolName = input.getPoolName();
        return FutureRpcResults.fromBuilder(LOG, "allocateId", input, () -> {
            String localPoolName = idUtils.getLocalPoolName(poolName);
            // allocateIdFromLocalPool method returns a list of IDs with one element. This element is obtained by get(0)
            long newIdValue = allocateIdFromLocalPool(poolName, localPoolName, idKey, 1).get(0).toJava();
            return new AllocateIdOutputBuilder().setIdValue(newIdValue);
        }).onFailure(e -> completeExceptionallyIfPresent(poolName, idKey, e)).build();
    }

    private void completeExceptionallyIfPresent(String poolName, String idKey, Throwable exception) {
        CompletableFuture<List<Uint32>> completableFuture =
                idUtils.removeAllocatedIds(idUtils.getUniqueKey(poolName, idKey));
        if (completableFuture != null) {
            completableFuture.completeExceptionally(exception);
        }
    }

    @Override
    public ListenableFuture<RpcResult<AllocateIdRangeOutput>> allocateIdRange(AllocateIdRangeInput input) {
        String idKey = input.getIdKey();
        String poolName = input.getPoolName();
        long size = input.getSize().toJava();
        String localPoolName = idUtils.getLocalPoolName(poolName);
        AllocateIdRangeOutputBuilder output = new AllocateIdRangeOutputBuilder();
        return FutureRpcResults.fromBuilder(LOG, "allocateIdRange", input, () -> {
            List<Uint32> newIdValuesList = allocateIdFromLocalPool(poolName, localPoolName, idKey, size);
            Collections.sort(newIdValuesList);
            output.setIdValues(newIdValuesList);
            return output;
        }).onFailure(e -> completeExceptionallyIfPresent(poolName, idKey, e)).build();
    }

    @Override
    public ListenableFuture<RpcResult<DeleteIdPoolOutput>> deleteIdPool(DeleteIdPoolInput input) {
        return FutureRpcResults.fromListenableFuture(LOG, "deleteIdPool", input, () -> {
            String poolName = input.getPoolName().intern();
            InstanceIdentifier<IdPool> idPoolToBeDeleted = idUtils.getIdPoolInstance(poolName);
            synchronized (poolName) {
                IdPool idPool = singleTxDB.syncRead(LogicalDatastoreType.CONFIGURATION, idPoolToBeDeleted);
                //List<ChildPools> childPoolList = idPool.getChildPools();


                @Nullable Map<ChildPoolsKey, ChildPools> childPoolList = idPool.getChildPools();
                if (childPoolList != null) {
                    childPoolList.forEach((childPool, childPools) -> {
                        deletePool(childPool.getChildPoolName());
                    });
                }
                singleTxDB.syncDelete(LogicalDatastoreType.CONFIGURATION, idPoolToBeDeleted);
            }
            // TODO return the Future from a TBD asyncDelete instead.. BUT check that all callers @CheckReturnValue
            return Futures.immediateFuture((DeleteIdPoolOutput) null);
        }).build();
    }

    @Override
    public ListenableFuture<RpcResult<ReleaseIdOutput>> releaseId(ReleaseIdInput input) {
        String poolName = input.getPoolName();
        String idKey = input.getIdKey();
        String uniqueKey = idUtils.getUniqueKey(poolName, idKey);
        return FutureRpcResults.fromBuilder(LOG, "releaseId", input, () -> {
            idUtils.lock(lockManager, uniqueKey);
            return releaseIdFromLocalPool(poolName, idUtils.getLocalPoolName(poolName), idKey);
        }).onFailureLogLevel(FutureRpcResults.LogLevel.NONE)
                .onFailure(e -> {
                    if (e instanceof IdDoesNotExistException) {
                        // Do not log full stack trace in case ID does not exist
                        LOG.error("RPC releaseId() failed due to IdDoesNotExistException; input = {}", input);
                    } else {
                        // But for all other cases do:
                        LOG.error("RPC releaseId() failed; input = {}", input, e);
                    }
                    idUtils.unlock(lockManager, uniqueKey);
                }).build();
    }

    private List<Uint32> allocateIdFromLocalPool(String parentPoolName, String localPoolName,
            String idKey, long size) throws OperationFailedException, IdManagerException, ExecutionException,
            InterruptedException {
        LOG.debug("Allocating id from local pool {}. Parent pool {}. Idkey {}", localPoolName, parentPoolName, idKey);
        String uniqueIdKey = idUtils.getUniqueKey(parentPoolName, idKey);
        CompletableFuture<List<Uint32>> futureIdValues = new CompletableFuture<>();
        CompletableFuture<List<Uint32>> existingFutureIdValue =
                idUtils.putAllocatedIdsIfAbsent(uniqueIdKey, futureIdValues);
        if (existingFutureIdValue != null) {
            try {
                return existingFutureIdValue.get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.warn("Could not obtain id from existing futureIdValue for idKey {} and pool {}.",
                        idKey, parentPoolName);
                throw new IdManagerException(e.getMessage(), e);
            }
        }
        try {
            List<Uint32> newIdValuesList = checkForIdInIdEntries(parentPoolName, idKey, uniqueIdKey, futureIdValues,
                    false);
            if (!newIdValuesList.isEmpty()) {
                return newIdValuesList;
            }
            //This get will not help in concurrent reads. Hence the same read needs to be done again.
            IdLocalPool localIdPool = getOrCreateLocalIdPool(parentPoolName, localPoolName);
            LOG.debug("Got pool {}", localIdPool);
            long newIdValue = -1;
            localPoolName = localPoolName.intern();
            if (size == 1) {
                newIdValue = getIdFromLocalPoolCache(localIdPool, parentPoolName);
                newIdValuesList.add(Uint32.valueOf(newIdValue));
            } else {
                getRangeOfIds(parentPoolName, localPoolName, size, newIdValuesList, localIdPool, newIdValue);
            }
            LOG.debug("The newIdValues {} for the idKey {}", newIdValuesList, idKey);
            idUtils.putReleaseIdLatch(uniqueIdKey, new CountDownLatch(1));
            UpdateIdEntryJob job = new UpdateIdEntryJob(parentPoolName, localPoolName, idKey, newIdValuesList, txRunner,
                    idUtils, lockManager);
            jobCoordinator.enqueueJob(parentPoolName, job, IdUtils.RETRY_COUNT);
            futureIdValues.complete(newIdValuesList);
            return newIdValuesList;
        } catch (OperationFailedException | IdManagerException e) {
            idUtils.unlock(lockManager, uniqueIdKey);
            throw e;
        }
    }

    private Long getIdFromLocalPoolCache(IdLocalPool localIdPool, String parentPoolName)
            throws IdManagerException {
        while (true) {
            IdHolder availableIds = localIdPool.getAvailableIds();
            if (availableIds != null) {
                Optional<Long> availableId = availableIds.allocateId();
                if (availableId.isPresent()) {
                    IdHolderSyncJob poolSyncJob =
                            new IdHolderSyncJob(localIdPool.getPoolName(), localIdPool.getAvailableIds(), txRunner,
                                    idUtils);
                    jobCoordinator.enqueueJob(localIdPool.getPoolName(), poolSyncJob, IdUtils.RETRY_COUNT);
                    return availableId.get();
                }
            }
            IdHolder releasedIds = localIdPool.getReleasedIds();
            Optional<Long> releasedId = releasedIds.allocateId();
            if (releasedId.isPresent()) {
                IdHolderSyncJob poolSyncJob =
                        new IdHolderSyncJob(localIdPool.getPoolName(), localIdPool.getReleasedIds(), txRunner,
                                idUtils);
                jobCoordinator.enqueueJob(localIdPool.getPoolName(), poolSyncJob, IdUtils.RETRY_COUNT);
                return releasedId.get();
            }
            long idCount = getIdBlockFromParentPool(parentPoolName, localIdPool);
            if (idCount <= 0) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Unable to allocate Id block from global pool {}", parentPoolName);
                }
                throw new IdManagerException(String.format("Ids exhausted for pool : %s", parentPoolName));
            }
        }
    }

    /**
     * Changes made to availableIds and releasedIds will not be persisted to the datastore.
     */
    private long getIdBlockFromParentPool(String parentPoolName, IdLocalPool localIdPool)
            throws IdManagerException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Allocating block of id from parent pool {}", parentPoolName);
        }
        InstanceIdentifier<IdPool> idPoolInstanceIdentifier = idUtils.getIdPoolInstance(parentPoolName);
        parentPoolName = parentPoolName.intern();
        idUtils.lock(lockManager, parentPoolName);
        try {
            // Check if the childpool already got id block.
            long availableIdCount =
                    localIdPool.getAvailableIds().getAvailableIdCount()
                            + localIdPool.getReleasedIds().getAvailableIdCount();
            if (availableIdCount > 0) {
                return availableIdCount;
            }
            return txRunner.applyWithNewReadWriteTransactionAndSubmit(CONFIGURATION, confTx -> {
                Optional<IdPool> parentIdPool = confTx.read(idPoolInstanceIdentifier).get();
                if (parentIdPool.isPresent()) {
                    return allocateIdBlockFromParentPool(localIdPool, parentIdPool.get(), confTx);
                } else {
                    throw new ExpectedDataObjectNotFoundException(LogicalDatastoreType.CONFIGURATION,
                            idPoolInstanceIdentifier);
                }
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IdManagerException("Error getting id block from parent pool", e);
        } finally {
            idUtils.unlock(lockManager, parentPoolName);
        }
    }

    private long allocateIdBlockFromParentPool(IdLocalPool localPoolCache, IdPool parentIdPool,
            TypedWriteTransaction<Configuration> confTx)
            throws OperationFailedException, IdManagerException {

        long idCount = allocateIdBlockFromAvailableIdsHolder(localPoolCache, parentIdPool, confTx);
        if (idCount > 0) {
            return idCount;
        }

        ReleasedIdsHolderBuilder releasedIdsBuilderParent = IdUtils.getReleaseIdsHolderBuilder(parentIdPool);
        final List<DelayedIdEntries> delayedEntries = releasedIdsBuilderParent.getDelayedIdEntries();
        if (delayedEntries != null) {
            releasedIdsBuilderParent.setDelayedIdEntries(new ArrayList<>(delayedEntries));
        }

        while (true) {
            idCount = allocateIdBlockFromReleasedIdsHolder(localPoolCache, releasedIdsBuilderParent, parentIdPool,
                    confTx);
            if (idCount > 0) {
                return idCount;
            }
            idCount = getIdsFromOtherChildPools(releasedIdsBuilderParent, parentIdPool);
            if (idCount <= 0) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Unable to allocate Id block from global pool");
                }
                throw new IdManagerException(String.format("Ids exhausted for pool : %s", parentIdPool.getPoolName()));
            }

            idCount = allocateIdBlockFromAvailableIdsHolder(localPoolCache, parentIdPool, confTx);
            if (idCount > 0) {
                return idCount;
            }
        }
    }

    private long getIdsFromOtherChildPools(ReleasedIdsHolderBuilder releasedIdsBuilderParent, IdPool parentIdPool)
            throws OperationFailedException {
        @Nullable Map<ChildPoolsKey, ChildPools> childPoolsMap = parentIdPool.nonnullChildPools();
        // Sorting the child pools on last accessed time so that the pool that
        // was not accessed for a long time comes first.

        List<ChildPools> list = childPoolsMap.values().stream().collect(Collectors.toList());
        list.sort(comparing(ChildPools::getLastAccessTime));

        long currentTime = System.currentTimeMillis() / 1000;
        for (ChildPools childPools : childPoolsMap.values()) {
            if (childPools.getLastAccessTime().toJava() + DEFAULT_IDLE_TIME > currentTime) {
                break;
            }
            if (!Objects.equals(childPools.getChildPoolName(), idUtils.getLocalPoolName(parentIdPool.getPoolName()))) {
                InstanceIdentifier<IdPool> idPoolInstanceIdentifier = idUtils
                        .getIdPoolInstance(childPools.getChildPoolName());
                IdPool otherChildPool =
                        singleTxDB.syncRead(LogicalDatastoreType.CONFIGURATION, idPoolInstanceIdentifier);
                ReleasedIdsHolderBuilder releasedIds = IdUtils.getReleaseIdsHolderBuilder(otherChildPool);

                List<DelayedIdEntries> delayedIdEntriesChild = releasedIds.getDelayedIdEntries();
                List<DelayedIdEntries> delayedIdEntriesParent = releasedIdsBuilderParent.getDelayedIdEntries();
                if (delayedIdEntriesParent == null) {
                    delayedIdEntriesParent = new LinkedList<>();
                }
                delayedIdEntriesParent.addAll(delayedIdEntriesChild);
                delayedIdEntriesChild.clear();

                AvailableIdsHolderBuilder availableIds = idUtils.getAvailableIdsHolderBuilder(otherChildPool);
                while (idUtils.isIdAvailable(availableIds)) {
                    long cursor = availableIds.getCursor() + 1;
                    delayedIdEntriesParent.add(idUtils.createDelayedIdEntry(cursor, currentTime));
                    availableIds.setCursor(cursor);
                }

                long totalAvailableIdCount = releasedIds.getDelayedIdEntries().size()
                        + idUtils.getAvailableIdsCount(availableIds);
                long count = releasedIdsBuilderParent.getAvailableIdCount().toJava() + totalAvailableIdCount;
                releasedIdsBuilderParent.setDelayedIdEntries(delayedIdEntriesParent).setAvailableIdCount(count);
                singleTxDB.syncUpdate(LogicalDatastoreType.CONFIGURATION, idPoolInstanceIdentifier,
                        new IdPoolBuilder().withKey(new IdPoolKey(otherChildPool.getPoolName()))
                                .setAvailableIdsHolder(availableIds.build()).setReleasedIdsHolder(releasedIds.build())
                                .build());
                return totalAvailableIdCount;
            }
        }
        return 0;
    }

    private static long allocateIdBlockFromReleasedIdsHolder(IdLocalPool localIdPool,
            ReleasedIdsHolderBuilder releasedIdsBuilderParent, IdPool parentIdPool,
            TypedWriteTransaction<Configuration> confTx) {
        if (releasedIdsBuilderParent.getAvailableIdCount().toJava() == 0) {
            LOG.debug("Ids unavailable in releasedIds of parent pool {}", parentIdPool);
            return 0;
        }
        List<DelayedIdEntries> delayedIdEntriesParent = releasedIdsBuilderParent.getDelayedIdEntries();
        int idCount = Math.min(delayedIdEntriesParent.size(), parentIdPool.getBlockSize().toJava());
        List<DelayedIdEntries> idEntriesToBeRemoved = delayedIdEntriesParent.subList(0, idCount);
        ReleasedIdHolder releasedIds = (ReleasedIdHolder) localIdPool.getReleasedIds();
        List<DelayedIdEntry> delayedIdEntriesLocalCache = releasedIds.getDelayedEntries();
        List<DelayedIdEntry> delayedIdEntriesFromParentPool = idEntriesToBeRemoved
                .stream()
                .map(delayedIdEntry -> new DelayedIdEntry(delayedIdEntry
                        .getId().toJava(), delayedIdEntry.getReadyTimeSec().toJava()))
                .sorted(comparing(DelayedIdEntry::getReadyTimeSec))
                .collect(toCollection(ArrayList::new));
        delayedIdEntriesFromParentPool.addAll(delayedIdEntriesLocalCache);
        releasedIds.replaceDelayedEntries(delayedIdEntriesFromParentPool);
        releasedIds.setAvailableIdCount(releasedIds.getAvailableIdCount() + idCount);
        localIdPool.setReleasedIds(releasedIds);
        delayedIdEntriesParent.removeAll(idEntriesToBeRemoved);
        releasedIdsBuilderParent.setDelayedIdEntries(delayedIdEntriesParent);
        InstanceIdentifier<ReleasedIdsHolder> releasedIdsHolderInstanceIdentifier = InstanceIdentifier
                .builder(IdPools.class).child(IdPool.class,
                        new IdPoolKey(parentIdPool.getPoolName())).child(ReleasedIdsHolder.class).build();
        releasedIdsBuilderParent.setAvailableIdCount(releasedIdsBuilderParent.getAvailableIdCount().toJava() - idCount);
        LOG.debug("Allocated {} ids from releasedIds of parent pool {}", idCount, parentIdPool);
        confTx.mergeParentStructureMerge(releasedIdsHolderInstanceIdentifier, releasedIdsBuilderParent.build());
        return idCount;
    }

    private long allocateIdBlockFromAvailableIdsHolder(IdLocalPool localIdPool, IdPool parentIdPool,
            TypedWriteTransaction<Configuration> confTx) {
        long idCount = 0;
        AvailableIdsHolderBuilder availableIdsBuilderParent = idUtils.getAvailableIdsHolderBuilder(parentIdPool);
        long end = availableIdsBuilderParent.getEnd().toJava();
        long cur = availableIdsBuilderParent.getCursor();
        if (!idUtils.isIdAvailable(availableIdsBuilderParent)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Ids exhausted in parent pool {}", parentIdPool);
            }
            return idCount;
        }
        // Update availableIdsHolder of Local Pool
        idCount = Math.min(end - cur, parentIdPool.getBlockSize().toJava());
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
        confTx.mergeParentStructureMerge(availableIdsHolderInstanceIdentifier, availableIdsBuilderParent.build());
        return idCount;
    }

    private ReleaseIdOutputBuilder releaseIdFromLocalPool(String parentPoolName, String localPoolName, String idKey)
            throws IdManagerException, ReadFailedException, ExecutionException, InterruptedException {
        String idLatchKey = idUtils.getUniqueKey(parentPoolName, idKey);
        LOG.debug("Releasing ID {} from pool {}", idKey, localPoolName);
        CountDownLatch latch = idUtils.getReleaseIdLatch(idLatchKey);
        if (latch != null) {
            try {
                if (!latch.await(10, TimeUnit.SECONDS)) {
                    LOG.warn("Timed out while releasing id {} from id pool {}", idKey, parentPoolName);
                }
            } catch (InterruptedException ignored) {
                LOG.warn("Thread interrupted while releasing id {} from id pool {}", idKey, parentPoolName);
            } finally {
                idUtils.removeReleaseIdLatch(idLatchKey);
            }
        }
        localPoolName = localPoolName.intern();
        InstanceIdentifier<IdPool> parentIdPoolInstanceIdentifier = idUtils.getIdPoolInstance(parentPoolName);
        IdPool parentIdPool = singleTxDB.syncRead(LogicalDatastoreType.CONFIGURATION, parentIdPoolInstanceIdentifier);
        @Nullable Map<IdEntriesKey, IdEntries> idEntries = parentIdPool.getIdEntries();
        if (idEntries == null) {
            throw new IdDoesNotExistException(parentPoolName, idKey);
        }
        InstanceIdentifier<IdEntries> existingId = idUtils.getIdEntry(parentIdPoolInstanceIdentifier, idKey);
        Optional<IdEntries> existingIdEntryObject =
                singleTxDB.syncReadOptional(LogicalDatastoreType.CONFIGURATION, existingId);
        if (!existingIdEntryObject.isPresent()) {
            LOG.info("Specified Id key {} does not exist in id pool {}", idKey, parentPoolName);
            idUtils.unlock(lockManager, idLatchKey);
            throw new IdManagerException(String.format("Specified Id key %s does not exist in id pool %s",
                    idKey, parentPoolName));
        }
        IdEntries existingIdEntry = existingIdEntryObject.get();
        List<Uint32> idValuesList = nonnull(existingIdEntry.getIdValue());
        IdLocalPool localIdPoolCache = localPool.get(parentPoolName);
        boolean isRemoved = idEntries.values().contains(existingIdEntry);
        LOG.debug("The entry {} is removed {}", existingIdEntry, isRemoved);
        updateDelayedEntriesInLocalCache(idValuesList, parentPoolName, localIdPoolCache);
        IdHolderSyncJob poolSyncJob = new IdHolderSyncJob(localPoolName, localIdPoolCache.getReleasedIds(), txRunner,
                idUtils);
        jobCoordinator.enqueueJob(localPoolName, poolSyncJob, IdUtils.RETRY_COUNT);
        scheduleCleanUpTask(localIdPoolCache, parentPoolName, parentIdPool.getBlockSize().toJava());
        LOG.debug("Released id ({}, {}) from pool {}", idKey, idValuesList, localPoolName);
        // Updating id entries in the parent pool. This will be used for restart scenario
        UpdateIdEntryJob job = new UpdateIdEntryJob(parentPoolName, localPoolName, idKey, null, txRunner, idUtils,
                        lockManager);
        jobCoordinator.enqueueJob(parentPoolName, job, IdUtils.RETRY_COUNT);
        return new ReleaseIdOutputBuilder().setIdValues(idValuesList);
    }

    private void scheduleCleanUpTask(final IdLocalPool localIdPoolCache,
            final String parentPoolName, final int blockSize) {
        TimerTask scheduledTask = new TimerTask() {
            @Override
            public void run() {
                CleanUpJob job =
                        new CleanUpJob(localIdPoolCache, txRunner, broker, parentPoolName, blockSize, lockManager,
                                idUtils, jobCoordinator);
                jobCoordinator.enqueueJob(localIdPoolCache.getPoolName(), job, IdUtils.RETRY_COUNT);
            }
        };
        cleanJobTimer.schedule(scheduledTask, IdUtils.DEFAULT_DELAY_TIME * 1000);
    }

    private IdPool createGlobalPool(TypedReadWriteTransaction<Configuration> confTx, String poolName, long low,
            long high, long blockSize) throws IdManagerException {
        IdPool idPool;
        InstanceIdentifier<IdPool> idPoolInstanceIdentifier = idUtils.getIdPoolInstance(poolName);
        try {
            Optional<IdPool> existingIdPool = confTx.read(idPoolInstanceIdentifier).get();
            if (!existingIdPool.isPresent()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Creating new global pool {}", poolName);
                }
                idPool = idUtils.createGlobalPool(poolName, low, high, blockSize);
                confTx.mergeParentStructurePut(idPoolInstanceIdentifier, idPool);
            } else {
                idPool = existingIdPool.get();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("GlobalPool exists {}", idPool);
                }
            }
            return idPool;
        } catch (ExecutionException | InterruptedException e) {
            throw new IdManagerException("Error retrieving the existing id pool for " + poolName, e);
        }
    }

    private IdLocalPool createLocalPool(TypedWriteTransaction<Configuration> confTx, String localPoolName,
            IdPool idPool)
            throws OperationFailedException, IdManagerException {
        localPoolName = localPoolName.intern();
        IdLocalPool idLocalPool = new IdLocalPool(idUtils, localPoolName);
        allocateIdBlockFromParentPool(idLocalPool, idPool, confTx);
        String parentPool = idPool.getPoolName();
        localPool.put(parentPool, idLocalPool);
        LocalPoolCreateJob job = new LocalPoolCreateJob(idLocalPool, txRunner, idPool.getPoolName(),
                idPool.getBlockSize().toJava(), idUtils);
        jobCoordinator.enqueueJob(localPoolName, job, IdUtils.RETRY_COUNT);
        return idLocalPool;
    }

    private void deletePool(String poolName) {
        LocalPoolDeleteJob job = new LocalPoolDeleteJob(poolName, txRunner, idUtils);
        jobCoordinator.enqueueJob(poolName, job, IdUtils.RETRY_COUNT);
    }

    public void poolDeleted(String parentPoolName, String poolName) {
        IdLocalPool idLocalPool = localPool.get(parentPoolName);
        if (idLocalPool != null) {
            if (idLocalPool.getPoolName().equals(poolName)) {
                localPool.remove(parentPoolName);
            }
        }
    }

    private void updateDelayedEntriesInLocalCache(List<Uint32> idsList, String parentPoolName,
            IdLocalPool localPoolCache) {
        for (Uint32 idValue : idsList) {
            localPoolCache.getReleasedIds().addId(idValue.toJava());
        }
        localPool.put(parentPoolName, localPoolCache);
    }

    private List<Uint32> checkForIdInIdEntries(String parentPoolName, String idKey, String uniqueIdKey,
            CompletableFuture<List<Uint32>> futureIdValues, boolean hasExistingFutureIdValues)
            throws IdManagerException, InterruptedException , ExecutionException {
        InstanceIdentifier<IdPool> parentIdPoolInstanceIdentifier = idUtils.getIdPoolInstance(parentPoolName);
        InstanceIdentifier<IdEntries> existingId = idUtils.getIdEntry(parentIdPoolInstanceIdentifier, idKey);
        idUtils.lock(lockManager, uniqueIdKey);
        List<Uint32> newIdValuesList = new ArrayList<>();
        Optional<IdEntries> existingIdEntry =
                singleTxDB.syncReadOptional(LogicalDatastoreType.CONFIGURATION, existingId);
        if (existingIdEntry.isPresent()) {
            newIdValuesList = existingIdEntry.get().getIdValue();
            LOG.debug("Existing ids {} for the key {} ", newIdValuesList, idKey);
            // Inform other waiting threads about this new value.
            futureIdValues.complete(newIdValuesList);
            // This is to avoid stale entries in the map. If this thread had populated the map,
            // then the entry should be removed.
            if (!hasExistingFutureIdValues) {
                idUtils.removeAllocatedIds(uniqueIdKey);
            }
            idUtils.unlock(lockManager, uniqueIdKey);
            return newIdValuesList;
        }
        return newIdValuesList;
    }

    private IdLocalPool getOrCreateLocalIdPool(String parentPoolName, String localPoolName)
        throws IdManagerException {
        IdLocalPool localIdPool = localPool.get(parentPoolName);
        if (localIdPool == null) {
            idUtils.lock(lockManager, parentPoolName);
            try {
                // Check if a previous thread that got the cluster-wide lock
                // first, has created the localPool
                try {
                    InstanceIdentifier<IdPool> childIdPoolInstanceIdentifier = idUtils
                            .getIdPoolInstance(localPoolName);
                    Optional<IdPool> childIdPoolOpt = singleTxDB.syncReadOptional(LogicalDatastoreType.CONFIGURATION,
                            childIdPoolInstanceIdentifier);
                    if (childIdPoolOpt.isPresent()) {
                        updateLocalIdPoolCache(childIdPoolOpt.get(), parentPoolName);
                    }
                }
                catch (ExecutionException | InterruptedException ex) {
                    LOG.debug("Failed to read id pool {} due to {}", localPoolName, ex.getMessage());
                }
                if (localPool.get(parentPoolName) == null) {
                    try {
                        return txRunner.applyWithNewReadWriteTransactionAndSubmit(CONFIGURATION, confTx -> {
                            InstanceIdentifier<IdPool> parentIdPoolInstanceIdentifier = idUtils
                                    .getIdPoolInstance(parentPoolName);
                            Optional<IdPool> parentIdPool = confTx.read(parentIdPoolInstanceIdentifier).get();
                            if (parentIdPool.isPresent()) {
                                // Return localIdPool
                                return createLocalPool(confTx, localPoolName, parentIdPool.get());
                            } else {
                                throw new ExpectedDataObjectNotFoundException(LogicalDatastoreType.CONFIGURATION,
                                        parentIdPoolInstanceIdentifier);
                            }
                        }).get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new IdManagerException("Error creating a local id pool", e);
                    }
                } else {
                    localIdPool = localPool.get(parentPoolName);
                }
            } finally {
                idUtils.unlock(lockManager, parentPoolName);
            }
        }
        return localIdPool;
    }

    private void getRangeOfIds(String parentPoolName, String localPoolName, long size, List<Uint32> newIdValuesList,
            IdLocalPool localIdPool, long newIdValue) throws ReadFailedException, IdManagerException {
        InstanceIdentifier<IdPool> parentIdPoolInstanceIdentifier1 = idUtils.getIdPoolInstance(parentPoolName);
        IdPool parentIdPool = singleTxDB.syncRead(LogicalDatastoreType.CONFIGURATION, parentIdPoolInstanceIdentifier1);
        long totalAvailableIdCount = localIdPool.getAvailableIds().getAvailableIdCount()
                + localIdPool.getReleasedIds().getAvailableIdCount();
        AvailableIdsHolderBuilder availableParentIds = idUtils.getAvailableIdsHolderBuilder(parentIdPool);
        ReleasedIdsHolderBuilder releasedParentIds = IdUtils.getReleaseIdsHolderBuilder(parentIdPool);
        totalAvailableIdCount = totalAvailableIdCount + releasedParentIds.getAvailableIdCount().toJava()
                + idUtils.getAvailableIdsCount(availableParentIds);
        if (totalAvailableIdCount > size) {
            while (size > 0) {
                try {
                    newIdValue = getIdFromLocalPoolCache(localIdPool, parentPoolName);
                } catch (IdManagerException e) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Releasing IDs to pool {}", localPoolName);
                    }
                    // Releasing the IDs added in newIdValuesList since
                    // a null list would be returned now, as the
                    // requested size of list IDs exceeds the number of
                    // available IDs.
                    updateDelayedEntriesInLocalCache(newIdValuesList, parentPoolName, localIdPool);
                }
                newIdValuesList.add(Uint32.valueOf(newIdValue));
                size--;
            }
        } else {
            throw new IdManagerException(String.format("Ids exhausted for pool : %s", parentPoolName));
        }
    }
}
