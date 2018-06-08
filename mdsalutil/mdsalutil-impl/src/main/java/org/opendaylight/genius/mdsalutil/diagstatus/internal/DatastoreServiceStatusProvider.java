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
import javax.management.JMException;
import org.opendaylight.infrautils.diagstatus.DiagStatusService;
import org.opendaylight.infrautils.diagstatus.MBeanUtils;
import org.opendaylight.infrautils.diagstatus.ServiceDescriptor;
import org.opendaylight.infrautils.diagstatus.ServiceState;
import org.opendaylight.infrautils.diagstatus.ServiceStatusProvider;
import org.ops4j.pax.cdi.api.OsgiService;
import org.ops4j.pax.cdi.api.OsgiServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@OsgiServiceProvider(classes = ServiceStatusProvider.class)
public class DatastoreServiceStatusProvider implements ServiceStatusProvider {

    private static final Logger LOG = LoggerFactory.getLogger(DatastoreServiceStatusProvider.class);

    private static final String DATASTORE_SERVICE_NAME = "DATASTORE";

    private final DiagStatusService diagStatusService;

    @Inject
    public DatastoreServiceStatusProvider(@OsgiService DiagStatusService diagStatusService) {
        this.diagStatusService = diagStatusService;
        diagStatusService.register(DATASTORE_SERVICE_NAME);
        diagStatusService.report(getServiceDescriptor());
    }

    @PreDestroy
    public void close() {
        diagStatusService.report(new ServiceDescriptor(DATASTORE_SERVICE_NAME, ServiceState.UNREGISTERED,
                "Service Closed"));
    }

    @Override
    public ServiceDescriptor getServiceDescriptor() {
        ServiceState dataStoreServiceState;
        String statusDesc;
        try {
            Boolean operSyncStatusValue = (Boolean) MBeanUtils.getMBeanAttribute("org.opendaylight.controller:type="
                            + "DistributedOperationalDatastore,Category=ShardManager,name=shard-manager-operational",
                    "SyncStatus");
            Boolean configSyncStatusValue = (Boolean) MBeanUtils.getMBeanAttribute("org.opendaylight.controller:type="
                            + "DistributedConfigDatastore,Category=ShardManager,name=shard-manager-config",
                    "SyncStatus");
            if (operSyncStatusValue != null && configSyncStatusValue != null) {
                if (operSyncStatusValue && configSyncStatusValue) {
                    dataStoreServiceState = ServiceState.OPERATIONAL;
                    statusDesc = dataStoreServiceState.name();
                } else {
                    dataStoreServiceState = ServiceState.ERROR;
                    statusDesc = "datastore out of sync";
                }
            } else {
                dataStoreServiceState = ServiceState.ERROR;
                statusDesc = "Unable to obtain the datastore status (getMBeanAttribute returned null?!)";
            }
        } catch (JMException e) {
            LOG.error("Unable to obtain the datastore status due to JMException", e);
            dataStoreServiceState = ServiceState.ERROR;
            statusDesc = "Unable to obtain the datastore status: " + e.getMessage();
            // TODO use https://jira.opendaylight.org/browse/INFRAUTILS-31 here when available
            // to report the details of the root cause of the JMX problem to diagstatus consumers.
        }

        return new ServiceDescriptor(DATASTORE_SERVICE_NAME, dataStoreServiceState, statusDesc);
    }
}
