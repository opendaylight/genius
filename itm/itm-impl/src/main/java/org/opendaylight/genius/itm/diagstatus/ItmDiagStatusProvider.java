/*
 * Copyright (c) 2017, 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.diagstatus;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.aries.blueprint.annotation.service.Service;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.infrautils.diagstatus.DiagStatusService;
import org.opendaylight.infrautils.diagstatus.ServiceDescriptor;
import org.opendaylight.infrautils.diagstatus.ServiceState;
import org.opendaylight.infrautils.diagstatus.ServiceStatusProvider;

@Singleton
@Service(classes = ServiceStatusProvider.class)
public class ItmDiagStatusProvider implements ServiceStatusProvider {

    private final DiagStatusService diagStatusService;
    private volatile ServiceDescriptor serviceDescriptor;

    @Inject
    public ItmDiagStatusProvider(final DiagStatusService diagStatusService) {
        this.diagStatusService = diagStatusService;
        diagStatusService.register(ITMConstants.ITM_SERVICE_NAME);
        reportStatus(ServiceState.STARTING);
    }

    @PreDestroy
    public void close() {
        reportStatus(ServiceState.UNREGISTERED);
    }

    public void reportStatus(ServiceState serviceState) {
        serviceDescriptor = new ServiceDescriptor(ITMConstants.ITM_SERVICE_NAME, serviceState);
        diagStatusService.report(serviceDescriptor);
    }

    public void reportStatus(Exception exception) {
        serviceDescriptor = new ServiceDescriptor(ITMConstants.ITM_SERVICE_NAME, exception);
        diagStatusService.report(serviceDescriptor);
    }

    @Override
    public ServiceDescriptor getServiceDescriptor() {
        // TODO Add logic here to derive the dynamic service state.
        // Currently this is just returning the initial state.
        return serviceDescriptor;
    }
}
