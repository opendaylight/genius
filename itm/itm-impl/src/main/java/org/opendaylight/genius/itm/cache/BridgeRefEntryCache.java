/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cache;

import java.math.BigInteger;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.mdsalutil.cache.DataObjectCache;
import org.opendaylight.infrautils.caches.CacheProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.BridgeRefInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge.ref.info.BridgeRefEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge.ref.info.BridgeRefEntryKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@Singleton
public class BridgeRefEntryCache extends DataObjectCache<BigInteger, BridgeRefEntry> {

    @Inject
    public BridgeRefEntryCache(DataBroker dataBroker, CacheProvider cacheProvider) {
        super(BridgeRefEntry.class, dataBroker, LogicalDatastoreType.OPERATIONAL,
            InstanceIdentifier.builder(BridgeRefInfo.class).child(BridgeRefEntry.class).build(), cacheProvider,
            (iid, bridgeRefEntry) -> bridgeRefEntry.key().getDpid(),
            dpId -> InstanceIdentifier.builder(BridgeRefInfo.class)
                    .child(BridgeRefEntry.class, new BridgeRefEntryKey(dpId)).build());
    }
}
