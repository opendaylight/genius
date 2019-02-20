/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cache;

import com.google.common.base.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.mdsalutil.cache.InstanceIdDataObjectCache;
import org.opendaylight.infrautils.caches.CacheProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Caches TransportZones objects.
 *
 * @author Tarun Thakur
 */
@Singleton
public class TransportZonesCache extends InstanceIdDataObjectCache<TransportZone> {
    private static final Logger LOG = LoggerFactory.getLogger(TransportZonesCache.class);

    @Inject
    public TransportZonesCache(DataBroker dataBroker, CacheProvider cacheProvider) {
        super(TransportZone.class, dataBroker, LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.builder(TransportZones.class).child(TransportZone.class).build(), cacheProvider);
    }

    public TransportZone getTransportZoneFromCache(String tzName) {
        try {
            Optional<TransportZone> optTransportZone = get(ItmUtils.getTZInstanceIdentifier(tzName));
            if (optTransportZone.isPresent()) {
                return optTransportZone.get();
            }
        } catch (ReadFailedException e) {
            LOG.error("While reading TZ: {} from TransportZonesCache, Exception occured {}", tzName, e.getMessage());
        }
        return null;
    }
}
