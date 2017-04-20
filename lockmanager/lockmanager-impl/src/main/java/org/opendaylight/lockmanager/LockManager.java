/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lockmanager;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.LockInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.LockManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.TryLockInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.UnlockInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.locks.Lock;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class LockManager implements LockManagerService {

    private final ConcurrentHashMap<String, CompletableFuture<Void>> lockSynchronizerMap =
            new ConcurrentHashMap<>();

    private static final Logger LOG = LoggerFactory.getLogger(LockManager.class);

    private static final int DEFAULT_RETRY_COUNT = 3;
    private static final int DEFAULT_WAIT_TIME_IN_MILLIS = 1000;

    private final DataBroker broker;

    @Inject
    public LockManager(final DataBroker dataBroker) {
        this.broker = dataBroker;
    }

    @PostConstruct
    public void start() {
        LOG.info("{} start", getClass().getSimpleName());
    }

    @PreDestroy
    public void close() {
        LOG.info("{} close", getClass().getSimpleName());
    }

    @Override
    public Future<RpcResult<Void>> lock(LockInput input) {
        String lockName = input.getLockName();
        LOG.info("Locking {}", lockName);
        InstanceIdentifier<Lock> lockInstanceIdentifier = LockManagerUtils.getLockInstanceIdentifier(lockName);
        Lock lockData = LockManagerUtils.buildLockData(lockName);
        try {
            getLock(lockInstanceIdentifier, lockData);
            RpcResultBuilder<Void> lockRpcBuilder = RpcResultBuilder.success();
            LOG.info("Acquired lock {}", lockName);
            return Futures.immediateFuture(lockRpcBuilder.build());
        } catch (InterruptedException e) {
            RpcResultBuilder<Void> lockRpcBuilder = RpcResultBuilder.failed();
            LOG.info("Failed to get lock {}", lockName, e);
            return Futures.immediateFuture(lockRpcBuilder.build());
        }
    }

    @Override
    public Future<RpcResult<Void>> tryLock(TryLockInput input) {
        String lockName = input.getLockName();
        LOG.info("Locking {}", lockName);
        long waitTime = input.getTime() == null ? DEFAULT_WAIT_TIME_IN_MILLIS * DEFAULT_RETRY_COUNT : input.getTime();
        TimeUnit timeUnit = input.getTimeUnit() == null ? TimeUnit.MILLISECONDS
                : LockManagerUtils.convertToTimeUnit(input.getTimeUnit());
        waitTime = timeUnit.toMillis(waitTime);
        long retryCount = waitTime / DEFAULT_WAIT_TIME_IN_MILLIS;
        InstanceIdentifier<Lock> lockInstanceIdentifier = LockManagerUtils.getLockInstanceIdentifier(lockName);
        Lock lockData = LockManagerUtils.buildLockData(lockName);

        RpcResultBuilder<Void> lockRpcBuilder;
        try {
            if (getLock(lockInstanceIdentifier, lockData, retryCount)) {
                lockRpcBuilder = RpcResultBuilder.success();
                LOG.info("Acquired lock {}", lockName);
            } else {
                lockRpcBuilder = RpcResultBuilder.failed();
                LOG.info("Failed to get lock {}", lockName);
            }
        } catch (InterruptedException e) {
            lockRpcBuilder = RpcResultBuilder.failed();
            LOG.info("Failed to get lock {}. Reason :", lockName, e);
        }
        return Futures.immediateFuture(lockRpcBuilder.build());
    }

    @Override
    public Future<RpcResult<Void>> unlock(UnlockInput input) {
        String lockName = input.getLockName();
        LOG.info("Unlocking {}", lockName);
        InstanceIdentifier<Lock> lockInstanceIdentifier = LockManagerUtils.getLockInstanceIdentifier(lockName);
        unlock(lockName, lockInstanceIdentifier);
        RpcResultBuilder<Void> lockRpcBuilder = RpcResultBuilder.success();
        return Futures.immediateFuture(lockRpcBuilder.build());
    }

    private void unlock(final String lockName, final InstanceIdentifier<Lock> lockInstanceIdentifier) {
        ReadWriteTransaction tx = broker.newReadWriteTransaction();
        try {
            Optional<Lock> result = tx.read(LogicalDatastoreType.OPERATIONAL, lockInstanceIdentifier).get();
            if (!result.isPresent()) {
                LOG.info("{} is already unlocked", lockName);
                return;
            }
            tx.delete(LogicalDatastoreType.OPERATIONAL, lockInstanceIdentifier);
            CheckedFuture<Void, TransactionCommitFailedException> futures = tx.submit();
            futures.get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("In unlock unable to unlock: {}. Reason :", lockName, e);
        }
    }

    public CompletableFuture<Void> getSynchronizerForLock(String lockName) {
        return lockSynchronizerMap.get(lockName);
    }

    /**
     * Try to acquire lock indefinitely until it is successful.
     */
    private void getLock(final InstanceIdentifier<Lock> lockInstanceIdentifier, final Lock lockData)
            throws InterruptedException {
        // Count from 1 to provide human-comprehensible messages
        String lockName = lockData.getLockName();
        for (int retry = 1;; retry++) {
            try {
                lockSynchronizerMap.putIfAbsent(lockName, new CompletableFuture<>());
                if (readWriteLock(lockInstanceIdentifier, lockData)) {
                    return;
                } else {
                    LOG.info("Already locked after waiting, try {}", retry);
                }
            } catch (ExecutionException e) {
                LOG.error("Unable to acquire lock for {}, try {}", lockName, retry);
            }
            java.util.Optional.ofNullable(lockSynchronizerMap.get(lockName)).ifPresent(future -> {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    LOG.error("Problems in waiting on lock synchronizer {}", lockName, e);
                }
            });
            lockSynchronizerMap.remove(lockName);
        }
    }

    /**
     * Try to acquire lock for mentioned retryCount. Returns true if
     * successfully acquired lock.
     */
    private boolean getLock(InstanceIdentifier<Lock> lockInstanceIdentifier, Lock lockData, long retryCount)
            throws InterruptedException {
        // Count from 1 to provide human-comprehensible messages
        String lockName = lockData.getLockName();
        for (int retry = 1; retry <= retryCount; retry++) {
            try {
                if (readWriteLock(lockInstanceIdentifier, lockData)) {
                    return true;
                } else {
                    LOG.debug("Already locked for {} after waiting {}ms, try {} of {}", lockName,
                            DEFAULT_WAIT_TIME_IN_MILLIS, retry, retryCount);
                }
            } catch (ExecutionException e) {
                LOG.error("Unable to acquire lock for {}, try {} of {}", lockName, retry,
                        retryCount);
            }
            Thread.sleep(DEFAULT_WAIT_TIME_IN_MILLIS);
        }
        return false;
    }

    /**
     * Read and write the lock immediately if available. Returns true if
     * successfully locked.
     */
    private boolean readWriteLock(final InstanceIdentifier<Lock> lockInstanceIdentifier, final Lock lockData)
            throws InterruptedException, ExecutionException {
        String lockName = lockData.getLockName();
        synchronized (lockName.intern()) {
            ReadWriteTransaction tx = broker.newReadWriteTransaction();
            Optional<Lock> result = tx.read(LogicalDatastoreType.OPERATIONAL, lockInstanceIdentifier).get();
            if (!result.isPresent()) {
                LOG.info("Writing lock lockData {}", lockData);
                tx.put(LogicalDatastoreType.OPERATIONAL, lockInstanceIdentifier, lockData, true);
                CheckedFuture<Void, TransactionCommitFailedException> futures = tx.submit();
                futures.get();
                return true;
            }
            return false;
        }
    }
}
