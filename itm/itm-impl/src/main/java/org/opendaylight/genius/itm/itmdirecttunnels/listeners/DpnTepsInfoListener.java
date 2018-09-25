/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.itmdirecttunnels.listeners;

import java.util.Collection;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.cache.DPNTEPsInfoCache;
import org.opendaylight.genius.itm.cache.DpnTepStateCache;
import org.opendaylight.genius.itm.cache.UnprocessedNodeConnectorCache;
import org.opendaylight.genius.itm.cache.UnprocessedNodeConnectorEndPointCache;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.genius.itm.utils.NodeConnectorInfo;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DpnTepsInfoListener extends AbstractTunnelListenerBase<DPNTEPsInfo> {

    private static final Logger LOG = LoggerFactory.getLogger(DpnTepsInfoListener.class);

    private final JobCoordinator coordinator;
    private final IInterfaceManager interfaceManager;

    public DpnTepsInfoListener(final DataBroker dataBroker,
                               final JobCoordinator coordinator,
                               final EntityOwnershipUtils entityOwnershipUtils,
                               final DpnTepStateCache dpnTepStateCache,
                               final DPNTEPsInfoCache dpntePsInfoCache,
                               final UnprocessedNodeConnectorCache unprocessedNodeConnectorCache,
                               final UnprocessedNodeConnectorEndPointCache unprocessedNodeConnectorEndPointCache,
                               final IInterfaceManager interfaceManager,
                               final DirectTunnelUtils directTunnelUtils) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(DpnEndpoints.class).child(DPNTEPsInfo.class),
                dpnTepStateCache, dpntePsInfoCache, unprocessedNodeConnectorCache,
                unprocessedNodeConnectorEndPointCache, entityOwnershipUtils,
                directTunnelUtils);
        this.coordinator = coordinator;
        this.interfaceManager = interfaceManager;
        super.register();
    }

    @Override
    public void add(@Nonnull InstanceIdentifier<DPNTEPsInfo> instanceIdentifier, @Nonnull DPNTEPsInfo dpnTepsInfo) {
        LOG.trace("DPN Teps Info Add {}", dpnTepsInfo);
        if (interfaceManager.isItmDirectTunnelsEnabled()) {
            //Process the unprocessed NodeConnector for the Tunnel, if present in the UnprocessedNodeConnectorEndPtCache
            // This may run in all node as its ClusteredDTCN but cache will be populated in only the Entity owner
            Collection<NodeConnectorInfo> nodeConnectorInfoList = unprocessedNodeConnectorEndPointCache.get(
                    dpnTepsInfo.getDPNID().toString());
            if (nodeConnectorInfoList != null) {
                for (NodeConnectorInfo ncInfo : nodeConnectorInfoList) {
                    LOG.info("Processing the Unprocessed NodeConnector for Tunnel {}", ncInfo
                            .getNodeConnector().getName());
                    // Queue the InterfaceStateAddWorker in DJC
                    String portName = ncInfo.getNodeConnector().getName();
                    InterfaceStateAddWorkerForUnprocessedNC ifStateAddWorker =
                            new InterfaceStateAddWorkerForUnprocessedNC(ncInfo.getNodeConnectorId(),
                                    ncInfo.getNodeConnector(), portName);
                    coordinator.enqueueJob(portName, ifStateAddWorker, ITMConstants.JOB_MAX_RETRIES);
                    // Remove the NodeConnector Entry from UnprocessedNodeConnectorEndPt Map
                    unprocessedNodeConnectorEndPointCache.remove(dpnTepsInfo.getDPNID().toString(), ncInfo);
                }
            }
        }
    }
}
