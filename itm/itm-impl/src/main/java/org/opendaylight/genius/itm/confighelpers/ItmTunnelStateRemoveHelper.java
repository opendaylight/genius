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
<<<<<<< HEAD
=======
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
>>>>>>> 553cec96e... GENIUS-251: Event Logger for Genius
import org.opendaylight.genius.itm.impl.ITMBatchingUtils;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelListKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ItmTunnelStateRemoveHelper {
    private static final Logger LOG = LoggerFactory.getLogger(ItmTunnelStateRemoveHelper.class);
    private static final Logger EVENT_LOGGER = LoggerFactory.getLogger("GeniusEventLogger");

    private ItmTunnelStateRemoveHelper() { }
    
    public static List<ListenableFuture<Void>> removeTunnel(Interface iface) throws Exception {
        LOG.debug("Invoking removeTunnel for Interface {}", iface);
        StateTunnelListKey tlKey = ItmUtils.getTunnelStateKey(iface);
        InstanceIdentifier<StateTunnelList> stListId = ItmUtils.buildStateTunnelListId(tlKey);
        LOG.trace("Deleting tunnel_state for Id: {}", stListId);
        EVENT_LOGGER.info(" REMOVE {} {} ", ItmTunnelStateRemoveHelper.class, stListId.toString());
        ITMBatchingUtils.delete(stListId, ITMBatchingUtils.EntityType.DEFAULT_OPERATIONAL);
        return Collections.emptyList();
    }
}
