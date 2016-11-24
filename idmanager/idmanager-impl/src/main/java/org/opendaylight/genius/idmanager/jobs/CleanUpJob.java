/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.idmanager.jobs;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.genius.idmanager.IdLocalPool;
import org.opendaylight.genius.idmanager.IdManagerException;
import org.opendaylight.genius.idmanager.IdUtils;
import org.opendaylight.genius.idmanager.ReleasedIdHolder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.ReleasedIdsHolder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.ReleasedIdsHolderBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.LockManagerService;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CleanUpJob implements Callable<List<ListenableFuture<Void>>> {

    private static final Logger LOG = LoggerFactory.getLogger(CleanUpJob.class);

    private final IdLocalPool idLocalPool;
    private final DataBroker broker;
    private final String parentPoolName;
    private final int blockSize;
    private final LockManagerService lockManager;

    public CleanUpJob(IdLocalPool idLocalPool, DataBroker broker,
            String parentPoolName, int blockSize, LockManagerService lockManager) {
        super();
        this.idLocalPool = idLocalPool;
        this.broker = broker;
        this.parentPoolName = parentPoolName;
        this.blockSize = blockSize;
        this.lockManager = lockManager;
    }

    @Override
    public List<ListenableFuture<Void>> call() throws Exception {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        cleanupExcessIds(parentPoolName, blockSize, idLocalPool);
        return futures;
    }

    private void cleanupExcessIds(String parentPoolName, int blockSize, IdLocalPool idLocalPool)
            throws IdManagerException, ReadFailedException, TransactionCommitFailedException {
        // We can update the availableCount here... and update it in DS using IdHolderSyncJob
        long totalAvailableIdCount = idLocalPool.getAvailableIds().getAvailableIdCount()
                + idLocalPool.getReleasedIds().getAvailableIdCount();
        if (totalAvailableIdCount > blockSize * 2) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Condition for cleanUp Satisfied for localPool {} - totalAvailableIdCount {}",
                        idLocalPool, totalAvailableIdCount);
            }
            parentPoolName = parentPoolName.intern();
            InstanceIdentifier<ReleasedIdsHolder> releasedIdInstanceIdentifier = IdUtils
                    .getReleasedIdsHolderInstance(parentPoolName);
            // We need lock manager because maybe one cluster tries to read the
            // available ids from the global pool while the other is writing. We
            // cannot rely on DSJC because that is not cluster-aware
            try {
                IdUtils.lockPool(lockManager, parentPoolName);
                Optional<ReleasedIdsHolder> releasedIdsHolder = SingleTransactionDataBroker.syncReadOptional(broker,
                        LogicalDatastoreType.CONFIGURATION, releasedIdInstanceIdentifier);
                ReleasedIdsHolderBuilder releasedIdsParent;
                if (!releasedIdsHolder.isPresent()) {
                    LOG.error("ReleasedIds not present in parent pool. Unable to cleanup excess ids");
                    return;
                }
                releasedIdsParent = new ReleasedIdsHolderBuilder(releasedIdsHolder.get());
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Releasing excesss Ids from local pool");
                }
                ReleasedIdHolder releasedIds = (ReleasedIdHolder) idLocalPool.getReleasedIds();
                IdUtils.freeExcessAvailableIds(releasedIds, releasedIdsParent, totalAvailableIdCount - blockSize * 2);
                IdHolderSyncJob job = new IdHolderSyncJob(idLocalPool.getPoolName(), releasedIds, broker);
                DataStoreJobCoordinator.getInstance().enqueueJob(idLocalPool.getPoolName(), job, IdUtils.RETRY_COUNT);
                SingleTransactionDataBroker.syncWrite(broker, LogicalDatastoreType.CONFIGURATION,
                        releasedIdInstanceIdentifier, releasedIdsParent.build());
            } finally {
                IdUtils.unlockPool(lockManager, parentPoolName);
            }
        }
    }
}
