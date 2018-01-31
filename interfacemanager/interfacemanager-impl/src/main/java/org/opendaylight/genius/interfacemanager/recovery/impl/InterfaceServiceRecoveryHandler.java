/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.recovery.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.opendaylight.genius.interfacemanager.recovery.ServiceRecoveryInterface;
import org.opendaylight.genius.interfacemanager.recovery.listeners.RecoverableListener;
import org.opendaylight.genius.interfacemanager.recovery.registry.ServiceRecoveryRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.srm.types.rev170711.GeniusIfm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class InterfaceServiceRecoveryHandler implements ServiceRecoveryInterface {

    private static final Logger LOG = LoggerFactory.getLogger(InterfaceServiceRecoveryHandler.class);

    private final List<RecoverableListener> recoverableListeners = Collections.synchronizedList(new ArrayList<>());

    @Inject
    public InterfaceServiceRecoveryHandler(final ServiceRecoveryRegistry serviceRecoveryRegistry) {
        LOG.info("registering IFM service recovery handlers");
        serviceRecoveryRegistry.registerServiceRecoveryRegistry(buildServiceRegistryKey(), this);
    }

    public void addRecoverableListener(final RecoverableListener recoverableListener) {
        recoverableListeners.add(recoverableListener);
    }

    public void removeRecoverableListener(final RecoverableListener recoverableListener) {
        recoverableListeners.add(recoverableListener);
    }

    private void deregisterListeners() {
        synchronized (recoverableListeners) {
            recoverableListeners.forEach((recoverableListener -> recoverableListener.deregisterListener()));
        }
    }

    private void registerListeners() {
        synchronized (recoverableListeners) {
            recoverableListeners.forEach((recoverableListener -> recoverableListener.registerListener()));
        }
    }

    @Override
    public void recoverService(final String entityId) {
        LOG.info("recover IFM service by deregistering and registering all relevant listeners");
        deregisterListeners();
        registerListeners();
    }

    public String buildServiceRegistryKey() {
        return GeniusIfm.class.toString();
    }
}
