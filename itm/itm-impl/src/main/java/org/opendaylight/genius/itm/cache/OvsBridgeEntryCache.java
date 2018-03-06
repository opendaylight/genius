/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cache;

import com.google.common.base.Optional;
import java.math.BigInteger;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.mdsalutil.cache.DataObjectCache;
import org.opendaylight.infrautils.caches.CacheProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.BridgeTunnelInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.bridge.tunnel.info.OvsBridgeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.bridge.tunnel.info.OvsBridgeEntryKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class OvsBridgeEntryCache extends DataObjectCache<BigInteger, OvsBridgeEntry> {

    private static final Logger LOG = LoggerFactory.getLogger(OvsBridgeEntryCache.class);

    private final DataBroker dataBroker;

    @Inject
    public OvsBridgeEntryCache(DataBroker dataBroker, CacheProvider cacheProvider) {
        super(OvsBridgeEntry.class, dataBroker, LogicalDatastoreType.CONFIGURATION,
            InstanceIdentifier.builder(BridgeTunnelInfo.class).child(OvsBridgeEntry.class).build(), cacheProvider,
            (iid, ovsBridgeEntry) -> ovsBridgeEntry.getKey().getDpid(),
            dpId -> InstanceIdentifier.builder(BridgeTunnelInfo.class)
                    .child(OvsBridgeEntry.class, new OvsBridgeEntryKey(dpId)).build());
        this.dataBroker = dataBroker;
    }

    public OvsBridgeEntry getOvsBridgeEntryFromConfigDS(BigInteger dpnId) {

        try {
            Optional<OvsBridgeEntry> bridgeEntry = get(dpnId);
            if (bridgeEntry.isPresent()) {
                return bridgeEntry.get();
            }
        } catch (ReadFailedException e) {
            LOG.error("OvsBridgeEntry cache read failed for {}", dpnId);
        }
        OvsBridgeEntryKey bridgeEntryKey = new OvsBridgeEntryKey(dpnId);
        InstanceIdentifier<OvsBridgeEntry> bridgeEntryInstanceIdentifier = getOvsBridgeEntryIdentifier(bridgeEntryKey);
        LOG.debug("Trying to retrieve bridge entry from config for Id: {}", bridgeEntryInstanceIdentifier);
        return ItmUtils.read(LogicalDatastoreType.CONFIGURATION, bridgeEntryInstanceIdentifier, dataBroker).orNull();
    }

    private InstanceIdentifier<OvsBridgeEntry> getOvsBridgeEntryIdentifier(OvsBridgeEntryKey bridgeEntryKey) {
        return InstanceIdentifier.builder(BridgeTunnelInfo.class).child(OvsBridgeEntry.class, bridgeEntryKey)
                .build();
    }
}
