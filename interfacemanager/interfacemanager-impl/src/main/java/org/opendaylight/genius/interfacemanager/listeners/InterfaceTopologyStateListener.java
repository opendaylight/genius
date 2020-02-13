/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.listeners;

import static org.opendaylight.genius.infra.Datastore.CONFIGURATION;
import static org.opendaylight.genius.infra.Datastore.OPERATIONAL;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.aries.blueprint.annotation.service.Reference;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncClusteredDataTreeChangeListenerBase;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.InterfacemgrProvider;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.genius.interfacemanager.recovery.impl.InterfaceServiceRecoveryHandler;
import org.opendaylight.genius.interfacemanager.renderer.ovs.statehelpers.OvsInterfaceTopologyStateUpdateHelper;
import org.opendaylight.genius.interfacemanager.renderer.ovs.utilities.SouthboundUtils;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.serviceutils.srm.RecoverableListener;
import org.opendaylight.serviceutils.srm.ServiceRecoveryRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge._interface.info.BridgeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class InterfaceTopologyStateListener
        extends AsyncClusteredDataTreeChangeListenerBase<OvsdbBridgeAugmentation, InterfaceTopologyStateListener>
        implements RecoverableListener {
    private static final Logger LOG = LoggerFactory.getLogger(InterfaceTopologyStateListener.class);
    private final DataBroker dataBroker;
    private final ManagedNewTransactionRunner txRunner;
    private final InterfacemgrProvider interfaceMgrProvider;
    private final EntityOwnershipUtils entityOwnershipUtils;
    private final JobCoordinator coordinator;
    private final InterfaceManagerCommonUtils interfaceManagerCommonUtils;
    private final OvsInterfaceTopologyStateUpdateHelper ovsInterfaceTopologyStateUpdateHelper;
    private final InterfaceMetaUtils interfaceMetaUtils;
    private final SouthboundUtils southboundUtils;

    @Inject
    public InterfaceTopologyStateListener(@Reference final DataBroker dataBroker,
                                          final InterfacemgrProvider interfaceMgrProvider,
                                          final EntityOwnershipUtils entityOwnershipUtils,
                                          @Reference final JobCoordinator coordinator,
                                          final InterfaceManagerCommonUtils interfaceManagerCommonUtils,
                                          final OvsInterfaceTopologyStateUpdateHelper
                                                      ovsInterfaceTopologyStateUpdateHelper,
                                          final InterfaceMetaUtils interfaceMetaUtils,
                                          final SouthboundUtils southboundUtils,
                                          final InterfaceServiceRecoveryHandler interfaceServiceRecoveryHandler,
                                          @Reference final ServiceRecoveryRegistry serviceRecoveryRegistry) {
        super(OvsdbBridgeAugmentation.class, InterfaceTopologyStateListener.class);
        this.dataBroker = dataBroker;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.interfaceMgrProvider = interfaceMgrProvider;
        this.entityOwnershipUtils = entityOwnershipUtils;
        this.coordinator = coordinator;
        this.interfaceManagerCommonUtils = interfaceManagerCommonUtils;
        this.ovsInterfaceTopologyStateUpdateHelper = ovsInterfaceTopologyStateUpdateHelper;
        this.interfaceMetaUtils = interfaceMetaUtils;
        this.southboundUtils = southboundUtils;
        registerListener();
        serviceRecoveryRegistry.addRecoverableListener(interfaceServiceRecoveryHandler.buildServiceRegistryKey(),
                this);
    }

    @Override
    public void registerListener() {
        this.registerListener(LogicalDatastoreType.OPERATIONAL, dataBroker);
    }

    @Override
    protected InstanceIdentifier<OvsdbBridgeAugmentation> getWildCardPath() {
        return InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class).child(Node.class)
                .augmentation(OvsdbBridgeAugmentation.class).build();
    }

    @Override
    protected InterfaceTopologyStateListener getDataTreeChangeListener() {
        return InterfaceTopologyStateListener.this;
    }

    private void runOnlyInOwnerNode(String jobDesc, Runnable job) {
        entityOwnershipUtils.runOnlyInOwnerNode(IfmConstants.INTERFACE_CONFIG_ENTITY,
                IfmConstants.INTERFACE_CONFIG_ENTITY, coordinator, jobDesc, job);
    }

    @Override
    protected void remove(InstanceIdentifier<OvsdbBridgeAugmentation> identifier,
                          OvsdbBridgeAugmentation bridgeOld) {
        LOG.debug("Received Remove DataChange Notification for identifier: {}, ovsdbBridgeAugmentation: {}",
                identifier, bridgeOld);

        InstanceIdentifier<Node> nodeIid = identifier.firstIdentifierOf(Node.class);
        interfaceMgrProvider.removeBridgeForNodeIid(nodeIid);

        runOnlyInOwnerNode("OVSDB bridge removed", () -> {
            RendererStateRemoveWorker rendererStateRemoveWorker =
                    new RendererStateRemoveWorker(identifier, bridgeOld);
            coordinator.enqueueJob(bridgeOld.getBridgeName().getValue(), rendererStateRemoveWorker,
                IfmConstants.JOB_MAX_RETRIES);
        });
    }

    @Override
    protected void update(InstanceIdentifier<OvsdbBridgeAugmentation> identifier,
                          OvsdbBridgeAugmentation bridgeOld,
                          OvsdbBridgeAugmentation bridgeNew) {
        LOG.debug(
                "Received Update DataChange Notification for identifier: {}, ovsdbBridgeAugmentation old: {}, new: {}.",
                identifier, bridgeOld, bridgeNew);

        InstanceIdentifier<Node> nodeIid = identifier.firstIdentifierOf(Node.class);
        interfaceMgrProvider.addBridgeForNodeIid(nodeIid, bridgeNew);

        runOnlyInOwnerNode("OVSDB bridge updated", () -> {
            DatapathId oldDpid = bridgeOld.getDatapathId();
            DatapathId newDpid = bridgeNew.getDatapathId();
            if (oldDpid == null && newDpid != null) {
                RendererStateAddWorker rendererStateAddWorker = new RendererStateAddWorker(identifier, bridgeNew);
                coordinator.enqueueJob(bridgeNew.getBridgeName().getValue(), rendererStateAddWorker,
                        IfmConstants.JOB_MAX_RETRIES);
            } else if (oldDpid != null && !oldDpid.equals(newDpid)) {
                RendererStateUpdateWorker rendererStateAddWorker =
                        new RendererStateUpdateWorker(identifier, bridgeNew, bridgeOld);
                coordinator.enqueueJob(bridgeNew.getBridgeName().getValue(), rendererStateAddWorker,
                        IfmConstants.JOB_MAX_RETRIES);
            }
        });
    }

    @Override
    protected void add(InstanceIdentifier<OvsdbBridgeAugmentation> identifier,
                       OvsdbBridgeAugmentation bridgeNew) {
        LOG.debug("Received Add DataChange Notification for identifier: {}, ovsdbBridgeAugmentation: {}",
                identifier, bridgeNew);

        InstanceIdentifier<Node> nodeIid = identifier.firstIdentifierOf(Node.class);
        interfaceMgrProvider.addBridgeForNodeIid(nodeIid, bridgeNew);

        runOnlyInOwnerNode("OVSDB bridge added", () -> {
            RendererStateAddWorker rendererStateAddWorker = new RendererStateAddWorker(identifier, bridgeNew);
            coordinator.enqueueJob(bridgeNew.getBridgeName().getValue(), rendererStateAddWorker,
                IfmConstants.JOB_MAX_RETRIES);
        });
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
        public List<ListenableFuture<Void>> call() {
            if (bridgeNew.getDatapathId() == null) {
                LOG.info("DataPathId found as null for Bridge Augmentation: {}... returning...", bridgeNew);
                return Collections.emptyList();
            }
            Uint64 dpnId = IfmUtil.getDpnId(bridgeNew.getDatapathId());
            LOG.debug("adding bridge references for bridge: {}, dpn: {}", bridgeNew, dpnId);
            // create bridge reference entry in interface meta operational DS
            return Collections.singletonList(txRunner.callWithNewWriteOnlyTransactionAndSubmit(OPERATIONAL, tx -> {
                InterfaceMetaUtils.createBridgeRefEntry(dpnId, instanceIdentifier, tx);

                // handle pre-provisioning of tunnels for the newly connected dpn
                BridgeEntry bridgeEntry = interfaceMetaUtils.getBridgeEntryFromConfigDS(dpnId);
                if (bridgeEntry == null) {
                    LOG.debug("Bridge entry not found in config DS for dpn: {}", dpnId);
                } else {
                    southboundUtils.addAllPortsToBridge(bridgeEntry, interfaceManagerCommonUtils, instanceIdentifier,
                            bridgeNew);
                }
            }));
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
        public List<ListenableFuture<Void>> call() {
            Uint64 dpnId = IfmUtil.getDpnId(bridgeNew.getDatapathId());

            if (dpnId == null) {
                LOG.warn("Got Null DPID for Bridge: {}", bridgeNew);
                return Collections.emptyList();
            }

            List<ListenableFuture<Void>> futures = new ArrayList<>();
            LOG.debug("removing bridge references for bridge: {}, dpn: {}", bridgeNew, dpnId);
            // TODO skitt Use a transaction chain
            futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(OPERATIONAL, tx -> {
                // delete bridge reference entry in interface meta operational DS
                InterfaceMetaUtils.deleteBridgeRefEntry(dpnId, tx);
            }));
            futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION, tx -> {
                // the bridge reference is copied to dpn-tunnel interfaces map, so that
                // whenever a northbound delete
                // happens when bridge is not connected, we need the bridge reference to
                // clean up the topology config DS
                InterfaceMetaUtils.addBridgeRefToBridgeInterfaceEntry(dpnId, new OvsdbBridgeRef(instanceIdentifier),
                    tx);
            }));
            return futures;
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
        public List<ListenableFuture<Void>> call() {
            return ovsInterfaceTopologyStateUpdateHelper.updateBridgeRefEntry(instanceIdentifier, bridgeNew, bridgeOld);
        }
    }
}
