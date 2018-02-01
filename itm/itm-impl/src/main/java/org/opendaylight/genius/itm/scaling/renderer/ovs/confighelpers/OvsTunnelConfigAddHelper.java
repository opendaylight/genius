/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.scaling.renderer.ovs.confighelpers;

import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.SouthboundUtils;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.TunnelMetaUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.ovs.bridge.ref.info.OvsBridgeRefEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.ovs.bridge.ref.info.OvsBridgeRefEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsTunnelConfigAddHelper {
    private static final Logger LOG = LoggerFactory.getLogger(OvsTunnelConfigAddHelper.class);

    public static List<ListenableFuture<Void>> addTunnelConfiguration(DataBroker dataBroker, Interface iface) {
        // ITM Direct Tunnels This transaction is not being used -- CHECK
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();

        ParentRefs parentRefs = iface.getAugmentation(ParentRefs.class);

        if (parentRefs == null) {
            LOG.warn("ParentRefs for interface: {} Not Found. Creation of Tunnel OF-Port not supported"
                    + " when dpid not provided.", iface.getName());
            return futures;
        }

        BigInteger dpId = parentRefs.getDatapathNodeIdentifier();
        if (dpId == null) {
            LOG.warn("dpid for interface: {} Not Found. No DPID provided. Creation of OF-Port not supported.",
                    iface.getName());
            return futures;
        }
        LOG.info("adding tunnel configuration for {}", iface.getName());
        LOG.debug("creating bridge interfaceEntry in ConfigDS {}", dpId);
        TunnelMetaUtils.createBridgeTunnelEntryInConfigDS(dpId,iface.getName());

        // create bridge on switch, if switch is connected
        OvsBridgeRefEntryKey ovsBridgeRefEntryKey = new OvsBridgeRefEntryKey(dpId);
        InstanceIdentifier<OvsBridgeRefEntry> dpnBridgeEntryIid =
                TunnelMetaUtils.getOvsBridgeRefEntryIdentifier(ovsBridgeRefEntryKey);
        OvsBridgeRefEntry ovsBridgeRefEntry =
                TunnelMetaUtils.getOvsBridgeRefEntryFromOperDS(dpId, dataBroker);
        if (ovsBridgeRefEntry != null && ovsBridgeRefEntry.getOvsBridgeReference() != null) {
            LOG.debug("creating bridge interface on dpn {}", dpId);
            InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIid =
                    (InstanceIdentifier<OvsdbBridgeAugmentation>) ovsBridgeRefEntry.getOvsBridgeReference().getValue();
            SouthboundUtils.addPortToBridge(bridgeIid, iface, iface.getName(), dataBroker, futures);
        }

        futures.add(transaction.submit());
        return futures;
    }
}
