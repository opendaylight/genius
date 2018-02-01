/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.scaling.renderer.ovs.utilities;

/*import org.opendaylight.controller.md.sal.common.api.clustering.CandidateAlreadyRegisteredException;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipService;
import org.opendaylight.genius.itm.globals.ITMConstants;*/
import org.opendaylight.genius.itm.impl.ItmProvider;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*import static org.opendaylight.genius.itm.globals.ITMConstants.ITM_CONFIG_ENTITY;*/

public class ItmClusterUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ItmClusterUtils.class);
    private static ItmProvider itmProvider = null;


    public static void setIfaceServiceProvider(ItmProvider provider) {
        itmProvider = provider;
    }

   /* public static void registerEntityForOwnership(ItmProvider provider,
                                                  EntityOwnershipService entityOwnershipService) {
        setIfaceServiceProvider(provider);
        try {
            EntityOwnerUtils.registerEntityCandidateForOwnerShip(entityOwnershipService, ITMConstants.ITM_CONFIG_ENTITY,
                    ITM_CONFIG_ENTITY, null*//**//* listener *//**//*);
        } catch (CandidateAlreadyRegisteredException e) {
            LOG.error("failed to register entity {} for entity-ownership-service", e.getEntity());
        }
    }*/
    /*
     * For cases where the EOS check needs to be run on the same thread.
     */
    public static Boolean isEntityOwner(String entity, EntityOwnershipUtils entityOwnershipUtils) {
        return entityOwnershipUtils.isEntityOwner(entity, entity);
    }
}
