/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
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
import org.opendaylight.genius.mdsalutil.BigIntCacheKey;
import org.opendaylight.genius.mdsalutil.cache.KeyedDataObjectCache;
import org.opendaylight.infrautils.caches.CacheProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.BridgeTunnelInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.bridge.tunnel.info.OvsBridgeEntry;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@Singleton
public class OvsBridgeEntryCache extends KeyedDataObjectCache<BigIntCacheKey, OvsBridgeEntry> {

    @Inject
    public OvsBridgeEntryCache(DataBroker dataBroker, CacheProvider cacheProvider) {
        super(OvsBridgeEntry.class, dataBroker, LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.builder(BridgeTunnelInfo.class).child(OvsBridgeEntry.class).build(), cacheProvider);
    }

    @Override
    protected BigIntCacheKey<OvsBridgeEntry> getKey(InstanceIdentifier<OvsBridgeEntry> path,
                                                    OvsBridgeEntry ovsBridgeEntry) {
        return new BigIntCacheKey<>(dataObjectClass, path, ovsBridgeEntry.getKey().getDpid());
    }
}
