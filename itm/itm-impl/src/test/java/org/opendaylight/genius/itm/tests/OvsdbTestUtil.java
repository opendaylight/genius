/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.tests;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;

import org.opendaylight.ovsdb.utils.southbound.utils.SouthboundUtils;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchExternalIdsBuilder;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;


public class OvsdbTestUtil {
    /* methods */
    public static Node createNode(String tepIp, String tzName, DataBroker dataBroker)
        throws Exception {

        int port = ItmTestConstants.OVSDB_CONN_PORT;
        IpAddress ipAddress = new IpAddress( new Ipv4Address(ItmTestConstants.LOCALHOST_IP) );
        PortNumber portNumber = new PortNumber(port);

        final ConnectionInfo connectionInfo =
            new ConnectionInfoBuilder().setRemoteIp(ipAddress).setRemotePort(portNumber).build();
        final InstanceIdentifier<Node> iid = SouthboundUtils.createInstanceIdentifier(connectionInfo);

        // build Node using its builder class
        NodeBuilder nodeBuilder = new NodeBuilder();
        NodeId ovsdbNodeId = SouthboundUtils.createNodeId(connectionInfo.getRemoteIp(),
            connectionInfo.getRemotePort());
        nodeBuilder.setNodeId(ovsdbNodeId);

        // build OvsdbNodeAugmentation for Node
        OvsdbNodeAugmentationBuilder ovsdbNodeAugBuilder = new OvsdbNodeAugmentationBuilder();
        ovsdbNodeAugBuilder.setConnectionInfo(connectionInfo);

        // create map of key-val pairs
        Map<String, String> externalIds = new HashMap<>();
        if (tepIp != null && !tepIp.isEmpty()) {
            externalIds.put(ItmTestConstants.EXTERNAL_ID_TEP_IP_KEY, tepIp);
        }

        if (tzName != null) {
            externalIds.put(ItmTestConstants.EXTERNAL_ID_TZNAME_KEY, tzName);
        }

        // get map-keys into set.
        Set<String> externalIdKeys = externalIds.keySet();

        List<OpenvswitchExternalIds> externalIdsList = new ArrayList<>();
        String externalIdValue = null;
        for (String externalIdKey : externalIdKeys) {
            externalIdValue = externalIds.get(externalIdKey);
            if (externalIdKey != null && externalIdValue != null) {
                externalIdsList.add(new OpenvswitchExternalIdsBuilder().setExternalIdKey(externalIdKey)
                    .setExternalIdValue(externalIdValue).build());
            }
        }

        // set ExternalIds list into Node
        ovsdbNodeAugBuilder.setOpenvswitchExternalIds(externalIdsList);
        // add OvsdbNodeAugmentation into Node
        nodeBuilder.addAugmentation(OvsdbNodeAugmentation.class, ovsdbNodeAugBuilder.build());
        Node ovsdbNode = nodeBuilder.build();

        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.put(LogicalDatastoreType.OPERATIONAL, iid, ovsdbNode, true);
        transaction.submit();

        return ovsdbNode;
    }

    public static Node updateNode(String tepIp, String tzName, String dpnBrName, DataBroker dataBroker)
        throws Exception {

        int port = ItmTestConstants.OVSDB_CONN_PORT;
        IpAddress ipAddress = new IpAddress( new Ipv4Address(ItmTestConstants.LOCALHOST_IP) );
        PortNumber portNumber = new PortNumber(port);

        final ConnectionInfo connectionInfo =
            new ConnectionInfoBuilder().setRemoteIp(ipAddress).setRemotePort(portNumber).build();
        final InstanceIdentifier<Node> iid = SouthboundUtils.createInstanceIdentifier(connectionInfo);

        Node oldOvsdbNode = dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.OPERATIONAL, iid).checkedGet().get();

        // build Node using its builder class
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(oldOvsdbNode.getNodeId());

        // build OvsdbNodeAugmentation for Node
        OvsdbNodeAugmentationBuilder ovsdbNodeAugBuilder = new OvsdbNodeAugmentationBuilder();
        ovsdbNodeAugBuilder.setConnectionInfo(connectionInfo);

        // create map of key-val pairs
        Map<String, String> externalIds = new HashMap<>();
        if (tepIp != null && !tepIp.isEmpty()) {
            externalIds.put(ItmTestConstants.EXTERNAL_ID_TEP_IP_KEY, tepIp);
        }

        if (tzName != null) {
            externalIds.put(ItmTestConstants.EXTERNAL_ID_TZNAME_KEY, tzName);
        }

        if (dpnBrName != null && !dpnBrName.isEmpty()) {
            externalIds.put(ItmTestConstants.EXTERNAL_ID_DPN_BR_NAME_KEY, dpnBrName);
        }

        // get map-keys into set.
        Set<String> externalIdKeys = externalIds.keySet();

        List<OpenvswitchExternalIds> externalIdsList = new ArrayList<>();
        String externalIdValue = null;
        for (String externalIdKey : externalIdKeys) {
            externalIdValue = externalIds.get(externalIdKey);
            if (externalIdKey != null && externalIdValue != null) {
                externalIdsList.add(new OpenvswitchExternalIdsBuilder().setExternalIdKey(externalIdKey)
                    .setExternalIdValue(externalIdValue).build());
            }
        }

        // set ExternalIds list into Node
        ovsdbNodeAugBuilder.setOpenvswitchExternalIds(externalIdsList);
        // add OvsdbNodeAugmentation into Node
        nodeBuilder.addAugmentation(OvsdbNodeAugmentation.class, ovsdbNodeAugBuilder.build());
        Node ovsdbNode = nodeBuilder.build();

        ReadWriteTransaction transaction = dataBroker.newReadWriteTransaction();
        transaction.put(LogicalDatastoreType.OPERATIONAL, iid, ovsdbNode, true);
        transaction.submit();

        return ovsdbNode;
    }

    public static void addBridgeIntoNode(Node ovsdbNode, String bridgeName, String dpid,
        DataBroker dataBroker) throws Exception {
        NodeBuilder bridgeNodeBuilder = new NodeBuilder();

        InstanceIdentifier<Node> bridgeIid = SouthboundUtils.createInstanceIdentifier(ovsdbNode.getKey(), bridgeName);

        NodeId bridgeNodeId = SouthboundUtils.createManagedNodeId(bridgeIid);
        bridgeNodeBuilder.setNodeId(bridgeNodeId);
        OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        ovsdbBridgeAugmentationBuilder.setBridgeName(new OvsdbBridgeName(bridgeName)).
            setDatapathId(new DatapathId(dpid));
        ovsdbBridgeAugmentationBuilder.setManagedBy(new OvsdbNodeRef(
            SouthboundUtils.createInstanceIdentifier(ovsdbNode.getKey().getNodeId())));

        bridgeNodeBuilder.addAugmentation(OvsdbBridgeAugmentation.class, ovsdbBridgeAugmentationBuilder.build());

        Node bridgeNode = bridgeNodeBuilder.build();

        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.merge(LogicalDatastoreType.OPERATIONAL, bridgeIid,
            bridgeNode, true);
        tx.submit();
    }
}
