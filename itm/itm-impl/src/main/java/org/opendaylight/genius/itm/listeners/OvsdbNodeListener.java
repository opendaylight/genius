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
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.serviceutils.tools.mdsal.listener.AbstractSyncDataTreeChangeListener;
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
    public void add(@Nonnull InstanceIdentifier<Node> instanceIdentifier, @Nonnull Node ovsdbNodeNew) {
        String bridgeName = null;
        String strDpnId = "";
        OvsdbNodeAugmentation ovsdbNewNodeAugmentation = null;

        LOG.trace("OvsdbNodeListener called for Ovsdb Node ({}) Add.", ovsdbNodeNew.getNodeId().getValue());

        // check for OVS bridge node
        OvsdbBridgeAugmentation ovsdbNewBridgeAugmentation = ovsdbNodeNew
                .augmentation(OvsdbBridgeAugmentation.class);

        if (ovsdbNewBridgeAugmentation != null) {
            processBridgeUpdate(ovsdbNewBridgeAugmentation, true);
        }
    }

    @Override
    public void remove(@Nonnull InstanceIdentifier<Node> instanceIdentifier, @Nonnull Node removedDataObject) {
        LOG.trace("OvsdbNodeListener called for Ovsdb Node {} Remove.", removedDataObject);
        processBridgeUpdate(removedDataObject, false);
    }

    @Override
    public void update(@Nonnull InstanceIdentifier<Node> instanceIdentifier, @Nonnull Node originalOvsdbNode,
                       @Nonnull Node updatedOvsdbNode) {
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

        // If this is a bridge update, see if dpid was added. If so, need to
        // addTep, as TEP would not be added in node add case above
        if (isBridgeDpIdAdded(originalOvsdbNode, updatedOvsdbNode)) {
            processBridgeUpdate(updatedOvsdbNode, true);
            return;
        }

        // get OVSDB TEP info from old ovsdb node
        OvsdbTepInfo newTepInfoObj = getOvsdbTepInfo(
                updatedOvsdbNode.augmentation(OvsdbNodeAugmentation.class));

        // get OVSDB TEP info from new ovsdb node
        OvsdbTepInfo oldTepInfoObj = getOvsdbTepInfo(
                originalOvsdbNode.augmentation(OvsdbNodeAugmentation.class));

        if (oldTepInfoObj == null && newTepInfoObj == null) {
            LOG.trace("Tep Info is not received in old and new Ovsdb Nodes.");
            return;
        }

        if (oldTepInfoObj != null && newTepInfoObj == null) {
            isTepInfoDeleted = true;
            LOG.trace("Tep Info is deleted from Ovsdb node: {}", originalOvsdbNode.getNodeId().getValue());
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
                LOG.trace("Old TEP (local-ip: {}, tz-name: {}) and New TEP (local-ip: {}, tz-name: {}). "
                        + "No updates in the TEP Info parameters. Nothing to do.", oldLocalIp, oldTzName,
                        newLocalIp, tzName);
                return;
            }
        }

        LOG.trace("TepInfo state change flags (isTepInfoUpdated: {}, isTepInfoDeleted: {}, isLocalIpRemoved: {},"
                + "isLocalIpAdded: {}, isLocalIpUpdated: {}, isTzChanged:{}, isDpnBrChanged: {})",
                isTepInfoUpdated, isTepInfoDeleted, isLocalIpRemoved, isLocalIpAdded, isLocalIpUpdated,
                isTzChanged, isDpnBrChanged);

        // handle TEP-remove in remove case, TZ change case, Bridge change case
        if (isTepInfoDeleted || isLocalIpRemoved || isTzChanged || isDpnBrChanged || isLocalIpUpdated) {
            // TBD: Move this time taking operations into DataStoreJobCoordinator
            String strOldDpnId = ItmUtils.getBridgeDpid(originalOvsdbNode, oldDpnBridgeName, dataBroker);
            if (strOldDpnId == null || strOldDpnId.isEmpty()) {
                LOG.error("TEP {} cannot be deleted. DPID for bridge {} is NULL.", oldLocalIp, oldDpnBridgeName);
                return;
            }
            addOrRemoveTep(oldTzName, strOldDpnId, oldLocalIp, oldDpnBridgeName, false, false);
        }
        // handle TEP-add in add case, TZ change case, Bridge change case
        if (isLocalIpAdded || isTzChanged || isDpnBrChanged || isLocalIpUpdated) {
            // TBD: Move this time taking operations into DataStoreJobCoordinator
            String strNewDpnId = ItmUtils.getBridgeDpid(updatedOvsdbNode, newDpnBridgeName, dataBroker);
            if (strNewDpnId == null || strNewDpnId.isEmpty()) {
                LOG.error("TEP {} cannot be added. DPID for bridge {} is NULL.", newLocalIp, newDpnBridgeName);
                return;
            }
            String localIp = isLocalIpUpdated ? oldLocalIp : newLocalIp;
            addOrRemoveTep(tzName, strNewDpnId, localIp, newDpnBridgeName,  newOfTunnel, true);
        }
    }

    private void addOrRemoveTep(String tzName, String strDpnId, String localIp, String  bridgeName,
                                boolean newOfTunnel, boolean isTepAdd) {
        // check if defTzEnabled flag is false in config file,
        // if flag is OFF, then no need to add TEP into ITM config DS.
        if (tzName == null || tzName.equals(ITMConstants.DEFAULT_TRANSPORT_ZONE)) {
            boolean defTzEnabled = itmConfig.isDefTzEnabled();
            if (!defTzEnabled) {
                if (isTepAdd) {
                    LOG.info("TEP ({}) cannot be added into {} when def-tz-enabled flag is false.", localIp,
                            ITMConstants.DEFAULT_TRANSPORT_ZONE);
                } else {
                    LOG.info("TEP ({}) cannot be removed from {} when def-tz-enabled flag is false.", localIp,
                            ITMConstants.DEFAULT_TRANSPORT_ZONE);
                }
                return;
            }
            tzName = ITMConstants.DEFAULT_TRANSPORT_ZONE;
        }

        if (isTepAdd) {
            // add TEP
            LOG.trace("Update case: Adding TEP-IP: {}, TZ name: {}, Bridge Name: {}, Bridge DPID: {}, of-tunnel: {}",
                    localIp, tzName, bridgeName, strDpnId, newOfTunnel);

            // Enqueue 'add TEP into new TZ' operation into DataStoreJobCoordinator
            jobCoordinator.enqueueJob(localIp,
                    new OvsdbTepAddWorker(localIp, strDpnId, tzName, newOfTunnel, dataBroker));
        } else {
            // remove TEP
            LOG.trace("Update case: Removing TEP-IP: {}, TZ name: {}, Bridge Name: {}, Bridge DPID: {}", localIp,
                    tzName, bridgeName, strDpnId);

            // Enqueue 'remove TEP from TZ' operation into DataStoreJobCoordinator
            jobCoordinator.enqueueJob(localIp, new OvsdbTepRemoveWorker(localIp, strDpnId, tzName, dataBroker));
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
                ovsdbNodeNew.augmentation(OvsdbBridgeAugmentation.class);
        if (ovsdbNewBridgeAugmentation != null) {
            // Read DPID from OVSDBBridgeAugmentation
            newDpId = ItmUtils.getStrDatapathId(ovsdbNewBridgeAugmentation);
        }

        OvsdbBridgeAugmentation ovsdbOldBridgeAugmentation =
                ovsdbNodeOld.augmentation(OvsdbBridgeAugmentation.class);
        if (ovsdbOldBridgeAugmentation != null) {
            oldBridgeName = ovsdbNewBridgeAugmentation.getBridgeName().getValue();
            // Read DPID from OVSDBBridgeAugmentation
            oldDpId = ItmUtils.getStrDatapathId(ovsdbOldBridgeAugmentation);
        }
        if (oldDpId == null && newDpId != null) {
            LOG.trace("DpId changed to {} for bridge {}", newDpId, oldBridgeName);
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

        OvsdbTepInfo ovsdbTepInfoObj = new OvsdbTepInfo();

        for (OpenvswitchOtherConfigs otherConfigs : ovsdbNodeOtherConfigsList) {
            if (ITMConstants.OTH_CFG_TEP_PARAM_KEY_LOCAL_IP.equals(otherConfigs.getOtherConfigKey())) {
                String tepIp = otherConfigs.getOtherConfigValue();
                ovsdbTepInfoObj.setLocalIp(tepIp);
            }
        }

        List<OpenvswitchExternalIds> ovsdbNodeExternalIdsList = ovsdbNodeAugmentation.getOpenvswitchExternalIds();
        if (ovsdbNodeExternalIdsList == null) {
            LOG.debug("ExternalIds list does not exist in the OVSDB Node Augmentation.");
        } else {
            for (OpenvswitchExternalIds externalId : ovsdbNodeExternalIdsList) {
                switch (externalId.getExternalIdKey()) {
                    case ITMConstants.EXT_ID_TEP_PARAM_KEY_TZNAME:
                        ovsdbTepInfoObj.setTzName(externalId.getExternalIdValue());
                        break;
                    case ITMConstants.EXT_ID_TEP_PARAM_KEY_BR_NAME:
                        ovsdbTepInfoObj.setBrName(externalId.getExternalIdValue());
                        break;
                    case ITMConstants.EXT_ID_TEP_PARAM_KEY_OF_TUNNEL:
                        ovsdbTepInfoObj.setOfTunnel(Boolean.parseBoolean(externalId.getExternalIdValue()));
                        break;
                    default:
                        break;
                }
            }
        }
        return ovsdbTepInfoObj;
    }

    private void processBridgeUpdate(Node ovsdbNodeNew, boolean isBridgeAdd) {
        OvsdbBridgeAugmentation ovsdbNewBridgeAugmentation =
                ovsdbNodeNew.augmentation(OvsdbBridgeAugmentation.class);
        if (ovsdbNewBridgeAugmentation != null) {
            processBridgeUpdate(ovsdbNewBridgeAugmentation, isBridgeAdd);
        }
    }

    private void processBridgeUpdate(OvsdbBridgeAugmentation ovsdbNewBridgeAugmentation, boolean isBridgeAdd) {
        String bridgeName = null;
        String strDpnId = null;
        OvsdbNodeAugmentation ovsdbNewNodeAugmentation = null;

        if (ovsdbNewBridgeAugmentation != null) {
            bridgeName = ovsdbNewBridgeAugmentation.getBridgeName().getValue();

            // Read DPID from OVSDBBridgeAugmentation
            strDpnId = ItmUtils.getStrDatapathId(ovsdbNewBridgeAugmentation);
            if (strDpnId == null || strDpnId.isEmpty()) {
                LOG.trace("OvsdbBridgeAugmentation processBridgeUpdate: DPID for bridge {} is NULL.", bridgeName);
                return;
            }

            // TBD: Move this time taking operations into DataStoreJobCoordinator
            Node ovsdbNodeFromBridge = ItmUtils.getOvsdbNode(ovsdbNewBridgeAugmentation, dataBroker);
            // check for OVSDB node
            if (ovsdbNodeFromBridge != null) {
                ovsdbNewNodeAugmentation = ovsdbNodeFromBridge.augmentation(OvsdbNodeAugmentation.class);
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
                    LOG.trace("processBridgeUpdate for bridge {} that is configured with Local IP.", bridgeName);
                    // add or remove tep based on bridge (br-int) is added or removed
                    addOrRemoveTep(tzName, strDpnId, newLocalIp, newBridgeName,  ofTunnel, isBridgeAdd);
                } else {
                    LOG.trace("processBridgeUpdate invoked for bridge {}, nothing to do.", bridgeName);
                }
            } else {
                LOG.trace("processBridgeUpdate for bridge {} without Local IP set for ovs node. Nothing to do.",
                          bridgeName);
            }
        }
    }
}