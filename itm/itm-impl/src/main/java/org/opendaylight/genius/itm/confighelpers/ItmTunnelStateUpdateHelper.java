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
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.itm.impl.ITMBatchingUtils;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelOperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelListKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItmTunnelStateUpdateHelper {
    private static final Logger LOG = LoggerFactory.getLogger(ItmTunnelStateUpdateHelper.class);

    public static List<ListenableFuture<Void>> updateTunnel(Interface updated, DataBroker broker) throws Exception {
        LOG.debug("Invoking ItmTunnelStateUpdateHelper for Interface {} ", updated);
        final WriteTransaction writeTransaction = broker.newWriteOnlyTransaction();

        StateTunnelListKey tlKey = ItmUtils.getTunnelStateKey(updated);
        LOG.trace("TunnelStateKey: {} for interface: {}", tlKey, updated.getName());
        InstanceIdentifier<StateTunnelList> stListId = ItmUtils.buildStateTunnelListId(tlKey);
        StateTunnelList tunnelsState = ItmUtils.getTunnelState(broker, updated.getName(), stListId);
        StateTunnelListBuilder stlBuilder;
        TunnelOperStatus tunnelOperStatus;
        boolean tunnelState = updated.getOperStatus().equals(Interface.OperStatus.Up);
        switch (updated.getOperStatus()) {
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
        if (tunnelsState != null) {
            stlBuilder = new StateTunnelListBuilder(tunnelsState);
            stlBuilder.setTunnelState(tunnelState);
            stlBuilder.setOperState(tunnelOperStatus);
            StateTunnelList stList = stlBuilder.build();
            LOG.trace("Batching the updation of tunnel_state: {} for Id: {}", stList, stListId);
            ITMBatchingUtils.update(stListId, stList, ITMBatchingUtils.EntityType.DEFAULT_OPERATIONAL);
        }

        return Collections.singletonList(writeTransaction.submit());
    }
}
