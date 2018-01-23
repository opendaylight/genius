/*
 * Copyright (c) 2017, 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.listeners;

import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.listeners.AbstractSyncDataTreeChangeListener;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.factory.FlowBasedServicesStateAddable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.factory.FlowBasedServicesStateRemovable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.factory.FlowBasedServicesStateRendererFactoryResolver;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class FlowBasedServicesNodeStateListener extends AbstractSyncDataTreeChangeListener<Node> {

    private static final Logger LOG = LoggerFactory.getLogger(FlowBasedServicesNodeStateListener.class);

    private final JobCoordinator jobCoordinator;
    private final FlowBasedServicesStateRendererFactoryResolver flowBasedServicesStateRendererFactoryResolver;

    @Inject
    public FlowBasedServicesNodeStateListener(DataBroker dataBroker, JobCoordinator jobCoordinator,
            FlowBasedServicesStateRendererFactoryResolver flowBasedServicesStateRendererFactoryResolver) {
        super(dataBroker, LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(Nodes.class).child(Node.class));
        this.jobCoordinator = jobCoordinator;
        this.flowBasedServicesStateRendererFactoryResolver = flowBasedServicesStateRendererFactoryResolver;
    }

    @Override
    public void remove(@Nonnull Node node) {
        BigInteger dpId = getDpnID(node);
        if (dpId == null) {
            return;
        }
        unbindServicesOnTunnelType(dpId);
    }

    @Override
    public void update(@Nonnull Node originalNode, @Nonnull Node updatedNode) {
        // Nothing to do
    }

    @Override
    public void add(@Nonnull Node node) {
        BigInteger dpId = getDpnID(node);
        if (dpId == null) {
            return;
        }
        bindServicesOnTunnelType(dpId);
    }

    private void bindServicesOnTunnelType(BigInteger dpId) {
        LOG.debug("Received node add event for {}", dpId);
        for (Class<?extends ServiceModeBase> serviceMode : FlowBasedServicesUtils.SERVICE_MODE_MAP.values()) {
            for (String interfaceName : FlowBasedServicesUtils.INTERFACE_TYPE_BASED_SERVICE_BINDING_KEYWORDS) {
                FlowBasedServicesStateAddable flowBasedServicesStateAddable =
                    flowBasedServicesStateRendererFactoryResolver
                        .getFlowBasedServicesStateRendererFactory(serviceMode).getFlowBasedServicesStateAddRenderer();
                jobCoordinator.enqueueJob(interfaceName,
                                          new RendererStateInterfaceBindWorker(flowBasedServicesStateAddable, dpId,
                                                                               interfaceName));
            }
        }
    }

    private void unbindServicesOnTunnelType(BigInteger dpId) {
        LOG.debug("Received node add event for {}", dpId);
        for (Class<?extends ServiceModeBase> serviceMode : FlowBasedServicesUtils.SERVICE_MODE_MAP.values()) {
            for (String interfaceName : FlowBasedServicesUtils.INTERFACE_TYPE_BASED_SERVICE_BINDING_KEYWORDS) {
                FlowBasedServicesStateRemovable flowBasedServicesStateRemovable =
                    flowBasedServicesStateRendererFactoryResolver.getFlowBasedServicesStateRendererFactory(serviceMode)
                        .getFlowBasedServicesStateRemoveRenderer();
                jobCoordinator.enqueueJob(interfaceName,
                                          new RendererStateInterfaceUnbindWorker(flowBasedServicesStateRemovable, dpId,
                                                                                 interfaceName));
            }
        }
    }

    private static class RendererStateInterfaceBindWorker implements Callable<List<ListenableFuture<Void>>> {
        private final String iface;
        final BigInteger dpnId;
        final FlowBasedServicesStateAddable flowBasedServicesStateAddable;

        RendererStateInterfaceBindWorker(FlowBasedServicesStateAddable flowBasedServicesStateAddable, BigInteger dpnId,
                                         String iface) {
            this.flowBasedServicesStateAddable = flowBasedServicesStateAddable;
            this.dpnId = dpnId;
            this.iface = iface;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            List<ListenableFuture<Void>> futures = new ArrayList<>();
            flowBasedServicesStateAddable.bindServicesOnInterfaceType(futures, dpnId, iface);
            return futures;
        }
    }

    private static class RendererStateInterfaceUnbindWorker implements Callable<List<ListenableFuture<Void>>> {
        private final String iface;
        final BigInteger dpnId;
        final FlowBasedServicesStateRemovable flowBasedServicesStateRemovable;

        RendererStateInterfaceUnbindWorker(FlowBasedServicesStateRemovable flowBasedServicesStateRemovable,
                                           BigInteger dpnId, String iface) {
            this.flowBasedServicesStateRemovable = flowBasedServicesStateRemovable;
            this.dpnId = dpnId;
            this.iface = iface;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            List<ListenableFuture<Void>> futures = new ArrayList<>();
            flowBasedServicesStateRemovable.unbindServicesOnInterfaceType(futures, dpnId, iface);
            return futures;
        }
    }

    private BigInteger getDpnID(Node id) {
        String[] node =  id.getId().getValue().split(":");
        if (node.length < 2) {
            LOG.warn("Unexpected nodeId {}", id.getId().getValue());
            return null;
        }
        return new BigInteger(node[1]);
    }
}
