/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.alivenessmonitor.protocols.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.alivenessmonitor.internal.AlivenessMonitor;
import org.opendaylight.genius.alivenessmonitor.protocols.AlivenessProtocolHandler;
import org.opendaylight.genius.alivenessmonitor.protocols.AlivenessProtocolHandlerRegistry;
import org.opendaylight.genius.alivenessmonitor.protocols.impl.AlivenessProtocolHandlerRegistryImpl;
import org.opendaylight.genius.alivenessmonitor.protocols.internal.AlivenessProtocolHandlerARP;
import org.opendaylight.genius.alivenessmonitor.protocols.internal.AlivenessProtocolHandlerLLDP;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.EtherTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorPauseInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorPauseInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorProfileCreateInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorProfileCreateInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorProfileCreateOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorProfileDeleteInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorProfileDeleteInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorProfileDeleteOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorStartInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorStartInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorStartOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorStopInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorStopInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorStopOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorUnpauseInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorUnpauseInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorUnpauseOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitoringMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411._interface.monitor.map.InterfaceMonitorEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411._interface.monitor.map.InterfaceMonitorEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.endpoint.endpoint.type.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.endpoint.endpoint.type.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitor.configs.MonitoringInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitor.configs.MonitoringInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitor.params.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitor.params.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitor.profile.create.input.ProfileBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitor.profiles.MonitorProfile;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitor.profiles.MonitorProfileBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitor.start.input.ConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitorid.key.map.MonitoridKeyEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitorid.key.map.MonitoridKeyEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitoring.states.MonitoringState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitoring.states.MonitoringStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.OdlArputilService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.CreateIdPoolInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.CreateIdPoolOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.ReleaseIdInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.ReleaseIdOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.OdlInterfaceRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

public class AlivenessMonitorTest {

    @Mock
    private DataBroker dataBroker;
    @Mock
    private IdManagerService idManager;
    @Mock
    private PacketProcessingService packetProcessingService;
    @Mock
    private NotificationPublishService notificationPublishService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private OdlInterfaceRpcService interfaceManager;
    @Mock
    private OdlArputilService arpService;
    private AlivenessMonitor alivenessMonitor;
    private AlivenessProtocolHandler arpHandler;
    private AlivenessProtocolHandler lldpHandler;
    private long mockId;
    @Mock
    private ReadOnlyTransaction readTx;
    @Mock
    private WriteTransaction writeTx;
    @Mock
    private ReadWriteTransaction readWriteTx;
    @Captor
    ArgumentCaptor<MonitoringState> stateCaptor;

    private <T extends DataObject> Matcher<InstanceIdentifier<T>> isType(
            final Class<T> klass) {
        return new TypeSafeMatcher<InstanceIdentifier<T>>() {
            @Override
            public void describeTo(Description desc) {
                desc.appendText(
                        "Instance Identifier should have Target Type " + klass);
            }

            @Override
            protected boolean matchesSafely(InstanceIdentifier<T> id) {
                return id.getTargetType().equals(klass);
            }
        };
    }

    private Matcher<RpcError> hasErrorType(final ErrorType errorType) {
        return new TypeSafeMatcher<RpcError>() {
            @Override
            public void describeTo(Description desc) {
                desc.appendText("Error type do not match " + errorType);
            }

            @Override
            protected boolean matchesSafely(RpcError error) {
                return error.getErrorType().equals(errorType);
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(idManager.createIdPool(any(CreateIdPoolInput.class)))
                .thenReturn(Futures.immediateFuture(
                        RpcResultBuilder.<CreateIdPoolOutput>success().build()));

        AlivenessProtocolHandlerRegistry alivenessProtocolHandlerRegistry = new AlivenessProtocolHandlerRegistryImpl();
        alivenessMonitor = new AlivenessMonitor(dataBroker, idManager,
                notificationPublishService, alivenessProtocolHandlerRegistry);
        arpHandler = new AlivenessProtocolHandlerARP(dataBroker,
                interfaceManager, alivenessProtocolHandlerRegistry, arpService);
        lldpHandler = new AlivenessProtocolHandlerLLDP(dataBroker,
                alivenessProtocolHandlerRegistry, packetProcessingService);
        mockId = 1L;
        when(idManager.allocateId(any(AllocateIdInput.class)))
                .thenReturn(
                        Futures.immediateFuture(RpcResultBuilder
                                .success(new AllocateIdOutputBuilder()
                                        .setIdValue(mockId++).build())
                                .build()));
        when(idManager.releaseId(any(ReleaseIdInput.class))).thenReturn(Futures
                .immediateFuture(RpcResultBuilder.<ReleaseIdOutput>success().build()));
        doReturn(readTx).when(dataBroker).newReadOnlyTransaction();
        doReturn(writeTx).when(dataBroker).newWriteOnlyTransaction();
        doReturn(readWriteTx).when(dataBroker).newReadWriteTransaction();
        doNothing().when(writeTx).put(eq(LogicalDatastoreType.OPERATIONAL),
                any(InstanceIdentifier.class), any(DataObject.class));
        doReturn(Futures.immediateCheckedFuture(null)).when(writeTx).submit();
        doReturn(Futures.immediateCheckedFuture(null)).when(readWriteTx)
                .submit();
    }

    @After
    public void tearDown() {
        alivenessMonitor.close();
    }

    @Test
    public void testMonitorProfileCreate()
            throws InterruptedException, ExecutionException {
        MonitorProfileCreateInput input = new MonitorProfileCreateInputBuilder()
                .setProfile(new ProfileBuilder().setFailureThreshold(10L)
                        .setMonitorInterval(10000L).setMonitorWindow(10L)
                        .setProtocolType(EtherTypes.Arp).build())
                .build();
        doReturn(Futures.immediateCheckedFuture(Optional.absent()))
                .when(readWriteTx).read(eq(LogicalDatastoreType.OPERATIONAL),
                        argThat(isType(MonitorProfile.class)));
        doReturn(Futures.immediateCheckedFuture(null)).when(readWriteTx)
                .submit();
        RpcResult<MonitorProfileCreateOutput> output = alivenessMonitor
                .monitorProfileCreate(input).get();
        assertTrue("Monitor Profile Create result", output.isSuccessful());
        assertNotNull("Monitor Profile Output",
                output.getResult().getProfileId());
    }

    @Test
    public void testMonitorProfileCreateAlreadyExist()
            throws InterruptedException, ExecutionException {
        MonitorProfileCreateInput input = new MonitorProfileCreateInputBuilder()
                .setProfile(new ProfileBuilder().setFailureThreshold(10L)
                        .setMonitorInterval(10000L).setMonitorWindow(10L)
                        .setProtocolType(EtherTypes.Arp).build())
                .build();
        @SuppressWarnings("unchecked")
        Optional<MonitorProfile> optionalProfile = mock(Optional.class);
        CheckedFuture<Optional<MonitorProfile>, ReadFailedException> proFuture = Futures
                .immediateCheckedFuture(optionalProfile);
        doReturn(true).when(optionalProfile).isPresent();
        doReturn(proFuture).when(readWriteTx).read(
                eq(LogicalDatastoreType.OPERATIONAL),
                argThat(isType(MonitorProfile.class)));
        RpcResult<MonitorProfileCreateOutput> output = alivenessMonitor
                .monitorProfileCreate(input).get();
        assertTrue("Monitor Profile Create result", output.isSuccessful());
        assertThat(output.getErrors(),
                CoreMatchers.hasItem(hasErrorType(ErrorType.PROTOCOL)));
    }

    @Test
    public void testMonitorStart()
            throws InterruptedException, ExecutionException {
        Long profileId = createProfile();
        MonitorStartInput input = new MonitorStartInputBuilder()
                .setConfig(
                        new ConfigBuilder()
                                .setDestination(new DestinationBuilder()
                                        .setEndpointType(
                                                getInterface("10.0.0.1"))
                                        .build())
                                .setSource(new SourceBuilder()
                                        .setEndpointType(getInterface(
                                                "testInterface", "10.1.1.1"))
                                        .build())
                                .setMode(MonitoringMode.OneOne)
                                .setProfileId(profileId).build())
                .build();
        Optional<MonitorProfile> optionalProfile = Optional
                .of(getTestMonitorProfile());
        CheckedFuture<Optional<MonitorProfile>, ReadFailedException> proFuture = Futures
                .immediateCheckedFuture(optionalProfile);
        when(readTx.read(eq(LogicalDatastoreType.OPERATIONAL),
                argThat(isType(MonitorProfile.class)))).thenReturn(proFuture);
        CheckedFuture<Optional<MonitoringInfo>, ReadFailedException> outFuture = Futures
                .immediateCheckedFuture(Optional.<MonitoringInfo>absent());
        when(readTx.read(eq(LogicalDatastoreType.OPERATIONAL),
                argThat(isType(MonitoringInfo.class)))).thenReturn(outFuture);
        RpcResult<MonitorStartOutput> output = alivenessMonitor
                .monitorStart(input).get();
        verify(idManager, times(2)).allocateId(any(AllocateIdInput.class));
        assertTrue("Monitor start output result", output.isSuccessful());
        assertNotNull("Monitor start output",
                output.getResult().getMonitorId());
    }

    @Test
    public void testMonitorPause()
            throws InterruptedException, ExecutionException {
        MonitorPauseInput input = new MonitorPauseInputBuilder()
                .setMonitorId(2L).build();
        Optional<MonitorProfile> optProfile = Optional
                .of(getTestMonitorProfile());
        when(readTx.read(eq(LogicalDatastoreType.OPERATIONAL),
                argThat(isType(MonitorProfile.class))))
                        .thenReturn(Futures.immediateCheckedFuture(optProfile));
        Optional<MonitoringInfo> optInfo = Optional.of(
                new MonitoringInfoBuilder().setId(2L).setProfileId(1L).build());
        when(readTx.read(eq(LogicalDatastoreType.OPERATIONAL),
                argThat(isType(MonitoringInfo.class))))
                        .thenReturn(Futures.immediateCheckedFuture(optInfo));
        Optional<MonitoringState> optState = Optional
                .of(new MonitoringStateBuilder()
                        .setStatus(MonitorStatus.Started).build());
        when(readWriteTx.read(eq(LogicalDatastoreType.OPERATIONAL),
                argThat(isType(MonitoringState.class))))
                        .thenReturn(Futures.immediateCheckedFuture(optState));
        Optional<MonitoridKeyEntry> optMap = Optional
                .of(new MonitoridKeyEntryBuilder().setMonitorId(2L)
                        .setMonitorKey("Test monitor Key").build());
        when(readTx.read(eq(LogicalDatastoreType.OPERATIONAL),
                argThat(isType(MonitoridKeyEntry.class))))
                        .thenReturn(Futures.immediateCheckedFuture(optMap));
        alivenessMonitor.monitorPause(input).get();
        verify(readWriteTx).merge(eq(LogicalDatastoreType.OPERATIONAL),
                argThat(isType(MonitoringState.class)), stateCaptor.capture());
        assertEquals(MonitorStatus.Paused, stateCaptor.getValue().getStatus());
    }

    @Test
    public void testMonitorUnpause()
            throws InterruptedException, ExecutionException {
        MonitorUnpauseInput input = new MonitorUnpauseInputBuilder()
                .setMonitorId(2L).build();
        Optional<MonitoringState> optState = Optional
                .of(new MonitoringStateBuilder().setStatus(MonitorStatus.Paused)
                        .build());
        when(readWriteTx.read(eq(LogicalDatastoreType.OPERATIONAL),
                argThat(isType(MonitoringState.class))))
                        .thenReturn(Futures.immediateCheckedFuture(optState));
        Optional<MonitoringInfo> optInfo = Optional.of(
                new MonitoringInfoBuilder().setId(2L).setProfileId(1L).build());
        when(readTx.read(eq(LogicalDatastoreType.OPERATIONAL),
                argThat(isType(MonitoringInfo.class))))
                        .thenReturn(Futures.immediateCheckedFuture(optInfo));
        Optional<MonitorProfile> optProfile = Optional
                .of(getTestMonitorProfile());
        when(readTx.read(eq(LogicalDatastoreType.OPERATIONAL),
                argThat(isType(MonitorProfile.class))))
                        .thenReturn(Futures.immediateCheckedFuture(optProfile));
        Optional<MonitoridKeyEntry> optMap = Optional
                .of(new MonitoridKeyEntryBuilder().setMonitorId(2L)
                        .setMonitorKey("Test monitor Key").build());
        when(readTx.read(eq(LogicalDatastoreType.OPERATIONAL),
                argThat(isType(MonitoridKeyEntry.class))))
                        .thenReturn(Futures.immediateCheckedFuture(optMap));
        RpcResult<MonitorUnpauseOutput> result = alivenessMonitor.monitorUnpause(input).get();
        verify(readWriteTx).merge(eq(LogicalDatastoreType.OPERATIONAL),
                argThat(isType(MonitoringState.class)), stateCaptor.capture());
        assertEquals(MonitorStatus.Started, stateCaptor.getValue().getStatus());
        assertTrue("Monitor unpause rpc result", result.isSuccessful());
    }

    @Test
    public void testMonitorStop()
            throws InterruptedException, ExecutionException {
        MonitorStopInput input = new MonitorStopInputBuilder().setMonitorId(2L)
                .build();
        Optional<MonitoringInfo> optInfo = Optional
                .of(new MonitoringInfoBuilder().setSource(new SourceBuilder()
                        .setEndpointType(
                                getInterface("testInterface", "10.1.1.1"))
                        .build()).build());
        CheckedFuture<Optional<MonitoringInfo>, ReadFailedException> outFuture = Futures
                .immediateCheckedFuture(optInfo);
        when(readTx.read(eq(LogicalDatastoreType.OPERATIONAL),
                argThat(isType(MonitoringInfo.class)))).thenReturn(outFuture);
        Optional<MonitoridKeyEntry> optMap = Optional
                .of(new MonitoridKeyEntryBuilder().setMonitorId(2L)
                        .setMonitorKey("Test monitor Key").build());
        when(readTx.read(eq(LogicalDatastoreType.OPERATIONAL),
                argThat(isType(MonitoridKeyEntry.class))))
                        .thenReturn(Futures.immediateCheckedFuture(optMap));
        Optional<MonitorProfile> optProfile = Optional
                .of(getTestMonitorProfile());
        when(readTx.read(eq(LogicalDatastoreType.OPERATIONAL),
                argThat(isType(MonitorProfile.class))))
                        .thenReturn(Futures.immediateCheckedFuture(optProfile));
        Optional<InterfaceMonitorEntry> optEntry = Optional
                .of(getInterfaceMonitorEntry());
        when(readWriteTx.read(eq(LogicalDatastoreType.OPERATIONAL),
                argThat(isType(InterfaceMonitorEntry.class))))
                        .thenReturn(Futures.immediateCheckedFuture(optEntry));
        RpcResult<MonitorStopOutput> result = alivenessMonitor.monitorStop(input).get();
        verify(idManager).releaseId(any(ReleaseIdInput.class));
        verify(writeTx, times(3)).delete(eq(LogicalDatastoreType.OPERATIONAL),
                any(InstanceIdentifier.class));
        assertTrue("Monitor stop rpc result", result.isSuccessful());
    }

    @Test
    public void testMonitorProfileDelete()
            throws InterruptedException, ExecutionException {
        MonitorProfileDeleteInput input = new MonitorProfileDeleteInputBuilder()
                .setProfileId(1L).build();
        Optional<MonitorProfile> optProfile = Optional
                .of(getTestMonitorProfile());
        when(readWriteTx.read(eq(LogicalDatastoreType.OPERATIONAL),
                argThat(isType(MonitorProfile.class))))
                        .thenReturn(Futures.immediateCheckedFuture(optProfile));
        RpcResult<MonitorProfileDeleteOutput> result = alivenessMonitor.monitorProfileDelete(input)
                .get();
        verify(idManager).releaseId(any(ReleaseIdInput.class));
        verify(readWriteTx).delete(eq(LogicalDatastoreType.OPERATIONAL),
                Matchers.<InstanceIdentifier<MonitorProfile>>any());
        assertTrue("Monitor profile delete result", result.isSuccessful());
    }

    @SuppressWarnings("unchecked")
    private long createProfile()
            throws InterruptedException, ExecutionException {
        MonitorProfileCreateInput input = new MonitorProfileCreateInputBuilder()
                .setProfile(new ProfileBuilder().setFailureThreshold(10L)
                        .setMonitorInterval(10000L).setMonitorWindow(10L)
                        .setProtocolType(EtherTypes.Arp).build())
                .build();
        doReturn(Futures.immediateCheckedFuture(Optional.absent()))
                .when(readWriteTx).read(eq(LogicalDatastoreType.OPERATIONAL),
                        any(InstanceIdentifier.class));
        doReturn(Futures.immediateCheckedFuture(null)).when(readWriteTx)
                .submit();
        RpcResult<MonitorProfileCreateOutput> output = alivenessMonitor
                .monitorProfileCreate(input).get();
        return output.getResult().getProfileId();
    }

    private MonitorProfile getTestMonitorProfile() {
        return new MonitorProfileBuilder().setFailureThreshold(10L)
                .setMonitorInterval(10000L).setMonitorWindow(10L)
                .setProtocolType(EtherTypes.Arp).build();
    }

    private InterfaceMonitorEntry getInterfaceMonitorEntry() {
        return new InterfaceMonitorEntryBuilder()
                .setInterfaceName("test-interface")
                .setMonitorIds(Arrays.asList(1L, 2L)).build();
    }

    private Interface getInterface(String ipAddress) {
        return new InterfaceBuilder()
                .setInterfaceIp(IpAddressBuilder.getDefaultInstance(ipAddress))
                .build();
    }

    private Interface getInterface(String interfaceName, String ipAddress) {
        return new InterfaceBuilder()
                .setInterfaceIp(IpAddressBuilder.getDefaultInstance(ipAddress))
                .setInterfaceName(interfaceName).build();
    }
}
