/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
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
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncDataTreeChangeListenerBase;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.factory.FlowBasedServicesStateAddable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.factory.FlowBasedServicesStateRemovable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.factory.FlowBasedServicesStateRendererFactory;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class FlowBasedServicesNodeStateListener
        extends AsyncDataTreeChangeListenerBase<Node, FlowBasedServicesNodeStateListener> {

    private static final Logger LOG = LoggerFactory.getLogger(FlowBasedServicesNodeStateListener.class);

    private final IMdsalApiManager mdsalManager;
    private final DataBroker broker;
    private ListenerRegistration<DataChangeListener> listenerRegistration;

    @Inject
    public FlowBasedServicesNodeStateListener(final DataBroker dataBroker, final IMdsalApiManager mdsalManager) {
        this.broker = dataBroker;
        this.mdsalManager = mdsalManager;
        registerListener(LogicalDatastoreType.OPERATIONAL, dataBroker);
    }

    @Override
    protected FlowBasedServicesNodeStateListener getDataTreeChangeListener() {
        return FlowBasedServicesNodeStateListener.this;
    }

    public InstanceIdentifier<Node> getWildCardPath() {
        return InstanceIdentifier.create(Nodes.class).child(Node.class);
    }


    @Override
    protected void remove(InstanceIdentifier<Node> identifier, Node del) {
        BigInteger dpId = getDpnID(del);
        if (dpId == null) {
            return;
        }
        unbindServicesOnTunnelType(dpId);
    }

    @Override
    protected void update(InstanceIdentifier<Node> identifier, Node original, Node update) {

    }

    @Override
    protected void add(InstanceIdentifier<Node> identifier, Node add) {
        BigInteger dpId = getDpnID(add);
        if (dpId == null) {
            return;
        }
        bindServicesOnTunnelType(dpId);
    }

    private void bindServicesOnTunnelType(BigInteger dpId) {
        LOG.debug("Received node add event for {}", dpId);
        for (Class<?extends ServiceModeBase> serviceMode : FlowBasedServicesUtils.SERVICE_MODE_MAP.values()) {
            for (String interfaceName : FlowBasedServicesUtils.INTERFACE_TYPE_BASED_SERVICE_BINDING_KEYWORDS) {
                FlowBasedServicesStateAddable flowBasedServicesStateAddable = FlowBasedServicesStateRendererFactory
                        .getFlowBasedServicesStateRendererFactory(serviceMode)
                        .getFlowBasedServicesStateAddRenderer();
                DataStoreJobCoordinator.getInstance().enqueueJob(interfaceName,
                        new RendererStateInterfaceBindWorker(flowBasedServicesStateAddable, dpId, interfaceName));
            }
        }
    }

    private void unbindServicesOnTunnelType(BigInteger dpId) {
        LOG.debug("Received node add event for {}", dpId);
        for (Class<?extends ServiceModeBase> serviceMode : FlowBasedServicesUtils.SERVICE_MODE_MAP.values()) {
            for (String interfaceName : FlowBasedServicesUtils.INTERFACE_TYPE_BASED_SERVICE_BINDING_KEYWORDS) {
                FlowBasedServicesStateRemovable flowBasedServicesStateRemovable = FlowBasedServicesStateRendererFactory
                        .getFlowBasedServicesStateRendererFactory(serviceMode)
                        .getFlowBasedServicesStateRemoveRenderer();
                DataStoreJobCoordinator.getInstance().enqueueJob(interfaceName,
                        new RendererStateInterfaceUnbindWorker(flowBasedServicesStateRemovable, dpId, interfaceName));
            }
        }
    }

    private class RendererStateInterfaceBindWorker implements Callable<List<ListenableFuture<Void>>> {
        private final String iface;
        BigInteger dpnId;
        FlowBasedServicesStateAddable flowBasedServicesStateAddable;

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

    private class RendererStateInterfaceUnbindWorker implements Callable<List<ListenableFuture<Void>>> {
        private final String iface;
        BigInteger dpnId;
        FlowBasedServicesStateRemovable flowBasedServicesStateRemovable;

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
