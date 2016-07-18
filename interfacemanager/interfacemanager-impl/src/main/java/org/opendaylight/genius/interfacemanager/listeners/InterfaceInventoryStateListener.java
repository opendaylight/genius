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
import org.opendaylight.genius.datastoreutils.AsyncDataTreeChangeListenerBase;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.genius.interfacemanager.renderer.ovs.statehelpers.OvsInterfaceStateAddHelper;
import org.opendaylight.genius.interfacemanager.renderer.ovs.statehelpers.OvsInterfaceStateRemoveHelper;
import org.opendaylight.genius.interfacemanager.renderer.ovs.statehelpers.OvsInterfaceStateUpdateHelper;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.AlivenessMonitorService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 *
 * This Class is a Data Change Listener for FlowCapableNodeConnector updates.
 * This creates an entry in the interface-state OperDS for every node-connector used.
 *
 * NOTE: This class just creates an ifstate entry whose interface-name will be the same as the node-connector portname.
 * If PortName is not unique across DPNs, this implementation can have problems.
 */

public class InterfaceInventoryStateListener extends AsyncDataTreeChangeListenerBase<FlowCapableNodeConnector, InterfaceInventoryStateListener> {
    private static final Logger LOG = LoggerFactory.getLogger(InterfaceInventoryStateListener.class);
    private DataBroker dataBroker;
    private IdManagerService idManager;
    private IMdsalApiManager mdsalApiManager;
    private AlivenessMonitorService alivenessMonitorService;

    public InterfaceInventoryStateListener(final DataBroker dataBroker, final IdManagerService idManager,
                                           final IMdsalApiManager mdsalApiManager, final AlivenessMonitorService alivenessMonitorService) {
        super(FlowCapableNodeConnector.class, InterfaceInventoryStateListener.class);
        this.dataBroker = dataBroker;
        this.idManager = idManager;
        this.mdsalApiManager = mdsalApiManager;
        this.alivenessMonitorService = alivenessMonitorService;
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
        LOG.debug("Received NodeConnector Remove Event: {}, {}", key, flowCapableNodeConnectorOld);
        String portName = flowCapableNodeConnectorOld.getName();
        NodeConnectorId nodeConnectorIdNew = InstanceIdentifier.keyOf(key.firstIdentifierOf(NodeConnector.class)).getId();

        //VM Migration: Skip OFPPR_DELETE event received after OFPPR_ADD for same interface from Older DPN
        NodeConnectorId nodeConnectorIdOld = IfmUtil.getNodeConnectorIdFromInterface(portName, dataBroker);
        if(nodeConnectorIdOld != null && !nodeConnectorIdNew.equals(nodeConnectorIdOld)) {
            LOG.info("Skipping the NodeConnector Remove Event received for the interface exists in newer DPN: {}, {}", nodeConnectorIdNew, nodeConnectorIdOld);
            return;
        }

        remove(nodeConnectorIdNew, nodeConnectorIdOld, flowCapableNodeConnectorOld, portName);
    }

    @Override
    protected void update(InstanceIdentifier<FlowCapableNodeConnector> key, FlowCapableNodeConnector fcNodeConnectorOld,
                          FlowCapableNodeConnector fcNodeConnectorNew) {
        LOG.debug("Received NodeConnector Update Event: {}, {}, {}", key, fcNodeConnectorOld, fcNodeConnectorNew);
        String portName = fcNodeConnectorNew.getName();
        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();

        InterfaceStateUpdateWorker portStateUpdateWorker = new InterfaceStateUpdateWorker(key, fcNodeConnectorOld,
                fcNodeConnectorNew, portName);
        coordinator.enqueueJob(portName, portStateUpdateWorker, 3);
    }

    @Override
    protected void add(InstanceIdentifier<FlowCapableNodeConnector> key, FlowCapableNodeConnector fcNodeConnectorNew) {
        LOG.debug("Received NodeConnector Add Event: {}, {}", key, fcNodeConnectorNew);
        String portName = fcNodeConnectorNew.getName();
        NodeConnectorId nodeConnectorId = InstanceIdentifier.keyOf(key.firstIdentifierOf(NodeConnector.class)).getId();

        //VM Migration: Delete existing interface entry for older DPN
        NodeConnectorId nodeConnectorIdOld = IfmUtil.getNodeConnectorIdFromInterface(portName, dataBroker);
        if(nodeConnectorIdOld != null && !nodeConnectorId.equals(nodeConnectorIdOld)) {
            LOG.info("Received NodeConnector Remove Event for the interface exists in older DPN: {}, {}", nodeConnectorId, nodeConnectorIdOld);
            remove(nodeConnectorId, nodeConnectorIdOld, fcNodeConnectorNew, portName);
        }

        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();

        InterfaceStateAddWorker ifStateAddWorker = new InterfaceStateAddWorker(idManager, nodeConnectorId,
                fcNodeConnectorNew, portName);
        coordinator.enqueueJob(portName, ifStateAddWorker, 3);
    }

    private void remove(NodeConnectorId nodeConnectorIdNew, NodeConnectorId nodeConnectorIdOld,
                        FlowCapableNodeConnector fcNodeConnectorNew, String portName) {
        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
        Interface ifState = InterfaceManagerCommonUtils.getInterfaceStateFromOperDS(portName, dataBroker);

        InterfaceStateRemoveWorker portStateRemoveWorker = new InterfaceStateRemoveWorker(idManager,
               nodeConnectorIdNew, nodeConnectorIdOld, fcNodeConnectorNew, portName, ifState);
        coordinator.enqueueJob(portName, portStateRemoveWorker, 3);
    }

    private class InterfaceStateAddWorker implements Callable {
        private final NodeConnectorId nodeConnectorId;
        private final FlowCapableNodeConnector fcNodeConnectorNew;
        private final String interfaceName;
        private final IdManagerService idManager;

        public InterfaceStateAddWorker(IdManagerService idManager, NodeConnectorId nodeConnectorId,
                                       FlowCapableNodeConnector fcNodeConnectorNew,
                                       String portName) {
            this.nodeConnectorId = nodeConnectorId;
            this.fcNodeConnectorNew = fcNodeConnectorNew;
            this.interfaceName = portName;
            this.idManager = idManager;
        }

        @Override
        public Object call() throws Exception {
            // If another renderer(for eg : CSS) needs to be supported, check can be performed here
            // to call the respective helpers.
            List<ListenableFuture<Void>> futures = OvsInterfaceStateAddHelper.addState(dataBroker, idManager, mdsalApiManager, alivenessMonitorService, nodeConnectorId,
                    interfaceName, fcNodeConnectorNew);
            List<InterfaceChildEntry> interfaceChildEntries = getInterfaceChildEntries(dataBroker, interfaceName, nodeConnectorId);
            for (InterfaceChildEntry interfaceChildEntry : interfaceChildEntries) {
                InterfaceStateAddWorker interfaceStateAddWorker = new InterfaceStateAddWorker(idManager, nodeConnectorId,
                        fcNodeConnectorNew, interfaceChildEntry.getChildInterface());
                DataStoreJobCoordinator.getInstance().enqueueJob(interfaceName, interfaceStateAddWorker);
            }
            return futures;
        }

        @Override
        public String toString() {
            return "InterfaceStateAddWorker{" +
                    "nodeConnectorId=" + nodeConnectorId +
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
            List<ListenableFuture<Void>> futures = OvsInterfaceStateUpdateHelper.updateState(key, alivenessMonitorService, dataBroker, interfaceName,
                    fcNodeConnectorNew, fcNodeConnectorOld);
            NodeConnectorId nodeConnectorId = InstanceIdentifier.keyOf(key.firstIdentifierOf(NodeConnector.class)).getId();
            List<InterfaceChildEntry> interfaceChildEntries = getInterfaceChildEntries(dataBroker, interfaceName, nodeConnectorId);
            for (InterfaceChildEntry interfaceChildEntry : interfaceChildEntries) {
                InterfaceStateUpdateWorker interfaceStateUpdateWorker = new InterfaceStateUpdateWorker(key, fcNodeConnectorOld,
                        fcNodeConnectorNew, interfaceChildEntry.getChildInterface());
                DataStoreJobCoordinator.getInstance().enqueueJob(interfaceName, interfaceStateUpdateWorker);
            }
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
        private final NodeConnectorId nodeConnectorIdOld;
        FlowCapableNodeConnector fcNodeConnectorOld;
        private final String interfaceName;
        private final IdManagerService idManager;
        private final Interface ifState;

        public InterfaceStateRemoveWorker(IdManagerService idManager, NodeConnectorId nodeConnectorIdNew,
                                          NodeConnectorId nodeConnectorIdOld,
                                          FlowCapableNodeConnector fcNodeConnectorOld,
                                          String portName,Interface ifState) {
            this.nodeConnectorIdNew = nodeConnectorIdNew;
            this.nodeConnectorIdOld = nodeConnectorIdOld;
            this.fcNodeConnectorOld = fcNodeConnectorOld;
            this.interfaceName = portName;
            this.idManager = idManager;
            this.ifState = ifState;
        }

        @Override
        public Object call() throws Exception {
            // If another renderer(for eg : CSS) needs to be supported, check can be performed here
            // to call the respective helpers.
            List<ListenableFuture<Void>> futures = OvsInterfaceStateRemoveHelper.removeInterfaceStateConfiguration(idManager, mdsalApiManager, alivenessMonitorService,
                    nodeConnectorIdNew, nodeConnectorIdOld, dataBroker, interfaceName, fcNodeConnectorOld, ifState);

            List<InterfaceChildEntry> interfaceChildEntries = getInterfaceChildEntries(dataBroker, interfaceName, nodeConnectorIdNew);
            for (InterfaceChildEntry interfaceChildEntry : interfaceChildEntries) {
                // Fetch all interfaces on this port and trigger remove worker for each of them
                InterfaceStateRemoveWorker interfaceStateRemoveWorker = new InterfaceStateRemoveWorker(idManager,
                        nodeConnectorIdNew, nodeConnectorIdOld, fcNodeConnectorOld, interfaceChildEntry.getChildInterface(),
                        InterfaceManagerCommonUtils.getInterfaceStateFromOperDS(interfaceChildEntry.getChildInterface(), dataBroker));
                DataStoreJobCoordinator.getInstance().enqueueJob(interfaceName, interfaceStateRemoveWorker);

            }
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

    public static List<InterfaceChildEntry> getInterfaceChildEntries(DataBroker dataBroker, String interfaceName, NodeConnectorId nodeConnectorId) {
        InterfaceParentEntry interfaceParentEntry =
                InterfaceMetaUtils.getInterfaceParentEntryFromConfigDS(interfaceName, dataBroker);
        if (interfaceParentEntry != null && interfaceParentEntry.getInterfaceChildEntry() != null) {
            return interfaceParentEntry.getInterfaceChildEntry();
        }
        StringBuilder parentInterface = new StringBuilder(IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId));
        parentInterface.append(IfmConstants.OF_URI_SEPARATOR);
        parentInterface.append(interfaceName);
        String dpnId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
        interfaceParentEntry = InterfaceMetaUtils.getInterfaceParentEntryFromConfigDS(parentInterface.toString(), dataBroker);
        if (interfaceParentEntry != null && interfaceParentEntry.getInterfaceChildEntry() != null) {
            return interfaceParentEntry.getInterfaceChildEntry();
        }
        return new ArrayList<>();
    }
}