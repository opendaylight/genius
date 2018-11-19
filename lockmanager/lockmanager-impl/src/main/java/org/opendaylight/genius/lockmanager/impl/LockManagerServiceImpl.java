/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.lockmanager.impl;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.aries.blueprint.annotation.service.Reference;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.OptimisticLockFailedException;
import org.opendaylight.genius.infra.Datastore;
import org.opendaylight.genius.infra.RetryingManagedNewTransactionRunner;
import org.opendaylight.genius.utils.JvmGlobalLocks;
import org.opendaylight.serviceutils.tools.mdsal.rpc.FutureRpcResults;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.LockInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.LockManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.LockOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.TryLockInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.TryLockOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.UnlockInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.UnlockOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.UnlockOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.locks.Lock;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class LockManagerServiceImpl implements LockManagerService {

    private static final int DEFAULT_NUMBER_LOCKING_ATTEMPS = 30;
    private static final int DEFAULT_RETRY_COUNT = 3;
    private static final int DEFAULT_WAIT_TIME_IN_MILLIS = 1000;

    private final ConcurrentHashMap<String, CompletableFuture<Void>> lockSynchronizerMap =
            new ConcurrentHashMap<>();

    private static final Logger LOG = LoggerFactory.getLogger(LockManagerServiceImpl.class);

    private final RetryingManagedNewTransactionRunner txRunner;
    private final LockManagerUtils lockManagerUtils;

    @Inject
    public LockManagerServiceImpl(final @Reference DataBroker dataBroker, final LockManagerUtils lockManagerUtils) {
        this.lockManagerUtils = lockManagerUtils;
        this.txRunner = new RetryingManagedNewTransactionRunner(dataBroker);
    }

    @Override
    public ListenableFuture<RpcResult<LockOutput>> lock(LockInput input) {
        final Lock lockData = lockManagerUtils.buildLock(input.getLockName(), lockManagerUtils.getUniqueID());
        return FutureRpcResults.fromListenableFuture(LOG, input, () -> {
            return getLock(lockData);
        }).build();
    }

    @Override
    public ListenableFuture<RpcResult<TryLockOutput>> tryLock(TryLockInput input) {
        String lockName = input.getLockName();
        String owner = lockManagerUtils.getUniqueID();
        LOG.debug("Locking {}, owner {}" , lockName, owner);
        long waitTime = input.getTime() == null ? DEFAULT_WAIT_TIME_IN_MILLIS * DEFAULT_RETRY_COUNT : input.getTime();
        TimeUnit timeUnit = input.getTimeUnit() == null ? TimeUnit.MILLISECONDS
                : lockManagerUtils.convertToTimeUnit(input.getTimeUnit());
        waitTime = timeUnit.toMillis(waitTime);
        long retryCount = waitTime / DEFAULT_WAIT_TIME_IN_MILLIS;
        Lock lockData = lockManagerUtils.buildLock(lockName, owner);

        RpcResultBuilder<TryLockOutput> lockRpcBuilder;
        try {
            if (getLock(lockData, retryCount)) {
                lockRpcBuilder = RpcResultBuilder.success();
                LOG.debug("Acquired lock {} by owner {}", lockName, owner);
            } else {
                lockRpcBuilder = RpcResultBuilder.failed();
                LOG.error("Failed to get lock {} owner {} after {} retries", lockName, owner, retryCount);
            }
        } catch (InterruptedException e) {
            lockRpcBuilder = RpcResultBuilder.failed();
            LOG.error("Failed to get lock {} owner {}", lockName, owner, e);
        }
        return Futures.immediateFuture(lockRpcBuilder.build());
    }

    @Override
    public ListenableFuture<RpcResult<UnlockOutput>> unlock(UnlockInput input) {
        String lockName = input.getLockName();
        LOG.debug("Unlocking {}", lockName);
        return FutureRpcResults.fromListenableFuture(LOG, input,
            () -> Futures.transform(txRunner.callWithNewReadWriteTransactionAndSubmit(tx -> {
                InstanceIdentifier<Lock> lockInstanceIdentifier = lockManagerUtils.getLockInstanceIdentifier(lockName);
                Optional<Lock> result = tx.read(LogicalDatastoreType.OPERATIONAL, lockInstanceIdentifier).get();
                if (!result.isPresent()) {
                    LOG.debug("unlock ignored, as unnecessary; lock is already unlocked: {}", lockName);
                } else {
                    tx.delete(LogicalDatastoreType.OPERATIONAL, lockInstanceIdentifier);
                }
            }), unused -> new UnlockOutputBuilder().build(), MoreExecutors.directExecutor())).build();
    }

    void removeLock(final Lock removedLock) {
        final String lockName = removedLock.getLockName();
        LOG.debug("Received remove for lock {} : {}", lockName, removedLock);
        CompletableFuture<Void> lock = lockSynchronizerMap.get(lockName);
        if (lock != null) {
            // FindBugs flags a false violation here - "passes a null value as the parameter of a method which must be
            // non-null. Either this parameter has been explicitly marked as @Nonnull, or analysis has determined that
            // this parameter is always dereferenced.". However neither is true. The type param is Void so you have to
            // pas null.
            lock.complete(null);
        }
    }

    /**
     * Try to acquire lock indefinitely until it is successful.
     */
    private ListenableFuture<LockOutput> getLock(final Lock lockData)
            throws InterruptedException {
        // Count from 1 to provide human-comprehensible messages
        String lockName = lockData.getLockName();
        for (int retry = 1;; retry++) {
            try {
                lockSynchronizerMap.putIfAbsent(lockName, new CompletableFuture<>());
                if (readWriteLock(lockData)) {
                    return Futures.immediateFuture(null);
                } else {
                    if (retry < DEFAULT_NUMBER_LOCKING_ATTEMPS) {
                        LOG.debug("Already locked for {} after waiting {}ms, try {}",
                                lockName, DEFAULT_WAIT_TIME_IN_MILLIS, retry);
                    } else {
                        LOG.warn("Already locked for {} after waiting {}ms, try {}",
                                lockName, DEFAULT_WAIT_TIME_IN_MILLIS, retry);
                    }
                }
            } catch (ExecutionException e) {
                logUnlessCauseIsOptimisticLockFailedException(lockName, retry, e);
                if (!(e.getCause() instanceof OptimisticLockFailedException)) {
                    return Futures.immediateFailedFuture(e.getCause());
                }
            }
            CompletableFuture<Void> future = lockSynchronizerMap.get(lockName);
            if (future != null) {
                try {
                    // Making this as timed get to avoid any missing signal for lock remove notifications
                    // in LockListener (which does the future.complete())
                    future.get(DEFAULT_WAIT_TIME_IN_MILLIS, TimeUnit.MILLISECONDS);
                } catch (InterruptedException | ExecutionException e) {
                    LOG.error("Problems in waiting on lock synchronizer {}", lockName, e);
                } catch (TimeoutException e) {
                    LOG.info("Waiting for the lock {} is timed out. retrying again", lockName);
                }
                lockSynchronizerMap.remove(lockName);
            }
        }
    }

    /**
     * Try to acquire lock for mentioned retryCount. Returns true if
     * successfully acquired lock.
     */
    private boolean getLock(Lock lockData, long retryCount) throws InterruptedException {
        // Count from 1 to provide human-comprehensible messages
        String lockName = lockData.getLockName();
        for (int retry = 1; retry <= retryCount; retry++) {
            try {
                if (readWriteLock(lockData)) {
                    return true;
                } else {
                    LOG.debug("Already locked for {} after waiting {}ms, try {} of {}", lockName,
                            DEFAULT_WAIT_TIME_IN_MILLIS, retry, retryCount);
                }
            } catch (ExecutionException e) {
                logUnlessCauseIsOptimisticLockFailedException(lockName, retry, e);
            }
            Thread.sleep(DEFAULT_WAIT_TIME_IN_MILLIS);
        }
        return false;
    }

    private static void logUnlessCauseIsOptimisticLockFailedException(String name, int retry,
            ExecutionException exception) {
        // Log anything else than OptimisticLockFailedException with level error.
        // Bug 8059: We do not log OptimisticLockFailedException, as those are "normal" in the current design,
        //           and this class is explicitly designed to retry obtaining a lock in case of an
        //           OptimisticLockFailedException, so we do not flood the log with events in case it's "just" that.
        // TODO This class may be completely reviewed in the future to work entirely differently;
        //      e.g. using an EntityOwnershipService, as proposed in Bug 8224.
        if (!(exception.getCause() instanceof OptimisticLockFailedException)) {
            LOG.error("Unable to acquire lock for {}, try {}", name, retry, exception);
        }
    }

    /**
     * Read and write the lock immediately if available. Returns true if
     * successfully locked.
     */
    private boolean readWriteLock(final Lock lockData)
            throws InterruptedException, ExecutionException {
        // FIXME: Since netvirt is currently also locking on strings, we need to ensure those places do not synchronize
        //        with us before switching to .getLockFor()
        final ReentrantLock lock = JvmGlobalLocks.getLockForString(lockData.getLockName());
        lock.lock();
        try {
            final InstanceIdentifier<Lock> lockInstanceIdentifier =
                    LockManagerUtils.getLockInstanceIdentifier(lockData.key());
            return txRunner.applyWithNewReadWriteTransactionAndSubmit(Datastore.OPERATIONAL, tx -> {
                Optional<Lock> result = tx.read(lockInstanceIdentifier).get();
                if (!result.isPresent()) {
                    LOG.debug("Writing lock lockData {}", lockData);
                    tx.put(lockInstanceIdentifier, lockData, true);
                    return true;
                }

                String lockDataOwner = result.get().getLockOwner();
                String currentOwner = lockData.getLockOwner();
                return Objects.equals(currentOwner, lockDataOwner);
            }).get();
        } finally {
            lock.unlock();
        }
    }
}
