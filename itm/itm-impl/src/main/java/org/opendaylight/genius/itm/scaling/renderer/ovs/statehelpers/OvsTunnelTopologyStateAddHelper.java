/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.scaling.renderer.ovs.statehelpers;

import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.TunnelMetaUtils;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.ItmScaleUtils;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.SouthboundUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev161113.bridge.tunnel.info.OvsBridgeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsTunnelTopologyStateAddHelper {
    private static final Logger LOG = LoggerFactory.getLogger(OvsTunnelTopologyStateAddHelper.class);

    public static List<ListenableFuture<Void>> addPortToBridge(InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIid,
                                                               OvsdbBridgeAugmentation bridgeNew,
                                                               DataBroker dataBroker) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();

        if (bridgeNew.getDatapathId() == null) {
            LOG.info("DataPathId found as null for Bridge Augmentation: {}... returning...", bridgeNew);
            return futures;
        }

        BigInteger dpnId = ItmScaleUtils.getDpnId(bridgeNew.getDatapathId());
        LOG.debug("adding bridge references for bridge: {}, dpn: {}", bridgeNew, dpnId);

        // create bridge reference entry in interface meta operational DS
        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        TunnelMetaUtils.createOvsBridgeRefEntry(dpnId, bridgeIid, writeTransaction);

        // handle pre-provisioning of tunnels for the newly connected dpn
        OvsBridgeEntry bridgeEntry = TunnelMetaUtils.getOvsBridgeEntryFromConfigDS(dpnId, dataBroker);
        if (bridgeEntry == null) {
            LOG.debug("Bridge entry not found in config DS for dpn: {}", dpnId);
            futures.add(writeTransaction.submit());
            return futures;
        }
        futures.add(writeTransaction.submit());
        SouthboundUtils.addAllPortsToBridge(bridgeEntry, dataBroker, bridgeIid, bridgeNew, futures);
        return futures;
    }
}