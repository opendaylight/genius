/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.renderer.ovs.utilities;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.common.api.clustering.CandidateAlreadyRegisteredException;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipService;
import org.opendaylight.genius.interfacemanager.InterfacemgrProvider;
import org.opendaylight.genius.utils.clustering.ClusteringUtils;
import org.opendaylight.genius.utils.clustering.EntityOwnerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IfmClusterUtils {
    private static final Logger LOG = LoggerFactory.getLogger(IfmClusterUtils.class);
    public static final String INTERFACE_CONFIG_ENTITY = "interface_config";
    public static final String INTERFACE_SERVICE_BINDING_ENTITY = "interface_service_binding";

    private  static InterfacemgrProvider ifaceServiceProvider = null;

    public static void setIfaceServiceProvider(InterfacemgrProvider provider) {
        ifaceServiceProvider = provider;
    }

    public static void registerEntityForOwnership(InterfacemgrProvider provider,
            EntityOwnershipService entityOwnershipService) {
        setIfaceServiceProvider(provider);
        try {
            EntityOwnerUtils.registerEntityCandidateForOwnerShip(entityOwnershipService, INTERFACE_CONFIG_ENTITY,
                INTERFACE_CONFIG_ENTITY, null/*listener*/);
            EntityOwnerUtils.registerEntityCandidateForOwnerShip(entityOwnershipService, INTERFACE_SERVICE_BINDING_ENTITY,
                INTERFACE_SERVICE_BINDING_ENTITY, null/*listener*/);
        } catch (CandidateAlreadyRegisteredException e) {
            LOG.error("failed to register entity {} for entity-owenership-service", e.getEntity());
        }
    }

    public static void runOnlyInLeaderNode(final Runnable job, String entity) {
        ListenableFuture<Boolean> checkEntityOwnerFuture = ClusteringUtils.checkNodeEntityOwner(
            ifaceServiceProvider.getEntityOwnershipService(), INTERFACE_CONFIG_ENTITY, INTERFACE_CONFIG_ENTITY);
        Futures.addCallback(checkEntityOwnerFuture, new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean isOwner) {
                LOG.debug("scheduling job {} ", isOwner);
                if (isOwner) {
                    job.run();
                } else {
                    LOG.trace("job is not run as node is not owner for :{} ", INTERFACE_CONFIG_ENTITY);
                }
            }

            @Override
            public void onFailure(Throwable error) {
                LOG.error("Failed to identify owner for entity {} due to {}", INTERFACE_CONFIG_ENTITY, error);
            }
        });
    }
}
