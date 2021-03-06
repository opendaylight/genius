/*
 * Copyright (c) 2016, 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.listeners;

import static org.opendaylight.genius.interfacemanager.renderer.hwvtep.utilities.SouthboundUtils.createGlobalNodeInstanceIdentifier;
import static org.opendaylight.genius.interfacemanager.renderer.hwvtep.utilities.SouthboundUtils.createPhysicalSwitchInstanceIdentifier;

import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.aries.blueprint.annotation.service.Reference;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.renderer.hwvtep.confighelpers.HwVTEPConfigRemoveHelper;
import org.opendaylight.genius.interfacemanager.renderer.hwvtep.confighelpers.HwVTEPInterfaceConfigAddHelper;
import org.opendaylight.genius.interfacemanager.renderer.hwvtep.confighelpers.HwVTEPInterfaceConfigUpdateHelper;
import org.opendaylight.genius.interfacemanager.renderer.hwvtep.utilities.SouthboundUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.util.ManagedNewTransactionRunner;
import org.opendaylight.mdsal.binding.util.ManagedNewTransactionRunnerImpl;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.serviceutils.tools.listener.AbstractSyncDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.interfaces._interface.NodeIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class HwVTEPConfigListener extends AbstractSyncDataTreeChangeListener<Interface> {

    private static final Logger LOG = LoggerFactory.getLogger(HwVTEPConfigListener.class);

    private final ManagedNewTransactionRunner txRunner;
    private final JobCoordinator coordinator;

    @Inject
    public HwVTEPConfigListener(@Reference final DataBroker dataBroker, @Reference final JobCoordinator coordinator) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION,
              InstanceIdentifier.create(Interfaces.class).child(Interface.class));
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.coordinator = coordinator;
    }

    @Override
    public void remove(@NonNull InstanceIdentifier<Interface> instanceIdentifier, @NonNull Interface removedInterface) {
        // HwVTEPs support only VXLAN
        IfTunnel ifTunnel = removedInterface.augmentation(IfTunnel.class);
        if (ifTunnel != null && ifTunnel.getTunnelInterfaceType().isAssignableFrom(TunnelTypeVxlan.class)) {
            ParentRefs parentRefs = removedInterface.augmentation(ParentRefs.class);
            if (parentRefs != null && parentRefs.getNodeIdentifier() != null) {
                LOG.debug("Received HwVTEP Interface Remove Event: {}", removedInterface.getName());
                LOG.trace("Received HwVTEP Interface Remove Event: {}", removedInterface);
                for (NodeIdentifier nodeIdentifier : parentRefs.getNodeIdentifier().values()) {
                    if (SouthboundUtils.HWVTEP_TOPOLOGY.equals(nodeIdentifier.getTopologyId())) {
                        coordinator.enqueueJob(removedInterface.getName(), () -> HwVTEPConfigRemoveHelper
                                                       .removeConfiguration(txRunner, removedInterface,
                                                                            createPhysicalSwitchInstanceIdentifier(
                                                                                    nodeIdentifier.getNodeId()),
                                                                            createGlobalNodeInstanceIdentifier(
                                                                                    nodeIdentifier.getNodeId())),
                                               IfmConstants.JOB_MAX_RETRIES);
                    }
                }
            }
        }
    }

    @Override
    public void update(@NonNull InstanceIdentifier<Interface> instanceIdentifier, @NonNull Interface originalInterface,
                       @NonNull Interface updatedInterface) {
        // HwVTEPs support only VXLAN
        IfTunnel ifTunnel = originalInterface.augmentation(IfTunnel.class);
        if (ifTunnel != null && ifTunnel.getTunnelInterfaceType().isAssignableFrom(TunnelTypeVxlan.class)) {
            ParentRefs parentRefs = originalInterface.augmentation(ParentRefs.class);
            if (parentRefs != null && parentRefs.getNodeIdentifier() != null) {
                LOG.debug("Received HwVTEP Interface Update Event: {}", originalInterface.getName());
                LOG.trace("Received HwVTEP Interface Update Event: updatedInterface: {}, OriginalInterface: {}",
                        updatedInterface, originalInterface);
                for (NodeIdentifier nodeIdentifier : parentRefs.getNodeIdentifier().values()) {
                    if (SouthboundUtils.HWVTEP_TOPOLOGY.equals(nodeIdentifier.getTopologyId())) {
                        coordinator.enqueueJob(originalInterface.getName(), () -> HwVTEPInterfaceConfigUpdateHelper
                                .updateConfiguration(txRunner,
                                                     createPhysicalSwitchInstanceIdentifier(nodeIdentifier.getNodeId()),
                                                     createGlobalNodeInstanceIdentifier(nodeIdentifier.getNodeId()),
                                                     updatedInterface, ifTunnel), IfmConstants.JOB_MAX_RETRIES);
                    }
                }
            }
        }
    }

    @Override
    public void add(@NonNull InstanceIdentifier<Interface> instanceIdentifier, @NonNull Interface newInterface) {
        // HwVTEPs support only VXLAN
        IfTunnel ifTunnel = newInterface.augmentation(IfTunnel.class);
        if (ifTunnel != null && ifTunnel.getTunnelInterfaceType().isAssignableFrom(TunnelTypeVxlan.class)) {
            ParentRefs parentRefs = newInterface.augmentation(ParentRefs.class);
            if (parentRefs != null && parentRefs.getNodeIdentifier() != null) {
                LOG.debug("Received HwVTEP Interface Add Event: {}", newInterface.getName());
                LOG.trace("Received HwVTEP Interface Add Event: {}", newInterface);
                for (NodeIdentifier nodeIdentifier : parentRefs.getNodeIdentifier().values()) {
                    if (SouthboundUtils.HWVTEP_TOPOLOGY.equals(nodeIdentifier.getTopologyId())) {
                        coordinator.enqueueJob(newInterface.getName(), () -> HwVTEPInterfaceConfigAddHelper
                                .addConfiguration(txRunner,
                                                  createPhysicalSwitchInstanceIdentifier(nodeIdentifier.getNodeId()),
                                                  createGlobalNodeInstanceIdentifier(nodeIdentifier.getNodeId()),
                                                  newInterface, ifTunnel), IfmConstants.JOB_MAX_RETRIES);
                    }
                }
            }
        }
    }
}
