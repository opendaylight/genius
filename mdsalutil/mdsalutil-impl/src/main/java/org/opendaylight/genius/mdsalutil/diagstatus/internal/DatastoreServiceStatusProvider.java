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
import org.opendaylight.controller.cluster.datastore.shardmanager.ShardManagerInfoMBean;
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
    @SuppressWarnings("checkstyle:IllegalCatch")
    public ServiceDescriptor getServiceDescriptor() {
        try {
            ShardManagerInfoMBean operationalShardManagerInfo = MBeanUtils.getMBean("org.opendaylight.controller:type="
                    + "DistributedOperationalDatastore,Category=ShardManager,name=shard-manager-operational",
                    ShardManagerInfoMBean.class);
            boolean operSyncStatusValue = operationalShardManagerInfo.getSyncStatus();

            ShardManagerInfoMBean configShardManagerInfo = MBeanUtils.getMBean("org.opendaylight.controller:type="
                    + "DistributedConfigDatastore,Category=ShardManager,name=shard-manager-config",
                    ShardManagerInfoMBean.class);
            boolean configSyncStatusValue = configShardManagerInfo.getSyncStatus();

            String statusDesc;
            ServiceState dataStoreServiceState;
            if (operSyncStatusValue && configSyncStatusValue) {
                dataStoreServiceState = ServiceState.OPERATIONAL;
                statusDesc = dataStoreServiceState.name();
            } else {
                dataStoreServiceState = ServiceState.ERROR;
                statusDesc = "datastore out of sync";
            }
            return new ServiceDescriptor(DATASTORE_SERVICE_NAME, dataStoreServiceState, statusDesc);

        } catch (Throwable e) { // not just JMException, but anything that could go wrong
            LOG.error("Unable to obtain the datastore status", e);
            return new ServiceDescriptor(DATASTORE_SERVICE_NAME, e);
        }
    }
}
