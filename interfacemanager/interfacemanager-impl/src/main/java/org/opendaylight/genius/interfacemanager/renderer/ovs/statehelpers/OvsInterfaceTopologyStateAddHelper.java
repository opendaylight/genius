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
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.genius.interfacemanager.renderer.ovs.utilities.SouthboundUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge._interface.info.BridgeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsInterfaceTopologyStateAddHelper {
    private static final Logger LOG = LoggerFactory.getLogger(OvsInterfaceTopologyStateAddHelper.class);

    public static List<ListenableFuture<Void>> addPortToBridge(InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIid,
            OvsdbBridgeAugmentation bridgeNew, DataBroker dataBroker) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();

        if (bridgeNew.getDatapathId() == null) {
            LOG.warn("DataPathId found as null for Bridge Augmentation: {}... returning...", bridgeNew);
            return futures;
        }
        BigInteger dpnId = IfmUtil.getDpnId(bridgeNew.getDatapathId());
        LOG.debug("adding bridge references for bridge: {}, dpn: {}", bridgeNew, dpnId);
        // create bridge reference entry in interface meta operational DS
        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        InterfaceMetaUtils.createBridgeRefEntry(dpnId, bridgeIid, writeTransaction);

        // handle pre-provisioning of tunnels for the newly connected dpn
        BridgeEntry bridgeEntry = InterfaceMetaUtils.getBridgeEntryFromConfigDS(dpnId, dataBroker);
        if (bridgeEntry == null) {
            LOG.debug("Bridge entry not found in config DS for dpn: {}", dpnId);
            futures.add(writeTransaction.submit());
            return futures;
        }
        futures.add(writeTransaction.submit());
        SouthboundUtils.addAllPortsToBridge(bridgeEntry, dataBroker, bridgeIid, bridgeNew);
        return futures;
    }
}
