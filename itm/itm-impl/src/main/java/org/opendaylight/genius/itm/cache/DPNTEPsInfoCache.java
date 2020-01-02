/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.genius.itm.itmdirecttunnels.workers.TunnelStateAddWorker;
import org.opendaylight.genius.itm.itmdirecttunnels.workers.TunnelStateAddWorkerForNodeConnector;
import org.opendaylight.genius.itm.utils.TunnelEndPointInfo;
import org.opendaylight.genius.itm.utils.TunnelStateInfo;
import org.opendaylight.genius.itm.utils.TunnelStateInfoBuilder;
import org.opendaylight.genius.mdsalutil.cache.InstanceIdDataObjectCache;
import org.opendaylight.infrautils.caches.CacheProvider;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.infrautils.utils.concurrent.NamedSimpleReentrantLock.Acquired;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Caches DPNTEPsInfo objects.
 *
 * @author Thomas Pantelis
 */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
@Singleton
public class DPNTEPsInfoCache extends InstanceIdDataObjectCache<DPNTEPsInfo> {

    private static final Logger LOG = LoggerFactory.getLogger(DPNTEPsInfoCache.class);
    private static final Logger EVENT_LOGGER = LoggerFactory.getLogger("GeniusEventLogger");

    private final DirectTunnelUtils directTunnelUtils;
    private final JobCoordinator coordinator;
    private final UnprocessedNodeConnectorEndPointCache unprocessedNodeConnectorEndPointCache;
    private final ManagedNewTransactionRunner txRunner;

    @Inject
    public DPNTEPsInfoCache(final DataBroker dataBroker, final CacheProvider cacheProvider,
                            final DirectTunnelUtils directTunnelUtils, final JobCoordinator coordinator,
                            final UnprocessedNodeConnectorEndPointCache unprocessedNodeConnectorEndPointCache) {
        super(DPNTEPsInfo.class, dataBroker, LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.builder(DpnEndpoints.class).child(DPNTEPsInfo.class).build(), cacheProvider);
        this.directTunnelUtils = directTunnelUtils;
        this.coordinator = coordinator;
        this.unprocessedNodeConnectorEndPointCache = unprocessedNodeConnectorEndPointCache;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
    }

    @Override
    protected void added(InstanceIdentifier<DPNTEPsInfo> path, DPNTEPsInfo dpnTepsInfo) {
        LOG.info("DPNTepsInfo Add Received for {}", dpnTepsInfo.getDPNID());
        String dpnId = dpnTepsInfo.getDPNID().toString();

        Collection<TunnelStateInfo> tunnelStateInfoList = null;
        try (Acquired lock = directTunnelUtils.lockTunnel(dpnId)) {
            if (tunnelStateInfoList != null) {
                tunnelStateInfoList = unprocessedNodeConnectorEndPointCache.remove(dpnId);
            } else {
                LOG.debug("TunnelStateInfo List for DPN Id {} is null", dpnId);
            }
        }

        if (tunnelStateInfoList != null) {
            for (TunnelStateInfo tsInfo : tunnelStateInfoList) {
                String interfaceName = tsInfo.getDpnTepInterfaceInfo().getTunnelName();
                DPNTEPsInfo srcDpnTepsInfo = null;
                DPNTEPsInfo dstDpnTepsInfo = null;
                LOG.debug("Processing the Unprocessed NodeConnector EndPoint Cache for DPN {}", dpnTepsInfo.getDPNID());
                TunnelEndPointInfo tunnelEndPointInfo = tsInfo.getTunnelEndPointInfo();
                if (dpnId.equals(tunnelEndPointInfo.getSrcEndPointInfo())) {
                    srcDpnTepsInfo = dpnTepsInfo;
                    dstDpnTepsInfo = tsInfo.getDstDpnTepsInfo();
                    if (dstDpnTepsInfo == null) {
                        // Check if the destination End Point has come
                        try (Acquired lock = directTunnelUtils.lockTunnel(tunnelEndPointInfo.getDstEndPointInfo())) {
                            Optional<DPNTEPsInfo> dstInfoOpt = getDPNTepFromDPNId(
                                    Uint64.valueOf(tunnelEndPointInfo.getDstEndPointInfo()));
                            if (dstInfoOpt.isPresent()) {
                                dstDpnTepsInfo = dstInfoOpt.get();
                            } else {
                                TunnelStateInfo tunnelStateInfoNew = new TunnelStateInfoBuilder()
                                    .setNodeConnectorInfo(tsInfo.getNodeConnectorInfo())
                                    .setDpnTepInterfaceInfo(tsInfo.getDpnTepInterfaceInfo())
                                    .setTunnelEndPointInfo(tsInfo.getTunnelEndPointInfo())
                                    .setSrcDpnTepsInfo(srcDpnTepsInfo).build();
                                LOG.trace("Destination DPNTepsInfo is null for tunnel {}. Hence Parking with key {}",
                                        interfaceName, tunnelEndPointInfo.getDstEndPointInfo());
                                unprocessedNodeConnectorEndPointCache.add(tunnelEndPointInfo
                                        .getDstEndPointInfo(), tunnelStateInfoNew);
                            }
                        }
                    }
                } else if (dpnId.equals(tunnelEndPointInfo.getDstEndPointInfo())) {
                    dstDpnTepsInfo = dpnTepsInfo;
                    srcDpnTepsInfo = tsInfo.getSrcDpnTepsInfo();
                    // Check if the destination End Point has come
                    if (srcDpnTepsInfo == null) {
                        try (Acquired lock = directTunnelUtils.lockTunnel(tunnelEndPointInfo.getSrcEndPointInfo())) {
                            Optional<DPNTEPsInfo> srcInfoOpt = getDPNTepFromDPNId(
                                    Uint64.valueOf(tunnelEndPointInfo.getSrcEndPointInfo()));
                            if (srcInfoOpt.isPresent()) {
                                srcDpnTepsInfo = srcInfoOpt.get();
                            } else {
                                TunnelStateInfo tunnelStateInfoNew = new TunnelStateInfoBuilder().setNodeConnectorInfo(
                                    tsInfo.getNodeConnectorInfo())
                                    .setDpnTepInterfaceInfo(tsInfo.getDpnTepInterfaceInfo())
                                    .setTunnelEndPointInfo(tsInfo.getTunnelEndPointInfo())
                                    .setDstDpnTepsInfo(dstDpnTepsInfo).build();
                                LOG.trace("Source DPNTepsInfo is null for tunnel {}. Hence Parking with key {}",
                                        interfaceName,
                                        tsInfo.getTunnelEndPointInfo().getSrcEndPointInfo());
                                unprocessedNodeConnectorEndPointCache.add(tunnelEndPointInfo.getSrcEndPointInfo(),
                                        tunnelStateInfoNew);
                            }
                        }
                    }
                }

                if (srcDpnTepsInfo != null && dstDpnTepsInfo != null && directTunnelUtils.isEntityOwner()) {
                    TunnelStateInfo tunnelStateInfoNew = new TunnelStateInfoBuilder()
                        .setNodeConnectorInfo(tsInfo.getNodeConnectorInfo())
                        .setDpnTepInterfaceInfo(tsInfo.getDpnTepInterfaceInfo())
                        .setTunnelEndPointInfo(tsInfo.getTunnelEndPointInfo())
                        .setSrcDpnTepsInfo(srcDpnTepsInfo).setDstDpnTepsInfo(dstDpnTepsInfo).build();
                    LOG.debug("Queueing TunnelStateAddWorker to DJC for tunnel {}", interfaceName);
                    EVENT_LOGGER.debug("ITM-DpnTepsInfoCache,ADD {}", interfaceName);
                    coordinator.enqueueJob(interfaceName,
                        new TunnelStateAddWorkerForNodeConnector(new TunnelStateAddWorker(directTunnelUtils, txRunner),
                            tunnelStateInfoNew), ITMConstants.JOB_MAX_RETRIES);
                }
            }
        }
    }

    public List<DPNTEPsInfo> getDPNTepListFromDPNId(List<Uint64> dpnIds) {
        Collection<DPNTEPsInfo> meshedDpnList = this.getAllPresent() ;
        List<DPNTEPsInfo> cfgDpnList = new ArrayList<>();
        for (Uint64 dpnId : dpnIds) {
            for (DPNTEPsInfo teps : meshedDpnList) {
                if (dpnId.equals(teps.getDPNID())) {
                    cfgDpnList.add(teps);
                }
            }
        }
        return cfgDpnList;
    }

    public Optional<DPNTEPsInfo> getDPNTepFromDPNId(Uint64 dpnId) {
        Collection<DPNTEPsInfo> meshedDpnList = this.getAllPresent() ;
        return meshedDpnList.stream().filter(info -> dpnId.equals(info.getDPNID())).findFirst();
    }
}
