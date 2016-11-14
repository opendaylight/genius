/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.renderer.ovs.utilities;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
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

public class SouthboundUtils {
    private static final Logger LOG = LoggerFactory.getLogger(SouthboundUtils.class);

    public static final String BFD_PARAM_ENABLE = "enable";
    static final String BFD_PARAM_MIN_TX = "min_tx";
    static final String BFD_PARAM_MIN_RX = "min_rx";
    static final String BFD_PARAM_DECAY_MIN_RX = "decay_min_rx";
    static final String BFD_PARAM_FORWARDING_IF_RX = "forwarding_if_rx";
    static final String BFD_PARAM_CPATH_DOWN = "cpath_down";
    static final String BFD_PARAM_CHECK_TNL_KEY = "check_tnl_key";

    // BFD parameters
    public static final String BFD_OP_STATE = "state";
    public static final String BFD_STATE_UP = "up";
    private static final String BFD_MIN_RX_VAL = "1000";
    private static final String BFD_MIN_TX_VAL = "100";
    private static final String BFD_DECAY_MIN_RX_VAL = "200";
    private static final String BFD_FORWARDING_IF_RX_VAL = "true";
    private static final String BFD_CPATH_DOWN_VAL = "false";
    private static final String BFD_CHECK_TNL_KEY_VAL = "false";

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

    // Option values for VxLAN-GPE + NSH tunnels
    private static final String TUNNEL_OPTIONS_VALUE_FLOW = "flow";
    private static final String TUNNEL_OPTIONS_VALUE_GPE = "gpe";
     // UDP port for VxLAN-GPE Tunnels
    private static final String TUNNEL_OPTIONS_VALUE_GPE_DESTINATION_PORT = "4880";

    //
    public static final TopologyId OVSDB_TOPOLOGY_ID = new TopologyId(new Uri("ovsdb:1"));

    // To keep the mapping between Tunnel Types and Tunnel Interfaces
    private static final Map<Class<? extends TunnelTypeBase>, Class<? extends InterfaceTypeBase>> TUNNEL_TYPE_MAP = new HashMap<Class<? extends TunnelTypeBase>, Class<? extends InterfaceTypeBase>>() {
        {
            put(TunnelTypeGre.class, InterfaceTypeGre.class);
            put(TunnelTypeVxlan.class, InterfaceTypeVxlan.class);
            put(TunnelTypeVxlanGpe.class, InterfaceTypeVxlan.class);
        }
    };

    public static void addPortToBridge(InstanceIdentifier<?> bridgeIid, Interface iface, String portName, DataBroker dataBroker,
            List<ListenableFuture<Void>> futures) {
        IfTunnel ifTunnel = iface.getAugmentation(IfTunnel.class);
        if (ifTunnel != null) {
            addTunnelPortToBridge(ifTunnel, bridgeIid, iface, portName, dataBroker);
        }
    }

    /*
     * Add all tunnels ports corresponding to the bridge to the topology config
     * DS
     */
    public static void addAllPortsToBridge(BridgeEntry bridgeEntry, DataBroker dataBroker,
            InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIid, OvsdbBridgeAugmentation bridgeNew,
            List<ListenableFuture<Void>> futures) {
        String bridgeName = bridgeNew.getBridgeName().getValue();
        LOG.debug("adding all ports to bridge: {}", bridgeName);
        List<BridgeInterfaceEntry> bridgeInterfaceEntries = bridgeEntry.getBridgeInterfaceEntry();
        if (bridgeInterfaceEntries != null) {
            for (BridgeInterfaceEntry bridgeInterfaceEntry : bridgeInterfaceEntries) {
                String portName = bridgeInterfaceEntry.getInterfaceName();
                InterfaceKey interfaceKey = new InterfaceKey(portName);
                Interface iface = InterfaceManagerCommonUtils.getInterfaceFromConfigDS(interfaceKey, dataBroker);
                if (iface != null) {
                    IfTunnel ifTunnel = iface.getAugmentation(IfTunnel.class);
                    if (ifTunnel != null) {
                        addTunnelPortToBridge(ifTunnel, bridgeIid, iface, portName, dataBroker);
                    }
                } else {
                    LOG.debug("Interface {} not found in config DS", portName);
                }
            }
        }
    }

    /*
     * add all tunnels ports corresponding to the bridge to the topology config
     * DS
     */
    public static void removeAllPortsFromBridge(BridgeEntry bridgeEntry, DataBroker dataBroker,
            InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIid, OvsdbBridgeAugmentation bridgeNew,
            List<ListenableFuture<Void>> futures) {
        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        String bridgeName = bridgeNew.getBridgeName().getValue();
        LOG.debug("removing all ports from bridge: {}", bridgeName);
        List<BridgeInterfaceEntry> bridgeInterfaceEntries = bridgeEntry.getBridgeInterfaceEntry();
        if (bridgeInterfaceEntries != null) {
            for (BridgeInterfaceEntry bridgeInterfaceEntry : bridgeInterfaceEntries) {
                String portName = bridgeInterfaceEntry.getInterfaceName();
                InterfaceKey interfaceKey = new InterfaceKey(portName);
                Interface iface = InterfaceManagerCommonUtils.getInterfaceFromConfigDS(interfaceKey, dataBroker);
                if (iface != null) {
                    IfTunnel ifTunnel = iface.getAugmentation(IfTunnel.class);
                    if (ifTunnel != null) {
                        removeTerminationEndPoint(futures, dataBroker, bridgeIid, iface.getName());
                    }
                } else {
                    LOG.debug("Interface {} not found in config DS", portName);
                }
            }
        }
        futures.add(writeTransaction.submit());
    }

    private static void addVlanPortToBridge(InstanceIdentifier<?> bridgeIid, IfL2vlan ifL2vlan, IfTunnel ifTunnel,
            OvsdbBridgeAugmentation bridgeAugmentation, String bridgeName, String portName, DataBroker dataBroker,
            WriteTransaction t) {
        if (ifL2vlan.getVlanId() != null) {
            addTerminationPoint(bridgeIid, portName, ifL2vlan.getVlanId().getValue(),
                    null, null, ifTunnel);
        }
    }

    private static void addTunnelPortToBridge(IfTunnel ifTunnel, InstanceIdentifier<?> bridgeIid, Interface iface,
            String portName, DataBroker dataBroker) {
        LOG.debug("adding tunnel port {} to bridge {}", portName, bridgeIid);

        Class<? extends InterfaceTypeBase> type = TUNNEL_TYPE_MAP.get(ifTunnel.getTunnelInterfaceType());

        if (type == null) {
            LOG.warn("Unknown Tunnel Type obtained while creating interface: {}", iface);
            return;
        }

        int vlanId = 0;
        IfL2vlan ifL2vlan = iface.getAugmentation(IfL2vlan.class);
        if (ifL2vlan != null && ifL2vlan.getVlanId() != null) {
            vlanId = ifL2vlan.getVlanId().getValue();
        }

        Map<String, String> options = Maps.newHashMap();

        // Options common to any kind of tunnel
        IpAddress localIp = ifTunnel.getTunnelSource();
        options.put(TUNNEL_OPTIONS_LOCAL_IP, localIp.getIpv4Address().getValue());

        IpAddress remoteIp = ifTunnel.getTunnelDestination();
        options.put(TUNNEL_OPTIONS_REMOTE_IP, remoteIp.getIpv4Address().getValue());

        // Specific options for each type of tunnel
        if (!ifTunnel.getTunnelInterfaceType().equals(TunnelTypeMplsOverGre.class)) {
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
            // VxLAN-GPE interfaces will not use the default UDP port to avoid problems with other meshes
            options.put(TUNNEL_OPTIONS_DESTINATION_PORT, TUNNEL_OPTIONS_VALUE_GPE_DESTINATION_PORT);
        }
        addTerminationPoint(bridgeIid, portName, vlanId, type, options, ifTunnel);
    }

    // Update is allowed only for tunnel monitoring attributes
    public static void updateBfdParamtersForTerminationPoint(InstanceIdentifier<?> bridgeIid, IfTunnel ifTunnel,
            String portName, WriteTransaction transaction) {
        InstanceIdentifier<TerminationPoint> tpIid = createTerminationPointInstanceIdentifier(
                InstanceIdentifier.keyOf(bridgeIid.firstIdentifierOf(Node.class)), portName);
        LOG.debug("update bfd parameters for interface {}", tpIid);
        OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();
        List<InterfaceBfd> bfdParams = getBfdParams(ifTunnel);
        tpAugmentationBuilder.setInterfaceBfd(bfdParams);

        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.setKey(InstanceIdentifier.keyOf(tpIid));
        tpBuilder.addAugmentation(OvsdbTerminationPointAugmentation.class, tpAugmentationBuilder.build());

        transaction.merge(LogicalDatastoreType.CONFIGURATION, tpIid, tpBuilder.build(), true);
    }

    private static void addTerminationPoint(InstanceIdentifier<?> bridgeIid, String portName, int vlanId,
                                            Class<? extends InterfaceTypeBase> type, Map<String, String> options, IfTunnel ifTunnel) {
        InstanceIdentifier<TerminationPoint> tpIid = createTerminationPointInstanceIdentifier(
                InstanceIdentifier.keyOf(bridgeIid.firstIdentifierOf(Node.class)), portName);
        OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();

        tpAugmentationBuilder.setName(portName);

        if (type != null) {
            tpAugmentationBuilder.setInterfaceType(type);
        }

        if (options != null) {
            List<Options> optionsList = new ArrayList<>();
            for (Map.Entry<String, String> entry : options.entrySet()) {
                OptionsBuilder optionsBuilder = new OptionsBuilder();
                optionsBuilder.setKey(new OptionsKey(entry.getKey()));
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

        if (bfdMonitoringEnabled(ifTunnel)) {
            List<InterfaceBfd> bfdParams = getBfdParams(ifTunnel);
            tpAugmentationBuilder.setInterfaceBfd(bfdParams);
        }

        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.setKey(InstanceIdentifier.keyOf(tpIid));
        tpBuilder.addAugmentation(OvsdbTerminationPointAugmentation.class, tpAugmentationBuilder.build());

        BatchingUtils.write(tpIid, tpBuilder.build(), BatchingUtils.EntityType.TOPOLOGY_CONFIG);

    }

    private static List<InterfaceBfd> getBfdParams(IfTunnel ifTunnel) {
        List<InterfaceBfd> bfdParams = new ArrayList<>();
        bfdParams.add(getIfBfdObj(BFD_PARAM_ENABLE,ifTunnel != null ? ifTunnel.isMonitorEnabled().toString() :"false"));
        bfdParams.add(getIfBfdObj(BFD_PARAM_MIN_TX, ifTunnel != null &&  ifTunnel.getMonitorInterval() != null ?
                ifTunnel.getMonitorInterval().toString() : BFD_MIN_TX_VAL));
        return bfdParams;
    }

    private static InterfaceBfd getIfBfdObj(String key, String value) {
        InterfaceBfdBuilder bfdBuilder = new InterfaceBfdBuilder();
        bfdBuilder.setBfdKey(key).setKey(new InterfaceBfdKey(key)).setBfdValue(value);
        return bfdBuilder.build();
    }

    public static InstanceIdentifier<TerminationPoint> createTerminationPointInstanceIdentifier(NodeKey nodekey,
            String portName) {
        InstanceIdentifier<TerminationPoint> terminationPointPath = InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(OVSDB_TOPOLOGY_ID)).child(Node.class, nodekey)
                .child(TerminationPoint.class, new TerminationPointKey(new TpId(portName)));

        LOG.debug("Termination point InstanceIdentifier generated : {}", terminationPointPath);
        return terminationPointPath;
    }

    public static void removeTerminationEndPoint(List<ListenableFuture<Void>> futures, DataBroker dataBroker,
            InstanceIdentifier<?> bridgeIid, String interfaceName) {
        LOG.debug("removing termination point for {}", interfaceName);
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        InstanceIdentifier<TerminationPoint> tpIid = SouthboundUtils.createTerminationPointInstanceIdentifier(
                InstanceIdentifier.keyOf(bridgeIid.firstIdentifierOf(Node.class)), interfaceName);
        transaction.delete(LogicalDatastoreType.CONFIGURATION, tpIid);
        futures.add(transaction.submit());
    }

    public static boolean bfdMonitoringEnabled(IfTunnel ifTunnel) {
        return ifTunnel.isMonitorEnabled()
                && TunnelMonitoringTypeBfd.class.isAssignableFrom(ifTunnel.getMonitorProtocol());
    }

    public static boolean isMonitorProtocolBfd(IfTunnel ifTunnel) {
        return TunnelMonitoringTypeBfd.class.isAssignableFrom(ifTunnel.getMonitorProtocol());
    }
}
