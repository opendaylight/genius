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
import org.opendaylight.genius.itm.commons.OvsdbOtherConfigInfo;
import org.opendaylight.genius.itm.confighelpers.OvsdbTepAddWorker;
import org.opendaylight.genius.itm.confighelpers.OvsdbTepRemoveWorker;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
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
            // get OVSDB other_config list from old ovsdb node
            OvsdbOtherConfigInfo ovsdbOtherConfigInfo = getOvsdbNodeOtherConfig(ovsdbNewNodeAugmentation);
            if (ovsdbOtherConfigInfo == null) {
                return;
            }
            // store Other Configs required parameters
            String newTepIp = ovsdbOtherConfigInfo.getTepIp();
            String tzName = ovsdbOtherConfigInfo.getTzName();
            String newBridgeName = ovsdbOtherConfigInfo.getBrName();
            boolean ofTunnel = ovsdbOtherConfigInfo.getOfTunnel();

            // check if TEP-IP is configured or not
            if (newTepIp != null && !newTepIp.isEmpty()) {
                // if bridge received is the one configured for TEPs from OVS side or
                // if it is br-int, then add TEP into Config DS
                if (newBridgeName.equals(bridgeName)) {
                    LOG.trace("Ovs Node [{}] is configured with TEP-IP.", ovsdbNodeNew.getNodeId().getValue());

                    // check if defTzEnabled flag is false in config file,
                    // if flag is OFF, then no need to add TEP into ITM config DS.
                    if (tzName == null || tzName.equals(ITMConstants.DEFAULT_TRANSPORT_ZONE)) {
                        boolean defTzEnabled = itmConfig.isDefTzEnabled();
                        if (!defTzEnabled) {
                            LOG.info("TEP ({}) cannot be added into {} when def-tz-enabled flag is false.", newTepIp,
                                     ITMConstants.DEFAULT_TRANSPORT_ZONE);
                            return;
                        }
                    }

                    LOG.trace("TEP-IP: {}, TZ name: {}, Bridge Name: {}, Bridge DPID: {}," + "of-tunnel flag: {}",
                              newTepIp, tzName, newBridgeName, strDpnId, ofTunnel);

                    // Enqueue 'add TEP received from southbound OVSDB into ITM config DS' operation
                    // into DataStoreJobCoordinator
                    jobCoordinator.enqueueJob(newTepIp,
                                           new OvsdbTepAddWorker(newTepIp, strDpnId, tzName, ofTunnel, dataBroker));
                } else {
                    LOG.trace("TEP ({}) would be added later when bridge ({}) gets added into Ovs Node [{}].", newTepIp,
                              newBridgeName, ovsdbNodeNew.getNodeId().getValue());
                }
            } else {
                LOG.trace("Ovs Node [{}] is not configured with TEP-IP. Nothing to do.",
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
        String newTepIp = null;
        String oldTepIp = null;
        String tzName = null;
        String oldTzName = null;
        String oldDpnBridgeName = null;
        String newDpnBridgeName = null;
        boolean newOfTunnel = false;
        boolean isOtherConfigUpdated = false;
        boolean isOtherConfigDeleted = false;
        boolean isTepIpAdded = false;
        boolean isTepIpRemoved = false;
        boolean isTepIpUpdated;
        boolean isTzChanged = false;
        boolean isDpnBrChanged = false;

        LOG.trace("OvsdbNodeListener called for Ovsdb Node ({}) Update.", originalOvsdbNode.getNodeId().getValue());

        // get OVSDB other_configs list from old ovsdb node
        OvsdbOtherConfigInfo newOtherConfigInfoObj = getOvsdbNodeOtherConfig(
                updatedOvsdbNode.getAugmentation(OvsdbNodeAugmentation.class));

        // get OVSDB other_configs list from new ovsdb node
        OvsdbOtherConfigInfo oldOtherConfigInfoObj = getOvsdbNodeOtherConfig(
                originalOvsdbNode.getAugmentation(OvsdbNodeAugmentation.class));

        if (oldOtherConfigInfoObj == null && newOtherConfigInfoObj == null) {
            LOG.trace("OtherConfig is not received in old and new Ovsdb Nodes.");
            return;
        }

        if (oldOtherConfigInfoObj != null && newOtherConfigInfoObj == null) {
            isOtherConfigDeleted = true;
            LOG.trace("OtherConfig is deleted from Ovsdb node: {}", originalOvsdbNode.getNodeId().getValue());
        }

        // store OtherConfigs required parameters
        if (newOtherConfigInfoObj != null) {
            newTepIp = newOtherConfigInfoObj.getTepIp();
            tzName = newOtherConfigInfoObj.getTzName();
            newDpnBridgeName = newOtherConfigInfoObj.getBrName();
            newOfTunnel = newOtherConfigInfoObj.getOfTunnel();
        }

        if (oldOtherConfigInfoObj != null) {
            oldDpnBridgeName = oldOtherConfigInfoObj.getBrName();
            oldTzName = oldOtherConfigInfoObj.getTzName();
            oldTepIp = oldOtherConfigInfoObj.getTepIp();
        }

        // handle case when TEP parameters are not configured from switch side
        if (newTepIp == null && oldTepIp == null) {
            LOG.trace("OtherConfig TEP parameters are not specified in old and new Ovsdb Nodes.");
            return;
        }

        if (!isOtherConfigDeleted) {
            isTepIpRemoved = isTepIpRemoved(oldTepIp, newTepIp);
            isTepIpAdded = isTepIpAdded(oldTepIp, newTepIp);
            isTepIpUpdated = isTepIpUpdated(oldTepIp, newTepIp);

            if (isTepIpUpdated) {
                LOG.info("TEP-IP cannot be updated. First delete the TEP and then add again.");
                return;
            }

            if (isTepIpAdded || isTepIpRemoved) {
                isOtherConfigUpdated = true;
            }
            if (isTzUpdated(oldTzName, tzName)) {
                isOtherConfigUpdated = true;
                if (oldTepIp != null && newTepIp != null) {
                    isTzChanged = true;
                    LOG.trace("tzname is changed from {} to {} for TEP-IP: {}", oldTzName, tzName, newTepIp);
                }
            }
            if (isDpnUpdated(oldDpnBridgeName, newDpnBridgeName)) {
                isOtherConfigUpdated = true;
                if (oldTepIp != null && newTepIp != null) {
                    isDpnBrChanged = true;
                    LOG.trace("dpn-br-name is changed from {} to {} for TEP-IP: {}", oldDpnBridgeName, newDpnBridgeName,
                            newTepIp);
                }
            }

            if (!isOtherConfigUpdated) {
                LOG.trace("No updates in the OtherConfig parameters. Nothing to do.");
                return;
            }
        }

        String strOldDpnId;
        String strNewDpnId;
        // handle TEP-remove in remove case, TZ change case, Bridge change case
        if (isOtherConfigDeleted || isTepIpRemoved || isTzChanged || isDpnBrChanged) {
            // check if defTzEnabled flag is false in config file,
            // if flag is OFF, then no need to add TEP into ITM config DS.
            if (oldTzName == null || oldTzName.equals(ITMConstants.DEFAULT_TRANSPORT_ZONE)) {
                boolean defTzEnabled = itmConfig.isDefTzEnabled();
                if (!defTzEnabled) {
                    LOG.info("TEP ({}) cannot be removed from {} when def-tz-enabled flag is false.", oldTepIp,
                             ITMConstants.DEFAULT_TRANSPORT_ZONE);
                    return;
                }
                oldTzName = ITMConstants.DEFAULT_TRANSPORT_ZONE;
            }
            // TBD: Move this time taking operations into DataStoreJobCoordinator
            strOldDpnId = ItmUtils.getBridgeDpid(updatedOvsdbNode, oldDpnBridgeName, dataBroker);
            if (strOldDpnId == null || strOldDpnId.isEmpty()) {
                LOG.error("TEP {} cannot be deleted. DPID for bridge {} is NULL.", oldTepIp, oldDpnBridgeName);
                return;
            }
            // remove TEP
            LOG.trace("Update case: Removing TEP-IP: {}, TZ name: {}, Bridge Name: {}, Bridge DPID: {}", oldTepIp,
                      oldTzName, oldDpnBridgeName, strOldDpnId);

            // Enqueue 'remove TEP from TZ' operation into DataStoreJobCoordinator
            jobCoordinator.enqueueJob(oldTepIp, new OvsdbTepRemoveWorker(oldTepIp, strOldDpnId, oldTzName, dataBroker));
        }
        // handle TEP-add in add case, TZ change case, Bridge change case
        if (isTepIpAdded || isTzChanged || isDpnBrChanged) {
            // check if defTzEnabled flag is false in config file,
            // if flag is OFF, then no need to add TEP into ITM config DS.
            if (tzName == null || tzName.equals(ITMConstants.DEFAULT_TRANSPORT_ZONE)) {
                boolean defTzEnabled = itmConfig.isDefTzEnabled();
                if (!defTzEnabled) {
                    LOG.info("TEP ({}) cannot be added into {} when def-tz-enabled flag is false.", newTepIp,
                             ITMConstants.DEFAULT_TRANSPORT_ZONE);
                    return;
                }
                tzName = ITMConstants.DEFAULT_TRANSPORT_ZONE;
            }
            // TBD: Move this time taking operations into DataStoreJobCoordinator
            // get Datapath ID for bridge
            strNewDpnId = ItmUtils.getBridgeDpid(updatedOvsdbNode, newDpnBridgeName, dataBroker);
            if (strNewDpnId == null || strNewDpnId.isEmpty()) {
                LOG.error("TEP {} cannot be added. DPID for bridge {} is NULL.", newTepIp, newDpnBridgeName);
                return;
            }
            LOG.trace(
                    "Update case: Adding TEP-IP: {}, TZ name: {}, Bridge Name: {}, Bridge DPID: {}," + "of-tunnel: {}",
                    newTepIp, tzName, newDpnBridgeName, strNewDpnId, newOfTunnel);

            // Enqueue 'add TEP into new TZ' operation into DataStoreJobCoordinator
            jobCoordinator.enqueueJob(newTepIp,
                                   new OvsdbTepAddWorker(newTepIp, strNewDpnId, tzName, newOfTunnel, dataBroker));
        }
    }

    private boolean isTepIpRemoved(String oldTepIp, String newTepIp) {
        return oldTepIp != null && newTepIp == null;
    }

    private boolean isTepIpAdded(String oldTepIp, String newTepIp) {
        return oldTepIp == null && newTepIp != null;
    }

    private boolean isTepIpUpdated(String oldTepIp, String newTepIp) {
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
                otherConfigInfoObj.setTepIp(tepIp);
            } else if (ITMConstants.OTH_CFG_TEP_PARAM_KEY_TZNAME.equals(otherConfigs.getOtherConfigKey())) {
                String tzName = otherConfigs.getOtherConfigValue();
                otherConfigInfoObj.setTzName(tzName);
            } else if (ITMConstants.OTH_CFG_TEP_PARAM_KEY_BR_NAME.equals(otherConfigs.getOtherConfigKey())) {
                String bridgeName = otherConfigs.getOtherConfigValue();
                otherConfigInfoObj.setBrName(bridgeName);
            } else if (ITMConstants.OTH_CFG_TEP_PARAM_KEY_OF_TUNNEL.equals(otherConfigs.getOtherConfigKey())) {
                boolean ofTunnel = Boolean.parseBoolean(otherConfigs.getOtherConfigValue());
                otherConfigInfoObj.setOfTunnel(ofTunnel);
            }
        }

        LOG.trace("{}", otherConfigInfoObj.toString());
        return otherConfigInfoObj;
    }
}
