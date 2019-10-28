/*
 * Copyright (c) 2017, 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.impl;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.utils.batching.ActionableResource;
import org.opendaylight.genius.utils.batching.ActionableResources;
import org.opendaylight.genius.utils.batching.DefaultBatchHandler;
import org.opendaylight.genius.utils.batching.ResourceBatchingManager;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ITMBatchingUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ITMBatchingUtils.class);


    private static final BlockingQueue<ActionableResource> DEFAULT_OPERATIONAL_SHARD_BUFFER_Q
            = new LinkedBlockingQueue<>();
    private static final BlockingQueue<ActionableResource> DEFAULT_CONFIG_SHARD_BUFFER_Q = new LinkedBlockingQueue<>();
    private static final BlockingQueue<ActionableResource> TOPOLOGY_CONFIG_SHARD_BUFFER_Q = new LinkedBlockingQueue<>();

    private static DataBroker dataBroker;

    // This could extend in future
    public enum EntityType  {
        DEFAULT_OPERATIONAL,
        DEFAULT_CONFIG,
        TOPOLOGY_CONFIG
    }

    private ITMBatchingUtils() {
    }

    public static DataBroker getBroker() {
        return dataBroker;
    }

    public static void setBroker(DataBroker broker) {
        dataBroker = broker;
    }

    public static void registerWithBatchManager(DataBroker broker,Integer batchSize,Integer batchInterval) {
        ITMBatchingUtils.setBroker(broker);
        ResourceBatchingManager resBatchingManager = ResourceBatchingManager.getInstance();
        resBatchingManager.registerBatchableResource("ITM-DEFAULT-OPERATIONAL", DEFAULT_OPERATIONAL_SHARD_BUFFER_Q,
                                                     new DefaultBatchHandler(broker, LogicalDatastoreType.OPERATIONAL,
                                                                             batchSize, batchInterval));
        resBatchingManager.registerBatchableResource("ITM-DEFAULT-CONFIG", DEFAULT_CONFIG_SHARD_BUFFER_Q,
                                                     new DefaultBatchHandler(broker, LogicalDatastoreType.CONFIGURATION,
                                                                             batchSize, batchInterval));
        resBatchingManager.registerBatchableResource("ITM-TOPOLOGY-CONFIG", TOPOLOGY_CONFIG_SHARD_BUFFER_Q,
                                                     new DefaultBatchHandler(broker, LogicalDatastoreType.CONFIGURATION,
                                                                             batchSize, batchInterval));
    }


    public static <T extends DataObject> void update(InstanceIdentifier<T> path, T data, EntityType entityType) {
        LOG.debug("Adding to the Queue to batch the update DS Operation - Id {} data {}", path, data);
        getQueue(entityType).add(ActionableResources.update(path, data));
    }

    public static <T extends DataObject> void write(InstanceIdentifier<T> path, T data, EntityType entityType) {
        LOG.debug("Adding to the Queue to batch the write DS Operation - Id {} data {}", path, data);
        getQueue(entityType).add(ActionableResources.create(path, data));
    }

    @NonNull
    public static BlockingQueue<ActionableResource> getQueue(EntityType entityType) {
        switch (entityType) {
            case DEFAULT_OPERATIONAL:
                return DEFAULT_OPERATIONAL_SHARD_BUFFER_Q;
            case DEFAULT_CONFIG:
                return DEFAULT_CONFIG_SHARD_BUFFER_Q;
            case TOPOLOGY_CONFIG:
                return TOPOLOGY_CONFIG_SHARD_BUFFER_Q;
            default:
                throw new IllegalArgumentException(
                    "Entity type " + entityType + " is neither operational or config, getQueue operation failed");
        }
    }

    public static <T extends DataObject> void delete(InstanceIdentifier<T> path, EntityType entityType) {
        LOG.debug("Adding to the Queue to batch the delete DS Operation - Id {}", path);
        getQueue(entityType).add(ActionableResources.delete(path));
    }
}
