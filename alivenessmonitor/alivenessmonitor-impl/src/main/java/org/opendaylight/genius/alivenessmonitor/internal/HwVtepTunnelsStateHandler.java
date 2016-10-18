/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.alivenessmonitor.internal;

import static org.opendaylight.genius.alivenessmonitor.internal.AlivenessMonitorUtil.getMonitorStateId;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Semaphore;
import org.opendaylight.controller.liblldp.Packet;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.hwvtep.HwvtepAbstractDataTreeChangeListener;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.LivenessState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.endpoint.EndpointType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.endpoint.endpoint.type.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitor.configs.MonitoringInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitor.profiles.MonitorProfile;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitoring.states.MonitoringState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitoring.states.MonitoringStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorRef;
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

public class HwVtepTunnelsStateHandler extends HwvtepAbstractDataTreeChangeListener<Tunnels,HwVtepTunnelsStateHandler> implements AlivenessProtocolHandler, AutoCloseable {
    private DataBroker dataBroker;
    private ServiceProvider serviceProvider;
    private static final Logger logger = LoggerFactory.getLogger(HwVtepTunnelsStateHandler.class);

    public HwVtepTunnelsStateHandler() {
        super(Tunnels.class,HwVtepTunnelsStateHandler.class);
    }

    public HwVtepTunnelsStateHandler(ServiceProvider serviceProvider) {
        this();
        this.serviceProvider = serviceProvider;
        dataBroker = serviceProvider.getDataBroker();
        registerListener(LogicalDatastoreType.CONFIGURATION,this.dataBroker);

    }
    
    @Override
    protected void removed(InstanceIdentifier<Tunnels> identifier, Tunnels del) {
        // TODO Auto-generated method stub

    }

    @Override
    protected void updated(InstanceIdentifier<Tunnels> identifier, Tunnels oldTunnelInfo, Tunnels updatedTunnelInfo) {
        List<BfdStatus> oldBfdStatus = oldTunnelInfo.getBfdStatus();
        List<BfdStatus> newBfdStatus = updatedTunnelInfo.getBfdStatus();
        LivenessState oldTunnelOpState = getTunnelOpState(oldBfdStatus);
        final LivenessState newTunnelOpState = getTunnelOpState(newBfdStatus);
        if (oldTunnelOpState == newTunnelOpState) {
            logger.debug("Tunnel state of old tunnel {} and update tunnel {} are same", oldTunnelInfo, updatedTunnelInfo);
            return;
        }
        updatedTunnelInfo.getTunnelUuid();
        String interfaceName = "<TODO>";
        //TODO: find out the corresponding interface using tunnelIdentifier or any attributes of tunneInfo object
        final String monitorKey = getBfdMonitorKey(interfaceName);
        logger.debug("Processing monitorKey: {} for received Tunnels update DCN", monitorKey);

        final AlivenessMonitor alivenessMonitor = (AlivenessMonitor) serviceProvider;
        final Semaphore lock = alivenessMonitor.lockMap.get(monitorKey);
        logger.debug("Acquiring lock for monitor key : {} to process monitor DCN", monitorKey);
        alivenessMonitor.acquireLock(lock);

        final ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();

        ListenableFuture<Optional<MonitoringState>> stateResult = tx.read(LogicalDatastoreType.OPERATIONAL, getMonitorStateId(monitorKey));
        Futures.addCallback(stateResult, new FutureCallback<Optional<MonitoringState>>() {

            @Override
            public void onSuccess(Optional<MonitoringState> optState) {
                if(optState.isPresent()) {
                    final MonitoringState currentState = optState.get();
                    if (currentState.getState() == newTunnelOpState) {
                        return;
                    }
                    final boolean stateChanged = true;
                    final MonitoringState state = new MonitoringStateBuilder().setMonitorKey(monitorKey).setState(newTunnelOpState).build();
                    tx.merge(LogicalDatastoreType.OPERATIONAL, getMonitorStateId(monitorKey), state);
                    ListenableFuture<Void> writeResult = tx.submit();
                    //WRITE Callback
                    Futures.addCallback(writeResult, new FutureCallback<Void>() {

                        @Override
                        public void onSuccess(Void arg0) {
                            alivenessMonitor.releaseLock(lock);
                            if(stateChanged) {
                                //send notifications
                                logger.info("Sending notification for monitor Id : {} with Current State: {}",
                                        currentState.getMonitorId(), newTunnelOpState);
                                alivenessMonitor.publishNotification(currentState.getMonitorId(), newTunnelOpState);
                            } else {
                                if(logger.isTraceEnabled()) {
                                    logger.trace("Successful in writing monitoring state {} to ODS", state);
                                }
                            }
                        }
                        @Override
                        public void onFailure(Throwable error) {
                            alivenessMonitor.releaseLock(lock);
                            logger.warn("Error in writing monitoring state : {} to Datastore", monitorKey, error);
                            if(logger.isTraceEnabled()) {
                                logger.trace("Error in writing monitoring state: {} to Datastore", state);
                            }
                        }
                    });
                } else {
                    logger.warn("Monitoring State not available for key: {} to process the Packet received", monitorKey);
                    //Complete the transaction
                    tx.submit();
                    alivenessMonitor.releaseLock(lock);
                }
            }

            @Override
            public void onFailure(Throwable error) {
                logger.error("Error when reading Monitoring State for key: {} to process the Packet received", monitorKey, error);
                //FIXME: Not sure if the transaction status is valid to cancel
                tx.cancel();
                alivenessMonitor.releaseLock(lock);
            }
        });
    }

    private LivenessState getTunnelOpState(List<BfdStatus> tunnelBfdStatus) {
        LivenessState livenessState = LivenessState.Unknown;
        if (tunnelBfdStatus == null || tunnelBfdStatus.isEmpty()) {
            return livenessState;
        }
        for (BfdStatus bfdState: tunnelBfdStatus) {
            if (bfdState.getBfdStatusKey().equalsIgnoreCase(AlivenessMonitorConstants.BFD_OP_STATE)) {
                String bfdOpState = bfdState.getBfdStatusValue();
                if (bfdOpState.equalsIgnoreCase(AlivenessMonitorConstants.BFD_STATE_UP)) {
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
    protected void added(InstanceIdentifier<Tunnels> identifier, Tunnels add) {
        // TODO: need to add the code to enable BFD if tunnels are created dynamically by TOR switch
    }

    @Override
    public Class<?> getPacketClass() {
        return null;
    }

    @Override
    public String handlePacketIn(Packet protocolPacket, PacketReceived packetReceived) {
        return null;
    }

    void resetMonitoringTask(MonitoringInfo monitorInfo, boolean isEnable) {
        // TODO: get the corresponding hwvtep tunnel from the sourceInterface once InterfaceMgr
        // implments renderer for hwvtep vxlan tunnels
        TunnelsKey tunnelKey = null;
        String nodeId = null;
        String topologyId = null;
        Optional<Tunnels> tunnelsOptional = ((AlivenessMonitor) serviceProvider).read(LogicalDatastoreType.CONFIGURATION,
                getTunnelIdentifier(topologyId, nodeId, tunnelKey));
        if (!tunnelsOptional.isPresent()) {
            logger.warn("Tunnel {} is not present on the Node {}. So not disabling the BFD monitoing", tunnelKey, nodeId);
            return;
        }
        Tunnels tunnel = tunnelsOptional.get();
        List<BfdParams> tunnelBfdParams = tunnel.getBfdParams();
        if (tunnelBfdParams == null || tunnelBfdParams.isEmpty()) {
            logger.debug("there is no bfd params available for the tunnel {}", tunnel);
        }
        Iterator<BfdParams> tunnelBfdParamsInterator = tunnelBfdParams.iterator();
        while (tunnelBfdParamsInterator.hasNext()) {
            BfdParams bfdParam = tunnelBfdParamsInterator.next();
            if (bfdParam.getBfdParamKey().equals(AlivenessMonitorConstants.BFD_PARAM_ENABLE)) {
                tunnelBfdParamsInterator.remove();
                break;
            }
        }
        setBfdParamForEnable(tunnelBfdParams, isEnable);
        Tunnels tunnelWithBfdReset = new TunnelsBuilder().setKey(tunnelKey).setBfdParams(tunnelBfdParams).build();
        MDSALUtil.syncUpdate(dataBroker, LogicalDatastoreType.CONFIGURATION, getTunnelIdentifier(topologyId, nodeId, tunnelKey), tunnelWithBfdReset);
    }

    @Override
    public void startMonitoringTask(MonitoringInfo monitorInfo) {
        EndpointType source = monitorInfo.getSource().getEndpointType();
        if( source instanceof Interface) {
            Interface intf = (Interface)source;
            intf.getInterfaceName();
        } else {
            logger.warn("Invalid source endpoint. Could not retrieve source interface to configure BFD");
            return;
        }
        MonitorProfile profile;
        long profileId = monitorInfo.getProfileId();
        Optional<MonitorProfile> optProfile = ((AlivenessMonitor) serviceProvider).getMonitorProfile(profileId);
        if(optProfile.isPresent()) {
            profile = optProfile.get();
        } else {
            logger.warn("No monitor profile associated with id {}. "
                    + "Could not send Monitor packet for monitor-id {}", profileId, monitorInfo);
            return;
        }
        // TODO: get the corresponding hwvtep tunnel from the sourceInterface once InterfaceMgr
        // implments renderer for hwvtep vxlan tunnels
        TunnelsKey tunnelKey = null;
        String tunnelLocalMacAddress = "<TODO>";
        String tunnelLocalIpAddress = "<TODO>";
        String tunnelRemoteMacAddress = "<TODO>";
        String tunnelRemoteIpAddress = "<TODO>";
        List<BfdParams> bfdParams = new ArrayList<>();
        fillBfdParams(bfdParams, profile);
        List<BfdLocalConfigs> bfdLocalConfigs = new ArrayList<>();
        fillBfdLocalConfigs(bfdLocalConfigs, tunnelLocalMacAddress, tunnelLocalIpAddress);
        List<BfdRemoteConfigs> bfdRemoteConfigs = new ArrayList<>();
        fillBfdRemoteConfigs(bfdRemoteConfigs, tunnelRemoteMacAddress, tunnelRemoteIpAddress);
        Tunnels tunnelWithBfd = new TunnelsBuilder().setKey(tunnelKey).setBfdParams(bfdParams)
                .setBfdLocalConfigs(bfdLocalConfigs).setBfdRemoteConfigs(bfdRemoteConfigs).build();
        // TODO: get the following parameters from the interface and use it to update hwvtep datastore
        // and not sure sure tunnels are creating immediately once interface mgr writes termination point
        // into hwvtep datastore. if tunnels are not created during that time, then start monitoring has to
        // be done as part of tunnel add DCN handling.
        HwvtepPhysicalLocatorRef remoteRef = null;
        HwvtepPhysicalLocatorRef localRef = null;
        String topologyId = "";
        String nodeId = "";
        MDSALUtil.syncUpdate(dataBroker, LogicalDatastoreType.CONFIGURATION, getTunnelIdentifier(topologyId, nodeId, new TunnelsKey(localRef, remoteRef)), tunnelWithBfd);
    }

    private void fillBfdRemoteConfigs(List<BfdRemoteConfigs> bfdRemoteConfigs, String tunnelRemoteMacAddress,
                                      String tunnelRemoteIpAddress) {
        bfdRemoteConfigs.add(getBfdRemoteConfig(AlivenessMonitorConstants.BFD_CONFIG_BFD_DST_MAC, tunnelRemoteMacAddress));
    }

    private BfdRemoteConfigs getBfdRemoteConfig(String key, String value) {
        return new BfdRemoteConfigsBuilder().setBfdRemoteConfigKey(key).setBfdRemoteConfigValue(value)
                .setKey(new BfdRemoteConfigsKey(key)).build();
    }

    private void fillBfdLocalConfigs(List<BfdLocalConfigs> bfdLocalConfigs, String tunnelLocalMacAddress,
                                     String tunnelLocalIpAddress) {
        bfdLocalConfigs.add(getBfdLocalConfig(AlivenessMonitorConstants.BFD_CONFIG_BFD_DST_MAC, tunnelLocalMacAddress));
        bfdLocalConfigs.add(getBfdLocalConfig(AlivenessMonitorConstants.BFD_CONFIG_BFD_DST_IP, tunnelLocalIpAddress));
    }

    private BfdLocalConfigs getBfdLocalConfig(String key, String value) {
        return new BfdLocalConfigsBuilder().setBfdLocalConfigKey(key).setKey(new BfdLocalConfigsKey(key))
                .setBfdLocalConfigValue(value).build();
    }

    private void fillBfdParams(List<BfdParams> bfdParams, MonitorProfile profile) {
        setBfdParamForEnable(bfdParams, true);
        bfdParams.add(getBfdParams(AlivenessMonitorConstants.BFD_PARAM_MIN_RX, Long.toString(profile.getMinRx())));
        bfdParams.add(getBfdParams(AlivenessMonitorConstants.BFD_PARAM_MIN_TX, Long.toString(profile.getMinTx())));
        bfdParams.add(getBfdParams(AlivenessMonitorConstants.BFD_PARAM_DECAY_MIN_RX, Long.toString(profile.getDecayMinRx())));
        bfdParams.add(getBfdParams(AlivenessMonitorConstants.BFD_PARAM_FORWARDING_IF_RX, profile.getForwardingIfRx()));
        bfdParams.add(getBfdParams(AlivenessMonitorConstants.BFD_PARAM_CPATH_DOWN, profile.getCpathDown()));
        bfdParams.add(getBfdParams(AlivenessMonitorConstants.BFD_PARAM_CHECK_TNL_KEY, profile.getCheckTnlKey()));
    }

    private void setBfdParamForEnable(List<BfdParams> bfdParams, boolean isEnabled) {
        bfdParams.add(getBfdParams(AlivenessMonitorConstants.BFD_PARAM_ENABLE, Boolean.toString(isEnabled)));
    }

    private BfdParams getBfdParams(String key, String value) {
        return new BfdParamsBuilder().setBfdParamKey(key).setKey(new BfdParamsKey(key))
                .setBfdParamValue(value).build();
    }

    @Override
    public String getUniqueMonitoringKey(MonitoringInfo monitorInfo) {
        String interfaceName = getInterfaceName(monitorInfo.getSource().getEndpointType());
        return getBfdMonitorKey(interfaceName);
    }

    private String getBfdMonitorKey(String interfaceName) {
        return new StringBuilder().append(interfaceName).append("bfd").toString();
    }

    private String getInterfaceName(EndpointType endpoint) {
        String interfaceName = null;
        if(endpoint instanceof Interface) {
            interfaceName = ((Interface)endpoint).getInterfaceName();
        }
        return interfaceName;
    }

    public static InstanceIdentifier<Tunnels> getTunnelIdentifier(String topologyId, String nodeId, TunnelsKey tunnelsKey) {
        return InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class, new TopologyKey(new TopologyId(topologyId)))
                .child(Node.class, new NodeKey(new NodeId(nodeId))).augmentation(PhysicalSwitchAugmentation.class)
                .child(Tunnels.class, tunnelsKey).build();
    }

    @Override
	protected InstanceIdentifier<Tunnels> getWildCardPath() {
		return InstanceIdentifier.create(NetworkTopology.class).child(Topology.class).child(Node.class)
                .augmentation(PhysicalSwitchAugmentation.class).child(Tunnels.class);
	}

	@Override
	protected HwVtepTunnelsStateHandler getDataTreeChangeListener() {
		return HwVtepTunnelsStateHandler.this;
	}

}
