/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.idmanager;

import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.datastoreutils.AsyncClusteredDataTreeChangeListenerBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdPools;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPool;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class IdPoolListener extends AsyncClusteredDataTreeChangeListenerBase<IdPool, IdPoolListener>
        implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(IdPoolListener.class);

    private final DataBroker broker;
    private final IdManager idManager;

    @Inject
    public IdPoolListener(DataBroker broker, IdManager idManager) {
        super(IdPool.class, IdPoolListener.class);
        this.broker = broker;
        this.idManager = idManager;
    }

    @Override
    protected void remove(InstanceIdentifier<IdPool> identifier, IdPool del) {
        String parentPoolName = del.getParentPoolName();
        String poolName = del.getPoolName();
        if (parentPoolName != null && !parentPoolName.isEmpty()) {
            idManager.poolDeleted(parentPoolName, poolName);
        }
    }

    @Override
    protected void update(InstanceIdentifier<IdPool> identifier,
            IdPool original, IdPool update) {
        if (update.getAvailableIdsHolder() != original.getAvailableIdsHolder()
                || update.getReleasedIdsHolder() != original.getReleasedIdsHolder()) {
            String parentPoolName = update.getParentPoolName();
            String poolName = update.getPoolName();
            if (parentPoolName != null && !parentPoolName.isEmpty()) {
                if (!IdUtils.getPoolUpdatedMap(poolName)) {
                    LOG.info("Received update for NAME {} : {} - {}", update.getPoolName(), original, update);
                    idManager.updateLocalIdPoolCache(update, parentPoolName);
                } else {
                    IdUtils.decrementPoolUpdatedMap(poolName);
                }
            }
        }
    }

    @Override
    protected void add(InstanceIdentifier<IdPool> identifier, IdPool add) {
        LOG.info("Received add for name {} : {}", add.getPoolName(), add);
    }

    @Override
    protected InstanceIdentifier<IdPool> getWildCardPath() {
        return InstanceIdentifier.create(IdPools.class).child(IdPool.class);
    }

    @Override
    protected IdPoolListener getDataTreeChangeListener() {
        return IdPoolListener.this;
    }

}
