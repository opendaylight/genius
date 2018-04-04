/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.itmdirecttunnels.factory;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.itm.cache.DpnTepStateCache;
import org.opendaylight.genius.itm.cache.OvsBridgeEntryCache;
import org.opendaylight.genius.itm.itmdirecttunnels.listeners.TunnelTopologyStateListener;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;

public final class DirectTunnelListenerFactory {

    private DirectTunnelListenerFactory() {
    }

    public static TunnelTopologyStateListener
        getTopologyStateListener(final DataBroker dataBroker, final JobCoordinator coordinator,
                                 final EntityOwnershipUtils entityOwnershipUtils,
                                 final DirectTunnelUtils directTunnelUtils, final DpnTepStateCache dpnTepStateCache,
                                 final OvsBridgeEntryCache ovsBridgeEntryCache)  {
        return new TunnelTopologyStateListener(dataBroker, coordinator, entityOwnershipUtils, directTunnelUtils,
                dpnTepStateCache,  ovsBridgeEntryCache) ;
    }
}
