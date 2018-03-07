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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.itm.utils.DpnTepInterfaceInfo;
import org.opendaylight.genius.itm.utils.DpnTepInterfaceInfoBuilder;
import org.opendaylight.genius.mdsalutil.cache.DataObjectCache;
import org.opendaylight.infrautils.caches.CacheProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnTepsState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.DpnsTeps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.DpnsTepsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.dpns.teps.RemoteDpns;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DpnTepStateCache extends DataObjectCache<BigInteger, DpnsTeps> {

    private static final Logger LOG = LoggerFactory.getLogger(DpnTepStateCache.class);

    private final ConcurrentMap<String, DpnTepInterfaceInfo> dpnTepInterfaceMap = new ConcurrentHashMap<>();

    @Inject
    public DpnTepStateCache(DataBroker dataBroker, CacheProvider cacheProvider) {
        super(DpnsTeps.class, dataBroker, LogicalDatastoreType.OPERATIONAL,
            InstanceIdentifier.builder(DpnTepsState.class).child(DpnsTeps.class).build(), cacheProvider,
            (iid, dpnsTeps) -> dpnsTeps.getSourceDpnId(),
            sourceDpnId -> InstanceIdentifier.builder(DpnTepsState.class)
                    .child(DpnsTeps.class, new DpnsTepsKey(sourceDpnId)).build());
    }

    @Override
    protected void added(InstanceIdentifier<DpnsTeps> path, DpnsTeps dpnsTeps) {
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
        for (RemoteDpns remoteDpns : dpnsTeps.getRemoteDpns()) {
            dpnTepInterfaceMap.remove(getDpnId(dpnsTeps.getSourceDpnId(), remoteDpns.getDestinationDpnId()));
        }
    }

    public DpnTepInterfaceInfo getDpnTepInterface(BigInteger srcDpnId, BigInteger dstDpnId) {
        DpnTepInterfaceInfo  dpnTepInterfaceInfo = dpnTepInterfaceMap.get(getDpnId(srcDpnId, dstDpnId));
        if (dpnTepInterfaceInfo == null) {
            try {
                Optional<DpnsTeps> dpnsTeps = super.get(srcDpnId);
                if (dpnsTeps.isPresent()) {
                    DpnsTeps teps = dpnsTeps.get();
                    teps.getRemoteDpns().forEach(remoteDpns
                        -> dpnTepInterfaceMap.putIfAbsent(getDpnId(srcDpnId, remoteDpns.getDestinationDpnId()),
                        new DpnTepInterfaceInfoBuilder()
                            .setTunnelName(remoteDpns.getTunnelName())
                            .setGroupId(teps.getGroupId())
                            .setIsMonitoringEnabled(remoteDpns.isMonitoringEnabled())
                            .setIsMonitoringEnabled(remoteDpns.isInternal())
                            .setTunnelType(teps.getTunnelType()).build()
                        ));
                }
            } catch (ReadFailedException e) {
                LOG.debug("cache read for dpnID {} in DpnTepStateCache failed", srcDpnId);
            }
        }
        return dpnTepInterfaceMap.get(getDpnId(srcDpnId, dstDpnId));
    }

    private static String getDpnId(BigInteger src, BigInteger dst) {
        return src + ":" + dst;
    }
}
