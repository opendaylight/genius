/*
 * Copyright (c) 2015, 2017 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.interfacemanager.test;

import static org.opendaylight.mdsal.common.api.LogicalDatastoreType.OPERATIONAL;

import com.google.common.collect.ImmutableBiMap;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeDpdk;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeDpdkr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeDpdkvhost;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeDpdkvhostuser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeGeneve;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeGre64;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeInternal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeIpsecGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeIpsecGre64;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeLisp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypePatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeSystem;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeTap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeVxlanGpe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceBfdStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceBfdStatusBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceExternalIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbSouthboundTestUtil {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbSouthboundTestUtil.class);
    private static final int OVSDB_UPDATE_TIMEOUT = 1000;
    public static final TopologyId OVSDB_TOPOLOGY_ID = new TopologyId(new Uri("ovsdb:1"));
    public static final String OVSDB_URI_PREFIX = "ovsdb";
    public static final String BRIDGE_URI_PREFIX = "bridge";

    public static final ImmutableBiMap<String, Class<? extends InterfaceTypeBase>>
        OVSDB_INTERFACE_TYPE_MAP = new ImmutableBiMap.Builder<String, Class<? extends InterfaceTypeBase>>()
            .put("internal", InterfaceTypeInternal.class).put("vxlan", InterfaceTypeVxlan.class)
            .put("vxlan-gpe", InterfaceTypeVxlanGpe.class).put("patch", InterfaceTypePatch.class)
            .put("system", InterfaceTypeSystem.class).put("tap", InterfaceTypeTap.class)
            .put("geneve", InterfaceTypeGeneve.class).put("gre", InterfaceTypeGre.class)
            .put("ipsec_gre", InterfaceTypeIpsecGre.class).put("gre64", InterfaceTypeGre64.class)
            .put("ipsec_gre64", InterfaceTypeIpsecGre64.class).put("lisp", InterfaceTypeLisp.class)
            .put("dpdk", InterfaceTypeDpdk.class).put("dpdkr", InterfaceTypeDpdkr.class)
            .put("dpdkvhost", InterfaceTypeDpdkvhost.class).put("dpdkvhostuser", InterfaceTypeDpdkvhostuser.class)
            .build();

    public static NodeId createNodeId(String ip, Integer port) {
        String uriString = OVSDB_URI_PREFIX + "://" + ip + ":" + port;
        Uri uri = new Uri(uriString);
        return new NodeId(uri);
    }

    public static void createBridge(DataBroker dataBroker) throws ExecutionException, InterruptedException {
        final OvsdbBridgeName ovsdbBridgeName = new OvsdbBridgeName("s2");
        final InstanceIdentifier<Node> bridgeIid = createInstanceIdentifier("192.168.56.101", 6640, ovsdbBridgeName);
        final InstanceIdentifier<OvsdbBridgeAugmentation> ovsdbBridgeIid = bridgeIid.builder()
                .augmentation(OvsdbBridgeAugmentation.class).build();
        final NodeId bridgeNodeId = createManagedNodeId(bridgeIid);
        final NodeBuilder bridgeCreateNodeBuilder = new NodeBuilder();
        bridgeCreateNodeBuilder.setNodeId(bridgeNodeId);
        OvsdbBridgeAugmentationBuilder bridgeCreateAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        bridgeCreateAugmentationBuilder.setBridgeName(ovsdbBridgeName)
                .setDatapathId(new DatapathId("00:00:00:00:00:00:00:01"));
        bridgeCreateNodeBuilder.addAugmentation(OvsdbBridgeAugmentation.class, bridgeCreateAugmentationBuilder.build());
        LOG.debug("Built with the intent to store bridge data {}", bridgeCreateAugmentationBuilder.toString());
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.put(LogicalDatastoreType.OPERATIONAL, ovsdbBridgeIid, bridgeCreateAugmentationBuilder.build(), true);
        tx.commit().get();
    }

    public static void updateBridge(DataBroker dataBroker, String datapathId)
            throws ExecutionException, InterruptedException {
        final OvsdbBridgeName ovsdbBridgeName = new OvsdbBridgeName("s2");
        final InstanceIdentifier<Node> bridgeIid = createInstanceIdentifier("192.168.56.101", 6640, ovsdbBridgeName);
        final InstanceIdentifier<OvsdbBridgeAugmentation> ovsdbBridgeIid = bridgeIid.builder()
            .augmentation(OvsdbBridgeAugmentation.class).build();
        final NodeId bridgeNodeId = createManagedNodeId(bridgeIid);
        final NodeBuilder bridgeCreateNodeBuilder = new NodeBuilder();
        bridgeCreateNodeBuilder.setNodeId(bridgeNodeId);
        OvsdbBridgeAugmentationBuilder bridgeCreateAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        bridgeCreateAugmentationBuilder.setBridgeName(ovsdbBridgeName)
            .setDatapathId(new DatapathId(datapathId));
        bridgeCreateNodeBuilder.addAugmentation(OvsdbBridgeAugmentation.class, bridgeCreateAugmentationBuilder.build());
        LOG.debug("Built with the intent to store bridge data {}", bridgeCreateAugmentationBuilder.toString());
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.merge(LogicalDatastoreType.OPERATIONAL, ovsdbBridgeIid, bridgeCreateAugmentationBuilder.build(), true);
        tx.commit().get();
    }

    public static void deleteBridge(DataBroker dataBroker) throws ExecutionException, InterruptedException {
        final OvsdbBridgeName ovsdbBridgeName = new OvsdbBridgeName("s2");
        final InstanceIdentifier<Node> bridgeIid = createInstanceIdentifier("192.168.56.101", 6640, ovsdbBridgeName);
        final InstanceIdentifier<OvsdbBridgeAugmentation> ovsdbBridgeIid = bridgeIid.builder()
                .augmentation(OvsdbBridgeAugmentation.class).build();
        LOG.debug("Built with the intent to delete bridge data {}", bridgeIid.toString());
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.delete(LogicalDatastoreType.OPERATIONAL, ovsdbBridgeIid);
        tx.commit().get();
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(NodeId nodeId) {
        return InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(OVSDB_TOPOLOGY_ID)).child(Node.class, new NodeKey(nodeId));
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(String ip, Integer port) {
        InstanceIdentifier<Node> path = InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(OVSDB_TOPOLOGY_ID)).child(Node.class, createNodeKey(ip, port));
        LOG.debug("Created ovsdb path: {}", path);
        return path;
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(String remoteIp, Integer remotePort,
            OvsdbBridgeName bridgeName) {
        return createInstanceIdentifier(createManagedNodeId(remoteIp, remotePort, bridgeName));
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(NodeKey ovsdbNodeKey, String bridgeName) {
        return createInstanceIdentifier(createManagedNodeId(ovsdbNodeKey.getNodeId(), bridgeName));
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(String ip, Integer port, String bridgeName) {
        return createInstanceIdentifier(ip, port, new OvsdbBridgeName(bridgeName));
    }

    public static NodeId createManagedNodeId(String ip, Integer port, OvsdbBridgeName bridgeName) {
        return new NodeId(createNodeId(ip, port).getValue() + "/" + BRIDGE_URI_PREFIX + "/" + bridgeName.getValue());
    }

    public static NodeId createManagedNodeId(InstanceIdentifier<Node> iid) {
        NodeKey nodeKey = iid.firstKeyOf(Node.class);
        return nodeKey.getNodeId();
    }

    public static NodeId createManagedNodeId(NodeId ovsdbNodeId, String bridgeName) {
        return new NodeId(ovsdbNodeId.getValue() + "/" + BRIDGE_URI_PREFIX + "/" + bridgeName);
    }

    public static InstanceIdentifier<TerminationPoint> createTerminationPointInstanceIdentifier(NodeKey nodeKey, String
        portName) {
        InstanceIdentifier<TerminationPoint> terminationPointPath = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(OVSDB_TOPOLOGY_ID))
                .child(Node.class,nodeKey)
                .child(TerminationPoint.class, new TerminationPointKey(new TpId(portName)));

        LOG.debug("Termination point InstanceIdentifier generated : {}", terminationPointPath);
        return terminationPointPath;
    }

    public static void createTerminationPoint(DataBroker dataBroker, String interfaceName,
                                              Class<? extends InterfaceTypeBase> type, String externalId) throws
            ExecutionException, InterruptedException {
        final OvsdbBridgeName ovsdbBridgeName = new OvsdbBridgeName("s2");
        final InstanceIdentifier<Node> bridgeIid =
            createInstanceIdentifier("192.168.56.101", 6640,  ovsdbBridgeName);
        InstanceIdentifier<TerminationPoint> tpId = createTerminationPointInstanceIdentifier(
                InstanceIdentifier.keyOf(bridgeIid.firstIdentifierOf(Node.class)), interfaceName);
        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.withKey(InstanceIdentifier.keyOf(tpId));
        OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();

        tpAugmentationBuilder.setName(interfaceName);
        if (type != null) {
            tpAugmentationBuilder.setInterfaceType(type);
        }
        if (externalId != null) {
            List<InterfaceExternalIds> interfaceExternalIds = new ArrayList<>();
            InterfaceExternalIds interfaceExternalIds1 = new InterfaceExternalIdsBuilder().setExternalIdKey("iface-id")
                .setExternalIdValue(externalId).build();
            interfaceExternalIds.add(interfaceExternalIds1);
            tpAugmentationBuilder.setInterfaceExternalIds(interfaceExternalIds);
        }
        tpBuilder.addAugmentation(OvsdbTerminationPointAugmentation.class, tpAugmentationBuilder.build());
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.put(OPERATIONAL, tpId, tpBuilder.build(), true);
        tx.commit().get();
    }

    public static void updateTerminationPoint(DataBroker dataBroker, String interfaceName,
                                              Class<? extends InterfaceTypeBase> type) throws
            ExecutionException, InterruptedException {
        final OvsdbBridgeName ovsdbBridgeName = new OvsdbBridgeName("s2");
        final InstanceIdentifier<Node> bridgeIid =
            createInstanceIdentifier("192.168.56.101", 6640,  ovsdbBridgeName);
        InstanceIdentifier<TerminationPoint> tpId = createTerminationPointInstanceIdentifier(
            InstanceIdentifier.keyOf(bridgeIid.firstIdentifierOf(Node.class)), interfaceName);
        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.withKey(InstanceIdentifier.keyOf(tpId));
        OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();

        tpAugmentationBuilder.setName(interfaceName);
        if (type != null) {
            tpAugmentationBuilder.setInterfaceType(type);
        }
        List<InterfaceBfdStatus> interfaceBfdStatuses = Arrays.asList(new InterfaceBfdStatusBuilder()
            .setBfdStatusKey("state").setBfdStatusValue("down").build());

        tpAugmentationBuilder.setInterfaceBfdStatus(interfaceBfdStatuses);
        tpBuilder.addAugmentation(OvsdbTerminationPointAugmentation.class, tpAugmentationBuilder.build());
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.merge(OPERATIONAL, tpId, tpBuilder.build(), true);
        tx.commit().get();
    }

    public static NodeKey createNodeKey(String ip, Integer port) {
        return new NodeKey(createNodeId(ip, port));
    }

    public static ConnectionInfo getConnectionInfo(final String addressStr, final String portStr) {
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getByName(addressStr);
        } catch (UnknownHostException e) {
            LOG.warn("Could not allocate InetAddress", e);
        }

        IpAddress address = createIpAddress(inetAddress);
        PortNumber port = new PortNumber(Integer.parseInt(portStr));

        LOG.info("connectionInfo: {}", new ConnectionInfoBuilder().setRemoteIp(address).setRemotePort(port).build());
        return new ConnectionInfoBuilder().setRemoteIp(address).setRemotePort(port).build();
    }

    public ConnectionInfo getConnectionInfo(Node ovsdbNode) {
        ConnectionInfo connectionInfo = null;
        OvsdbNodeAugmentation ovsdbNodeAugmentation = extractOvsdbNode(ovsdbNode);
        if (ovsdbNodeAugmentation != null) {
            connectionInfo = ovsdbNodeAugmentation.getConnectionInfo();
        }
        return connectionInfo;
    }

    public OvsdbNodeAugmentation extractOvsdbNode(Node node) {
        return node.augmentation(OvsdbNodeAugmentation.class);
    }

    public static IpAddress createIpAddress(InetAddress address) {
        IpAddress ip = null;
        if (address instanceof Inet4Address) {
            ip = createIpAddress((Inet4Address) address);
        } else if (address instanceof Inet6Address) {
            ip = createIpAddress((Inet6Address) address);
        }
        return ip;
    }

    public static IpAddress createIpAddress(Inet4Address address) {
        return IetfInetUtil.INSTANCE.ipAddressFor(address);
    }

    public static IpAddress createIpAddress(Inet6Address address) {
        Ipv6Address ipv6 = new Ipv6Address(address.getHostAddress());
        return new IpAddress(ipv6);
    }

    public static String connectionInfoToString(final ConnectionInfo connectionInfo) {
        return connectionInfo.getRemoteIp().stringValue() + ":"
                + connectionInfo.getRemotePort().getValue();
    }

    public boolean addOvsdbNode(final ConnectionInfo connectionInfo) {
        return addOvsdbNode(connectionInfo, OVSDB_UPDATE_TIMEOUT);
    }

    public boolean addOvsdbNode(final ConnectionInfo connectionInfo, long timeout) {
        boolean result = true;
        // mdsalUtils.put(LogicalDatastoreType.CONFIGURATION,
        // createInstanceIdentifier(connectionInfo),
        // createNode(connectionInfo));
        if (timeout != 0) {
            try {
                Thread.sleep(timeout);
            } catch (InterruptedException e) {
                LOG.warn("Interrupted while waiting after adding OVSDB node {}", connectionInfoToString(connectionInfo),
                        e);
            }
        }
        return result;
    }

    public Node getOvsdbNode(final ConnectionInfo connectionInfo) {
        // return mdsalUtils.read(LogicalDatastoreType.OPERATIONAL,
        // createInstanceIdentifier(connectionInfo));
        return null;
    }

    public boolean deleteOvsdbNode(final ConnectionInfo connectionInfo) {
        return deleteOvsdbNode(connectionInfo, OVSDB_UPDATE_TIMEOUT);
    }

    public boolean deleteOvsdbNode(final ConnectionInfo connectionInfo, long timeout) {
        boolean result = true;
        // mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION,
        // createInstanceIdentifier(connectionInfo));
        if (timeout != 0) {
            try {
                Thread.sleep(timeout);
            } catch (InterruptedException e) {
                LOG.warn("Interrupted while waiting after deleting OVSDB node {}",
                        connectionInfoToString(connectionInfo), e);
            }
        }
        return result;
    }

    public Node connectOvsdbNode(final ConnectionInfo connectionInfo) {
        return connectOvsdbNode(connectionInfo, OVSDB_UPDATE_TIMEOUT);
    }

    public Node connectOvsdbNode(final ConnectionInfo connectionInfo, long timeout) {
        addOvsdbNode(connectionInfo, timeout);
        Node node = getOvsdbNode(connectionInfo);
        LOG.info("Connected to {}", connectionInfoToString(connectionInfo));
        return node;
    }

    public boolean disconnectOvsdbNode(final ConnectionInfo connectionInfo) {
        return disconnectOvsdbNode(connectionInfo, OVSDB_UPDATE_TIMEOUT);
    }

    public boolean disconnectOvsdbNode(final ConnectionInfo connectionInfo, long timeout) {
        deleteOvsdbNode(connectionInfo, timeout);
        LOG.info("Disconnected from {}", connectionInfoToString(connectionInfo));
        return true;
    }

    public List<ControllerEntry> createControllerEntry(String controllerTarget) {
        List<ControllerEntry> controllerEntriesList = new ArrayList<>();
        controllerEntriesList.add(new ControllerEntryBuilder().setTarget(new Uri(controllerTarget)).build());
        return controllerEntriesList;
    }

    public OvsdbNodeAugmentation extractNodeAugmentation(Node node) {
        return node.augmentation(OvsdbNodeAugmentation.class);
    }

    public OvsdbBridgeAugmentation extractBridgeAugmentation(Node node) {
        if (node == null) {
            return null;
        }
        return node.augmentation(OvsdbBridgeAugmentation.class);
    }
}
