/*
 * Copyright (c) 2017, 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.listeners;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.aries.blueprint.annotation.service.Reference;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.factory.FlowBasedServicesStateAddable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.factory.FlowBasedServicesStateRendererFactoryResolver;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.serviceutils.tools.listener.AbstractSyncDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class FlowBasedServicesNodeStateListener extends AbstractSyncDataTreeChangeListener<Node> {

    private static final Logger LOG = LoggerFactory.getLogger(FlowBasedServicesNodeStateListener.class);

    private final JobCoordinator jobCoordinator;
    private final FlowBasedServicesStateRendererFactoryResolver flowBasedServicesStateRendererFactoryResolver;

    @Inject
    public FlowBasedServicesNodeStateListener(@Reference final DataBroker dataBroker,
                                              @Reference final JobCoordinator jobCoordinator,
                                              final FlowBasedServicesStateRendererFactoryResolver
                                                      flowBasedServicesStateRendererFactoryResolver) {
        super(dataBroker, LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(Nodes.class).child(Node.class));
        this.jobCoordinator = jobCoordinator;
        this.flowBasedServicesStateRendererFactoryResolver = flowBasedServicesStateRendererFactoryResolver;
    }

    @Override
    public void remove(@NonNull InstanceIdentifier<Node> instanceIdentifier, @NonNull Node node) {
        // Do nothing
    }

    @Override
    public void update(@NonNull InstanceIdentifier<Node> instanceIdentifier, @NonNull Node originalNode,
                       @NonNull final Node updatedNode) {
        // Nothing to do
    }

    @Override
    public void add(@NonNull InstanceIdentifier<Node> instanceIdentifier, @NonNull Node node) {
        final Uint64 dpId = getDpnID(node);
        if (dpId == null) {
            return;
        }
        bindServicesOnTunnelType(dpId);
    }

    private void bindServicesOnTunnelType(final Uint64 dpId) {
        LOG.debug("Received node add event for {}", dpId);
        for (final Class<?extends ServiceModeBase> serviceMode : FlowBasedServicesUtils.SERVICE_MODE_MAP.values()) {
            for (final String interfaceName : FlowBasedServicesUtils.INTERFACE_TYPE_BASED_SERVICE_BINDING_KEYWORDS) {
                final FlowBasedServicesStateAddable flowBasedServicesStateAddable =
                    flowBasedServicesStateRendererFactoryResolver
                        .getFlowBasedServicesStateRendererFactory(serviceMode).getFlowBasedServicesStateAddRenderer();
                jobCoordinator.enqueueJob(interfaceName,
                                          new RendererStateInterfaceBindWorker(flowBasedServicesStateAddable, dpId,
                                                                               interfaceName));
            }
        }
    }

    private static class RendererStateInterfaceBindWorker implements Callable<List<ListenableFuture<Void>>> {
        private final String iface;
        final Uint64 dpnId;
        final FlowBasedServicesStateAddable flowBasedServicesStateAddable;

        RendererStateInterfaceBindWorker(final FlowBasedServicesStateAddable flowBasedServicesStateAddable,
                                         final Uint64 dpnId,
                                         final String iface) {
            this.flowBasedServicesStateAddable = flowBasedServicesStateAddable;
            this.dpnId = dpnId;
            this.iface = iface;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            final List<ListenableFuture<Void>> futures = new ArrayList<>();
            flowBasedServicesStateAddable.bindServicesOnInterfaceType(futures, dpnId, iface);
            return futures;
        }
    }

    private static Uint64 getDpnID(final Node id) {
        final String[] node =  id.getId().getValue().split(":");
        if (node.length < 2) {
            LOG.warn("Unexpected nodeId {}", id.getId().getValue());
            return null;
        }
        return Uint64.valueOf(node[1]);
    }
}
