/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.listeners;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.genius.datastoreutils.AsyncDataChangeListenerBase;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.interfacemanager.renderer.hwvtep.statehelpers.HwVTEPInterfaceStateUpdateHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.Tunnels;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Callable;

public class HwVTEPTunnelsStateListener extends AsyncDataChangeListenerBase<Tunnels, HwVTEPTunnelsStateListener> {
    private static final Logger LOG = LoggerFactory.getLogger(HwVTEPTunnelsStateListener.class);
    private DataBroker dataBroker;

    public HwVTEPTunnelsStateListener(DataBroker dataBroker) {
        super(Tunnels.class, HwVTEPTunnelsStateListener.class);
        this.dataBroker = dataBroker;
    }

    @Override
    protected InstanceIdentifier<Tunnels> getWildCardPath() {
        return InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class)
                .child(Node.class).augmentation(PhysicalSwitchAugmentation.class).child(Tunnels.class).build();
    }

    @Override
    protected DataChangeListener getDataChangeListener() {
        return HwVTEPTunnelsStateListener.this;
    }

    @Override
    protected AsyncDataBroker.DataChangeScope getDataChangeScope() {
        return AsyncDataBroker.DataChangeScope.BASE;
    }

    @Override
    protected void remove(InstanceIdentifier<Tunnels> identifier, Tunnels tunnelOld) {
        LOG.info("Received Remove DataChange Notification for identifier: {}, physicalSwitchAugmentation: {}",
                identifier, tunnelOld);
    }

    @Override
    protected void update(InstanceIdentifier<Tunnels> identifier, Tunnels tunnelOld,
                          Tunnels tunnelNew) {
        LOG.info("Received Update Tunnel Update Notification for identifier: {}", identifier);
        DataStoreJobCoordinator jobCoordinator = DataStoreJobCoordinator.getInstance();
        RendererStateUpdateWorker rendererStateUpdateWorker = new RendererStateUpdateWorker(identifier, tunnelNew, tunnelOld);
        jobCoordinator.enqueueJob(tunnelNew.getTunnelUuid().getValue(), rendererStateUpdateWorker);
    }

    @Override
    protected void add(InstanceIdentifier<Tunnels> identifier, Tunnels tunnelNew) {
        LOG.info("Received Add DataChange Notification for identifier: {}, tunnels: {}",
                identifier, tunnelNew);
        DataStoreJobCoordinator jobCoordinator = DataStoreJobCoordinator.getInstance();
        RendererStateAddWorker rendererStateAddWorker = new RendererStateAddWorker(identifier, tunnelNew);
        jobCoordinator.enqueueJob(tunnelNew.getTunnelUuid().getValue(), rendererStateAddWorker);
    }

    private class RendererStateUpdateWorker implements Callable<List<ListenableFuture<Void>>> {
        InstanceIdentifier<Tunnels> instanceIdentifier;
        Tunnels tunnelsNew;
        Tunnels tunnelsOld;

        public RendererStateUpdateWorker(InstanceIdentifier<Tunnels> instanceIdentifier,
                                         Tunnels tunnelsNew, Tunnels tunnelsOld) {
            this.instanceIdentifier = instanceIdentifier;
            this.tunnelsNew = tunnelsNew;
            this.tunnelsOld = tunnelsOld;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            // If another renderer(for eg : CSS) needs to be supported, check can be performed here
            // to call the respective helpers.
            return HwVTEPInterfaceStateUpdateHelper.updatePhysicalSwitch(dataBroker,
                    instanceIdentifier, tunnelsOld, tunnelsNew);
        }
    }

    private class RendererStateAddWorker implements Callable<List<ListenableFuture<Void>>> {
        InstanceIdentifier<Tunnels> instanceIdentifier;
        Tunnels tunnelsNew;

        public RendererStateAddWorker(InstanceIdentifier<Tunnels> instanceIdentifier,
                                         Tunnels tunnelsNew) {
            this.instanceIdentifier = instanceIdentifier;
            this.tunnelsNew = tunnelsNew;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            // If another renderer(for eg : CSS) needs to be supported, check can be performed here
            // to call the respective helpers.
            return HwVTEPInterfaceStateUpdateHelper.startBfdMonitoring(dataBroker, instanceIdentifier, tunnelsNew);
        }
    }
}