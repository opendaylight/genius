/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.utils.clustering;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipService;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipState;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.utils.SystemPropertyReader;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusteringUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ClusteringUtils.class);
    private static DataStoreJobCoordinator dataStoreJobCoordinator;

    static DataStoreJobCoordinator getDataStoreJobCoordinator() {
        if (dataStoreJobCoordinator == null) {
            dataStoreJobCoordinator = DataStoreJobCoordinator.getInstance();
        }
        return dataStoreJobCoordinator;
    }

    public static void setDataStoreJobCoordinator(DataStoreJobCoordinator ds) {
        dataStoreJobCoordinator = ds;
    }

    public static ListenableFuture<Boolean> checkNodeEntityOwner(EntityOwnershipService entityOwnershipService,
                                                                 String entityType, String nodeId) {
        return checkNodeEntityOwner(entityOwnershipService, new Entity(entityType, nodeId),
               SystemPropertyReader.Cluster.getSleepTimeBetweenRetries(), SystemPropertyReader.Cluster.getMaxRetries());
    }

    public static ListenableFuture<Boolean> checkNodeEntityOwner(EntityOwnershipService entityOwnershipService,
                                                                 String entityType, YangInstanceIdentifier nodeId) {
        return checkNodeEntityOwner(entityOwnershipService, new Entity(entityType, nodeId),
               SystemPropertyReader.Cluster.getSleepTimeBetweenRetries(), SystemPropertyReader.Cluster.getMaxRetries());
    }

    public static ListenableFuture<Boolean> checkNodeEntityOwner(EntityOwnershipService entityOwnershipService,
                                                                 Entity entity, long sleepBetweenRetries,
                                                                 int maxRetries) {
        SettableFuture<Boolean> checkNodeEntityfuture = SettableFuture.create();
        CheckEntityOwnerTask checkEntityOwnerTask = new CheckEntityOwnerTask(entityOwnershipService, entity,
                checkNodeEntityfuture, sleepBetweenRetries, maxRetries);
        getDataStoreJobCoordinator().enqueueJob(entityOwnershipService.toString(), checkEntityOwnerTask);
        return checkNodeEntityfuture;
    }

    private static class CheckEntityOwnerTask implements Callable<List<ListenableFuture<Void>>> {
        EntityOwnershipService entityOwnershipService;
        Entity entity;
        SettableFuture<Boolean> checkNodeEntityfuture;
        long sleepBetweenRetries;
        int retries;

        CheckEntityOwnerTask(EntityOwnershipService entityOwnershipService, Entity entity,
                                    SettableFuture<Boolean> checkNodeEntityfuture, long sleepBetweenRetries,
                                    int retries) {
            this.entityOwnershipService = entityOwnershipService;
            this.entity = entity;
            this.checkNodeEntityfuture = checkNodeEntityfuture;
            this.sleepBetweenRetries = sleepBetweenRetries;
            this.retries = retries;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            while (retries > 0) {
                retries = retries - 1;
                Optional<EntityOwnershipState> entityState = entityOwnershipService.getOwnershipState(entity);
                if (entityState.isPresent()) {
                    EntityOwnershipState entityOwnershipState = entityState.get();
                    if (entityOwnershipState.hasOwner()) {
                        checkNodeEntityfuture.set(entityOwnershipState.isOwner());
                        return getResultFuture();
                    }
                }
                LOG.trace("EntityOwnershipState for entity type {} is not yet available. {} retries left",
                        entity.getType(), retries);
                Thread.sleep(sleepBetweenRetries);
            }
            checkNodeEntityfuture.setException(new EntityOwnerNotPresentException("Entity Owner Not Present"));
            return getResultFuture();
        }

        private List<ListenableFuture<Void>> getResultFuture() {
            return Collections.singletonList(Futures.immediateFuture(null));
        }
    }
}
