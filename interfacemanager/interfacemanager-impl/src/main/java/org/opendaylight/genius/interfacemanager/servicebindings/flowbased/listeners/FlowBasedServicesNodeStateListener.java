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
import java.util.List;
import java.util.concurrent.Callable;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowBasedServicesNodeStateListener
        extends AsyncDataTreeChangeListenerBase<Node, FlowBasedServicesNodeStateListener> {

    private static final Logger LOG = LoggerFactory.getLogger(FlowBasedServicesNodeStateListener.class);

    private IMdsalApiManager mdsalManager;
    private ListenerRegistration<DataChangeListener> listenerRegistration;
    private final DataBroker broker;

    public FlowBasedServicesNodeStateListener(final DataBroker db, IMdsalApiManager mdsalManager) {
        broker = db;
        this.mdsalManager = mdsalManager;
        registerListener(LogicalDatastoreType.OPERATIONAL, db);
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
        NodeId nodeId = del.getId();
        String[] node =  nodeId.getValue().split(":");
        if (node.length < 2) {
            LOG.warn("Unexpected nodeId {}", nodeId.getValue());
            return;
        }
        BigInteger dpId = new BigInteger(node[1]);
        unbindServicesOnTunnelType(dpId);
    }

    @Override
    protected void update(InstanceIdentifier<Node> identifier, Node original, Node update) {

    }

    @Override
    protected void add(InstanceIdentifier<Node> identifier, Node add) {
        NodeId nodeId = add.getId();
        String[] node =  nodeId.getValue().split(":");
        if (node.length < 2) {
            LOG.warn("Unexpected nodeId {}", nodeId.getValue());
            return;
        }
        BigInteger dpId = new BigInteger(node[1]);
        bindServicesOnInterfaceType(dpId);
    }

    private void bindServicesOnInterfaceType(BigInteger dpId) {
        LOG.debug("Received node add event for {}", dpId);
        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
        for (Object serviceMode : FlowBasedServicesUtils.SERVICE_MODE_MAP.values()) {
            for (String interfaceName : FlowBasedServicesUtils.INTERFACE_TYPE_BASED_SERVICE_BINDING_KEYWORDS) {
                FlowBasedServicesStateAddable flowBasedServicesStateAddable = FlowBasedServicesStateRendererFactory
                        .getFlowBasedServicesStateRendererFactory((Class<? extends ServiceModeBase>) serviceMode)
                        .getFlowBasedServicesStateAddRenderer();
                RendererStateInterfaceBindWorker stateBindWorker =
                        new RendererStateInterfaceBindWorker(flowBasedServicesStateAddable, dpId, interfaceName,
                                (Class<? extends ServiceModeBase>) serviceMode);
                coordinator.enqueueJob(interfaceName, stateBindWorker);
            }
        }
    }

    private void unbindServicesOnTunnelType(BigInteger dpId) {
        LOG.debug("Received node add event for {}", dpId);
        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
        for (Object serviceMode : FlowBasedServicesUtils.SERVICE_MODE_MAP.values()) {
            for (String interfaceName : FlowBasedServicesUtils.INTERFACE_TYPE_BASED_SERVICE_BINDING_KEYWORDS) {
                FlowBasedServicesStateRemovable flowBasedServicesStateRemovable = FlowBasedServicesStateRendererFactory
                        .getFlowBasedServicesStateRendererFactory((Class<? extends ServiceModeBase>) serviceMode)
                        .getFlowBasedServicesStateRemoveRenderer();
                RendererStateInterfaceUnbindWorker stateUnbindWorker =
                        new RendererStateInterfaceUnbindWorker(flowBasedServicesStateRemovable, dpId, interfaceName,
                                (Class<? extends ServiceModeBase>) serviceMode);
                coordinator.enqueueJob(interfaceName, stateUnbindWorker);
            }
        }
    }

    private class RendererStateInterfaceBindWorker implements Callable<List<ListenableFuture<Void>>> {
        String iface;
        BigInteger dpnId;
        FlowBasedServicesStateAddable flowBasedServicesStateAddable;
        Class<? extends ServiceModeBase> serviceMode;

        RendererStateInterfaceBindWorker(FlowBasedServicesStateAddable flowBasedServicesStateAddable, BigInteger dpnId,
                                         String iface, Class<? extends ServiceModeBase> serviceMode) {
            this.flowBasedServicesStateAddable = flowBasedServicesStateAddable;
            this.dpnId = dpnId;
            this.iface = iface;
            this.serviceMode = serviceMode;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            ServicesInfo servicesInfo = FlowBasedServicesUtils.getServicesInfoForInterface(iface,
                    serviceMode, broker);
            List<BoundServices> allServices = servicesInfo.getBoundServices();
            return flowBasedServicesStateAddable.bindServicesOnInterfaceType(dpnId, iface, servicesInfo, allServices,
                    broker);
        }
    }

    private class RendererStateInterfaceUnbindWorker implements Callable<List<ListenableFuture<Void>>> {
        String iface;
        BigInteger dpnId;
        FlowBasedServicesStateRemovable flowBasedServicesStateRemovable;
        Class<? extends ServiceModeBase> serviceMode;

        RendererStateInterfaceUnbindWorker(FlowBasedServicesStateRemovable flowBasedServicesStateRemovable,
                                           BigInteger dpnId, String iface,
                                           Class<? extends ServiceModeBase> serviceMode) {
            this.flowBasedServicesStateRemovable = flowBasedServicesStateRemovable;
            this.dpnId = dpnId;
            this.iface = iface;
            this.serviceMode = serviceMode;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            ServicesInfo servicesInfo = FlowBasedServicesUtils.getServicesInfoForInterface(iface, serviceMode, broker);
            List<BoundServices> allServices = servicesInfo.getBoundServices();
            return flowBasedServicesStateRemovable.unbindServicesFromInterfaceType(dpnId, iface, servicesInfo,
                    allServices, broker);
        }
    }
}
