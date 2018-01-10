/*
 * Copyright (c) 2015 - 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.utils.batching;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.infrautils.utils.concurrent.ThreadFactoryProvider;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class lets other modules submit their CRUD methods to it. This class
 * will then supply a single transaction to such CRUD methods of the
 * subscribers, on which such subscribers write data to that transaction.
 * Finally the framework attempts to reliably write this single transaction
 * which represents a batch of an ordered list of entities owned by that subscriber,
 * to be written/updated/removed from a specific datastore as registered by the subscriber.
 */
public class ResourceBatchingManager implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceBatchingManager.class);

    private static final int INITIAL_DELAY = 3000;
    private static final TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;

    private static final int PERIODICITY_IN_MS = 500;
    private static final int BATCH_SIZE = 1000;

    public enum ShardResource {
        CONFIG_TOPOLOGY(LogicalDatastoreType.CONFIGURATION),
        OPERATIONAL_TOPOLOGY(LogicalDatastoreType.OPERATIONAL),
        CONFIG_INVENTORY(LogicalDatastoreType.CONFIGURATION),
        OPERATIONAL_INVENTORY(LogicalDatastoreType.OPERATIONAL);

        BlockingQueue<ActionableResource> queue = new LinkedBlockingQueue<>();
        LogicalDatastoreType datastoreType;

        ShardResource(LogicalDatastoreType datastoreType) {
            this.datastoreType = datastoreType;
        }

        public LogicalDatastoreType getDatastoreType() {
            return datastoreType;
        }

        BlockingQueue<ActionableResource> getQueue() {
            return queue;
        }
    }

    private final ConcurrentHashMap<String, Pair<BlockingQueue<ActionableResource>, ResourceHandler>>
            resourceHandlerMapper = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, ScheduledThreadPoolExecutor>
            resourceBatchingThreadMapper = new ConcurrentHashMap<>();

    private final Map<String, Set<InstanceIdentifier>> toBeDeletedByResourceType = new ConcurrentHashMap<>();
    private final Map<String, Map<InstanceIdentifier, DataObject>> toBeAddedByResourceType = new ConcurrentHashMap<>();

    private static ResourceBatchingManager instance;

    static {
        instance = new ResourceBatchingManager();
    }

    public static ResourceBatchingManager getInstance() {
        return instance;
    }

    @Override
    public void close() {
        LOG.trace("ResourceBatchingManager Closed, closing all batched resources");
        resourceBatchingThreadMapper.values().forEach(ScheduledThreadPoolExecutor::shutdown);
    }

    public void registerBatchableResource(
            String resourceType, final BlockingQueue<ActionableResource> resQueue, final ResourceHandler resHandler) {
        Preconditions.checkNotNull(resQueue, "ResourceQueue to use for batching cannot not be null.");
        Preconditions.checkNotNull(resHandler, "ResourceHandler cannot not be null.");

        resourceHandlerMapper.put(resourceType, new ImmutablePair<>(resQueue, resHandler));
        ScheduledThreadPoolExecutor resDelegatorService = (ScheduledThreadPoolExecutor)
                Executors.newScheduledThreadPool(1, ThreadFactoryProvider.builder()
                        .namePrefix("ResourceBatchingManager").logger(LOG).build().get());
        resourceBatchingThreadMapper.put(resourceType, resDelegatorService);
        LOG.info("Registered resourceType {} with batchSize {} and batchInterval {}", resourceType,
                resHandler.getBatchSize(), resHandler.getBatchInterval());
        if (resDelegatorService.getPoolSize() == 0) {
            resDelegatorService.scheduleWithFixedDelay(
                    new Batcher(resourceType), INITIAL_DELAY, resHandler.getBatchInterval(), TIME_UNIT);
        }
        toBeDeletedByResourceType.putIfAbsent(resourceType, ConcurrentHashMap.newKeySet());
        toBeAddedByResourceType.putIfAbsent(resourceType, new ConcurrentHashMap<>());
    }

    public void registerDefaultBatchHandlers(DataBroker broker) {
        LOG.trace("Registering default batch handlers");
        Integer batchSize = Integer.getInteger("resource.manager.batch.size", BATCH_SIZE);
        Integer batchInterval = Integer.getInteger("resource.manager.batch.periodicity.ms", PERIODICITY_IN_MS);

        for (ShardResource shardResource : ShardResource.values()) {
            if (resourceHandlerMapper.containsKey(shardResource.name())) {
                continue;
            }
            DefaultBatchHandler batchHandler = new DefaultBatchHandler(broker, shardResource.datastoreType, batchSize,
                    batchInterval);
            registerBatchableResource(shardResource.name(), shardResource.getQueue(), batchHandler);
        }
    }

    private void prePut(String resoureType, InstanceIdentifier<?> iid, DataObject data) {
        toBeDeletedByResourceType.get(resoureType).remove(iid);
        toBeAddedByResourceType.get(resoureType).put(iid, data);
    }

    private void preDelete(String resoureType, InstanceIdentifier<?> iid) {
        toBeDeletedByResourceType.get(resoureType).add(iid);
        toBeAddedByResourceType.get(resoureType).remove(iid);
    }

    private void postPut(String resoureType, InstanceIdentifier<?> iid) {
        toBeAddedByResourceType.get(resoureType).remove(iid);
    }

    private void postDelete(String resoureType, InstanceIdentifier<?> iid) {
        toBeDeletedByResourceType.get(resoureType).remove(iid);
    }

    /**
     * Reads the identifier of given resource type
     * If the identifier is going to be deleted , returns Optional.absent()
     * If the identifier is going to be added , returns the to be added DataObject
     * else goes to the datastore and reads it.
     *
     * @param resourceType resource type that was registered with batch manager
     * @param identifier   identifier to be read
     * @return Optional optional dataObject
     * @throws ReadFailedException throws read failed exception if the datastore read fails
     */
    public Optional<? extends DataObject> read(String resourceType,
                                               InstanceIdentifier<?> identifier) throws ReadFailedException {
        if (toBeDeletedByResourceType.get(resourceType).contains(identifier)) {
            return Optional.absent();
        }
        if (toBeAddedByResourceType.get(resourceType).containsKey(identifier)) {
            return Optional.of(toBeAddedByResourceType.get(resourceType).get(identifier));
        }
        ResourceHandler resourceHandler  = resourceHandlerMapper.get(resourceType).getRight();
        try (ReadOnlyTransaction tx = resourceHandler.getResourceBroker().newReadOnlyTransaction()) {
            return tx.read(resourceHandler.getDatastoreType(), identifier).checkedGet();
        }
    }

    public ListenableFuture<Void> merge(ShardResource shardResource, InstanceIdentifier<?> identifier,
                                        DataObject updatedData) {
        BlockingQueue<ActionableResource> queue = shardResource.getQueue();
        if (queue != null) {
            prePut(shardResource.name(), identifier, updatedData);
            ActionableResource actResource = new ActionableResourceImpl(identifier.toString(),
                    identifier, ActionableResource.UPDATE, updatedData, null/*oldData*/);
            queue.add(actResource);
            return actResource.getResultFuture();
        }
        return Futures
                .immediateFailedFuture(new IllegalStateException("Queue missing for provided shardResource "
                        + shardResource.name()));
    }

    public void merge(String resourceType, InstanceIdentifier<?> identifier, DataObject updatedData) {
        BlockingQueue<ActionableResource> queue = getQueue(resourceType);
        if (queue != null) {
            prePut(resourceType, identifier, updatedData);
            ActionableResource actResource = new ActionableResourceImpl(identifier.toString(),
                    identifier, ActionableResource.UPDATE, updatedData, null/*oldData*/);
            queue.add(actResource);
        }
    }

    public ListenableFuture<Void> delete(ShardResource shardResource, InstanceIdentifier<?> identifier) {
        BlockingQueue<ActionableResource> queue = shardResource.getQueue();
        if (queue != null) {
            preDelete(shardResource.name(), identifier);
            ActionableResource actResource = new ActionableResourceImpl(identifier.toString(),
                    identifier, ActionableResource.DELETE, null, null/*oldData*/);
            queue.add(actResource);
            return actResource.getResultFuture();
        }
        return Futures
                .immediateFailedFuture(new IllegalStateException("Queue missing for provided shardResource "
                        + shardResource.name()));
    }

    public void delete(String resourceType, InstanceIdentifier<?> identifier) {
        BlockingQueue<ActionableResource> queue = getQueue(resourceType);
        if (queue != null) {
            preDelete(resourceType, identifier);
            ActionableResource actResource = new ActionableResourceImpl(identifier.toString(),
                    identifier, ActionableResource.DELETE, null, null/*oldData*/);
            queue.add(actResource);
        }
    }

    public ListenableFuture<Void> put(ShardResource shardResource, InstanceIdentifier<?> identifier,
                                      DataObject updatedData) {
        BlockingQueue<ActionableResource> queue = shardResource.getQueue();
        if (queue != null) {
            prePut(shardResource.name(), identifier, updatedData);
            ActionableResource actResource = new ActionableResourceImpl(identifier.toString(),
                    identifier, ActionableResource.CREATE, updatedData, null/*oldData*/);
            queue.add(actResource);
            return actResource.getResultFuture();
        }
        return Futures
                .immediateFailedFuture(new IllegalStateException("Queue missing for provided shardResource "
                        + shardResource.name()));
    }

    public void put(String resourceType, InstanceIdentifier<?> identifier, DataObject updatedData) {
        BlockingQueue<ActionableResource> queue = getQueue(resourceType);
        if (queue != null) {
            prePut(resourceType, identifier, updatedData);
            ActionableResource actResource = new ActionableResourceImpl(identifier.toString(),
                    identifier, ActionableResource.CREATE, updatedData, null/*oldData*/);
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
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = resourceBatchingThreadMapper.get(resourceType);
        if (scheduledThreadPoolExecutor != null) {
            scheduledThreadPoolExecutor.shutdown();
        }
        resourceHandlerMapper.remove(resourceType);
        resourceBatchingThreadMapper.remove(resourceType);
    }

    private class Batcher implements Runnable {
        private final String resourceType;

        Batcher(String resourceType) {
            this.resourceType = resourceType;
        }

        @Override
        public void run() {
            List<ActionableResource> resList = new ArrayList<>();

            try {
                Pair<BlockingQueue<ActionableResource>, ResourceHandler> resMapper =
                        resourceHandlerMapper.get(resourceType);
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

                int batches = resList.size() / batchSize;
                if (resList.size() > batchSize) {
                    LOG.info("Batched up resources of size {} into batches {} for resourcetype {}",
                            resList.size(), batches, resourceType);
                    for (int i = 0, j = 0; i < batches; j = j + batchSize,i++) {
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
                LOG.info("Total taken ##time = {}ms for resourceList of size {} for resourceType {}",
                        timetaken, resList.size(), resourceType);

            } catch (InterruptedException e) {
                LOG.error("InterruptedException during run()", e);
            }

        }
    }

    private class MdsalDsTask<T extends DataObject> {
        String resourceType;
        List<ActionableResource> actResourceList;

        MdsalDsTask(String resourceType, List<ActionableResource> actResourceList) {
            this.resourceType = resourceType;
            this.actResourceList = actResourceList;
        }

        public void process() {
            LOG.trace("Picked up 3 size {} of resourceType {}", actResourceList.size(), resourceType);
            Pair<BlockingQueue<ActionableResource>, ResourceHandler> resMapper =
                    resourceHandlerMapper.get(resourceType);
            if (resMapper == null) {
                LOG.error("Unable to find resourceMapper for batching the ResourceType {}", resourceType);
                return;
            }
            ResourceHandler resHandler = resMapper.getRight();
            DataBroker broker = resHandler.getResourceBroker();
            LogicalDatastoreType dsType = resHandler.getDatastoreType();
            WriteTransaction tx = broker.newWriteOnlyTransaction();
            List<SubTransaction> transactionObjects = new ArrayList<>();
            Map<SubTransaction, SettableFuture<Void>> txMap = new HashMap<>();
            for (ActionableResource actResource : actResourceList) {
                int startSize = transactionObjects.size();
                switch (actResource.getAction()) {
                    case ActionableResource.CREATE:
                        resHandler.create(tx, dsType, actResource.getInstanceIdentifier(), actResource.getInstance(),
                                transactionObjects);
                        break;
                    case ActionableResource.UPDATE:
                        Object updated = actResource.getInstance();
                        Object original = actResource.getOldInstance();
                        resHandler.update(tx, dsType, actResource.getInstanceIdentifier(), original,
                                updated,transactionObjects);
                        break;
                    case ActionableResource.DELETE:
                        resHandler.delete(tx, dsType, actResource.getInstanceIdentifier(), actResource.getInstance(),
                                transactionObjects);
                        break;
                    default:
                        LOG.error("Unable to determine Action for ResourceType {} with ResourceKey {}",
                                resourceType, actResource.getKey());
                }
                int endSize = transactionObjects.size();
                if (endSize > startSize) {
                    txMap.put(transactionObjects.get(endSize - 1),
                            (SettableFuture<Void>) actResource.getResultFuture());
                }
            }

            long start = System.currentTimeMillis();
            ListenableFuture<Void> futures = tx.submit();

            try {
                futures.get();
                actResourceList.forEach(actionableResource -> {
                    ((SettableFuture<Void>) actionableResource.getResultFuture()).set(null);
                    postCommit(actionableResource);
                });
                long time = System.currentTimeMillis() - start;
                LOG.trace("##### Time taken for {} = {}ms", actResourceList.size(), time);

            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Exception occurred while batch writing to datastore", e);
                LOG.info("Trying to submit transaction operations one at a time for resType {}", resourceType);
                for (SubTransaction object : transactionObjects) {
                    WriteTransaction writeTransaction = broker.newWriteOnlyTransaction();
                    switch (object.getAction()) {
                        case SubTransaction.CREATE:
                            writeTransaction.put(dsType, object.getInstanceIdentifier(),
                                    (DataObject) object.getInstance(), true);
                            break;
                        case SubTransaction.DELETE:
                            writeTransaction.delete(dsType, object.getInstanceIdentifier());
                            break;
                        case SubTransaction.UPDATE:
                            writeTransaction.merge(dsType, object.getInstanceIdentifier(),
                                    (DataObject) object.getInstance(), true);
                            break;
                        default:
                            LOG.error("Unable to determine Action for transaction object with id {}",
                                    object.getInstanceIdentifier());
                    }
                    ListenableFuture<Void> futureOperation = writeTransaction.submit();
                    try {
                        futureOperation.get();
                        if (txMap.containsKey(object)) {
                            txMap.get(object).set(null);
                        } else {
                            LOG.error("Subtx object {} has no Actionable-resource associated with it !! ",
                                    object.getInstanceIdentifier());
                        }
                    } catch (InterruptedException | ExecutionException exception) {
                        if (txMap.containsKey(object)) {
                            txMap.get(object).setException(exception);
                        }
                        LOG.error("Error {} to datastore (path, data) : ({}, {})", object.getAction(),
                                object.getInstanceIdentifier(), object.getInstance(), exception);
                    } finally {
                        postCommit(object);
                    }
                }
            }
        }

        private void postCommit(SubTransaction subTransaction) {
            switch (subTransaction.getAction()) {
                case SubTransaction.CREATE:
                case SubTransaction.UPDATE:
                    postPut(resourceType, subTransaction.getInstanceIdentifier());
                    break;
                case SubTransaction.DELETE:
                    postDelete(resourceType, subTransaction.getInstanceIdentifier());
                    break;
                default:
                    break;
            }
        }

        private void postCommit(ActionableResource actionableResource) {
            switch (actionableResource.getAction()) {
                case ActionableResource.CREATE:
                case ActionableResource.UPDATE:
                    postPut(resourceType, actionableResource.getInstanceIdentifier());
                    break;
                case ActionableResource.DELETE:
                    postDelete(resourceType, actionableResource.getInstanceIdentifier());
                    break;
                default:
                    break;
            }
        }
    }
}
