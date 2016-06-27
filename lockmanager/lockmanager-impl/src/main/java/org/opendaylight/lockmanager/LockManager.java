/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lockmanager;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class LockManager implements LockManagerService {
    private static final Logger LOG = LoggerFactory.getLogger(LockManager.class);

    private static final int DEFAULT_RETRY_COUNT = 3;
    private static final int DEFAULT_WAIT_TIME_IN_MILLIS = 1000;

    private final DataBroker broker;

    public LockManager(final DataBroker db) {
        broker = db;
    }

    @Override
    public Future<RpcResult<Void>> lock(LockInput input) {
        String lockName = input.getLockName();
        LOG.info("Locking {}" , lockName);
        InstanceIdentifier<Lock> lockInstanceIdentifier = LockManagerUtils.getLockInstanceIdentifier(lockName);
        Lock lockData = LockManagerUtils.buildLockData(lockName);
        try {
            getLock(lockInstanceIdentifier, lockData);
            RpcResultBuilder<Void> lockRpcBuilder = RpcResultBuilder.success();
            LOG.info("Acquired lock {}" , lockName);
            return Futures.immediateFuture(lockRpcBuilder.build());
        } catch (InterruptedException e) {
            RpcResultBuilder<Void> lockRpcBuilder = RpcResultBuilder.failed();
            LOG.info("Failed to get lock {}" , lockName);
            return Futures.immediateFuture(lockRpcBuilder.build());
        }
    }

    @Override
    public Future<RpcResult<Void>> tryLock(TryLockInput input) {
        String lockName = input.getLockName();
        LOG.info("Locking {}" , lockName);
        long waitTime = input.getTime() == null ? DEFAULT_WAIT_TIME_IN_MILLIS * DEFAULT_RETRY_COUNT : input.getTime();
        TimeUnit timeUnit = (TimeUnit) (input.getTimeUnit() == null ? TimeUnit.MILLISECONDS: LockManagerUtils.convertToTimeUnit(input.getTimeUnit()));
        waitTime = LockManagerUtils.convertToMillis(waitTime, timeUnit);
        long retryCount = waitTime / DEFAULT_WAIT_TIME_IN_MILLIS;
        InstanceIdentifier<Lock> lockInstanceIdentifier = LockManagerUtils.getLockInstanceIdentifier(lockName);
        Lock lockData = LockManagerUtils.buildLockData(lockName);
        try {
            if (getLock(lockInstanceIdentifier, lockData, retryCount)) {
                RpcResultBuilder<Void> lockRpcBuilder = RpcResultBuilder.success();
                LOG.info("Acquired lock {}" , lockName);
                return Futures.immediateFuture(lockRpcBuilder.build());
            }
            RpcResultBuilder<Void> lockRpcBuilder = RpcResultBuilder.failed();
            LOG.info("Failed to get lock {}" , lockName);
            return Futures.immediateFuture(lockRpcBuilder.build());
        } catch (Exception e) {
            RpcResultBuilder<Void> lockRpcBuilder = RpcResultBuilder.failed();
            LOG.info("Failed to get lock {}" , lockName);
            return Futures.immediateFuture(lockRpcBuilder.build());
        }
    }

    @Override
    public Future<RpcResult<Void>> unlock(UnlockInput input) {
        String lockName = input.getLockName();
        LOG.info("Unlocking {}" , lockName);
        InstanceIdentifier<Lock> lockInstanceIdentifier = LockManagerUtils.getLockInstanceIdentifier(lockName);
        unlock(lockName, lockInstanceIdentifier);
        RpcResultBuilder<Void> lockRpcBuilder = RpcResultBuilder.success();
        return Futures.immediateFuture(lockRpcBuilder.build());
    }

    /**
     * Try to acquire lock indefinitely until it is successful.
     * @param lockInstanceIdentifier
     * @param lockData
     */
    private void getLock(final InstanceIdentifier<Lock> lockInstanceIdentifier, final Lock lockData) throws InterruptedException {
        // Count from 1 to provide human-comprehensible messages
        for (int retry = 1; ; retry++) {
            try {
                if (readWriteLock(lockInstanceIdentifier, lockData)) {
                    return;
                } else {
                    LOG.debug("Already locked after waiting {}ms, try {}", DEFAULT_WAIT_TIME_IN_MILLIS, retry);
                }
            } catch (ExecutionException e) {
                LOG.error("Unable to acquire lock, try {}", retry, e);
            }
            Thread.sleep(DEFAULT_WAIT_TIME_IN_MILLIS);
        }
    }

    /**
     * Try to acquire lock for mentioned retryCount. Returns true if successfully acquired lock.
     * @param lockInstanceIdentifier
     * @param lockData
     * @param retryCount
     * @return
     * @throws InterruptedException
     */
    private boolean getLock(InstanceIdentifier<Lock> lockInstanceIdentifier,
            Lock lockData, long retryCount) throws InterruptedException {
        // Count from 1 to provide human-comprehensible messages
        for (int retry = 1; retry <= retryCount; retry++) {
            try {
                if (readWriteLock(lockInstanceIdentifier, lockData)) {
                    return true;
                } else {
                    LOG.debug("Already locked after waiting {}ms, try {} of {}", DEFAULT_WAIT_TIME_IN_MILLIS, retry,
                            retryCount);
                }
            } catch (ExecutionException e) {
                LOG.error("Unable to acquire lock, try {} of {}", retry, retryCount, e);
            }
            Thread.sleep(DEFAULT_WAIT_TIME_IN_MILLIS);
        }
        return false;
    }

    /**
     * Read and write the lock immediately if available. Returns true if successfully locked.
     * @param lockInstanceIdentifier
     * @param lockData
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private boolean readWriteLock (final InstanceIdentifier<Lock> lockInstanceIdentifier, final Lock lockData) throws InterruptedException, ExecutionException {
        ReadWriteTransaction tx = broker.newReadWriteTransaction();
        Optional<Lock> result = Optional.absent();
        result = tx.read(LogicalDatastoreType.OPERATIONAL, lockInstanceIdentifier).get();
        if (!result.isPresent()) {
            tx.put(LogicalDatastoreType.OPERATIONAL, lockInstanceIdentifier, lockData, true);
            CheckedFuture<Void, TransactionCommitFailedException> futures = tx.submit();
            futures.get();
            return true;
        }
        if (result.get().getLockOwner() == Thread.currentThread().getName()) {
            return true;
        }
        return false;
    }

    private void unlock(final String lockName, final InstanceIdentifier<Lock> lockInstanceIdentifier) {
        ReadWriteTransaction tx = broker.newReadWriteTransaction();
        Optional<Lock> result = Optional.absent();
        try {
            result = tx.read(LogicalDatastoreType.OPERATIONAL, lockInstanceIdentifier).get();
            if (!result.isPresent()) {
                LOG.info("{} is already unlocked", lockName);
                return;
            }
            tx.delete(LogicalDatastoreType.OPERATIONAL, lockInstanceIdentifier);
            CheckedFuture<Void, TransactionCommitFailedException> futures = tx.submit();
            futures.get();
        } catch (Exception e) {
            LOG.error("In unlock unable to unlock due to {}", e.getMessage());
        }
    }


}
