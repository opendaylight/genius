/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.resourcemanager;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPool;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.AvailableIdsHolder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.ReleasedIdsHolder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.released.ids.DelayedIdEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.rev160622.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.rev160622.getresourcepool.output.AvailableIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.rev160622.released.resource.ids.DelayedResourceEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.rev160622.released.resource.ids.DelayedResourceEntriesBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.rmi.runtime.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class ResourceManager implements ResourceManagerService, AutoCloseable{

    private static final Logger LOG = LoggerFactory.getLogger(ResourceManager.class);
    private final DataBroker broker;
    private final IdManagerService idManager;
    private final ConcurrentHashMap<Class<? extends ResourceTypeBase>, String> resourceMap;
    private final String TABLESNAME = "resource.tables.name";
    private final String GROUPSNAME = "resource.groups.name";
    private final String METERSNAME = "resource.meters.name";

    public ResourceManager(DataBroker broker, IdManagerService idManager) {
        this.broker = broker;
        this.idManager = idManager;
        this.resourceMap = new ConcurrentHashMap<>();
        if(System.getProperty(TABLESNAME)!=null && System.getProperty(GROUPSNAME)!=null
                && System.getProperty(METERSNAME)!=null){
            resourceMap.put(ResourceTypeTableIds.class, System.getProperty(TABLESNAME));
            resourceMap.put(ResourceTypeGroupIds.class, System.getProperty(GROUPSNAME));
            resourceMap.put(ResourceTypeMeterIds.class, System.getProperty(METERSNAME));
        } else{
            //Updating Map with default values
            resourceMap.put(ResourceTypeTableIds.class, "tables");
            resourceMap.put(ResourceTypeGroupIds.class, "groups");
            resourceMap.put(ResourceTypeMeterIds.class, "meters");
        }
    }

    @Override
    public void close() throws Exception {
        LOG.info("ResourceManager closed");
    }

    @Override
    public Future<RpcResult<AllocateResourceOutput>> allocateResource(AllocateResourceInput input) {
        try {
            Preconditions.checkNotNull(input.getResourceType());
            Preconditions.checkNotNull(input.getIdKey());
            Preconditions.checkNotNull(input.getSize());
            Preconditions.checkNotNull(resourceMap.get(input.getResourceType()));
        } catch (NullPointerException e){
            LOG.error("Incorrect parameters for AllocateResourceInput");
        }
        AllocateIdRangeInputBuilder allocateIdRangeBuilder = new AllocateIdRangeInputBuilder();
        allocateIdRangeBuilder.setIdKey(input.getIdKey()).setPoolName(resourceMap.get(input.getResourceType()))
                .setSize(input.getSize());
        Future<RpcResult<AllocateIdRangeOutput>> output = idManager.allocateIdRange(allocateIdRangeBuilder.build());
        AllocateResourceOutputBuilder allocateResourceOutputBuilder = new AllocateResourceOutputBuilder();
        RpcResultBuilder<AllocateResourceOutput> allocateResourceOutputRpcBuilder = null;
        List<Long> idValues = new ArrayList<>();
        try {
            if(output.get().isSuccessful()) {
                AllocateIdRangeOutput allocateIdRangeOutput = output.get().getResult();
                idValues = allocateIdRangeOutput.getIdValues();
                allocateResourceOutputBuilder.setIdValues(idValues);
                allocateResourceOutputRpcBuilder = RpcResultBuilder.success();
                allocateResourceOutputRpcBuilder.withResult(allocateResourceOutputBuilder.build());
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            allocateResourceOutputRpcBuilder = RpcResultBuilder.failed();
            allocateResourceOutputRpcBuilder.withError(RpcError.ErrorType.APPLICATION, e.getMessage());
        } catch(Exception e){
            LOG.error("Allocate Resource failed for resource {} due to {} ", input.getResourceType(), e);
            allocateResourceOutputRpcBuilder = RpcResultBuilder.failed();
            allocateResourceOutputRpcBuilder.withError(RpcError.ErrorType.APPLICATION, e.getMessage());
        }

        if(allocateResourceOutputRpcBuilder == null){
            allocateResourceOutputRpcBuilder = RpcResultBuilder.failed();
            allocateResourceOutputRpcBuilder.withError(RpcError.ErrorType.APPLICATION, "Resource cannot be  allocated");
        }
        return Futures.immediateFuture(allocateResourceOutputRpcBuilder.build());
    }

    @Override
    public Future<RpcResult<GetResourcePoolOutput>> getResourcePool(GetResourcePoolInput input) {
        IdPool parentIdPool = null;
        IdPool localIdPool = null;
        long delayedTime = 0;
        GetResourcePoolOutputBuilder outputBuilder = null;
        RpcResultBuilder<GetResourcePoolOutput> rpcOutputBuilder = null;
        long currentTimeSec = System.currentTimeMillis() / 1000;
        try {
            Preconditions.checkNotNull(input.getResourceType());
            Preconditions.checkNotNull(resourceMap.get(input.getResourceType()));
        } catch (NullPointerException e){
            LOG.error("Incorrect parameters for GetResourcePool");
        }

        List<org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager
                .rev160622.getresourcepool.output.AvailableIds> availableIdsList= new ArrayList<>();
        List<DelayedResourceEntries> delayedIdEntriesList = new ArrayList<>();

        InstanceIdentifier<IdPool> parentId = ResourceManagerUtils.getIdPoolInstance(resourceMap.get(input.getResourceType()));
        Optional<IdPool> optionalParentIdPool = MDSALUtil.read(LogicalDatastoreType.CONFIGURATION, parentId, broker);
        if(optionalParentIdPool!=null && optionalParentIdPool.isPresent()){
            parentIdPool = optionalParentIdPool.get();
            AvailableIdsHolder availableParentIdsHolder = parentIdPool.getAvailableIdsHolder();
            if (availableParentIdsHolder.getStart() < availableParentIdsHolder.getEnd()) {
                availableIdsList.add(new AvailableIdsBuilder().setStart(availableParentIdsHolder.getStart())
                        .setEnd(availableParentIdsHolder.getEnd()).build());
            }
            ReleasedIdsHolder releasedParentIdsHolder = parentIdPool.getReleasedIdsHolder();
            if(releasedParentIdsHolder!=null){
                List<DelayedIdEntries> delayedIdParentList = releasedParentIdsHolder.getDelayedIdEntries();
                if(delayedIdParentList!=null && !delayedIdParentList.isEmpty()){
                    for(DelayedIdEntries delayedParentEntry : delayedIdParentList){
                        delayedIdEntriesList.add(new DelayedResourceEntriesBuilder().setId(delayedParentEntry.getId()).
                                setReadyTimeSec(delayedParentEntry.getReadyTimeSec()).build());
                        delayedTime = releasedParentIdsHolder.getDelayedTimeSec();
                    }
                }
            }
        }

        String localPool = ResourceManagerUtils.getLocalPoolName(resourceMap.get(input.getResourceType()));
        InstanceIdentifier<IdPool> localId = ResourceManagerUtils.getIdPoolInstance(localPool);
        Optional<IdPool> optionalLocalId = MDSALUtil.read(LogicalDatastoreType.CONFIGURATION, localId,broker);
        if(optionalLocalId!=null && optionalLocalId.isPresent()){
            localIdPool = optionalLocalId.get();
            AvailableIdsHolder availableLocalIdsHolder = localIdPool.getAvailableIdsHolder();
            if(availableLocalIdsHolder!=null && availableLocalIdsHolder.getStart()<availableLocalIdsHolder.getEnd()){
                availableIdsList.add(new AvailableIdsBuilder().setStart(availableLocalIdsHolder.getStart())
                        .setEnd(availableLocalIdsHolder.getEnd()).build());
            }
            ReleasedIdsHolder releasedLocalIdsHolder = localIdPool.getReleasedIdsHolder();
            if(releasedLocalIdsHolder!=null){
                List<DelayedIdEntries> delayedIdLocalList = releasedLocalIdsHolder.getDelayedIdEntries();
                if(delayedIdLocalList!=null && !delayedIdLocalList.isEmpty()) {
                    for(DelayedIdEntries delayedLocalEntry : delayedIdLocalList){
                        if (delayedLocalEntry.getReadyTimeSec() > currentTimeSec) {
                            break;
                        }
                        delayedIdEntriesList.add(new DelayedResourceEntriesBuilder().setId(delayedLocalEntry.getId()).
                                setReadyTimeSec(delayedLocalEntry.getReadyTimeSec()).build());
                    }
                }

            }
        }

        try{
            outputBuilder = new GetResourcePoolOutputBuilder().setAvailableIds(availableIdsList)
                    .setDelayedResourceEntries(delayedIdEntriesList);
            rpcOutputBuilder =  RpcResultBuilder.success();
            rpcOutputBuilder.withResult(outputBuilder.build());
        } catch (Exception e){
            rpcOutputBuilder =  RpcResultBuilder.failed();
            rpcOutputBuilder.withError(RpcError.ErrorType.APPLICATION, e.getMessage());
        }
        return Futures.immediateFuture(rpcOutputBuilder.build());
    }

    @Override
    public Future<RpcResult<GetAvailableResourcesOutput>> getAvailableResources(GetAvailableResourcesInput input) {

        IdPool parentIdPool = null;
        IdPool localIdPool = null;
        long totalIdsAvailableForAllocation = 0;
        long currentTimeSec = System.currentTimeMillis() / 1000;
        try {
            Preconditions.checkNotNull(input.getResourceType());
            Preconditions.checkNotNull(resourceMap.get(input.getResourceType()));
        } catch (NullPointerException e){
            LOG.error("Incorrect parameters for GetAvailableResources");
        }
        InstanceIdentifier<IdPool> parentId = ResourceManagerUtils.getIdPoolInstance(resourceMap.get(input.getResourceType()));
        Optional<IdPool> optionalParentIdPool = MDSALUtil.read(LogicalDatastoreType.CONFIGURATION, parentId, broker);
        if(optionalParentIdPool!=null && optionalParentIdPool.isPresent()){
            parentIdPool = optionalParentIdPool.get();
            AvailableIdsHolder availableParentIdsHolder = parentIdPool.getAvailableIdsHolder();
            totalIdsAvailableForAllocation = availableParentIdsHolder.getEnd() - availableParentIdsHolder.getCursor();
            ReleasedIdsHolder releasedParentIdsHolder = parentIdPool.getReleasedIdsHolder();
            if(releasedParentIdsHolder!=null){
                List<DelayedIdEntries> delayedIdParentList = releasedParentIdsHolder.getDelayedIdEntries();
                if(delayedIdParentList!=null && !delayedIdParentList.isEmpty()) {
                    totalIdsAvailableForAllocation+= delayedIdParentList.size();
                }
            }
        }

        String localPool = ResourceManagerUtils.getLocalPoolName(resourceMap.get(input.getResourceType()));
        InstanceIdentifier<IdPool> localId = ResourceManagerUtils.getIdPoolInstance(localPool);
        Optional<IdPool> optionalLocalId = MDSALUtil.read(LogicalDatastoreType.CONFIGURATION, localId,broker);
        if(optionalLocalId!=null && optionalLocalId.isPresent()){
            localIdPool = optionalLocalId.get();
            AvailableIdsHolder availableLocalIdsHolder = localIdPool.getAvailableIdsHolder();
            if(availableLocalIdsHolder!=null){
                totalIdsAvailableForAllocation += availableLocalIdsHolder.getEnd() - availableLocalIdsHolder.getCursor();
            }
            ReleasedIdsHolder releasedLocalIdsHolder = localIdPool.getReleasedIdsHolder();
            if(releasedLocalIdsHolder!=null){
                long count = 0;
                List<DelayedIdEntries> delayedIdLocalList = releasedLocalIdsHolder.getDelayedIdEntries();
                if(delayedIdLocalList!=null && !delayedIdLocalList.isEmpty()) {
                    for(DelayedIdEntries delayedLocalEntry : delayedIdLocalList){
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
        } catch (NullPointerException e){
            LOG.error("Incorrect parameters for the ReleaseResourceInput");
        }
        ReleaseIdInputBuilder releaseIdInputBuilder = new ReleaseIdInputBuilder();
        releaseIdInputBuilder.setIdKey(input.getIdKey()).setPoolName(resourceMap.get(input.getResourceType()));
        RpcResultBuilder<Void> releaseIdRpcBuilder;
        try {
            idManager.releaseId(releaseIdInputBuilder.build());
            releaseIdRpcBuilder = RpcResultBuilder.success();
        } catch (Exception e){
            LOG.error("Release resource failed for resource {} due to {}", input.getResourceType(), e);
            releaseIdRpcBuilder = RpcResultBuilder.failed();
            releaseIdRpcBuilder.withError(RpcError.ErrorType.APPLICATION, e.getMessage());
        }
        return Futures.immediateFuture(releaseIdRpcBuilder.build());
    }
}