/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cache;

import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.utils.DpnTepInterfaceInfo;
import org.opendaylight.genius.itm.utils.DpnTepInterfaceInfoBuilder;
import org.opendaylight.genius.mdsalutil.BigIntCacheKey;
import org.opendaylight.genius.mdsalutil.cache.KeyedDataObjectCache;
import org.opendaylight.infrautils.caches.CacheProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnTepsState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.DpnsTeps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.dpns.teps.RemoteDpns;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@Singleton
public class DpnTepStateCache extends KeyedDataObjectCache<BigIntCacheKey, DpnsTeps> {

    private final ConcurrentMap<String, DpnTepInterfaceInfo> dpnTepInterfaceMap = new ConcurrentHashMap<>();

    @Inject
    public DpnTepStateCache(DataBroker dataBroker, CacheProvider cacheProvider) {
        super(DpnsTeps.class, dataBroker, LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.builder(DpnTepsState.class).child(DpnsTeps.class).build(), cacheProvider);
    }

    //overrides added, thereby not populating the default <instanceIdentifier, dataObj> cache
    @Override
    protected void added(InstanceIdentifier<DpnsTeps> path, DpnsTeps dpnsTeps) {
        keyedCache.put(getKey(path, dpnsTeps), dpnsTeps);
        for (RemoteDpns remoteDpns : dpnsTeps.getRemoteDpns()) {
            final String dpn = getDpnId(dpnsTeps.getSourceDpnId(), remoteDpns.getDestinationDpnId());
            DpnTepInterfaceInfo value = new DpnTepInterfaceInfoBuilder()
                    .setTunnelName(remoteDpns.getTunnelName())
                    .setGroupId(dpnsTeps.getGroupId())
                    .setIsMonitoringEnabled(remoteDpns.isMonitoringEnabled())
                    .setIsMonitoringEnabled(remoteDpns.isInternal())
                    .setTunnelType(dpnsTeps.getTunnelType()).build();
            dpnTepInterfaceMap.put(dpn, value);
        }
    }

    @Override
    protected void removed(InstanceIdentifier<DpnsTeps> path, DpnsTeps dpnsTeps) {
        keyedCache.remove(getKey(path, dpnsTeps));
        for (RemoteDpns remoteDpns : dpnsTeps.getRemoteDpns()) {
            dpnTepInterfaceMap.remove(getDpnId(dpnsTeps.getSourceDpnId(), remoteDpns.getDestinationDpnId()));
        }
    }

    @Override
    protected BigIntCacheKey<DpnsTeps> getKey(InstanceIdentifier<DpnsTeps> path, DpnsTeps dpnsTeps) {
        return new BigIntCacheKey<>(dataObjectClass, path, dpnsTeps.getSourceDpnId());
    }

    public DpnTepInterfaceInfo getDpnTepInterface(BigInteger srcDpnId, BigInteger dstDpnId) {
        return dpnTepInterfaceMap.get(getDpnId(srcDpnId, dstDpnId));
    }

    private static String getDpnId(BigInteger src, BigInteger dst) {
        return src + ":" + dst;
    }
}