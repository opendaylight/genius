/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
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
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncClusteredDataTreeChangeListenerBase;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.factory.FlowBasedServicesStateAddable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.factory.FlowBasedServicesStateRemovable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.factory.FlowBasedServicesStateRendererFactoryResolver;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Other;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class FlowBasedServicesInterfaceStateListener
        extends AsyncClusteredDataTreeChangeListenerBase<Interface, FlowBasedServicesInterfaceStateListener> {

    private static final Logger LOG = LoggerFactory.getLogger(FlowBasedServicesInterfaceStateListener.class);

    private final DataBroker dataBroker;
    private final EntityOwnershipUtils entityOwnershipUtils;
    private final JobCoordinator coordinator;
    private final FlowBasedServicesStateRendererFactoryResolver flowBasedServicesStateRendererFactoryResolver;

    @Inject
    public FlowBasedServicesInterfaceStateListener(final DataBroker dataBroker,
            final EntityOwnershipUtils entityOwnershipUtils, final JobCoordinator coordinator,
            final FlowBasedServicesStateRendererFactoryResolver flowBasedServicesStateRendererFactoryResolver) {
        super(Interface.class, FlowBasedServicesInterfaceStateListener.class);
        this.dataBroker = dataBroker;
        this.entityOwnershipUtils = entityOwnershipUtils;
        this.coordinator = coordinator;
        this.flowBasedServicesStateRendererFactoryResolver = flowBasedServicesStateRendererFactoryResolver;
        this.registerListener(LogicalDatastoreType.OPERATIONAL, dataBroker);
    }

    @Override
    protected InstanceIdentifier<Interface> getWildCardPath() {
        return InstanceIdentifier.create(InterfacesState.class).child(Interface.class);
    }

    @Override
    protected void remove(InstanceIdentifier<Interface> key, Interface interfaceStateOld) {
        if (Other.class.equals(interfaceStateOld.getType())
                || !entityOwnershipUtils.isEntityOwner(IfmConstants.INTERFACE_SERVICE_BINDING_ENTITY,
                        IfmConstants.INTERFACE_SERVICE_BINDING_ENTITY)) {
            return;
        }

        LOG.debug("Received interface state remove event for {}", interfaceStateOld.getName());
        FlowBasedServicesUtils.SERVICE_MODE_MAP.values().stream()
                .forEach(serviceMode -> coordinator.enqueueJob(interfaceStateOld.getName(),
                        new RendererStateInterfaceUnbindWorker(flowBasedServicesStateRendererFactoryResolver
                            .getFlowBasedServicesStateRendererFactory(serviceMode)
                                .getFlowBasedServicesStateRemoveRenderer(), interfaceStateOld, serviceMode),
                        IfmConstants.JOB_MAX_RETRIES));
    }

    @Override
    protected void update(InstanceIdentifier<Interface> key, Interface interfaceStateOld, Interface interfaceStateNew) {
        // Do nothing
    }

    @Override
    protected void add(InstanceIdentifier<Interface> key, Interface interfaceStateNew) {
        if (interfaceStateNew.getType() == null
            || !entityOwnershipUtils.isEntityOwner(IfmConstants.INTERFACE_SERVICE_BINDING_ENTITY,
                    IfmConstants.INTERFACE_SERVICE_BINDING_ENTITY)) {
            return;
        }

        LOG.debug("Received interface state add event for {}", interfaceStateNew.getName());
        FlowBasedServicesUtils.SERVICE_MODE_MAP.values().stream().forEach(serviceMode -> coordinator.enqueueJob(
                interfaceStateNew.getName(),
                new RendererStateInterfaceBindWorker(flowBasedServicesStateRendererFactoryResolver
                        .getFlowBasedServicesStateRendererFactory(serviceMode).getFlowBasedServicesStateAddRenderer(),
                        interfaceStateNew, serviceMode),
                IfmConstants.JOB_MAX_RETRIES));
    }

    @Override
    protected FlowBasedServicesInterfaceStateListener getDataTreeChangeListener() {
        return FlowBasedServicesInterfaceStateListener.this;
    }

    private class RendererStateInterfaceBindWorker implements Callable<List<ListenableFuture<Void>>> {
        Interface iface;
        FlowBasedServicesStateAddable flowBasedServicesStateAddable;
        Class<? extends ServiceModeBase> serviceMode;

        RendererStateInterfaceBindWorker(FlowBasedServicesStateAddable flowBasedServicesStateAddable,
                                         Interface iface, Class<? extends ServiceModeBase> serviceMode) {
            this.flowBasedServicesStateAddable = flowBasedServicesStateAddable;
            this.iface = iface;
            this.serviceMode = serviceMode;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            ServicesInfo servicesInfo = FlowBasedServicesUtils.getServicesInfoForInterface(iface.getName(),
                serviceMode, dataBroker);
            if (servicesInfo == null) {
                LOG.trace("service info is null for interface {}", iface.getName());
                return null;
            }

            List<BoundServices> allServices = servicesInfo.getBoundServices();
            if (allServices == null || allServices.isEmpty()) {
                LOG.trace("bound services is empty for interface {}", iface.getName());
                return null;
            }
            List<ListenableFuture<Void>> futures = new ArrayList<>();
            // Build the service-binding state if there are services bound on this interface
            FlowBasedServicesUtils.addBoundServicesState(futures, dataBroker, iface.getName(),
                FlowBasedServicesUtils.buildBoundServicesState(iface, serviceMode));
            flowBasedServicesStateAddable.bindServices(futures, iface, allServices, serviceMode);
            return futures;
        }
    }

    private static class RendererStateInterfaceUnbindWorker implements Callable<List<ListenableFuture<Void>>> {
        Interface iface;
        FlowBasedServicesStateRemovable flowBasedServicesStateRemovable;
        Class<? extends ServiceModeBase> serviceMode;

        RendererStateInterfaceUnbindWorker(FlowBasedServicesStateRemovable flowBasedServicesStateRemovable,
                Interface iface, Class<? extends ServiceModeBase> serviceMode) {
            this.flowBasedServicesStateRemovable = flowBasedServicesStateRemovable;
            this.iface = iface;
            this.serviceMode = serviceMode;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            List<ListenableFuture<Void>> futures = new ArrayList<>();
            flowBasedServicesStateRemovable.unbindServices(futures, iface, serviceMode);
            return futures;
        }
    }
}
