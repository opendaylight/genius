/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.recovery.impl;

import org.opendaylight.genius.interfacemanager.listeners.HwVTEPTunnelsStateListener;
import org.opendaylight.genius.interfacemanager.listeners.InterfaceConfigListener;
import org.opendaylight.genius.interfacemanager.listeners.InterfaceInventoryStateListener;
import org.opendaylight.genius.interfacemanager.listeners.InterfaceTopologyStateListener;
import org.opendaylight.genius.interfacemanager.listeners.TerminationPointStateListener;
import org.opendaylight.genius.interfacemanager.recovery.ServiceRecoveryInterface;
import org.opendaylight.genius.interfacemanager.recovery.registry.ServiceRecoveryRegistry;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.listeners.FlowBasedServicesConfigListener;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.listeners.FlowBasedServicesInterfaceStateListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.srm.types.rev170711.GeniusIfm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class InterfaceServiceRecoveryHandler implements ServiceRecoveryInterface {
    private static final Logger LOG = LoggerFactory.getLogger(InterfaceServiceRecoveryHandler.class);
    @Inject
    private InterfaceConfigListener interfaceConfigListener;
    @Inject
    private InterfaceInventoryStateListener interfaceInventoryStateListener;
    @Inject
    private InterfaceTopologyStateListener interfaceTopologyStateListener;
    @Inject
    private TerminationPointStateListener terminationPointStateListener;
    @Inject
    private FlowBasedServicesConfigListener flowBasedServicesConfigListener;
    @Inject
    private FlowBasedServicesInterfaceStateListener flowBasedServicesInterfaceStateListener;
    @Inject
    private HwVTEPTunnelsStateListener hwVTEPTunnelsStateListener;

    @Inject
    public InterfaceServiceRecoveryHandler() {
        LOG.info("registering IFM service recovery handlers");
        ServiceRecoveryRegistry.registerServiceRecoveryRegistry(buildServiceRegistryKey(), this);
    }


    private void deregisterListeners() {
        interfaceConfigListener.close();
        interfaceInventoryStateListener.close();
        interfaceTopologyStateListener.close();
        terminationPointStateListener.close();
        flowBasedServicesConfigListener.close();
        flowBasedServicesInterfaceStateListener.close();
        hwVTEPTunnelsStateListener.close();
    }

    private void registerListeners() {
        interfaceConfigListener.registerListener();
        interfaceInventoryStateListener.registerListener();
        interfaceTopologyStateListener.registerListener();
        terminationPointStateListener.registerListener();
        flowBasedServicesConfigListener.registerListener();
        flowBasedServicesInterfaceStateListener.registerListener();
        hwVTEPTunnelsStateListener.registerListener();
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
