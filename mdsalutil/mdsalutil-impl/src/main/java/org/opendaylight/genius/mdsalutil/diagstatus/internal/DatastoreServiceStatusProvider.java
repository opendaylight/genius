/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.diagstatus.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.management.MalformedObjectNameException;
import org.apache.aries.blueprint.annotation.service.Reference;
import org.apache.aries.blueprint.annotation.service.Service;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard.ShardStatsMXBean;
import org.opendaylight.controller.cluster.datastore.shardmanager.ShardManagerInfoMBean;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.infrautils.diagstatus.DiagStatusService;
import org.opendaylight.infrautils.diagstatus.MBeanUtils;
import org.opendaylight.infrautils.diagstatus.ServiceDescriptor;
import org.opendaylight.infrautils.diagstatus.ServiceState;
import org.opendaylight.infrautils.diagstatus.ServiceStatusProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Service(classes = ServiceStatusProvider.class)
public class DatastoreServiceStatusProvider implements ServiceStatusProvider {

    private static final Logger LOG = LoggerFactory.getLogger(DatastoreServiceStatusProvider.class);

    private static final String DATASTORE_SERVICE_NAME = "DATASTORE";

    private final DiagStatusService diagStatusService;
    private final ShardManagerInfoMBean operationalShardManagerInfo;
    private final ShardManagerInfoMBean configShardManagerInfo;
    private final List<ShardStatsMXBean> allShardStats;

    @Inject
    public DatastoreServiceStatusProvider(@Reference DiagStatusService diagStatusService,
            @Reference DataBroker dataBroker) throws MalformedObjectNameException {
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
        LOG.info("Watching SyncStatus in oper and config ShardManagerInfoMBean");

        allShardStats = new ArrayList<>(
                operationalShardManagerInfo.getLocalShards().size() + configShardManagerInfo.getLocalShards().size());
        for (String operationalShardName : operationalShardManagerInfo.getLocalShards()) {
            allShardStats.add(MBeanUtils.getMBean("org.opendaylight.controller:type=DistributedOperationalDatastore,"
                    + "Category=Shards,name=" + operationalShardName, ShardStatsMXBean.class));
        }
        for (String configShardName : configShardManagerInfo.getLocalShards()) {
            allShardStats.add(MBeanUtils.getMBean("org.opendaylight.controller:type=DistributedConfigDatastore,"
                    + "Category=Shards,name=" + configShardName, ShardStatsMXBean.class));
        }
        LOG.info("Watching RaftState in {}x ShardStatsMXBean", allShardStats.size());

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
            final ServiceState dataStoreServiceState;
            final StringBuilder statusDescBuilder = new StringBuilder();
            if (operationalShardManagerInfo.getSyncStatus() && configShardManagerInfo.getSyncStatus()) {
                for (ShardStatsMXBean shardStats : allShardStats) {
                    String raftState = shardStats.getRaftState();
                    if (!("Leader".equals(raftState) || "Follower".equals(raftState))) {
                        if (statusDescBuilder.length() == 0) {
                            statusDescBuilder.append("Some Shard(s) are not Leader or Follower: ");
                        } else {
                            statusDescBuilder.append(", ");
                        }
                        statusDescBuilder.append(shardStats.getShardName());
                        statusDescBuilder.append(':');
                        statusDescBuilder.append(raftState);
                    }
                }
                if (statusDescBuilder.length() > 0) {
                    dataStoreServiceState = ServiceState.ERROR;
                } else {
                    dataStoreServiceState = ServiceState.OPERATIONAL;
                }
            } else {
                dataStoreServiceState = ServiceState.ERROR;
                statusDescBuilder.append("Data store out of sync");
            }
            return new ServiceDescriptor(DATASTORE_SERVICE_NAME, dataStoreServiceState, statusDescBuilder.toString());

        } catch (Throwable e) { // not just JMException, but anything that could go wrong
            LOG.error("Unable to obtain the datastore status", e);
            return new ServiceDescriptor(DATASTORE_SERVICE_NAME, e);
        }
    }
}
