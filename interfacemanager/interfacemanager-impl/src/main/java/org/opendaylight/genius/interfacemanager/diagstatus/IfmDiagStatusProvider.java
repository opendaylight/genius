/*
 * Copyright (c) 2017, 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.diagstatus;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.aries.blueprint.annotation.service.Service;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.infrautils.diagstatus.DiagStatusService;
import org.opendaylight.infrautils.diagstatus.ServiceDescriptor;
import org.opendaylight.infrautils.diagstatus.ServiceState;
import org.opendaylight.infrautils.diagstatus.ServiceStatusProvider;

@Singleton
@Service(classes = ServiceStatusProvider.class)
public class IfmDiagStatusProvider implements ServiceStatusProvider {

    private final DiagStatusService diagStatusService;
    private volatile ServiceDescriptor serviceDescriptor;

    @Inject
    public IfmDiagStatusProvider(final DiagStatusService diagStatusService) {
        this.diagStatusService = diagStatusService;
        diagStatusService.register(IfmConstants.INTERFACE_SERVICE_NAME);
        reportStatus(ServiceState.STARTING);
    }

    public void reportStatus(ServiceState serviceState) {
        serviceDescriptor = new ServiceDescriptor(IfmConstants.INTERFACE_SERVICE_NAME, serviceState);
        diagStatusService.report(serviceDescriptor);
    }

    public void reportStatus(Throwable exception) {
        serviceDescriptor = new ServiceDescriptor(IfmConstants.INTERFACE_SERVICE_NAME, exception);
        diagStatusService.report(serviceDescriptor);
    }

    @PreDestroy
    public void close() {
        reportStatus(ServiceState.UNREGISTERED);
    }

    @Override
    public ServiceDescriptor getServiceDescriptor() {
        // TODO Add logic here to derive the dynamic service state.
        return serviceDescriptor;
    }
}
