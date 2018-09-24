/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.utils.clustering;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import javax.inject.Inject;
import org.opendaylight.genius.utils.SystemPropertyReader;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.mdsal.eos.binding.api.Entity;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipService;
import org.opendaylight.mdsal.eos.common.api.EntityOwnershipState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EntityOwnershipService utilities.
 *
 * @author Thomas Pantelis
 */
public class EntityOwnershipUtils {
    private static final Logger LOG = LoggerFactory.getLogger(EntityOwnershipUtils.class);

    private final EntityOwnershipService entityOwnershipService;

    @Inject
    public EntityOwnershipUtils(EntityOwnershipService entityOwnershipService) {
        this.entityOwnershipService = Objects.requireNonNull(entityOwnershipService);
    }

    public EntityOwnershipService getEntityOwnershipService() {
        return entityOwnershipService;
    }

    /**
     * Checks if the local node is the owner of an entity.
     *
     * @param entityType the entity type
     * @param entityName the entity name
     * @return true is the owner, false otherwise
     */
    public boolean isEntityOwner(String entityType, String entityName) {
        return isEntityOwner(new Entity(entityType, entityName));
    }

    /**
     * Checks if the local node is the owner of an entity.
     *
     * @param entity the entity
     * @return true is the owner, false otherwise
     */
    public boolean isEntityOwner(Entity entity) {
        return isEntityOwner(entity, SystemPropertyReader.Cluster.getSleepTimeBetweenRetries(),
                SystemPropertyReader.Cluster.getMaxRetries());
    }

    /**
     * Checks if the local node is the owner of an entity.
     *
     * @param entity the entity
     * @param sleepBetweenRetries the busy wait interval in millis if the entity is not yet present
     * @param tries the total number of busy wait tries
     * @return true is the owner, false otherwise
     */
    public boolean isEntityOwner(Entity entity, long sleepBetweenRetries, int tries) {
        while (true) {
            java.util.Optional<EntityOwnershipState> entityState = entityOwnershipService.getOwnershipState(entity);
            if (entityState.isPresent()) {
                EntityOwnershipState entityOwnershipState = entityState.get();
                return entityOwnershipState == EntityOwnershipState.IS_OWNER;
            }

            tries--;

            LOG.debug("EntityOwnershipState for {} is not yet available. {} tries left", entity, tries);

            if (tries > 0) {
                try {
                    Thread.sleep(sleepBetweenRetries);
                } catch (InterruptedException e) {
                    break;
                }
            } else {
                break;
            }
        }

        return false;
    }

    /**
     * Runs a job task if the local node is the owner of an entity.
     *
     * @param entityType the entity type
     * @param entityName the entity name
     * @param coordinator the JobCoordinator on which to run the job task
     * @param jobDesc description of the job for logging
     * @param job the job task
     */
    public void runOnlyInOwnerNode(String entityType, String entityName, JobCoordinator coordinator, String jobDesc,
            Runnable job) {
        final Entity entity = new Entity(entityType, entityName);
        coordinator.enqueueJob(entity.toString(), () -> {
            if (isEntityOwner(entity)) {
                LOG.debug("Running job {} for {}", jobDesc, entity);
                job.run();
            } else {
                LOG.debug("runOnlyInOwnerNode: job {} was not run as I'm not the owner of {} ", jobDesc, entity);
            }

            return Collections.singletonList(Futures.immediateFuture(null));
        });
    }

    /**
     * Runs a job task if the local node is the owner of an entity.
     *
     * @param entityType the entity type
     * @param entityName the entity name
     * @param coordinator the JobCoordinator on which to run the job
     * @param jobKey the job key
     * @param jobDesc description of the job for logging
     * @param job the job task
     */
    public void runOnlyInOwnerNode(String entityType, String entityName, JobCoordinator coordinator,
            String jobKey, String jobDesc, Callable<List<ListenableFuture<Void>>> job) {
        final Entity entity = new Entity(entityType, entityName);
        coordinator.enqueueJob(entity.toString(), () -> {
            if (isEntityOwner(entity)) {
                LOG.debug("Scheduling job {} for {}", jobDesc, entity);
                coordinator.enqueueJob(jobKey, job, SystemPropertyReader.getDataStoreJobCoordinatorMaxRetries());
            } else {
                LOG.debug("runOnlyInOwnerNode: job {} was not run as I'm not the owner of {} ", jobDesc, entity);
            }

            return Collections.singletonList(Futures.immediateFuture(null));
        });
    }
}
