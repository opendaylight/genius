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
import org.opendaylight.genius.itm.cache.BfdStateCache;
import org.opendaylight.genius.itm.cache.DPNTEPsInfoCache;
import org.opendaylight.genius.itm.cache.DpnTepStateCache;
import org.opendaylight.genius.itm.cache.OvsBridgeEntryCache;
import org.opendaylight.genius.itm.cache.OvsBridgeRefEntryCache;
import org.opendaylight.genius.itm.cache.TunnelStateCache;
import org.opendaylight.genius.itm.cache.UnprocessedNodeConnectorCache;
import org.opendaylight.genius.itm.impl.TunnelMonitoringConfig;
import org.opendaylight.genius.itm.itmdirecttunnels.listeners.TunnelTopologyStateListener;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DirectTunnelListenerResolver  implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(DirectTunnelListenerResolver.class);

    private final DataBroker dataBroker;
    private final EntityOwnershipUtils entityOwnershipUtils;
    private final JobCoordinator coordinator;
    private final BfdStateCache bfdStateCache;
    private final DpnTepStateCache dpnTepStateCache;
    private final DPNTEPsInfoCache dpntePsInfoCache;
    private final TunnelStateCache tunnelStateCache;
    private final UnprocessedNodeConnectorCache unprocessedNCCache;
    private final OvsBridgeRefEntryCache ovsBridgeRefEntryCache;
    private final OvsBridgeEntryCache ovsBridgeEntryCache;
    private final TunnelMonitoringConfig tunnelMonitoringConfig;
    private final DirectTunnelUtils directTunnelUtils;
    private final IInterfaceManager interfaceManager;
    private final TunnelTopologyStateListener tunnelTopologyStateListener;

    @Inject
    public DirectTunnelListenerResolver(final DataBroker dataBroker, final EntityOwnershipUtils entityOwnershipUtils,
                                        final JobCoordinator coordinator, final BfdStateCache bfdStateCache,
                                        final DpnTepStateCache dpnTepStateCache,
                                        final DPNTEPsInfoCache dpntePsInfoCache,
                                        final TunnelStateCache tunnelStateCache,
                                        final UnprocessedNodeConnectorCache unprocessedNCCache,
                                        final OvsBridgeRefEntryCache ovsBridgeRefEntryCache,
                                        final OvsBridgeEntryCache ovsBridgeEntryCache,
                                        final TunnelMonitoringConfig tunnelMonitoringConfig,
                                        final DirectTunnelUtils directTunnelUtils,
                                        final IInterfaceManager interfaceManager) {
        this.dataBroker = dataBroker;
        this.entityOwnershipUtils = entityOwnershipUtils;
        this.coordinator = coordinator;
        this.bfdStateCache = bfdStateCache;
        this.dpnTepStateCache = dpnTepStateCache;
        this.dpntePsInfoCache = dpntePsInfoCache;
        this.tunnelStateCache = tunnelStateCache;
        this.unprocessedNCCache = unprocessedNCCache;
        this.ovsBridgeRefEntryCache = ovsBridgeRefEntryCache;
        this.ovsBridgeEntryCache = ovsBridgeEntryCache;
        this.tunnelMonitoringConfig = tunnelMonitoringConfig;
        this.directTunnelUtils = directTunnelUtils;
        this.interfaceManager = interfaceManager;

        if (interfaceManager.isItmDirectTunnelsEnabled()) {
            LOG.trace("ITM Direct Tunnels is enabled. Initializing the listeners");
            this.tunnelTopologyStateListener = DirectTunnelListenerFactory.getTopologyStateListener(dataBroker,
                    interfaceManager, coordinator, entityOwnershipUtils, directTunnelUtils, dpnTepStateCache,
                    dpntePsInfoCache, bfdStateCache, tunnelStateCache, ovsBridgeRefEntryCache, ovsBridgeEntryCache,
                    tunnelMonitoringConfig);
        } else {
            LOG.trace("ITM Direct Tunnels is disabled. Listeners are not registered");
            this.tunnelTopologyStateListener = null;
        }
    }

    @Override
    public void close() throws Exception {
        if (tunnelTopologyStateListener != null) {
            this.tunnelTopologyStateListener.close();
        }
    }
}
