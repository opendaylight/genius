/*
 * Copyright (c) 2019 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.listeners;

import org.opendaylight.genius.itm.cache.DpnTepStateCache;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.serviceutils.tools.listener.AbstractClusteredSyncDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnTepsState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.DpnsTeps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.dpns.teps.RemoteDpns;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteDpnListener extends AbstractClusteredSyncDataTreeChangeListener<RemoteDpns> {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteDpnListener.class);

    private final DpnTepStateCache dpnTepStateCache;

    public RemoteDpnListener(final DataBroker dataBroker, final DpnTepStateCache dpnTepStateCache) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(DpnTepsState.class).child(DpnsTeps.class).child(RemoteDpns.class));
        this.dpnTepStateCache = dpnTepStateCache;
        super.register();
    }

    public void remove(InstanceIdentifier<RemoteDpns> key, RemoteDpns remoteDpns) {
        final String tunnelName = remoteDpns.getTunnelName();
        LOG.debug("Removing TunnelEndPointInfoCache on RemoteDpns removal for tunnelName {}", tunnelName);
        dpnTepStateCache.removeFromTunnelEndPointMap(tunnelName);
    }
}