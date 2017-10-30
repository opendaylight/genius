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
import org.opendaylight.genius.datastoreutils.AsyncDataTreeChangeListenerBase;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.renderer.ovs.confighelpers.OvsVlanMemberConfigAddHelper;
import org.opendaylight.genius.interfacemanager.renderer.ovs.confighelpers.OvsVlanMemberConfigRemoveHelper;
import org.opendaylight.genius.interfacemanager.renderer.ovs.confighelpers.OvsVlanMemberConfigUpdateHelper;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.AlivenessMonitorService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class VlanMemberConfigListener extends AsyncDataTreeChangeListenerBase<Interface, VlanMemberConfigListener> {
    private static final Logger LOG = LoggerFactory.getLogger(VlanMemberConfigListener.class);
    private final JobCoordinator coordinator;
    private final OvsVlanMemberConfigAddHelper ovsVlanMemberConfigAddHelper;
    private final OvsVlanMemberConfigRemoveHelper ovsVlanMemberConfigRemoveHelper;
    private final OvsVlanMemberConfigUpdateHelper ovsVlanMemberConfigUpdateHelper;

    @Inject
    public VlanMemberConfigListener(final DataBroker dataBroker, final IdManagerService idManagerService,
            final IMdsalApiManager mdsalApiManager, final AlivenessMonitorService alivenessMonitorService,
            final JobCoordinator coordinator, final InterfaceManagerCommonUtils interfaceManagerCommonUtils,
            final OvsVlanMemberConfigAddHelper ovsVlanMemberConfigAddHelper,
            final OvsVlanMemberConfigRemoveHelper ovsVlanMemberConfigRemoveHelper,
            final OvsVlanMemberConfigUpdateHelper ovsVlanMemberConfigUpdateHelper) {
        super(Interface.class, VlanMemberConfigListener.class);
        this.coordinator = coordinator;
        this.ovsVlanMemberConfigAddHelper = ovsVlanMemberConfigAddHelper;
        this.ovsVlanMemberConfigRemoveHelper = ovsVlanMemberConfigRemoveHelper;
        this.ovsVlanMemberConfigUpdateHelper = ovsVlanMemberConfigUpdateHelper;
        this.registerListener(LogicalDatastoreType.CONFIGURATION, dataBroker);
    }

    @Override
    protected InstanceIdentifier<Interface> getWildCardPath() {
        return InstanceIdentifier.create(Interfaces.class).child(Interface.class);
    }

    @Override
    protected void remove(InstanceIdentifier<Interface> key, Interface interfaceOld) {
        IfL2vlan ifL2vlan = interfaceOld.getAugmentation(IfL2vlan.class);
        if (ifL2vlan == null || IfL2vlan.L2vlanMode.TrunkMember != ifL2vlan.getL2vlanMode()) {
            return;
        }
        removeVlanMember(key, interfaceOld);
    }

    private void removeVlanMember(InstanceIdentifier<Interface> key, Interface deleted) {
        ParentRefs parentRefs = deleted.getAugmentation(ParentRefs.class);
        if (parentRefs == null) {
            LOG.error("Attempt to remove Vlan Trunk-Member {} without a parent interface", deleted);
            return;
        }

        String lowerLayerIf = parentRefs.getParentInterface();
        if (lowerLayerIf.equals(deleted.getName())) {
            LOG.error("Attempt to remove Vlan Trunk-Member {} with same parent interface name.", deleted);
            return;
        }

        RendererConfigRemoveWorker removeWorker = new RendererConfigRemoveWorker(deleted, parentRefs);
        coordinator.enqueueJob(lowerLayerIf, removeWorker, IfmConstants.JOB_MAX_RETRIES);
    }

    @Override
    protected void update(InstanceIdentifier<Interface> key, Interface interfaceOld, Interface interfaceNew) {
        IfL2vlan ifL2vlanNew = interfaceNew.getAugmentation(IfL2vlan.class);
        if (ifL2vlanNew == null) {
            return;
        }
        IfL2vlan ifL2vlanOld = interfaceOld.getAugmentation(IfL2vlan.class);
        if (IfL2vlan.L2vlanMode.TrunkMember == ifL2vlanNew.getL2vlanMode()
                && IfL2vlan.L2vlanMode.Trunk == ifL2vlanOld.getL2vlanMode()) {
            // Trunk subport add use case
            addVlanMember(key, interfaceNew);
            return;
        } else if (IfL2vlan.L2vlanMode.Trunk == ifL2vlanNew.getL2vlanMode()
                && IfL2vlan.L2vlanMode.TrunkMember == ifL2vlanOld.getL2vlanMode()) {
            // Trunk subport remove use case
            removeVlanMember(key, interfaceOld);
        } else if (IfL2vlan.L2vlanMode.TrunkMember != ifL2vlanNew.getL2vlanMode()) {
            return;
        }

        ParentRefs parentRefsNew = interfaceNew.getAugmentation(ParentRefs.class);
        if (parentRefsNew == null) {
            LOG.error("Configuration Error. Attempt to update Vlan Trunk-Member {} without a " + "parent interface",
                    interfaceNew);
            return;
        }

        String lowerLayerIf = parentRefsNew.getParentInterface();
        if (lowerLayerIf.equals(interfaceNew.getName())) {
            LOG.error(
                    "Configuration Error. Attempt to update Vlan Trunk-Member {} with same parent " + "interface name.",
                    interfaceNew);
            return;
        }

        RendererConfigUpdateWorker updateWorker = new RendererConfigUpdateWorker(interfaceNew, interfaceOld,
                parentRefsNew, ifL2vlanNew);
        coordinator.enqueueJob(lowerLayerIf, updateWorker, IfmConstants.JOB_MAX_RETRIES);
    }

    @Override
    protected void add(InstanceIdentifier<Interface> key, Interface interfaceNew) {
        IfL2vlan ifL2vlan = interfaceNew.getAugmentation(IfL2vlan.class);
        if (ifL2vlan == null || IfL2vlan.L2vlanMode.TrunkMember != ifL2vlan.getL2vlanMode()) {
            return;
        }
        addVlanMember(key, interfaceNew);
    }

    private void addVlanMember(InstanceIdentifier<Interface> key, Interface added) {
        ParentRefs parentRefs = added.getAugmentation(ParentRefs.class);
        if (parentRefs == null) {
            return;
        }

        String lowerLayerIf = parentRefs.getParentInterface();
        if (lowerLayerIf.equals(added.getName())) {
            LOG.error("Attempt to add Vlan Trunk-Member {} with same parent interface name.", added);
            return;
        }

        RendererConfigAddWorker configWorker = new RendererConfigAddWorker(added, parentRefs);
        coordinator.enqueueJob(lowerLayerIf, configWorker, IfmConstants.JOB_MAX_RETRIES);
    }

    @Override
    protected VlanMemberConfigListener getDataTreeChangeListener() {
        return VlanMemberConfigListener.this;
    }

    private class RendererConfigAddWorker implements Callable<List<ListenableFuture<Void>>> {
        Interface interfaceNew;
        ParentRefs parentRefs;

        RendererConfigAddWorker(Interface interfaceNew, ParentRefs parentRefs) {
            this.interfaceNew = interfaceNew;
            this.parentRefs = parentRefs;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            return ovsVlanMemberConfigAddHelper.addConfiguration(parentRefs, interfaceNew);
        }
    }

    private class RendererConfigUpdateWorker implements Callable<List<ListenableFuture<Void>>> {
        Interface interfaceNew;
        Interface interfaceOld;
        IfL2vlan ifL2vlanNew;
        ParentRefs parentRefsNew;

        RendererConfigUpdateWorker(Interface interfaceNew, Interface interfaceOld, ParentRefs parentRefsNew,
                IfL2vlan ifL2vlanNew) {
            this.interfaceNew = interfaceNew;
            this.interfaceOld = interfaceOld;
            this.ifL2vlanNew = ifL2vlanNew;
            this.parentRefsNew = parentRefsNew;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            return ovsVlanMemberConfigUpdateHelper.updateConfiguration(parentRefsNew, interfaceOld, ifL2vlanNew,
                    interfaceNew);
        }
    }

    private class RendererConfigRemoveWorker implements Callable<List<ListenableFuture<Void>>> {
        Interface interfaceOld;
        ParentRefs parentRefs;

        RendererConfigRemoveWorker(Interface interfaceOld, ParentRefs parentRefs) {
            this.interfaceOld = interfaceOld;
            this.parentRefs = parentRefs;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            return ovsVlanMemberConfigRemoveHelper.removeConfiguration(parentRefs, interfaceOld);
        }
    }
}
