/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
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
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.DpnTepInterfaceInfo;
import org.opendaylight.genius.mdsalutil.cache.DataObjectCache;
import org.opendaylight.infrautils.caches.CacheProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnTepsState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.DpnsTeps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.dpns.teps.RemoteDpns;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
@Singleton
public class DPNTepStateCache extends DataObjectCache<DpnsTeps> {
    private static final Logger LOG = LoggerFactory.getLogger(DPNTepStateCache.class);

    private final ConcurrentHashMap<BigInteger, DpnsTeps> dpnsTepsMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DpnTepInterfaceInfo> dpnTepInterfaceMap = new ConcurrentHashMap<>();

    @Inject
    public DPNTepStateCache(DataBroker dataBroker, CacheProvider cacheProvider, IInterfaceManager interfaceManager) {
        super(DpnsTeps.class, dataBroker, LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.builder(DpnTepsState.class).child(DpnsTeps.class).build(), cacheProvider,
                interfaceManager.isItmDirectTunnelsEnabled());
    }

    @Override
    protected void added(InstanceIdentifier<DpnsTeps> path, DpnsTeps dpnsTeps) {
        dpnsTepsMap.put(dpnsTeps.getSourceDpnId(), dpnsTeps);
        for (RemoteDpns remoteDpns : dpnsTeps.getRemoteDpns()) {
            final String dpn = dpnsTeps.getSourceDpnId() + ":" + remoteDpns.getDestinationDpnId();
            DpnTepInterfaceInfo value = DpnTepInterfaceInfo.builder()
                    .tunnelName(remoteDpns.getTunnelName())
                    .groupId(dpnsTeps.getGroupId())
                    .monitoringEnabled(remoteDpns.isMonitoringEnabled())
                    .isInternal(remoteDpns.isInternal())
                    .tunnelType(dpnsTeps.getTunnelType()).build();
            dpnTepInterfaceMap.put(dpn, value);
        }
    }

    @Override
    protected void removed(InstanceIdentifier<DpnsTeps> path, DpnsTeps dpnsTeps) {
        dpnsTepsMap.remove(dpnsTeps.getSourceDpnId());
        for (RemoteDpns remoteDpns : dpnsTeps.getRemoteDpns()) {
            String key = dpnsTeps.getSourceDpnId().toString() + ":" + remoteDpns.getDestinationDpnId().toString();
            dpnTepInterfaceMap.remove(key);
        }
    }

    public DpnsTeps getDpnsTepsFromCache(BigInteger dpnId) {
        return dpnsTepsMap.get(dpnId);
    }

    public DpnTepInterfaceInfo getDpnTepInterfaceFromCache(BigInteger srcDpnId, BigInteger dstDpnId) {
        return dpnTepInterfaceMap.get(srcDpnId.toString() + ":" + dstDpnId.toString());
    }

    public List<DpnsTeps> getAllDpnsTeps() {
        List<DpnsTeps> dpnsTeps = new ArrayList<>();
        Collection<DpnsTeps> values = dpnsTepsMap.values();
        for (DpnsTeps value : values) {
            dpnsTeps.add(value);
        }
        return dpnsTeps;
    }
}