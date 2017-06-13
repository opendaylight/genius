/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.recovery.impl;

import org.opendaylight.genius.interfacemanager.recovery.registry.ServiceRecoveryRegistry;
import org.opendaylight.genius.interfacemanager.recovery.utils.ServiceRecoveryConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceRecoveryManager {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceRecoveryManager.class);

    public static String getServiceRegistryKey(String entityName, String entityType) {
        if (ServiceRecoveryConstants.SERVICE_PREFIX.equals(entityType)) {
            return new StringBuilder(entityType).append(ServiceRecoveryConstants.SERVICE_ENTITY_NAME_SEPARATOR)
                .append(ServiceRecoveryConstants.SERVICE_NAME).toString();
        } else {
            return new StringBuilder(entityType).append(ServiceRecoveryConstants.SERVICE_ENTITY_NAME_SEPARATOR)
                .append(entityName).toString();
        }
    }

    /**
     * Initiates recovery mechanism for a particular interface-manager entity.
     * This method tries to check whether there is a registered handler for the incoming
     * service recovery request within interface-manager and redirects the call
     * to the respective handler if found.
     *
     * @param entityName
     *            The type entity for which recovery has to be started. eg : INTERFACE or DPN.
     * @param entityType
     *            The type of service recovery. eg :SERVICE or INSTANCE.
     * @param entityId
     *            The unique identifier for the service instance.
     */
    public static boolean recoverService(String entityType, String entityName, String entityId) {
        String serviceRegistryKey = getServiceRegistryKey(entityName, entityType);
        return (ServiceRecoveryRegistry.getRegisteredServiceRecoveryHandler(serviceRegistryKey)
            .recoverService(entityName, entityType, entityId));
    }
}
