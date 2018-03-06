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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.itm.impl.ITMBatchingUtils;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.genius.itm.utils.DpnTepInterfaceInfo;
import org.opendaylight.genius.itm.utils.DpnTepInterfaceInfoBuilder;
import org.opendaylight.genius.itm.utils.TunnelEndPointInfo;
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
    private final DirectTunnelUtils directTunnelUtils;
    private final DPNTEPsInfoCache dpnTepsInfoCache;
    private final ConcurrentMap<String, DpnTepInterfaceInfo> dpnTepInterfaceMap = new ConcurrentHashMap<>();

    @Inject
    public DpnTepStateCache(DataBroker dataBroker, CacheProvider cacheProvider, DirectTunnelUtils directTunnelUtils,
                            DPNTEPsInfoCache dpnTepsInfoCache) {
        super(DpnsTeps.class, dataBroker, LogicalDatastoreType.OPERATIONAL,
            InstanceIdentifier.builder(DpnTepsState.class).child(DpnsTeps.class).build(), cacheProvider,
            (iid, dpnsTeps) -> dpnsTeps.getSourceDpnId(),
            sourceDpnId -> InstanceIdentifier.builder(DpnTepsState.class)
                    .child(DpnsTeps.class, new DpnsTepsKey(sourceDpnId)).build());
        this.dataBroker = dataBroker;
        this.directTunnelUtils = directTunnelUtils;
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
                    .setIsMonitoringEnabled(remoteDpns.isInternal())
                    .setTunnelType(dpnsTeps.getTunnelType()).build();
            dpnTepInterfaceMap.put(dpn, value);
            directTunnelUtils.addTunnelEndPointInfoToCache(remoteDpns.getTunnelName(),
                    dpnsTeps.getSourceDpnId().toString(), remoteDpns.getDestinationDpnId().toString());
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

    private DpnTepInterfaceInfo getDpnTepInterfaceInfo(String srcDpnId, String dstDpnId) {
        return dpnTepInterfaceMap.get(getDpnId(srcDpnId,dstDpnId));
    }

    private DpnTepInterfaceInfo getDpnTepInterfaceInfo(BigInteger srcDpnId, BigInteger dstDpnId) {
        return dpnTepInterfaceMap.get(getDpnId(srcDpnId,dstDpnId));
    }

    public void removeFromDpnTepInterfaceCache(DpnsTeps dpnsTeps) {
        for (RemoteDpns remoteDpns : dpnsTeps.getRemoteDpns()) {
            String key = getDpnId(dpnsTeps.getSourceDpnId(), remoteDpns.getDestinationDpnId());
            dpnTepInterfaceMap.remove(key);
        }
    }

    public void removeFromDpnTepInterfaceCache(String srcDstDpnId) {
        dpnTepInterfaceMap.remove(srcDstDpnId);
    }

    public void removeTepFromDpnTepInterfaceConfigDS(BigInteger srcDpnId) {
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
                        ITMBatchingUtils.delete(remoteDpnII, ITMBatchingUtils.EntityType.DEFAULT_CONFIG);
                        break;
                    }
                }
            } else {
                // The source DPn id is the one to be removed
                InstanceIdentifier<DpnsTeps> dpnsTepsII = buildDpnsTepsInstanceIdentifier(dpnTep.getSourceDpnId());
                ITMBatchingUtils.delete(dpnsTepsII, ITMBatchingUtils.EntityType.DEFAULT_CONFIG);
            }
        }
    }

    // Given the source and destination DPN id get the tunnel name
    public String getInterfaceNameFromDPNIds(BigInteger srcDpnId, BigInteger dstDpnId) {
        DpnTepInterfaceInfo dpnTepInterfaceInfo = getDpnTepInterfaceInfo(srcDpnId, dstDpnId);
        if (dpnTepInterfaceInfo != null) {
            return dpnTepInterfaceInfo.getTunnelName();
        } else {
            return getInterfaceNameFromDS(srcDpnId, dstDpnId);// Fetch it from DS
        }
    }

    public String getInterfaceNameFromDS(BigInteger srcDpnId, BigInteger dstDpnId) {
        InstanceIdentifier<DpnsTeps> dpnsTepsII = buildDpnsTepsInstanceIdentifier(srcDpnId);
        Optional<DpnsTeps> dpnsTeps = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, dpnsTepsII, dataBroker);
        if (dpnsTeps.isPresent()) {
            List<RemoteDpns> remoteDpns = dpnsTeps.get().getRemoteDpns();
            if (remoteDpns != null) {
                for (RemoteDpns remoteDpn : remoteDpns) {
                    if (remoteDpn.getDestinationDpnId().equals(dstDpnId)) {
                        return remoteDpn.getTunnelName();// Get tunnel Name
                    }
                }
                LOG.debug("Destination DPN supplied for getInterfaceName is not valid {}", dstDpnId);
            }
        } else {
            LOG.debug("Source DPN supplied for getInterfaceName is not valid {}", srcDpnId);
        }
        return null;
    }

    // Given the tunnel name find out if its internal or external
    public boolean isInternal(String tunnelName) {
        TunnelEndPointInfo endPointInfo = directTunnelUtils.getTunnelEndPointInfoFromCache(tunnelName);
        if (endPointInfo != null) {
            DpnTepInterfaceInfo dpnTepInfo = getDpnTepInterfaceInfo(endPointInfo.getSrcEndPointInfo(),
                    endPointInfo.getDstEndPointInfo());
            return dpnTepInfo != null && dpnTepInfo.isInternal();
        }
        return false;
    }

    public boolean isConfigAvailable(String tunnelName) {
        TunnelEndPointInfo endPointInfo = directTunnelUtils.getTunnelEndPointInfoFromCache(tunnelName);
        if (endPointInfo != null) {
            DpnTepInterfaceInfo dpnTepInfo = getDpnTepInterfaceInfo(endPointInfo.getSrcEndPointInfo(),
                    endPointInfo.getDstEndPointInfo());
            return dpnTepInfo != null;
        }
        return false;
    }

    public DpnTepInterfaceInfo getTunnelFromConfigDS(String tunnelName) {
        TunnelEndPointInfo endPointInfo = directTunnelUtils.getTunnelEndPointInfoFromCache(tunnelName);
        DpnTepInterfaceInfo dpnTepInfo = null ;
        if (endPointInfo != null) {
            dpnTepInfo = getDpnTepInterfaceInfo(endPointInfo.getSrcEndPointInfo(), endPointInfo.getDstEndPointInfo());
            if (dpnTepInfo != null) {
                return dpnTepInfo;
            } else {
                //TODO read if from Datastore fill in IfTunnel
            }
        }
        return dpnTepInfo;
    }

    private String getDpnId(BigInteger src, BigInteger dst) {
        return src + ":" + dst;
    }

    private String getDpnId(String src, String dst) {
        return src + ":" + dst;
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

    public Interface getInterfaceFromConfigDS(String tunnelName) {
        TunnelEndPointInfo endPointInfo = directTunnelUtils.getTunnelEndPointInfoFromCache(tunnelName);
        BigInteger srcDpnId = new BigInteger(endPointInfo.getSrcEndPointInfo());
        BigInteger dstDpnId = new BigInteger(endPointInfo.getDstEndPointInfo());
        Interface iface = null ;
        int monitoringInt = 1000;
        DpnTepInterfaceInfo dpnTepInfo;
        if (endPointInfo != null) {
            try {
                Optional<DpnsTeps> srcDpnTeps = get(srcDpnId);
                Optional<DpnsTeps> dstDpnTeps = get(dstDpnId);
            } catch (ReadFailedException e) {
                LOG.info("error while fetching DpnTeps for DpnId {}, {}", endPointInfo.getSrcEndPointInfo(),
                        endPointInfo.getDstEndPointInfo());
            }
            dpnTepInfo = getDpnTepInterfaceInfo(new BigInteger(endPointInfo.getSrcEndPointInfo()),
                    new BigInteger(endPointInfo.getDstEndPointInfo()));
            if (dpnTepInfo != null) {
                List<DPNTEPsInfo> srcDpnTEPsInfo = dpnTepsInfoCache
                        .getDPNTepListFromDPNId(new ArrayList<>(Arrays.asList(srcDpnId)));
                List<DPNTEPsInfo> dstDpnTEPsInfo = dpnTepsInfoCache
                        .getDPNTepListFromDPNId(new ArrayList<>(Arrays.asList(dstDpnId)));
                iface = ItmUtils.buildTunnelInterface(srcDpnId, tunnelName,
                        String.format("%s %s", ItmUtils.convertTunnelTypetoString(dpnTepInfo.getTunnelType()),
                                "Trunk Interface"), true, dpnTepInfo.getTunnelType(),
                        srcDpnTEPsInfo.get(0).getTunnelEndPoints().get(0).getIpAddress(),
                        dstDpnTEPsInfo.get(0).getTunnelEndPoints().get(0).getIpAddress(),
                        srcDpnTEPsInfo.get(0).getTunnelEndPoints().get(0).getGwIpAddress(),
                        srcDpnTEPsInfo.get(0).getTunnelEndPoints().get(0).getVLANID(), true,
                        dpnTepInfo.isMonitoringEnabled(), TunnelMonitoringTypeBfd.class,
                        monitoringInt, true, null);
            } else {
                //SF419 TO DO
                // Read if from Datastore
            }
        }
        return iface;
    }
}
