/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cache;

import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.aries.blueprint.annotation.service.Service;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.mdsalutil.cache.InstanceIdDataObjectCache;
import org.opendaylight.infrautils.caches.CacheProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TepTypeInternal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelsState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelListKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Caches StateTunnelList objects.
 *
 * @author Thomas Pantelis
 */
@Singleton
@Service(classes = TunnelStateCache.class) // only because TepShowState needs a @Reference to this
public class TunnelStateCache extends InstanceIdDataObjectCache<StateTunnelList> {

    @Inject
    public TunnelStateCache(DataBroker dataBroker, CacheProvider cacheProvider) {
        super(StateTunnelList.class, dataBroker, LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.builder(TunnelsState.class).child(StateTunnelList.class).build(), cacheProvider);
    }

    public boolean isInternalBasedOnState(String tunnelName) throws ReadFailedException {
        Optional<StateTunnelList> stateTunnelList = get(getStateTunnelListIdentifier(tunnelName));
        return stateTunnelList.isPresent() && TepTypeInternal.class
                .equals(stateTunnelList.get().getDstInfo().getTepDeviceType());
    }

    public long getNodeConnectorIdFromInterface(String interfaceName) throws ReadFailedException {
        Optional<StateTunnelList> stateTnl = get(getStateTunnelListIdentifier(interfaceName));
        if (stateTnl.isPresent()) {
            return Long.parseLong(stateTnl.get().getPortNumber());
        }
        return ITMConstants.INVALID_PORT_NO;
    }

    public InstanceIdentifier<StateTunnelList> getStateTunnelListIdentifier(String key) {
        return InstanceIdentifier.builder(TunnelsState.class).child(StateTunnelList.class, new StateTunnelListKey(key))
                .build();
    }
}
