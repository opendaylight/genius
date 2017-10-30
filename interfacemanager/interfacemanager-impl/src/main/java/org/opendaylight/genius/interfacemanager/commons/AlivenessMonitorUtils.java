/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.commons;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.AlivenessMonitorService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.EtherTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorProfileCreateInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorProfileCreateInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorProfileCreateOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorProfileDeleteInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorProfileDeleteInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorProfileGetInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorProfileGetInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorProfileGetOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorStartInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorStartInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorStartOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorStopInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorStopInputBuilder;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class AlivenessMonitorUtils {

    private static final Logger LOG = LoggerFactory.getLogger(AlivenessMonitorUtils.class);
    private static final long FAILURE_THRESHOLD = 4;
    private static final long MONITORING_WINDOW = 4;

    private final AlivenessMonitorService alivenessMonitorService;
    private final DataBroker dataBroker;

    @Inject
    public AlivenessMonitorUtils(AlivenessMonitorService alivenessMonitor, DataBroker dataBroker) {
        this.alivenessMonitorService = alivenessMonitor;
        this.dataBroker = dataBroker;
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
                                    ifTunnel.getMonitorInterval(), MONITORING_WINDOW, EtherTypes.Lldp))
                            .build())
                    .build();
            try {
                Future<RpcResult<MonitorStartOutput>> result = alivenessMonitorService.monitorStart(lldpMonitorInput);
                RpcResult<MonitorStartOutput> rpcResult = result.get();
                long monitorId;
                if (rpcResult.isSuccessful()) {
                    monitorId = rpcResult.getResult().getMonitorId();
                    createOrUpdateInterfaceMonitorIdMap(trunkInterfaceName, monitorId);
                    createOrUpdateMonitorIdInterfaceMap(trunkInterfaceName, monitorId);
                    LOG.trace("Started LLDP monitoring with id {}", monitorId);
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
        List<Long> monitorIds = getMonitorIdForInterface(trunkInterface);
        if (monitorIds == null) {
            LOG.error("Monitor Id doesn't exist for Interface {}", trunkInterface);
            return;
        }
        for (Long monitorId : monitorIds) {
            String interfaceName = getInterfaceFromMonitorId(monitorId);
            if (interfaceName != null) {
                MonitorStopInput input = new MonitorStopInputBuilder().setMonitorId(monitorId).build();
                alivenessMonitorService.monitorStop(input);
                removeMonitorIdInterfaceMap(monitorId);
                removeMonitorIdFromInterfaceMonitorIdMap(interfaceName, monitorId);
                return;
            }
        }
    }

    public String getInterfaceFromMonitorId(Long monitorId) {
        InstanceIdentifier<MonitorIdInterface> id = InstanceIdentifier.builder(MonitorIdInterfaceMap.class)
                .child(MonitorIdInterface.class, new MonitorIdInterfaceKey(monitorId)).build();
        return IfmUtil.read(LogicalDatastoreType.OPERATIONAL, id, dataBroker)
                .toJavaUtil().map(MonitorIdInterface::getInterfaceName).orElse(null);
    }

    private void removeMonitorIdInterfaceMap(long monitorId) {
        InstanceIdentifier<MonitorIdInterface> id = InstanceIdentifier.builder(MonitorIdInterfaceMap.class)
                .child(MonitorIdInterface.class, new MonitorIdInterfaceKey(monitorId)).build();
        Optional<MonitorIdInterface> monitorIdInterfaceMap = IfmUtil.read(LogicalDatastoreType.OPERATIONAL,
                id, dataBroker);
        if (monitorIdInterfaceMap.isPresent()) {
            MDSALUtil.syncDelete(dataBroker, LogicalDatastoreType.OPERATIONAL, id);
        }
    }

    private void removeMonitorIdFromInterfaceMonitorIdMap(String infName, long monitorId) {
        InstanceIdentifier<InterfaceMonitorId> id = InstanceIdentifier.builder(InterfaceMonitorIdMap.class)
                .child(InterfaceMonitorId.class, new InterfaceMonitorIdKey(infName)).build();
        Optional<InterfaceMonitorId> interfaceMonitorIdMap = IfmUtil.read(LogicalDatastoreType.OPERATIONAL,
                id, dataBroker);
        if (interfaceMonitorIdMap.isPresent()) {
            InterfaceMonitorId interfaceMonitorIdInstance = interfaceMonitorIdMap.get();
            List<Long> existingMonitorIds = interfaceMonitorIdInstance.getMonitorId();
            if (existingMonitorIds != null && existingMonitorIds.contains(monitorId)) {
                existingMonitorIds.remove(monitorId);
                InterfaceMonitorIdBuilder interfaceMonitorIdBuilder = new InterfaceMonitorIdBuilder();
                interfaceMonitorIdInstance = interfaceMonitorIdBuilder.setKey(new InterfaceMonitorIdKey(infName))
                        .setMonitorId(existingMonitorIds).build();
                MDSALUtil.syncWrite(dataBroker, LogicalDatastoreType.OPERATIONAL, id, interfaceMonitorIdInstance);
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
        IfTunnel ifTunnelNew = interfaceNew.getAugmentation(IfTunnel.class);
        if (!lldpMonitoringEnabled(ifTunnelNew)) {
            return;
        }
        LOG.debug("handling tunnel monitoring updates for interface {}", interfaceName);

        stopLLDPMonitoring(ifTunnelNew, interfaceOld.getName());
        if (ifTunnelNew.isMonitorEnabled()) {
            startLLDPMonitoring(ifTunnelNew, interfaceName);

            // Delete old profile from Aliveness Manager
            IfTunnel ifTunnelOld = interfaceOld.getAugmentation(IfTunnel.class);
            if (!ifTunnelNew.getMonitorInterval().equals(ifTunnelOld.getMonitorInterval())) {
                LOG.debug("deleting older monitor profile for interface {}", interfaceName);
                long profileId = allocateProfile(FAILURE_THRESHOLD, ifTunnelOld.getMonitorInterval(), MONITORING_WINDOW,
                        EtherTypes.Lldp);
                MonitorProfileDeleteInput profileDeleteInput = new MonitorProfileDeleteInputBuilder()
                        .setProfileId(profileId).build();
                alivenessMonitorService.monitorProfileDelete(profileDeleteInput);
            }
        }
    }

    public void createOrUpdateInterfaceMonitorIdMap(String infName, long monitorId) {
        InterfaceMonitorId interfaceMonitorIdInstance;
        List<Long> existingMonitorIds;
        InterfaceMonitorIdBuilder interfaceMonitorIdBuilder = new InterfaceMonitorIdBuilder();
        InstanceIdentifier<InterfaceMonitorId> id = InstanceIdentifier.builder(InterfaceMonitorIdMap.class)
                .child(InterfaceMonitorId.class, new InterfaceMonitorIdKey(infName)).build();
        Optional<InterfaceMonitorId> interfaceMonitorIdMap = IfmUtil.read(LogicalDatastoreType.OPERATIONAL,
                id, dataBroker);
        if (interfaceMonitorIdMap.isPresent()) {
            interfaceMonitorIdInstance = interfaceMonitorIdMap.get();
            existingMonitorIds = interfaceMonitorIdInstance.getMonitorId();
            if (existingMonitorIds == null) {
                existingMonitorIds = new ArrayList<>();
            }
            if (!existingMonitorIds.contains(monitorId)) {
                existingMonitorIds.add(monitorId);
                interfaceMonitorIdInstance = interfaceMonitorIdBuilder.setKey(new InterfaceMonitorIdKey(infName))
                        .setMonitorId(existingMonitorIds).build();
                MDSALUtil.syncUpdate(dataBroker, LogicalDatastoreType.OPERATIONAL, id, interfaceMonitorIdInstance);
            }
        } else {
            existingMonitorIds = new ArrayList<>();
            existingMonitorIds.add(monitorId);
            interfaceMonitorIdInstance = interfaceMonitorIdBuilder.setMonitorId(existingMonitorIds)
                    .setKey(new InterfaceMonitorIdKey(infName)).setInterfaceName(infName).build();
            MDSALUtil.syncWrite(dataBroker, LogicalDatastoreType.OPERATIONAL, id, interfaceMonitorIdInstance);
        }
    }

    public void createOrUpdateMonitorIdInterfaceMap(String infName, long monitorId) {
        MonitorIdInterface monitorIdInterfaceInstance;
        String existinginterfaceName;
        MonitorIdInterfaceBuilder monitorIdInterfaceBuilder = new MonitorIdInterfaceBuilder();
        InstanceIdentifier<MonitorIdInterface> id = InstanceIdentifier.builder(MonitorIdInterfaceMap.class)
                .child(MonitorIdInterface.class, new MonitorIdInterfaceKey(monitorId)).build();
        Optional<MonitorIdInterface> monitorIdInterfaceMap = IfmUtil.read(LogicalDatastoreType.OPERATIONAL,
                id, dataBroker);
        if (monitorIdInterfaceMap.isPresent()) {
            monitorIdInterfaceInstance = monitorIdInterfaceMap.get();
            existinginterfaceName = monitorIdInterfaceInstance.getInterfaceName();
            if (!existinginterfaceName.equals(infName)) {
                monitorIdInterfaceInstance = monitorIdInterfaceBuilder.setKey(new MonitorIdInterfaceKey(monitorId))
                        .setInterfaceName(infName).build();
                MDSALUtil.syncUpdate(dataBroker, LogicalDatastoreType.OPERATIONAL, id, monitorIdInterfaceInstance);
            }
        } else {
            monitorIdInterfaceInstance = monitorIdInterfaceBuilder.setMonitorId(monitorId)
                    .setKey(new MonitorIdInterfaceKey(monitorId)).setInterfaceName(infName).build();
            MDSALUtil.syncWrite(dataBroker, LogicalDatastoreType.OPERATIONAL, id, monitorIdInterfaceInstance);
        }
    }

    public List<Long> getMonitorIdForInterface(String infName) {
        InstanceIdentifier<InterfaceMonitorId> id = InstanceIdentifier.builder(InterfaceMonitorIdMap.class)
                .child(InterfaceMonitorId.class, new InterfaceMonitorIdKey(infName)).build();
        return IfmUtil.read(LogicalDatastoreType.OPERATIONAL, id, dataBroker).toJavaUtil().map(
                InterfaceMonitorId::getMonitorId).orElse(null);
    }

    public long createMonitorProfile(MonitorProfileCreateInput monitorProfileCreateInput) {
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
                        buildMonitorGetProfile(createProfile.getMonitorInterval(), createProfile.getMonitorWindow(),
                                createProfile.getFailureThreshold(), createProfile.getProtocolType()));
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
        return 0;
    }

    private static MonitorProfileGetInput buildMonitorGetProfile(long monitorInterval, long monitorWindow,
            long failureThreshold, EtherTypes protocolType) {
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

    public long allocateProfile(long failureThreshold, long monitoringInterval, long monitoringWindow,
            EtherTypes etherTypes) {
        MonitorProfileCreateInput input = new MonitorProfileCreateInputBuilder().setProfile(
                new ProfileBuilder().setFailureThreshold(failureThreshold).setMonitorInterval(monitoringInterval)
                        .setMonitorWindow(monitoringWindow).setProtocolType(etherTypes).build())
                .build();
        return createMonitorProfile(input);
    }

    public static boolean lldpMonitoringEnabled(IfTunnel ifTunnel) {
        return ifTunnel.isInternal() && ifTunnel.isMonitorEnabled()
                && TunnelMonitoringTypeLldp.class.isAssignableFrom(ifTunnel.getMonitorProtocol());
    }
}
