/*
 * Copyright (c) 2015 - 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.utils.batching;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ResourceBatchingManager implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ResourceBatchingManager.class);
    private static final int INITIAL_DELAY = 3000;
    private static final TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;

    private DataBroker broker;
    private ConcurrentHashMap<String, Pair<BlockingQueue, ResourceHandler>> resourceHandlerMapper = new ConcurrentHashMap();
    private ConcurrentHashMap<String, ScheduledThreadPoolExecutor> resourceBatchingThreadMapper = new ConcurrentHashMap();

    private static ResourceBatchingManager instance;

    static {
        instance = new ResourceBatchingManager();
    }

    public static ResourceBatchingManager getInstance() {
        return instance;
    }

    @Override
    public void close() throws Exception {
        LOG.trace("ResourceBatchingManager Closed");
    }

    public void registerBatchableResource(String resourceType, final BlockingQueue<ActionableResource> resQueue, final ResourceHandler resHandler) {
        Preconditions.checkNotNull(resQueue, "ResourceQueue to use for batching cannot not be null.");
        Preconditions.checkNotNull(resHandler, "ResourceHandler cannot not be null.");
        if (resourceHandlerMapper.contains(resourceType)) {
            throw new RuntimeException("Resource type already registered");
        }
        resourceHandlerMapper.put(resourceType, new ImmutablePair<>(resQueue, resHandler));
        ScheduledThreadPoolExecutor resDelegatorService =(ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);
        resourceBatchingThreadMapper.put(resourceType, resDelegatorService);
        LOG.info("Registered resourceType {} with batchSize {} and batchInterval {}", resourceType,
                resHandler.getBatchSize(), resHandler.getBatchInterval());
        if (resDelegatorService.getPoolSize() == 0 )
            resDelegatorService.scheduleWithFixedDelay(new Batcher(resourceType), INITIAL_DELAY, resHandler.getBatchInterval(), TIME_UNIT);
    }

    public void put(String resourceType, InstanceIdentifier identifier, DataObject updatedData) {
        BlockingQueue<ActionableResource> queue = getQueue(resourceType);
        if (queue != null) {
            ActionableResource actResource = new ActionableResourceImpl(identifier.toString(),
                    identifier, ActionableResource.CREATE, updatedData, null/*oldData*/);
            queue.add(actResource);
        }
    }

    public void merge(String resourceType, InstanceIdentifier identifier, DataObject updatedData) {
        BlockingQueue<ActionableResource> queue = getQueue(resourceType);
        if (queue != null) {
            ActionableResource actResource = new ActionableResourceImpl(identifier.toString(),
                    identifier, ActionableResource.UPDATE, updatedData, null/*oldData*/);
            queue.add(actResource);
        }
    }

    public void delete(String resourceType, InstanceIdentifier identifier) {
        BlockingQueue<ActionableResource> queue = getQueue(resourceType);
        if (queue != null) {
            ActionableResource actResource = new ActionableResourceImpl(identifier.toString(),
                    identifier, ActionableResource.DELETE, null, null/*oldData*/);
            queue.add(actResource);
        }
    }

    private BlockingQueue<ActionableResource> getQueue(String resourceType) {
        if (resourceHandlerMapper.containsKey(resourceType)) {
            return resourceHandlerMapper.get(resourceType).getLeft();
        }
        return null;
    }


    public void deregisterBatchableResource(String resourceType) {
        resourceHandlerMapper.remove(resourceType);
        resourceBatchingThreadMapper.remove(resourceType);
    }

    private class Batcher implements Runnable
    {
        private String resourceType;

        Batcher(String resourceType) {
            this.resourceType = resourceType;
        }

        public void run()
        {
            List<ActionableResource> resList = new ArrayList<>();

            try
            {
                Pair<BlockingQueue, ResourceHandler> resMapper = resourceHandlerMapper.get(resourceType);
                if (resMapper == null) {
                    LOG.error("Unable to find resourceMapper for batching the ResourceType {}", resourceType);
                    return;
                }
                BlockingQueue<ActionableResource> resQueue = resMapper.getLeft();
                ResourceHandler resHandler = resMapper.getRight();
                resList.add(resQueue.take());
                resQueue.drainTo(resList);

                long start = System.currentTimeMillis();
                int batchSize = resHandler.getBatchSize();

                int batches = resList.size()/ batchSize;
                if ( resList.size() > batchSize)
                {
                    LOG.info("Batched up resources of size {} into batches {} for resourcetype {}", resList.size(), batches, resourceType);
                    for (int i = 0, j = 0; i < batches; j = j + batchSize,i++)
                    {
                        new MdsalDsTask<>(resourceType, resList.subList(j, j + batchSize)).process();
                    }
                    // process remaining routes
                    LOG.trace("Picked up 1 size {} ", resList.subList(batches * batchSize, resList.size()).size());
                    new MdsalDsTask<>(resourceType, resList.subList(batches * batchSize, resList.size())).process();
                } else {
                    // process less than OR == batchsize routes
                    LOG.trace("Picked up 2 size {}", resList.size());
                    new MdsalDsTask<>(resourceType, resList).process();
                }

                long timetaken = System.currentTimeMillis() - start;
                LOG.info( "Total taken ##time = {}ms for resourceList of size {} for resourceType {}", timetaken, resList.size(), resourceType);

            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }

        }
    }

    private class MdsalDsTask<T extends DataObject>
    {
        String resourceType;
        List<ActionableResource> actResourceList;

        public MdsalDsTask(String resourceType, List<ActionableResource> actResourceList)
        {
            this.resourceType = resourceType;
            this.actResourceList = actResourceList;
        }

        public void process() {
            InstanceIdentifier<T> identifier;
            Object instance;
            try {
                LOG.trace("Picked up 3 size {} of resourceType {}", actResourceList.size(), resourceType);
                Pair<BlockingQueue, ResourceHandler> resMapper = resourceHandlerMapper.get(resourceType);
                if (resMapper == null) {
                    LOG.error("Unable to find resourceMapper for batching the ResourceType {}", resourceType);
                    return;
                }
                ResourceHandler resHandler = resMapper.getRight();
                DataBroker broker = resHandler.getResourceBroker();
                LogicalDatastoreType dsType = resHandler.getDatastoreType();
                WriteTransaction tx = broker.newWriteOnlyTransaction();
                List<SubTransaction> transactionObjects = new ArrayList<>();
                for (ActionableResource actResource : actResourceList)
                {
                    switch (actResource.getAction()) {
                        case ActionableResource.CREATE:
                            identifier = actResource.getInstanceIdentifier();
                            instance = actResource.getInstance();
                            resHandler.create(tx, dsType, identifier, instance,transactionObjects);
                            break;
                        case ActionableResource.UPDATE:
                            identifier = actResource.getInstanceIdentifier();
                            Object updated = actResource.getInstance();
                            Object original = actResource.getOldInstance();
                            resHandler.update(tx, dsType, identifier, original, updated,transactionObjects);
                            break;
                        case ActionableResource.DELETE:
                            identifier = actResource.getInstanceIdentifier();
                            instance = actResource.getInstance();
                            resHandler.delete(tx, dsType, identifier, instance,transactionObjects);
                            break;
                        default:
                            LOG.error("Unable to determine Action for ResourceType {} with ResourceKey {}", resourceType,
                                    actResource.getKey());
                    }
                }

                long start = System.currentTimeMillis();
                CheckedFuture<Void, TransactionCommitFailedException> futures = tx.submit();

                try
                {
                    futures.get();
                    long time = System.currentTimeMillis() - start;

                    LOG.trace( " ##### Time taken for " + actResourceList.size() + " = " + time + "ms");

                } catch (InterruptedException | ExecutionException e)
                {
                    LOG.error("Exception occurred while batch writing to datastore {} ", e);
                    LOG.info("Trying to submit transaction operations one at a time for resType {}", resourceType);
                    for (SubTransaction object : transactionObjects) {
                        WriteTransaction writeTransaction = broker.newWriteOnlyTransaction();
                        switch (object.getAction()) {
                            case SubTransaction.CREATE :
                                writeTransaction.put(dsType, object.getInstanceIdentifier(), (DataObject)object.getInstance(), true);
                                break;
                            case SubTransaction.DELETE :
                                writeTransaction.delete(dsType, object.getInstanceIdentifier());
                                break;
                            case SubTransaction.UPDATE :
                                writeTransaction.merge(dsType, object.getInstanceIdentifier(), (DataObject)object.getInstance(), true);
                                break;
                            default:
                                LOG.error("Unable to determine Action for transaction object with id {}", object.getInstanceIdentifier());
                        }
                        CheckedFuture<Void, TransactionCommitFailedException> futureOperation = writeTransaction.submit();
                        try {
                            futureOperation.get();
                        } catch (InterruptedException | ExecutionException exception) {
                            LOG.error("Error {} to datastore (path, data) : ({}, {})", object.getAction(), object.getInstanceIdentifier(), object.getInstance());
                            LOG.error(exception.getMessage());
                        }
                    }
                }

            } catch (final Exception e)
            {
                LOG.error("Transaction submission failed: ", e);
            }
        }
    }
}
