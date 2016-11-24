/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.renderer.ovs.utilities;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.utils.batching.ActionableResource;
import org.opendaylight.genius.utils.batching.ActionableResourceImpl;
import org.opendaylight.genius.utils.batching.ResourceBatchingManager;
import org.opendaylight.genius.utils.batching.ResourceHandler;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class BatchingUtils {
    private static final Logger LOG = LoggerFactory.getLogger(BatchingUtils.class);
    public static final int BATCH_SIZE = 1000;
    public static final int PERIODICITY = 500;
    public static Integer batchSize;
    public static Integer batchInterval;
    private static DataBroker dataBroker;
    private static BlockingQueue<ActionableResource> topologyConfigShardBufferQ;
    private static BlockingQueue<ActionableResource> defaultConfigShardBufferQ;

    public enum EntityType  {
        DEFAULT_CONFIG,
        TOPOLOGY_CONFIG
    }

    public static DataBroker getBroker() {
        return dataBroker;
    }

    public static void setBroker(DataBroker broker) {
        dataBroker = broker;
    }

    public static void registerWithBatchManager(ResourceHandler resourceHandler, DataBroker dataBroker) {
        BatchingUtils.setBroker(dataBroker);
        batchSize = 1000;
        if (Integer.getInteger("batch.size") != null) {
            batchSize = Integer.getInteger("batch.size");
        }
        batchInterval = 500;
        if (Integer.getInteger("batch.wait.time") != null) {
            batchInterval = Integer.getInteger("batch.wait.time");
        }
        ResourceBatchingManager resBatchingManager = ResourceBatchingManager.getInstance();
        resBatchingManager.registerBatchableResource("INTERFACEMGR-TOPOLOGY-CONFIG", topologyConfigShardBufferQ, resourceHandler);
        resBatchingManager.registerBatchableResource("INTERFACEMGR-DEFAULT-CONFIG", defaultConfigShardBufferQ, resourceHandler);
    }

    static <T extends DataObject> void update(InstanceIdentifier<T> path, T data, EntityType entityType) {
        ActionableResourceImpl actResource = new ActionableResourceImpl(path.toString());
        actResource.setAction(ActionableResource.UPDATE);
        actResource.setInstanceIdentifier(path);
        actResource.setInstance(data);
        getQueue(entityType).add(actResource);
    }

    public static <T extends DataObject> void write(InstanceIdentifier<T> path, T data, EntityType entityType) {
        ActionableResourceImpl actResource = new ActionableResourceImpl(path.toString());
        actResource.setAction(ActionableResource.CREATE);
        actResource.setInstanceIdentifier(path);
        actResource.setInstance(data);
        getQueue(entityType).add(actResource);
    }

    public static BlockingQueue<ActionableResource> getQueue(EntityType entityType){
       switch (entityType){
           case DEFAULT_CONFIG: return defaultConfigShardBufferQ;
           case TOPOLOGY_CONFIG:return topologyConfigShardBufferQ;
       }
       return null;
    }

    public static <T extends DataObject> void delete(InstanceIdentifier<T> path, EntityType entityType) {
        ActionableResourceImpl actResource = new ActionableResourceImpl(path.toString());
        actResource.setAction(ActionableResource.DELETE);
        actResource.setInstanceIdentifier(path);
        actResource.setInstance(null);
        getQueue(entityType).add(actResource);
    }

    static {
        topologyConfigShardBufferQ = new LinkedBlockingQueue<>();
        defaultConfigShardBufferQ = new LinkedBlockingQueue<>();
    }
}
