/*
 * Copyright (c) 2016, 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.alivenessmonitor.internal;

import static org.opendaylight.genius.alivenessmonitor.utils.AlivenessMonitorUtil.getMonitorStateId;
import static org.opendaylight.mdsal.binding.util.Datastore.CONFIGURATION;
import static org.opendaylight.mdsal.binding.util.Datastore.OPERATIONAL;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.aries.blueprint.annotation.service.Reference;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.genius.alivenessmonitor.protocols.AlivenessProtocolHandler;
import org.opendaylight.genius.alivenessmonitor.protocols.AlivenessProtocolHandlerRegistry;
import org.opendaylight.infrautils.utils.concurrent.LoggingFutures;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.util.Datastore;
import org.opendaylight.mdsal.binding.util.InterruptibleCheckedConsumer;
import org.opendaylight.mdsal.binding.util.ManagedNewTransactionRunner;
import org.opendaylight.mdsal.binding.util.RetryingManagedNewTransactionRunner;
import org.opendaylight.mdsal.binding.util.TypedReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.openflowplugin.libraries.liblldp.Packet;
import org.opendaylight.serviceutils.tools.listener.AbstractSyncDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.LivenessState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorProtocolType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.endpoint.EndpointType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.endpoint.endpoint.type.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitor.configs.MonitoringInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitor.profiles.MonitorProfile;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitoring.states.MonitoringState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitoring.states.MonitoringStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.Tunnels;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.TunnelsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.TunnelsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.tunnel.attributes.BfdLocalConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.tunnel.attributes.BfdLocalConfigsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.tunnel.attributes.BfdLocalConfigsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.tunnel.attributes.BfdParams;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.tunnel.attributes.BfdParamsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.tunnel.attributes.BfdParamsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.tunnel.attributes.BfdRemoteConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.tunnel.attributes.BfdRemoteConfigsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.tunnel.attributes.BfdRemoteConfigsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.tunnel.attributes.BfdStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class HwVtepTunnelsStateHandler extends AbstractSyncDataTreeChangeListener<Tunnels>
        implements AlivenessProtocolHandler<Packet> {

    private static final Logger LOG = LoggerFactory.getLogger(HwVtepTunnelsStateHandler.class);

    private final ManagedNewTransactionRunner txRunner;
    private final AlivenessMonitor alivenessMonitor;

    @Inject
    public HwVtepTunnelsStateHandler(@Reference final DataBroker dataBroker,
                                     final AlivenessMonitor alivenessMonitor,
                                     final AlivenessProtocolHandlerRegistry alivenessProtocolHandlerRegistry) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION,
              InstanceIdentifier.create(NetworkTopology.class).child(Topology.class).child(Node.class)
                      .augmentation(PhysicalSwitchAugmentation.class).child(Tunnels.class));
        this.txRunner = new RetryingManagedNewTransactionRunner(dataBroker);
        this.alivenessMonitor = alivenessMonitor;
        alivenessProtocolHandlerRegistry.register(MonitorProtocolType.Bfd, this);
    }

    @Override
    public void remove(@NonNull InstanceIdentifier<Tunnels> instanceIdentifier, @NonNull Tunnels tunnelInfo) {
        // Do nothing
    }

    @Override
    public void update(@NonNull InstanceIdentifier<Tunnels> instanceIdentifier, @NonNull Tunnels oldTunnelInfo,
                       @NonNull Tunnels updatedTunnelInfo) {
        List<BfdStatus> oldBfdStatus = oldTunnelInfo.getBfdStatus();
        List<BfdStatus> newBfdStatus = updatedTunnelInfo.getBfdStatus();
        LivenessState oldTunnelOpState = getTunnelOpState(oldBfdStatus);
        final LivenessState newTunnelOpState = getTunnelOpState(newBfdStatus);
        if (oldTunnelOpState == newTunnelOpState) {
            LOG.debug("Tunnel state of old tunnel {} and update tunnel {} are same", oldTunnelInfo, updatedTunnelInfo);
            return;
        }
        updatedTunnelInfo.getTunnelUuid();
        String interfaceName = "<TODO>";
        // TODO: find out the corresponding interface using tunnelIdentifier or
        // any attributes of tunnelInfo object
        final String monitorKey = getBfdMonitorKey(interfaceName);
        LOG.debug("Processing monitorKey: {} for received Tunnels update DCN", monitorKey);

        final Semaphore lock = alivenessMonitor.getLock(monitorKey);
        LOG.debug("Acquiring lock for monitor key : {} to process monitor DCN", monitorKey);
        alivenessMonitor.acquireLock(lock);

        AtomicBoolean stateChanged = new AtomicBoolean();
        AtomicReference<MonitoringState> currentState = new AtomicReference<>();
        txRunner.callWithNewReadWriteTransactionAndSubmit(OPERATIONAL, tx -> {
            Optional<MonitoringState> optState = tx.read(getMonitorStateId(monitorKey)).get();
            if (optState.isPresent()) {
                currentState.set(optState.get());
                if (currentState.get().getState() == newTunnelOpState) {
                    return;
                }
                stateChanged.set(true);
                final MonitoringState state = new MonitoringStateBuilder().setMonitorKey(monitorKey)
                    .setState(newTunnelOpState).build();
                tx.merge(getMonitorStateId(monitorKey), state);
            } else {
                LOG.warn("Monitoring State not available for key: {} to process the Packet received", monitorKey);
            }
        }).addCallback(new FutureCallback<Object>() {
            @Override
            public void onSuccess(@Nullable Object result) {
                alivenessMonitor.releaseLock(lock);
                if (stateChanged.get()) {
                    // send notifications
                    LOG.info("Sending notification for monitor Id : {} with Current State: {}",
                        currentState.get().getMonitorId(), newTunnelOpState);
                    alivenessMonitor.publishNotification(currentState.get().getMonitorId(), newTunnelOpState);
                } else {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Successful in writing monitoring state {} to ODS", newTunnelOpState);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Throwable error) {
                alivenessMonitor.releaseLock(lock);
                LOG.warn("Error in writing monitoring state for {} to Datastore", monitorKey, error);
            }
        }, MoreExecutors.directExecutor());
    }

    private LivenessState getTunnelOpState(List<BfdStatus> tunnelBfdStatus) {
        LivenessState livenessState = LivenessState.Unknown;
        if (tunnelBfdStatus == null || tunnelBfdStatus.isEmpty()) {
            return livenessState;
        }
        for (BfdStatus bfdState : tunnelBfdStatus) {
            if (AlivenessMonitorConstants.BFD_OP_STATE.equalsIgnoreCase(bfdState.getBfdStatusKey())) {
                String bfdOpState = bfdState.getBfdStatusValue();
                if (AlivenessMonitorConstants.BFD_STATE_UP.equalsIgnoreCase(bfdOpState)) {
                    livenessState = LivenessState.Up;
                } else {
                    livenessState = LivenessState.Down;
                }
                break;
            }
        }
        return livenessState;
    }

    @Override
    public void add(@NonNull InstanceIdentifier<Tunnels> instanceIdentifier, @NonNull Tunnels tunnelInfo) {
        // TODO: need to add the code to enable BFD if tunnels are created
        // dynamically by TOR switch
    }

    @Override
    public Class<Packet> getPacketClass() {
        return Packet.class;
    }

    @Override
    @SuppressFBWarnings("NP_NONNULL_RETURN_VIOLATION")
    public String handlePacketIn(Packet protocolPacket, PacketReceived packetReceived) {
        return null;
    }

    void resetMonitoringTask(boolean isEnable) {
        // TODO: get the corresponding hwvtep tunnel from the sourceInterface
        // once InterfaceMgr implements renderer for HWVTEP VXLAN tunnels

        LoggingFutures.addErrorLogging(txRunner.callWithNewReadWriteTransactionAndSubmit(CONFIGURATION,
            new InterruptibleCheckedConsumer<TypedReadWriteTransaction<Datastore.Configuration>, ExecutionException>() {
                @Override
                // tunnelKey, nodeId, topologyId are initialized to null and immediately passed to
                // getTunnelIdentifier which FindBugs as a "Load of known null value" violation. Not sure sure what
                // the intent...
                @SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE")
                public void accept(TypedReadWriteTransaction<Datastore.Configuration> tx)
                    throws ExecutionException, InterruptedException {
                    TunnelsKey tunnelKey = null;
                    String nodeId = null;
                    String topologyId = null;
                    Optional<Tunnels> tunnelsOptional =
                        tx.read(getTunnelIdentifier(topologyId, nodeId, tunnelKey)).get();
                    if (!tunnelsOptional.isPresent()) {
                        LOG.warn("Tunnel {} is not present on the Node {}. So not disabling the BFD monitoring",
                            tunnelKey,
                            nodeId);
                        return;
                    }
                    Tunnels tunnel = tunnelsOptional.get();
                    List<BfdParams> tunnelBfdParams = tunnel.getBfdParams();
                    if (tunnelBfdParams == null || tunnelBfdParams.isEmpty()) {
                        LOG.debug("there is no bfd params available for the tunnel {}", tunnel);
                        return;
                    }

                    Iterator<BfdParams> tunnelBfdParamsIterator = tunnelBfdParams.iterator();
                    while (tunnelBfdParamsIterator.hasNext()) {
                        BfdParams bfdParam = tunnelBfdParamsIterator.next();
                        if (AlivenessMonitorConstants.BFD_PARAM_ENABLE.equals(bfdParam.getBfdParamKey())) {
                            tunnelBfdParamsIterator.remove();
                            break;
                        }
                    }
                    HwVtepTunnelsStateHandler.this.setBfdParamForEnable(tunnelBfdParams, isEnable);
                    Tunnels tunnelWithBfdReset =
                        new TunnelsBuilder().withKey(tunnelKey).setBfdParams(tunnelBfdParams).build();
                    tx.mergeParentStructureMerge(
                        getTunnelIdentifier(topologyId, nodeId, tunnelKey), tunnelWithBfdReset);
                }
            }), LOG, "Error resetting monitoring task");
    }

    @Override
    public void startMonitoringTask(MonitoringInfo monitorInfo) {
        EndpointType source = monitorInfo.getSource().getEndpointType();
        if (source instanceof Interface) {
            Interface intf = (Interface) source;
            intf.getInterfaceName();
        } else {
            LOG.warn("Invalid source endpoint. Could not retrieve source interface to configure BFD");
            return;
        }
        MonitorProfile profile;
        long profileId = monitorInfo.getProfileId();
        java.util.Optional<MonitorProfile> optProfile = alivenessMonitor.getMonitorProfile(profileId);
        if (optProfile.isPresent()) {
            profile = optProfile.get();
        } else {
            LOG.warn("No monitor profile associated with id {}. " + "Could not send Monitor packet for monitor-id {}",
                    profileId, monitorInfo);
            return;
        }
        // TODO: get the corresponding hwvtep tunnel from the sourceInterface
        // once InterfaceMgr
        // Implements renderer for hwvtep VXLAN tunnels
        String tunnelLocalMacAddress = "<TODO>";
        String tunnelLocalIpAddress = "<TODO>";
        String tunnelRemoteMacAddress = "<TODO>";
        List<BfdParams> bfdParams = new ArrayList<>();
        fillBfdParams(bfdParams, profile);
        List<BfdLocalConfigs> bfdLocalConfigs = new ArrayList<>();
        fillBfdLocalConfigs(bfdLocalConfigs, tunnelLocalMacAddress, tunnelLocalIpAddress);
        List<BfdRemoteConfigs> bfdRemoteConfigs = new ArrayList<>();
        fillBfdRemoteConfigs(bfdRemoteConfigs, tunnelRemoteMacAddress);
        // tunnelKey is initialized to null and passed to withKey which FindBugs flags as a
        // "Load of known null value" violation. Not sure sure what the intent is...
        //TunnelsKey tunnelKey = null;
        Tunnels tunnelWithBfd = new TunnelsBuilder().withKey(/*tunnelKey*/ null).setBfdParams(bfdParams)
                .setBfdLocalConfigs(bfdLocalConfigs).setBfdRemoteConfigs(bfdRemoteConfigs).build();
        // TODO: get the following parameters from the interface and use it to
        // update hwvtep datastore
        // and not sure sure tunnels are creating immediately once interface mgr
        // writes termination point
        // into hwvtep datastore. if tunnels are not created during that time,
        // then start monitoring has to
        // be done as part of tunnel add DCN handling.
        String topologyId = "";
        String nodeId = "";
        LoggingFutures.addErrorLogging(
            txRunner.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION, tx -> tx.mergeParentStructureMerge(
                getTunnelIdentifier(topologyId, nodeId, new TunnelsKey(/*localRef*/ null, /*remoteRef*/ null)),
                tunnelWithBfd)), LOG, "Error starting a monitoring task");
    }

    private void fillBfdRemoteConfigs(List<BfdRemoteConfigs> bfdRemoteConfigs, String tunnelRemoteMacAddress) {
        bfdRemoteConfigs
                .add(getBfdRemoteConfig(AlivenessMonitorConstants.BFD_CONFIG_BFD_DST_MAC, tunnelRemoteMacAddress));
    }

    private BfdRemoteConfigs getBfdRemoteConfig(String key, String value) {
        return new BfdRemoteConfigsBuilder().setBfdRemoteConfigKey(key).setBfdRemoteConfigValue(value)
                .withKey(new BfdRemoteConfigsKey(key)).build();
    }

    private void fillBfdLocalConfigs(List<BfdLocalConfigs> bfdLocalConfigs, String tunnelLocalMacAddress,
            String tunnelLocalIpAddress) {
        bfdLocalConfigs.add(getBfdLocalConfig(AlivenessMonitorConstants.BFD_CONFIG_BFD_DST_MAC, tunnelLocalMacAddress));
        bfdLocalConfigs.add(getBfdLocalConfig(AlivenessMonitorConstants.BFD_CONFIG_BFD_DST_IP, tunnelLocalIpAddress));
    }

    private BfdLocalConfigs getBfdLocalConfig(String key, String value) {
        return new BfdLocalConfigsBuilder().setBfdLocalConfigKey(key).withKey(new BfdLocalConfigsKey(key))
                .setBfdLocalConfigValue(value).build();
    }

    private void fillBfdParams(List<BfdParams> bfdParams, MonitorProfile profile) {
        setBfdParamForEnable(bfdParams, true);
        bfdParams.add(getBfdParams(AlivenessMonitorConstants.BFD_PARAM_MIN_RX, Long.toString(profile.getMinRx())));
        bfdParams.add(getBfdParams(AlivenessMonitorConstants.BFD_PARAM_MIN_TX, Long.toString(profile.getMinTx())));
        bfdParams.add(
                getBfdParams(AlivenessMonitorConstants.BFD_PARAM_DECAY_MIN_RX, Long.toString(profile.getDecayMinRx())));
        bfdParams.add(getBfdParams(AlivenessMonitorConstants.BFD_PARAM_FORWARDING_IF_RX, profile.getForwardingIfRx()));
        bfdParams.add(getBfdParams(AlivenessMonitorConstants.BFD_PARAM_CPATH_DOWN, profile.getCpathDown()));
        bfdParams.add(getBfdParams(AlivenessMonitorConstants.BFD_PARAM_CHECK_TNL_KEY, profile.getCheckTnlKey()));
    }

    private void setBfdParamForEnable(List<BfdParams> bfdParams, boolean isEnabled) {
        bfdParams.add(getBfdParams(AlivenessMonitorConstants.BFD_PARAM_ENABLE, Boolean.toString(isEnabled)));
    }

    private BfdParams getBfdParams(String key, String value) {
        return new BfdParamsBuilder().setBfdParamKey(key).withKey(new BfdParamsKey(key)).setBfdParamValue(value)
                .build();
    }

    @Override
    public String getUniqueMonitoringKey(MonitoringInfo monitorInfo) {
        String interfaceName = getInterfaceName(monitorInfo.getSource().getEndpointType());
        return getBfdMonitorKey(interfaceName);
    }

    private String getBfdMonitorKey(String interfaceName) {
        return interfaceName + "bfd";
    }

    private String getInterfaceName(EndpointType endpoint) {
        String interfaceName = null;
        if (endpoint instanceof Interface) {
            interfaceName = ((Interface) endpoint).getInterfaceName();
        }
        return interfaceName;
    }

    private static InstanceIdentifier<Tunnels> getTunnelIdentifier(String topologyId, String nodeId,
                                                                   TunnelsKey tunnelsKey) {
        return InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(topologyId)))
                .child(Node.class, new NodeKey(new NodeId(nodeId))).augmentation(PhysicalSwitchAugmentation.class)
                .child(Tunnels.class, tunnelsKey).build();
    }
}
