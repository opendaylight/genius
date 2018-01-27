/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.impl;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.utils.batching.ActionableResource;
import org.opendaylight.genius.utils.batching.ActionableResourceImpl;
import org.opendaylight.genius.utils.batching.DefaultBatchHandler;
import org.opendaylight.genius.utils.batching.ResourceBatchingManager;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ITMBatchingUtils {
    private static final Logger LOG = LoggerFactory.getLogger((Class)ITMBatchingUtils.class);
    public static final int BATCH_SIZE = 1000;
    public static final int PERIODICITY = 500;
    public static Integer batchSize;
    public static Integer batchInterval;
    private static DataBroker dataBroker;
    private static BlockingQueue<ActionableResource> defaultOperationalShardBufferQ;
    private static BlockingQueue<ActionableResource> defaultConfigShardBufferQ;
    private static BlockingQueue<ActionableResource> topologyConfigShardBufferQ;

    // This could extend in future
    public enum EntityType  {
        DEFAULT_OPERATIONAL,
        DEFAULT_CONFIG,
        TOPOLOGY_CONFIG
    }

    private ITMBatchingUtils() { }

    public static DataBroker getBroker() {
        return dataBroker;
    }

    public static void setBroker(DataBroker broker) {
        dataBroker = broker;
    }

    public static void registerWithBatchManager(DataBroker broker) {
        ITMBatchingUtils.setBroker(broker);
        batchSize = 1000;
        if (Integer.getInteger("batch.size") != null) {
            batchSize = Integer.getInteger("batch.size");
        }
        batchInterval = 500;
        if (Integer.getInteger("batch.wait.time") != null) {
            batchInterval = Integer.getInteger("batch.wait.time");
        }
        ResourceBatchingManager resBatchingManager = ResourceBatchingManager.getInstance();
        resBatchingManager.registerBatchableResource("ITM-DEFAULT-OPERATIONAL", defaultOperationalShardBufferQ,
                new DefaultBatchHandler(broker, LogicalDatastoreType.OPERATIONAL, batchSize, batchInterval));
        resBatchingManager.registerBatchableResource("ITM-DEFAULT-CONFIG", defaultConfigShardBufferQ,
                new DefaultBatchHandler(broker, LogicalDatastoreType.CONFIGURATION, batchSize, batchInterval));
        resBatchingManager.registerBatchableResource("ITM-TOPOLOGY-CONFIG", topologyConfigShardBufferQ,
                new DefaultBatchHandler(broker, LogicalDatastoreType.CONFIGURATION, batchSize, batchInterval));
    }


    public static <T extends DataObject> void update(InstanceIdentifier<T> path, T data, EntityType entityType) {
        ActionableResourceImpl actResource = new ActionableResourceImpl(path.toString());
        actResource.setAction(ActionableResource.UPDATE);
        actResource.setInstanceIdentifier(path);
        actResource.setInstance(data);
        LOG.debug("Adding to the Queue to batch the update DS Operation - Id {} data {}", path, data);
        getQueue(entityType).add(actResource);
    }

    public static <T extends DataObject> void write(InstanceIdentifier<T> path, T data, EntityType entityType) {
        ActionableResourceImpl actResource = new ActionableResourceImpl(path.toString());
        actResource.setAction(ActionableResource.CREATE);
        actResource.setInstanceIdentifier(path);
        actResource.setInstance(data);
        LOG.debug("Adding to the Queue to batch the write DS Operation - Id {} data {}", path, data);
        getQueue(entityType).add(actResource);
    }

    public static BlockingQueue<ActionableResource> getQueue(EntityType entityType) {
        switch (entityType) {
            case DEFAULT_OPERATIONAL : return defaultOperationalShardBufferQ;
            case DEFAULT_CONFIG : return defaultConfigShardBufferQ;
            case TOPOLOGY_CONFIG: return topologyConfigShardBufferQ;
            default : {
                LOG.debug("entity type is neither operational or config, getQueue operation failed");
                return null;
            }
        }
    }

    public static <T extends DataObject> void delete(InstanceIdentifier<T> path, EntityType entityType) {
        ActionableResourceImpl actResource = new ActionableResourceImpl(path.toString());
        actResource.setAction(ActionableResource.DELETE);
        actResource.setInstanceIdentifier(path);
        actResource.setInstance(null);
        LOG.debug("Adding to the Queue to batch the delete DS Operation - Id {}", path);
        getQueue(entityType).add(actResource);
    }

    static {
        defaultOperationalShardBufferQ = new LinkedBlockingQueue<>();
        defaultConfigShardBufferQ = new LinkedBlockingQueue<>();
        topologyConfigShardBufferQ = new LinkedBlockingQueue<>();
    }
}
