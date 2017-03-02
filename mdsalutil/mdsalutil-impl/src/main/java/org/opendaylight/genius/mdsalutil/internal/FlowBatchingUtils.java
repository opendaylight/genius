/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.internal;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.utils.batching.ActionableResource;
import org.opendaylight.genius.utils.batching.ActionableResourceImpl;
import org.opendaylight.genius.utils.batching.ResourceBatchingManager;
import org.opendaylight.genius.utils.batching.ResourceHandler;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowBatchingUtils {
    private static final Logger LOG = LoggerFactory.getLogger(FlowBatchingUtils.class);
    public static final int BATCH_SIZE = 1000;
    public static final int PERIODICITY = 500;
    public static Integer batchSize;
    public static Integer batchInterval;
    private static DataBroker dataBroker;
    private static BlockingQueue<ActionableResource> inventoryConfigShardBufferQ;

    public static DataBroker getBroker() {
        return dataBroker;
    }

    public static void setBroker(DataBroker broker) {
        dataBroker = broker;
    }

    public static void registerWithBatchManager(ResourceHandler resourceHandler, DataBroker dataBroker) {
        FlowBatchingUtils.setBroker(dataBroker);
        batchSize = 1000;
        if (Integer.getInteger("batch.size") != null) {
            batchSize = Integer.getInteger("batch.size");
        }
        batchInterval = 500;
        if (Integer.getInteger("batch.wait.time") != null) {
            batchInterval = Integer.getInteger("batch.wait.time");
        }
        ResourceBatchingManager resBatchingManager = ResourceBatchingManager.getInstance();
        resBatchingManager.registerBatchableResource("MDSALUTIL-INVENTORY-CONFIG", inventoryConfigShardBufferQ,
                resourceHandler);
    }

    static <T extends DataObject> void update(InstanceIdentifier<T> path, T data) {
        ActionableResourceImpl actResource = new ActionableResourceImpl(path.toString());
        actResource.setAction(ActionableResource.UPDATE);
        actResource.setInstanceIdentifier(path);
        actResource.setInstance(data);
        inventoryConfigShardBufferQ.add(actResource);
    }

    public static <T extends DataObject> void write(InstanceIdentifier<T> path, T data) {
        ActionableResourceImpl actResource = new ActionableResourceImpl(path.toString());
        actResource.setAction(ActionableResource.CREATE);
        actResource.setInstanceIdentifier(path);
        actResource.setInstance(data);
        inventoryConfigShardBufferQ.add(actResource);
    }

    static <T extends DataObject> void delete(InstanceIdentifier<T> path) {
        ActionableResourceImpl actResource = new ActionableResourceImpl(path.toString());
        actResource.setAction(ActionableResource.DELETE);
        actResource.setInstanceIdentifier(path);
        actResource.setInstance(null);
        inventoryConfigShardBufferQ.add(actResource);
    }

    static {
        inventoryConfigShardBufferQ = new LinkedBlockingQueue<>();
    }
}
