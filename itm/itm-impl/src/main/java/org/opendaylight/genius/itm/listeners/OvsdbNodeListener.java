/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.listeners;

import java.util.List;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncDataTreeChangeListenerBase;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.itm.commons.OvsdbTepInfo;
import org.opendaylight.genius.itm.confighelpers.OvsdbTepAddWorker;
import org.opendaylight.genius.itm.confighelpers.OvsdbTepRemoveWorker;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchExternalIds;
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
@Singleton
public class OvsdbNodeListener extends AsyncDataTreeChangeListenerBase<Node, OvsdbNodeListener>
        implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(OvsdbNodeListener.class);

    private final DataBroker dataBroker;
    private final ItmConfig itmConfig;

    @Inject
    public OvsdbNodeListener(final DataBroker dataBroker, final ItmConfig itmConfig) {
        super(Node.class, OvsdbNodeListener.class);
        this.dataBroker = dataBroker;
        this.itmConfig = itmConfig;
        LOG.trace("OvsdbNodeListener Created");
    }

    @PostConstruct
    public void start() throws Exception {
        registerListener(this.dataBroker);
        LOG.info("OvsdbNodeListener Started");
    }

    @Override
    @PreDestroy
    public void close() {
        LOG.trace("OvsdbNodeListener Closed");
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
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

        String bridgeName = null;
        String strDpnId = "";
        OvsdbNodeAugmentation ovsdbNewNodeAugmentation = null;

        LOG.trace("OvsdbNodeListener called for Ovsdb Node ({}) Add.",
            ovsdbNodeNew.getNodeId().getValue());

        // check for OVS bridge node
        OvsdbBridgeAugmentation ovsdbNewBridgeAugmentation =
            ovsdbNodeNew.getAugmentation(OvsdbBridgeAugmentation.class);
        if (ovsdbNewBridgeAugmentation != null) {
            bridgeName = ovsdbNewBridgeAugmentation.getBridgeName().getValue();

            // Read DPID from OVSDBBridgeAugmentation
            strDpnId = ItmUtils.getStrDatapathId(ovsdbNewBridgeAugmentation);
            if (strDpnId == null || strDpnId.isEmpty()) {
                LOG.info("OvsdbBridgeAugmentation ADD: DPID for bridge {} is NULL.",
                    bridgeName);
                return;
            }

            // TBD: Move this time taking operations into DataStoreJobCoordinator
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
            // get OVSDB TEP info from old ovsdb node
            OvsdbTepInfo ovsdbTepInfo = getOvsdbTepInfo(ovsdbNewNodeAugmentation);
            // get OVSDB other_config list from old ovsdb node
            if (ovsdbTepInfo == null) {
                return;
            }
            // store TEP info required parameters
            String newLocalIp = ovsdbTepInfo.getLocalIp();
            String tzName = ovsdbTepInfo.getTzName();
            String newBridgeName = ovsdbTepInfo.getBrName();
            boolean ofTunnel = ovsdbTepInfo.getOfTunnel();

            // check if Local IP is configured or not
            if (newLocalIp != null && !newLocalIp.isEmpty()) {
                // if bridge received is the one configured for TEPs from OVS side or
                // if it is br-int, then add TEP into Config DS
                if (newBridgeName.equals(bridgeName)) {
                    LOG.trace("Ovs Node [{}] is configured with Local IP.", ovsdbNodeNew.getNodeId().getValue());

                    // check if defTzEnabled flag is false in config file,
                    // if flag is OFF, then no need to add TEP into ITM config DS.
                    if (tzName == null || tzName.equals(ITMConstants.DEFAULT_TRANSPORT_ZONE)) {
                        boolean defTzEnabled = itmConfig.isDefTzEnabled();
                        if (!defTzEnabled) {
                            LOG.info("TEP ({}) cannot be added into {} when def-tz-enabled flag is false.", newLocalIp,
                                     ITMConstants.DEFAULT_TRANSPORT_ZONE);
                            return;
                        }
                    }

                    LOG.trace("Local-IP: {}, TZ name: {}, Bridge Name: {}, Bridge DPID: {}," + "of-tunnel flag: {}",
                            newLocalIp, tzName, newBridgeName, strDpnId, ofTunnel);

                    // Enqueue 'add TEP received from southbound OVSDB into ITM config DS' operation
                    // into DataStoreJobCoordinator
                    DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
                    OvsdbTepAddWorker addWorker =
                        new OvsdbTepAddWorker(newLocalIp, strDpnId, tzName, ofTunnel, dataBroker);
                    coordinator.enqueueJob(newLocalIp, addWorker);
                } else {
                    LOG.trace("TEP ({}) would be added later when bridge ({}) gets added into Ovs Node [{}].",
                            newLocalIp, newBridgeName, ovsdbNodeNew.getNodeId().getValue());
                }
            } else {
                LOG.trace("Ovs Node [{}] is not configured with Local IP. Nothing to do.",
                          ovsdbNodeNew.getNodeId().getValue());
                return;
            }
        }
    }

    @Override protected void update(InstanceIdentifier<Node> identifier, Node ovsdbNodeOld,
        Node ovsdbNodeNew) {
        String newLocalIp = null;
        String oldLocalIp = null;
        String tzName = null;
        String oldTzName = null;
        String oldDpnBridgeName = null;
        String newDpnBridgeName = null;
        boolean newOfTunnel = false;
        boolean isTepInfoUpdated = false;
        boolean isTepInfoDeleted = false;
        boolean isLocalIpAdded = false;
        boolean isLocalIpRemoved = false;
        boolean isLocalIpUpdated;
        boolean isTzChanged = false;
        boolean isDpnBrChanged = false;

        LOG.trace("OvsdbNodeListener called for Ovsdb Node ({}) Update.",
            ovsdbNodeOld.getNodeId().getValue());

        // get OVSDB TEP info from old ovsdb node
        OvsdbTepInfo newTepInfoObj = getOvsdbTepInfo(
                ovsdbNodeNew.getAugmentation(OvsdbNodeAugmentation.class));

        // get OVSDB TEP info from new ovsdb node
        OvsdbTepInfo oldTepInfoObj = getOvsdbTepInfo(
                ovsdbNodeOld.getAugmentation(OvsdbNodeAugmentation.class));

        if (oldTepInfoObj == null && newTepInfoObj == null) {
            LOG.trace("Tep Info is not received in old and new Ovsdb Nodes.");
            return;
        }

        if (oldTepInfoObj != null && newTepInfoObj == null) {
            isTepInfoDeleted = true;
            LOG.trace("Tep Info is deleted from Ovsdb node: {}",
                    ovsdbNodeOld.getNodeId().getValue());
        }

        // store TEP info required parameters
        if (newTepInfoObj != null) {
            tzName = newTepInfoObj.getTzName();
            newLocalIp = newTepInfoObj.getLocalIp();
            newDpnBridgeName = newTepInfoObj.getBrName();
            newOfTunnel = newTepInfoObj.getOfTunnel();

            // All map params have been read, now clear it up.
            newTepInfoObj = null;
        }

        if (oldTepInfoObj != null) {
            oldLocalIp = oldTepInfoObj.getLocalIp();
            oldDpnBridgeName = oldTepInfoObj.getBrName();
            oldTzName = oldTepInfoObj.getTzName();

            // All map params have been read, now clear it up.
            oldTepInfoObj = null;

        }

        // handle case when TEP parameters are not configured from switch side
        if (newLocalIp == null && oldLocalIp == null) {
            LOG.trace("TEP info Local IP parameters are not specified in old and new Ovsdb Nodes.");
            return;
        }

        if (!isTepInfoDeleted) {
            isLocalIpRemoved = isLocalIpRemoved(oldLocalIp, newLocalIp);
            isLocalIpAdded = isLocalIpAdded(oldLocalIp, newLocalIp);
            isLocalIpUpdated = isLocalIpUpdated(oldLocalIp, newLocalIp);

            if (isLocalIpUpdated) {
                LOG.info("Local IP cannot be updated. First delete the Local IP and then add again.");
                return;
            }

            if (isLocalIpAdded || isLocalIpRemoved) {
                isTepInfoUpdated = true;
            }
            if (isTzUpdated(oldTzName, tzName)) {
                isTepInfoUpdated = true;
                if (oldLocalIp != null && newLocalIp != null) {
                    isTzChanged = true;
                    LOG.trace("tzname is changed from {} to {} for Local IP: {}", oldTzName, tzName, newLocalIp);
                }
            }
            if (isDpnUpdated(oldDpnBridgeName, newDpnBridgeName)) {
                isTepInfoUpdated = true;
                if (oldLocalIp != null && newLocalIp != null) {
                    isDpnBrChanged = true;
                    LOG.trace("dpn-br-name is changed from {} to {} for Local IP: {}", oldDpnBridgeName,
                            newDpnBridgeName, newLocalIp);
                }
            }

            if (!isTepInfoUpdated) {
                LOG.trace("No updates in the TEP Info parameters. Nothing to do.");
                return;
            }
        }

        String strOldDpnId = "";
        String strNewDpnId = "";
        // handle TEP-remove in remove case, TZ change case, Bridge change case
        if (isTepInfoDeleted || isLocalIpRemoved || isTzChanged || isDpnBrChanged) {
            // check if defTzEnabled flag is false in config file,
            // if flag is OFF, then no need to add TEP into ITM config DS.
            if (oldTzName == null || oldTzName.equals(ITMConstants.DEFAULT_TRANSPORT_ZONE)) {
                boolean defTzEnabled = itmConfig.isDefTzEnabled();
                if (!defTzEnabled) {
                    LOG.info("TEP ({}) cannot be removed from {} when def-tz-enabled flag is false.", oldLocalIp,
                             ITMConstants.DEFAULT_TRANSPORT_ZONE);
                    return;
                }
                oldTzName = ITMConstants.DEFAULT_TRANSPORT_ZONE;
            }
            // TBD: Move this time taking operations into DataStoreJobCoordinator
            strOldDpnId = ItmUtils.getBridgeDpid(ovsdbNodeNew, oldDpnBridgeName,
                dataBroker);
            if (strOldDpnId == null || strOldDpnId.isEmpty()) {
                LOG.error("TEP {} cannot be deleted. DPID for bridge {} is NULL.", oldLocalIp, oldDpnBridgeName);
                return;
            }
            // remove TEP
            LOG.trace("Update case: Removing TEP-IP: {}, TZ name: {}, Bridge Name: {}, Bridge DPID: {}", oldLocalIp,
                      oldTzName, oldDpnBridgeName, strOldDpnId);

            // Enqueue 'remove TEP from TZ' operation into DataStoreJobCoordinator
            DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
            OvsdbTepRemoveWorker
                removeWorker = new OvsdbTepRemoveWorker(oldLocalIp, strOldDpnId, oldTzName, dataBroker);
            coordinator.enqueueJob(oldLocalIp, removeWorker);
        }
        // handle TEP-add in add case, TZ change case, Bridge change case
        if (isLocalIpAdded || isTzChanged || isDpnBrChanged) {
            // check if defTzEnabled flag is false in config file,
            // if flag is OFF, then no need to add TEP into ITM config DS.
            if (tzName == null || tzName.equals(ITMConstants.DEFAULT_TRANSPORT_ZONE)) {
                boolean defTzEnabled = itmConfig.isDefTzEnabled();
                if (!defTzEnabled) {
                    LOG.info("TEP ({}) cannot be added into {} when def-tz-enabled flag is false.", newLocalIp,
                             ITMConstants.DEFAULT_TRANSPORT_ZONE);
                    return;
                }
                tzName = ITMConstants.DEFAULT_TRANSPORT_ZONE;
            }
            // TBD: Move this time taking operations into DataStoreJobCoordinator
            // get Datapath ID for bridge
            strNewDpnId = ItmUtils.getBridgeDpid(ovsdbNodeNew, newDpnBridgeName,
                dataBroker);
            if (strNewDpnId == null || strNewDpnId.isEmpty()) {
                LOG.error("TEP {} cannot be added. DPID for bridge {} is NULL.", newLocalIp, newDpnBridgeName);
                return;
            }
            LOG.trace(
                    "Update case: Adding TEP-IP: {}, TZ name: {}, Bridge Name: {}, Bridge DPID: {}," + "of-tunnel: {}",
                    newLocalIp, tzName, newDpnBridgeName, strNewDpnId, newOfTunnel);

            // Enqueue 'add TEP into new TZ' operation into DataStoreJobCoordinator
            DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
            OvsdbTepAddWorker
                addWorker = new OvsdbTepAddWorker(newLocalIp, strNewDpnId, tzName, newOfTunnel, dataBroker);
            coordinator.enqueueJob(newLocalIp, addWorker);
        }
    }

    public boolean isLocalIpRemoved(String oldTepIp, String newTepIp) {
        if (oldTepIp != null && newTepIp == null) {
            return true;
        }
        return false;
    }

    public boolean isLocalIpAdded(String oldTepIp, String newTepIp) {
        if (oldTepIp == null && newTepIp != null) {
            return true;
        }
        return false;
    }

    public boolean isLocalIpUpdated(String oldTepIp, String newTepIp) {
        if (oldTepIp != null && newTepIp != null && !oldTepIp.equals(newTepIp)) {
            return true;
        }
        return false;
    }

    public boolean isTzUpdated(String oldTzName, String tzName) {
        if (oldTzName == null && tzName != null) {
            return true;
        }
        if (oldTzName != null && tzName == null) {
            return true;
        }
        if (oldTzName != null && tzName != null && !oldTzName.equals(tzName)) {
            return true;
        }
        return false;
    }

    private boolean isDpnUpdated(String oldDpnBridgeName, String dpnBridgeName) {
        return oldDpnBridgeName == null && dpnBridgeName != null || oldDpnBridgeName != null && dpnBridgeName == null
                || oldDpnBridgeName != null && !oldDpnBridgeName.equals(dpnBridgeName);
    }

    private OvsdbTepInfo getOvsdbTepInfo(OvsdbNodeAugmentation ovsdbNodeAugmentation) {
        if (ovsdbNodeAugmentation == null) {
            return null;
        }

        List<OpenvswitchOtherConfigs> ovsdbNodeOtherConfigsList = ovsdbNodeAugmentation.getOpenvswitchOtherConfigs();
        if (ovsdbNodeOtherConfigsList == null) {
            //Local IP is not configured
            LOG.debug("OtherConfigs list does not exist in the OVSDB Node Augmentation.");
            return null;
        }

        List<OpenvswitchExternalIds> ovsdbNodeExternalIdsList = ovsdbNodeAugmentation.getOpenvswitchExternalIds();
        if (ovsdbNodeExternalIdsList == null) {
            LOG.debug("ExternalIds list does not exist in the OVSDB Node Augmentation.");
        }

        OvsdbTepInfo ovsdbTepInfoObj = new OvsdbTepInfo();

        for (OpenvswitchOtherConfigs otherConfigs : ovsdbNodeOtherConfigsList) {
            if (ITMConstants.OTH_CFG_TEP_PARAM_KEY_LOCAL_IP.equals(otherConfigs.getOtherConfigKey())) {
                String tepIp = otherConfigs.getOtherConfigValue();
                ovsdbTepInfoObj.setLocalIp(tepIp);
            }
        }

        for (OpenvswitchExternalIds externalId : ovsdbNodeExternalIdsList) {
            if (ITMConstants.EXT_ID_TEP_PARAM_KEY_TZNAME.equals(externalId.getExternalIdKey())) {
                String tzName = externalId.getExternalIdValue();
                ovsdbTepInfoObj.setTzName(tzName);
            } else if (ITMConstants.EXT_ID_TEP_PARAM_KEY_BR_NAME.equals(externalId.getExternalIdKey())) {
                String bridgeName = externalId.getExternalIdValue();
                ovsdbTepInfoObj.setBrName(bridgeName);
            } else if (ITMConstants.EXT_ID_TEP_PARAM_KEY_OF_TUNNEL.equals(externalId.getExternalIdKey())) {
                boolean ofTunnel = Boolean.parseBoolean(externalId.getExternalIdValue());
                ovsdbTepInfoObj.setOfTunnel(ofTunnel);
            }
        }

        LOG.trace("{}", ovsdbTepInfoObj.toString());
        return ovsdbTepInfoObj;
    }
    // End of class
}
