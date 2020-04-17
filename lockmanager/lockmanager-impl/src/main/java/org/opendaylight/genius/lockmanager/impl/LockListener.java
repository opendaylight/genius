/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.lockmanager.impl;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.aries.blueprint.annotation.service.Reference;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.infrautils.utils.concurrent.Executors;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.serviceutils.tools.listener.AbstractClusteredAsyncDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.Locks;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.locks.Lock;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class LockListener extends AbstractClusteredAsyncDataTreeChangeListener<Lock> {

    private static final Logger LOG = LoggerFactory.getLogger(LockListener.class);

    private final LockManagerServiceImpl lockManager;

    @Inject
    public LockListener(@Reference DataBroker dataBroker, LockManagerServiceImpl lockManager) {
        super(dataBroker, LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(Locks.class).child(Lock.class),
              Executors.newSingleThreadExecutor("LockListener", LOG));
        this.lockManager = lockManager;
    }

    @Override
    public void remove(@NonNull InstanceIdentifier<Lock> instanceIdentifier, @NonNull Lock removedLock) {
        lockManager.removeLock(removedLock);
    }

    @Override
    public void update(@NonNull InstanceIdentifier<Lock> instanceIdentifier, @NonNull Lock originalLock,
                       @NonNull Lock updatedLock) {
        // NOOP
    }

    @Override
    public void add(@NonNull InstanceIdentifier<Lock> instanceIdentifier, @NonNull Lock lock) {
        LOG.debug("Received add for lock {} : {}", lock.getLockName(), lock);
    }

    @Override
    @PreDestroy
    public void close() {
        super.close();
        Executors.shutdownAndAwaitTermination(getExecutorService());
    }
}
