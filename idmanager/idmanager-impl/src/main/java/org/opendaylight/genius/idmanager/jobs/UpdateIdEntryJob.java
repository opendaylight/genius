/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.idmanager.jobs;

import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.opendaylight.genius.idmanager.IdUtils;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.infrautils.utils.concurrent.ThreadFactoryProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.IdEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.LockManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateIdEntryJob implements Callable<List<ListenableFuture<Void>>> {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateIdEntryJob.class);
    private final String parentPoolName;
    private final String localPoolName;
    private final String idKey;
    private final List<Long> newIdValues = new ArrayList<>();
    private final ManagedNewTransactionRunner txRunner;
    private final IdUtils idUtils;
    private final LockManagerService lockManager;
    //TODO: change this to use shared executor service once available
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(15,
            ThreadFactoryProvider.builder().namePrefix("UpdateIdEntryJob").logger(LOG).build().get());

    public UpdateIdEntryJob(String parentPoolName, String localPoolName, String idKey, List<Long> newIdValues,
            ManagedNewTransactionRunner txRunner, IdUtils idUtils, LockManagerService lockManager) {
        this.parentPoolName = parentPoolName;
        this.localPoolName = localPoolName;
        this.idKey = idKey;
        this.txRunner = txRunner;
        this.idUtils = idUtils;
        this.lockManager = lockManager;
        if (newIdValues != null) {
            this.newIdValues.addAll(newIdValues);
        }
    }

    @Override
    public List<ListenableFuture<Void>> call() {
        ListenableFuture<Void> future = txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx -> {
            idUtils.updateChildPool(tx, parentPoolName, localPoolName);
            if (!newIdValues.isEmpty()) {
                IdEntries newIdEntry = idUtils.createIdEntries(idKey, newIdValues);
                tx.merge(CONFIGURATION, idUtils.getIdEntriesInstanceIdentifier(parentPoolName, idKey), newIdEntry);
            } else {
                tx.delete(CONFIGURATION, idUtils.getIdEntriesInstanceIdentifier(parentPoolName, idKey));
            }
        });
        future.addListener(() -> cleanUp(), EXECUTOR_SERVICE);
        return Collections.singletonList(future);
    }

    private void cleanUp() {
        String uniqueIdKey = idUtils.getUniqueKey(parentPoolName, idKey);
        CountDownLatch latch = idUtils.getReleaseIdLatch(uniqueIdKey);
        if (latch != null) {
            latch.countDown();
        }
        // Once the id is written to DS, removing the id value from map.
        idUtils.removeAllocatedIds(uniqueIdKey);
        idUtils.unlock(lockManager, uniqueIdKey);
    }
}
