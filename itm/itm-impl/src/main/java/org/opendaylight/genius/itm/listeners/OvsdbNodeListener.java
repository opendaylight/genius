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
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncDataTreeChangeListenerBase;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.itm.confighelpers.OvsdbTepAddWorker;
import org.opendaylight.genius.itm.confighelpers.OvsdbTepRemoveWorker;
import org.opendaylight.genius.itm.commons.OvsdbExternalIdsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchExternalIds;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * This class listens for OvsdbNode creation/removal/update in Network Topology Operational DS.
 * This is used to handle add/update/remove of TEPs of switches into/from ITM.
 */
@Singleton
public class OvsdbNodeListener extends AsyncDataTreeChangeListenerBase<Node, OvsdbNodeListener>
    implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbNodeListener.class);
    private DataBroker dataBroker;

    @Inject
    public OvsdbNodeListener(final DataBroker dataBroker) {
        super(Node.class, OvsdbNodeListener.class);
        this.dataBroker = dataBroker;
        LOG.trace("OvsdbNodeListener Created");
    }

    @PostConstruct
    public void start() throws Exception {
        registerListener(this.dataBroker);
        LOG.info("OvsdbNodeListener Started");
    }

    @Override
    @PreDestroy
    public void close() throws Exception {
        LOG.trace("OvsdbNodeListener Closed");
    }

    private void registerListener(final DataBroker db) {
        try {
            registerListener(LogicalDatastoreType.OPERATIONAL, db);
        } catch (final Exception e) {
            LOG.error("Network Topology Node listener registration failed.", e);
            throw new IllegalStateException("Network Topology Node listener registration failed.", e);
        }
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
        String bridgeName = null, strDpnId = "";
        OvsdbNodeAugmentation ovsdbNewNodeAugmentation = null;

        LOG.trace("OvsdbNodeListener called for Ovsdb Node ({}) Add.",
            ovsdbNodeNew.getNodeId().getValue());

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
            if (ovsdbNodeFromBridge != null) {
                ovsdbNewNodeAugmentation = ovsdbNodeFromBridge.getAugmentation(OvsdbNodeAugmentation.class);
            } else {
                LOG.error("Ovsdb Node could not be fetched from Oper DS for bridge {}.",
                    bridgeName);
                return;
            }
        }

        if (ovsdbNewNodeAugmentation != null) {
            // get OVSDB external_ids list from old ovsdb node
            OvsdbExternalIdsInfo ovsdbExternalIdsInfo = getOvsdbNodeExternalIds(ovsdbNewNodeAugmentation);
            if (ovsdbExternalIdsInfo == null) {
                return;
            }
            // store ExternalIds required parameters
            String newTepIp = ovsdbExternalIdsInfo.getTepIp();
            String tzName = ovsdbExternalIdsInfo.getTzName();
            String dpnBridgeName = ovsdbExternalIdsInfo.getDpnBrName();

            // check if TEP-IP is configured or not
            if (newTepIp != null && !newTepIp.isEmpty()) {
                // if bridge received is the one configured for TEPs from OVS side or
                // if it is br-int, then add TEP into Config DS
                if (dpnBridgeName.equals(bridgeName)) {
                    LOG.trace("Ovs Node [{}] is configured with TEP-IP.",
                        ovsdbNodeNew.getNodeId().getValue());
                    LOG.trace("TEP-IP: {}, TZ name: {}, DPN Bridge Name: {}, Bridge DPID: {}",
                        newTepIp, tzName, dpnBridgeName, strDpnId);

                    // Enqueue 'add TEP received from southbound OVSDB into ITM config DS' operation
                    // into DataStoreJobCoordinator
                    DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
                    OvsdbTepAddWorker addWorker =
                        new OvsdbTepAddWorker(newTepIp, strDpnId, tzName, dataBroker);
                    coordinator.enqueueJob(newTepIp, addWorker);
                }
            } else {
                LOG.trace("Ovs Node [{}] is not configured with TEP-IP. Nothing to do.",
                    ovsdbNodeNew.getNodeId().getValue());
                return;
            }
        }
    }

    @Override protected void update(InstanceIdentifier<Node> identifier, Node ovsdbNodeOld,
        Node ovsdbNodeNew) {
        String newTepIp = "", oldTepIp = "";
        String tzName = "", oldTzName = "";
        String oldDpnBridgeName = "", newDpnBridgeName = "";
        boolean isExternalIdsUpdated = false, isExternalIdsDeleted = false;
        boolean isTepIpAdded = false, isTepIpRemoved = false, isTepIpUpdated = false;
        boolean isTzChanged = false, isDpnBrChanged = false;

        LOG.trace("OvsdbNodeListener called for Ovsdb Node ({}) Update.",
            ovsdbNodeOld.getNodeId().getValue());

        // get OVSDB external_ids list from old ovsdb node
        OvsdbExternalIdsInfo newExternalIdsInfoObj = getOvsdbNodeExternalIds(
            ovsdbNodeNew.getAugmentation(OvsdbNodeAugmentation.class));

        // get OVSDB external_ids list from new ovsdb node
        OvsdbExternalIdsInfo oldExternalIdsInfoObj = getOvsdbNodeExternalIds(
            ovsdbNodeOld.getAugmentation(OvsdbNodeAugmentation.class));

        if (oldExternalIdsInfoObj == null && newExternalIdsInfoObj == null) {
            LOG.trace("ExternalIds is not received in old and new Ovsdb Nodes.");
            return;
        }

        if (oldExternalIdsInfoObj != null && newExternalIdsInfoObj == null) {
            isExternalIdsDeleted = true;
            LOG.trace("ExternalIds is deleted from Ovsdb node: {}",
                ovsdbNodeOld.getNodeId().getValue());
        }

        // store ExternalIds required parameters
        if (newExternalIdsInfoObj != null) {
            newTepIp = newExternalIdsInfoObj.getTepIp();
            tzName = newExternalIdsInfoObj.getTzName();
            newDpnBridgeName = newExternalIdsInfoObj.getDpnBrName();

            // All map params have been read, now clear it up.
            newExternalIdsInfoObj = null;
        }

        if (oldExternalIdsInfoObj != null) {
            oldDpnBridgeName = oldExternalIdsInfoObj.getDpnBrName();
            oldTzName = oldExternalIdsInfoObj.getTzName();
            oldTepIp = oldExternalIdsInfoObj.getTepIp();

            // All map params have been read, now clear it up.
            oldExternalIdsInfoObj = null;
        }

        // handle case when TEP parameters are not configured from switch side
        if (newTepIp == null && oldTepIp == null) {
            LOG.trace("OtherConfig parameters are not specified in old and new Ovsdb Nodes.");
            return;
        }

        if (!isExternalIdsDeleted) {
            isTepIpRemoved = isTepIpRemoved(oldTepIp, newTepIp);
            isTepIpAdded = isTepIpAdded(oldTepIp, newTepIp);
            isTepIpUpdated = isTepIpUpdated(oldTepIp, newTepIp);

            if (isTepIpUpdated) {
                LOG.info("TEP-IP cannot be updated. First delete the TEP and then add again.");
                return;
            }

            if (isTepIpAdded || isTepIpRemoved) {
                isExternalIdsUpdated = true;
            }
            if (!oldTzName.equals(tzName)) {
                isExternalIdsUpdated = true;
                if (oldTepIp != null && newTepIp != null) {
                    isTzChanged = true;
                    LOG.trace("tzname is changed from {} to {} for TEP-IP: {}", oldTzName, tzName, newTepIp);
                }
            }
            if (!oldDpnBridgeName.equals(newDpnBridgeName)) {
                isExternalIdsUpdated = true;
                if (oldTepIp != null && newTepIp != null) {
                    isDpnBrChanged = true;
                    LOG.trace("dpn-br-name is changed from {} to {} for TEP-IP: {}", oldDpnBridgeName, newDpnBridgeName, newTepIp);
                }
            }

            if (!isExternalIdsUpdated) {
                LOG.trace("No updates in the ExternalIds parameters. Nothing to do.");
                return;
            }
        }

        String strOldDpnId = "", strNewDpnId = "";
        // handle TEP-remove in remove case, TZ change case, Bridge change case
        if (isExternalIdsDeleted || isTepIpRemoved || isTzChanged || isDpnBrChanged) {
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
    }

    public boolean isTepIpRemoved(String oldTepIp, String newTepIp) {
        if (oldTepIp != null && newTepIp == null) {
            return true;
        }
        return false;
    }

    public boolean isTepIpAdded(String oldTepIp, String newTepIp) {
        if (oldTepIp == null && newTepIp != null) {
            return true;
        }
        return false;
    }

    public boolean isTepIpUpdated(String oldTepIp, String newTepIp) {
        if (oldTepIp != null && newTepIp != null && !oldTepIp.equals(newTepIp)) {
            return true;
        }
        return false;
    }

    public OvsdbExternalIdsInfo getOvsdbNodeExternalIds(OvsdbNodeAugmentation ovsdbNodeAugmentation) {
        if (ovsdbNodeAugmentation == null) {
            return null;
        }

        List<OpenvswitchExternalIds> ovsdbNodeExternalIdsList =
            ovsdbNodeAugmentation.getOpenvswitchExternalIds();
        if (ovsdbNodeExternalIdsList == null) {
            LOG.error("ExternalIds list does not exist in the OVSDB Node Augmentation.");
            return null;
        }

        OvsdbExternalIdsInfo externalIdsInfoObj = new OvsdbExternalIdsInfo();

        if (externalIdsInfoObj == null) {
            LOG.error("Memory could not be allocated. System fatal error.");
            return null;
        }

        if (ovsdbNodeExternalIdsList != null) {
            for (OpenvswitchExternalIds externalId : ovsdbNodeExternalIdsList) {
                if (externalId.getExternalIdKey().equals("tep-ip")) {
                    String tepIp = externalId.getExternalIdValue();
                    externalIdsInfoObj.setTepIp(tepIp);
                } else if (externalId.getExternalIdKey().equals("tzname")) {
                    String tzName = externalId.getExternalIdValue();
                    externalIdsInfoObj.setTzName(tzName);
                } else if (externalId.getExternalIdKey().equals("dpn-br-name")) {
                    String dpnBridgeName = externalId.getExternalIdValue();
                    externalIdsInfoObj.setDpnBrName(dpnBridgeName);
                }
            }
            LOG.trace("{}", externalIdsInfoObj.toString());
        }
        return externalIdsInfoObj;
    }
    // End of class
}
