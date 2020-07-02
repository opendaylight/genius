/*
 * Copyright (c) 2020 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cache;

import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.genius.mdsalutil.cache.DataObjectCache;
import org.opendaylight.infrautils.caches.CacheProvider;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.OfTepsState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.of.teps.state.OfTep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.of.teps.state.OfTepKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Singleton
public class OfTepStateCache extends DataObjectCache<String, OfTep> {

    private static final Logger LOG = LoggerFactory.getLogger(OfTepStateCache.class);

    @Inject
    public OfTepStateCache(DataBroker dataBroker, CacheProvider cacheProvider) {
        super(OfTep.class, dataBroker, LogicalDatastoreType.OPERATIONAL,
            InstanceIdentifier.builder(OfTepsState.class).child(OfTep.class).build(), cacheProvider,
            (iid, ofTepList) -> ofTepList.getOfPortName(), ofPortName -> InstanceIdentifier.builder(OfTepsState.class)
                    .child(OfTep.class, new OfTepKey(ofPortName)).build());
    }
}
