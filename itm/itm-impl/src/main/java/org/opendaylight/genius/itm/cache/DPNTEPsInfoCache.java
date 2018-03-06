/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cache;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.mdsalutil.cache.DataObjectCache;
import org.opendaylight.infrautils.caches.CacheProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Caches DPNTEPsInfo objects.
 *
 * @author Thomas Pantelis
 */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
@Singleton
public class DPNTEPsInfoCache extends DataObjectCache<DPNTEPsInfo> {

    @Inject
    public DPNTEPsInfoCache(DataBroker dataBroker, CacheProvider cacheProvider) {
        super(DPNTEPsInfo.class, dataBroker, LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.builder(DpnEndpoints.class).child(DPNTEPsInfo.class).build(), cacheProvider);
    }

    public static List<DPNTEPsInfo> getDPNTepListFromDPNId(DPNTEPsInfoCache dpntePsInfoCache, List<BigInteger> dpnIds) {
        Collection<DPNTEPsInfo> meshedDpnList = dpntePsInfoCache.getAllPresent() ;
        List<DPNTEPsInfo> cfgDpnList = new ArrayList<>();
        for (BigInteger dpnId : dpnIds) {
            for (DPNTEPsInfo teps : meshedDpnList) {
                if (dpnId.equals(teps.getDPNID())) {
                    cfgDpnList.add(teps);
                }
            }
        }
        return cfgDpnList;
    }

    public static DPNTEPsInfo getDPNTepFromDPNId(DPNTEPsInfoCache dpntePsInfoCache, BigInteger dpnId) {
        Collection<DPNTEPsInfo> meshedDpnList = dpntePsInfoCache.getAllPresent() ;
        DPNTEPsInfo cfgDpn = null;
        meshedDpnList.stream().filter(info -> dpnId.equals(info.getDPNID())).findFirst();
        for (DPNTEPsInfo teps : meshedDpnList) {
            if (dpnId.equals(teps.getDPNID())) {
                cfgDpn = teps;
            }
        }

        return cfgDpn;
    }
}
