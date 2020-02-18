/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.tests;

import com.google.common.util.concurrent.FluentFuture;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.ovsdb.utils.southbound.utils.SouthboundUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchExternalIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchOtherConfigsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class OvsdbTestUtil {
    private OvsdbTestUtil() {

    }

    /* methods */
    public static ConnectionInfo getConnectionInfo(int port, String strIpAddress) {
        IpAddress ipAddress = IpAddressBuilder.getDefaultInstance(strIpAddress);
        PortNumber portNumber = new PortNumber(port);

        ConnectionInfo connectionInfo =
            new ConnectionInfoBuilder().setRemoteIp(ipAddress).setRemotePort(portNumber).build();

        return connectionInfo;
    }

    public static FluentFuture<Void> createNode(
        ConnectionInfo connectionInfo, String tepIp, String tzName, DataBroker dataBroker)
        throws Exception {
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
        Map<String, String> otherConfigs = new HashMap<>();

        if (tepIp != null && !tepIp.isEmpty()) {
            otherConfigs.put(ItmTestConstants.OTHER_CFG_TEP_IP_KEY, tepIp);
        }

        if (tzName != null) {
            externalIds.put(ItmTestConstants.EXTERNAL_ID_TZNAME_KEY, tzName);
        }

        // get map-keys into set.
        Set<String> externalIdKeys = externalIds.keySet();
        Set<String> otherConfigKeys = otherConfigs.keySet();

        List<OpenvswitchExternalIds> externalIdsList = new ArrayList<>();
        String externalIdValue = null;
        for (String externalIdKey : externalIdKeys) {
            externalIdValue = externalIds.get(externalIdKey);
            if (externalIdKey != null && externalIdValue != null) {
                externalIdsList.add(new OpenvswitchExternalIdsBuilder().setExternalIdKey(externalIdKey)
                    .setExternalIdValue(externalIdValue).build());
            }
        }

        List<OpenvswitchOtherConfigs> otherConfigsList = new ArrayList<>();
        String otherConfigValue = null;
        for (String otherConfigKey : otherConfigKeys) {
            otherConfigValue = otherConfigs.get(otherConfigKey);
            if (otherConfigKey != null && otherConfigValue != null) {
                otherConfigsList.add(new OpenvswitchOtherConfigsBuilder().setOtherConfigKey(otherConfigKey)
                    .setOtherConfigValue(otherConfigValue).build());
            }
        }

        // set ExternalIds list into Node
        ovsdbNodeAugBuilder.setOpenvswitchExternalIds(externalIdsList);

        // set OtherConfig list into Node
        ovsdbNodeAugBuilder.setOpenvswitchOtherConfigs(otherConfigsList);

        // add OvsdbNodeAugmentation into Node
        nodeBuilder.addAugmentation(OvsdbNodeAugmentation.class, ovsdbNodeAugBuilder.build());
        Node ovsdbNode = nodeBuilder.build();

        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.put(LogicalDatastoreType.OPERATIONAL, iid, ovsdbNode, true);
        return transaction.commit();
    }

    public static FluentFuture<Void> updateNode(
        ConnectionInfo connectionInfo, String tepIp, String tzName, String brName,
        DataBroker dataBroker) throws Exception {
        final InstanceIdentifier<Node> iid = SouthboundUtils.createInstanceIdentifier(connectionInfo);

        Node oldOvsdbNode = dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.OPERATIONAL, iid).get();

        // build Node using its builder class
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(oldOvsdbNode.getNodeId());

        // build OvsdbNodeAugmentation for Node
        OvsdbNodeAugmentationBuilder ovsdbNodeAugBuilder = new OvsdbNodeAugmentationBuilder();
        ovsdbNodeAugBuilder.setConnectionInfo(connectionInfo);

        // create map of key-val pairs
        Map<String, String> externalIds = new HashMap<>();
        Map<String, String> otherConfigs = new HashMap<>();
        if (tepIp != null && !tepIp.isEmpty()) {
            otherConfigs.put(ItmTestConstants.OTHER_CFG_TEP_IP_KEY, tepIp);
        }

        if (tzName != null) {
            externalIds.put(ItmTestConstants.EXTERNAL_ID_TZNAME_KEY, tzName);
        }

        if (brName != null && !brName.isEmpty()) {
            externalIds.put(ItmTestConstants.EXTERNAL_ID_BR_NAME_KEY, brName);
        }

        // get map-keys into set.
        Set<String> externalIdKeys = externalIds.keySet();
        Set<String> otherConfigKeys = otherConfigs.keySet();

        List<OpenvswitchExternalIds> externalIdsList = new ArrayList<>();
        String externalIdValue = null;
        for (String externalIdKey : externalIdKeys) {
            externalIdValue = externalIds.get(externalIdKey);
            if (externalIdKey != null && externalIdValue != null) {
                externalIdsList.add(new OpenvswitchExternalIdsBuilder().setExternalIdKey(externalIdKey)
                    .setExternalIdValue(externalIdValue).build());
            }
        }

        List<OpenvswitchOtherConfigs> otherConfigsList = new ArrayList<>();
        String otherConfigsValue = null;
        for (String otherConfigKey : otherConfigKeys) {
            otherConfigsValue = otherConfigs.get(otherConfigKey);
            if (otherConfigKey != null && otherConfigsValue != null) {
                otherConfigsList.add(new OpenvswitchOtherConfigsBuilder().setOtherConfigKey(otherConfigKey)
                        .setOtherConfigValue(otherConfigsValue).build());
            }
        }

        // set ExternalIds list into Node
        ovsdbNodeAugBuilder.setOpenvswitchExternalIds(externalIdsList);

        // set OtherConfigs list into Node
        ovsdbNodeAugBuilder.setOpenvswitchOtherConfigs(otherConfigsList);

        // add OvsdbNodeAugmentation into Node
        nodeBuilder.addAugmentation(OvsdbNodeAugmentation.class, ovsdbNodeAugBuilder.build());
        Node ovsdbNode = nodeBuilder.build();

        //ReadWriteTransaction transaction = dataBroker.newReadWriteTransaction();
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.put(LogicalDatastoreType.OPERATIONAL, iid, ovsdbNode, true);
        return transaction.commit();
    }

    public static FluentFuture<Void> addBridgeIntoNode(
        ConnectionInfo connectionInfo, String bridgeName, String dpid, DataBroker dataBroker) throws Exception {
        NodeId ovsdbNodeId = SouthboundUtils.createNodeId(connectionInfo.getRemoteIp(),
            connectionInfo.getRemotePort());
        NodeKey nodeKey = new NodeKey(ovsdbNodeId);

        NodeBuilder bridgeNodeBuilder = new NodeBuilder();

        InstanceIdentifier<Node> bridgeIid = SouthboundUtils.createInstanceIdentifier(nodeKey, bridgeName);

        NodeId bridgeNodeId = SouthboundUtils.createManagedNodeId(bridgeIid);
        bridgeNodeBuilder.setNodeId(bridgeNodeId);
        OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        ovsdbBridgeAugmentationBuilder.setBridgeName(new OvsdbBridgeName(bridgeName))
            .setDatapathId(new DatapathId(dpid));
        ovsdbBridgeAugmentationBuilder.setManagedBy(new OvsdbNodeRef(
            SouthboundUtils.createInstanceIdentifier(nodeKey.getNodeId())));

        bridgeNodeBuilder.addAugmentation(OvsdbBridgeAugmentation.class, ovsdbBridgeAugmentationBuilder.build());

        Node bridgeNode = bridgeNodeBuilder.build();

        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.merge(LogicalDatastoreType.OPERATIONAL, bridgeIid,
            bridgeNode, true);
        return tx.commit();
    }
}
