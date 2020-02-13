/*
 * Copyright (c) 2015 - 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.utils.batching;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.*;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.infrautils.utils.concurrent.Executors;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.ReadFailedException;
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

    private final ConcurrentHashMap<String, ScheduledExecutorService>
            resourceBatchingThreadMapper = new ConcurrentHashMap<>();

    private final Map<String, Set<InstanceIdentifier<?>>> pendingModificationByResourceType = new ConcurrentHashMap<>();

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
        resourceBatchingThreadMapper.values().forEach(ScheduledExecutorService::shutdown);
    }

    public void registerBatchableResource(
            String resourceType, final BlockingQueue<ActionableResource> resQueue, final ResourceHandler resHandler) {
        Preconditions.checkNotNull(resQueue, "ResourceQueue to use for batching cannot not be null.");
        Preconditions.checkNotNull(resHandler, "ResourceHandler cannot not be null.");

        resourceHandlerMapper.put(resourceType, new ImmutablePair<>(resQueue, resHandler));
        ScheduledExecutorService resDelegatorService =
                Executors.newListeningScheduledThreadPool(1, "ResourceBatchingManager", LOG);
        resourceBatchingThreadMapper.put(resourceType, resDelegatorService);
        LOG.info("Registered resourceType {} with batchSize {} and batchInterval {}", resourceType,
                resHandler.getBatchSize(), resHandler.getBatchInterval());
        resDelegatorService.scheduleWithFixedDelay(
                new Batcher(resourceType), resHandler.getBatchInterval(), resHandler.getBatchInterval(), TIME_UNIT);
        pendingModificationByResourceType.putIfAbsent(resourceType, ConcurrentHashMap.newKeySet());
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

    private void beforeModification(String resoureType, InstanceIdentifier<?> iid) {
        pendingModificationByResourceType.get(resoureType).add(iid);
    }

    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
        justification = "https://github.com/spotbugs/spotbugs/issues/811")
    private void afterModification(String resoureType, InstanceIdentifier<?> iid) {
        pendingModificationByResourceType.get(resoureType).remove(iid);
    }

    /**
     * Reads the identifier of the given resource type.
     * Not to be used by the applications  which uses their own resource queue
     *
     * @param resourceType resource type that was registered with batch manager
     * @param identifier   identifier to be read
     * @return a CheckFuture containing the result of the read
     */
    public <T extends DataObject> CheckedFuture<Optional<T>, ReadFailedException> read(
            String resourceType, InstanceIdentifier<T> identifier) {
        BlockingQueue<ActionableResource> queue = getQueue(resourceType);
        if (queue != null) {
            if (pendingModificationByResourceType.get(resourceType).contains(identifier)) {
                SettableFuture<Optional<T>> readFuture = SettableFuture.create();
                queue.add(new ActionableReadResource<>(identifier, readFuture));
                return Futures.makeChecked(readFuture, ReadFailedException.MAPPER);
            } else {
                ResourceHandler resourceHandler = resourceHandlerMapper.get(resourceType).getRight();
                try (ReadTransaction tx = resourceHandler.getResourceBroker().newReadOnlyTransaction()) {
                    return tx.read(resourceHandler.getDatastoreType(), identifier);
                }
            }
        }
        return Futures.immediateFailedCheckedFuture(new ReadFailedException(
                "No batch handler was registered for resource " + resourceType));
    }

    public ListenableFuture<Void> merge(ShardResource shardResource, InstanceIdentifier<?> identifier,
                                        DataObject updatedData) {
        BlockingQueue<ActionableResource> queue = shardResource.getQueue();
        if (queue != null) {
            beforeModification(shardResource.name(), identifier);
            ActionableResource actResource = new ActionableResourceImpl(
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
            beforeModification(resourceType, identifier);
            ActionableResource actResource = new ActionableResourceImpl(
                    identifier, ActionableResource.UPDATE, updatedData, null/*oldData*/);
            queue.add(actResource);
        }
    }

    public ListenableFuture<Void> delete(ShardResource shardResource, InstanceIdentifier<?> identifier) {
        BlockingQueue<ActionableResource> queue = shardResource.getQueue();
        if (queue != null) {
            beforeModification(shardResource.name(), identifier);
            ActionableResource actResource = new ActionableResourceImpl(
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
            beforeModification(resourceType, identifier);
            ActionableResource actResource = new ActionableResourceImpl(
                    identifier, ActionableResource.DELETE, null, null/*oldData*/);
            queue.add(actResource);
        }
    }

    public ListenableFuture<Void> put(ShardResource shardResource, InstanceIdentifier<?> identifier,
                                      DataObject updatedData) {
        BlockingQueue<ActionableResource> queue = shardResource.getQueue();
        if (queue != null) {
            beforeModification(shardResource.name(), identifier);
            ActionableResource actResource = new ActionableResourceImpl(
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
            beforeModification(resourceType, identifier);
            ActionableResource actResource = new ActionableResourceImpl(
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
        ScheduledExecutorService scheduledThreadPoolExecutor = resourceBatchingThreadMapper.get(resourceType);
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
                LOG.debug("Total taken ##time = {}ms for resourceList of size {} for resourceType {}",
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

        @SuppressWarnings("unchecked")
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
            ReadWriteTransaction tx = broker.newReadWriteTransaction();
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
                    case ActionableResource.READ:
                        ActionableReadResource<DataObject> readAction = (ActionableReadResource<DataObject>)actResource;
                        ListenableFuture<Optional<DataObject>> future =
                                tx.read(dsType, readAction.getInstanceIdentifier());
                        Futures.addCallback(future, new FutureCallback<Optional<DataObject>>() {
                            @Override
                            public void onSuccess(Optional<DataObject> result) {
                                readAction.getReadFuture().set(result);
                            }

                            @Override
                            public void onFailure(Throwable failure) {
                                readAction.getReadFuture().setException(failure);
                            }
                        }, MoreExecutors.directExecutor());
                        break;
                    default:
                        LOG.error("Unable to determine Action for ResourceType {} with ResourceKey {}",
                                resourceType, actResource);
                }
                int endSize = transactionObjects.size();
                if (endSize > startSize) {
                    txMap.put(transactionObjects.get(endSize - 1),
                            (SettableFuture<Void>) actResource.getResultFuture());
                }
            }


            long start = System.currentTimeMillis();
            FluentFuture<? extends @NonNull CommitInfo> futures = tx.commit();

            try {
                futures.get();
                actResourceList.forEach(actionableResource -> {
                    ((SettableFuture<Void>) actionableResource.getResultFuture()).set(null);
                    postCommit(actionableResource.getAction(), actionableResource.getInstanceIdentifier());
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
                    FluentFuture<? extends @NonNull CommitInfo> futureOperation = writeTransaction.commit();
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
                        postCommit(object.getAction(), object.getInstanceIdentifier());
                    }
                }
            }
        }

        private void postCommit(int action, InstanceIdentifier iid) {
            switch (action) {
                case ActionableResource.CREATE:
                case ActionableResource.UPDATE:
                case ActionableResource.DELETE:
                    afterModification(resourceType, iid);
                    break;
                default:
                    break;
            }
        }
    }

    private static class ActionableReadResource<T extends DataObject> extends ActionableResourceImpl {
        private final SettableFuture<Optional<T>> readFuture;

        ActionableReadResource(InstanceIdentifier<T> identifier, SettableFuture<Optional<T>> readFuture) {
            super(identifier, ActionableResource.READ, null, null);
            this.readFuture = readFuture;
        }

        SettableFuture<Optional<T>> getReadFuture() {
            return readFuture;
        }
    }
}
