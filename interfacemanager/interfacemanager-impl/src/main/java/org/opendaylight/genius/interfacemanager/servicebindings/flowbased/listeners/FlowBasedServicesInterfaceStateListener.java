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
import org.opendaylight.genius.datastoreutils.AsyncDataTreeChangeListenerBase;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.statehelpers.FlowBasedServicesStateBindHelper;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.statehelpers.FlowBasedServicesStateUnbindHelper;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeBase;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Callable;

public class FlowBasedServicesInterfaceStateListener extends AsyncDataTreeChangeListenerBase<Interface, FlowBasedServicesInterfaceStateListener> {
    private static final Logger LOG = LoggerFactory.getLogger(FlowBasedServicesInterfaceStateListener.class);
    private DataBroker dataBroker;

    public FlowBasedServicesInterfaceStateListener(final DataBroker dataBroker) {
        super(Interface.class, FlowBasedServicesInterfaceStateListener.class);
        this.dataBroker = dataBroker;
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
            RendererStateInterfaceUnbindWorker stateUnbindWorker =
                    new RendererStateInterfaceUnbindWorker((Class<? extends ServiceModeBase>) serviceMode, interfaceStateOld);
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
            RendererStateInterfaceBindWorker stateBindWorker = new RendererStateInterfaceBindWorker((Class<? extends ServiceModeBase>) serviceMode, interfaceStateNew);
            coordinator.enqueueJob(interfaceStateNew.getName(), stateBindWorker);
        }
    }

    @Override
    protected FlowBasedServicesInterfaceStateListener getDataTreeChangeListener() {
        return FlowBasedServicesInterfaceStateListener.this;
    }

    private class RendererStateInterfaceBindWorker implements Callable<List<ListenableFuture<Void>>> {
        Interface iface;
        Class<? extends ServiceModeBase> serviceMode;

        public RendererStateInterfaceBindWorker(Class<? extends ServiceModeBase> serviceMode,
                                                Interface iface) {
            this.serviceMode = serviceMode;
            this.iface = iface;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            return FlowBasedServicesStateBindHelper.bindServicesOnInterface(iface, serviceMode, dataBroker);
        }
    }

    private class RendererStateInterfaceUnbindWorker implements Callable<List<ListenableFuture<Void>>> {
        Interface iface;
        Class<? extends ServiceModeBase> serviceMode;

        public RendererStateInterfaceUnbindWorker(Class<? extends ServiceModeBase> serviceMode,
                                                  Interface iface) {
            this.serviceMode = serviceMode;
            this.iface = iface;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            return FlowBasedServicesStateUnbindHelper.unbindServicesFromInterface(iface, serviceMode, dataBroker);
        }
    }
}