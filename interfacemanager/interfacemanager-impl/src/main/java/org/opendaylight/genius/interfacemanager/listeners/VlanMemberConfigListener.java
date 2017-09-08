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
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.renderer.ovs.confighelpers.OvsVlanMemberConfigAddHelper;
import org.opendaylight.genius.interfacemanager.renderer.ovs.confighelpers.OvsVlanMemberConfigRemoveHelper;
import org.opendaylight.genius.interfacemanager.renderer.ovs.confighelpers.OvsVlanMemberConfigUpdateHelper;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
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
    private final DataBroker dataBroker;
    private final IdManagerService idManager;
    private final AlivenessMonitorService alivenessMonitorService;
    private final IMdsalApiManager mdsalApiManager;

    @Inject
    public VlanMemberConfigListener(final DataBroker dataBroker, final IdManagerService idManagerService,
            final IMdsalApiManager mdsalApiManager, final AlivenessMonitorService alivenessMonitorService) {
        super(Interface.class, VlanMemberConfigListener.class);
        this.dataBroker = dataBroker;
        this.idManager = idManagerService;
        this.mdsalApiManager = mdsalApiManager;
        this.alivenessMonitorService = alivenessMonitorService;
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
        IfL2vlan ifL2vlan = deleted.getAugmentation(IfL2vlan.class);
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

        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
        RendererConfigRemoveWorker removeWorker = new RendererConfigRemoveWorker(key, deleted, parentRefs, ifL2vlan);
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

        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
        RendererConfigUpdateWorker updateWorker = new RendererConfigUpdateWorker(key, interfaceNew, interfaceOld,
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
        IfL2vlan ifL2vlan = added.getAugmentation(IfL2vlan.class);
        ParentRefs parentRefs = added.getAugmentation(ParentRefs.class);
        if (parentRefs == null) {
            return;
        }

        String lowerLayerIf = parentRefs.getParentInterface();
        if (lowerLayerIf.equals(added.getName())) {
            LOG.error("Attempt to add Vlan Trunk-Member {} with same parent interface name.", added);
            return;
        }

        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
        RendererConfigAddWorker configWorker = new RendererConfigAddWorker(key, added, parentRefs, ifL2vlan);
        coordinator.enqueueJob(lowerLayerIf, configWorker, IfmConstants.JOB_MAX_RETRIES);
    }

    @Override
    protected VlanMemberConfigListener getDataTreeChangeListener() {
        return VlanMemberConfigListener.this;
    }

    private class RendererConfigAddWorker implements Callable<List<ListenableFuture<Void>>> {
        InstanceIdentifier<Interface> key;
        Interface interfaceNew;
        IfL2vlan ifL2vlan;
        ParentRefs parentRefs;

        RendererConfigAddWorker(InstanceIdentifier<Interface> key, Interface interfaceNew, ParentRefs parentRefs,
                IfL2vlan ifL2vlan) {
            this.key = key;
            this.interfaceNew = interfaceNew;
            this.ifL2vlan = ifL2vlan;
            this.parentRefs = parentRefs;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            return OvsVlanMemberConfigAddHelper.addConfiguration(dataBroker, parentRefs, interfaceNew,
                    idManager);
        }
    }

    private class RendererConfigUpdateWorker implements Callable<List<ListenableFuture<Void>>> {
        InstanceIdentifier<Interface> key;
        Interface interfaceNew;
        Interface interfaceOld;
        IfL2vlan ifL2vlanNew;
        ParentRefs parentRefsNew;

        RendererConfigUpdateWorker(InstanceIdentifier<Interface> key, Interface interfaceNew, Interface interfaceOld,
                ParentRefs parentRefsNew, IfL2vlan ifL2vlanNew) {
            this.key = key;
            this.interfaceNew = interfaceNew;
            this.interfaceOld = interfaceOld;
            this.ifL2vlanNew = ifL2vlanNew;
            this.parentRefsNew = parentRefsNew;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            return OvsVlanMemberConfigUpdateHelper.updateConfiguration(dataBroker, alivenessMonitorService,
                    parentRefsNew, interfaceOld, ifL2vlanNew, interfaceNew, idManager, mdsalApiManager);
        }
    }

    private class RendererConfigRemoveWorker implements Callable<List<ListenableFuture<Void>>> {
        InstanceIdentifier<Interface> key;
        Interface interfaceOld;
        IfL2vlan ifL2vlan;
        ParentRefs parentRefs;

        RendererConfigRemoveWorker(InstanceIdentifier<Interface> key, Interface interfaceOld, ParentRefs parentRefs,
                IfL2vlan ifL2vlan) {
            this.key = key;
            this.interfaceOld = interfaceOld;
            this.ifL2vlan = ifL2vlan;
            this.parentRefs = parentRefs;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            return OvsVlanMemberConfigRemoveHelper.removeConfiguration(dataBroker, parentRefs, interfaceOld,
                    idManager);
        }
    }
}
