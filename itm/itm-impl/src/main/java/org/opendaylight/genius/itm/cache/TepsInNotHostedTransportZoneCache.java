/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cache;

import java.util.Collection;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.mdsalutil.cache.InstanceIdDataObjectCache;
import org.opendaylight.infrautils.caches.CacheProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.NotHostedTransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.not.hosted.transport.zones.TepsInNotHostedTransportZone;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class TepsInNotHostedTransportZoneCache extends InstanceIdDataObjectCache<TepsInNotHostedTransportZone> {
    private static final Logger LOG = LoggerFactory.getLogger(TepsInNotHostedTransportZoneCache.class);

    @Inject
    public TepsInNotHostedTransportZoneCache(DataBroker dataBroker, CacheProvider cacheProvider) {
        super(TepsInNotHostedTransportZone.class, dataBroker, LogicalDatastoreType.OPERATIONAL,
            InstanceIdentifier.builder(NotHostedTransportZones.class).child(TepsInNotHostedTransportZone.class).build(),
                cacheProvider);
    }

    public TepsInNotHostedTransportZone getNotHostedTZFromCache(String transportZoneName) {
        Collection<TepsInNotHostedTransportZone> tepsInNotHostedTransportZoneLst = this.getAllPresent();
        if (!tepsInNotHostedTransportZoneLst.isEmpty()) {
            for (TepsInNotHostedTransportZone tepsInNotHostedTransportZone : tepsInNotHostedTransportZoneLst) {
                if (tepsInNotHostedTransportZone.getZoneName().equals(transportZoneName)) {
                    return tepsInNotHostedTransportZone;
                }
            }
        }
        return null;
    }

}
