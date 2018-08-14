/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.confighelpers;

import com.google.common.util.concurrent.*;
import java.util.*;
import org.opendaylight.controller.md.sal.binding.api.*;
import org.opendaylight.genius.itm.impl.*;
import org.opendaylight.genius.utils.*;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.*;
import org.opendaylight.yangtools.yang.binding.*;
import org.slf4j.*;

public final class ItmTunnelStateRemoveHelper {
    private static final Logger LOG = LoggerFactory.getLogger(ItmTunnelStateRemoveHelper.class);

    private ItmTunnelStateRemoveHelper() { }

    public static List<ListenableFuture<Void>> removeTunnel(Interface iface, DataBroker broker) throws Exception {
        LOG.debug("Invoking ItmTunnelStateRemoveHelper for Interface {} ", iface);
        StateTunnelListKey tlKey = ItmUtils.getTunnelStateKey(iface);
        InstanceIdentifier<StateTunnelList> stListId = ItmUtils.buildStateTunnelListId(tlKey);
        LOG.trace("Deleting tunnel_state for Id: {}", stListId);
        GeniusEventLogger.logInfo(ItmTunnelStateRemoveHelper.class.getSimpleName(), " REMOVE ", stListId.toString());
        ITMBatchingUtils.delete(stListId, ITMBatchingUtils.EntityType.DEFAULT_OPERATIONAL);
        return Collections.emptyList();
    }
}
