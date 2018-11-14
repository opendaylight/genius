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
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
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

public final class ItmTunnelStateAddHelper {
    private static final Logger LOG = LoggerFactory.getLogger(ItmTunnelStateAddHelper.class);

    private ItmTunnelStateAddHelper() { }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public static List<ListenableFuture<Void>> addTunnel(Interface iface, IInterfaceManager ifaceManager,
                                                         DataBroker broker) throws Exception {
        LOG.debug("Invoking ItmTunnelStateAddHelper for Interface {} ", iface);
        StateTunnelListKey tlKey = ItmUtils.getTunnelStateKey(iface);
        LOG.trace("TunnelStateKey: {} for interface: {}", tlKey, iface.getName());
        InstanceIdentifier<StateTunnelList> stListId = ItmUtils.buildStateTunnelListId(tlKey);
        StateTunnelList tunnelStateList;
        TunnelOperStatus tunnelOperStatus;
        boolean tunnelState = Interface.OperStatus.Up.equals(iface.getOperStatus());
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
        return Collections.emptyList();
    }

}
