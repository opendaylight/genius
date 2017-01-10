/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.renderer.ovs.statehelpers;

import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsInterfaceTopologyStateRemoveHelper {
    private static final Logger LOG = LoggerFactory.getLogger(OvsInterfaceTopologyStateRemoveHelper.class);

    public static List<ListenableFuture<Void>> removePortFromBridge(InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIid,
                                                                    OvsdbBridgeAugmentation bridgeOld, DataBroker dataBroker) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        BigInteger dpnId = IfmUtil.getDpnId(bridgeOld.getDatapathId());

        if (dpnId == null) {
            LOG.warn("Got Null DPID for Bridge: {}", bridgeOld);
            return futures;
        }

        //delete bridge reference entry in interface meta operational DS
        InterfaceMetaUtils.deleteBridgeRefEntry(dpnId, transaction);

        // the bridge reference is copied to dpn-tunnel interfaces map, so that whenever a northbound delete
        // happens when bridge is not connected, we need the bridge reference to clean up the topology config DS
        InterfaceMetaUtils.addBridgeRefToBridgeInterfaceEntry(dpnId, new OvsdbBridgeRef(bridgeIid), transaction);

        futures.add(transaction.submit());
        return futures;
    }
}