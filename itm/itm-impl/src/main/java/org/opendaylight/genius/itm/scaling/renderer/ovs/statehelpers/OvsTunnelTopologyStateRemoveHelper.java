/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.scaling.renderer.ovs.statehelpers;

import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.ItmScaleUtils;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.TunnelMetaUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OvsTunnelTopologyStateRemoveHelper {
    private static final Logger LOG = LoggerFactory.getLogger(OvsTunnelTopologyStateRemoveHelper.class);

    private OvsTunnelTopologyStateRemoveHelper() {
    }

    public static List<ListenableFuture<Void>> removePortFromBridge(InstanceIdentifier<OvsdbBridgeAugmentation>
                                                                            bridgeIid,
                                                                    OvsdbBridgeAugmentation bridgeOld,
                                                                    DataBroker dataBroker) {
        BigInteger dpnId = ItmScaleUtils.getDpnId(bridgeOld.getDatapathId());
        if (dpnId == null) {
            LOG.warn("Got Null DPID for Bridge: {}", bridgeOld);
            return Collections.emptyList();
        }
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        LOG.debug("removing bridge references for bridge: {}, dpn: {}", bridgeOld,
            dpnId);
        //delete bridge reference entry in interface meta operational DS
        TunnelMetaUtils.deleteOvsBridgeRefEntry(dpnId, transaction);

        // the bridge reference is copied to dpn-tunnel interfaces map, so that whenever a northbound delete
        // happens when bridge is not connected, we need the bridge reference to clean up the topology config DS
        TunnelMetaUtils.addBridgeRefToBridgeTunnelEntry(dpnId, new OvsdbBridgeRef(bridgeIid), transaction);
        return Collections.singletonList(transaction.submit());
    }
}
