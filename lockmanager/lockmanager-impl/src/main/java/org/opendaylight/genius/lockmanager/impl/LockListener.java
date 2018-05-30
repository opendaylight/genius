/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.lockmanager.impl;

import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.tools.mdsal.listener.AbstractClusteredAsyncDataTreeChangeListener;
import org.opendaylight.infrautils.utils.concurrent.Executors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.Locks;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.locks.Lock;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.ops4j.pax.cdi.api.OsgiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class LockListener extends AbstractClusteredAsyncDataTreeChangeListener<Lock> {

    private static final long TIMEOUT_FOR_SHUTDOWN = 30;

    private static final Logger LOG = LoggerFactory.getLogger(LockListener.class);

    private final LockManagerServiceImpl lockManager;

    @Inject
    public LockListener(@OsgiService DataBroker dataBroker, LockManagerServiceImpl lockManager) {
        super(dataBroker, LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(Locks.class).child(Lock.class),
              Executors.newSingleThreadExecutor("LockListener", LOG));
        this.lockManager = lockManager;
    }

    @Override
    public void remove(@Nonnull InstanceIdentifier<Lock> instanceIdentifier, @Nonnull Lock removedLock) {
        String lockName = removedLock.getLockName();
        LOG.debug("Received remove for lock {} : {}", lockName, removedLock);
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
    public void update(@Nonnull InstanceIdentifier<Lock> instanceIdentifier, @Nonnull Lock originalLock,
                       @Nonnull Lock updatedLock) {
        // NOOP
    }

    @Override
    public void add(@Nonnull InstanceIdentifier<Lock> instanceIdentifier, @Nonnull Lock lock) {
        LOG.debug("Received add for lock {} : {}", lock.getLockName(), lock);
    }

    @Override
    @PreDestroy
    public void close() {
        super.close();
        MoreExecutors.shutdownAndAwaitTermination(getExecutorService(), TIMEOUT_FOR_SHUTDOWN, TimeUnit.SECONDS);
    }
}
