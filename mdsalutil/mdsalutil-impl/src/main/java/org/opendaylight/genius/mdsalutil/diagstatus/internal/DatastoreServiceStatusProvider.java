/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.diagstatus.internal;

import java.util.Objects;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.management.MalformedObjectNameException;
import org.opendaylight.controller.cluster.datastore.shardmanager.ShardManagerInfoMBean;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
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
    private final ShardManagerInfoMBean operationalShardManagerInfo;
    private final ShardManagerInfoMBean configShardManagerInfo;

    @Inject
    public DatastoreServiceStatusProvider(@OsgiService DiagStatusService diagStatusService,
            @OsgiService DataBroker dataBroker) throws MalformedObjectNameException {
        this.diagStatusService = diagStatusService;
        diagStatusService.register(DATASTORE_SERVICE_NAME);

        // We don't really require a DataBroker, but we do this to enforce a dependency
        // which will make sure that this happens after the datastore is up and running.
        Objects.requireNonNull(dataBroker);
        operationalShardManagerInfo = MBeanUtils.getMBean("org.opendaylight.controller:type="
                + "DistributedOperationalDatastore,Category=ShardManager,name=shard-manager-operational",
                ShardManagerInfoMBean.class);
        configShardManagerInfo = MBeanUtils.getMBean("org.opendaylight.controller:type="
                + "DistributedConfigDatastore,Category=ShardManager,name=shard-manager-config",
                ShardManagerInfoMBean.class);

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
            boolean operSyncStatusValue = operationalShardManagerInfo.getSyncStatus();
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
