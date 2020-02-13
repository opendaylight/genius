/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.listeners;

import java.time.Duration;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.listeners.DataTreeEventCallbackRegistrar;
import org.opendaylight.genius.itm.commons.OvsdbTepInfo;
import org.opendaylight.genius.itm.confighelpers.OvsdbTepAddWorker;
import org.opendaylight.genius.itm.confighelpers.OvsdbTepRemoveWorker;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.serviceutils.tools.mdsal.listener.AbstractSyncDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfoKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.tunnel.end.points.TzMembership;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint64;
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
    private final DataTreeEventCallbackRegistrar eventCallbacks;

    @Inject
    public OvsdbNodeListener(DataBroker dataBroker, ItmConfig itmConfig, JobCoordinator jobCoordinator,
                             DataTreeEventCallbackRegistrar eventCallbacks) {
        super(dataBroker, LogicalDatastoreType.OPERATIONAL,
              InstanceIdentifier.create(NetworkTopology.class).child(Topology.class).child(Node.class));
        this.dataBroker = dataBroker;
        this.jobCoordinator = jobCoordinator;
        this.itmConfig = itmConfig;
        this.eventCallbacks = eventCallbacks;
    }

    @Override
    public void add(@NonNull InstanceIdentifier<Node> instanceIdentifier, @NonNull Node ovsdbNodeNew) {
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
    public void remove(@NonNull InstanceIdentifier<Node> instanceIdentifier, @NonNull Node removedDataObject) {
        LOG.trace("OvsdbNodeListener called for Ovsdb Node {} Remove.", removedDataObject);
        processBridgeUpdate(removedDataObject, false);
    }

    @Override
    public void update(@NonNull InstanceIdentifier<Node> instanceIdentifier, @NonNull Node originalOvsdbNode,
                       @NonNull Node updatedOvsdbNode) {
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

        String jobKey = oldLocalIp;
        // handle TEP-remove in remove case, tep-ip update case, TZ change case, Bridge change case
        if (isTepInfoDeleted || isLocalIpRemoved || isTzChanged || isDpnBrChanged || isLocalIpUpdated) {
            // TBD: Move this time taking operations into DataStoreJobCoordinator
            String strOldDpnId = ItmUtils.getBridgeDpid(originalOvsdbNode, oldDpnBridgeName, dataBroker);
            if (strOldDpnId == null || strOldDpnId.isEmpty()) {
                LOG.error("TEP {} cannot be deleted. DPID for bridge {} is NULL.", oldLocalIp, oldDpnBridgeName);
                return;
            }
            addOrRemoveTep(oldTzName, strOldDpnId, jobKey, oldLocalIp, oldDpnBridgeName, false, false);
        }
        // handle TEP-add in add case, tep-ip update case, TZ change case, Bridge change case
        if (isLocalIpAdded || isTzChanged || isDpnBrChanged || isLocalIpUpdated) {
            // TBD: Move this time taking operations into DataStoreJobCoordinator
            String strNewDpnId = ItmUtils.getBridgeDpid(updatedOvsdbNode, newDpnBridgeName, dataBroker);
            if (strNewDpnId == null || strNewDpnId.isEmpty()) {
                LOG.error("TEP {} cannot be added. DPID for bridge {} is NULL.", newLocalIp, newDpnBridgeName);
                return;
            }
            /*
             * Special handling for TEP movement from one TZ to another TZ
             * Register for DpnTepsInfo remove event to make sure TEP remove is happened through ITM internal logic,
             * then after perform TEP addition into updated TZ
             */
            if (isTzChanged) {
                IpAddress tepIpAddress = IpAddressBuilder.getDefaultInstance(newLocalIp);
                Uint64 dpnId = MDSALUtil.getDpnId(strNewDpnId);
                String tos = itmConfig.getDefaultTunnelTos();
                Class<? extends TunnelTypeBase> tunnelType  = TunnelTypeVxlan.class;
                List<TzMembership> zones = ItmUtils.createTransportZoneMembership(oldTzName);

                String portName = itmConfig.getPortname() == null ? ITMConstants.DUMMY_PORT : itmConfig.getPortname();
                int vlanId = itmConfig.getVlanId() != null ? itmConfig.getVlanId().toJava()
                                                             : ITMConstants.DUMMY_VLANID;

                TunnelEndPoints tunnelEndPoints = ItmUtils.createDummyTunnelEndPoints(dpnId, tepIpAddress, newOfTunnel,
                        tos, zones, tunnelType, portName, vlanId);
                String finalTzName = tzName;
                String finalJobKey = jobKey;
                String finalLocalIp = newLocalIp;
                String finalDpnBridgeName = newDpnBridgeName;
                boolean finalOfTunnel = newOfTunnel;

                InstanceIdentifier<TunnelEndPoints> tunnelEndPointsIdentifier =
                        InstanceIdentifier.builder(DpnEndpoints.class).child(DPNTEPsInfo.class,
                                new DPNTEPsInfoKey(dpnId)).child(TunnelEndPoints.class, tunnelEndPoints.key()).build();
                eventCallbacks.onRemove(LogicalDatastoreType.CONFIGURATION, tunnelEndPointsIdentifier, (unused) -> {
                    LOG.info("TZ movement case: callback event for a deletion of {} from DpnTepsInfo.", dpnId);
                    addOrRemoveTep(finalTzName, strNewDpnId, finalJobKey, finalLocalIp,
                                    finalDpnBridgeName, finalOfTunnel, true);
                    return DataTreeEventCallbackRegistrar.NextAction.UNREGISTER;
                }, Duration.ofMillis(5000), (id) -> {
                        LOG.info("TZ movement case: callback event timed-out for a deletion of {} from DpnTepsInfo.",
                                dpnId);
                        addOrRemoveTep(finalTzName, strNewDpnId, finalJobKey, finalLocalIp,
                            finalDpnBridgeName, finalOfTunnel, true);
                    });
            } else {
                jobKey = isLocalIpUpdated ? oldLocalIp : newLocalIp;
                addOrRemoveTep(tzName, strNewDpnId, jobKey, newLocalIp, newDpnBridgeName,  newOfTunnel, true);
            }
        }
    }

    private void addOrRemoveTep(String tzName, String strDpnId, String jobKey, String localIp, String  bridgeName,
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
            jobCoordinator.enqueueJob(jobKey,
                    new OvsdbTepAddWorker(localIp, strDpnId, tzName, newOfTunnel, dataBroker));
        } else {
            // remove TEP
            LOG.trace("Update case: Removing TEP-IP: {}, TZ name: {}, Bridge Name: {}, Bridge DPID: {}", localIp,
                    tzName, bridgeName, strDpnId);

            // Enqueue 'remove TEP from TZ' operation into DataStoreJobCoordinator
            jobCoordinator.enqueueJob(jobKey, new OvsdbTepRemoveWorker(localIp, strDpnId, tzName, dataBroker));
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
            // check for OVSDB node. NOTE: it can be null during bridge removal notification
            // when switch is disconnected
            if (ovsdbNodeFromBridge != null) {
                ovsdbNewNodeAugmentation = ovsdbNodeFromBridge.augmentation(OvsdbNodeAugmentation.class);
            } else {
                LOG.warn("processBridgeUpdate: bridge {} removal case when Switch is disconnected."
                         + "Hence, Ovsdb Node could not be fetched from Oper DS.", bridgeName);
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
                    String jobKey = newLocalIp;
                    // add or remove tep based on bridge (br-int) is added or removed
                    addOrRemoveTep(tzName, strDpnId, jobKey, newLocalIp, newBridgeName,  ofTunnel, isBridgeAdd);
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
