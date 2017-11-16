/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.idmanager;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncClusteredDataTreeChangeListenerBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdPools;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPool;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class IdPoolListener extends AsyncClusteredDataTreeChangeListenerBase<IdPool, IdPoolListener> {

    private static final Logger LOG = LoggerFactory.getLogger(IdPoolListener.class);

    private final DataBroker broker;
    private final IdManager idManager;
    private final IdUtils idUtils;

    @Inject
    public IdPoolListener(DataBroker broker, IdManager idManager, IdUtils idUtils) {
        super(IdPool.class, IdPoolListener.class);
        this.broker = broker;
        this.idManager = idManager;
        this.idUtils = idUtils;
    }

    @PostConstruct
    public void start() throws Exception {
        registerListener(LogicalDatastoreType.CONFIGURATION, this.broker);
        LOG.info("IdPoolListener listener Started");
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
        if (!update.getAvailableIdsHolder().equals(original.getAvailableIdsHolder())
                || !update.getReleasedIdsHolder().equals(original.getReleasedIdsHolder())) {
            String parentPoolName = update.getParentPoolName();
            String poolName = update.getPoolName();
            if (parentPoolName != null && !parentPoolName.isEmpty()) {
                if (!idUtils.getPoolUpdatedMap(poolName)
                        && poolName.equals(idUtils.getLocalPoolName(parentPoolName))) {
                    LOG.info("Received update for pool {} : {} - {}", update.getPoolName(), original, update);
                    idManager.updateLocalIdPoolCache(update, parentPoolName);
                } else {
                    idUtils.decrementPoolUpdatedMap(poolName);
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
