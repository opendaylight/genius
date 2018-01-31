/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.recovery.registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Singleton;
import org.opendaylight.genius.interfacemanager.recovery.ServiceRecoveryInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ServiceRecoveryRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceRecoveryRegistry.class);

    private final Map<String, ServiceRecoveryInterface> serviceRecoveryRegistry = new ConcurrentHashMap<>();

    public void registerServiceRecoveryRegistry(String entityName,
                                                ServiceRecoveryInterface serviceRecoveryHandler) {
        serviceRecoveryRegistry.put(entityName, serviceRecoveryHandler);
        LOG.trace("Registered service recovery handler for {}", entityName);
    }

    public ServiceRecoveryInterface getRegisteredServiceRecoveryHandler(String entityName) {
        return serviceRecoveryRegistry.get(entityName);
    }
}