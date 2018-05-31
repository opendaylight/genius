/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.idmanager.jobs;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.idmanager.IdLocalPool;
import org.opendaylight.genius.idmanager.IdUtils;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPool;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPoolBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPoolKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalPoolCreateJob implements Callable<List<ListenableFuture<Void>>> {

    private static final Logger LOG = LoggerFactory.getLogger(LocalPoolCreateJob.class);

    private final IdLocalPool idLocalPool;
    private final ManagedNewTransactionRunner txRunner;
    private final String parentPoolName;
    private final int blockSize;
    private final IdUtils idUtils;

    public LocalPoolCreateJob(IdLocalPool idLocalPool, ManagedNewTransactionRunner txRunner,
            String parentPoolName, int blockSize, IdUtils idUtils) {
        this.idLocalPool = idLocalPool;
        this.txRunner = txRunner;
        this.parentPoolName = parentPoolName;
        this.blockSize = blockSize;
        this.idUtils = idUtils;
    }

    @Override
    public List<ListenableFuture<Void>> call() {
        String localPoolName = idLocalPool.getPoolName();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Started localPoolCreateJob for {}", localPoolName);
        }
        InstanceIdentifier<IdPool> localPoolInstanceIdentifier = idUtils.getIdPoolInstance(localPoolName);
        IdPoolBuilder idPool = new IdPoolBuilder().withKey(new IdPoolKey(localPoolName)).setBlockSize(blockSize)
                .setParentPoolName(parentPoolName).setPoolName(localPoolName);
        idLocalPool.getAvailableIds().refreshDataStore(idPool);
        idLocalPool.getReleasedIds().refreshDataStore(idPool);
        return Collections.singletonList(txRunner.callWithNewWriteOnlyTransactionAndSubmit(
            tx -> tx.put(LogicalDatastoreType.CONFIGURATION, localPoolInstanceIdentifier, idPool.build(),
                    WriteTransaction.CREATE_MISSING_PARENTS)));
    }
}
