/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities;

import com.google.common.collect.ImmutableMap;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import javax.inject.Singleton;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlanGpe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.BridgeTunnelInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.OvsBridgeRefInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.bridge.tunnel.info.OvsBridgeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.bridge.tunnel.info.OvsBridgeEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.bridge.tunnel.info.ovs.bridge.entry.OvsBridgeTunnelEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.bridge.tunnel.info.ovs.bridge.entry.OvsBridgeTunnelEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.ovs.bridge.ref.info.OvsBridgeRefEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.ovs.bridge.ref.info.OvsBridgeRefEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelOperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceBfd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceBfdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceBfdKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class DirectTunnelUtils {

    private static final Logger LOG = LoggerFactory.getLogger(DirectTunnelUtils.class);

    private static final String BFD_PARAM_ENABLE = "enable";
    private static final String BFD_PARAM_MIN_TX = "min_tx";
    private static final String BFD_PARAM_FORWARDING_IF_RX = "forwarding_if_rx";
    // BFD parameters
    private static final String BFD_ENABLE_KEY = "enable";
    private static final String BFD_ENABLE_VALUE = "true";
    public static final String BFD_OP_STATE = "state";
    public static final String BFD_STATE_UP = "up";
    private static final String BFD_MIN_TX_VAL = "100";
    private static final String BFD_FORWARDING_IF_RX_VAL = "true";

    // Tunnel options
    public static final String TUNNEL_OPTIONS_KEY = "key";
    public static final String TUNNEL_OPTIONS_LOCAL_IP = "local_ip";
    public static final String TUNNEL_OPTIONS_REMOTE_IP = "remote_ip";
    public static final String TUNNEL_OPTIONS_DESTINATION_PORT = "dst_port";
    public static final String TUNNEL_OPTIONS_TOS = "tos";

    // Option values for VxLAN-GPE + NSH tunnels
    public static final String TUNNEL_OPTIONS_EXTS = "exts";
    public static final String TUNNEL_OPTIONS_NSI = "nsi";
    public static final String TUNNEL_OPTIONS_NSP = "nsp";
    public static final String TUNNEL_OPTIONS_NSHC1 = "nshc1";
    public static final String TUNNEL_OPTIONS_NSHC2 = "nshc2";
    public static final String TUNNEL_OPTIONS_NSHC3 = "nshc3";
    public static final String TUNNEL_OPTIONS_NSHC4 = "nshc4";

    // Option values for VxLAN-GPE + NSH tunnels
    public static final String TUNNEL_OPTIONS_VALUE_FLOW = "flow";
    public static final String TUNNEL_OPTIONS_VALUE_GPE = "gpe";
    // UDP port for VxLAN-GPE Tunnels
    public static final String TUNNEL_OPTIONS_VALUE_GPE_DESTINATION_PORT = "4880";

    //tos option value for tunnels
    public static final String TUNNEL_OPTIONS_TOS_VALUE_INHERIT = "inherit";

    private static final TopologyId OVSDB_TOPOLOGY_ID = new TopologyId(new Uri("ovsdb:1"));

    private static final long INVALID_ID = 0;
    // To keep the mapping between Tunnel Types and Tunnel Interfaces

    public static final ImmutableMap<Class<? extends TunnelTypeBase>,
            Class<? extends InterfaceTypeBase>> TUNNEL_TYPE_MAP = new ImmutableMap
            .Builder<Class<? extends TunnelTypeBase>, Class<? extends InterfaceTypeBase>>()
            .put(TunnelTypeGre.class, InterfaceTypeGre.class)
            .put(TunnelTypeVxlan.class, InterfaceTypeVxlan.class)
            .put(TunnelTypeVxlanGpe.class, InterfaceTypeVxlan.class).build();

    private static final String TUNNEL_PORT_REGEX = "tun[0-9a-f]{11}";
    private static final Pattern TUNNEL_PORT_PATTERN = Pattern.compile(TUNNEL_PORT_REGEX);
    public  static final Predicate<String> TUNNEL_PORT_PREDICATE =
        portName -> TUNNEL_PORT_PATTERN.matcher(portName).matches();

    public BigInteger getDpnId(DatapathId datapathId) {
        if (datapathId != null) {
            String dpIdStr = datapathId.getValue().replace(":", "");
            return new BigInteger(dpIdStr, 16);
        }
        return null;
    }

    public static BigInteger getDpnFromNodeConnectorId(NodeConnectorId portId) {
        /*
         * NodeConnectorId is of form 'openflow:dpnid:portnum'
         */
        return new BigInteger(portId.getValue().split(ITMConstants.OF_URI_SEPARATOR)[1]);
    }

    public static long getPortNumberFromNodeConnectorId(NodeConnectorId portId) {
        String portNo = getPortNoFromNodeConnectorId(portId);
        try {
            return Long.parseLong(portNo);
        } catch (NumberFormatException ex) {
            LOG.error("Unable to retrieve port number from nodeconnector id for {} ", portId, ex);
            return ITMConstants.INVALID_PORT_NO;
        }
    }

    private static String getPortNoFromNodeConnectorId(NodeConnectorId portId) {
        /*
         * NodeConnectorId is of form 'openflow:dpnid:portnum'
         */
        return portId.getValue().split(ITMConstants.OF_URI_SEPARATOR)[2];
    }

    // Convert Interface Oper State to Tunnel Oper state
    public static TunnelOperStatus convertInterfaceToTunnelOperState(Interface.OperStatus opState) {
        TunnelOperStatus tunnelOperStatus;
        switch (opState) {
            case Up:
                tunnelOperStatus = TunnelOperStatus.Up;
                break;
            case Down:
                tunnelOperStatus = TunnelOperStatus.Down;
                break;
            case Unknown:
                tunnelOperStatus = TunnelOperStatus.Unknown;
                break;
            default:
                tunnelOperStatus = TunnelOperStatus.Ignore;
        }
        return tunnelOperStatus;
    }

    public static InstanceIdentifier<OvsBridgeTunnelEntry> getBridgeTunnelEntryIdentifier(
            OvsBridgeEntryKey bridgeEntryKey, OvsBridgeTunnelEntryKey bridgeInterfaceEntryKey) {
        return InstanceIdentifier.builder(BridgeTunnelInfo.class)
                .child(OvsBridgeEntry.class, bridgeEntryKey)
                .child(OvsBridgeTunnelEntry.class, bridgeInterfaceEntryKey).build();

    }

    public static InstanceIdentifier<OvsBridgeRefEntry>
        getOvsBridgeRefEntryIdentifier(OvsBridgeRefEntryKey bridgeRefEntryKey) {
        return InstanceIdentifier.builder(OvsBridgeRefInfo.class)
                .child(OvsBridgeRefEntry.class, bridgeRefEntryKey).build();
    }

    public static InstanceIdentifier<OvsBridgeEntry> getOvsBridgeEntryIdentifier(OvsBridgeEntryKey bridgeEntryKey) {
        return InstanceIdentifier.builder(BridgeTunnelInfo.class).child(OvsBridgeEntry.class, bridgeEntryKey).build();
    }

    public static InstanceIdentifier<TerminationPoint> createTerminationPointInstanceIdentifier(
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021
                    .network.topology.topology.NodeKey nodekey, String portName) {
        InstanceIdentifier<TerminationPoint> terminationPointPath = InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(OVSDB_TOPOLOGY_ID))
                .child(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021
                        .network.topology.topology.Node.class, nodekey)
                .child(TerminationPoint.class, new TerminationPointKey(new TpId(portName)));

        LOG.debug("Termination point InstanceIdentifier generated : {}", terminationPointPath);
        return terminationPointPath;
    }

    public static List<InterfaceBfd> getBfdParams(IfTunnel ifTunnel) {
        List<InterfaceBfd> bfdParams = new ArrayList<>();
        bfdParams.add(getIfBfdObj(BFD_PARAM_ENABLE,ifTunnel != null ? ifTunnel.isMonitorEnabled().toString()
                : "false"));
        bfdParams.add(getIfBfdObj(BFD_PARAM_MIN_TX, ifTunnel != null &&  ifTunnel.getMonitorInterval() != null
                ? ifTunnel.getMonitorInterval().toString() : BFD_MIN_TX_VAL));
        bfdParams.add(getIfBfdObj(BFD_PARAM_FORWARDING_IF_RX, BFD_FORWARDING_IF_RX_VAL));
        return bfdParams;
    }

    private static InterfaceBfd getIfBfdObj(String key, String value) {
        InterfaceBfdBuilder bfdBuilder = new InterfaceBfdBuilder();
        bfdBuilder.setBfdKey(key).setKey(new InterfaceBfdKey(key)).setBfdValue(value);
        return bfdBuilder.build();
    }

    public static boolean bfdMonitoringEnabled(List<InterfaceBfd> interfaceBfds) {
        if (interfaceBfds != null && !interfaceBfds.isEmpty()) {
            for (InterfaceBfd interfaceBfd : interfaceBfds) {
                if (BFD_ENABLE_KEY.equalsIgnoreCase(interfaceBfd.getBfdKey())) {
                    return BFD_ENABLE_VALUE.equalsIgnoreCase(interfaceBfd.getBfdValue());
                }
            }
        }
        return false;
    }

    public static boolean changeInBfdMonitoringDetected(OvsdbTerminationPointAugmentation tpOld,
                                                        OvsdbTerminationPointAugmentation tpNew) {
        return tpOld != null
                && bfdMonitoringEnabled(tpNew.getInterfaceBfd()) != bfdMonitoringEnabled(tpOld.getInterfaceBfd());
    }

    public static boolean ifBfdStatusNotEqual(OvsdbTerminationPointAugmentation tpOld,
                                              OvsdbTerminationPointAugmentation tpNew) {
        return tpNew.getInterfaceBfdStatus() != null
                && (tpOld == null || !tpNew.getInterfaceBfdStatus().equals(tpOld.getInterfaceBfdStatus()));
    }
}