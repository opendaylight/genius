/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.idmanager.jobs;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.idmanager.IdLocalPool;
import org.opendaylight.genius.idmanager.IdUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPool;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPoolBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPoolKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;

public class LocalPoolCreateJob implements Callable<List<ListenableFuture<Void>>> {

    private static final Logger LOG = LoggerFactory.getLogger(LocalPoolCreateJob.class);

    private final IdLocalPool idLocalPool;
    private final DataBroker broker;
    private final String parentPoolName;
    private final int blockSize;
    private final IdUtils idUtils;

    public LocalPoolCreateJob(IdLocalPool idLocalPool, DataBroker broker,
            String parentPoolName, int blockSize, IdUtils idUtils) {
        this.idLocalPool = idLocalPool;
        this.broker = broker;
        this.parentPoolName = parentPoolName;
        this.blockSize = blockSize;
        this.idUtils = idUtils;
    }

    @Override
    public List<ListenableFuture<Void>> call() throws Exception {
        String localPoolName = idLocalPool.getPoolName();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Started localPoolCreateJob for {}", localPoolName);
        }
        ArrayList<ListenableFuture<Void>> futures = new ArrayList<>();
        InstanceIdentifier<IdPool> localPoolInstanceIdentifier = idUtils.getIdPoolInstance(localPoolName);
        IdPoolBuilder idPool = new IdPoolBuilder().setKey(new IdPoolKey(localPoolName)).setBlockSize(blockSize).setParentPoolName(parentPoolName).setPoolName(localPoolName);
        idLocalPool.getAvailableIds().refreshDataStore(idPool);
        idLocalPool.getReleasedIds().refreshDataStore(idPool);
        WriteTransaction tx = broker.newWriteOnlyTransaction();
        tx.put(LogicalDatastoreType.CONFIGURATION, localPoolInstanceIdentifier, idPool.build(), true);
        futures.add(tx.submit());
        return futures;
    }
}
