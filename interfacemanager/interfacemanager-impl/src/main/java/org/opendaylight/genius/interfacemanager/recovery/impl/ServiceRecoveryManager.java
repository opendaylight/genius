/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.recovery.impl;

import org.opendaylight.genius.interfacemanager.recovery.registry.ServiceRecoveryRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.srm.types.rev170711.EntityNameBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.srm.types.rev170711.EntityTypeBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceRecoveryManager {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceRecoveryManager.class);

    public static String getServiceRegistryKey(Class<? extends EntityNameBase> entityName) {
        return entityName.toString();
    }

    /**
     * Initiates recovery mechanism for a particular interface-manager entity.
     * This method tries to check whether there is a registered handler for the incoming
     * service recovery request within interface-manager and redirects the call
     * to the respective handler if found.
     *  @param entityType
     *            The type of service recovery. eg :SERVICE or INSTANCE.
     * @param entityName
     *            The type entity for which recovery has to be started. eg : INTERFACE or DPN.
     * @param entityId
     *            The unique id to represent the entity to be recovered
     */
    public static void recoverService(Class<? extends EntityTypeBase> entityType,
                                         Class<? extends EntityNameBase> entityName, String entityId) {
        String serviceRegistryKey = getServiceRegistryKey(entityName);
        ServiceRecoveryRegistry.getRegisteredServiceRecoveryHandler(serviceRegistryKey).recoverService(entityId);
    }
}
