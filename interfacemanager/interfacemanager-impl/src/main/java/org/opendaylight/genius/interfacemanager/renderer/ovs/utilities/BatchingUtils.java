/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.renderer.ovs.utilities;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.aries.blueprint.annotation.service.Reference;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.utils.batching.ActionableResource;
import org.opendaylight.genius.utils.batching.ActionableResourceImpl;
import org.opendaylight.genius.utils.batching.ResourceBatchingManager;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@Singleton
public class BatchingUtils implements AutoCloseable {
    public enum EntityType {
        DEFAULT_CONFIG,
        DEFAULT_OPERATIONAL,
        TOPOLOGY_CONFIG
    }

    private static final String DEFAULT_OPERATIONAL_RES_TYPE = "INTERFACEMGR-DEFAULT-OPERATIONAL";
    private static final String DEFAULT_CONFIG_RES_TYPE = "INTERFACEMGR-DEFAULT-CONFIG";
    private static final String TOPOLOGY_CONFIG_RES_TYPE = "INTERFACEMGR-TOPOLOGY-CONFIG";

    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final int DEFAULT_BATCH_INTERVAL = 500;

    private final BlockingQueue<ActionableResource> topologyConfigShardBufferQ = new LinkedBlockingQueue<>();
    private final BlockingQueue<ActionableResource> defaultConfigShardBufferQ = new LinkedBlockingQueue<>();
    private final BlockingQueue<ActionableResource> defaultOperationalShardBufferQ = new LinkedBlockingQueue<>();

    private final DataBroker dataBroker;
    private final ResourceBatchingManager resourceBatchingManager = ResourceBatchingManager.getInstance();

    @Inject
    public BatchingUtils(@Reference DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    @PostConstruct
    public void init() {
        int batchSize = Integer.getInteger("batch.size", DEFAULT_BATCH_SIZE);
        int batchInterval = Integer.getInteger("batch.wait.time", DEFAULT_BATCH_INTERVAL);

        resourceBatchingManager.registerBatchableResource(TOPOLOGY_CONFIG_RES_TYPE, topologyConfigShardBufferQ,
                new InterfaceBatchHandler(dataBroker, LogicalDatastoreType.CONFIGURATION, batchSize, batchInterval));
        resourceBatchingManager.registerBatchableResource(DEFAULT_CONFIG_RES_TYPE, defaultConfigShardBufferQ,
                new InterfaceBatchHandler(dataBroker, LogicalDatastoreType.CONFIGURATION, batchSize, batchInterval));
        resourceBatchingManager.registerBatchableResource(DEFAULT_OPERATIONAL_RES_TYPE, defaultOperationalShardBufferQ,
                new InterfaceBatchHandler(dataBroker, LogicalDatastoreType.OPERATIONAL, batchSize, batchInterval));
    }

    @Override
    @PreDestroy
    public void close() {
        resourceBatchingManager.deregisterBatchableResource(TOPOLOGY_CONFIG_RES_TYPE);
        resourceBatchingManager.deregisterBatchableResource(DEFAULT_CONFIG_RES_TYPE);
        resourceBatchingManager.deregisterBatchableResource(DEFAULT_OPERATIONAL_RES_TYPE);
    }

    <T extends DataObject> void update(InstanceIdentifier<T> path, T data, EntityType entityType) {
        ActionableResourceImpl actResource = new ActionableResourceImpl(path.toString());
        actResource.setAction(ActionableResource.UPDATE);
        actResource.setInstanceIdentifier(path);
        actResource.setInstance(data);
        getQueue(entityType).add(actResource);
    }

    public <T extends DataObject> void write(InstanceIdentifier<T> path, T data, EntityType entityType) {
        ActionableResourceImpl actResource = new ActionableResourceImpl(path.toString());
        actResource.setAction(ActionableResource.CREATE);
        actResource.setInstanceIdentifier(path);
        actResource.setInstance(data);
        getQueue(entityType).add(actResource);
    }

    public BlockingQueue<ActionableResource> getQueue(EntityType entityType) {
        switch (entityType) {
            case DEFAULT_CONFIG:
                return defaultConfigShardBufferQ;
            case TOPOLOGY_CONFIG:
                return topologyConfigShardBufferQ;
            case DEFAULT_OPERATIONAL:
                return defaultOperationalShardBufferQ;
            default:
                return null;
        }
    }

    public <T extends DataObject> void delete(InstanceIdentifier<T> path, EntityType entityType) {
        ActionableResourceImpl actResource = new ActionableResourceImpl(path.toString());
        actResource.setAction(ActionableResource.DELETE);
        actResource.setInstanceIdentifier(path);
        actResource.setInstance(null);
        getQueue(entityType).add(actResource);
    }
}
