/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.listeners;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import javax.annotation.Nonnull;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServicesKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class FlowBasedServicesConfigListener implements ClusteredDataTreeChangeListener<ServicesInfo>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(FlowBasedServicesConfigListener.class);
    private ListenerRegistration<FlowBasedServicesConfigListener> listenerRegistration;

    @Inject
    public FlowBasedServicesConfigListener(final DataBroker dataBroker,
                                           final InterfacemgrProvider interfacemgrProvider) {
        initializeFlowBasedServiceHelpers(interfacemgrProvider);
        registerListener(LogicalDatastoreType.CONFIGURATION, dataBroker);
    }

    protected InstanceIdentifier<ServicesInfo> getWildCardPath() {
        return InstanceIdentifier.create(ServiceBindings.class).child(ServicesInfo.class);
    }

    public void registerListener(LogicalDatastoreType dsType, final DataBroker db) {
        final DataTreeIdentifier<ServicesInfo> treeId = new DataTreeIdentifier<>(dsType, getWildCardPath());
        listenerRegistration = db.registerDataTreeChangeListener(treeId, FlowBasedServicesConfigListener.this);
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
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<ServicesInfo>> collection) {
        for (final DataTreeModification<ServicesInfo> dataTreeModification : collection) {
            final InstanceIdentifier<ServicesInfo> rootIdentifier =
                dataTreeModification.getRootPath().getRootIdentifier();
            final DataObjectModification<ServicesInfo> rootNode = dataTreeModification.getRootNode();
            LOG.trace("Services Info configuration has changed: {}", rootNode);
            for (final DataObjectModification<? extends DataObject> dataObjectModification
                : rootNode.getModifiedChildren()) {
                if (dataObjectModification.getDataType().equals(BoundServices.class)) {
                    LOG.debug("modified children" + dataObjectModification);
                    onBoundServicesChanged((DataObjectModification<BoundServices>) dataObjectModification,
                        rootIdentifier, rootNode);
                }
            }
        }
    }

    private InstanceIdentifier<BoundServices> getBoundServicesInstanceIdentifier(
        InstanceIdentifier<ServicesInfo> rootIdentifier, BoundServicesKey boundServicesKey) {
        return rootIdentifier.child(BoundServices.class , boundServicesKey);
    }

    private synchronized void onBoundServicesChanged(final DataObjectModification<BoundServices> dataObjectModification,
                                                     final InstanceIdentifier<ServicesInfo> rootIdentifier,
                                                     DataObjectModification<ServicesInfo> rootNode) {
        List<BoundServices> boundServices = rootNode.getDataAfter().getBoundServices();
        ServicesInfoKey servicesInfoKey = rootNode.getDataAfter().getKey();
        BoundServices boundServicesBefore = dataObjectModification.getDataBefore();
        BoundServices boundServicesAfter =  dataObjectModification.getDataAfter();

        switch (dataObjectModification.getModificationType()) {
            case DELETE:
                remove(servicesInfoKey, getBoundServicesInstanceIdentifier(rootIdentifier,
                    boundServicesBefore.getKey()), boundServicesBefore, boundServices);
                break;
            case SUBTREE_MODIFIED:
                update(getBoundServicesInstanceIdentifier(rootIdentifier, boundServicesBefore.getKey()),
                    boundServicesBefore, boundServicesAfter);
                break;
            case WRITE:
                if (boundServicesBefore == null) {
                    add(servicesInfoKey, getBoundServicesInstanceIdentifier(rootIdentifier,
                        boundServicesAfter.getKey()), boundServicesAfter, boundServices);
                }
                break;
            default:
                LOG.error("Unhandled Modificiation Type{} for {}", dataObjectModification.getModificationType(),
                    rootIdentifier);

        }
    }

    protected void remove(ServicesInfoKey serviceKey, InstanceIdentifier<BoundServices> key, BoundServices
        boundServiceOld, List<BoundServices> boundServicesList) {
        IfmClusterUtils.runOnlyInLeaderNode(() -> {
            LOG.info("Service Binding Entry removed for Interface: {}, Data: {}", serviceKey.getInterfaceName(),
                boundServiceOld);
            FlowBasedServicesConfigRemovable flowBasedServicesConfigRemovable = FlowBasedServicesRendererFactory
                .getFlowBasedServicesRendererFactory(serviceKey.getServiceMode())
                .getFlowBasedServicesRemoveRenderer();
            DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
            RendererConfigRemoveWorker configWorker = new RendererConfigRemoveWorker(serviceKey
                .getInterfaceName(), flowBasedServicesConfigRemovable, boundServiceOld, boundServicesList);
            coordinator.enqueueJob(serviceKey.getInterfaceName(), configWorker, IfmConstants.JOB_MAX_RETRIES);
        }, IfmClusterUtils.INTERFACE_SERVICE_BINDING_ENTITY);
    }

    protected void update(InstanceIdentifier<BoundServices> key, BoundServices boundServiceOld,
                          BoundServices boundServiceNew) {
        if (!Objects.equals(boundServiceOld, boundServiceNew)) {
            LOG.error("Service Binding entry update not allowed for: {}, Data: {}",
                InstanceIdentifier.keyOf(key.firstIdentifierOf(ServicesInfo.class)).getInterfaceName(),
                boundServiceNew);
        }
    }

    protected void add(ServicesInfoKey serviceKey, InstanceIdentifier<BoundServices> key, BoundServices
        boundServicesNew, List<BoundServices> boundServicesList) {
        IfmClusterUtils.runOnlyInLeaderNode(() -> {
            LOG.info("Service Binding Entry created for Interface: {}, Data: {}", serviceKey.getInterfaceName(),
                boundServicesNew);
            FlowBasedServicesConfigAddable flowBasedServicesAddable = FlowBasedServicesRendererFactory
                .getFlowBasedServicesRendererFactory(serviceKey.getServiceMode()).getFlowBasedServicesAddRenderer();
            DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
            RendererConfigAddWorker configWorker = new RendererConfigAddWorker(serviceKey.getInterfaceName(),
                flowBasedServicesAddable, boundServicesNew, boundServicesList);
            coordinator.enqueueJob(serviceKey.getInterfaceName(), configWorker, IfmConstants.JOB_MAX_RETRIES);
        }, IfmClusterUtils.INTERFACE_SERVICE_BINDING_ENTITY);
    }

    private class RendererConfigAddWorker implements Callable<List<ListenableFuture<Void>>> {
        String interfaceName;
        FlowBasedServicesConfigAddable flowBasedServicesAddable;
        BoundServices boundServicesNew;
        List<BoundServices> boundServicesList;

        RendererConfigAddWorker(String interfaceName, FlowBasedServicesConfigAddable flowBasedServicesAddable,
                                BoundServices boundServicesNew, List<BoundServices> boundServicesList) {
            this.interfaceName = interfaceName;
            this.flowBasedServicesAddable = flowBasedServicesAddable;
            this.boundServicesNew = boundServicesNew;
            this.boundServicesList = boundServicesList;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            return flowBasedServicesAddable.bindService(interfaceName, boundServicesNew,
                boundServicesList);
        }
    }

    private class RendererConfigRemoveWorker implements Callable<List<ListenableFuture<Void>>> {
        String interfaceName;
        FlowBasedServicesConfigRemovable flowBasedServicesConfigRemovable;
        BoundServices boundServicesNew;
        List<BoundServices> boundServicesList;

        RendererConfigRemoveWorker(String interfaceName,
                                   FlowBasedServicesConfigRemovable flowBasedServicesConfigRemovable,
                                   BoundServices boundServicesNew, List<BoundServices> boundServicesList) {
            this.interfaceName = interfaceName;
            this.flowBasedServicesConfigRemovable = flowBasedServicesConfigRemovable;
            this.boundServicesNew = boundServicesNew;
            this.boundServicesList = boundServicesList;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            return flowBasedServicesConfigRemovable.unbindService(interfaceName, boundServicesNew,
                boundServicesList);
        }
    }
}
