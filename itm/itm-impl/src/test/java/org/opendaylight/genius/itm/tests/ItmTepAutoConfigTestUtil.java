/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.tests;

import com.google.common.util.concurrent.CheckedFuture;
import java.math.BigInteger;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.itm.confighelpers.OvsdbTepAddConfigHelper;
import org.opendaylight.genius.itm.confighelpers.OvsdbTepRemoveConfigHelper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
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
    public static CheckedFuture<Void, TransactionCommitFailedException> addTep(String tepIp,
        String strDpnId, String tzName, boolean ofTunnel, DataBroker dataBroker) throws Exception {
        WriteTransaction wrTx = dataBroker.newWriteOnlyTransaction();

        // add TEP received from southbound OVSDB into ITM config DS.
        OvsdbTepAddConfigHelper.addTepReceivedFromOvsdb(tepIp, strDpnId, tzName, ofTunnel,
            dataBroker, wrTx);
        return wrTx.submit();
    }

    public static CheckedFuture<Void, TransactionCommitFailedException> deleteTep(String tepIp,
        String strDpnId, String tzName, DataBroker dataBroker) {
        WriteTransaction wrTx = dataBroker.newWriteOnlyTransaction();

        // remove TEP received from southbound OVSDB from ITM config DS.
        OvsdbTepRemoveConfigHelper.removeTepReceivedFromOvsdb(tepIp, strDpnId, tzName, dataBroker, wrTx);
        return wrTx.submit();
    }

    public static CheckedFuture<Void, TransactionCommitFailedException> writeItmConfig(
        InstanceIdentifier<ItmConfig> iid, ItmConfig itmConfig, DataBroker dataBroker) {
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.put(LogicalDatastoreType.CONFIGURATION, iid, itmConfig);
        return tx.submit();
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
