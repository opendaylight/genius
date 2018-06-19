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
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.mdsalutil.cache.InstanceIdDataObjectCache;
import org.opendaylight.infrautils.caches.CacheProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.tunnel.end.points.TzMembership;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Caches DPNTEPsInfo objects.
 *
 * @author Thomas Pantelis
 */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
@Singleton
public class DPNTEPsInfoCache extends InstanceIdDataObjectCache<DPNTEPsInfo> {
    private final ConcurrentMap<BigInteger, List<TzMembership>> tepTransportZonesMap = new ConcurrentHashMap<>();

    @Inject
    public DPNTEPsInfoCache(DataBroker dataBroker, CacheProvider cacheProvider) {
        super(DPNTEPsInfo.class, dataBroker, LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.builder(DpnEndpoints.class).child(DPNTEPsInfo.class).build(), cacheProvider);
    }

    @Override
    protected void added(InstanceIdentifier<DPNTEPsInfo> path, DPNTEPsInfo dpnTepsInfo) {
        for (TunnelEndPoints tep : dpnTepsInfo.getTunnelEndPoints()) {
            tepTransportZonesMap.put(dpnTepsInfo.getDPNID(), tep.getTzMembership());
            // since there can be only one tunnel-endpoint, so come out of loop.
            break;
        }
    }

    @Override
    protected void removed(InstanceIdentifier<DPNTEPsInfo> path, DPNTEPsInfo dpnTepsInfo) {
        tepTransportZonesMap.remove(dpnTepsInfo.getDPNID());
    }

    public List<DPNTEPsInfo> getDPNTepListFromDPNId(List<BigInteger> dpnIds) {
        Collection<DPNTEPsInfo> meshedDpnList = this.getAllPresent() ;
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

    public Optional<DPNTEPsInfo> getDPNTepFromDPNId(BigInteger dpnId) {
        Collection<DPNTEPsInfo> meshedDpnList = this.getAllPresent() ;
        return meshedDpnList.stream().filter(info -> dpnId.equals(info.getDPNID())).findFirst();
    }

    public List<TzMembership> getTransportZoneOfVtep(BigInteger dpnId) {
        return tepTransportZonesMap.get(dpnId);
    }
}
