/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.listeners;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncDataTreeChangeListenerBase;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.renderer.hwvtep.confighelpers.HwVTEPConfigRemoveHelper;
import org.opendaylight.genius.interfacemanager.renderer.hwvtep.confighelpers.HwVTEPInterfaceConfigAddHelper;
import org.opendaylight.genius.interfacemanager.renderer.hwvtep.confighelpers.HwVTEPInterfaceConfigUpdateHelper;
import org.opendaylight.genius.interfacemanager.renderer.hwvtep.utilities.SouthboundUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.interfaces._interface.NodeIdentifier;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
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
public class HwVTEPConfigListener extends AsyncDataTreeChangeListenerBase<Interface, HwVTEPConfigListener> {
    private static final Logger LOG = LoggerFactory.getLogger(HwVTEPConfigListener.class);
    private final DataBroker dataBroker;

    @Inject
    public HwVTEPConfigListener(final DataBroker dataBroker) {
        super(Interface.class, HwVTEPConfigListener.class);
        this.dataBroker = dataBroker;
    }

    @PostConstruct
    public void start() throws Exception {
        this.registerListener(LogicalDatastoreType.CONFIGURATION, this.dataBroker);
        LOG.info("HwVTEPConfigListener started");
    }

    @PreDestroy
    public void close() throws Exception {
        LOG.info("HwVTEPConfigListener closed");
    }

    @Override
    protected InstanceIdentifier<Interface> getWildCardPath() {
        return InstanceIdentifier.create(Interfaces.class).child(Interface.class);
    }

    @Override
    protected void remove(InstanceIdentifier<Interface> key, Interface interfaceOld) {
        // HwVTEPs support only vxlan
        IfTunnel ifTunnel = interfaceOld.getAugmentation(IfTunnel.class);
        if (ifTunnel != null && ifTunnel.getTunnelInterfaceType().isAssignableFrom(TunnelTypeVxlan.class)) {
            ParentRefs parentRefs = interfaceOld.getAugmentation(ParentRefs.class);
            if (parentRefs != null && parentRefs.getNodeIdentifier() != null) {
                for(NodeIdentifier nodeIdentifier : parentRefs.getNodeIdentifier()) {
                    if(SouthboundUtils.HWVTEP_TOPOLOGY.equals(nodeIdentifier.getTopologyId())) {
                        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
                        RendererConfigRemoveWorker configWorker = new RendererConfigRemoveWorker(key, interfaceOld,
                                SouthboundUtils.createPhysicalSwitchInstanceIdentifier(nodeIdentifier.getNodeId()),
                                SouthboundUtils.createGlobalNodeInstanceIdentifier(dataBroker, nodeIdentifier.getNodeId()));
                        coordinator.enqueueJob(interfaceOld.getName(), configWorker, IfmConstants.JOB_MAX_RETRIES);
                    }
                }
            }
        }
    }

    @Override
    protected void update(InstanceIdentifier<Interface> key, Interface interfaceOld, Interface interfaceNew) {
        // HwVTEPs support only vxlan
        IfTunnel ifTunnel = interfaceNew.getAugmentation(IfTunnel.class);
        if (ifTunnel != null && ifTunnel.getTunnelInterfaceType().isAssignableFrom(TunnelTypeVxlan.class)) {
            ParentRefs parentRefs = interfaceNew.getAugmentation(ParentRefs.class);
            if (parentRefs != null && parentRefs.getNodeIdentifier() != null) {
                for(NodeIdentifier nodeIdentifier : parentRefs.getNodeIdentifier()) {
                    if(SouthboundUtils.HWVTEP_TOPOLOGY.equals(nodeIdentifier.getTopologyId())) {
                        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
                        RendererConfigUpdateWorker configWorker = new RendererConfigUpdateWorker(key, interfaceNew,
                                SouthboundUtils.createPhysicalSwitchInstanceIdentifier(nodeIdentifier.getNodeId()),
                                SouthboundUtils.createGlobalNodeInstanceIdentifier(dataBroker, nodeIdentifier.getNodeId()), ifTunnel);
                        coordinator.enqueueJob(interfaceNew.getName(), configWorker, IfmConstants.JOB_MAX_RETRIES);
                    }
                }
            }
        }
    }

    @Override
    protected void add(InstanceIdentifier<Interface> key, Interface interfaceNew) {
        // HwVTEPs support only vxlan
        IfTunnel ifTunnel = interfaceNew.getAugmentation(IfTunnel.class);
        if (ifTunnel != null && ifTunnel.getTunnelInterfaceType().isAssignableFrom(TunnelTypeVxlan.class)) {
            ParentRefs parentRefs = interfaceNew.getAugmentation(ParentRefs.class);
            if (parentRefs != null && parentRefs.getNodeIdentifier() != null) {
                for(NodeIdentifier nodeIdentifier : parentRefs.getNodeIdentifier()) {
                    if(SouthboundUtils.HWVTEP_TOPOLOGY.equals(nodeIdentifier.getTopologyId())) {
                        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
                        RendererConfigAddWorker configWorker = new RendererConfigAddWorker(key, interfaceNew,
                                SouthboundUtils.createPhysicalSwitchInstanceIdentifier(nodeIdentifier.getNodeId()),
                                SouthboundUtils.createGlobalNodeInstanceIdentifier(dataBroker, nodeIdentifier.getNodeId()), ifTunnel);
                        coordinator.enqueueJob(interfaceNew.getName(), configWorker, IfmConstants.JOB_MAX_RETRIES);
                    }
                }
            }
        }
    }

    @Override
    protected HwVTEPConfigListener getDataTreeChangeListener() {
        return HwVTEPConfigListener.this;
    }

    private class RendererConfigAddWorker implements Callable<List<ListenableFuture<Void>>> {
        InstanceIdentifier<Interface> key;
        Interface interfaceNew;
        InstanceIdentifier<Node> physicalSwitchNodeId;
        InstanceIdentifier<Node> globalNodeId;
        IfTunnel ifTunnel;

        public RendererConfigAddWorker(InstanceIdentifier<Interface> key, Interface interfaceNew,
                                       InstanceIdentifier<Node> physicalSwitchNodeId, InstanceIdentifier<Node> globalNodeId, IfTunnel ifTunnel) {
            this.key = key;
            this.interfaceNew = interfaceNew;
            this.physicalSwitchNodeId = physicalSwitchNodeId;
            this.globalNodeId = globalNodeId;
            this.ifTunnel = ifTunnel;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            return HwVTEPInterfaceConfigAddHelper.addConfiguration(dataBroker,
                    physicalSwitchNodeId, globalNodeId, interfaceNew, ifTunnel);
        }
    }

    private class RendererConfigUpdateWorker implements Callable<List<ListenableFuture<Void>>> {
        InstanceIdentifier<Interface> key;
        Interface interfaceNew;
        InstanceIdentifier<Node> globalNodeId;
        InstanceIdentifier<Node> physicalSwitchNodeId;
        IfTunnel ifTunnel;

        public RendererConfigUpdateWorker(InstanceIdentifier<Interface> key, Interface interfaceNew,
                                          InstanceIdentifier<Node> physicalSwitchNodeId, InstanceIdentifier<Node> globalNodeId, IfTunnel ifTunnel) {
            this.key = key;
            this.interfaceNew = interfaceNew;
            this.physicalSwitchNodeId = physicalSwitchNodeId;
            this.ifTunnel = ifTunnel;
            this.globalNodeId = globalNodeId;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            return HwVTEPInterfaceConfigUpdateHelper.updateConfiguration(dataBroker,
                    physicalSwitchNodeId, globalNodeId, interfaceNew, ifTunnel);
        }
    }

    private class RendererConfigRemoveWorker implements Callable<List<ListenableFuture<Void>>> {
        InstanceIdentifier<Interface> key;
        Interface interfaceOld;
        InstanceIdentifier<Node> physicalSwitchNodeId;
        InstanceIdentifier<Node> globalNodeId;

        public RendererConfigRemoveWorker(InstanceIdentifier<Interface> key, Interface interfaceOld,
                                          InstanceIdentifier<Node> physicalSwitchNodeId, InstanceIdentifier<Node> globalNodeId) {
            this.key = key;
            this.interfaceOld = interfaceOld;
            this.physicalSwitchNodeId = physicalSwitchNodeId;
            this.globalNodeId = globalNodeId;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            return HwVTEPConfigRemoveHelper.removeConfiguration(dataBroker,
                    interfaceOld, globalNodeId, physicalSwitchNodeId);
        }
    }
}