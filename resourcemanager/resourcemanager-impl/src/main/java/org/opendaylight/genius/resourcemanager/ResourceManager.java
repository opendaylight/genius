/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.resourcemanager;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
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
    private static final Logger LOG = LoggerFactory.getLogger(ResourceManager.class);

    private static final String TABLES_NAME = "resource.tables.name";
    private static final String GROUPS_NAME = "resource.groups.name";
    private static final String METERS_NAME = "resource.meters.name";

    private static final String TABLES_START_ID = "resource.tables.startId";
    private static final String TABLES_END_ID = "resource.tables.endId";
    private static final String GROUPS_START_ID = "resource.groups.startId";
    private static final String GROUPS_END_ID = "resource.groups.endId";
    private static final String METERS_START_ID = "resource.meters.startId";
    private static final String METERS_END_ID = "resource.meters.endId";

    private static final String CREATING_POOL_WITH_DEFAULT_VALUES = "Creating pool with default values";

    private final DataBroker dataBroker;
    private final IdManagerService idManager;

    private final ConcurrentHashMap<Class<? extends ResourceTypeBase>, String> resourceMap;

    @Inject
    public ResourceManager(final DataBroker dataBroker, final IdManagerService idManager) {
        this.dataBroker = dataBroker;
        this.idManager = idManager;
        this.resourceMap = new ConcurrentHashMap<>();
        if (System.getProperty(TABLES_NAME) != null && System.getProperty(GROUPS_NAME) != null
                && System.getProperty(METERS_NAME) != null) {
            resourceMap.put(ResourceTypeTableIds.class, System.getProperty(TABLES_NAME));
            resourceMap.put(ResourceTypeGroupIds.class, System.getProperty(GROUPS_NAME));
            resourceMap.put(ResourceTypeMeterIds.class, System.getProperty(METERS_NAME));
        } else {
            // Updating Map with default values
            resourceMap.put(ResourceTypeTableIds.class, "tables");
            resourceMap.put(ResourceTypeGroupIds.class, "groups");
            resourceMap.put(ResourceTypeMeterIds.class, "meters");
        }
    }

    @PostConstruct
    public void start() {
        LOG.info("{} start", getClass().getSimpleName());
        createIdpools();
    }

    @Override
    public Future<RpcResult<AllocateResourceOutput>> allocateResource(AllocateResourceInput input) {
        try {
            Preconditions.checkNotNull(input.getResourceType());
            Preconditions.checkNotNull(input.getIdKey());
            Preconditions.checkNotNull(input.getSize());
            Preconditions.checkNotNull(resourceMap.get(input.getResourceType()));
        } catch (NullPointerException e) {
            LOG.error("Incorrect parameters for AllocateResourceInput: {}", e);
        }
        AllocateIdRangeInputBuilder allocateIdRangeBuilder = new AllocateIdRangeInputBuilder();
        allocateIdRangeBuilder.setIdKey(input.getIdKey()).setPoolName(resourceMap.get(input.getResourceType()))
                .setSize(input.getSize());
        Future<RpcResult<AllocateIdRangeOutput>> output = idManager.allocateIdRange(allocateIdRangeBuilder.build());
        AllocateResourceOutputBuilder allocateResourceOutputBuilder = new AllocateResourceOutputBuilder();
        RpcResultBuilder<AllocateResourceOutput> allocateResourceOutputRpcBuilder = null;
        List<Long> idValues = new ArrayList<>();
        try {
            if (output.get().isSuccessful()) {
                AllocateIdRangeOutput allocateIdRangeOutput = output.get().getResult();
                idValues = allocateIdRangeOutput.getIdValues();
                allocateResourceOutputBuilder.setIdValues(idValues);
                allocateResourceOutputRpcBuilder = RpcResultBuilder.success();
                allocateResourceOutputRpcBuilder.withResult(allocateResourceOutputBuilder.build());
            }
        } catch (InterruptedException | ExecutionException | NullPointerException e) {
            LOG.error("Allocate Resource failed for resource {} due to {} ", input.getResourceType(), e);
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
        long currentTimeSec = System.currentTimeMillis() / 1000;
        try {
            Preconditions.checkNotNull(input.getResourceType());
            Preconditions.checkNotNull(resourceMap.get(input.getResourceType()));
        } catch (NullPointerException e) {
            LOG.error("Incorrect parameters for GetResourcePool: {}", e);
        }

        List<AvailableIds> availableIdsList = new ArrayList<>();
        List<DelayedResourceEntries> delayedIdEntriesList = new ArrayList<>();
        InstanceIdentifier<IdPool> parentId = ResourceManagerUtils
                .getIdPoolInstance(resourceMap.get(input.getResourceType()));
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

        String localPool = ResourceManagerUtils.getLocalPoolName(resourceMap.get(input.getResourceType()));
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

        RpcResultBuilder<GetResourcePoolOutput>  rpcOutputBuilder;
        try {
            GetResourcePoolOutputBuilder outputBuilder = new GetResourcePoolOutputBuilder()
                    .setAvailableIds(availableIdsList).setDelayedResourceEntries(delayedIdEntriesList);
            rpcOutputBuilder = RpcResultBuilder.success();
            rpcOutputBuilder.withResult(outputBuilder.build());
        } catch (NullPointerException e) {
            LOG.error("Resource allocation failed: {}", e);
            rpcOutputBuilder = RpcResultBuilder.failed();
            rpcOutputBuilder.withError(RpcError.ErrorType.APPLICATION, e.getMessage());
        }
        return Futures.immediateFuture(rpcOutputBuilder.build());
    }

    @Override
    public Future<RpcResult<GetAvailableResourcesOutput>> getAvailableResources(GetAvailableResourcesInput input) {
        long totalIdsAvailableForAllocation = 0;
        long currentTimeSec = System.currentTimeMillis() / 1000;
        try {
            Preconditions.checkNotNull(input.getResourceType());
            Preconditions.checkNotNull(resourceMap.get(input.getResourceType()));
        } catch (NullPointerException e) {
            LOG.error("Incorrect parameters for GetAvailableResources: {}", e);
        }
        InstanceIdentifier<IdPool> parentId = ResourceManagerUtils
                .getIdPoolInstance(resourceMap.get(input.getResourceType()));
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

        String localPool = ResourceManagerUtils.getLocalPoolName(resourceMap.get(input.getResourceType()));
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
        try {
            Preconditions.checkNotNull(input.getResourceType());
            Preconditions.checkNotNull(input.getIdKey());
            Preconditions.checkNotNull(resourceMap.get(input.getResourceType()));
        } catch (NullPointerException e) {
            LOG.error("Incorrect parameters for the ReleaseResourceInput: {}", e);
        }
        ReleaseIdInputBuilder releaseIdInputBuilder = new ReleaseIdInputBuilder();
        releaseIdInputBuilder.setIdKey(input.getIdKey()).setPoolName(resourceMap.get(input.getResourceType()));
        RpcResultBuilder<Void> releaseIdRpcBuilder;
        try {
            idManager.releaseId(releaseIdInputBuilder.build());
            releaseIdRpcBuilder = RpcResultBuilder.success();
        } catch (NullPointerException e) {
            LOG.error("Release resource failed for resource {} due to {}", input.getResourceType(), e);
            releaseIdRpcBuilder = RpcResultBuilder.failed();
            releaseIdRpcBuilder.withError(RpcError.ErrorType.APPLICATION, e.getMessage());
        }
        return Futures.immediateFuture(releaseIdRpcBuilder.build());
    }

    private void createIdpools() {
        // Create Tables Id Pool
        if (System.getProperty(TABLES_NAME) != null && System.getProperty(TABLES_START_ID) != null
                && System.getProperty(TABLES_END_ID) != null) {
            idManager.createIdPool(new CreateIdPoolInputBuilder().setPoolName(System.getProperty(TABLES_NAME))
                    .setLow(Long.valueOf(System.getProperty(TABLES_START_ID)))
                    .setHigh(Long.valueOf(System.getProperty(TABLES_END_ID))).build());
        } else {
            LOG.trace(CREATING_POOL_WITH_DEFAULT_VALUES);
            idManager.createIdPool(
                    new CreateIdPoolInputBuilder().setPoolName("tables").setLow((long) 0).setHigh((long) 254).build());
        }

        // Create Groups Id Pool
        if (System.getProperty(GROUPS_NAME) != null && System.getProperty(GROUPS_START_ID) != null
                && System.getProperty(GROUPS_END_ID) != null) {
            idManager.createIdPool(new CreateIdPoolInputBuilder().setPoolName(System.getProperty(GROUPS_NAME))
                    .setLow(Long.valueOf(System.getProperty(GROUPS_START_ID)))
                    .setHigh(Long.valueOf(System.getProperty(GROUPS_END_ID))).build());
        } else {
            LOG.trace(CREATING_POOL_WITH_DEFAULT_VALUES);
            idManager.createIdPool(
                    new CreateIdPoolInputBuilder().setPoolName("meters").setLow((long) 0).setHigh((long) 254).build());
        }

        // Create Meters Id Pool
        if (System.getProperty(METERS_NAME) != null && System.getProperty(METERS_START_ID) != null
                && System.getProperty(METERS_END_ID) != null) {
            idManager.createIdPool(new CreateIdPoolInputBuilder().setPoolName(System.getProperty(METERS_NAME))
                    .setLow(Long.valueOf(System.getProperty(METERS_START_ID)))
                    .setHigh(Long.valueOf(System.getProperty(METERS_END_ID))).build());
        } else {
            LOG.trace(CREATING_POOL_WITH_DEFAULT_VALUES);
            idManager.createIdPool(
                    new CreateIdPoolInputBuilder().setPoolName("groups").setLow((long) 0).setHigh((long) 254).build());
        }
    }

    @Override
    @PreDestroy
    public void close() throws Exception {
        LOG.info("{} close", getClass().getSimpleName());
    }
}
