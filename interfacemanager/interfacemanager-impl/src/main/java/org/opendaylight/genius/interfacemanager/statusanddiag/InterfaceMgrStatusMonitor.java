/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.statusanddiag;

import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.InterfacemgrProvider;
import org.opendaylight.infrautils.diagstatus.DiagStatusService;
import org.opendaylight.infrautils.diagstatus.ServiceDescriptor;
import org.opendaylight.infrautils.diagstatus.ServiceState;
import org.opendaylight.infrautils.diagstatus.ServiceStatusProvider;
import org.ops4j.pax.cdi.api.OsgiServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@OsgiServiceProvider(classes = ServiceStatusProvider.class)
public class InterfaceMgrStatusMonitor implements ServiceStatusProvider {
    private ServiceDescriptor serviceDescriptor;
    private static final Logger LOG = LoggerFactory.getLogger(InterfaceMgrStatusMonitor.class);
    private final DiagStatusService diagStatusService;

    @Inject
    public InterfaceMgrStatusMonitor(final InterfacemgrProvider interfaceMgrProvider,
                                      final DiagStatusService diagStatusService) {
        this.diagStatusService = diagStatusService;
        diagStatusService.register(IfmConstants.INTERFACE_SERVICE_NAME);
    }

    @PostConstruct
    public void start() {
        serviceDescriptor = new ServiceDescriptor(IfmConstants.INTERFACE_SERVICE_NAME, ServiceState.OPERATIONAL,"Service started");
        diagStatusService.report(serviceDescriptor);
    }

    @PreDestroy
    public void close() {
        serviceDescriptor = new ServiceDescriptor(IfmConstants.INTERFACE_SERVICE_NAME, ServiceState.UNREGISTERED, "Service Closed");
        diagStatusService.report(serviceDescriptor);
    }

    @Override
    public ServiceDescriptor getServiceDescriptor() {
        // TODO Add logic here to derive the dynamic service state.
        // Currently this is just returning the initial state.
        return serviceDescriptor;
    }
}
