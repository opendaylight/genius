/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.listeners;

import java.math.BigInteger;
import java.util.*;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncDataTreeChangeListenerBase;

import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.globals.ITMConstants;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;

import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.tepsnothostedintransportzone.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.Subnets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.SubnetsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.SubnetsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.Vteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.VtepsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.VtepsKey;
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
 * This is used to handle TEPs of switches.
 */
public class OvsdbNodeListener extends AsyncDataTreeChangeListenerBase<Node, OvsdbNodeListener>
    implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbNodeListener.class);
    private DataBroker dataBroker;
    private Map<String, OvsdbOtherConfigInfo> ovsNodeToBridgesDpnIdMap;
    private Map<String, String> bridgesMap;

    public OvsdbNodeListener(final DataBroker dataBroker) {
        super(Node.class, OvsdbNodeListener.class);
        this.dataBroker = dataBroker;
        this.ovsNodeToBridgesDpnIdMap = new HashMap<>();
        this.bridgesMap = new HashMap<>();
        LOG.info("OvsdbNodeListener Created");
    }

    @Override public void close() throws Exception {
        LOG.info("OvsdbNodeListener Closed");
        // clear the map
        ovsNodeToBridgesDpnIdMap.clear();
        bridgesMap.clear();
    }

    @Override protected InstanceIdentifier<Node> getWildCardPath() {
        return InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class)
            .child(Node.class).build();
    }

    @Override protected OvsdbNodeListener getDataTreeChangeListener() {
        return OvsdbNodeListener.this;
    }

    @Override protected void remove(InstanceIdentifier<Node> identifier, Node ovsdbNode) {
        LOG.info("OvsdbNodeListener called for Ovsdb Node Remove.");

        String strOvsNodeId = ovsdbNode.getNodeId().getValue();
        LOG.info("Node with ID [{}] received to remove.", strOvsNodeId);

        if (ovsNodeToBridgesDpnIdMap.containsKey(strOvsNodeId)) {
            LOG.info("Removed Node [{}] from OvsdbNodeListener map.", strOvsNodeId);
            ovsNodeToBridgesDpnIdMap.remove(strOvsNodeId);
        } else {
            String bridgeName = "";
            OvsdbBridgeAugmentation ovsdbNewBridgeAugmentation =
                ovsdbNode.getAugmentation(OvsdbBridgeAugmentation.class);
            if (ovsdbNewBridgeAugmentation != null) {
                bridgeName = ovsdbNewBridgeAugmentation.getBridgeName().getValue();
                if (bridgeName != null) {
                    LOG.info("Removed Node [{}] from bridges map.", strOvsNodeId);
                    bridgesMap.remove(bridgeName);
                }
            }
        }
    }

    @Override protected void add(InstanceIdentifier<Node> identifier, Node ovsdbNodeNew) {

        String tep_ip = "", tzName = "", dpnBridgeName = "";
        String strDpnId = "";
        OvsdbOtherConfigInfo ovsdbOtherConfigObj = null;

        LOG.trace("OvsdbNodeListener called for Ovsdb Node Add.");

        // check for OVS node
        OvsdbNodeAugmentation ovsdbNewNodeAugmentation =
            ovsdbNodeNew.getAugmentation(OvsdbNodeAugmentation.class);
        if (ovsdbNewNodeAugmentation != null) {
            LOG.trace("OvsdbNodeListener add() called for OvsdbNodeAugmentation.");

            // get OVSDB other_configs list from old ovsdb node
            ovsdbOtherConfigObj = getOvsdbNodeOtherConfigs(ovsdbNodeNew);
            if (ovsdbOtherConfigObj == null) {
                return;
            }
            // store other config required parameters
            tep_ip = ovsdbOtherConfigObj.getTepIp();
            tzName = ovsdbOtherConfigObj.getTzName();
            dpnBridgeName = ovsdbOtherConfigObj.getDpnBrName();
            // it is mandatory, check if it is not received, throw error
            if (!tep_ip.isEmpty()) {
                LOG.trace("Ovs Node [{}] is configured with TEP-IP.",
                    ovsdbNodeNew.getNodeId().getValue());
            } else {
                LOG.trace("Ovs Node [{}] is not configured with TEP-IP.",
                    ovsdbNodeNew.getNodeId().getValue());
            }

            ovsdbOtherConfigObj.setDpnBrName(dpnBridgeName);

            LOG.trace(
                "OpenvswitchOtherConfigs parameters in new Node: TEP-IP: {}, TZ name: {}, DPN Bridge Name: {}",
                tep_ip, tzName, dpnBridgeName);

            String newOvsNode = ovsdbNodeNew.getNodeId().getValue();

            // check if add-bridge has been received earlier
            strDpnId = bridgesMap.get(dpnBridgeName);
            if (strDpnId != null) {
                ovsdbOtherConfigObj.setStrDpnId(strDpnId);
            }
            if (ovsNodeToBridgesDpnIdMap.containsKey(newOvsNode)) {
                LOG.error("New OVS node [{}] already exists.", newOvsNode);
                ovsdbOtherConfigObj = null;
                return;
            } else {
                ovsNodeToBridgesDpnIdMap.put(newOvsNode, ovsdbOtherConfigObj);
                LOG.trace("Added Ovs Node [{}] into OVSDB Listener class map.", newOvsNode);
            }


            if (strDpnId == null) {
                // check if add-br notification for configured bridge has been received earlier,
                // if no then return, TEP will be added when add-br notification for configured
                //                    bridge will be received
                // if yes then add TEP now itself.
                return;
            }
        }
        // check for OVS bridge node
        OvsdbBridgeAugmentation ovsdbNewBridgeAugmentation =
            ovsdbNodeNew.getAugmentation(OvsdbBridgeAugmentation.class);
        if (ovsdbNewBridgeAugmentation != null) {
            dpnBridgeName = ovsdbNewBridgeAugmentation.getBridgeName().getValue();
            LOG.trace("OvsdbNodeListener add() called for OvsdbBridgeAugmentation of bridge: {}.",
                dpnBridgeName);

            // store it into map
            String updatedOvsNode = ovsdbNodeNew.getNodeId().getValue();
            String replaceStr = "/bridge/" + dpnBridgeName;
            updatedOvsNode = updatedOvsNode.replace(replaceStr, "");

            // Read DPN ID from OVSDBBridgeAugmentation
            strDpnId = ItmListenerUtils.getStrDatapathId(ovsdbNodeNew);
            if (strDpnId == null) {
                strDpnId = "";
            }

            if (strDpnId.isEmpty()) {
                LOG.error("OvsdbBridgeAugmentation ADD: DPID for bridge {} is NULL.",
                    dpnBridgeName);
                return;
            }

            if (ovsNodeToBridgesDpnIdMap.containsKey(updatedOvsNode)) {
                ovsdbOtherConfigObj = ovsNodeToBridgesDpnIdMap.get(updatedOvsNode);
                if (ovsdbOtherConfigObj != null) {
                    if (ovsdbOtherConfigObj.getDpnBrName().equals(dpnBridgeName)) {
                        LOG.trace(
                            "OvsdbBridgeAugmentation ADD: Received bridge (Bridge name: {}, datapath ID: {}) would be used for Tunneling.",
                            dpnBridgeName, strDpnId);
                        ovsdbOtherConfigObj.setStrDpnId(strDpnId);
                        // update OvsdbOtherConfigObj into map
                        ovsNodeToBridgesDpnIdMap.put(updatedOvsNode, ovsdbOtherConfigObj);
                    } else {
                        LOG.trace(
                            "OvsdbBridgeAugmentation ADD: Received bridge (Bridge name: {},  datapath ID: {}) is NOT configured for Tunneling.",
                            dpnBridgeName, strDpnId);
                        return;
                    }
                } else {
                    LOG.error("Ovsdb Node [{}] details map does not exist.", updatedOvsNode);
                    return;
                }
            } else {
                LOG.trace(
                    "Ovs Node [{}] should exist. Bridge Add notification is received earlier than OVS node add.",
                    updatedOvsNode);
                bridgesMap.put(dpnBridgeName, strDpnId);
                return;
            }
        }

        // store other config required parameters
        tep_ip = ovsdbOtherConfigObj.getTepIp();
        tzName = ovsdbOtherConfigObj.getTzName();
        dpnBridgeName = ovsdbOtherConfigObj.getDpnBrName();

        LOG.trace("TEP-IP: {}, TZ name: {}, DPN Bridge Name: {}, Bridge DPID: {}", tep_ip,
            tzName, dpnBridgeName, strDpnId);

        // add TEP received from southbound OVSDB into ITM config DS.
        addTepReceivedFromOvsdb(tep_ip, strDpnId, tzName);
    }

    @Override protected void update(InstanceIdentifier<Node> identifier, Node ovsdbNodeOld,
        Node ovsdbNodeNew) {

        String tep_ip = "", oldTepIp = "", tzName = "", oldTzName = "", oldDpnBridgeName = "",
            newDpnBridgeName = "";
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
            tep_ip = newOtherConfigsInfoObj.getTepIp();
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
            isTepIpRemoved = isTepIpRemoved(oldTepIp, tep_ip);
            isTepIpAdded = isTepIpAdded(oldTepIp, tep_ip);

            if (!oldTepIp.equals(tep_ip)) {
                isOtherConfigUpdated = true;
            }
            if (!oldTzName.equals(tzName)) {
                isOtherConfigUpdated = true;
                if (!oldTepIp.isEmpty() && !tep_ip.isEmpty()) {
                    isTzChanged = true;
                }
            }
            if (!oldDpnBridgeName.equals(newDpnBridgeName)) {
                isOtherConfigUpdated = true;
                if (!oldTepIp.isEmpty() && !tep_ip.isEmpty()) {
                    isDpnBrChanged = true;
                }
            }

            if (!isOtherConfigUpdated) {
                LOG.trace("No updates in the other config parameters. Nothing to do.");
                return;
            }
        }

        String ovsNewNode = ovsdbNodeNew.getNodeId().getValue();
        String strOldDpnId = "", strNewDpnId = "";

        if (isOtherConfigDeleted || isTepIpRemoved || isTzChanged || isDpnBrChanged) {
            strOldDpnId = getDpnIdForBridge(ovsNewNode, oldDpnBridgeName);
        }
        if (isTepIpAdded || isTzChanged || isDpnBrChanged) {
            strNewDpnId = getDpnIdForBridge(ovsNewNode, newDpnBridgeName);
        }

        // handle TEP-remove in remove case, TZ change case, Bridge change case
        if (isOtherConfigDeleted || isTepIpRemoved || isTzChanged || isDpnBrChanged) {
            if (strOldDpnId == null || strOldDpnId.isEmpty()) {
                LOG.error(
                    "Update case of TEP-Delete: DPN-ID for bridge {} is not available. Nothing to do.",
                    oldDpnBridgeName);
                return;
            }
            // remove TEP
            LOG.info(
                "Update case: Removing TEP-IP: {}, TZ name: {}, DPN Bridge Name: {}, Bridge DPID: {}",
                oldTepIp, oldTzName, oldDpnBridgeName, strOldDpnId);
            removeTepReceivedFromOvsdb(oldTepIp, strOldDpnId, oldTzName);
        }
        // handle TEP-add in add case, TZ change case, Bridge change case
        if (isTepIpAdded || isTzChanged || isDpnBrChanged) {
            if (strNewDpnId == null || strNewDpnId.isEmpty()) {
                LOG.error(
                    "Update case of TEP-Add: DPN-ID for bridge {} is not available. Nothing to do.",
                    newDpnBridgeName);
                return;
            }
            LOG.info(
                "Update case: Adding TEP-IP: {}, TZ name: {}, DPN Bridge Name: {}, Bridge DPID: {}",
                tep_ip, tzName, newDpnBridgeName, strNewDpnId);
            // add TEP into new TZ
            addTepReceivedFromOvsdb(tep_ip, strNewDpnId, tzName);
        }
    }

    public String getDpnIdForBridge(String ovsNewNode, String brName) {
        if (ovsNodeToBridgesDpnIdMap.containsKey(ovsNewNode)) {
            OvsdbOtherConfigInfo ovsdbOtherConfigInfo = ovsNodeToBridgesDpnIdMap.get(ovsNewNode);
            if (ovsdbOtherConfigInfo != null) {
                if (brName.isEmpty()) {
                    brName = ITMConstants.defaultBridgeName;
                }

                if (ovsdbOtherConfigInfo.getDpnBrName().equals(brName)) {
                    return ovsdbOtherConfigInfo.getStrDpnId();
                } else {
                    LOG.error(
                        "Bridge ({}) specified in Other-configs should be pre-configured on the OVS node.",
                        brName);
                }
            }
        } else {
            LOG.error("Ovsdb Node received for update does not exist in ovsNodeToBridgesDpnIdMap.");
        }
        return null;
    }

    public boolean isTepIpRemoved(String oldTepIp, String tep_ip) {
        if (!oldTepIp.isEmpty() && tep_ip.isEmpty()) {
            return true;
        }
        return false;
    }

    public boolean isTepIpAdded(String oldTepIp, String tep_ip) {
        if (oldTepIp.isEmpty() && !tep_ip.isEmpty()) {
            return true;
        }
        return false;
    }

    public void removeTepReceivedFromOvsdb(String tep_ip, String strDpnId, String tzName) {
        BigInteger dpnId = BigInteger.valueOf(0);

        LOG.trace("TEP-IP: {}, TZ name: {}, DPN ID: {}", tep_ip, tzName, strDpnId);

        if (strDpnId != null && !strDpnId.isEmpty()) {
            dpnId = new BigInteger(strDpnId.replaceAll(":", ""), 16);
        }

        // Get tep IP
        IpAddress tepIpAddress = new IpAddress(tep_ip.toCharArray());
        TransportZone tZone = null;

        // Case: TZ name is not given with CSS TEP.
        if (tzName.isEmpty()) {
            tzName = ITMConstants.DEFAULT_TRANSPORT_ZONE;
            // add TEP into default-TZ
            tZone = ItmListenerUtils.getTransportZoneFromITMConfigDS(tzName, dataBroker);
            if (tZone == null) {
                LOG.error("Error: default-transport-zone is not yet created.");
                return;
            }
            LOG.info("Remove TEP into default-transport-zone.");
        } else {
            // Case: Add TEP into corresponding TZ created from Northbound.
            tZone = ItmListenerUtils.getTransportZoneFromITMConfigDS(tzName, dataBroker);
            if (tZone == null) {
                // Case: TZ is not configured from Northbound, then add TEP into "teps-not-hosted-in-transport-zone"
                LOG.info("Removing TEP from unknown TZ into teps-not-hosted-in-transport-zone.");
                ItmListenerUtils
                    .removeUnknownTzTepFromTepsNotHosted(tzName, tepIpAddress, dpnId, dataBroker);
                return;
            } else {
                LOG.info("Remove TEP from transport-zone already configured by Northbound.");
            }
        }

        // Remove TEP from (default transport-zone) OR (transport-zone already configured by Northbound)

        // Get subnet list of corresponding TZ created from Northbound.
        List<Subnets> subnetList = tZone.getSubnets();

        if (subnetList == null || subnetList.isEmpty()) {
            LOG.trace("No subnet list in transport-zone. Nothing to do.");
        } else {
            String portName = "";
            IpPrefix subnetMaskObj = ItmListenerUtils.getDummySubnet();

            List<Vteps> vtepList = null;

            // subnet list already exists case; check for dummy-subnet
            for (Subnets subnet : subnetList) {
                if (subnet.getKey().getPrefix().equals(subnetMaskObj)) {
                    LOG.trace("Subnet exists in the subnet list of transport-zone {}.", tzName);
                    // get vtep list of existing subnet
                    vtepList = subnet.getVteps();
                    break;
                }
            }

            if (vtepList == null || vtepList.isEmpty()) {
                //  case: vtep list does not exist or it has no elements
                LOG.trace("No vtep list in subnet list of transport-zone. Nothing to do.");
            } else {
                //  case: vtep list has elements
                boolean vtepFound = false;
                Vteps oldVtep = null;

                for (Vteps vtep : vtepList) {
                    if (vtep.getDpnId().equals(dpnId)) {
                        vtepFound = true;
                        oldVtep = vtep;
                        // get portName of existing vtep
                        portName = vtep.getPortname();
                        break;
                    }
                }
                if (vtepFound) {
                    // vtep is found, update it with tep-ip
                    LOG.info("Remove TEP from vtep list in subnet list of transport-zone.");
                    dpnId = oldVtep.getDpnId();
                    portName = oldVtep.getPortname();
                    removeVtepInITMConfigDS(subnetMaskObj, tzName, dpnId, portName);
                } else {
                    LOG.info(
                        "TEP is not found in the vtep list in subnet list of transport-zone. Nothing to do.");
                }
            }
        }
    }

    public void addTepReceivedFromOvsdb(String tep_ip, String strDpnId, String tzName) {
        BigInteger dpnId = BigInteger.valueOf(0);

        LOG.trace("TEP-IP: {}, TZ name: {}, DPN ID: {}", tep_ip, tzName, strDpnId);

        if (strDpnId != null && !strDpnId.isEmpty()) {
            dpnId = new BigInteger(strDpnId.replaceAll(":", ""), 16);
        }

        // Get tep IP
        IpAddress tepIpAddress = new IpAddress(tep_ip.toCharArray());
        TransportZone tZone = null;

        // Case: TZ name is not given with CSS TEP.
        if (tzName.isEmpty()) {
            tzName = ITMConstants.DEFAULT_TRANSPORT_ZONE;
            // add TEP into default-TZ
            tZone = ItmListenerUtils.getTransportZoneFromITMConfigDS(tzName, dataBroker);
            if (tZone == null) {
                LOG.error("Error: default-transport-zone is not yet created.");
                return;
            }
            LOG.info("Add TEP into default-transport-zone.");
        } else {
            // Case: Add TEP into corresponding TZ created from Northbound.
            tZone = ItmListenerUtils.getTransportZoneFromITMConfigDS(tzName, dataBroker);
            if (tZone == null) {
                // Case: TZ is not configured from Northbound, then add TEP into "teps-not-hosted-in-transport-zone"
                LOG.info("Adding TEP with unknown TZ into teps-not-hosted-in-transport-zone.");
                addUnknownTzTepIntoTepsNotHosted(tzName, tepIpAddress, dpnId);
                return;
            } else {
                LOG.info("Add TEP into transport-zone already configured by Northbound.");
            }
        }

        // Get subnet list of corresponding TZ created from Northbound.
        List<Subnets> subnetList = tZone.getSubnets();
        String portName = "";

        IpPrefix subnetMaskObj = ItmListenerUtils.getDummySubnet();

        if (subnetList == null || subnetList.isEmpty()) {
            if (subnetList == null) {
                subnetList = new ArrayList<>();
            }
            List<Vteps> vtepList = new ArrayList<>();
            LOG.trace("Add TEP in transport-zone when no subnet-list.");
            addVtepInITMConfigDS(subnetList, subnetMaskObj, vtepList, tepIpAddress, tzName, dpnId,
                portName);
        } else {
            List<Vteps> vtepList = null;

            // subnet list already exists case; check for dummy-subnet
            for (Subnets subnet : subnetList) {
                if (subnet.getKey().getPrefix().equals(subnetMaskObj)) {
                    LOG.trace("Subnet exists in the subnet list of transport-zone {}.", tzName);
                    // get vtep list of existing subnet
                    vtepList = subnet.getVteps();
                    break;
                }
            }

            if (vtepList == null || vtepList.isEmpty()) {
                //  case: vtep list does not exist or it has no elements
                if (vtepList == null) {
                    vtepList = new ArrayList<>();
                }
                LOG.trace("Add TEP in transport-zone when no vtep-list for specific subnet.");
                addVtepInITMConfigDS(subnetList, subnetMaskObj, vtepList, tepIpAddress, tzName,
                    dpnId, portName);
            } else {
                //  case: vtep list has elements
                boolean vtepFound = false;
                Vteps oldVtep = null;

                for (Vteps vtep : vtepList) {
                    if (vtep.getDpnId().equals(dpnId)) {
                        vtepFound = true;
                        oldVtep = vtep;
                        // get portName of existing vtep
                        portName = vtep.getPortname();
                        break;
                    }
                }
                if (!vtepFound) {
                    addVtepInITMConfigDS(subnetList, subnetMaskObj, vtepList, tepIpAddress, tzName,
                        dpnId, portName);
                } else {
                    // vtep is found, update it with tep-ip
                    vtepList.remove(oldVtep);
                    addVtepInITMConfigDS(subnetList, subnetMaskObj, vtepList, tepIpAddress, tzName,
                        dpnId, portName);
                }
            }
        }
    }

    public OvsdbOtherConfigInfo getOvsdbNodeOtherConfigs(Node ovsdbNode) {
        String tep_ip = "", tzName = "", dpnBridgeName = "";

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
            LOG.error(
                "OvsdbOtherConfigInfo class object could not be created. System fatal error.");
            return null;
        }

        if (ovsdbNodeOtherConfigsList != null) {
            for (OpenvswitchOtherConfigs otherConfig : ovsdbNodeOtherConfigsList) {
                if (otherConfig.getOtherConfigKey().equals("tep-ip")) {
                    tep_ip = otherConfig.getOtherConfigValue();
                    otherConfigInfoObj.setTepIp(tep_ip);
                    LOG.trace("tep-ip: {}", tep_ip);
                } else if (otherConfig.getOtherConfigKey().equals("tzname")) {
                    tzName = otherConfig.getOtherConfigValue();
                    otherConfigInfoObj.setTzName(tzName);
                    LOG.trace("tzname: {}", tzName);
                } else if (otherConfig.getOtherConfigKey().equals("dpn-br-name")) {
                    dpnBridgeName = otherConfig.getOtherConfigValue();
                    otherConfigInfoObj.setDpnBrName(dpnBridgeName);
                    LOG.trace("dpn-br-name: {}", dpnBridgeName);
                } else {
                    LOG.trace("other_config {}:{}", otherConfig.getOtherConfigKey(),
                        otherConfig.getOtherConfigValue());
                }
            }
        }

        return otherConfigInfoObj;
    }

    public void addUnknownTzTepIntoTepsNotHosted(String tzName, IpAddress tepIpAddress,
        BigInteger dpnId) {
        List<UnknownVteps> vtepList = null;

        TepsNotHostedInTransportZone unknownTz =
            ItmListenerUtils.getUnknownTransportZoneFromITMConfigDS(tzName, dataBroker);
        if (unknownTz == null) {
            LOG.trace("Unknown TransportZone does not exist.");
            vtepList = new ArrayList<>();
            ItmListenerUtils
                .addVtepIntoTepsNotHosted(vtepList, tepIpAddress, tzName, dpnId, dataBroker);
        } else {
            vtepList = unknownTz.getUnknownVteps();
            if (vtepList == null || vtepList.isEmpty()) {
                //  case: vtep list does not exist or it has no elements
                if (vtepList == null) {
                    vtepList = new ArrayList<>();
                }
                LOG.trace("Add TEP in unknown TZ ({}) when no vtep-list in the TZ.", tzName);
                ItmListenerUtils
                    .addVtepIntoTepsNotHosted(vtepList, tepIpAddress, tzName, dpnId, dataBroker);
            } else {
                //  case: vtep list has elements
                boolean vtepFound = false;
                UnknownVteps oldVtep = null;

                for (UnknownVteps vtep : vtepList) {
                    if (vtep.getDpnId().equals(dpnId)) {
                        vtepFound = true;
                        oldVtep = vtep;
                        break;
                    }
                }
                if (!vtepFound) {
                    ItmListenerUtils.addVtepIntoTepsNotHosted(vtepList, tepIpAddress, tzName, dpnId,
                        dataBroker);
                } else {
                    // vtep is found, update it with tep-ip
                    vtepList.remove(oldVtep);
                    ItmListenerUtils.addVtepIntoTepsNotHosted(vtepList, tepIpAddress, tzName, dpnId,
                        dataBroker);
                }
            }
        }
    }

    public void removeVtepInITMConfigDS(IpPrefix subnetMaskObj, String tzName, BigInteger dpnId,
        String portName) {
        SubnetsKey subnetsKey = new SubnetsKey(subnetMaskObj);
        VtepsKey vtepkey = new VtepsKey(dpnId, portName);

        InstanceIdentifier<Vteps> vTepPath = InstanceIdentifier.builder(TransportZones.class)
            .child(TransportZone.class, new TransportZoneKey(tzName))
            .child(Subnets.class, subnetsKey).child(Vteps.class, vtepkey).build();

        LOG.info("Removing TEP (TZ: {} Subnet: {} DPN-ID: {}) in ITM Config DS.", tzName,
            subnetMaskObj.getValue(), dpnId);
        // remove vtep
        ItmUtils.asyncDelete(LogicalDatastoreType.CONFIGURATION, vTepPath, dataBroker,
            ItmUtils.DEFAULT_CALLBACK);
    }

    public void addVtepInITMConfigDS(List<Subnets> subnetList, IpPrefix subnetMaskObj,
        List<Vteps> updatedVtepList, IpAddress tepIpAddress, String tzName, BigInteger dpnId,
        String portName) {
        //Create TZ node path
        InstanceIdentifier<TransportZone> tZonepath =
            InstanceIdentifier.builder(TransportZones.class)
                .child(TransportZone.class, new TransportZoneKey(tzName)).build();

        // create vtep
        VtepsKey vtepkey = new VtepsKey(dpnId, portName);
        Vteps vtepObj =
            new VtepsBuilder().setDpnId(dpnId).setIpAddress(tepIpAddress).setKey(vtepkey)
                .setPortname(portName).build();

        // Add vtep obtained from DPN into list
        updatedVtepList.add(vtepObj);

        // Create subnet object
        SubnetsKey subKey = new SubnetsKey(subnetMaskObj);
        IpAddress gatewayIP = new IpAddress(ITMConstants.dummyGateway.toCharArray());
        int vlanID = ITMConstants.dummyVlanID;

        Subnets subnet =
            new SubnetsBuilder().setGatewayIp(gatewayIP)
                .setKey(subKey).setPrefix(subnetMaskObj)
                .setVlanId(vlanID).setVteps(updatedVtepList).build();

        // add subnet into subnet list
        subnetList.add(subnet);

        // create TZ node with updated subnet having new vtep
        TransportZone updatedTzone =
            new TransportZoneBuilder().setKey(new TransportZoneKey(tzName)).setSubnets(subnetList)
                .setZoneName(tzName).build();

        LOG.info("Adding TEP (TZ: {} Subnet: {} TEP IP: {} DPN-ID: {}) in ITM Config DS.", tzName,
            subnetMaskObj.getValue(), tepIpAddress, dpnId);
        // Update TZ in Config DS to add vtep in TZ
        ItmUtils
            .asyncUpdate(LogicalDatastoreType.CONFIGURATION, tZonepath, updatedTzone, dataBroker,
                ItmUtils.DEFAULT_CALLBACK);
    }
    // End of class
}
