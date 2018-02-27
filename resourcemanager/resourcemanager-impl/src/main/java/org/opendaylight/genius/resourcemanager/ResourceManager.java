/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.resourcemanager;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.infrautils.utils.concurrent.JdkFutures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdRangeInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdRangeOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.CreateIdPoolInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.ReleaseIdInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPool;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.AvailableIdsHolder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.ReleasedIdsHolder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.released.ids.DelayedIdEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.rev160622.AllocateResourceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.rev160622.AllocateResourceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.rev160622.AllocateResourceOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.rev160622.GetAvailableResourcesInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.rev160622.GetAvailableResourcesOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.rev160622.GetAvailableResourcesOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.rev160622.GetResourcePoolInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.rev160622.GetResourcePoolOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.rev160622.GetResourcePoolOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.rev160622.ReleaseResourceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.rev160622.ResourceManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.rev160622.ResourceTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.rev160622.ResourceTypeGroupIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.rev160622.ResourceTypeMeterIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.rev160622.ResourceTypeTableIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.rev160622.getresourcepool.output.AvailableIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.rev160622.getresourcepool.output.AvailableIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.rev160622.released.resource.ids.DelayedResourceEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.rev160622.released.resource.ids.DelayedResourceEntriesBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ResourceManager implements ResourceManagerService, AutoCloseable {
    // Property names
    private static final String RESOURCE_TABLES_NAME_PROPERTY = "resource.tables.name";
    private static final String RESOURCE_GROUPS_NAME_PROPERTY = "resource.groups.name";
    private static final String RESOURCE_METERS_NAME_PROPERTY = "resource.meters.name";

    private static final String RESOURCE_TABLES_START_ID_PROPERTY = "resource.tables.startId";
    private static final String RESOURCE_TABLES_END_ID_PROPERTY = "resource.tables.endId";
    private static final String RESOURCE_GROUPS_START_ID_PROPERTY = "resource.groups.startId";
    private static final String RESOURCE_GROUPS_END_ID_PROPERTY = "resource.groups.endId";
    private static final String RESOURCE_METERS_START_ID_PROPERTY = "resource.meters.startId";
    private static final String RESOURCE_METERS_END_ID_PROPERTY = "resource.meters.endId";

    // Cache default values
    private static final String RESOURCE_TABLES_DEFAULT_NAME = "tables";
    private static final String RESOURCE_GROUPS_DEFAULT_NAME = "groups";
    private static final String RESOURCE_METERS_DEFAULT_NAME = "meters";

    // Default ranges of IDs
    private static final String DEFAULT_LOW_RANGE = "0";
    private static final String DEFAULT_HIGH_RANGE = "254";

    // Messages
    private static final String RESOURCE_TYPE_CANNOT_BE_NULL = "Resource type cannot be null";
    private static final String RESOURCE_TYPE_NOT_FOUND = "Resource type not found";
    private static final String RESOURCE_ID_CANNOT_BE_NULL = "Id key cannot be null";
    private static final String RESOURCE_SIZE_CANNOT_BE_NULL = "Resource size cannot be null";

    // Other services
    private final DataBroker dataBroker;
    private final IdManagerService idManager;

    // Cache of resources
    private final ConcurrentMap<Class<? extends ResourceTypeBase>, String> resourcesCache;

    private static final Logger LOG = LoggerFactory.getLogger(ResourceManager.class);

    @Inject
    public ResourceManager(final DataBroker dataBroker, final IdManagerService idManager) {
        this.dataBroker = dataBroker;
        this.idManager = idManager;
        this.resourcesCache = loadCache();
        createIdpools();
    }

    private ConcurrentMap<Class<? extends ResourceTypeBase>, String> loadCache() {
        ConcurrentMap<Class<? extends ResourceTypeBase>, String> cache = new ConcurrentHashMap<>();
        cache.put(ResourceTypeTableIds.class,
                System.getProperty(RESOURCE_TABLES_NAME_PROPERTY, RESOURCE_TABLES_DEFAULT_NAME));
        cache.put(ResourceTypeGroupIds.class,
                System.getProperty(RESOURCE_GROUPS_NAME_PROPERTY, RESOURCE_GROUPS_DEFAULT_NAME));
        cache.put(ResourceTypeMeterIds.class,
                System.getProperty(RESOURCE_METERS_NAME_PROPERTY, RESOURCE_METERS_DEFAULT_NAME));
        return cache;
    }

    @Override
    public Future<RpcResult<AllocateResourceOutput>> allocateResource(AllocateResourceInput input) {
        Objects.requireNonNull(input.getResourceType(), RESOURCE_TYPE_CANNOT_BE_NULL);
        Objects.requireNonNull(input.getIdKey(), RESOURCE_ID_CANNOT_BE_NULL);
        Objects.requireNonNull(input.getSize(), RESOURCE_SIZE_CANNOT_BE_NULL);

        Objects.requireNonNull(resourcesCache.get(input.getResourceType()), RESOURCE_TYPE_NOT_FOUND);

        AllocateIdRangeInputBuilder allocateIdRangeBuilder = new AllocateIdRangeInputBuilder();
        allocateIdRangeBuilder.setIdKey(input.getIdKey()).setPoolName(resourcesCache.get(input.getResourceType()))
                .setSize(input.getSize());
        Future<RpcResult<AllocateIdRangeOutput>> output = idManager.allocateIdRange(allocateIdRangeBuilder.build());
        AllocateResourceOutputBuilder allocateResourceOutputBuilder = new AllocateResourceOutputBuilder();
        RpcResultBuilder<AllocateResourceOutput> allocateResourceOutputRpcBuilder = null;
        try {
            if (output.get().isSuccessful()) {
                AllocateIdRangeOutput allocateIdRangeOutput = output.get().getResult();
                List<Long> idValues = allocateIdRangeOutput.getIdValues();
                allocateResourceOutputBuilder.setIdValues(idValues);
                allocateResourceOutputRpcBuilder = RpcResultBuilder.success();
                allocateResourceOutputRpcBuilder.withResult(allocateResourceOutputBuilder.build());
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Allocate Resource failed for resource {} due to ", input.getResourceType(), e);
            allocateResourceOutputRpcBuilder = RpcResultBuilder.failed();
            allocateResourceOutputRpcBuilder.withError(RpcError.ErrorType.APPLICATION, e.getMessage());
        }

        if (allocateResourceOutputRpcBuilder == null) {
            allocateResourceOutputRpcBuilder = RpcResultBuilder.failed();
            allocateResourceOutputRpcBuilder.withError(RpcError.ErrorType.APPLICATION, "Resource cannot be  allocated");
        }
        return Futures.immediateFuture(allocateResourceOutputRpcBuilder.build());
    }

    @Override
    public Future<RpcResult<GetResourcePoolOutput>> getResourcePool(GetResourcePoolInput input) {
        Objects.requireNonNull(input.getResourceType(), RESOURCE_TYPE_CANNOT_BE_NULL);
        Objects.requireNonNull(resourcesCache.get(input.getResourceType()), RESOURCE_TYPE_CANNOT_BE_NULL);

        long currentTimeSec = System.currentTimeMillis() / 1000;
        List<AvailableIds> availableIdsList = new ArrayList<>();
        List<DelayedResourceEntries> delayedIdEntriesList = new ArrayList<>();
        InstanceIdentifier<IdPool> parentId = ResourceManagerUtils
                .getIdPoolInstance(resourcesCache.get(input.getResourceType()));
        Optional<IdPool> optionalParentIdPool = MDSALUtil.read(LogicalDatastoreType.CONFIGURATION, parentId,
                dataBroker);
        if (optionalParentIdPool != null && optionalParentIdPool.isPresent()) {
            IdPool parentIdPool = optionalParentIdPool.get();
            AvailableIdsHolder availableParentIdsHolder = parentIdPool.getAvailableIdsHolder();
            if (availableParentIdsHolder.getStart() < availableParentIdsHolder.getEnd()) {
                availableIdsList.add(new AvailableIdsBuilder().setStart(availableParentIdsHolder.getStart())
                        .setEnd(availableParentIdsHolder.getEnd()).build());
            }
            ReleasedIdsHolder releasedParentIdsHolder = parentIdPool.getReleasedIdsHolder();
            if (releasedParentIdsHolder != null) {
                List<DelayedIdEntries> delayedIdParentList = releasedParentIdsHolder.getDelayedIdEntries();
                if (delayedIdParentList != null && !delayedIdParentList.isEmpty()) {
                    for (DelayedIdEntries delayedParentEntry : delayedIdParentList) {
                        delayedIdEntriesList.add(new DelayedResourceEntriesBuilder().setId(delayedParentEntry.getId())
                                .setReadyTimeSec(delayedParentEntry.getReadyTimeSec()).build());
                    }
                }
            }
        }

        String localPool = ResourceManagerUtils.getLocalPoolName(resourcesCache.get(input.getResourceType()));
        InstanceIdentifier<IdPool> localId = ResourceManagerUtils.getIdPoolInstance(localPool);
        Optional<IdPool> optionalLocalId = MDSALUtil.read(LogicalDatastoreType.CONFIGURATION, localId, dataBroker);
        if (optionalLocalId != null && optionalLocalId.isPresent()) {
            IdPool localIdPool = optionalLocalId.get();
            AvailableIdsHolder availableLocalIdsHolder = localIdPool.getAvailableIdsHolder();
            if (availableLocalIdsHolder != null
                    && availableLocalIdsHolder.getStart() < availableLocalIdsHolder.getEnd()) {
                availableIdsList.add(new AvailableIdsBuilder().setStart(availableLocalIdsHolder.getStart())
                        .setEnd(availableLocalIdsHolder.getEnd()).build());
            }
            ReleasedIdsHolder releasedLocalIdsHolder = localIdPool.getReleasedIdsHolder();
            if (releasedLocalIdsHolder != null) {
                List<DelayedIdEntries> delayedIdLocalList = releasedLocalIdsHolder.getDelayedIdEntries();
                if (delayedIdLocalList != null && !delayedIdLocalList.isEmpty()) {
                    for (DelayedIdEntries delayedLocalEntry : delayedIdLocalList) {
                        if (delayedLocalEntry.getReadyTimeSec() > currentTimeSec) {
                            break;
                        }
                        delayedIdEntriesList.add(new DelayedResourceEntriesBuilder().setId(delayedLocalEntry.getId())
                                .setReadyTimeSec(delayedLocalEntry.getReadyTimeSec()).build());
                    }
                }
            }
        }
        GetResourcePoolOutput output = new GetResourcePoolOutputBuilder().setAvailableIds(availableIdsList)
                .setDelayedResourceEntries(delayedIdEntriesList).build();
        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public Future<RpcResult<GetAvailableResourcesOutput>> getAvailableResources(GetAvailableResourcesInput input) {
        Objects.requireNonNull(input.getResourceType(), RESOURCE_TYPE_CANNOT_BE_NULL);
        Objects.requireNonNull(resourcesCache.get(input.getResourceType()), RESOURCE_TYPE_NOT_FOUND);

        long totalIdsAvailableForAllocation = 0;
        long currentTimeSec = System.currentTimeMillis() / 1000;
        InstanceIdentifier<IdPool> parentId = ResourceManagerUtils
                .getIdPoolInstance(resourcesCache.get(input.getResourceType()));
        Optional<IdPool> optionalParentIdPool = MDSALUtil.read(LogicalDatastoreType.CONFIGURATION, parentId,
                dataBroker);
        if (optionalParentIdPool != null && optionalParentIdPool.isPresent()) {
            IdPool parentIdPool = optionalParentIdPool.get();
            AvailableIdsHolder availableParentIdsHolder = parentIdPool.getAvailableIdsHolder();
            totalIdsAvailableForAllocation = availableParentIdsHolder.getEnd() - availableParentIdsHolder.getCursor();
            ReleasedIdsHolder releasedParentIdsHolder = parentIdPool.getReleasedIdsHolder();
            if (releasedParentIdsHolder != null) {
                List<DelayedIdEntries> delayedIdParentList = releasedParentIdsHolder.getDelayedIdEntries();
                if (delayedIdParentList != null && !delayedIdParentList.isEmpty()) {
                    totalIdsAvailableForAllocation += delayedIdParentList.size();
                }
            }
        }

        String localPool = ResourceManagerUtils.getLocalPoolName(resourcesCache.get(input.getResourceType()));
        InstanceIdentifier<IdPool> localId = ResourceManagerUtils.getIdPoolInstance(localPool);
        Optional<IdPool> optionalLocalId = MDSALUtil.read(LogicalDatastoreType.CONFIGURATION, localId, dataBroker);
        if (optionalLocalId != null && optionalLocalId.isPresent()) {
            IdPool localIdPool = optionalLocalId.get();
            AvailableIdsHolder availableLocalIdsHolder = localIdPool.getAvailableIdsHolder();
            if (availableLocalIdsHolder != null) {
                totalIdsAvailableForAllocation += availableLocalIdsHolder.getEnd()
                        - availableLocalIdsHolder.getCursor();
            }
            ReleasedIdsHolder releasedLocalIdsHolder = localIdPool.getReleasedIdsHolder();
            if (releasedLocalIdsHolder != null) {
                long count = 0;
                List<DelayedIdEntries> delayedIdLocalList = releasedLocalIdsHolder.getDelayedIdEntries();
                if (delayedIdLocalList != null && !delayedIdLocalList.isEmpty()) {
                    for (DelayedIdEntries delayedLocalEntry : delayedIdLocalList) {
                        if (delayedLocalEntry.getReadyTimeSec() > currentTimeSec) {
                            break;
                        }
                    }
                    count++;
                }
                totalIdsAvailableForAllocation += count;
            }
        }

        GetAvailableResourcesOutputBuilder outputBuilder = new GetAvailableResourcesOutputBuilder()
                .setTotalAvailableIdCount(totalIdsAvailableForAllocation);
        RpcResultBuilder<GetAvailableResourcesOutput> rpcOutputBuilder = null;
        rpcOutputBuilder = RpcResultBuilder.success();
        rpcOutputBuilder.withResult(outputBuilder.build());

        return Futures.immediateFuture(rpcOutputBuilder.build());
    }

    @Override
    public Future<RpcResult<Void>> releaseResource(ReleaseResourceInput input) {
        Objects.requireNonNull(input.getIdKey(), RESOURCE_ID_CANNOT_BE_NULL);
        Objects.requireNonNull(input.getResourceType(), RESOURCE_TYPE_CANNOT_BE_NULL);

        Objects.requireNonNull(resourcesCache.get(input.getResourceType()), RESOURCE_TYPE_NOT_FOUND);

        ReleaseIdInputBuilder releaseIdInputBuilder = new ReleaseIdInputBuilder();
        releaseIdInputBuilder.setIdKey(input.getIdKey()).setPoolName(resourcesCache.get(input.getResourceType()));

        return idManager.releaseId(releaseIdInputBuilder.build());
    }

    private void createIdPool(String poolNameProperty, String lowIdProperty, String highIdProperty,
            String poolDefaultName) {
        JdkFutures.addErrorLogging(idManager.createIdPool(
                new CreateIdPoolInputBuilder().setPoolName(System.getProperty(poolNameProperty, poolDefaultName))
                        .setLow(Long.valueOf(System.getProperty(lowIdProperty, DEFAULT_LOW_RANGE)))
                        .setHigh(Long.valueOf(System.getProperty(highIdProperty, DEFAULT_HIGH_RANGE))).build()),
                LOG, "createIdPool");
    }

    private void createIdpools() {
        // Create Tables Id Pool
        createIdPool(RESOURCE_TABLES_NAME_PROPERTY, RESOURCE_TABLES_START_ID_PROPERTY, RESOURCE_TABLES_END_ID_PROPERTY,
                RESOURCE_TABLES_DEFAULT_NAME);

        // Create Groups Id Pool
        createIdPool(RESOURCE_GROUPS_NAME_PROPERTY, RESOURCE_GROUPS_START_ID_PROPERTY, RESOURCE_GROUPS_END_ID_PROPERTY,
                RESOURCE_GROUPS_DEFAULT_NAME);

        // Create Meters Id Pool
        createIdPool(RESOURCE_METERS_NAME_PROPERTY, RESOURCE_METERS_START_ID_PROPERTY, RESOURCE_METERS_END_ID_PROPERTY,
                RESOURCE_METERS_DEFAULT_NAME);
    }

    @Override
    @PreDestroy
    public void close() {
        LOG.debug("{} close", getClass().getSimpleName());
    }
}
