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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.OvsBridgeRefInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.bridge.tunnel.info.OvsBridgeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.ovs.bridge.ref.info.OvsBridgeRefEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.ovs.bridge.ref.info.OvsBridgeRefEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class OvsBridgeRefEntryCache extends DataObjectCache<BigInteger, OvsBridgeRefEntry> {

    private static final Logger LOG = LoggerFactory.getLogger(OvsBridgeRefEntryCache.class);

    public final DataBroker dataBroker;
    public final OvsBridgeEntryCache ovsBridgeEntryCache;

    @Inject
    public OvsBridgeRefEntryCache(DataBroker dataBroker, CacheProvider cacheProvider,
                                  OvsBridgeEntryCache ovsBridgeEntryCache) {
        super(OvsBridgeRefEntry.class, dataBroker, LogicalDatastoreType.OPERATIONAL,
            InstanceIdentifier.builder(OvsBridgeRefInfo.class).child(OvsBridgeRefEntry.class).build(), cacheProvider,
            (iid, ovsBridgeRefEntry) -> ovsBridgeRefEntry.getKey().getDpid(),
            dpId -> InstanceIdentifier.builder(OvsBridgeRefInfo.class)
                    .child(OvsBridgeRefEntry.class, new OvsBridgeRefEntryKey(dpId)).build());
        this.dataBroker = dataBroker;
        this.ovsBridgeEntryCache = ovsBridgeEntryCache;
    }

    public OvsdbBridgeRef getOvsdbBridgeRef(BigInteger dpId) {
        OvsBridgeRefEntry bridgeRefEntry = getOvsBridgeRefEntryFromOperDS(dpId);
        if (bridgeRefEntry == null) {
            // bridge ref entry will be null if the bridge is disconnected from controller.
            // In that case, fetch bridge reference from bridge interface entry config DS
            OvsBridgeEntry bridgeEntry = ovsBridgeEntryCache.getOvsBridgeEntryFromConfigDS(dpId);
            if (bridgeEntry == null) {
                return null;
            }
            return bridgeEntry.getOvsBridgeReference();
        }
        return bridgeRefEntry.getOvsBridgeReference();
    }

    public OvsBridgeRefEntry getOvsBridgeRefEntryFromOperDS(BigInteger dpId) {
        try {
            Optional<OvsBridgeRefEntry> bridgeRefEntryOptional = get(dpId);
            if (bridgeRefEntryOptional.isPresent()) {
                return bridgeRefEntryOptional.get();
            }
        } catch (ReadFailedException e) {
            LOG.error("OvsBridgeRefEntry cache read failed for {}", dpId);
        }

        OvsBridgeRefEntryKey bridgeRefEntryKey = new OvsBridgeRefEntryKey(dpId);
        InstanceIdentifier<OvsBridgeRefEntry> bridgeRefEntryIid = getOvsBridgeRefEntryIdentifier(bridgeRefEntryKey);
        return ItmUtils.read(LogicalDatastoreType.OPERATIONAL, bridgeRefEntryIid, dataBroker).orNull();
    }

    private InstanceIdentifier<OvsBridgeRefEntry>
        getOvsBridgeRefEntryIdentifier(OvsBridgeRefEntryKey bridgeRefEntryKey) {
        return InstanceIdentifier.builder(OvsBridgeRefInfo.class)
                .child(OvsBridgeRefEntry.class, bridgeRefEntryKey).build();
    }
}
