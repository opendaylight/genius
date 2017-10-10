/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.utils.clustering;

import com.google.common.base.Optional;
import org.opendaylight.genius.utils.SystemPropertyReader;
import org.opendaylight.mdsal.eos.binding.api.Entity;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipService;
import org.opendaylight.mdsal.eos.common.api.EntityOwnershipState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntityOwnershipUtils {
    private static final Logger LOG = LoggerFactory.getLogger(EntityOwnershipUtils.class);

    private EntityOwnershipUtils() {
    }

    public static boolean isEntityOwner(EntityOwnershipService entityOwnershipService, String entityType,
            String nodeId) {
        return isEntityOwner(entityOwnershipService, new Entity(entityType, nodeId));
    }

    public static boolean isEntityOwner(EntityOwnershipService entityOwnershipService, Entity entity) {
        return isEntityOwner(entityOwnershipService, entity, SystemPropertyReader.Cluster.getSleepTimeBetweenRetries(),
                SystemPropertyReader.Cluster.getMaxRetries());
    }

    public static boolean isEntityOwner(EntityOwnershipService entityOwnershipService,
            Entity entity, long sleepBetweenRetries, int retries) {
        while (retries-- > 0) {
            Optional<EntityOwnershipState> entityState = entityOwnershipService.getOwnershipState(entity);
            if (entityState.isPresent()) {
                EntityOwnershipState entityOwnershipState = entityState.get();
                return entityOwnershipState == EntityOwnershipState.IS_OWNER;
            }

            LOG.trace("EntityOwnershipState for entity type {} is not yet available. {} retries left",
                    entity.getType(), retries);
            try {
                Thread.sleep(sleepBetweenRetries);
            } catch (InterruptedException e) {
                break;
            }
        }

        return false;
    }
}
