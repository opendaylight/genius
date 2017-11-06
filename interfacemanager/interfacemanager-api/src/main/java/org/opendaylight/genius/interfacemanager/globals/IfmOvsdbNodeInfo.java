/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.globals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchExternalIds;

public class IfmOvsdbNodeInfo extends IfmNodeInfo {

    private Map<String, String> externalIds;
    private final String ovsVersion;
    private final String dbVersion;
    private List<String> dpns; //TODO: Not used yet. May not need at all

    private static final String OVS_DEVICE_TYPE = "device_type";

    public IfmOvsdbNodeInfo(String nodeInfoId, OvsdbNodeAugmentation ovsdbNode) {
        super(nodeInfoId);
        this.ovsVersion = ovsdbNode.getOvsVersion();
        this.dbVersion = ovsdbNode.getDbVersion();
        this.dpns = new ArrayList<>();
        createExternalIds(ovsdbNode.getOpenvswitchExternalIds());
    }

    private void createExternalIds(List<OpenvswitchExternalIds> openvswitchExternalIds) {
        externalIds = new HashMap<>();
        for (OpenvswitchExternalIds externalId : openvswitchExternalIds) {
            externalIds.put(externalId.getExternalIdKey(), externalId.getExternalIdValue());
        }
    }

    public Map<String, String> getExternalIds() {
        return this.externalIds;
    }

    public String getOvsVersion() {
        return ovsVersion;
    }


    public String getDbVersion() {
        return dbVersion;
    }

    public List<String> getDpns() {
        return dpns;
    }

    public void addDpn(String dpnId) {
        dpns.add(dpnId);
    }

    public void removeDpn(String dpnId) {
        dpns.remove(dpnId);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("IfmOvsBridgeInfo{nodeInfoId=").append(this.getNodeInfoId())
            .append(", ovsVersion=").append(this.ovsVersion)
            .append(", dbVersion=").append(this.dbVersion)
            .append(", externalIds=[").append(externalIds.toString()).append("]")
            .append(", dpns=").append(this.dpns.toString());
        return sb.toString();
    }
}
