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

import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.listeners.AbstractClusteredSyncDataTreeChangeListener;
import org.opendaylight.genius.itm.cache.UnprocessedNodeConnectorCache;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.genius.itm.utils.NodeConnectorInfo;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnTepsState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.DpnsTeps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.dpns.teps.RemoteDpns;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DpnTepStateListener extends AbstractClusteredSyncDataTreeChangeListener<DpnsTeps> {
    private static final Logger LOG = LoggerFactory.getLogger(DpnTepStateListener.class);

    private final DataBroker dataBroker;
    private final UnprocessedNodeConnectorCache unprocessedNCCache;
    private final JobCoordinator coordinator;
    private final DirectTunnelUtils directTunnelUtils;
    private final EntityOwnershipUtils entityOwnershipUtils;


    @Inject
    public DpnTepStateListener(DataBroker dataBroker, UnprocessedNodeConnectorCache unprocessedNCCache,
                               JobCoordinator coordinator, DirectTunnelUtils directTunnelUtils,
                               EntityOwnershipUtils entityOwnershipUtils) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(DpnTepsState.class).child(DpnsTeps.class));
        this.dataBroker = dataBroker;
        this.unprocessedNCCache = unprocessedNCCache;
        this.coordinator = coordinator;
        this.directTunnelUtils = directTunnelUtils;
        this.entityOwnershipUtils = entityOwnershipUtils;
    }

    @Override
    public void add(InstanceIdentifier<DpnsTeps> instanceIdentifier, DpnsTeps dpnsTeps) {
        runOnlyInOwnerNode("DpnsTeps Added", () -> {
            for (RemoteDpns remoteDpns : dpnsTeps.getRemoteDpns()) {
                //Process the unprocessed NodeConnector for the Tunnel, if present in the UnprocessedNodeConnectorCache
                // This may run in all node as its ClusteredDTCN but cache will be populated in only the Entity owner
                synchronized (remoteDpns.getTunnelName()) {
                    NodeConnectorInfo nodeConnectorInfo =
                            unprocessedNCCache.get(remoteDpns.getTunnelName());
                    if (nodeConnectorInfo != null) {
                        LOG.debug("Processing the Unprocessed NodeConnector for Tunnel {}", remoteDpns.getTunnelName());

                        // Queue the InterfaceStateAddWorker in DJC
                        String portName = nodeConnectorInfo.getNodeConnector().getName();
                        InterfaceStateAddWorkerForUnprocessedNC ifStateAddWorker =
                                new InterfaceStateAddWorkerForUnprocessedNC(nodeConnectorInfo.getNodeConnectorId(),
                                        nodeConnectorInfo.getNodeConnector(), portName);
                        coordinator.enqueueJob(portName, ifStateAddWorker, ITMConstants.JOB_MAX_RETRIES);
                        // Remove the NodeConnector Entry from Unprocessed Map
                        unprocessedNCCache.remove(remoteDpns.getTunnelName());
                    }
                }
            }
        });
    }

    private void runOnlyInOwnerNode(String jobDesc, Runnable job) {
        entityOwnershipUtils.runOnlyInOwnerNode(ITMConstants.ITM_CONFIG_ENTITY, ITMConstants.ITM_CONFIG_ENTITY,
                coordinator, jobDesc, job);
    }

    private class InterfaceStateAddWorkerForUnprocessedNC implements Callable<List<ListenableFuture<Void>>> {
        final InstanceIdentifier<FlowCapableNodeConnector> key;
        final FlowCapableNodeConnector fcNodeConnectorNew;
        final String interfaceName;

        InterfaceStateAddWorkerForUnprocessedNC(InstanceIdentifier<FlowCapableNodeConnector> key,
                                                       FlowCapableNodeConnector fcNodeConnectorNew,
                                                       String portName) {
            this.key = key;
            this.fcNodeConnectorNew = fcNodeConnectorNew;
            this.interfaceName = portName;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            // If another renderer(for eg : CSS) needs to be supported, check can be performed here
            // to call the respective helpers.
            List<ListenableFuture<Void>> futures = directTunnelUtils.addState(key,
                    interfaceName, fcNodeConnectorNew);
            return futures;
        }

        @Override
        public String toString() {
            return "InterfaceStateAddWorker{"
                    + "fcNodeConnectorIdentifier=" + key
                    + ", fcNodeConnectorNew=" + fcNodeConnectorNew
                    + ", interfaceName='" + interfaceName + '\''
                    +
                    '}';
        }
    }
}
