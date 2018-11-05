/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.listeners;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.listeners.DataTreeEventCallbackRegistrar;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.renderer.ovs.confighelpers.OvsVlanMemberConfigAddHelper;
import org.opendaylight.genius.interfacemanager.renderer.ovs.confighelpers.OvsVlanMemberConfigRemoveHelper;
import org.opendaylight.genius.interfacemanager.renderer.ovs.confighelpers.OvsVlanMemberConfigUpdateHelper;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.serviceutils.tools.mdsal.listener.AbstractSyncDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class VlanMemberConfigListener extends AbstractSyncDataTreeChangeListener<Interface> {

    private static final Logger LOG = LoggerFactory.getLogger(VlanMemberConfigListener.class);

    private final JobCoordinator coordinator;
    private final OvsVlanMemberConfigAddHelper ovsVlanMemberConfigAddHelper;
    private final OvsVlanMemberConfigRemoveHelper ovsVlanMemberConfigRemoveHelper;
    private final OvsVlanMemberConfigUpdateHelper ovsVlanMemberConfigUpdateHelper;
    private final DataTreeEventCallbackRegistrar eventCallbacks;
    private final ManagedNewTransactionRunner txRunner;

    @Inject
    public VlanMemberConfigListener(final DataBroker dataBroker,
            final JobCoordinator coordinator,
            final OvsVlanMemberConfigAddHelper ovsVlanMemberConfigAddHelper,
            final OvsVlanMemberConfigRemoveHelper ovsVlanMemberConfigRemoveHelper,
            final OvsVlanMemberConfigUpdateHelper ovsVlanMemberConfigUpdateHelper,
            final DataTreeEventCallbackRegistrar eventCallbacks) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION,
              InstanceIdentifier.create(Interfaces.class).child(Interface.class));
        this.coordinator = coordinator;
        this.ovsVlanMemberConfigAddHelper = ovsVlanMemberConfigAddHelper;
        this.ovsVlanMemberConfigRemoveHelper = ovsVlanMemberConfigRemoveHelper;
        this.ovsVlanMemberConfigUpdateHelper = ovsVlanMemberConfigUpdateHelper;
        this.eventCallbacks = eventCallbacks;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
    }

    @Override
    public void remove(@Nonnull InstanceIdentifier<Interface> instanceIdentifier, @Nonnull Interface removedInterface) {
        IfL2vlan ifL2vlan = removedInterface.augmentation(IfL2vlan.class);
        if (ifL2vlan == null || IfL2vlan.L2vlanMode.TrunkMember != ifL2vlan.getL2vlanMode()) {
            return;
        }
        removeVlanMember(removedInterface);
    }

    private void removeVlanMember(Interface removedInterface) {
        ParentRefs parentRefs = removedInterface.augmentation(ParentRefs.class);
        if (parentRefs == null) {
            LOG.error("Attempt to remove Vlan Trunk-Member {} without a parent interface", removedInterface);
            return;
        }

        String lowerLayerIf = parentRefs.getParentInterface();
        if (Objects.equals(lowerLayerIf, removedInterface.getName())) {
            LOG.error("Attempt to remove Vlan Trunk-Member {} with same parent interface name.", removedInterface);
            return;
        }
        coordinator.enqueueJob(lowerLayerIf,
            () -> ovsVlanMemberConfigRemoveHelper.removeConfiguration(parentRefs, removedInterface),
                               IfmConstants.JOB_MAX_RETRIES);
    }

    @Override
    public void update(@Nonnull InstanceIdentifier<Interface> instanceIdentifier,
                       @Nonnull Interface originalInterface, @Nonnull Interface updatedInterface) {
        IfL2vlan ifL2vlanNew = updatedInterface.augmentation(IfL2vlan.class);
        if (ifL2vlanNew == null) {
            return;
        }
        IfL2vlan ifL2vlanOld = originalInterface.augmentation(IfL2vlan.class);
        if (IfL2vlan.L2vlanMode.TrunkMember == ifL2vlanNew.getL2vlanMode()
                && IfL2vlan.L2vlanMode.Trunk == ifL2vlanOld.getL2vlanMode()) {
            // Trunk subport add use case
            addVlanMember(updatedInterface);
            return;
        } else if (IfL2vlan.L2vlanMode.Trunk == ifL2vlanNew.getL2vlanMode()
                && IfL2vlan.L2vlanMode.TrunkMember == ifL2vlanOld.getL2vlanMode()) {
            // Trunk subport remove use case
            removeVlanMember(originalInterface);
            return;
        } else if (IfL2vlan.L2vlanMode.TrunkMember != ifL2vlanNew.getL2vlanMode()) {
            return;
        }

        ParentRefs parentRefsNew = updatedInterface.augmentation(ParentRefs.class);
        if (parentRefsNew == null) {
            LOG.error("Configuration Error. Attempt to update Vlan Trunk-Member {} without a " + "parent interface",
                      updatedInterface);
            return;
        }

        String lowerLayerIf = parentRefsNew.getParentInterface();
        if (Objects.equals(lowerLayerIf, updatedInterface.getName())) {
            LOG.error(
                    "Configuration Error. Attempt to update Vlan Trunk-Member {} with same parent " + "interface name.",
                    updatedInterface);
            return;
        }
        coordinator.enqueueJob(lowerLayerIf, () -> ovsVlanMemberConfigUpdateHelper
                                       .updateConfiguration(parentRefsNew, originalInterface, ifL2vlanNew,
                                                            updatedInterface),
                               IfmConstants.JOB_MAX_RETRIES);
    }

    @Override
    public void add(@Nonnull InstanceIdentifier<Interface> instanceIdentifier,
                    @Nonnull Interface newInterface) {
        IfL2vlan ifL2vlan = newInterface.augmentation(IfL2vlan.class);
        if (ifL2vlan == null || IfL2vlan.L2vlanMode.TrunkMember != ifL2vlan.getL2vlanMode()) {
            return;
        }
        addVlanMember(newInterface);
    }

    private void addVlanMember(Interface added) {
        ParentRefs parentRefs = added.augmentation(ParentRefs.class);
        if (parentRefs == null) {
            return;
        }

        String lowerLayerIf = parentRefs.getParentInterface();
        if (Objects.equals(lowerLayerIf, added.getName())) {
            LOG.error("Attempt to add Vlan Trunk-Member {} with same parent interface name.", added);
            return;
        }

        LOG.info("registering callback on interface-state for {} for proceeding with "
                + "vlan member configuration for interface {}", lowerLayerIf, added.getName());
        eventCallbacks.onAddOrUpdate(LogicalDatastoreType.OPERATIONAL,
                IfmUtil.buildStateInterfaceId(parentRefs.getParentInterface()), (unused, alsoUnused) -> {
                LOG.info("parent interface configuration {} detected for l2vlan-member {},"
                    + "proceeding with state creation", parentRefs.getParentInterface(), added.getName());
                ovsVlanMemberConfigAddHelper.addConfiguration(parentRefs, added);
                return DataTreeEventCallbackRegistrar.NextAction.UNREGISTER;
            });
    }
}
