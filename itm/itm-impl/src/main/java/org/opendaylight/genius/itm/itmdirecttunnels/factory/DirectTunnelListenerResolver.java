/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.itmdirecttunnels.factory;

import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.cache.DPNTEPsInfoCache;
import org.opendaylight.genius.itm.cache.DpnTepStateCache;
import org.opendaylight.genius.itm.cache.OvsBridgeEntryCache;
import org.opendaylight.genius.itm.cache.TunnelStateCache;
import org.opendaylight.genius.itm.cache.UnprocessedNodeConnectorCache;
import org.opendaylight.genius.itm.itmdirecttunnels.listeners.TunnelInventoryStateListener;
import org.opendaylight.genius.itm.itmdirecttunnels.listeners.TunnelTopologyStateListener;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DirectTunnelListenerResolver  implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(DirectTunnelListenerResolver.class);

    private final TunnelTopologyStateListener tunnelTopologyStateListener;
    private final TunnelInventoryStateListener tunnelInventoryStateListener;

    @Inject
    public DirectTunnelListenerResolver(final DataBroker dataBroker, final EntityOwnershipUtils entityOwnershipUtils,
                                        final JobCoordinator coordinator, final DPNTEPsInfoCache dpntePsInfoCache,
                                        final DpnTepStateCache dpnTepStateCache,
                                        final TunnelStateCache tunnelStateCache,
                                        final OvsBridgeEntryCache ovsBridgeEntryCache,
                                        final UnprocessedNodeConnectorCache unprocessedNodeConnectorCache,
                                        final DirectTunnelUtils directTunnelUtils,
                                        final IInterfaceManager interfaceManager) {

        if (interfaceManager.isItmDirectTunnelsEnabled()) {
            LOG.trace("ITM Direct Tunnels is enabled. Initializing the listeners");
            this.tunnelTopologyStateListener = DirectTunnelListenerFactory.getTopologyStateListener(dataBroker,
                    coordinator, entityOwnershipUtils, directTunnelUtils, dpnTepStateCache, ovsBridgeEntryCache);
            this.tunnelInventoryStateListener = DirectTunnelListenerFactory.getTunnelInventoryStateListener(
                    dataBroker, coordinator, entityOwnershipUtils, directTunnelUtils,
                    dpntePsInfoCache, tunnelStateCache, dpnTepStateCache, unprocessedNodeConnectorCache);
        } else {
            LOG.trace("ITM Direct Tunnels is disabled. Listeners are not registered");
            this.tunnelTopologyStateListener = null;
            this.tunnelInventoryStateListener = null;
        }
    }

    @Override
    public void close() throws Exception {
        if (tunnelTopologyStateListener != null) {
            this.tunnelTopologyStateListener.close();
        }
        if (tunnelInventoryStateListener != null) {
            this.tunnelInventoryStateListener.close();
        }
    }
}
