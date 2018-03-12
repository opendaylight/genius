/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.diagstatus.internal;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.opendaylight.infrautils.diagstatus.DiagStatusService;
import org.opendaylight.infrautils.diagstatus.MBeanUtils;
import org.opendaylight.infrautils.diagstatus.ServiceDescriptor;
import org.opendaylight.infrautils.diagstatus.ServiceState;
import org.opendaylight.infrautils.diagstatus.ServiceStatusProvider;
import org.ops4j.pax.cdi.api.OsgiService;
import org.ops4j.pax.cdi.api.OsgiServiceProvider;

@Singleton
@OsgiServiceProvider(classes = ServiceStatusProvider.class)
public class DatastoreServiceStatusProvider implements ServiceStatusProvider {

    private static final String DATASTORE_SERVICE_NAME = "DATASTORE";
    private volatile ServiceDescriptor serviceDescriptor;
    private DiagStatusService diagStatusService;

    @Inject
    public DatastoreServiceStatusProvider(@OsgiService DiagStatusService diagStatusService) {
        this.diagStatusService = diagStatusService;
        diagStatusService.register(DATASTORE_SERVICE_NAME);
        serviceDescriptor = new ServiceDescriptor(DATASTORE_SERVICE_NAME, ServiceState.OPERATIONAL,
                "Service started");
        diagStatusService.report(serviceDescriptor);
    }

    @PreDestroy
    public void close() {
        serviceDescriptor = new ServiceDescriptor(DATASTORE_SERVICE_NAME, ServiceState.UNREGISTERED,
                "Service Closed");
        diagStatusService.report(serviceDescriptor);
    }

    @Override
    public ServiceDescriptor getServiceDescriptor() {
        ServiceState dataStoreServiceState = ServiceState.ERROR;
        String statusDesc;
        Boolean operSyncStatusValue = (Boolean) MBeanUtils.readMBeanAttribute("org.opendaylight.controller:type="
                        + "DistributedOperationalDatastore,Category=ShardManager,name=shard-manager-operational",
                "SyncStatus");
        Boolean configSyncStatusValue = (Boolean) MBeanUtils.readMBeanAttribute("org.opendaylight.controller:type="
                        + "DistributedConfigDatastore,Category=ShardManager,name=shard-manager-config",
                "SyncStatus");
        if (operSyncStatusValue != null && configSyncStatusValue != null) {
            if (operSyncStatusValue && configSyncStatusValue) {
                dataStoreServiceState =  ServiceState.OPERATIONAL;
                statusDesc = dataStoreServiceState.name();
            } else {
                statusDesc = "datastore out of sync";
            }
        } else {
            statusDesc = "Unable to obtain the datastore status";
        }
        return new ServiceDescriptor(DATASTORE_SERVICE_NAME, dataStoreServiceState, statusDesc);
    }
}