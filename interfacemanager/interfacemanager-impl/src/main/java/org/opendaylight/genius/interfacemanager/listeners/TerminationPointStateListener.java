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
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncClusteredDataTreeChangeListenerBase;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.InterfacemgrProvider;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.renderer.ovs.statehelpers.OvsInterfaceTopologyStateUpdateHelper;
import org.opendaylight.genius.interfacemanager.renderer.ovs.utilities.IfmClusterUtils;
import org.opendaylight.ovsdb.utils.southbound.utils.SouthboundUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class TerminationPointStateListener extends AsyncClusteredDataTreeChangeListenerBase<OvsdbTerminationPointAugmentation, TerminationPointStateListener> {
    private static final Logger LOG = LoggerFactory.getLogger(TerminationPointStateListener.class);
    private final DataBroker dataBroker;
    private final InterfacemgrProvider interfaceMgrProvider;

    @Inject
    public TerminationPointStateListener(DataBroker dataBroker,
                                         final InterfacemgrProvider interfaceMgrProvider) {
        this.dataBroker = dataBroker;
        this.interfaceMgrProvider = interfaceMgrProvider;
        this.registerListener(LogicalDatastoreType.OPERATIONAL, this.dataBroker);
    }

    @Override
    protected InstanceIdentifier<OvsdbTerminationPointAugmentation> getWildCardPath() {
        return InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class)
                .child(Node.class).child(TerminationPoint.class).augmentation(OvsdbTerminationPointAugmentation.class).build();
    }

    @Override
    protected TerminationPointStateListener getDataTreeChangeListener() {
        return TerminationPointStateListener.this;
    }

    @Override
    protected void remove(InstanceIdentifier<OvsdbTerminationPointAugmentation> identifier,
                          OvsdbTerminationPointAugmentation tpOld) {
        LOG.debug("Received remove DataChange Notification for ovsdb termination point {}", tpOld.getName());

        String oldInterfaceName = SouthboundUtils.getExternalInterfaceIdValue(tpOld);
        interfaceMgrProvider.removeTerminationPointForInterface(oldInterfaceName);
        if (tpOld.getInterfaceBfdStatus() != null) {
            LOG.debug("Received termination point removed notification with bfd status values {}", tpOld.getName());
            DataStoreJobCoordinator jobCoordinator = DataStoreJobCoordinator.getInstance();
            RendererStateRemoveWorker rendererStateRemoveWorker = new RendererStateRemoveWorker(tpOld);
            jobCoordinator.enqueueJob(tpOld.getName(), rendererStateRemoveWorker);
        }
    }

    @Override
    protected void update(InstanceIdentifier<OvsdbTerminationPointAugmentation> identifier,
                          OvsdbTerminationPointAugmentation tpOld,
                          OvsdbTerminationPointAugmentation tpNew) {
        LOG.debug("Received Update DataChange Notification for ovsdb termination point {}", tpNew.getName());
        if (tpNew.getInterfaceBfdStatus() != null && (tpOld == null ||
                !tpNew.getInterfaceBfdStatus().equals(tpOld.getInterfaceBfdStatus()))) {
            LOG.trace("Bfd Status changed for ovsdb termination point identifier: {},  old: {}, new: {}.",
                    identifier, tpOld, tpNew);
            DataStoreJobCoordinator jobCoordinator = DataStoreJobCoordinator.getInstance();
            RendererStateUpdateWorker rendererStateAddWorker = new RendererStateUpdateWorker(identifier, tpNew);
            jobCoordinator.enqueueJob(tpNew.getName(), rendererStateAddWorker, IfmConstants.JOB_MAX_RETRIES);
        }
        String newInterfaceName = SouthboundUtils.getExternalInterfaceIdValue(tpNew);
        String oldInterfaceName = SouthboundUtils.getExternalInterfaceIdValue(tpOld);
        interfaceMgrProvider.addTerminationPointForInterface(newInterfaceName, tpNew);
        String dpnId = interfaceMgrProvider.getDpidForInterface(newInterfaceName,
                        identifier.firstIdentifierOf(Node.class));
        IfmClusterUtils.runOnlyInLeaderNode(() -> {
            if (dpnId != null && newInterfaceName != null &&
                    (oldInterfaceName == null || !oldInterfaceName.equals(newInterfaceName))) {
                String parentRefName = InterfaceManagerCommonUtils.getPortNameForInterface(dpnId, tpNew.getName());
                LOG.debug("Detected update to termination point {} with external ID {}, updating parent ref "
                        + "of that interface ID to this termination point's interface-state name {}",
                        tpNew.getName(), newInterfaceName, parentRefName);
                interfaceMgrProvider.updateInterfaceParentRef(newInterfaceName, parentRefName);
            }
        });
    }

    @Override
    protected void add(InstanceIdentifier<OvsdbTerminationPointAugmentation> identifier,
                       OvsdbTerminationPointAugmentation tpNew) {
        update(identifier, null, tpNew);
    }

    private class RendererStateUpdateWorker implements Callable<List<ListenableFuture<Void>>> {
        InstanceIdentifier<OvsdbTerminationPointAugmentation> instanceIdentifier;
        OvsdbTerminationPointAugmentation terminationPointNew;


        public RendererStateUpdateWorker(InstanceIdentifier<OvsdbTerminationPointAugmentation> instanceIdentifier,
                                         OvsdbTerminationPointAugmentation tpNew) {
            this.instanceIdentifier = instanceIdentifier;
            this.terminationPointNew = tpNew;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            // If another renderer(for eg : CSS) needs to be supported, check can be performed here
            // to call the respective helpers.
            return OvsInterfaceTopologyStateUpdateHelper.updateTunnelState(dataBroker,
                    terminationPointNew);
        }
    }

    private class RendererStateRemoveWorker implements Callable<List<ListenableFuture<Void>>> {
        OvsdbTerminationPointAugmentation terminationPointOld;


        public RendererStateRemoveWorker(OvsdbTerminationPointAugmentation tpNew) {
            this.terminationPointOld = tpNew;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            LOG.debug("Removing bfd state from cache, if any, for {}", terminationPointOld.getName());
            InterfaceManagerCommonUtils.removeBfdStateFromCache(terminationPointOld.getName());
            return null;
        }
    }
}
