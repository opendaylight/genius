/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.tests;


import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.itm.confighelpers.OvsdbTepAddConfigHelper;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.concurrent.ExecutionException;

public class ItmTepAutoConfigTestUtil {

    /* transaction methods */
      public static void addTep(String tepIp, String strDpnId, String tzName, boolean ofTunnel,
        DataBroker dataBroker) {
        WriteTransaction wrTx = dataBroker.newWriteOnlyTransaction();

        // add TEP received from southbound OVSDB into ITM config DS.
        OvsdbTepAddConfigHelper.addTepReceivedFromOvsdb(tepIp, strDpnId, tzName, ofTunnel,
            dataBroker, wrTx);
        wrTx.submit();
    }

    public static void writeItmConfig(InstanceIdentifier<ItmConfig> iid, ItmConfig itmConfig, DataBroker dataBroker) {
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.put(LogicalDatastoreType.CONFIGURATION, iid, itmConfig);
        CheckedFuture<Void, TransactionCommitFailedException> futures = tx.submit();
        try {
            futures.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static ItmConfig readItmConfig(InstanceIdentifier<ItmConfig> iid, DataBroker dataBroker) {

        ReadOnlyTransaction tx = dataBroker.newReadOnlyTransaction();
        Optional<ItmConfig> result = Optional.absent();
        try {
            result = tx.read(LogicalDatastoreType.CONFIGURATION, iid).get();
        } catch (Exception e) {
            return null;
        }
        if (result.isPresent()) {
            return result.get();
        }
        return null;
    }

    /* utility methods */
    public static InstanceIdentifier<TransportZone> getDefTzIid() {
        InstanceIdentifier<TransportZone> tzoneIid =
            InstanceIdentifier.builder(TransportZones.class)
                .child(TransportZone.class, new TransportZoneKey(ITMConstants.DEFAULT_TRANSPORT_ZONE)).build();

        return tzoneIid;
    }
}
