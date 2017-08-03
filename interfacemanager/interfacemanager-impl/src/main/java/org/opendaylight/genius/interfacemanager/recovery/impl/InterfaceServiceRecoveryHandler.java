/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.recovery.impl;

import org.opendaylight.genius.interfacemanager.InterfacemgrProvider;
import org.opendaylight.genius.interfacemanager.recovery.ServiceRecoveryInterface;
import org.opendaylight.genius.interfacemanager.recovery.registry.ServiceRecoveryRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.srm.types.rev170711.GeniusIfm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class InterfaceServiceRecoveryHandler implements ServiceRecoveryInterface {
    private static final Logger LOG = LoggerFactory.getLogger(InterfaceServiceRecoveryHandler.class);
    private InterfacemgrProvider interfacemgrProvider;

    @Inject
    public InterfaceServiceRecoveryHandler(InterfacemgrProvider interfacemgrProvider) {
        this.interfacemgrProvider = interfacemgrProvider;
        init();
    }

    public void init() {
        LOG.info("{} start", getClass().getSimpleName());
        ServiceRecoveryRegistry.registerServiceRecoveryRegistry(buildServiceRegistryKey(), this);
    }

    private static void deregisterListeners() {
        // TODO
    }

    private void registerListeners() {
        // TODO
    }

    public void recoverService(String entityId) {
        LOG.info("recover IFM service by deregistering and registering all relevant listeners");
        deregisterListeners();
        registerListeners();
    }

    public String buildServiceRegistryKey() {
        return GeniusIfm.class.toString();
    }
}
