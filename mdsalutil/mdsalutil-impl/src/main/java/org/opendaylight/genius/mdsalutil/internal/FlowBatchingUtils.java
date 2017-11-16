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
import org.opendaylight.genius.utils.batching.ActionableResource;
import org.opendaylight.genius.utils.batching.ActionableResourceImpl;
import org.opendaylight.genius.utils.batching.ResourceBatchingManager;
import org.opendaylight.genius.utils.batching.ResourceHandler;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

class FlowBatchingUtils {
    private final BlockingQueue<ActionableResource> inventoryConfigShardBufferQ = new LinkedBlockingQueue<>();

    public void registerWithBatchManager(ResourceHandler resourceHandler) {
        ResourceBatchingManager resBatchingManager = ResourceBatchingManager.getInstance();
        resBatchingManager.registerBatchableResource("MDSALUTIL-INVENTORY-CONFIG", inventoryConfigShardBufferQ,
                resourceHandler);
    }

    <T extends DataObject> void update(InstanceIdentifier<T> path, T data) {
        ActionableResourceImpl actResource = new ActionableResourceImpl(path.toString());
        actResource.setAction(ActionableResource.UPDATE);
        actResource.setInstanceIdentifier(path);
        actResource.setInstance(data);
        inventoryConfigShardBufferQ.add(actResource);
    }

    <T extends DataObject> void write(InstanceIdentifier<T> path, T data) {
        ActionableResourceImpl actResource = new ActionableResourceImpl(path.toString());
        actResource.setAction(ActionableResource.CREATE);
        actResource.setInstanceIdentifier(path);
        actResource.setInstance(data);
        inventoryConfigShardBufferQ.add(actResource);
    }

    <T extends DataObject> void delete(InstanceIdentifier<T> path) {
        ActionableResourceImpl actResource = new ActionableResourceImpl(path.toString());
        actResource.setAction(ActionableResource.DELETE);
        actResource.setInstanceIdentifier(path);
        actResource.setInstance(null);
        inventoryConfigShardBufferQ.add(actResource);
    }
}
