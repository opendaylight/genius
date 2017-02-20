/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.confighelpers;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.impl.ITMBatchingUtils;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelOperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelListKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ehemgop on 06-02-2017.
 */
public class ItmTunnelStateAddHelper {
    private static final Logger LOG = LoggerFactory.getLogger(ItmTunnelStateAddHelper.class);

    public static List<ListenableFuture<Void>> addTunnel(Interface iface, IInterfaceManager ifaceManager, DataBroker broker) throws Exception {
        LOG.debug( "Invoking ItmTunnelStateAddHelper for Interface {} ", iface);
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        WriteTransaction writeTransaction = broker.newWriteOnlyTransaction();
        StateTunnelListKey tlKey = ItmUtils.getTunnelStateKey(iface);
        LOG.trace("TunnelStateKey: {} for interface: {}", tlKey, iface.getName());
        InstanceIdentifier<StateTunnelList> stListId = ItmUtils.buildStateTunnelListId(tlKey);
        StateTunnelList tunnelStateList;
        TunnelOperStatus tunnelOperStatus;
        boolean tunnelState = (iface.getOperStatus().equals(Interface.OperStatus.Up)) ? (true):(false);
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
            tunnelStateList = ItmUtils.buildStateTunnelList(tlKey, iface.getName(), tunnelState, tunnelOperStatus, ifaceManager, broker);
            LOG.trace("Batching the Creation of tunnel_state: {} for Id: {}", tunnelStateList, stListId);
            ITMBatchingUtils.write(stListId, tunnelStateList, ITMBatchingUtils.EntityType.DEFAULT_OPERATIONAL);
        } catch (Exception e) {
            LOG.warn("Exception trying to create tunnel state for {}", iface.getName(), e);
        }

        futures.add(writeTransaction.submit());
        return futures;
    }

}