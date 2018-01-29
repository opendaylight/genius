/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.confighelpers;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.impl.ITMBatchingUtils;
import org.opendaylight.genius.itm.impl.ItmTepUtils;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelOperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ItmTunnelStateAddHelper {
    private static final Logger LOG = LoggerFactory.getLogger(ItmTunnelStateAddHelper.class);

    private ItmTunnelStateAddHelper() { }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public static List<ListenableFuture<Void>> addTunnel(Interface iface, IInterfaceManager ifaceManager,
                                                         DataBroker broker) throws Exception {
        LOG.debug("Invoking ItmTunnelStateAddHelper for Interface {} ", iface);
        final WriteTransaction writeTransaction = broker.newWriteOnlyTransaction();
        StateTunnelListKey tlKey = ItmUtils.getTunnelStateKey(iface);
        LOG.trace("TunnelStateKey: {} for interface: {}", tlKey, iface.getName());
        InstanceIdentifier<StateTunnelList> stListId = ItmUtils.buildStateTunnelListId(tlKey);
        StateTunnelList tunnelStateList;
        TunnelOperStatus tunnelOperStatus;
        boolean tunnelState = iface.getOperStatus().equals(Interface.OperStatus.Up);
        switch (iface.getOperStatus()) {
            case Up:
                tunnelOperStatus = TunnelOperStatus.Up;
                break;
            case Down:
                tunnelOperStatus = TunnelOperStatus.Down;
                break;
            case Unknown:
                tunnelOperStatus = TunnelOperStatus.Unknown;
                break;
            default:
                tunnelOperStatus = TunnelOperStatus.Ignore;
        }

        // Create new Tunnel State
        try {
            /*
             * FIXME: A defensive try-catch to find issues without
             * disrupting existing behavior.
             */
            tunnelStateList = ItmUtils.buildStateTunnelList(tlKey, iface.getName(), tunnelState, tunnelOperStatus,
                    ifaceManager, broker);
            LOG.trace("Batching the Creation of tunnel_state: {} for Id: {}", tunnelStateList, stListId);
            ITMBatchingUtils.write(stListId, tunnelStateList, ITMBatchingUtils.EntityType.DEFAULT_OPERATIONAL);
        } catch (Exception e) {
            LOG.warn("Exception trying to create tunnel state for {}", iface.getName(), e);
        }

        return Collections.singletonList(writeTransaction.submit());
    }

    public static List<ListenableFuture<Void>> addTunnel(OvsdbTerminationPointAugmentation tp,
                                                         Map<String, String> externalIds,
                                                         DataBroker broker, ItmTepUtils itmTepUtils) {

        LOG.debug("Invoking ItmTunnelStateAddHelper for {} ", tp.getName());
        final WriteTransaction writeTransaction = broker.newWriteOnlyTransaction();
        StateTunnelListKey tlKey = new StateTunnelListKey(tp.getName());
        InstanceIdentifier<StateTunnelList> stListId = ItmUtils.buildStateTunnelListId(tlKey);
        StateTunnelList tunnelStateList;
        TunnelOperStatus tunnelOperStatus;
        //TODO: Handle state as per BFD enable/disable
        tunnelOperStatus = TunnelOperStatus.Up;
        boolean tunnelState = true;

        // Create new Tunnel State
        try {
            /*
             * FIXME: A defensive try-catch to find issues without
             * disrupting existing behavior.
             */
            String srcIp = itmTepUtils.getOption(itmTepUtils.TUNNEL_OPTIONS_LOCAL_IP, tp.getOptions());
            String dstIp = itmTepUtils.getOption(itmTepUtils.TUNNEL_OPTIONS_REMOTE_IP, tp.getOptions());
            tunnelStateList = ItmUtils.buildStateTunnelList(broker, tlKey, tp.getName(), tunnelState, tunnelOperStatus,
                new IpAddress(srcIp.toCharArray()),
                new IpAddress((dstIp.toCharArray())),
                externalIds.get(itmTepUtils.IFACE_EXTERNAL_ID_DPNID),
                externalIds.get(itmTepUtils.IFACE_EXTERNAL_ID_PEER_ID),
                ItmUtils.TUNNEL_TYPE_MAP.get(externalIds.get(itmTepUtils.IFACE_EXTERNAL_ID_TUNNEL_TYPE)));
            LOG.trace("Batching the Creation of tunnel_state: {} for Id: {}", tunnelStateList, stListId);
            ITMBatchingUtils.write(stListId, tunnelStateList, ITMBatchingUtils.EntityType.DEFAULT_OPERATIONAL);
        } catch (Exception e) {
            LOG.warn("Exception trying to create tunnel state for {}", tp.getName(), e);
        }

        return Collections.singletonList(writeTransaction.submit());
    }
}
