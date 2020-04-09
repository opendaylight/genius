/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.commons;

import static org.opendaylight.genius.infra.Datastore.OPERATIONAL;
import static org.opendaylight.mdsal.binding.api.WriteTransaction.CREATE_MISSING_PARENTS;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.aries.blueprint.annotation.service.Reference;
import org.opendaylight.genius.infra.Datastore.Operational;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.infra.TypedReadTransaction;
import org.opendaylight.genius.infra.TypedReadWriteTransaction;
import org.opendaylight.infrautils.utils.concurrent.ListenableFutures;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.AlivenessMonitorService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorProfileCreateInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorProfileCreateInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorProfileCreateOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorProfileDeleteInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorProfileDeleteInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorProfileDeleteOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorProfileGetInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorProfileGetInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorProfileGetOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorProtocolType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorStartInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorStartInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorStartOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorStopInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorStopInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorStopOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitoringMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitor.params.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitor.profile.create.input.Profile;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitor.profile.create.input.ProfileBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitor.start.input.ConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.InterfaceMonitorIdMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.MonitorIdInterfaceMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.monitor.id.map.InterfaceMonitorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.monitor.id.map.InterfaceMonitorIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.monitor.id.map.InterfaceMonitorIdKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.monitor.id._interface.map.MonitorIdInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.monitor.id._interface.map.MonitorIdInterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.monitor.id._interface.map.MonitorIdInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeLldp;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.Uint32;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class AlivenessMonitorUtils {

    private static final Logger LOG = LoggerFactory.getLogger(AlivenessMonitorUtils.class);
    private static final long FAILURE_THRESHOLD = 4;
    private static final long MONITORING_WINDOW = 4;

    private final AlivenessMonitorService alivenessMonitorService;
    private final ManagedNewTransactionRunner txRunner;

    @Inject
    public AlivenessMonitorUtils(AlivenessMonitorService alivenessMonitor, @Reference DataBroker dataBroker) {
        this.alivenessMonitorService = alivenessMonitor;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
    }

    public void startLLDPMonitoring(IfTunnel ifTunnel, String trunkInterfaceName) {
        // LLDP monitoring for the tunnel interface
        if (lldpMonitoringEnabled(ifTunnel)) {
            MonitorStartInput lldpMonitorInput = new MonitorStartInputBuilder()
                    .setConfig(new ConfigBuilder()
                            .setSource(new SourceBuilder()
                                    .setEndpointType(
                                            getInterfaceForMonitoring(trunkInterfaceName, ifTunnel.getTunnelSource()))
                                    .build())
                            .setMode(MonitoringMode.OneOne)
                            .setProfileId(allocateProfile(FAILURE_THRESHOLD,
                                    ifTunnel.getMonitorInterval().toJava(), MONITORING_WINDOW,
                                    MonitorProtocolType.Lldp))
                            .build())
                    .build();
            try {
                Future<RpcResult<MonitorStartOutput>> result = alivenessMonitorService.monitorStart(lldpMonitorInput);
                RpcResult<MonitorStartOutput> rpcResult = result.get();
                if (rpcResult.isSuccessful()) {
                    long monitorId = rpcResult.getResult().getMonitorId().toJava();
                    ListenableFutures.addErrorLogging(
                        txRunner.callWithNewReadWriteTransactionAndSubmit(OPERATIONAL, tx -> {
                            createOrUpdateInterfaceMonitorIdMap(tx, trunkInterfaceName, monitorId);
                            createOrUpdateMonitorIdInterfaceMap(tx, trunkInterfaceName, monitorId);
                            LOG.trace("Started LLDP monitoring with id {}", monitorId);
                        }), LOG, "Error starting monitoring");
                } else {
                    LOG.warn("RPC Call to start monitoring returned with Errors {}", rpcResult.getErrors());
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.warn("Exception when starting monitoring", e);
            }
        }
    }

    public void stopLLDPMonitoring(IfTunnel ifTunnel, String trunkInterface) {
        if (!lldpMonitoringEnabled(ifTunnel)) {
            return;
        }
        LOG.debug("stop LLDP monitoring for {}", trunkInterface);
        ListenableFutures.addErrorLogging(txRunner.callWithNewReadWriteTransactionAndSubmit(OPERATIONAL, tx -> {
            List<Uint32> monitorIds = getMonitorIdForInterface(tx, trunkInterface);
            if (monitorIds == null) {
                LOG.error("Monitor Id doesn't exist for Interface {}", trunkInterface);
                return;
            }
            for (Uint32 monitorId : monitorIds) {
                String interfaceName = getInterfaceFromMonitorId(tx, monitorId);
                if (interfaceName != null) {
                    MonitorStopInput input = new MonitorStopInputBuilder().setMonitorId(monitorId).build();

                    ListenableFuture<RpcResult<MonitorStopOutput>> future = alivenessMonitorService.monitorStop(input);
                    ListenableFutures.addErrorLogging(future, LOG, "Stop LLDP monitoring for {}", trunkInterface);

                    removeMonitorIdInterfaceMap(tx, monitorId);
                    removeMonitorIdFromInterfaceMonitorIdMap(tx, interfaceName, monitorId);
                    return;
                }
            }
        }), LOG, "Error stopping LLDP monitoring for {}", trunkInterface);
    }

    public static String getInterfaceFromMonitorId(TypedReadTransaction<Operational> tx, Uint32 monitorId)
        throws ExecutionException, InterruptedException {
        InstanceIdentifier<MonitorIdInterface> id = InstanceIdentifier.builder(MonitorIdInterfaceMap.class)
                .child(MonitorIdInterface.class, new MonitorIdInterfaceKey(monitorId)).build();
        return tx.read(id).get().map(MonitorIdInterface::getInterfaceName).orElse(null);
    }

    private void removeMonitorIdInterfaceMap(TypedReadWriteTransaction<Operational> tx, Uint32 monitorId)
        throws ExecutionException, InterruptedException {
        InstanceIdentifier<MonitorIdInterface> id = InstanceIdentifier.builder(MonitorIdInterfaceMap.class)
                .child(MonitorIdInterface.class, new MonitorIdInterfaceKey(monitorId)).build();
        if (tx.read(id).get().isPresent()) {
            tx.delete(id);
        }
    }

    private void removeMonitorIdFromInterfaceMonitorIdMap(TypedReadWriteTransaction<Operational> tx, String infName,
        Uint32 monitorId) throws ExecutionException, InterruptedException {
        InstanceIdentifier<InterfaceMonitorId> id = InstanceIdentifier.builder(InterfaceMonitorIdMap.class)
                .child(InterfaceMonitorId.class, new InterfaceMonitorIdKey(infName)).build();
        Optional<InterfaceMonitorId> interfaceMonitorIdMap = tx.read(id).get();
        if (interfaceMonitorIdMap.isPresent()) {
            InterfaceMonitorId interfaceMonitorIdInstance = interfaceMonitorIdMap.get();
            List<Uint32> existingMonitorIds = interfaceMonitorIdInstance.getMonitorId();
            if (existingMonitorIds != null && existingMonitorIds.contains(monitorId)) {
                existingMonitorIds.remove(monitorId);
                InterfaceMonitorIdBuilder interfaceMonitorIdBuilder = new InterfaceMonitorIdBuilder();
                interfaceMonitorIdInstance = interfaceMonitorIdBuilder.withKey(new InterfaceMonitorIdKey(infName))
                        .setMonitorId(existingMonitorIds).build();
                tx.merge(id, interfaceMonitorIdInstance, CREATE_MISSING_PARENTS);
            }
        }
    }

    private static org.opendaylight.yang.gen.v1
        .urn.opendaylight.genius.alivenessmonitor
        .rev160411.endpoint.endpoint.type.Interface getInterfaceForMonitoring(
            String interfaceName,
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress ipAddress) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight
                .genius.alivenessmonitor.rev160411.endpoint.endpoint.type.InterfaceBuilder()
                .setInterfaceIp(ipAddress).setInterfaceName(interfaceName).build();
    }

    public void handleTunnelMonitorUpdates(Interface interfaceOld, Interface interfaceNew) {
        String interfaceName = interfaceNew.getName();
        IfTunnel ifTunnelNew = interfaceNew.augmentation(IfTunnel.class);
        if (!lldpMonitoringEnabled(ifTunnelNew)) {
            return;
        }
        LOG.debug("handling tunnel monitoring updates for interface {}", interfaceName);

        stopLLDPMonitoring(ifTunnelNew, interfaceOld.getName());
        if (ifTunnelNew.isMonitorEnabled()) {
            startLLDPMonitoring(ifTunnelNew, interfaceName);

            // Delete old profile from Aliveness Manager
            IfTunnel ifTunnelOld = interfaceOld.augmentation(IfTunnel.class);
            if (!Objects.equals(ifTunnelNew.getMonitorInterval(), ifTunnelOld.getMonitorInterval())) {
                LOG.debug("deleting older monitor profile for interface {}", interfaceName);
                Uint32 profileId = allocateProfile(FAILURE_THRESHOLD, ifTunnelOld.getMonitorInterval().toJava(),
                        MONITORING_WINDOW, MonitorProtocolType.Lldp);
                MonitorProfileDeleteInput profileDeleteInput = new MonitorProfileDeleteInputBuilder()
                        .setProfileId(profileId).build();

                ListenableFuture<RpcResult<MonitorProfileDeleteOutput>> future =
                        alivenessMonitorService.monitorProfileDelete(profileDeleteInput);
                ListenableFutures.addErrorLogging(future, LOG, "Delete monitor profile {}", interfaceName);
            }
        }
    }

    private static void createOrUpdateInterfaceMonitorIdMap(TypedReadWriteTransaction<Operational> tx, String infName,
        long monitorId) throws ExecutionException, InterruptedException {
        InterfaceMonitorId interfaceMonitorIdInstance;
        List<Uint32> existingMonitorIds;
        InterfaceMonitorIdBuilder interfaceMonitorIdBuilder = new InterfaceMonitorIdBuilder();
        InstanceIdentifier<InterfaceMonitorId> id = InstanceIdentifier.builder(InterfaceMonitorIdMap.class)
                .child(InterfaceMonitorId.class, new InterfaceMonitorIdKey(infName)).build();
        Optional<InterfaceMonitorId> interfaceMonitorIdMap = tx.read(id).get();
        if (interfaceMonitorIdMap.isPresent()) {
            interfaceMonitorIdInstance = interfaceMonitorIdMap.get();
            existingMonitorIds = interfaceMonitorIdInstance.getMonitorId();
            if (existingMonitorIds == null) {
                existingMonitorIds = new ArrayList<>();
            }
            if (!existingMonitorIds.contains(Uint32.valueOf(monitorId))) {
                existingMonitorIds.add(Uint32.valueOf(monitorId));
                interfaceMonitorIdInstance = interfaceMonitorIdBuilder.withKey(new InterfaceMonitorIdKey(infName))
                        .setMonitorId(existingMonitorIds).build();
                tx.merge(id, interfaceMonitorIdInstance, CREATE_MISSING_PARENTS);
            }
        } else {
            existingMonitorIds = new ArrayList<>();
            existingMonitorIds.add(Uint32.valueOf(monitorId));
            interfaceMonitorIdInstance = interfaceMonitorIdBuilder.setMonitorId(existingMonitorIds)
                    .withKey(new InterfaceMonitorIdKey(infName)).setInterfaceName(infName).build();
            tx.merge(id, interfaceMonitorIdInstance, CREATE_MISSING_PARENTS);
        }
    }

    private static void createOrUpdateMonitorIdInterfaceMap(TypedReadWriteTransaction<Operational> tx, String infName,
        long monitorId) throws ExecutionException, InterruptedException {
        MonitorIdInterface monitorIdInterfaceInstance;
        String existinginterfaceName;
        MonitorIdInterfaceBuilder monitorIdInterfaceBuilder = new MonitorIdInterfaceBuilder();
        InstanceIdentifier<MonitorIdInterface> id = InstanceIdentifier.builder(MonitorIdInterfaceMap.class)
                .child(MonitorIdInterface.class, new MonitorIdInterfaceKey(monitorId)).build();
        Optional<MonitorIdInterface> monitorIdInterfaceMap = tx.read(id).get();
        if (monitorIdInterfaceMap.isPresent()) {
            monitorIdInterfaceInstance = monitorIdInterfaceMap.get();
            existinginterfaceName = monitorIdInterfaceInstance.getInterfaceName();
            if (!Objects.equals(existinginterfaceName, infName)) {
                monitorIdInterfaceInstance = monitorIdInterfaceBuilder.withKey(new MonitorIdInterfaceKey(monitorId))
                        .setInterfaceName(infName).build();
                tx.merge(id, monitorIdInterfaceInstance, CREATE_MISSING_PARENTS);
            }
        } else {
            monitorIdInterfaceInstance = monitorIdInterfaceBuilder.setMonitorId(monitorId)
                    .withKey(new MonitorIdInterfaceKey(monitorId)).setInterfaceName(infName).build();
            tx.merge(id, monitorIdInterfaceInstance, CREATE_MISSING_PARENTS);
        }
    }

    private static List<Uint32> getMonitorIdForInterface(TypedReadTransaction<Operational> tx, String infName)
        throws ExecutionException, InterruptedException {
        InstanceIdentifier<InterfaceMonitorId> id = InstanceIdentifier.builder(InterfaceMonitorIdMap.class)
                .child(InterfaceMonitorId.class, new InterfaceMonitorIdKey(infName)).build();
        return tx.read(id).get().map(InterfaceMonitorId::getMonitorId).orElse(null);
    }

    public Uint32 createMonitorProfile(MonitorProfileCreateInput monitorProfileCreateInput) {
        try {
            Future<RpcResult<MonitorProfileCreateOutput>> result = alivenessMonitorService
                    .monitorProfileCreate(monitorProfileCreateInput);
            RpcResult<MonitorProfileCreateOutput> rpcResult = result.get();
            if (rpcResult.isSuccessful()) {
                return rpcResult.getResult().getProfileId();
            } else {
                LOG.warn("RPC Call to Get Profile Id Id returned with Errors {}.. Trying to fetch existing profile ID",
                        rpcResult.getErrors());
                Profile createProfile = monitorProfileCreateInput.getProfile();
                Future<RpcResult<MonitorProfileGetOutput>> existingProfile = alivenessMonitorService.monitorProfileGet(
                        buildMonitorGetProfile(createProfile.getMonitorInterval().toJava(),
                                createProfile.getMonitorWindow().toJava(),
                                createProfile.getFailureThreshold().toJava(), createProfile.getProtocolType()));
                RpcResult<MonitorProfileGetOutput> rpcGetResult = existingProfile.get();
                if (rpcGetResult.isSuccessful()) {
                    return rpcGetResult.getResult().getProfileId();
                } else {
                    LOG.warn("RPC Call to Get Existing Profile Id returned with Errors {}", rpcGetResult.getErrors());
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Exception when allocating profile Id", e);
        }
        return Uint32.valueOf(0);
    }

    private static MonitorProfileGetInput buildMonitorGetProfile(long monitorInterval, long monitorWindow,
            long failureThreshold, MonitorProtocolType protocolType) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor
            .rev160411.monitor.profile.get.input.ProfileBuilder profileBuilder =
            new org.opendaylight.yang.gen.v1.urn.opendaylight
            .genius.alivenessmonitor.rev160411.monitor.profile.get.input.ProfileBuilder();

        profileBuilder.setFailureThreshold(failureThreshold);
        profileBuilder.setMonitorInterval(monitorInterval);
        profileBuilder.setMonitorWindow(monitorWindow);
        profileBuilder.setProtocolType(protocolType);
        MonitorProfileGetInputBuilder buildGetProfile = new MonitorProfileGetInputBuilder();
        buildGetProfile.setProfile(profileBuilder.build());
        return buildGetProfile.build();
    }

    public Uint32 allocateProfile(long failureThreshold, long monitoringInterval, long monitoringWindow,
            MonitorProtocolType protoType) {
        MonitorProfileCreateInput input = new MonitorProfileCreateInputBuilder().setProfile(
                new ProfileBuilder().setFailureThreshold(failureThreshold).setMonitorInterval(monitoringInterval)
                        .setMonitorWindow(monitoringWindow).setProtocolType(protoType).build())
                .build();
        return createMonitorProfile(input);
    }

    public static boolean lldpMonitoringEnabled(IfTunnel ifTunnel) {
        return ifTunnel.isInternal() && ifTunnel.isMonitorEnabled()
                && TunnelMonitoringTypeLldp.class.isAssignableFrom(ifTunnel.getMonitorProtocol());
    }
}
