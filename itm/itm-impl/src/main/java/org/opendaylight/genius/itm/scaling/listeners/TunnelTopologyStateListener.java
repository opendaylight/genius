/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.scaling.listeners;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.concurrent.Callable;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncDataTreeChangeListenerBase;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.cache.DPNTEPsInfoCache;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.TunnelMonitoringConfig;
import org.opendaylight.genius.itm.scaling.renderer.ovs.statehelpers.OvsTunnelTopologyStateAddHelper;
import org.opendaylight.genius.itm.scaling.renderer.ovs.statehelpers.OvsTunnelTopologyStateRemoveHelper;
import org.opendaylight.genius.itm.scaling.renderer.ovs.statehelpers.OvsTunnelTopologyStateUpdateHelper;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class TunnelTopologyStateListener
        extends AsyncDataTreeChangeListenerBase<OvsdbBridgeAugmentation, TunnelTopologyStateListener> {

    private static final Logger LOG = LoggerFactory.getLogger(TunnelTopologyStateListener.class);

    private final DataBroker dataBroker;
    private final JobCoordinator coordinator;
    private final OvsTunnelTopologyStateUpdateHelper ovsTunnelTopologyStateUpdateHelper;
    private final DPNTEPsInfoCache dpntePsInfoCache;
    private final TunnelMonitoringConfig tunnelMonitoringConfig;

    @Inject
    public TunnelTopologyStateListener(final DataBroker dataBroker, final IInterfaceManager interfaceManager,
                                       final JobCoordinator coordinator,
                                       final OvsTunnelTopologyStateUpdateHelper ovsTunnelTopologyStateUpdateHelper,
                                       final DPNTEPsInfoCache dpntePsInfoCache,
                                       final TunnelMonitoringConfig tunnelMonitoringConfig)  {
        super(OvsdbBridgeAugmentation.class, TunnelTopologyStateListener.class);
        this.dataBroker = dataBroker;
        this.coordinator = coordinator;
        this.ovsTunnelTopologyStateUpdateHelper = ovsTunnelTopologyStateUpdateHelper;
        this.dpntePsInfoCache = dpntePsInfoCache;
        this.tunnelMonitoringConfig = tunnelMonitoringConfig;
        if (interfaceManager.isItmDirectTunnelsEnabled()) {
            LOG.debug("ITM Direct Tunnels is Enabled, hence registering this listener");
            this.registerListener(LogicalDatastoreType.OPERATIONAL, this.dataBroker);
        } else {
            LOG.debug("ITM Direct Tunnels is not Enabled, therefore not registering this listener");
        }
    }

    @Override
    protected InstanceIdentifier<OvsdbBridgeAugmentation> getWildCardPath() {
        return InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class)
                .child(Node.class).augmentation(OvsdbBridgeAugmentation.class).build();
    }

    @Override
    protected TunnelTopologyStateListener getDataTreeChangeListener() {
        return TunnelTopologyStateListener.this;
    }

    @Override
    protected void remove(InstanceIdentifier<OvsdbBridgeAugmentation> identifier, OvsdbBridgeAugmentation bridgeOld) {
        LOG.debug("Received Remove DataChange Notification for identifier: {}, ovsdbBridgeAugmentation: {}",
                identifier, bridgeOld);
        RendererStateRemoveWorker rendererStateRemoveWorker = new RendererStateRemoveWorker(identifier, bridgeOld);
        coordinator.enqueueJob(bridgeOld.getBridgeName().getValue(), rendererStateRemoveWorker,
                ITMConstants.JOB_MAX_RETRIES);
    }

    @Override
    protected void update(InstanceIdentifier<OvsdbBridgeAugmentation> identifier, OvsdbBridgeAugmentation bridgeOld,
                          OvsdbBridgeAugmentation bridgeNew) {
        LOG.debug("Received Update DataChange Notification for identifier: {}, + ovsdbBridgeAugmentation old: {},"
                + " new: {}.", identifier, bridgeOld, bridgeNew);
        DatapathId oldDpid = bridgeOld.getDatapathId();
        DatapathId newDpid = bridgeNew.getDatapathId();
        if (oldDpid == null && newDpid != null) {
            RendererStateAddWorker rendererStateAddWorker = new RendererStateAddWorker(identifier, bridgeNew);
            coordinator.enqueueJob(bridgeNew.getBridgeName().getValue(), rendererStateAddWorker,
                    ITMConstants.JOB_MAX_RETRIES);
        } else if (oldDpid != null && !oldDpid.equals(newDpid)) {
            RendererStateUpdateWorker rendererStateAddWorker = new RendererStateUpdateWorker(identifier, bridgeNew,
                    bridgeOld);
            coordinator.enqueueJob(bridgeNew.getBridgeName().getValue(), rendererStateAddWorker,
                    ITMConstants.JOB_MAX_RETRIES);
        }
    }

    @Override
    protected void add(InstanceIdentifier<OvsdbBridgeAugmentation> identifier, OvsdbBridgeAugmentation bridgeNew) {
        LOG.debug("Received Add DataChange Notification for identifier: {}, ovsdbBridgeAugmentation: {}",
                identifier, bridgeNew);
        RendererStateAddWorker rendererStateAddWorker = new RendererStateAddWorker(identifier, bridgeNew);
        coordinator.enqueueJob(bridgeNew.getBridgeName().getValue(), rendererStateAddWorker,
                ITMConstants.JOB_MAX_RETRIES);
    }

    private class RendererStateAddWorker implements Callable<List<ListenableFuture<Void>>> {
        InstanceIdentifier<OvsdbBridgeAugmentation> instanceIdentifier;
        OvsdbBridgeAugmentation bridgeNew;


        RendererStateAddWorker(InstanceIdentifier<OvsdbBridgeAugmentation> instanceIdentifier,
                                      OvsdbBridgeAugmentation bridgeNew) {
            this.instanceIdentifier = instanceIdentifier;
            this.bridgeNew = bridgeNew;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            // If another renderer(for eg : CSS) needs to be supported, check can be performed here
            // to call the respective helpers.
            return OvsTunnelTopologyStateAddHelper.addPortToBridge(instanceIdentifier,
                    bridgeNew, dataBroker, tunnelMonitoringConfig, dpntePsInfoCache);
        }
    }

    private class RendererStateRemoveWorker implements Callable<List<ListenableFuture<Void>>> {
        InstanceIdentifier<OvsdbBridgeAugmentation> instanceIdentifier;
        OvsdbBridgeAugmentation bridgeNew;


        RendererStateRemoveWorker(InstanceIdentifier<OvsdbBridgeAugmentation> instanceIdentifier,
                                  OvsdbBridgeAugmentation bridgeNew) {
            this.instanceIdentifier = instanceIdentifier;
            this.bridgeNew = bridgeNew;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            // If another renderer(for eg : CSS) needs to be supported, check can be performed here
            // to call the respective helpers.
            return OvsTunnelTopologyStateRemoveHelper.removePortFromBridge(instanceIdentifier,
                    bridgeNew, dataBroker);
        }
    }

    private class RendererStateUpdateWorker implements Callable<List<ListenableFuture<Void>>> {
        InstanceIdentifier<OvsdbBridgeAugmentation> instanceIdentifier;
        OvsdbBridgeAugmentation bridgeNew;
        OvsdbBridgeAugmentation bridgeOld;


        RendererStateUpdateWorker(InstanceIdentifier<OvsdbBridgeAugmentation> instanceIdentifier,
                                  OvsdbBridgeAugmentation bridgeNew, OvsdbBridgeAugmentation bridgeOld) {
            this.instanceIdentifier = instanceIdentifier;
            this.bridgeNew = bridgeNew;
            this.bridgeOld = bridgeOld;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            // If another renderer(for eg : CSS) needs to be supported, check can be performed here
            // to call the respective helpers.
            return ovsTunnelTopologyStateUpdateHelper.updateOvsBridgeRefEntry(instanceIdentifier, bridgeNew,
                    bridgeOld);
        }
    }
}