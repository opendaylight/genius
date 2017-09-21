/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.snd;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmProvider;
import org.opendaylight.infrautils.diagstatus.DiagStatusService;
import org.opendaylight.infrautils.diagstatus.ServiceDescriptor;
import org.opendaylight.infrautils.diagstatus.ServiceState;
import org.opendaylight.infrautils.diagstatus.ServiceStatusProvider;
import org.ops4j.pax.cdi.api.OsgiServiceProvider;

@Singleton
@OsgiServiceProvider(classes = ServiceStatusProvider.class)
public class ItmDiagStatusProvider implements ServiceStatusProvider {

    private final DiagStatusService diagStatusService;
    private volatile ServiceDescriptor serviceDescriptor;

    @Inject
    public ItmDiagStatusProvider(final ItmProvider itmProvider,
                                 final DiagStatusService diagStatusService) {
        this.diagStatusService = diagStatusService;
        diagStatusService.register(ITMConstants.ITM_SERVICE_NAME);
        serviceDescriptor = new ServiceDescriptor(ITMConstants.ITM_SERVICE_NAME, ServiceState.OPERATIONAL,
                "Service started");
        diagStatusService.report(serviceDescriptor);
    }

    @PreDestroy
    public void close() {
        serviceDescriptor = new ServiceDescriptor(ITMConstants.ITM_SERVICE_NAME, ServiceState.UNREGISTERED,
                "Service Closed");
        diagStatusService.report(serviceDescriptor);
    }

    @Override
    public ServiceDescriptor getServiceDescriptor() {
        // TODO Add logic here to derive the dynamic service state.
        // Currently this is just returning the initial state.
        return serviceDescriptor;
    }
}