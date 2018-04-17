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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.utils.DpnTepInterfaceInfo;
import org.opendaylight.genius.itm.utils.DpnTepInterfaceInfoBuilder;
import org.opendaylight.genius.itm.utils.TunnelEndPointInfo;
import org.opendaylight.genius.itm.utils.TunnelEndPointInfoBuilder;
import org.opendaylight.genius.mdsalutil.cache.DataObjectCache;
import org.opendaylight.infrautils.caches.CacheProvider;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBfd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnTepsState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.DpnsTeps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.DpnsTepsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.dpns.teps.RemoteDpns;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.dpns.teps.RemoteDpnsKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DpnTepStateCache extends DataObjectCache<BigInteger, DpnsTeps> {

    private static final Logger LOG = LoggerFactory.getLogger(DpnTepStateCache.class);

    private final DataBroker dataBroker;
    private final DPNTEPsInfoCache dpnTepsInfoCache;
    private final ConcurrentMap<String, DpnTepInterfaceInfo> dpnTepInterfaceMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, TunnelEndPointInfo> tunnelEndpointMap = new ConcurrentHashMap<>();

    @Inject
    public DpnTepStateCache(DataBroker dataBroker, CacheProvider cacheProvider, DPNTEPsInfoCache dpnTepsInfoCache) {
        super(DpnsTeps.class, dataBroker, LogicalDatastoreType.CONFIGURATION,
            InstanceIdentifier.builder(DpnTepsState.class).child(DpnsTeps.class).build(), cacheProvider,
            (iid, dpnsTeps) -> dpnsTeps.getSourceDpnId(),
            sourceDpnId -> InstanceIdentifier.builder(DpnTepsState.class)
                    .child(DpnsTeps.class, new DpnsTepsKey(sourceDpnId)).build());
        this.dataBroker = dataBroker;
        this.dpnTepsInfoCache = dpnTepsInfoCache;
    }

    @Override
    protected void added(InstanceIdentifier<DpnsTeps> path, DpnsTeps dpnsTeps) {
        for (RemoteDpns remoteDpns : dpnsTeps.getRemoteDpns()) {
            final String dpn = getDpnId(dpnsTeps.getSourceDpnId(), remoteDpns.getDestinationDpnId());
            DpnTepInterfaceInfo value = new DpnTepInterfaceInfoBuilder()
                    .setTunnelName(remoteDpns.getTunnelName())
                    .setGroupId(dpnsTeps.getGroupId())
                    .setIsMonitoringEnabled(remoteDpns.isMonitoringEnabled())
                    .setIsInternal(remoteDpns.isInternal())
                    .setTunnelType(dpnsTeps.getTunnelType()).build();
            dpnTepInterfaceMap.put(dpn, value);
            addTunnelEndPointInfoToCache(remoteDpns.getTunnelName(),
                    dpnsTeps.getSourceDpnId().toString(), remoteDpns.getDestinationDpnId().toString());
        }
    }

    @Override
    protected void removed(InstanceIdentifier<DpnsTeps> path, DpnsTeps dpnsTeps) {
        for (RemoteDpns remoteDpns : dpnsTeps.getRemoteDpns()) {
            dpnTepInterfaceMap.remove(getDpnId(dpnsTeps.getSourceDpnId(), remoteDpns.getDestinationDpnId()));
            tunnelEndpointMap.remove(remoteDpns.getTunnelName());
        }
    }

    private DpnTepInterfaceInfo getDpnTepInterface(String srcDpnId, String dstDpnId) {
        return getDpnTepInterface(new BigInteger(srcDpnId), new BigInteger(dstDpnId));
    }

    public DpnTepInterfaceInfo getDpnTepInterface(BigInteger srcDpnId, BigInteger dstDpnId) {
        DpnTepInterfaceInfo  dpnTepInterfaceInfo = dpnTepInterfaceMap.get(getDpnId(srcDpnId, dstDpnId));
        if (dpnTepInterfaceInfo == null) {
            try {
                Optional<DpnsTeps> dpnsTeps = super.get(srcDpnId);
                if (dpnsTeps.isPresent()) {
                    DpnsTeps teps = dpnsTeps.get();
                    teps.getRemoteDpns().forEach(remoteDpns -> {
                        DpnTepInterfaceInfo value = new DpnTepInterfaceInfoBuilder()
                                .setTunnelName(remoteDpns.getTunnelName())
                                .setGroupId(teps.getGroupId())
                                .setIsMonitoringEnabled(remoteDpns.isMonitoringEnabled())
                                .setIsInternal(remoteDpns.isInternal())
                                .setTunnelType(teps.getTunnelType()).build();
                        dpnTepInterfaceMap.putIfAbsent(getDpnId(srcDpnId, remoteDpns.getDestinationDpnId()), value);
                        addTunnelEndPointInfoToCache(remoteDpns.getTunnelName(),
                                teps.getSourceDpnId().toString(), remoteDpns.getDestinationDpnId().toString());
                        }
                    );
                }
            } catch (ReadFailedException e) {
                LOG.error("cache read for dpnID {} in DpnTepStateCache failed ", srcDpnId, e);
            }
        }
        return dpnTepInterfaceMap.get(getDpnId(srcDpnId, dstDpnId));
    }

    public void removeTepFromDpnTepInterfaceConfigDS(BigInteger srcDpnId) throws TransactionCommitFailedException {
        Collection<DpnsTeps> dpnsTeps = this.getAllPresent();
        for (DpnsTeps dpnTep : dpnsTeps) {
            if (!dpnTep.getSourceDpnId().equals(srcDpnId)) {
                List<RemoteDpns> remoteDpns = dpnTep.getRemoteDpns();
                for (RemoteDpns remoteDpn : remoteDpns) {
                    if (remoteDpn.getDestinationDpnId().equals(srcDpnId)) {
                        // Remote the SrcDpnId from the remote List. Remove it from COnfig DS. 4
                        // This will be reflected in cache by the ClusteredDTCN. Not removing it here !
                        //Caution :- Batching Delete !!
                        InstanceIdentifier<RemoteDpns> remoteDpnII =
                                buildRemoteDpnsInstanceIdentifier(dpnTep.getSourceDpnId(),
                                        remoteDpn.getDestinationDpnId());
                        SingleTransactionDataBroker.syncDelete(dataBroker, LogicalDatastoreType.CONFIGURATION,
                                remoteDpnII);
                        break;
                    }
                }
            } else {
                // The source DPn id is the one to be removed
                InstanceIdentifier<DpnsTeps> dpnsTepsII = buildDpnsTepsInstanceIdentifier(dpnTep.getSourceDpnId());
                SingleTransactionDataBroker.syncDelete(dataBroker, LogicalDatastoreType.CONFIGURATION, dpnsTepsII);
            }
        }
    }

    private InstanceIdentifier<DpnsTeps> buildDpnsTepsInstanceIdentifier(BigInteger srcDpnId) {
        return InstanceIdentifier.builder(DpnTepsState.class).child(DpnsTeps.class, new DpnsTepsKey(srcDpnId)).build();
    }

    private InstanceIdentifier<RemoteDpns> buildRemoteDpnsInstanceIdentifier(BigInteger srcDpnId, BigInteger dstDpnId) {
        DpnsTepsKey dpnsTepsKey = new DpnsTepsKey(srcDpnId);
        RemoteDpnsKey remoteDpnsKey = new RemoteDpnsKey(dstDpnId);
        return InstanceIdentifier.builder(DpnTepsState.class).child(DpnsTeps.class, dpnsTepsKey)
                .child(RemoteDpns.class, remoteDpnsKey).build();
    }

    // Given the tunnel name find out if its internal or external
    public boolean isInternal(String tunnelName) {
        TunnelEndPointInfo endPointInfo = getTunnelEndPointInfoFromCache(tunnelName);
        if (endPointInfo != null) {
            DpnTepInterfaceInfo dpnTepInfo = getDpnTepInterface(endPointInfo.getSrcEndPointInfo(),
                    endPointInfo.getDstEndPointInfo());
            return dpnTepInfo != null && dpnTepInfo.isInternal();
        }
        return false;
    }

    public boolean isConfigAvailable(String tunnelName) {
        TunnelEndPointInfo endPointInfo = getTunnelEndPointInfoFromCache(tunnelName);
        if (endPointInfo != null) {
            DpnTepInterfaceInfo dpnTepInfo = getDpnTepInterface(endPointInfo.getSrcEndPointInfo(),
                    endPointInfo.getDstEndPointInfo());
            return dpnTepInfo != null;
        }
        return false;
    }

    public DpnTepInterfaceInfo getTunnelFromCache(String tunnelName) {
        TunnelEndPointInfo endPointInfo = getTunnelEndPointInfoFromCache(tunnelName);
        return getDpnTepInterface(endPointInfo.getSrcEndPointInfo(), endPointInfo.getDstEndPointInfo());
    }

    private String getDpnId(BigInteger src, BigInteger dst) {
        return src + ":" + dst;
    }

    public Interface getInterfaceFromCache(String tunnelName) {
        TunnelEndPointInfo endPointInfo = getTunnelEndPointInfoFromCache(tunnelName);
        BigInteger srcDpnId = new BigInteger(endPointInfo.getSrcEndPointInfo());
        BigInteger dstDpnId = new BigInteger(endPointInfo.getDstEndPointInfo());
        Interface iface = null ;
        int monitoringInt = 1000;
        DpnTepInterfaceInfo dpnTepInfo = getDpnTepInterface(srcDpnId, dstDpnId);
        if (dpnTepInfo != null) {
            List<DPNTEPsInfo> srcDpnTEPsInfo = dpnTepsInfoCache
                    .getDPNTepListFromDPNId(Collections.singletonList(srcDpnId));
            List<DPNTEPsInfo> dstDpnTEPsInfo = dpnTepsInfoCache
                    .getDPNTepListFromDPNId(Collections.singletonList(dstDpnId));
            iface = ItmUtils.buildTunnelInterface(srcDpnId, tunnelName,
                    String.format("%s %s", ItmUtils.convertTunnelTypetoString(dpnTepInfo.getTunnelType()),
                            "Trunk Interface"), true, dpnTepInfo.getTunnelType(),
                    srcDpnTEPsInfo.get(0).getTunnelEndPoints().get(0).getIpAddress(),
                    dstDpnTEPsInfo.get(0).getTunnelEndPoints().get(0).getIpAddress(),
                    srcDpnTEPsInfo.get(0).getTunnelEndPoints().get(0).getGwIpAddress(),
                    srcDpnTEPsInfo.get(0).getTunnelEndPoints().get(0).getVLANID(), true,
                    dpnTepInfo.isMonitoringEnabled(), TunnelMonitoringTypeBfd.class,
                    monitoringInt, true, null);
        }
        return iface;
    }

    //Start: TunnelEndPoint Cache accessors
    private void addTunnelEndPointInfoToCache(String tunnelName, String srcEndPtInfo, String dstEndPtInfo) {
        TunnelEndPointInfo tunnelEndPointInfo = new TunnelEndPointInfoBuilder().setSrcEndPointInfo(srcEndPtInfo)
                .setDstEndPointInfo(dstEndPtInfo).build();
        tunnelEndpointMap.put(tunnelName, tunnelEndPointInfo);
    }

    public TunnelEndPointInfo getTunnelEndPointInfoFromCache(String tunnelName) {
        return tunnelEndpointMap.get(tunnelName);
    }
}
