/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.idmanager.jobs;

import static org.opendaylight.genius.infra.Datastore.CONFIGURATION;
import static org.opendaylight.mdsal.binding.api.WriteTransaction.CREATE_MISSING_PARENTS;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.opendaylight.genius.idmanager.IdHolder;
import org.opendaylight.genius.idmanager.IdUtils;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPool;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPoolBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPoolKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdHolderSyncJob implements Callable<List<ListenableFuture<Void>>> {

    private static final Logger LOG = LoggerFactory.getLogger(IdHolderSyncJob.class);

    private final String localPoolName;
    private final IdHolder idHolder;
    private final ManagedNewTransactionRunner txRunner;
    private final IdUtils idUtils;

    public IdHolderSyncJob(String localPoolName, IdHolder idHolder,
            ManagedNewTransactionRunner txRunner, IdUtils idUtils) {
        this.localPoolName = localPoolName;
        this.idHolder = idHolder;
        this.txRunner = txRunner;
        this.idUtils = idUtils;
    }

    @Override
    public List<ListenableFuture<Void>> call() {
        IdPoolBuilder idPool = new IdPoolBuilder().withKey(new IdPoolKey(localPoolName));
        idHolder.refreshDataStore(idPool);
        InstanceIdentifier<IdPool> localPoolInstanceIdentifier = idUtils.getIdPoolInstance(localPoolName);
        return Collections.singletonList(txRunner.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION, tx -> {
            tx.merge(localPoolInstanceIdentifier, idPool.build(), CREATE_MISSING_PARENTS);
            idUtils.incrementPoolUpdatedMap(localPoolName);

            if (LOG.isDebugEnabled()) {
                LOG.debug("IdHolder synced {}", idHolder);
            }
        }));
    }
}
