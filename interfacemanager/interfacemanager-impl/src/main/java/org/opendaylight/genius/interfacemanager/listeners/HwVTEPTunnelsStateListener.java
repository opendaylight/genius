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
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.datastoreutils.hwvtep.HwvtepAbstractDataTreeChangeListener;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.renderer.hwvtep.statehelpers.HwVTEPInterfaceStateRemoveHelper;
import org.opendaylight.genius.interfacemanager.renderer.hwvtep.statehelpers.HwVTEPInterfaceStateUpdateHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.Tunnels;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class HwVTEPTunnelsStateListener
        extends HwvtepAbstractDataTreeChangeListener<Tunnels, HwVTEPTunnelsStateListener> {
    private static final Logger LOG = LoggerFactory.getLogger(HwVTEPTunnelsStateListener.class);
    private final DataBroker dataBroker;
    private final ManagedNewTransactionRunner txRunner;

    @Inject
    public HwVTEPTunnelsStateListener(final DataBroker dataBroker) {
        super(Tunnels.class, HwVTEPTunnelsStateListener.class);
        this.dataBroker = dataBroker;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.registerListener(LogicalDatastoreType.OPERATIONAL, dataBroker);
    }

    @Override
    protected InstanceIdentifier<Tunnels> getWildCardPath() {
        return InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class).child(Node.class)
                .augmentation(PhysicalSwitchAugmentation.class).child(Tunnels.class).build();
    }

    @Override
    protected HwVTEPTunnelsStateListener getDataTreeChangeListener() {
        return HwVTEPTunnelsStateListener.this;
    }

    @Override
    protected void removed(InstanceIdentifier<Tunnels> identifier, Tunnels tunnel) {
        LOG.debug("Received Remove DataChange Notification for identifier: {}, physicalSwitchAugmentation: {}",
                identifier, tunnel);
        DataStoreJobCoordinator jobCoordinator = DataStoreJobCoordinator.getInstance();
        RendererStateRemoveWorker rendererStateRemoveWorker = new RendererStateRemoveWorker(identifier, tunnel);
        jobCoordinator.enqueueJob(tunnel.getTunnelUuid().getValue(), rendererStateRemoveWorker,
                IfmConstants.JOB_MAX_RETRIES);
    }

    @Override
    protected void updated(InstanceIdentifier<Tunnels> identifier, Tunnels tunnelOld, Tunnels tunnelNew) {
        LOG.debug("Received Update Tunnel Update Notification for identifier: {}", identifier);
        DataStoreJobCoordinator jobCoordinator = DataStoreJobCoordinator.getInstance();
        RendererStateUpdateWorker rendererStateUpdateWorker = new RendererStateUpdateWorker(identifier, tunnelNew,
                tunnelOld);
        jobCoordinator.enqueueJob(tunnelNew.getTunnelUuid().getValue(), rendererStateUpdateWorker,
                IfmConstants.JOB_MAX_RETRIES);
    }

    @Override
    protected void added(InstanceIdentifier<Tunnels> identifier, Tunnels tunnelNew) {
        LOG.debug("Received Add DataChange Notification for identifier: {}, tunnels: {}", identifier, tunnelNew);
        DataStoreJobCoordinator jobCoordinator = DataStoreJobCoordinator.getInstance();
        RendererStateAddWorker rendererStateAddWorker = new RendererStateAddWorker(identifier, tunnelNew);
        jobCoordinator.enqueueJob(tunnelNew.getTunnelUuid().getValue(), rendererStateAddWorker,
                IfmConstants.JOB_MAX_RETRIES);
    }

    private class RendererStateUpdateWorker implements Callable<List<ListenableFuture<Void>>> {
        InstanceIdentifier<Tunnels> instanceIdentifier;
        Tunnels tunnelsNew;
        Tunnels tunnelsOld;

        RendererStateUpdateWorker(InstanceIdentifier<Tunnels> instanceIdentifier, Tunnels tunnelsNew,
                Tunnels tunnelsOld) {
            this.instanceIdentifier = instanceIdentifier;
            this.tunnelsNew = tunnelsNew;
            this.tunnelsOld = tunnelsOld;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            return HwVTEPInterfaceStateUpdateHelper.updatePhysicalSwitch(dataBroker, txRunner, instanceIdentifier,
                    tunnelsOld, tunnelsNew);
        }
    }

    private class RendererStateAddWorker implements Callable<List<ListenableFuture<Void>>> {
        InstanceIdentifier<Tunnels> instanceIdentifier;
        Tunnels tunnelsNew;

        RendererStateAddWorker(InstanceIdentifier<Tunnels> instanceIdentifier, Tunnels tunnelsNew) {
            this.instanceIdentifier = instanceIdentifier;
            this.tunnelsNew = tunnelsNew;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            return HwVTEPInterfaceStateUpdateHelper.startBfdMonitoring(txRunner, instanceIdentifier, tunnelsNew);
        }
    }

    private class RendererStateRemoveWorker implements Callable<List<ListenableFuture<Void>>> {
        InstanceIdentifier<Tunnels> instanceIdentifier;
        Tunnels tunnel;

        RendererStateRemoveWorker(InstanceIdentifier<Tunnels> instanceIdentifier, Tunnels tunnel) {
            this.instanceIdentifier = instanceIdentifier;
            this.tunnel = tunnel;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            return HwVTEPInterfaceStateRemoveHelper.removeExternalTunnel(txRunner, instanceIdentifier);
        }
    }
}
