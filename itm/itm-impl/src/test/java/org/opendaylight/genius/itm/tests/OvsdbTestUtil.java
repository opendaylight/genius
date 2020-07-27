/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.tests;

import com.google.common.util.concurrent.FluentFuture;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.ovsdb.utils.southbound.utils.SouthboundUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchExternalIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchExternalIdsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchOtherConfigsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchOtherConfigsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint16;

public final class OvsdbTestUtil {
    private OvsdbTestUtil() {

    }

    /* methods */
    public static ConnectionInfo getConnectionInfo(Uint16 port, String strIpAddress) {
        IpAddress ipAddress = IpAddressBuilder.getDefaultInstance(strIpAddress);
        PortNumber portNumber = new PortNumber(port);

        ConnectionInfo connectionInfo =
            new ConnectionInfoBuilder().setRemoteIp(ipAddress).setRemotePort(portNumber).build();

        return connectionInfo;
    }

    public static FluentFuture<? extends @NonNull CommitInfo> createNode(
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

        Map<OpenvswitchExternalIdsKey, OpenvswitchExternalIds> externalIdsList = new HashMap<>();
        String externalIdValue = null;
        for (String externalIdKey : externalIdKeys) {
            externalIdValue = externalIds.get(externalIdKey);
            if (externalIdKey != null && externalIdValue != null) {
                externalIdsList.put(new OpenvswitchExternalIdsKey(externalIdKey),
                        new OpenvswitchExternalIdsBuilder().setExternalIdKey(externalIdKey)
                                .setExternalIdValue(externalIdValue).build());
            }
        }

        Map<OpenvswitchOtherConfigsKey, OpenvswitchOtherConfigs> otherConfigsList = new HashMap<>();
        String otherConfigValue = null;
        for (String otherConfigKey : otherConfigKeys) {
            otherConfigValue = otherConfigs.get(otherConfigKey);
            if (otherConfigKey != null && otherConfigValue != null) {
                otherConfigsList.put(new OpenvswitchOtherConfigsKey(otherConfigKey),
                        new OpenvswitchOtherConfigsBuilder().setOtherConfigKey(otherConfigKey)
                                .setOtherConfigValue(otherConfigValue).build());
            }
        }

        // set ExternalIds list into Node
        ovsdbNodeAugBuilder.setOpenvswitchExternalIds(externalIdsList);

        // set OtherConfig list into Node
        ovsdbNodeAugBuilder.setOpenvswitchOtherConfigs(otherConfigsList);

        // add OvsdbNodeAugmentation into Node
        nodeBuilder.addAugmentation(ovsdbNodeAugBuilder.build());
        Node ovsdbNode = nodeBuilder.build();

        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.mergeParentStructurePut(LogicalDatastoreType.OPERATIONAL, iid, ovsdbNode);
        return transaction.commit();
    }

    public static FluentFuture<? extends @NonNull CommitInfo> updateNode(
        ConnectionInfo connectionInfo, String tepIp, String tzName, String brName,
        DataBroker dataBroker) throws Exception {
        final InstanceIdentifier<Node> iid = SouthboundUtils.createInstanceIdentifier(connectionInfo);

        Node oldOvsdbNode = dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.OPERATIONAL, iid).get().get();

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

        Map<OpenvswitchExternalIdsKey, OpenvswitchExternalIds> externalIdsList = new HashMap<>();
        String externalIdValue = null;
        for (String externalIdKey : externalIdKeys) {
            externalIdValue = externalIds.get(externalIdKey);
            if (externalIdKey != null && externalIdValue != null) {
                externalIdsList.put(new OpenvswitchExternalIdsKey(externalIdKey),
                        new OpenvswitchExternalIdsBuilder().setExternalIdKey(externalIdKey)
                                .setExternalIdValue(externalIdValue).build());
            }
        }

        Map<OpenvswitchOtherConfigsKey, OpenvswitchOtherConfigs> otherConfigsList = new HashMap<>();
        String otherConfigsValue = null;
        for (String otherConfigKey : otherConfigKeys) {
            otherConfigsValue = otherConfigs.get(otherConfigKey);
            if (otherConfigKey != null && otherConfigsValue != null) {
                otherConfigsList.put(new OpenvswitchOtherConfigsKey(otherConfigKey),
                        new OpenvswitchOtherConfigsBuilder().setOtherConfigKey(otherConfigKey)
                                .setOtherConfigValue(otherConfigsValue).build());
            }
        }

        // set ExternalIds list into Node
        ovsdbNodeAugBuilder.setOpenvswitchExternalIds(externalIdsList);

        // set OtherConfigs list into Node
        ovsdbNodeAugBuilder.setOpenvswitchOtherConfigs(otherConfigsList);

        // add OvsdbNodeAugmentation into Node
        nodeBuilder.addAugmentation(ovsdbNodeAugBuilder.build());
        Node ovsdbNode = nodeBuilder.build();

        //ReadWriteTransaction transaction = dataBroker.newReadWriteTransaction();
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.mergeParentStructurePut(LogicalDatastoreType.OPERATIONAL, iid, ovsdbNode);
        return transaction.commit();
    }

    public static FluentFuture<? extends @NonNull CommitInfo> addBridgeIntoNode(
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

        bridgeNodeBuilder.addAugmentation(ovsdbBridgeAugmentationBuilder.build());

        Node bridgeNode = bridgeNodeBuilder.build();

        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.mergeParentStructureMerge(LogicalDatastoreType.OPERATIONAL, bridgeIid,
            bridgeNode);
        return tx.commit();
    }
}
