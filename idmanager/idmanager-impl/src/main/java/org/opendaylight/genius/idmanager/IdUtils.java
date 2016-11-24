/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.idmanager;

import com.google.common.base.Optional;
import com.google.common.net.InetAddresses;
import com.google.common.util.concurrent.CheckedFuture;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.idmanager.ReleasedIdHolder.DelayedIdEntry;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.LockInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.LockInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.LockManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.UnlockInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.UnlockInputBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class IdUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdUtils.class);

    public static final long DEFAULT_DELAY_TIME = 30;
    private static final long DEFAULT_AVAILABLE_ID_COUNT = 0;
    private static final int DEFAULT_BLOCK_SIZE_DIFF = 10;
    public static final int RETRY_COUNT = 6;
    public static final String ID_POOL_CACHE = "ID_POOL_CACHE";

    private final ConcurrentHashMap<String, Integer> poolUpdatedMap = new ConcurrentHashMap<>();

    private static int BLADE_ID;

    static {
        try {
            BLADE_ID = InetAddresses.coerceToInteger(InetAddress.getLocalHost());
        } catch (Exception e) {
            LOGGER.error("IdManager static { InetAddresses.coerceToInteger(InetAddress.getLocalHost()) } failed "
                    + "due to caught Exception", e);
        }
    }

    protected InstanceIdentifier<IdEntries> getIdEntry(InstanceIdentifier<IdPool> poolName, String idKey) {
        InstanceIdentifier.InstanceIdentifierBuilder<IdEntries> idEntriesBuilder = poolName
                .builder().child(IdEntries.class, new IdEntriesKey(idKey));
        InstanceIdentifier<IdEntries> idEntry = idEntriesBuilder.build();
        return idEntry;
    }

    public IdEntries createIdEntries(String idKey, List<Long> newIdVals) {
        return new IdEntriesBuilder().setKey(new IdEntriesKey(idKey))
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
        return new IdPoolBuilder().setKey(new IdPoolKey(poolName))
                .setBlockSize(size).setPoolName(poolName)
                .setAvailableIdsHolder(availableIdsHolder)
                .setReleasedIdsHolder(releasedIdsHolder).build();
    }

    public AvailableIdsHolder createAvailableIdsHolder(long low, long high, long cursor) {
        AvailableIdsHolder availableIdsHolder = new AvailableIdsHolderBuilder()
                .setStart(low).setEnd(high).setCursor(cursor).build();
        return availableIdsHolder;
    }

    protected ReleasedIdsHolder createReleasedIdsHolder(long availableIdCount, long delayTime) {
        ReleasedIdsHolder releasedIdsHolder = new ReleasedIdsHolderBuilder()
                .setAvailableIdCount(availableIdCount)
                .setDelayedTimeSec(delayTime).build();
        return releasedIdsHolder;
    }

    public InstanceIdentifier<IdPool> getIdPoolInstance(String poolName) {
        InstanceIdentifier.InstanceIdentifierBuilder<IdPool> idPoolBuilder = InstanceIdentifier
                .builder(IdPools.class).child(IdPool.class,
                        new IdPoolKey(poolName));
        InstanceIdentifier<IdPool> id = idPoolBuilder.build();
        return id;
    }

    public InstanceIdentifier<ReleasedIdsHolder> getReleasedIdsHolderInstance(String poolName) {
        InstanceIdentifier.InstanceIdentifierBuilder<ReleasedIdsHolder> releasedIdsHolder = InstanceIdentifier
                .builder(IdPools.class).child(IdPool.class,
                        new IdPoolKey(poolName)).child(ReleasedIdsHolder.class);
        InstanceIdentifier<ReleasedIdsHolder> releasedIds = releasedIdsHolder.build();
        return releasedIds;
    }

    protected boolean isIdAvailable(AvailableIdsHolderBuilder availableIds) {
        if (availableIds.getCursor() != null && availableIds.getEnd() != null) {
            return availableIds.getCursor() < availableIds.getEnd();
        }
        return false;
    }

    protected String getLocalPoolName(String poolName) {
        return poolName + "." + BLADE_ID;
    }

    protected ChildPools createChildPool(String childPoolName) {
        return new ChildPoolsBuilder().setKey(new ChildPoolsKey(childPoolName)).setChildPoolName(childPoolName)
                .setLastAccessTime(System.currentTimeMillis() / 1000).build();
    }

    protected AvailableIdsHolderBuilder getAvailableIdsHolderBuilder(IdPool pool) {
        AvailableIdsHolder availableIds = pool.getAvailableIdsHolder();
        if (availableIds != null ) {
            return new AvailableIdsHolderBuilder(availableIds);
        }
        return new AvailableIdsHolderBuilder();
    }

    protected ReleasedIdsHolderBuilder getReleaseIdsHolderBuilder(IdPool pool) {
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
        long availableIdCountParent = releasedIdsParent.getAvailableIdCount();
        releasedIdsParent.setDelayedIdEntries(existingDelayedIdEntriesInParent)
                .setAvailableIdCount(availableIdCountParent + idCountToBeFreed);
    }

    public InstanceIdentifier<IdEntries> getIdEntriesInstanceIdentifier(String poolName, String idKey) {
        InstanceIdentifier<IdEntries> idEntries = InstanceIdentifier
                .builder(IdPools.class).child(IdPool.class,
                        new IdPoolKey(poolName)).child(IdEntries.class, new IdEntriesKey(idKey)).build();
        return idEntries;
    }

    protected InstanceIdentifier<ChildPools> getChildPoolsInstanceIdentifier(String poolName, String localPoolName) {
        InstanceIdentifier<ChildPools> childPools = InstanceIdentifier
                .builder(IdPools.class)
                .child(IdPool.class, new IdPoolKey(poolName))
                .child(ChildPools.class, new ChildPoolsKey(localPoolName)).build();
        return childPools;
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
            return availableIds.getEnd() - availableIds.getCursor();
        }
        return 0;
    }

    public void lockPool(LockManagerService lockManager, String poolName) {
        LockInput input = new LockInputBuilder().setLockName(poolName).build();
        Future<RpcResult<Void>> result = lockManager.lock(input);
        try {
            if (result != null && result.get().isSuccessful()) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Acquired lock {}", poolName);
                }
            } else {
                throw new RuntimeException(String.format("Unable to getLock for pool %s", poolName));
            }
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Unable to getLock for pool {}", poolName, e);
            throw new RuntimeException(String.format("Unable to getLock for pool %s", poolName), e.getCause());
        }
    }

    public void unlockPool(LockManagerService lockManager, String poolName) {
        UnlockInput input = new UnlockInputBuilder().setLockName(poolName).build();
        Future<RpcResult<Void>> result = lockManager.unlock(input);
        try {
            if (result != null && result.get().isSuccessful()) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Unlocked {}", poolName);
                }
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Unable to unlock pool {}", poolName);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Unable to unlock for pool {}", poolName, e);
            throw new RuntimeException(String.format("Unable to unlock pool %s", poolName), e.getCause());
        }
    }

    public void submitTransaction(WriteTransaction tx) {
        CheckedFuture<Void, TransactionCommitFailedException> futures = tx.submit();
        try {
            futures.get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Error writing to datastore tx", e);
            throw new RuntimeException(e);
        }
    }

    public InstanceIdentifier<IdPools> getIdPools() {
        InstanceIdentifier.InstanceIdentifierBuilder<IdPools> idPoolsBuilder = InstanceIdentifier
                .builder(IdPools.class);
        InstanceIdentifier<IdPools> id = idPoolsBuilder.build();
        return id;
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

    public void updateChildPool(WriteTransaction tx, String poolName, String localPoolName) {
        ChildPools childPool = createChildPool(localPoolName);
        InstanceIdentifier<ChildPools> childPoolInstanceIdentifier =
                getChildPoolsInstanceIdentifier(poolName, localPoolName);
        tx.merge(LogicalDatastoreType.CONFIGURATION, childPoolInstanceIdentifier, childPool, true);
    }

    public void incrementPoolUpdatedMap(String localPoolName) {
        Integer value = poolUpdatedMap.get(localPoolName);
        if (value == null) {
            value = 0;
        }
        poolUpdatedMap.put(localPoolName, value + 1);
    }

    public void decrementPoolUpdatedMap(String localPoolName) {
        Integer value = poolUpdatedMap.get(localPoolName);
        if (value == null) {
            value = 1;
        }
        poolUpdatedMap.put(localPoolName, value - 1);
    }

    public boolean getPoolUpdatedMap(String localPoolName) {
        Integer value = poolUpdatedMap.get(localPoolName);
        return value != null && value >= 0;
    }

    public void removeFromPoolUpdatedMap(String localPoolName) {
        poolUpdatedMap.remove(localPoolName);
    }
}
