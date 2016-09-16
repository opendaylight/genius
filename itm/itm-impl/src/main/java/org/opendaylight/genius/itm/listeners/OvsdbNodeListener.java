/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.listeners;

import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.datastoreutils.AsyncDataTreeChangeListenerBase;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.itm.confighelpers.OvsdbTepAddWorker;
import org.opendaylight.genius.itm.confighelpers.OvsdbTepRemoveWorker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens for OvsdbNode creation/removal/update in Network Topology Operational DS.
 * This is used to handle add/update/remove of TEPs of switches into/from ITM.
 */
public class OvsdbNodeListener extends AsyncDataTreeChangeListenerBase<Node, OvsdbNodeListener>
    implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbNodeListener.class);
    private DataBroker dataBroker;

    public OvsdbNodeListener(final DataBroker dataBroker) {
        super(Node.class, OvsdbNodeListener.class);
        this.dataBroker = dataBroker;
        LOG.trace("OvsdbNodeListener Created");
    }

    @Override public void close() throws Exception {
        LOG.trace("OvsdbNodeListener Closed");
    }

    @Override protected InstanceIdentifier<Node> getWildCardPath() {
        return InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class)
            .child(Node.class).build();
    }

    @Override protected OvsdbNodeListener getDataTreeChangeListener() {
        return OvsdbNodeListener.this;
    }

    @Override protected void remove(InstanceIdentifier<Node> identifier, Node ovsdbNode) {
        LOG.trace("OvsdbNodeListener called for Ovsdb Node Remove.");
    }

    @Override protected void add(InstanceIdentifier<Node> identifier, Node ovsdbNodeNew) {
        String newTepIp = "", tzName = "", dpnBridgeName = "", bridgeName = "";
        String strDpnId = "";
        OvsdbNodeAugmentation ovsdbNewNodeAugmentation = null;

        LOG.trace("OvsdbNodeListener called for Ovsdb Node Add.");

        // check for OVS bridge node
        OvsdbBridgeAugmentation ovsdbNewBridgeAugmentation =
            ovsdbNodeNew.getAugmentation(OvsdbBridgeAugmentation.class);
        if (ovsdbNewBridgeAugmentation != null) {
            bridgeName = ovsdbNewBridgeAugmentation.getBridgeName().getValue();

            // Read DPN ID from OVSDBBridgeAugmentation
            strDpnId = ItmUtils.getStrDatapathId(ovsdbNodeNew);
            if (strDpnId == null || strDpnId.isEmpty()) {
                LOG.error("OvsdbBridgeAugmentation ADD: DPID for bridge {} is NULL.",
                    bridgeName);
                return;
            }

            Node ovsdbNodeFromBridge = ItmUtils.getOvsdbNode(ovsdbNewBridgeAugmentation, dataBroker);
            // check for OVSDB node
            ovsdbNewNodeAugmentation = ovsdbNodeFromBridge.getAugmentation(OvsdbNodeAugmentation.class);
        }

        if (ovsdbNewNodeAugmentation != null) {
            // get OVSDB other_configs list from old ovsdb node
            OvsdbOtherConfigInfo ovsdbOtherConfigObj = getOvsdbNodeOtherConfigs(ovsdbNodeNew);
            if (ovsdbOtherConfigObj == null) {
                return;
            }
            // store other config required parameters
            newTepIp = ovsdbOtherConfigObj.getTepIp();
            tzName = ovsdbOtherConfigObj.getTzName();
            dpnBridgeName = ovsdbOtherConfigObj.getDpnBrName();

            // if bridge received is the one configured for TEPs from OVS side or
            // if it is br-int, then add TEP into Config DS
            if (dpnBridgeName.equals(bridgeName)) {
                // check if TEP-IP is configured or not
                if (!newTepIp.isEmpty()) {
                    LOG.trace("Ovs Node [{}] is configured with TEP-IP.",
                        ovsdbNodeNew.getNodeId().getValue());
                } else {
                    LOG.trace("Ovs Node [{}] is not configured with TEP-IP. Nothing to do.",
                        ovsdbNodeNew.getNodeId().getValue());
                    return;
                }

                LOG.trace("TEP-IP: {}, TZ name: {}, DPN Bridge Name: {}, Bridge DPID: {}", newTepIp,
                    tzName, dpnBridgeName, strDpnId);

                // Enqueue 'add TEP received from southbound OVSDB into ITM config DS' operation
                // into DataStoreJobCoordinator
                DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
                OvsdbTepAddWorker addWorker =
                    new OvsdbTepAddWorker(newTepIp, strDpnId, tzName, dataBroker);
                coordinator.enqueueJob(newTepIp, addWorker);
            }
        }
    }

    @Override protected void update(InstanceIdentifier<Node> identifier, Node ovsdbNodeOld,
        Node ovsdbNodeNew) {
        String newTepIp = "", oldTepIp = "";
        String tzName = "", oldTzName = "";
        String oldDpnBridgeName = "", newDpnBridgeName = "";
        boolean isOtherConfigUpdated = false, isOtherConfigDeleted = false;
        boolean isTepIpAdded = false, isTepIpRemoved = false;
        boolean isTzChanged = false, isDpnBrChanged = false;

        LOG.trace("OvsdbNodeListener called for Ovsdb Node ({}) Update.",
            ovsdbNodeOld.getNodeId().getValue());

        // get OVSDB other_configs list from old ovsdb node
        OvsdbOtherConfigInfo newOtherConfigsInfoObj = getOvsdbNodeOtherConfigs(ovsdbNodeNew);

        // get OVSDB other_configs list from new ovsdb node
        OvsdbOtherConfigInfo oldOtherConfigInfoObj = getOvsdbNodeOtherConfigs(ovsdbNodeOld);

        if (oldOtherConfigInfoObj == null && newOtherConfigsInfoObj == null) {
            LOG.trace("OtherConfig is not received in old and new Ovsdb Nodes.");
            return;
        }

        if (oldOtherConfigInfoObj != null && newOtherConfigsInfoObj == null) {
            isOtherConfigDeleted = true;
            LOG.trace("OtherConfig is deleted from Ovsdb node: {}",
                ovsdbNodeOld.getNodeId().getValue());
        }

        // store other config required parameters
        if (newOtherConfigsInfoObj != null) {
            newTepIp = newOtherConfigsInfoObj.getTepIp();
            tzName = newOtherConfigsInfoObj.getTzName();
            newDpnBridgeName = newOtherConfigsInfoObj.getDpnBrName();

            // All map params have been read, now clear it up.
            newOtherConfigsInfoObj = null;
        }

        if (oldOtherConfigInfoObj != null) {
            oldDpnBridgeName = oldOtherConfigInfoObj.getDpnBrName();
            oldTzName = oldOtherConfigInfoObj.getTzName();
            oldTepIp = oldOtherConfigInfoObj.getTepIp();

            // All map params have been read, now clear it up.
            oldOtherConfigInfoObj = null;
        }

        if (!isOtherConfigDeleted) {
            isTepIpRemoved = isTepIpRemoved(oldTepIp, newTepIp);
            isTepIpAdded = isTepIpAdded(oldTepIp, newTepIp);

            if (!oldTepIp.equals(newTepIp)) {
                isOtherConfigUpdated = true;
            }
            if (!oldTzName.equals(tzName)) {
                isOtherConfigUpdated = true;
                if (!oldTepIp.isEmpty() && !newTepIp.isEmpty()) {
                    isTzChanged = true;
                    LOG.trace("tzname is changed from {} to {} for TEP-IP: {}", oldTzName, tzName, newTepIp);
                }
            }
            if (!oldDpnBridgeName.equals(newDpnBridgeName)) {
                isOtherConfigUpdated = true;
                if (!oldTepIp.isEmpty() && !newTepIp.isEmpty()) {
                    isDpnBrChanged = true;
                    LOG.trace("dpn-br-name is changed from {} to {} for TEP-IP: {}", oldDpnBridgeName, newDpnBridgeName, newTepIp);
                }
            }

            if (!isOtherConfigUpdated) {
                LOG.trace("No updates in the other config parameters. Nothing to do.");
                return;
            }
        }

        String strOldDpnId = "", strNewDpnId = "";
        // handle TEP-add in add case, TZ change case, Bridge change case
        if (isTepIpAdded || isTzChanged || isDpnBrChanged) {
            // get Datapath ID for bridge
            strNewDpnId = ItmUtils.getBridgeDpid(ovsdbNodeNew, newDpnBridgeName,
                dataBroker);
            if (strNewDpnId == null || strNewDpnId.isEmpty()) {
                LOG.error(
                    "TEP {} cannot be added. DPN-ID for bridge {} is NULL.",
                    newTepIp, newDpnBridgeName);
                return;
            }
            LOG.trace(
                "Update case: Adding TEP-IP: {}, TZ name: {}, DPN Bridge Name: {}, Bridge DPID: {}",
                newTepIp, tzName, newDpnBridgeName, strNewDpnId);

            // Enqueue 'add TEP into new TZ' operation into DataStoreJobCoordinator
            DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
            OvsdbTepAddWorker
                addWorker = new OvsdbTepAddWorker(newTepIp, strNewDpnId, tzName, dataBroker);
            coordinator.enqueueJob(newTepIp, addWorker);
        }

        // handle TEP-remove in remove case, TZ change case, Bridge change case
        if (isOtherConfigDeleted || isTepIpRemoved || isTzChanged || isDpnBrChanged) {
            strOldDpnId = ItmUtils.getBridgeDpid(ovsdbNodeNew, oldDpnBridgeName,
                dataBroker);
            if (strOldDpnId == null || strOldDpnId.isEmpty()) {
                LOG.error(
                    "TEP {} cannot be deleted. DPN-ID for bridge {} is NULL.",
                    oldTepIp, oldDpnBridgeName);
                return;
            }
            // remove TEP
            LOG.trace(
                "Update case: Removing TEP-IP: {}, TZ name: {}, DPN Bridge Name: {}, Bridge DPID: {}",
                oldTepIp, oldTzName, oldDpnBridgeName, strOldDpnId);

            // Enqueue 'remove TEP from TZ' operation into DataStoreJobCoordinator
            DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
            OvsdbTepRemoveWorker
                removeWorker = new OvsdbTepRemoveWorker(oldTepIp, strOldDpnId, oldTzName, dataBroker);
            coordinator.enqueueJob(oldTepIp, removeWorker);
        }
    }

    public boolean isTepIpRemoved(String oldTepIp, String newTepIp) {
        if (!oldTepIp.isEmpty() && newTepIp.isEmpty()) {
            return true;
        }
        return false;
    }

    public boolean isTepIpAdded(String oldTepIp, String newTepIp) {
        if (oldTepIp.isEmpty() && !newTepIp.isEmpty()) {
            return true;
        }
        return false;
    }

    public OvsdbOtherConfigInfo getOvsdbNodeOtherConfigs(Node ovsdbNode) {
        String tepIp = "", tzName = "", dpnBridgeName = "";

        OvsdbNodeAugmentation ovsdbNewNodeAugmentation =
            ovsdbNode.getAugmentation(OvsdbNodeAugmentation.class);
        if (ovsdbNewNodeAugmentation == null) {
            return null;
        }

        List<OpenvswitchOtherConfigs> ovsdbNodeOtherConfigsList =
            ovsdbNewNodeAugmentation.getOpenvswitchOtherConfigs();
        if (ovsdbNodeOtherConfigsList == null) {
            LOG.error("Other-configs list does not exist in the OVSDB node [{}].",
                ovsdbNode.getNodeId().getValue());
            return null;
        }

        OvsdbOtherConfigInfo otherConfigInfoObj = new OvsdbOtherConfigInfo();

        if (otherConfigInfoObj == null) {
            LOG.error("Memory could not be allocated. System fatal error.");
            return null;
        }

        if (ovsdbNodeOtherConfigsList != null) {
            for (OpenvswitchOtherConfigs otherConfig : ovsdbNodeOtherConfigsList) {
                if (otherConfig.getOtherConfigKey().equals("tep-ip")) {
                    tepIp = otherConfig.getOtherConfigValue();
                    otherConfigInfoObj.setTepIp(tepIp);
                } else if (otherConfig.getOtherConfigKey().equals("tzname")) {
                    tzName = otherConfig.getOtherConfigValue();
                    otherConfigInfoObj.setTzName(tzName);
                } else if (otherConfig.getOtherConfigKey().equals("dpn-br-name")) {
                    dpnBridgeName = otherConfig.getOtherConfigValue();
                    otherConfigInfoObj.setDpnBrName(dpnBridgeName);
                } else {
                    LOG.trace("other_config {}:{}", otherConfig.getOtherConfigKey(),
                        otherConfig.getOtherConfigValue());
                }
            }
            LOG.trace("{}", otherConfigInfoObj.toString());
        }
        return otherConfigInfoObj;
    }
    // End of class
}
