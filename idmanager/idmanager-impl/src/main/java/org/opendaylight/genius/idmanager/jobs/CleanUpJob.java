/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.idmanager.jobs;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.idmanager.IdLocalPool;
import org.opendaylight.genius.idmanager.IdManagerException;
import org.opendaylight.genius.idmanager.IdUtils;
import org.opendaylight.genius.idmanager.ReleasedIdHolder;
import org.opendaylight.genius.infra.Datastore;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.RetryingManagedNewTransactionRunner;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.ReleasedIdsHolder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.ReleasedIdsHolderBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.LockManagerService;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CleanUpJob implements Callable<List<ListenableFuture<Void>>> {

    private static final Logger LOG = LoggerFactory.getLogger(CleanUpJob.class);

    private final IdLocalPool idLocalPool;
    private final ManagedNewTransactionRunner txRunner;
    private final RetryingManagedNewTransactionRunner retryingTxRunner;
    private final String parentPoolName;
    private final int blockSize;
    private final LockManagerService lockManager;
    private final IdUtils idUtils;
    private final JobCoordinator jobCoordinator;

    public CleanUpJob(IdLocalPool idLocalPool, ManagedNewTransactionRunner txRunner,
            RetryingManagedNewTransactionRunner retryingTxRunner, String parentPoolName, int blockSize,
            LockManagerService lockManager, IdUtils idUtils, JobCoordinator jobCoordinator) {
        this.idLocalPool = idLocalPool;
        this.txRunner = txRunner;
        this.retryingTxRunner = retryingTxRunner;
        this.parentPoolName = parentPoolName;
        this.blockSize = blockSize;
        this.lockManager = lockManager;
        this.idUtils = idUtils;
        this.jobCoordinator = jobCoordinator;
    }

    @Override
    public List<ListenableFuture<Void>> call() throws Exception {
        return Collections.singletonList(cleanupExcessIds());
    }

    private FluentFuture<Void> cleanupExcessIds()
            throws IdManagerException, ReadFailedException, TransactionCommitFailedException {
        // We can update the availableCount here... and update it in DS using IdHolderSyncJob
        long totalAvailableIdCount = idLocalPool.getAvailableIds().getAvailableIdCount()
                + idLocalPool.getReleasedIds().getAvailableIdCount();
        if (totalAvailableIdCount > blockSize * 2) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Condition for cleanUp Satisfied for localPool {} - totalAvailableIdCount {}",
                        idLocalPool, totalAvailableIdCount);
            }
            String parentPoolNameIntern = parentPoolName.intern();
            InstanceIdentifier<ReleasedIdsHolder> releasedIdInstanceIdentifier
                    = idUtils.getReleasedIdsHolderInstance(parentPoolNameIntern);
            // We need lock manager because maybe one cluster tries to read the
            // available ids from the global pool while the other is writing. We
            // cannot rely on DSJC because that is not cluster-aware.
            //
            // TODO https://git.opendaylight.org/gerrit/#/c/78055/ : The managed
            // transaction here does not (cannot, yet) cover the LockManager! :(
            try {
                idUtils.lock(lockManager, parentPoolNameIntern);
                return retryingTxRunner.callWithNewReadWriteTransactionAndSubmit(Datastore.CONFIGURATION, tx -> {
                    Optional<ReleasedIdsHolder> releasedIdsHolder = tx.read(releasedIdInstanceIdentifier).get();
                    if (!releasedIdsHolder.isPresent()) {
                        LOG.error("ReleasedIds not present in parent pool. Unable to cleanup excess ids");
                        return;
                    }
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Releasing excesss Ids from local pool");
                    }
                    ReleasedIdHolder releasedIds = (ReleasedIdHolder) idLocalPool.getReleasedIds();
                    ReleasedIdsHolderBuilder releasedIdsParent = new ReleasedIdsHolderBuilder(releasedIdsHolder.get());
                    idUtils.freeExcessAvailableIds(releasedIds, releasedIdsParent,
                        totalAvailableIdCount - blockSize * 2);
                    IdHolderSyncJob job = new IdHolderSyncJob(idLocalPool.getPoolName(), releasedIds, txRunner,
                            idUtils);
                    jobCoordinator.enqueueJob(idLocalPool.getPoolName(), job, IdUtils.RETRY_COUNT);
                    tx.put(releasedIdInstanceIdentifier, releasedIdsParent.build(), true);
                });
            } finally {
                idUtils.unlock(lockManager, parentPoolNameIntern);
            }
        } else {
            return FluentFutures.immediateNullFluentFuture();
        }
    }
}
