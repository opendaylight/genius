/*
 * Copyright (c) 2016, 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.listeners;

import static org.opendaylight.genius.infra.Datastore.CONFIGURATION;
import static org.opendaylight.genius.infra.Datastore.OPERATIONAL;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.aries.blueprint.annotation.service.Reference;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncClusteredDataTreeChangeListenerBase;
import org.opendaylight.genius.infra.Datastore.Configuration;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.infra.TypedReadWriteTransaction;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.InterfacemgrProvider;
import org.opendaylight.genius.interfacemanager.commons.AlivenessMonitorUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.genius.interfacemanager.recovery.impl.InterfaceServiceRecoveryHandler;
import org.opendaylight.genius.interfacemanager.renderer.ovs.statehelpers.OvsInterfaceStateAddHelper;
import org.opendaylight.genius.interfacemanager.renderer.ovs.statehelpers.OvsInterfaceStateUpdateHelper;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.infrautils.utils.function.InterruptibleCheckedConsumer;
import org.opendaylight.serviceutils.srm.RecoverableListener;
import org.opendaylight.serviceutils.srm.ServiceRecoveryRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortReason;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint64;
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
        extends AsyncClusteredDataTreeChangeListenerBase<FlowCapableNodeConnector, InterfaceInventoryStateListener>
        implements RecoverableListener {

    private static final Logger LOG = LoggerFactory.getLogger(InterfaceInventoryStateListener.class);
    private static final Logger EVENT_LOGGER = LoggerFactory.getLogger("GeniusEventLogger");

    private final DataBroker dataBroker;
    private final ManagedNewTransactionRunner txRunner;
    private final IdManagerService idManager;
    private final EntityOwnershipUtils entityOwnershipUtils;
    private final JobCoordinator coordinator;
    private final InterfaceManagerCommonUtils interfaceManagerCommonUtils;
    private final AlivenessMonitorUtils alivenessMonitorUtils;
    private final OvsInterfaceStateUpdateHelper ovsInterfaceStateUpdateHelper;
    private final OvsInterfaceStateAddHelper ovsInterfaceStateAddHelper;
    private final InterfaceMetaUtils interfaceMetaUtils;
    private final PortNameCache portNameCache;
    private final InterfacemgrProvider interfacemgrProvider;

    @Inject
    public InterfaceInventoryStateListener(@Reference final DataBroker dataBroker,
                                           final IdManagerService idManagerService,
                                           final EntityOwnershipUtils entityOwnershipUtils,
                                           @Reference final JobCoordinator coordinator,
                                           final InterfaceManagerCommonUtils interfaceManagerCommonUtils,
                                           final OvsInterfaceStateAddHelper ovsInterfaceStateAddHelper,
                                           final OvsInterfaceStateUpdateHelper ovsInterfaceStateUpdateHelper,
                                           final AlivenessMonitorUtils alivenessMonitorUtils,
                                           final InterfaceMetaUtils interfaceMetaUtils,
                                           final PortNameCache portNameCache,
                                           final InterfaceServiceRecoveryHandler interfaceServiceRecoveryHandler,
                                           @Reference final ServiceRecoveryRegistry serviceRecoveryRegistry,
                                           final InterfacemgrProvider interfacemgrProvider) {
        super(FlowCapableNodeConnector.class, InterfaceInventoryStateListener.class);
        this.dataBroker = dataBroker;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.idManager = idManagerService;
        this.entityOwnershipUtils = entityOwnershipUtils;
        this.coordinator = coordinator;
        this.interfaceManagerCommonUtils = interfaceManagerCommonUtils;
        this.alivenessMonitorUtils = alivenessMonitorUtils;
        this.ovsInterfaceStateUpdateHelper = ovsInterfaceStateUpdateHelper;
        this.ovsInterfaceStateAddHelper = ovsInterfaceStateAddHelper;
        this.interfaceMetaUtils = interfaceMetaUtils;
        this.portNameCache = portNameCache;
        this.interfacemgrProvider = interfacemgrProvider;
        registerListener();
        serviceRecoveryRegistry.addRecoverableListener(interfaceServiceRecoveryHandler.buildServiceRegistryKey(),
                this);
    }

    @Override
    public void registerListener() {
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
        String interfaceName = flowCapableNodeConnectorOld.getName();
        EVENT_LOGGER.debug("IFM-InterfaceInventoryState,REMOVE {}", interfaceName);
        if (interfacemgrProvider.isItmDirectTunnelsEnabled()
            && InterfaceManagerCommonUtils.isTunnelPort(interfaceName)
            && interfaceManagerCommonUtils.getInterfaceFromConfigDS(interfaceName) == null) {
            LOG.debug("ITM Direct Tunnels is enabled, node connector removed event for"
                    + " internal tunnel {}", interfaceName);
            return;
        }

        NodeConnectorId nodeConnectorId = InstanceIdentifier.keyOf(key.firstIdentifierOf(NodeConnector.class))
                .getId();
        LOG.trace("Removing entry for port id {} from map",nodeConnectorId.getValue());
        portNameCache.remove(nodeConnectorId.getValue());


        if (!entityOwnershipUtils.isEntityOwner(IfmConstants.INTERFACE_CONFIG_ENTITY,
                IfmConstants.INTERFACE_CONFIG_ENTITY)) {
            return;
        }

        LOG.debug("Received NodeConnector Remove Event: {}, {}", key, flowCapableNodeConnectorOld);
        String portName = InterfaceManagerCommonUtils.getPortNameForInterface(nodeConnectorId,
            flowCapableNodeConnectorOld.getName());
        EVENT_LOGGER.debug("IFM-InterfaceInventoryState Entity Owner,REMOVE {},{}", portName,
                nodeConnectorId.getValue());

        remove(nodeConnectorId, null, flowCapableNodeConnectorOld, portName, true);
    }

    private void remove(NodeConnectorId nodeConnectorIdNew, NodeConnectorId nodeConnectorIdOld,
                        FlowCapableNodeConnector fcNodeConnectorNew, String portName, boolean isNetworkEvent) {
        InterfaceStateRemoveWorker portStateRemoveWorker = new InterfaceStateRemoveWorker(idManager,
                nodeConnectorIdNew, nodeConnectorIdOld, fcNodeConnectorNew, portName,
                isNetworkEvent, true);
        coordinator.enqueueJob(portName, portStateRemoveWorker, IfmConstants.JOB_MAX_RETRIES);
    }

    @Override
    protected void update(InstanceIdentifier<FlowCapableNodeConnector> key, FlowCapableNodeConnector fcNodeConnectorOld,
        FlowCapableNodeConnector fcNodeConnectorNew) {
        String interfaceName = fcNodeConnectorNew.getName();
        EVENT_LOGGER.debug("IFM-InterfaceInventoryState,UPDATE {},{}", fcNodeConnectorNew.getName(),
                fcNodeConnectorNew.getReason());
        if (interfacemgrProvider.isItmDirectTunnelsEnabled()
            && InterfaceManagerCommonUtils.isTunnelPort(interfaceName)
            && interfaceManagerCommonUtils.getInterfaceFromConfigDS(interfaceName) == null) {
            LOG.debug("ITM Direct Tunnels is enabled, hence ignoring node connector Update event for"
                    + " internal tunnel {}", interfaceName);
            return;
        }


        if (fcNodeConnectorNew.getReason() == PortReason.Delete
                || !entityOwnershipUtils.isEntityOwner(IfmConstants.INTERFACE_CONFIG_ENTITY,
                IfmConstants.INTERFACE_CONFIG_ENTITY)) {
            return;
        }

        LOG.debug("Received NodeConnector Update Event: {}, {}, {}", key, fcNodeConnectorOld, fcNodeConnectorNew);
        NodeConnectorId nodeConnectorId = InstanceIdentifier.keyOf(key.firstIdentifierOf(NodeConnector.class)).getId();
        String portName = InterfaceManagerCommonUtils.getPortNameForInterface(nodeConnectorId,
                fcNodeConnectorNew.getName());
        EVENT_LOGGER.debug("IFM-InterfaceInventoryState Entity Owner,UPDATE {},{}", portName,
                nodeConnectorId.getValue());

        InterfaceStateUpdateWorker portStateUpdateWorker = new InterfaceStateUpdateWorker(key, fcNodeConnectorOld,
            fcNodeConnectorNew, portName);
        coordinator.enqueueJob(portName, portStateUpdateWorker, IfmConstants.JOB_MAX_RETRIES);
    }

    @Override
    protected void add(InstanceIdentifier<FlowCapableNodeConnector> key, FlowCapableNodeConnector fcNodeConnectorNew) {
        String interfaceName = fcNodeConnectorNew.getName();
        EVENT_LOGGER.debug("IFM-InterfaceInventoryState,ADD {}", interfaceName);
        if (interfacemgrProvider.isItmDirectTunnelsEnabled()
            && InterfaceManagerCommonUtils.isTunnelPort(interfaceName)
            && interfaceManagerCommonUtils.getInterfaceFromConfigDS(interfaceName) == null) {
            LOG.debug("ITM Direct Tunnels is enabled, ignoring node connector add for"
                    + " internal tunnel {}", interfaceName);
            return;
        }

        NodeConnectorId nodeConnectorId = InstanceIdentifier.keyOf(key.firstIdentifierOf(NodeConnector.class))
                .getId();
        LOG.trace("Adding entry for portid {} portname {} in map", nodeConnectorId.getValue(),
                fcNodeConnectorNew.getName());
        portNameCache.put(nodeConnectorId.getValue(),fcNodeConnectorNew.getName());
        if (!entityOwnershipUtils.isEntityOwner(IfmConstants.INTERFACE_CONFIG_ENTITY,
                IfmConstants.INTERFACE_CONFIG_ENTITY)) {
            return;
        }

        LOG.debug("Received NodeConnector Add Event: {}, {}", key, fcNodeConnectorNew);
        String portName = InterfaceManagerCommonUtils.getPortNameForInterface(nodeConnectorId,
            fcNodeConnectorNew.getName());
        EVENT_LOGGER.debug("IFM-InterfaceInventoryState Entity Owner,ADD {},{}", portName, nodeConnectorId.getValue());

        if (InterfaceManagerCommonUtils.isNovaPort(portName) || InterfaceManagerCommonUtils.isK8SPort(portName)) {
            NodeConnectorId nodeConnectorIdOld = null;
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                    .ietf.interfaces.rev140508.interfaces.state.Interface interfaceState = interfaceManagerCommonUtils
                    .getInterfaceState(interfaceName);
            if (interfaceState != null) {
                List<String> ofportIds = interfaceState.getLowerLayerIf();
                nodeConnectorIdOld = new NodeConnectorId(ofportIds.get(0));
            }
            if (nodeConnectorIdOld != null && !nodeConnectorId.equals(nodeConnectorIdOld)) {
                Uint64 dpnIdOld = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorIdOld);
                Uint64 dpnIdNew = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
                if (!Objects.equals(dpnIdOld, dpnIdNew)) {
                    if (fcNodeConnectorNew.getReason() != PortReason.Add
                            && interfaceState.getOperStatus()
                            != Interface.OperStatus.Unknown) {
                        LOG.error("Dropping Port update event for {}, as DPN id is changed from {} to {}",
                            fcNodeConnectorNew.getName(), dpnIdOld, dpnIdNew);
                        return;
                    }
                } else {
                    LOG.warn("Port number update detected for {}", fcNodeConnectorNew.getName());
                }
                //VM Migration or Port Number Update: Delete existing interface entry for older DPN
                LOG.trace("Removing entry for port id {} from map",nodeConnectorIdOld.getValue());
                portNameCache.remove(nodeConnectorIdOld.getValue());
                EVENT_LOGGER.debug("IFM-VMMigration,{}", portName);
                LOG.debug("Triggering NodeConnector Remove Event for the interface: {}, {}, {}", portName,
                    nodeConnectorId, nodeConnectorIdOld);
                remove(nodeConnectorId, nodeConnectorIdOld, fcNodeConnectorNew, portName, false);
                // Adding a delay of 10sec for VM migration, so applications will have sufficient time
                // for processing remove before add
                try {
                    Thread.sleep(IfmConstants.DELAY_TIME_IN_MILLISECOND);
                } catch (final InterruptedException e) {
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
            List<ListenableFuture<Void>> futures = ovsInterfaceStateAddHelper.addState(nodeConnectorId,
                    interfaceName, fcNodeConnectorNew);
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

    private class InterfaceStateUpdateWorker implements Callable<List<ListenableFuture<Void>>> {
        private final InstanceIdentifier<FlowCapableNodeConnector> key;
        private final FlowCapableNodeConnector fcNodeConnectorOld;
        private final FlowCapableNodeConnector fcNodeConnectorNew;
        private final String interfaceName;

        InterfaceStateUpdateWorker(InstanceIdentifier<FlowCapableNodeConnector> key,
                                   FlowCapableNodeConnector fcNodeConnectorOld,
                                   FlowCapableNodeConnector fcNodeConnectorNew,
                                   String portName) {
            this.key = key;
            this.fcNodeConnectorOld = fcNodeConnectorOld;
            this.fcNodeConnectorNew = fcNodeConnectorNew;
            this.interfaceName = portName;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
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

    private class InterfaceStateRemoveWorker implements Callable<List<ListenableFuture<Void>>> {
        private final NodeConnectorId nodeConnectorIdNew;
        private NodeConnectorId nodeConnectorIdOld;
        private final FlowCapableNodeConnector fcNodeConnectorOld;
        private final String interfaceName;
        private final IdManagerService idManager;
        private final boolean isNetworkEvent;
        private final boolean isParentInterface;

        InterfaceStateRemoveWorker(IdManagerService idManager, NodeConnectorId nodeConnectorIdNew,
                                   NodeConnectorId nodeConnectorIdOld,
                                   FlowCapableNodeConnector fcNodeConnectorOld, String interfaceName,
                                   boolean isNetworkEvent,
                                   boolean isParentInterface) {
            this.nodeConnectorIdNew = nodeConnectorIdNew;
            this.nodeConnectorIdOld = nodeConnectorIdOld;
            this.fcNodeConnectorOld = fcNodeConnectorOld;
            this.interfaceName = interfaceName;
            this.idManager = idManager;
            this.isNetworkEvent = isNetworkEvent;
            this.isParentInterface = isParentInterface;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            // VM Migration: Skip OFPPR_DELETE event received after OFPPR_ADD
            // for same interface from Older DPN
            if (isParentInterface && isNetworkEvent) {
                nodeConnectorIdOld = FlowBasedServicesUtils.getNodeConnectorIdFromInterface(interfaceName,
                        interfaceManagerCommonUtils);
                if (nodeConnectorIdOld != null && !nodeConnectorIdNew.equals(nodeConnectorIdOld)) {
                    LOG.debug("Dropping the NodeConnector Remove Event for the interface: {}, {}, {}", interfaceName,
                            nodeConnectorIdNew, nodeConnectorIdOld);
                    return Collections.emptyList();
                }
            }

            List<ListenableFuture<Void>> futures = removeInterfaceStateConfiguration();

            List<InterfaceChildEntry> interfaceChildEntries = getInterfaceChildEntries(interfaceName);
            for (InterfaceChildEntry interfaceChildEntry : interfaceChildEntries) {
                // Fetch all interfaces on this port and trigger remove worker
                // for each of them
                InterfaceStateRemoveWorker interfaceStateRemoveWorker = new InterfaceStateRemoveWorker(idManager,
                        nodeConnectorIdNew, nodeConnectorIdOld, fcNodeConnectorOld,
                        interfaceChildEntry.getChildInterface(), isNetworkEvent, false);
                coordinator.enqueueJob(interfaceName, interfaceStateRemoveWorker);
            }
            return futures;
        }

        private List<ListenableFuture<Void>> removeInterfaceStateConfiguration() {
            List<ListenableFuture<Void>> futures = new ArrayList<>();

            //VM Migration: Use old nodeConnectorId to delete the interface entry
            NodeConnectorId nodeConnectorId = nodeConnectorIdOld != null
                    && !nodeConnectorIdNew.equals(nodeConnectorIdOld) ? nodeConnectorIdOld : nodeConnectorIdNew;
            // delete the port entry from interface operational DS
            Uint64 dpId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);

            futures.add(txRunner.applyWithNewTransactionChainAndClose(txChain ->
                txChain.applyWithNewReadWriteTransactionAndSubmit(OPERATIONAL, operTx -> {
                    // In a genuine port delete scenario, the reason will be there in the incoming event, for all
                    // remaining
                    // cases treat the event as DPN disconnect, if old and new ports are same. Else, this is a VM
                    // migration
                    // scenario, and should be treated as port removal.
                    LOG.debug("Removing interface state information for interface: {}", interfaceName);
                    if (fcNodeConnectorOld.getReason() != PortReason.Delete
                        && nodeConnectorIdNew.equals(nodeConnectorIdOld)) {
                        //Remove event is because of connection lost between controller and switch, or switch shutdown.
                        // Hence, don't remove the interface but set the status as "unknown"
                        ovsInterfaceStateUpdateHelper.updateInterfaceStateOnNodeRemove(interfaceName,
                            fcNodeConnectorOld,
                            operTx);
                    } else {
                        EVENT_LOGGER.debug("IFM-OvsInterfaceState,REMOVE {}", interfaceName);
                        InterfaceManagerCommonUtils.deleteStateEntry(operTx, interfaceName);
                        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces
                            .Interface iface = interfaceManagerCommonUtils.getInterfaceFromConfigDS(interfaceName);

                        if (InterfaceManagerCommonUtils.isTunnelInterface(iface)) {
                            // If this interface is a tunnel interface, remove the tunnel ingress flow and stop LLDP
                            // monitoring
                            interfaceMetaUtils.removeLportTagInterfaceMap(operTx, interfaceName);
                            return Optional.of((InterruptibleCheckedConsumer<TypedReadWriteTransaction<Configuration>,
                                ExecutionException>) confTx -> handleTunnelMonitoringRemoval(confTx, dpId,
                                iface.getName(),
                                iface.augmentation(IfTunnel.class)));
                        }
                        // remove ingress flow only for northbound configured interfaces
                        // skip this check for non-unique ports(Ex: br-int,br-ex)

                        if (iface != null) {
                            FlowBasedServicesUtils.removeIngressFlow(interfaceName, dpId, txRunner, futures);
                            IfmUtil.unbindService(txRunner, coordinator, iface.getName(),
                                    FlowBasedServicesUtils.buildDefaultServiceId(iface.getName()));
                            EVENT_LOGGER.debug("IFM-InterfaceState, REMOVE, IngressFlow {}", interfaceName);
                        }
                        // Delete the Vpn Interface from DpnToInterface Op DS.
                        InterfaceManagerCommonUtils.deleteDpnToInterface(dpId, interfaceName, operTx);
                    }
                    return Optional.empty();
                }).transform((@Nullable Optional<?> optionalJob) -> {
                    if (optionalJob != null && optionalJob.isPresent()) {
                        txChain.callWithNewReadWriteTransactionAndSubmit(CONFIGURATION,
                            (InterruptibleCheckedConsumer<TypedReadWriteTransaction<Configuration>, ?
                                extends Exception>) optionalJob.get());
                    }
                    return null;
                }, MoreExecutors.directExecutor())));
            return futures;
        }

        private void handleTunnelMonitoringRemoval(TypedReadWriteTransaction<Configuration> tx, Uint64 dpId,
            String removedInterfaceName, IfTunnel ifTunnel) throws ExecutionException, InterruptedException {
            interfaceManagerCommonUtils.removeTunnelIngressFlow(tx, ifTunnel, dpId, removedInterfaceName);

            IfmUtil.unbindService(txRunner, coordinator, removedInterfaceName,
                    FlowBasedServicesUtils.buildDefaultServiceId(removedInterfaceName));

            alivenessMonitorUtils.stopLLDPMonitoring(ifTunnel, removedInterfaceName);
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
