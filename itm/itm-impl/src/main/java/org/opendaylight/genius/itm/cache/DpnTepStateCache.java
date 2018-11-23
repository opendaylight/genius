/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cache;

import static org.opendaylight.genius.itm.impl.ItmUtils.nullToEmpty;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.genius.itm.itmdirecttunnels.workers.TunnelStateAddWorker;
import org.opendaylight.genius.itm.itmdirecttunnels.workers.TunnelStateAddWorkerForNodeConnector;
import org.opendaylight.genius.itm.utils.DpnTepInterfaceInfo;
import org.opendaylight.genius.itm.utils.DpnTepInterfaceInfoBuilder;
import org.opendaylight.genius.itm.utils.TunnelEndPointInfo;
import org.opendaylight.genius.itm.utils.TunnelEndPointInfoBuilder;
import org.opendaylight.genius.itm.utils.TunnelStateInfo;
import org.opendaylight.genius.itm.utils.TunnelStateInfoBuilder;
import org.opendaylight.genius.mdsalutil.cache.DataObjectCache;
import org.opendaylight.infrautils.caches.CacheProvider;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
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
    private final JobCoordinator coordinator;
    private final DirectTunnelUtils directTunnelUtils;
    private final DPNTEPsInfoCache dpnTepsInfoCache;
    private final UnprocessedNodeConnectorCache unprocessedNCCache;
    private final UnprocessedNodeConnectorEndPointCache unprocessedNodeConnectorEndPointCache;
    private final ManagedNewTransactionRunner txRunner;
    private final ConcurrentMap<String, DpnTepInterfaceInfo> dpnTepInterfaceMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, TunnelEndPointInfo> tunnelEndpointMap = new ConcurrentHashMap<>();

    @Inject
    public DpnTepStateCache(DataBroker dataBroker, JobCoordinator coordinator,
                            CacheProvider cacheProvider, DirectTunnelUtils directTunnelUtils,
                            DPNTEPsInfoCache dpnTepsInfoCache,
                            UnprocessedNodeConnectorCache unprocessedNCCache,
                            UnprocessedNodeConnectorEndPointCache unprocessedNodeConnectorEndPointCache) {
        super(DpnsTeps.class, dataBroker, LogicalDatastoreType.CONFIGURATION,
            InstanceIdentifier.builder(DpnTepsState.class).child(DpnsTeps.class).build(), cacheProvider,
            (iid, dpnsTeps) -> dpnsTeps.getSourceDpnId(),
            sourceDpnId -> InstanceIdentifier.builder(DpnTepsState.class)
                    .child(DpnsTeps.class, new DpnsTepsKey(sourceDpnId)).build());
        this.dataBroker = dataBroker;
        this.coordinator = coordinator;
        this.directTunnelUtils = directTunnelUtils;
        this.dpnTepsInfoCache = dpnTepsInfoCache;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.unprocessedNCCache = unprocessedNCCache;
        this.unprocessedNodeConnectorEndPointCache = unprocessedNodeConnectorEndPointCache;
    }

    @Override
    protected void added(InstanceIdentifier<DpnsTeps> path, DpnsTeps dpnsTeps) {
        for (RemoteDpns remoteDpns : nullToEmpty(dpnsTeps.getRemoteDpns())) {
            final String dpn = getDpnId(dpnsTeps.getSourceDpnId(), remoteDpns.getDestinationDpnId());
            DpnTepInterfaceInfo value = new DpnTepInterfaceInfoBuilder()
                .setTunnelName(remoteDpns.getTunnelName())
                .setIsMonitoringEnabled(remoteDpns.isMonitoringEnabled())
                .setIsInternal(remoteDpns.isInternal())
                .setTunnelType(dpnsTeps.getTunnelType()).build();
            dpnTepInterfaceMap.put(dpn, value);
            addTunnelEndPointInfoToCache(remoteDpns.getTunnelName(),
                    dpnsTeps.getSourceDpnId().toString(), remoteDpns.getDestinationDpnId().toString());

            //Process the unprocessed NodeConnector for the Tunnel, if present in the UnprocessedNodeConnectorCache

            TunnelStateInfo tunnelStateInfoNew = null;

            directTunnelUtils.getTunnelLocks().lock(remoteDpns.getTunnelName());
            TunnelStateInfo tunnelStateInfo = unprocessedNCCache.remove(remoteDpns.getTunnelName());
            directTunnelUtils.getTunnelLocks().unlock(remoteDpns.getTunnelName());

            if (tunnelStateInfo != null) {
                LOG.debug("Processing the Unprocessed NodeConnector for Tunnel {}", remoteDpns.getTunnelName());

                TunnelEndPointInfo tunnelEndPtInfo = getTunnelEndPointInfo(dpnsTeps.getSourceDpnId().toString(),
                        remoteDpns.getDestinationDpnId().toString());
                TunnelStateInfoBuilder builder = new TunnelStateInfoBuilder()
                    .setNodeConnectorInfo(tunnelStateInfo.getNodeConnectorInfo()).setDpnTepInterfaceInfo(value)
                    .setTunnelEndPointInfo(tunnelEndPtInfo);

                dpnTepsInfoCache.getDPNTepFromDPNId(dpnsTeps.getSourceDpnId()).ifPresent(builder::setSrcDpnTepsInfo);
                dpnTepsInfoCache.getDPNTepFromDPNId(remoteDpns.getDestinationDpnId())
                    .ifPresent(builder::setDstDpnTepsInfo);

                tunnelStateInfoNew = builder.build();
                if (tunnelStateInfoNew.getSrcDpnTepsInfo() == null) {
                    String srcDpnId = tunnelStateInfoNew.getTunnelEndPointInfo().getSrcEndPointInfo();
                    directTunnelUtils.getTunnelLocks().lock(srcDpnId);
                    LOG.debug("Source DPNTepsInfo is null for tunnel {}. Hence Parking with key {}",
                        remoteDpns.getTunnelName(), srcDpnId);
                    unprocessedNodeConnectorEndPointCache.add(srcDpnId, tunnelStateInfoNew);
                    directTunnelUtils.getTunnelLocks().unlock(srcDpnId);
                }

                if (tunnelStateInfoNew.getDstDpnTepsInfo() == null) {
                    String dstDpnId = tunnelStateInfo.getTunnelEndPointInfo().getDstEndPointInfo();
                    directTunnelUtils.getTunnelLocks().lock(dstDpnId);
                    LOG.debug("Destination DPNTepsInfo is null for tunnel {}. Hence Parking with key {}",
                        remoteDpns.getTunnelName(), dstDpnId);
                    unprocessedNodeConnectorEndPointCache.add(dstDpnId, tunnelStateInfoNew);
                    directTunnelUtils.getTunnelLocks().unlock(dstDpnId);
                }
            }

            if (tunnelStateInfoNew != null && tunnelStateInfoNew.getSrcDpnTepsInfo() != null
                && tunnelStateInfoNew.getDstDpnTepsInfo() != null && directTunnelUtils.isEntityOwner()) {
                TunnelStateAddWorkerForNodeConnector ifStateAddWorker =
                    new TunnelStateAddWorkerForNodeConnector(new TunnelStateAddWorker(directTunnelUtils, txRunner),
                        tunnelStateInfoNew);
                coordinator.enqueueJob(remoteDpns.getTunnelName(), ifStateAddWorker, ITMConstants.JOB_MAX_RETRIES);
            }
        }
    }

    @Override
    protected void removed(InstanceIdentifier<DpnsTeps> path, DpnsTeps dpnsTeps) {
        for (RemoteDpns remoteDpns : nullToEmpty(dpnsTeps.getRemoteDpns())) {
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
                com.google.common.base.Optional<DpnsTeps> dpnsTeps = super.get(srcDpnId);
                if (dpnsTeps.isPresent()) {
                    DpnsTeps teps = dpnsTeps.get();
                    nullToEmpty(teps.getRemoteDpns()).forEach(remoteDpns -> {
                        DpnTepInterfaceInfo value = new DpnTepInterfaceInfoBuilder()
                                .setTunnelName(remoteDpns.getTunnelName())
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
            if (!Objects.equals(dpnTep.getSourceDpnId(), srcDpnId)) {
                for (RemoteDpns remoteDpns : nullToEmpty(dpnTep.getRemoteDpns())) {
                    if (Objects.equals(remoteDpns.getDestinationDpnId(), srcDpnId)) {
                        // Remote the SrcDpnId from the remote List. Remove it from COnfig DS. 4
                        // This will be reflected in cache by the ClusteredDTCN. Not removing it here !
                        //Caution :- Batching Delete !!
                        InstanceIdentifier<RemoteDpns> remoteDpnII =
                                buildRemoteDpnsInstanceIdentifier(dpnTep.getSourceDpnId(),
                                        remoteDpns.getDestinationDpnId());
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
        tunnelEndpointMap.put(tunnelName, getTunnelEndPointInfo(srcEndPtInfo,dstEndPtInfo));
    }

    private TunnelEndPointInfo getTunnelEndPointInfo(String srcEndPtInfo, String dstEndPtInfo) {
        return
            new TunnelEndPointInfoBuilder().setSrcEndPointInfo(srcEndPtInfo).setDstEndPointInfo(dstEndPtInfo).build();
    }

    public TunnelEndPointInfo getTunnelEndPointInfoFromCache(String tunnelName) {
        return tunnelEndpointMap.get(tunnelName);
    }
}
