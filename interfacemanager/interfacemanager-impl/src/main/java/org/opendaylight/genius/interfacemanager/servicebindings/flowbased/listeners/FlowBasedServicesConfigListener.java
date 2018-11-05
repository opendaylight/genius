/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.listeners;

import static org.opendaylight.genius.infra.Datastore.OPERATIONAL;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.recovery.impl.InterfaceServiceRecoveryHandler;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.factory.FlowBasedServicesConfigAddable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.factory.FlowBasedServicesConfigRemovable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.factory.FlowBasedServicesRendererFactoryResolver;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.serviceutils.srm.RecoverableListener;
import org.opendaylight.serviceutils.srm.ServiceRecoveryRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceBindings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.bound.services.state.list.BoundServicesState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfoKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServicesKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class FlowBasedServicesConfigListener implements ClusteredDataTreeChangeListener<ServicesInfo>,
        RecoverableListener {

    private static final Logger LOG = LoggerFactory.getLogger(FlowBasedServicesConfigListener.class);

    private ListenerRegistration<FlowBasedServicesConfigListener> listenerRegistration;
    private final DataBroker dataBroker;
    private final ManagedNewTransactionRunner txRunner;
    private final EntityOwnershipUtils entityOwnershipUtils;
    private final JobCoordinator coordinator;
    private final FlowBasedServicesRendererFactoryResolver flowBasedServicesRendererFactoryResolver;
    private final InterfaceManagerCommonUtils interfaceManagerCommonUtils;

    @Inject
    public FlowBasedServicesConfigListener(final DataBroker dataBroker,
                                           final EntityOwnershipUtils entityOwnershipUtils,
                                           final JobCoordinator coordinator,
                                           final FlowBasedServicesRendererFactoryResolver
                                                       flowBasedServicesRendererFactoryResolver,
                                           final InterfaceManagerCommonUtils interfaceManagerCommonUtils,
                                           final InterfaceServiceRecoveryHandler interfaceServiceRecoveryHandler,
                                           final ServiceRecoveryRegistry serviceRecoveryRegistry) {
        this.dataBroker = dataBroker;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.entityOwnershipUtils = entityOwnershipUtils;
        this.coordinator = coordinator;
        this.flowBasedServicesRendererFactoryResolver = flowBasedServicesRendererFactoryResolver;
        this.interfaceManagerCommonUtils = interfaceManagerCommonUtils;
        registerListener();
        serviceRecoveryRegistry.addRecoverableListener(interfaceServiceRecoveryHandler.buildServiceRegistryKey(),
                this);
    }

    protected InstanceIdentifier<ServicesInfo> getWildCardPath() {
        return InstanceIdentifier.create(ServiceBindings.class).child(ServicesInfo.class);
    }

    @Override
    public void registerListener() {
        registerListener(LogicalDatastoreType.CONFIGURATION, dataBroker);
    }

    public void registerListener(final LogicalDatastoreType dsType, final DataBroker db) {
        final DataTreeIdentifier<ServicesInfo> treeId = new DataTreeIdentifier<>(dsType, getWildCardPath());
        listenerRegistration = db.registerDataTreeChangeListener(treeId, FlowBasedServicesConfigListener.this);
    }

    @Override
    public  void deregisterListener() {
        close();
    }

    @PreDestroy
    public void close() {
        if (listenerRegistration != null) {
            try {
                listenerRegistration.close();
            } finally {
                listenerRegistration = null;
            }
        }
    }

    @Override
    public void onDataTreeChanged(@Nonnull final Collection<DataTreeModification<ServicesInfo>> collection) {
        collection.forEach(
            servicesInfoDataTreeModification -> servicesInfoDataTreeModification.getRootNode()
                    .getModifiedChildren().stream()
                    .filter(dataObjectModification -> dataObjectModification.getDataType().equals(
                            BoundServices.class))
                    .forEach(dataObjectModification -> onBoundServicesChanged(
                            (DataObjectModification<BoundServices>) dataObjectModification,
                            servicesInfoDataTreeModification.getRootPath().getRootIdentifier(),
                            servicesInfoDataTreeModification.getRootNode()))
        );
    }

    private InstanceIdentifier<BoundServices> getBoundServicesInstanceIdentifier(
            final InstanceIdentifier<ServicesInfo> rootIdentifier, final BoundServicesKey boundServicesKey) {
        return rootIdentifier.child(BoundServices.class , boundServicesKey);
    }

    private synchronized void onBoundServicesChanged(final DataObjectModification<BoundServices> dataObjectModification,
                                                     final InstanceIdentifier<ServicesInfo> rootIdentifier,
                                                     final DataObjectModification<ServicesInfo> rootNode) {
        if (rootNode.getDataAfter() != null) {
            final List<BoundServices> boundServices = rootNode.getDataAfter().getBoundServices();
            final ServicesInfoKey servicesInfoKey = rootNode.getDataAfter().key();
            final BoundServices boundServicesBefore = dataObjectModification.getDataBefore();
            final BoundServices boundServicesAfter =  dataObjectModification.getDataAfter();

            switch (dataObjectModification.getModificationType()) {
                case DELETE:
                    if (boundServicesBefore != null) {
                        remove(servicesInfoKey, boundServicesBefore, boundServices);
                    }
                    break;
                case SUBTREE_MODIFIED:
                    if (boundServicesBefore != null) {
                        update(servicesInfoKey, getBoundServicesInstanceIdentifier(rootIdentifier,
                            boundServicesBefore.key()), boundServicesBefore, boundServicesAfter, boundServices);
                    }
                    break;
                case WRITE:
                    if (boundServicesBefore == null) {
                        add(servicesInfoKey, boundServicesAfter, boundServices);
                    } else {
                        update(servicesInfoKey, getBoundServicesInstanceIdentifier(rootIdentifier,
                            boundServicesBefore.key()), boundServicesBefore, boundServicesAfter, boundServices);
                    }
                    break;
                default:
                    LOG.error("Unhandled Modificiation Type{} for {}", dataObjectModification.getModificationType(),
                        rootIdentifier);

            }
        }
    }

    protected void remove(@Nonnull final ServicesInfoKey serviceKey, @Nonnull  final BoundServices boundServiceOld,
                          final List<BoundServices> boundServicesList) {
        if (!entityOwnershipUtils.isEntityOwner(IfmConstants.INTERFACE_SERVICE_BINDING_ENTITY,
                IfmConstants.INTERFACE_SERVICE_BINDING_ENTITY)) {
            return;
        }
        LOG.info("Service Binding Entry removed for Interface: {}, ServiceName: {}, ServicePriority {}",
                serviceKey.getInterfaceName(), boundServiceOld.getServiceName(), boundServiceOld.getServicePriority());
        LOG.trace("Service Binding Entry removed for Interface: {}, Data: {}", serviceKey.getInterfaceName(),
            boundServiceOld);
        FlowBasedServicesConfigRemovable flowBasedServicesConfigRemovable =
                flowBasedServicesRendererFactoryResolver.getFlowBasedServicesRendererFactory(
                        serviceKey.getServiceMode()).getFlowBasedServicesRemoveRenderer();
        RendererConfigRemoveWorker configWorker = new RendererConfigRemoveWorker(serviceKey.getInterfaceName(),
            serviceKey.getServiceMode(), flowBasedServicesConfigRemovable, boundServiceOld, boundServicesList);
        coordinator.enqueueJob(serviceKey.getInterfaceName(), configWorker, IfmConstants.JOB_MAX_RETRIES);
    }

    protected void update(ServicesInfoKey serviceKey, InstanceIdentifier<BoundServices> key,
                          BoundServices boundServiceOld, BoundServices boundServiceNew,
                          List<BoundServices> boundServicesList) {
        if (!Objects.equals(boundServiceOld, boundServiceNew)) {
            /*
             * In some cases ACL needs to change metadata passed from dispatcher tables to ACL tables dynamically.
             * For this update operation has been enhanced to support same. This is only supported for ACL for now
             * and the functionality will remain same for all other applications as it was earlier.
             */
            if (boundServiceNew.getServicePriority() != null && (
                boundServiceNew.getServicePriority() == NwConstants.ACL_SERVICE_INDEX
                    || boundServiceNew.getServicePriority() == NwConstants.EGRESS_ACL_SERVICE_INDEX)
                    && !Objects.equals(boundServiceOld, boundServiceNew)) {
                LOG.info("Bound services flow update for service {}", boundServiceNew.getServiceName());
                add(serviceKey, boundServiceNew, boundServicesList);
            } else {
                LOG.warn("Service Binding entry update not allowed for: {}, ServiceName: {}",
                        serviceKey.getInterfaceName(), boundServiceNew.getServiceName());
                LOG.trace("Service Binding Entry update not allowed for Interface: {}, Old Data: {}, New Data: {}",
                        serviceKey.getInterfaceName(), boundServiceNew, boundServiceOld);
            }
        }
    }

    protected void add(ServicesInfoKey serviceKey, BoundServices boundServicesNew,
                       List<BoundServices> boundServicesList) {
        if (!entityOwnershipUtils.isEntityOwner(IfmConstants.INTERFACE_SERVICE_BINDING_ENTITY,
                IfmConstants.INTERFACE_SERVICE_BINDING_ENTITY)) {
            return;
        }
        LOG.info("Service Binding Entry created for Interface: {}, ServiceName: {}, ServicePriority {}",
                serviceKey.getInterfaceName(), boundServicesNew.getServiceName(),
                boundServicesNew.getServicePriority());
        LOG.trace("Service Binding Entry created for Interface: {}, Data: {}", serviceKey.getInterfaceName(),
            boundServicesNew);
        FlowBasedServicesConfigAddable flowBasedServicesAddable = flowBasedServicesRendererFactoryResolver
            .getFlowBasedServicesRendererFactory(serviceKey.getServiceMode()).getFlowBasedServicesAddRenderer();
        RendererConfigAddWorker configWorker = new RendererConfigAddWorker(serviceKey.getInterfaceName(),
            serviceKey.getServiceMode(), flowBasedServicesAddable, boundServicesNew, boundServicesList);
        coordinator.enqueueJob(serviceKey.getInterfaceName(), configWorker, IfmConstants.JOB_MAX_RETRIES);
    }

    private class RendererConfigAddWorker implements Callable<List<ListenableFuture<Void>>> {
        private final String interfaceName;
        Class<? extends ServiceModeBase> serviceMode;
        FlowBasedServicesConfigAddable flowBasedServicesAddable;
        BoundServices boundServicesNew;
        List<BoundServices> boundServicesList;

        RendererConfigAddWorker(String interfaceName, Class<? extends ServiceModeBase> serviceMode,
                                FlowBasedServicesConfigAddable flowBasedServicesAddable,
                                BoundServices boundServicesNew, List<BoundServices> boundServicesList) {
            this.interfaceName = interfaceName;
            this.serviceMode = serviceMode;
            this.flowBasedServicesAddable = flowBasedServicesAddable;
            this.boundServicesNew = boundServicesNew;
            this.boundServicesList = boundServicesList;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            List<ListenableFuture<Void>> futures = new ArrayList<>();
            futures.add(txRunner.callWithNewReadWriteTransactionAndSubmit(OPERATIONAL, tx -> {
                BoundServicesState boundServicesState = FlowBasedServicesUtils
                        .getBoundServicesState(tx, interfaceName, serviceMode);
                // if service-binding state is not present, construct the same using ifstate
                if (boundServicesState == null) {
                    Interface ifState = interfaceManagerCommonUtils.getInterfaceState(interfaceName);
                    if (ifState == null) {
                        LOG.debug("Interface not operational, will bind service whenever interface comes up: {}",
                                interfaceName);
                        return;
                    }
                    boundServicesState = FlowBasedServicesUtils.buildBoundServicesState(ifState, serviceMode);
                    FlowBasedServicesUtils.addBoundServicesState(tx, interfaceName, boundServicesState);
                }
                flowBasedServicesAddable.bindService(futures, interfaceName, boundServicesNew, boundServicesList,
                        boundServicesState);
            }));
            return futures;
        }
    }

    private class RendererConfigRemoveWorker implements Callable<List<ListenableFuture<Void>>> {
        private final String interfaceName;
        Class<? extends ServiceModeBase> serviceMode;
        FlowBasedServicesConfigRemovable flowBasedServicesConfigRemovable;
        BoundServices boundServicesNew;
        @Nullable List<BoundServices> boundServicesList;

        RendererConfigRemoveWorker(String interfaceName, Class<? extends ServiceModeBase> serviceMode,
                                   FlowBasedServicesConfigRemovable flowBasedServicesConfigRemovable,
                                   BoundServices boundServicesNew, @Nullable List<BoundServices> boundServicesList) {
            this.interfaceName = interfaceName;
            this.serviceMode = serviceMode;
            this.flowBasedServicesConfigRemovable = flowBasedServicesConfigRemovable;
            this.boundServicesNew = boundServicesNew;
            this.boundServicesList = boundServicesList;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            List<ListenableFuture<Void>> futures = new ArrayList<>();
            futures.add(txRunner.callWithNewReadWriteTransactionAndSubmit(OPERATIONAL, tx -> {
                // if this is the last service getting unbound, remove service-state cache information
                BoundServicesState boundServiceState = FlowBasedServicesUtils.getBoundServicesState(
                        tx, interfaceName, serviceMode);
                if (boundServiceState == null) {
                    LOG.warn("bound-service-state is not present for interface:{}, service-mode:{}, "
                                    + "service-name:{}, service-priority:{}", interfaceName, serviceMode,
                            boundServicesNew.getServiceName(), boundServicesNew.getServicePriority());
                    return;
                }
                if (boundServicesList == null || boundServicesList.isEmpty()) {
                    FlowBasedServicesUtils.removeBoundServicesState(tx, interfaceName, serviceMode);
                }
                flowBasedServicesConfigRemovable.unbindService(futures, interfaceName, boundServicesNew,
                        boundServicesList, boundServiceState);
            }));
            return futures;
        }
    }
}
