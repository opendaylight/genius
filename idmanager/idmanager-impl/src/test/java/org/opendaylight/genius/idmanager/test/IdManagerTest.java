/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.idmanager.test;

import static java.util.Comparator.comparing;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.inject.Inject;
import junit.framework.AssertionFailedError;
import org.junit.Before;
import org.junit.ComparisonFailure;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.genius.datastoreutils.testutils.AsyncEventsWaiter;
import org.opendaylight.genius.datastoreutils.testutils.JobCoordinatorEventsWaiter;
import org.opendaylight.genius.datastoreutils.testutils.JobCoordinatorTestModule;
import org.opendaylight.genius.datastoreutils.testutils.TestableDataTreeChangeListenerModule;
import org.opendaylight.genius.idmanager.IdUtils;
import org.opendaylight.infrautils.inject.guice.testutils.GuiceRule;
import org.opendaylight.infrautils.testutils.LogCaptureRule;
import org.opendaylight.infrautils.testutils.LogRule;
import org.opendaylight.infrautils.utils.concurrent.Executors;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.binding.testutils.AssertDataObjects;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.ReadFailedException;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.ReleaseIdOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPool;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPoolBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPoolKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.AvailableIdsHolder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.AvailableIdsHolderBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.ChildPools;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.ChildPoolsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.IdEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.ReleasedIdsHolder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.ReleasedIdsHolderBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.released.ids.DelayedIdEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.released.ids.DelayedIdEntriesBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class IdManagerTest {

    private static final Logger LOG = LoggerFactory.getLogger(IdManagerTest.class);

    private static final String TEST_KEY1 = "test-key1";
    private static final String TEST_KEY2 = "test-key2";
    private static final String ID_POOL_NAME = "test-pool";
    private static final int BLOCK_SIZE = 10;
    private static final long ID_LOW = 0L;
    private static final long ID_HIGH = 100L;

    // public static @ClassRule RunUntilFailureClassRule classRepeater = new RunUntilFailureClassRule();
    // public @Rule RunUntilFailureRule repeater = new RunUntilFailureRule(classRepeater);

    public @Rule LogRule logRule = new LogRule();
    public @Rule LogCaptureRule logCaptureRule = new LogCaptureRule();

    public @Rule MethodRule guice = new GuiceRule(IdManagerTestModule.class,
            TestableDataTreeChangeListenerModule.class, JobCoordinatorTestModule.class);

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
        CreateIdPoolInput createIdPoolInput = new CreateIdPoolInputBuilder().setHigh(ID_HIGH).setLow(ID_LOW)
                .setPoolName(ID_POOL_NAME).build();
        assertTrue(idManagerService.createIdPool(createIdPoolInput).get().isSuccessful());
        coordinatorEventsWaiter.awaitEventsConsumption();
        validateIdPools(ExpectedCreateIdPoolObjects.idPoolCreateParent(),
                ExpectedCreateIdPoolObjects.idPoolCreateChild());
    }

    @Test
    public void testAllocateId() throws Exception {
        CreateIdPoolInput createIdPoolInput = new CreateIdPoolInputBuilder().setHigh(ID_HIGH).setLow(ID_LOW)
                .setPoolName(ID_POOL_NAME).build();
        AllocateIdInput allocateIdInput = new AllocateIdInputBuilder().setIdKey(TEST_KEY1).setPoolName(ID_POOL_NAME)
                .build();
        idManagerService.createIdPool(createIdPoolInput);
        assertEquals(idManagerService.allocateId(allocateIdInput).get().getResult().getIdValue().longValue(), 0L);
        coordinatorEventsWaiter.awaitEventsConsumption();

        validateIdPools(ExpectedAllocateIdObjects.idPoolParent(), ExpectedAllocateIdObjects.idPoolChild());
    }

    @Test
    public void testReleaseId() throws Exception {
        CreateIdPoolInput createIdPoolInput = new CreateIdPoolInputBuilder().setHigh(ID_HIGH).setLow(ID_LOW)
                .setPoolName(ID_POOL_NAME).build();
        AllocateIdInput allocateIdInput = new AllocateIdInputBuilder().setIdKey(TEST_KEY1).setPoolName(ID_POOL_NAME)
                .build();
        ReleaseIdInput releaseIdInput = new ReleaseIdInputBuilder().setIdKey(TEST_KEY1).setPoolName(ID_POOL_NAME)
                .build();
        idManagerService.createIdPool(createIdPoolInput);
        Future<RpcResult<AllocateIdOutput>> allocateIdResult = idManagerService.allocateId(allocateIdInput);
        assertTrue(allocateIdResult.get().isSuccessful());
        Future<RpcResult<ReleaseIdOutput>> result = idManagerService.releaseId(releaseIdInput);
        assertTrue(result.get().isSuccessful());
        assertEquals(allocateIdResult.get().getResult().getIdValue(), result.get().getResult().getIdValues().get(0));
        coordinatorEventsWaiter.awaitEventsConsumption();

        validateIdPools(ExpectedReleaseIdObjects.idPoolParent(), ExpectedReleaseIdObjects.idPoolChild());
    }

    @Test
    public void testAllocateIdBlockFromReleasedIds() throws Exception {
        CreateIdPoolInput createIdPoolInput = new CreateIdPoolInputBuilder().setHigh(ID_HIGH).setLow(ID_LOW)
                .setPoolName(ID_POOL_NAME).build();
        AllocateIdInput allocateIdInput = new AllocateIdInputBuilder().setIdKey(TEST_KEY2).setPoolName(ID_POOL_NAME)
                .build();
        idManagerService.createIdPool(createIdPoolInput);
        idManagerService.allocateId(allocateIdInput);
        coordinatorEventsWaiter.awaitEventsConsumption();
        asyncEventsWaiter.awaitEventsConsumption();

        String localPoolName = idUtils.getLocalPoolName(ID_POOL_NAME);
        IdPool parentIdPool = new IdPoolBuilder().setPoolName(ID_POOL_NAME).withKey(new IdPoolKey(ID_POOL_NAME))
                .setAvailableIdsHolder(createAvailableIdHolder(ID_LOW, ID_HIGH, ID_HIGH + 1))
                .setReleasedIdsHolder(createReleaseIdHolder(Arrays.asList(1L, 2L, 3L))).build();
        IdPool childPool = new IdPoolBuilder().setPoolName(localPoolName).withKey(new IdPoolKey(localPoolName))
                .setAvailableIdsHolder(createAvailableIdHolder(0L, 9L, 10L)).build();
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.merge(LogicalDatastoreType.CONFIGURATION, getIdPoolIdentifier(ID_POOL_NAME), parentIdPool);
        tx.merge(LogicalDatastoreType.CONFIGURATION, getIdPoolIdentifier(localPoolName), childPool);
        tx.commit().get();

        AllocateIdInput allocateIdInput2 = new AllocateIdInputBuilder().setIdKey(TEST_KEY1).setPoolName(ID_POOL_NAME)
                .build();
        assertEquals(idManagerService.allocateId(allocateIdInput2).get().getResult().getIdValue().longValue(), 1L);

        coordinatorEventsWaiter.awaitEventsConsumption();
        validateIdPools(ExpectedAllocateIdFromReleasedId.idPoolParent(),
                ExpectedAllocateIdFromReleasedId.idPoolChild());
    }

    @Test
    public void testDeletePool() throws Exception {
        CreateIdPoolInput createIdPoolInput = new CreateIdPoolInputBuilder().setHigh(ID_HIGH).setLow(ID_LOW)
                .setPoolName(ID_POOL_NAME).build();
        idManagerService.createIdPool(createIdPoolInput);
        DeleteIdPoolInput deleteIdPoolInput = new DeleteIdPoolInputBuilder().setPoolName(ID_POOL_NAME).build();
        assertTrue(idManagerService.deleteIdPool(deleteIdPoolInput).get().isSuccessful());
        coordinatorEventsWaiter.awaitEventsConsumption();
        Optional<IdPool> actualIdPoolParent = singleTxdataBroker.syncReadOptional(LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.builder(IdPools.class).child(IdPool.class, new IdPoolKey(ID_POOL_NAME)).build());
        Optional<IdPool> actualIdPoolChild = singleTxdataBroker.syncReadOptional(LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.builder(IdPools.class)
                        .child(IdPool.class, new IdPoolKey(idUtils.getLocalPoolName(ID_POOL_NAME))).build());
        assertFalse(actualIdPoolParent.isPresent());
        assertFalse(actualIdPoolChild.isPresent());
    }

    @Test
    @SuppressWarnings("checkstyle:IllegalThrows") // OK as exceptionInExecutor can't be Exception & AssertionFailedError
    public void testMultithreadedIdAllocationFromAvailableIds() throws Throwable {
        CreateIdPoolInput createIdPoolInput = new CreateIdPoolInputBuilder().setHigh(ID_HIGH).setLow(ID_LOW)
                .setPoolName(ID_POOL_NAME).build();
        idManagerService.createIdPool(createIdPoolInput);
        requestIdsConcurrently(false);
        coordinatorEventsWaiter.awaitEventsConsumption();
        IdPool actualIdPoolChild = getUpdatedActualChildPool();
        IdPool actualIdPoolParent = getUpdatedActualParentPool();
        // Cannot compare the idEntries since we cannot guarantee which idKey gets what value.
        // However the allocated id values uniqueness is verified in requestIdsConcurrently method.
        AssertDataObjects.assertEqualBeans(
                ExpectedAllocateIdMultipleRequestsFromAvailableIds.idPoolParent().getAvailableIdsHolder(),
                actualIdPoolParent.getAvailableIdsHolder());
        AssertDataObjects.assertEqualBeans(ExpectedAllocateIdMultipleRequestsFromAvailableIds.idPoolChild(),
                actualIdPoolChild);
    }

    @Test
    @SuppressWarnings("checkstyle:IllegalThrows") // OK as exceptionInExecutor can't be Exception & AssertionFailedError
    public void testMultithreadedIdAllocationFromReleaseIds() throws Throwable {
        CreateIdPoolInput createIdPoolInput = new CreateIdPoolInputBuilder().setHigh(ID_HIGH).setLow(ID_LOW)
                .setPoolName(ID_POOL_NAME).build();
        AllocateIdInput allocateIdInput = new AllocateIdInputBuilder().setIdKey(TEST_KEY1).setPoolName(ID_POOL_NAME)
                .build();
        idManagerService.createIdPool(createIdPoolInput);
        idManagerService.allocateId(allocateIdInput);
        // Should wait for all job to complete.
        coordinatorEventsWaiter.awaitEventsConsumption();

        String localPoolName = idUtils.getLocalPoolName(ID_POOL_NAME);
        IdPool parentIdPool = new IdPoolBuilder().setPoolName(ID_POOL_NAME).withKey(new IdPoolKey(ID_POOL_NAME))
                .setReleasedIdsHolder(createReleaseIdHolder(Collections.emptyList())).build();
        IdPool childPool = new IdPoolBuilder().setPoolName(localPoolName).withKey(new IdPoolKey(localPoolName))
                .setReleasedIdsHolder(createReleaseIdHolder(Arrays.asList(1L, 2L, 3L)))
                .setAvailableIdsHolder(createAvailableIdHolder(0L, 9L, 10L)).build();
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.merge(LogicalDatastoreType.CONFIGURATION, getIdPoolIdentifier(ID_POOL_NAME), parentIdPool);
        tx.merge(LogicalDatastoreType.CONFIGURATION, getIdPoolIdentifier(localPoolName), childPool);
        tx.commit().get();
        // Wait for the changes to be available on the caches.
        asyncEventsWaiter.awaitEventsConsumption();
        requestIdsConcurrently(false);
        coordinatorEventsWaiter.awaitEventsConsumption();
        asyncEventsWaiter.awaitEventsConsumption();

        IdPool actualIdPoolChild = getUpdatedActualChildPool();
        IdPool actualIdPoolParent = getUpdatedActualParentPool();
        // Cannot compare the idEntries since we cannot guarantee which idKey gets what value.
        // However the allocated id values uniqueness is verified in requestIdsConcurrently method.
        AssertDataObjects.assertEqualBeans(
                ExpectedAllocateIdMultipleRequestsFromReleaseIds.idPoolParent().getReleasedIdsHolder(),
                actualIdPoolParent.getReleasedIdsHolder());
        AssertDataObjects.assertEqualBeans(ExpectedAllocateIdMultipleRequestsFromReleaseIds.idPoolChild(),
                actualIdPoolChild);
    }

    @Test
    @SuppressWarnings("checkstyle:IllegalThrows") // OK as exceptionInExecutor can't be Exception & AssertionFailedError
    public void testMultithreadedIdAllocationForSameKeyFromAvailableIds() throws Throwable {
        CreateIdPoolInput createIdPoolInput = new CreateIdPoolInputBuilder().setHigh(ID_HIGH).setLow(ID_LOW)
                .setPoolName(ID_POOL_NAME).build();
        idManagerService.createIdPool(createIdPoolInput);
        requestIdsConcurrently(true);
        coordinatorEventsWaiter.awaitEventsConsumption();

        validateIdPools(ExpectedAllocateIdObjects.idPoolParent(), ExpectedAllocateIdObjects.idPoolChild());
    }

    @Test
    @SuppressWarnings("checkstyle:IllegalThrows") // OK as exceptionInExecutor can't be Exception & AssertionFailedError
    public void testMultithreadedIdAllocationForSameKeyFromReleasedIds() throws Throwable {
        CreateIdPoolInput createIdPoolInput = new CreateIdPoolInputBuilder().setHigh(ID_HIGH).setLow(ID_LOW)
                .setPoolName(ID_POOL_NAME).build();
        AllocateIdInput allocateIdInput = new AllocateIdInputBuilder().setIdKey(TEST_KEY2).setPoolName(ID_POOL_NAME)
                .build();
        idManagerService.createIdPool(createIdPoolInput);
        idManagerService.allocateId(allocateIdInput);
        coordinatorEventsWaiter.awaitEventsConsumption();
        asyncEventsWaiter.awaitEventsConsumption();

        String localPoolName = idUtils.getLocalPoolName(ID_POOL_NAME);
        IdPool parentIdPool = new IdPoolBuilder().setPoolName(ID_POOL_NAME).withKey(new IdPoolKey(ID_POOL_NAME))
                .setAvailableIdsHolder(createAvailableIdHolder(ID_LOW, ID_HIGH, ID_HIGH + 1))
                .setReleasedIdsHolder(createReleaseIdHolder(Collections.emptyList())).build();
        IdPool childPool = new IdPoolBuilder().setPoolName(localPoolName).withKey(new IdPoolKey(localPoolName))
                .setReleasedIdsHolder(createReleaseIdHolder(Arrays.asList(1L, 2L, 3L)))
                .setAvailableIdsHolder(createAvailableIdHolder(0L, 9L, 10L)).build();
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.merge(LogicalDatastoreType.CONFIGURATION, getIdPoolIdentifier(ID_POOL_NAME), parentIdPool);
        tx.merge(LogicalDatastoreType.CONFIGURATION, getIdPoolIdentifier(localPoolName), childPool);
        tx.commit().get();
        requestIdsConcurrently(true);
        coordinatorEventsWaiter.awaitEventsConsumption();

        validateIdPools(ExpectedAllocateIdFromReleasedId.idPoolParent(),
                ExpectedAllocateIdFromReleasedId.idPoolChild());
    }

    @SuppressWarnings("checkstyle:IllegalThrows") // OK as exceptionInExecutor can't be Exception & AssertionFailedError
    private void requestIdsConcurrently(boolean isSameKey) throws Throwable {
        int numberOfTasks = 3;
        CountDownLatch latch = new CountDownLatch(numberOfTasks);
        Set<Long> idSet = Sets.newConcurrentHashSet();
        ExecutorService executor = Executors.newCachedThreadPool("requestIdsConcurrently()", LOG);
        AtomicReference<Throwable> exceptionInExecutorAtomic = new AtomicReference<>();
        for (int i = 0; i < numberOfTasks; i++) {
            final String idKey;
            if (isSameKey) {
                idKey = TEST_KEY1;
            } else {
                idKey = TEST_KEY1 + i;
            }
            executor.execute(() -> {
                // Any exception thrown inside this background thread will not cause the test to fail
                // so you cannot use assert* here but must set the exceptionInExecutor which is checked after
                Future<RpcResult<AllocateIdOutput>> result;
                result = idManagerService.allocateId(
                        new AllocateIdInputBuilder().setPoolName(ID_POOL_NAME).setIdKey(idKey).build());
                try {
                    if (result.get().isSuccessful()) {
                        Long idValue = result.get().getResult().getIdValue().toJava();
                        idSet.add(idValue);
                        if (idValue > ID_LOW + BLOCK_SIZE) {
                            exceptionInExecutorAtomic.set(new AssertionFailedError("idValue <= ID_LOW + BLOCK_SIZE"));
                        }
                    } else {
                        RpcError error = result.get().getErrors().iterator().next();
                        if (!error.getCause().getMessage().contains("Ids exhausted for pool : " + ID_POOL_NAME)) {
                            exceptionInExecutorAtomic.set(error.getCause());
                        }
                    }
                } catch (InterruptedException | ExecutionException e) {
                    exceptionInExecutorAtomic.set(e);
                } finally {
                    latch.countDown();
                }
            });
        }
        if (!latch.await(13, SECONDS)) {
            fail("latch.await(13, SECONDS) timed out :(");
        }
        Throwable exceptionInExecutor = exceptionInExecutorAtomic.get();
        if (exceptionInExecutor != null) {
            throw exceptionInExecutor;
        }
        if (isSameKey) {
            assertEquals(1, idSet.size());
        } else {
            assertEquals(numberOfTasks, idSet.size());
        }
    }

    private InstanceIdentifier<IdPool> getIdPoolIdentifier(String poolName) {
        InstanceIdentifier.InstanceIdentifierBuilder<IdPool> idBuilder =
                InstanceIdentifier.builder(IdPools.class).child(IdPool.class, new IdPoolKey(poolName));
        return idBuilder.build();
    }

    private ReleasedIdsHolder createReleaseIdHolder(List<Long> delayedIds) {
        List<DelayedIdEntries> delayedIdEntries = new ArrayList<>();
        for (Long id : delayedIds) {
            delayedIdEntries.add(new DelayedIdEntriesBuilder().setId(id).setReadyTimeSec(0L).build());
        }
        return new ReleasedIdsHolderBuilder().setDelayedIdEntries(delayedIdEntries)
                .setAvailableIdCount((long) delayedIds.size()).build();
    }

    private AvailableIdsHolder createAvailableIdHolder(long start, long end, long cursor) {
        return new AvailableIdsHolderBuilder().setStart(start).setEnd(end).setCursor(cursor).build();
    }

    private void validateIdPools(IdPool expectedIdPoolParent, IdPool expectedIdPoolChild)
            throws ReadFailedException, ComparisonFailure {
        IdPool actualIdPoolParent = getUpdatedActualParentPool();
        IdPool actualIdPoolChild = getUpdatedActualChildPool();

        assertNotNull(actualIdPoolParent);
        assertNotNull(actualIdPoolChild);
        AssertDataObjects.assertEqualBeans(expectedIdPoolParent, actualIdPoolParent);
        AssertDataObjects.assertEqualBeans(expectedIdPoolChild, actualIdPoolChild);

    }

    private IdPool getUpdatedActualChildPool() throws ReadFailedException {
        String localPoolName = idUtils.getLocalPoolName(ID_POOL_NAME);
        IdPool idPoolChildFromDS = singleTxdataBroker.syncRead(LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.builder(IdPools.class).child(IdPool.class, new IdPoolKey(localPoolName)).build());
        List<DelayedIdEntries> actualDelayedIdEntries = idPoolChildFromDS.getReleasedIdsHolder().getDelayedIdEntries();
        IdPool actualIdPoolChild = idPoolChildFromDS;
        if (actualDelayedIdEntries != null) {
            List<DelayedIdEntries> updatedDelayedIdEntries = actualDelayedIdEntries.stream()
                    .map(delayedIdEntry -> new DelayedIdEntriesBuilder().setId(delayedIdEntry.getId())
                            .setReadyTimeSec(0L).build())
                    .collect(Collectors.toList());
            ReleasedIdsHolder releasedId = new ReleasedIdsHolderBuilder(idPoolChildFromDS.getReleasedIdsHolder())
                    .setDelayedIdEntries(updatedDelayedIdEntries).build();
            actualIdPoolChild = new IdPoolBuilder(idPoolChildFromDS).setReleasedIdsHolder(releasedId).build();
        }
        return actualIdPoolChild;
    }

    private IdPool getUpdatedActualParentPool() throws ReadFailedException {
        IdPool idPoolParentFromDS = singleTxdataBroker.syncRead(LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.builder(IdPools.class).child(IdPool.class, new IdPoolKey(ID_POOL_NAME)).build());
        List<ChildPools> childPool = idPoolParentFromDS.nonnullChildPools();
        List<ChildPools> updatedChildPool = childPool.stream()
                .map(child -> new ChildPoolsBuilder(child).setLastAccessTime(0L).build()).collect(Collectors.toList());
        List<IdEntries> idEntries = idPoolParentFromDS.getIdEntries();
        IdPoolBuilder idPoolBuilder = new IdPoolBuilder(idPoolParentFromDS);
        if (idEntries != null) {
            List<IdEntries> sortedIdEntries = idEntries.stream().sorted(comparing(IdEntries::getIdKey))
                    .collect(Collectors.toList());
            idPoolBuilder.setIdEntries(sortedIdEntries);
        }
        return idPoolBuilder.setChildPools(updatedChildPool).build();
    }
}
