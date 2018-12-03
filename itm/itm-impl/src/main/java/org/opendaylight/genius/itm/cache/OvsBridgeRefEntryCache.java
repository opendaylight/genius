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

import org.apache.aries.blueprint.annotation.service.Service;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.mdsalutil.cache.DataObjectCache;
import org.opendaylight.infrautils.caches.CacheProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.OvsBridgeRefInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.ovs.bridge.ref.info.OvsBridgeRefEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.ovs.bridge.ref.info.OvsBridgeRefEntryKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@Singleton
@Service(classes = OvsBridgeRefEntryCache.class) // only because TepShowBridges needs a @Reference to this
public class OvsBridgeRefEntryCache extends DataObjectCache<BigInteger, OvsBridgeRefEntry> {

    @Inject
    public OvsBridgeRefEntryCache(DataBroker dataBroker, CacheProvider cacheProvider) {
        super(OvsBridgeRefEntry.class, dataBroker, LogicalDatastoreType.OPERATIONAL,
            InstanceIdentifier.builder(OvsBridgeRefInfo.class).child(OvsBridgeRefEntry.class).build(), cacheProvider,
            (iid, ovsBridgeRefEntry) -> ovsBridgeRefEntry.key().getDpid(),
            dpId -> InstanceIdentifier.builder(OvsBridgeRefInfo.class)
                    .child(OvsBridgeRefEntry.class, new OvsBridgeRefEntryKey(dpId)).build());
    }
}
