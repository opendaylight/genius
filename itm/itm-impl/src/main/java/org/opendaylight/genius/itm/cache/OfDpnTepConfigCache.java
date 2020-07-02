/*
 * Copyright (c) 2020 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.cache;

import java.math.BigInteger;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.genius.mdsalutil.cache.DataObjectCache;
import org.opendaylight.infrautils.caches.CacheProvider;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnTepConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.tep.config.OfDpnTep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.tep.config.OfDpnTepKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint64;

@Singleton
public class OfDpnTepConfigCache extends DataObjectCache<BigInteger, OfDpnTep> {

    @Inject
    public OfDpnTepConfigCache(DataBroker dataBroker, CacheProvider cacheProvider) {
        super(OfDpnTep.class, dataBroker, LogicalDatastoreType.CONFIGURATION,
            InstanceIdentifier.builder(DpnTepConfig.class).child(OfDpnTep.class).build(), cacheProvider,
            (iid, dpnsTeps) -> dpnsTeps.getSourceDpnId().toJava(),
            sourceDpnId -> InstanceIdentifier.builder(DpnTepConfig.class)
                    .child(OfDpnTep.class, new OfDpnTepKey(Uint64.valueOf(sourceDpnId))).build());
    }
}
