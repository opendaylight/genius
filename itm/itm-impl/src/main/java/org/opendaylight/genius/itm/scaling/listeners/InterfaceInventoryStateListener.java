/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.scaling.listeners;

import com.google.common.util.concurrent.ListenableFuture;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncClusteredDataTreeChangeListenerBase;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.scaling.renderer.ovs.statehelpers.OvsInterfaceStateUpdateHelper;
import org.opendaylight.genius.itm.scaling.renderer.ovs.statehelpers.OvsInterfaceStateAddHelper;
import org.opendaylight.genius.itm.scaling.renderer.ovs.statehelpers.OvsInterfaceStateRemoveHelper;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.ItmClusterUtils;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.ItmScaleUtils;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.NodeConnectorInfo;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.TunnelUtils;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortReason;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Callable;

/**
 *
 * This Class is a Data Change Listener for FlowCapableNodeConnector updates.
 * This creates an entry in the tunnels-state OperDS for every node-connector used.
 *
 */

@Singleton
public class InterfaceInventoryStateListener extends AsyncClusteredDataTreeChangeListenerBase<FlowCapableNodeConnector,
        InterfaceInventoryStateListener> {
    private static final Logger LOG = LoggerFactory.getLogger(InterfaceInventoryStateListener.class);

    private final DataBroker dataBroker;
    private final IdManagerService idManager;
    private final IMdsalApiManager mdsalApiManager;
    private final IInterfaceManager iInterfaceManager;
    private final JobCoordinator coordinator;

    @Inject
    public InterfaceInventoryStateListener(final DataBroker dataBroker, final IdManagerService idManagerService,
                                           final IMdsalApiManager iMdsalApiManager,
                                           final IInterfaceManager iInterfaceManager,
                                           final JobCoordinator coordinator) {
        super(FlowCapableNodeConnector.class, InterfaceInventoryStateListener.class);
        this.dataBroker = dataBroker;
        this.idManager = idManagerService;
        this.mdsalApiManager = iMdsalApiManager;
        this.iInterfaceManager = iInterfaceManager;
        this.coordinator = coordinator;
        if (iInterfaceManager.isItmDirectTunnelsEnabled()) {
            LOG.debug( "ITM Direct Tunnels is Enabled, hence registering this listener");
            this.registerListener(LogicalDatastoreType.OPERATIONAL, dataBroker);
        } else {
            LOG.debug( "ITM Direct Tunnels is not Enabled, therefore not registering this listener");
        }
    }

    @Override
    protected InstanceIdentifier<FlowCapableNodeConnector> getWildCardPath() {
        return InstanceIdentifier.create(Nodes.class).child(Node.class).child(NodeConnector.class)
                .augmentation(FlowCapableNodeConnector.class);
    }

    @Override
    protected InterfaceInventoryStateListener getDataTreeChangeListener() {
        return InterfaceInventoryStateListener.this;
    }

    @Override
    protected void remove(InstanceIdentifier<FlowCapableNodeConnector> key,
                          FlowCapableNodeConnector flowCapableNodeConnectorOld) {
        String portName = flowCapableNodeConnectorOld.getName();
        LOG.debug( "InterfaceInventoryState Remove for {}", portName);
        // ITM Direct Tunnels Return if its not tunnel port and if its not Internal
        if (!TunnelUtils.tunnelPortPredicate.test(portName) ) {
            LOG.debug( "Node Connector Remove - {} Interface is not a tunnel I/f, so no-op", portName);
            return;
        } else if (!ItmScaleUtils.isInternalBasedOnState(portName,dataBroker)) {
            LOG.debug( "Node Connector Remove {} Interface is not a internal tunnel I/f, so no-op", portName);
            return;
        }
        if(!ItmClusterUtils.isEntityOwner(ITMConstants.ITM_CONFIG_ENTITY)) {
            return;
        }
        LOG.debug("Received NodeConnector Remove Event: {}, {}", key, flowCapableNodeConnectorOld);
        NodeConnectorId nodeConnectorId = InstanceIdentifier.keyOf(key.firstIdentifierOf(NodeConnector.class)).getId();
        remove(nodeConnectorId, null, flowCapableNodeConnectorOld, portName, true);
    }

    @Override
    protected void update(InstanceIdentifier<FlowCapableNodeConnector> key, FlowCapableNodeConnector fcNodeConnectorOld,
                          FlowCapableNodeConnector fcNodeConnectorNew) {
        String portName = fcNodeConnectorNew.getName();
        if (!TunnelUtils.tunnelPortPredicate.test(portName) ) {
            LOG.debug( "Node Connector Update - {} Interface is not a tunnel I/f, so no-op", portName);
            return;
        } else if (!ItmScaleUtils.isInternal(portName)) {
            LOG.debug( "Node Connector Update {} Interface is not a internal tunnel I/f, so no-op", portName);
            return;
        }
        if (fcNodeConnectorNew.getReason() == PortReason.Delete
            || !ItmClusterUtils.isEntityOwner(ITMConstants.ITM_CONFIG_ENTITY)) {
            return;
        }
        LOG.debug("Received NodeConnector Update Event: {}, {}, {}", key, fcNodeConnectorOld, fcNodeConnectorNew);

        InterfaceStateUpdateWorker portStateUpdateWorker = new InterfaceStateUpdateWorker(key, fcNodeConnectorOld,
                fcNodeConnectorNew, portName);
        coordinator.enqueueJob(portName, portStateUpdateWorker, ITMConstants.JOB_MAX_RETRIES);
    }

    @Override
    protected void add(InstanceIdentifier<FlowCapableNodeConnector> key, FlowCapableNodeConnector fcNodeConnectorNew) {
        String portName = fcNodeConnectorNew.getName();
        LOG.debug( "InterfaceInventoryState ADD for {}", portName);
        // SF419 Return if its not tunnel port and if its not Internal
        if (!TunnelUtils.tunnelPortPredicate.test(portName) ) {
            LOG.debug( "Node Connector Add {} Interface is not a tunnel I/f, so no-op", portName);
            return;
        }
        if(!ItmClusterUtils.isEntityOwner(ITMConstants.ITM_CONFIG_ENTITY)) {
            return;
        }
        if(!ItmScaleUtils.isConfigAvailable(portName)) {
            // Park the notification
            LOG.debug(" Unable to process the NodeConnector ADD event for {} as Config not available." +
                    "Hence parking it", portName);
            NodeConnectorInfo nodeConnectorInfo = new NodeConnectorInfo(key, fcNodeConnectorNew);
            ItmScaleUtils.addNodeConnectorInfoToCache(portName, nodeConnectorInfo);
            return;
        } else if (!ItmScaleUtils.isInternal(portName)) {
                LOG.debug("{} Interface is not a internal tunnel I/f, so no-op", portName);
                return;
        }

        LOG.debug("Received NodeConnector Add Event: {}, {}", key, fcNodeConnectorNew);
        if (TunnelUtils.tunnelPortPredicate.test(portName) && ItmScaleUtils.isInternal(portName)) {
            //NodeConnectorId nodeConnectorId = InstanceIdentifier.keyOf(key.firstIdentifierOf(NodeConnector.class)).getId();


            InterfaceStateAddWorker ifStateAddWorker = new InterfaceStateAddWorker(idManager, key,
                    fcNodeConnectorNew, portName);
            coordinator.enqueueJob(portName, ifStateAddWorker, ITMConstants.JOB_MAX_RETRIES);
        }
    }

    private void remove(NodeConnectorId nodeConnectorIdNew, NodeConnectorId nodeConnectorIdOld,
                        FlowCapableNodeConnector fcNodeConnectorNew, String portName, boolean isNetworkEvent) {
        LOG.debug( "InterfaceInventoryState REMOVE for {}", portName);
        InterfaceStateRemoveWorker portStateRemoveWorker = new InterfaceStateRemoveWorker(idManager, nodeConnectorIdNew,
                nodeConnectorIdOld, fcNodeConnectorNew, portName, portName, isNetworkEvent, true);
        coordinator.enqueueJob(portName, portStateRemoveWorker, ITMConstants.JOB_MAX_RETRIES);
    }

    // ITM Direct Tunnels Maybe this is not required -- CHECK
    private String getDpnPrefixedPortName(NodeConnectorId nodeConnectorId, String portName) {
        portName = (ItmScaleUtils.getDpnFromNodeConnectorId(nodeConnectorId)).toString() +
                ITMConstants.OF_URI_SEPARATOR +
                portName;
        return portName;
    }
    private class InterfaceStateAddWorker implements Callable {
        private final InstanceIdentifier<FlowCapableNodeConnector> key;
        private final FlowCapableNodeConnector fcNodeConnectorNew;
        private final String interfaceName;
        private final IdManagerService idManager;

        public InterfaceStateAddWorker(IdManagerService idManager, InstanceIdentifier<FlowCapableNodeConnector> key,
                                       FlowCapableNodeConnector fcNodeConnectorNew,
                                       String portName) {
            this.key = key;
            this.fcNodeConnectorNew = fcNodeConnectorNew;
            this.interfaceName = portName;
            this.idManager = idManager;
        }

        @Override
        public Object call() throws Exception {
            // If another renderer(for eg : CSS) needs to be supported, check can be performed here
            // to call the respective helpers.
            List<ListenableFuture<Void>> futures = OvsInterfaceStateAddHelper.addState(dataBroker, idManager, mdsalApiManager, key,
                    interfaceName, fcNodeConnectorNew);
            return futures;
        }

        @Override
        public String toString() {
            return "InterfaceStateAddWorker{" +
                    "fcNodeConnectorIdentifier=" + key +
                    ", fcNodeConnectorNew=" + fcNodeConnectorNew +
                    ", interfaceName='" + interfaceName + '\'' +
                    '}';
        }
    }

    private class InterfaceStateUpdateWorker implements Callable {
        private InstanceIdentifier<FlowCapableNodeConnector> key;
        private final FlowCapableNodeConnector fcNodeConnectorOld;
        private final FlowCapableNodeConnector fcNodeConnectorNew;
        private String interfaceName;


        public InterfaceStateUpdateWorker(InstanceIdentifier<FlowCapableNodeConnector> key,
                                          FlowCapableNodeConnector fcNodeConnectorOld,
                                          FlowCapableNodeConnector fcNodeConnectorNew,
                                          String portName) {
            this.key = key;
            this.fcNodeConnectorOld = fcNodeConnectorOld;
            this.fcNodeConnectorNew = fcNodeConnectorNew;
            this.interfaceName = portName;
        }

        @Override
        public Object call() throws Exception {
            // If another renderer(for eg : CSS) needs to be supported, check can be performed here
            // to call the respective helpers.
            List<ListenableFuture<Void>> futures = OvsInterfaceStateUpdateHelper.updateState(key, dataBroker, interfaceName,
                    fcNodeConnectorNew, fcNodeConnectorOld);
            return futures;
        }

        @Override
        public String toString() {
            return "InterfaceStateUpdateWorker{" +
                    "key=" + key +
                    ", fcNodeConnectorOld=" + fcNodeConnectorOld +
                    ", fcNodeConnectorNew=" + fcNodeConnectorNew +
                    ", interfaceName='" + interfaceName + '\'' +
                    '}';
        }
    }

    private class InterfaceStateRemoveWorker implements Callable {
        private final NodeConnectorId nodeConnectorIdNew;
        private NodeConnectorId nodeConnectorIdOld;
        FlowCapableNodeConnector fcNodeConnectorOld;
        private final String interfaceName;
        private final String parentInterface;
        private final IdManagerService idManager;
        private final boolean isNetworkEvent;
        private final boolean isParentInterface;

        public InterfaceStateRemoveWorker(IdManagerService idManager, NodeConnectorId nodeConnectorIdNew,
                                          NodeConnectorId nodeConnectorIdOld,
                                          FlowCapableNodeConnector fcNodeConnectorOld,
                                          String interfaceName,
                                          String parentInterface,
                                          boolean isNetworkEvent,
                                          boolean isParentInterface) {
            this.nodeConnectorIdNew = nodeConnectorIdNew;
            this.nodeConnectorIdOld = nodeConnectorIdOld;
            this.fcNodeConnectorOld = fcNodeConnectorOld;
            this.interfaceName = interfaceName;
            this.parentInterface = parentInterface;
            this.idManager = idManager;
            this.isNetworkEvent = isNetworkEvent;
            this.isParentInterface = isParentInterface;
        }

        @Override
        public Object call() throws Exception {
            // If another renderer(for eg : CSS) needs to be supported, check can be performed here
            // to call the respective helpers.

            List<ListenableFuture<Void>> futures = null;

            futures = OvsInterfaceStateRemoveHelper.removeInterfaceStateConfiguration(idManager, mdsalApiManager,
                nodeConnectorIdNew, nodeConnectorIdOld, dataBroker, interfaceName,
                fcNodeConnectorOld, parentInterface);

            return futures;
        }

        @Override
        public String toString() {
            return "InterfaceStateRemoveWorker{" +
                    "nodeConnectorIdNew=" + nodeConnectorIdNew +
                    ", nodeConnectorIdOld=" + nodeConnectorIdOld +
                    ", fcNodeConnectorOld=" + fcNodeConnectorOld +
                    ", interfaceName='" + interfaceName + '\'' +
                    '}';
        }
    }
}
