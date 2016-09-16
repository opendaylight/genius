/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.listeners;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.datastoreutils.AsyncDataTreeChangeListenerBase;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.itm.cli.SubnetObject;

import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.globals.ITMConstants;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;

import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZonesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.tepsnothostedintransportzone.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.Subnets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.SubnetsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.SubnetsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.DeviceVteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.Vteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.VtepsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.VtepsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntry;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens for OvsdbNode creation/removal/update in Network Topology Operational DS.
 * This is used to handle TEPs of switches.
 */
public class OvsdbNodeListener extends AsyncDataTreeChangeListenerBase<Node, OvsdbNodeListener> implements AutoCloseable{
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbNodeListener.class);
    private DataBroker dataBroker;
    private Map<String, Map<String, String>> ovsNodeToBridgesDpnIdMap;

    public OvsdbNodeListener(final DataBroker dataBroker) {
        super(Node.class, OvsdbNodeListener.class);
        this.dataBroker = dataBroker;
        this.ovsNodeToBridgesDpnIdMap = new HashMap<>();
        LOG.info("OvsdbNodeListener Created");
    }

    @Override
    public void close() throws Exception {
        for (String ovsdbNode : ovsNodeToBridgesDpnIdMap.keySet()) {
            Map<String, String> innerMap = ovsNodeToBridgesDpnIdMap.get(ovsdbNode);
            innerMap.clear();
        }

        // clear the map
        ovsNodeToBridgesDpnIdMap.clear();

        LOG.info("OvsdbNodeListener Closed");
    }

    @Override
    protected InstanceIdentifier<Node> getWildCardPath() {
        return InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class)
                .child(Node.class).build();
    }

    @Override
    protected OvsdbNodeListener getDataTreeChangeListener() {
        return OvsdbNodeListener.this;
    }

    @Override
    protected void remove(InstanceIdentifier<Node> identifier, Node ovsdbNode) {
        LOG.info("OvsdbNodeListener called for Ovsdb Node Remove.");

        String strOvsNodeId = ovsdbNode.getNodeId().getValue();
        LOG.info("Node with ID [{}] received to remove.", strOvsNodeId);

        if (ovsNodeToBridgesDpnIdMap.containsKey(strOvsNodeId)) {
            Map<String, String> otherConfigsMap = ovsNodeToBridgesDpnIdMap.get(strOvsNodeId);
            if (otherConfigsMap != null) {
                otherConfigsMap.clear();
            }
            ovsNodeToBridgesDpnIdMap.remove(strOvsNodeId);
        }
    }

    @Override
    protected void add(InstanceIdentifier<Node> identifier, Node ovsdbNodeNew) {

        String tep_ip="", tzName="", dpnBridgeName="", dummyPrefix="";
        String strDpnId = "";

        LOG.info("OvsdbNodeListener called for Ovsdb Node Add.");

        // check for OVS node
        OvsdbNodeAugmentation ovsdbNewNodeAugmentation = ovsdbNodeNew.getAugmentation(OvsdbNodeAugmentation.class);
        if (ovsdbNewNodeAugmentation != null) {
            LOG.info("OvsdbNodeListener add() called for OvsdbNodeAugmentation.");

            // get OVSDB other_configs list from old ovsdb node
            Map<String, String> newOtherConfigsMap = getOvsdbNodeOtherConfigs(ovsdbNodeNew);
            if (newOtherConfigsMap == null) {
                return;
            }
            // store other config required parameters
            tep_ip = newOtherConfigsMap.get("tep-ip");
            tzName = newOtherConfigsMap.get("tzname");
            dpnBridgeName = newOtherConfigsMap.get("dpn-br-name");
            // it is mandatory, check if it is not received, throw error
            if (!tep_ip.isEmpty()) {
                LOG.info("Ovs Node [{}] is not configured with TEP-IP.", ovsdbNodeNew.getNodeId().getValue());

                // if br-name is not received, take br-int by default
                if (dpnBridgeName.isEmpty()) {
                    dpnBridgeName = "br-int";
                    newOtherConfigsMap.put("dpn-br-name", dpnBridgeName);
                }
            } else {
                LOG.info("Ovs Node [{}] is not configured with TEP-IP.", ovsdbNodeNew.getNodeId().getValue());
            }

            LOG.info("OpenvswitchOtherConfigs parameters: TEP-IP: {}, TZ name: {}, DPN Bridge Name: {}",
                    tep_ip, tzName, dpnBridgeName);

            String newOvsNode = ovsdbNodeNew.getNodeId().getValue();
            if (ovsNodeToBridgesDpnIdMap.containsKey(newOvsNode)) {
                LOG.error("New OVS node [{}] already exists.", newOvsNode);
            } else {
                ovsNodeToBridgesDpnIdMap.put(newOvsNode, newOtherConfigsMap);
                LOG.info("Added Ovs Node [{}] as key and OtherConfigsMap as value.", newOvsNode);
            }

            newOtherConfigsMap.clear();
            return;
        }
        // check for OVS bridge node
        OvsdbBridgeAugmentation ovsdbNewBridgeAugmentation = ovsdbNodeNew.getAugmentation(OvsdbBridgeAugmentation.class);
        if (ovsdbNewBridgeAugmentation != null) {

            Map<String, String> bridgeDpnMap = null;
            dpnBridgeName = ovsdbNewBridgeAugmentation.getBridgeName().getValue();
            LOG.info("OvsdbNodeListener add() called for OvsdbBridgeAugmentation of bridge: {}.", dpnBridgeName);

            // store it into map
            String updatedOvsNode = ovsdbNodeNew.getNodeId().getValue();
            String replaceStr = "/bridge/" + dpnBridgeName;
            updatedOvsNode = updatedOvsNode.replace(replaceStr, "");
            if (ovsNodeToBridgesDpnIdMap.containsKey(updatedOvsNode)) {
                bridgeDpnMap = ovsNodeToBridgesDpnIdMap.get(updatedOvsNode);
                if (bridgeDpnMap != null) {
                    // Read DPN ID from OVSDBBridgeAugmentation
                    strDpnId = ItmListenerUtils.getStrDatapathId(ovsdbNodeNew);
                    bridgeDpnMap.put(dpnBridgeName, strDpnId);
                    if (bridgeDpnMap.get("dpn-br-name") == dpnBridgeName) {
                        LOG.info("OvsdbBridgeAugmentation ADD: Received bridge (Bridge name: {}, datapath ID: {}) would be used for Tunneling.", dpnBridgeName, strDpnId);
                    } else {
                        LOG.info("OvsdbBridgeAugmentation ADD: Received bridge (Bridge name: {}) is NOT configured for Tunneling.", dpnBridgeName);
                        return;
                    }
                } else {
                    LOG.error("Ovsdb Node [{}] details map does not exist.", updatedOvsNode);
                    return;
                }
            } else {
                LOG.error("Ovs Node [{}] must exist.", updatedOvsNode);
                return;
            }

            // store other config required parameters
            tep_ip = bridgeDpnMap.get("tep-ip");
            tzName = bridgeDpnMap.get("tzname");
            dpnBridgeName = bridgeDpnMap.get("dpn-br-name");

            LOG.info("TEP-IP: {}, TZ name: {}, DPN Bridge Name: {}, Bridge DPID: {}",
                    tep_ip, tzName, dpnBridgeName, strDpnId);

            // add TEP received from southbound OVSDB into ITM config DS.
            addTepReceivedFromOvsdb(tep_ip, strDpnId, tzName);
        }
    }

    @Override
    protected void update(InstanceIdentifier<Node> identifier, Node ovsdbNodeOld, Node ovsdbNodeNew) {

        String tep_ip="", tzName="", odlTzName="", dpnBridgeName="", dummyPrefix="";
        BigInteger dpnId = BigInteger.valueOf(0);
        String strDpnId = "";

        LOG.info("OvsdbNodeListener called for Ovsdb Node Update.");

        // get OVSDB other_configs list from old ovsdb node
        Map<String, String> newOtherConfigsMap = getOvsdbNodeOtherConfigs(ovsdbNodeNew);
        if (newOtherConfigsMap == null) {
            return;
        }

        // get OVSDB other_configs list from new ovsdb node
        Map<String, String> oldOtherConfigsMap = getOvsdbNodeOtherConfigs(ovsdbNodeOld);
        if (oldOtherConfigsMap == null) {
            return;
        }

        // store other config required parameters
        tep_ip = newOtherConfigsMap.get("tep-ip");
        tzName = newOtherConfigsMap.get("tzname");
        odlTzName = oldOtherConfigsMap.get("tzname");
        dpnBridgeName = newOtherConfigsMap.get("dpn-br-name");

        // check oldNode otherconfig is empty.
        boolean isOvsdbNodeOtherConfigEmpty = isOvsdbNodeOtherConfigEmpty(oldOtherConfigsMap);
        boolean isOtherConfigUpdated = false, isTepIpChanged = false, isTzNameChanged = false;
        if (isOvsdbNodeOtherConfigEmpty) {
            LOG.info("Old Ovsdb node other-configs is empty.");
        } else {
            // check for updates in the otherConfig
            if (oldOtherConfigsMap.get("tep-ip") != tep_ip) {
                isOtherConfigUpdated = true;
                isTepIpChanged = true;
            } else if (oldOtherConfigsMap.get("tzname") != tzName) {

                isOtherConfigUpdated = true;
                isTzNameChanged = true;
            } else {
                isOtherConfigUpdated = false;
            }
        }
        // All map params has been obtained, now clear it up.
        newOtherConfigsMap.clear();
        oldOtherConfigsMap.clear();

        if (!isOvsdbNodeOtherConfigEmpty && !isOtherConfigUpdated) {
            LOG.info("No updates in the other config parameters. Nothing to do.");
            return;
        }

        String ovsNewNode = ovsdbNodeNew.getNodeId().getValue();
        String ovsOldNode = ovsdbNodeOld.getNodeId().getValue();
        LOG.info("ovsdb update new NodeId: {}", ovsNewNode);
        LOG.info("ovsdb update old NodeId: {}", ovsOldNode);

        if (ovsNodeToBridgesDpnIdMap.containsKey(ovsNewNode)) {
            Map <String, String> bridgeDpnMap = ovsNodeToBridgesDpnIdMap.get(ovsNewNode);
            if (bridgeDpnMap != null) {
                if (bridgeDpnMap.containsKey(dpnBridgeName)) {
                    strDpnId = bridgeDpnMap.get(dpnBridgeName);
                } else {
                    LOG.error("Bridge specified in Other-configs should be pre-configured on the OVS node.");
                    return;
                }
            }
        } else {
            LOG.error("Ovsdb Node received for update does not exist in ovsNodeToBridgesDpnIdMap.");
            return;
        }

        LOG.info("TEP-IP: {}, TZ name: {}, DPN Bridge Name: {}, Bridge DPID: {}",
                tep_ip, tzName, dpnBridgeName, strDpnId);

        // Handle the TZ name change case
        if (isTzNameChanged) {
            // remove TEP from old TZ
            removeTepReceivedFromOvsdb(tep_ip, strDpnId, tzName);

            // add TEP into new TZ
            addTepReceivedFromOvsdb(tep_ip, strDpnId, tzName);

            return;
        }

        // Handle the TEP-IP change case
        addTepReceivedFromOvsdb(tep_ip, strDpnId, tzName);
    }

    public void removeTepReceivedFromOvsdb(String tep_ip, String strDpnId, String tzName) {
        BigInteger dpnId = BigInteger.valueOf(0);
        String dummyPrefix="";
        boolean addRemoveFlag = false;

        LOG.info("TEP-IP: {}, TZ name: {}, DPN ID: {}", tep_ip, tzName, dpnId);

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
                ItmListenerUtils.removeUnknownTzTepIntoTepsNotHosted(tzName, tepIpAddress, dpnId, dataBroker);
                return;
            } else {
                LOG.info("Remove TEP from transport-zone already configured by Northbound.");
            }
        }

        // Remove TEP from (default transport-zone) OR (transport-zone already configured by Northbound)

        // Get subnet list of corresponding TZ created from Northbound.
        List<Subnets> subnetList = tZone.getSubnets();

        if (subnetList == null || subnetList.isEmpty()) {
            LOG.info("No subnet list in transport-zone. Nothing to do.");
        } else {
            String portName = "";
            IpPrefix subnetMaskObj = ItmListenerUtils.getDummySubnet();

            List<Vteps> vtepList = null;
            // subnet list already exists case; check for dummy-subnet
            if (subnetList.contains(subnetMaskObj)) {
                // Case when subnet of TEP exists in the subnet list
                int subnetIndex = subnetList.indexOf(subnetMaskObj);
                // get vtep list of existing subnet
                vtepList = subnetList.get(subnetIndex).getVteps();
            }

            if (vtepList == null || vtepList.isEmpty()) {
                //  case: vtep list does not exist or it has no elements
                LOG.info("No vtep list in subnet list of transport-zone. Nothing to do.");
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
                    vtepList.remove(oldVtep);
                    LOG.info("Remove TEP from vtep list in subnet list of transport-zone.");
                    addRemoveVtepInITMConfigDS(subnetList, subnetMaskObj, vtepList, tepIpAddress, tzName, dpnId, portName, addRemoveFlag);
                } else {
                    LOG.info("TEP is not found in the vtep list in subnet list of transport-zone. Nothing to do.");
                }
            }
        }
    }

    public void addTepReceivedFromOvsdb(String tep_ip, String strDpnId, String tzName) {
        BigInteger dpnId = BigInteger.valueOf(0);
        String dummyPrefix="";
        boolean addRemoveFlag = true;

        LOG.info("TEP-IP: {}, TZ name: {}, DPN ID: {}", tep_ip, tzName, dpnId);

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
            LOG.info("Add TEP in transport-zone when no subnet-list.");
            addRemoveVtepInITMConfigDS(subnetList, subnetMaskObj, vtepList, tepIpAddress, tzName, dpnId, portName, addRemoveFlag);
        } else {
            List<Vteps> vtepList = null;
            // subnet list already exists case; check for dummy-subnet
            if (subnetList.contains(subnetMaskObj)) {
                // Case when subnet of TEP exists in the subnet list
                int subnetIndex = subnetList.indexOf(subnetMaskObj);
                // get vtep list of existing subnet
                vtepList = subnetList.get(subnetIndex).getVteps();
            }

            if (vtepList == null || vtepList.isEmpty()) {
                //  case: vtep list does not exist or it has no elements
                if (vtepList == null) {
                    vtepList = new ArrayList<>();
                }
                LOG.info("Add TEP in transport-zone when no vtep-list for specific subnet.");
                addRemoveVtepInITMConfigDS(subnetList, subnetMaskObj, vtepList, tepIpAddress, tzName, dpnId, portName, addRemoveFlag);
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
                    LOG.info("Add TEP in transport-zone as new TEP into vtep-list for specific subnet.");
                    addRemoveVtepInITMConfigDS(subnetList, subnetMaskObj, vtepList, tepIpAddress, tzName, dpnId, portName, addRemoveFlag);
                } else {
                    // vtep is found, update it with tep-ip
                    vtepList.remove(oldVtep);
                    LOG.info("Add TEP in transport-zone as updated TEP into vtep-list for specific subnet.");
                    addRemoveVtepInITMConfigDS(subnetList, subnetMaskObj, vtepList, tepIpAddress, tzName, dpnId, portName, addRemoveFlag);
                }
            }
        }
    }

    public boolean isOvsdbNodeOtherConfigEmpty(Map<String, String> oldOtherConfigsMap) {
        if (!oldOtherConfigsMap.get("tep-ip").isEmpty()) {
            return false;
        }
        if (!oldOtherConfigsMap.get("tzname").isEmpty()) {
            return false;
        }
        // all required parameters are empty.
        return true;
    }

    public Map<String, String> getOvsdbNodeOtherConfigs(Node ovsdbNode) {
        String tep_ip="", tzName="", dpnBridgeName="", emptyStr="";

        OvsdbNodeAugmentation ovsdbNewNodeAugmentation = ovsdbNode.getAugmentation(OvsdbNodeAugmentation.class);
        if (ovsdbNewNodeAugmentation == null) {
            LOG.warn("OvsdbNodeAugmentation does not exist in the OVSDB node [{}].", ovsdbNode.getNodeId().getValue());
            return null;
        }

        List<OpenvswitchOtherConfigs> ovsdbNodeOtherConfigsList = ovsdbNewNodeAugmentation.getOpenvswitchOtherConfigs();
        if (ovsdbNodeOtherConfigsList == null) {
            LOG.error("Other-configs list does not exist in the OVSDB node [{}].", ovsdbNode.getNodeId().getValue());
            return null;
        }

        Map<String, String> otherConfigsMap =  new HashMap<>();

        if (otherConfigsMap == null) {
            LOG.error("Map could not be created. System fatal error.");
            return null;
        }
        // Set default values in map
        otherConfigsMap.put("tep-ip", emptyStr);
        otherConfigsMap.put("tzname", emptyStr);
        otherConfigsMap.put("dpn-br-name", emptyStr);

        if (ovsdbNodeOtherConfigsList != null) {
            for (OpenvswitchOtherConfigs otherConfig : ovsdbNodeOtherConfigsList) {
                if (otherConfig.getOtherConfigKey().equals("tep-ip")) {
                    tep_ip =  otherConfig.getOtherConfigValue();
                    otherConfigsMap.put("tep-ip", tep_ip);
                    LOG.info("tep-ip: {}", tep_ip);
                } else if (otherConfig.getOtherConfigKey().equals("tzname")) {
                    tzName =  otherConfig.getOtherConfigValue();
                    otherConfigsMap.put("tzname", tzName);
                    LOG.info("tzname: {}", tzName);
                } else if (otherConfig.getOtherConfigKey().equals("dpn-br-name")) {
                    dpnBridgeName =  otherConfig.getOtherConfigValue();
                    otherConfigsMap.put("dpn-br-name", dpnBridgeName);
                    LOG.info("dpn-br-name: {}", dpnBridgeName);
                } else {
                    LOG.trace("other_config {}:{}", otherConfig.getOtherConfigKey(), otherConfig.getOtherConfigValue());
                }
            }
        }

        return otherConfigsMap;
    }

    public void addUnknownTzTepIntoTepsNotHosted(String tzName, IpAddress tepIpAddress, BigInteger dpnId) {
        List<UnknownVteps> vtepList = null;
        boolean addRemoveFlag = true; // TRUE is add case

        TepsNotHostedInTransportZone unknownTz = ItmListenerUtils.getUnknownTransportZoneFromITMConfigDS(tzName, dataBroker);
        if (unknownTz == null) {
            LOG.info("Unknown TransportZone does not exist.");
            vtepList = new ArrayList<>();
            ItmListenerUtils.addRemoveVtepIntoTepsNotHosted(vtepList, tepIpAddress, tzName, dpnId, dataBroker, addRemoveFlag);
        } else {
            vtepList = unknownTz.getUnknownVteps();
            if (vtepList == null || vtepList.isEmpty()) {
                //  case: vtep list does not exist or it has no elements
                if (vtepList == null) {
                    vtepList = new ArrayList<>();
                }
                LOG.info("Add TEP in unknown TZ ({}) when no vtep-list in the TZ.", tzName);
                ItmListenerUtils.addRemoveVtepIntoTepsNotHosted(vtepList, tepIpAddress, tzName, dpnId, dataBroker, addRemoveFlag);
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
                    LOG.info("Add TEP in unknown TZ ({})  as new TEP into vtep-list in the TZ.", tzName);
                    ItmListenerUtils.addRemoveVtepIntoTepsNotHosted(vtepList, tepIpAddress, tzName, dpnId, dataBroker, addRemoveFlag);
                } else {
                    // vtep is found, update it with tep-ip
                    vtepList.remove(oldVtep);
                    LOG.info("Add TEP in unknown TZ ({})  as updated TEP into vtep-list in the TZ.", tzName);
                    ItmListenerUtils.addRemoveVtepIntoTepsNotHosted(vtepList, tepIpAddress, tzName, dpnId, dataBroker, addRemoveFlag);
                }
            }
        }
    }

    public void addRemoveVtepInITMConfigDS(List<Subnets> subnetList, IpPrefix subnetMaskObj,
                                               List<Vteps> updatedVtepList, IpAddress tepIpAddress,
                                               String tzName,
                                               BigInteger dpnId, String portName, boolean addRemoveFlag) {
        //Create TZ node path
        InstanceIdentifier<TransportZone> tZonepath =
                InstanceIdentifier.builder(TransportZones.class)
                        .child(TransportZone.class, new TransportZoneKey(tzName)).build();

        if (addRemoveFlag) {
            // create vtep
            VtepsKey vtepkey = new VtepsKey(dpnId, portName);
            Vteps vtepObj = new VtepsBuilder().setDpnId(dpnId).setIpAddress(tepIpAddress).setKey(vtepkey)
                    .setPortname(portName).build();

            // Add vtep obtained from DPN into list
            updatedVtepList.add(vtepObj);
        }

        // Create subnet object
        SubnetsKey subKey = new SubnetsKey(subnetMaskObj);
        Subnets subnet =
                new SubnetsBuilder()
                        .setKey(subKey).setPrefix(subnetMaskObj)
                        .setVteps(updatedVtepList).build();

        // add subnet into subnet list
        subnetList.add(subnet);

        // create TZ node with updated subnet having new vtep
        TransportZone updatedTzone =
                new TransportZoneBuilder().setKey(new TransportZoneKey(tzName))
                        .setSubnets(subnetList).setZoneName(tzName)
                        .build();

        if (addRemoveFlag) {
            LOG.info("Adding TEP (TZ: {} Subnet: {} TEP IP: {}) in ITM Config DS.", tzName, subnetMaskObj, tepIpAddress);
        } else {
            LOG.info("Removing TEP (TZ: {} Subnet: {} TEP IP: {}) in ITM Config DS.", tzName, subnetMaskObj, tepIpAddress);
        }

        // Update TZ in Config DS.
        ItmUtils.asyncUpdate(LogicalDatastoreType.CONFIGURATION, tZonepath, updatedTzone, dataBroker,
                ItmUtils.DEFAULT_CALLBACK);
    }
    // End of class
}