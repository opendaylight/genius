/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.listeners;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.genius.datastoreutils.AsyncDataTreeChangeListenerBase;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.interfacemanager.InterfacemgrProvider;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.confighelpers.FlowBasedEgressServicesConfigBindHelper;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.confighelpers.FlowBasedEgressServicesConfigUnbindHelper;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.confighelpers.FlowBasedIngressServicesConfigBindHelper;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.confighelpers.FlowBasedIngressServicesConfigUnbindHelper;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesAddable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesRemovable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesRendererFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceBindings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Callable;

public class FlowBasedServicesConfigListener extends AsyncDataTreeChangeListenerBase<BoundServices, FlowBasedServicesConfigListener> {
    private static final Logger LOG = LoggerFactory.getLogger(FlowBasedServicesConfigListener.class);
    private InterfacemgrProvider interfacemgrProvider;

    public FlowBasedServicesConfigListener(InterfacemgrProvider interfacemgrProvider) {
        super(BoundServices.class, FlowBasedServicesConfigListener.class);
        this.interfacemgrProvider = interfacemgrProvider;
        initializeFlowBasedServiceHelpers(interfacemgrProvider);
    }

    private void initializeFlowBasedServiceHelpers(InterfacemgrProvider interfaceMgrProvider) {
        FlowBasedIngressServicesConfigBindHelper.intitializeFlowBasedIngressServicesConfigAddHelper(interfaceMgrProvider);
        FlowBasedIngressServicesConfigUnbindHelper.intitializeFlowBasedIngressServicesConfigRemoveHelper(interfaceMgrProvider);
        FlowBasedEgressServicesConfigBindHelper.intitializeFlowBasedEgressServicesConfigAddHelper(interfaceMgrProvider);
        FlowBasedEgressServicesConfigUnbindHelper.intitializeFlowBasedEgressServicesConfigRemoveHelper(interfaceMgrProvider);
    }
    @Override
    protected InstanceIdentifier<BoundServices> getWildCardPath() {
        return InstanceIdentifier.create(ServiceBindings.class).child(ServicesInfo.class)
                .child(BoundServices.class);
    }

    @Override
    protected void remove(InstanceIdentifier<BoundServices> key, BoundServices boundServiceOld) {
        String interfaceName = InstanceIdentifier.keyOf(key.firstIdentifierOf(ServicesInfo.class)).getInterfaceName();
        LOG.info("Service Binding Entry removed for Interface: {}, Data: {}",
                interfaceName, boundServiceOld);
        Class<? extends ServiceModeBase> serviceMode = InstanceIdentifier.keyOf(key.firstIdentifierOf(ServicesInfo.class)).getServiceMode();
        FlowBasedServicesRemovable flowBasedServicesRemovable = FlowBasedServicesRendererFactory.getFlowBasedServicesRendererFactory(serviceMode).
                getFlowBasedServicesRemoveRenderer();
        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
        RendererConfigRemoveWorker configWorker = new RendererConfigRemoveWorker(flowBasedServicesRemovable, key, boundServiceOld);
        coordinator.enqueueJob(interfaceName, configWorker);
    }

    @Override
    protected void update(InstanceIdentifier<BoundServices> key, BoundServices boundServiceOld,
                          BoundServices boundServiceNew) {
        LOG.error("Service Binding entry update not allowed for: {}, Data: {}",
                InstanceIdentifier.keyOf(key.firstIdentifierOf(ServicesInfo.class)).getInterfaceName(), boundServiceNew);
    }

    @Override
    protected void add(InstanceIdentifier<BoundServices> key, BoundServices boundServicesNew) {
        String interfaceName = InstanceIdentifier.keyOf(key.firstIdentifierOf(ServicesInfo.class)).getInterfaceName();
        LOG.info("Service Binding Entry created for Interface: {}, Data: {}",
                interfaceName, boundServicesNew);
        Class<? extends ServiceModeBase> serviceMode = InstanceIdentifier.keyOf(key.firstIdentifierOf(ServicesInfo.class)).getServiceMode();

        FlowBasedServicesAddable flowBasedServicesAddable = FlowBasedServicesRendererFactory.
                getFlowBasedServicesRendererFactory(serviceMode).getFlowBasedServicesAddRenderer();
        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
        RendererConfigAddWorker configWorker = new RendererConfigAddWorker(flowBasedServicesAddable, key, boundServicesNew);
        coordinator.enqueueJob(interfaceName, configWorker);
    }

    @Override
    protected FlowBasedServicesConfigListener getDataTreeChangeListener() {
        return FlowBasedServicesConfigListener.this;
    }

    private class RendererConfigAddWorker implements Callable<List<ListenableFuture<Void>>> {
        FlowBasedServicesAddable flowBasedServicesAddable;
        InstanceIdentifier<BoundServices> instanceIdentifier;
        BoundServices boundServicesNew;

        public RendererConfigAddWorker(FlowBasedServicesAddable flowBasedServicesAddable,
                                       InstanceIdentifier<BoundServices> instanceIdentifier,
                                       BoundServices boundServicesNew) {
            this.flowBasedServicesAddable = flowBasedServicesAddable;
            this.instanceIdentifier = instanceIdentifier;
            this.boundServicesNew = boundServicesNew;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            return flowBasedServicesAddable.bindService(instanceIdentifier,
                    boundServicesNew);
        }
    }

    private class RendererConfigRemoveWorker implements Callable<List<ListenableFuture<Void>>> {
        FlowBasedServicesRemovable flowBasedServicesRemovable;
        InstanceIdentifier<BoundServices> instanceIdentifier;
        BoundServices boundServicesNew;

        public RendererConfigRemoveWorker(FlowBasedServicesRemovable flowBasedServicesRemovable,
                                          InstanceIdentifier<BoundServices> instanceIdentifier,
                                          BoundServices boundServicesNew) {
            this.flowBasedServicesRemovable = flowBasedServicesRemovable;
            this.instanceIdentifier = instanceIdentifier;
            this.boundServicesNew = boundServicesNew;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            return flowBasedServicesRemovable.unbindService(instanceIdentifier,
                    boundServicesNew);
        }
    }
}