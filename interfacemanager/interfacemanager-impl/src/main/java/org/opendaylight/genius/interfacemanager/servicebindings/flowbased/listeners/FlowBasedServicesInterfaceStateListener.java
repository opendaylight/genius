/*
 * Copyright (c) 2016, 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.listeners;

import static org.opendaylight.genius.infra.Datastore.CONFIGURATION;
import static org.opendaylight.genius.infra.Datastore.OPERATIONAL;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import com.google.common.util.concurrent.MoreExecutors;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.recovery.impl.InterfaceServiceRecoveryHandler;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.factory.FlowBasedServicesStateAddable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.factory.FlowBasedServicesStateRendererFactoryResolver;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.serviceutils.srm.RecoverableListener;
import org.opendaylight.serviceutils.srm.ServiceRecoveryRegistry;
import org.opendaylight.serviceutils.tools.mdsal.listener.AbstractClusteredSyncDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev170119.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev170119.Other;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeEgress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class FlowBasedServicesInterfaceStateListener extends AbstractClusteredSyncDataTreeChangeListener<Interface>
        implements RecoverableListener {

    private static final Logger LOG = LoggerFactory.getLogger(FlowBasedServicesInterfaceStateListener.class);

    private final ManagedNewTransactionRunner txRunner;
    private final EntityOwnershipUtils entityOwnershipUtils;
    private final JobCoordinator coordinator;
    private final FlowBasedServicesStateRendererFactoryResolver flowBasedServicesStateRendererFactoryResolver;

    @Inject
    public FlowBasedServicesInterfaceStateListener(final DataBroker dataBroker,
                                                   final EntityOwnershipUtils entityOwnershipUtils,
                                                   final JobCoordinator coordinator,
                                                   final FlowBasedServicesStateRendererFactoryResolver
                                                           flowBasedServicesStateRendererFactoryResolver,
                                                   final InterfaceServiceRecoveryHandler
                                                           interfaceServiceRecoveryHandler,
                                                   final ServiceRecoveryRegistry serviceRecoveryRegistry) {
        super(dataBroker, LogicalDatastoreType.OPERATIONAL,
              InstanceIdentifier.create(InterfacesState.class).child(Interface.class));
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.entityOwnershipUtils = entityOwnershipUtils;
        this.coordinator = coordinator;
        this.flowBasedServicesStateRendererFactoryResolver = flowBasedServicesStateRendererFactoryResolver;
        registerListener();
        serviceRecoveryRegistry.addRecoverableListener(interfaceServiceRecoveryHandler.buildServiceRegistryKey(), this);
    }

    @Override
    public void remove(@Nonnull final InstanceIdentifier<Interface> instanceIdentifier,
                       @Nonnull final Interface interfaceStateOld) {
        if (Other.class.equals(interfaceStateOld.getType()) || !entityOwnershipUtils
                .isEntityOwner(IfmConstants.INTERFACE_SERVICE_BINDING_ENTITY,
                               IfmConstants.INTERFACE_SERVICE_BINDING_ENTITY)) {
            return;
        }

        LOG.debug("Received interface state remove event for {}", interfaceStateOld.getName());
        // Unbind Default Egress Dispatcher Service when interface-state is removed.
        coordinator.enqueueJob(interfaceStateOld.getName(),
                               new RendererStateInterfaceUnbindWorker(coordinator, txRunner, interfaceStateOld),
                               IfmConstants.JOB_MAX_RETRIES);
    }

    @Override
    public void update(@Nonnull InstanceIdentifier<Interface> instanceIdentifier, @Nonnull Interface interfaceStateOld,
                       @Nonnull Interface interfaceStateNew) {
        // Do nothing
    }

    @Override
    public void add(@Nonnull InstanceIdentifier<Interface> instanceIdentifier, @Nonnull Interface interfaceStateNew) {
        if (interfaceStateNew.getType() == null || !entityOwnershipUtils
                .isEntityOwner(IfmConstants.INTERFACE_SERVICE_BINDING_ENTITY,
                               IfmConstants.INTERFACE_SERVICE_BINDING_ENTITY)) {
            return;
        }

        LOG.debug("Received interface state add event for {}", interfaceStateNew.getName());
        FlowBasedServicesUtils.SERVICE_MODE_MAP.values().forEach(serviceMode -> coordinator
                .enqueueJob(interfaceStateNew.getName(), new RendererStateInterfaceBindWorker(
                                    flowBasedServicesStateRendererFactoryResolver
                                            .getFlowBasedServicesStateRendererFactory(serviceMode)
                                            .getFlowBasedServicesStateAddRenderer(), interfaceStateNew, serviceMode),
                            IfmConstants.JOB_MAX_RETRIES));
    }

    @Override
    public void registerListener() {
        super.register();
    }

    @Override
    public void deregisterListener() {
        close();
    }

    private class RendererStateInterfaceBindWorker implements Callable<List<ListenableFuture<Void>>> {
        Interface iface;
        FlowBasedServicesStateAddable flowBasedServicesStateAddable;
        Class<? extends ServiceModeBase> serviceMode;

        RendererStateInterfaceBindWorker(FlowBasedServicesStateAddable flowBasedServicesStateAddable, Interface iface,
                                         Class<? extends ServiceModeBase> serviceMode) {
            this.flowBasedServicesStateAddable = flowBasedServicesStateAddable;
            this.iface = iface;
            this.serviceMode = serviceMode;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            List<ListenableFuture<Void>> futures = new ArrayList<>();
            futures.add(txRunner.applyWithNewReadWriteTransactionAndSubmit(CONFIGURATION,
                confTx -> FlowBasedServicesUtils.getServicesInfoForInterface(confTx, iface.getName(),
                    serviceMode)).transformAsync(servicesInfo -> {
                        if (servicesInfo == null) {
                            LOG.trace("service info is null for interface {}", iface.getName());
                            return Futures.immediateFuture(null);
                        }

                        List<BoundServices> allServices = servicesInfo.getBoundServices();
                        if (allServices == null || allServices.isEmpty()) {
                            LOG.trace("bound services is empty for interface {}", iface.getName());
                            return Futures.immediateFuture(null);
                        }
                        return txRunner.callWithNewWriteOnlyTransactionAndSubmit(OPERATIONAL, operTx -> {
                            // Build the service-binding state if there are services bound on this interface
                            FlowBasedServicesUtils.addBoundServicesState(operTx, iface.getName(), FlowBasedServicesUtils
                                .buildBoundServicesState(iface, serviceMode));
                            flowBasedServicesStateAddable.bindServices(futures, iface, allServices, serviceMode);
                        });
                    }, MoreExecutors.directExecutor()));
            return futures;
        }
    }

    private static class RendererStateInterfaceUnbindWorker implements Callable<List<ListenableFuture<Void>>> {
        private final Interface iface;
        private final JobCoordinator coordinator;
        private final ManagedNewTransactionRunner txRunner;

        RendererStateInterfaceUnbindWorker(JobCoordinator coordinator, ManagedNewTransactionRunner txRunner,
                                           Interface iface) {
            this.iface = iface;
            this.coordinator = coordinator;
            this.txRunner = txRunner;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            coordinator.enqueueJob(iface.getName(), () -> Collections
                    .singletonList(txRunner.callWithNewReadWriteTransactionAndSubmit(CONFIGURATION, tx -> {
                        LOG.debug("unbinding services on interface {}", iface.getName());
                        ServicesInfo servicesInfo = FlowBasedServicesUtils
                                .getServicesInfoForInterface(tx, iface.getName(), ServiceModeEgress.class);
                        if (servicesInfo == null) {
                            LOG.trace("service info is null for interface {}", iface.getName());
                            return;
                        }

                        List<BoundServices> allServices = servicesInfo.getBoundServices();
                        if (allServices == null || allServices.isEmpty()) {
                            LOG.trace("bound services is empty for interface {}", iface.getName());
                            return;
                        }

                        if (L2vlan.class.equals(iface.getType())) {
                            // remove the default egress service bound on the interface
                            IfmUtil.unbindService(tx, iface.getName(),
                                                  FlowBasedServicesUtils.buildDefaultServiceId(iface.getName()));
                        }
                    })));
            return Collections.emptyList();
        }
    }
}
