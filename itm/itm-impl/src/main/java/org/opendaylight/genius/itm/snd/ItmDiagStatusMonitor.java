/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.snd;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmProvider;
import org.opendaylight.infrautils.diagstatus.ServiceDescriptor;
import org.opendaylight.infrautils.diagstatus.ServiceState;
import org.opendaylight.infrautils.diagstatus.ServiceStatusProvider;
import org.ops4j.pax.cdi.api.OsgiServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@OsgiServiceProvider(classes = ServiceStatusProvider.class)
public class ItmDiagStatusMonitor implements ServiceStatusProvider {
    private static final Logger LOG = LoggerFactory.getLogger(ItmDiagStatusMonitor.class);
    private volatile ServiceDescriptor serviceDescriptor;

    @Inject
    public ItmDiagStatusMonitor(final ItmProvider itmProvider) {
        serviceDescriptor = new ServiceDescriptor(ITMConstants.ITM_SERVICE_NAME, ServiceState.OPERATIONAL,
                "Service started");
    }

    @PreDestroy
    public void close() {
        serviceDescriptor = new ServiceDescriptor(ITMConstants.ITM_SERVICE_NAME, ServiceState.UNREGISTERED,
                "Service Closed");
    }

    @Override
    public ServiceDescriptor getServiceDescriptor() {
        // TODO Add logic here to derive the dynamic service state.
        // Currently this is just returning the initial state.
        LOG.debug("Retrieving service status {}", serviceDescriptor);
        return serviceDescriptor;
    }
}