/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.globals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

public class IfmOvsdbBridgeInfo extends IfmNodeInfo {

    private final Uuid uuid;
    private final String name;
    private String parentNodeId;
    private Map<String, String> externalIds;
    private Map<String, String> otherConfigs;
    private DatapathId datapathId;

    public IfmOvsdbBridgeInfo(String nodeInfoId, OvsdbBridgeAugmentation ovsdbBridge) {
        super(nodeInfoId);
        this.uuid = ovsdbBridge.getBridgeUuid();
        this.name = ovsdbBridge.getBridgeName().getValue();
        this.datapathId = ovsdbBridge.getDatapathId();
        createOtherConfigs(ovsdbBridge.getBridgeOtherConfigs());
        createExternalIds(ovsdbBridge.getBridgeExternalIds());
        setParentNodeId(ovsdbBridge);
    }

    private void createExternalIds(List<BridgeExternalIds> bridgeExternalIds) {
        externalIds = new HashMap<>();
        if (bridgeExternalIds != null) {
            for (BridgeExternalIds externalId : bridgeExternalIds) {
                externalIds.put(externalId.getBridgeExternalIdKey(), externalId.getBridgeExternalIdValue());
            }
        }
    }

    public Map<String, String> getExternalIds() {
        return this.externalIds;
    }

    private void createOtherConfigs(List<BridgeOtherConfigs> bridgeOtherConfigs) {
        otherConfigs = new HashMap<>();
        if (bridgeOtherConfigs != null) {
            for (BridgeOtherConfigs otherConfig : bridgeOtherConfigs) {
                otherConfigs.put(otherConfig.getBridgeOtherConfigKey(), otherConfig.getBridgeOtherConfigValue());
            }
        }
    }

    public Map<String, String> getOtherConfigs() {
        return this.otherConfigs;
    }

    public void setParentNodeId(OvsdbBridgeAugmentation ovsdbBridge) {
        if (ovsdbBridge.getManagedBy() != null) {
            NodeId nodeId = ovsdbBridge.getManagedBy().getValue().firstKeyOf(Node.class).getNodeId();
            this.parentNodeId = nodeId.getValue();
        }
    }

    public String getParentNodeId() {
        return parentNodeId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("IfmOvsBridgeInfo{nodeInfoId=").append(this.getNodeInfoId())
            .append(", name=").append(this.name)
            .append(", uuid=").append(this.uuid.getValue())
            .append(", datapathId=").append(this.datapathId.getValue())
            .append(", parentNodeId=").append(this.parentNodeId)
            .append(", otherConfigs=[").append(this.otherConfigs.toString()).append("], ")
            .append(", externalIds=[").append(externalIds.toString()).append("]");
        return sb.toString();
    }
}
