/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.itmdirecttunnels.factory;

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
import org.opendaylight.genius.itm.itmdirecttunnels.listeners.TerminationPointStateListener;
import org.opendaylight.genius.itm.itmdirecttunnels.listeners.TunnelInventoryStateListener;
import org.opendaylight.genius.itm.itmdirecttunnels.listeners.TunnelTopologyStateListener;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;

public final class DirectTunnelListenerFactory {

    private DirectTunnelListenerFactory() {
    }

    public static TerminationPointStateListener
        getTerminationPointStateListener(final DataBroker dataBroker, final EntityOwnershipUtils entityOwnershipUtils,
                                     final JobCoordinator coordinator, final BfdStateCache bfdStateCache,
                                     final DpnTepStateCache dpnTepStateCache,
                                     final TunnelStateCache tunnelStateCache,
                                     final DirectTunnelUtils directTunnelUtils) {
        return new TerminationPointStateListener(dataBroker, entityOwnershipUtils, coordinator, bfdStateCache,
                    dpnTepStateCache,tunnelStateCache, directTunnelUtils) ;
    }

    public static TunnelInventoryStateListener
        getTunnelInventoryStateListener(final DataBroker dataBroker, final IdManagerService idManagerService,
                                    final IMdsalApiManager mdsalApiManager, final IInterfaceManager interfaceManager,
                                    final JobCoordinator coordinator, final EntityOwnershipUtils entityOwnershipUtils,
                                    final DirectTunnelUtils directTunnelUtils, final DPNTEPsInfoCache dpntePsInfoCache,
                                    final TunnelStateCache tunnelStateCache, final DpnTepStateCache dpnTepStateCache,
                                    final UnprocessedNodeConnectorCache unprocessedNCCache) {
        return new TunnelInventoryStateListener(dataBroker, idManagerService, mdsalApiManager, interfaceManager,
                    coordinator, entityOwnershipUtils, directTunnelUtils, dpntePsInfoCache, tunnelStateCache,
                    dpnTepStateCache, unprocessedNCCache) ;
    }

    public static TunnelTopologyStateListener
        getTopologyStateListener(final DataBroker dataBroker, final IInterfaceManager interfaceManager,
                             final JobCoordinator coordinator, final EntityOwnershipUtils entityOwnershipUtils,
                             final DirectTunnelUtils directTunnelUtils, final DpnTepStateCache dpnTepStateCache,
                             final DPNTEPsInfoCache dpntePsInfoCache, final BfdStateCache bfdStateCache,
                             final TunnelStateCache tunnelStateCache,
                             final OvsBridgeRefEntryCache ovsBridgeRefEntryCache,
                             final OvsBridgeEntryCache ovsBridgeEntryCache,
                             final TunnelMonitoringConfig tunnelMonitoringConfig)  {
        return new TunnelTopologyStateListener(dataBroker, interfaceManager, coordinator, entityOwnershipUtils,
                directTunnelUtils, dpnTepStateCache, dpntePsInfoCache, bfdStateCache, tunnelStateCache,
                ovsBridgeRefEntryCache, ovsBridgeEntryCache, tunnelMonitoringConfig) ;
    }
}
