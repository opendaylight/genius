/*
 * Copyright (c) 2017 Ericsson Spain, S.A. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.resourcemanager.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import javax.inject.Inject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.infrautils.inject.guice.testutils.GuiceRule;
import org.opendaylight.infrautils.testutils.LogCaptureRule;
import org.opendaylight.infrautils.testutils.LogRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.rev160622.AllocateResourceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.rev160622.AllocateResourceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.rev160622.AllocateResourceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.rev160622.GetAvailableResourcesInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.rev160622.GetAvailableResourcesInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.rev160622.GetAvailableResourcesOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.rev160622.GetResourcePoolInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.rev160622.GetResourcePoolInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.rev160622.GetResourcePoolOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.rev160622.ReleaseResourceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.rev160622.ReleaseResourceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.rev160622.ResourceManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.rev160622.ResourceTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.rev160622.ResourceTypeGroupIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.rev160622.ResourceTypeMeterIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.rev160622.ResourceTypeTableIds;
import org.opendaylight.yangtools.yang.common.RpcResult;

/**
 * Resource Manager Service Test Suite.
 *
 * @author David Suárez
 */
public class ResourceManagerTest extends AbstractConcurrentDataBrokerTest {

    public final @Rule LogRule logRule = new LogRule();
    public final @Rule LogCaptureRule logCaptureRule = new LogCaptureRule();

    public final @Rule MethodRule guice = new GuiceRule(
            new ResourceManagerTestModule());

    @Inject DataBroker dataBroker;
    @Inject ResourceManagerService resourceManager;

    private static final Long NUMBER_OF_RESOURCES = 5L;

    @SuppressWarnings("serial")
    private static final Map<String, Class<? extends ResourceTypeBase>>
        RESOURCE_TYPES = new HashMap<String, Class<? extends ResourceTypeBase>>() {
                {
                    put("Tables", ResourceTypeTableIds.class);
                    put("Meters", ResourceTypeMeterIds.class);
                    put("Groups", ResourceTypeGroupIds.class);
                }
        };

    @Test
    public void testGetAvailableResources() throws Exception {
        for (Class<? extends ResourceTypeBase> resourceType : RESOURCE_TYPES.values()) {
            RpcResult<GetAvailableResourcesOutput> result = getAvailableResources(resourceType).get();
            assertSuccessfulFutureRpcResult(result);
            assertEquals(255L, result.getResult().getTotalAvailableIdCount().longValue());
        }
    }

    private Future<RpcResult<GetAvailableResourcesOutput>> getAvailableResources(
            Class<? extends ResourceTypeBase> resourceType)
            throws Exception {
        final GetAvailableResourcesInput input = new GetAvailableResourcesInputBuilder().setResourceType(resourceType)
                .build();
        return resourceManager.getAvailableResources(input);
    }

    @Test
    public void testGetResourcePools() throws Exception {
        for (Class<? extends ResourceTypeBase> resourceType : RESOURCE_TYPES.values()) {
            RpcResult<GetResourcePoolOutput> result = getResourcePool(resourceType).get();
            assertSuccessfulFutureRpcResult(result);
            for (org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.rev160622.getresourcepool.output
                     .AvailableIds availableIds : result.getResult().getAvailableIds()) {
                assertTrue(availableIds.getStart().longValue() < availableIds.getEnd().longValue());
            }
            assertTrue(result.getResult().getDelayedResourceEntries().isEmpty());
        }
    }

    private Future<RpcResult<GetResourcePoolOutput>> getResourcePool(Class<? extends ResourceTypeBase> resourceType)
            throws Exception {
        final GetResourcePoolInput input = new GetResourcePoolInputBuilder().setResourceType(resourceType).build();
        return resourceManager.getResourcePool(input);
    }

    @Test
    public void testAllocateResource() throws Exception {
        for (Class<? extends ResourceTypeBase> resourceType : RESOURCE_TYPES.values()) {
            // Allocate resources
            RpcResult<AllocateResourceOutput> result = allocateResource(resourceType.getName(), resourceType,
                    NUMBER_OF_RESOURCES).get();
            assertSuccessfulFutureRpcResult(result);
            assertEquals(NUMBER_OF_RESOURCES, Long.valueOf(result.getResult().getIdValues().size()));

            // Release resources
            releaseResource(resourceType.getName(), resourceType);
        }
    }

    @Test(expected = NullPointerException.class)
    public void testAllocateNullResource() throws Exception {
        allocateResource(null, ResourceTypeTableIds.class, NUMBER_OF_RESOURCES).get();
    }

    private Future<RpcResult<AllocateResourceOutput>> allocateResource(String resourceKey,
            Class<? extends ResourceTypeBase> resourceType,
            Long numberOfResources) throws Exception {
        final AllocateResourceInput input = new AllocateResourceInputBuilder().setResourceType(resourceType)
                .setSize(numberOfResources).setIdKey(resourceKey).setSize(numberOfResources).build();
        return resourceManager.allocateResource(input);
    }

    @Test
    public void testReleaseResource() throws Exception {
        for (Class<? extends ResourceTypeBase> resourceType : RESOURCE_TYPES.values()) {
            // Allocate resources
            allocateResource(resourceType.getName(), resourceType, NUMBER_OF_RESOURCES);

            // Release resources
            RpcResult<Void> result = releaseResource(resourceType.getName(), resourceType).get();
            assertSuccessfulFutureRpcResult(result);
        }
    }

    @Test(expected = NullPointerException.class)
    public void testReleaseNullResource() throws Exception {
        releaseResource(null, ResourceTypeTableIds.class).get();
    }

    private Future<RpcResult<Void>> releaseResource(String resourceKey,
            Class<? extends ResourceTypeBase> resourceType) throws Exception {
        final ReleaseResourceInput input = new ReleaseResourceInputBuilder().setResourceType(resourceType)
                .setIdKey(resourceKey).build();
        return resourceManager.releaseResource(input);
    }

    private void assertSuccessfulFutureRpcResult(RpcResult<?> result)
            throws InterruptedException, ExecutionException, TimeoutException {
        assertTrue(result.isSuccessful());
        assertTrue(result.getErrors().isEmpty());
    }
}
