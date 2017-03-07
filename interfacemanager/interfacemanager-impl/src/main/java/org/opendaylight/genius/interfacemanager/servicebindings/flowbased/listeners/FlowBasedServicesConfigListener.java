/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.listeners;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncClusteredDataTreeChangeListenerBase;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.InterfacemgrProvider;
import org.opendaylight.genius.interfacemanager.renderer.ovs.utilities.IfmClusterUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.factory.FlowBasedServicesConfigAddable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.factory.FlowBasedServicesConfigRemovable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.factory.FlowBasedServicesRendererFactory;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.helpers.FlowBasedEgressServicesConfigBindHelper;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.helpers.FlowBasedEgressServicesConfigUnbindHelper;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.helpers.FlowBasedIngressServicesConfigBindHelper;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.helpers.FlowBasedIngressServicesConfigUnbindHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceBindings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfoKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class FlowBasedServicesConfigListener extends AsyncClusteredDataTreeChangeListenerBase<BoundServices, FlowBasedServicesConfigListener> {
    private static final Logger LOG = LoggerFactory.getLogger(FlowBasedServicesConfigListener.class);

    @Inject
    public FlowBasedServicesConfigListener(final DataBroker dataBroker, final InterfacemgrProvider interfacemgrProvider) {
        super(BoundServices.class, FlowBasedServicesConfigListener.class);
        initializeFlowBasedServiceHelpers(interfacemgrProvider);
        registerListener(LogicalDatastoreType.CONFIGURATION, dataBroker);
    }

    private void initializeFlowBasedServiceHelpers(InterfacemgrProvider interfaceMgrProvider) {
        FlowBasedIngressServicesConfigBindHelper
                .intitializeFlowBasedIngressServicesConfigAddHelper(interfaceMgrProvider);
        FlowBasedIngressServicesConfigUnbindHelper
                .intitializeFlowBasedIngressServicesConfigRemoveHelper(interfaceMgrProvider);
        FlowBasedEgressServicesConfigBindHelper.intitializeFlowBasedEgressServicesConfigAddHelper(interfaceMgrProvider);
        FlowBasedEgressServicesConfigUnbindHelper
                .intitializeFlowBasedEgressServicesConfigRemoveHelper(interfaceMgrProvider);
    }

    @Override
    protected InstanceIdentifier<BoundServices> getWildCardPath() {
        return InstanceIdentifier.create(ServiceBindings.class).child(ServicesInfo.class)
                .child(BoundServices.class);
    }

    @Override
    protected void remove(InstanceIdentifier<BoundServices> key, BoundServices boundServiceOld) {
        IfmClusterUtils.runOnlyInLeaderNode(() -> {
            ServicesInfoKey serviceKey = InstanceIdentifier.keyOf(key.firstIdentifierOf(ServicesInfo.class));
            LOG.info("Service Binding Entry removed for Interface: {}, Data: {}", serviceKey.getInterfaceName(),
                boundServiceOld);
            FlowBasedServicesConfigRemovable flowBasedServicesConfigRemovable =
                FlowBasedServicesRendererFactory.getFlowBasedServicesRendererFactory(serviceKey.getServiceMode())
                    .getFlowBasedServicesRemoveRenderer();
            DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
            RendererConfigRemoveWorker configWorker =
                new RendererConfigRemoveWorker(flowBasedServicesConfigRemovable, key, boundServiceOld);
            coordinator.enqueueJob(serviceKey.getInterfaceName(), configWorker, IfmConstants.JOB_MAX_RETRIES);
        }, IfmClusterUtils.INTERFACE_SERVICE_BINDING_ENTITY);
    }

    @Override
    protected void update(InstanceIdentifier<BoundServices> key, BoundServices boundServiceOld,
            BoundServices boundServiceNew) {
        if (!Objects.equals(boundServiceOld, boundServiceNew)) {
            LOG.error("Service Binding entry update not allowed for: {}, Data: {}",
                    InstanceIdentifier.keyOf(key.firstIdentifierOf(ServicesInfo.class)).getInterfaceName(),
                    boundServiceNew);
        }
    }

    @Override
    protected void add(InstanceIdentifier<BoundServices> key, BoundServices boundServicesNew) {
        IfmClusterUtils.runOnlyInLeaderNode(() -> {
            ServicesInfoKey serviceKey = InstanceIdentifier.keyOf(key.firstIdentifierOf(ServicesInfo.class));
            LOG.info("Service Binding Entry created for Interface: {}, Data: {}", serviceKey.getInterfaceName(),
                boundServicesNew);
            FlowBasedServicesConfigAddable flowBasedServicesAddable =
                FlowBasedServicesRendererFactory.getFlowBasedServicesRendererFactory(serviceKey.getServiceMode())
                    .getFlowBasedServicesAddRenderer();
            DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
            RendererConfigAddWorker configWorker =
                new RendererConfigAddWorker(flowBasedServicesAddable, key, boundServicesNew);
            coordinator.enqueueJob(serviceKey.getInterfaceName(), configWorker, IfmConstants.JOB_MAX_RETRIES);
        }, IfmClusterUtils.INTERFACE_SERVICE_BINDING_ENTITY);
    }

    @Override
    protected FlowBasedServicesConfigListener getDataTreeChangeListener() {
        return FlowBasedServicesConfigListener.this;
    }

    private class RendererConfigAddWorker implements Callable<List<ListenableFuture<Void>>> {
        FlowBasedServicesConfigAddable flowBasedServicesAddable;
        InstanceIdentifier<BoundServices> instanceIdentifier;
        BoundServices boundServicesNew;

        RendererConfigAddWorker(FlowBasedServicesConfigAddable flowBasedServicesAddable,
                InstanceIdentifier<BoundServices> instanceIdentifier, BoundServices boundServicesNew) {
            this.flowBasedServicesAddable = flowBasedServicesAddable;
            this.instanceIdentifier = instanceIdentifier;
            this.boundServicesNew = boundServicesNew;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            return flowBasedServicesAddable.bindService(instanceIdentifier,
                    boundServicesNew);
        }
    }

    private class RendererConfigRemoveWorker implements Callable<List<ListenableFuture<Void>>> {
        FlowBasedServicesConfigRemovable flowBasedServicesConfigRemovable;
        InstanceIdentifier<BoundServices> instanceIdentifier;
        BoundServices boundServicesNew;

        RendererConfigRemoveWorker(FlowBasedServicesConfigRemovable flowBasedServicesConfigRemovable,
                                          InstanceIdentifier<BoundServices> instanceIdentifier,
                                          BoundServices boundServicesNew) {
            this.flowBasedServicesConfigRemovable = flowBasedServicesConfigRemovable;
            this.instanceIdentifier = instanceIdentifier;
            this.boundServicesNew = boundServicesNew;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            return flowBasedServicesConfigRemovable.unbindService(instanceIdentifier,
                    boundServicesNew);
        }
    }
}
