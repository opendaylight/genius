/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.lockmanager.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncClusteredDataTreeChangeListenerBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.Locks;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.locks.Lock;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.ops4j.pax.cdi.api.OsgiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class LockListener extends AsyncClusteredDataTreeChangeListenerBase<Lock, LockListener> {

    private static final Logger LOG = LoggerFactory.getLogger(LockListener.class);
    private final LockManagerServiceImpl lockManager;

    @Inject
    public LockListener(@OsgiService DataBroker broker, LockManagerServiceImpl lockManager) {
        super(Lock.class, LockListener.class);
        this.lockManager = lockManager;
        registerListener(LogicalDatastoreType.OPERATIONAL, broker);
    }

    @Override
    protected InstanceIdentifier<Lock> getWildCardPath() {
        return InstanceIdentifier.create(Locks.class).child(Lock.class);
    }

    @Override
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    protected void remove(InstanceIdentifier<Lock> key, Lock remove) {
        String lockName = remove.getLockName();
        LOG.debug("Received remove for lock {} : {}", lockName, remove);
        CompletableFuture<Void> lock = lockManager.getSynchronizerForLock(lockName);
        if (lock != null) {
            // FindBugs flags a false violation here - "passes a null value as the parameter of a method which must be
            // non-null. Either this parameter has been explicitly marked as @Nonnull, or analysis has determined that
            // this parameter is always dereferenced.". However neither is true. The type param is Void so you have to
            // pas null.
            lock.complete(null);
        }
    }

    @Override
    protected void update(InstanceIdentifier<Lock> key, Lock dataObjectModificationBefore,
            Lock dataObjectModificationAfter) {
    }

    @Override
    protected void add(InstanceIdentifier<Lock> key, Lock add) {
        LOG.debug("Received add for lock {} : {}", add.getLockName(), add);
    }

    @Override
    protected LockListener getDataTreeChangeListener() {
        return this;
    }
}
