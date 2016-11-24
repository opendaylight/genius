/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.idmanager.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.genius.idmanager.IdManager;
import org.opendaylight.genius.idmanager.IdUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.CreateIdPoolInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.CreateIdPoolInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.DeleteIdPoolInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.DeleteIdPoolInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdPools;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdPoolsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.ReleaseIdInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.ReleaseIdInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPool;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPoolBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPoolKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.AvailableIdsHolder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.AvailableIdsHolderBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.ChildPools;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.ChildPoolsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.ChildPoolsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.IdEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.IdEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.IdEntriesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.ReleasedIdsHolder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.ReleasedIdsHolderBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.released.ids.DelayedIdEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.released.ids.DelayedIdEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.LockInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.LockManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.UnlockInput;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

@RunWith(MockitoJUnitRunner.class)
public class IdManagerTest {

    Map<InstanceIdentifier<?>, DataObject> configDataStore = new HashMap<>();
    @Mock DataBroker dataBroker;
    @Mock ReadOnlyTransaction mockReadTx;
    @Mock WriteTransaction mockWriteTx;
    @Mock LockManagerService lockManager;
    Future<RpcResult<Void>> rpcResult;
    IdUtils idUtils;
    IdManager idManager;
    IdPool globalIdPool;
    String allocateIdPoolName = "allocateIdTest";
    InstanceIdentifier<IdPool> parentPoolIdentifier;
    InstanceIdentifier<IdPool> localPoolIdentifier;
    InstanceIdentifier<ChildPools> childPoolIdentifier;
    final String poolName = "test-pool";
    int idStart = 100;
    int idEnd = 200;
    int blockSize = 2;
    String idKey = "test-key";
    String localPoolName;
    long idValue = 2;

    @Before
    public void setUp() throws Exception {
        idUtils = new IdUtils();
        localPoolName = idUtils.getLocalPoolName(poolName);

        parentPoolIdentifier = buildInstanceIdentifier(poolName);
        localPoolIdentifier = buildInstanceIdentifier(localPoolName);
        childPoolIdentifier = buildChildPoolInstanceIdentifier(poolName, localPoolName);
    }

    private void setupMocks(List<IdPool> idPools) throws ReadFailedException, UnknownHostException {
        when(dataBroker.newReadOnlyTransaction()).thenReturn(mockReadTx);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(mockWriteTx);
        when(lockManager.lock(any(LockInput.class)))
                .thenReturn(Futures.immediateFuture(RpcResultBuilder.<Void>success().build()));
        when(lockManager.unlock(any(UnlockInput.class)))
                .thenReturn(Futures.immediateFuture(RpcResultBuilder.<Void>success().build()));
        doReturn(Futures.immediateCheckedFuture(null)).when(mockWriteTx).submit();
        doAnswer(invocation -> {
            configDataStore.put(invocation.getArgumentAt(1, KeyedInstanceIdentifier.class),
                    invocation.getArgumentAt(2, IdPool.class));
            return null;
        }).when(mockWriteTx).put(eq(LogicalDatastoreType.CONFIGURATION), Matchers.any(), any(IdPool.class), eq(true));
        doAnswer(invocation -> {
            configDataStore.put(invocation.getArgumentAt(1, KeyedInstanceIdentifier.class),
                    invocation.getArgumentAt(2, IdPool.class));
            return null;
        }).when(mockWriteTx).merge(eq(LogicalDatastoreType.CONFIGURATION), Matchers.any(), any(ChildPools.class),
                eq(true));
        doAnswer(invocation -> {
            configDataStore.put(invocation.getArgumentAt(1, KeyedInstanceIdentifier.class),
                    invocation.getArgumentAt(2, IdPool.class));
            return null;
        }).when(mockWriteTx).merge(eq(LogicalDatastoreType.CONFIGURATION), Matchers.any(), any(IdPool.class), eq(true));
        doAnswer(invocation -> {
            configDataStore.put(invocation.getArgumentAt(1, KeyedInstanceIdentifier.class), null);
            return null;
        }).when(mockWriteTx).delete(eq(LogicalDatastoreType.CONFIGURATION), Matchers.<InstanceIdentifier<IdPool>>any());

        doReturn(Futures.immediateCheckedFuture(Optional.absent())).when(mockReadTx)
                .read(eq(LogicalDatastoreType.CONFIGURATION), anyObject());
        if (idPools != null && !idPools.isEmpty()) {
            Optional<IdPools> optionalIdPools = Optional.of(new IdPoolsBuilder().setIdPool(idPools).build());
            doReturn(Futures.immediateCheckedFuture(optionalIdPools)).when(mockReadTx)
                    .read(LogicalDatastoreType.CONFIGURATION, idUtils.getIdPools());
        }
        idManager = new IdManager(dataBroker, new SingleTransactionDataBroker(dataBroker), lockManager, idUtils);
    }

    @Test
    public void testCreateIdPool() throws Exception {
        setupMocks(null);
        CreateIdPoolInput createPoolTest = buildCreateIdPool(poolName, idStart, idEnd);
        long expectedBlockSize = idUtils.computeBlockSize(idStart, idEnd);

        Future<RpcResult<Void>> result = idManager.createIdPool(createPoolTest);
        IdPool pool;
        assertTrue(result.get().isSuccessful());
        // Just to ensure the local pool is also written. Even if it is not triggered Test case will pass.
        Thread.sleep(100);
        assertTrue(configDataStore.size() > 0);
        DataObject dataObject = configDataStore.get(localPoolIdentifier);
        if (dataObject instanceof IdPool) {
            pool = (IdPool) dataObject;
            assertEquals(localPoolName, pool.getPoolName());
            assertEquals(createPoolTest.getPoolName(), pool.getParentPoolName());
            assertEquals(idStart, pool.getAvailableIdsHolder().getStart().intValue());
            assertEquals(idStart + expectedBlockSize - 1, pool.getAvailableIdsHolder().getEnd().intValue());
            assertEquals(idStart - 1, pool.getAvailableIdsHolder().getCursor().intValue());
            assertEquals(30, pool.getReleasedIdsHolder().getDelayedTimeSec().longValue());
            assertEquals(0, pool.getReleasedIdsHolder().getAvailableIdCount().longValue());
            assertEquals(expectedBlockSize, pool.getBlockSize().longValue());
        }
        dataObject = configDataStore.get(parentPoolIdentifier);
        if (dataObject instanceof IdPool) {
            pool = (IdPool) dataObject;
            assertEquals(createPoolTest.getPoolName(), pool.getPoolName());
            assertEquals(0, pool.getReleasedIdsHolder().getDelayedTimeSec().longValue());
            assertEquals(0, pool.getReleasedIdsHolder().getAvailableIdCount().longValue());
            assertEquals(createPoolTest.getLow(), pool.getAvailableIdsHolder().getStart());
            assertEquals(createPoolTest.getHigh(), pool.getAvailableIdsHolder().getEnd());
            assertEquals(createPoolTest.getLow() - 1, pool.getAvailableIdsHolder().getCursor().intValue());
            assertEquals(expectedBlockSize, pool.getBlockSize().longValue());
        }
        dataObject = configDataStore.get(childPoolIdentifier);
        if (dataObject instanceof ChildPools) {
            ChildPools childPool = (ChildPools) dataObject;
            assertEquals(localPoolName, childPool.getChildPoolName());
        }
    }

    @Test
    public void testAllocateId() throws Exception {
        List<IdPool> listOfIdPool = new ArrayList<>();
        IdPool localIdPool = buildLocalIdPool(blockSize, idStart, idStart + blockSize - 1, idStart - 1, localPoolName,
                poolName).build();
        listOfIdPool.add(localIdPool);
        IdPool globalIdPool = buildGlobalIdPool(poolName, idStart, idEnd, idStart + blockSize,
                buildChildPool(localPoolName)).build();
        listOfIdPool.add(globalIdPool);
        setupMocks(listOfIdPool);
        doReturn(Futures.immediateCheckedFuture(Optional.of(globalIdPool))).when(mockReadTx).read(
                LogicalDatastoreType.CONFIGURATION, parentPoolIdentifier);

        AllocateIdInput allocateIdInput = buildAllocateId(poolName, idKey);
        Future<RpcResult<AllocateIdOutput>> result = idManager.allocateId(allocateIdInput);
        assertTrue(result.get().isSuccessful());
        Thread.sleep(100);
        assertTrue(configDataStore.size() > 0);
        DataObject dataObject = configDataStore.get(localPoolIdentifier);
        if (dataObject instanceof IdPool) {
            IdPool pool = (IdPool) dataObject;
            assertEquals(localPoolName, pool.getPoolName());
            assertEquals(idStart, pool.getAvailableIdsHolder().getStart().intValue());
            assertEquals(idStart + blockSize - 1 , pool.getAvailableIdsHolder().getEnd().intValue());
            assertEquals(idStart, pool.getAvailableIdsHolder().getCursor().intValue());
        }
        dataObject = configDataStore.get(parentPoolIdentifier);
        if (dataObject instanceof IdPool) {
            IdPool parentPool = (IdPool) dataObject;
            assertEquals(1, parentPool.getIdEntries().size());
        }
        dataObject = configDataStore.get(childPoolIdentifier);
        if (dataObject instanceof ChildPools) {
            ChildPools childPool = (ChildPools) dataObject;
            assertEquals(localPoolName, childPool.getChildPoolName());
        }
    }

    @Test
    public void testReleaseId() throws Exception {
        List<IdEntries> idEntries = new ArrayList<>();
        List<Long> idValuesList = new ArrayList<>();
        idValuesList.add(idValue);

        List<IdPool> listOfIdPool = new ArrayList<>();
        IdPool expectedLocalPool = buildLocalIdPool(blockSize, idStart, idStart + blockSize - 1, idStart - 1,
                localPoolName, poolName).build();
        IdPool globalIdPool = buildGlobalIdPool(poolName, idStart, idEnd, blockSize, buildChildPool(localPoolName))
                .setIdEntries(idEntries).build();
        listOfIdPool.add(expectedLocalPool);
        listOfIdPool.add(globalIdPool);
        setupMocks(listOfIdPool);
        doReturn(Futures.immediateCheckedFuture(Optional.of(globalIdPool))).when(mockReadTx).read(
                LogicalDatastoreType.CONFIGURATION, parentPoolIdentifier);

        InstanceIdentifier<IdEntries> idEntriesIdentifier = buildIdEntriesIdentifier(parentPoolIdentifier, idKey);
        Optional<IdEntries> expectedIdEntry = Optional.of(buildIdEntry(idKey, idValuesList));
        doReturn(Futures.immediateCheckedFuture(expectedIdEntry)).when(mockReadTx).read(
                LogicalDatastoreType.CONFIGURATION, idEntriesIdentifier);

        ReleaseIdInput releaseIdInput = createReleaseIdInput(poolName, idKey);
        Future<RpcResult<Void>> result = idManager.releaseId(releaseIdInput);
        assertTrue(result.get().isSuccessful());
        Thread.sleep(100);
        assertTrue(configDataStore.size() > 0);
        DataObject dataObject = configDataStore.get(localPoolIdentifier);
        if (dataObject instanceof IdPool) {
            IdPool pool = (IdPool) dataObject;
            assertEquals(1, pool.getReleasedIdsHolder().getAvailableIdCount().intValue());
            assertEquals(idValue, pool.getReleasedIdsHolder().getDelayedIdEntries().get(0).getId().intValue());
        }
        dataObject = configDataStore.get(parentPoolIdentifier);
        if (dataObject instanceof IdPool) {
            IdPool parentPool = (IdPool) dataObject;
            assertEquals(0, parentPool.getIdEntries().size());
        }
        dataObject = configDataStore.get(childPoolIdentifier);
        if (dataObject instanceof ChildPools) {
            ChildPools childPool = (ChildPools) dataObject;
            assertEquals(localPoolName, childPool.getChildPoolName());
        }
    }

    /**
     * Ignoring this test case since cleanup task gets scheduled only after 30
     * seconds. Therefore in order to validate the pool state the test has to
     * wait for at least 30 seconds.
     */
    @Ignore
    public void testCleanupReleasedIds() throws Exception {
        List<Long> idValues = Arrays.asList(1L, 2L, 3L, 4L, 5L);
        IdEntries idEntry = buildIdEntry(idKey, idValues);
        List<IdEntries> listOfIdEntries = new ArrayList<>();
        listOfIdEntries.add(idEntry);

        IdPool globalIdPool = buildGlobalIdPool(poolName, idStart, idEnd, blockSize, buildChildPool(localPoolName))
                .setIdEntries(listOfIdEntries).build();
        IdPool expectedLocalPool = buildLocalIdPool(blockSize, idStart, idStart + blockSize - 1, idStart - 1,
                localPoolName, poolName).build();
        List<IdPool> listOfIdPool = new ArrayList<>();

        listOfIdPool.add(expectedLocalPool);
        listOfIdPool.add(globalIdPool);
        setupMocks(listOfIdPool);

        Optional<IdPool> expectedGlobalPool = Optional.of(globalIdPool);
        doReturn(Futures.immediateCheckedFuture(expectedGlobalPool)).when(mockReadTx).read(
                LogicalDatastoreType.CONFIGURATION, parentPoolIdentifier);

        InstanceIdentifier<IdEntries> idEntriesIdentifier = buildIdEntriesIdentifier(parentPoolIdentifier, idKey);
        Optional<IdEntries> expectedIdEntry = Optional.of(idEntry);
        doReturn(Futures.immediateCheckedFuture(expectedIdEntry)).when(mockReadTx).read(
                LogicalDatastoreType.CONFIGURATION, idEntriesIdentifier);

        ReleasedIdsHolder releaseIdsHolder = createReleasedIdsHolder(0, new ArrayList<>(), 0);
        InstanceIdentifier<ReleasedIdsHolder> releaseHolderIdentifier = buildReleaseIdsIdentifier(poolName);
        doReturn(Futures.immediateCheckedFuture(Optional.of(releaseIdsHolder))).when(mockReadTx).read(
                LogicalDatastoreType.CONFIGURATION, releaseHolderIdentifier);

        ReleaseIdInput releaseIdInput = createReleaseIdInput(poolName, idKey);
        Future<RpcResult<Void>> result = idManager.releaseId(releaseIdInput);
        Thread.sleep(40000);
        assertTrue(result.get().isSuccessful());
        assertTrue(configDataStore.size() > 0);

        DataObject dataObject = configDataStore.get(localPoolIdentifier);
        if (dataObject instanceof IdPool) {
            IdPool pool = (IdPool) dataObject;
            assertEquals(2, pool.getReleasedIdsHolder().getAvailableIdCount().intValue());
        }

        dataObject = configDataStore.get(parentPoolIdentifier);
        if (dataObject instanceof IdPool) {
            IdPool parentPool = (IdPool) dataObject;
            assertEquals(0, parentPool.getIdEntries().size());
        }

        dataObject = configDataStore.get(childPoolIdentifier);
        if (dataObject instanceof ChildPools) {
            ChildPools childPool = (ChildPools) dataObject;
            assertEquals(localPoolName, childPool.getChildPoolName());
        }

        InstanceIdentifier<ReleasedIdsHolder> releaseIdsIdentifier = buildReleaseIdsIdentifier(poolName);
        dataObject = configDataStore.get(releaseIdsIdentifier);
        if (dataObject instanceof ReleasedIdsHolder) {
            ReleasedIdsHolder releasedIds = (ReleasedIdsHolder) dataObject;
            assertEquals(3, releasedIds.getAvailableIdCount().intValue());
            assertEquals(3, releasedIds.getDelayedIdEntries().size());
        }
    }

    @Test
    public void testAllocateIdBlockFromReleasedIds() throws Exception {
        List<DelayedIdEntries> delayedIdEntries = buildDelayedIdEntries(new long[] {150, 151, 152});
        ReleasedIdsHolder expectedReleasedIds = createReleasedIdsHolder(3, delayedIdEntries , 0);
        IdPool globalIdPool = buildGlobalIdPool(poolName, idStart, idEnd, blockSize, buildChildPool(localPoolName))
                .setReleasedIdsHolder(expectedReleasedIds).build();
        IdPool localPool = buildLocalIdPool(blockSize, idStart, idStart + blockSize - 1, idStart + blockSize - 1,
                localPoolName, poolName).build();
        Optional<IdPool> expected = Optional.of(globalIdPool);
        List<IdPool> listOfIdPool = new ArrayList<>();
        listOfIdPool.add(localPool);
        listOfIdPool.add(globalIdPool);
        InstanceIdentifier<IdPool> parentPoolIdentifier = buildInstanceIdentifier(poolName);
        doReturn(Futures.immediateCheckedFuture(expected)).when(mockReadTx).read(
                LogicalDatastoreType.CONFIGURATION, parentPoolIdentifier);

        setupMocks(listOfIdPool);
        doReturn(Futures.immediateCheckedFuture(Optional.of(globalIdPool))).when(mockReadTx).read(
                LogicalDatastoreType.CONFIGURATION, parentPoolIdentifier);

        AllocateIdInput allocateIdInput = buildAllocateId(poolName, idKey);
        Future<RpcResult<AllocateIdOutput>> result = idManager.allocateId(allocateIdInput);
        assertTrue(result.get().isSuccessful());
        Thread.sleep(3);
        assertTrue(configDataStore.size() > 0);
        InstanceIdentifier<IdPool> localPoolIdentifier = buildInstanceIdentifier(localPoolName);
        DataObject dataObject = configDataStore.get(localPoolIdentifier);
        if (dataObject instanceof IdPool) {
            IdPool pool = (IdPool) dataObject;
            assertEquals(localPoolName, pool.getPoolName());
            assertEquals(1, pool.getReleasedIdsHolder().getDelayedIdEntries().size());
            assertEquals(1, pool.getReleasedIdsHolder().getAvailableIdCount().intValue());
        }

        InstanceIdentifier<ReleasedIdsHolder> releaseIdsIdentifier = buildReleaseIdsIdentifier(poolName);
        dataObject = configDataStore.get(releaseIdsIdentifier);
        if (dataObject instanceof ReleasedIdsHolder) {
            ReleasedIdsHolder releasedIds = (ReleasedIdsHolder) dataObject;
            assertEquals(1, releasedIds.getAvailableIdCount().intValue());
            assertEquals(1, releasedIds.getDelayedIdEntries().size());
        }

        InstanceIdentifier<ChildPools> childPoolIdentifier = buildChildPoolInstanceIdentifier(poolName, localPoolName);
        dataObject = configDataStore.get(childPoolIdentifier);
        if (dataObject instanceof ChildPools) {
            ChildPools childPool = (ChildPools) dataObject;
            assertEquals(localPoolName, childPool.getChildPoolName());
        }
    }

    @Test
    public void testDeletePool() throws Exception {
        IdPool globalIdPool = buildGlobalIdPool(poolName, idStart, idEnd, blockSize, buildChildPool(localPoolName))
                .build();
        IdPool localPool = buildLocalIdPool(blockSize, idStart, idStart + blockSize - 1, idStart + blockSize - 1,
                localPoolName, poolName).build();
        List<IdPool> listOfIdPool = new ArrayList<>();
        listOfIdPool.add(localPool);
        listOfIdPool.add(globalIdPool);
        setupMocks(listOfIdPool);
        Optional<IdPool> expected = Optional.of(globalIdPool);
        doReturn(Futures.immediateCheckedFuture(expected)).when(mockReadTx).read(
                LogicalDatastoreType.CONFIGURATION, parentPoolIdentifier);
        DeleteIdPoolInput deleteIdPoolInput = createDeleteIdPoolInput(poolName);
        Future<RpcResult<Void>> result = idManager.deleteIdPool(deleteIdPoolInput);
        Thread.sleep(3);
        assertTrue(result.get().isSuccessful());
        assertTrue(configDataStore.size() > 0);
        DataObject dataObject = configDataStore.get(localPoolIdentifier);
        assertEquals(dataObject, null);
        dataObject = configDataStore.get(parentPoolIdentifier);
        assertEquals(dataObject, null);
    }

    private InstanceIdentifier<ReleasedIdsHolder> buildReleaseIdsIdentifier(
            String poolName) {
        InstanceIdentifier<ReleasedIdsHolder> releasedIds = InstanceIdentifier
                .builder(IdPools.class).child(IdPool.class,
                        new IdPoolKey(poolName)).child(ReleasedIdsHolder.class).build();
        return releasedIds;
    }

    private InstanceIdentifier<ChildPools> buildChildPoolInstanceIdentifier(String poolName, String childPoolName) {
        InstanceIdentifier<ChildPools> childPool = InstanceIdentifier
                .builder(IdPools.class).child(IdPool.class,
                        new IdPoolKey(poolName)).child(ChildPools.class, new ChildPoolsKey(childPoolName)).build();
        return childPool;
    }

    private ReleaseIdInput createReleaseIdInput(String poolName, String idKey) {
        return new ReleaseIdInputBuilder().setIdKey(idKey).setPoolName(poolName).build();
    }

    private IdEntries buildIdEntry(String idKey, List<Long> idValuesList) {
        return new IdEntriesBuilder().setIdKey(idKey).setIdValue(idValuesList).build();
    }

    private InstanceIdentifier<IdEntries> buildIdEntriesIdentifier(InstanceIdentifier<IdPool> identifier,
            String idKey) {
        InstanceIdentifier.InstanceIdentifierBuilder<IdEntries> idEntriesBuilder = identifier
                .builder().child(IdEntries.class, new IdEntriesKey(idKey));
        InstanceIdentifier<IdEntries> idEntry = idEntriesBuilder.build();
        return idEntry;
    }

    private CreateIdPoolInput buildCreateIdPool(String poolName, long low, long high) {
        CreateIdPoolInput createPool = new CreateIdPoolInputBuilder().setPoolName(poolName)
                .setLow(low)
                .setHigh(high)
                .build();
        return createPool;
    }

    private IdPoolBuilder buildGlobalIdPool(String poolName, long idStart, long poolSize, int blockSize,
            List<ChildPools> childPools) {
        AvailableIdsHolder availableIdsHolder = createAvailableIdsHolder(idStart, poolSize, idStart - 1);
        ReleasedIdsHolder releasedIdsHolder = createReleasedIdsHolder(0, null, 0);
        return new IdPoolBuilder().setKey(new IdPoolKey(poolName))
                .setPoolName(poolName)
                .setBlockSize(blockSize)
                .setChildPools(childPools)
                .setAvailableIdsHolder(availableIdsHolder)
                .setReleasedIdsHolder(releasedIdsHolder);
    }

    private IdPoolBuilder buildLocalIdPool(int blockSize, int start, int end, int cursor, String localPoolName,
            String parentPoolName) {
        ReleasedIdsHolder releasedIdsHolder = createReleasedIdsHolder(0, null, 30);
        return new IdPoolBuilder().setBlockSize(blockSize)
                .setKey(new IdPoolKey(localPoolName))
                .setParentPoolName(parentPoolName)
                .setReleasedIdsHolder(releasedIdsHolder)
                .setAvailableIdsHolder(createAvailableIdsHolder(start, end, cursor));
    }

    private AllocateIdInput buildAllocateId(String poolName, String idKey) {
        AllocateIdInput getIdInput = new AllocateIdInputBuilder().setPoolName(poolName)
                .setIdKey(idKey).build();
        return getIdInput;
    }

    private InstanceIdentifier<IdPool> buildInstanceIdentifier(String poolName) {
        InstanceIdentifier.InstanceIdentifierBuilder<IdPool> idBuilder =
                InstanceIdentifier.builder(IdPools.class).child(IdPool.class, new IdPoolKey(poolName));
        InstanceIdentifier<IdPool> id = idBuilder.build();
        return id;
    }

    private AvailableIdsHolder createAvailableIdsHolder(long low, long high, long cursor) {
        AvailableIdsHolder availableIdsHolder = new AvailableIdsHolderBuilder()
                .setStart(low).setEnd(high).setCursor(cursor).build();
        return availableIdsHolder;
    }

    private ReleasedIdsHolder createReleasedIdsHolder(long availableIdCount, List<DelayedIdEntries> delayedIdEntries,
            long delayTime) {
        ReleasedIdsHolder releasedIdsHolder = new ReleasedIdsHolderBuilder()
                .setAvailableIdCount(availableIdCount)
                .setDelayedIdEntries(delayedIdEntries)
                .setDelayedTimeSec(delayTime).build();
        return releasedIdsHolder;
    }

    private DeleteIdPoolInput createDeleteIdPoolInput(String poolName) {
        return new DeleteIdPoolInputBuilder().setPoolName(poolName).build();
    }

    private List<DelayedIdEntries> buildDelayedIdEntries(long[] idValues) {
        List<DelayedIdEntries> delayedIdEntriesList = new ArrayList<>();
        for (long idValue : idValues) {
            DelayedIdEntries delayedIdEntries = new DelayedIdEntriesBuilder().setId(idValue).setReadyTimeSec(0L)
                    .build();
            delayedIdEntriesList.add(delayedIdEntries);
        }
        return delayedIdEntriesList;
    }

    private List<ChildPools> buildChildPool(String childPoolName) {
        ChildPools childPools = new ChildPoolsBuilder().setChildPoolName(childPoolName).build();
        List<ChildPools> childPoolsList = new ArrayList<>();
        childPoolsList.add(childPools);
        return childPoolsList;
    }
}
