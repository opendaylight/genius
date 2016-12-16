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
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncDataChangeListenerBase;
import org.opendaylight.genius.datastoreutils.AsyncDataTreeChangeListenerBase;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.renderer.ovs.statehelpers.OvsInterfaceTopologyStateAddHelper;
import org.opendaylight.genius.interfacemanager.renderer.ovs.statehelpers.OvsInterfaceTopologyStateRemoveHelper;
import org.opendaylight.genius.interfacemanager.renderer.ovs.statehelpers.OvsInterfaceTopologyStateUpdateHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
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
public class InterfaceTopologyStateListener extends AsyncDataTreeChangeListenerBase<OvsdbBridgeAugmentation, InterfaceTopologyStateListener> {
    private static final Logger LOG = LoggerFactory.getLogger(InterfaceTopologyStateListener.class);
    private final DataBroker dataBroker;

    @Inject
    public InterfaceTopologyStateListener(final DataBroker dataBroker) {
        super(OvsdbBridgeAugmentation.class, InterfaceTopologyStateListener.class);
        this.dataBroker = dataBroker;
    }

    @PostConstruct
    public void start() throws Exception {
        this.registerListener(LogicalDatastoreType.OPERATIONAL, this.dataBroker);
        LOG.info("InterfaceTopologyStateListener started");
    }

    @PreDestroy
    public void close() throws Exception {
        LOG.info("InterfaceTopologyStateListener closed");
    }

    @Override
    protected InstanceIdentifier<OvsdbBridgeAugmentation> getWildCardPath() {
        return InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class)
                .child(Node.class).augmentation(OvsdbBridgeAugmentation.class).build();
    }

    @Override
    protected InterfaceTopologyStateListener getDataTreeChangeListener() {
        return InterfaceTopologyStateListener.this;
    }

    @Override
    protected void remove(InstanceIdentifier<OvsdbBridgeAugmentation> identifier, OvsdbBridgeAugmentation bridgeOld) {
        LOG.debug("Received Remove DataChange Notification for identifier: {}, ovsdbBridgeAugmentation: {}",
                identifier, bridgeOld);
        DataStoreJobCoordinator jobCoordinator = DataStoreJobCoordinator.getInstance();
        RendererStateRemoveWorker rendererStateRemoveWorker = new RendererStateRemoveWorker(identifier, bridgeOld);
        jobCoordinator.enqueueJob(bridgeOld.getBridgeName().getValue(), rendererStateRemoveWorker, IfmConstants.JOB_MAX_RETRIES);
    }

    @Override
    protected void update(InstanceIdentifier<OvsdbBridgeAugmentation> identifier, OvsdbBridgeAugmentation bridgeOld,
                          OvsdbBridgeAugmentation bridgeNew) {
        LOG.debug("Received Update DataChange Notification for identifier: {}, ovsdbBridgeAugmentation old: {}, new: {}.",
                identifier, bridgeOld, bridgeNew);
        DatapathId oldDpid = bridgeOld.getDatapathId();
        DatapathId newDpid = bridgeNew.getDatapathId();
        if(oldDpid == null && newDpid != null){
            DataStoreJobCoordinator jobCoordinator = DataStoreJobCoordinator.getInstance();
            RendererStateAddWorker rendererStateAddWorker = new RendererStateAddWorker(identifier, bridgeNew);
            jobCoordinator.enqueueJob(bridgeNew.getBridgeName().getValue(), rendererStateAddWorker, IfmConstants.JOB_MAX_RETRIES);
        } else if(oldDpid != null && !oldDpid.equals(newDpid)){
            DataStoreJobCoordinator jobCoordinator = DataStoreJobCoordinator.getInstance();
            RendererStateUpdateWorker rendererStateAddWorker = new RendererStateUpdateWorker(identifier, bridgeNew, bridgeOld);
            jobCoordinator.enqueueJob(bridgeNew.getBridgeName().getValue(), rendererStateAddWorker, IfmConstants.JOB_MAX_RETRIES);
        }
    }

    @Override
    protected void add(InstanceIdentifier<OvsdbBridgeAugmentation> identifier, OvsdbBridgeAugmentation bridgeNew) {
        LOG.debug("Received Add DataChange Notification for identifier: {}, ovsdbBridgeAugmentation: {}",
                identifier, bridgeNew);
        DataStoreJobCoordinator jobCoordinator = DataStoreJobCoordinator.getInstance();
        RendererStateAddWorker rendererStateAddWorker = new RendererStateAddWorker(identifier, bridgeNew);
        jobCoordinator.enqueueJob(bridgeNew.getBridgeName().getValue(), rendererStateAddWorker, IfmConstants.JOB_MAX_RETRIES);
    }

    private class RendererStateAddWorker implements Callable<List<ListenableFuture<Void>>> {
        InstanceIdentifier<OvsdbBridgeAugmentation> instanceIdentifier;
        OvsdbBridgeAugmentation bridgeNew;


        public RendererStateAddWorker(InstanceIdentifier<OvsdbBridgeAugmentation> instanceIdentifier,
                                      OvsdbBridgeAugmentation bridgeNew) {
            this.instanceIdentifier = instanceIdentifier;
            this.bridgeNew = bridgeNew;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            // If another renderer(for eg : CSS) needs to be supported, check can be performed here
            // to call the respective helpers.
            return OvsInterfaceTopologyStateAddHelper.addPortToBridge(instanceIdentifier,
                    bridgeNew, dataBroker);
        }
    }

    private class RendererStateRemoveWorker implements Callable<List<ListenableFuture<Void>>> {
        InstanceIdentifier<OvsdbBridgeAugmentation> instanceIdentifier;
        OvsdbBridgeAugmentation bridgeNew;


        public RendererStateRemoveWorker(InstanceIdentifier<OvsdbBridgeAugmentation> instanceIdentifier,
                                         OvsdbBridgeAugmentation bridgeNew) {
            this.instanceIdentifier = instanceIdentifier;
            this.bridgeNew = bridgeNew;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            // If another renderer(for eg : CSS) needs to be supported, check can be performed here
            // to call the respective helpers.
            return OvsInterfaceTopologyStateRemoveHelper.removePortFromBridge(instanceIdentifier,
                    bridgeNew, dataBroker);
        }
    }
    private class RendererStateUpdateWorker implements Callable<List<ListenableFuture<Void>>> {
        InstanceIdentifier<OvsdbBridgeAugmentation> instanceIdentifier;
        OvsdbBridgeAugmentation bridgeNew;
        OvsdbBridgeAugmentation bridgeOld;


        public RendererStateUpdateWorker(InstanceIdentifier<OvsdbBridgeAugmentation> instanceIdentifier,
                                         OvsdbBridgeAugmentation bridgeNew, OvsdbBridgeAugmentation bridgeOld) {
            this.instanceIdentifier = instanceIdentifier;
            this.bridgeNew = bridgeNew;
            this.bridgeOld = bridgeOld;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            // If another renderer(for eg : CSS) needs to be supported, check can be performed here
            // to call the respective helpers.
            return OvsInterfaceTopologyStateUpdateHelper.updateBridgeRefEntry(instanceIdentifier,
                    bridgeNew, bridgeOld, dataBroker);
        }
    }
}