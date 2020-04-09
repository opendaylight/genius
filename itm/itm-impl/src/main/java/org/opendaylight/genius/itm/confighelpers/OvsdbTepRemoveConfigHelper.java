/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.confighelpers;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.opendaylight.genius.infra.Datastore;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.TypedWriteTransaction;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.NotHostedTransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.not.hosted.transport.zones.TepsInNotHostedTransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.not.hosted.transport.zones.TepsInNotHostedTransportZoneKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.not.hosted.transport.zones.tepsinnothostedtransportzone.UnknownVteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.not.hosted.transport.zones.tepsinnothostedtransportzone.UnknownVtepsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.Vteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.VtepsKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OvsdbTepRemoveConfigHelper {

    private static final Logger LOG = LoggerFactory.getLogger(OvsdbTepRemoveConfigHelper.class);

    private OvsdbTepRemoveConfigHelper() {

    }

    /**
     * Removes the TEP from ITM configuration/operational Datastore in one of the following cases.
     * 1) default transport zone
     * 2) Configured transport zone
     * 3) Unhosted transport zone
     * Function checks for above three cases and calls other sub-function to remove the TEP
     *
     * @param tepIp TEP-IP address in string
     * @param strDpnId bridge datapath ID in string
     * @param tzName transport zone name in string
     * @param dataBroker data broker handle to perform operations on config/operational datastore
     * @param txRunner ManagedNewTransactionRunner object
     */

    public static List<ListenableFuture<Void>> removeTepReceivedFromOvsdb(String tepIp, String strDpnId, String tzName,
                                                                          DataBroker dataBroker,
                                                                          ManagedNewTransactionRunner txRunner) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        Uint64 dpnId = Uint64.ZERO;
        LOG.trace("Remove TEP: TEP-IP: {}, TZ name: {}, DPID: {}", tepIp, tzName, strDpnId);

        if (strDpnId != null && !strDpnId.isEmpty()) {
            dpnId = MDSALUtil.getDpnId(strDpnId);
        }
        // Get tep IP
        IpAddress tepIpAddress = IpAddressBuilder.getDefaultInstance(tepIp);
        TransportZone transportZone;

        // Case: TZ name is not given from OVS's other_config parameters.
        if (tzName == null) {
            tzName = ITMConstants.DEFAULT_TRANSPORT_ZONE;
            // add TEP into default-TZ
            transportZone = ItmUtils.getTransportZoneFromConfigDS(tzName, dataBroker);
            if (transportZone == null) {
                LOG.error("Error: default-transport-zone is not yet created.");
                return futures;
            }
            LOG.trace("Remove TEP from default-transport-zone.");
        } else {
            // Case: Add TEP into corresponding TZ created from Northbound.
            transportZone = ItmUtils.getTransportZoneFromConfigDS(tzName, dataBroker);
            String name = tzName;
            Uint64 id = dpnId;
            if (transportZone == null) {
                // Case: TZ is not configured from Northbound, then add TEP into
                // "teps-in-not-hosted-transport-zone"
                LOG.trace("Removing TEP from teps-in-not-hosted-transport-zone list.");
                futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(Datastore.OPERATIONAL,
                    tx -> removeUnknownTzTepFromTepsNotHosted(name, tepIpAddress, id, dataBroker, tx)));
                return futures;
            } else {
                LOG.trace("Remove TEP from transport-zone already configured by Northbound.");
            }
        }

        // Remove TEP from (default transport-zone) OR (transport-zone already configured by Northbound)

        List<Vteps> vtepList = transportZone.getVteps();
        if (vtepList == null || vtepList.isEmpty()) {
            //  case: vtep list does not exist or it has no elements
            LOG.trace("No vtep list in subnet list of transport-zone. Nothing to do.");
        } else {
            //  case: vtep list has elements
            boolean vtepFound = false;
            Vteps oldVtep = null;

            for (Vteps vtep : vtepList) {
                if (Objects.equals(vtep.getDpnId(), dpnId)) {
                    vtepFound = true;
                    oldVtep = vtep;
                    break;
                }
            }
            if (vtepFound) {
                // vtep is found, update it with tep-ip
                LOG.trace("Remove TEP from vtep list in subnet list of transport-zone.");
                dpnId = oldVtep.getDpnId();
                String name = tzName;
                Uint64 id = dpnId;
                futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(Datastore.CONFIGURATION,
                    tx -> removeVtepFromTZConfig(name, id, tx)));
            } else {
                LOG.trace(
                        "TEP is not found in the vtep list in subnet list of transport-zone. Nothing to do.");
            }
        }
        return futures;
    }

    /**
     * Removes the TEP from subnet list in the transport zone list
     * from ITM configuration Datastore by delete operation with write transaction.
     *
     * @param dpnId bridge datapath ID
     * @param tzName transport zone name in string
     * @param tx TypedWriteTransaction object
     */
    private static void removeVtepFromTZConfig(String tzName, Uint64 dpnId,
                                               TypedWriteTransaction<Datastore.Configuration> tx) {

        VtepsKey vtepkey = new VtepsKey(dpnId);

        InstanceIdentifier<Vteps> vtepPath = InstanceIdentifier.builder(TransportZones.class)
                .child(TransportZone.class, new TransportZoneKey(tzName))
                .child(Vteps.class, vtepkey).build();

        LOG.trace("Removing TEP from (TZ: {} DPN-ID: {}) inside ITM Config DS.",
                tzName, dpnId);
        // remove vtep
        tx.delete(vtepPath);
    }

    /**
     * Removes the TEP from the not-hosted transport zone in the TepsNotHosted list
     * from ITM Operational Datastore.
     *
     * @param tzName transport zone name in string
     * @param tepIpAddress TEP IP address in IpAddress object
     * @param dpnId bridge datapath ID
     * @param dataBroker data broker handle to perform operations on operational datastore
     * @param tx TypedWriteTransaction object
     */
    public static void removeUnknownTzTepFromTepsNotHosted(String tzName, IpAddress tepIpAddress,
                                                           Uint64 dpnId, DataBroker dataBroker,
                                                           TypedWriteTransaction<Datastore.Operational> tx) {
        List<UnknownVteps> vtepList;
        TepsInNotHostedTransportZone tepsInNotHostedTransportZone =
                ItmUtils.getUnknownTransportZoneFromITMOperDS(tzName, dataBroker);
        if (tepsInNotHostedTransportZone == null) {
            LOG.trace("Unhosted TransportZone ({}) does not exist in OperDS. Nothing to do for TEP removal.", tzName);
            return;
        } else {
            vtepList = tepsInNotHostedTransportZone.getUnknownVteps();
            if (vtepList == null || vtepList.isEmpty()) {
                //  case: vtep list does not exist or it has no elements
                LOG.trace("Remove TEP from unhosted TZ ({}) when no vtep-list in the TZ. Nothing to do.", tzName);
            } else {
                //  case: vtep list has elements
                boolean vtepFound = false;

                for (UnknownVteps vtep : vtepList) {
                    if (Objects.equals(vtep.getDpnId(), dpnId)) {
                        vtepFound = true;
                        break;
                    }
                }
                if (vtepFound) {
                    // vtep is found, update it with tep-ip
                    LOG.trace(
                            "Remove TEP with IP ({}) from unhosted TZ ({}) inside not-hosted-transport-zones list.",
                            tepIpAddress, tzName);
                    if (vtepList.size() == 1) {
                        removeTzFromTepsNotHosted(tzName, tx);
                    } else {
                        removeVtepFromTepsNotHosted(tzName, dpnId, tx);
                    }
                }
            }
        }
    }

    /**
     * Removes the TEP from unknown vtep list under the transport zone in the TepsNotHosted list
     * from ITM operational Datastore by delete operation with write transaction.
     *
     * @param tzName transport zone name in string
     * @param dpnId bridge datapath ID
     * @param tx TypedWriteTransaction object
     */
    private static void removeVtepFromTepsNotHosted(String tzName, Uint64 dpnId,
                                                    TypedWriteTransaction<Datastore.Operational> tx) {
        InstanceIdentifier<UnknownVteps> vtepPath = InstanceIdentifier.builder(NotHostedTransportZones.class)
                .child(TepsInNotHostedTransportZone.class, new TepsInNotHostedTransportZoneKey(tzName))
                .child(UnknownVteps.class, new UnknownVtepsKey(dpnId)).build();
        LOG.trace("Removing TEP from unhosted (TZ: {}, DPID: {}) inside ITM Oper DS.", tzName, dpnId);
        tx.delete(vtepPath);
    }

    /**
     * Removes the transport zone in the TepsNotHosted list
     * from ITM operational Datastore by delete operation with write transaction.
     *
     * @param tzName transport zone name in string
     * @param tx TypedWriteTransaction object
     */
    private static void removeTzFromTepsNotHosted(String tzName, TypedWriteTransaction<Datastore.Operational> tx) {
        InstanceIdentifier<TepsInNotHostedTransportZone> tepsInNotHostedTransportZoneIid =
                InstanceIdentifier.builder(NotHostedTransportZones.class).child(TepsInNotHostedTransportZone.class,
                        new TepsInNotHostedTransportZoneKey(tzName)).build();
        LOG.trace("Removing TZ ({})from not-hosted-transport-zones list inside ITM Oper DS.", tzName);
        tx.delete(tepsInNotHostedTransportZoneIid);
    }
}
