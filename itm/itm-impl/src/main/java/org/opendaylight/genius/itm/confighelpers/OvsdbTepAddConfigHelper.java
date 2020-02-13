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
import java.util.Collections;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.not.hosted.transport.zones.TepsInNotHostedTransportZoneBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.not.hosted.transport.zones.TepsInNotHostedTransportZoneKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.not.hosted.transport.zones.tepsinnothostedtransportzone.UnknownVteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.not.hosted.transport.zones.tepsinnothostedtransportzone.UnknownVtepsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.not.hosted.transport.zones.tepsinnothostedtransportzone.UnknownVtepsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.Vteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.VtepsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.VtepsKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OvsdbTepAddConfigHelper {

    private static final Logger LOG = LoggerFactory.getLogger(OvsdbTepAddConfigHelper.class);

    private OvsdbTepAddConfigHelper() {

    }

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
     * @param txRunner ManagedTransactionRunner object
     */

    public static List<ListenableFuture<Void>> addTepReceivedFromOvsdb(String tepIp, String strDpnId, String tzName,
                                                                       boolean ofTunnel, DataBroker dataBroker,
                                                                       ManagedNewTransactionRunner txRunner) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        Uint64 dpnId = Uint64.ZERO;

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
                // Case: default-TZ is not yet created, then add TEP into "teps-in-not-hosted-transport-zone"
                LOG.trace("Adding TEP with default TZ into teps-in-not-hosted-transport-zone.");
                return addUnknownTzTepIntoTepsNotHostedAndReturnFutures(tzName, tepIpAddress, dpnId, ofTunnel,
                        dataBroker, txRunner);
            }
            LOG.trace("Add TEP into default-transport-zone.");
        } else {
            // Case: Add TEP into corresponding TZ created from Northbound.
            tzone = ItmUtils.getTransportZoneFromConfigDS(tzName, dataBroker);
            if (tzone == null) {
                // Case: TZ is not configured from Northbound, then add TEP into "teps-in-not-hosted-transport-zone"
                LOG.trace("Adding TEP with unknown TZ into teps-in-not-hosted-transport-zone.");
                return addUnknownTzTepIntoTepsNotHostedAndReturnFutures(tzName, tepIpAddress, dpnId, ofTunnel,
                        dataBroker, txRunner);
            } else {
                LOG.trace("Add TEP into transport-zone already configured by Northbound.");
            }
        }


        final Uint64 id = dpnId;
        final String name = tzName;
        futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(Datastore.CONFIGURATION,
            tx -> addConfig(name, id, tepIpAddress, ofTunnel, tx)));
        return futures;
    }

    /**
     * Adds the TEP into Vtep list in the subnet list in the transport zone list
     * from ITM configuration Datastore by merge operation with write transaction.
     *
     * @param updatedVtepList updated Vteps list object which will have new TEP for addition
     * @param tepIpAddress TEP IP address in IpAddress object
     * @param tzName transport zone name in string
     * @param dpid bridge datapath ID
     * @param ofTunnel boolean flag for TEP to enable/disable of-tunnel feature on it
     * @param tx TypedWriteTransaction object
     */
    public static void addVtepInITMConfigDS(List<Vteps> updatedVtepList, IpAddress tepIpAddress, String tzName,
                                            Uint64 dpid, boolean ofTunnel,
                                            TypedWriteTransaction<Datastore.Configuration> tx) {
        //Create TZ node path
        InstanceIdentifier<TransportZone> tranzportZonePath =
                InstanceIdentifier.builder(TransportZones.class)
                        .child(TransportZone.class, new TransportZoneKey(tzName)).build();

        // this check is needed to reuse same function from TransportZoneListener
        // when VTEP is moved from TepsNotHosted list to TZ configured from Northbound.
        if (dpid.compareTo(Uint64.ZERO) > 0) {
            // create vtep
            VtepsKey vtepkey = new VtepsKey(dpid);
            Vteps vtepObj =
                    new VtepsBuilder().setDpnId(dpid).setIpAddress(tepIpAddress).withKey(vtepkey)
                            .setOptionOfTunnel(ofTunnel).build();

            // Add vtep obtained from bridge into list
            updatedVtepList.add(vtepObj);

            LOG.trace("Adding TEP (TZ: {} TEP IP: {} DPID: {}, of-tunnel: {}) in ITM Config DS.", tzName,
                    tepIpAddress, dpid, ofTunnel);
        } else {
            // this is case when this function is called while TEPs movement from tepsNotHosted list when
            // corresponding TZ is configured from northbound.
            for (Vteps vtep: updatedVtepList) {
                LOG.trace("Moving TEP (TEP IP: {} DPID: {}, of-tunnel: {})"
                                + "from not-hosted-transport-zone {} into  ITM Config DS.",
                        vtep.getIpAddress(), vtep.getDpnId(), ofTunnel, tzName);
            }
        }

        // create TZ node with updated subnet having new vtep
        TransportZone updatedTzone =
                new TransportZoneBuilder().withKey(new TransportZoneKey(tzName)).setVteps(updatedVtepList)
                        .setZoneName(tzName).build();

        // Update TZ in Config DS to add vtep in TZ
        tx.merge(tranzportZonePath, updatedTzone, true);
    }

    /**
     * Adds the TEP into Vtep list in the subnet list in the transport zone list
     * from ITM operational Datastore by merge operation with write transaction.
     *
     * @param tzName transport zone name in string
     * @param tepIpAddress TEP IP address in IpAddress object
     * @param dpid bridge datapath ID
     * @param ofTunnel boolean flag for TEP to enable/disable of-tunnel feature on it
     * @param dataBroker data broker handle to perform operations on operational datastore
     * @param tx TypedWriteTransaction object
     */
    protected static void addUnknownTzTepIntoTepsNotHosted(String tzName, IpAddress tepIpAddress,
                                                           Uint64 dpid, boolean ofTunnel, DataBroker dataBroker,
                                                           TypedWriteTransaction<Datastore.Operational> tx) {
        List<UnknownVteps> vtepList;
        TepsInNotHostedTransportZone tepsInNotHostedTransportZone =
                ItmUtils.getUnknownTransportZoneFromITMOperDS(tzName, dataBroker);
        if (tepsInNotHostedTransportZone == null) {
            LOG.trace("Unhosted TransportZone ({}) does not exist in OperDS.", tzName);
            vtepList = new ArrayList<>();
            addVtepIntoTepsNotHosted(addVtepToUnknownVtepsList(vtepList, tepIpAddress, dpid, ofTunnel), tzName, tx);
        } else {
            vtepList = tepsInNotHostedTransportZone.getUnknownVteps();
            if (vtepList == null || vtepList.isEmpty()) {
                //  case: vtep list does not exist or it has no elements
                if (vtepList == null) {
                    LOG.trace("Add TEP into unhosted TZ ({}) when no vtep-list in the TZ.", tzName);
                    vtepList = new ArrayList<>();
                }
                LOG.trace("Add TEP into unhosted TZ ({}) when no vtep-list in the TZ.", tzName);
                addVtepIntoTepsNotHosted(addVtepToUnknownVtepsList(vtepList, tepIpAddress, dpid, ofTunnel), tzName, tx);
            } else {
                //  case: vtep list has elements
                boolean vtepFound = false;
                UnknownVteps oldVtep = null;

                for (UnknownVteps vtep : vtepList) {
                    if (Objects.equals(vtep.getDpnId(), dpid)) {
                        vtepFound = true;
                        oldVtep = vtep;
                        break;
                    }
                }
                if (!vtepFound) {
                    addVtepIntoTepsNotHosted(addVtepToUnknownVtepsList(vtepList,
                            tepIpAddress, dpid, ofTunnel), tzName, tx);
                } else {
                    // vtep is found, update it with tep-ip
                    vtepList.remove(oldVtep);
                    addVtepIntoTepsNotHosted(addVtepToUnknownVtepsList(vtepList,
                            tepIpAddress, dpid, ofTunnel), tzName, tx);
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
     * @param tzName transport zone name in string
     * @param tx TypedWriteTransaction object
     */
    protected static void addVtepIntoTepsNotHosted(List<UnknownVteps> updatedVtepList, String tzName,
                                                   TypedWriteTransaction<Datastore.Operational> tx) {
        //Create TZ node path
        InstanceIdentifier<TepsInNotHostedTransportZone> tepsInNotHostedTransportZoneIid =
                InstanceIdentifier.builder(NotHostedTransportZones.class)
                        .child(TepsInNotHostedTransportZone.class,
                                new TepsInNotHostedTransportZoneKey(tzName)).build();

        // create unknown TZ node with updated vtep list
        TepsInNotHostedTransportZone updatedTzone = new TepsInNotHostedTransportZoneBuilder()
                .withKey(new TepsInNotHostedTransportZoneKey(tzName)).setZoneName(tzName)
                .setUnknownVteps(updatedVtepList).build();

        // Update TZ in Oper DS.
        tx.merge(tepsInNotHostedTransportZoneIid, updatedTzone, true);
    }

    private static void addConfig(String tzName, Uint64 dpnId, IpAddress ipAdd,
                                  boolean ofTunnel, TypedWriteTransaction<Datastore.Configuration> tx) {
        List<Vteps> vtepList = new ArrayList<>();

        LOG.trace("Add TEP in transport-zone when no vtep-list for specific subnet.");
        addVtepInITMConfigDS(vtepList, ipAdd, tzName, dpnId, ofTunnel, tx);
    }

    private static List<ListenableFuture<Void>> addUnknownTzTepIntoTepsNotHostedAndReturnFutures(String tzName,
                                                         IpAddress tepIpAddress, Uint64 id, boolean ofTunnel,
                                                         DataBroker dataBroker, ManagedNewTransactionRunner txRunner) {
        return Collections.singletonList(txRunner.callWithNewWriteOnlyTransactionAndSubmit(Datastore.OPERATIONAL,
            tx -> addUnknownTzTepIntoTepsNotHosted(tzName, tepIpAddress, id, ofTunnel, dataBroker, tx)));
    }

    private static List<UnknownVteps> addVtepToUnknownVtepsList(List<UnknownVteps> updatedVtepList,
                                                                IpAddress tepIpAddress, Uint64 dpid,
                                                                boolean ofTunnel) {
        // create vtep
        UnknownVtepsKey vtepkey = new UnknownVtepsKey(dpid);
        UnknownVteps vtepObj =
                new UnknownVtepsBuilder().setDpnId(dpid).setIpAddress(tepIpAddress).withKey(vtepkey)
                        .setOfTunnel(ofTunnel).build();

        // Add vtep obtained into unknown TZ tep list
        updatedVtepList.add(vtepObj);
        LOG.trace("Adding TEP  (DPID: {}, TEP IP: {}, of-tunnel: {}) into unhosted Transport Zone"
                + "inside ITM Oper DS.", dpid, tepIpAddress, ofTunnel);
        return updatedVtepList;
    }
}
