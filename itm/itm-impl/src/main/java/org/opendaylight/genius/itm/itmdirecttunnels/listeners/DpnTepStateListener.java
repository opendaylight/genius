/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.itmdirecttunnels.listeners;

import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.cache.DPNTEPsInfoCache;
import org.opendaylight.genius.itm.cache.DpnTepStateCache;
import org.opendaylight.genius.itm.cache.UnprocessedNodeConnectorCache;
import org.opendaylight.genius.itm.cache.UnprocessedNodeConnectorEndPointCache;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.genius.itm.utils.DpnTepInterfaceInfo;
import org.opendaylight.genius.itm.utils.DpnTepInterfaceInfoBuilder;
import org.opendaylight.genius.itm.utils.NodeConnectorInfo;
import org.opendaylight.genius.itm.utils.TunnelEndPointInfo;
import org.opendaylight.genius.itm.utils.TunnelEndPointInfoBuilder;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnTepsState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.DpnsTeps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.dpns.teps.RemoteDpns;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DpnTepStateListener extends AbstractTunnelListenerBase<DpnsTeps> {

    private static final Logger LOG = LoggerFactory.getLogger(DpnTepStateListener.class);

    private final JobCoordinator coordinator;

    public DpnTepStateListener(final DataBroker dataBroker,
                               final JobCoordinator coordinator,
                               final EntityOwnershipUtils entityOwnershipUtils,
                               final DpnTepStateCache dpnTepStateCache,
                               final DPNTEPsInfoCache dpntePsInfoCache,
                               final UnprocessedNodeConnectorCache unprocessedNodeConnectorCache,
                               final UnprocessedNodeConnectorEndPointCache unprocessedNodeConnectorEndPointCache,
                               final DirectTunnelUtils directTunnelUtils) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(DpnTepsState.class).child(DpnsTeps.class),
                dpnTepStateCache, dpntePsInfoCache, unprocessedNodeConnectorCache,
                unprocessedNodeConnectorEndPointCache, entityOwnershipUtils,
                directTunnelUtils);
        this.coordinator = coordinator;
        super.register();
    }

    @Override
    public void add(@Nonnull InstanceIdentifier<DpnsTeps> instanceIdentifier, @Nonnull DpnsTeps dpnsTeps) {
        for (RemoteDpns remoteDpns : dpnsTeps.getRemoteDpns()) {
            //Process the unprocessed NodeConnector for the Tunnel, if present in the UnprocessedNodeConnectorCache
            // This may run in all node as its ClusteredDTCN but cache will be populated in only the Entity owner
            NodeConnectorInfo nodeConnectorInfo = unprocessedNCCache.remove(remoteDpns.getTunnelName());
            if (nodeConnectorInfo != null) {
                LOG.debug("Processing the Unprocessed NodeConnector for Tunnel {}", remoteDpns.getTunnelName());
                // Queue the IntefaceAddWorkerForUnprocessNC in DJC
                String portName = nodeConnectorInfo.getNodeConnector().getName();
                TunnelEndPointInfo tunnelEndPointInfo = new TunnelEndPointInfoBuilder()
                        .setSrcEndPointInfo(dpnsTeps.getSourceDpnId().toString())
                        .setDstEndPointInfo(remoteDpns.getDestinationDpnId().toString()).build();
                DpnTepInterfaceInfo dpnTepConfigInfo = new DpnTepInterfaceInfoBuilder()
                        .setTunnelName(remoteDpns.getTunnelName())
                        .setIsMonitoringEnabled(remoteDpns.isMonitoringEnabled())
                        .setIsInternal(remoteDpns.isInternal())
                        .setTunnelType(dpnsTeps.getTunnelType()).build();
                InterfaceStateAddWorkerForUnprocessedNC ifStateAddWorker =
                        new InterfaceStateAddWorkerForUnprocessedNC(nodeConnectorInfo.getNodeConnectorId(),
                                nodeConnectorInfo.getNodeConnector(), portName, tunnelEndPointInfo,dpnTepConfigInfo);
                coordinator.enqueueJob(portName, ifStateAddWorker, ITMConstants.JOB_MAX_RETRIES);
            }
        }
    }
}
