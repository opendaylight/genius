/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.idmanager.jobs;

import static org.opendaylight.mdsal.binding.util.Datastore.CONFIGURATION;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import org.opendaylight.genius.idmanager.IdLocalPool;
import org.opendaylight.genius.idmanager.IdManagerException;
import org.opendaylight.genius.idmanager.IdUtils;
import org.opendaylight.genius.idmanager.ReleasedIdHolder;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.infrautils.utils.concurrent.LoggingFutures;
import org.opendaylight.mdsal.binding.util.ManagedNewTransactionRunner;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.ReleasedIdsHolder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.ReleasedIdsHolderBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.LockManagerService;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CleanUpJob implements Callable<List<ListenableFuture<Void>>> {

    private static final Logger LOG = LoggerFactory.getLogger(CleanUpJob.class);

    private final IdLocalPool idLocalPool;
    private final ManagedNewTransactionRunner txRunner;
    private final String parentPoolName;
    private final int blockSize;
    private final LockManagerService lockManager;
    private final IdUtils idUtils;
    private final JobCoordinator jobCoordinator;

    public CleanUpJob(IdLocalPool idLocalPool, ManagedNewTransactionRunner txRunner,
            String parentPoolName, int blockSize,
            LockManagerService lockManager, IdUtils idUtils, JobCoordinator jobCoordinator) {
        this.idLocalPool = idLocalPool;
        this.txRunner = txRunner;
        this.parentPoolName = parentPoolName;
        this.blockSize = blockSize;
        this.lockManager = lockManager;
        this.idUtils = idUtils;
        this.jobCoordinator = jobCoordinator;
    }

    @Override
    public List<ListenableFuture<Void>> call() throws Exception {
        cleanupExcessIds();
        return Collections.emptyList();
    }

    private void cleanupExcessIds() throws IdManagerException {
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
            // cannot rely on DSJC because that is not cluster-aware
            try {
                idUtils.lock(lockManager, parentPoolNameIntern);
                LoggingFutures.addErrorLogging(txRunner.callWithNewReadWriteTransactionAndSubmit(CONFIGURATION, tx -> {
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
                    IdHolderSyncJob job =
                        new IdHolderSyncJob(idLocalPool.getPoolName(), releasedIds, txRunner, idUtils);
                    jobCoordinator.enqueueJob(idLocalPool.getPoolName(), job, IdUtils.RETRY_COUNT);
                    tx.mergeParentStructurePut(releasedIdInstanceIdentifier, releasedIdsParent.build());
                }), LOG, "Error cleaning up excess ids");
            } finally {
                idUtils.unlock(lockManager, parentPoolNameIntern);
            }
        }
    }
}
