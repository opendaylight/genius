/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.lockmanager;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncClusteredDataTreeChangeListenerBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.Locks;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.locks.Lock;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class LockListener extends AsyncClusteredDataTreeChangeListenerBase<Lock, LockListener>
        implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(LockListener.class);
    private final DataBroker broker;
    private final LockManager lockManager;

    @Inject
    public LockListener(DataBroker broker, LockManager lockManager) {
        super(Lock.class, LockListener.class);
        this.broker = broker;
        this.lockManager = lockManager;
    }

    @PostConstruct
    public void start() throws Exception {
        registerListener(LogicalDatastoreType.OPERATIONAL, broker);
        LOG.info("LockListener listener Started");
    }

    @Override
    protected InstanceIdentifier<Lock> getWildCardPath() {
        return InstanceIdentifier.create(Locks.class).child(Lock.class);
    }

    @Override
    protected void remove(InstanceIdentifier<Lock> key, Lock remove) {
        String lockName = remove.getLockName();
        LOG.info("Received remove for lock {} : {}", lockName, remove);
        java.util.Optional.ofNullable(lockManager.getSynchronizerForLock(lockName))
            .ifPresent(future -> future.complete(null));
    }

    @Override
    protected void update(InstanceIdentifier<Lock> key, Lock dataObjectModificationBefore,
            Lock dataObjectModificationAfter) {
    }

    @Override
    protected void add(InstanceIdentifier<Lock> key, Lock add) {
        LOG.info("Received add for lock {} : {}", add.getLockName(), add);
    }

    @Override
    protected LockListener getDataTreeChangeListener() {
        return this;
    }

}
