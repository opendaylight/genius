/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.cache;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
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
import org.mockito.ArgumentCaptor;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.infrautils.caches.baseimpl.internal.CacheManagersRegistryImpl;
import org.opendaylight.infrautils.caches.guava.internal.GuavaCacheProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.mdsalutil.rev170830.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.mdsalutil.rev170830.ConfigBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Unit tests for DataObjectCache.
 *
 * @author Thomas Pantelis
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class DataObjectCacheTest {
    private static final InstanceIdentifier<Config> PATH = InstanceIdentifier.create(Config.class);
    private static final Config CONFIG_OBJ = new ConfigBuilder().build();

    private final ArgumentCaptor<DataTreeChangeListener> listenerCapture =
            ArgumentCaptor.forClass(DataTreeChangeListener.class);
    private final DataObjectModification<Config> mockModification = mock(DataObjectModification.class);
    private final DataTreeModification<Config> mockDataTreeModification = mock(DataTreeModification.class);
    private final ListenerRegistration<?> mockListenerReg = mock(ListenerRegistration.class);
    private final DataBroker mockDataBroker = mock(DataBroker.class);
    private final ReadOnlyTransaction mockReadTx = mock(ReadOnlyTransaction.class);

    private InstanceIdDataObjectCache<Config> cache;

    @Before
    public void setup() {
        doReturn(mockReadTx).when(mockDataBroker).newReadOnlyTransaction();

        doReturn(mockListenerReg).when(mockDataBroker).registerDataTreeChangeListener(eq(new DataTreeIdentifier<>(
                LogicalDatastoreType.OPERATIONAL, PATH)), any());

        cache = new InstanceIdDataObjectCache<>(Config.class, mockDataBroker, LogicalDatastoreType.OPERATIONAL, PATH,
                new GuavaCacheProvider(new CacheManagersRegistryImpl()));

        verify(mockDataBroker).registerDataTreeChangeListener(eq(new DataTreeIdentifier<>(
                LogicalDatastoreType.OPERATIONAL, PATH)), listenerCapture.capture());

        doReturn(mockModification).when(mockDataTreeModification).getRootNode();
        doReturn(new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, PATH))
                .when(mockDataTreeModification).getRootPath();
        doReturn(CONFIG_OBJ).when(mockModification).getDataAfter();
    }

    @Test
    public void testGet() throws ReadFailedException {
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

        assertEquals("getAllPresent", Arrays.asList(CONFIG_OBJ), cache.getAllPresent());
    }

    @Test
    public void testListenerUpdates() throws ReadFailedException {
        doReturn(Futures.immediateCheckedFuture(Optional.absent())).when(mockReadTx)
            .read(LogicalDatastoreType.OPERATIONAL, PATH);

        Optional<Config> optional = cache.get(PATH);
        assertEquals("isPresent", Boolean.FALSE, optional.isPresent());
        verify(mockReadTx).read(LogicalDatastoreType.OPERATIONAL, PATH);

        reset(mockReadTx);
        doReturn(Futures.immediateCheckedFuture(Optional.absent())).when(mockReadTx)
            .read(LogicalDatastoreType.OPERATIONAL, PATH);

        doReturn(DataObjectModification.ModificationType.WRITE).when(mockModification).getModificationType();
        listenerCapture.getValue().onDataTreeChanged(Arrays.asList(mockDataTreeModification));

        optional = cache.get(PATH);
        assertEquals("isPresent", Boolean.TRUE, optional.isPresent());
        assertEquals("get", CONFIG_OBJ, optional.get());
        verifyNoMoreInteractions(mockReadTx);

        doReturn(DataObjectModification.ModificationType.DELETE).when(mockModification).getModificationType();
        listenerCapture.getValue().onDataTreeChanged(Arrays.asList(mockDataTreeModification));

        optional = cache.get(PATH);
        assertEquals("isPresent", Boolean.FALSE, optional.isPresent());
        verify(mockReadTx).read(LogicalDatastoreType.OPERATIONAL, PATH);
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

        assertEquals("getAllPresent", 0, cache.getAllPresent().size());
    }

    @Test(expected = ReadFailedException.class)
    public void testGetWithReadFailure() throws ReadFailedException {
        doReturn(Futures.immediateFailedCheckedFuture(new ReadFailedException("mock"))).when(mockReadTx)
            .read(LogicalDatastoreType.OPERATIONAL, PATH);

        cache.get(PATH);
    }

    @Test
    public void testClose()  {
        cache.close();
        verify(mockListenerReg).close();
    }
}
