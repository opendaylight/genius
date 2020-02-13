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
import java.util.List;
import java.util.concurrent.Callable;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.aries.blueprint.annotation.service.Reference;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.recovery.impl.InterfaceServiceRecoveryHandler;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.factory.FlowBasedServicesStateAddable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.factory.FlowBasedServicesStateRendererFactoryResolver;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.serviceutils.srm.RecoverableListener;
import org.opendaylight.serviceutils.srm.ServiceRecoveryRegistry;
import org.opendaylight.serviceutils.tools.listener.AbstractClusteredSyncDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev170119.Other;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeBase;
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
    public FlowBasedServicesInterfaceStateListener(@Reference final DataBroker dataBroker,
                                                   final EntityOwnershipUtils entityOwnershipUtils,
                                                   @Reference final JobCoordinator coordinator,
                                                   final FlowBasedServicesStateRendererFactoryResolver
                                                           flowBasedServicesStateRendererFactoryResolver,
                                                   final InterfaceServiceRecoveryHandler
                                                           interfaceServiceRecoveryHandler,
                                                   @Reference final ServiceRecoveryRegistry serviceRecoveryRegistry) {
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
    public void remove(@NonNull final InstanceIdentifier<Interface> instanceIdentifier,
                       @NonNull final Interface interfaceStateOld) {
        //Do Nothing
    }

    @Override
    public void update(@NonNull InstanceIdentifier<Interface> instanceIdentifier, @NonNull Interface interfaceStateOld,
                       @NonNull Interface interfaceStateNew) {
        // Do nothing
    }

    @Override
    public void add(@NonNull InstanceIdentifier<Interface> instanceIdentifier, @NonNull Interface interfaceStateNew) {
        if (Other.class.equals(interfaceStateNew.getType()) || !entityOwnershipUtils
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

                        List<BoundServices> allServices = new ArrayList<>(servicesInfo.getBoundServices());
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
}
