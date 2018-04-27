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
import org.opendaylight.genius.itm.commons.OvsdbTepInfo;
import org.opendaylight.genius.itm.confighelpers.OvsdbTepAddWorker;
import org.opendaylight.genius.itm.confighelpers.OvsdbTepRemoveWorker;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.tools.mdsal.listener.AbstractSyncDataTreeChangeListener;
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
            processBridgeUpdate(ovsdbNewBridgeAugmentation);
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
        boolean isLocalIpUpdated = false;
        boolean isTzChanged = false;
        boolean isDpnBrChanged = false;

        LOG.trace("OvsdbNodeListener called for Ovsdb Node ({}) Update.", originalOvsdbNode.getNodeId().getValue());

        LOG.trace("Update: originalOvsdbNode: {}  updatedOvsdbNode: {}", originalOvsdbNode, updatedOvsdbNode);

        // If this is a bridge update, see if dpid was added. If so, need to
        // addTep, as TEP would not be added in node add case above
        if (isBridgeDpIdAdded(originalOvsdbNode, updatedOvsdbNode)) {
            processBridgeUpdate(updatedOvsdbNode);
            return;
        }

        // get OVSDB TEP info from old ovsdb node
        OvsdbTepInfo newTepInfoObj = getOvsdbTepInfo(
                updatedOvsdbNode.getAugmentation(OvsdbNodeAugmentation.class));

        // get OVSDB TEP info from new ovsdb node
        OvsdbTepInfo oldTepInfoObj = getOvsdbTepInfo(
                originalOvsdbNode.getAugmentation(OvsdbNodeAugmentation.class));

        if (oldTepInfoObj == null && newTepInfoObj == null) {
            LOG.trace("Tep Info is not received in old and new Ovsdb Nodes.");
            return;
        }

        if (oldTepInfoObj != null && newTepInfoObj == null) {
            isTepInfoDeleted = true;
            LOG.trace("Tep Info is deleted from Ovsdb node: {}",
                    originalOvsdbNode.getNodeId().getValue());
        }

        // store TEP info required parameters
        if (newTepInfoObj != null) {
            tzName = newTepInfoObj.getTzName();
            newLocalIp = newTepInfoObj.getLocalIp();
            newDpnBridgeName = newTepInfoObj.getBrName();
            newOfTunnel = newTepInfoObj.getOfTunnel();
        }

        if (oldTepInfoObj != null) {
            oldLocalIp = oldTepInfoObj.getLocalIp();
            oldDpnBridgeName = oldTepInfoObj.getBrName();
            oldTzName = oldTepInfoObj.getTzName();
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

            if (isLocalIpAdded || isLocalIpRemoved || isLocalIpUpdated) {
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

        LOG.trace("TepInfo state change flags (isTepInfoUpdated: {}, isTepInfoDeleted: {}, isLocalIpRemoved: {},"
                + "isLocalIpAdded: {}, isLocalIpUpdated: {}, isTzChanged:{}, isDpnBrChanged: {})",
                isTepInfoUpdated, isTepInfoDeleted, isLocalIpRemoved, isLocalIpAdded, isLocalIpUpdated,
                isTzChanged, isDpnBrChanged);

        String strOldDpnId;
        String strNewDpnId;
        // handle TEP-remove in remove case, TZ change case, Bridge change case
        if (isTepInfoDeleted || isLocalIpRemoved || isTzChanged || isDpnBrChanged || isLocalIpUpdated) {
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
            jobCoordinator.enqueueJob(oldLocalIp, new OvsdbTepRemoveWorker(oldLocalIp, strOldDpnId, oldTzName,
                    dataBroker));
        }
        // handle TEP-add in add case, TZ change case, Bridge change case
        if (isLocalIpAdded || isTzChanged || isDpnBrChanged || isLocalIpUpdated) {
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

            String jobKey = isLocalIpUpdated ? oldLocalIp : newLocalIp;
            // Enqueue 'add TEP into new TZ' operation into DataStoreJobCoordinator
            jobCoordinator.enqueueJob(jobKey,
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

    private boolean isBridgeDpIdAdded(Node ovsdbNodeOld, Node ovsdbNodeNew) {
        String oldBridgeName = null;
        String oldDpId = null;
        String newDpId = null;

        OvsdbBridgeAugmentation ovsdbNewBridgeAugmentation =
                ovsdbNodeNew.getAugmentation(OvsdbBridgeAugmentation.class);
        if (ovsdbNewBridgeAugmentation != null) {
            // Read DPID from OVSDBBridgeAugmentation
            newDpId = ItmUtils.getStrDatapathId(ovsdbNewBridgeAugmentation);
        }

        OvsdbBridgeAugmentation ovsdbOldBridgeAugmentation =
                ovsdbNodeOld.getAugmentation(OvsdbBridgeAugmentation.class);
        if (ovsdbOldBridgeAugmentation != null) {
            oldBridgeName = ovsdbNewBridgeAugmentation.getBridgeName().getValue();
            // Read DPID from OVSDBBridgeAugmentation
            oldDpId = ItmUtils.getStrDatapathId(ovsdbOldBridgeAugmentation);
        }
        if (oldDpId == null && newDpId != null) {
            LOG.trace("DpId changed from {} to {} for bridge {}",
                    oldDpId, newDpId, oldBridgeName);
            return true;
        }
        return false;
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

    private void processBridgeUpdate(Node ovsdbNodeNew) {
        OvsdbBridgeAugmentation ovsdbNewBridgeAugmentation =
                ovsdbNodeNew.getAugmentation(OvsdbBridgeAugmentation.class);
        if (ovsdbNewBridgeAugmentation != null) {
            processBridgeUpdate(ovsdbNewBridgeAugmentation);
        }
    }

    private void processBridgeUpdate(OvsdbBridgeAugmentation ovsdbNewBridgeAugmentation) {
        String bridgeName = null;
        String strDpnId = null;
        OvsdbNodeAugmentation ovsdbNewNodeAugmentation = null;

        if (ovsdbNewBridgeAugmentation != null) {
            bridgeName = ovsdbNewBridgeAugmentation.getBridgeName().getValue();

            // Read DPID from OVSDBBridgeAugmentation
            strDpnId = ItmUtils.getStrDatapathId(ovsdbNewBridgeAugmentation);
            if (strDpnId == null || strDpnId.isEmpty()) {
                LOG.info("OvsdbBridgeAugmentation processBridgeUpdate: DPID for bridge {} is NULL.", bridgeName);
                return;
            }

            // TBD: Move this time taking operations into DataStoreJobCoordinator
            Node ovsdbNodeFromBridge = ItmUtils.getOvsdbNode(ovsdbNewBridgeAugmentation, dataBroker);
            // check for OVSDB node
            if (ovsdbNodeFromBridge != null) {
                ovsdbNewNodeAugmentation = ovsdbNodeFromBridge.getAugmentation(OvsdbNodeAugmentation.class);
            } else {
                LOG.error("processBridgeUpdate: Ovsdb Node could not be fetched from Oper DS for bridge {}.",
                        bridgeName);
                return;
            }
        }

        if (ovsdbNewNodeAugmentation != null) {
            OvsdbTepInfo ovsdbTepInfo = getOvsdbTepInfo(ovsdbNewNodeAugmentation);

            if (ovsdbTepInfo == null) {
                LOG.trace("processBridgeUpdate: No Tep Info");
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
                    LOG.trace("Ovs Node with bridge {} is configured with Local IP.", bridgeName);

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

                    // Enqueue 'add TEP into new TZ' operation into DataStoreJobCoordinator
                    jobCoordinator.enqueueJob(newLocalIp,
                            new OvsdbTepAddWorker(newLocalIp, strDpnId, tzName, ofTunnel, dataBroker));
                } else {
                    LOG.trace("TEP ({}) would be added later when bridge {} gets added into Ovs Node.",
                               newLocalIp, newBridgeName);
                }
            } else {
                LOG.trace("Ovs Node with bridge {} is not configured with Local IP. Nothing to do.",
                          bridgeName);
            }
        }
    }
    // End of class
}
