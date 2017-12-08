/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.listeners;

import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncClusteredDataTreeChangeListenerBase;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.commons.AlivenessMonitorUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.genius.interfacemanager.renderer.ovs.statehelpers.OvsInterfaceStateAddHelper;
import org.opendaylight.genius.interfacemanager.renderer.ovs.statehelpers.OvsInterfaceStateUpdateHelper;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortReason;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.AlivenessMonitorService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Class is a Data Change Listener for FlowCapableNodeConnector updates.
 * This creates an entry in the interface-state OperDS for every node-connector
 * used.
 *
 * <p>
 * NOTE: This class just creates an ifstate entry whose interface-name will be
 * the same as the node-connector portname. If PortName is not unique across
 * DPNs, this implementation can have problems.
 */
@Singleton
public class InterfaceInventoryStateListener
        extends AsyncClusteredDataTreeChangeListenerBase<FlowCapableNodeConnector, InterfaceInventoryStateListener> {
    private static final Logger LOG = LoggerFactory.getLogger(InterfaceInventoryStateListener.class);
    private final DataBroker dataBroker;
    private final IdManagerService idManager;
    private final EntityOwnershipUtils entityOwnershipUtils;
    private final JobCoordinator coordinator;
    private final InterfaceManagerCommonUtils interfaceManagerCommonUtils;
    private final AlivenessMonitorUtils alivenessMonitorUtils;
    private final OvsInterfaceStateUpdateHelper ovsInterfaceStateUpdateHelper;
    private final OvsInterfaceStateAddHelper ovsInterfaceStateAddHelper;
    private final InterfaceMetaUtils interfaceMetaUtils;

    @Inject
    public InterfaceInventoryStateListener(final DataBroker dataBroker, final IdManagerService idManagerService,
            final IMdsalApiManager mdsalApiManager, final AlivenessMonitorService alivenessMonitorService,
            final EntityOwnershipUtils entityOwnershipUtils, final JobCoordinator coordinator,
            final InterfaceManagerCommonUtils interfaceManagerCommonUtils,
            final OvsInterfaceStateAddHelper ovsInterfaceStateAddHelper,
            final OvsInterfaceStateUpdateHelper ovsInterfaceStateUpdateHelper,
            final AlivenessMonitorUtils alivenessMonitorUtils,
            final InterfaceMetaUtils interfaceMetaUtils) {
        super(FlowCapableNodeConnector.class, InterfaceInventoryStateListener.class);
        this.dataBroker = dataBroker;
        this.idManager = idManagerService;
        this.entityOwnershipUtils = entityOwnershipUtils;
        this.coordinator = coordinator;
        this.interfaceManagerCommonUtils = interfaceManagerCommonUtils;
        this.alivenessMonitorUtils = alivenessMonitorUtils;
        this.ovsInterfaceStateUpdateHelper = ovsInterfaceStateUpdateHelper;
        this.ovsInterfaceStateAddHelper = ovsInterfaceStateAddHelper;
        this.interfaceMetaUtils = interfaceMetaUtils;
        this.registerListener(LogicalDatastoreType.OPERATIONAL, dataBroker);
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
        if (!entityOwnershipUtils.isEntityOwner(IfmConstants.INTERFACE_CONFIG_ENTITY,
                IfmConstants.INTERFACE_CONFIG_ENTITY)) {
            return;
        }

        LOG.debug("Received NodeConnector Remove Event: {}, {}", key, flowCapableNodeConnectorOld);
        NodeConnectorId nodeConnectorId = InstanceIdentifier.keyOf(key.firstIdentifierOf(NodeConnector.class))
            .getId();
        String portName = InterfaceManagerCommonUtils.getPortNameForInterface(nodeConnectorId,
            flowCapableNodeConnectorOld.getName());

        remove(nodeConnectorId, null, flowCapableNodeConnectorOld, portName, true);
    }

    private void remove(NodeConnectorId nodeConnectorIdNew, NodeConnectorId nodeConnectorIdOld,
            FlowCapableNodeConnector fcNodeConnectorNew, String portName, boolean isNetworkEvent) {
        boolean isNodePresent = interfaceManagerCommonUtils.isNodePresent(nodeConnectorIdNew);
        InterfaceStateRemoveWorker portStateRemoveWorker = new InterfaceStateRemoveWorker(idManager, nodeConnectorIdNew,
                nodeConnectorIdOld, fcNodeConnectorNew, portName, isNodePresent, isNetworkEvent, true);
        coordinator.enqueueJob(portName, portStateRemoveWorker, IfmConstants.JOB_MAX_RETRIES);
    }

    @Override
    protected void update(InstanceIdentifier<FlowCapableNodeConnector> key, FlowCapableNodeConnector fcNodeConnectorOld,
            FlowCapableNodeConnector fcNodeConnectorNew) {
        if (!entityOwnershipUtils.isEntityOwner(IfmConstants.INTERFACE_CONFIG_ENTITY,
                IfmConstants.INTERFACE_CONFIG_ENTITY)) {
            return;
        }

        LOG.debug("Received NodeConnector Update Event: {}, {}, {}", key, fcNodeConnectorOld, fcNodeConnectorNew);
        NodeConnectorId nodeConnectorId = InstanceIdentifier.keyOf(key.firstIdentifierOf(NodeConnector.class))
            .getId();
        String portName = InterfaceManagerCommonUtils.getPortNameForInterface(nodeConnectorId,
            fcNodeConnectorNew.getName());

        InterfaceStateUpdateWorker portStateUpdateWorker = new InterfaceStateUpdateWorker(key, fcNodeConnectorOld,
            fcNodeConnectorNew, portName);
        coordinator.enqueueJob(portName, portStateUpdateWorker, IfmConstants.JOB_MAX_RETRIES);
    }

    @Override
    protected void add(InstanceIdentifier<FlowCapableNodeConnector> key, FlowCapableNodeConnector fcNodeConnectorNew) {
        if (!entityOwnershipUtils.isEntityOwner(IfmConstants.INTERFACE_CONFIG_ENTITY,
                IfmConstants.INTERFACE_CONFIG_ENTITY)) {
            return;
        }

        LOG.debug("Received NodeConnector Add Event: {}, {}", key, fcNodeConnectorNew);
        NodeConnectorId nodeConnectorId = InstanceIdentifier.keyOf(key.firstIdentifierOf(NodeConnector.class))
            .getId();
        String portName = InterfaceManagerCommonUtils.getPortNameForInterface(nodeConnectorId,
            fcNodeConnectorNew.getName());

        if (InterfaceManagerCommonUtils.isNovaPort(portName) || InterfaceManagerCommonUtils.isK8SPort(portName)) {
            NodeConnectorId nodeConnectorIdOld =
                    FlowBasedServicesUtils.getNodeConnectorIdFromInterface(portName, interfaceManagerCommonUtils);
            if (nodeConnectorIdOld != null && !nodeConnectorId.equals(nodeConnectorIdOld)) {
                BigInteger dpnIdOld = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorIdOld);
                BigInteger dpnIdNew = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
                if (!Objects.equals(dpnIdOld, dpnIdNew)) {
                    if (fcNodeConnectorNew.getReason() != PortReason.Add) {
                        LOG.error("Dropping Port update event for {}, as DPN id is changed from {} to {}",
                            fcNodeConnectorNew.getName(), dpnIdOld, dpnIdNew);
                        return;
                    }
                } else {
                    LOG.warn("Port number update detected for {}", fcNodeConnectorNew.getName());
                }
                //VM Migration or Port Number Update: Delete existing interface entry for older DPN
                LOG.debug("Triggering NodeConnector Remove Event for the interface: {}, {}, {}", portName,
                    nodeConnectorId, nodeConnectorIdOld);
                remove(nodeConnectorId, nodeConnectorIdOld, fcNodeConnectorNew, portName, false);
                // Adding a delay of 10sec for VM migration, so applications will have sufficient time
                // for processing remove before add
                try {
                    Thread.sleep(IfmConstants.DELAY_TIME_IN_MILLISECOND);
                } catch (InterruptedException e) {
                    LOG.error("Error while waiting for the vm migration remove events to get processed");
                }
            }
        }

        InterfaceStateAddWorker ifStateAddWorker = new InterfaceStateAddWorker(idManager, nodeConnectorId,
            fcNodeConnectorNew, portName);
        coordinator.enqueueJob(portName, ifStateAddWorker, IfmConstants.JOB_MAX_RETRIES);
    }


    private class InterfaceStateAddWorker implements Callable {
        private final NodeConnectorId nodeConnectorId;
        private final FlowCapableNodeConnector fcNodeConnectorNew;
        private final String interfaceName;
        private final IdManagerService idManager;

        InterfaceStateAddWorker(IdManagerService idManager, NodeConnectorId nodeConnectorId,
                FlowCapableNodeConnector fcNodeConnectorNew, String portName) {
            this.nodeConnectorId = nodeConnectorId;
            this.fcNodeConnectorNew = fcNodeConnectorNew;
            this.interfaceName = portName;
            this.idManager = idManager;
        }

        @Override
        public Object call() {
            List<ListenableFuture<Void>> futures = ovsInterfaceStateAddHelper.addState(nodeConnectorId, interfaceName,
                    fcNodeConnectorNew);
            List<InterfaceChildEntry> interfaceChildEntries = getInterfaceChildEntries(interfaceName);
            for (InterfaceChildEntry interfaceChildEntry : interfaceChildEntries) {
                InterfaceStateAddWorker interfaceStateAddWorker = new InterfaceStateAddWorker(idManager,
                        nodeConnectorId, fcNodeConnectorNew, interfaceChildEntry.getChildInterface());
                coordinator.enqueueJob(interfaceName, interfaceStateAddWorker);
            }
            return futures;
        }

        @Override
        public String toString() {
            return "InterfaceStateAddWorker{" + "nodeConnectorId=" + nodeConnectorId + ", fcNodeConnectorNew="
                    + fcNodeConnectorNew + ", interfaceName='" + interfaceName + '\'' + '}';
        }
    }

    private class InterfaceStateUpdateWorker implements Callable {
        private final InstanceIdentifier<FlowCapableNodeConnector> key;
        private final FlowCapableNodeConnector fcNodeConnectorOld;
        private final FlowCapableNodeConnector fcNodeConnectorNew;
        private final String interfaceName;

        InterfaceStateUpdateWorker(InstanceIdentifier<FlowCapableNodeConnector> key,
                FlowCapableNodeConnector fcNodeConnectorOld, FlowCapableNodeConnector fcNodeConnectorNew,
                String portName) {
            this.key = key;
            this.fcNodeConnectorOld = fcNodeConnectorOld;
            this.fcNodeConnectorNew = fcNodeConnectorNew;
            this.interfaceName = portName;
        }

        @Override
        public Object call() {
            List<ListenableFuture<Void>> futures = ovsInterfaceStateUpdateHelper.updateState(
                    interfaceName, fcNodeConnectorNew, fcNodeConnectorOld);
            List<InterfaceChildEntry> interfaceChildEntries = getInterfaceChildEntries(interfaceName);
            for (InterfaceChildEntry interfaceChildEntry : interfaceChildEntries) {
                InterfaceStateUpdateWorker interfaceStateUpdateWorker = new InterfaceStateUpdateWorker(key,
                        fcNodeConnectorOld, fcNodeConnectorNew, interfaceChildEntry.getChildInterface());
                coordinator.enqueueJob(interfaceName, interfaceStateUpdateWorker);
            }
            return futures;
        }

        @Override
        public String toString() {
            return "InterfaceStateUpdateWorker{" + "key=" + key + ", fcNodeConnectorOld=" + fcNodeConnectorOld
                    + ", fcNodeConnectorNew=" + fcNodeConnectorNew + ", interfaceName='" + interfaceName + '\'' + '}';
        }
    }

    private class InterfaceStateRemoveWorker implements Callable {
        private final NodeConnectorId nodeConnectorIdNew;
        private NodeConnectorId nodeConnectorIdOld;
        FlowCapableNodeConnector fcNodeConnectorOld;
        private final String interfaceName;
        private final IdManagerService idManager;
        private final boolean isNodePresent;
        private final boolean isNetworkEvent;
        private final boolean isParentInterface;

        InterfaceStateRemoveWorker(IdManagerService idManager, NodeConnectorId nodeConnectorIdNew,
                NodeConnectorId nodeConnectorIdOld, FlowCapableNodeConnector fcNodeConnectorOld, String interfaceName,
                boolean isNodePresent, boolean isNetworkEvent, boolean isParentInterface) {
            this.nodeConnectorIdNew = nodeConnectorIdNew;
            this.nodeConnectorIdOld = nodeConnectorIdOld;
            this.fcNodeConnectorOld = fcNodeConnectorOld;
            this.interfaceName = interfaceName;
            this.idManager = idManager;
            this.isNodePresent = isNodePresent;
            this.isNetworkEvent = isNetworkEvent;
            this.isParentInterface = isParentInterface;
        }

        @Override
        public Object call() {
            List<ListenableFuture<Void>> futures = null;
            // VM Migration: Skip OFPPR_DELETE event received after OFPPR_ADD
            // for same interface from Older DPN
            if (isParentInterface && isNetworkEvent) {
                nodeConnectorIdOld = FlowBasedServicesUtils.getNodeConnectorIdFromInterface(interfaceName,
                        interfaceManagerCommonUtils);
                if (nodeConnectorIdOld != null && !nodeConnectorIdNew.equals(nodeConnectorIdOld)) {
                    LOG.debug("Dropping the NodeConnector Remove Event for the interface: {}, {}, {}", interfaceName,
                            nodeConnectorIdNew, nodeConnectorIdOld);
                    return futures;
                }
            }

            futures = removeInterfaceStateConfiguration(nodeConnectorIdNew, nodeConnectorIdOld, interfaceName,
                    fcNodeConnectorOld, isNodePresent);

            List<InterfaceChildEntry> interfaceChildEntries = getInterfaceChildEntries(interfaceName);
            for (InterfaceChildEntry interfaceChildEntry : interfaceChildEntries) {
                // Fetch all interfaces on this port and trigger remove worker
                // for each of them
                InterfaceStateRemoveWorker interfaceStateRemoveWorker = new InterfaceStateRemoveWorker(idManager,
                        nodeConnectorIdNew, nodeConnectorIdOld, fcNodeConnectorOld,
                        interfaceChildEntry.getChildInterface(), isNodePresent, isNetworkEvent, false);
                coordinator.enqueueJob(interfaceName, interfaceStateRemoveWorker);
            }
            return futures;
        }

        private List<ListenableFuture<Void>> removeInterfaceStateConfiguration(NodeConnectorId nodeConnectorIdNew,
                NodeConnectorId nodeConnectorIdOld, String interfaceName, FlowCapableNodeConnector fcNodeConnectorOld,
                boolean isNodePresent) {
            LOG.debug("Removing interface state information for interface: {} {}", interfaceName, isNodePresent);
            List<ListenableFuture<Void>> futures = new ArrayList<>();
            WriteTransaction defaultOperationalShardTransaction = dataBroker.newWriteOnlyTransaction();

            //VM Migration: Use old nodeConnectorId to delete the interface entry
            NodeConnectorId nodeConnectorId = nodeConnectorIdOld != null
                    && !nodeConnectorIdNew.equals(nodeConnectorIdOld) ? nodeConnectorIdOld : nodeConnectorIdNew;
            // delete the port entry from interface operational DS
            BigInteger dpId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);

            //VM Migration: Update the interface state to unknown only if remove event received for same switch
            if (!isNodePresent && nodeConnectorIdNew.equals(nodeConnectorIdOld)) {
                //Remove event is because of connection lost between controller and switch, or switch shutdown.
                // Hence, don't remove the interface but set the status as "unknown"
                ovsInterfaceStateUpdateHelper.updateInterfaceStateOnNodeRemove(interfaceName, fcNodeConnectorOld,
                        defaultOperationalShardTransaction);
            } else {
                InterfaceManagerCommonUtils.deleteStateEntry(interfaceName, defaultOperationalShardTransaction);
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces
                    .Interface iface = interfaceManagerCommonUtils.getInterfaceFromConfigDS(interfaceName);

                if (iface != null) {
                    // If this interface is a tunnel interface, remove the tunnel ingress flow and stop LLDP monitoring
                    if (InterfaceManagerCommonUtils.isTunnelInterface(iface)) {
                        interfaceMetaUtils.removeLportTagInterfaceMap(defaultOperationalShardTransaction,
                                interfaceName);
                        handleTunnelMonitoringRemoval(dpId, iface.getName(), iface.getAugmentation(IfTunnel.class),
                                defaultOperationalShardTransaction, futures);
                        return futures;
                    }
                }
                // remove ingress flow only for northbound configured interfaces
                // skip this check for non-unique ports(Ex: br-int,br-ex)
                if (iface != null || !interfaceName.contains(fcNodeConnectorOld.getName())) {
                    FlowBasedServicesUtils.removeIngressFlow(interfaceName, dpId, dataBroker, futures);
                }

                // Delete the Vpn Interface from DpnToInterface Op DS.
                interfaceManagerCommonUtils.deleteDpnToInterface(dpId, interfaceName,
                        defaultOperationalShardTransaction);
            }
            futures.add(defaultOperationalShardTransaction.submit());
            return futures;
        }

        private void handleTunnelMonitoringRemoval(BigInteger dpId, String interfaceName,
                IfTunnel ifTunnel, WriteTransaction transaction, List<ListenableFuture<Void>> futures) {
            interfaceManagerCommonUtils.removeTunnelIngressFlow(ifTunnel, dpId, interfaceName);

            IfmUtil.unbindService(dataBroker, coordinator, interfaceName,
                    FlowBasedServicesUtils.buildDefaultServiceId(interfaceName));

            futures.add(transaction.submit());
            alivenessMonitorUtils.stopLLDPMonitoring(ifTunnel, interfaceName);
        }

        @Override
        public String toString() {
            return "InterfaceStateRemoveWorker{" + "nodeConnectorIdNew=" + nodeConnectorIdNew + ", nodeConnectorIdOld="
                    + nodeConnectorIdOld + ", fcNodeConnectorOld=" + fcNodeConnectorOld + ", interfaceName='"
                    + interfaceName + '\'' + '}';
        }
    }

    public List<InterfaceChildEntry> getInterfaceChildEntries(String interfaceName) {
        InterfaceParentEntry interfaceParentEntry =
                interfaceMetaUtils.getInterfaceParentEntryFromConfigDS(interfaceName);
        if (interfaceParentEntry != null && interfaceParentEntry.getInterfaceChildEntry() != null) {
            return interfaceParentEntry.getInterfaceChildEntry();
        }
        return new ArrayList<>();
    }
}
