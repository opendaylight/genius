/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.idmanager;

import static org.opendaylight.mdsal.binding.api.WriteTransaction.CREATE_MISSING_PARENTS;

import com.google.common.net.InetAddresses;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Singleton;
import org.opendaylight.genius.idmanager.ReleasedIdHolder.DelayedIdEntry;
import org.opendaylight.genius.infra.Datastore.Configuration;
import org.opendaylight.genius.infra.TypedWriteTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdPools;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPool;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPoolBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPoolKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.AvailableIdsHolder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.AvailableIdsHolderBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.ChildPools;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.ChildPoolsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.ChildPoolsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.IdEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.IdEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.IdEntriesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.ReleasedIdsHolder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.ReleasedIdsHolderBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.released.ids.DelayedIdEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.released.ids.DelayedIdEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.LockManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.TryLockInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.TryLockInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.TryLockOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.UnlockInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.UnlockInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.UnlockOutput;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class IdUtils {

    private static final Logger LOG = LoggerFactory.getLogger(IdUtils.class);

    public static final long DEFAULT_DELAY_TIME = 30;
    private static final long DEFAULT_AVAILABLE_ID_COUNT = 0;
    private static final int DEFAULT_BLOCK_SIZE_DIFF = 10;
    public static final int RETRY_COUNT = 6;

    private final ConcurrentHashMap<String, CompletableFuture<List<Uint32>>> allocatedIdMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CountDownLatch> releaseIdLatchMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> poolUpdatedMap = new ConcurrentHashMap<>();

    private final int bladeId;

    public IdUtils() throws UnknownHostException {
        bladeId = InetAddresses.coerceToInteger(InetAddress.getLocalHost());
    }

    public CompletableFuture<List<Uint32>> removeAllocatedIds(String uniqueIdKey) {
        return allocatedIdMap.remove(uniqueIdKey);
    }

    public CompletableFuture<List<Uint32>> putAllocatedIdsIfAbsent(String uniqueIdKey,
            CompletableFuture<List<Uint32>> futureIdValues) {
        return allocatedIdMap.putIfAbsent(uniqueIdKey, futureIdValues);
    }

    public void putReleaseIdLatch(String uniqueIdKey, CountDownLatch latch) {
        releaseIdLatchMap.put(uniqueIdKey, latch);
    }

    public CountDownLatch getReleaseIdLatch(String uniqueIdKey) {
        return releaseIdLatchMap.get(uniqueIdKey);
    }

    public CountDownLatch removeReleaseIdLatch(String uniqueIdKey) {
        return releaseIdLatchMap.remove(uniqueIdKey);
    }

    public InstanceIdentifier<IdEntries> getIdEntry(InstanceIdentifier<IdPool> poolName, String idKey) {
        InstanceIdentifier.InstanceIdentifierBuilder<IdEntries> idEntriesBuilder = poolName
                .builder().child(IdEntries.class, new IdEntriesKey(idKey));
        return idEntriesBuilder.build();
    }

    public IdEntries createIdEntries(String idKey, List<Uint32> newIdVals) {
        return new IdEntriesBuilder().withKey(new IdEntriesKey(idKey))
                .setIdKey(idKey).setIdValue(newIdVals).build();
    }

    public DelayedIdEntries createDelayedIdEntry(long idValue, long delayTime) {
        return new DelayedIdEntriesBuilder()
                .setId(idValue)
                .setReadyTimeSec(delayTime).build();
    }

    protected IdPool createGlobalPool(String poolName, long low, long high, long blockSize) {
        AvailableIdsHolder availableIdsHolder = createAvailableIdsHolder(low, high, low - 1);
        ReleasedIdsHolder releasedIdsHolder = createReleasedIdsHolder(DEFAULT_AVAILABLE_ID_COUNT, 0);
        int size = (int) blockSize;
        return new IdPoolBuilder().withKey(new IdPoolKey(poolName))
                .setBlockSize(size).setPoolName(poolName)
                .setAvailableIdsHolder(availableIdsHolder)
                .setReleasedIdsHolder(releasedIdsHolder).build();
    }

    public AvailableIdsHolder createAvailableIdsHolder(long low, long high, long cursor) {
        return new AvailableIdsHolderBuilder()
                .setStart(low).setEnd(high).setCursor(cursor).build();
    }

    protected ReleasedIdsHolder createReleasedIdsHolder(long availableIdCount, long delayTime) {
        return new ReleasedIdsHolderBuilder()
                .setAvailableIdCount(availableIdCount)
                .setDelayedTimeSec(delayTime).build();
    }

    public InstanceIdentifier<IdPool> getIdPoolInstance(String poolName) {
        InstanceIdentifier.InstanceIdentifierBuilder<IdPool> idPoolBuilder = InstanceIdentifier
                .builder(IdPools.class).child(IdPool.class,
                        new IdPoolKey(poolName));
        return idPoolBuilder.build();
    }

    public InstanceIdentifier<ReleasedIdsHolder> getReleasedIdsHolderInstance(String poolName) {
        InstanceIdentifier.InstanceIdentifierBuilder<ReleasedIdsHolder> releasedIdsHolder = InstanceIdentifier
                .builder(IdPools.class).child(IdPool.class,
                        new IdPoolKey(poolName)).child(ReleasedIdsHolder.class);
        return releasedIdsHolder.build();
    }

    protected boolean isIdAvailable(AvailableIdsHolderBuilder availableIds) {
        if (availableIds.getCursor() != null && availableIds.getEnd() != null) {
            return availableIds.getCursor() < availableIds.getEnd().toJava();
        }
        return false;
    }

    // public only to re-use this from IdManagerTest
    public String getLocalPoolName(String poolName) {
        return poolName + "." + bladeId;
    }

    protected ChildPools createChildPool(String childPoolName) {
        return new ChildPoolsBuilder().withKey(new ChildPoolsKey(childPoolName)).setChildPoolName(childPoolName)
                .setLastAccessTime(System.currentTimeMillis() / 1000).build();
    }

    protected AvailableIdsHolderBuilder getAvailableIdsHolderBuilder(IdPool pool) {
        AvailableIdsHolder availableIds = pool.getAvailableIdsHolder();
        if (availableIds != null) {
            return new AvailableIdsHolderBuilder(availableIds);
        }
        return new AvailableIdsHolderBuilder();
    }

    protected static ReleasedIdsHolderBuilder getReleaseIdsHolderBuilder(IdPool pool) {
        ReleasedIdsHolder releasedIds = pool.getReleasedIdsHolder();
        if (releasedIds != null) {
            return new ReleasedIdsHolderBuilder(releasedIds);
        }
        return new ReleasedIdsHolderBuilder();
    }

    /**
     * Changes made to the parameters passed are not persisted to the Datastore.
     * Method invoking should ensure that these gets persisted.
     */
    public void freeExcessAvailableIds(ReleasedIdHolder releasedIdHolder,
        ReleasedIdsHolderBuilder releasedIdsParent, long idCountToBeFreed) {
        List<DelayedIdEntries> existingDelayedIdEntriesInParent = releasedIdsParent.getDelayedIdEntries();
        long availableIdCountChild = releasedIdHolder.getAvailableIdCount();
        if (existingDelayedIdEntriesInParent == null) {
            existingDelayedIdEntriesInParent = new LinkedList<>();
        }
        idCountToBeFreed = Math.min(idCountToBeFreed, availableIdCountChild);
        for (int index = 0; index < idCountToBeFreed; index++) {
            Optional<Long> idValueOptional = releasedIdHolder.allocateId();
            if (!idValueOptional.isPresent()) {
                break;
            }
            long idValue = idValueOptional.get();
            DelayedIdEntries delayedIdEntries = new DelayedIdEntriesBuilder().setId(idValue)
                    .setReadyTimeSec(System.currentTimeMillis() / 1000).build();
            existingDelayedIdEntriesInParent.add(delayedIdEntries);
        }
        long availableIdCountParent = releasedIdsParent.getAvailableIdCount().toJava();
        releasedIdsParent.setDelayedIdEntries(existingDelayedIdEntriesInParent)
                .setAvailableIdCount(availableIdCountParent + idCountToBeFreed);
    }

    public InstanceIdentifier<IdEntries> getIdEntriesInstanceIdentifier(String poolName, String idKey) {
        return InstanceIdentifier
                .builder(IdPools.class).child(IdPool.class,
                        new IdPoolKey(poolName)).child(IdEntries.class, new IdEntriesKey(idKey)).build();
    }

    protected InstanceIdentifier<ChildPools> getChildPoolsInstanceIdentifier(String poolName, String localPoolName) {
        return InstanceIdentifier
                .builder(IdPools.class)
                .child(IdPool.class, new IdPoolKey(poolName))
                .child(ChildPools.class, new ChildPoolsKey(localPoolName)).build();
    }

    public long computeBlockSize(long low, long high) {
        long blockSize;

        long diff = high - low;
        if (diff > DEFAULT_BLOCK_SIZE_DIFF) {
            blockSize = diff / DEFAULT_BLOCK_SIZE_DIFF;
        } else {
            blockSize = 1;
        }
        return blockSize;
    }

    public long getAvailableIdsCount(AvailableIdsHolderBuilder availableIds) {
        if (availableIds != null && isIdAvailable(availableIds)) {
            return availableIds.getEnd().toJava() - availableIds.getCursor();
        }
        return 0;
    }

    public void lock(LockManagerService lockManager, String poolName) throws IdManagerException {
        TryLockInput input = new TryLockInputBuilder().setLockName(poolName).build();
        Future<RpcResult<TryLockOutput>> result = lockManager.tryLock(input);
        try {
            if (result != null && result.get().isSuccessful()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Acquired lock {}", poolName);
                }
            } else {
                throw new IdManagerException(String.format("Unable to getLock for pool %s", poolName));
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Unable to getLock for pool {}", poolName, e);
            throw new RuntimeException(String.format("Unable to getLock for pool %s", poolName), e);
        }
    }

    public void unlock(LockManagerService lockManager, String poolName) {
        UnlockInput input = new UnlockInputBuilder().setLockName(poolName).build();
        Future<RpcResult<UnlockOutput>> result = lockManager.unlock(input);
        try {
            if (result != null && result.get().isSuccessful()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Unlocked {}", poolName);
                }
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Unable to unlock pool {}", poolName);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Unable to unlock for pool {}", poolName, e);
            throw new RuntimeException(String.format("Unable to unlock pool %s", poolName), e);
        }
    }

    public InstanceIdentifier<IdPools> getIdPools() {
        return InstanceIdentifier.builder(IdPools.class).build();
    }

    public void syncReleaseIdHolder(ReleasedIdHolder releasedIdHolder, IdPoolBuilder idPool) {
        long delayTime = releasedIdHolder.getTimeDelaySec();
        ReleasedIdsHolderBuilder releasedIdsBuilder = new ReleasedIdsHolderBuilder();
        List<DelayedIdEntries> delayedIdEntriesList = new ArrayList<>();
        List<DelayedIdEntry> delayList = releasedIdHolder.getDelayedEntries();
        for (DelayedIdEntry delayedId : delayList) {
            DelayedIdEntries delayedIdEntry = createDelayedIdEntry(delayedId.getId(), delayedId.getReadyTimeSec());
            delayedIdEntriesList.add(delayedIdEntry);
        }
        releasedIdsBuilder.setAvailableIdCount((long) delayedIdEntriesList.size()).setDelayedTimeSec(delayTime)
                .setDelayedIdEntries(delayedIdEntriesList);
        idPool.setReleasedIdsHolder(releasedIdsBuilder.build());
    }

    public void syncAvailableIdHolder(AvailableIdHolder availableIdHolder, IdPoolBuilder idPool) {
        long cur = availableIdHolder.getCur().get();
        long low = availableIdHolder.getLow();
        long high = availableIdHolder.getHigh();
        AvailableIdsHolder availableIdsHolder = createAvailableIdsHolder(low, high, cur);
        idPool.setAvailableIdsHolder(availableIdsHolder);
    }

    public void updateChildPool(TypedWriteTransaction<Configuration> tx, String poolName, String localPoolName) {
        ChildPools childPool = createChildPool(localPoolName);
        InstanceIdentifier<ChildPools> childPoolInstanceIdentifier =
                getChildPoolsInstanceIdentifier(poolName, localPoolName);
        tx.merge(childPoolInstanceIdentifier, childPool, CREATE_MISSING_PARENTS);
    }

    public void incrementPoolUpdatedMap(String localPoolName) {
        AtomicInteger value = poolUpdatedMap.putIfAbsent(localPoolName, new AtomicInteger(0));
        if (value == null) {
            value = poolUpdatedMap.get(localPoolName);
        }
        value.incrementAndGet();
    }

    public void decrementPoolUpdatedMap(String localPoolName) {
        AtomicInteger value = poolUpdatedMap.get(localPoolName);
        if (value != null && value.get() >= 1) {
            value.decrementAndGet();
        }
    }

    public boolean getPoolUpdatedMap(String localPoolName) {
        AtomicInteger value = poolUpdatedMap.get(localPoolName);
        return value != null && value.get() > 0;
    }

    public void removeFromPoolUpdatedMap(String localPoolName) {
        poolUpdatedMap.remove(localPoolName);
    }

    public String getUniqueKey(String parentPoolName, String idKey) {
        return parentPoolName + idKey;
    }
}
