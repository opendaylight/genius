/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.listeners;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.Callable;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.aries.blueprint.annotation.service.Reference;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncClusteredDataTreeChangeListenerBase;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.InterfacemgrProvider;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.recovery.impl.InterfaceServiceRecoveryHandler;
import org.opendaylight.genius.interfacemanager.renderer.ovs.statehelpers.OvsInterfaceTopologyStateUpdateHelper;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.ovsdb.utils.southbound.utils.SouthboundUtils;
import org.opendaylight.serviceutils.srm.RecoverableListener;
import org.opendaylight.serviceutils.srm.ServiceRecoveryRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class TerminationPointStateListener extends
        AsyncClusteredDataTreeChangeListenerBase<OvsdbTerminationPointAugmentation, TerminationPointStateListener>
        implements RecoverableListener {
    private static final Logger LOG = LoggerFactory.getLogger(TerminationPointStateListener.class);
    private static final Logger EVENT_LOGGER = LoggerFactory.getLogger("GeniusEventLogger");

    private final InterfacemgrProvider interfaceMgrProvider;
    private final DataBroker dataBroker;
    private final EntityOwnershipUtils entityOwnershipUtils;
    private final JobCoordinator coordinator;
    private final InterfaceManagerCommonUtils interfaceManagerCommonUtils;
    private final OvsInterfaceTopologyStateUpdateHelper ovsInterfaceTopologyStateUpdateHelper;

    @Inject
    public TerminationPointStateListener(@Reference final DataBroker dataBroker,
                                         final InterfacemgrProvider interfaceMgrProvider,
                                         final EntityOwnershipUtils entityOwnershipUtils,
                                         @Reference final JobCoordinator coordinator,
                                         final InterfaceManagerCommonUtils interfaceManagerCommonUtils,
                                         final OvsInterfaceTopologyStateUpdateHelper
                                                     ovsInterfaceTopologyStateUpdateHelper,
                                         final InterfaceServiceRecoveryHandler interfaceServiceRecoveryHandler,
                                         @Reference final ServiceRecoveryRegistry serviceRecoveryRegistry) {
        this.interfaceMgrProvider = interfaceMgrProvider;
        this.entityOwnershipUtils = entityOwnershipUtils;
        this.coordinator = coordinator;
        this.interfaceManagerCommonUtils = interfaceManagerCommonUtils;
        this.ovsInterfaceTopologyStateUpdateHelper = ovsInterfaceTopologyStateUpdateHelper;
        this.dataBroker = dataBroker;
        registerListener();
        serviceRecoveryRegistry.addRecoverableListener(interfaceServiceRecoveryHandler.buildServiceRegistryKey(),
                this);
    }

    @Override
    public void registerListener() {
        this.registerListener(LogicalDatastoreType.OPERATIONAL, dataBroker);
    }

    @Override
    protected InstanceIdentifier<OvsdbTerminationPointAugmentation> getWildCardPath() {
        return InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class).child(Node.class)
                .child(TerminationPoint.class).augmentation(OvsdbTerminationPointAugmentation.class).build();
    }

    @Override
    protected TerminationPointStateListener getDataTreeChangeListener() {
        return TerminationPointStateListener.this;
    }

    @Override
    protected void remove(InstanceIdentifier<OvsdbTerminationPointAugmentation> identifier,
                          OvsdbTerminationPointAugmentation tpOld) {
        // No ItmDirectTunnels or Internal Tunnel checking is done here as this DTCN only results in removal
        // of interface entry from BFD internal cache. For internal tunnels when ItmDirectTunnel is enabled,
        // the entry won't be in cache, but ConcurrentHashMap will just ignore when key is not present.

        LOG.debug("Received remove DataChange Notification for ovsdb termination point {}", tpOld.getName());

        String oldInterfaceName = SouthboundUtils.getExternalInterfaceIdValue(tpOld);
        if (oldInterfaceName == null && InterfaceManagerCommonUtils.isTunnelPort(tpOld.getName())) {
            interfaceMgrProvider.removeTerminationPointForInterface(tpOld.getName());
            interfaceMgrProvider.removeNodeIidForInterface(tpOld.getName());
        } else {
            interfaceMgrProvider.removeTerminationPointForInterface(oldInterfaceName);
            interfaceMgrProvider.removeNodeIidForInterface(oldInterfaceName);
        }
        if (tpOld.getInterfaceBfdStatus() != null) {
            LOG.debug("Received termination point removed notification with bfd status values {}", tpOld.getName());
            EVENT_LOGGER.debug("IFM-TerminationPointState,REMOVE {}", tpOld.getName());
            RendererStateRemoveWorker rendererStateRemoveWorker = new RendererStateRemoveWorker(tpOld);
            coordinator.enqueueJob(tpOld.getName(), rendererStateRemoveWorker);
        }
    }

    @Override
    protected void update(InstanceIdentifier<OvsdbTerminationPointAugmentation> identifier,
                          OvsdbTerminationPointAugmentation tpOld,
                          OvsdbTerminationPointAugmentation tpNew) {
        if (interfaceMgrProvider.isItmDirectTunnelsEnabled()
            && InterfaceManagerCommonUtils.isTunnelPort(tpNew.getName())
            && interfaceManagerCommonUtils.getInterfaceFromConfigDS(tpNew.getName()) == null) {
            LOG.debug("ITM Direct Tunnels is enabled, hence ignoring termination point update - "
                    + "old {}, new {} internal tunnel", tpOld.getName(), tpNew.getName());
            return;
        }


        LOG.debug("Received Update DataChange Notification for ovsdb termination point {}", tpNew.getName());
        EVENT_LOGGER.debug("IFM-TerminationPointState,UPDATE {}", tpNew.getName());
        if (org.opendaylight.genius.interfacemanager.renderer.ovs.utilities
                .SouthboundUtils.changeInBfdMonitoringDetected(tpOld, tpNew)
                || org.opendaylight.genius.interfacemanager.renderer.ovs.utilities
                .SouthboundUtils.ifBfdStatusNotEqual(tpOld, tpNew)) {
            LOG.info("Bfd Status changed for ovsdb termination point {}", tpNew.getName());
            LOG.debug("Bfd Status changed for ovsdb termination point identifier: {},  old: {}, new: {}",
                    identifier, tpOld, tpNew);
            RendererStateUpdateWorker rendererStateAddWorker = new RendererStateUpdateWorker(tpNew);
            coordinator.enqueueJob(tpNew.getName(), rendererStateAddWorker, IfmConstants.JOB_MAX_RETRIES);
        }

        InstanceIdentifier<Node> nodeIid = identifier.firstIdentifierOf(Node.class);
        String newInterfaceName = SouthboundUtils.getExternalInterfaceIdValue(tpNew);
        if (newInterfaceName == null && InterfaceManagerCommonUtils.isTunnelPort(tpNew.getName())) {
            interfaceMgrProvider.addTerminationPointForInterface(tpNew.getName(), tpNew);
            interfaceMgrProvider.addNodeIidForInterface(tpNew.getName(), nodeIid);
        } else {
            interfaceMgrProvider.addTerminationPointForInterface(newInterfaceName, tpNew);
            interfaceMgrProvider.addNodeIidForInterface(newInterfaceName, nodeIid);
        }

        // skip parent-refs updation for interfaces with external-id for tunnels
        if (!org.opendaylight.genius.interfacemanager.renderer.ovs.utilities.SouthboundUtils.isInterfaceTypeTunnel(
            tpNew.getInterfaceType())) {
            if (!entityOwnershipUtils.isEntityOwner(IfmConstants.INTERFACE_CONFIG_ENTITY,
                    IfmConstants.INTERFACE_CONFIG_ENTITY)) {
                return;
            }
            String dpnId = interfaceMgrProvider.getDpidForInterface(newInterfaceName, nodeIid);
            String oldInterfaceName = SouthboundUtils.getExternalInterfaceIdValue(tpOld);
            if (dpnId != null && newInterfaceName != null && (oldInterfaceName == null
                || !oldInterfaceName.equals(newInterfaceName))) {
                String parentRefName =
                        InterfaceManagerCommonUtils.getPortNameForInterface(dpnId, tpNew.getName());
                LOG.debug("Detected update to termination point {} with external ID {}, updating parent ref "
                    + "of that interface ID to this termination point's interface-state name {}", tpNew.getName(),
                    newInterfaceName, parentRefName);
                interfaceMgrProvider.updateInterfaceParentRef(newInterfaceName, parentRefName);
            }
        }
    }

    @Override
    protected void add(InstanceIdentifier<OvsdbTerminationPointAugmentation> identifier,
                       OvsdbTerminationPointAugmentation tpNew) {
        if (interfaceMgrProvider.isItmDirectTunnelsEnabled()
            && InterfaceManagerCommonUtils.isTunnelPort(tpNew.getName())
            && interfaceManagerCommonUtils.getInterfaceFromConfigDS(tpNew.getName()) == null) {
            LOG.debug("ITM Direct Tunnels is enabled, hence ignoring termination point add for"
                    + " internal tunnel {}", tpNew.getName());
            return;
        }
        update(identifier, null, tpNew);
    }

    private class RendererStateUpdateWorker implements Callable<List<ListenableFuture<Void>>> {
        OvsdbTerminationPointAugmentation terminationPointNew;

        RendererStateUpdateWorker(OvsdbTerminationPointAugmentation tpNew) {
            this.terminationPointNew = tpNew;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            return ovsInterfaceTopologyStateUpdateHelper.updateTunnelState(terminationPointNew);
        }
    }

    private class RendererStateRemoveWorker implements Callable<List<ListenableFuture<Void>>> {
        OvsdbTerminationPointAugmentation terminationPointOld;

        RendererStateRemoveWorker(OvsdbTerminationPointAugmentation tpNew) {
            this.terminationPointOld = tpNew;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            LOG.debug("Removing bfd state from cache, if any, for {}", terminationPointOld.getName());
            interfaceManagerCommonUtils.removeBfdStateFromCache(terminationPointOld.getName());
            return null;
        }
    }
}
