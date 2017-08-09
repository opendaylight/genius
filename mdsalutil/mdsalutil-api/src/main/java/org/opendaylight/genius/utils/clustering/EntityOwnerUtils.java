/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.utils.clustering;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentMap;
import org.opendaylight.controller.md.sal.common.api.clustering.CandidateAlreadyRegisteredException;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipChange;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListener;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipService;
import org.opendaylight.genius.utils.cache.CacheUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntityOwnerUtils {

    private static final Logger LOG = LoggerFactory.getLogger(EntityOwnerUtils.class);

    public static final String ENTITY_OWNER_CACHE = "entity.owner.cache";
    private static final ArrayList<EntityEvent> EVENTS_HISTORY = new ArrayList<>();

    private EntityOwnerUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static class EntityEvent {
        private final long time;
        private final String entityName;
        private final boolean isOwner;
        private final boolean hasOwner;

        public EntityEvent(long time, String entityName, boolean hasOwner, boolean isOwner) {
            this.time = time;
            this.entityName = entityName;
            this.hasOwner = hasOwner;
            this.isOwner = isOwner;
        }

        public long getTime() {
            return time;
        }

        public String getEntityName() {
            return entityName;
        }

        public boolean isOwner() {
            return isOwner;
        }

        public boolean hasOwner() {
            return hasOwner;
        }
    }

    public static ArrayList<EntityEvent> getEventsHistory() {
        return EVENTS_HISTORY;
    }

    static {
        createEntityOwnerCache();
    }

    private static void createEntityOwnerCache() {
        if (CacheUtil.getCache(ENTITY_OWNER_CACHE) == null) {
            CacheUtil.createCache(ENTITY_OWNER_CACHE);
        }
    }

    private static String getEntity(String entityType, String entityName) {
        return entityType;
    }

    public static boolean amIEntityOwner(String entityType, String entityName) {
        ConcurrentMap<String, Boolean> entityOwnerCache = (ConcurrentMap<String, Boolean>) CacheUtil
                .getCache(ENTITY_OWNER_CACHE);
        String entity = getEntity(entityType, entityName);
        boolean ret = false;
        if (entityOwnerCache != null) {
            if (entityOwnerCache.get(entity) != null) {
                ret = entityOwnerCache.get(entity);
            }
        } else {
            LOG.error("entity owner cache null");
        }
        LOG.trace("get entity owner result {} for type {}", ret, entity);
        return ret;
    }

    /**
     * Registers the entityName for ownership for given entityType adds a local
     * listener which takes care of updating the cached entity status.
     *
     * @param listener
     *            also adds this listener for ownership events if provided
     */
    public static void registerEntityCandidateForOwnerShip(EntityOwnershipService entityOwnershipService,
            String entityType, String entityName, EntityOwnershipListener listener)
            throws CandidateAlreadyRegisteredException {
        LOG.info("registering for entity ownership for type {}", entityType);
        Entity candidateEntity = new Entity(entityType, entityName);
        entityOwnershipService.registerCandidate(candidateEntity);
        entityOwnershipService.registerListener(entityType, entityOwnershipListener);
        if (listener != null) {
            entityOwnershipService.registerListener(entityType, listener);
        }
        LOG.info("registered for entity ownership for type {}", entityType);
        // TODO track registrations for closing
    }

    private static Listener entityOwnershipListener = new Listener();

    static class Listener implements EntityOwnershipListener {

        @Override
        public void ownershipChanged(EntityOwnershipChange ownershipChange) {
            String entityType = ownershipChange.getEntity().getType();
            String entityName = ownershipChange.getEntity().getId().toString();
            LOG.info("entity ownership changed for {}", entityType);
            EVENTS_HISTORY.add(new EntityEvent(System.currentTimeMillis(), entityName, ownershipChange.hasOwner(),
                    ownershipChange.isOwner()));
            if (ownershipChange.hasOwner() && ownershipChange.isOwner()) {
                LOG.info("entity ownership change became owner for type {}", entityType);
                updateEntityOwner(entityType, entityName, Boolean.TRUE);
            } else {
                LOG.info("entity ownership lost ownership for type {} ", entityType);
                updateEntityOwner(entityType, entityName, Boolean.FALSE);
            }
        }

        private void updateEntityOwner(String entityType, String entityName, Boolean isOwner) {
            ConcurrentMap<String, Boolean> entityOwnerCache = (ConcurrentMap<String, Boolean>) CacheUtil
                    .getCache(ENTITY_OWNER_CACHE);
            String entity = getEntity(entityType, entityName);
            if (entityOwnerCache != null) {
                LOG.trace("updating entity owner {} {}", isOwner, entity);
                entityOwnerCache.put(entity, isOwner);
            }
        }
    }
}
