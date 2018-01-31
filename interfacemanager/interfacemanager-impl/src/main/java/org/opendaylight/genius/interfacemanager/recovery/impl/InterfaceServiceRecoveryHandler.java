/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.recovery.impl;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
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

@Singleton
public class InterfaceServiceRecoveryHandler implements ServiceRecoveryInterface {
    private static final Logger LOG = LoggerFactory.getLogger(InterfaceServiceRecoveryHandler.class);
    private DataBroker dataBroker;
    private InterfaceConfigListener interfaceConfigListener;
    private InterfaceInventoryStateListener interfaceInventoryStateListener;
    private InterfaceTopologyStateListener interfaceTopologyStateListener;
    private TerminationPointStateListener terminationPointStateListener;
    private FlowBasedServicesConfigListener flowBasedServicesConfigListener;
    private FlowBasedServicesInterfaceStateListener flowBasedServicesInterfaceStateListener;
    private HwVTEPTunnelsStateListener hwVTEPTunnelsStateListener;

    @Inject
    public InterfaceServiceRecoveryHandler(DataBroker dataBroker,
                                           InterfaceConfigListener interfaceConfigListener,
                                           InterfaceInventoryStateListener interfaceInventoryStateListener,
                                           InterfaceTopologyStateListener interfaceTopologyStateListener,
                                           TerminationPointStateListener terminationPointStateListener,
                                           FlowBasedServicesConfigListener flowBasedServicesConfigListener,
                                           FlowBasedServicesInterfaceStateListener flowBasedServicesStateListener,
                                           HwVTEPTunnelsStateListener hwVTEPTunnelsStateListener) {
        this.dataBroker = dataBroker;
        this.interfaceConfigListener = interfaceConfigListener;
        this.interfaceInventoryStateListener = interfaceInventoryStateListener;
        this.interfaceTopologyStateListener = interfaceTopologyStateListener;
        this.terminationPointStateListener = terminationPointStateListener;
        this.flowBasedServicesConfigListener = flowBasedServicesConfigListener;
        this.flowBasedServicesInterfaceStateListener = flowBasedServicesStateListener;
        this.hwVTEPTunnelsStateListener = hwVTEPTunnelsStateListener;
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
        interfaceConfigListener.registerListener(dataBroker);
        interfaceInventoryStateListener.registerListener();
        interfaceTopologyStateListener.registerListener();
        terminationPointStateListener.registerListener(dataBroker);
        flowBasedServicesConfigListener.registerListener();
        flowBasedServicesInterfaceStateListener.registerListener();
        hwVTEPTunnelsStateListener.registerListener(dataBroker);
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
