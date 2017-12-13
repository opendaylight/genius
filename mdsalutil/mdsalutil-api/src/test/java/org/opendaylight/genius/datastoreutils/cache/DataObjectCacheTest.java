/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.cache;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.mdsalutil.rev170830.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.mdsalutil.rev170830.ConfigBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Unit tests for DataObjectCache.
 *
 * @author Thomas Pantelis
 */
public class DataObjectCacheTest {
    private static final InstanceIdentifier<Config> PATH = InstanceIdentifier.create(Config.class);
    private static final  Config CONFIG_OBJ = new ConfigBuilder().build();

    private final DataBroker mockDataBroker = mock(DataBroker.class);
    private final ReadOnlyTransaction mockReadTx = mock(ReadOnlyTransaction.class);

    private final DataObjectCache<Config> cache =
            new DataObjectCache<>(mockDataBroker, LogicalDatastoreType.OPERATIONAL);


    @Before
    public void setup() {
        doReturn(mockReadTx).when(mockDataBroker).newReadOnlyTransaction();
    }

    @Test
    public void testGetRemovePut() throws ReadFailedException {
        doReturn(Futures.immediateCheckedFuture(Optional.of(CONFIG_OBJ))).when(mockReadTx)
                .read(LogicalDatastoreType.OPERATIONAL, PATH);

        Optional<Config> optional = cache.get(PATH);
        assertEquals("isPresent", Boolean.TRUE, optional.isPresent());
        assertEquals("get", CONFIG_OBJ, optional.get());
        verify(mockReadTx).read(LogicalDatastoreType.OPERATIONAL, PATH);

        reset(mockReadTx);

        optional = cache.get(PATH);
        assertEquals("isPresent", Boolean.TRUE, optional.isPresent());
        assertEquals("get", CONFIG_OBJ, optional.get());
        verifyNoMoreInteractions(mockReadTx);

        optional = cache.getIfPresent(PATH);
        assertEquals("isPresent", Boolean.TRUE, optional.isPresent());
        assertEquals("get", CONFIG_OBJ, optional.get());
        verifyNoMoreInteractions(mockReadTx);

        cache.remove(PATH);
        optional = cache.getIfPresent(PATH);
        assertEquals("isPresent", Boolean.FALSE, optional.isPresent());

        cache.put(PATH, CONFIG_OBJ);
        optional = cache.getIfPresent(PATH);
        assertEquals("isPresent", Boolean.TRUE, optional.isPresent());
        assertEquals("get", CONFIG_OBJ, optional.get());
        verifyNoMoreInteractions(mockReadTx);

        assertEquals("getAll", Arrays.asList(CONFIG_OBJ), cache.getAll());
    }

    @Test
    public void testGetWithDataAbsent() throws ReadFailedException {
        doReturn(Futures.immediateCheckedFuture(Optional.absent())).when(mockReadTx)
            .read(LogicalDatastoreType.OPERATIONAL, PATH);

        Optional<Config> optional = cache.get(PATH);
        assertEquals("isPresent", Boolean.FALSE, optional.isPresent());
        verify(mockReadTx).read(LogicalDatastoreType.OPERATIONAL, PATH);

        reset(mockReadTx);
        optional = cache.get(PATH);
        assertEquals("isPresent", Boolean.FALSE, optional.isPresent());
        verifyNoMoreInteractions(mockReadTx);

        assertEquals("getAll", 0, cache.getAll().size());
    }

    @Test(expected = ReadFailedException.class)
    public void testGetWithReadFailure() throws ReadFailedException {
        doReturn(Futures.immediateFailedCheckedFuture(new ReadFailedException("mock"))).when(mockReadTx)
            .read(LogicalDatastoreType.OPERATIONAL, PATH);

        cache.get(PATH);
    }
}
