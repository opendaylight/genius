/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.alivenessmonitor.internal;

import static org.opendaylight.genius.alivenessmonitor.utils.AlivenessMonitorUtil.getInterfaceMonitorMapId;
import static org.opendaylight.genius.alivenessmonitor.utils.AlivenessMonitorUtil.getMonitorMapId;
import static org.opendaylight.genius.alivenessmonitor.utils.AlivenessMonitorUtil.getMonitorProfileId;
import static org.opendaylight.genius.alivenessmonitor.utils.AlivenessMonitorUtil.getMonitorStateId;
import static org.opendaylight.genius.alivenessmonitor.utils.AlivenessMonitorUtil.getMonitoringInfoId;
import static org.opendaylight.genius.infra.Datastore.OPERATIONAL;
import static org.opendaylight.yangtools.yang.binding.CodeHelpers.nonnull;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.alivenessmonitor.protocols.AlivenessProtocolHandler;
import org.opendaylight.genius.alivenessmonitor.protocols.AlivenessProtocolHandlerRegistry;
import org.opendaylight.genius.alivenessmonitor.utils.AlivenessMonitorUtil;
import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.mdsalutil.packet.Ethernet;
import org.opendaylight.genius.mdsalutil.packet.utils.PacketUtil;
import org.opendaylight.infrautils.utils.concurrent.Executors;
import org.opendaylight.openflowplugin.libraries.liblldp.Packet;
import org.opendaylight.openflowplugin.libraries.liblldp.PacketException;
import org.opendaylight.serviceutils.tools.mdsal.listener.AbstractClusteredSyncDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.AlivenessMonitorService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.LivenessState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorEvent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorEventBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorPauseInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorPauseOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorProfileCreateInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorProfileCreateOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorProfileCreateOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorProfileDeleteInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorProfileDeleteOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorProfileGetInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorProfileGetOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorProfileGetOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorProtocolType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorStartInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorStartOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorStartOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorStopInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorStopOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorUnpauseInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorUnpauseOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitoringMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitoringStates;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411._interface.monitor.map.InterfaceMonitorEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411._interface.monitor.map.InterfaceMonitorEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411._interface.monitor.map.InterfaceMonitorEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.endpoint.EndpointType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.endpoint.endpoint.type.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitor.configs.MonitoringInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitor.configs.MonitoringInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitor.event.EventData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitor.event.EventDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitor.profile.create.input.Profile;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitor.profiles.MonitorProfile;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitor.profiles.MonitorProfileBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitor.start.input.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitorid.key.map.MonitoridKeyEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitorid.key.map.MonitoridKeyEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitoring.states.MonitoringState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitoring.states.MonitoringStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.CreateIdPoolInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.CreateIdPoolInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.CreateIdPoolOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.ReleaseIdInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.ReleaseIdInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.ReleaseIdOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketInReason;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.SendToController;
import org.opendaylight.yangtools.yang.binding.CodeHelpers;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class AlivenessMonitor extends AbstractClusteredSyncDataTreeChangeListener<MonitoringState>
        implements AlivenessMonitorService, PacketProcessingListener, InterfaceStateListener {

    private static final Logger LOG = LoggerFactory.getLogger(AlivenessMonitor.class);

    private static final int THREAD_POOL_SIZE = 4;
    private static final boolean INTERRUPT_TASK = true;
    private static final int NO_DELAY = 0;
    private static final Long INITIAL_COUNT = 0L;
    private static final boolean CREATE_MISSING_PARENT = true;
    private static final int INVALID_ID = 0;

    private static class FutureCallbackImpl implements FutureCallback<Object> {
        private final String message;

        FutureCallbackImpl(String message) {
            this.message = message;
        }

        @Override
        public void onFailure(Throwable error) {
            LOG.warn("Error in Datastore operation - {}", message, error);
        }

        @Override
        public void onSuccess(Object result) {
            LOG.debug("Success in Datastore operation - {}", message);
        }
    }

    private class AlivenessMonitorTask implements Runnable {
        private final MonitoringInfo monitoringInfo;

        AlivenessMonitorTask(MonitoringInfo monitoringInfo) {
            this.monitoringInfo = monitoringInfo;
        }

        @Override
        public void run() {
            LOG.trace("send monitor packet - {}", monitoringInfo);
            sendMonitorPacket(monitoringInfo);
        }
    }

    private final DataBroker dataBroker;
    private final ManagedNewTransactionRunner txRunner;
    private final IdManagerService idManager;
    private final NotificationPublishService notificationPublishService;
    private final AlivenessProtocolHandlerRegistry alivenessProtocolHandlerRegistry;
    private final ConcurrentMap<Long, ScheduledFuture<?>> monitoringTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService monitorService;
    private final ExecutorService callbackExecutorService;
    private final LoadingCache<Long, String> monitorIdKeyCache;
    private final ConcurrentMap<String, Semaphore> lockMap = new ConcurrentHashMap<>();

    @Inject
    public AlivenessMonitor(final DataBroker dataBroker, final IdManagerService idManager,
            final NotificationPublishService notificationPublishService,
            AlivenessProtocolHandlerRegistry alivenessProtocolHandlerRegistry) {
        super(dataBroker, LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.create(MonitoringStates.class).child(MonitoringState.class));
        this.dataBroker = dataBroker;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.idManager = idManager;
        this.notificationPublishService = notificationPublishService;
        this.alivenessProtocolHandlerRegistry = alivenessProtocolHandlerRegistry;

        monitorService = Executors.newListeningScheduledThreadPool(THREAD_POOL_SIZE, "Aliveness Monitoring Task", LOG);
        callbackExecutorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE, "Aliveness Callback Handler", LOG);

        createIdPool();
        monitorIdKeyCache = CacheBuilder.newBuilder().build(new CacheLoader<Long, String>() {
            @Override
            public String load(@Nonnull Long monitorId) {
                return SingleTransactionDataBroker.syncReadOptionalAndTreatReadFailedExceptionAsAbsentOptional(
                        dataBroker, LogicalDatastoreType.OPERATIONAL, getMonitorMapId(monitorId))
                        .toJavaUtil().map(MonitoridKeyEntry::getMonitorKey).orElse(null);
            }
        });

        LOG.info("{} started", getClass().getSimpleName());
    }

    @Override
    @PreDestroy
    public void close() {
        monitorIdKeyCache.cleanUp();
        monitorService.shutdown();
        callbackExecutorService.shutdown();
        LOG.info("{} close", getClass().getSimpleName());
    }

    Semaphore getLock(String key) {
        return lockMap.get(key);
    }

    private void createIdPool() {
        CreateIdPoolInput createPool = new CreateIdPoolInputBuilder()
                .setPoolName(AlivenessMonitorConstants.MONITOR_IDPOOL_NAME)
                .setLow(AlivenessMonitorConstants.MONITOR_IDPOOL_START)
                .setHigh(AlivenessMonitorConstants.MONITOR_IDPOOL_SIZE).build();
        Futures.addCallback(idManager.createIdPool(createPool), new FutureCallback<RpcResult<CreateIdPoolOutput>>() {
            @Override
            public void onFailure(Throwable error) {
                LOG.error("Failed to create idPool for Aliveness Monitor Service", error);
            }

            @Override
            public void onSuccess(@Nonnull RpcResult<CreateIdPoolOutput> result) {
                if (result.isSuccessful()) {
                    LOG.debug("Created IdPool for Aliveness Monitor Service");
                } else {
                    LOG.error("RPC to create Idpool failed {}", result.getErrors());
                }
            }
        }, callbackExecutorService);
    }

    private int getUniqueId(final String idKey) {
        AllocateIdInput getIdInput = new AllocateIdInputBuilder()
                .setPoolName(AlivenessMonitorConstants.MONITOR_IDPOOL_NAME).setIdKey(idKey).build();

        Future<RpcResult<AllocateIdOutput>> result = idManager.allocateId(getIdInput);

        try {
            RpcResult<AllocateIdOutput> rpcResult = result.get();
            if (rpcResult.isSuccessful()) {
                return rpcResult.getResult().getIdValue().intValue();
            } else {
                LOG.warn("RPC Call to Get Unique Id returned with Errors {}", rpcResult.getErrors());
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Exception when getting Unique Id for key {}", idKey, e);
        }
        return INVALID_ID;
    }

    private void releaseId(String idKey) {
        ReleaseIdInput idInput = new ReleaseIdInputBuilder().setPoolName(AlivenessMonitorConstants.MONITOR_IDPOOL_NAME)
                .setIdKey(idKey).build();
        try {
            ListenableFuture<RpcResult<ReleaseIdOutput>> result = idManager.releaseId(idInput);
            RpcResult<ReleaseIdOutput> rpcResult = result.get();
            if (!rpcResult.isSuccessful()) {
                LOG.warn("RPC Call to release Id {} returned with Errors {}", idKey, rpcResult.getErrors());
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Exception when releasing Id for key {}", idKey, e);
        }
    }

    @Override
    public void onPacketReceived(PacketReceived packetReceived) {
        Class<? extends PacketInReason> pktInReason = packetReceived.getPacketInReason();
        if (LOG.isTraceEnabled()) {
            LOG.trace("Packet Received {}", packetReceived);
        }
        if (pktInReason != SendToController.class) {
            return;
        }
        byte[] data = packetReceived.getPayload();
        Packet protocolPacket = null;
        AlivenessProtocolHandler<Packet> livenessProtocolHandler = null;

        if (!PacketUtil.isIpv6NaPacket(data)) {
            Packet packetInFormatted;
            Ethernet res = new Ethernet();
            try {
                packetInFormatted = res.deserialize(data, 0, data.length * Byte.SIZE);
            } catch (PacketException e) {
                LOG.warn("Failed to decode packet: ", e);
                return;
            }

            if (packetInFormatted == null) {
                LOG.warn("Failed to deserialize Received Packet from table {}", packetReceived.getTableId().getValue());
                return;
            }

            protocolPacket = packetInFormatted.getPayload();
            if (protocolPacket == null) {
                LOG.trace("Unsupported packet type. Ignoring the packet...");
                return;
            }
            if (LOG.isTraceEnabled()) {
                LOG.trace("onPacketReceived packet: {}, packet class: {}", packetReceived, protocolPacket.getClass());
            }
            livenessProtocolHandler = getAlivenessProtocolHandler(protocolPacket.getClass());

        } else if (PacketUtil.isIpv6NaPacket(data)) {
            livenessProtocolHandler = getAlivenessProtocolHandler(MonitorProtocolType.Ipv6Nd);
        }
        if (livenessProtocolHandler == null) {
            return;
        }

        String monitorKey = livenessProtocolHandler.handlePacketIn(protocolPacket, packetReceived);
        if (monitorKey != null) {
            processReceivedMonitorKey(monitorKey);
        } else {
            LOG.debug("No monitorkey associated with received packet");
        }
    }

    @SuppressWarnings("unchecked")
    private AlivenessProtocolHandler<Packet> getAlivenessProtocolHandler(Class<? extends Packet> protocolHandlerClass) {
        return (AlivenessProtocolHandler<Packet>) alivenessProtocolHandlerRegistry.getOpt(protocolHandlerClass);
    }

    @SuppressWarnings("unchecked")
    private AlivenessProtocolHandler<Packet> getAlivenessProtocolHandler(MonitorProtocolType protocolType) {
        return (AlivenessProtocolHandler<Packet>) alivenessProtocolHandlerRegistry.get(protocolType);
    }

    private void processReceivedMonitorKey(final String monitorKey) {
        class Result {
            final MonitoringState currentState;
            final MonitoringState state;

            Result(MonitoringState currentState, MonitoringState state) {
                this.currentState = currentState;
                this.state = state;
            }
        }

        Preconditions.checkNotNull(monitorKey, "Monitor Key required to process the state");

        LOG.debug("Processing monitorKey: {} for received packet", monitorKey);

        final Semaphore lock = lockMap.get(monitorKey);
        LOG.debug("Acquiring lock for monitor key : {} to process monitor packet", monitorKey);
        acquireLock(lock);

        txRunner.applyWithNewReadWriteTransactionAndSubmit(OPERATIONAL, tx -> {
            Optional<MonitoringState> optState = tx.read(getMonitorStateId(monitorKey)).get();
            if (optState.isPresent()) {
                final MonitoringState currentState = optState.get();

                if (LOG.isTraceEnabled()) {
                    LOG.trace("OnPacketReceived : Monitoring state from ODS : {} ", currentState);
                }

                // Long responsePendingCount = currentState.getResponsePendingCount();
                //
                // Need to relook at the pending count logic to support N
                // out of M scenarios
                // if (currentState.getState() != LivenessState.Up) {
                // //Reset responsePendingCount when state changes from DOWN
                // to UP
                // responsePendingCount = INITIAL_COUNT;
                // }
                //
                // if (responsePendingCount > INITIAL_COUNT) {
                // responsePendingCount =
                // currentState.getResponsePendingCount() - 1;
                // }
                Long responsePendingCount = INITIAL_COUNT;

                final MonitoringState state = new MonitoringStateBuilder().setMonitorKey(monitorKey)
                        .setState(LivenessState.Up).setResponsePendingCount(responsePendingCount).build();
                tx.merge(getMonitorStateId(monitorKey), state);

                return Optional.of(new Result(currentState, state));
            } else {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Monitoring State not available for key: {} to process the Packet received",
                            monitorKey);
                }
                return Optional.<Result>absent();
            }
        }).addCallback(new FutureCallback<Optional<Result>>() {
            @Override
            public void onSuccess(Optional<Result> optResult) {
                releaseLock(lock);
                optResult.toJavaUtil().ifPresent(result -> {
                    final boolean stateChanged = result.currentState.getState() == LivenessState.Down
                            || result.currentState.getState() == LivenessState.Unknown;
                    if (stateChanged) {
                        // send notifications
                        if (LOG.isTraceEnabled()) {
                            LOG.trace("Sending notification for monitor Id : {} with Current State: {}",
                                    result.currentState.getMonitorId(), LivenessState.Up);
                        }
                        publishNotification(result.currentState.getMonitorId(), LivenessState.Up);
                    } else {
                        if (LOG.isTraceEnabled()) {
                            LOG.trace("Successful in writing monitoring state {} to ODS", result.state);
                        }
                    }
                });
            }

            @Override
            public void onFailure(Throwable error) {
                releaseLock(lock);
                LOG.warn("Error in reading or writing monitoring state : {} to Datastore", monitorKey, error);
            }
        }, callbackExecutorService);
    }

    private String getUniqueKey(String interfaceName, String protocolType, EndpointType source,
            EndpointType destination) {
        StringBuilder builder = new StringBuilder().append(interfaceName).append(AlivenessMonitorConstants.SEPERATOR)
                .append(protocolType);
        if (source != null) {
            builder.append(AlivenessMonitorConstants.SEPERATOR).append(AlivenessMonitorUtil.getIpAddress(source));
        }

        if (destination != null) {
            builder.append(AlivenessMonitorConstants.SEPERATOR).append(AlivenessMonitorUtil.getIpAddress(destination));
        }
        return builder.toString();
    }

    @Override
    public ListenableFuture<RpcResult<MonitorStartOutput>> monitorStart(MonitorStartInput input) {
        RpcResultBuilder<MonitorStartOutput> rpcResultBuilder;
        final Config in = input.getConfig();
        Long profileId = in.getProfileId();
        LOG.debug("Monitor Start invoked with Config: {}, Profile Id: {}", in, profileId);

        try {
            if (in.getMode() != MonitoringMode.OneOne) {
                throw new UnsupportedConfigException(
                        "Unsupported Monitoring mode. Currently one-one mode is supported");
            }

            Optional<MonitorProfile> optProfile =
                    SingleTransactionDataBroker.syncReadOptionalAndTreatReadFailedExceptionAsAbsentOptional(
                            dataBroker, LogicalDatastoreType.OPERATIONAL, getMonitorProfileId(profileId));
            final MonitorProfile profile;
            if (!optProfile.isPresent()) {
                String errMsg = String.format("No monitoring profile associated with Id: %d", profileId);
                LOG.error("Monitor start failed. {}", errMsg);
                throw new RuntimeException(errMsg);
            } else {
                profile = optProfile.get();
            }

            final MonitorProtocolType protocolType = profile.getProtocolType();

            String interfaceName = null;
            EndpointType srcEndpointType = in.getSource().getEndpointType();

            if (srcEndpointType instanceof Interface) {
                Interface endPoint = (Interface) srcEndpointType;
                interfaceName = endPoint.getInterfaceName();
            } else {
                throw new UnsupportedConfigException(
                        "Unsupported source Endpoint type. Only Interface Endpoint currently supported for monitoring");
            }

            if (Strings.isNullOrEmpty(interfaceName)) {
                throw new RuntimeException("Interface Name not defined in the source Endpoint");
            }

            // Initially the support is for one monitoring per interface.
            // Revisit the retrieving monitor id logic when the multiple
            // monitoring for same interface is needed.
            EndpointType destEndpointType = null;
            if (in.getDestination() != null) {
                destEndpointType = in.getDestination().getEndpointType();
            }
            String idKey = getUniqueKey(interfaceName, protocolType.toString(), srcEndpointType, destEndpointType);
            final long monitorId = getUniqueId(idKey);
            Optional<MonitoringInfo> optKey =
                    SingleTransactionDataBroker.syncReadOptionalAndTreatReadFailedExceptionAsAbsentOptional(
                            dataBroker, LogicalDatastoreType.OPERATIONAL, getMonitoringInfoId(monitorId));
            final AlivenessProtocolHandler<?> handler;
            if (optKey.isPresent()) {
                String message = String.format(
                        "Monitoring for the interface %s with this configuration " + "is already registered.",
                        interfaceName);
                LOG.warn("Monitoring for the interface {} with this configuration is already registered.",
                        interfaceName);
                MonitorStartOutput output = new MonitorStartOutputBuilder().setMonitorId(monitorId).build();
                rpcResultBuilder = RpcResultBuilder.success(output).withWarning(ErrorType.APPLICATION,
                        "config-exists", message);
                return Futures.immediateFuture(rpcResultBuilder.build());
            } else {
                // Construct the monitor key
                final MonitoringInfo monitoringInfo = new MonitoringInfoBuilder().setId(monitorId).setMode(in.getMode())
                        .setProfileId(profileId).setDestination(in.getDestination()).setSource(in.getSource()).build();
                // Construct the initial monitor state
                handler = alivenessProtocolHandlerRegistry.get(protocolType);
                final String monitoringKey = handler.getUniqueMonitoringKey(monitoringInfo);

                MonitoringStateBuilder monitoringStateBuilder =
                        new MonitoringStateBuilder().setMonitorKey(monitoringKey).setMonitorId(monitorId)
                                .setState(LivenessState.Unknown).setStatus(MonitorStatus.Started);
                if (protocolType != MonitorProtocolType.Bfd) {
                    monitoringStateBuilder.setRequestCount(INITIAL_COUNT).setResponsePendingCount(INITIAL_COUNT);
                }
                MonitoringState monitoringState = monitoringStateBuilder.build();

                txRunner.callWithNewWriteOnlyTransactionAndSubmit(OPERATIONAL, operTx -> {
                    operTx.put(getMonitoringInfoId(monitorId), monitoringInfo, CREATE_MISSING_PARENT);
                    LOG.debug("adding oper monitoring info {}", monitoringInfo);

                    operTx.put(getMonitorStateId(monitoringKey), monitoringState, CREATE_MISSING_PARENT);
                    LOG.debug("adding oper monitoring state {}", monitoringState);

                    MonitoridKeyEntry mapEntry = new MonitoridKeyEntryBuilder().setMonitorId(monitorId)
                            .setMonitorKey(monitoringKey).build();
                    operTx.put(getMonitorMapId(monitorId), mapEntry, CREATE_MISSING_PARENT);
                    LOG.debug("adding oper map entry {}", mapEntry);
                }).addCallback(new FutureCallback<Void>() {
                    @Override
                    public void onFailure(Throwable error) {
                        String errorMsg = String.format("Adding Monitoring info: %s in Datastore failed",
                                monitoringInfo);
                        LOG.warn("Adding Monitoring info: {} in Datastore failed", monitoringInfo, error);
                        throw new RuntimeException(errorMsg, error);
                    }

                    @Override
                    public void onSuccess(Void ignored) {
                        lockMap.put(monitoringKey, new Semaphore(1, true));
                        if (protocolType == MonitorProtocolType.Bfd) {
                            handler.startMonitoringTask(monitoringInfo);
                            return;
                        }
                        // Schedule task
                        LOG.debug("Scheduling monitor task for config: {}", in);
                        scheduleMonitoringTask(monitoringInfo, profile.getMonitorInterval());
                    }
                }, callbackExecutorService);
            }

            associateMonitorIdWithInterface(monitorId, interfaceName);

            MonitorStartOutput output = new MonitorStartOutputBuilder().setMonitorId(monitorId).build();

            rpcResultBuilder = RpcResultBuilder.success(output);
        } catch (UnsupportedConfigException e) {
            LOG.error("Start Monitoring Failed. ", e);
            rpcResultBuilder = RpcResultBuilder.<MonitorStartOutput>failed().withError(ErrorType.APPLICATION,
                    e.getMessage(), e);
        }
        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    private void associateMonitorIdWithInterface(final Long monitorId, final String interfaceName) {
        LOG.debug("associate monitor Id {} with interface {}", monitorId, interfaceName);
        final ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        ListenableFuture<Optional<InterfaceMonitorEntry>> readFuture = tx.read(LogicalDatastoreType.OPERATIONAL,
                getInterfaceMonitorMapId(interfaceName));
        ListenableFuture<Void> updateFuture = Futures.transformAsync(readFuture, optEntry -> {
            if (optEntry.isPresent()) {
                InterfaceMonitorEntry entry = optEntry.get();
                List<Long> monitorIds1 = new ArrayList<>(nonnull(entry.getMonitorIds()));
                monitorIds1.add(monitorId);
                InterfaceMonitorEntry newEntry1 = new InterfaceMonitorEntryBuilder()
                         .withKey(new InterfaceMonitorEntryKey(interfaceName)).setMonitorIds(monitorIds1).build();
                tx.merge(LogicalDatastoreType.OPERATIONAL, getInterfaceMonitorMapId(interfaceName), newEntry1);
            } else {
                    // Create new monitor entry
                LOG.debug("Adding new interface-monitor association for interface {} with id {}", interfaceName,
                            monitorId);
                List<Long> monitorIds2 = new ArrayList<>();
                monitorIds2.add(monitorId);
                InterfaceMonitorEntry newEntry2 = new InterfaceMonitorEntryBuilder()
                            .setInterfaceName(interfaceName).setMonitorIds(monitorIds2).build();
                tx.put(LogicalDatastoreType.OPERATIONAL, getInterfaceMonitorMapId(interfaceName), newEntry2,
                            CREATE_MISSING_PARENT);
            }
            return tx.submit();
        }, callbackExecutorService);

        Futures.addCallback(updateFuture, new FutureCallbackImpl(
                String.format("Association of monitorId %d with Interface %s", monitorId, interfaceName)),
                MoreExecutors.directExecutor());
    }

    private void scheduleMonitoringTask(MonitoringInfo monitoringInfo, long monitorInterval) {
        AlivenessMonitorTask monitorTask = new AlivenessMonitorTask(monitoringInfo);
        ScheduledFuture<?> scheduledFutureResult = monitorService.scheduleAtFixedRate(monitorTask, NO_DELAY,
                monitorInterval, TimeUnit.MILLISECONDS);
        monitoringTasks.put(monitoringInfo.getId(), scheduledFutureResult);
    }

    @Override
    public ListenableFuture<RpcResult<MonitorPauseOutput>> monitorPause(MonitorPauseInput input) {
        LOG.debug("Monitor Pause operation invoked for monitor id: {}", input.getMonitorId());
        SettableFuture<RpcResult<MonitorPauseOutput>> result = SettableFuture.create();
        final Long monitorId = input.getMonitorId();

        // Set the monitoring status to Paused
        updateMonitorStatusTo(monitorId, MonitorStatus.Paused, currentStatus -> currentStatus == MonitorStatus.Started);

        if (stopMonitoringTask(monitorId)) {
            result.set(RpcResultBuilder.<MonitorPauseOutput>success().build());
        } else {
            String errorMsg = String.format("No Monitoring Task availble to pause for the given monitor id : %d",
                    monitorId);
            LOG.error("Monitor Pause operation failed- {}", errorMsg);
            result.set(RpcResultBuilder.<MonitorPauseOutput>failed()
                    .withError(ErrorType.APPLICATION, errorMsg).build());
        }

        return result;
    }

    @Override
    public ListenableFuture<RpcResult<MonitorUnpauseOutput>> monitorUnpause(MonitorUnpauseInput input) {
        LOG.debug("Monitor Unpause operation invoked for monitor id: {}", input.getMonitorId());
        final SettableFuture<RpcResult<MonitorUnpauseOutput>> result = SettableFuture.create();

        final Long monitorId = input.getMonitorId();
        final ReadOnlyTransaction tx = dataBroker.newReadOnlyTransaction();
        ListenableFuture<Optional<MonitoringInfo>> readInfoResult = tx.read(LogicalDatastoreType.OPERATIONAL,
                getMonitoringInfoId(monitorId));

        Futures.addCallback(readInfoResult, new FutureCallback<Optional<MonitoringInfo>>() {

            @Override
            public void onFailure(Throwable error) {
                tx.close();
                String msg = String.format("Unable to read monitoring info associated with monitor id %d", monitorId);
                LOG.error("Monitor unpause Failed. {}", msg, error);
                result.set(RpcResultBuilder.<MonitorUnpauseOutput>failed()
                        .withError(ErrorType.APPLICATION, msg, error).build());
            }

            @Override
            public void onSuccess(@Nonnull Optional<MonitoringInfo> optInfo) {
                if (optInfo.isPresent()) {
                    final MonitoringInfo info = optInfo.get();
                    ListenableFuture<Optional<MonitorProfile>> readProfile = tx.read(LogicalDatastoreType.OPERATIONAL,
                            getMonitorProfileId(info.getProfileId()));
                    Futures.addCallback(readProfile, new FutureCallback<Optional<MonitorProfile>>() {

                        @Override
                        public void onFailure(Throwable error) {
                            tx.close();
                            String msg = String.format("Unable to read Monitoring profile associated with id %d",
                                    info.getProfileId());
                            LOG.warn("Monitor unpause Failed. {}", msg, error);
                            result.set(RpcResultBuilder.<MonitorUnpauseOutput>failed()
                                    .withError(ErrorType.APPLICATION, msg, error).build());
                        }

                        @Override
                        public void onSuccess(@Nonnull Optional<MonitorProfile> optProfile) {
                            tx.close();
                            if (optProfile.isPresent()) {
                                updateMonitorStatusTo(monitorId, MonitorStatus.Started,
                                    currentStatus -> (currentStatus == MonitorStatus.Paused
                                                || currentStatus == MonitorStatus.Stopped));
                                MonitorProfile profile = optProfile.get();
                                LOG.debug("Monitor Resume - Scheduling monitoring task with Id: {}", monitorId);
                                MonitorProtocolType protocolType = profile.getProtocolType();
                                if (protocolType == MonitorProtocolType.Bfd) {
                                    LOG.debug("disabling bfd for hwvtep tunnel montior id {}", monitorId);
                                    ((HwVtepTunnelsStateHandler) alivenessProtocolHandlerRegistry.get(protocolType))
                                            .resetMonitoringTask(true);
                                } else {
                                    scheduleMonitoringTask(info, profile.getMonitorInterval());
                                }
                                result.set(RpcResultBuilder.<MonitorUnpauseOutput>success().build());
                            } else {
                                String msg = String.format("Monitoring profile associated with id %d is not present",
                                        info.getProfileId());
                                LOG.warn("Monitor unpause Failed. {}", msg);
                                result.set(
                                        RpcResultBuilder.<MonitorUnpauseOutput>failed()
                                                .withError(ErrorType.APPLICATION, msg).build());
                            }
                        }
                    }, callbackExecutorService);
                } else {
                    tx.close();
                    String msg = String.format("Monitoring info associated with id %d is not present", monitorId);
                    LOG.warn("Monitor unpause Failed. {}", msg);
                    result.set(RpcResultBuilder.<MonitorUnpauseOutput>failed()
                            .withError(ErrorType.APPLICATION, msg).build());
                }
            }
        }, callbackExecutorService);

        return result;
    }

    private boolean stopMonitoringTask(Long monitorId) {
        return stopMonitoringTask(monitorId, INTERRUPT_TASK);
    }

    private boolean stopMonitoringTask(Long monitorId, boolean interruptTask) {
        Optional<MonitoringInfo> optInfo =
                SingleTransactionDataBroker.syncReadOptionalAndTreatReadFailedExceptionAsAbsentOptional(dataBroker,
                        LogicalDatastoreType.OPERATIONAL, getMonitoringInfoId(monitorId));
        if (!optInfo.isPresent()) {
            LOG.warn("There is no monitoring info present for monitor id {}", monitorId);
            return false;
        }
        MonitoringInfo monitoringInfo = optInfo.get();
        Optional<MonitorProfile> optProfile =
                SingleTransactionDataBroker.syncReadOptionalAndTreatReadFailedExceptionAsAbsentOptional(dataBroker,
                        LogicalDatastoreType.OPERATIONAL, getMonitorProfileId(monitoringInfo.getProfileId()));
        MonitorProtocolType protocolType = optProfile.get().getProtocolType();
        if (protocolType == MonitorProtocolType.Bfd) {
            LOG.debug("disabling bfd for hwvtep tunnel montior id {}", monitorId);
            ((HwVtepTunnelsStateHandler) alivenessProtocolHandlerRegistry.get(protocolType))
                    .resetMonitoringTask(false);
            return true;
        }
        ScheduledFuture<?> scheduledFutureResult = monitoringTasks.remove(monitorId);
        if (scheduledFutureResult != null) {
            scheduledFutureResult.cancel(interruptTask);
            return true;
        }
        return false;
    }

    Optional<MonitorProfile> getMonitorProfile(Long profileId) {
        return SingleTransactionDataBroker.syncReadOptionalAndTreatReadFailedExceptionAsAbsentOptional(
                dataBroker, LogicalDatastoreType.OPERATIONAL, getMonitorProfileId(profileId));
    }

    void acquireLock(Semaphore lock) {
        if (lock == null) {
            return;
        }

        boolean acquiredLock = false;
        try {
            acquiredLock = lock.tryAcquire(50, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LOG.warn("Thread interrupted when waiting to acquire the lock");
        }

        if (!acquiredLock) {
            LOG.warn("Previous transaction did not complete in time. Releasing the lock to proceed");
            lock.release();
            try {
                lock.acquire();
                LOG.trace("Lock acquired successfully");
            } catch (InterruptedException e) {
                LOG.warn("Acquire failed");
            }
        } else {
            LOG.trace("Lock acquired successfully");
        }
    }

    void releaseLock(Semaphore lock) {
        if (lock != null) {
            lock.release();
        }
    }

    private void sendMonitorPacket(final MonitoringInfo monitoringInfo) {
        // TODO: Handle interrupts
        final Long monitorId = monitoringInfo.getId();
        final String monitorKey = monitorIdKeyCache.getUnchecked(monitorId);
        if (monitorKey == null) {
            LOG.warn("No monitor Key associated with id {} to send the monitor packet", monitorId);
            return;
        } else {
            LOG.debug("Sending monitoring packet for key: {}", monitorKey);
        }

        final MonitorProfile profile;
        Optional<MonitorProfile> optProfile = getMonitorProfile(monitoringInfo.getProfileId());
        if (optProfile.isPresent()) {
            profile = optProfile.get();
        } else {
            LOG.warn("No monitor profile associated with id {}. " + "Could not send Monitor packet for monitor-id {}",
                    monitoringInfo.getProfileId(), monitorId);
            return;
        }

        final Semaphore lock = lockMap.get(monitorKey);
        LOG.debug("Acquiring lock for monitor key : {} to send monitor packet", monitorKey);
        acquireLock(lock);

        final ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        ListenableFuture<Optional<MonitoringState>> readResult = tx.read(LogicalDatastoreType.OPERATIONAL,
                getMonitorStateId(monitorKey));
        ListenableFuture<Void> writeResult = Futures.transformAsync(readResult, optState -> {
            if (optState.isPresent()) {
                MonitoringState state = optState.get();

                // Increase the request count
                Long requestCount = state.getRequestCount() + 1;

                // Check with the monitor window
                LivenessState currentLivenessState = state.getState();

                // Increase the pending response count
                long responsePendingCount = state.getResponsePendingCount();
                if (responsePendingCount < profile.getMonitorWindow()) {
                    responsePendingCount = responsePendingCount + 1;
                }

                // Check with the failure threshold
                if (responsePendingCount >= profile.getFailureThreshold()) {
                    // Change the state to down and notify
                    if (currentLivenessState != LivenessState.Down) {
                        LOG.debug("Response pending Count: {}, Failure threshold: {} for monitorId {}",
                                responsePendingCount, profile.getFailureThreshold(), state.getMonitorId());
                        LOG.info("Sending notification for monitor Id : {} with State: {}",
                                state.getMonitorId(), LivenessState.Down);
                        publishNotification(monitorId, LivenessState.Down);
                        currentLivenessState = LivenessState.Down;
                        // Reset requestCount when state changes
                        // from UP to DOWN
                        requestCount = INITIAL_COUNT;
                    }
                }

                // Update the ODS with state
                MonitoringState updatedState = new MonitoringStateBuilder(/* state */)
                            .setMonitorKey(state.getMonitorKey()).setRequestCount(requestCount)
                            .setResponsePendingCount(responsePendingCount).setState(currentLivenessState).build();
                tx.merge(LogicalDatastoreType.OPERATIONAL, getMonitorStateId(state.getMonitorKey()),
                            updatedState);
                return tx.submit();
            } else {
                // Close the transaction
                tx.submit();
                String errorMsg = String.format(
                        "Monitoring State associated with id %d is not present to send packet out.", monitorId);
                return Futures.immediateFailedFuture(new RuntimeException(errorMsg));
            }
        }, callbackExecutorService);

        Futures.addCallback(writeResult, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void noarg) {
                // invoke packetout on protocol handler
                AlivenessProtocolHandler<?> handler =
                        alivenessProtocolHandlerRegistry.getOpt(profile.getProtocolType());
                if (handler != null) {
                    LOG.debug("Sending monitoring packet {}", monitoringInfo);
                    handler.startMonitoringTask(monitoringInfo);
                }
                releaseLock(lock);
            }

            @Override
            public void onFailure(Throwable error) {
                LOG.warn("Updating monitoring state for key: {} failed. Monitoring packet is not sent", monitorKey,
                        error);
                releaseLock(lock);
            }
        }, callbackExecutorService);
    }

    void publishNotification(final Long monitorId, final LivenessState state) {
        LOG.debug("Sending notification for id {}  - state {}", monitorId, state);
        EventData data = new EventDataBuilder().setMonitorId(monitorId).setMonitorState(state).build();
        MonitorEvent event = new MonitorEventBuilder().setEventData(data).build();
        final ListenableFuture<?> eventFuture = notificationPublishService.offerNotification(event);
        Futures.addCallback(eventFuture, new FutureCallback<Object>() {
            @Override
            public void onFailure(Throwable error) {
                LOG.warn("Error in notifying listeners for id {} - state {}", monitorId, state, error);
            }

            @Override
            public void onSuccess(Object arg) {
                LOG.trace("Successful in notifying listeners for id {} - state {}", monitorId, state);
            }
        }, callbackExecutorService);
    }

    @Override
    public ListenableFuture<RpcResult<MonitorProfileCreateOutput>> monitorProfileCreate(
            final MonitorProfileCreateInput input) {
        LOG.debug("Monitor Profile Create operation - {}", input.getProfile());
        final SettableFuture<RpcResult<MonitorProfileCreateOutput>> returnFuture = SettableFuture.create();
        Profile profile = input.getProfile();
        final Long failureThreshold = profile.getFailureThreshold();
        final Long monitorInterval = profile.getMonitorInterval();
        final Long monitorWindow = profile.getMonitorWindow();
        final MonitorProtocolType protocolType = profile.getProtocolType();
        String idKey = getUniqueProfileKey(failureThreshold, monitorInterval, monitorWindow, protocolType);
        final Long profileId = (long) getUniqueId(idKey);

        final ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        ListenableFuture<Optional<MonitorProfile>> readFuture = tx.read(LogicalDatastoreType.OPERATIONAL,
                getMonitorProfileId(profileId));
        ListenableFuture<RpcResult<MonitorProfileCreateOutput>> resultFuture = Futures.transformAsync(readFuture,
            optProfile -> {
                if (optProfile.isPresent()) {
                    tx.cancel();
                    MonitorProfileCreateOutput output = new MonitorProfileCreateOutputBuilder()
                                .setProfileId(profileId).build();
                    String msg = String.format("Monitor profile %s already present for the given input", input);
                    LOG.warn(msg);
                    returnFuture.set(RpcResultBuilder.success(output)
                                .withWarning(ErrorType.PROTOCOL, "profile-exists", msg).build());
                } else {
                    final MonitorProfile monitorProfile = new MonitorProfileBuilder().setId(profileId)
                                .setFailureThreshold(failureThreshold).setMonitorInterval(monitorInterval)
                                .setMonitorWindow(monitorWindow).setProtocolType(protocolType).build();
                    tx.put(LogicalDatastoreType.OPERATIONAL, getMonitorProfileId(profileId), monitorProfile,
                          CREATE_MISSING_PARENT);
                    Futures.addCallback(tx.submit(), new FutureCallback<Void>() {
                            @Override
                            public void onFailure(Throwable error) {
                                String msg = String.format("Error when storing monitorprofile %s in datastore",
                                        monitorProfile);
                                LOG.error("Error when storing monitorprofile {} in datastore", monitorProfile, error);
                                returnFuture.set(RpcResultBuilder.<MonitorProfileCreateOutput>failed()
                                        .withError(ErrorType.APPLICATION, msg, error).build());
                            }

                            @Override
                            public void onSuccess(Void noarg) {
                                MonitorProfileCreateOutput output = new MonitorProfileCreateOutputBuilder()
                                        .setProfileId(profileId).build();
                                returnFuture.set(RpcResultBuilder.success(output).build());
                            }
                        }, callbackExecutorService);
                }
                return returnFuture;
            }, callbackExecutorService);
        Futures.addCallback(resultFuture, new FutureCallback<RpcResult<MonitorProfileCreateOutput>>() {
            @Override
            public void onFailure(Throwable error) {
                // This would happen when any error happens during reading for
                // monitoring profile
                String msg = String.format("Error in creating monitorprofile - %s", input);
                returnFuture.set(RpcResultBuilder.<MonitorProfileCreateOutput>failed()
                        .withError(ErrorType.APPLICATION, msg, error).build());
                LOG.error("Error in creating monitorprofile - {} ", input, error);
            }

            @Override
            public void onSuccess(RpcResult<MonitorProfileCreateOutput> result) {
                LOG.debug("Successfully created monitor Profile {} ", input);
            }
        }, callbackExecutorService);
        return returnFuture;
    }

    @Override
    public ListenableFuture<RpcResult<MonitorProfileGetOutput>> monitorProfileGet(MonitorProfileGetInput input) {
        LOG.debug("Monitor Profile Get operation for input profile- {}", input.getProfile());
        RpcResultBuilder<MonitorProfileGetOutput> rpcResultBuilder;
        final Long profileId = getExistingProfileId(input);

        MonitorProfileGetOutputBuilder output = new MonitorProfileGetOutputBuilder().setProfileId(profileId);
        rpcResultBuilder = RpcResultBuilder.success();
        rpcResultBuilder.withResult(output.build());
        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    private Long getExistingProfileId(MonitorProfileGetInput input) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.genius
            .alivenessmonitor.rev160411.monitor.profile.get.input.Profile profile = input
                .getProfile();
        final Long failureThreshold = profile.getFailureThreshold();
        final Long monitorInterval = profile.getMonitorInterval();
        final Long monitorWindow = profile.getMonitorWindow();
        final MonitorProtocolType protocolType = profile.getProtocolType();
        LOG.debug("getExistingProfileId for profile : {}", input.getProfile());
        String idKey = getUniqueProfileKey(failureThreshold, monitorInterval, monitorWindow, protocolType);
        LOG.debug("Obtained existing profile ID for profile : {}", input.getProfile());
        return (long) getUniqueId(idKey);
    }

    private String getUniqueProfileKey(Long failureThreshold, Long monitorInterval, Long monitorWindow,
            MonitorProtocolType protocolType) {
        return String.valueOf(failureThreshold) + AlivenessMonitorConstants.SEPERATOR + monitorInterval
                + AlivenessMonitorConstants.SEPERATOR + monitorWindow + AlivenessMonitorConstants.SEPERATOR
                + protocolType + AlivenessMonitorConstants.SEPERATOR;
    }

    @Override
    public ListenableFuture<RpcResult<MonitorProfileDeleteOutput>> monitorProfileDelete(
            final MonitorProfileDeleteInput input) {
        LOG.debug("Monitor Profile delete for Id: {}", input.getProfileId());
        final SettableFuture<RpcResult<MonitorProfileDeleteOutput>> result = SettableFuture.create();
        final Long profileId = input.getProfileId();
        final ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        ListenableFuture<Optional<MonitorProfile>> readFuture = tx.read(LogicalDatastoreType.OPERATIONAL,
                getMonitorProfileId(profileId));
        ListenableFuture<RpcResult<MonitorProfileDeleteOutput>> writeFuture =
                Futures.transformAsync(readFuture, optProfile -> {
                    if (optProfile.isPresent()) {
                        tx.delete(LogicalDatastoreType.OPERATIONAL, getMonitorProfileId(profileId));
                        Futures.addCallback(tx.submit(), new FutureCallback<Void>() {
                            @Override
                            public void onFailure(Throwable error) {
                                String msg = String.format("Error when removing monitor profile %d from datastore",
                                        profileId);
                                LOG.error("Error when removing monitor profile {} from datastore", profileId, error);
                                result.set(RpcResultBuilder.<MonitorProfileDeleteOutput>failed()
                                        .withError(ErrorType.APPLICATION, msg, error)
                                        .build());
                            }

                            @Override
                            public void onSuccess(Void noarg) {
                                MonitorProfile profile = optProfile.get();
                                String id = getUniqueProfileKey(profile.getFailureThreshold(),
                                        profile.getMonitorInterval(), profile.getMonitorWindow(),
                                        profile.getProtocolType());
                                releaseId(id);
                                result.set(RpcResultBuilder.<MonitorProfileDeleteOutput>success().build());
                            }
                        }, callbackExecutorService);
                    } else {
                        String msg = String.format("Monitor profile with Id: %d does not exist", profileId);
                        LOG.info(msg);
                        result.set(RpcResultBuilder.<MonitorProfileDeleteOutput>success()
                                .withWarning(ErrorType.PROTOCOL, "invalid-value", msg).build());
                    }
                    return result;
                }, callbackExecutorService);

        Futures.addCallback(writeFuture, new FutureCallback<RpcResult<MonitorProfileDeleteOutput>>() {

            @Override
            public void onFailure(Throwable error) {
                String msg = String.format("Error when removing monitor profile %d from datastore", profileId);
                LOG.error("Error when removing monitor profile {} from datastore", profileId, error);
                result.set(RpcResultBuilder.<MonitorProfileDeleteOutput>failed()
                        .withError(ErrorType.APPLICATION, msg, error).build());
            }

            @Override
            public void onSuccess(RpcResult<MonitorProfileDeleteOutput> noarg) {
                LOG.debug("Successfully removed Monitor Profile {}", profileId);
            }
        }, callbackExecutorService);
        return result;
    }

    @Override
    public ListenableFuture<RpcResult<MonitorStopOutput>> monitorStop(MonitorStopInput input) {
        LOG.debug("Monitor Stop operation for monitor id - {}", input.getMonitorId());
        SettableFuture<RpcResult<MonitorStopOutput>> result = SettableFuture.create();

        final Long monitorId = input.getMonitorId();
        Optional<MonitoringInfo> optInfo =
                SingleTransactionDataBroker.syncReadOptionalAndTreatReadFailedExceptionAsAbsentOptional(dataBroker,
                        LogicalDatastoreType.OPERATIONAL, getMonitoringInfoId(monitorId));
        if (optInfo.isPresent()) {
            // Stop the monitoring task
            stopMonitoringTask(monitorId);

            String monitorKey = monitorIdKeyCache.getUnchecked(monitorId);

            // Cleanup the Data store
            txRunner.callWithNewWriteOnlyTransactionAndSubmit(OPERATIONAL, tx -> {
                if (monitorKey != null) {
                    tx.delete(getMonitorStateId(monitorKey));
                    monitorIdKeyCache.invalidate(monitorId);
                }

                tx.delete(getMonitoringInfoId(monitorId));

                //Remove monitorid-key-map
                tx.delete(getMonitorMapId(monitorId));
            }).addCallback(new FutureCallbackImpl(String.format("Delete monitor state with Id %d", monitorId)),
                    MoreExecutors.directExecutor());

            MonitoringInfo info = optInfo.get();
            String interfaceName = getInterfaceName(info.getSource().getEndpointType());
            if (interfaceName != null) {
                removeMonitorIdFromInterfaceAssociation(monitorId, interfaceName);
            }
            releaseIdForMonitoringInfo(info);

            if (monitorKey != null) {
                lockMap.remove(monitorKey);
            }

            result.set(RpcResultBuilder.<MonitorStopOutput>success().build());
        } else {
            String errorMsg = String.format("Do not have monitoring information associated with key %d", monitorId);
            LOG.error("Delete monitoring operation Failed - {}", errorMsg);
            result.set(RpcResultBuilder.<MonitorStopOutput>failed().withError(ErrorType.APPLICATION, errorMsg).build());
        }

        return result;
    }

    private void removeMonitorIdFromInterfaceAssociation(final Long monitorId, final String interfaceName) {
        LOG.debug("Remove monitorId {} from Interface association {}", monitorId, interfaceName);
        final ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        ListenableFuture<Optional<InterfaceMonitorEntry>> readFuture = tx.read(LogicalDatastoreType.OPERATIONAL,
                getInterfaceMonitorMapId(interfaceName));
        ListenableFuture<Void> updateFuture = Futures.transformAsync(readFuture, optEntry -> {
            if (optEntry.isPresent()) {
                InterfaceMonitorEntry entry = optEntry.get();
                List<Long> monitorIds = new ArrayList<>(nonnull(entry.getMonitorIds()));
                monitorIds.remove(monitorId);
                if (monitorIds.isEmpty()) {
                    tx.delete(LogicalDatastoreType.OPERATIONAL, getInterfaceMonitorMapId(interfaceName));
                } else {
                    InterfaceMonitorEntry newEntry = new InterfaceMonitorEntryBuilder(entry)
                            .withKey(new InterfaceMonitorEntryKey(interfaceName)).setMonitorIds(monitorIds).build();
                    tx.put(LogicalDatastoreType.OPERATIONAL, getInterfaceMonitorMapId(interfaceName), newEntry,
                            CREATE_MISSING_PARENT);
                }
                return tx.submit();
            } else {
                LOG.warn("No Interface map entry found {} to remove monitorId {}", interfaceName, monitorId);
                tx.cancel();
                return Futures.immediateFuture(null);
            }
        }, MoreExecutors.directExecutor());

        Futures.addCallback(updateFuture, new FutureCallbackImpl(
                String.format("Dis-association of monitorId %d with Interface %s", monitorId, interfaceName)),
                MoreExecutors.directExecutor());
    }

    private void releaseIdForMonitoringInfo(MonitoringInfo info) {
        Long monitorId = info.getId();
        EndpointType source = info.getSource().getEndpointType();
        String interfaceName = getInterfaceName(source);
        if (!Strings.isNullOrEmpty(interfaceName)) {
            Optional<MonitorProfile> optProfile =
                    SingleTransactionDataBroker.syncReadOptionalAndTreatReadFailedExceptionAsAbsentOptional(dataBroker,
                            LogicalDatastoreType.OPERATIONAL, getMonitorProfileId(info.getProfileId()));
            if (optProfile.isPresent()) {
                MonitorProtocolType protocolType = optProfile.get().getProtocolType();
                EndpointType destination = info.getDestination() != null ? info.getDestination().getEndpointType()
                        : null;
                String idKey = getUniqueKey(interfaceName, protocolType.toString(), source, destination);
                releaseId(idKey);
            } else {
                LOG.warn("Could not release monitorId {}. No profile associated with it", monitorId);
            }
        }
    }

    private String getInterfaceName(EndpointType endpoint) {
        String interfaceName = null;
        if (endpoint instanceof Interface) {
            interfaceName = ((Interface) endpoint).getInterfaceName();
        }
        return interfaceName;
    }

    private void stopMonitoring(long monitorId) {
        updateMonitorStatusTo(monitorId, MonitorStatus.Stopped,
            currentStatus -> currentStatus != MonitorStatus.Stopped);
        if (!stopMonitoringTask(monitorId)) {
            LOG.warn("No monitoring task running to perform cancel operation for monitorId {}", monitorId);
        }
    }

    private void updateMonitorStatusTo(final Long monitorId, final MonitorStatus newStatus,
            final Predicate<MonitorStatus> isValidStatus) {
        final String monitorKey = monitorIdKeyCache.getUnchecked(monitorId);
        if (monitorKey == null) {
            LOG.warn("No monitor Key associated with id {} to change the monitor status to {}", monitorId, newStatus);
            return;
        }
        final ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();

        ListenableFuture<Optional<MonitoringState>> readResult = tx.read(LogicalDatastoreType.OPERATIONAL,
                getMonitorStateId(monitorKey));

        ListenableFuture<Void> writeResult = Futures.transformAsync(readResult, optState -> {
            if (optState.isPresent()) {
                MonitoringState state = optState.get();
                if (isValidStatus.apply(state.getStatus())) {
                    MonitoringState updatedState = new MonitoringStateBuilder().setMonitorKey(monitorKey)
                                .setStatus(newStatus).build();
                    tx.merge(LogicalDatastoreType.OPERATIONAL, getMonitorStateId(monitorKey), updatedState);
                } else {
                    LOG.warn("Invalid Monitoring status {}, cannot be updated to {} for monitorId {}",
                                state.getStatus(), newStatus, monitorId);
                }
            } else {
                LOG.warn("No associated monitoring state data available to update the status to {} for {}",
                           newStatus, monitorId);
            }
            return tx.submit();
        }, MoreExecutors.directExecutor());

        Futures.addCallback(writeResult, new FutureCallbackImpl(
                String.format("Monitor status update for %d to %s", monitorId, newStatus.toString())),
                MoreExecutors.directExecutor());
    }

    private void resumeMonitoring(final long monitorId) {
        final ReadOnlyTransaction tx = dataBroker.newReadOnlyTransaction();
        ListenableFuture<Optional<MonitoringInfo>> readInfoResult = tx.read(LogicalDatastoreType.OPERATIONAL,
                getMonitoringInfoId(monitorId));

        Futures.addCallback(readInfoResult, new FutureCallback<Optional<MonitoringInfo>>() {

            @Override
            public void onFailure(Throwable error) {
                String msg = String.format("Unable to read monitoring info associated with monitor id %d", monitorId);
                LOG.error("Monitor resume Failed. {}", msg, error);
                tx.close();
            }

            @Override
            public void onSuccess(@Nonnull Optional<MonitoringInfo> optInfo) {
                if (optInfo.isPresent()) {
                    final MonitoringInfo info = optInfo.get();
                    ListenableFuture<Optional<MonitorProfile>> readProfile = tx.read(LogicalDatastoreType.OPERATIONAL,
                            getMonitorProfileId(info.getProfileId()));
                    Futures.addCallback(readProfile, new FutureCallback<Optional<MonitorProfile>>() {

                        @Override
                        public void onFailure(Throwable error) {
                            String msg = String.format("Unable to read Monitoring profile associated with id %d",
                                    info.getProfileId());
                            LOG.warn("Monitor resume Failed. {}", msg, error);
                            tx.close();
                        }

                        @Override
                        public void onSuccess(@Nonnull Optional<MonitorProfile> optProfile) {
                            tx.close();
                            if (optProfile.isPresent()) {
                                updateMonitorStatusTo(monitorId, MonitorStatus.Started,
                                    currentStatus -> currentStatus != MonitorStatus.Started);
                                MonitorProfile profile = optProfile.get();
                                LOG.debug("Monitor Resume - Scheduling monitoring task for Id: {}", monitorId);
                                scheduleMonitoringTask(info, profile.getMonitorInterval());
                            } else {
                                String msg = String.format("Monitoring profile associated with id %d is not present",
                                        info.getProfileId());
                                LOG.warn("Monitor resume Failed. {}", msg);
                            }
                        }
                    }, MoreExecutors.directExecutor());
                } else {
                    tx.close();
                    String msg = String.format("Monitoring info associated with id %d is not present", monitorId);
                    LOG.warn("Monitor resume Failed. {}", msg);
                }
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    public void onInterfaceStateUp(String interfaceName) {
        List<Long> monitorIds = getMonitorIds(interfaceName);
        if (monitorIds.isEmpty()) {
            LOG.warn("Could not get monitorId for interface: {}", interfaceName);
            return;
        }
        for (Long monitorId : monitorIds) {
            LOG.debug("Resume monitoring on interface: {} with monitorId: {}", interfaceName, monitorId);
            resumeMonitoring(monitorId);
        }
    }

    @Override
    public void onInterfaceStateDown(String interfaceName) {
        List<Long> monitorIds = getMonitorIds(interfaceName);
        if (monitorIds.isEmpty()) {
            LOG.warn("Could not get monitorIds for interface: {}", interfaceName);
            return;
        }
        for (Long monitorId : monitorIds) {
            LOG.debug("Suspend monitoring on interface: {} with monitorId: {}", interfaceName, monitorId);
            stopMonitoring(monitorId);
        }
    }

    private List<Long> getMonitorIds(String interfaceName) {
        return SingleTransactionDataBroker.syncReadOptionalAndTreatReadFailedExceptionAsAbsentOptional(
                dataBroker, LogicalDatastoreType.OPERATIONAL, getInterfaceMonitorMapId(interfaceName))
                .toJavaUtil().map(InterfaceMonitorEntry::getMonitorIds).orElse(Collections.emptyList());
    }

    //handle monitor stop
    @Override
    public void remove(@Nonnull InstanceIdentifier<MonitoringState> instanceIdentifier,
                       @Nonnull MonitoringState removedDataObject) {
        final Long monitorId = removedDataObject.getMonitorId();
        LOG.debug("Monitor State remove listener invoked for monitor id: {}", monitorId);

        if (removedDataObject.getStatus() != MonitorStatus.Paused) {
            ScheduledFuture<?> scheduledFutureResult = monitoringTasks.get(monitorId);
            if (scheduledFutureResult != null) {
                LOG.debug("Stopping the task for Monitor id: {}", monitorId);
                stopMonitoringTask(monitorId);
            }
        }
    }

    //handle monitor pause
    @Override
    public void update(@Nonnull InstanceIdentifier<MonitoringState> instanceIdentifier,
                       @Nonnull MonitoringState originalDataObject,
                       @Nonnull MonitoringState updatedDataObject) {
        final Long monitorId = updatedDataObject.getMonitorId();
        LOG.debug("Monitor State update listener invoked for monitor id: {}", monitorId);

        if (updatedDataObject.getStatus() == MonitorStatus.Paused
                && originalDataObject.getStatus() != MonitorStatus.Paused) {
            ScheduledFuture<?> scheduledFutureResult = monitoringTasks.get(monitorId);
            if (scheduledFutureResult != null) {
                LOG.debug("Stopping the task for Monitor id: {}", monitorId);
                stopMonitoringTask(monitorId);
            }
        }
    }
}
