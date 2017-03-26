/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.listeners;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncClusteredDataTreeChangeListenerBase;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.InterfacemgrProvider;
import org.opendaylight.genius.interfacemanager.renderer.ovs.utilities.IfmClusterUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.factory.FlowBasedServicesStateAddable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.factory.FlowBasedServicesStateRemovable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.factory.FlowBasedServicesStateRendererFactory;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.helpers.FlowBasedEgressServicesStateBindHelper;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.helpers.FlowBasedEgressServicesStateUnbindHelper;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.helpers.FlowBasedIngressServicesStateBindHelper;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.helpers.FlowBasedIngressServicesStateUnbindHelper;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class FlowBasedServicesInterfaceStateListener
        extends AsyncClusteredDataTreeChangeListenerBase<Interface, FlowBasedServicesInterfaceStateListener> {
    private static final Logger LOG = LoggerFactory.getLogger(FlowBasedServicesInterfaceStateListener.class);

    @Inject
    public FlowBasedServicesInterfaceStateListener(final InterfacemgrProvider interfacemgrProvider,
            DataBroker dataBroker) {
        super(Interface.class, FlowBasedServicesInterfaceStateListener.class);
        initializeFlowBasedServiceStateBindHelpers(interfacemgrProvider);
        this.registerListener(LogicalDatastoreType.OPERATIONAL, dataBroker);
    }

    @PostConstruct
    public void start() throws Exception {

        LOG.info("FlowBasedServicesInterfaceStateListener started");
    }

    @Override
    @PreDestroy
    public void close() {
        LOG.info("FlowBasedServicesInterfaceStateListener closed");
    }

    private void initializeFlowBasedServiceStateBindHelpers(InterfacemgrProvider interfaceMgrProvider) {
        FlowBasedIngressServicesStateBindHelper.intitializeFlowBasedIngressServicesStateAddHelper(interfaceMgrProvider);
        FlowBasedIngressServicesStateUnbindHelper
                .intitializeFlowBasedIngressServicesStateRemoveHelper(interfaceMgrProvider);
        FlowBasedEgressServicesStateBindHelper.intitializeFlowBasedEgressServicesStateBindHelper(interfaceMgrProvider);
        FlowBasedEgressServicesStateUnbindHelper
                .intitializeFlowBasedEgressServicesStateUnbindHelper(interfaceMgrProvider);
    }

    @Override
    protected InstanceIdentifier<Interface> getWildCardPath() {
        return InstanceIdentifier.create(InterfacesState.class).child(Interface.class);
    }

    @Override
    protected void remove(InstanceIdentifier<Interface> key, Interface interfaceStateOld) {
        if (interfaceStateOld.getType() == null
                || !IfmClusterUtils.isEntityOwner(IfmClusterUtils.INTERFACE_SERVICE_BINDING_ENTITY)) {
            return;
        }

        LOG.debug("Received interface state remove event for {}", interfaceStateOld.getName());
        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
        FlowBasedServicesUtils.SERVICE_MODE_MAP.values().stream()
                .forEach(serviceMode -> coordinator.enqueueJob(interfaceStateOld.getName(),
                        new RendererStateInterfaceUnbindWorker(FlowBasedServicesStateRendererFactory
                                .getFlowBasedServicesStateRendererFactory(serviceMode)
                                .getFlowBasedServicesStateRemoveRenderer(), interfaceStateOld),
                        IfmConstants.JOB_MAX_RETRIES));
    }

    @Override
    protected void update(InstanceIdentifier<Interface> key, Interface interfaceStateOld, Interface interfaceStateNew) {
        LOG.debug("Received interface state update event for {},ignoring...", interfaceStateOld.getName());
    }

    @Override
    protected void add(InstanceIdentifier<Interface> key, Interface interfaceStateNew) {
        if (interfaceStateNew.getType() == null
                || !IfmClusterUtils.isEntityOwner(IfmClusterUtils.INTERFACE_SERVICE_BINDING_ENTITY)) {
            return;
        }
        LOG.debug("Received interface state add event for {}", interfaceStateNew.getName());
        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
        FlowBasedServicesUtils.SERVICE_MODE_MAP.values().stream().forEach(serviceMode -> coordinator.enqueueJob(
                interfaceStateNew.getName(),
                new RendererStateInterfaceBindWorker(FlowBasedServicesStateRendererFactory
                        .getFlowBasedServicesStateRendererFactory(serviceMode).getFlowBasedServicesStateAddRenderer(),
                        interfaceStateNew),
                IfmConstants.JOB_MAX_RETRIES));
    }

    @Override
    protected FlowBasedServicesInterfaceStateListener getDataTreeChangeListener() {
        return FlowBasedServicesInterfaceStateListener.this;
    }

    private class RendererStateInterfaceBindWorker implements Callable<List<ListenableFuture<Void>>> {
        Interface iface;
        FlowBasedServicesStateAddable flowBasedServicesStateAddable;

        RendererStateInterfaceBindWorker(FlowBasedServicesStateAddable flowBasedServicesStateAddable,
                Interface iface) {
            this.flowBasedServicesStateAddable = flowBasedServicesStateAddable;
            this.iface = iface;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            return flowBasedServicesStateAddable.bindServicesOnInterface(iface);
        }
    }

    private class RendererStateInterfaceUnbindWorker implements Callable<List<ListenableFuture<Void>>> {
        Interface iface;
        FlowBasedServicesStateRemovable flowBasedServicesStateRemovable;

        RendererStateInterfaceUnbindWorker(FlowBasedServicesStateRemovable flowBasedServicesStateRemovable,
                Interface iface) {
            this.flowBasedServicesStateRemovable = flowBasedServicesStateRemovable;
            this.iface = iface;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            return flowBasedServicesStateRemovable.unbindServicesFromInterface(iface);
        }
    }
}
