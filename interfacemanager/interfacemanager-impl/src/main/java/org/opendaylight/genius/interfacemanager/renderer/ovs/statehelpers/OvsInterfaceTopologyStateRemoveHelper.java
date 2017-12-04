/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.renderer.ovs.statehelpers;

import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsInterfaceTopologyStateRemoveHelper {
    private static final Logger LOG = LoggerFactory.getLogger(OvsInterfaceTopologyStateRemoveHelper.class);

    public static List<ListenableFuture<Void>> removePortFromBridge(
            InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIid, OvsdbBridgeAugmentation bridgeOld,
            ManagedNewTransactionRunner txRunner) {
        BigInteger dpnId = IfmUtil.getDpnId(bridgeOld.getDatapathId());

        if (dpnId == null) {
            LOG.warn("Got Null DPID for Bridge: {}", bridgeOld);
            return Collections.emptyList();
        }

        LOG.debug("removing bridge references for bridge: {}, dpn: {}", bridgeOld, dpnId);
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(operTx -> {
            // delete bridge reference entry in interface meta operational DS
            InterfaceMetaUtils.deleteBridgeRefEntry(dpnId, operTx);
        }));
        futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(configTx -> {
            // the bridge reference is copied to dpn-tunnel interfaces map, so that
            // whenever a northbound delete
            // happens when bridge is not connected, we need the bridge reference to
            // clean up the topology config DS
            InterfaceMetaUtils.addBridgeRefToBridgeInterfaceEntry(dpnId, new OvsdbBridgeRef(bridgeIid), configTx);
        }));
        return futures;
    }
}
