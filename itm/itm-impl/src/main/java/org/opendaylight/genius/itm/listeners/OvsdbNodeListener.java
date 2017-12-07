/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.listeners;

import java.util.List;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.listeners.AbstractSyncDataTreeChangeListener;
import org.opendaylight.genius.itm.commons.OvsdbExternalIdsInfo;
import org.opendaylight.genius.itm.commons.OvsdbOtherConfigInfo;
import org.opendaylight.genius.itm.confighelpers.OvsdbTepAddWorker;
import org.opendaylight.genius.itm.confighelpers.OvsdbTepRemoveWorker;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
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
public class OvsdbNodeListener extends AbstractSyncDataTreeChangeListener<Node> {

    private static final Logger LOG = LoggerFactory.getLogger(OvsdbNodeListener.class);

    private final DataBroker dataBroker;
    private final JobCoordinator jobCoordinator;
    private final ItmConfig itmConfig;

    @Inject
    public OvsdbNodeListener(DataBroker dataBroker, ItmConfig itmConfig, JobCoordinator jobCoordinator) {
        super(dataBroker, LogicalDatastoreType.OPERATIONAL,
              InstanceIdentifier.create(NetworkTopology.class).child(Topology.class).child(Node.class));
        this.dataBroker = dataBroker;
        this.jobCoordinator = jobCoordinator;
        this.itmConfig = itmConfig;
    }

    @Override
    public void add(@Nonnull Node ovsdbNodeNew) {
        String bridgeName = null;
        String strDpnId = "";
        OvsdbNodeAugmentation ovsdbNewNodeAugmentation = null;

        LOG.trace("OvsdbNodeListener called for Ovsdb Node ({}) Add.", ovsdbNodeNew.getNodeId().getValue());

        // check for OVS bridge node
        OvsdbBridgeAugmentation ovsdbNewBridgeAugmentation = ovsdbNodeNew
                .getAugmentation(OvsdbBridgeAugmentation.class);

        if (ovsdbNewBridgeAugmentation != null) {
            bridgeName = ovsdbNewBridgeAugmentation.getBridgeName().getValue();

            // Read DPID from OVSDBBridgeAugmentation
            strDpnId = ItmUtils.getStrDatapathId(ovsdbNewBridgeAugmentation);
            if (strDpnId == null || strDpnId.isEmpty()) {
                LOG.info("OvsdbBridgeAugmentation ADD: DPID for bridge {} is NULL.", bridgeName);
                return;
            }

            // TBD: Move this time taking operations into DataStoreJobCoordinator
            Node ovsdbNodeFromBridge = ItmUtils.getOvsdbNode(ovsdbNewBridgeAugmentation, dataBroker);
            // check for OVSDB node
            if (ovsdbNodeFromBridge != null) {
                ovsdbNewNodeAugmentation = ovsdbNodeFromBridge.getAugmentation(OvsdbNodeAugmentation.class);
            } else {
                LOG.error("Ovsdb Node could not be fetched from Oper DS for bridge {}.", bridgeName);
                return;
            }
        }

        if (ovsdbNewNodeAugmentation != null) {
            // get OVSDB external_ids list from old ovsdb node
            OvsdbExternalIdsInfo ovsdbExternalIdsInfo = getOvsdbNodeExternalIds(ovsdbNewNodeAugmentation);
            // get OVSDB other_config list from old ovsdb node
            OvsdbOtherConfigInfo ovsdbOtherConfigInfo = getOvsdbNodeOtherConfig(ovsdbNewNodeAugmentation);
            if (ovsdbExternalIdsInfo == null && ovsdbOtherConfigInfo == null) {
                return;
            }
            // store OtherConfig required parameters
            String newLocalIp = ovsdbOtherConfigInfo.getLocalIp();
            // store ExternalIds required parameters
            String tzName = ovsdbExternalIdsInfo.getTzName();
            String newBridgeName = ovsdbExternalIdsInfo.getBrName();
            boolean ofTunnel = ovsdbExternalIdsInfo.getOfTunnel();

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
                    jobCoordinator.enqueueJob(newLocalIp,
                                           new OvsdbTepAddWorker(newLocalIp, strDpnId, tzName, ofTunnel, dataBroker));
                } else {
                    LOG.trace("TEP ({}) would be added later when bridge ({}) gets added into Ovs Node [{}].", newLocalIp,
                              newBridgeName, ovsdbNodeNew.getNodeId().getValue());
                }
            } else {
                LOG.trace("Ovs Node [{}] is not configured with Local IP. Nothing to do.",
                          ovsdbNodeNew.getNodeId().getValue());
            }
        }
    }

    @Override
    public void remove(@Nonnull Node removedDataObject) {
        LOG.trace("OvsdbNodeListener called for Ovsdb Node Remove.");
    }

    @Override
    public void update(@Nonnull Node originalOvsdbNode, Node updatedOvsdbNode) {
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

        LOG.trace("OvsdbNodeListener called for Ovsdb Node ({}) Update.", originalOvsdbNode.getNodeId().getValue());

        // get OVSDB external_ids list from old ovsdb node
        OvsdbExternalIdsInfo newExternalIdsInfoObj = getOvsdbNodeExternalIds(
                updatedOvsdbNode.getAugmentation(OvsdbNodeAugmentation.class));

        // get OVSDB external_ids list from new ovsdb node
        OvsdbExternalIdsInfo oldExternalIdsInfoObj = getOvsdbNodeExternalIds(
                originalOvsdbNode.getAugmentation(OvsdbNodeAugmentation.class));

          // get OVSDB other_configs list from old ovsdb node
        OvsdbOtherConfigInfo newOtherConfigInfoObj = getOvsdbNodeOtherConfig(
                updatedOvsdbNode.getAugmentation(OvsdbNodeAugmentation.class));

        // get OVSDB other_configs list from new ovsdb node
        OvsdbOtherConfigInfo oldOtherConfigInfoObj = getOvsdbNodeOtherConfig(
                originalOvsdbNode.getAugmentation(OvsdbNodeAugmentation.class));


        if (oldExternalIdsInfoObj == null && newExternalIdsInfoObj == null && oldOtherConfigInfoObj == null
                && newOtherConfigInfoObj == null) {
            LOG.trace("ExternalIds and OtherConfigs is not received in old and new Ovsdb Nodes.");
            return;
        }

        if (oldExternalIdsInfoObj != null && newExternalIdsInfoObj == null ||
                oldOtherConfigInfoObj != null && newOtherConfigInfoObj == null) {
            isTepInfoDeleted = true;
            LOG.trace("ExternalIds or OtherConfig is deleted from Ovsdb node: {}", originalOvsdbNode.getNodeId().getValue());
        }

        // store ExternalIds required parameters
        if (newExternalIdsInfoObj != null) {
            tzName = newExternalIdsInfoObj.getTzName();
            newDpnBridgeName = newExternalIdsInfoObj.getBrName();
            newOfTunnel = newExternalIdsInfoObj.getOfTunnel();
        }

        if (oldExternalIdsInfoObj != null) {
            oldDpnBridgeName = oldExternalIdsInfoObj.getBrName();
            oldTzName = oldExternalIdsInfoObj.getTzName();
        }

        // store OtherConfigs required parameters
        if (newOtherConfigInfoObj != null) {
            newLocalIp = newOtherConfigInfoObj.getLocalIp();
        }

        if (oldOtherConfigInfoObj != null) {
            oldLocalIp = oldOtherConfigInfoObj.getLocalIp();
        }

        // handle case when TEP parameters are not configured from switch side
        if (newLocalIp == null && oldLocalIp == null) {
            LOG.trace("OtherConfigs Local IP TEP parameters are not specified in old and new Ovsdb Nodes.");
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
                    LOG.trace("dpn-br-name is changed from {} to {} for Local IP: {}", oldDpnBridgeName, newDpnBridgeName,
                            newLocalIp);
                }
            }

            if (!isTepInfoUpdated) {
                LOG.trace("No updates in the ExternalIds or OtherConfigs parameters. Nothing to do.");
                return;
            }
        }

        String strOldDpnId;
        String strNewDpnId;
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
            strOldDpnId = ItmUtils.getBridgeDpid(updatedOvsdbNode, oldDpnBridgeName, dataBroker);
            if (strOldDpnId == null || strOldDpnId.isEmpty()) {
                LOG.error("TEP {} cannot be deleted. DPID for bridge {} is NULL.", oldLocalIp, oldDpnBridgeName);
                return;
            }
            // remove TEP
            LOG.trace("Update case: Removing TEP-IP: {}, TZ name: {}, Bridge Name: {}, Bridge DPID: {}", oldLocalIp,
                      oldTzName, oldDpnBridgeName, strOldDpnId);

            // Enqueue 'remove TEP from TZ' operation into DataStoreJobCoordinator
            jobCoordinator.enqueueJob(oldLocalIp, new OvsdbTepRemoveWorker(oldLocalIp, strOldDpnId, oldTzName, dataBroker));
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
            strNewDpnId = ItmUtils.getBridgeDpid(updatedOvsdbNode, newDpnBridgeName, dataBroker);
            if (strNewDpnId == null || strNewDpnId.isEmpty()) {
                LOG.error("TEP {} cannot be added. DPID for bridge {} is NULL.", newLocalIp, newDpnBridgeName);
                return;
            }
            LOG.trace(
                    "Update case: Adding TEP-IP: {}, TZ name: {}, Bridge Name: {}, Bridge DPID: {}," + "of-tunnel: {}",
                    newLocalIp, tzName, newDpnBridgeName, strNewDpnId, newOfTunnel);

            // Enqueue 'add TEP into new TZ' operation into DataStoreJobCoordinator
            jobCoordinator.enqueueJob(newLocalIp,
                                   new OvsdbTepAddWorker(newLocalIp, strNewDpnId, tzName, newOfTunnel, dataBroker));
        }
    }

    private boolean isLocalIpRemoved(String oldTepIp, String newTepIp) {
        return oldTepIp != null && newTepIp == null;
    }

    private boolean isLocalIpAdded(String oldTepIp, String newTepIp) {
        return oldTepIp == null && newTepIp != null;
    }

    private boolean isLocalIpUpdated(String oldTepIp, String newTepIp) {
        return oldTepIp != null && newTepIp != null && !oldTepIp.equals(newTepIp);
    }

    private boolean isTzUpdated(String oldTzName, String tzName) {
        return oldTzName == null && tzName != null || oldTzName != null && tzName == null
                || oldTzName != null && !oldTzName.equals(tzName);
    }

    private boolean isDpnUpdated(String oldDpnBridgeName, String dpnBridgeName) {
        return oldDpnBridgeName == null && dpnBridgeName != null || oldDpnBridgeName != null && dpnBridgeName == null
                || oldDpnBridgeName != null && !oldDpnBridgeName.equals(dpnBridgeName);
    }

    private OvsdbExternalIdsInfo getOvsdbNodeExternalIds(OvsdbNodeAugmentation ovsdbNodeAugmentation) {
        if (ovsdbNodeAugmentation == null) {
            return null;
        }

        List<OpenvswitchExternalIds> ovsdbNodeExternalIdsList = ovsdbNodeAugmentation.getOpenvswitchExternalIds();
        if (ovsdbNodeExternalIdsList == null) {
            LOG.warn("ExternalIds list does not exist in the OVSDB Node Augmentation.");
            return null;
        }

        OvsdbExternalIdsInfo externalIdsInfoObj = new OvsdbExternalIdsInfo();

        for (OpenvswitchExternalIds externalId : ovsdbNodeExternalIdsList) {
            if (ITMConstants.EXT_ID_TEP_PARAM_KEY_TZNAME.equals(externalId.getExternalIdKey())) {
                String tzName = externalId.getExternalIdValue();
                externalIdsInfoObj.setTzName(tzName);
            } else if (ITMConstants.EXT_ID_TEP_PARAM_KEY_BR_NAME.equals(externalId.getExternalIdKey())) {
                String bridgeName = externalId.getExternalIdValue();
                externalIdsInfoObj.setBrName(bridgeName);
            } else if (ITMConstants.EXT_ID_TEP_PARAM_KEY_OF_TUNNEL.equals(externalId.getExternalIdKey())) {
                boolean ofTunnel = Boolean.parseBoolean(externalId.getExternalIdValue());
                externalIdsInfoObj.setOfTunnel(ofTunnel);
            }
        }

        LOG.trace("{}", externalIdsInfoObj.toString());
        return externalIdsInfoObj;
    }

    private OvsdbOtherConfigInfo getOvsdbNodeOtherConfig(OvsdbNodeAugmentation ovsdbNodeAugmentation) {
        if (ovsdbNodeAugmentation == null) {
            return null;
        }

        List<OpenvswitchOtherConfigs> ovsdbNodeOtherConfigsList = ovsdbNodeAugmentation.getOpenvswitchOtherConfigs();
        if (ovsdbNodeOtherConfigsList == null) {
            LOG.debug("OtherConfigs list does not exist in the OVSDB Node Augmentation.");
            return null;
        }

        OvsdbOtherConfigInfo otherConfigInfoObj = new OvsdbOtherConfigInfo();

        for (OpenvswitchOtherConfigs otherConfigs : ovsdbNodeOtherConfigsList) {
            if (ITMConstants.OTH_CFG_TEP_PARAM_KEY_LOCAL_IP.equals(otherConfigs.getOtherConfigKey())) {
                String tepIp = otherConfigs.getOtherConfigValue();
                otherConfigInfoObj.setLocalIp(tepIp);
            }
        }

        LOG.trace("{}", otherConfigInfoObj.toString());
        return otherConfigInfoObj;
    }

}
