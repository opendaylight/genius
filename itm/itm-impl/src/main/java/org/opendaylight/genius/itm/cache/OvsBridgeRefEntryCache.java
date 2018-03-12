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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.OvsBridgeRefInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.ovs.bridge.ref.info.OvsBridgeRefEntry;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@Singleton
public class OvsBridgeRefEntryCache extends KeyedDataObjectCache<BigIntCacheKey, OvsBridgeRefEntry> {

    @Inject
    public OvsBridgeRefEntryCache(DataBroker dataBroker, CacheProvider cacheProvider) {
        super(OvsBridgeRefEntry.class, dataBroker, LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.builder(OvsBridgeRefInfo.class).child(OvsBridgeRefEntry.class).build(),
                cacheProvider);
    }

    @Override
    protected BigIntCacheKey<OvsBridgeRefEntry> getKey(InstanceIdentifier<OvsBridgeRefEntry> path,
                                                       OvsBridgeRefEntry ovsBridgeRefEntry) {
        return new BigIntCacheKey<>(dataObjectClass, path, ovsBridgeRefEntry.getKey().getDpid());
    }
}
