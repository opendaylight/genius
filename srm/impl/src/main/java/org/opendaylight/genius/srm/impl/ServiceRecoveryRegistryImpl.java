/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.srm.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Singleton;

import org.opendaylight.genius.srm.RecoverableListener;
import org.opendaylight.genius.srm.ServiceRecoveryInterface;
import org.opendaylight.genius.srm.ServiceRecoveryRegistry;
import org.ops4j.pax.cdi.api.OsgiServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@OsgiServiceProvider(classes = ServiceRecoveryRegistry.class)
public class ServiceRecoveryRegistryImpl implements ServiceRecoveryRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceRecoveryRegistryImpl.class);

    private final Map<String, ServiceRecoveryInterface> serviceRecoveryRegistry = new ConcurrentHashMap<>();

    private final Map<String, List<RecoverableListener>> recoverableListenersMap =
            new ConcurrentHashMap<String, List<RecoverableListener>>();

    @Override
    public void registerServiceRecoveryRegistry(String entityName,
                                                ServiceRecoveryInterface serviceRecoveryHandler) {
        serviceRecoveryRegistry.put(entityName, serviceRecoveryHandler);
        LOG.trace("Registered service recovery handler for {}", entityName);
    }

    @Override
    public synchronized void addRecoverableListener(String serviceName, final RecoverableListener recoverableListener) {
        List<RecoverableListener> recoverableListeners = recoverableListenersMap.get(serviceName);
        if (recoverableListeners == null) {
            recoverableListeners = Collections.synchronizedList(new ArrayList<>());
            recoverableListenersMap.put(serviceName, recoverableListeners);
        }

        recoverableListeners.add(recoverableListener);
    }

    @Override
    public void removeRecoverableListener(String serviceName, final RecoverableListener recoverableListener) {
        if (recoverableListenersMap.get(serviceName) != null) {
            recoverableListenersMap.get(serviceName).remove(recoverableListener);
        }
    }

    @Override
    public List<RecoverableListener> getRecoverableListeners(String serviceName) {
        return recoverableListenersMap.get(serviceName);
    }

    ServiceRecoveryInterface getRegisteredServiceRecoveryHandler(String entityName) {
        return serviceRecoveryRegistry.get(entityName);
    }
}