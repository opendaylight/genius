/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.itmdirecttunnels.listeners;

import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.cache.BfdStateCache;
import org.opendaylight.genius.itm.cache.DPNTEPsInfoCache;
import org.opendaylight.genius.itm.cache.DpnTepStateCache;
import org.opendaylight.genius.itm.cache.OvsBridgeEntryCache;
import org.opendaylight.genius.itm.cache.OvsBridgeRefEntryCache;
import org.opendaylight.genius.itm.cache.TunnelStateCache;
import org.opendaylight.genius.itm.cache.UnprocessedNodeConnectorCache;
import org.opendaylight.genius.itm.impl.TunnelMonitoringConfig;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DirectTunnelListenerCreator implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(DirectTunnelListenerCreator.class);

    private final TunnelTopologyStateListener tunnelTopologyStateListener;
    private final TunnelInventoryStateListener tunnelInventoryStateListener;
    private final TerminationPointStateListener terminationPointStateListener;

    @Inject
    public DirectTunnelListenerCreator(final DataBroker dataBroker, final EntityOwnershipUtils entityOwnershipUtils,
                                       final JobCoordinator coordinator, final BfdStateCache bfdStateCache,
                                       final DpnTepStateCache dpnTepStateCache,
                                       final DPNTEPsInfoCache dpntePsInfoCache,
                                       final TunnelStateCache tunnelStateCache,
                                       final OvsBridgeRefEntryCache ovsBridgeRefEntryCache,
                                       final OvsBridgeEntryCache ovsBridgeEntryCache,
                                       final TunnelMonitoringConfig tunnelMonitoringConfig,
                                       final DirectTunnelUtils directTunnelUtils,
                                       final IInterfaceManager interfaceManager,
                                       final UnprocessedNodeConnectorCache unprocessedNCCache) {

        if (interfaceManager.isItmDirectTunnelsEnabled()) {
            LOG.trace("ITM Direct Tunnels is enabled. Initializing the listeners");
            this.tunnelTopologyStateListener = new TunnelTopologyStateListener(dataBroker, interfaceManager,
                    coordinator, entityOwnershipUtils, directTunnelUtils, dpnTepStateCache, dpntePsInfoCache,
                    bfdStateCache, tunnelStateCache, ovsBridgeRefEntryCache, ovsBridgeEntryCache,
                    tunnelMonitoringConfig);
            this.tunnelInventoryStateListener = new TunnelInventoryStateListener(dataBroker, coordinator,
                    entityOwnershipUtils, directTunnelUtils, tunnelStateCache, dpnTepStateCache, unprocessedNCCache);
            this.terminationPointStateListener = new TerminationPointStateListener(dataBroker, entityOwnershipUtils,
                    coordinator, bfdStateCache, dpnTepStateCache,tunnelStateCache, directTunnelUtils);
        } else {
            LOG.trace("ITM Direct Tunnels is disabled. Listeners are not registered");
            this.tunnelTopologyStateListener = null;
            this.tunnelInventoryStateListener = null;
            this.terminationPointStateListener = null;
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
        if (terminationPointStateListener != null) {
            this.terminationPointStateListener.close();
        }
    }
}
