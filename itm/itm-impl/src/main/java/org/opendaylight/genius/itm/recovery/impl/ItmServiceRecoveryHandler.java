/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.recovery.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.genius.srm.RecoverableListener;
import org.opendaylight.genius.srm.ServiceRecoveryInterface;
import org.opendaylight.genius.srm.ServiceRecoveryRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.srm.types.rev170711.GeniusItm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ItmServiceRecoveryHandler implements ServiceRecoveryInterface {

    private static final Logger LOG = LoggerFactory.getLogger(ItmServiceRecoveryHandler.class);

    private final ServiceRecoveryRegistry serviceRecoveryRegistry;
    private final List<RecoverableListener> listeners = Collections.synchronizedList(new ArrayList<>());

    @Inject
    public ItmServiceRecoveryHandler(final ServiceRecoveryRegistry serviceRecoveryRegistry) {
        LOG.info("registering ITM service recovery handlers");
        this.serviceRecoveryRegistry = serviceRecoveryRegistry;
        serviceRecoveryRegistry.registerServiceRecoveryRegistry(getServiceRegistryKey(),this);
    }

    public static String getServiceRegistryKey() {
        return GeniusItm.class.toString();
    }

    @Override
    public void recoverService(String entityId) {
        LOG.info("recover ITM service by deregistering and registering all relevant listeners");
        deregisterListeners();
        registerListeners();
    }

    private void registerListeners() {
        LOG.info("Re-Registering ITM Listeners for recovery");
        serviceRecoveryRegistry.getRecoverableListeners(getServiceRegistryKey())
                .forEach((recoverableListener -> recoverableListener.registerListener()));
    }

    private void deregisterListeners() {
        LOG.info("De-Registering ITM Listeners for recovery");
        serviceRecoveryRegistry.getRecoverableListeners(getServiceRegistryKey())
                .forEach((recoverableListener -> recoverableListener.deregisterListener()));
    }
}
