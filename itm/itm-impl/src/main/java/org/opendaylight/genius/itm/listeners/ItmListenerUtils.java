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
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.tepsnothostedintransportzone.UnknownVteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.tepsnothostedintransportzone.UnknownVtepsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.tepsnothostedintransportzone.UnknownVtepsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItmListenerUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ItmListenerUtils.class);

    public static String getStrDatapathId(Node node) {
        OvsdbBridgeAugmentation ovsdbBridgeAugmentation =
            node.getAugmentation(OvsdbBridgeAugmentation.class);
        String datapathId = null;
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

    public static void removeUnknownTzTepFromTepsNotHosted(String tzName, IpAddress tepIpAddress,
        BigInteger dpnId, DataBroker dataBroker) {
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

                for (UnknownVteps vtep : vtepList) {
                    if (vtep.getDpnId().equals(dpnId)) {
                        vtepFound = true;
                        break;
                    }
                }
                if (vtepFound) {
                    // vtep is found, update it with tep-ip
                    LOG.trace(
                        "Remove TEP with IP ({}) in unknown TZ ({}) as new TEP into vtep-list in the TZ.",
                        tepIpAddress, tzName);
                    if (vtepList.size() == 1) {
                        removeVtepFromTepsNotHosted(tzName, dpnId, dataBroker);
                        removeTzFromTepsNotHosted(tzName, dataBroker);
                    } else {
                        removeVtepFromTepsNotHosted(tzName, dpnId, dataBroker);
                    }
                }
            }
        }
    }

    public static void addVtepIntoTepsNotHosted(List<UnknownVteps> updatedVtepList,
        IpAddress tepIpAddress, String tzName, BigInteger dpnId, DataBroker dataBroker) {
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
        ItmUtils
            .asyncUpdate(LogicalDatastoreType.CONFIGURATION, tZonepath, updatedTzone, dataBroker,
                ItmUtils.DEFAULT_CALLBACK);
    }

    public static void removeVtepFromTepsNotHosted(String tzName, BigInteger dpnId,
        DataBroker dataBroker) {

        UnknownVtepsKey unknownVtepkey = new UnknownVtepsKey(dpnId);
        InstanceIdentifier<UnknownVteps> vTepPath = InstanceIdentifier.builder(TransportZones.class)
            .child(TepsNotHostedInTransportZone.class, new TepsNotHostedInTransportZoneKey(tzName))
            .child(UnknownVteps.class, unknownVtepkey).build();

        LOG.trace("Removing TEP from unknown (TZ: {}, DPID: {}) from ITM Config DS.", tzName,
            dpnId);
        // remove vtep
        ItmUtils.asyncDelete(LogicalDatastoreType.CONFIGURATION, vTepPath, dataBroker,
            ItmUtils.DEFAULT_CALLBACK);
    }

    public static void removeTzFromTepsNotHosted(String tzName, DataBroker dataBroker) {
        InstanceIdentifier<TepsNotHostedInTransportZone> tzTepsNotHostedTepPath =
            InstanceIdentifier.builder(TransportZones.class)
                .child(TepsNotHostedInTransportZone.class,
                    new TepsNotHostedInTransportZoneKey(tzName)).build();

        LOG.trace("Removing TZ ({})from TepsNotHosted list  from ITM Config DS.", tzName);
        // remove TZ from TepsNotHosted list
        ItmUtils.asyncDelete(LogicalDatastoreType.CONFIGURATION, tzTepsNotHostedTepPath, dataBroker,
            ItmUtils.DEFAULT_CALLBACK);
    }

    public static IpPrefix getDummySubnet() {
        IpPrefix subnetMaskObj = null;

        // Get subnet prefix
        try {
            subnetMaskObj = new IpPrefix(ITMConstants.dummyPrefix.toCharArray());
        } catch (Exception e) {
            LOG.error("Invalid Subnet Mask. Expected: 0.0.0.0/0 to 255.255.255.255/32");
            return subnetMaskObj;
        }
        return subnetMaskObj;
    }
}
