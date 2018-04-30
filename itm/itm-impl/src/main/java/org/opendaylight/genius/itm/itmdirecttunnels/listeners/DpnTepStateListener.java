/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.itmdirecttunnels.listeners;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.cache.DPNTEPsInfoCache;
import org.opendaylight.genius.itm.cache.DpnTepStateCache;
import org.opendaylight.genius.itm.cache.UnprocessedNodeConnectorCache;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.utils.NodeConnectorInfo;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
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
                               final IdManagerService idManager,
                               final IMdsalApiManager mdsalApiManager,
                               final DpnTepStateCache dpnTepStateCache,
                               final DPNTEPsInfoCache dpntePsInfoCache,
                               final UnprocessedNodeConnectorCache unprocessedNodeConnectorCache) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(DpnTepsState.class).child(DpnsTeps.class), idManager, mdsalApiManager,
                dpnTepStateCache, dpntePsInfoCache, unprocessedNodeConnectorCache, entityOwnershipUtils);
        this.coordinator = coordinator;
        super.register();
    }

    @Override
    public void add(@Nonnull InstanceIdentifier<DpnsTeps> instanceIdentifier, @Nonnull DpnsTeps dpnsTeps) {
        if (!entityOwner()) {
            return;
        }
        for (RemoteDpns remoteDpns : dpnsTeps.getRemoteDpns()) {
            //Process the unprocessed NodeConnector for the Tunnel, if present in the UnprocessedNodeConnectorCache
            // This may run in all node as its ClusteredDTCN but cache will be populated in only the Entity owner
            NodeConnectorInfo nodeConnectorInfo = unprocessedNCCache.remove(remoteDpns.getTunnelName());
            if (nodeConnectorInfo != null) {
                LOG.debug("Processing the Unprocessed NodeConnector for Tunnel {}", remoteDpns.getTunnelName());
                // Queue the IntefaceAddWorkerForUnprocessNC in DJC
                String portName = nodeConnectorInfo.getNodeConnector().getName();
                InterfaceStateAddWorkerForUnprocessedNC ifStateAddWorker =
                        new InterfaceStateAddWorkerForUnprocessedNC(nodeConnectorInfo.getNodeConnectorId(),
                                nodeConnectorInfo.getNodeConnector(), portName);
                coordinator.enqueueJob(portName, ifStateAddWorker, ITMConstants.JOB_MAX_RETRIES);
            }
        }
    }

    private class InterfaceStateAddWorkerForUnprocessedNC implements Callable<List<ListenableFuture<Void>>> {
        private final InstanceIdentifier<FlowCapableNodeConnector> key;
        private final FlowCapableNodeConnector fcNodeConnectorNew;
        private final String interfaceName;

        InterfaceStateAddWorkerForUnprocessedNC(InstanceIdentifier<FlowCapableNodeConnector> key,
                                                FlowCapableNodeConnector fcNodeConnectorNew, String portName) {
            this.key = key;
            this.fcNodeConnectorNew = fcNodeConnectorNew;
            this.interfaceName = portName;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            // If another renderer(for eg : OVS) needs to be supported, check can be performed here
            // to call the respective helpers.
            return addState(key, interfaceName, fcNodeConnectorNew);
        }

        @Override
        public String toString() {
            return "InterfaceStateAddWorkerForUnprocessedNC{"
                    + "fcNodeConnectorIdentifier=" + key
                    + ", fcNodeConnectorNew=" + fcNodeConnectorNew
                    + ", interfaceName='" + interfaceName + '\''
                    + '}';
        }
    }
}
