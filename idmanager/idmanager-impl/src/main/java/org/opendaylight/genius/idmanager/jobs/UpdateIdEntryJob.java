/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.idmanager.jobs;

import static org.opendaylight.mdsal.binding.util.Datastore.CONFIGURATION;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.genius.idmanager.IdUtils;
import org.opendaylight.infrautils.utils.concurrent.Executors;
import org.opendaylight.mdsal.binding.util.ManagedNewTransactionRunner;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.IdEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.LockManagerService;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateIdEntryJob implements Callable<List<? extends ListenableFuture<?>>> {
    private static final Logger LOG = LoggerFactory.getLogger(UpdateIdEntryJob.class);

    // static to have total threads globally, not per UpdateIdEntryJob (of which there are many)
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(5, "UpdateIdEntryJob", LOG);

    private final String parentPoolName;
    private final String localPoolName;
    private final String idKey;
    private final List<Uint32> newIdValues = new ArrayList<>();
    private final ManagedNewTransactionRunner txRunner;
    private final IdUtils idUtils;
    private final LockManagerService lockManager;

    public UpdateIdEntryJob(String parentPoolName, String localPoolName, String idKey,
            List<Uint32> newIdValues, ManagedNewTransactionRunner txRunner, IdUtils idUtils,
            LockManagerService lockManager) {
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
    public List<? extends ListenableFuture<?>> call() {
        FluentFuture<?> future = txRunner.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION, tx -> {
            idUtils.updateChildPool(tx, parentPoolName, localPoolName);
            if (!newIdValues.isEmpty()) {
                IdEntries newIdEntry = idUtils.createIdEntries(idKey, newIdValues);
                tx.merge(idUtils.getIdEntriesInstanceIdentifier(parentPoolName, idKey), newIdEntry);
            } else {
                tx.delete(idUtils.getIdEntriesInstanceIdentifier(parentPoolName, idKey));
            }
        });
        future.addCallback(new FutureCallback<Object>() {
            @Override
            public void onSuccess(@Nullable Object result) {
                cleanUp();
            }

            @Override
            public void onFailure(Throwable throwable) {
                cleanUp();
            }
        }, EXECUTOR_SERVICE);
        return Collections.singletonList(future);
    }

    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "https://github.com/spotbugs/spotbugs/issues/811")
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
