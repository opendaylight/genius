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
import org.opendaylight.genius.itm.cache.DPNTEPsInfoCache;
import org.opendaylight.genius.itm.cache.DpnTepStateCache;
import org.opendaylight.genius.itm.cache.OvsBridgeEntryCache;
import org.opendaylight.genius.itm.cache.UnprocessedNodeConnectorCache;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DirectTunnelListenerCreator implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(DirectTunnelListenerCreator.class);

    private final TunnelTopologyStateListener tunnelTopologyStateListener;

    @Inject
    public DirectTunnelListenerCreator(final DataBroker dataBroker,
                                       final JobCoordinator coordinator,
                                       final EntityOwnershipUtils entityOwnershipUtils,
                                       final IdManagerService idManager,
                                       final IMdsalApiManager mdsalApiManager,
                                       final IInterfaceManager interfaceManager,
                                       final DirectTunnelUtils directTunnelUtils,
                                       final DpnTepStateCache dpnTepStateCache,
                                       final DPNTEPsInfoCache dpntePsInfoCache,
                                       final OvsBridgeEntryCache ovsBridgeEntryCache,
                                       final UnprocessedNodeConnectorCache unprocessedNodeConnectorCache) {

        if (interfaceManager.isItmDirectTunnelsEnabled()) {
            LOG.trace("ITM Direct Tunnels is enabled. Initializing the listeners");
            this.tunnelTopologyStateListener = new TunnelTopologyStateListener(dataBroker, coordinator,
                    entityOwnershipUtils, idManager, mdsalApiManager, directTunnelUtils, dpnTepStateCache,
                    dpntePsInfoCache, ovsBridgeEntryCache, unprocessedNodeConnectorCache);
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
