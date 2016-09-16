/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.listeners;

import com.google.common.base.Optional;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.tepsnothostedintransportzone.UnknownVteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.tepsnothostedintransportzone.UnknownVtepsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.tepsnothostedintransportzone.UnknownVtepsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.Subnets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.SubnetsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.SubnetsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.Vteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.VtepsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.VtepsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItmListenerUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ItmListenerUtils.class);
    public static final TopologyId OVSDB_TOPOLOGY_ID = new TopologyId(new Uri("ovsdb:1"));

    public static String getBridgeFromConfig(Node node, String bridge, DataBroker dataBroker) {
        OvsdbBridgeAugmentation ovsdbBridgeAugmentation = null;
        Node bridgeNode = null;
        String datapathId = null;

        NodeId ovsdbNodeId = node.getKey().getNodeId();

        NodeId brNodeId = new NodeId(ovsdbNodeId.getValue()
            + "/" + ITMConstants.BRIDGE_URI_PREFIX + "/" + bridge);

        InstanceIdentifier<Node> bridgeIid =
            InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(OVSDB_TOPOLOGY_ID))
                .child(Node.class,new NodeKey(brNodeId));

        Optional<Node> opBridgeNode = ItmUtils.read(LogicalDatastoreType.OPERATIONAL, bridgeIid, dataBroker);

        if (opBridgeNode != null) {
            bridgeNode = opBridgeNode.get();
        }
        if (bridgeNode != null) {
            ovsdbBridgeAugmentation = bridgeNode.getAugmentation(OvsdbBridgeAugmentation.class);
        }

        if (ovsdbBridgeAugmentation != null && ovsdbBridgeAugmentation.getDatapathId() != null) {
            datapathId = ovsdbBridgeAugmentation.getDatapathId().getValue();
        }
        return datapathId;
    }

    public static TransportZone getTransportZoneFromITMConfigDS(String tzone,
        DataBroker dataBroker) {
        InstanceIdentifier<TransportZone> tzonePath =
            InstanceIdentifier.builder(TransportZones.class)
                .child(TransportZone.class, new TransportZoneKey(tzone)).build();
        Optional<TransportZone> tZoneOptional =
            ItmUtils.read(LogicalDatastoreType.CONFIGURATION, tzonePath, dataBroker);
        if (tZoneOptional.isPresent()) {
            return tZoneOptional.get();
        }
        return null;
    }

    public static TepsNotHostedInTransportZone getUnknownTransportZoneFromITMConfigDS(
        String unknownTz, DataBroker dataBroker) {
        InstanceIdentifier<TepsNotHostedInTransportZone> unknownTzPath =
            InstanceIdentifier.builder(TransportZones.class)
                .child(TepsNotHostedInTransportZone.class,
                    new TepsNotHostedInTransportZoneKey(unknownTz)).build();
        Optional<TepsNotHostedInTransportZone> unknownTzOptional =
            ItmUtils.read(LogicalDatastoreType.CONFIGURATION, unknownTzPath, dataBroker);
        if (unknownTzOptional.isPresent()) {
            return unknownTzOptional.get();
        }
        return null;
    }


    public static void removeTepReceivedFromOvsdb(String tepIp, String strDpnId, String tzName,
        DataBroker dataBroker, WriteTransaction wrTx) {
        BigInteger dpnId = BigInteger.valueOf(0);

        LOG.trace("Remove TEP: TEP-IP: {}, TZ name: {}, DPN ID: {}", tepIp, tzName, strDpnId);

        if (strDpnId != null && !strDpnId.isEmpty()) {
            dpnId = new BigInteger(strDpnId.replaceAll(":", ""), 16);
        }

        // Get tep IP
        IpAddress tepIpAddress = new IpAddress(tepIp.toCharArray());
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
            LOG.trace("Remove TEP from default-transport-zone.");
        } else {
            // Case: Add TEP into corresponding TZ created from Northbound.
            tZone = ItmListenerUtils.getTransportZoneFromITMConfigDS(tzName, dataBroker);
            if (tZone == null) {
                // Case: TZ is not configured from Northbound, then add TEP into
                // "teps-not-hosted-in-transport-zone"
                LOG.trace("Removing TEP from unknown TZ into teps-not-hosted-in-transport-zone.");
                ItmListenerUtils
                    .removeUnknownTzTepFromTepsNotHosted(tzName, tepIpAddress, dpnId, dataBroker,
                        wrTx);
                return;
            } else {
                LOG.trace("Remove TEP from transport-zone already configured by Northbound.");
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
                    LOG.trace("Remove TEP from vtep list in subnet list of transport-zone.");
                    dpnId = oldVtep.getDpnId();
                    portName = oldVtep.getPortname();
                    removeVtepInITMConfigDS(subnetMaskObj, tzName, dpnId, portName, wrTx);
                } else {
                    LOG.trace(
                        "TEP is not found in the vtep list in subnet list of transport-zone. Nothing to do.");
                }
            }
        }
    }

    public static void addTepReceivedFromOvsdb(String tepIp, String strDpnId, String tzName,
        DataBroker dataBroker, WriteTransaction wrTx) {
        BigInteger dpnId = BigInteger.valueOf(0);

        if (strDpnId != null && !strDpnId.isEmpty()) {
            dpnId = new BigInteger(strDpnId.replaceAll(":", ""), 16);
        }

        // Get tep IP
        IpAddress tepIpAddress = new IpAddress(tepIp.toCharArray());
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
            LOG.trace("Add TEP into default-transport-zone.");
        } else {
            // Case: Add TEP into corresponding TZ created from Northbound.
            tZone = ItmListenerUtils.getTransportZoneFromITMConfigDS(tzName, dataBroker);
            if (tZone == null) {
                // Case: TZ is not configured from Northbound, then add TEP into "teps-not-hosted-in-transport-zone"
                LOG.trace("Adding TEP with unknown TZ into teps-not-hosted-in-transport-zone.");
                addUnknownTzTepIntoTepsNotHosted(tzName, tepIpAddress, dpnId, dataBroker, wrTx);
                return;
            } else {
                LOG.trace("Add TEP into transport-zone already configured by Northbound.");
            }
        }

        // Get subnet list of corresponding TZ created from Northbound.
        List<Subnets> subnetList = tZone.getSubnets();
        String portName = "";

        IpPrefix subnetMaskObj = ItmListenerUtils.getDummySubnet();

        if (subnetList == null || subnetList.isEmpty()) {
            if (subnetList == null) {
                subnetList = new ArrayList<Subnets>();
            }
            List<Vteps> vtepList = new ArrayList<Vteps>();
            LOG.trace("Add TEP in transport-zone when no subnet-list.");
            addVtepInITMConfigDS(subnetList, subnetMaskObj, vtepList, tepIpAddress, tzName, dpnId,
                portName, wrTx);
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
                    vtepList = new ArrayList<Vteps>();
                }
                LOG.trace("Add TEP in transport-zone when no vtep-list for specific subnet.");
                addVtepInITMConfigDS(subnetList, subnetMaskObj, vtepList, tepIpAddress, tzName,
                    dpnId, portName, wrTx);
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
                        dpnId, portName, wrTx);
                } else {
                    // vtep is found, update it with tep-ip
                    vtepList.remove(oldVtep);
                    addVtepInITMConfigDS(subnetList, subnetMaskObj, vtepList, tepIpAddress, tzName,
                        dpnId, portName, wrTx);
                }
            }
        }
    }

    public static void removeVtepInITMConfigDS(IpPrefix subnetMaskObj, String tzName, BigInteger dpnId,
        String portName, WriteTransaction wrTx) {
        SubnetsKey subnetsKey = new SubnetsKey(subnetMaskObj);
        VtepsKey vtepkey = new VtepsKey(dpnId, portName);

        InstanceIdentifier<Vteps> vTepPath = InstanceIdentifier.builder(TransportZones.class)
            .child(TransportZone.class, new TransportZoneKey(tzName))
            .child(Subnets.class, subnetsKey).child(Vteps.class, vtepkey).build();

        LOG.trace("Removing TEP (TZ: {} Subnet: {} DPN-ID: {}) in ITM Config DS.", tzName,
            subnetMaskObj.getValue().toString(), dpnId);
        // remove vtep
        wrTx.delete(LogicalDatastoreType.CONFIGURATION, vTepPath);
    }

    public static void addVtepInITMConfigDS(List<Subnets> subnetList, IpPrefix subnetMaskObj,
        List<Vteps> updatedVtepList, IpAddress tepIpAddress, String tzName, BigInteger dpnId,
        String portName, WriteTransaction wrTx) {
        //Create TZ node path
        InstanceIdentifier<TransportZone> tZonepath =
            InstanceIdentifier.builder(TransportZones.class)
                .child(TransportZone.class, new TransportZoneKey(tzName)).build();

        // this check is needed to reuse same function from TransportZoneListener
        // when VTEP is moved from TepsNotHosted list to TZ configured from Northbound.
        if (dpnId.compareTo(BigInteger.ZERO) > 0) {
            // create vtep
            VtepsKey vtepkey = new VtepsKey(dpnId, portName);
            Vteps vtepObj =
                new VtepsBuilder().setDpnId(dpnId).setIpAddress(tepIpAddress).setKey(vtepkey).setPortname(portName).build();

            // Add vtep obtained from DPN into list
            updatedVtepList.add(vtepObj);
        }

        // Create subnet object
        SubnetsKey subKey = new SubnetsKey(subnetMaskObj);
        IpAddress gatewayIP = new IpAddress(ITMConstants.DUMMY_GATEWAY_IP.toCharArray());
        int vlanID = ITMConstants.DUMMY_VLANID;

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

        LOG.trace("Adding TEP (TZ: {} Subnet: {} TEP IP: {} DPN-ID: {}) in ITM Config DS.", tzName,
            subnetMaskObj.getValue().toString(), tepIpAddress, dpnId);
        // Update TZ in Config DS to add vtep in TZ
        wrTx.merge(LogicalDatastoreType.CONFIGURATION, tZonepath, updatedTzone, true);
    }

    public static void addUnknownTzTepIntoTepsNotHosted(String tzName, IpAddress tepIpAddress,
        BigInteger dpnId, DataBroker dataBroker, WriteTransaction wrTx) {
        List<UnknownVteps> vtepList = null;

        TepsNotHostedInTransportZone unknownTz =
            ItmListenerUtils.getUnknownTransportZoneFromITMConfigDS(tzName, dataBroker);
        if (unknownTz == null) {
            LOG.trace("Unknown TransportZone does not exist.");
            vtepList = new ArrayList<UnknownVteps>();
            ItmListenerUtils
                .addVtepIntoTepsNotHosted(vtepList, tepIpAddress, tzName, dpnId, wrTx);
        } else {
            vtepList = unknownTz.getUnknownVteps();
            if (vtepList == null || vtepList.isEmpty()) {
                //  case: vtep list does not exist or it has no elements
                if (vtepList == null) {
                    vtepList = new ArrayList<UnknownVteps>();
                }
                LOG.trace("Add TEP in unknown TZ ({}) when no vtep-list in the TZ.", tzName);
                ItmListenerUtils
                    .addVtepIntoTepsNotHosted(vtepList, tepIpAddress, tzName, dpnId, wrTx);
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
                        wrTx);
                } else {
                    // vtep is found, update it with tep-ip
                    vtepList.remove(oldVtep);
                    ItmListenerUtils.addVtepIntoTepsNotHosted(vtepList, tepIpAddress, tzName, dpnId,
                        wrTx);
                }
            }
        }
    }

    public static void removeUnknownTzTepFromTepsNotHosted(String tzName, IpAddress tepIpAddress,
        BigInteger dpnId, DataBroker dataBroker, WriteTransaction wrTx) {
        List<UnknownVteps> vtepList = null;

        TepsNotHostedInTransportZone unknownTz =
            ItmListenerUtils.getUnknownTransportZoneFromITMConfigDS(tzName, dataBroker);
        if (unknownTz == null) {
            LOG.trace("Unknown TransportZone does not exist. Nothing to do for TEP removal.");
            return;
        } else {
            vtepList = unknownTz.getUnknownVteps();
            if (vtepList == null || vtepList.isEmpty()) {
                //  case: vtep list does not exist or it has no elements
                LOG.trace(
                    "Remove TEP in unknown TZ ({}) when no vtep-list in the TZ. Nothing to do.",
                    tzName);
            } else {
                //  case: vtep list has elements
                boolean vtepFound = false;
                UnknownVteps foundVtep = null;

                for (UnknownVteps vtep : vtepList) {
                    if (vtep.getDpnId().equals(dpnId)) {
                        vtepFound = true;
                        foundVtep = vtep;
                        break;
                    }
                }
                if (vtepFound) {
                    // vtep is found, update it with tep-ip
                    LOG.trace(
                        "Remove TEP with IP ({}) from unknown TZ ({}) in TepsNotHosted list.",
                        tepIpAddress, tzName);
                    if (vtepList.size() == 1) {
                        removeTzFromTepsNotHosted(tzName, wrTx);
                    } else {
                        removeVtepFromTepsNotHosted(tzName, dpnId, wrTx);
                    }
                    vtepList.remove(foundVtep);
                }
            }
        }
    }

    public static void addVtepIntoTepsNotHosted(List<UnknownVteps> updatedVtepList,
        IpAddress tepIpAddress, String tzName, BigInteger dpnId, WriteTransaction wrTx) {
        //Create TZ node path
        InstanceIdentifier<TepsNotHostedInTransportZone> tZonepath =
            InstanceIdentifier.builder(TransportZones.class)
                .child(TepsNotHostedInTransportZone.class,
                    new TepsNotHostedInTransportZoneKey(tzName)).build();

        // create vtep
        UnknownVtepsKey vtepkey = new UnknownVtepsKey(dpnId);
        UnknownVteps vtepObj =
            new UnknownVtepsBuilder().setDpnId(dpnId).setIpAddress(tepIpAddress).setKey(vtepkey)
                .build();

        // Add vtep obtained into unknown TZ tep list
        updatedVtepList.add(vtepObj);

        // create unknown TZ node with updated vtep list
        TepsNotHostedInTransportZone updatedTzone = new TepsNotHostedInTransportZoneBuilder()
            .setKey(new TepsNotHostedInTransportZoneKey(tzName)).setZoneName(tzName)
            .setUnknownVteps(updatedVtepList).build();

        LOG.trace("Adding TEP into unknown (TZ: {}, DPID: {}, TEP IP: {}) in ITM Config DS.",
            tzName, dpnId, tepIpAddress);

        // Update TZ in Config DS.
        wrTx.merge(LogicalDatastoreType.CONFIGURATION, tZonepath, updatedTzone, true);
    }

    public static void removeVtepFromTepsNotHosted(String tzName, BigInteger dpnId,
        WriteTransaction wrTx) {

        UnknownVtepsKey unknownVtepkey = new UnknownVtepsKey(dpnId);
        InstanceIdentifier<UnknownVteps> vTepPath = InstanceIdentifier.builder(TransportZones.class)
            .child(TepsNotHostedInTransportZone.class, new TepsNotHostedInTransportZoneKey(tzName))
            .child(UnknownVteps.class, unknownVtepkey).build();

        LOG.trace("Removing TEP from unknown (TZ: {}, DPID: {}) from ITM Config DS.", tzName,
            dpnId);
        // remove vtep
        wrTx.delete(LogicalDatastoreType.CONFIGURATION, vTepPath);
    }

    public static void removeTzFromTepsNotHosted(String tzName, WriteTransaction wrTx) {
        InstanceIdentifier<TepsNotHostedInTransportZone> tzTepsNotHostedTepPath =
            InstanceIdentifier.builder(TransportZones.class)
                .child(TepsNotHostedInTransportZone.class,
                    new TepsNotHostedInTransportZoneKey(tzName)).build();

        LOG.trace("Removing TZ ({})from TepsNotHosted list  from ITM Config DS.", tzName);
        // remove TZ from TepsNotHosted list
        wrTx.delete(LogicalDatastoreType.CONFIGURATION, tzTepsNotHostedTepPath);
    }

    public static IpPrefix getDummySubnet() {
        IpPrefix subnetMaskObj = null;

        // Get subnet prefix
        try {
            subnetMaskObj = new IpPrefix(ITMConstants.DUMMY_PREFIX.toCharArray());
        } catch (Exception e) {
            LOG.error("Invalid Subnet Mask. Expected: 0.0.0.0/0 to 255.255.255.255/32");
            return subnetMaskObj;
        }
        return subnetMaskObj;
    }
}
