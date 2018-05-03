/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.listeners;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.recovery.impl.InterfaceServiceRecoveryHandler;
import org.opendaylight.genius.interfacemanager.renderer.hwvtep.statehelpers.HwVTEPInterfaceStateRemoveHelper;
import org.opendaylight.genius.interfacemanager.renderer.hwvtep.statehelpers.HwVTEPInterfaceStateUpdateHelper;
import org.opendaylight.genius.srm.RecoverableListener;
import org.opendaylight.genius.srm.ServiceRecoveryRegistry;
import org.opendaylight.genius.tools.mdsal.listener.AbstractSyncDataTreeChangeListener;
import org.opendaylight.genius.utils.hwvtep.HwvtepNodeHACache;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.Tunnels;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class HwVTEPTunnelsStateListener extends AbstractSyncDataTreeChangeListener<Tunnels> implements
        RecoverableListener {

    private static final Logger LOG = LoggerFactory.getLogger(HwVTEPTunnelsStateListener.class);

    private final ManagedNewTransactionRunner txRunner;
    private final JobCoordinator coordinator;

    @Inject
    public HwVTEPTunnelsStateListener(DataBroker dataBroker,
                                      JobCoordinator coordinator,
                                      InterfaceServiceRecoveryHandler interfaceServiceRecoveryHandler,
                                      ServiceRecoveryRegistry serviceRecoveryRegistry,
                                      HwvtepNodeHACache hwvtepNodeHACache) {
        super(dataBroker, LogicalDatastoreType.OPERATIONAL,
              InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class).child(Node.class)
                      .augmentation(PhysicalSwitchAugmentation.class).child(Tunnels.class).build());
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.coordinator = coordinator;
        registerListener();
        serviceRecoveryRegistry.addRecoverableListener(interfaceServiceRecoveryHandler.buildServiceRegistryKey(),
                this);
    }

    @Override
    public void remove(@Nonnull InstanceIdentifier<Tunnels> instanceIdentifier, @Nonnull Tunnels tunnel) {
        LOG.debug("Received Remove DataChange Notification for identifier: {}, physicalSwitchAugmentation: {}",
                  instanceIdentifier, tunnel);
        coordinator.enqueueJob(tunnel.getTunnelUuid().getValue(), () -> HwVTEPInterfaceStateRemoveHelper
                .removeExternalTunnel(txRunner, instanceIdentifier), IfmConstants.JOB_MAX_RETRIES);
    }

    @Override
    public void update(@Nonnull InstanceIdentifier<Tunnels> instanceIdentifier, @Nonnull Tunnels tunnelOld,
                       @Nonnull Tunnels tunnelNew) {
        LOG.debug("Received Update Tunnel Update Notification for identifier: {}", instanceIdentifier);
        coordinator.enqueueJob(tunnelNew.getTunnelUuid().getValue(), () -> HwVTEPInterfaceStateUpdateHelper
                .updatePhysicalSwitch(txRunner, instanceIdentifier, tunnelOld), IfmConstants.JOB_MAX_RETRIES);
    }

    @Override
    public void add(@Nonnull InstanceIdentifier<Tunnels> instanceIdentifier, @Nonnull Tunnels tunnelNew) {
        LOG.debug("Received Add DataChange Notification for identifier: {}, tunnels: {}", instanceIdentifier,
                  tunnelNew);
        coordinator.enqueueJob(tunnelNew.getTunnelUuid().getValue(), () -> HwVTEPInterfaceStateUpdateHelper
                .startBfdMonitoring(txRunner, instanceIdentifier, tunnelNew), IfmConstants.JOB_MAX_RETRIES);
    }

    @Override
    public void registerListener() {
        super.register();
    }

    @Override
    public void deregisterListener() {
        close();
    }
}
