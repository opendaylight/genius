/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.tests;

import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.itm.confighelpers.OvsdbTepAddConfigHelper;
import org.opendaylight.genius.itm.confighelpers.OvsdbTepRemoveConfigHelper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.NotHostedTransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.not.hosted.transport.zones.TepsInNotHostedTransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.not.hosted.transport.zones.TepsInNotHostedTransportZoneKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.Subnets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.SubnetsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.Vteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.VtepsKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class ItmTepAutoConfigTestUtil {

    private ItmTepAutoConfigTestUtil() { }

    /* transaction methods */
    public static ListenableFuture<Void> addTep(String tepIp, String strDpnId, String tzName, boolean ofTunnel,
                                                DataBroker dataBroker, ManagedNewTransactionRunner tx) {
        return
            OvsdbTepAddConfigHelper.addTepReceivedFromOvsdb(tepIp, strDpnId, tzName, ofTunnel, dataBroker, tx).get(0);
    }

    public static ListenableFuture<Void> deleteTep(String tepIp, String strDpnId, String tzName,
                                                   DataBroker dataBroker, ManagedNewTransactionRunner tx) {
        return OvsdbTepRemoveConfigHelper.removeTepReceivedFromOvsdb(tepIp, strDpnId, tzName, dataBroker, tx).get(0);
    }

    /* utility methods */
    public static InstanceIdentifier<TransportZone> getTzIid(String tzName) {
        InstanceIdentifier<TransportZone> tzoneIid =
            InstanceIdentifier.builder(TransportZones.class)
                .child(TransportZone.class, new TransportZoneKey(tzName))
                .build();

        return tzoneIid;
    }

    public static InstanceIdentifier<Vteps> getTepIid(IpPrefix subnetMaskObj, String tzName,
        BigInteger dpnId, String portName) {
        SubnetsKey subnetsKey = new SubnetsKey(subnetMaskObj);
        VtepsKey vtepkey = new VtepsKey(dpnId, portName);

        InstanceIdentifier<Vteps> vtepIid = InstanceIdentifier.builder(TransportZones.class)
            .child(TransportZone.class, new TransportZoneKey(tzName))
            .child(Subnets.class, subnetsKey).child(Vteps.class, vtepkey).build();

        return vtepIid;
    }

    public static InstanceIdentifier<TepsInNotHostedTransportZone> getTepNotHostedInTZIid(String tzName) {
        InstanceIdentifier<TepsInNotHostedTransportZone> tzonePath =
            InstanceIdentifier.builder(NotHostedTransportZones.class)
                .child(TepsInNotHostedTransportZone.class,
                    new TepsInNotHostedTransportZoneKey(tzName)).build();

        return tzonePath;
    }
}
