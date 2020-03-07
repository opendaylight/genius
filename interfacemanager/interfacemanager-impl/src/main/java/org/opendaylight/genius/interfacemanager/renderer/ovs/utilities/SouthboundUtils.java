/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.renderer.ovs.utilities;

import static org.opendaylight.controller.md.sal.binding.api.WriteTransaction.CREATE_MISSING_PARENTS;

import com.google.common.collect.Maps;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.aries.blueprint.annotation.service.Reference;
import org.apache.commons.lang3.BooleanUtils;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.genius.infra.Datastore.Configuration;
import org.opendaylight.genius.infra.TypedWriteTransaction;
import org.opendaylight.genius.interfacemanager.InterfacemgrProvider;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.globals.IfmConstants;
import org.opendaylight.infrautils.caches.Cache;
import org.opendaylight.infrautils.caches.CacheConfigBuilder;
import org.opendaylight.infrautils.caches.CachePolicyBuilder;
import org.opendaylight.infrautils.caches.CacheProvider;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge._interface.info.BridgeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge._interface.info.bridge.entry.BridgeInterfaceEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBfd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeMplsOverGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlanGpe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.tunnel.optional.params.TunnelOptions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbPortInterfaceAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceBfd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceBfdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceBfdKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SouthboundUtils {
    private static final Logger LOG = LoggerFactory.getLogger(SouthboundUtils.class);
    private static final Logger EVENT_LOGGER = LoggerFactory.getLogger("GeniusEventLogger");

    private static final String BFD_PARAM_ENABLE = "enable";
    private static final String BFD_PARAM_MIN_TX = "min_tx";
    private static final String BFD_PARAM_FORWARDING_IF_RX = "forwarding_if_rx";

    // BFD parameters
    public static final String BFD_ENABLE_KEY = "enable";
    public static final String BFD_ENABLE_VALUE = "true";
    public static final String BFD_OP_STATE = "state";
    public static final String BFD_STATE_UP = "up";
    private static final String BFD_MIN_TX_VAL = "100";
    private static final String BFD_FORWARDING_IF_RX_VAL = "true";

    // Tunnel options
    private static final String TUNNEL_OPTIONS_KEY = "key";
    private static final String TUNNEL_OPTIONS_LOCAL_IP = "local_ip";
    private static final String TUNNEL_OPTIONS_REMOTE_IP = "remote_ip";
    private static final String TUNNEL_OPTIONS_DESTINATION_PORT = "dst_port";

    // Option values for VxLAN-GPE + NSH tunnels
    private static final String TUNNEL_OPTIONS_EXTS = "exts";
    private static final String TUNNEL_OPTIONS_NSI = "nsi";
    private static final String TUNNEL_OPTIONS_NSP = "nsp";
    private static final String TUNNEL_OPTIONS_NSHC1 = "nshc1";
    private static final String TUNNEL_OPTIONS_NSHC2 = "nshc2";
    private static final String TUNNEL_OPTIONS_NSHC3 = "nshc3";
    private static final String TUNNEL_OPTIONS_NSHC4 = "nshc4";

    // Tunnel options for MPLS-GRE tunnels [requires OVS 2.8+)
    private static final String TUNNEL_OPTIONS_PKT_TYPE = "packet_type";

    // Option values for VxLAN-GPE + NSH tunnels
    private static final String TUNNEL_OPTIONS_VALUE_FLOW = "flow";
    private static final String TUNNEL_OPTIONS_VALUE_GPE = "gpe";
    // UDP port for VxLAN-GPE Tunnels
    private static final String TUNNEL_OPTIONS_VALUE_GPE_DESTINATION_PORT = "4880";

    // Tunnel option values for MPLS-GRE tunnels [requires OVS 2.8+)
    private static final String TUNNEL_OPTIONS_VALUE_LEGACY_L3 = "legacy_l3";

    // To keep the mapping between Tunnel Types and Tunnel Interfaces
    private static final Map<Class<? extends TunnelTypeBase>, Class<? extends InterfaceTypeBase>>
        TUNNEL_TYPE_MAP = new HashMap<Class<? extends TunnelTypeBase>, Class<? extends InterfaceTypeBase>>() {
            {
                put(TunnelTypeGre.class, InterfaceTypeGre.class);
                put(TunnelTypeMplsOverGre.class, InterfaceTypeGre.class);
                put(TunnelTypeVxlan.class, InterfaceTypeVxlan.class);
                put(TunnelTypeVxlanGpe.class, InterfaceTypeVxlan.class);
            }
        };

    // OVS Detection statics
    private static final String DEFAULT_OVS_VERSION = "2.8.0";
    private static final String MIN_GRE_VERSION = "2.8.0";
    private static final long MAX_CACHE_SIZE = 1024;

    private final SingleTransactionDataBroker singleTxDB;

    private final BatchingUtils batchingUtils;
    private final InterfacemgrProvider interfacemgrProvider;
    private final Cache<String, String> ovsVersionCache;

    @Inject
    public SouthboundUtils(@Reference final DataBroker dataBroker,
                           final BatchingUtils batchingUtils, InterfacemgrProvider interfacemgrProvider,
                           @Reference final CacheProvider cacheProvider) {
        this.batchingUtils = batchingUtils;
        this.interfacemgrProvider = interfacemgrProvider;
        this.singleTxDB = new SingleTransactionDataBroker(dataBroker);
        ovsVersionCache = cacheProvider.newCache(
                new CacheConfigBuilder<String, String>()
                        .anchor(this)
                        .id("ovsVersionCache")
                        .cacheFunction(key -> getVersionForBridgeNodeId(key))
                        .description("BridgeNodeId to OVS Version cache")
                        .build(),
                new CachePolicyBuilder().maxEntries(MAX_CACHE_SIZE).build());
    }

    public void addPortToBridge(InstanceIdentifier<?> bridgeIid, Interface iface, String portName) {
        IfTunnel ifTunnel = iface.augmentation(IfTunnel.class);
        if (ifTunnel != null) {
            addTunnelPortToBridge(ifTunnel, bridgeIid, iface, portName);
        }
    }

    /*
     * Add all tunnels ports corresponding to the bridge to the topology config DS.
     */
    public void addAllPortsToBridge(BridgeEntry bridgeEntry, InterfaceManagerCommonUtils interfaceManagerCommonUtils,
                                    InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIid,
                                    OvsdbBridgeAugmentation bridgeNew) {
        String bridgeName = bridgeNew.getBridgeName().getValue();
        LOG.debug("adding all ports to bridge: {}", bridgeName);
        List<BridgeInterfaceEntry> bridgeInterfaceEntries = bridgeEntry.getBridgeInterfaceEntry();
        if (bridgeInterfaceEntries != null) {
            for (BridgeInterfaceEntry bridgeInterfaceEntry : bridgeInterfaceEntries) {
                String portName = bridgeInterfaceEntry.getInterfaceName();
                InterfaceKey interfaceKey = new InterfaceKey(portName);
                Interface iface = interfaceManagerCommonUtils.getInterfaceFromConfigDS(interfaceKey);
                if (iface != null) {
                    IfTunnel ifTunnel = iface.augmentation(IfTunnel.class);
                    if (ifTunnel != null) {
                        if (!(interfacemgrProvider.isItmDirectTunnelsEnabled() && ifTunnel.isInternal())) {
                            addTunnelPortToBridge(ifTunnel, bridgeIid, iface, portName);
                        }
                        if (isOfTunnel(ifTunnel)) {
                            LOG.debug("Using OFTunnel. Only one tunnel port will be added");
                            return;
                        }
                    }
                } else {
                    LOG.debug("Interface {} not found in config DS", portName);
                }
            }
        }
    }

    private void addTunnelPortToBridge(IfTunnel ifTunnel, InstanceIdentifier<?> bridgeIid, Interface iface,
            String portName) {
        LOG.debug("adding tunnel port {} to bridge {}", portName, bridgeIid);
        EVENT_LOGGER.debug("IFM-OvsInterfaceConfig,ADD Tunnelport {} Bridgeid {}", portName, bridgeIid);
        Class<? extends InterfaceTypeBase> type = TUNNEL_TYPE_MAP.get(ifTunnel.getTunnelInterfaceType());

        if (type == null) {
            LOG.warn("Unknown Tunnel Type obtained while creating interface: {}", iface);
            return;
        }

        int vlanId = 0;
        IfL2vlan ifL2vlan = iface.augmentation(IfL2vlan.class);
        if (ifL2vlan != null && ifL2vlan.getVlanId() != null) {
            vlanId = ifL2vlan.getVlanId().getValue();
        }

        Map<String, String> options = Maps.newHashMap();

        // Options common to any kind of tunnel
        if (BooleanUtils.isTrue(ifTunnel.isTunnelSourceIpFlow())) {
            options.put(TUNNEL_OPTIONS_LOCAL_IP, TUNNEL_OPTIONS_VALUE_FLOW);
        } else {
            IpAddress localIp = ifTunnel.getTunnelSource();
            options.put(TUNNEL_OPTIONS_LOCAL_IP, localIp.stringValue());
        }
        if (BooleanUtils.isTrue(ifTunnel.isTunnelRemoteIpFlow())) {
            options.put(TUNNEL_OPTIONS_REMOTE_IP, TUNNEL_OPTIONS_VALUE_FLOW);
        } else {
            IpAddress remoteIp = ifTunnel.getTunnelDestination();
            options.put(TUNNEL_OPTIONS_REMOTE_IP, remoteIp.stringValue());
        }
        // Specific options for each type of tunnel
        if (TunnelTypeMplsOverGre.class.equals(ifTunnel.getTunnelInterfaceType())) {
            String switchVersion = getSwitchVersion((InstanceIdentifier<Node>) bridgeIid);
            LOG.debug("Switch OVS Version: {}", switchVersion);
            if (org.opendaylight.ovsdb.utils.southbound.utils.SouthboundUtils.compareDbVersionToMinVersion(
                    switchVersion, MIN_GRE_VERSION)) {
                options.put(TUNNEL_OPTIONS_PKT_TYPE, TUNNEL_OPTIONS_VALUE_LEGACY_L3);
            } else {
                LOG.warn("{} OVS version {} less than {} required for MplsOverGre",
                        bridgeIid.firstKeyOf(Node.class).getNodeId().getValue(),
                        switchVersion, MIN_GRE_VERSION);
                return;
            }
        } else {
            options.put(TUNNEL_OPTIONS_KEY, TUNNEL_OPTIONS_VALUE_FLOW);
        }
        if (ifTunnel.getTunnelInterfaceType().equals(TunnelTypeVxlanGpe.class)) {
            options.put(TUNNEL_OPTIONS_EXTS, TUNNEL_OPTIONS_VALUE_GPE);
            options.put(TUNNEL_OPTIONS_NSI, TUNNEL_OPTIONS_VALUE_FLOW);
            options.put(TUNNEL_OPTIONS_NSP, TUNNEL_OPTIONS_VALUE_FLOW);
            options.put(TUNNEL_OPTIONS_NSHC1, TUNNEL_OPTIONS_VALUE_FLOW);
            options.put(TUNNEL_OPTIONS_NSHC2, TUNNEL_OPTIONS_VALUE_FLOW);
            options.put(TUNNEL_OPTIONS_NSHC3, TUNNEL_OPTIONS_VALUE_FLOW);
            options.put(TUNNEL_OPTIONS_NSHC4, TUNNEL_OPTIONS_VALUE_FLOW);
            // VxLAN-GPE interfaces will not use the default UDP port to avoid
            // problems with other meshes
            options.put(TUNNEL_OPTIONS_DESTINATION_PORT, TUNNEL_OPTIONS_VALUE_GPE_DESTINATION_PORT);
        }

        if (ifTunnel.getTunnelOptions() != null) {
            for (TunnelOptions tunOpt : ifTunnel.getTunnelOptions()) {
                options.putIfAbsent(tunOpt.getTunnelOption(), tunOpt.getValue());
            }
        }

        addTerminationPoint(bridgeIid, portName, vlanId, type, options, ifTunnel);
    }

    // Update is allowed only for tunnel monitoring attributes
    public static void updateBfdParamtersForTerminationPoint(InstanceIdentifier<?> bridgeIid, IfTunnel ifTunnel,
            String portName, TypedWriteTransaction<Configuration> transaction) {
        InstanceIdentifier<TerminationPoint> tpIid = createTerminationPointInstanceIdentifier(
                InstanceIdentifier.keyOf(bridgeIid.firstIdentifierOf(Node.class)), portName);
        if (isOfTunnel(ifTunnel)) {
            LOG.warn("BFD monitoring not supported for OFTunnels. Skipping BFD parameters for {}", portName);
            return;
        }
        LOG.debug("update bfd parameters for interface {}", tpIid);
        OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();
        List<InterfaceBfd> bfdParams = getBfdParams(ifTunnel);
        tpAugmentationBuilder.setInterfaceBfd(bfdParams);

        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.withKey(InstanceIdentifier.keyOf(tpIid));
        tpBuilder.addAugmentation(OvsdbTerminationPointAugmentation.class, tpAugmentationBuilder.build());

        transaction.merge(tpIid, tpBuilder.build(), CREATE_MISSING_PARENTS);
    }

    private void addTerminationPoint(InstanceIdentifier<?> bridgeIid, String portName, int vlanId,
            Class<? extends InterfaceTypeBase> type, Map<String, String> options, IfTunnel ifTunnel) {
        OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();

        tpAugmentationBuilder.setName(portName);

        if (type != null) {
            tpAugmentationBuilder.setInterfaceType(type);
        }

        if (options != null) {
            List<Options> optionsList = new ArrayList<>();
            for (Map.Entry<String, String> entry : options.entrySet()) {
                OptionsBuilder optionsBuilder = new OptionsBuilder();
                optionsBuilder.withKey(new OptionsKey(entry.getKey()));
                optionsBuilder.setOption(entry.getKey());
                optionsBuilder.setValue(entry.getValue());
                optionsList.add(optionsBuilder.build());
            }
            tpAugmentationBuilder.setOptions(optionsList);
        }

        if (vlanId != 0) {
            tpAugmentationBuilder.setVlanMode(OvsdbPortInterfaceAttributes.VlanMode.Access);
            tpAugmentationBuilder.setVlanTag(new VlanId(vlanId));
        }
        if (isOfTunnel(ifTunnel)) {
            LOG.warn("BFD Monitoring not supported for OFTunnels");
        } else {
            List<InterfaceBfd> bfdParams = getBfdParams(ifTunnel);
            tpAugmentationBuilder.setInterfaceBfd(bfdParams);
        }
        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        InstanceIdentifier<TerminationPoint> tpIid = createTerminationPointInstanceIdentifier(
                InstanceIdentifier.keyOf(bridgeIid.firstIdentifierOf(Node.class)), portName);
        tpBuilder.withKey(InstanceIdentifier.keyOf(tpIid));
        tpBuilder.addAugmentation(OvsdbTerminationPointAugmentation.class, tpAugmentationBuilder.build());

        batchingUtils.write(tpIid, tpBuilder.build(), BatchingUtils.EntityType.TOPOLOGY_CONFIG);
    }

    private static List<InterfaceBfd> getBfdParams(IfTunnel ifTunnel) {
        List<InterfaceBfd> bfdParams = new ArrayList<>();
        bfdParams.add(
                getIfBfdObj(BFD_PARAM_ENABLE, ifTunnel != null ? ifTunnel.isMonitorEnabled().toString() : "false"));
        bfdParams.add(getIfBfdObj(BFD_PARAM_MIN_TX,
                ifTunnel != null && ifTunnel.getMonitorInterval() != null ? ifTunnel.getMonitorInterval().toString()
                        : BFD_MIN_TX_VAL));
        bfdParams.add(getIfBfdObj(BFD_PARAM_FORWARDING_IF_RX, BFD_FORWARDING_IF_RX_VAL));
        return bfdParams;
    }

    private static InterfaceBfd getIfBfdObj(String key, String value) {
        InterfaceBfdBuilder bfdBuilder = new InterfaceBfdBuilder();
        bfdBuilder.setBfdKey(key).withKey(new InterfaceBfdKey(key)).setBfdValue(value);
        return bfdBuilder.build();
    }

    private String getSwitchVersion(InstanceIdentifier<Node> bridgeIid) {
        String ovsNodeId = bridgeIid.firstKeyOf(Node.class).getNodeId().getValue().split("/bridge")[0];
        return ovsVersionCache.get(ovsNodeId);
    }

    private String getVersionForBridgeNodeId(String ovsNodeId) {
        InstanceIdentifier<Node> ovsNodeIid = InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(IfmConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(new NodeId(ovsNodeId)));
        String ovsVersion = DEFAULT_OVS_VERSION;
        try {
            Node ovsNode = singleTxDB.syncRead(LogicalDatastoreType.OPERATIONAL, ovsNodeIid);
            ovsVersion = ovsNode.augmentation(OvsdbNodeAugmentation.class).getOvsVersion()
                    .toLowerCase(Locale.ROOT);
        } catch (ReadFailedException e) {
            LOG.error("OVS Node {} not present", ovsNodeId);
        }
        return ovsVersion;
    }

    public static InstanceIdentifier<TerminationPoint> createTerminationPointInstanceIdentifier(NodeKey nodekey,
            String portName) {
        InstanceIdentifier<TerminationPoint> terminationPointPath = InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(IfmConstants.OVSDB_TOPOLOGY_ID)).child(Node.class, nodekey)
                .child(TerminationPoint.class, new TerminationPointKey(new TpId(portName)));

        LOG.debug("Termination point InstanceIdentifier generated : {}", terminationPointPath);
        return terminationPointPath;
    }

    public void removeTerminationEndPoint(InstanceIdentifier<?> bridgeIid, String interfaceName) {
        LOG.debug("removing termination point for {}", interfaceName);
        InstanceIdentifier<TerminationPoint> tpIid = SouthboundUtils.createTerminationPointInstanceIdentifier(
                InstanceIdentifier.keyOf(bridgeIid.firstIdentifierOf(Node.class)), interfaceName);
        batchingUtils.delete(tpIid, BatchingUtils.EntityType.TOPOLOGY_CONFIG);
    }

    public static boolean ifBfdStatusNotEqual(OvsdbTerminationPointAugmentation tpOld,
                                           OvsdbTerminationPointAugmentation tpNew) {
        return (tpNew.getInterfaceBfdStatus() != null
                && (tpOld == null || !tpNew.getInterfaceBfdStatus().equals(tpOld.getInterfaceBfdStatus())));
    }

    public static boolean changeInBfdMonitoringDetected(OvsdbTerminationPointAugmentation tpOld,
                                                        OvsdbTerminationPointAugmentation tpNew) {
        if (tpOld != null) {
            return org.opendaylight.genius.interfacemanager.renderer.ovs.utilities.SouthboundUtils
                    .bfdMonitoringEnabled(tpNew.getInterfaceBfd())
                    != org.opendaylight.genius.interfacemanager.renderer.ovs.utilities.SouthboundUtils
                    .bfdMonitoringEnabled(tpOld.getInterfaceBfd());
        }
        return false;
    }

    public static boolean bfdMonitoringEnabled(IfTunnel ifTunnel) {
        return ifTunnel.isMonitorEnabled()
                && TunnelMonitoringTypeBfd.class.isAssignableFrom(ifTunnel.getMonitorProtocol());
    }

    public static boolean bfdMonitoringEnabled(List<InterfaceBfd> interfaceBfds) {
        if (interfaceBfds == null) {
            return false;
        }
        for (InterfaceBfd interfaceBfd : interfaceBfds) {
            if (SouthboundUtils.BFD_ENABLE_KEY.equalsIgnoreCase(interfaceBfd.getBfdKey())) {
                return SouthboundUtils.BFD_ENABLE_VALUE.equalsIgnoreCase(interfaceBfd.getBfdValue());//checkBfdEnabled
            }
        }
        return false;
    }

    public static boolean isMonitorProtocolBfd(IfTunnel ifTunnel) {
        return TunnelMonitoringTypeBfd.class.isAssignableFrom(ifTunnel.getMonitorProtocol());
    }

    @SuppressFBWarnings("DM_DEFAULT_ENCODING")
    public static String generateOfTunnelName(BigInteger dpId, IfTunnel ifTunnel) {
        String sourceKey = ifTunnel.getTunnelSource().stringValue();
        String remoteKey = ifTunnel.getTunnelDestination().stringValue();
        if (ifTunnel.isTunnelSourceIpFlow() != null) {
            sourceKey = "flow";
        }
        if (ifTunnel.isTunnelRemoteIpFlow() != null) {
            remoteKey = "flow";
        }
        String tunnelNameKey = dpId.toString() + sourceKey + remoteKey;
        String uuidStr = UUID.nameUUIDFromBytes(tunnelNameKey.getBytes()).toString().substring(0, 12).replace("-", "");
        return String.format("%s%s", "tun", uuidStr);
    }

    public static boolean isOfTunnel(IfTunnel ifTunnel) {
        return Boolean.TRUE.equals(ifTunnel.isTunnelRemoteIpFlow())
                || Boolean.TRUE.equals(ifTunnel.isTunnelSourceIpFlow());
    }

    public static boolean isInterfaceTypeTunnel(Class<? extends InterfaceTypeBase> interfaceType) {
        return InterfaceTypeGre.class.equals(interfaceType) || InterfaceTypeVxlan.class.equals(interfaceType);
    }

}
