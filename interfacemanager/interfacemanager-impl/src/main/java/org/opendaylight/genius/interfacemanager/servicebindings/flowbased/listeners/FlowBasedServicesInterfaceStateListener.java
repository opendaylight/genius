/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.listeners;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncDataTreeChangeListenerBase;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.interfacemanager.InterfacemgrProvider;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeBase;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.Callable;

@Singleton
public class FlowBasedServicesInterfaceStateListener extends AsyncDataTreeChangeListenerBase<Interface, FlowBasedServicesInterfaceStateListener> {
    private static final Logger LOG = LoggerFactory.getLogger(FlowBasedServicesInterfaceStateListener.class);
    private final DataBroker dataBroker;
    private final InterfacemgrProvider interfacemgrProvider;

    @Inject
    public FlowBasedServicesInterfaceStateListener(final InterfacemgrProvider interfacemgrProvider, DataBroker dataBroker) {
        super(Interface.class, FlowBasedServicesInterfaceStateListener.class);
        this.dataBroker = dataBroker;
        this.interfacemgrProvider = interfacemgrProvider;
        initializeFlowBasedServiceStateBindHelpers(interfacemgrProvider);
    }

    @PostConstruct
    public void start() throws Exception {
        this.registerListener(LogicalDatastoreType.OPERATIONAL, this.dataBroker);
        LOG.info("FlowBasedServicesInterfaceStateListener started");
    }

    @PreDestroy
    public void close() throws Exception {
        LOG.info("FlowBasedServicesInterfaceStateListener closed");
    }

    private void initializeFlowBasedServiceStateBindHelpers(InterfacemgrProvider interfaceMgrProvider) {
        FlowBasedIngressServicesStateBindHelper.intitializeFlowBasedIngressServicesStateAddHelper(interfaceMgrProvider);
        FlowBasedIngressServicesStateUnbindHelper.intitializeFlowBasedIngressServicesStateRemoveHelper(interfaceMgrProvider);
        FlowBasedEgressServicesStateBindHelper.intitializeFlowBasedEgressServicesStateBindHelper(interfaceMgrProvider);
        FlowBasedEgressServicesStateUnbindHelper.intitializeFlowBasedEgressServicesStateUnbindHelper(interfaceMgrProvider);
    }
    @Override
    protected InstanceIdentifier<Interface> getWildCardPath() {
        return InstanceIdentifier.create(InterfacesState.class).child(Interface.class);
    }

    @Override
    protected void remove(InstanceIdentifier<Interface> key, Interface interfaceStateOld) {
        LOG.debug("Received interface state remove event for {}", interfaceStateOld.getName());
        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
        for(Object serviceMode : FlowBasedServicesUtils.SERVICE_MODE_MAP.values()) {
            FlowBasedServicesStateRemovable flowBasedServicesStateRemovable = FlowBasedServicesStateRendererFactory.
                    getFlowBasedServicesStateRendererFactory((Class<? extends ServiceModeBase>) serviceMode).
                    getFlowBasedServicesStateRemoveRenderer();
            RendererStateInterfaceUnbindWorker stateUnbindWorker =
                    new RendererStateInterfaceUnbindWorker(flowBasedServicesStateRemovable, interfaceStateOld);
            coordinator.enqueueJob(interfaceStateOld.getName(), stateUnbindWorker);
        }
    }

    @Override
    protected void update(InstanceIdentifier<Interface> key, Interface interfaceStateOld, Interface interfaceStateNew) {
        LOG.debug("Received interface state update event for {},ignoring...", interfaceStateOld.getName());
    }

    @Override
    protected void add(InstanceIdentifier<Interface> key, Interface interfaceStateNew) {
        if (interfaceStateNew.getOperStatus() == Interface.OperStatus.Down) {
            LOG.info("Interface: {} operstate is down when adding. Not Binding services", interfaceStateNew.getName());
            return;
        }
        LOG.debug("Received interface state add event for {}", interfaceStateNew.getName());
        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
        for(Object serviceMode : FlowBasedServicesUtils.SERVICE_MODE_MAP.values()) {
            FlowBasedServicesStateAddable flowBasedServicesStateAddable = FlowBasedServicesStateRendererFactory.
                    getFlowBasedServicesStateRendererFactory((Class<? extends ServiceModeBase>) serviceMode).
                    getFlowBasedServicesStateAddRenderer();
            RendererStateInterfaceBindWorker stateBindWorker = new RendererStateInterfaceBindWorker(flowBasedServicesStateAddable,
                    interfaceStateNew);
            coordinator.enqueueJob(interfaceStateNew.getName(), stateBindWorker);
        }
    }

    @Override
    protected FlowBasedServicesInterfaceStateListener getDataTreeChangeListener() {
        return FlowBasedServicesInterfaceStateListener.this;
    }

    private class RendererStateInterfaceBindWorker implements Callable<List<ListenableFuture<Void>>> {
        Interface iface;
        FlowBasedServicesStateAddable flowBasedServicesStateAddable;

        public RendererStateInterfaceBindWorker(FlowBasedServicesStateAddable flowBasedServicesStateAddable,
                                                Interface iface) {
            this.flowBasedServicesStateAddable = flowBasedServicesStateAddable;
            this.iface = iface;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            return flowBasedServicesStateAddable.bindServicesOnInterface(iface);
        }
    }

    private class RendererStateInterfaceUnbindWorker implements Callable<List<ListenableFuture<Void>>> {
        Interface iface;
        FlowBasedServicesStateRemovable flowBasedServicesStateRemovable;

        public RendererStateInterfaceUnbindWorker(FlowBasedServicesStateRemovable flowBasedServicesStateRemovable,
                                                  Interface iface) {
            this.flowBasedServicesStateRemovable = flowBasedServicesStateRemovable;
            this.iface = iface;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            return flowBasedServicesStateRemovable.unbindServicesFromInterface(iface);
        }
    }
}