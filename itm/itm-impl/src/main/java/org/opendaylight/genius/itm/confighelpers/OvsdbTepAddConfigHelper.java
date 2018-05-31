/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.confighelpers;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.NotHostedTransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.not.hosted.transport.zones.TepsInNotHostedTransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.not.hosted.transport.zones.TepsInNotHostedTransportZoneBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.not.hosted.transport.zones.TepsInNotHostedTransportZoneKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.not.hosted.transport.zones.tepsinnothostedtransportzone.UnknownVteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.not.hosted.transport.zones.tepsinnothostedtransportzone.UnknownVtepsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.not.hosted.transport.zones.tepsinnothostedtransportzone.UnknownVtepsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.Subnets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.SubnetsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.SubnetsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.Vteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.VtepsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.VtepsKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OvsdbTepAddConfigHelper {

    private static final Logger LOG = LoggerFactory.getLogger(OvsdbTepAddConfigHelper.class);

    private OvsdbTepAddConfigHelper() { }

    /**
     * Adds the TEP into ITM configuration/operational Datastore in one of the following cases.
     * 1) default transport zone
     * 2) Configured transport zone
     * 3) Unhosted transport zone
     *
     * @param tepIp TEP-IP address in string
     * @param strDpnId bridge datapath ID in string
     * @param tzName transport zone name in string
     * @param ofTunnel boolean flag for TEP to enable/disable of-tunnel feature on it
     * @param dataBroker data broker handle to perform operations on config/operational datastore
     * @param wrTx WriteTransaction object
     */

    public static void addTepReceivedFromOvsdb(String tepIp, String strDpnId, String tzName,
                                               boolean ofTunnel, DataBroker dataBroker, WriteTransaction wrTx) {
        BigInteger dpnId = BigInteger.valueOf(0);

        if (strDpnId != null && !strDpnId.isEmpty()) {
            dpnId = MDSALUtil.getDpnId(strDpnId);
        }

        // Get tep IP
        IpAddress tepIpAddress = IpAddressBuilder.getDefaultInstance(tepIp);
        TransportZone tzone = null;

        // Case: TZ name is not given with OVS TEP.
        if (tzName == null) {
            tzName = ITMConstants.DEFAULT_TRANSPORT_ZONE;
            // add TEP into default-TZ
            tzone = ItmUtils.getTransportZoneFromConfigDS(tzName, dataBroker);
            if (tzone == null) {
                LOG.error("Error: default-transport-zone is not yet created.");
                return;
            }
            LOG.trace("Add TEP into default-transport-zone.");
        } else {
            // Case: Add TEP into corresponding TZ created from Northbound.
            tzone = ItmUtils.getTransportZoneFromConfigDS(tzName, dataBroker);
            if (tzone == null) {
                // Case: TZ is not configured from Northbound, then add TEP into "teps-in-not-hosted-transport-zone"
                LOG.trace("Adding TEP with unknown TZ into teps-in-not-hosted-transport-zone.");
                addUnknownTzTepIntoTepsNotHosted(tzName, tepIpAddress, dpnId, ofTunnel,
                    dataBroker, wrTx);
                return;
            } else {
                LOG.trace("Add TEP into transport-zone already configured by Northbound.");
            }
        }

        // Get subnet list of corresponding TZ created from Northbound.
        List<Subnets> subnetList = tzone.getSubnets();
        String portName = ITMConstants.DUMMY_PORT;

        IpPrefix subnetMaskObj = ItmUtils.getDummySubnet();

        if (subnetList == null || subnetList.isEmpty()) {
            if (subnetList == null) {
                subnetList = new ArrayList<>();
            }
            List<Vteps> vtepList = new ArrayList<>();
            LOG.trace("Add TEP in transport-zone when no subnet-list.");
            addVtepInITMConfigDS(subnetList, subnetMaskObj, vtepList, tepIpAddress, tzName, dpnId,
                portName, ofTunnel, wrTx);
        } else {
            List<Vteps> vtepList = null;

            // subnet list already exists case; check for dummy-subnet
            for (Subnets subnet : subnetList) {
                if (subnet.key().getPrefix().equals(subnetMaskObj)) {
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
                    dpnId, portName, ofTunnel, wrTx);
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
                        dpnId, portName, ofTunnel, wrTx);
                } else {
                    // vtep is found, update it with tep-ip
                    vtepList.remove(oldVtep);
                    addVtepInITMConfigDS(subnetList, subnetMaskObj, vtepList, tepIpAddress, tzName,
                        dpnId, portName, ofTunnel, wrTx);
                }
            }
        }
    }

    /**
     * Adds the TEP into Vtep list in the subnet list in the transport zone list
     * from ITM configuration Datastore by merge operation with write transaction.
     *
     * @param subnetList subnets list object
     * @param subnetMaskObj subnet mask in IpPrefix object
     * @param updatedVtepList updated Vteps list object which will have new TEP for addition
     * @param tepIpAddress TEP IP address in IpAddress object
     * @param tzName transport zone name in string
     * @param dpid bridge datapath ID in BigInteger
     * @param portName port name as a part of VtepsKey
     * @param ofTunnel boolean flag for TEP to enable/disable of-tunnel feature on it
     * @param wrTx WriteTransaction object
     */
    public static void addVtepInITMConfigDS(List<Subnets> subnetList, IpPrefix subnetMaskObj,
        List<Vteps> updatedVtepList, IpAddress tepIpAddress, String tzName, BigInteger dpid,
        String portName, boolean ofTunnel, WriteTransaction wrTx) {
        //Create TZ node path
        InstanceIdentifier<TransportZone> tranzportZonePath =
            InstanceIdentifier.builder(TransportZones.class)
                .child(TransportZone.class, new TransportZoneKey(tzName)).build();

        // this check is needed to reuse same function from TransportZoneListener
        // when VTEP is moved from TepsNotHosted list to TZ configured from Northbound.
        if (dpid.compareTo(BigInteger.ZERO) > 0) {
            // create vtep
            VtepsKey vtepkey = new VtepsKey(dpid, portName);
            Vteps vtepObj =
                new VtepsBuilder().setDpnId(dpid).setIpAddress(tepIpAddress).withKey(vtepkey).setPortname(portName)
                        .setOptionOfTunnel(ofTunnel).build();

            // Add vtep obtained from bridge into list
            updatedVtepList.add(vtepObj);

            LOG.trace("Adding TEP (TZ: {} Subnet: {} TEP IP: {} DPID: {}, of-tunnel: {}) in ITM Config DS.", tzName,
                    subnetMaskObj, tepIpAddress, dpid, ofTunnel);
        } else {
            // this is case when this function is called while TEPs movement from tepsNotHosted list when
            // corresponding TZ is configured from northbound.
            for (Vteps vtep: updatedVtepList) {
                LOG.trace("Moving TEP (TEP IP: {} DPID: {}, of-tunnel: {})"
                        + "from not-hosted-transport-zone {} into  ITM Config DS.",
                    vtep.getIpAddress(), vtep.getDpnId(), ofTunnel, tzName);
            }
        }

        // Create subnet object
        SubnetsKey subKey = new SubnetsKey(subnetMaskObj);
        IpAddress gatewayIP = IpAddressBuilder.getDefaultInstance(ITMConstants.DUMMY_GATEWAY_IP);
        int vlanID = ITMConstants.DUMMY_VLANID;

        Subnets subnet =
            new SubnetsBuilder().setGatewayIp(gatewayIP)
                .withKey(subKey).setPrefix(subnetMaskObj)
                .setVlanId(vlanID).setVteps(updatedVtepList).build();

        // add subnet into subnet list
        subnetList.add(subnet);

        // create TZ node with updated subnet having new vtep
        TransportZone updatedTzone =
            new TransportZoneBuilder().withKey(new TransportZoneKey(tzName)).setSubnets(subnetList)
                .setZoneName(tzName).build();

        // Update TZ in Config DS to add vtep in TZ
        wrTx.merge(LogicalDatastoreType.CONFIGURATION, tranzportZonePath, updatedTzone, true);
    }

    /**
     * Adds the TEP into Vtep list in the subnet list in the transport zone list
     * from ITM operational Datastore by merge operation with write transaction.
     *
     * @param tzName transport zone name in string
     * @param tepIpAddress TEP IP address in IpAddress object
     * @param dpid bridge datapath ID in BigInteger
     * @param ofTunnel boolean flag for TEP to enable/disable of-tunnel feature on it
     * @param dataBroker data broker handle to perform operations on operational datastore
     * @param wrTx WriteTransaction object
     */
    protected static void addUnknownTzTepIntoTepsNotHosted(String tzName, IpAddress tepIpAddress,
        BigInteger dpid, boolean ofTunnel, DataBroker dataBroker, WriteTransaction wrTx) {
        List<UnknownVteps> vtepList = null;

        TepsInNotHostedTransportZone tepsInNotHostedTransportZone =
            ItmUtils.getUnknownTransportZoneFromITMOperDS(tzName, dataBroker);
        if (tepsInNotHostedTransportZone == null) {
            LOG.trace("Unhosted TransportZone ({}) does not exist in OperDS.", tzName);
            vtepList = new ArrayList<>();
            addVtepIntoTepsNotHosted(vtepList, tepIpAddress, tzName, dpid, ofTunnel, wrTx);
        } else {
            vtepList = tepsInNotHostedTransportZone.getUnknownVteps();
            if (vtepList == null || vtepList.isEmpty()) {
                //  case: vtep list does not exist or it has no elements
                if (vtepList == null) {
                    vtepList = new ArrayList<>();
                }
                LOG.trace("Add TEP into unhosted TZ ({}) when no vtep-list in the TZ.", tzName);
                addVtepIntoTepsNotHosted(vtepList, tepIpAddress, tzName, dpid, ofTunnel, wrTx);
            } else {
                //  case: vtep list has elements
                boolean vtepFound = false;
                UnknownVteps oldVtep = null;

                for (UnknownVteps vtep : vtepList) {
                    if (vtep.getDpnId().equals(dpid)) {
                        vtepFound = true;
                        oldVtep = vtep;
                        break;
                    }
                }
                if (!vtepFound) {
                    addVtepIntoTepsNotHosted(vtepList, tepIpAddress, tzName, dpid,
                        ofTunnel, wrTx);
                } else {
                    // vtep is found, update it with tep-ip
                    vtepList.remove(oldVtep);
                    addVtepIntoTepsNotHosted(vtepList, tepIpAddress, tzName, dpid,
                        ofTunnel, wrTx);
                }
            }
        }
    }

    /**
     * Adds the TEP into Unknown Vtep list under the transport zone in the TepsNotHosted list
     * from ITM operational Datastore by merge operation with write transaction.
     *
     * @param updatedVtepList updated UnknownVteps list object which will have new TEP for addition
     *                        into TepsNotHosted
     * @param tepIpAddress TEP IP address in IpAddress object
     * @param tzName transport zone name in string
     * @param dpid bridge datapath ID in BigInteger
     * @param ofTunnel boolean flag for TEP to enable/disable of-tunnel feature on it
     * @param wrTx WriteTransaction object
     */
    protected static void addVtepIntoTepsNotHosted(List<UnknownVteps> updatedVtepList,
        IpAddress tepIpAddress, String tzName, BigInteger dpid, boolean ofTunnel,
        WriteTransaction wrTx) {
        //Create TZ node path
        InstanceIdentifier<TepsInNotHostedTransportZone> tepsInNotHostedTransportZoneIid =
            InstanceIdentifier.builder(NotHostedTransportZones.class)
                .child(TepsInNotHostedTransportZone.class,
                    new TepsInNotHostedTransportZoneKey(tzName)).build();

        // create vtep
        UnknownVtepsKey vtepkey = new UnknownVtepsKey(dpid);
        UnknownVteps vtepObj =
            new UnknownVtepsBuilder().setDpnId(dpid).setIpAddress(tepIpAddress).withKey(vtepkey)
                .setOfTunnel(ofTunnel).build();

        // Add vtep obtained into unknown TZ tep list
        updatedVtepList.add(vtepObj);

        // create unknown TZ node with updated vtep list
        TepsInNotHostedTransportZone updatedTzone = new TepsInNotHostedTransportZoneBuilder()
            .withKey(new TepsInNotHostedTransportZoneKey(tzName)).setZoneName(tzName)
            .setUnknownVteps(updatedVtepList).build();

        LOG.trace("Adding TEP  (DPID: {}, TEP IP: {}, of-tunnel: {}) into unhosted TZ: {}"
                + "inside ITM Oper DS.", dpid, tepIpAddress, ofTunnel, tzName);

        // Update TZ in Oper DS.
        wrTx.merge(LogicalDatastoreType.OPERATIONAL, tepsInNotHostedTransportZoneIid, updatedTzone, true);
    }
}
