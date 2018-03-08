/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cache;

import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.mdsalutil.cache.InstanceIdDataObjectCache;
import org.opendaylight.infrautils.caches.CacheProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelsState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Caches StateTunnelList objects.
 *
 * @author Thomas Pantelis
 */
@Singleton
public class TunnelStateCache extends InstanceIdDataObjectCache<StateTunnelList> {

    @Inject
    public TunnelStateCache(DataBroker dataBroker, CacheProvider cacheProvider) {
        super(StateTunnelList.class, dataBroker, LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.builder(TunnelsState.class).child(StateTunnelList.class).build(), cacheProvider);
    }
}
