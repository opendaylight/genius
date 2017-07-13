/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.datastoreutils;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.concurrent.Callable;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinatorMonitor;

/**
 * DataStoreJobCoordinator.
 *
 * @deprecated Use org.opendaylight.infrautils.jobcoordinator.JobCoordinator
 *             instead of this. Please note that in its new reincarnation it's no
 *             longer a static singleton but now an OSGi service which you can (must)
 *             {@literal @}Inject as {@literal @}OsgiService into your class using it.
 */
@Deprecated
public class DataStoreJobCoordinator {

    private static DataStoreJobCoordinator instance;

    public static DataStoreJobCoordinator getInstance() {
        if (instance == null) {
            throw new IllegalStateException("@Deprecated DataStoreJobCoordinator instance static not initalized "
                    + "(Component Tests should use JobCoordinatorTestModule in their GuiceRule)");
        }
        return instance;
    }

    private static void setInstance(DataStoreJobCoordinator dataStoreJobCoordinator) {
        if (instance != null && dataStoreJobCoordinator != null) {
            throw new IllegalStateException("@Deprecated DataStoreJobCoordinator instance static already initalized "
                    + "with a previous instance which needs to be closed and then null-ified, instead of overwritten "
                    + "(Component Tests should use JobCoordinatorTestModule in their GuiceRule)");
        }
        instance = dataStoreJobCoordinator;
    }

    private final JobCoordinator infrautilsJobCoordinatorDelegate;
    private final JobCoordinatorMonitor infrautilsJobCoordinatorMonitor;

    public DataStoreJobCoordinator(JobCoordinator jobCoordinator, JobCoordinatorMonitor jobCoordinatorMonitor) {
        this.infrautilsJobCoordinatorDelegate = jobCoordinator;
        this.infrautilsJobCoordinatorMonitor = jobCoordinatorMonitor;
        setInstance(this);
    }

    public void close() {
        setInstance(null);
    }

    public void enqueueJob(String key, Callable<List<ListenableFuture<Void>>> mainWorker) {
        infrautilsJobCoordinatorDelegate.enqueueJob(key, mainWorker);
    }

    public void enqueueJob(String key, Callable<List<ListenableFuture<Void>>> mainWorker,
            RollbackCallable rollbackWorker) {
        infrautilsJobCoordinatorDelegate.enqueueJob(key, mainWorker,
                new InfrautilsRollbackCallableDelegate(rollbackWorker));
    }

    public void enqueueJob(String key, Callable<List<ListenableFuture<Void>>> mainWorker, int maxRetries) {
        infrautilsJobCoordinatorDelegate.enqueueJob(key, mainWorker, maxRetries);
    }

    public void enqueueJob(String key, Callable<List<ListenableFuture<Void>>> mainWorker,
                           RollbackCallable rollbackWorker, int maxRetries) {
        infrautilsJobCoordinatorDelegate.enqueueJob(key, mainWorker,
                new InfrautilsRollbackCallableDelegate(rollbackWorker), maxRetries);
    }

    public long getIncompleteTaskCount() {
        return infrautilsJobCoordinatorMonitor.getIncompleteTaskCount();
    }

    private static class InfrautilsRollbackCallableDelegate
        extends org.opendaylight.infrautils.jobcoordinator.RollbackCallable {

        private final RollbackCallable geniusRollbackCallable;

        InfrautilsRollbackCallableDelegate(RollbackCallable rollbackCallable) {
            this.geniusRollbackCallable = rollbackCallable;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            return geniusRollbackCallable.call();
        }
    }
}
