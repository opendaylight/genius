/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.idmanager;

import java.util.Objects;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.aries.blueprint.annotation.service.Reference;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.infrautils.utils.concurrent.Executors;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.serviceutils.tools.listener.AbstractClusteredAsyncDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdPools;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPool;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class IdPoolListener extends AbstractClusteredAsyncDataTreeChangeListener<IdPool> {

    private static final Logger LOG = LoggerFactory.getLogger(IdPoolListener.class);

    private final IdManager idManager;
    private final IdUtils idUtils;

    @Inject
    public IdPoolListener(@Reference DataBroker dataBroker, IdManager idManager, IdUtils idUtils) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION,
              InstanceIdentifier.create(IdPools.class).child(IdPool.class), Executors
                      .newSingleThreadExecutor("IdPoolListener", LOG));
        this.idManager = idManager;
        this.idUtils = idUtils;
    }

    @Override
    public void add(@NonNull InstanceIdentifier<IdPool> instanceIdentifier, @NonNull IdPool idPool) {
        LOG.info("Add pool name: {}", idPool.getPoolName());
    }

    @Override
    public void remove(@NonNull InstanceIdentifier<IdPool> instanceIdentifier, @NonNull IdPool idPool) {
        String parentPoolName = idPool.getParentPoolName();
        String poolName = idPool.getPoolName();
        if (parentPoolName != null && !parentPoolName.isEmpty()) {
            idManager.poolDeleted(parentPoolName, poolName);
        }
    }

    @Override
    public void update(@NonNull InstanceIdentifier<IdPool> instanceIdentifier, @NonNull IdPool originalIdPool,
                       @NonNull IdPool updatedIdPool) {
        if (!Objects.equals(updatedIdPool.getAvailableIdsHolder(), originalIdPool.getAvailableIdsHolder())
                || !Objects.equals(updatedIdPool.getReleasedIdsHolder().getAvailableIdCount(),
                       originalIdPool.getReleasedIdsHolder().getAvailableIdCount())) {
            String parentPoolName = updatedIdPool.getParentPoolName();
            String poolName = updatedIdPool.getPoolName();
            if (poolName != null && parentPoolName != null && !parentPoolName.isEmpty()) {
                if (!idUtils.getPoolUpdatedMap(poolName) && poolName.equals(idUtils.getLocalPoolName(parentPoolName))) {
                    LOG.info("Received update for pool {} : {} - {}", updatedIdPool.getPoolName(), originalIdPool,
                             updatedIdPool);
                    idManager.updateLocalIdPoolCache(updatedIdPool, parentPoolName);
                } else {
                    idUtils.decrementPoolUpdatedMap(poolName);
                }
            }
        }
    }

    @Override
    @PreDestroy
    public void close() {
        super.close();
        Executors.shutdownAndAwaitTermination(getExecutorService());
    }
}
