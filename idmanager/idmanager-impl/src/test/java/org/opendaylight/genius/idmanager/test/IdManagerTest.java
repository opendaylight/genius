/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.idmanager.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.ComparisonFailure;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.genius.datastoreutils.testutils.AsyncEventsWaiter;
import org.opendaylight.genius.datastoreutils.testutils.JobCoordinatorEventsWaiter;
import org.opendaylight.genius.datastoreutils.testutils.TestableDataTreeChangeListenerModule;
import org.opendaylight.genius.idmanager.IdUtils;
import org.opendaylight.infrautils.inject.guice.testutils.GuiceRule;
import org.opendaylight.infrautils.testutils.LogRule;
import org.opendaylight.mdsal.binding.testutils.AssertDataObjects;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.CreateIdPoolInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.CreateIdPoolInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.DeleteIdPoolInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.DeleteIdPoolInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdPools;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.ReleaseIdInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.ReleaseIdInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPool;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPoolBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPoolKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.AvailableIdsHolder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.AvailableIdsHolderBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.ReleasedIdsHolder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.ReleasedIdsHolderBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.released.ids.DelayedIdEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.released.ids.DelayedIdEntriesBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class IdManagerTest {

    private static final String TEST_KEY1 = "test-key1";
    private static final String TEST_KEY2 = "test-key2";
    private static final String ID_POOL_NAME = "test-pool";
    private static final int BLOCK_SIZE = 10;
    private static final long ID_LOW = 0l;
    private static final long ID_HIGH = 100l;

    public @Rule LogRule logRule = new LogRule();
    public @Rule MethodRule guice = new GuiceRule(IdManagerTestModule.class, TestableDataTreeChangeListenerModule.class);

    private @Inject IdManagerService idManagerService;
    private @Inject DataBroker dataBroker;
    private @Inject AsyncEventsWaiter asyncEventsWaiter;
    private @Inject JobCoordinatorEventsWaiter coordinatorEventsWaiter;
    private @Inject IdUtils idUtils;

    private SingleTransactionDataBroker singleTxdataBroker;

    @Before
    public void before() {
        singleTxdataBroker = new SingleTransactionDataBroker(dataBroker);
    }

    @Test
    public void testCreateIdPool() throws Exception {
        CreateIdPoolInput createIdPoolInput = new CreateIdPoolInputBuilder().setHigh(ID_HIGH).setLow(ID_LOW).setPoolName(ID_POOL_NAME).build();
        idManagerService.createIdPool(createIdPoolInput);
        coordinatorEventsWaiter.awaitEventsConsumption();
        validateIdPools(ExpectedCreateIdPoolObjects.idPoolCreateParent(), ExpectedCreateIdPoolObjects.idPoolCreateChild());
    }

    @Test
    public void testAllocateId() throws Exception {
        CreateIdPoolInput createIdPoolInput = new CreateIdPoolInputBuilder().setHigh(ID_HIGH).setLow(ID_LOW).setPoolName(ID_POOL_NAME).build();
        AllocateIdInput allocateIdInput = new AllocateIdInputBuilder().setIdKey(TEST_KEY1).setPoolName(ID_POOL_NAME).build();
        idManagerService.createIdPool(createIdPoolInput);
        idManagerService.allocateId(allocateIdInput);
        coordinatorEventsWaiter.awaitEventsConsumption();

        validateIdPools(ExpectedAllocateIdObjects.idPoolParent(), ExpectedAllocateIdObjects.idPoolChild());
    }

    @Test
    public void testReleaseId() throws Exception {
        CreateIdPoolInput createIdPoolInput = new CreateIdPoolInputBuilder().setHigh(ID_HIGH).setLow(ID_LOW).setPoolName(ID_POOL_NAME).build();
        AllocateIdInput allocateIdInput = new AllocateIdInputBuilder().setIdKey(TEST_KEY1).setPoolName(ID_POOL_NAME).build();
        ReleaseIdInput releaseIdInput = new ReleaseIdInputBuilder().setIdKey(TEST_KEY1).setPoolName(ID_POOL_NAME).build();
        idManagerService.createIdPool(createIdPoolInput);
        idManagerService.allocateId(allocateIdInput);
        idManagerService.releaseId(releaseIdInput);
        coordinatorEventsWaiter.awaitEventsConsumption();

        validateIdPools(ExpectedReleaseIdObjects.idPoolParent(), ExpectedReleaseIdObjects.idPoolChild());
    }

    @Test
    public void testAllocateIdBlockFromReleasedIds() throws Exception {
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        CreateIdPoolInput createIdPoolInput = new CreateIdPoolInputBuilder().setHigh(ID_HIGH).setLow(ID_LOW).setPoolName(ID_POOL_NAME).build();
        AllocateIdInput allocateIdInput = new AllocateIdInputBuilder().setIdKey(TEST_KEY1).setPoolName(ID_POOL_NAME).build();
        idManagerService.createIdPool(createIdPoolInput);
        idManagerService.allocateId(allocateIdInput);
        coordinatorEventsWaiter.awaitEventsConsumption();
        asyncEventsWaiter.awaitEventsConsumption();

        String localPoolName = idUtils.getLocalPoolName(ID_POOL_NAME);
        IdPool parentIdPool = new IdPoolBuilder().setPoolName(ID_POOL_NAME).setKey(new IdPoolKey(ID_POOL_NAME))
                .setReleasedIdsHolder(createReleaseIdHolder(Arrays.asList(1l, 2l, 3l))).build();
        IdPool childPool = new IdPoolBuilder().setPoolName(localPoolName).setKey(new IdPoolKey(localPoolName))
                .setAvailableIdsHolder(createAvailableIdHolder(0l, 9l, 9l)).build();
        tx.merge(LogicalDatastoreType.CONFIGURATION, getIdPoolIdentifier(ID_POOL_NAME), parentIdPool);
        tx.merge(LogicalDatastoreType.CONFIGURATION, getIdPoolIdentifier(localPoolName), childPool);
        tx.submit().get();

        AllocateIdInput allocateIdInput2 = new AllocateIdInputBuilder().setIdKey(TEST_KEY2).setPoolName(ID_POOL_NAME).build();
        idManagerService.allocateId(allocateIdInput2);
        coordinatorEventsWaiter.awaitEventsConsumption();
        validateIdPools(ExpectedAllocateIdFromReleasedId.idPoolParent(), ExpectedAllocateIdFromReleasedId.idPoolChild());
    }

    @Test
    public void testDeletePool() throws Exception {
        CreateIdPoolInput createIdPoolInput = new CreateIdPoolInputBuilder().setHigh(ID_HIGH).setLow(ID_LOW).setPoolName(ID_POOL_NAME).build();
        idManagerService.createIdPool(createIdPoolInput);
        DeleteIdPoolInput deleteIdPoolInput = new DeleteIdPoolInputBuilder().setPoolName(ID_POOL_NAME).build();
        idManagerService.deleteIdPool(deleteIdPoolInput);
        coordinatorEventsWaiter.awaitEventsConsumption();
        Optional<IdPool> actualIdPoolParent = singleTxdataBroker.syncReadOptional(LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.builder(IdPools.class).child(IdPool.class, new IdPoolKey(ID_POOL_NAME)).build());
        Optional<IdPool> actualIdPoolChild = singleTxdataBroker.syncReadOptional(LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.builder(IdPools.class).child(IdPool.class, new IdPoolKey(idUtils.getLocalPoolName(ID_POOL_NAME))).build());
        assertEquals(false, actualIdPoolParent.isPresent());
        assertEquals(false, actualIdPoolChild.isPresent());
    }

    @Test
    public void testMultithreadedIdAllocationFromAvailableIds() throws Exception {
        CreateIdPoolInput createIdPoolInput = new CreateIdPoolInputBuilder().setHigh(ID_HIGH).setLow(ID_LOW).setPoolName(ID_POOL_NAME).build();
        idManagerService.createIdPool(createIdPoolInput);
        requestIdsConcurrently(false);
        coordinatorEventsWaiter.awaitEventsConsumption();

        validateIdPools(ExpectedAllocateIdMultipleRequestsFromAvailableIds.idPoolParent(), ExpectedAllocateIdMultipleRequestsFromAvailableIds.idPoolChild());
    }

    @Test
    public void testMultithreadedIdAllocationFromReleaseIds() throws Exception {
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        CreateIdPoolInput createIdPoolInput = new CreateIdPoolInputBuilder().setHigh(ID_HIGH).setLow(ID_LOW).setPoolName(ID_POOL_NAME).build();
        AllocateIdInput allocateIdInput = new AllocateIdInputBuilder().setIdKey(TEST_KEY1).setPoolName(ID_POOL_NAME).build();
        idManagerService.createIdPool(createIdPoolInput);
        idManagerService.allocateId(allocateIdInput);
        coordinatorEventsWaiter.awaitEventsConsumption();
        asyncEventsWaiter.awaitEventsConsumption();

        String localPoolName = idUtils.getLocalPoolName(ID_POOL_NAME);
        IdPool parentIdPool = new IdPoolBuilder().setPoolName(ID_POOL_NAME).setKey(new IdPoolKey(ID_POOL_NAME))
                .setReleasedIdsHolder(createReleaseIdHolder(Arrays.asList(1l, 2l, 3l))).build();
        IdPool childPool = new IdPoolBuilder().setPoolName(localPoolName).setKey(new IdPoolKey(localPoolName))
                .setAvailableIdsHolder(createAvailableIdHolder(0l, 9l, 9l)).build();
        tx.merge(LogicalDatastoreType.CONFIGURATION, getIdPoolIdentifier(ID_POOL_NAME), parentIdPool);
        tx.merge(LogicalDatastoreType.CONFIGURATION, getIdPoolIdentifier(localPoolName), childPool);
        tx.submit().get();
        requestIdsConcurrently(false);
        coordinatorEventsWaiter.awaitEventsConsumption();

        validateIdPools(ExpectedAllocateIdMultipleRequestsFromReleaseIds.idPoolParent(), ExpectedAllocateIdMultipleRequestsFromReleaseIds.idPoolChild());
    }

    @Test
    public void testMultithreadedIdAllocationForSameKeyFromAvailableIds() throws Exception {
        CreateIdPoolInput createIdPoolInput = new CreateIdPoolInputBuilder().setHigh(ID_HIGH).setLow(ID_LOW).setPoolName(ID_POOL_NAME).build();
        idManagerService.createIdPool(createIdPoolInput);
        requestIdsConcurrently(true);
        coordinatorEventsWaiter.awaitEventsConsumption();

        validateIdPools(ExpectedAllocateIdObjects.idPoolParent(), ExpectedAllocateIdObjects.idPoolChild());
    }

    @Test
    public void testMultithreadedIdAllocationForSameKeyFromReleasedIds() throws Exception {
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        CreateIdPoolInput createIdPoolInput = new CreateIdPoolInputBuilder().setHigh(ID_HIGH).setLow(ID_LOW).setPoolName(ID_POOL_NAME).build();
        AllocateIdInput allocateIdInput = new AllocateIdInputBuilder().setIdKey(TEST_KEY1).setPoolName(ID_POOL_NAME).build();
        idManagerService.createIdPool(createIdPoolInput);
        idManagerService.allocateId(allocateIdInput);
        coordinatorEventsWaiter.awaitEventsConsumption();
        asyncEventsWaiter.awaitEventsConsumption();

        String localPoolName = idUtils.getLocalPoolName(ID_POOL_NAME);
        IdPool parentIdPool = new IdPoolBuilder().setPoolName(ID_POOL_NAME).setKey(new IdPoolKey(ID_POOL_NAME))
                .setReleasedIdsHolder(createReleaseIdHolder(Arrays.asList(1l, 2l, 3l))).build();
        IdPool childPool = new IdPoolBuilder().setPoolName(localPoolName).setKey(new IdPoolKey(localPoolName))
                .setAvailableIdsHolder(createAvailableIdHolder(0l, 9l, 9l)).build();
        tx.merge(LogicalDatastoreType.CONFIGURATION, getIdPoolIdentifier(ID_POOL_NAME), parentIdPool);
        tx.merge(LogicalDatastoreType.CONFIGURATION, getIdPoolIdentifier(localPoolName), childPool);
        tx.submit().get();
        requestIdsConcurrently(true);
        coordinatorEventsWaiter.awaitEventsConsumption();

        validateIdPools(ExpectedAllocateIdFromReleasedId.idPoolParent(), ExpectedAllocateIdFromReleasedId.idPoolChild());
    }

    private void requestIdsConcurrently(boolean isSameKey) throws InterruptedException {
        int numberOfTasks = 3;
        CountDownLatch latch = new CountDownLatch(numberOfTasks);

        ExecutorService executor = Executors.newCachedThreadPool();
        for (int i = 0; i < numberOfTasks; i++) {
            final String idKey;
            if (isSameKey) {
                idKey = TEST_KEY2;
            } else {
                idKey = TEST_KEY1 + i;
            }
            executor.execute(() -> {
                Future<RpcResult<AllocateIdOutput>> result;
                result = idManagerService.allocateId(
                        new AllocateIdInputBuilder().setPoolName(ID_POOL_NAME).setIdKey(idKey).build());
                try {
                    if (result.get().isSuccessful()) {
                        Long idValue = result.get().getResult().getIdValue();
                        assertTrue(idValue <= ID_LOW + BLOCK_SIZE);
                    } else {
                        RpcError error = result.get().getErrors().iterator().next();
                        assertTrue(error.getCause().getMessage().contains("Ids exhausted for pool : " + ID_POOL_NAME));
                    }
                } catch (ExecutionException | InterruptedException e) {
                    assertTrue(e.getCause().getMessage(), false);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
    }

    private InstanceIdentifier<IdPool> getIdPoolIdentifier(String poolName) {
        InstanceIdentifier.InstanceIdentifierBuilder<IdPool> idBuilder =
                InstanceIdentifier.builder(IdPools.class).child(IdPool.class, new IdPoolKey(poolName));
        InstanceIdentifier<IdPool> id = idBuilder.build();
        return id;
    }

    private ReleasedIdsHolder createReleaseIdHolder(List<Long> delayedIds) {
        List<DelayedIdEntries> delayedIdEntries = new ArrayList<>();
        for (Long id : delayedIds) {
            delayedIdEntries.add(new DelayedIdEntriesBuilder().setId(id).setReadyTimeSec(0l).build());
        }
        return new ReleasedIdsHolderBuilder().setDelayedIdEntries(delayedIdEntries).setAvailableIdCount(Long.valueOf(delayedIds.size())).build();
    }

    private AvailableIdsHolder createAvailableIdHolder(long start, long end, long cursor) {
        return new AvailableIdsHolderBuilder().setStart(start).setEnd(end).setCursor(cursor).build();
    }

    private void validateIdPools(IdPool expectedIdPoolParent, IdPool expectedIdPoolChild) throws ReadFailedException, ComparisonFailure {
        IdPool actualIdPoolParent = singleTxdataBroker.syncRead(LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.builder(IdPools.class).child(IdPool.class, new IdPoolKey(ID_POOL_NAME)).build());
        String localPoolName = idUtils.getLocalPoolName(ID_POOL_NAME);
        IdPool actualIdPoolChild = singleTxdataBroker.syncRead(LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.builder(IdPools.class).child(IdPool.class, new IdPoolKey(localPoolName)).build());
        AssertDataObjects.assertEqualBeans(expectedIdPoolParent.getAvailableIdsHolder(), actualIdPoolParent.getAvailableIdsHolder());
        AssertDataObjects.assertEqualBeans(expectedIdPoolParent.getReleasedIdsHolder(), actualIdPoolParent.getReleasedIdsHolder());
        AssertDataObjects.assertEqualBeans(expectedIdPoolChild.getAvailableIdsHolder(), actualIdPoolChild.getAvailableIdsHolder());
        if (actualIdPoolChild.getReleasedIdsHolder().getDelayedIdEntries() == null) {
            AssertDataObjects.assertEqualBeans(expectedIdPoolChild.getReleasedIdsHolder(), actualIdPoolChild.getReleasedIdsHolder());
        } else {
            assertEquals(expectedIdPoolChild.getReleasedIdsHolder().getDelayedTimeSec(),
                    actualIdPoolChild.getReleasedIdsHolder().getDelayedTimeSec());
            assertEquals(expectedIdPoolChild.getReleasedIdsHolder().getAvailableIdCount(),
                    actualIdPoolChild.getReleasedIdsHolder().getAvailableIdCount());
            assertEquals(expectedIdPoolChild.getReleasedIdsHolder().getDelayedIdEntries().size(),
                    actualIdPoolChild.getReleasedIdsHolder().getDelayedIdEntries().size());
        }
        if (actualIdPoolParent.getIdEntries() != null) {
            assertEquals(expectedIdPoolParent.getIdEntries().size(), actualIdPoolParent.getIdEntries().size());
        }
        assertNotNull(actualIdPoolParent);
        assertNotNull(actualIdPoolChild);
        assertEquals(localPoolName, actualIdPoolParent.getChildPools().get(0).getChildPoolName());
        assertEquals(ID_POOL_NAME, actualIdPoolChild.getParentPoolName());
        assertNull(actualIdPoolParent.getParentPoolName());
        assertNull(actualIdPoolChild.getChildPools());
    }
}
